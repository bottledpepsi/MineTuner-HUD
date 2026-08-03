package bottled.mtss.hud;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.config.MtssConfig.Stat;
import bottled.mtss.stat.StatDefinition;
import bottled.mtss.stat.StatRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses and renders "template lines" — freeform text with stat tokens mixed
 * in, e.g. {@code "FPS: {fps} | TPS: {tps:2} | {ping}ms"}. Used instead of
 * classic per-stat-line mode when {@code StatListConfig.useTemplate} is true.
 *
 * <h2>Token grammar</h2>
 * <ul>
 *   <li>{@code {token}} — a stat's value, using its default decimal count.</li>
 *   <li>{@code {token:N}} — N decimal places, for stats that support decimals
 *       (see {@code StatDefinition.supportsDecimals()}). Ignored (with a
 *       warning) otherwise.</li>
 *   <li>{@code "{{"} and {@code "}}"} escape literal {@code {} and {@code }}.</li>
 *   <li>Anything else in braces that isn't a known token is malformed and
 *       renders back out as plain text, so a typo is visible, not silent.</li>
 * </ul>
 * Stat tokens render as bare values via {@link StatDefinition#rawValue}, with
 * no "Label: " prefix and no unit suffix (e.g. {speed} -&gt; "1.0", not
 * "Speed: 1.0 b/s") — the user's own surrounding literal text is the label.
 * Token names and their decimal support both come from {@link StatRegistry}
 * now — a new stat's token "just works" here as soon as it's registered.
 * Full token table: README's "Template Mode" section.
 */
public final class TemplateEngine {

    private TemplateEngine() {}

    // ── Token model ──────────────────────────────────────────────────────────

    public sealed interface Token permits LiteralToken, StatToken {}

    /** A run of plain text, copied through unchanged. */
    public record LiteralToken(String text) implements Token {}

    /** A {@code {statname}} or {@code {statname:N}} reference. decimals is -1 if omitted. */
    public record StatToken(Stat stat, int decimals) implements Token {}

    // ── Parse cache ──────────────────────────────────────────────────────────
    // Keyed per list. Reparses only when that list's template text changes,
    // so parsing happens once per edit, not once per frame.
    private record CacheEntry(String sourceHash, List<List<Token>> perLine) {}
    private static final Map<Integer, CacheEntry> PARSE_CACHE = new HashMap<>();

    /** Per-list set of bad tokens already warned about, so each is only logged once. */
    private static final Map<Integer, Set<String>> WARNED_BAD_TOKENS = new HashMap<>();

    /** Parsed token lists for {@code cfg.templateLines}, using the cache when the template text hasn't changed. */
    public static List<List<Token>> getParsedLines(MtssConfig.StatListConfig cfg) {
        String joined = String.join("\u0000", cfg.templateLines); // NUL is a safe separator; can't appear in a template string
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

    /** Drops the cached parse for a list. getParsedLines() also self-invalidates on content change, but call this after saving an edit. */
    public static void invalidate(int listId) {
        PARSE_CACHE.remove(listId);
        WARNED_BAD_TOKENS.remove(listId);
    }

    // ── Parser ───────────────────────────────────────────────────────────────

    /** Parses a template line into tokens. Never throws — malformed tokens fall back to literal text. */
    public static List<Token> parse(int listIdForWarnings, String template) {
        List<Token> out = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int i = 0, len = template.length();

        while (i < len) {
            char c = template.charAt(i);

            if (c == '{') {
                // Escaped "{{"
                if (i + 1 < len && template.charAt(i + 1) == '{') {
                    literal.append('{');
                    i += 2;
                    continue;
                }
                int close = template.indexOf('}', i + 1);
                if (close < 0) {
                    // No closing brace — rest of the string is literal.
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
                    // Malformed token — render the original "{body}" back as literal text.
                    warnOnce(listIdForWarnings, body);
                    literal.append('{').append(body).append('}');
                }
                i = close + 1;
                continue;
            }

            if (c == '}') {
                // Escaped "}}"
                if (i + 1 < len && template.charAt(i + 1) == '}') {
                    literal.append('}');
                    i += 2;
                    continue;
                }
                // Lone "}" — just literal text.
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

    /** Parses a "{...}" body into a token, or returns null if it isn't a recognized one. */
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
                return null; // e.g. "{tps:abc}"
            }
            if (decimals < 0 || decimals > 6) return null;
        }

        StatDefinition def = StatRegistry.byToken(name.trim().toLowerCase(java.util.Locale.ROOT));
        if (def == null) return null; // typo'd/unknown name, e.g. "{tsp}"

        if (decimals >= 0 && !def.supportsDecimals()) {
            return null; // e.g. "{ping:2}" — Ping has no decimals
        }

        return new StatToken(def.key(), decimals);
    }

    /** Logs each bad token once per list, not every parse. */
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
     * Renders a parsed token list to text, reusing the same
     * {@code StatDefinition.format()} call classic mode uses. Classic mode's
     * prefix/label stripping doesn't apply here — the user's own literal
     * text is the label.
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
        StatDefinition def = StatRegistry.get(token.stat());
        int decimals = token.decimals() >= 0 ? token.decimals() : def.defaultDecimals();
        String text = def.rawValue(decimals);
        // Safety net: rawValue() defaults to format() for stats that don't
        // override it, which bakes in a "Label: " prefix for classic mode.
        // Strip it the same way MtssRenderer does for showPrefix=off, so a
        // stat that forgets to override rawValue() still degrades cleanly
        // instead of doubling the label in template mode.
        int sep = text.indexOf(": ");
        if (sep >= 0) text = text.substring(sep + 2);
        return text;
    }
}
