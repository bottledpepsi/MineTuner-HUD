package bottled.mtss.gui.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;


public final class PanelChrome {

    private PanelChrome() {}

    public static final int ROW_H     = 13;
    public static final int PANEL_W   = 160;
    public static final int PANEL_PAD = 4;

    /**
     * Wider panel width used by the stat toggle/reorder panel (see
     * {@code ReorderPanel}) — with 40+ stats across categories, the normal
     * {@link #PANEL_W} is too narrow to fit a stat name alongside its
     * enable state, ⚙, and reorder arrows without heavy truncation.
     */
    public static final int WIDE_PANEL_W = 216;

    private static final int PANEL_BG      = 0xEE111111;
    private static final int PANEL_BORDER  = 0xFFFFAA00;
    private static final int ROW_HOVER_COL = 0x44FFFFFF;
    private static final int DIVIDER_COL   = 0x33FFFFFF;

    /** Draws a panel's background fill + border outline at (px, py). */
    public static void drawBackground(GuiGraphicsExtractor g, int px, int py, int panelW, int panelH) {
        g.fill(px, py, px + panelW, py + panelH, PANEL_BG);
        g.outline(px, py, panelW, panelH, PANEL_BORDER);
    }

    /** Draws the hover highlight for one row if (mx, my) falls inside it. */
    public static void drawRowHoverIfNeeded(GuiGraphicsExtractor g, int mx, int my,
                                            int px, int ry, int rowW, int rowH) {
        if (isHoveringRow(mx, my, px, ry, rowW, rowH)) {
            g.fill(px + 1, ry, px + rowW - 1, ry + rowH, ROW_HOVER_COL);
        }
    }

    /**
     * Draws a simple top-to-bottom list of labeled rows inside a fresh
     * bordered panel — the shared shape used by the context menu and the
     * empty-space menu (both are just "a stack of clickable labels").
     */
    public static void drawLabelPanel(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                                      String[] labels, int mx, int my, int panelX, int panelY, int panelW) {
        int panelH = PANEL_PAD * 2 + ROW_H * labels.length;
        drawBackground(g, panelX, panelY, panelW, panelH);
        for (int i = 0; i < labels.length; i++) {
            int ry = panelY + PANEL_PAD + i * ROW_H;
            drawRowHoverIfNeeded(g, mx, my, panelX, ry, panelW, ROW_H);
            g.text(font, labels[i], panelX + PANEL_PAD, ry + 2, 0xFFFFFFFF, false);
        }
    }

    /** Thin 1px horizontal divider, e.g. between category groups in a long panel. */
    public static void drawDivider(GuiGraphicsExtractor g, int px, int ry, int rowW) {
        g.fill(px + 2, ry, px + rowW - 2, ry + 1, DIVIDER_COL);
    }

    public static boolean isHoveringRow(int mx, int my, int px, int ry, int pw, int rh) {
        return mx >= px && mx <= px + pw && my >= ry && my < ry + rh;
    }

    public static boolean isHoveringBox(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx - 1 && mx <= bx + bw + 1 && my >= by - 1 && my <= by + bh + 1;
    }

    public static boolean isInsidePanel(int mx, int my, int px, int py, int panelW, int panelH) {
        return mx >= px && mx <= px + panelW && my >= py && my <= py + panelH;
    }

    /** Clamps a panel's left edge so it stays fully on-screen given screen width. */
    public static int clampX(int x, int w, int screenWidth) {
        return Math.max(0, Math.min(screenWidth - w - 4, x));
    }

    /** Clamps a panel's top edge so it stays fully on-screen given screen height. */
    public static int clampY(int y, int h, int screenHeight) {
        return Math.max(0, Math.min(screenHeight - h - 4, y));
    }
}
