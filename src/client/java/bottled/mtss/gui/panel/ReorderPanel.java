package bottled.mtss.gui.panel;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.gui.render.PanelChrome;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static bottled.mtss.gui.render.PanelChrome.PANEL_PAD;
import static bottled.mtss.gui.render.PanelChrome.ROW_H;
import static bottled.mtss.gui.render.PanelChrome.WIDE_PANEL_W;

/**
 * The stat toggle/reorder panel ("Edit Stats"). With 40+ stats spanning
 * several very different domains (performance, player vitals, world state,
 * position), a flat one-row-per-stat list — the original design — would run
 * to 500+ pixels tall and force the user to hunt through a wall of
 * unrelated rows for the handful of stats they actually want.
 * <p>
 * This version groups stats into collapsible {@link MtssConfig.StatCategory}
 * sections (via {@link MtssConfig#categoryOf}), shows only a header with an
 * enabled-count summary for a collapsed category, and caps the panel's
 * total visible height with a scroll window (paged via the title row —
 * click its left/right half — since the raw mouse-wheel event's exact
 * signature isn't something this codebase's input layer exposes yet; see
 * {@code MtssGuiScreen}'s click-driven input pattern) so the panel never
 * grows past a fixed on-screen budget no matter how many stats exist or how
 * many categories are expanded. Reordering (▲/▼ next to a stat) moves the
 * stat within its own category's slice of the shared {@code statOrder}
 * list — categories are always presented in a fixed order, so cross-
 * category ordering isn't something the user needs to manage directly.
 * <p>
 * All panel layout state (which categories are expanded, current scroll
 * offset) lives in {@link UiState}, owned by the caller ({@code
 * MtssGuiScreen}) and passed in — this class stays a pure
 * render/hit-test/mutate helper with no static or instance state of its
 * own, matching every other panel in this package.
 */
public final class ReorderPanel {

    private ReorderPanel() {}

    // ── Sizing ───────────────────────────────────────────────────────────────

    /** Wider than the standard panel — see {@link PanelChrome#WIDE_PANEL_W}'s doc for why. */
    public static final int PANEL_W = WIDE_PANEL_W;

    /** Max number of stat/header rows visible at once before paging kicks in. Keeps the panel's height bounded regardless of stat count or how many categories are expanded. */
    private static final int MAX_VISIBLE_ROWS = 14;

    private static final MtssConfig.StatCategory[] CATEGORY_ORDER = {
            MtssConfig.StatCategory.PERFORMANCE,
            MtssConfig.StatCategory.PLAYER,
            MtssConfig.StatCategory.WORLD,
            MtssConfig.StatCategory.POSITION,
    };

    /**
     * Per-list UI state for this panel: which categories are expanded and
     * the current scroll offset. Reset (via {@link #reset()}) whenever the
     * panel is opened for a (possibly different) list, so state from a
     * previously-viewed list never bleeds into another.
     */
    public static final class UiState {
        private final Set<MtssConfig.StatCategory> expanded = EnumSet.noneOf(MtssConfig.StatCategory.class);
        private int scrollOffset = 0;

        /** Collapses every category and scrolls to the top — call when the panel opens (or opens for a different list). */
        public void reset() {
            expanded.clear();
            scrollOffset = 0;
        }

        boolean isExpanded(MtssConfig.StatCategory cat) { return expanded.contains(cat); }

        void toggle(MtssConfig.StatCategory cat) {
            if (!expanded.remove(cat)) expanded.add(cat);
            scrollOffset = 0; // expanding/collapsing shifts everything below it — avoid a disorienting jump mid-list
        }
    }

    // ── Row model ────────────────────────────────────────────────────────────
    // A "visible row" is either a category header or one stat beneath an
    // expanded category. Built fresh each frame from lc.statOrder + the UI
    // state's expanded set — cheap enough (≤ 43 stats) not to need caching.

    private sealed interface Row permits HeaderRow, StatRow {}
    private record HeaderRow(MtssConfig.StatCategory category, int enabledCount, int totalCount) implements Row {}
    private record StatRow(MtssConfig.Stat stat, int indexInCategory, int categorySize) implements Row {}

    /** Stats in {@code lc.statOrder} order, grouped by category, preserving each category's relative stat order. */
    private static Map<MtssConfig.StatCategory, List<MtssConfig.Stat>> groupByCategory(MtssConfig.StatListConfig lc) {
        Map<MtssConfig.StatCategory, List<MtssConfig.Stat>> byCat = new EnumMap<>(MtssConfig.StatCategory.class);
        for (MtssConfig.StatCategory cat : CATEGORY_ORDER) byCat.put(cat, new ArrayList<>());
        for (String name : lc.statOrder) {
            MtssConfig.Stat stat;
            try { stat = MtssConfig.Stat.valueOf(name); } catch (IllegalArgumentException ignored) { continue; }
            byCat.get(MtssConfig.categoryOf(stat)).add(stat);
        }
        return byCat;
    }

    private static List<Row> buildRows(MtssConfig.StatListConfig lc, UiState ui) {
        Map<MtssConfig.StatCategory, List<MtssConfig.Stat>> byCat = groupByCategory(lc);
        List<Row> rows = new ArrayList<>();
        for (MtssConfig.StatCategory cat : CATEGORY_ORDER) {
            List<MtssConfig.Stat> stats = byCat.get(cat);
            int enabledCount = 0;
            for (MtssConfig.Stat s : stats) if (lc.isEnabled(s)) enabledCount++;
            rows.add(new HeaderRow(cat, enabledCount, stats.size()));
            if (ui.isExpanded(cat)) {
                for (int i = 0; i < stats.size(); i++) {
                    rows.add(new StatRow(stats.get(i), i, stats.size()));
                }
            }
        }
        return rows;
    }

    // ── Sizing ───────────────────────────────────────────────────────────────

    /** Total row count including the "Close" footer — used to decide whether paging controls are needed. */
    private static int totalRowCount(MtssConfig.StatListConfig lc, UiState ui) {
        return buildRows(lc, ui).size() + 1; // + Close row
    }

    public static int panelHeight(MtssConfig.StatListConfig lc, UiState ui) {
        int visibleRows = Math.min(MAX_VISIBLE_ROWS, totalRowCount(lc, ui));
        return PANEL_PAD * 2 + ROW_H /* title */ + ROW_H * visibleRows;
    }

    // ── Render ───────────────────────────────────────────────────────────────

    public static void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                              int mx, int my, int menuX, int menuY, int screenW, int screenH,
                              MtssConfig.StatListConfig lc, UiState ui) {
        List<Row> rows = buildRows(lc, ui);
        int totalRows = rows.size() + 1; // + Close
        int visibleRows = Math.min(MAX_VISIBLE_ROWS, totalRows);
        boolean paged = totalRows > MAX_VISIBLE_ROWS;

        int maxOffset = Math.max(0, totalRows - MAX_VISIBLE_ROWS);
        ui.scrollOffset = Math.max(0, Math.min(ui.scrollOffset, maxOffset));

        int panelH = PANEL_PAD * 2 + ROW_H + ROW_H * visibleRows;
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);

        PanelChrome.drawBackground(g, px, py, PANEL_W, panelH);

        // Title, with a paging hint so it's clear more rows exist off-screen.
        String title = "§e" + I18n.get("gui.mtss.reorder.title");
        if (paged) title += "  §7(" + (ui.scrollOffset + 1) + "-" + Math.min(ui.scrollOffset + visibleRows, totalRows) + "/" + totalRows + ")";
        g.text(font, title, px + PANEL_PAD, py + PANEL_PAD, 0xFFFFFFFF, false);

        int rowTop = py + PANEL_PAD + ROW_H;

        for (int visIdx = 0; visIdx < visibleRows; visIdx++) {
            int logicalIdx = ui.scrollOffset + visIdx;
            int ry = rowTop + visIdx * ROW_H;

            if (logicalIdx == rows.size()) {
                // Close row — always the last logical row.
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
        // needing per-stat-name truncation logic.
        g.text(font, truncate(font, label, PANEL_W - 16 - 36), px + PANEL_PAD + 8, ry + 2, 0xFFFFFFFF, false);

        boolean cogHovered = PanelChrome.isHoveringRow(mx, my, px + PANEL_W - 34, ry, 12, ROW_H);
        g.text(font, cogHovered ? "§e⚙" : "§7⚙", px + PANEL_W - 34, ry + 2, 0xFFFFFFFF, false);
        if (statRow.indexInCategory() > 0)
            g.text(font, "§7▲", px + PANEL_W - 22, ry + 2, 0xFFFFFFFF, false);
        if (statRow.indexInCategory() < statRow.categorySize() - 1)
            g.text(font, "§7▼", px + PANEL_W - 12, ry + 2, 0xFFFFFFFF, false);
    }

    /** Truncates with an ellipsis so a long stat name never collides with the ⚙/▲/▼ cluster. */
    private static String truncate(net.minecraft.client.gui.Font font, String s, int maxWidth) {
        if (font.width(s) <= maxWidth) return s;
        String ellipsis = "..";
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (font.width(s.substring(0, mid) + ellipsis) <= maxWidth) lo = mid; else hi = mid - 1;
        }
        return s.substring(0, lo) + ellipsis;
    }

    // ── Hit-testing / bounds ────────────────────────────────────────────────

    public static boolean isInside(int mx, int my, int menuX, int menuY, int screenW, int screenH,
                                   MtssConfig.StatListConfig lc, UiState ui) {
        int panelH = panelHeight(lc, ui);
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW), py = PanelChrome.clampY(menuY, panelH, screenH);
        return PanelChrome.isInsidePanel(mx, my, px, py, PANEL_W, panelH);
    }

    // ── Click handling ───────────────────────────────────────────────────────

    /**
     * Handles a click at (mx, my). Returns the stat whose ⚙ was clicked (caller opens
     * {@link StatSettingsPanel} for it), or null if the click affected this panel directly
     * (expand/collapse/reorder/toggle/scroll/close) or missed entirely — callers distinguish
     * "closed" via the list's own reorderOpen flag, since a plain close click has no other
     * return signal here.
     */
    public static MtssConfig.Stat handleClick(int mx, int my, int menuX, int menuY, int screenW, int screenH,
                                               MtssConfig.StatListConfig lc, UiState ui, Runnable onClose) {
        List<Row> rows = buildRows(lc, ui);
        int totalRows = rows.size() + 1;
        int visibleRows = Math.min(MAX_VISIBLE_ROWS, totalRows);
        int panelH = PANEL_PAD * 2 + ROW_H + ROW_H * visibleRows;
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);
        int rowTop = py + PANEL_PAD + ROW_H;

        // Title-row paging: click the left half to scroll up a page, right half to scroll down —
        // no dedicated arrow glyphs needed since the title row already shows the "(a-b/n)" range as a hint.
        boolean paged = totalRows > MAX_VISIBLE_ROWS;
        if (paged && PanelChrome.isHoveringRow(mx, my, px, py + PANEL_PAD, PANEL_W, ROW_H)) {
            int maxOffset = Math.max(0, totalRows - MAX_VISIBLE_ROWS);
            if (mx < px + PANEL_W / 2) ui.scrollOffset = Math.max(0, ui.scrollOffset - MAX_VISIBLE_ROWS);
            else                       ui.scrollOffset = Math.min(maxOffset, ui.scrollOffset + MAX_VISIBLE_ROWS);
            return null;
        }

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

        // ⚙ cog — open per-stat settings
        if (mx >= px + PANEL_W - 34 && mx < px + PANEL_W - 22) {
            return stat;
        }
        // ▲ — move up within this stat's own category (swap with the nearest
        // earlier statOrder entry that shares its category, so reordering
        // never crosses a category boundary).
        if (mx >= px + PANEL_W - 22 && mx < px + PANEL_W - 12 && statRow.indexInCategory() > 0) {
            moveWithinCategory(lc, stat, -1);
            MtssConfig.getInstance().save();
        // ▼
        } else if (mx >= px + PANEL_W - 12 && statRow.indexInCategory() < statRow.categorySize() - 1) {
            moveWithinCategory(lc, stat, +1);
            MtssConfig.getInstance().save();
        // toggle enable
        } else {
            lc.setEnabled(stat, !lc.isEnabled(stat));
            MtssConfig.getInstance().save();
        }
        return null;
    }

    /**
     * Swaps {@code stat} in-place with the nearest other stat in the same
     * category, {@code direction} steps away in {@code statOrder} (-1 =
     * toward the front, +1 = toward the back) — skipping over any stats
     * from other categories in between, so a stat's ▲/▼ only ever reorders
     * it relative to its own category, never spilling into a neighboring
     * one. A plain two-element swap (rather than remove+re-insert) sidesteps
     * any index-shifting arithmetic entirely — both slots keep their
     * position in the list, they just trade contents.
     */
    private static void moveWithinCategory(MtssConfig.StatListConfig lc, MtssConfig.Stat stat, int direction) {
        MtssConfig.StatCategory cat = MtssConfig.categoryOf(stat);
        int from = lc.statOrder.indexOf(stat.name());
        if (from < 0) return;

        int target = from + direction;
        while (target >= 0 && target < lc.statOrder.size()) {
            MtssConfig.Stat candidate;
            try { candidate = MtssConfig.Stat.valueOf(lc.statOrder.get(target)); } catch (IllegalArgumentException e) { target += direction; continue; }
            if (MtssConfig.categoryOf(candidate) == cat) {
                java.util.Collections.swap(lc.statOrder, from, target);
                return;
            }
            target += direction;
        }
    }
}
