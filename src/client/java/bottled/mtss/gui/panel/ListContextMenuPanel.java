package bottled.mtss.gui.panel;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.gui.render.PanelChrome;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import static bottled.mtss.gui.render.PanelChrome.*;


public final class ListContextMenuPanel {

    // separator
    // 4 grouped rows.
    // the template line editor (template mode).
    // for rename/background/shadow/color/template-mode.
    // are single actions.
    public static final int LM_STATS = 0; // "Edit Stats" or "Edit Template Lines".
    public static final int LM_APPEARANCE = 1;
    public static final int LM_DUPLICATE = 2;
    public static final int LM_DELETE = 3;
    public static final int LM_COUNT = 4;
    private ListContextMenuPanel() {
    }

    public static int panelHeight() {
        return PANEL_PAD * 2 + ROW_H * LM_COUNT;
    }

    public static void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                              int mx, int my, int menuX, int menuY, int screenW, int screenH,
                              MtssConfig.StatListConfig lc) {
        String[] labels = new String[LM_COUNT];
        labels[LM_STATS] = "§f⚙ " + (lc.useTemplate
                ? I18n.get("gui.mtss.menu.edit_template")
                : I18n.get("gui.mtss.menu.reorder"));
        labels[LM_APPEARANCE] = "§f▤ " + I18n.get("gui.mtss.menu.appearance") + " »";
        labels[LM_DUPLICATE] = "§b⧉ " + I18n.get("gui.mtss.menu.duplicate");
        labels[LM_DELETE] = "§c✕ " + I18n.get("gui.mtss.menu.delete");

        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelHeight(), screenH);
        PanelChrome.drawLabelPanel(g, font, labels, mx, my, px, py, PANEL_W);
    }

    public static boolean isInside(int mx, int my, int menuX, int menuY, int screenW, int screenH) {
        int panelH = panelHeight();
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW), py = PanelChrome.clampY(menuY, panelH, screenH);
        return PanelChrome.isInsidePanel(mx, my, px, py, PANEL_W, panelH);
    }

    /** Returns the clicked row index (LM_* constant), or -1 if the click missed. */
    public static int rowAt(int mx, int my, int menuX, int menuY, int screenW, int screenH) {
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW), py = PanelChrome.clampY(menuY, panelHeight(), screenH);
        if (mx < px || mx > px + PANEL_W) return -1;
        int rel = my - (py + PANEL_PAD);
        if (rel < 0 || rel >= ROW_H * LM_COUNT) return -1;
        return rel / ROW_H;
    }
}
