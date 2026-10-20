package bottled.minetuner.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An old-format config (no themes / overlayEnabled / hardware keys, legacy anchorDx/Dy and
 * "alignment", unknown stat names) must keep loading and normalise without exceptions.
 */
class GoldenConfigTest {

    @Test
    void oldFormatConfigLoadsAndNormalises(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("minetuner.json");
        try (InputStream in = getClass().getResourceAsStream("/golden/minetuner-old-format.json")) {
            assertNotNull(in, "fixture missing");
            Files.copy(in, file);
        }
        MineTunerConfig.setConfigPathSupplier(() -> file);
        try {
            MineTunerConfig cfg = MineTunerConfig.load();

            assertEquals(2, cfg.lists.size());
            assertTrue(cfg.overlayEnabled);
            assertFalse(cfg.hardwareSensorsEnabled);
            assertNotNull(cfg.themes.get(MineTunerConfig.BUILTIN_DEFAULT_THEME));
            assertEquals(MineTunerConfig.BUILTIN_DEFAULT_THEME, cfg.defaultThemeName);

            MineTunerConfig.StatListConfig main = cfg.lists.get(0);
            assertNull(main.anchorDx);
            assertNull(main.anchorDy);
            assertEquals(8 / 320.0, main.anchorFracX, 1e-9);
            assertEquals(12 / 240.0, main.anchorFracY, 1e-9);
            assertTrue(main.isEnabled(MineTunerConfig.Stat.TPS));
            // Every known stat has settings and an order entry after back-fill.
            for (MineTunerConfig.Stat s : MineTunerConfig.Stat.values()) {
                assertTrue(main.statOrder.contains(s.name()), s.name());
                assertNotNull(main.statSettings.get(s.name()).graphStyle);
            }

            MineTunerConfig.StatListConfig second = cfg.lists.get(1);
            assertTrue(second.statSettings.get("MEMORY").renderAsGraph);
            assertEquals(100, second.statSettings.get("MEMORY").graphStyle.width);
            assertNotNull(second.anchorCorner);
        } finally {
            MineTunerConfig.setConfigPathSupplier(() -> {
                throw new IllegalStateException("config path not set");
            });
        }
    }
}
