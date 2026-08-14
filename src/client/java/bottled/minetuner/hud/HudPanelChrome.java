package bottled.minetuner.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Shared geometry and chrome for both the in-game HUD and the layout editor.
 * Keeping these values here prevents preview hit boxes from drifting away from
 * the overlay whenever the presentation is adjusted.
 */
public final class HudPanelChrome {

    /** Classic theme padding; kept as the default for backwards compatibility. */
    public static final int PADDING_X = 6;
    public static final int PADDING_Y = 5;
    public static final int ROW_GAP = 2;

    private HudPanelChrome() {
    }

    public static int contentWidth(int panelWidth) {
        return contentWidth(panelWidth, PADDING_X);
    }

    public static int contentWidth(int panelWidth, int paddingX) {
        return Math.max(0, panelWidth - Math.max(0, paddingX) * 2);
    }

    public static int paddingX(int paddingX) {
        return Math.max(0, paddingX);
    }

    public static int paddingY(int paddingY) {
        return Math.max(0, paddingY);
    }

    /** Draws the classic MineTuner panel with its original accent edge. */
    public static void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, 0xB8141820);
        graphics.fill(x, y, x + width, y + 1, 0x54FFFFFF);
        graphics.fill(x, y, x + 1, y + height, 0x684B9CFF);
        graphics.outline(x, y, width, height, 0x5E9BA9BE);
    }

    /**
     * Draws a theme-controlled flat panel. Unlike the classic panel above, this
     * deliberately adds no extra accent strips, so the theme's fill/outline
     * colors are rendered exactly as supplied, including their alpha channel.
     */
    public static void drawPanel(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                                 int fillColor, int outlineColor) {
        if ((fillColor >>> 24) != 0) {
            graphics.fill(x, y, x + width, y + height, fillColor);
        }
        if ((outlineColor >>> 24) != 0) {
            graphics.outline(x, y, width, height, outlineColor);
        }
    }

}
