package bottled.mtss.gui.panel;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.gui.render.PanelChrome;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import java.util.*;

import static bottled.mtss.gui.render.PanelChrome.*;

/** The stat toggle/reorder panel ("Edit Stats"). */
public final class ReorderPanel {

    private static final MtssConfig.StatCategory[] CATEGORY_ORDER = {
            MtssConfig.StatCategory.PERFORMANCE,
            MtssConfig.StatCategory.PLAYER,
            MtssConfig.StatCategory.WORLD,
            MtssConfig.StatCategory.POSITION,
    };

    /** Title row + the always-present search row, both above the scrollable
     *  category/stat list, always counted in row-count math regardless of scroll. */
    private static final int HEADER_ROWS = 2;
    /** Wider than the standard panel. */
    public static int PANEL_W = WIDE_PANEL_W;
    /** Max number of stat/header rows visible at once before paging kicks in. */
    public static int MAX_VISIBLE_ROWS = 16;

    private ReorderPanel() {
    }

    /** Re-reads {@link #PANEL_W} and {@link #MAX_VISIBLE_ROWS} from the given config. */
    public static void syncFromConfig(MtssConfig cfg) {
        PANEL_W = cfg.widePanelWidth;
        MAX_VISIBLE_ROWS = cfg.reorderPanelMaxVisibleRows;
    }

    // A "visible row" is either a category header or one stat beneath an
    // expanded category. Which categories are expanded is tracked in the UI
    // state's expanded set.

    /** Stats in {@code lc.statOrder} order, grouped by category, preserving each
     *  category's stats in their original relative order from statOrder. */
    private static Map<MtssConfig.StatCategory, List<MtssConfig.Stat>> groupByCategory(MtssConfig.StatListConfig lc) {
        Map<MtssConfig.StatCategory, List<MtssConfig.Stat>> byCat = new EnumMap<>(MtssConfig.StatCategory.class);
        for (MtssConfig.StatCategory cat : CATEGORY_ORDER) byCat.put(cat, new ArrayList<>());
        for (String name : lc.statOrder) {
            MtssConfig.Stat stat;
            try {
                stat = MtssConfig.Stat.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                continue;
            }
            byCat.get(MtssConfig.categoryOf(stat)).add(stat);
        }
        return byCat;
    }

    private static List<Row> buildRows(MtssConfig.StatListConfig lc, UiState ui) {
        Map<MtssConfig.StatCategory, List<MtssConfig.Stat>> byCat = groupByCategory(lc);
        boolean filtering = ui.hasSearch();
        String needle = filtering ? ui.searchText().toLowerCase(java.util.Locale.ROOT) : null;

        List<Row> rows = new ArrayList<>();
        for (MtssConfig.StatCategory cat : CATEGORY_ORDER) {
            List<MtssConfig.Stat> stats = byCat.get(cat);
            int enabledCount = 0;
            for (MtssConfig.Stat s : stats) if (lc.isEnabled(s)) enabledCount++;

            if (filtering) {
                // Filtered view: every category is shown expanded (regardless of the
                // UI state's expanded set) but only stats matching the search text are
                // listed, and only categories that have at least one match get a header —
                // an empty category header with nothing to expand into would
                // just be a dead end for the user typing a query.
                List<MtssConfig.Stat> matches = new ArrayList<>();
                for (MtssConfig.Stat s : stats) {
                    if (statMatches(s, needle)) matches.add(s);
                }
                if (matches.isEmpty()) continue;
                rows.add(new HeaderRow(cat, enabledCount, stats.size()));
                for (int i = 0; i < matches.size(); i++) {
                    // indexInCategory/categorySize stay relative to the *unfiltered*
                    // category so ▲/▼ reordering (which operates on the full
                    // statOrder) still makes sense even while the filter hides some
                    // of that category's other stats from view. A filtered-out
                    // neighbor being skipped in the visible list just means ▲/▼
                    // silently reorders past it, same as it already does past any
                    // other same-category stat.
                    int fullIdx = stats.indexOf(matches.get(i));
                    rows.add(new StatRow(matches.get(i), fullIdx, stats.size()));
                }
                continue;
            }

            rows.add(new HeaderRow(cat, enabledCount, stats.size()));
            if (ui.isExpanded(cat)) {
                for (int i = 0; i < stats.size(); i++) {
                    rows.add(new StatRow(stats.get(i), i, stats.size()));
                }
            }
        }
        return rows;
    }

    /** Case-insensitive substring match against a stat's localized display name. */
    private static boolean statMatches(MtssConfig.Stat stat, String lowercaseNeedle) {
        String displayName = I18n.get("stat.mtss." + stat.name().toLowerCase(java.util.Locale.ROOT));
        return displayName.toLowerCase(java.util.Locale.ROOT).contains(lowercaseNeedle);
    }

    /** Total row count including the "Close" footer. */
    private static int totalRowCount(MtssConfig.StatListConfig lc, UiState ui) {
        return buildRows(lc, ui).size() + 1; // + Close row.
    }

    public static int panelHeight(MtssConfig.StatListConfig lc, UiState ui) {
        int visibleRows = Math.min(MAX_VISIBLE_ROWS, totalRowCount(lc, ui));
        return PANEL_PAD * 2 + ROW_H * HEADER_ROWS + ROW_H * visibleRows;
    }

    public static void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                              int mx, int my, int menuX, int menuY, int screenW, int screenH,
                              MtssConfig.StatListConfig lc, UiState ui) {
        List<Row> rows = buildRows(lc, ui);
        int totalRows = rows.size() + 1; // + Close.
        int visibleRows = Math.min(MAX_VISIBLE_ROWS, totalRows);
        boolean paged = totalRows > MAX_VISIBLE_ROWS;

        int maxOffset = Math.max(0, totalRows - MAX_VISIBLE_ROWS);
        ui.scrollOffset = Math.max(0, Math.min(ui.scrollOffset, maxOffset));

        int panelH = PANEL_PAD * 2 + ROW_H * HEADER_ROWS + ROW_H * visibleRows;
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);

        PanelChrome.drawBackground(g, px, py, PANEL_W, panelH);

        // Title, with a paging hint so it's clear more rows exist off-screen.
        String title = "§e" + I18n.get("gui.mtss.reorder.title");
        if (paged)
            title += "  §7(" + (ui.scrollOffset + 1) + "-" + Math.min(ui.scrollOffset + visibleRows, totalRows) + "/" + totalRows + ")";
        g.text(font, title, px + PANEL_PAD, py + PANEL_PAD, 0xFFFFFFFF, false);

        int searchY = py + PANEL_PAD + ROW_H;
        renderSearchRow(g, font, mx, my, px, searchY, ui);

        int rowTop = searchY + ROW_H;

        for (int visIdx = 0; visIdx < visibleRows; visIdx++) {
            int logicalIdx = ui.scrollOffset + visIdx;
            int ry = rowTop + visIdx * ROW_H;

            if (logicalIdx == rows.size()) {
                // Close row.
                PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry, PANEL_W, ROW_H);
                g.text(font, "§7" + I18n.get("gui.mtss.reorder.close"), px + PANEL_PAD, ry + 2, 0xFFFFFFFF, false);
                continue;
            }

            Row row = rows.get(logicalIdx);
            if (row instanceof HeaderRow header) {
                renderHeaderRow(g, font, mx, my, px, ry, ui, header);
            } else if (row instanceof StatRow statRow) {
                renderStatRow(g, font, mx, my, px, ry, lc, statRow);
            }
        }
    }

    /** The search row. */
    private static void renderSearchRow(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                                        int mx, int my, int px, int ry, UiState ui) {
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry, PANEL_W, ROW_H);

        boolean hasText = ui.hasSearch();
        String icon = ui.isSearchFocused() ? "§e🔍" : "§7🔍";
        String text;
        if (hasText) {
            text = "§f" + ui.searchText() + (ui.isSearchFocused() ? "§7|" : "");
        } else if (ui.isSearchFocused()) {
            text = "§7|"; // empty field, focused.
        } else {
            text = "§8" + I18n.get("gui.mtss.reorder.search_hint");
        }
        g.text(font, icon + " " + truncate(font, text, PANEL_W - 16 - (hasText ? 14 : 0)),
                px + PANEL_PAD, ry + 2, 0xFFFFFFFF, false);

        if (hasText) {
            boolean clearHovered = PanelChrome.isHoveringRow(mx, my, px + PANEL_W - 14, ry, 12, ROW_H);
            g.text(font, clearHovered ? "§c✕" : "§7✕", px + PANEL_W - 14, ry + 2, 0xFFFFFFFF, false);
        }
    }

    private static void renderHeaderRow(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                                        int mx, int my, int px, int ry, UiState ui, HeaderRow header) {
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry, PANEL_W, ROW_H);
        boolean expanded = ui.isExpanded(header.category());
        String arrow = expanded ? "§f▾" : "§f▸";
        String catName = I18n.get("gui.mtss.category." + header.category().name().toLowerCase());
        String count = "§7(" + header.enabledCount() + "/" + header.totalCount() + ")";
        g.text(font, arrow + " §e" + catName, px + PANEL_PAD, ry + 2, 0xFFFFFFFF, false);
        g.text(font, count, px + PANEL_W - PANEL_PAD - font.width(count), ry + 2, 0xFFFFFFFF, false);
    }

    private static void renderStatRow(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                                      int mx, int my, int px, int ry,
                                      MtssConfig.StatListConfig lc, StatRow statRow) {
        MtssConfig.Stat stat = statRow.stat();
        boolean enabled = lc.isEnabled(stat);

        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry, PANEL_W, ROW_H);

        String statName = I18n.get("stat.mtss." + stat.name().toLowerCase());
        String label = (enabled ? "§a✔ " : "§c✘ ") + statName;
        // Indented under its category header, and narrower than the full
        // width to leave room for the ⚙/▲/▼ cluster on the right without
        // needing per-stat-name truncation logic beyond the generic truncate()
        // helper below.
        g.text(font, truncate(font, label, PANEL_W - 16 - 36), px + PANEL_PAD + 8, ry + 2, 0xFFFFFFFF, false);

        boolean cogHovered = PanelChrome.isHoveringRow(mx, my, px + PANEL_W - 34, ry, 12, ROW_H);
        g.text(font, cogHovered ? "§e⚙" : "§7⚙", px + PANEL_W - 34, ry + 2, 0xFFFFFFFF, false);
        if (statRow.indexInCategory() > 0)
            g.text(font, "§7▲", px + PANEL_W - 22, ry + 2, 0xFFFFFFFF, false);
        if (statRow.indexInCategory() < statRow.categorySize() - 1)
            g.text(font, "§7▼", px + PANEL_W - 12, ry + 2, 0xFFFFFFFF, false);
    }

    /** Truncates with an ellipsis so a long stat name never collides with the ⚙/▲/▼
     *  cluster; picks the longest prefix (via binary search on rendered width) that
     *  still fits maxWidth once the ellipsis is appended. */
    private static String truncate(net.minecraft.client.gui.Font font, String s, int maxWidth) {
        if (font.width(s) <= maxWidth) return s;
        String ellipsis = "..";
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (font.width(s.substring(0, mid) + ellipsis) <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, lo) + ellipsis;
    }

    public static boolean isInside(int mx, int my, int menuX, int menuY, int screenW, int screenH,
                                   MtssConfig.StatListConfig lc, UiState ui) {
        int panelH = panelHeight(lc, ui);
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW), py = PanelChrome.clampY(menuY, panelH, screenH);
        return PanelChrome.isInsidePanel(mx, my, px, py, PANEL_W, panelH);
    }

    /** Handles a click at (mx, my). */
    public static MtssConfig.Stat handleClick(int mx, int my, int menuX, int menuY, int screenW, int screenH,
                                              MtssConfig.StatListConfig lc, UiState ui, Runnable onClose) {
        List<Row> rows = buildRows(lc, ui);
        int totalRows = rows.size() + 1;
        int visibleRows = Math.min(MAX_VISIBLE_ROWS, totalRows);
        int panelH = PANEL_PAD * 2 + ROW_H * HEADER_ROWS + ROW_H * visibleRows;
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);

        // Title-row paging: clicking the left half of the title row pages back,
        // the right half pages forward — no dedicated arrow glyphs needed since
        // the title row already shows the "(X-Y/N)" range as a paging hint.
        boolean paged = totalRows > MAX_VISIBLE_ROWS;
        if (paged && PanelChrome.isHoveringRow(mx, my, px, py + PANEL_PAD, PANEL_W, ROW_H)) {
            int maxOffset = Math.max(0, totalRows - MAX_VISIBLE_ROWS);
            if (mx < px + PANEL_W / 2) ui.scrollOffset = Math.max(0, ui.scrollOffset - MAX_VISIBLE_ROWS);
            else ui.scrollOffset = Math.min(maxOffset, ui.scrollOffset + MAX_VISIBLE_ROWS);
            return null;
        }

        int searchY = py + PANEL_PAD + ROW_H;
        if (PanelChrome.isHoveringRow(mx, my, px, searchY, PANEL_W, ROW_H)) {
            if (ui.hasSearch() && mx >= px + PANEL_W - 14) {
                ui.clearSearch(); // ✕.
            } else {
                ui.toggleSearchFocus();
            }
            return null;
        } else if (ui.isSearchFocused()) {
            // Clicked elsewhere in the panel while the field had focus. Same
            // "clicking away cancels focus/editing" convention RENAME/TEMPLATE_EDIT use
            // elsewhere in this GUI, except here it only drops keyboard focus rather
            // than closing the whole panel, since the filter itself might still be
            // wanted — the user may just want to click a result while the field is
            // no longer focused.
            ui.toggleSearchFocus();
        }

        int rowTop = searchY + ROW_H;

        for (int visIdx = 0; visIdx < visibleRows; visIdx++) {
            int logicalIdx = ui.scrollOffset + visIdx;
            int ry = rowTop + visIdx * ROW_H;
            if (my < ry || my >= ry + ROW_H) continue;

            if (logicalIdx == rows.size()) {
                onClose.run();
                return null;
            }

            Row row = rows.get(logicalIdx);
            if (row instanceof HeaderRow header) {
                ui.toggle(header.category());
                return null;
            } else if (row instanceof StatRow statRow) {
                return handleStatRowClick(mx, px, lc, statRow);
            }
        }
        return null;
    }

    private static MtssConfig.Stat handleStatRowClick(int mx, int px, MtssConfig.StatListConfig lc, StatRow statRow) {
        MtssConfig.Stat stat = statRow.stat();

        // ⚙ cog: opens this stat's settings panel.
        if (mx >= px + PANEL_W - 34 && mx < px + PANEL_W - 22) {
            return stat;
        }
        // ▲: move up. moveWithinCategory() only ever swaps with the nearest
        // earlier statOrder entry that shares its category, so reordering
        // never crosses a category boundary.
        if (mx >= px + PANEL_W - 22 && mx < px + PANEL_W - 12 && statRow.indexInCategory() > 0) {
            moveWithinCategory(lc, stat, -1);
            MtssConfig.getInstance().save();
            // ▼: move down, same category-boundary rule as ▲ above.
        } else if (mx >= px + PANEL_W - 12 && statRow.indexInCategory() < statRow.categorySize() - 1) {
            moveWithinCategory(lc, stat, +1);
            MtssConfig.getInstance().save();
            // Clicked the row itself (not ⚙/▲/▼): toggle enabled state.
        } else {
            lc.setEnabled(stat, !lc.isEnabled(stat));
            MtssConfig.getInstance().save();
        }
        return null;
    }

    /** Swaps {@code stat} in-place with the nearest other stat in the same category,
     *  {@code direction} steps away in statOrder (-1 = toward the front/up, +1 = toward
     *  the back/down). Stats from other categories in between are skipped over rather
     *  than swapped with, so a move can never cross a category boundary. */
    private static void moveWithinCategory(MtssConfig.StatListConfig lc, MtssConfig.Stat stat, int direction) {
        MtssConfig.StatCategory cat = MtssConfig.categoryOf(stat);
        int from = lc.statOrder.indexOf(stat.name());
        if (from < 0) return;

        int target = from + direction;
        while (target >= 0 && target < lc.statOrder.size()) {
            MtssConfig.Stat candidate;
            try {
                candidate = MtssConfig.Stat.valueOf(lc.statOrder.get(target));
            } catch (IllegalArgumentException e) {
                target += direction;
                continue;
            }
            if (MtssConfig.categoryOf(candidate) == cat) {
                java.util.Collections.swap(lc.statOrder, from, target);
                return;
            }
            target += direction;
        }
    }

    private sealed interface Row permits HeaderRow, StatRow {
    }

    /** Per-list UI state for this panel. */
    public static final class UiState {
        private final Set<MtssConfig.StatCategory> expanded = EnumSet.noneOf(MtssConfig.StatCategory.class);
        /** Live filter text, entered by the search field toggled from the title row. */
        private final StringBuilder search = new StringBuilder();
        private int scrollOffset = 0;
        /** Whether the search text field currently has keyboard focus. */
        private boolean searchFocused = false;

        /** Collapses every category, scrolls to the top, and clears any search. */
        public void reset() {
            expanded.clear();
            scrollOffset = 0;
            search.setLength(0);
            searchFocused = false;
        }

        boolean isExpanded(MtssConfig.StatCategory cat) {
            return expanded.contains(cat);
        }

        void toggle(MtssConfig.StatCategory cat) {
            if (!expanded.remove(cat)) expanded.add(cat);
            scrollOffset = 0; // expanding/collapsing shifts everything below it.
        }

        public boolean isSearchFocused() {
            return searchFocused;
        }

        public String searchText() {
            return search.toString();
        }

        public boolean hasSearch() {
            return !search.isEmpty();
        }

        /** Toggles the search field's focus. */
        public void toggleSearchFocus() {
            searchFocused = !searchFocused;
            scrollOffset = 0;
        }

        public void appendSearch(char c) {
            if (search.length() < 32) {
                search.append(c);
                scrollOffset = 0;
            }
        }

        public void backspaceSearch() {
            if (!search.isEmpty()) {
                search.deleteCharAt(search.length() - 1);
                scrollOffset = 0;
            }
        }

        public void clearSearch() {
            search.setLength(0);
            scrollOffset = 0;
        }
    }

    private record HeaderRow(MtssConfig.StatCategory category, int enabledCount, int totalCount) implements Row {
    }

    private record StatRow(MtssConfig.Stat stat, int indexInCategory, int categorySize) implements Row {
    }
}
