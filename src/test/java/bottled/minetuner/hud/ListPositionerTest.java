package bottled.minetuner.hud;

import bottled.minetuner.config.MineTunerConfig.Corner;
import bottled.minetuner.config.MineTunerConfig.SnapX;
import bottled.minetuner.config.MineTunerConfig.SnapY;
import bottled.minetuner.config.MineTunerConfig.StatListConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * Corner/snap/clamp math of {@link ListPositioner}: the pure position resolution the
 * HUD and the drag editor both rely on. Previously only smoke-tested indirectly via
 * ConfigRobustnessTest's render-safety invariant.
 */
class ListPositionerTest {

    private static StatListConfig list() {
        return new StatListConfig(0); // TOP_LEFT, 0.01/0.01, no snap.
    }

    private static final int W = 320;
    private static final int H = 240;

    @Test
    void topLeftDefaultPlacesAtFractionOffset() {
        // round(0.01*320)=3; round(0.01*240)=2, then floored up to the 3px
        // SCREEN_INSET by the final clamp (Math.max(minY, ...)).
        assertArrayEquals(new int[]{3, 3},
                ListPositioner.getPosition(list(), W, H, 50, 20));
    }

    @Test
    void topRightMirrorsX() {
        StatListConfig cfg = list();
        cfg.anchorCorner = Corner.TOP_RIGHT;
        cfg.anchorFracX = 0.1;
        // x = 320 - 50 - 32 = 238; y = max(3, 2) = 3.
        assertArrayEquals(new int[]{238, 3},
                ListPositioner.getPosition(cfg, W, H, 50, 20));
    }

    @Test
    void bottomLeftMirrorsY() {
        StatListConfig cfg = list();
        cfg.anchorCorner = Corner.BOTTOM_LEFT;
        cfg.anchorFracY = 0.1;
        // y = 240 - 20 - 24 = 196; x = 3.
        assertArrayEquals(new int[]{3, 196},
                ListPositioner.getPosition(cfg, W, H, 50, 20));
    }

    @Test
    void bottomRightMirrorsBoth() {
        StatListConfig cfg = list();
        cfg.anchorCorner = Corner.BOTTOM_RIGHT;
        cfg.anchorFracX = 0.0;
        cfg.anchorFracY = 0.0;
        // x = max(3, min(267, 320-50-0)) = 267; y = max(3, min(217, 220)) = 217.
        assertArrayEquals(new int[]{267, 217},
                ListPositioner.getPosition(cfg, W, H, 50, 20));
    }

    @Test
    void snapXVariantsAnchorToCenterLine() {
        StatListConfig left = list();
        left.snapX = SnapX.LEFT_ON_CENTER;
        assertArrayEquals(new int[]{160, 3},
                ListPositioner.getPosition(left, W, H, 50, 20));

        StatListConfig center = list();
        center.snapX = SnapX.CENTER_ON_CENTER;
        assertArrayEquals(new int[]{135, 3},
                ListPositioner.getPosition(center, W, H, 50, 20));

        StatListConfig right = list();
        right.snapX = SnapX.RIGHT_ON_CENTER;
        assertArrayEquals(new int[]{110, 3},
                ListPositioner.getPosition(right, W, H, 50, 20));
    }

    @Test
    void snapYVariantsAnchorToCenterLine() {
        StatListConfig top = list();
        top.snapY = SnapY.TOP_ON_CENTER;
        assertArrayEquals(new int[]{3, 120},
                ListPositioner.getPosition(top, W, H, 50, 20));

        StatListConfig center = list();
        center.snapY = SnapY.CENTER_ON_CENTER;
        assertArrayEquals(new int[]{3, 110},
                ListPositioner.getPosition(center, W, H, 50, 20));

        StatListConfig bottom = list();
        bottom.snapY = SnapY.BOTTOM_ON_CENTER;
        assertArrayEquals(new int[]{3, 100},
                ListPositioner.getPosition(bottom, W, H, 50, 20));
    }

    @Test
    void snapOverridesCornerMath() {
        StatListConfig cfg = list();
        cfg.anchorCorner = Corner.BOTTOM_RIGHT;
        cfg.snapX = SnapX.CENTER_ON_CENTER;
        cfg.snapY = SnapY.CENTER_ON_CENTER;
        assertArrayEquals(new int[]{135, 110},
                ListPositioner.getPosition(cfg, W, H, 50, 20));
    }

    @Test
    void positionIsClampedInsideInset() {
        StatListConfig cfg = list();
        cfg.anchorFracX = 1.0;
        cfg.anchorFracY = 1.0;
        // x=320 clamps to 320-50-3=267; y=240 clamps to 240-20-3=217.
        assertArrayEquals(new int[]{267, 217},
                ListPositioner.getPosition(cfg, W, H, 50, 20));
    }

    @Test
    void boxWiderThanScreenClampsToZero() {
        StatListConfig cfg = list();
        cfg.anchorFracX = 0.5;
        // minX=0, maxX=max(0, 320-400-3)=0 for x; y clamps to the 3px inset.
        assertArrayEquals(new int[]{0, 3},
                ListPositioner.getPosition(cfg, W, H, 400, 20));
    }

    @Test
    void nanFractionIsTreatedAsZero() {
        StatListConfig cfg = list();
        cfg.anchorFracX = Double.NaN;
        cfg.anchorFracY = Double.NEGATIVE_INFINITY;
        // Fraction sanitized to 0, then floored at the 3px screen inset.
        assertArrayEquals(new int[]{3, 3},
                ListPositioner.getPosition(cfg, W, H, 50, 20));
    }

    @Test
    void negativeFractionClampsToZero() {
        StatListConfig cfg = list();
        cfg.anchorFracX = -0.5;
        cfg.anchorFracY = -0.5;
        assertArrayEquals(new int[]{3, 3},
                ListPositioner.getPosition(cfg, W, H, 50, 20));
    }

    @Test
    void zeroScreenStillReturnsFinitePosition() {
        assertArrayEquals(new int[]{0, 0},
                ListPositioner.getPosition(list(), 0, 0, 50, 20));
    }
}
