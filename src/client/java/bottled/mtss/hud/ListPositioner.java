package bottled.mtss.hud;

import bottled.mtss.config.MtssConfig;


public final class ListPositioner {

    private ListPositioner() {}

    public static int[] getPosition(MtssConfig.StatListConfig cfg,
                                    int screenW, int screenH, int boxW, int boxH) {
        int dx = (int) Math.round(cfg.anchorFracX * screenW);
        int dy = (int) Math.round(cfg.anchorFracY * screenH);

        int x, y;
        switch (cfg.anchorCorner) {
            case TOP_RIGHT    -> { x = screenW - boxW - dx; y = dy; }
            case BOTTOM_LEFT  -> { x = dx;                  y = screenH - boxH - dy; }
            case BOTTOM_RIGHT -> { x = screenW - boxW - dx; y = screenH - boxH - dy; }
            default           -> { x = dx;                  y = dy; } // TOP_LEFT
        }
        // Snap overrides beat the corner anchor on the snapped axis
        int cx = screenW / 2;
        int cy = screenH / 2;
        x = switch (cfg.snapX) {
            case LEFT_ON_CENTER   -> cx;
            case CENTER_ON_CENTER -> cx - boxW / 2;
            case RIGHT_ON_CENTER  -> cx - boxW;
            default -> x;
        };
        y = switch (cfg.snapY) {
            case TOP_ON_CENTER    -> cy;
            case CENTER_ON_CENTER -> cy - boxH / 2;
            case BOTTOM_ON_CENTER -> cy - boxH;
            default -> y;
        };
        x = Math.max(0, Math.min(screenW - boxW, x));
        y = Math.max(0, Math.min(screenH - boxH, y));
        return new int[]{x, y};
    }
}
