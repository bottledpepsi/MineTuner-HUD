package bottled.minetuner.config;

import bottled.minetuner.hud.ListPositioner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Malformed or forward-incompatible configs must load (or fall back to defaults with a
 * backup) and must never leave a null enum where the HUD does a switch on it.
 */
class ConfigRobustnessTest {

    @TempDir
    Path dir;

    /**
     * Everything the per-frame render path switches on must be non-null.
     */
    private static void assertRenderSafe(MineTunerConfig cfg) {
        for (MineTunerConfig.StatListConfig l : cfg.lists) {
            assertNotNull(l);
            assertNotNull(l.anchorCorner);
            assertNotNull(l.snapX);
            assertNotNull(l.snapY);
            ListPositioner.getPosition(l, 320, 240, 100, 50); // must not throw
            for (MineTunerConfig.StatSettings ss : l.statSettings.values()) {
                assertNotNull(ss);
                assertNotNull(ss.graphStyle);
                assertNotNull(ss.graphStyle.colorMode);
                assertNotNull(ss.graphStyle.valueDisplay);
            }
        }
    }

    @AfterEach
    void resetPath() {
        MineTunerConfig.setConfigPathSupplier(() -> {
            throw new IllegalStateException("config path not set");
        });
    }

    private MineTunerConfig load(String json) throws IOException {
        Path file = dir.resolve("minetuner.json");
        Files.writeString(file, json);
        MineTunerConfig.setConfigPathSupplier(() -> file);
        return MineTunerConfig.load();
    }

    private long backupCount() throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.getFileName().toString().startsWith("minetuner.json.bak-")).count();
        }
    }

    @Test
    void unknownSnapAndGraphEnumsAreRepaired() throws IOException {
        MineTunerConfig cfg = load("""
                {"lists":[{"id":0,"snapX":"FROM_THE_FUTURE","snapY":"ALSO_NEW",
                 "anchorCorner":"MIDDLE",
                 "statSettings":{"FPS":{"graphStyle":{"colorMode":"RAINBOW","valueDisplay":"EVERYTHING"}}}}]}
                """);
        MineTunerConfig.StatListConfig l = cfg.lists.get(0);
        assertEquals(MineTunerConfig.SnapX.NONE, l.snapX);
        assertEquals(MineTunerConfig.SnapY.NONE, l.snapY);
        assertEquals(MineTunerConfig.Corner.TOP_LEFT, l.anchorCorner);
        assertEquals(MineTunerConfig.GraphColorMode.CURRENT_THRESHOLD,
                l.statSettings.get("FPS").graphStyle.colorMode);
        assertEquals(MineTunerConfig.GraphValueDisplay.CURRENT,
                l.statSettings.get("FPS").graphStyle.valueDisplay);
        assertRenderSafe(cfg);
    }

    @Test
    void nullListElementIsDropped() throws IOException {
        MineTunerConfig cfg = load("{\"lists\":[null,{\"id\":4,\"name\":\"ok\"}]}");
        assertEquals(1, cfg.lists.size());
        assertEquals("ok", cfg.lists.get(0).name);
        assertRenderSafe(cfg);
    }

    @Test
    void wrongTypedFieldFallsBackToDefaultsWithBackup() throws IOException {
        MineTunerConfig cfg = load("{\"lists\":[{\"id\":0,\"textScale\":\"huge\"}]}");
        assertEquals(1, cfg.lists.size());
        assertEquals(1, backupCount());
        assertRenderSafe(cfg);
    }

    @Test
    void absurdNumericsAreClamped() throws IOException {
        MineTunerConfig cfg = load("""
                {"lists":[{"id":0,"textScale":9999,"paddingX":-5,"anchorFracX":42.0,
                 "statSettings":{"FPS":{"decimals":99,"graphStyle":{"width":100000000,"height":-3,"smoothing":50}}}}]}
                """);
        MineTunerConfig.StatListConfig l = cfg.lists.get(0);
        assertEquals(cfg.textScaleMax, l.textScale, 0f);
        assertEquals(0, l.paddingX);
        assertEquals(1.0, l.anchorFracX, 0.0);
        MineTunerConfig.StatSettings fps = l.statSettings.get("FPS");
        assertEquals(6, fps.decimals);
        assertEquals(400, fps.graphStyle.width);
        assertEquals(10, fps.graphStyle.height);
        assertEquals(4, fps.graphStyle.smoothing);
    }

    @Test
    void duplicateIdsAreReassignedAndNextIdCatchesUp() throws IOException {
        MineTunerConfig cfg = load("{\"nextId\":1,\"lists\":[{\"id\":5},{\"id\":5},{\"id\":2}]}");
        long distinct = cfg.lists.stream().map(l -> l.id).distinct().count();
        assertEquals(3, distinct);
        int max = cfg.lists.stream().mapToInt(l -> l.id).max().getAsInt();
        assertTrue(cfg.nextId > max, "nextId " + cfg.nextId + " must exceed max id " + max);
        assertEquals(5, cfg.lists.get(0).id, "first list keeps its id");
    }

    @Test
    void unknownStatNamesArePrunedFromOrder() throws IOException {
        MineTunerConfig cfg = load("{\"lists\":[{\"id\":0,\"statOrder\":[\"TPS\",\"NOT_A_STAT\",null,\"fps\"]}]}");
        for (String n : cfg.lists.get(0).statOrder) {
            assertNotNull(MineTunerConfig.Stat.byName(n), n);
        }
        assertFalse(cfg.lists.get(0).statOrder.contains("NOT_A_STAT"));
        assertEquals("TPS", cfg.lists.get(0).statOrder.get(0));
    }

    @Test
    void nullThemeAndNullGraphStylesAreRepaired() throws IOException {
        MineTunerConfig cfg = load("""
                {"themes":{"Broken":null,"Odd":{"name":"Odd","graphStyles":null}},"lists":[]}
                """);
        assertFalse(cfg.themes.containsKey("Broken"));
        assertNotNull(cfg.themes.get("Odd").graphStyles);
    }
}
