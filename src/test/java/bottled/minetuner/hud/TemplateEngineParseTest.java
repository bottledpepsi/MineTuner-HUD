package bottled.minetuner.hud;

import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.hud.TemplateEngine.LiteralToken;
import bottled.minetuner.hud.TemplateEngine.StatToken;
import bottled.minetuner.hud.TemplateEngine.Token;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Grammar coverage for {@link TemplateEngine#parse}: literals, escapes, token lookup,
 * and the full modifier grammar (decimals, graph, color) including every rejection
 * path. The parser is the richest pure logic in the mod and previously had no direct
 * tests — only its cache identity was covered by TemplateCacheAndRunsTest.
 */
class TemplateEngineParseTest {

    private static StatToken stat(List<Token> tokens, int index) {
        return assertInstanceOf(StatToken.class, tokens.get(index));
    }

    private static String literal(List<Token> tokens, int index) {
        return assertInstanceOf(LiteralToken.class, tokens.get(index)).text();
    }

    // --- Literals ---

    @Test
    void plainTextIsOneLiteralToken() {
        List<Token> tokens = TemplateEngine.parse(1, "hello world");
        assertEquals(1, tokens.size());
        assertEquals("hello world", literal(tokens, 0));
    }

    @Test
    void emptyTemplateParsesToNoTokens() {
        assertTrue(TemplateEngine.parse(1, "").isEmpty());
    }

    @Test
    void loneClosingBraceIsLiteral() {
        List<Token> tokens = TemplateEngine.parse(1, "a}b");
        assertEquals(1, tokens.size());
        assertEquals("a}b", literal(tokens, 0));
    }

    @Test
    void missingClosingBracePreservesText() {
        List<Token> tokens = TemplateEngine.parse(1, "{tps");
        assertEquals(1, tokens.size());
        assertEquals("{tps", literal(tokens, 0));
    }

    // --- Escapes ---

    @Test
    void doubleOpenBraceEscapesToSingleBrace() {
        List<Token> tokens = TemplateEngine.parse(1, "{{");
        assertEquals(1, tokens.size());
        assertEquals("{", literal(tokens, 0));
    }

    @Test
    void doubleCloseBraceEscapesToSingleBrace() {
        List<Token> tokens = TemplateEngine.parse(1, "}}");
        assertEquals(1, tokens.size());
        assertEquals("}", literal(tokens, 0));
    }

    @Test
    void escapeAroundTokenYieldsLiteralBraceTokenLiteralBrace() {
        List<Token> tokens = TemplateEngine.parse(1, "{{{tps}}}");
        assertEquals(3, tokens.size());
        assertEquals("{", literal(tokens, 0));
        assertEquals(MineTunerConfig.Stat.TPS, stat(tokens, 1).stat());
        assertEquals("}", literal(tokens, 2));
    }

    // --- Token lookup ---

    @Test
    void simpleTokenResolvesStat() {
        List<Token> tokens = TemplateEngine.parse(1, "{tps}");
        assertEquals(1, tokens.size());
        StatToken st = stat(tokens, 0);
        assertEquals(MineTunerConfig.Stat.TPS, st.stat());
        assertEquals(-1, st.decimals());
        assertEquals(false, st.asGraph());
        assertNull(st.color());
    }

    @Test
    void tokenNameIsCaseInsensitive() {
        assertEquals(MineTunerConfig.Stat.TPS, stat(TemplateEngine.parse(1, "{TPS}"), 0).stat());
        assertEquals(MineTunerConfig.Stat.TPS, stat(TemplateEngine.parse(1, "{Tps}"), 0).stat());
    }

    @Test
    void tokenNameIsTrimmed() {
        assertEquals(MineTunerConfig.Stat.TPS, stat(TemplateEngine.parse(1, "{ tps }"), 0).stat());
    }

    @Test
    void unknownTokenFallsBackToLiteralText() {
        List<Token> tokens = TemplateEngine.parse(1, "{tpss}");
        assertEquals(1, tokens.size());
        assertEquals("{tpss}", literal(tokens, 0));
    }

    // --- Decimals modifier ---

    @Test
    void integerModifierIsDecimals() {
        assertEquals(2, stat(TemplateEngine.parse(1, "{tps:2}"), 0).decimals());
    }

    @Test
    void zeroDecimalsIsValid() {
        assertEquals(0, stat(TemplateEngine.parse(1, "{tps:0}"), 0).decimals());
    }

    @Test
    void decimalsAboveSixIsRejected() {
        assertEquals("{tps:7}", literal(TemplateEngine.parse(1, "{tps:7}"), 0));
    }

    @Test
    void negativeDecimalsIsRejected() {
        assertEquals("{tps:-1}", literal(TemplateEngine.parse(1, "{tps:-1}"), 0));
    }

    @Test
    void decimalsOnNonDecimalStatIsRejected() {
        // ENTITIES has no fractional part, so {entities:2} is malformed.
        assertEquals("{entities:2}", literal(TemplateEngine.parse(1, "{entities:2}"), 0));
    }

    // --- Graph modifier ---

    @Test
    void graphTrueParses() {
        assertTrue(stat(TemplateEngine.parse(1, "{fps:graph=true}"), 0).asGraph());
    }

    @Test
    void graphFalseParses() {
        assertEquals(false, stat(TemplateEngine.parse(1, "{fps:graph=false}"), 0).asGraph());
    }

    @Test
    void graphOnNonGraphStatIsRejected() {
        assertEquals("{biome:graph=true}", literal(TemplateEngine.parse(1, "{biome:graph=true}"), 0));
    }

    @Test
    void graphNonBooleanValueIsRejected() {
        assertEquals("{fps:graph=maybe}", literal(TemplateEngine.parse(1, "{fps:graph=maybe}"), 0));
    }

    // --- Color modifier ---

    @Test
    void sixDigitHexColorParses() {
        assertEquals(0xFF10FF20, stat(TemplateEngine.parse(1, "{tps:color=#10FF20}"), 0).color());
    }

    @Test
    void threeDigitHexColorIsExpanded() {
        assertEquals(0xFF00FF00, stat(TemplateEngine.parse(1, "{fps:color=#0f0}"), 0).color());
    }

    @Test
    void colorWithoutHashIsRejected() {
        assertEquals("{fps:color=00ff00}", literal(TemplateEngine.parse(1, "{fps:color=00ff00}"), 0));
    }

    @Test
    void colorWithWrongLengthIsRejected() {
        assertEquals("{fps:color=#00ff}", literal(TemplateEngine.parse(1, "{fps:color=#00ff}"), 0));
    }

    @Test
    void colorWithNonHexIsRejected() {
        assertEquals("{fps:color=zzz}", literal(TemplateEngine.parse(1, "{fps:color=zzz}"), 0));
    }

    // --- Malformed modifier syntax ---

    @Test
    void colonWithNothingAfterIsRejected() {
        assertEquals("{tps:}", literal(TemplateEngine.parse(1, "{tps:}"), 0));
    }

    @Test
    void blankOnlyModifiersIsRejected() {
        assertEquals("{tps: }", literal(TemplateEngine.parse(1, "{tps: }"), 0));
    }

    @Test
    void emptyModifierBetweenCommasIsRejected() {
        assertEquals("{tps:1,,graph=true}", literal(TemplateEngine.parse(1, "{tps:1,,graph=true}"), 0));
    }

    @Test
    void unknownModifierKeyIsRejected() {
        assertEquals("{tps:bogus=1}", literal(TemplateEngine.parse(1, "{tps:bogus=1}"), 0));
    }

    @Test
    void nonIntegerBareModifierIsRejected() {
        assertEquals("{tps:abc}", literal(TemplateEngine.parse(1, "{tps:abc}"), 0));
    }

    @Test
    void multipleModifiersCombine() {
        StatToken st = stat(TemplateEngine.parse(1, "{tps:2,graph=true,color=#abc}"), 0);
        assertEquals(2, st.decimals());
        assertTrue(st.asGraph());
        assertEquals(0xFFAABBCC, st.color());
    }

    // --- Mixed lines ---

    @Test
    void literalAndTokensInterleave() {
        List<Token> tokens = TemplateEngine.parse(1, "FPS: {fps}!");
        assertEquals(3, tokens.size());
        assertEquals("FPS: ", literal(tokens, 0));
        assertEquals(MineTunerConfig.Stat.FPS, stat(tokens, 1).stat());
        assertEquals("!", literal(tokens, 2));
    }

    @Test
    void adjacentTokensParseWithoutSeparator() {
        List<Token> tokens = TemplateEngine.parse(1, "{tps}{fps}");
        assertEquals(2, tokens.size());
        assertEquals(MineTunerConfig.Stat.TPS, stat(tokens, 0).stat());
        assertEquals(MineTunerConfig.Stat.FPS, stat(tokens, 1).stat());
    }

    @Test
    void malformedThenValidTokenOnSameLine() {
        // The malformed token's literal text merges with the following space into
        // one literal run before the valid token starts.
        List<Token> tokens = TemplateEngine.parse(1, "{nope} {tps}");
        assertEquals(2, tokens.size());
        assertEquals("{nope} ", literal(tokens, 0));
        assertEquals(MineTunerConfig.Stat.TPS, stat(tokens, 1).stat());
    }
}
