package bottled.minetuner.gui.panel;

import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.gui.render.PanelChrome;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import static bottled.minetuner.gui.render.PanelChrome.*;


public final class ColorScalePanel {

    /** Row count: use-custom-color, cycle-color, scale (+/- share one row), back. */
    private static final int CS_COUNT = 4;
    /** Small curated swatch palette to cycle through for the custom list color. */
    public static final int[] COLOR_SWATCHES = {
            0xFFFFFFFF, // white.
            0xFF55FF55, // green.
            0xFFFFFF55, // yellow.
            0xFFFF5555, // red.
            0xFF55FFFF, // cyan.
            0xFFFF55FF, // magenta.
            0xFF5555FF, // blue.
            0xFFFFAA00, // orange.
    };

    private ColorScalePanel() {
    }

    public static int panelHeight() {
        return PANEL_PAD * 2 + ROW_H * CS_COUNT;
    }

    public static void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                              int mx, int my, int menuX, int menuY, int screenW, int screenH,
                              MineTunerConfig.StatListConfig lc) {
        int panelH = panelHeight();
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);

        PanelChrome.drawBackground(g, px, py, PANEL_W, panelH);

        // Row 0.
        int ry0 = py + PANEL_PAD;
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry0, PANEL_W, ROW_H);
        String useCustomLabel = I18n.get("gui.minetuner.color_scale.use_custom")
                + (lc.useCustomColor ? " §a" + I18n.get("gui.minetuner.menu.on")
                : " §c" + I18n.get("gui.minetuner.menu.off"));
        g.text(font, "§f" + useCustomLabel, px + PANEL_PAD, ry0 + 2, 0xFFFFFFFF, false);

        // Row 1.
        int ry1 = py + PANEL_PAD + ROW_H;
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry1, PANEL_W, ROW_H);
        g.text(font, "§f" + I18n.get("gui.minetuner.color_scale.cycle_color"),
                px + PANEL_PAD, ry1 + 2, 0xFFFFFFFF, false);
        // Small color swatch preview on the right.
        g.fill(px + PANEL_W - 20, ry1 + 2, px + PANEL_W - 8, ry1 + ROW_H - 2, lc.overrideColor);
        g.outline(px + PANEL_W - 20, ry1 + 2, 12, ROW_H - 4, 0xFF000000);

        // Row 2.
        int ry2 = py + PANEL_PAD + ROW_H * 2;
        boolean hoverDown = PanelChrome.isHoveringRow(mx, my, px, ry2, PANEL_W / 2, ROW_H);
        boolean hoverUp = PanelChrome.isHoveringRow(mx, my, px + PANEL_W / 2, ry2, PANEL_W / 2, ROW_H);
        if (hoverDown) g.fill(px + 1, ry2, px + PANEL_W / 2, ry2 + ROW_H, 0x44FFFFFF);
        if (hoverUp) g.fill(px + PANEL_W / 2, ry2, px + PANEL_W - 1, ry2 + ROW_H, 0x44FFFFFF);
        g.text(font, "§f- " + I18n.get("gui.minetuner.color_scale.scale", String.format("%.2f", lc.textScale)),
                px + PANEL_PAD, ry2 + 2, 0xFFFFFFFF, false);
        g.text(font, "§f+", px + PANEL_W - 14, ry2 + 2, 0xFFFFFFFF, false);

        // Row 3.
        int ry3 = py + PANEL_PAD + ROW_H * 3;
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry3, PANEL_W, ROW_H);
        g.text(font, "§7" + I18n.get("gui.minetuner.stat_settings.back"),
                px + PANEL_PAD, ry3 + 2, 0xFFFFFFFF, false);
    }

    public static boolean isInside(int mx, int my, int menuX, int menuY, int screenW, int screenH) {
        int panelH = panelHeight();
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW), py = PanelChrome.clampY(menuY, panelH, screenH);
        return PanelChrome.isInsidePanel(mx, my, px, py, PANEL_W, panelH);
    }

    /** Applies a click at (mx, my) to the given list's color/scale settings. */
    public static boolean handleClick(int mx, int my, int menuX, int menuY, int screenW, int screenH,
                                      MineTunerConfig.StatListConfig lc, MineTunerConfig root) {
        int panelH = panelHeight();
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);

        int ry0 = py + PANEL_PAD;
        int ry1 = py + PANEL_PAD + ROW_H;
        int ry2 = py + PANEL_PAD + ROW_H * 2;
        int ry3 = py + PANEL_PAD + ROW_H * 3;

        if (PanelChrome.isHoveringRow(mx, my, px, ry0, PANEL_W, ROW_H)) {
            lc.useCustomColor = !lc.useCustomColor;
            root.save();
        } else if (PanelChrome.isHoveringRow(mx, my, px, ry1, PANEL_W, ROW_H)) {
            int idx = java.util.stream.IntStream.range(0, COLOR_SWATCHES.length)
                    .filter(i -> COLOR_SWATCHES[i] == lc.overrideColor)
                    .findFirst().orElse(-1);
            lc.overrideColor = COLOR_SWATCHES[(idx + 1) % COLOR_SWATCHES.length];
            root.save();
        } else if (PanelChrome.isHoveringRow(mx, my, px, ry2, PANEL_W / 2, ROW_H)) {
            lc.textScale = Math.max(root.textScaleMin, Math.round((lc.textScale - 0.1f) * 100f) / 100f);
            root.save();
        } else if (PanelChrome.isHoveringRow(mx, my, px + PANEL_W / 2, ry2, PANEL_W / 2, ROW_H)) {
            lc.textScale = Math.min(root.textScaleMax, Math.round((lc.textScale + 0.1f) * 100f) / 100f);
            root.save();
        } else
            return PanelChrome.isHoveringRow(mx, my, px, ry3, PANEL_W, ROW_H); // Color/Scale nests inside Appearance, so Back returns there.
        return false;
    }
}
