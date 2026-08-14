package bottled.minetuner.gui.panel;

import bottled.minetuner.config.ListTheme;
import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.gui.render.PanelChrome;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static bottled.minetuner.gui.render.PanelChrome.*;

/**
 * Lets the user browse every available theme (built-in and user-saved), apply one
 * to the currently-open list (click a theme row), save the list's current
 * appearance as a new theme ("+ Save as new theme"), or delete a user-created
 * theme (✕ on its row). Reachable from a list's Appearance panel via the new
 * "Theme »" row, following the same "sub-panel reachable from Appearance"
 * pattern Color/Scale already uses.
 *
 * <p>"Update an existing theme" isn't a separate control here — typing an
 * existing user-created theme's name into the save-as-new prompt re-captures
 * it in place instead of erroring (see {@link MineTunerConfig#saveTheme}),
 * which covers the same need without a second row/interaction in this panel.
 */
public final class ThemePanel {

    private ThemePanel() {
    }

    /** Row layout: one row per theme, then "+ Save as new theme", then Back. */
    public static int rowCount(MineTunerConfig root) {
        return root.themes.size() + 2;
    }

    public static int panelHeight(MineTunerConfig root) {
        return PANEL_PAD * 2 + ROW_H * rowCount(root);
    }

    public static void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                              int mx, int my, int menuX, int menuY, int screenW, int screenH,
                              MineTunerConfig root, MineTunerConfig.StatListConfig lc) {
        int panelH = panelHeight(root);
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);

        PanelChrome.drawBackground(g, px, py, PANEL_W, panelH);

        List<String> names = orderedThemeNames(root);
        int rowTop = py + PANEL_PAD;
        for (int i = 0; i < names.size(); i++) {
            int ry = rowTop + i * ROW_H;
            renderThemeRow(g, font, mx, my, px, ry, root, names.get(i));
        }

        int saveY = rowTop + names.size() * ROW_H;
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, saveY, PANEL_W, ROW_H);
        g.text(font, "§a" + I18n.get("gui.minetuner.theme.save_new"),
                px + PANEL_PAD, saveY + 2, 0xFFFFFFFF, false);

        int backY = saveY + ROW_H;
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, backY, PANEL_W, ROW_H);
        g.text(font, "§7" + I18n.get("gui.minetuner.stat_settings.back"),
                px + PANEL_PAD, backY + 2, 0xFFFFFFFF, false);
    }

    private static void renderThemeRow(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                                       int mx, int my, int px, int ry,
                                       MineTunerConfig root, String name) {
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry, PANEL_W, ROW_H);
        ListTheme theme = root.themes.get(name);
        boolean builtin = theme != null && theme.builtin;

        // Built-ins are clearly marked and never show a delete control, matching
        // this codebase's preference for preventing invalid actions in the UI
        // rather than allowing them and erroring.
        String label = builtin
                ? "§f" + name + " §7(" + I18n.get("gui.minetuner.theme.builtin_suffix") + ")"
                : "§f" + name;
        int labelMaxWidth = PANEL_W - PANEL_PAD - (builtin ? 4 : 14);
        g.text(font, truncateForRow(font, label, labelMaxWidth), px + PANEL_PAD, ry + 2, 0xFFFFFFFF, false);

        if (!builtin) {
            boolean removeHovered = PanelChrome.isHoveringRow(mx, my, px + PANEL_W - 12, ry, 10, ROW_H);
            g.text(font, removeHovered ? "§c✕" : "§7✕", px + PANEL_W - 12, ry + 2, 0xFFFFFFFF, false);
        }
    }

    private static String truncateForRow(net.minecraft.client.gui.Font font, String s, int maxWidth) {
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

    public static boolean isInside(int mx, int my, int menuX, int menuY, int screenW, int screenH,
                                   MineTunerConfig root) {
        int panelH = panelHeight(root);
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW), py = PanelChrome.clampY(menuY, panelH, screenH);
        return PanelChrome.isInsidePanel(mx, my, px, py, PANEL_W, panelH);
    }

    /** Handles a click at (mx, my) against the theme list. */
    public static ClickResult handleClick(int mx, int my, int menuX, int menuY, int screenW, int screenH,
                                          MineTunerConfig root, MineTunerConfig.StatListConfig lc) {
        int panelH = panelHeight(root);
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);

        List<String> names = orderedThemeNames(root);
        int rowTop = py + PANEL_PAD;
        int saveY = rowTop + names.size() * ROW_H;
        int backY = saveY + ROW_H;

        if (PanelChrome.isHoveringRow(mx, my, px, backY, PANEL_W, ROW_H)) return ClickResult.BACK_RESULT;

        if (PanelChrome.isHoveringRow(mx, my, px, saveY, PANEL_W, ROW_H)) {
            return ClickResult.PROMPT_SAVE_RESULT;
        }

        for (int i = 0; i < names.size(); i++) {
            int ry = rowTop + i * ROW_H;
            if (my < ry || my >= ry + ROW_H) continue;
            String name = names.get(i);
            ListTheme theme = root.themes.get(name);
            boolean builtin = theme != null && theme.builtin;
            if (!builtin && mx >= px + PANEL_W - 12) {
                root.deleteTheme(name);
                root.save();
                return ClickResult.NONE_RESULT;
            }
            // Applying is a one-time field copy — no per-frame cost regardless of
            // how many appearance fields the theme covers.
            if (theme != null) theme.applyTo(lc);
            root.save();
            return ClickResult.NONE_RESULT;
        }
        return ClickResult.NONE_RESULT;
    }

    /** Built-ins first (in their fixed backfill order), then user-created themes
     *  in insertion order — {@code themes} is already a LinkedHashMap, so this is
     *  just a stable partition, not a re-sort. */
    private static List<String> orderedThemeNames(MineTunerConfig root) {
        List<String> builtins = new ArrayList<>();
        List<String> userMade = new ArrayList<>();
        for (Map.Entry<String, ListTheme> e : root.themes.entrySet()) {
            if (e.getValue() != null && e.getValue().builtin) builtins.add(e.getKey());
            else userMade.add(e.getKey());
        }
        builtins.addAll(userMade);
        return builtins;
    }

    public enum Kind {NONE, BACK, PROMPT_SAVE}

    public record ClickResult(Kind kind) {
        private static final ClickResult NONE_RESULT = new ClickResult(Kind.NONE);
        private static final ClickResult BACK_RESULT = new ClickResult(Kind.BACK);
        private static final ClickResult PROMPT_SAVE_RESULT = new ClickResult(Kind.PROMPT_SAVE);
    }
}
