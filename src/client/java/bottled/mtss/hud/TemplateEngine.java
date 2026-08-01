package bottled.mtss.hud;

import bottled.mtss.MtssDataHolder;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.config.MtssConfig.Stat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses and renders freeform "template lines" (step 6) — a small
 * hypertext-style markup grammar that lets a single rendered line mix
 * literal text with interpolated stat tokens, e.g.
 * {@code "FPS: {fps} | TPS: {tps:2} | {ping}ms"}.
 * <p>
 * This is entirely separate from, and does not alter, classic per-stat-line
 * mode — {@code MtssRenderer.buildLines} only calls into this class when
 * {@code StatListConfig.useTemplate} is true for a given list.
 *
 * <h2>Token grammar</h2>
 * <ul>
 *   <li>{@code {token}} — interpolates a stat using its default decimal count.</li>
 *   <li>{@code {token:N}} — interpolates a stat with N decimal places, for
 *       stats that support a decimals setting (see {@link #DECIMAL_TOKENS}).
 *       Ignored (with a one-time warning) for stats that don't.</li>
 *   <li>Doubled braces escape a literal brace character: {@code "{{"} renders
 *       as {@code "{"}, and {@code "}}"} renders as {@code "}"}.</li>
 *   <li>Anything else inside {@code { }} that isn't a recognized token name
 *       is treated as a malformed token: it renders back out as literal text
 *       unchanged (braces and all) rather than throwing or vanishing, so a
 *       typo like {@code {tsp}} is visible and fixable in-game.</li>
 * </ul>
 * See the README's "Template Mode" section for the full token table.
 */
public final class TemplateEngine {

    private TemplateEngine() {}

    // ── Token table ──────────────────────────────────────────────────────────
    // Maps each markup token name (lowercase, as typed in a template string,
    // e.g. "tps" for "{tps}") to the Stat it represents. One entry per
    // existing Stat enum value. Keep this in sync with the "Template Mode"
    // token table in README.md if either changes.
    private static final Map<String, Stat> TOKEN_TO_STAT = new LinkedHashMap<>();
    static {
        TOKEN_TO_STAT.put("tps",       Stat.TPS);
        TOKEN_TO_STAT.put("mspt",      Stat.MSPT);
        TOKEN_TO_STAT.put("fps",       Stat.FPS);
        TOKEN_TO_STAT.put("ping",      Stat.PING);
        TOKEN_TO_STAT.put("mem",       Stat.MEMORY);
        TOKEN_TO_STAT.put("cpu",       Stat.CPU);
        TOKEN_TO_STAT.put("entities",  Stat.ENTITIES);
        TOKEN_TO_STAT.put("chunks",    Stat.CHUNKS);
        TOKEN_TO_STAT.put("rendered",  Stat.RENDERED_SECTIONS);
        TOKEN_TO_STAT.put("coords",    Stat.COORDS);
        TOKEN_TO_STAT.put("facing",    Stat.FACING);
        TOKEN_TO_STAT.put("speed",     Stat.SPEED);
        TOKEN_TO_STAT.put("gc",        Stat.GC_TIME);
        TOKEN_TO_STAT.put("biome",     Stat.BIOME);
        TOKEN_TO_STAT.put("light",     Stat.LIGHT_LEVEL);
        TOKEN_TO_STAT.put("dimension", Stat.DIMENSION);
    }

    /** Stats whose {@code :N} decimals suffix is meaningful (matches StatSettings.decimals' scope). */
    private static final Set<Stat> DECIMAL_TOKENS = Set.of(Stat.TPS, Stat.MSPT, Stat.CPU, Stat.SPEED);

    /** Each stat's built-in default decimal count, used when {@code :N} is omitted — mirrors MtssDataHolder's getFormattedX() no-arg overloads. */
    private static int defaultDecimals(Stat stat) {
        return switch (stat) {
            case SPEED -> 2;
            default -> 1; // TPS, MSPT, CPU
        };
    }

    // ── Token model ──────────────────────────────────────────────────────────

    public sealed interface Token permits LiteralToken, StatToken {}

    /** A run of literal text (including resolved {@code {{}/{@code }}} escapes and malformed-token fallback text) copied through unchanged. */
    public record LiteralToken(String text) implements Token {}

    /** A {@code {statname}} or {@code {statname:N}} reference. decimals is -1 when omitted (use the stat's default). */
    public record StatToken(Stat stat, int decimals) implements Token {}

    // ── Parse cache ──────────────────────────────────────────────────────────
    // Keyed per list (by StatListConfig.id) and invalidated whenever that
    // list's templateLines content changes, so parsing only happens once per
    // edit rather than once per frame. Each cache entry stores the exact
    // joined source it was parsed from, so a stale cache is detected cheaply
    // (a single String.equals) without needing an explicit "dirty" flag
    // threaded through the GUI's save path.
    private record CacheEntry(String sourceHash, List<List<Token>> perLine) {}
    private static final Map<Integer, CacheEntry> PARSE_CACHE = new HashMap<>();

    /** Per-list, per-template-content one-time warning tracker for malformed tokens (see class doc). */
    private static final Map<Integer, Set<String>> WARNED_BAD_TOKENS = new HashMap<>();

    /**
     * Returns the parsed token lists for {@code cfg.templateLines} (one
     * {@code List<Token>} per line), reusing the cached parse unless the
     * joined template content has changed since it was last parsed.
     */
    public static List<List<Token>> getParsedLines(MtssConfig.StatListConfig cfg) {
        String joined = String.join("\u0000", cfg.templateLines); // NUL can't appear in a normal template string, so this is a safe unambiguous join separator
        CacheEntry cached = PARSE_CACHE.get(cfg.id);
        if (cached != null && cached.sourceHash().equals(joined)) {
            return cached.perLine();
        }
        List<List<Token>> parsed = new ArrayList<>(cfg.templateLines.size());
        for (String line : cfg.templateLines) {
            parsed.add(parse(cfg.id, line));
        }
        PARSE_CACHE.put(cfg.id, new CacheEntry(joined, parsed));
        return parsed;
    }

    /** Explicitly drops the cached parse for a list — call after the GUI's template editor saves a change, though getParsedLines() also self-invalidates via the content hash. */
    public static void invalidate(int listId) {
        PARSE_CACHE.remove(listId);
        WARNED_BAD_TOKENS.remove(listId);
    }

    // ── Parser ───────────────────────────────────────────────────────────────

    /**
     * Parses a single template line into literal/stat tokens. Never throws:
     * malformed token bodies fall back to literal text (braces included) so
     * a bad template renders visibly instead of silently losing the line.
     */
    public static List<Token> parse(int listIdForWarnings, String template) {
        List<Token> out = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int i = 0, len = template.length();

        while (i < len) {
            char c = template.charAt(i);

            if (c == '{') {
                // Escaped literal "{{"
                if (i + 1 < len && template.charAt(i + 1) == '{') {
                    literal.append('{');
                    i += 2;
                    continue;
                }
                int close = template.indexOf('}', i + 1);
                if (close < 0) {
                    // No matching close brace at all — rest of the string is literal.
                    literal.append(template.substring(i));
                    i = len;
                    continue;
                }
                String body = template.substring(i + 1, close);
                Token parsedToken = tryParseTokenBody(body);
                if (parsedToken != null) {
                    if (!literal.isEmpty()) { out.add(new LiteralToken(literal.toString())); literal.setLength(0); }
                    out.add(parsedToken);
                } else {
                    // Malformed token (unknown stat name, bad decimals, empty, etc.) —
                    // render the original "{body}" back out as literal text unchanged.
                    warnOnce(listIdForWarnings, body);
                    literal.append('{').append(body).append('}');
                }
                i = close + 1;
                continue;
            }

            if (c == '}') {
                // Escaped literal "}}"
                if (i + 1 < len && template.charAt(i + 1) == '}') {
                    literal.append('}');
                    i += 2;
                    continue;
                }
                // Lone unmatched "}" — treat as literal text (nothing to escape/parse).
                literal.append('}');
                i++;
                continue;
            }

            literal.append(c);
            i++;
        }

        if (!literal.isEmpty()) out.add(new LiteralToken(literal.toString()));
        return out;
    }

    /** Returns the parsed StatToken for a "{...}" body, or null if the body isn't a recognized token. */
    private static StatToken tryParseTokenBody(String body) {
        if (body.isEmpty()) return null;

        String name = body;
        int decimals = -1;
        int colon = body.indexOf(':');
        if (colon >= 0) {
            name = body.substring(0, colon);
            String decStr = body.substring(colon + 1);
            try {
                decimals = Integer.parseInt(decStr.trim());
            } catch (NumberFormatException e) {
                return null; // e.g. "{tps:abc}" — malformed, fall back to literal
            }
            if (decimals < 0 || decimals > 6) return null; // out of MtssDataHolder.fmt()'s supported range
        }

        Stat stat = TOKEN_TO_STAT.get(name.trim().toLowerCase(java.util.Locale.ROOT));
        if (stat == null) return null; // typo'd/unknown stat name, e.g. "{tsp}"

        if (decimals >= 0 && !DECIMAL_TOKENS.contains(stat)) {
            // e.g. "{ping:2}" — Ping has no decimals concept. Treat as malformed
            // rather than silently dropping the suffix, so the user notices.
            return null;
        }

        return new StatToken(stat, decimals);
    }

    /** Logs a one-time-per-list-per-bad-token warning to the client log, not every frame/parse. */
    private static void warnOnce(int listId, String badBody) {
        Set<String> warned = WARNED_BAD_TOKENS.computeIfAbsent(listId, k -> new java.util.HashSet<>());
        if (warned.add(badBody)) {
            System.out.println("[MTSS] Template list " + listId
                    + ": unrecognized token \"{" + badBody + "}\" — rendering it as literal text. "
                    + "Check the token spelling against the README's Template Mode table.");
        }
    }

    // ── Renderer ─────────────────────────────────────────────────────────────

    /**
     * Interpolates a parsed token list into the final line text, reusing the
     * exact same {@code MtssDataHolder.getFormattedX(decimals)} calls classic
     * mode uses — no formatting logic is duplicated here. Prefix/label
     * stripping (showPrefix) from classic mode intentionally does NOT apply:
     * template mode is the user's own literal text, so they simply don't
     * type "TPS: " if they don't want the label.
     */
    public static String render(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token t : tokens) {
            switch (t) {
                case LiteralToken lit -> sb.append(lit.text());
                case StatToken st -> sb.append(renderStat(st));
            }
        }
        return sb.toString();
    }

    private static String renderStat(StatToken token) {
        Stat stat = token.stat();
        int decimals = token.decimals() >= 0 ? token.decimals() : defaultDecimals(stat);
        return switch (stat) {
            case TPS    -> MtssDataHolder.getFormattedTps(decimals);
            case MSPT   -> MtssDataHolder.getFormattedMspt(decimals);
            case FPS    -> MtssDataHolder.getFormattedFps();
            case PING   -> MtssDataHolder.getFormattedPing();
            case MEMORY -> MtssDataHolder.getFormattedMem();
            case CPU    -> MtssDataHolder.getFormattedCpu(decimals);
            case ENTITIES          -> MtssDataHolder.getFormattedEntities();
            case CHUNKS            -> MtssDataHolder.getFormattedChunks();
            case RENDERED_SECTIONS -> MtssDataHolder.getFormattedRendered();
            case COORDS            -> MtssDataHolder.getFormattedCoords();
            case FACING            -> MtssDataHolder.getFormattedFacing();
            case SPEED             -> MtssDataHolder.getFormattedSpeed(decimals);
            case GC_TIME           -> MtssDataHolder.getFormattedGcTime();
            case BIOME             -> MtssDataHolder.getFormattedBiome();
            case LIGHT_LEVEL       -> MtssDataHolder.getFormattedLight();
            case DIMENSION         -> MtssDataHolder.getFormattedDimension();
        };
    }
}
