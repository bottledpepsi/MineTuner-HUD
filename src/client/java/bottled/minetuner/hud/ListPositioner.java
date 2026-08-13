package bottled.minetuner.hud;

import bottled.minetuner.config.MineTunerConfig;

/** Resolves normalized, corner-relative positions into safe GUI-space coordinates. */
public final class ListPositioner {

    private static final int SCREEN_INSET = 3;

    private ListPositioner() {
    }

    public static int[] getPosition(MineTunerConfig.StatListConfig cfg,
                                    int screenW, int screenH, int boxW, int boxH) {
        int dx = (int) Math.round(sanitizeFraction(cfg.anchorFracX) * screenW);
        int dy = (int) Math.round(sanitizeFraction(cfg.anchorFracY) * screenH);
        int x;
        int y;
        switch (cfg.anchorCorner) {
            case TOP_RIGHT -> { x = screenW - boxW - dx; y = dy; }
            case BOTTOM_LEFT -> { x = dx; y = screenH - boxH - dy; }
            case BOTTOM_RIGHT -> { x = screenW - boxW - dx; y = screenH - boxH - dy; }
            default -> { x = dx; y = dy; }
        }

        int cx = screenW / 2;
        int cy = screenH / 2;
        x = switch (cfg.snapX) {
            case LEFT_ON_CENTER -> cx;
            case CENTER_ON_CENTER -> cx - boxW / 2;
            case RIGHT_ON_CENTER -> cx - boxW;
            default -> x;
        };
        y = switch (cfg.snapY) {
            case TOP_ON_CENTER -> cy;
            case CENTER_ON_CENTER -> cy - boxH / 2;
            case BOTTOM_ON_CENTER -> cy - boxH;
            default -> y;
        };

        int minX = boxW + SCREEN_INSET <= screenW ? SCREEN_INSET : 0;
        int minY = boxH + SCREEN_INSET <= screenH ? SCREEN_INSET : 0;
        int maxX = Math.max(minX, screenW - boxW - SCREEN_INSET);
        int maxY = Math.max(minY, screenH - boxH - SCREEN_INSET);
        return new int[]{Math.max(minX, Math.min(maxX, x)), Math.max(minY, Math.min(maxY, y))};
    }

    private static double sanitizeFraction(double fraction) {
        return Double.isFinite(fraction) ? Math.max(0d, Math.min(1d, fraction)) : 0d;
    }
}
