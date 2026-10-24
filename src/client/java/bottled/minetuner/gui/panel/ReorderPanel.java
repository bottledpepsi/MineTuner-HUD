package bottled.minetuner.gui.panel;

import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.core.EditBuffer;
import bottled.minetuner.core.LangKeys;
import bottled.minetuner.gui.render.PanelChrome;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import java.util.*;

import static bottled.minetuner.gui.render.PanelChrome.*;

/** The stat toggle/reorder panel ("Edit Stats"). */
public final class ReorderPanel {

    private static final MineTunerConfig.StatCategory[] CATEGORY_ORDER = {
            MineTunerConfig.StatCategory.PERFORMANCE,
            MineTunerConfig.StatCategory.PLAYER,
            MineTunerConfig.StatCategory.WORLD,
            MineTunerConfig.StatCategory.POSITION,
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
    public static void syncFromConfig(MineTunerConfig cfg) {
        PANEL_W = cfg.widePanelWidth;
        MAX_VISIBLE_ROWS = cfg.reorderPanelMaxVisibleRows;
    }

    // A "visible row" is either a category header or one stat beneath an
    // expanded category. Which categories are expanded is tracked in the UI
    // state's expanded set.

    /** Stats in {@code lc.statOrder} order, grouped by category, preserving each
     *  category's stats in their original relative order from statOrder. */
    private static Map<MineTunerConfig.StatCategory, List<MineTunerConfig.Stat>> groupByCategory(MineTunerConfig.StatListConfig lc) {
        Map<MineTunerConfig.StatCategory, List<MineTunerConfig.Stat>> byCat = new EnumMap<>(MineTunerConfig.StatCategory.class);
        for (MineTunerConfig.StatCategory cat : CATEGORY_ORDER) byCat.put(cat, new ArrayList<>());
        for (String name : lc.statOrder) {
            MineTunerConfig.Stat stat = MineTunerConfig.Stat.byName(name);
            if (stat == null) continue;
            byCat.get(MineTunerConfig.categoryOf(stat)).add(stat);
        }
        return byCat;
    }

    private static List<Row> buildRows(MineTunerConfig.StatListConfig lc, UiState ui) {
        Map<MineTunerConfig.StatCategory, List<MineTunerConfig.Stat>> byCat = groupByCategory(lc);
        boolean filtering = ui.hasSearch();
        String needle = filtering ? ui.searchText().toLowerCase(java.util.Locale.ROOT) : null;

        List<Row> rows = new ArrayList<>();
        for (MineTunerConfig.StatCategory cat : CATEGORY_ORDER) {
            List<MineTunerConfig.Stat> stats = byCat.get(cat);
            int enabledCount = 0;
            for (MineTunerConfig.Stat s : stats) if (lc.isEnabled(s)) enabledCount++;

            if (filtering) {
                // Filtered view: every category is shown expanded (regardless of the
                // UI state's expanded set) but only stats matching the search text are
                // listed, and only categories that have at least one match get a header —
                // an empty category header with nothing to expand into would
                // just be a dead end for the user typing a query.
                List<MineTunerConfig.Stat> matches = new ArrayList<>();
                for (MineTunerConfig.Stat s : stats) {
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
                    rows.add(new StatRow(matches.get(i), cat, fullIdx, stats.size()));
                }
                continue;
            }

            rows.add(new HeaderRow(cat, enabledCount, stats.size()));
            // On collapse, retain child rows until their reverse rollout has
            // finished instead of removing them in the first animation frame.
            if (ui.isExpanded(cat) || ui.isAnimating(cat)) {
                for (int i = 0; i < stats.size(); i++) {
                    rows.add(new StatRow(stats.get(i), cat, i, stats.size()));
                }
            }
        }
        return rows;
    }

    /** Case-insensitive substring match against a stat's localized display name. */
    private static boolean statMatches(MineTunerConfig.Stat stat, String lowercaseNeedle) {
        String displayName = I18n.get("stat.minetuner." + stat.name().toLowerCase(java.util.Locale.ROOT));
        return displayName.toLowerCase(java.util.Locale.ROOT).contains(lowercaseNeedle);
    }

    /** Total row count including the "Close" footer. */
    private static int totalRowCount(MineTunerConfig.StatListConfig lc, UiState ui) {
        return buildRows(lc, ui).size() + 1; // + Close row.
    }

    public static int panelHeight(MineTunerConfig.StatListConfig lc, UiState ui) {
        return layout(lc, ui).panelH;
    }

    /** Everything render/isInside/handleClick need, computed once from the same
     *  inputs so hit-testing can never disagree with what the eye sees — the
     *  previous handleClick recomputed its own full-height, unsmoothed, uncompressed
     *  layout and clicked the wrong rows during smooth scrolling and rollout
     *  animations. */
    private record Layout(List<Row> rows, int totalRows, int fullVisibleRows, int visibleRows,
                          boolean paged, int rowsToRender, int panelH, int fullPanelH) {
    }

    private static Layout layout(MineTunerConfig.StatListConfig lc, UiState ui) {
        List<Row> rows = buildRows(lc, ui);
        int totalRows = rows.size() + 1; // + Close.
        int fullVisibleRows = Math.min(MAX_VISIBLE_ROWS, totalRows);
        int visibleRows = animatedVisibleRows(rows, ui, fullVisibleRows);
        boolean paged = totalRows > MAX_VISIBLE_ROWS;
        // With no paging, render the whole logical list while the panel grows:
        // category headers below the unfolding section then move smoothly too.
        int rowsToRender = paged ? visibleRows : totalRows;
        int panelH = PANEL_PAD * 2 + ROW_H * HEADER_ROWS + ROW_H * visibleRows - 1;
        int fullPanelH = PANEL_PAD * 2 + ROW_H * HEADER_ROWS + ROW_H * fullVisibleRows;
        return new Layout(rows, totalRows, fullVisibleRows, visibleRows, paged, rowsToRender, panelH, fullPanelH);
    }

    /** Precomputed per-row rollout-compression data for one layout pass — replaces
     *  the old compressedPixelsBefore() rescan per row, which was O(rows²) per frame. */
    private static final class Compression {
        /** Compressed pixels above row i from ALL earlier stat rows. */
        private final float[] prefixBefore;
        /** For a StatRow at index i: how many same-category stat rows sit above it. */
        private final int[] ownCategoryRowsBefore;

        private Compression(float[] prefixBefore, int[] ownCategoryRowsBefore) {
            this.prefixBefore = prefixBefore;
            this.ownCategoryRowsBefore = ownCategoryRowsBefore;
        }

        private static Compression compute(List<Row> rows, UiState ui) {
            float[] prefix = new float[rows.size() + 1];
            int[] ownBefore = new int[rows.size()];
            Map<MineTunerConfig.StatCategory, Integer> counts = new EnumMap<>(MineTunerConfig.StatCategory.class);
            float running = 0f;
            for (int i = 0; i < rows.size(); i++) {
                prefix[i] = running;
                if (rows.get(i) instanceof StatRow statRow) {
                    ownBefore[i] = counts.getOrDefault(statRow.category(), 0);
                    counts.merge(statRow.category(), 1, Integer::sum);
                    running += ROW_H * (1f - ui.rolloutProgress(statRow.category()));
                }
            }
            prefix[rows.size()] = running;
            return new Compression(prefix, ownBefore);
        }

        /** Space hidden by earlier, partially opened category children (all of them). */
        private float before(int exclusiveRow) {
            return prefixBefore[exclusiveRow];
        }

        /** Same, but excluding the given stat row's own category (the row's own
         *  group compresses around it rather than shifting it). */
        private float beforeExcludingOwnCategory(int rowIdx, float ownCategoryCompression) {
            return prefixBefore[rowIdx] - ownCategoryRowsBefore[rowIdx] * ownCategoryCompression;
        }
    }

    public static void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                              int mx, int my, int menuX, int menuY, int screenW, int screenH,
                              MineTunerConfig.StatListConfig lc, UiState ui) {
        Layout layout = layout(lc, ui);
        List<Row> rows = layout.rows;
        int totalRows = layout.totalRows;
        int rowsToRender = layout.rowsToRender;

        int maxOffset = Math.max(0, totalRows - MAX_VISIBLE_ROWS);
        ui.clampAndAnimateScroll(maxOffset);
        int renderOffset = ui.renderOffset();
        float scrollPixels = ui.scrollFraction() * ROW_H;

        int panelH = layout.panelH;
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        // Clamp against the final height so a menu opened near the lower edge
        // grows down from one stable anchor instead of sliding while unfolding.
        int py = PanelChrome.clampY(menuY, layout.fullPanelH, screenH);

        PanelChrome.drawBackground(g, px, py, PANEL_W, panelH);

        g.enableScissor(px, py, px + PANEL_W, py + panelH - 1);

        // Title, with a paging hint so it's clear more rows exist off-screen. The
        // ◀/▶ glyphs mark the invisible left/right half-click paging zones.
        String title = "§e" + I18n.get("gui.minetuner.reorder.title");
        if (layout.paged)
            title += "  §7(" + (ui.scrollOffset + 1) + "-" + Math.min(ui.scrollOffset + layout.visibleRows, totalRows) + "/" + totalRows + ")";
        g.text(font, title, px + PANEL_PAD, py + PANEL_PAD, 0xFFFFFFFF, false);
        if (layout.paged) {
            boolean pageBackHover = mx >= px && mx < px + PANEL_W / 2 && my >= py + PANEL_PAD && my < py + PANEL_PAD + ROW_H;
            boolean pageFwdHover = mx >= px + PANEL_W / 2 && mx < px + PANEL_W && my >= py + PANEL_PAD && my < py + PANEL_PAD + ROW_H;
            g.text(font, pageBackHover ? "§f◀" : "§7◀", px + PANEL_W - 24, py + PANEL_PAD + (ROW_H - font.lineHeight) / 2, 0xFFFFFFFF, false);
            g.text(font, pageFwdHover ? "§f▶" : "§7▶", px + PANEL_W - 12, py + PANEL_PAD + (ROW_H - font.lineHeight) / 2, 0xFFFFFFFF, false);
        }

        int searchY = py + PANEL_PAD + ROW_H;
        renderSearchRow(g, font, mx, my, px, searchY, ui);

        // Clip only the scrollable list below the search field.
        int listTop = searchY + ROW_H;
        int listBottom = py + panelH;

        g.enableScissor(px, listTop, px + PANEL_W, listBottom);

        int rowTop = listTop;

        Compression compression = Compression.compute(rows, ui);

        int renderCount = layout.paged ? Math.min(rowsToRender + 1, totalRows - renderOffset) : rowsToRender;
        for (int visIdx = 0; visIdx < renderCount; visIdx++) {
            int logicalIdx = renderOffset + visIdx;
            int staticY = rowTop + visIdx * ROW_H - Math.round(scrollPixels);
            int ry = staticY - Math.round(compression.before(logicalIdx));

            if (logicalIdx == rows.size()) {
                // Close row.
                PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry, PANEL_W, ROW_H);
                g.text(font, "§7" + I18n.get("gui.minetuner.stat_settings.back"), px + PANEL_PAD, ry + 2, 0xFFFFFFFF, false);
                continue;
            }

            Row row = rows.get(logicalIdx);
            if (row instanceof HeaderRow header) {
                renderHeaderRow(g, font, mx, my, px, ry, ui, header);
            } else if (row instanceof StatRow statRow) {
                float reveal = ui.rolloutProgress(statRow.category());
                if (reveal <= 0.01f) continue;

                // Scale every child around the same category baseline. This
                // compresses the entire group as one rolling surface, so rows
                // never pile into a single visible line at animation start.
                float ownCompression = ROW_H * (1f - reveal);
                int categoryShift = Math.round(compression.beforeExcludingOwnCategory(logicalIdx, ownCompression));
                int unrolledY = staticY - categoryShift;
                int headerBottom = unrolledY - (statRow.indexInCategory() + 1) * ROW_H + ROW_H;
                var pose = g.pose();
                pose.pushMatrix();
                pose.translate(0, headerBottom);
                pose.scale(1f, reveal);
                pose.translate(0, -headerBottom);
                renderStatRow(g, font, mx, my, px, unrolledY, lc, statRow, reveal >= 0.98f);
                pose.popMatrix();
            }
        }

        g.disableScissor();
        // Pop the outer (whole-panel) scissor too — previously only the inner one
        // was popped, leaking the outer clip into everything drawn afterwards
        // wherever GuiGraphicsExtractor's scissor stacks.
        g.disableScissor();

        // Do this only after every layout calculation above has observed the
        // same state. Retiring a collapse mid-render was the source of the
        // expansion flicker: later rows treated it as fully open again.
        ui.finishCompletedRollouts();
    }

    /** Expands the panel surface with its child rows rather than snapping to final height. */
    private static int animatedVisibleRows(List<Row> rows, UiState ui, int maximum) {
        float rowUnits = 1f; // Back footer.
        for (Row row : rows) {
            rowUnits += row instanceof StatRow statRow ? ui.rolloutProgress(statRow.category()) : 1f;
        }
        return Math.max(1, Math.min(maximum, (int) Math.ceil(rowUnits)));
    }

    /** The search row. */
    private static void renderSearchRow(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                                        int mx, int my, int px, int ry, UiState ui) {
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry, PANEL_W, ROW_H);

        boolean hasText = ui.hasSearch();
        String icon = ui.isSearchFocused() ? "§e🔍" : "§7🔍";
        if (hasText || ui.isSearchFocused()) {
            g.text(font, icon + " ", px + PANEL_PAD, ry + 2, 0xFFFFFFFF, false);
            int textX = px + PANEL_PAD + font.width(icon + " ");
            RenameBoxPanel.drawBufferWithCaret(g, font, ui.searchBuffer(), textX, ry + 2,
                    PANEL_W - (textX - px) - PANEL_PAD - (hasText ? 14 : 0));
        } else {
            g.text(font, icon + " §8" + I18n.get("gui.minetuner.reorder.search_hint"),
                    px + PANEL_PAD, ry + 2, 0xFFFFFFFF, false);
        }

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
        String catName = I18n.get(LangKeys.category(header.category()));
        String count = "§7(" + header.enabledCount() + "/" + header.totalCount() + ")";
        g.text(font, arrow + " §e" + catName, px + PANEL_PAD, ry + 2, 0xFFFFFFFF, false);
        g.text(font, count, px + PANEL_W - PANEL_PAD - font.width(count), ry + 2, 0xFFFFFFFF, false);
    }

    private static void renderStatRow(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                                      int mx, int my, int px, int ry,
                                      MineTunerConfig.StatListConfig lc, StatRow statRow, boolean allowHover) {
        MineTunerConfig.Stat stat = statRow.stat();
        boolean enabled = lc.isEnabled(stat);

        if (allowHover) PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry, PANEL_W, ROW_H);

        String statName = I18n.get(LangKeys.stat(stat));
        String label = (enabled ? "§a✔ " : "§c✘ ") + statName;
        // Indented under its category header, and narrower than the full
        // width to leave room for the ⚙/▲/▼ cluster on the right without
        // needing per-stat-name truncation logic beyond the generic truncate()
        // helper below.
        g.text(font, truncate(font, label, PANEL_W - 16 - 36), px + PANEL_PAD + 8, ry + 2, 0xFFFFFFFF, false);

        boolean cogHovered = PanelChrome.isHoveringRow(mx, my, px + PANEL_W - 34, ry, 12, ROW_H);
        g.text(font, cogHovered ? "§e⚙" : "§7⚙", px + PANEL_W - 34, ry + 2, 0xFFFFFFFF, false);
        if (statRow.indexInCategory() > 0) {
            boolean upHovered = PanelChrome.isHoveringRow(mx, my, px + PANEL_W - 22, ry, 12, ROW_H);
            g.text(font, upHovered ? "§f▲" : "§7▲", px + PANEL_W - 22, ry + 2, 0xFFFFFFFF, false);
        }
        if (statRow.indexInCategory() < statRow.categorySize() - 1) {
            boolean downHovered = PanelChrome.isHoveringRow(mx, my, px + PANEL_W - 12, ry, 10, ROW_H);
            g.text(font, downHovered ? "§f▼" : "§7▼", px + PANEL_W - 12, ry + 2, 0xFFFFFFFF, false);
        }
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
                                   MineTunerConfig.StatListConfig lc, UiState ui) {
        int panelH = panelHeight(lc, ui);
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW), py = PanelChrome.clampY(menuY, panelH, screenH);
        return PanelChrome.isInsidePanel(mx, my, px, py, PANEL_W, panelH);
    }

    /** Handles a click at (mx, my). Uses the exact same layout math as {@link #render}
     *  (animated height, smoothed scroll offset, rollout compression) so a click lands
     *  on the row the user actually sees — previously it recomputed a full-height,
     *  unsmoothed, uncompressed grid and hit the wrong rows mid-animation/scroll. */
    public static MineTunerConfig.Stat handleClick(int mx, int my, int menuX, int menuY, int screenW, int screenH,
                                                   MineTunerConfig.StatListConfig lc, UiState ui, Runnable onClose) {
        Layout layout = layout(lc, ui);
        List<Row> rows = layout.rows;
        int totalRows = layout.totalRows;
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        // Same anchor clamp as render: against the final height, so the panel the
        // click math maps onto is the one on screen.
        int py = PanelChrome.clampY(menuY, layout.fullPanelH, screenH);

        // Title-row paging: clicking the left half of the title row pages back,
        // the right half pages forward — no dedicated arrow glyphs needed since
        // the title row already shows the "(X-Y/N)" range as a paging hint.
        if (layout.paged && PanelChrome.isHoveringRow(mx, my, px, py + PANEL_PAD, PANEL_W, ROW_H)) {
            if (mx < px + PANEL_W / 2) ui.setScrollOffset(ui.scrollOffset - MAX_VISIBLE_ROWS);
            else ui.setScrollOffset(ui.scrollOffset + MAX_VISIBLE_ROWS);
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

        int maxOffset = Math.max(0, totalRows - MAX_VISIBLE_ROWS);
        ui.clampAndAnimateScroll(maxOffset);
        int renderOffset = ui.renderOffset();
        float scrollPixels = ui.scrollFraction() * ROW_H;

        Compression compression = Compression.compute(rows, ui);

        int renderCount = layout.paged
                ? Math.min(layout.rowsToRender + 1, totalRows - renderOffset)
                : layout.rowsToRender;
        for (int visIdx = 0; visIdx < renderCount; visIdx++) {
            int logicalIdx = renderOffset + visIdx;
            int staticY = rowTop + visIdx * ROW_H - Math.round(scrollPixels);
            int ry = staticY - Math.round(compression.before(logicalIdx));
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
                // A row still mid-rollout (its category opening/closing) isn't
                // reliably where the hit-test math says it is — skip it, exactly
                // like render skips drawing interaction for it (reveal < 0.98).
                if (ui.rolloutProgress(statRow.category()) < 0.98f) return null;
                // Re-derive the stat row's SETTLED position: its own category's
                // compression collapses around it instead of shifting it.
                float ownCompression = ROW_H * (1f - ui.rolloutProgress(statRow.category()));
                int unrolledRy = staticY - Math.round(
                        compression.beforeExcludingOwnCategory(logicalIdx, ownCompression));
                if (my < unrolledRy || my >= unrolledRy + ROW_H) continue;
                return handleStatRowClick(mx, px, lc, statRow);
            }
        }
        return null;
    }

    /** Smooth mouse-wheel scrolling for the stat/category list. */
    public static boolean scrollBy(double mx, double my, double verticalAmount,
                                   int menuX, int menuY, int screenW, int screenH,
                                   MineTunerConfig.StatListConfig lc, UiState ui) {
        if (verticalAmount == 0d || !isInside((int) mx, (int) my, menuX, menuY, screenW, screenH, lc, ui)) return false;
        int maxOffset = Math.max(0, totalRowCount(lc, ui) - MAX_VISIBLE_ROWS);
        ui.setScrollOffset(ui.scrollOffset - (int) Math.signum(verticalAmount) * 3);
        ui.clampAndAnimateScroll(maxOffset);
        return true;
    }

    private static MineTunerConfig.Stat handleStatRowClick(int mx, int px, MineTunerConfig.StatListConfig lc, StatRow statRow) {
        MineTunerConfig.Stat stat = statRow.stat();

        // ⚙ cog: opens this stat's settings panel.
        if (mx >= px + PANEL_W - 34 && mx < px + PANEL_W - 22) {
            return stat;
        }
        // ▲: move up. moveWithinCategory() only ever swaps with the nearest
        // earlier statOrder entry that shares its category, so reordering
        // never crosses a category boundary.
        if (mx >= px + PANEL_W - 22 && mx < px + PANEL_W - 12 && statRow.indexInCategory() > 0) {
            moveWithinCategory(lc, stat, -1);
            MineTunerConfig.getInstance().save();
            // ▼: move down, same category-boundary rule as ▲ above.
        } else if (mx >= px + PANEL_W - 12 && statRow.indexInCategory() < statRow.categorySize() - 1) {
            moveWithinCategory(lc, stat, +1);
            MineTunerConfig.getInstance().save();
            // Clicked the row itself (not ⚙/▲/▼): toggle enabled state.
        } else {
            lc.setEnabled(stat, !lc.isEnabled(stat));
            MineTunerConfig.getInstance().save();
        }
        return null;
    }

    /** Swaps {@code stat} in-place with the nearest other stat in the same category,
     *  {@code direction} steps away in statOrder (-1 = toward the front/up, +1 = toward
     *  the back/down). Stats from other categories in between are skipped over rather
     *  than swapped with, so a move can never cross a category boundary. */
    private static void moveWithinCategory(MineTunerConfig.StatListConfig lc, MineTunerConfig.Stat stat, int direction) {
        MineTunerConfig.StatCategory cat = MineTunerConfig.categoryOf(stat);
        int from = lc.statOrder.indexOf(stat.name());
        if (from < 0) return;

        int target = from + direction;
        while (target >= 0 && target < lc.statOrder.size()) {
            MineTunerConfig.Stat candidate = MineTunerConfig.Stat.byName(lc.statOrder.get(target));
            if (candidate == null) {
                target += direction;
                continue;
            }
            if (MineTunerConfig.categoryOf(candidate) == cat) {
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
        private final Set<MineTunerConfig.StatCategory> expanded = EnumSet.noneOf(MineTunerConfig.StatCategory.class);
        private final Map<MineTunerConfig.StatCategory, Long> rolloutStartedAt =
                new EnumMap<>(MineTunerConfig.StatCategory.class);
        private static final long ROLLOUT_NANOS = 210_000_000L;
        /** Live filter text, entered by the search field toggled from the title row. */
        private final EditBuffer search = new EditBuffer(32);
        private int scrollOffset = 0;
        private float renderedScrollOffset = 0f;
        private long lastScrollUpdateNanos;
        /** Whether the search text field currently has keyboard focus. */
        private boolean searchFocused = false;

        /** Collapses every category, scrolls to the top, and clears any search. */
        public void reset() {
            expanded.clear();
            rolloutStartedAt.clear();
            resetScroll();
            search.clear();
            searchFocused = false;
        }

        boolean isExpanded(MineTunerConfig.StatCategory cat) {
            return expanded.contains(cat);
        }

        void toggle(MineTunerConfig.StatCategory cat) {
            boolean expanding = !expanded.remove(cat);
            if (expanding) expanded.add(cat);
            // Store the direction in the sign: positive is rolling open,
            // negative is rolling closed. This makes reversal symmetric.
            rolloutStartedAt.put(cat, expanding ? System.nanoTime() : -System.nanoTime());
            // Deliberately NOT resetting scroll here (the old behavior): a
            // category toggle no longer yanks the list back to the top —
            // clampAndAnimateScroll keeps the position valid as the row count
            // changes, so the list stays anchored where the user was looking.
        }

        boolean isAnimating(MineTunerConfig.StatCategory category) {
            return rolloutStartedAt.containsKey(category);
        }

        /** Eased reveal progress for one category, shared by all of its rows. */
        float rolloutProgress(MineTunerConfig.StatCategory category) {
            Long encodedStart = rolloutStartedAt.get(category);
            if (encodedStart == null) return 1f;
            boolean opening = encodedStart > 0L;
            float elapsed = Math.max(0f, Math.min(1f, (System.nanoTime() - Math.abs(encodedStart)) / (float) ROLLOUT_NANOS));
            if (elapsed >= 1f) {
                return opening ? 1f : 0f;
            }
            float inverse = 1f - elapsed;
            float eased = 1f - inverse * inverse * inverse;
            return opening ? eased : 1f - eased;
        }

        /** Removes completed transitions after a complete render pass. */
        void finishCompletedRollouts() {
            long now = System.nanoTime();
            rolloutStartedAt.entrySet().removeIf(entry ->
                    now - Math.abs(entry.getValue()) >= ROLLOUT_NANOS);
        }

        public boolean isSearchFocused() {
            return searchFocused;
        }

        public String searchText() {
            return search.toString();
        }

        /** Direct access for the shared caret/clipboard key handling — the existing
         *  append/backspace/clear forwarders above remain the common path. */
        public EditBuffer searchBuffer() {
            return search;
        }

        public boolean hasSearch() {
            return !search.isEmpty();
        }

        /** Toggles the search field's focus. */
        public void toggleSearchFocus() {
            searchFocused = !searchFocused;
            resetScroll();
        }

        public void appendSearch(int codePoint) {
            if (search.appendCodePoint(codePoint)) resetScroll();
        }

        public void backspaceSearch() {
            if (search.backspace()) resetScroll();
        }

        public void clearSearch() {
            search.clear();
            resetScroll();
        }

        private void resetScroll() {
            scrollOffset = 0;
            renderedScrollOffset = 0f;
            lastScrollUpdateNanos = 0L;
        }

        void setScrollOffset(int target) {
            scrollOffset = target;
        }

        void clampAndAnimateScroll(int maxOffset) {
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));
            if (lastScrollUpdateNanos == 0L) {
                renderedScrollOffset = scrollOffset;
                lastScrollUpdateNanos = System.nanoTime();
                return;
            }
            long now = System.nanoTime();
            float seconds = Math.min(0.1f, (now - lastScrollUpdateNanos) / 1_000_000_000f);
            lastScrollUpdateNanos = now;
            float smoothing = 1f - (float) Math.exp(-16f * seconds);
            renderedScrollOffset += (scrollOffset - renderedScrollOffset) * smoothing;
            if (Math.abs(scrollOffset - renderedScrollOffset) < 0.01f) renderedScrollOffset = scrollOffset;
            renderedScrollOffset = Math.max(0f, Math.min(renderedScrollOffset, maxOffset));
        }

        int renderOffset() {
            return (int) Math.floor(renderedScrollOffset);
        }

        float scrollFraction() {
            return renderedScrollOffset - (float) Math.floor(renderedScrollOffset);
        }
    }

    private record HeaderRow(MineTunerConfig.StatCategory category, int enabledCount, int totalCount) implements Row {
    }

    private record StatRow(MineTunerConfig.Stat stat, MineTunerConfig.StatCategory category,
                           int indexInCategory, int categorySize) implements Row {
    }
}