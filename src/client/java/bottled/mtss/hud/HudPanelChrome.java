package bottled.mtss.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared geometry and chrome for both the in-game HUD and the layout editor.
 * Keeping these values here prevents preview hit boxes from drifting away from
 * the overlay whenever the presentation is adjusted.
 */
public final class HudPanelChrome {

    public static final int PADDING_X = 6;
    public static final int PADDING_Y = 5;
    public static final int ROW_GAP = 2;

    private HudPanelChrome() {
    }

    public static int contentWidth(int panelWidth) {
        return Math.max(0, panelWidth - PADDING_X * 2);
    }

    /** Draws a compact translucent surface with a restrained accent edge. */
    public static void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xB8141820);
        graphics.fill(x, y, x + width, y + 1, 0x54FFFFFF);
        graphics.fill(x, y, x + 1, y + height, 0x684B9CFF);
        graphics.outline(x, y, width, height, 0x5E9BA9BE);
    }

}
