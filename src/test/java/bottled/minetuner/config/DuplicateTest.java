package bottled.minetuner.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateTest {

    @Test
    void duplicateCopiesEveryField() throws Exception {
        MineTunerConfig.StatListConfig src = new MineTunerConfig.StatListConfig(3);
        // anchorDx/Dy are legacy and deliberately never copied; set them aside from the fill.
        ReflectionFill.fill(src, Set.of("anchorDx", "anchorDy"));

        MineTunerConfig.StatListConfig copy = src.duplicate(9);

        assertEquals(9, copy.id);
        assertEquals(src.displayName() + " (copy)", copy.name);
        assertEquals(src.anchorFracX + 0.02, copy.anchorFracX, 1e-9);
        assertEquals(src.anchorFracY + 0.02, copy.anchorFracY, 1e-9);
        assertNull(copy.anchorDx);
        assertNull(copy.anchorDy);

        List<String> differences = ReflectionFill.diff(src, copy,
                Set.of("id", "name", "anchorFracX", "anchorFracY", "anchorDx", "anchorDy"));
        assertTrue(differences.isEmpty(), "fields not copied: " + differences);
    }

    @Test
    void duplicateIsDeep() {
        MineTunerConfig.StatListConfig src = new MineTunerConfig.StatListConfig(1);
        src.backFill();
        MineTunerConfig.StatListConfig copy = src.duplicate(2);
        copy.getStatSettings(MineTunerConfig.Stat.TPS).graphStyle.width = 999;
        copy.statOrder.clear();
        assertNotEquals(999, src.getStatSettings(MineTunerConfig.Stat.TPS).graphStyle.width);
        assertFalse(src.statOrder.isEmpty());
    }

    @Test
    void themeCaptureApplyRoundTrip() throws Exception {
        MineTunerConfig.StatListConfig src = new MineTunerConfig.StatListConfig(1);
        src.backFill();
        src.overrideFillColor = 0x11223344;
        src.overrideOutlineColor = 0x55667788;
        src.paddingX = 11;
        src.getStatSettings(MineTunerConfig.Stat.FPS).graphStyle.height = 77;

        ListTheme theme = ListTheme.captureFrom("T", src);
        MineTunerConfig.StatListConfig dst = new MineTunerConfig.StatListConfig(2);
        dst.backFill();
        theme.applyTo(dst);

        assertEquals(0x11223344, dst.overrideFillColor);
        assertEquals(0x55667788, dst.overrideOutlineColor);
        assertEquals(11, dst.paddingX);
        assertEquals(77, dst.getStatSettings(MineTunerConfig.Stat.FPS).graphStyle.height);

        ListTheme dup = theme.duplicate();
        assertTrue(ReflectionFill.diff(theme, dup, Set.of()).isEmpty());
    }
}
