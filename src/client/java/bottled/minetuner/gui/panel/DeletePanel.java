package bottled.minetuner.gui.panel;

import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.gui.render.PanelChrome;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import static bottled.minetuner.gui.render.PanelChrome.*;

public final class DeletePanel {
    public static final int LM_DELETEBUTTON = 0;
    public static final int LM_CANCELBUTTON = 1;
    public static final int LM_COUNT = 2;

    private static final int TITLE_ROWS = 1;

    private DeletePanel() {
    }

    public static int panelHeight() {
        // Title/confirmation text + the two action rows.
        return PANEL_PAD * 2 + ROW_H * (TITLE_ROWS + LM_COUNT);
    }

    public static void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                              int mx, int my, int menuX, int menuY, int screenW, int screenH,
                              MineTunerConfig.StatListConfig lc) {
        int fullHeight = panelHeight();
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, fullHeight, screenH);

        PanelChrome.drawBackground(g, px, py, PANEL_W, fullHeight);

        String name = lc == null ? "" : lc.displayName();
        String title = "§e" + I18n.get("gui.minetuner.delete.title");

        // Keep the confirmation text inside the narrow standard panel. The
        // second line is truncated rather than letting a long user-created
        // list name escape the panel like a badly behaved UI gremlin.
        g.text(font, title, px + PANEL_PAD, py + PANEL_PAD, 0xFFFFFFFF, false);

        int rowTop = py + PANEL_PAD + ROW_H * TITLE_ROWS;

        renderRow(g, font, mx, my, px, rowTop,
                "§c" + I18n.get("gui.minetuner.delete.delete"));
        renderRow(g, font, mx, my, px, rowTop + ROW_H,
                "§7" + I18n.get("gui.minetuner.delete.cancel"));
    }

    private static void renderRow(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                                  int mx, int my, int px, int ry, String label) {
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry, PANEL_W, ROW_H);
        g.text(font, label, px + PANEL_PAD, ry + 2, 0xFFFFFFFF, false);
    }

    private static String truncate(net.minecraft.client.gui.Font font, String s, int maxWidth) {
        if (font.width(s) <= maxWidth) return s;
        String ellipsis = "...";
        int lo = 0;
        int hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (font.width(s.substring(0, mid) + ellipsis) <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, lo) + ellipsis;
    }

    public static boolean isInside(int mx, int my, int menuX, int menuY, int screenW, int screenH) {
        int panelH = panelHeight();
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);
        return PanelChrome.isInsidePanel(mx, my, px, py, PANEL_W, panelH);
    }

    /** Returns the clicked action row, or -1 if the click missed the action rows. */
    public static int rowAt(int mx, int my, int menuX, int menuY, int screenW, int screenH) {
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelHeight(), screenH);

        if (mx < px || mx > px + PANEL_W) return -1;

        int rel = my - (py + PANEL_PAD + ROW_H * TITLE_ROWS);
        if (rel < 0 || rel >= ROW_H * LM_COUNT) return -1;
        return rel / ROW_H;
    }
}
