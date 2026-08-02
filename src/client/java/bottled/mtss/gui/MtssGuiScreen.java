package bottled.mtss.gui;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.hud.MtssRenderer;
import bottled.mtss.hud.TemplateEngine;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * MineTuner Statistics Server editor screen.
 *
 * <ul>
 *   <li>Left-click + drag — move any list</li>
 *   <li>Right-click on a list — context menu (edit stats, appearance, duplicate, delete)</li>
 *   <li>Right-click on empty space — create new list</li>
 *   <li>Escape — close and save</li>
 * </ul>
 *
 * Opened via {@code /mtss gui} or the configurable keybind (default: H).
 */
public class MtssGuiScreen extends Screen {

    // ── Drag state ────────────────────────────────────────────────────────────
    private boolean dragging       = false;
    private int     draggingListId = -1;
    private int     dragOffsetX, dragOffsetY;
    private int     dragLiveX, dragLiveY;
    private int     dragBoxW, dragBoxH;
    private MtssConfig.SnapX dragSnapX = MtssConfig.SnapX.NONE;
    private MtssConfig.SnapY dragSnapY = MtssConfig.SnapY.NONE;

    // ── Menu / panel state ────────────────────────────────────────────────────
    private enum MenuKind { NONE, LIST_CONTEXT, EMPTY_SPACE, RENAME, TEMPLATE_EDIT }
    private MenuKind menuKind   = MenuKind.NONE;
    private int      menuListId = -1;
    private int      menuX, menuY;
    private boolean  reorderOpen       = false;
    /** Which stat's settings panel is open (null = reorder panel showing). */
    private MtssConfig.Stat statSettingsStat = null;
    private StringBuilder renameBuffer = new StringBuilder();

    // ── Template line editor state ────────────────────────────────────────────
    /** Whether the template line list (one row per templateLines entry + add/remove/back) is open. */
    private boolean templateListOpen = false;
    /**
     * Index into templateLines being text-edited, or -1 when the line list
     * itself is showing. Mirrors the RENAME flow's renameBuffer pattern.
     */
    private int templateEditIndex = -1;
    private StringBuilder templateEditBuffer = new StringBuilder();

    // ── Appearance sub-panel state ────────────────────────────────────────────
    /** True when the Appearance sub-panel (rename, background, shadow, color/scale, template mode) is open. */
    private boolean appearanceOpen = false;

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int ROW_H     = 13;
    private static final int PANEL_W   = 160;
    private static final int PANEL_PAD = 4;

    private static final int SNAP_THRESHOLD = 6;
    private static final int SNAP_LINE_COL  = 0xBBFFFFFF;
    private static final int SNAP_HIT_COL   = 0xFFFFAA00;
    private static final int SNAP_TICK      = 6;

    // ── Context menu item indices ─────────────────────────────────────────────
    // 4 grouped rows: Stats opens the reorder/toggle panel (classic mode) or
    // the template line editor (template mode); Appearance opens a sub-panel
    // for rename/background/shadow/color/template-mode; Duplicate and Delete
    // are single actions.
    private static final int LM_STATS      = 0; // "Edit Stats" or "Edit Template Lines"
    private static final int LM_APPEARANCE = 1;
    private static final int LM_DUPLICATE  = 2;
    private static final int LM_DELETE     = 3;
    private static final int LM_COUNT      = 4;

    // ── Appearance sub-panel row indices ──────────────────────────────────────
    private static final int AP_RENAME        = 0;
    private static final int AP_BG            = 1;
    private static final int AP_SHADOW        = 2;
    private static final int AP_COLOR_SCALE   = 3;
    private static final int AP_TEMPLATE_MODE = 4;
    private static final int AP_BACK          = 5;
    private static final int AP_COUNT         = 6;

    // ── Template line list sub-panel row indices ──────────────────────────────
    // Rendered as: header, one row per templateLines entry, "+ Add line", "Back".
    // Row count is dynamic — see templateListRowCount()/templateListPanelHeight().

    // ── Color/scale sub-panel row indices ─────────────────────────────────────
    private static final int CS_USE_CUSTOM = 0;
    private static final int CS_CYCLE      = 1;
    private static final int CS_SCALE_DOWN = 2;
    private static final int CS_SCALE_UP   = 2; // same row as SCALE_DOWN, split by x position
    private static final int CS_BACK       = 3;
    private static final int CS_COUNT      = 4;

    /** Small curated swatch palette to cycle through for the custom list color. */
    private static final int[] COLOR_SWATCHES = {
        0xFFFFFFFF, // white
        0xFF55FF55, // green
        0xFFFFFF55, // yellow
        0xFFFF5555, // red
        0xFF55FFFF, // cyan
        0xFFFF55FF, // magenta
        0xFF5555FF, // blue
        0xFFFFAA00, // orange
    };

    private boolean colorScaleOpen = false;

    /** Whether the per-stat custom-threshold sub-panel is open (opened from the stat settings panel). */
    private boolean thresholdPanelOpen = false;

    // ── Threshold sub-panel row indices ───────────────────────────────────────
    private static final int TH_USE_CUSTOM = 0;
    private static final int TH_GOOD       = 1;
    private static final int TH_WARN       = 2;
    private static final int TH_BACK       = 3;
    private static final int TH_COUNT      = 4;

    public MtssGuiScreen() {
        super(Component.translatable("gui.mtss.title"));
    }

    @Override public boolean isPauseScreen() { return false; }

    /** Force-save on close as a safety net, even though every mutation already saves individually. */
    @Override
    public void onClose() {
        MtssConfig.getInstance().save();
        super.onClose();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Rendering
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0x80000000);

        MtssRenderer.tickCache();

        MtssConfig root = MtssConfig.getInstance();
        var font = this.font;

        if (root.lists.isEmpty()) {
            g.centeredText(font, "§7" + I18n.get("gui.mtss.no_lists"),
                    width / 2, height / 2 - 6, 0xFFAAAAAA);
        }

        if (dragging) drawSnapLines(g);

        for (MtssConfig.StatListConfig lc : root.lists) {
            drawList(g, font, lc, mx, my, dragging && lc.id == draggingListId);
        }

        g.centeredText(font, "§7" + I18n.get("gui.mtss.hint"),
                width / 2, height - 14, 0xFFAAAAAA);

        if      (menuKind == MenuKind.LIST_CONTEXT)   renderListContextMenu(g, font, mx, my);
        else if (menuKind == MenuKind.EMPTY_SPACE)    renderEmptySpaceMenu(g, font, mx, my);
        else if (menuKind == MenuKind.RENAME)          renderRenameBox(g, font);
        else if (menuKind == MenuKind.TEMPLATE_EDIT)   renderTemplateEditBox(g, font);
        else if (colorScaleOpen)                       renderColorScalePanel(g, font, mx, my);
        else if (appearanceOpen)                       renderAppearancePanel(g, font, mx, my);
        else if (reorderOpen && statSettingsStat != null && thresholdPanelOpen) renderThresholdPanel(g, font, mx, my);
        else if (reorderOpen && statSettingsStat != null) renderStatSettingsPanel(g, font, mx, my);
        else if (reorderOpen)                          renderReorderPanel(g, font, mx, my);
        else if (templateListOpen)                     renderTemplateListPanel(g, font, mx, my);

        super.extractRenderState(g, mx, my, partial);
    }

    // ── List box ──────────────────────────────────────────────────────────────

    private void drawList(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                          MtssConfig.StatListConfig lc, int mx, int my,
                          boolean isBeingDragged) {
        MtssRenderer.LineCache cache = MtssRenderer.getCachedLines(lc);
        boolean empty = cache.rowKinds().isEmpty();

        // For the empty placeholder, use the old single-line sizing (no
        // LineCache row to measure). Otherwise use LineCache.boxW/boxH — the
        // same math MtssRenderer.render() uses, so the preview and the live
        // overlay always agree on size.
        int lineH = font.lineHeight + 1;
        int boxW, boxH;
        if (empty) {
            String placeholder = I18n.get("gui.mtss.no_stats");
            boxW = font.width(placeholder) + 4;
            boxH = lineH + 3;
        } else {
            boxW = cache.boxW(font);
            boxH = cache.boxH(font);
        }

        int wx, wy;
        if (isBeingDragged) {
            wx = Math.max(0, Math.min(width  - boxW, dragLiveX));
            wy = Math.max(0, Math.min(height - boxH, dragLiveY));
        } else {
            int[] pos = MtssRenderer.getPosition(lc, width, height, boxW, boxH);
            wx = pos[0]; wy = pos[1];
        }

        if (lc.showBackground || empty) {
            g.fill(wx - 1, wy - 1, wx + boxW + 1, wy + boxH + 1,
                    empty ? 0xAA222222 : 0xCC000000);
        }
        if (isHoveringBox(mx, my, wx, wy, boxW, boxH) || isBeingDragged) {
            g.outline(wx - 1, wy - 1, boxW + 2, boxH + 2, 0xFFFFAA00);
        }

        boolean shadow = lc.textShadow;
        if (empty) {
            g.text(font, "§7" + I18n.get("gui.mtss.no_stats"), wx + 2, wy + 2, 0xFFAAAAAA, shadow);
        } else {
            MtssRenderer.drawRows(g, font, cache, wx + 2, wy + 2, shadow);
        }
    }

    // ── Snap lines ────────────────────────────────────────────────────────────

    private void drawSnapLines(GuiGraphicsExtractor g) {
        int cx = width  / 2;
        int cy = height / 2;
        if (dragSnapX != MtssConfig.SnapX.NONE) {
            g.fill(cx, 0, cx + 1, height, SNAP_LINE_COL);
            int hitY = dragLiveY + dragBoxH / 2;
            g.fill(cx - SNAP_TICK, hitY, cx + SNAP_TICK + 1, hitY + 1, SNAP_HIT_COL);
        }
        if (dragSnapY != MtssConfig.SnapY.NONE) {
            g.fill(0, cy, width, cy + 1, SNAP_LINE_COL);
            int hitX = dragLiveX + dragBoxW / 2;
            g.fill(hitX, cy - SNAP_TICK, hitX + 1, cy + SNAP_TICK + 1, SNAP_HIT_COL);
        }
    }

    // ── Context menu ──────────────────────────────────────────────────────────

    private void renderListContextMenu(GuiGraphicsExtractor g,
                                       net.minecraft.client.gui.Font font,
                                       int mx, int my) {
        MtssConfig.StatListConfig lc = getListById(menuListId);
        if (lc == null) { menuKind = MenuKind.NONE; return; }

        String[] labels = new String[LM_COUNT];
        labels[LM_STATS]      = "§f⚙ " + (lc.useTemplate
                ? I18n.get("gui.mtss.menu.edit_template")
                : I18n.get("gui.mtss.menu.reorder"));
        labels[LM_APPEARANCE] = "§f▤ " + I18n.get("gui.mtss.menu.appearance") + " »";
        labels[LM_DUPLICATE]  = "§b⧉ " + I18n.get("gui.mtss.menu.duplicate");
        labels[LM_DELETE]     = "§c✕ " + I18n.get("gui.mtss.menu.delete");

        drawPanel(g, font, labels, mx, my, PANEL_W, LM_COUNT);
    }

    // ── Appearance sub-panel ──────────────────────────────────────────────────
    // Bundles the less-frequently-touched cosmetic settings (rename,
    // background, shadow, color/scale, template mode) behind one menu entry
    // instead of five separate top-level rows.

    private void renderAppearancePanel(GuiGraphicsExtractor g,
                                       net.minecraft.client.gui.Font font,
                                       int mx, int my) {
        MtssConfig.StatListConfig lc = getListById(menuListId);
        if (lc == null) { appearanceOpen = false; return; }

        String onOff_bg  = lc.showBackground ? " §a" + I18n.get("gui.mtss.menu.on")
                                              : " §c" + I18n.get("gui.mtss.menu.off");
        String onOff_sh  = lc.textShadow     ? " §a" + I18n.get("gui.mtss.menu.on")
                                              : " §c" + I18n.get("gui.mtss.menu.off");
        String onOff_tpl = lc.useTemplate    ? " §a" + I18n.get("gui.mtss.menu.on")
                                              : " §c" + I18n.get("gui.mtss.menu.off");

        String[] labels = new String[AP_COUNT];
        labels[AP_RENAME]        = "§e" + I18n.get("gui.mtss.menu.rename");
        labels[AP_BG]            = "§f" + I18n.get("gui.mtss.menu.background") + onOff_bg;
        labels[AP_SHADOW]        = "§f" + I18n.get("gui.mtss.menu.shadow")     + onOff_sh;
        labels[AP_COLOR_SCALE]   = "§f" + I18n.get("gui.mtss.menu.color_scale") + " »";
        labels[AP_TEMPLATE_MODE] = "§f" + I18n.get("gui.mtss.menu.template_mode") + onOff_tpl;
        labels[AP_BACK]          = "§7" + I18n.get("gui.mtss.stat_settings.back");

        drawPanel(g, font, labels, mx, my, PANEL_W, AP_COUNT);
    }

    private boolean isInsideAppearancePanel(int mx, int my) {
        int panelH = PANEL_PAD * 2 + ROW_H * AP_COUNT;
        int px = clampX(menuX, PANEL_W), py = clampY(menuY, panelH);
        return mx >= px && mx <= px + PANEL_W && my >= py && my <= py + panelH;
    }

    private void handleAppearancePanelClick(int mx, int my, MtssConfig.StatListConfig lc) {
        int panelH = PANEL_PAD * 2 + ROW_H * AP_COUNT;
        int px = clampX(menuX, PANEL_W), py = clampY(menuY, panelH);
        if (mx < px || mx > px + PANEL_W) return;
        int rel = my - (py + PANEL_PAD);
        if (rel < 0 || rel >= ROW_H * AP_COUNT) return;
        int idx = rel / ROW_H;

        MtssConfig root = MtssConfig.getInstance();

        switch (idx) {
            case AP_RENAME        -> { appearanceOpen = false; menuKind = MenuKind.RENAME; renameBuffer = new StringBuilder(lc.displayName()); }
            case AP_BG            -> { lc.showBackground = !lc.showBackground; root.save(); }
            case AP_SHADOW        -> { lc.textShadow     = !lc.textShadow;     root.save(); }
            case AP_COLOR_SCALE   -> { appearanceOpen = false; colorScaleOpen = true; }
            case AP_TEMPLATE_MODE -> { lc.useTemplate = !lc.useTemplate; root.save(); }
            case AP_BACK          -> { appearanceOpen = false; menuKind = MenuKind.LIST_CONTEXT; }
        }
    }

    // ── Color / scale sub-panel ───────────────────────────────────────────────

    private void renderColorScalePanel(GuiGraphicsExtractor g,
                                       net.minecraft.client.gui.Font font,
                                       int mx, int my) {
        MtssConfig.StatListConfig lc = getListById(menuListId);
        if (lc == null) { colorScaleOpen = false; return; }

        int panelH = PANEL_PAD * 2 + ROW_H * CS_COUNT;
        int px = clampX(menuX, PANEL_W);
        int py = clampY(menuY, panelH);

        g.fill(px, py, px + PANEL_W, py + panelH, 0xEE111111);
        g.outline(px, py, PANEL_W, panelH, 0xFFFFAA00);

        // Row 0: use custom color toggle
        int ry0 = py + PANEL_PAD;
        if (isHoveringRow(mx, my, px, ry0, PANEL_W, ROW_H))
            g.fill(px + 1, ry0, px + PANEL_W - 1, ry0 + ROW_H, 0x44FFFFFF);
        String useCustomLabel = I18n.get("gui.mtss.color_scale.use_custom")
                + (lc.useCustomColor ? " §a" + I18n.get("gui.mtss.menu.on")
                                     : " §c" + I18n.get("gui.mtss.menu.off"));
        g.text(font, "§f" + useCustomLabel, px + PANEL_PAD, ry0 + 2, 0xFFFFFFFF, false);

        // Row 1: cycle swatch (only meaningful when custom color is on, but always clickable)
        int ry1 = py + PANEL_PAD + ROW_H;
        if (isHoveringRow(mx, my, px, ry1, PANEL_W, ROW_H))
            g.fill(px + 1, ry1, px + PANEL_W - 1, ry1 + ROW_H, 0x44FFFFFF);
        g.text(font, "§f" + I18n.get("gui.mtss.color_scale.cycle_color"),
                px + PANEL_PAD, ry1 + 2, 0xFFFFFFFF, false);
        // Small color swatch preview on the right
        g.fill(px + PANEL_W - 20, ry1 + 2, px + PANEL_W - 8, ry1 + ROW_H - 2, lc.overrideColor);
        g.outline(px + PANEL_W - 20, ry1 + 2, 12, ROW_H - 4, 0xFF000000);

        // Row 2: scale down / up
        int ry2 = py + PANEL_PAD + ROW_H * 2;
        boolean hoverDown = isHoveringRow(mx, my, px, ry2, PANEL_W / 2, ROW_H);
        boolean hoverUp   = isHoveringRow(mx, my, px + PANEL_W / 2, ry2, PANEL_W / 2, ROW_H);
        if (hoverDown) g.fill(px + 1, ry2, px + PANEL_W / 2, ry2 + ROW_H, 0x44FFFFFF);
        if (hoverUp)   g.fill(px + PANEL_W / 2, ry2, px + PANEL_W - 1, ry2 + ROW_H, 0x44FFFFFF);
        g.text(font, "§f- " + I18n.get("gui.mtss.color_scale.scale", String.format("%.2f", lc.textScale)),
                px + PANEL_PAD, ry2 + 2, 0xFFFFFFFF, false);
        g.text(font, "§f+", px + PANEL_W - 14, ry2 + 2, 0xFFFFFFFF, false);

        // Row 3: back
        int ry3 = py + PANEL_PAD + ROW_H * 3;
        if (isHoveringRow(mx, my, px, ry3, PANEL_W, ROW_H))
            g.fill(px + 1, ry3, px + PANEL_W - 1, ry3 + ROW_H, 0x44FFFFFF);
        g.text(font, "§7" + I18n.get("gui.mtss.stat_settings.back"),
                px + PANEL_PAD, ry3 + 2, 0xFFFFFFFF, false);
    }

    private boolean isInsideColorScalePanel(int mx, int my) {
        int panelH = PANEL_PAD * 2 + ROW_H * CS_COUNT;
        int px = clampX(menuX, PANEL_W), py = clampY(menuY, panelH);
        return mx >= px && mx <= px + PANEL_W && my >= py && my <= py + panelH;
    }

    private void handleColorScalePanelClick(int mx, int my, MtssConfig.StatListConfig lc) {
        int panelH = PANEL_PAD * 2 + ROW_H * CS_COUNT;
        int px = clampX(menuX, PANEL_W);
        int py = clampY(menuY, panelH);
        MtssConfig root = MtssConfig.getInstance();

        int ry0 = py + PANEL_PAD;
        int ry1 = py + PANEL_PAD + ROW_H;
        int ry2 = py + PANEL_PAD + ROW_H * 2;
        int ry3 = py + PANEL_PAD + ROW_H * 3;

        if (isHoveringRow(mx, my, px, ry0, PANEL_W, ROW_H)) {
            lc.useCustomColor = !lc.useCustomColor;
            root.save();
        } else if (isHoveringRow(mx, my, px, ry1, PANEL_W, ROW_H)) {
            int idx = java.util.stream.IntStream.range(0, COLOR_SWATCHES.length)
                    .filter(i -> COLOR_SWATCHES[i] == lc.overrideColor)
                    .findFirst().orElse(-1);
            lc.overrideColor = COLOR_SWATCHES[(idx + 1) % COLOR_SWATCHES.length];
            root.save();
        } else if (isHoveringRow(mx, my, px, ry2, PANEL_W / 2, ROW_H)) {
            lc.textScale = Math.max(0.5f, Math.round((lc.textScale - 0.1f) * 100f) / 100f);
            root.save();
        } else if (isHoveringRow(mx, my, px + PANEL_W / 2, ry2, PANEL_W / 2, ROW_H)) {
            lc.textScale = Math.min(2.0f, Math.round((lc.textScale + 0.1f) * 100f) / 100f);
            root.save();
        } else if (isHoveringRow(mx, my, px, ry3, PANEL_W, ROW_H)) {
            colorScaleOpen = false;
            appearanceOpen = true; // Color/Scale nests inside Appearance, so Back returns there
        }
    }

    // ── Empty-space menu ──────────────────────────────────────────────────────

    private void renderEmptySpaceMenu(GuiGraphicsExtractor g,
                                      net.minecraft.client.gui.Font font,
                                      int mx, int my) {
        String[] labels = { "§a" + I18n.get("gui.mtss.menu.create") };
        drawPanel(g, font, labels, mx, my, PANEL_W, 1);
    }

    // ── Rename box ────────────────────────────────────────────────────────────

    private void renderRenameBox(GuiGraphicsExtractor g,
                                 net.minecraft.client.gui.Font font) {
        String prompt  = "§e" + I18n.get("gui.mtss.rename.prompt");
        String display = renameBuffer.toString() + "§7|";
        int panelW = PANEL_W + 40;
        int panelH = PANEL_PAD * 2 + ROW_H * 2 + 2;
        int px = clampX(menuX, panelW);
        int py = clampY(menuY, panelH);

        g.fill(px, py, px + panelW, py + panelH, 0xEE111111);
        g.outline(px, py, panelW, panelH, 0xFFFFAA00);
        g.text(font, prompt,  px + PANEL_PAD, py + PANEL_PAD,             0xFFFFFFFF, false);
        g.text(font, display, px + PANEL_PAD, py + PANEL_PAD + ROW_H + 2, 0xFFFFFFFF, false);
    }

    // ── Reorder / Toggle panel ────────────────────────────────────────────────

    private void renderReorderPanel(GuiGraphicsExtractor g,
                                    net.minecraft.client.gui.Font font,
                                    int mx, int my) {
        MtssConfig.StatListConfig lc = getListById(menuListId);
        if (lc == null) { reorderOpen = false; return; }

        List<MtssConfig.Stat> all = allStatsOrdered(lc);
        int panelH = reorderPanelHeight(lc);
        int px = clampX(menuX, PANEL_W);
        int py = clampY(menuY, panelH);

        g.fill(px, py, px + PANEL_W, py + panelH, 0xEE111111);
        g.outline(px, py, PANEL_W, panelH, 0xFFFFAA00);
        g.text(font, "§e" + I18n.get("gui.mtss.reorder.title"),
                px + PANEL_PAD, py + PANEL_PAD, 0xFFFFFFFF, false);

        int rowTop = py + PANEL_PAD + ROW_H;
        for (int i = 0; i < all.size(); i++) {
            MtssConfig.Stat stat = all.get(i);
            boolean enabled = lc.isEnabled(stat);
            int ry = rowTop + i * ROW_H;

            if (isHoveringRow(mx, my, px, ry, PANEL_W, ROW_H))
                g.fill(px + 1, ry, px + PANEL_W - 1, ry + ROW_H, 0x44FFFFFF);

            // Use the lang key for each stat's display name
            String statName = I18n.get("stat.mtss." + stat.name().toLowerCase());
            String label = (enabled ? "§a✔ " : "§c✘ ") + statName;
            g.text(font, label, px + PANEL_PAD + 12, ry + 2, 0xFFFFFFFF, false);

            int orderIdx = lc.statOrder.indexOf(stat.name());
            // ⚙ cog button
            boolean cogHovered = isHoveringRow(mx, my, px + PANEL_W - 28, ry, 10, ROW_H);
            g.text(font, cogHovered ? "§e⚙" : "§7⚙", px + PANEL_W - 28, ry + 2, 0xFFFFFFFF, false);
            if (orderIdx > 0)
                g.text(font, "§7▲", px + PANEL_W - 18, ry + 2, 0xFFFFFFFF, false);
            if (orderIdx < lc.statOrder.size() - 1)
                g.text(font, "§7▼", px + PANEL_W - 10, ry + 2, 0xFFFFFFFF, false);
        }

        int closeY = rowTop + all.size() * ROW_H;
        if (isHoveringRow(mx, my, px, closeY, PANEL_W, ROW_H))
            g.fill(px + 1, closeY, px + PANEL_W - 1, closeY + ROW_H, 0x44FFFFFF);
        g.text(font, "§7" + I18n.get("gui.mtss.reorder.close"),
                px + PANEL_PAD, closeY + 2, 0xFFFFFFFF, false);
    }

    // ── Template line editor ──────────────────────────────────────────────────
    // Minimal by design: a flat list of templateLines entries with
    // add/remove/edit, reusing the RENAME text-entry pattern for editing one
    // line at a time.

    /** Row count for the template line list panel: header + one row per line + "+ Add line" + "Back". */
    private int templateListRowCount(MtssConfig.StatListConfig lc) {
        return 1 + lc.templateLines.size() + 2; // header + lines + add + back
    }

    private int templateListPanelHeight(MtssConfig.StatListConfig lc) {
        return PANEL_PAD * 2 + ROW_H * templateListRowCount(lc);
    }

    private void renderTemplateListPanel(GuiGraphicsExtractor g,
                                         net.minecraft.client.gui.Font font,
                                         int mx, int my) {
        MtssConfig.StatListConfig lc = getListById(menuListId);
        if (lc == null) { templateListOpen = false; return; }

        int panelH = templateListPanelHeight(lc);
        int px = clampX(menuX, PANEL_W);
        int py = clampY(menuY, panelH);

        g.fill(px, py, px + PANEL_W, py + panelH, 0xEE111111);
        g.outline(px, py, PANEL_W, panelH, 0xFFFFAA00);
        g.text(font, "§e" + I18n.get("gui.mtss.template.title"),
                px + PANEL_PAD, py + PANEL_PAD, 0xFFFFFFFF, false);

        int rowTop = py + PANEL_PAD + ROW_H;
        for (int i = 0; i < lc.templateLines.size(); i++) {
            int ry = rowTop + i * ROW_H;
            if (isHoveringRow(mx, my, px, ry, PANEL_W, ROW_H))
                g.fill(px + 1, ry, px + PANEL_W - 1, ry + ROW_H, 0x44FFFFFF);

            String raw = lc.templateLines.get(i);
            String preview = raw.isEmpty() ? "§7" + I18n.get("gui.mtss.template.empty_line")
                                            : "§f" + truncateForRow(font, raw, PANEL_W - PANEL_PAD - 24);
            g.text(font, I18n.get("gui.mtss.template.line_number", i + 1) + " " + preview,
                    px + PANEL_PAD, ry + 2, 0xFFFFFFFF, false);

            // ✕ remove button, right-aligned
            boolean removeHovered = isHoveringRow(mx, my, px + PANEL_W - 12, ry, 10, ROW_H);
            g.text(font, removeHovered ? "§c✕" : "§7✕", px + PANEL_W - 12, ry + 2, 0xFFFFFFFF, false);
        }

        int addY = rowTop + lc.templateLines.size() * ROW_H;
        if (isHoveringRow(mx, my, px, addY, PANEL_W, ROW_H))
            g.fill(px + 1, addY, px + PANEL_W - 1, addY + ROW_H, 0x44FFFFFF);
        g.text(font, "§a" + I18n.get("gui.mtss.template.add_line"),
                px + PANEL_PAD, addY + 2, 0xFFFFFFFF, false);

        int backY = addY + ROW_H;
        if (isHoveringRow(mx, my, px, backY, PANEL_W, ROW_H))
            g.fill(px + 1, backY, px + PANEL_W - 1, backY + ROW_H, 0x44FFFFFF);
        g.text(font, "§7" + I18n.get("gui.mtss.reorder.close"),
                px + PANEL_PAD, backY + 2, 0xFFFFFFFF, false);
    }

    /** Truncates a template line preview to fit the panel row, appending an ellipsis marker when cut. */
    private String truncateForRow(net.minecraft.client.gui.Font font, String s, int maxWidth) {
        if (font.width(s) <= maxWidth) return s;
        String ellipsis = "...";
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (font.width(s.substring(0, mid) + ellipsis) <= maxWidth) lo = mid; else hi = mid - 1;
        }
        return s.substring(0, lo) + ellipsis;
    }

    private boolean isInsideTemplateListPanel(int mx, int my, MtssConfig.StatListConfig lc) {
        int panelH = templateListPanelHeight(lc);
        int px = clampX(menuX, PANEL_W), py = clampY(menuY, panelH);
        return mx >= px && mx <= px + PANEL_W && my >= py && my <= py + panelH;
    }

    private void handleTemplateListPanelClick(int mx, int my, MtssConfig.StatListConfig lc) {
        int panelH = templateListPanelHeight(lc);
        int px = clampX(menuX, PANEL_W);
        int py = clampY(menuY, panelH);
        int rowTop = py + PANEL_PAD + ROW_H;

        int addY  = rowTop + lc.templateLines.size() * ROW_H;
        int backY = addY + ROW_H;

        if (isHoveringRow(mx, my, px, backY, PANEL_W, ROW_H)) { templateListOpen = false; return; }

        if (isHoveringRow(mx, my, px, addY, PANEL_W, ROW_H)) {
            lc.templateLines.add("");
            MtssConfig.getInstance().save();
            TemplateEngine.invalidate(lc.id);
            return;
        }

        for (int i = 0; i < lc.templateLines.size(); i++) {
            int ry = rowTop + i * ROW_H;
            if (my < ry || my >= ry + ROW_H) continue;
            if (mx >= px + PANEL_W - 12) {
                // ✕ remove this line
                lc.templateLines.remove(i);
                MtssConfig.getInstance().save();
                TemplateEngine.invalidate(lc.id);
            } else {
                // Open the text-entry box for this line
                templateEditIndex  = i;
                templateEditBuffer = new StringBuilder(lc.templateLines.get(i));
                menuKind = MenuKind.TEMPLATE_EDIT;
            }
            return;
        }
    }

    /** Text-entry box for one templateLines entry — same pattern as renderRenameBox, just wider since template strings run longer. */
    private void renderTemplateEditBox(GuiGraphicsExtractor g,
                                       net.minecraft.client.gui.Font font) {
        String prompt  = "§e" + I18n.get("gui.mtss.template.edit_prompt", templateEditIndex + 1);
        String display = templateEditBuffer.toString() + "§7|";
        int panelW = Math.max(PANEL_W + 40, Math.min(300, font.width(display) + PANEL_PAD * 2));
        int panelH = PANEL_PAD * 2 + ROW_H * 2 + 2;
        int px = clampX(menuX, panelW);
        int py = clampY(menuY, panelH);

        g.fill(px, py, px + panelW, py + panelH, 0xEE111111);
        g.outline(px, py, panelW, panelH, 0xFFFFAA00);
        g.text(font, prompt,  px + PANEL_PAD, py + PANEL_PAD,             0xFFFFFFFF, false);
        g.text(font, display, px + PANEL_PAD, py + PANEL_PAD + ROW_H + 2, 0xFFFFFFFF, false);
    }

    // ── Per-stat settings panel ───────────────────────────────────────────────

    /** Stats whose formatted value supports a configurable decimal-places setting. */
    private boolean supportsDecimals(MtssConfig.Stat stat) {
        return stat == MtssConfig.Stat.TPS || stat == MtssConfig.Stat.MSPT
            || stat == MtssConfig.Stat.CPU || stat == MtssConfig.Stat.SPEED;
    }

    /** Stats that can be rendered as a rolling graph instead of text. */
    private boolean supportsGraph(MtssConfig.Stat stat) {
        return MtssConfig.GRAPHABLE_STATS.contains(stat);
    }

    /** Stats that have a user-configurable good/warn color threshold (step 4's ThresholdSettings). */
    private boolean supportsThresholds(MtssConfig.Stat stat) {
        return MtssConfig.THRESHOLD_STATS.contains(stat);
    }

    /**
     * True when a higher value is better (green at/above goodMin) — TPS, FPS.
     * False means lower is better. Must match MtssDataHolder's xColorFor() direction.
     */
    private boolean isHigherBetter(MtssConfig.Stat stat) {
        return stat == MtssConfig.Stat.TPS || stat == MtssConfig.Stat.FPS;
    }

    /** Increment step for each threshold stat's scale: 0.5 for TPS, whole units otherwise. */
    private float thresholdStep(MtssConfig.Stat stat) {
        return (stat == MtssConfig.Stat.TPS) ? 0.5f : 1.0f;
    }

    /**
     * Row count for the stat settings panel: header + prefix + optional
     * decimals/graph/thresholds rows + back. Shared by render, click, and
     * hit-test so they can't drift out of sync.
     */
    private int statSettingsPanelRows(MtssConfig.Stat stat) {
        int rows = 3; // header + prefix + back
        if (supportsDecimals(stat))    rows++;
        if (supportsGraph(stat))       rows++;
        if (supportsThresholds(stat))  rows++;
        return rows;
    }

    private void renderStatSettingsPanel(GuiGraphicsExtractor g,
                                         net.minecraft.client.gui.Font font,
                                         int mx, int my) {
        MtssConfig.StatListConfig lc = getListById(menuListId);
        if (lc == null || statSettingsStat == null) { statSettingsStat = null; return; }

        MtssConfig.StatSettings ss = lc.getStatSettings(statSettingsStat);
        String statLabel = I18n.get("stat.mtss." + statSettingsStat.name().toLowerCase());
        boolean decimalsRow = supportsDecimals(statSettingsStat);
        boolean graphRow    = supportsGraph(statSettingsStat);

        // 1 header row + 1 prefix row + (optional decimals row) + (optional graph row) + 1 back row
        int rows = statSettingsPanelRows(statSettingsStat);
        int panelH = PANEL_PAD * 2 + ROW_H * rows;
        int px = clampX(menuX, PANEL_W);
        int py = clampY(menuY, panelH);

        g.fill(px, py, px + PANEL_W, py + panelH, 0xEE111111);
        g.outline(px, py, PANEL_W, panelH, 0xFFFFAA00);

        // Header
        g.text(font, "§e" + I18n.get("gui.mtss.stat_settings.title", statLabel),
                px + PANEL_PAD, py + PANEL_PAD, 0xFFFFFFFF, false);

        // Show Prefix toggle
        int ry1 = py + PANEL_PAD + ROW_H;
        if (isHoveringRow(mx, my, px, ry1, PANEL_W, ROW_H))
            g.fill(px + 1, ry1, px + PANEL_W - 1, ry1 + ROW_H, 0x44FFFFFF);
        String prefixToggle = I18n.get("gui.mtss.stat_settings.show_prefix")
                + (ss.showPrefix ? " §a" + I18n.get("gui.mtss.menu.on")
                                 : " §c" + I18n.get("gui.mtss.menu.off"));
        g.text(font, "§f" + prefixToggle, px + PANEL_PAD, ry1 + 2, 0xFFFFFFFF, false);

        // Decimals stepper (only for numeric stats)
        int nextRow = 2;
        if (decimalsRow) {
            int ryDec = py + PANEL_PAD + ROW_H * nextRow;
            boolean hoverDown = isHoveringRow(mx, my, px, ryDec, PANEL_W / 2, ROW_H);
            boolean hoverUp   = isHoveringRow(mx, my, px + PANEL_W / 2, ryDec, PANEL_W / 2, ROW_H);
            if (hoverDown) g.fill(px + 1, ryDec, px + PANEL_W / 2, ryDec + ROW_H, 0x44FFFFFF);
            if (hoverUp)   g.fill(px + PANEL_W / 2, ryDec, px + PANEL_W - 1, ryDec + ROW_H, 0x44FFFFFF);
            g.text(font, "§f- " + I18n.get("gui.mtss.stat_settings.decimals", ss.decimals),
                    px + PANEL_PAD, ryDec + 2, 0xFFFFFFFF, false);
            g.text(font, "§f+", px + PANEL_W - 14, ryDec + 2, 0xFFFFFFFF, false);
            nextRow++;
        }

        // Render-as-graph toggle (only for graphable stats: TPS, MSPT, FPS, CPU, Ping, Memory, Speed)
        if (graphRow) {
            int ryGraph = py + PANEL_PAD + ROW_H * nextRow;
            if (isHoveringRow(mx, my, px, ryGraph, PANEL_W, ROW_H))
                g.fill(px + 1, ryGraph, px + PANEL_W - 1, ryGraph + ROW_H, 0x44FFFFFF);
            String graphToggle = I18n.get("gui.mtss.stat_settings.render_as_graph")
                    + (ss.renderAsGraph ? " §a" + I18n.get("gui.mtss.menu.on")
                                        : " §c" + I18n.get("gui.mtss.menu.off"));
            g.text(font, "§f" + graphToggle, px + PANEL_PAD, ryGraph + 2, 0xFFFFFFFF, false);
            nextRow++;
        }

        // Custom Thresholds sub-panel opener (only for threshold-eligible stats)
        boolean thresholdsRow = supportsThresholds(statSettingsStat);
        if (thresholdsRow) {
            int ryTh = py + PANEL_PAD + ROW_H * nextRow;
            if (isHoveringRow(mx, my, px, ryTh, PANEL_W, ROW_H))
                g.fill(px + 1, ryTh, px + PANEL_W - 1, ryTh + ROW_H, 0x44FFFFFF);
            g.text(font, "§f" + I18n.get("gui.mtss.stat_settings.custom_thresholds"),
                    px + PANEL_PAD, ryTh + 2, 0xFFFFFFFF, false);
            nextRow++;
        }

        // Back button
        int ryBack = py + PANEL_PAD + ROW_H * nextRow;
        if (isHoveringRow(mx, my, px, ryBack, PANEL_W, ROW_H))
            g.fill(px + 1, ryBack, px + PANEL_W - 1, ryBack + ROW_H, 0x44FFFFFF);
        g.text(font, "§7" + I18n.get("gui.mtss.stat_settings.back"),
                px + PANEL_PAD, ryBack + 2, 0xFFFFFFFF, false);
    }

    private void handleStatSettingsPanelClick(int mx, int my,
                                              MtssConfig.StatListConfig lc) {
        if (statSettingsStat == null) return;
        MtssConfig.StatSettings ss = lc.getStatSettings(statSettingsStat);
        boolean decimalsRow   = supportsDecimals(statSettingsStat);
        boolean graphRow      = supportsGraph(statSettingsStat);
        boolean thresholdsRow = supportsThresholds(statSettingsStat);

        int rows = statSettingsPanelRows(statSettingsStat);
        int panelH = PANEL_PAD * 2 + ROW_H * rows;
        int px = clampX(menuX, PANEL_W);
        int py = clampY(menuY, panelH);

        int ry1 = py + PANEL_PAD + ROW_H;
        int nextRow = 2;

        if (isHoveringRow(mx, my, px, ry1, PANEL_W, ROW_H)) {
            ss.showPrefix = !ss.showPrefix;
            MtssConfig.getInstance().save();
            return;
        }

        if (decimalsRow) {
            int ryDec = py + PANEL_PAD + ROW_H * nextRow;
            if (isHoveringRow(mx, my, px, ryDec, PANEL_W / 2, ROW_H)) {
                ss.decimals = Math.max(0, ss.decimals - 1);
                MtssConfig.getInstance().save();
                return;
            } else if (isHoveringRow(mx, my, px + PANEL_W / 2, ryDec, PANEL_W / 2, ROW_H)) {
                ss.decimals = Math.min(4, ss.decimals + 1);
                MtssConfig.getInstance().save();
                return;
            }
            nextRow++;
        }

        if (graphRow) {
            int ryGraph = py + PANEL_PAD + ROW_H * nextRow;
            if (isHoveringRow(mx, my, px, ryGraph, PANEL_W, ROW_H)) {
                ss.renderAsGraph = !ss.renderAsGraph;
                MtssConfig.getInstance().save();
                return;
            }
            nextRow++;
        }

        if (thresholdsRow) {
            int ryTh = py + PANEL_PAD + ROW_H * nextRow;
            if (isHoveringRow(mx, my, px, ryTh, PANEL_W, ROW_H)) {
                thresholdPanelOpen = true;
                return;
            }
            nextRow++;
        }

        int ryBack = py + PANEL_PAD + ROW_H * nextRow;
        if (isHoveringRow(mx, my, px, ryBack, PANEL_W, ROW_H)) {
            statSettingsStat = null; // back to reorder panel
        }
    }

    // ── Custom-threshold sub-panel ────────────────────────────────────────────
    // Same nesting pattern as colorScaleOpen: a boolean flag opens a sibling
    // sub-panel from the stat settings panel, with its own render/click/hit-test.

    private void renderThresholdPanel(GuiGraphicsExtractor g,
                                      net.minecraft.client.gui.Font font,
                                      int mx, int my) {
        MtssConfig.StatListConfig lc = getListById(menuListId);
        if (lc == null || statSettingsStat == null) { thresholdPanelOpen = false; return; }

        MtssConfig.ThresholdSettings ts = lc.getThreshold(statSettingsStat);
        if (ts == null) { thresholdPanelOpen = false; return; }

        boolean higherBetter = isHigherBetter(statSettingsStat);
        String statLabel = I18n.get("stat.mtss." + statSettingsStat.name().toLowerCase());

        int panelH = PANEL_PAD * 2 + ROW_H * TH_COUNT + font.lineHeight + 2;
        int px = clampX(menuX, PANEL_W);
        int py = clampY(menuY, panelH);

        g.fill(px, py, px + PANEL_W, py + panelH, 0xEE111111);
        g.outline(px, py, PANEL_W, panelH, 0xFFFFAA00);

        // Header + direction subtitle
        g.text(font, "§e" + I18n.get("gui.mtss.threshold.title", statLabel),
                px + PANEL_PAD, py + PANEL_PAD, 0xFFFFFFFF, false);
        String dirLabel = higherBetter ? I18n.get("gui.mtss.threshold.higher_is_better")
                                        : I18n.get("gui.mtss.threshold.lower_is_better");
        g.text(font, "§7" + dirLabel, px + PANEL_PAD, py + PANEL_PAD + font.lineHeight, 0xFF999999, false);

        int rowTop = py + PANEL_PAD + font.lineHeight + 2;

        // Row 0: Use custom thresholds toggle
        int ry0 = rowTop + ROW_H * TH_USE_CUSTOM;
        if (isHoveringRow(mx, my, px, ry0, PANEL_W, ROW_H))
            g.fill(px + 1, ry0, px + PANEL_W - 1, ry0 + ROW_H, 0x44FFFFFF);
        String useCustomLabel = I18n.get("gui.mtss.threshold.use_custom")
                + (ts.enabled ? " §a" + I18n.get("gui.mtss.menu.on")
                              : " §c" + I18n.get("gui.mtss.menu.off"));
        g.text(font, "§f" + useCustomLabel, px + PANEL_PAD, ry0 + 2, 0xFFFFFFFF, false);

        // Row 1: Good threshold stepper
        int ry1 = rowTop + ROW_H * TH_GOOD;
        boolean hoverGoodDown = isHoveringRow(mx, my, px, ry1, PANEL_W / 2, ROW_H);
        boolean hoverGoodUp   = isHoveringRow(mx, my, px + PANEL_W / 2, ry1, PANEL_W / 2, ROW_H);
        if (hoverGoodDown) g.fill(px + 1, ry1, px + PANEL_W / 2, ry1 + ROW_H, 0x44FFFFFF);
        if (hoverGoodUp)   g.fill(px + PANEL_W / 2, ry1, px + PANEL_W - 1, ry1 + ROW_H, 0x44FFFFFF);
        g.text(font, "§f- " + I18n.get("gui.mtss.threshold.good", formatThreshold(ts.goodMin)),
                px + PANEL_PAD, ry1 + 2, 0xFFFFFFFF, false);
        g.text(font, "§f+", px + PANEL_W - 14, ry1 + 2, 0xFFFFFFFF, false);

        // Row 2: Warn threshold stepper
        int ry2 = rowTop + ROW_H * TH_WARN;
        boolean hoverWarnDown = isHoveringRow(mx, my, px, ry2, PANEL_W / 2, ROW_H);
        boolean hoverWarnUp   = isHoveringRow(mx, my, px + PANEL_W / 2, ry2, PANEL_W / 2, ROW_H);
        if (hoverWarnDown) g.fill(px + 1, ry2, px + PANEL_W / 2, ry2 + ROW_H, 0x44FFFFFF);
        if (hoverWarnUp)   g.fill(px + PANEL_W / 2, ry2, px + PANEL_W - 1, ry2 + ROW_H, 0x44FFFFFF);
        g.text(font, "§f- " + I18n.get("gui.mtss.threshold.warn", formatThreshold(ts.warnMin)),
                px + PANEL_PAD, ry2 + 2, 0xFFFFFFFF, false);
        g.text(font, "§f+", px + PANEL_W - 14, ry2 + 2, 0xFFFFFFFF, false);

        // Row 3: Back
        int ry3 = rowTop + ROW_H * TH_BACK;
        if (isHoveringRow(mx, my, px, ry3, PANEL_W, ROW_H))
            g.fill(px + 1, ry3, px + PANEL_W - 1, ry3 + ROW_H, 0x44FFFFFF);
        g.text(font, "§7" + I18n.get("gui.mtss.stat_settings.back"),
                px + PANEL_PAD, ry3 + 2, 0xFFFFFFFF, false);
    }

    /** Formats a threshold value without a trailing ".0" for whole-number steps. */
    private String formatThreshold(float v) {
        if (v == Math.floor(v)) return String.valueOf((int) v);
        return String.format("%.1f", v);
    }

    private boolean isInsideThresholdPanel(int mx, int my) {
        int panelH = PANEL_PAD * 2 + ROW_H * TH_COUNT + font.lineHeight + 2;
        int px = clampX(menuX, PANEL_W), py = clampY(menuY, panelH);
        return mx >= px && mx <= px + PANEL_W && my >= py && my <= py + panelH;
    }

    private void handleThresholdPanelClick(int mx, int my, MtssConfig.StatListConfig lc) {
        if (statSettingsStat == null) return;
        MtssConfig.ThresholdSettings ts = lc.getThreshold(statSettingsStat);
        if (ts == null) { thresholdPanelOpen = false; return; }

        boolean higherBetter = isHigherBetter(statSettingsStat);
        float step = thresholdStep(statSettingsStat);

        int panelH = PANEL_PAD * 2 + ROW_H * TH_COUNT + font.lineHeight + 2;
        int px = clampX(menuX, PANEL_W);
        int py = clampY(menuY, panelH);
        int rowTop = py + PANEL_PAD + font.lineHeight + 2;

        int ry0 = rowTop + ROW_H * TH_USE_CUSTOM;
        int ry1 = rowTop + ROW_H * TH_GOOD;
        int ry2 = rowTop + ROW_H * TH_WARN;
        int ry3 = rowTop + ROW_H * TH_BACK;

        MtssConfig root = MtssConfig.getInstance();

        if (isHoveringRow(mx, my, px, ry0, PANEL_W, ROW_H)) {
            ts.enabled = !ts.enabled;
            root.save();
            return;
        }

        if (isHoveringRow(mx, my, px, ry1, PANEL_W / 2, ROW_H)) {
            ts.goodMin = Math.max(0f, roundStep(ts.goodMin - step));
            clampThresholdOrder(ts, higherBetter, true);
            root.save();
            return;
        } else if (isHoveringRow(mx, my, px + PANEL_W / 2, ry1, PANEL_W / 2, ROW_H)) {
            ts.goodMin = Math.max(0f, roundStep(ts.goodMin + step));
            clampThresholdOrder(ts, higherBetter, true);
            root.save();
            return;
        }

        if (isHoveringRow(mx, my, px, ry2, PANEL_W / 2, ROW_H)) {
            ts.warnMin = Math.max(0f, roundStep(ts.warnMin - step));
            clampThresholdOrder(ts, higherBetter, false);
            root.save();
            return;
        } else if (isHoveringRow(mx, my, px + PANEL_W / 2, ry2, PANEL_W / 2, ROW_H)) {
            ts.warnMin = Math.max(0f, roundStep(ts.warnMin + step));
            clampThresholdOrder(ts, higherBetter, false);
            root.save();
            return;
        }

        if (isHoveringRow(mx, my, px, ry3, PANEL_W, ROW_H)) {
            thresholdPanelOpen = false;
        }
    }

    /**
     * Keeps goodMin/warnMin from inverting after an adjustment: for
     * higher-is-better stats warnMin stays &lt;= goodMin, for lower-is-better
     * it stays &gt;= goodMin. adjustedGood says which field just moved.
     */
    private void clampThresholdOrder(MtssConfig.ThresholdSettings ts, boolean higherBetter, boolean adjustedGood) {
        if (higherBetter) {
            if (adjustedGood) {
                if (ts.warnMin > ts.goodMin) ts.warnMin = ts.goodMin;
            } else {
                if (ts.warnMin > ts.goodMin) ts.goodMin = ts.warnMin;
            }
        } else {
            if (adjustedGood) {
                if (ts.warnMin < ts.goodMin) ts.warnMin = ts.goodMin;
            } else {
                if (ts.warnMin < ts.goodMin) ts.goodMin = ts.warnMin;
            }
        }
    }

    /** Rounds to the nearest 0.1 to avoid float drift from repeated +/- 0.5 steps. */
    private float roundStep(float v) {
        return Math.round(v * 10f) / 10f;
    }

    // ── Shared panel drawing helper ───────────────────────────────────────────

    private void drawPanel(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                           String[] labels, int mx, int my, int panelW, int count) {
        int panelH = PANEL_PAD * 2 + ROW_H * count;
        int px = clampX(menuX, panelW);
        int py = clampY(menuY, panelH);
        g.fill(px, py, px + panelW, py + panelH, 0xEE111111);
        g.outline(px, py, panelW, panelH, 0xFFFFAA00);
        for (int i = 0; i < labels.length; i++) {
            int ry = py + PANEL_PAD + i * ROW_H;
            if (isHoveringRow(mx, my, px, ry, panelW, ROW_H))
                g.fill(px + 1, ry, px + panelW - 1, ry + ROW_H, 0x44FFFFFF);
            g.text(font, labels[i], px + PANEL_PAD, ry + 2, 0xFFFFFFFF, false);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mouse events
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx  = (int) event.x();
        int my  = (int) event.y();
        int btn = event.button();

        MtssConfig root = MtssConfig.getInstance();

        if (menuKind == MenuKind.RENAME) {
            menuKind = MenuKind.NONE;
            return true;
        }
        if (menuKind == MenuKind.TEMPLATE_EDIT) {
            // Click anywhere outside the text box cancels, same as RENAME —
            // Enter (in keyPressed) is the confirm path.
            menuKind = MenuKind.NONE;
            templateEditIndex = -1;
            return true;
        }
        if (menuKind == MenuKind.LIST_CONTEXT) {
            MtssConfig.StatListConfig lc = getListById(menuListId);
            if (lc != null && isInsideListContextMenu(mx, my)) handleListContextMenuClick(mx, my, lc);
            else menuKind = MenuKind.NONE;
            return true;
        }
        if (menuKind == MenuKind.EMPTY_SPACE) {
            if (isInsideEmptySpaceMenu(mx, my)) handleEmptySpaceMenuClick(mx, my, root);
            else menuKind = MenuKind.NONE;
            return true;
        }
        if (templateListOpen) {
            MtssConfig.StatListConfig lc = getListById(menuListId);
            if (lc != null && isInsideTemplateListPanel(mx, my, lc)) handleTemplateListPanelClick(mx, my, lc);
            else templateListOpen = false;
            return true;
        }
        if (colorScaleOpen) {
            MtssConfig.StatListConfig lc = getListById(menuListId);
            if (lc != null && isInsideColorScalePanel(mx, my)) handleColorScalePanelClick(mx, my, lc);
            else { colorScaleOpen = false; appearanceOpen = true; } // click outside → back to Appearance, since Color/Scale nests inside it
            return true;
        }
        if (appearanceOpen) {
            MtssConfig.StatListConfig lc = getListById(menuListId);
            if (lc != null && isInsideAppearancePanel(mx, my)) handleAppearancePanelClick(mx, my, lc);
            else appearanceOpen = false;
            return true;
        }
        if (reorderOpen) {
            MtssConfig.StatListConfig lc = getListById(menuListId);
            if (lc != null && statSettingsStat != null && thresholdPanelOpen) {
                // Threshold sub-panel is open — check bounds and route
                if (isInsideThresholdPanel(mx, my)) handleThresholdPanelClick(mx, my, lc);
                else { thresholdPanelOpen = false; } // click outside → back to stat settings
            } else if (lc != null && statSettingsStat != null) {
                // Stat settings panel is open — check bounds and route
                if (isInsideStatSettingsPanel(mx, my)) handleStatSettingsPanelClick(mx, my, lc);
                else { statSettingsStat = null; } // click outside → back to reorder
            } else if (lc != null && isInsideReorderPanel(mx, my, lc)) {
                handleReorderPanelClick(mx, my, lc);
            } else {
                reorderOpen = false;
                statSettingsStat = null;
            }
            return true;
        }

        List<MtssConfig.StatListConfig> lists = root.lists;
        for (int i = lists.size() - 1; i >= 0; i--) {
            MtssConfig.StatListConfig lc = lists.get(i);
            int[] b = getListBounds(lc);
            if (!isHoveringBox(mx, my, b[0], b[1], b[2], b[3])) continue;
            if (btn == 0) {
                dragging       = true;
                draggingListId = lc.id;
                dragOffsetX    = mx - b[0];
                dragOffsetY    = my - b[1];
                dragLiveX      = b[0];
                dragLiveY      = b[1];
                dragBoxW       = b[2];
                dragBoxH       = b[3];
                dragSnapX      = MtssConfig.SnapX.NONE;
                dragSnapY      = MtssConfig.SnapY.NONE;
                return true;
            } else if (btn == 1) {
                menuKind   = MenuKind.LIST_CONTEXT;
                menuListId = lc.id;
                menuX      = mx;
                menuY      = my;
                return true;
            }
        }

        if (btn == 1) {
            menuKind = MenuKind.EMPTY_SPACE;
            menuX    = mx;
            menuY    = my;
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging && event.button() == 0) {
            int mx   = (int) event.x();
            int my   = (int) event.y();
            int rawX = Math.max(0, Math.min(width  - dragBoxW, mx - dragOffsetX));
            int rawY = Math.max(0, Math.min(height - dragBoxH, my - dragOffsetY));
            int[] snapped = applySnap(rawX, rawY, dragBoxW, dragBoxH);
            dragLiveX = snapped[0];
            dragLiveY = snapped[1];
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging && event.button() == 0) {
            MtssConfig.StatListConfig lc = getListById(draggingListId);
            if (lc != null) {
                lc.snapX = dragSnapX;
                lc.snapY = dragSnapY;
                snapToNearestCorner(lc, dragLiveX, dragLiveY, dragBoxW, dragBoxH);
            }
            dragging       = false;
            draggingListId = -1;
            dragSnapX      = MtssConfig.SnapX.NONE;
            dragSnapY      = MtssConfig.SnapY.NONE;
            MtssConfig.getInstance().save();
            return true;
        }
        return super.mouseReleased(event);
    }

    // ── Keyboard ─────────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (menuKind == MenuKind.RENAME) {
            if (keyCode == 256) { // Escape — cancel
                menuKind = MenuKind.NONE;
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter / numpad Enter — confirm
                MtssConfig.StatListConfig lc = getListById(menuListId);
                if (lc != null) {
                    String trimmed = renameBuffer.toString().trim();
                    lc.name = trimmed.isEmpty() ? "List " + lc.id : trimmed;
                    MtssConfig.getInstance().save();
                }
                menuKind = MenuKind.NONE;
                return true;
            }
            if (keyCode == 259 && !renameBuffer.isEmpty()) { // Backspace
                renameBuffer.deleteCharAt(renameBuffer.length() - 1);
                return true;
            }
            return true;
        }
        if (menuKind == MenuKind.TEMPLATE_EDIT) {
            if (keyCode == 256) { // Escape — cancel, discard edits to this line
                menuKind = MenuKind.NONE;
                templateEditIndex = -1;
                templateListOpen = true; // return to the line list, not the raw canvas
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter / numpad Enter — confirm
                MtssConfig.StatListConfig lc = getListById(menuListId);
                if (lc != null && templateEditIndex >= 0 && templateEditIndex < lc.templateLines.size()) {
                    lc.templateLines.set(templateEditIndex, templateEditBuffer.toString());
                    MtssConfig.getInstance().save();
                    TemplateEngine.invalidate(lc.id);
                }
                menuKind = MenuKind.NONE;
                templateEditIndex = -1;
                templateListOpen = true; // return to the line list to keep editing other lines
                return true;
            }
            if (keyCode == 259 && !templateEditBuffer.isEmpty()) { // Backspace
                templateEditBuffer.deleteCharAt(templateEditBuffer.length() - 1);
                return true;
            }
            return true;
        }
        if (keyCode == 256) { onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (menuKind == MenuKind.RENAME) {
            char ch = (char) event.codepoint();
            if (ch >= 32 && renameBuffer.length() < 32)
                renameBuffer.append(ch);
            return true;
        }
        if (menuKind == MenuKind.TEMPLATE_EDIT) {
            char ch = (char) event.codepoint();
            // Higher cap than renameBuffer's 32 since template lines mix text
            // and tokens and run longer — still bounded so it can't grow unbounded.
            if (ch >= 32 && templateEditBuffer.length() < 200)
                templateEditBuffer.append(ch);
            return true;
        }
        return super.charTyped(event);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Menu click handlers
    // ─────────────────────────────────────────────────────────────────────────

    private void handleListContextMenuClick(int mx, int my,
                                            MtssConfig.StatListConfig lc) {
        int panelH = PANEL_PAD * 2 + ROW_H * LM_COUNT;
        int px = clampX(menuX, PANEL_W), py = clampY(menuY, panelH);
        if (mx < px || mx > px + PANEL_W) return;
        int rel = my - (py + PANEL_PAD);
        if (rel < 0 || rel >= ROW_H * LM_COUNT) return;
        int idx = rel / ROW_H;

        MtssConfig root = MtssConfig.getInstance();
        menuKind = MenuKind.NONE;

        switch (idx) {
            case LM_STATS -> {
                if (lc.useTemplate) { templateListOpen = true; templateEditIndex = -1; }
                else reorderOpen = true;
            }
            case LM_APPEARANCE -> appearanceOpen = true;
            case LM_DUPLICATE  -> { root.duplicateList(lc.id); root.save(); }
            case LM_DELETE     -> { root.removeList(lc.id); root.save(); reorderOpen = false; statSettingsStat = null; thresholdPanelOpen = false; templateListOpen = false; appearanceOpen = false; colorScaleOpen = false; }
        }
    }

    private void handleEmptySpaceMenuClick(int mx, int my, MtssConfig root) {
        int panelH = PANEL_PAD * 2 + ROW_H;
        int px = clampX(menuX, PANEL_W), py = clampY(menuY, panelH);
        if (isHoveringRow(mx, my, px, py + PANEL_PAD, PANEL_W, ROW_H)) {
            MtssConfig.StatListConfig nl = root.createList();
            snapToNearestCorner(nl, mx, my, 0, 0);
            root.save();
        }
        menuKind = MenuKind.NONE;
    }

    private void handleReorderPanelClick(int mx, int my, MtssConfig.StatListConfig lc) {
        List<MtssConfig.Stat> all = allStatsOrdered(lc);
        int panelH = reorderPanelHeight(lc);
        int px     = clampX(menuX, PANEL_W);
        int py     = clampY(menuY, panelH);
        int rowTop = py + PANEL_PAD + ROW_H;

        int closeY = rowTop + all.size() * ROW_H;
        if (isHoveringRow(mx, my, px, closeY, PANEL_W, ROW_H)) { reorderOpen = false; statSettingsStat = null; thresholdPanelOpen = false; return; }

        for (int i = 0; i < all.size(); i++) {
            MtssConfig.Stat stat = all.get(i);
            int ry = rowTop + i * ROW_H;
            if (my < ry || my >= ry + ROW_H) continue;
            int orderIdx = lc.statOrder.indexOf(stat.name());
            // ⚙ cog — open per-stat settings
            if (mx >= px + PANEL_W - 28 && mx < px + PANEL_W - 18) {
                statSettingsStat = stat;
                return;
            }
            // ▲
            if (mx >= px + PANEL_W - 18 && mx < px + PANEL_W - 10 && orderIdx > 0) {
                lc.statOrder.add(orderIdx - 1, lc.statOrder.remove(orderIdx));
            // ▼
            } else if (mx >= px + PANEL_W - 10 && orderIdx < lc.statOrder.size() - 1) {
                lc.statOrder.add(orderIdx + 1, lc.statOrder.remove(orderIdx));
            // toggle enable
            } else {
                lc.setEnabled(stat, !lc.isEnabled(stat));
            }
            MtssConfig.getInstance().save();
            return;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hit-test helpers
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isHoveringBox(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx - 1 && mx <= bx + bw + 1 && my >= by - 1 && my <= by + bh + 1;
    }
    private boolean isHoveringRow(int mx, int my, int px, int ry, int pw, int rh) {
        return mx >= px && mx <= px + pw && my >= ry && my < ry + rh;
    }
    private boolean isInsideListContextMenu(int mx, int my) {
        int panelH = PANEL_PAD * 2 + ROW_H * LM_COUNT;
        int px = clampX(menuX, PANEL_W), py = clampY(menuY, panelH);
        return mx >= px && mx <= px + PANEL_W && my >= py && my <= py + panelH;
    }
    private boolean isInsideEmptySpaceMenu(int mx, int my) {
        int panelH = PANEL_PAD * 2 + ROW_H;
        int px = clampX(menuX, PANEL_W), py = clampY(menuY, panelH);
        return mx >= px && mx <= px + PANEL_W && my >= py && my <= py + panelH;
    }
    private boolean isInsideStatSettingsPanel(int mx, int my) {
        int rows = (statSettingsStat != null) ? statSettingsPanelRows(statSettingsStat) : 3;
        int panelH = PANEL_PAD * 2 + ROW_H * rows;
        int px = clampX(menuX, PANEL_W), py = clampY(menuY, panelH);
        return mx >= px && mx <= px + PANEL_W && my >= py && my <= py + panelH;
    }
    private boolean isInsideReorderPanel(int mx, int my, MtssConfig.StatListConfig lc) {
        int panelH = reorderPanelHeight(lc);
        int px = clampX(menuX, PANEL_W), py = clampY(menuY, panelH);
        return mx >= px && mx <= px + PANEL_W && my >= py && my <= py + panelH;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Snap helpers
    // ─────────────────────────────────────────────────────────────────────────

    private int[] applySnap(int bx, int by, int bw, int bh) {
        int cx = width  / 2, cy = height / 2, T = SNAP_THRESHOLD;

        int snappedX = bx; dragSnapX = MtssConfig.SnapX.NONE; int bestDx = T + 1;
        int d = Math.abs(bx - cx);
        if (d <= T && d < bestDx) { bestDx = d; snappedX = cx;          dragSnapX = MtssConfig.SnapX.LEFT_ON_CENTER; }
        d = Math.abs((bx + bw / 2) - cx);
        if (d <= T && d < bestDx) { bestDx = d; snappedX = cx - bw / 2; dragSnapX = MtssConfig.SnapX.CENTER_ON_CENTER; }
        d = Math.abs((bx + bw) - cx);
        if (d <= T && d < bestDx) {              snappedX = cx - bw;     dragSnapX = MtssConfig.SnapX.RIGHT_ON_CENTER; }

        int snappedY = by; dragSnapY = MtssConfig.SnapY.NONE; int bestDy = T + 1;
        d = Math.abs(by - cy);
        if (d <= T && d < bestDy) { bestDy = d; snappedY = cy;          dragSnapY = MtssConfig.SnapY.TOP_ON_CENTER; }
        d = Math.abs((by + bh / 2) - cy);
        if (d <= T && d < bestDy) { bestDy = d; snappedY = cy - bh / 2; dragSnapY = MtssConfig.SnapY.CENTER_ON_CENTER; }
        d = Math.abs((by + bh) - cy);
        if (d <= T && d < bestDy) {              snappedY = cy - bh;     dragSnapY = MtssConfig.SnapY.BOTTOM_ON_CENTER; }

        return new int[]{ snappedX, snappedY };
    }

    private void snapToNearestCorner(MtssConfig.StatListConfig lc,
                                     int bx, int by, int boxW, int boxH) {
        boolean nearRight  = (bx + boxW / 2) > width  / 2;
        boolean nearBottom = (by + boxH / 2) > height / 2;
        if (!nearRight && !nearBottom) {
            lc.anchorCorner = MtssConfig.Corner.TOP_LEFT;
            lc.anchorDx = bx;                    lc.anchorDy = by;
        } else if (nearRight && !nearBottom) {
            lc.anchorCorner = MtssConfig.Corner.TOP_RIGHT;
            lc.anchorDx = width - (bx + boxW);   lc.anchorDy = by;
        } else if (!nearRight) {
            lc.anchorCorner = MtssConfig.Corner.BOTTOM_LEFT;
            lc.anchorDx = bx;                    lc.anchorDy = height - (by + boxH);
        } else {
            lc.anchorCorner = MtssConfig.Corner.BOTTOM_RIGHT;
            lc.anchorDx = width - (bx + boxW);   lc.anchorDy = height - (by + boxH);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utility
    // ─────────────────────────────────────────────────────────────────────────

    private int[] getListBounds(MtssConfig.StatListConfig lc) {
        var font = this.font;
        MtssRenderer.LineCache cache = MtssRenderer.getCachedLines(lc);
        int lineH = font.lineHeight + 1;
        int boxW, boxH;
        if (cache.rowKinds().isEmpty()) {
            // Mirrors drawList's empty-placeholder sizing.
            String placeholder = I18n.get("gui.mtss.no_stats");
            boxW = font.width(placeholder) + 4;
            boxH = lineH + 3;
        } else {
            boxW = cache.boxW(font);
            boxH = cache.boxH(font);
        }
        int[] pos = MtssRenderer.getPosition(lc, width, height, boxW, boxH);
        return new int[]{ pos[0], pos[1], boxW, boxH };
    }

    private MtssConfig.StatListConfig getListById(int id) {
        for (MtssConfig.StatListConfig lc : MtssConfig.getInstance().lists)
            if (lc.id == id) return lc;
        return null;
    }

    private List<MtssConfig.Stat> allStatsOrdered(MtssConfig.StatListConfig lc) {
        List<MtssConfig.Stat> result = new ArrayList<>();
        for (String name : lc.statOrder) {
            try { result.add(MtssConfig.Stat.valueOf(name)); }
            catch (IllegalArgumentException ignored) {}
        }
        return result;
    }

    /** Shared height formula for the reorder/toggle panel: header + one row per stat + close row. */
    private int reorderPanelHeight(MtssConfig.StatListConfig lc) {
        return PANEL_PAD * 2 + ROW_H + ROW_H * allStatsOrdered(lc).size() + ROW_H;
    }

    private int clampX(int x, int w) { return Math.max(0, Math.min(width  - w - 4, x)); }
    private int clampY(int y, int h) { return Math.max(0, Math.min(height - h - 4, y)); }
}
