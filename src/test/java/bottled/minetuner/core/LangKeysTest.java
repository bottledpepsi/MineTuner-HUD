package bottled.minetuner.core;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LangKeysTest {

    @Test
    void keysAreLocaleIndependent() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("stat.minetuner.ping", LangKeys.stat(Sample.PING));
            assertEquals("stat.minetuner.frametime", LangKeys.stat(Sample.FRAMETIME));
            assertEquals("stat.minetuner.light_level", LangKeys.stat(Sample.LIGHT_LEVEL));
            assertEquals("stat.minetuner.difficulty", LangKeys.stat(Sample.DIFFICULTY));
            assertEquals("gui.minetuner.category.ping", LangKeys.category(Sample.PING));
            assertEquals("gui.minetuner.corner.light_level",
                    LangKeys.enumValue("gui.minetuner.corner.", Sample.LIGHT_LEVEL));
        } finally {
            Locale.setDefault(saved);
        }
    }

    private enum Sample {PING, FRAMETIME, LIGHT_LEVEL, DIFFICULTY}
}
