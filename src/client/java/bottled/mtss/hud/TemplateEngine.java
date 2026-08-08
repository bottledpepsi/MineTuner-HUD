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
 *   <li>{@code {token:graph=true}} — renders that stat as a rolling history
 *       graph instead of text, for stats that support it (see
 *       {@code StatDefinition.supportsGraph()}). The graph uses that stat's
 *       normal {@code GraphStyle} settings (same lazily-created per-list
 *       settings classic mode's graph mode uses). A graph token must be the
 *       line's only content — see "Graph tokens" below.</li>
 *   <li>{@code {token:color=#RRGGBB}} — renders just that token's value in a
 *       fixed color, overriding both the list's normal text color and any
 *       threshold coloring for that one value. {@code #RGB} shorthand
 *       (3 hex digits) is also accepted, same as CSS. See "Inline coloring"
 *       below.</li>
 *   <li>Modifiers after {@code :} are comma-separated, e.g.
 *       {@code {tps:2,graph=true}} sets both decimals and graph mode at
 *       once, and {@code {tps:2,color=#55FF55}} sets decimals and an inline
 *       color together. A bare number is always decimals; {@code graph=true}
 *       / {@code graph=false} toggles graph mode explicitly.</li>
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
 *
 * <h2>Inline coloring</h2>
 * A template line renders as a single flat color by default (the list's
 * override color, or white) — the same as before this feature existed, so
 * an unmodified template's appearance never changes. Adding
 * {@code color=#RRGGBB} to one token's modifiers colors just that token's
 * rendered value; every other token and all literal text on the line keep
 * the line's normal color. This is a per-token override, not a per-stat
 * default — it doesn't touch {@code StatDefinition.color()} or the
 * threshold system, so classic mode and graph-row coloring are unaffected.
 * Rendering therefore produces a sequence of colored runs
 * ({@link ColoredRun}) instead of one flat string — see {@link #renderRuns}.
 *
 * <h2>Graph tokens</h2>
 * A classic-mode stat line is either all text or all graph — there's no such
 * thing as a graph with text stitched onto the same row. Template mode keeps
 * that rule: a line renders as a graph row only when a {@code graph=true}
 * token is the <em>entire</em> line (no literal text, no other tokens). A
 * graph token mixed with anything else on the same line is treated like any
 * other malformed token — it falls back to literal text (rendering the
 * stat's plain value) with a one-time warning, rather than silently dropping
 * either the graph or the surrounding text.
 */
public final class TemplateEngine {

    private TemplateEngine() {}

    // ── Token model ──────────────────────────────────────────────────────────

    public sealed interface Token permits LiteralToken, StatToken {}

    /** A run of plain text, copied through unchanged. */
    public record LiteralToken(String text) implements Token {}

    /** A {@code {statname}}, {@code {statname:N}}, {@code {statname:graph=true}}, and/or {@code {statname:color=#RRGGBB}} reference. decimals is -1 if omitted, color is null if omitted. */
    public record StatToken(Stat stat, int decimals, boolean asGraph, Integer color) implements Token {}

    /** One contiguously-colored run of rendered text, e.g. one token's value or a literal-text span. Never empty — {@link #renderRuns} skips zero-length runs. */
    public record ColoredRun(String text, int color) {}

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
        boolean asGraph = false;
        Integer color = null;

        int colon = body.indexOf(':');
        if (colon >= 0) {
            name = body.substring(0, colon);
            String modifiers = body.substring(colon + 1);
            if (modifiers.isBlank()) return null; // e.g. "{tps:}"

            for (String part : modifiers.split(",", -1)) {
                String mod = part.trim();
                if (mod.isEmpty()) return null; // e.g. "{tps:2,,graph=true}"

                int eq = mod.indexOf('=');
                if (eq >= 0) {
                    String key = mod.substring(0, eq).trim().toLowerCase(java.util.Locale.ROOT);
                    // Color's value is case-sensitive-agnostic hex, but keep the
                    // original casing out of the lowercased "value" used for the
                    // graph=true/false comparison below — parseHexColor lowercases
                    // internally anyway, so this is just keeping the two checks
                    // clearly separate rather than reusing one variable for both.
                    String rawValue = mod.substring(eq + 1).trim();
                    String value = rawValue.toLowerCase(java.util.Locale.ROOT);
                    if (key.equals("graph") && value.equals("true")) {
                        asGraph = true;
                    } else if (key.equals("graph") && value.equals("false")) {
                        asGraph = false;
                    } else if (key.equals("color")) {
                        Integer parsed = parseHexColor(rawValue);
                        if (parsed == null) return null; // e.g. "{tps:color=red}", "{tps:color=#12}"
                        color = parsed;
                    } else {
                        return null; // unknown key or non-boolean value, e.g. "{tps:foo=1}", "{tps:graph=yes}"
                    }
                } else {
                    // Bare token — must be the decimals count, e.g. "{tps:2}"
                    try {
                        decimals = Integer.parseInt(mod);
                    } catch (NumberFormatException e) {
                        return null; // e.g. "{tps:abc}"
                    }
                    if (decimals < 0 || decimals > 6) return null;
                }
            }
        }

        StatDefinition def = StatRegistry.byToken(name.trim().toLowerCase(java.util.Locale.ROOT));
        if (def == null) return null; // typo'd/unknown name, e.g. "{tsp}"

        if (decimals >= 0 && !def.supportsDecimals()) {
            return null; // e.g. "{ping:2}" — Ping has no decimals
        }
        if (asGraph && !def.supportsGraph()) {
            return null; // e.g. "{ping:graph=true}" — Ping has no graph history
        }

        return new StatToken(def.key(), decimals, asGraph, color);
    }

    /**
     * Parses a CSS-style hex color: {@code #RRGGBB} or the 3-digit shorthand
     * {@code #RGB} (each digit doubled, e.g. {@code #5F0} -&gt;
     * {@code #55FF00}, matching CSS's own shorthand expansion). Returns an
     * opaque ARGB int (alpha forced to 0xFF — templates aren't compositing
     * translucent text over each other, so there's no use case for a
     * per-token alpha channel) or null if the string isn't a valid hex color.
     */
    private static Integer parseHexColor(String raw) {
        if (raw.length() < 2 || raw.charAt(0) != '#') return null;
        String hex = raw.substring(1);
        if (!hex.matches("[0-9a-fA-F]{3}|[0-9a-fA-F]{6}")) return null;
        if (hex.length() == 3) {
            StringBuilder expanded = new StringBuilder(6);
            for (char c : hex.toCharArray()) { expanded.append(c).append(c); }
            hex = expanded.toString();
        }
        try {
            return 0xFF000000 | Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return null; // unreachable given the regex guard above, but no reason to risk an uncaught throw from the parser
        }
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

    /**
     * Renders a parsed token list to a sequence of colored runs: literal
     * text and {@code color=}-less tokens use {@code baseColor} (the list's
     * normal text color); a token with an inline {@code color=#RRGGBB}
     * modifier gets its own run in that color instead. Adjacent runs that
     * end up the same color are merged, so a template with no inline colors
     * at all collapses back down to a single run — same rendering cost as
     * the old flat-string path in that common case.
     */
    public static List<ColoredRun> renderRuns(List<Token> tokens, int baseColor) {
        List<ColoredRun> runs = new ArrayList<>();
        StringBuilder pending = new StringBuilder();
        int pendingColor = baseColor;

        for (Token t : tokens) {
            String text;
            int color;
            switch (t) {
                case LiteralToken lit -> { text = lit.text(); color = baseColor; }
                case StatToken st -> { text = renderStat(st); color = st.color() != null ? st.color() : baseColor; }
            }
            if (text.isEmpty()) continue;

            if (color == pendingColor) {
                pending.append(text);
            } else {
                if (!pending.isEmpty()) runs.add(new ColoredRun(pending.toString(), pendingColor));
                pending.setLength(0);
                pending.append(text);
                pendingColor = color;
            }
        }
        if (!pending.isEmpty()) runs.add(new ColoredRun(pending.toString(), pendingColor));
        return runs;
    }
}
