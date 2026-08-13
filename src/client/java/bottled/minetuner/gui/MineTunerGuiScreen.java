package bottled.minetuner.gui;

import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.config.cloth.MineTunerClothConfigScreen;
import bottled.minetuner.gui.panel.*;
import bottled.minetuner.gui.render.ListPreviewRenderer;
import bottled.minetuner.gui.render.PanelChrome;
import bottled.minetuner.gui.render.PanelTransition;
import bottled.minetuner.hud.LineCache;
import bottled.minetuner.hud.ListPositioner;
import bottled.minetuner.hud.TemplateEngine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.List;


public class MineTunerGuiScreen extends Screen {

    /**
     * Category-expansion/scroll state for the redesigned {@link ReorderPanel}.
     */
    private final ReorderPanel.UiState reorderUiState = new ReorderPanel.UiState();
    private boolean dragging = false;
    private int draggingListId = -1;
    private int dragOffsetX, dragOffsetY;
    private int dragLiveX, dragLiveY;
    private int dragBoxW, dragBoxH;

    private MineTunerConfig.SnapX dragSnapX = MineTunerConfig.SnapX.NONE;
    private MineTunerConfig.SnapY dragSnapY = MineTunerConfig.SnapY.NONE;
    private MenuKind menuKind = MenuKind.NONE;
    private int menuListId = -1;
    private int menuX, menuY;
    private boolean reorderOpen = false;
    /**
     * Which stat's settings panel is open (null = reorder panel showing).
     */
    private MineTunerConfig.Stat statSettingsStat = null;
    private StringBuilder renameBuffer = new StringBuilder();

    /**
     * Whether the template line list (one row per templateLines entry, plus an
     * "add new line" row and a "back" row) is the currently open menu panel.
     */
    private boolean templateListOpen = false;
    /**
     * Index into templateLines being text-edited, or -1 when the line list itself
     * (not a specific line) is what's open, or when nothing template-related is open.
     */
    private int templateEditIndex = -1;
    private StringBuilder templateEditBuffer = new StringBuilder();

    /**
     * True when the Appearance sub-panel (rename, background, shadow, color/scale,
     * snap settings) is the currently open menu panel.
     */
    private boolean appearanceOpen = false;
    private boolean colorScaleOpen = false;
    /**
     * Whether the per-stat custom-threshold sub-panel is open (opened from the
     * ⚙ cog on a stat's settings panel via StatSettingsPanel.ClickResult.OPEN_THRESHOLDS).
     */
    private boolean thresholdPanelOpen = false;
    /**
     * Confirm deletion panel
     */
    private boolean deletePanelOpen = false;
    /**
     * Render-only transition state for popup and nested-panel navigation.
     */
    private final PanelTransition panelTransition = new PanelTransition();

    public MineTunerGuiScreen() {
        super(Component.translatable("gui.minetuner.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Force-save on close as a safety net, even though every mutation already.
     */
    @Override
    public void onClose() {
        MineTunerConfig.getInstance().save();
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
        g.fill(0, 0, width, height, 0x80000000);

        LineCache.tickCache();

        MineTunerConfig root = MineTunerConfig.getInstance();
        var font = this.font;

        if (root.lists.isEmpty()) {
            g.centeredText(font, "§7" + I18n.get("gui.minetuner.no_lists"),
                    width / 2, height / 2 - 6, 0xFFAAAAAA);
        }

        if (dragging) {
            ListPreviewRenderer.drawSnapLines(g, width, height, dragSnapX, dragSnapY,
                    dragLiveX, dragLiveY, dragBoxW, dragBoxH);
        }

        for (MineTunerConfig.StatListConfig lc : root.lists) {
            boolean isBeingDragged = dragging && lc.id == draggingListId;
            ListPreviewRenderer.drawList(g, font, lc, mx, my, isBeingDragged, dragLiveX, dragLiveY,
                    width, height);
        }

        g.centeredText(font, "§7" + I18n.get("gui.minetuner.hint"),
                width / 2, height - 14, 0xFFAAAAAA);

        PanelRoute route = activePanelRoute();
        panelTransition.updateRoute(route.signature(statSettingsStat));
        if (route != PanelRoute.NONE) panelTransition.push(g, menuX, menuY);

        if (menuKind == MenuKind.LIST_CONTEXT) {
            MineTunerConfig.StatListConfig lc = getListById(menuListId);
            if (lc == null) menuKind = MenuKind.NONE;
            else ListContextMenuPanel.render(g, font, mx, my, menuX, menuY, width, height, lc,
                    panelTransition.progress());
        } else if (menuKind == MenuKind.EMPTY_SPACE) {
            EmptySpaceMenuPanel.render(g, font, mx, my, menuX, menuY, width, height);
        } else if (menuKind == MenuKind.RENAME) {
            RenameBoxPanel.render(g, font, menuX, menuY, width, height, renameBuffer.toString());
        } else if (menuKind == MenuKind.TEMPLATE_EDIT) {
            TemplateListPanel.renderEditBox(g, font, menuX, menuY, width, height,
                    templateEditIndex, templateEditBuffer.toString());
        } else if (deletePanelOpen) {
            MineTunerConfig.StatListConfig lc = getListById(menuListId);
            if (lc == null) {
                deletePanelOpen = false;
                menuKind = MenuKind.NONE;
            } else {
                DeletePanel.render(g, font, mx, my, menuX, menuY, width, height, lc);
            }
        } else if (colorScaleOpen) {
            MineTunerConfig.StatListConfig lc = getListById(menuListId);
            if (lc == null) colorScaleOpen = false;
            else ColorScalePanel.render(g, font, mx, my, menuX, menuY, width, height, lc);
        } else if (appearanceOpen) {
            MineTunerConfig.StatListConfig lc = getListById(menuListId);
            if (lc == null) appearanceOpen = false;
            else AppearancePanel.render(g, font, mx, my, menuX, menuY, width, height, lc);
        } else if (reorderOpen && statSettingsStat != null && thresholdPanelOpen) {
            MineTunerConfig.StatListConfig lc = getListById(menuListId);
            if (lc == null) {
                thresholdPanelOpen = false;
            } else {
                MineTunerConfig.ThresholdSettings ts = lc.getThreshold(statSettingsStat);
                if (ts == null) thresholdPanelOpen = false;
                else ThresholdPanel.render(g, font, mx, my, menuX, menuY, width, height, lc, statSettingsStat, ts);
            }
        } else if (reorderOpen && statSettingsStat != null) {
            MineTunerConfig.StatListConfig lc = getListById(menuListId);
            if (lc == null) statSettingsStat = null;
            else StatSettingsPanel.render(g, font, mx, my, menuX, menuY, width, height, lc, statSettingsStat);
        } else if (reorderOpen) {
            MineTunerConfig.StatListConfig lc = getListById(menuListId);
            if (lc == null) reorderOpen = false;
            else ReorderPanel.render(g, font, mx, my, menuX, menuY, width, height, lc, reorderUiState);
        } else if (templateListOpen) {
            MineTunerConfig.StatListConfig lc = getListById(menuListId);
            if (lc == null) templateListOpen = false;
            else TemplateListPanel.render(g, font, mx, my, menuX, menuY, width, height, lc);
        }

        if (route != PanelRoute.NONE) panelTransition.pop(g);

        super.extractRenderState(g, mx, my, partial);
    }

    // --- Rendering ---

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int mx = (int) event.x();
        int my = (int) event.y();
        int btn = event.button();

        MineTunerConfig root = MineTunerConfig.getInstance();

        if (menuKind == MenuKind.RENAME) {
            menuKind = MenuKind.NONE;
            return true;
        }
        if (menuKind == MenuKind.TEMPLATE_EDIT) {
            // Click anywhere outside the text box cancels, same as RENAME.
            // Enter (in keyPressed) is the confirm path.
            menuKind = MenuKind.NONE;
            templateEditIndex = -1;
            return true;
        }
        if (menuKind == MenuKind.LIST_CONTEXT) {
            MineTunerConfig.StatListConfig lc = getListById(menuListId);
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
        if (deletePanelOpen) {
            MineTunerConfig.StatListConfig lc = getListById(menuListId);
            if (lc != null && DeletePanel.isInside(mx, my, menuX, menuY, width, height))
                handleDeletePanelClick(mx, my, lc);
            else deletePanelOpen = false;
            return true;
        }
        if (templateListOpen) {
            MineTunerConfig.StatListConfig lc = getListById(menuListId);
            if (lc != null && TemplateListPanel.isInside(mx, my, menuX, menuY, width, height, lc))
                handleTemplateListPanelClick(mx, my, lc);
            else templateListOpen = false;
            return true;
        }
        if (colorScaleOpen) {
            MineTunerConfig.StatListConfig lc = getListById(menuListId);
            if (lc != null && ColorScalePanel.isInside(mx, my, menuX, menuY, width, height)) {
                boolean back = ColorScalePanel.handleClick(mx, my, menuX, menuY, width, height, lc, root);
                if (back) {
                    colorScaleOpen = false;
                    appearanceOpen = true;
                }
            } else {
                colorScaleOpen = false;
                appearanceOpen = true; // click outside → back to Appearance, since Color/Scale nests inside it.
            }
            return true;
        }
        if (appearanceOpen) {
            MineTunerConfig.StatListConfig lc = getListById(menuListId);
            if (lc != null && AppearancePanel.isInside(mx, my, menuX, menuY, width, height))
                handleAppearancePanelClick(mx, my, lc);
            else appearanceOpen = false;
            return true;
        }
        if (reorderOpen) {
            MineTunerConfig.StatListConfig lc = getListById(menuListId);
            if (lc != null && statSettingsStat != null && thresholdPanelOpen) {
                // Threshold sub-panel is open.
                if (ThresholdPanel.isInside(mx, my, menuX, menuY, width, height, font)) {
                    handleThresholdPanelClick(mx, my, lc);
                } else {
                    thresholdPanelOpen = false; // click outside → back to stat settings.
                }
            } else if (lc != null && statSettingsStat != null) {
                // Stat settings panel is open.
                if (StatSettingsPanel.isInside(mx, my, menuX, menuY, width, height, statSettingsStat)) {
                    handleStatSettingsPanelClick(mx, my, lc);
                } else {
                    statSettingsStat = null; // click outside → back to reorder.
                }
            } else if (lc != null && ReorderPanel.isInside(mx, my, menuX, menuY, width, height, lc, reorderUiState)) {
                handleReorderPanelClick(mx, my, lc);
            } else {
                reorderOpen = false;
                statSettingsStat = null;
            }
            return true;
        }

        List<MineTunerConfig.StatListConfig> lists = root.lists;
        for (int i = lists.size() - 1; i >= 0; i--) {
            MineTunerConfig.StatListConfig lc = lists.get(i);
            int[] b = getListBounds(lc);
            if (!PanelChrome.isHoveringBox(mx, my, b[0], b[1], b[2], b[3])) continue;
            if (btn == 0) {
                dragging = true;
                draggingListId = lc.id;
                dragOffsetX = mx - b[0];
                dragOffsetY = my - b[1];
                dragLiveX = b[0];
                dragLiveY = b[1];
                dragBoxW = b[2];
                dragBoxH = b[3];
                dragSnapX = MineTunerConfig.SnapX.NONE;
                dragSnapY = MineTunerConfig.SnapY.NONE;
                return true;
            } else if (btn == 1) {
                menuKind = MenuKind.LIST_CONTEXT;
                menuListId = lc.id;
                menuX = mx;
                menuY = my;
                return true;
            }
        }

        if (btn == 1) {
            menuKind = MenuKind.EMPTY_SPACE;
            menuX = mx;
            menuY = my;
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    // --- Mouse events ---

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging && event.button() == 0) {
            int mx = (int) event.x();
            int my = (int) event.y();
            int rawX = Math.max(0, Math.min(width - dragBoxW, mx - dragOffsetX));
            int rawY = Math.max(0, Math.min(height - dragBoxH, my - dragOffsetY));
            int[] snapped = applySnap(rawX, rawY, dragBoxW, dragBoxH);
            dragLiveX = snapped[0];
            dragLiveY = snapped[1];
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (reorderOpen && statSettingsStat == null) {
            MineTunerConfig.StatListConfig lc = getListById(menuListId);
            if (lc != null && ReorderPanel.scrollBy(mouseX, mouseY, verticalAmount,
                    menuX, menuY, width, height, lc, reorderUiState)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging && event.button() == 0) {
            MineTunerConfig.StatListConfig lc = getListById(draggingListId);
            if (lc != null) {
                lc.snapX = dragSnapX;
                lc.snapY = dragSnapY;
                snapToNearestCorner(lc, dragLiveX, dragLiveY, dragBoxW, dragBoxH);
            }
            dragging = false;
            draggingListId = -1;
            dragSnapX = MineTunerConfig.SnapX.NONE;
            dragSnapY = MineTunerConfig.SnapY.NONE;
            MineTunerConfig.getInstance().save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (menuKind == MenuKind.RENAME) {
            if (keyCode == 256) { // Escape.
                menuKind = MenuKind.NONE;
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter / numpad Enter.
                MineTunerConfig.StatListConfig lc = getListById(menuListId);
                if (lc != null) {
                    String trimmed = renameBuffer.toString().trim();
                    lc.name = trimmed.isEmpty() ? "List " + lc.id : trimmed;
                    MineTunerConfig.getInstance().save();
                }
                menuKind = MenuKind.NONE;
                return true;
            }
            if (keyCode == 259 && !renameBuffer.isEmpty()) { // Backspace.
                renameBuffer.deleteCharAt(renameBuffer.length() - 1);
                return true;
            }
            return true;
        }
        if (menuKind == MenuKind.TEMPLATE_EDIT) {
            if (keyCode == 256) { // Escape.
                menuKind = MenuKind.NONE;
                templateEditIndex = -1;
                templateListOpen = true; // return to the line list, not the raw canvas.
                return true;
            }
            if (keyCode == 257 || keyCode == 335) { // Enter / numpad Enter.
                MineTunerConfig.StatListConfig lc = getListById(menuListId);
                if (lc != null && templateEditIndex >= 0 && templateEditIndex < lc.templateLines.size()) {
                    lc.templateLines.set(templateEditIndex, templateEditBuffer.toString());
                    MineTunerConfig.getInstance().save();
                    TemplateEngine.invalidate(lc.id);
                }
                menuKind = MenuKind.NONE;
                templateEditIndex = -1;
                templateListOpen = true; // return to the line list to keep editing other lines.
                return true;
            }
            if (keyCode == 259 && !templateEditBuffer.isEmpty()) { // Backspace.
                templateEditBuffer.deleteCharAt(templateEditBuffer.length() - 1);
                return true;
            }
            return true;
        }
        if (reorderOpen && statSettingsStat == null && reorderUiState.isSearchFocused()) {
            // Search field lives inside ReorderPanel's own row layout rather.
            // than a MenuKind case ( ReorderPanel's UiState doc).
            // Escape-to-unfocus / Enter-to-confirm / Backspace pattern as.
            // RENAME and TEMPLATE_EDIT above, just against reorderUiState.
            // instead of a local StringBuilder field.
            if (keyCode == 256 || keyCode == 257 || keyCode == 335) { // Escape or Enter.
                reorderUiState.toggleSearchFocus();
                return true;
            }
            if (keyCode == 259) { // Backspace.
                reorderUiState.backspaceSearch();
                return true;
            }
            return true;
        }
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int codepoint = event.codepoint();
        if (menuKind == MenuKind.RENAME) {
            // (char) truncates codepoints outside the BMP (e.g. most emoji) down to
            // their low 16 bits, corrupting them, so this uses
            // Character.toChars() which expands to a surrogate pair instead when needed.
            if (codepoint >= 32 && renameBuffer.length() < 32)
                renameBuffer.append(Character.toChars(codepoint));
            return true;
        }
        if (menuKind == MenuKind.TEMPLATE_EDIT) {
            // Higher cap than renameBuffer's 32 since template lines mix text.
            // and tokens and run longer.
            if (codepoint >= 32 && templateEditBuffer.length() < 200)
                templateEditBuffer.append(Character.toChars(codepoint));
            return true;
        }
        if (reorderOpen && statSettingsStat == null && reorderUiState.isSearchFocused()) {
            if (codepoint >= 32)
                reorderUiState.appendSearch((char) codepoint); // stat names are plain ASCII, so no surrogate-pair handling needed here unlike.
            return true;
        }
        return super.charTyped(event);
    }

    private void handleListContextMenuClick(int mx, int my, MineTunerConfig.StatListConfig lc) {
        int idx = ListContextMenuPanel.rowAt(mx, my, menuX, menuY, width, height);
        if (idx < 0) return;

        MineTunerConfig root = MineTunerConfig.getInstance();
        menuKind = MenuKind.NONE;

        switch (idx) {
            case ListContextMenuPanel.LM_STATS -> {
                if (lc.useTemplate) {
                    templateListOpen = true;
                    templateEditIndex = -1;
                } else {
                    reorderOpen = true;
                    reorderUiState.reset();
                }
            }
            case ListContextMenuPanel.LM_APPEARANCE -> appearanceOpen = true;
            case ListContextMenuPanel.LM_DUPLICATE -> {
                root.duplicateList(lc.id);
                root.save();
            }
            case ListContextMenuPanel.LM_DELETE -> {
                deletePanelOpen = true;
                reorderOpen = false;
                statSettingsStat = null;
                thresholdPanelOpen = false;
                templateListOpen = false;
                appearanceOpen = false;
                colorScaleOpen = false;
            }
        }
    }

    private void handleEmptySpaceMenuClick(int mx, int my, MineTunerConfig root) {
        int row = EmptySpaceMenuPanel.rowAt(mx, my, menuX, menuY, width, height);
        if (row == EmptySpaceMenuPanel.ROW_CREATE_LIST) {
            MineTunerConfig.StatListConfig nl = root.createList();
            snapToNearestCorner(nl, mx, my, 0, 0);
            root.save();
        }
        menuKind = MenuKind.NONE;
    }

    private void handleAppearancePanelClick(int mx, int my, MineTunerConfig.StatListConfig lc) {
        int idx = AppearancePanel.rowAt(mx, my, menuX, menuY, width, height);
        if (idx < 0) return;

        MineTunerConfig root = MineTunerConfig.getInstance();

        switch (idx) {
            case AppearancePanel.AP_RENAME -> {
                appearanceOpen = false;
                menuKind = MenuKind.RENAME;
                renameBuffer = new StringBuilder(lc.displayName());
            }
            case AppearancePanel.AP_BG -> {
                lc.showBackground = !lc.showBackground;
                root.save();
            }
            case AppearancePanel.AP_SHADOW -> {
                lc.textShadow = !lc.textShadow;
                root.save();
            }
            case AppearancePanel.AP_COLOR_SCALE -> {
                appearanceOpen = false;
                colorScaleOpen = true;
            }
            case AppearancePanel.AP_TEMPLATE_MODE -> {
                lc.useTemplate = !lc.useTemplate;
                root.save();
            }
            case AppearancePanel.AP_BACK -> {
                appearanceOpen = false;
                menuKind = MenuKind.LIST_CONTEXT;
            }
        }
    }

    private void handleReorderPanelClick(int mx, int my, MineTunerConfig.StatListConfig lc) {
        MineTunerConfig.Stat clickedCog = ReorderPanel.handleClick(mx, my, menuX, menuY, width, height, lc, reorderUiState,
                () -> {
                    reorderOpen = false;
                    statSettingsStat = null;
                    thresholdPanelOpen = false;
                    menuKind = MenuKind.LIST_CONTEXT;
                });
        if (clickedCog != null) {
            statSettingsStat = clickedCog;
        }
    }

    private void handleStatSettingsPanelClick(int mx, int my, MineTunerConfig.StatListConfig lc) {
        if (statSettingsStat == null) return;
        StatSettingsPanel.ClickResult result =
                StatSettingsPanel.handleClick(mx, my, menuX, menuY, width, height, lc, statSettingsStat);
        switch (result) {
            case OPEN_THRESHOLDS -> thresholdPanelOpen = true;
            case BACK -> statSettingsStat = null; // back to reorder panel.
            case HANDLED, NONE -> { /** no state transition. */}
        }
    }

    private void handleThresholdPanelClick(int mx, int my, MineTunerConfig.StatListConfig lc) {
        if (statSettingsStat == null) return;
        MineTunerConfig.ThresholdSettings ts = lc.getThreshold(statSettingsStat);
        if (ts == null) {
            thresholdPanelOpen = false;
            return;
        }

        boolean back = ThresholdPanel.handleClick(mx, my, menuX, menuY, width, height, font,
                statSettingsStat, ts, MineTunerConfig.getInstance());
        if (back) thresholdPanelOpen = false;
    }

    private void handleTemplateListPanelClick(int mx, int my, MineTunerConfig.StatListConfig lc) {
        TemplateListPanel.ClickResult result =
                TemplateListPanel.handleClick(mx, my, menuX, menuY, width, height, lc);
        switch (result.kind()) {
            case CLOSED -> templateListOpen = false;
            case EDIT_LINE -> {
                templateEditIndex = result.editIndex();
                templateEditBuffer = new StringBuilder(lc.templateLines.get(result.editIndex()));
                menuKind = MenuKind.TEMPLATE_EDIT;
            }
            case NONE -> { /** add/remove already applied inside handleClick. */}
        }
    }

    private void handleDeletePanelClick(int mx, int my, MineTunerConfig.StatListConfig lc) {
        int idx = DeletePanel.rowAt(mx, my, menuX, menuY, width, height);
        if (idx < 0) return;

        MineTunerConfig root = MineTunerConfig.getInstance();

        switch (idx) {
            case DeletePanel.LM_CANCELBUTTON -> {
                deletePanelOpen = false;
                menuKind = MenuKind.LIST_CONTEXT;
            }

            case DeletePanel.LM_DELETEBUTTON -> {
                root.removeList(lc.id);
                root.save();

                reorderOpen = false;
                statSettingsStat = null;
                thresholdPanelOpen = false;
                templateListOpen = false;
                appearanceOpen = false;
                colorScaleOpen = false;
                deletePanelOpen = false;
                menuKind = MenuKind.NONE;
            }
        }
    }

    private int[] applySnap(int bx, int by, int bw, int bh) {
        int cx = width / 2, cy = height / 2, snapThreshold = MineTunerConfig.getInstance().dragSnapThresholdPx;

        int snappedX = bx;
        dragSnapX = MineTunerConfig.SnapX.NONE;
        int bestDx = snapThreshold + 1;
        int d = Math.abs(bx - cx);
        if (d <= snapThreshold && d < bestDx) {
            bestDx = d;
            snappedX = cx;
            dragSnapX = MineTunerConfig.SnapX.LEFT_ON_CENTER;
        }
        d = Math.abs((bx + bw / 2) - cx);
        if (d <= snapThreshold && d < bestDx) {
            bestDx = d;
            snappedX = cx - bw / 2;
            dragSnapX = MineTunerConfig.SnapX.CENTER_ON_CENTER;
        }
        d = Math.abs((bx + bw) - cx);
        if (d <= snapThreshold && d < bestDx) {
            snappedX = cx - bw;
            dragSnapX = MineTunerConfig.SnapX.RIGHT_ON_CENTER;
        }

        int snappedY = by;
        dragSnapY = MineTunerConfig.SnapY.NONE;
        int bestDy = snapThreshold + 1;
        d = Math.abs(by - cy);
        if (d <= snapThreshold && d < bestDy) {
            bestDy = d;
            snappedY = cy;
            dragSnapY = MineTunerConfig.SnapY.TOP_ON_CENTER;
        }
        d = Math.abs((by + bh / 2) - cy);
        if (d <= snapThreshold && d < bestDy) {
            bestDy = d;
            snappedY = cy - bh / 2;
            dragSnapY = MineTunerConfig.SnapY.CENTER_ON_CENTER;
        }
        d = Math.abs((by + bh) - cy);
        if (d <= snapThreshold && d < bestDy) {
            snappedY = cy - bh;
            dragSnapY = MineTunerConfig.SnapY.BOTTOM_ON_CENTER;
        }

        return new int[]{snappedX, snappedY};
    }

    // --- Snap helpers ---

    /**
     * Picks the nearest corner for (bx, by) and stores the offset from it as a
     * normalized fraction of screen size (anchorFracX/Y), the same representation
     * {@link MineTunerConfig.StatListConfig#anchorFracX} uses everywhere else.
     */
    private void snapToNearestCorner(MineTunerConfig.StatListConfig lc,
                                     int bx, int by, int boxW, int boxH) {
        boolean nearRight = (bx + boxW / 2) > width / 2;
        boolean nearBottom = (by + boxH / 2) > height / 2;
        int pixelDx, pixelDy;
        if (!nearRight && !nearBottom) {
            lc.anchorCorner = MineTunerConfig.Corner.TOP_LEFT;
            pixelDx = bx;
            pixelDy = by;
        } else if (nearRight && !nearBottom) {
            lc.anchorCorner = MineTunerConfig.Corner.TOP_RIGHT;
            pixelDx = width - (bx + boxW);
            pixelDy = by;
        } else if (!nearRight) {
            lc.anchorCorner = MineTunerConfig.Corner.BOTTOM_LEFT;
            pixelDx = bx;
            pixelDy = height - (by + boxH);
        } else {
            lc.anchorCorner = MineTunerConfig.Corner.BOTTOM_RIGHT;
            pixelDx = width - (bx + boxW);
            pixelDy = height - (by + boxH);
        }
        // width/height are never 0 for an open Screen, so no guard needed.
        lc.anchorFracX = pixelDx / (double) width;
        lc.anchorFracY = pixelDy / (double) height;
    }

    private int[] getListBounds(MineTunerConfig.StatListConfig lc) {
        var font = this.font;
        LineCache cache = LineCache.getCachedLines(lc);
        int lineH = font.lineHeight + 1;
        float scale = lc.textScale <= 0f ? 1f : lc.textScale;
        int boxW, boxH;
        if (cache.rowKinds().isEmpty()) {
            // Mirrors ListPreviewRenderer.drawList's empty-placeholder sizing (no scale).
            String placeholder = I18n.get("gui.minetuner.no_stats");
            boxW = font.width(placeholder) + 4;
            boxH = lineH + 3;
        } else {
            // Mirrors ListPreviewRenderer.drawList's scaled sizing, which mirrors.
            boxW = Math.round(cache.boxW(font) * scale);
            boxH = Math.round(cache.boxH(font) * scale);
        }
        int[] pos = ListPositioner.getPosition(lc, width, height, boxW, boxH);
        return new int[]{pos[0], pos[1], boxW, boxH};
    }

    // --- Utility ---

    private MineTunerConfig.StatListConfig getListById(int id) {
        for (MineTunerConfig.StatListConfig lc : MineTunerConfig.getInstance().lists)
            if (lc.id == id) return lc;
        return null;
    }

    /**
     * The single panel currently visible in the editor's mutually-exclusive popup stack.
     */
    private PanelRoute activePanelRoute() {
        if (menuKind == MenuKind.LIST_CONTEXT) return PanelRoute.LIST_CONTEXT;
        if (menuKind == MenuKind.EMPTY_SPACE) return PanelRoute.EMPTY_SPACE;
        if (menuKind == MenuKind.RENAME) return PanelRoute.RENAME;
        if (menuKind == MenuKind.TEMPLATE_EDIT) return PanelRoute.TEMPLATE_EDIT;
        if (deletePanelOpen) return PanelRoute.DELETE;
        if (colorScaleOpen) return PanelRoute.COLOR_SCALE;
        if (appearanceOpen) return PanelRoute.APPEARANCE;
        if (reorderOpen && statSettingsStat != null && thresholdPanelOpen) return PanelRoute.THRESHOLDS;
        if (reorderOpen && statSettingsStat != null) return PanelRoute.STAT_SETTINGS;
        if (reorderOpen) return PanelRoute.REORDER;
        if (templateListOpen) return PanelRoute.TEMPLATE_LIST;
        return PanelRoute.NONE;
    }

    /**
     * Which top-level popup (if any) is open.
     */
    private enum MenuKind {NONE, LIST_CONTEXT, EMPTY_SPACE, RENAME, TEMPLATE_EDIT}

    private enum PanelRoute {
        NONE, LIST_CONTEXT, EMPTY_SPACE, RENAME, TEMPLATE_EDIT, COLOR_SCALE,
        APPEARANCE, THRESHOLDS, STAT_SETTINGS, REORDER, TEMPLATE_LIST, DELETE;

        int signature(MineTunerConfig.Stat stat) {
            return ordinal() * 97 + (stat == null ? 0 : stat.ordinal() + 1);
        }
    }
}
