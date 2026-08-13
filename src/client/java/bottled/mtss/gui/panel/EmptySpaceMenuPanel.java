package bottled.mtss.gui.panel;

import bottled.mtss.gui.render.PanelChrome;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import static bottled.mtss.gui.render.PanelChrome.*;

/** Right-click menu shown on empty canvas space. */
public final class EmptySpaceMenuPanel {

    /** Row index returned by {@link #rowAt} for "no row hit". */
    public static final int ROW_NONE = -1;
    /** Row index for "create a new list here". */
    public static final int ROW_CREATE_LIST = 0;
    private static final int ROW_COUNT = 1;

    private EmptySpaceMenuPanel() {
    }

    public static int panelHeight() {
        return PANEL_PAD * 2 + ROW_H * ROW_COUNT;
    }

    public static void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                              int mx, int my, int menuX, int menuY, int screenW, int screenH) {
        String[] labels = {
                "§a" + I18n.get("gui.mtss.menu.create"),
        };
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelHeight(), screenH);
        PanelChrome.drawLabelPanel(g, font, labels, mx, my, px, py, PANEL_W);
    }

    public static boolean isInside(int mx, int my, int menuX, int menuY, int screenW, int screenH) {
        int panelH = panelHeight();
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW), py = PanelChrome.clampY(menuY, panelH, screenH);
        return PanelChrome.isInsidePanel(mx, my, px, py, PANEL_W, panelH);
    }

    /** Returns which row was clicked ({@link #ROW_CREATE_LIST}/{@link #ROW_OPEN_CONFIG}),
     *  or {@link #ROW_NONE} if the click missed every row. */
    public static int rowAt(int mx, int my, int menuX, int menuY, int screenW, int screenH) {
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelHeight(), screenH);
        for (int row = 0; row < ROW_COUNT; row++) {
            int ry = py + PANEL_PAD + row * ROW_H;
            if (PanelChrome.isHoveringRow(mx, my, px, ry, PANEL_W, ROW_H)) return row;
        }
        return ROW_NONE;
    }
}

