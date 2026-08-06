package bottled.mtss.gui;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.gui.panel.AppearancePanel;
import bottled.mtss.gui.panel.ColorScalePanel;
import bottled.mtss.gui.panel.EmptySpaceMenuPanel;
import bottled.mtss.gui.panel.ListContextMenuPanel;
import bottled.mtss.gui.panel.RenameBoxPanel;
import bottled.mtss.gui.panel.ReorderPanel;
import bottled.mtss.gui.panel.StatSettingsPanel;
import bottled.mtss.gui.panel.TemplateListPanel;
import bottled.mtss.gui.panel.ThresholdPanel;
import bottled.mtss.gui.render.ListPreviewRenderer;
import bottled.mtss.gui.render.PanelChrome;
import bottled.mtss.hud.LineCache;
import bottled.mtss.hud.ListPositioner;
import bottled.mtss.hud.TemplateEngine;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.List;


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
    /**
     * Which top-level popup (if any) is open. Sub-panels nested inside a
     * top-level popup (Appearance → Color/Scale, Stat Settings → Thresholds,
     * Template list → line edit box) are tracked by their own boolean/index
     * fields below rather than folded into this enum, mirroring the
     * original nesting: a boolean flag opens a sibling sub-panel from its
     * parent, with its own render/click/hit-test.
     */
    private enum MenuKind { NONE, LIST_CONTEXT, EMPTY_SPACE, RENAME, TEMPLATE_EDIT }
    private MenuKind menuKind   = MenuKind.NONE;
    private int      menuListId = -1;
    private int      menuX, menuY;
    private boolean  reorderOpen       = false;
    /** Category-expansion/scroll state for the redesigned {@link ReorderPanel} — reset whenever the panel opens (see the LM_STATS case in {@link #handleListContextMenuClick}). */
    private final ReorderPanel.UiState reorderUiState = new ReorderPanel.UiState();
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

    private static final int SNAP_THRESHOLD = 6;

    private boolean colorScaleOpen = false;

    /** Whether the per-stat custom-threshold sub-panel is open (opened from the stat settings panel). */
    private boolean thresholdPanelOpen = false;

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

        LineCache.tickCache();

        MtssConfig root = MtssConfig.getInstance();
        var font = this.font;

        if (root.lists.isEmpty()) {
            g.centeredText(font, "§7" + I18n.get("gui.mtss.no_lists"),
                    width / 2, height / 2 - 6, 0xFFAAAAAA);
        }

        if (dragging) {
            ListPreviewRenderer.drawSnapLines(g, width, height, dragSnapX, dragSnapY,
                    dragLiveX, dragLiveY, dragBoxW, dragBoxH);
        }

        for (MtssConfig.StatListConfig lc : root.lists) {
            boolean isBeingDragged = dragging && lc.id == draggingListId;
            ListPreviewRenderer.drawList(g, font, lc, mx, my, isBeingDragged, dragLiveX, dragLiveY,
                    width, height);
        }

        g.centeredText(font, "§7" + I18n.get("gui.mtss.hint"),
                width / 2, height - 14, 0xFFAAAAAA);

        if (menuKind == MenuKind.LIST_CONTEXT) {
            MtssConfig.StatListConfig lc = getListById(menuListId);
            if (lc == null) menuKind = MenuKind.NONE;
            else ListContextMenuPanel.render(g, font, mx, my, menuX, menuY, width, height, lc);
        } else if (menuKind == MenuKind.EMPTY_SPACE) {
            EmptySpaceMenuPanel.render(g, font, mx, my, menuX, menuY, width, height);
        } else if (menuKind == MenuKind.RENAME) {
            RenameBoxPanel.render(g, font, menuX, menuY, width, height, renameBuffer.toString());
        } else if (menuKind == MenuKind.TEMPLATE_EDIT) {
            TemplateListPanel.renderEditBox(g, font, menuX, menuY, width, height,
                    templateEditIndex, templateEditBuffer.toString());
        } else if (colorScaleOpen) {
            MtssConfig.StatListConfig lc = getListById(menuListId);
            if (lc == null) colorScaleOpen = false;
            else ColorScalePanel.render(g, font, mx, my, menuX, menuY, width, height, lc);
        } else if (appearanceOpen) {
            MtssConfig.StatListConfig lc = getListById(menuListId);
            if (lc == null) appearanceOpen = false;
            else AppearancePanel.render(g, font, mx, my, menuX, menuY, width, height, lc);
        } else if (reorderOpen && statSettingsStat != null && thresholdPanelOpen) {
            MtssConfig.StatListConfig lc = getListById(menuListId);
            if (lc == null) { thresholdPanelOpen = false; }
            else {
                MtssConfig.ThresholdSettings ts = lc.getThreshold(statSettingsStat);
                if (ts == null) thresholdPanelOpen = false;
                else ThresholdPanel.render(g, font, mx, my, menuX, menuY, width, height, lc, statSettingsStat, ts);
            }
        } else if (reorderOpen && statSettingsStat != null) {
            MtssConfig.StatListConfig lc = getListById(menuListId);
            if (lc == null) statSettingsStat = null;
            else StatSettingsPanel.render(g, font, mx, my, menuX, menuY, width, height, lc, statSettingsStat);
        } else if (reorderOpen) {
            MtssConfig.StatListConfig lc = getListById(menuListId);
            if (lc == null) reorderOpen = false;
            else ReorderPanel.render(g, font, mx, my, menuX, menuY, width, height, lc, reorderUiState);
        } else if (templateListOpen) {
            MtssConfig.StatListConfig lc = getListById(menuListId);
            if (lc == null) templateListOpen = false;
            else TemplateListPanel.render(g, font, mx, my, menuX, menuY, width, height, lc);
        }

        super.extractRenderState(g, mx, my, partial);
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
            if (lc != null && ListContextMenuPanel.isInside(mx, my, menuX, menuY, width, height))
                handleListContextMenuClick(mx, my, lc);
            else menuKind = MenuKind.NONE;
            return true;
        }
        if (menuKind == MenuKind.EMPTY_SPACE) {
            if (EmptySpaceMenuPanel.isInside(mx, my, menuX, menuY, width, height))
                handleEmptySpaceMenuClick(mx, my, root);
            else menuKind = MenuKind.NONE;
            return true;
        }
        if (templateListOpen) {
            MtssConfig.StatListConfig lc = getListById(menuListId);
            if (lc != null && TemplateListPanel.isInside(mx, my, menuX, menuY, width, height, lc))
                handleTemplateListPanelClick(mx, my, lc);
            else templateListOpen = false;
            return true;
        }
        if (colorScaleOpen) {
            MtssConfig.StatListConfig lc = getListById(menuListId);
            if (lc != null && ColorScalePanel.isInside(mx, my, menuX, menuY, width, height)) {
                boolean back = ColorScalePanel.handleClick(mx, my, menuX, menuY, width, height, lc, root);
                if (back) { colorScaleOpen = false; appearanceOpen = true; }
            } else {
                colorScaleOpen = false; appearanceOpen = true; // click outside → back to Appearance, since Color/Scale nests inside it
            }
            return true;
        }
        if (appearanceOpen) {
            MtssConfig.StatListConfig lc = getListById(menuListId);
            if (lc != null && AppearancePanel.isInside(mx, my, menuX, menuY, width, height))
                handleAppearancePanelClick(mx, my, lc);
            else appearanceOpen = false;
            return true;
        }
        if (reorderOpen) {
            MtssConfig.StatListConfig lc = getListById(menuListId);
            if (lc != null && statSettingsStat != null && thresholdPanelOpen) {
                // Threshold sub-panel is open — check bounds and route
                if (ThresholdPanel.isInside(mx, my, menuX, menuY, width, height, font)) {
                    handleThresholdPanelClick(mx, my, lc);
                } else {
                    thresholdPanelOpen = false; // click outside → back to stat settings
                }
            } else if (lc != null && statSettingsStat != null) {
                // Stat settings panel is open — check bounds and route
                if (StatSettingsPanel.isInside(mx, my, menuX, menuY, width, height, statSettingsStat)) {
                    handleStatSettingsPanelClick(mx, my, lc);
                } else {
                    statSettingsStat = null; // click outside → back to reorder
                }
            } else if (lc != null && ReorderPanel.isInside(mx, my, menuX, menuY, width, height, lc, reorderUiState)) {
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
            if (!PanelChrome.isHoveringBox(mx, my, b[0], b[1], b[2], b[3])) continue;
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
        int codepoint = event.codepoint();
        if (menuKind == MenuKind.RENAME) {
            // (char) truncates codepoints outside the BMP (e.g. most emoji) to garbage —
            // Character.toChars() expands to a surrogate pair instead when needed.
            if (codepoint >= 32 && renameBuffer.length() < 32)
                renameBuffer.append(Character.toChars(codepoint));
            return true;
        }
        if (menuKind == MenuKind.TEMPLATE_EDIT) {
            // Higher cap than renameBuffer's 32 since template lines mix text
            // and tokens and run longer — still bounded so it can't grow unbounded.
            if (codepoint >= 32 && templateEditBuffer.length() < 200)
                templateEditBuffer.append(Character.toChars(codepoint));
            return true;
        }
        return super.charTyped(event);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Menu click handlers
    // ─────────────────────────────────────────────────────────────────────────

    private void handleListContextMenuClick(int mx, int my, MtssConfig.StatListConfig lc) {
        int idx = ListContextMenuPanel.rowAt(mx, my, menuX, menuY, width, height);
        if (idx < 0) return;

        MtssConfig root = MtssConfig.getInstance();
        menuKind = MenuKind.NONE;

        switch (idx) {
            case ListContextMenuPanel.LM_STATS -> {
                if (lc.useTemplate) { templateListOpen = true; templateEditIndex = -1; }
                else { reorderOpen = true; reorderUiState.reset(); }
            }
            case ListContextMenuPanel.LM_APPEARANCE -> appearanceOpen = true;
            case ListContextMenuPanel.LM_DUPLICATE  -> { root.duplicateList(lc.id); root.save(); }
            case ListContextMenuPanel.LM_DELETE     -> {
                root.removeList(lc.id);
                root.save();
                reorderOpen = false;
                statSettingsStat = null;
                thresholdPanelOpen = false;
                templateListOpen = false;
                appearanceOpen = false;
                colorScaleOpen = false;
            }
        }
    }

    private void handleEmptySpaceMenuClick(int mx, int my, MtssConfig root) {
        if (EmptySpaceMenuPanel.handleClick(mx, my, menuX, menuY, width, height)) {
            MtssConfig.StatListConfig nl = root.createList();
            snapToNearestCorner(nl, mx, my, 0, 0);
            root.save();
        }
        menuKind = MenuKind.NONE;
    }

    private void handleAppearancePanelClick(int mx, int my, MtssConfig.StatListConfig lc) {
        int idx = AppearancePanel.rowAt(mx, my, menuX, menuY, width, height);
        if (idx < 0) return;

        MtssConfig root = MtssConfig.getInstance();

        switch (idx) {
            case AppearancePanel.AP_RENAME        -> { appearanceOpen = false; menuKind = MenuKind.RENAME; renameBuffer = new StringBuilder(lc.displayName()); }
            case AppearancePanel.AP_BG            -> { lc.showBackground = !lc.showBackground; root.save(); }
            case AppearancePanel.AP_SHADOW        -> { lc.textShadow     = !lc.textShadow;     root.save(); }
            case AppearancePanel.AP_COLOR_SCALE   -> { appearanceOpen = false; colorScaleOpen = true; }
            case AppearancePanel.AP_TEMPLATE_MODE -> { lc.useTemplate = !lc.useTemplate; root.save(); }
            case AppearancePanel.AP_BACK          -> { appearanceOpen = false; menuKind = MenuKind.LIST_CONTEXT; }
        }
    }

    private void handleReorderPanelClick(int mx, int my, MtssConfig.StatListConfig lc) {
        MtssConfig.Stat clickedCog = ReorderPanel.handleClick(mx, my, menuX, menuY, width, height, lc, reorderUiState,
                () -> { reorderOpen = false; statSettingsStat = null; thresholdPanelOpen = false; });
        if (clickedCog != null) {
            statSettingsStat = clickedCog;
        }
    }

    private void handleStatSettingsPanelClick(int mx, int my, MtssConfig.StatListConfig lc) {
        if (statSettingsStat == null) return;
        StatSettingsPanel.ClickResult result =
                StatSettingsPanel.handleClick(mx, my, menuX, menuY, width, height, lc, statSettingsStat);
        switch (result) {
            case OPEN_THRESHOLDS -> thresholdPanelOpen = true;
            case BACK            -> statSettingsStat = null; // back to reorder panel
            case HANDLED, NONE   -> { /* no state transition */ }
        }
    }

    private void handleThresholdPanelClick(int mx, int my, MtssConfig.StatListConfig lc) {
        if (statSettingsStat == null) return;
        MtssConfig.ThresholdSettings ts = lc.getThreshold(statSettingsStat);
        if (ts == null) { thresholdPanelOpen = false; return; }

        boolean back = ThresholdPanel.handleClick(mx, my, menuX, menuY, width, height, font,
                statSettingsStat, ts, MtssConfig.getInstance());
        if (back) thresholdPanelOpen = false;
    }

    private void handleTemplateListPanelClick(int mx, int my, MtssConfig.StatListConfig lc) {
        TemplateListPanel.ClickResult result =
                TemplateListPanel.handleClick(mx, my, menuX, menuY, width, height, lc);
        switch (result.kind()) {
            case CLOSED -> templateListOpen = false;
            case EDIT_LINE -> {
                templateEditIndex  = result.editIndex();
                templateEditBuffer = new StringBuilder(lc.templateLines.get(result.editIndex()));
                menuKind = MenuKind.TEMPLATE_EDIT;
            }
            case NONE -> { /* add/remove already applied inside handleClick */ }
        }
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
        LineCache cache = LineCache.getCachedLines(lc);
        int lineH = font.lineHeight + 1;
        float scale = lc.textScale <= 0f ? 1f : lc.textScale;
        int boxW, boxH;
        if (cache.rowKinds().isEmpty()) {
            // Mirrors ListPreviewRenderer.drawList's empty-placeholder sizing (no scale).
            String placeholder = I18n.get("gui.mtss.no_stats");
            boxW = font.width(placeholder) + 4;
            boxH = lineH + 3;
        } else {
            // Mirrors ListPreviewRenderer.drawList's scaled sizing, which mirrors MtssRenderer.render().
            boxW = Math.round(cache.boxW(font) * scale);
            boxH = Math.round(cache.boxH(font) * scale);
        }
        int[] pos = ListPositioner.getPosition(lc, width, height, boxW, boxH);
        return new int[]{ pos[0], pos[1], boxW, boxH };
    }

    private MtssConfig.StatListConfig getListById(int id) {
        for (MtssConfig.StatListConfig lc : MtssConfig.getInstance().lists)
            if (lc.id == id) return lc;
        return null;
    }
}
