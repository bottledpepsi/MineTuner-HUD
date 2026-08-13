package bottled.minetuner.gui.panel;

import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.gui.render.PanelChrome;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import static bottled.minetuner.gui.render.PanelChrome.*;


public final class ListContextMenuPanel {

    // 4 grouped rows: the first opens either the stat reorder panel or
    // the template line editor (template mode), depending on lc.useTemplate; the
    // second opens the Appearance sub-panel (rename/background/shadow/color/
    // template-mode toggle); the last two (duplicate, delete) are single actions.
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
                              MineTunerConfig.StatListConfig lc) {
        render(g, font, mx, my, menuX, menuY, screenW, screenH, lc, 1f);
    }

    /** Renders the menu as a vertically unfolding group of rows. */
    public static void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                              int mx, int my, int menuX, int menuY, int screenW, int screenH,
                              MineTunerConfig.StatListConfig lc, float reveal) {
        String[] labels = new String[LM_COUNT];
        labels[LM_STATS] = "§f⚙ " + (lc.useTemplate
                ? I18n.get("gui.minetuner.menu.edit_template")
                : I18n.get("gui.minetuner.menu.reorder"));
        labels[LM_APPEARANCE] = "§f▤ " + I18n.get("gui.minetuner.menu.appearance") + " »";
        labels[LM_DUPLICATE] = "§b⧉ " + I18n.get("gui.minetuner.menu.duplicate");
        labels[LM_DELETE] = "§c✕ " + I18n.get("gui.minetuner.menu.delete");

        int fullHeight = panelHeight();
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, fullHeight, screenH);
        float progress = Math.max(0f, Math.min(1f, reveal));
        int animatedHeight = PANEL_PAD * 2 + Math.round(ROW_H * LM_COUNT * progress);
        PanelChrome.drawBackground(g, px, py, PANEL_W, animatedHeight);

        // All rows share a single vertical scale origin. Their spacing expands
        // continuously, avoiding the one-row bundling that per-row offsets cause.
        var pose = g.pose();
        pose.pushMatrix();
        int rowOrigin = py + PANEL_PAD;
        pose.translate(0, rowOrigin);
        pose.scale(1f, Math.max(0.01f, progress));
        pose.translate(0, -rowOrigin);
        for (int i = 0; i < labels.length; i++) {
            int ry = rowOrigin + i * ROW_H;
            if (progress >= 0.98f) PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry, PANEL_W, ROW_H);
            g.text(font, labels[i], px + PANEL_PAD, ry + 2, 0xFFFFFFFF, false);
        }
        pose.popMatrix();
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
