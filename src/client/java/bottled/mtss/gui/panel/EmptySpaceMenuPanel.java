package bottled.mtss.gui.panel;

import bottled.mtss.gui.render.PanelChrome;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import static bottled.mtss.gui.render.PanelChrome.PANEL_PAD;
import static bottled.mtss.gui.render.PanelChrome.PANEL_W;
import static bottled.mtss.gui.render.PanelChrome.ROW_H;

/** Right-click menu shown on empty canvas space — its one row creates a new list at that spot. */
public final class EmptySpaceMenuPanel {

    private EmptySpaceMenuPanel() {}

    public static int panelHeight() {
        return PANEL_PAD * 2 + ROW_H;
    }

    public static void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                              int mx, int my, int menuX, int menuY, int screenW, int screenH) {
        String[] labels = { "§a" + I18n.get("gui.mtss.menu.create") };
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelHeight(), screenH);
        PanelChrome.drawLabelPanel(g, font, labels, mx, my, px, py, PANEL_W);
    }

    public static boolean isInside(int mx, int my, int menuX, int menuY, int screenW, int screenH) {
        int panelH = panelHeight();
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW), py = PanelChrome.clampY(menuY, panelH, screenH);
        return PanelChrome.isInsidePanel(mx, my, px, py, PANEL_W, panelH);
    }

    /** True if the click landed on the single "Create list" row. */
    public static boolean handleClick(int mx, int my, int menuX, int menuY, int screenW, int screenH) {
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelHeight(), screenH);
        return PanelChrome.isHoveringRow(mx, my, px, py + PANEL_PAD, PANEL_W, ROW_H);
    }
}
