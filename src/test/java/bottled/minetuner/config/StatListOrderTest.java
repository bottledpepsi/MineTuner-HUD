package bottled.minetuner.config;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StatListOrderTest {

    @Test
    void unknownNamesAreIgnoredAndDuplicatesCollapse() {
        MineTunerConfig.StatListConfig lc = new MineTunerConfig.StatListConfig(0);
        lc.statOrder = new ArrayList<>(List.of("PING", "NOPE", "TPS", "PING", "tps"));
        lc.setEnabled(MineTunerConfig.Stat.PING, true);
        lc.setEnabled(MineTunerConfig.Stat.TPS, true);
        assertEquals(List.of(MineTunerConfig.Stat.PING, MineTunerConfig.Stat.TPS), lc.getVisibleStats());
    }

    @Test
    void defaultThresholdKeysFollowEnumOrder() {
        List<String> expected = new ArrayList<>();
        for (MineTunerConfig.Stat s : MineTunerConfig.Stat.values()) {
            if (MineTunerConfig.THRESHOLD_STATS.contains(s)) expected.add(s.name());
        }
        for (int i = 0; i < 3; i++) {
            MineTunerConfig.StatListConfig lc = new MineTunerConfig.StatListConfig(i);
            assertEquals(expected, new ArrayList<>(lc.statThresholds.keySet()));
        }
        List<MineTunerConfig.Stat> iter = new ArrayList<>(MineTunerConfig.THRESHOLD_STATS);
        for (int i = 1; i < iter.size(); i++) assertTrue(iter.get(i - 1).ordinal() < iter.get(i).ordinal());
    }

    @Test
    void byNameNeverThrows() {
        assertNull(MineTunerConfig.Stat.byName(null));
        assertNull(MineTunerConfig.Stat.byName("not a stat"));
        assertEquals(MineTunerConfig.Stat.FPS, MineTunerConfig.Stat.byName("fps"));
    }
}
