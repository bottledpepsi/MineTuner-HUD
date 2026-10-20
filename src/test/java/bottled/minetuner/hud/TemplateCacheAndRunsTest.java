package bottled.minetuner.hud;

import bottled.minetuner.config.MineTunerConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TemplateCacheAndRunsTest {

    private static MineTunerConfig.StatListConfig list(int id, String... lines) {
        MineTunerConfig.StatListConfig lc = new MineTunerConfig.StatListConfig(id);
        lc.templateLines = new ArrayList<>(List.of(lines));
        return lc;
    }

    @Test
    void unchangedLinesReturnTheSameParsedInstance() {
        MineTunerConfig.StatListConfig lc = list(9001, "hello", "world");
        var first = TemplateEngine.getParsedLines(lc);
        assertSame(first, TemplateEngine.getParsedLines(lc));
    }

    @Test
    void setAddAndRemoveInvalidate() {
        MineTunerConfig.StatListConfig lc = list(9002, "a", "b");
        var first = TemplateEngine.getParsedLines(lc);
        lc.templateLines.set(0, "changed");
        var afterSet = TemplateEngine.getParsedLines(lc);
        assertNotSame(first, afterSet);
        lc.templateLines.add("c");
        var afterAdd = TemplateEngine.getParsedLines(lc);
        assertNotSame(afterSet, afterAdd);
        assertEquals(3, afterAdd.size());
        lc.templateLines.remove(0);
        assertEquals(2, TemplateEngine.getParsedLines(lc).size());
    }

    @Test
    void joinedRunTextEqualsLiteralConcatenation() {
        java.util.Random rnd = new java.util.Random(3);
        for (int iter = 0; iter < 500; iter++) {
            List<TemplateEngine.Token> tokens = new ArrayList<>();
            StringBuilder expected = new StringBuilder();
            int n = rnd.nextInt(8);
            for (int i = 0; i < n; i++) {
                String text = rnd.nextInt(4) == 0 ? "" : "t" + rnd.nextInt(100);
                tokens.add(new TemplateEngine.LiteralToken(text));
                expected.append(text);
            }
            var runs = TemplateEngine.renderRuns(tokens, 0xFFFFFFFF);
            assertEquals(expected.toString(), LineBuilder.joinRunText(runs));
        }
    }
}
