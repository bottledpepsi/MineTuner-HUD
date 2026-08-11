package bottled.mtss.hud;

import bottled.mtss.MtssMod;
import bottled.mtss.config.MtssConfig;
import bottled.mtss.config.MtssConfig.Stat;
import bottled.mtss.stat.StatDefinition;
import bottled.mtss.stat.StatRegistry;

import java.util.*;

/** Parses and renders "template lines". */
public final class TemplateEngine {

    private static final Map<Integer, CacheEntry> PARSE_CACHE = new HashMap<>();

    /** Per-list set of bad tokens already warned about, so each is only logged once. */
    private static final Map<Integer, Set<String>> WARNED_BAD_TOKENS = new HashMap<>();

    private TemplateEngine() {
    }

    /** Parsed token lists for {@code cfg.templateLines}, using the cache when the
     *  source hasn't changed since the last parse (see {@link #invalidate(int)} and
     *  the source-hash check below for how staleness is detected). */
    public static List<List<Token>> getParsedLines(MtssConfig.StatListConfig cfg) {
        String joined = String.join("\u0000", cfg.templateLines); // NUL is a safe separator.
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

    /** Drops the cached parse for a list. */
    public static void invalidate(int listId) {
        PARSE_CACHE.remove(listId);
        WARNED_BAD_TOKENS.remove(listId);
    }

    /** Parses a template line into tokens. */
    public static List<Token> parse(int listIdForWarnings, String template) {
        List<Token> out = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int i = 0, len = template.length();

        while (i < len) {
            char c = template.charAt(i);

            if (c == '{') {
                // Escaped "{{".
                if (i + 1 < len && template.charAt(i + 1) == '{') {
                    literal.append('{');
                    i += 2;
                    continue;
                }
                int close = template.indexOf('}', i + 1);
                if (close < 0) {
                    // No closing brace.
                    literal.append(template.substring(i));
                    i = len;
                    continue;
                }
                String body = template.substring(i + 1, close);
                Token parsedToken = tryParseTokenBody(body);
                if (parsedToken != null) {
                    if (!literal.isEmpty()) {
                        out.add(new LiteralToken(literal.toString()));
                        literal.setLength(0);
                    }
                    out.add(parsedToken);
                } else {
                    // Malformed token.
                    warnOnce(listIdForWarnings, body);
                    literal.append('{').append(body).append('}');
                }
                i = close + 1;
                continue;
            }

            if (c == '}') {
                // Escaped "}}".
                if (i + 1 < len && template.charAt(i + 1) == '}') {
                    literal.append('}');
                    i += 2;
                    continue;
                }
                // Lone "}".
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
            if (modifiers.isBlank()) return null; // e.g. "{tps:}" — colon with nothing after it.

            for (String part : modifiers.split(",", -1)) {
                String mod = part.trim();
                if (mod.isEmpty()) return null; // e.g. "{tps:1,,graph=true}" — empty modifier between commas.

                int eq = mod.indexOf('=');
                if (eq >= 0) {
                    String key = mod.substring(0, eq).trim().toLowerCase(java.util.Locale.ROOT);
                    // Color's value is case-sensitive-agnostic hex, but keep the
                    // original casing out of the lowercased "value" used for the
                    // graph=true/false comparison below. parseHexColor() lowercases
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
                        if (parsed == null) return null; // e.g. "{tps:color=notahex}".
                        color = parsed;
                    } else {
                        return null; // unknown key or non-boolean value, e.g. "{tps:graph=maybe}".
                    }
                } else {
                    // Bare token: an integer decimals count, e.g. the "2" in "{tps:2}".
                    try {
                        decimals = Integer.parseInt(mod);
                    } catch (NumberFormatException e) {
                        return null; // e.g. "{tps:abc}" — not a key=value pair or an integer.
                    }
                    if (decimals < 0 || decimals > 6) return null;
                }
            }
        }

        StatDefinition def = StatRegistry.byToken(name.trim().toLowerCase(java.util.Locale.ROOT));
        if (def == null) return null; // typo'd/unknown name, e.g. "{tpss}".

        if (decimals >= 0 && !def.supportsDecimals()) {
            return null; // e.g. "{entities:2}" — ENTITIES has no fractional part to show.
        }
        if (asGraph && !def.supportsGraph()) {
            return null; // e.g. "{biome:graph=true}" — BIOME has no history to plot.
        }

        return new StatToken(def.key(), decimals, asGraph, color);
    }

    /** Parses a CSS-style hex color. */
    private static Integer parseHexColor(String raw) {
        if (raw.length() < 2 || raw.charAt(0) != '#') return null;
        String hex = raw.substring(1);
        if (!hex.matches("[0-9a-fA-F]{3}|[0-9a-fA-F]{6}")) return null;
        if (hex.length() == 3) {
            StringBuilder expanded = new StringBuilder(6);
            for (char c : hex.toCharArray()) {
                expanded.append(c).append(c);
            }
            hex = expanded.toString();
        }
        try {
            return 0xFF000000 | Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return null; // unreachable given the regex guard above, but no reason to risk an uncaught
                         // exception here if that guard is ever loosened later.
        }
    }

    /** Logs each bad token once per list, not every parse. */
    private static void warnOnce(int listId, String badBody) {
        Set<String> warned = WARNED_BAD_TOKENS.computeIfAbsent(listId, k -> new java.util.HashSet<>());
        if (warned.add(badBody)) {
            // The bad token is pre-wrapped in its braces before substitution (rather than
            // e.g. "\"{{}}\"" as the format string) since SLF4J's "{}" placeholder scanner
            // would otherwise misparse literal braces adjacent to the placeholder.
            MtssMod.LOGGER.warn("Template list {}: unrecognized token \"{}\" — rendering it as "
                    + "literal text. Check the token spelling against the README's Template Mode table.",
                    listId, "{" + badBody + "}");
        }
    }

    /** Renders a parsed token list to text, reusing the same {@link #renderStat}
     *  helper that {@link #renderRuns} uses for its per-token value formatting. */
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
        // Safety net: rawValue() is supposed to already be label-free (see
        // StatDefinition#rawValue's contract), but a stat implementation could forget to
        // override it, which bakes in a "Label: " prefix from format() instead.
        // Strip it the same way MtssRenderer does for showPrefix=off, so a
        // stat that forgets to override rawValue() still degrades cleanly to a bare
        // value in a template line, instead of doubling the label in template mode.
        int sep = text.indexOf(": ");
        if (sep >= 0) text = text.substring(sep + 2);
        return text;
    }

    /** Renders a parsed token list to a sequence of colored runs. */
    public static List<ColoredRun> renderRuns(List<Token> tokens, int baseColor) {
        List<ColoredRun> runs = new ArrayList<>();
        StringBuilder pending = new StringBuilder();
        int pendingColor = baseColor;

        for (Token t : tokens) {
            String text;
            int color;
            switch (t) {
                case LiteralToken lit -> {
                    text = lit.text();
                    color = baseColor;
                }
                case StatToken st -> {
                    text = renderStat(st);
                    color = st.color() != null ? st.color() : baseColor;
                }
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

    public sealed interface Token permits LiteralToken, StatToken {
    }

    /** A run of plain text, copied through unchanged. */
    public record LiteralToken(String text) implements Token {
    }

    /** A parsed {@code {statname}} token, optionally with modifiers like
     *  {@code {statname:2}} (decimals), {@code {statname:graph=true}}, or
     *  {@code {statname:color=#RRGGBB}} — see {@link #tryParseTokenBody} for the
     *  full modifier grammar. {@code decimals} is -1 when not specified (meaning
     *  "use the stat's own default"), and {@code color} is null when not specified
     *  (meaning "use the line's base color"). */
    public record StatToken(Stat stat, int decimals, boolean asGraph, Integer color) implements Token {
    }

    /** One contiguously-colored run of rendered text, e.g. the literal text and a
     *  stat's value end up as separate runs whenever a {@code color=} modifier gives
     *  the stat a different color than the surrounding literal text. */
    public record ColoredRun(String text, int color) {
    }

    /** Keyed per list (by {@code MtssConfig.StatListConfig.id}), so parsing happens
     *  once per edit (or once after {@link #invalidate(int)}), not once per frame. */
    private record CacheEntry(String sourceHash, List<List<Token>> perLine) {
    }
}
