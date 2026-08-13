package bottled.minetuner.gui.panel;

import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.gui.render.PanelChrome;
import bottled.minetuner.stat.StatRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import static bottled.minetuner.gui.render.PanelChrome.*;


public final class ThresholdPanel {

    public static final int TH_USE_CUSTOM = 0;
    public static final int TH_GOOD = 1;
    public static final int TH_WARN = 2;
    public static final int TH_BACK = 3;
    public static final int TH_COUNT = 4;
    private ThresholdPanel() {
    }

    private static boolean isHigherBetter(MineTunerConfig.Stat stat) {
        return StatRegistry.get(stat).higherIsBetter();
    }

    private static float thresholdStep(MineTunerConfig.Stat stat) {
        return StatRegistry.get(stat).thresholdStep();
    }

    /** Total panel height for the threshold sub-panel (fixed row count, no
     *  scrolling needed since TH_COUNT never changes) plus one title-row's worth
     *  of extra height (font.lineHeight + 2) for the stat-name header above the rows. */
    public static int panelHeight(net.minecraft.client.gui.Font font) {
        return PANEL_PAD * 2 + ROW_H * TH_COUNT + font.lineHeight + 2;
    }

    /** Y-offsets (relative to the panel's top-left) for each of the threshold
     *  panel's rows, below the title-row space {@link #panelHeight} accounts for. */
    private static int[] rowOffsets(int py, net.minecraft.client.gui.Font font) {
        int rowTop = py + PANEL_PAD + font.lineHeight + 2;
        int[] rowY = new int[TH_COUNT];
        for (int i = 0; i < TH_COUNT; i++) rowY[i] = rowTop + ROW_H * i;
        return rowY;
    }

    /** Formats a threshold value without a trailing ".0" for whole-number steps. */
    private static String formatThreshold(float v) {
        if (v == Math.floor(v)) return String.valueOf((int) v);
        return String.format("%.1f", v);
    }

    public static void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                              int mx, int my, int menuX, int menuY, int screenW, int screenH,
                              MineTunerConfig.StatListConfig lc, MineTunerConfig.Stat statSettingsStat,
                              MineTunerConfig.ThresholdSettings ts) {
        boolean higherBetter = isHigherBetter(statSettingsStat);
        String statLabel = I18n.get("stat.minetuner." + statSettingsStat.name().toLowerCase());

        int panelH = panelHeight(font);
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);

        PanelChrome.drawBackground(g, px, py, PANEL_W, panelH);

        // Header + direction subtitle.
        g.text(font, "§e" + I18n.get("gui.minetuner.threshold.title", statLabel),
                px + PANEL_PAD, py + PANEL_PAD, 0xFFFFFFFF, false);
        String dirLabel = higherBetter ? I18n.get("gui.minetuner.threshold.higher_is_better")
                : I18n.get("gui.minetuner.threshold.lower_is_better");
        g.text(font, "§7" + dirLabel, px + PANEL_PAD, py + PANEL_PAD + font.lineHeight, 0xFF999999, false);

        int[] rowY = rowOffsets(py, font);

        // Row 0.
        int ry0 = rowY[TH_USE_CUSTOM];
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry0, PANEL_W, ROW_H);
        String useCustomLabel = I18n.get("gui.minetuner.threshold.use_custom")
                + (ts.enabled ? " §a" + I18n.get("gui.minetuner.menu.on")
                : " §c" + I18n.get("gui.minetuner.menu.off"));
        g.text(font, "§f" + useCustomLabel, px + PANEL_PAD, ry0 + 2, 0xFFFFFFFF, false);

        // Row 1.
        int ry1 = rowY[TH_GOOD];
        boolean hoverGoodDown = PanelChrome.isHoveringRow(mx, my, px, ry1, PANEL_W / 2, ROW_H);
        boolean hoverGoodUp = PanelChrome.isHoveringRow(mx, my, px + PANEL_W / 2, ry1, PANEL_W / 2, ROW_H);
        if (hoverGoodDown) g.fill(px + 1, ry1, px + PANEL_W / 2, ry1 + ROW_H, 0x44FFFFFF);
        if (hoverGoodUp) g.fill(px + PANEL_W / 2, ry1, px + PANEL_W - 1, ry1 + ROW_H, 0x44FFFFFF);
        g.text(font, "§f- " + I18n.get("gui.minetuner.threshold.good", formatThreshold(ts.goodMin)),
                px + PANEL_PAD, ry1 + 2, 0xFFFFFFFF, false);
        g.text(font, "§f+", px + PANEL_W - 14, ry1 + 2, 0xFFFFFFFF, false);

        // Row 2.
        int ry2 = rowY[TH_WARN];
        boolean hoverWarnDown = PanelChrome.isHoveringRow(mx, my, px, ry2, PANEL_W / 2, ROW_H);
        boolean hoverWarnUp = PanelChrome.isHoveringRow(mx, my, px + PANEL_W / 2, ry2, PANEL_W / 2, ROW_H);
        if (hoverWarnDown) g.fill(px + 1, ry2, px + PANEL_W / 2, ry2 + ROW_H, 0x44FFFFFF);
        if (hoverWarnUp) g.fill(px + PANEL_W / 2, ry2, px + PANEL_W - 1, ry2 + ROW_H, 0x44FFFFFF);
        g.text(font, "§f- " + I18n.get("gui.minetuner.threshold.warn", formatThreshold(ts.warnMin)),
                px + PANEL_PAD, ry2 + 2, 0xFFFFFFFF, false);
        g.text(font, "§f+", px + PANEL_W - 14, ry2 + 2, 0xFFFFFFFF, false);

        // Row 3.
        int ry3 = rowY[TH_BACK];
        PanelChrome.drawRowHoverIfNeeded(g, mx, my, px, ry3, PANEL_W, ROW_H);
        g.text(font, "§7" + I18n.get("gui.minetuner.stat_settings.back"),
                px + PANEL_PAD, ry3 + 2, 0xFFFFFFFF, false);
    }

    public static boolean isInside(int mx, int my, int menuX, int menuY, int screenW, int screenH,
                                   net.minecraft.client.gui.Font font) {
        int panelH = panelHeight(font);
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW), py = PanelChrome.clampY(menuY, panelH, screenH);
        return PanelChrome.isInsidePanel(mx, my, px, py, PANEL_W, panelH);
    }

    /** Keeps goodMin/warnMin from inverting after an adjustment. */
    private static void clampThresholdOrder(MineTunerConfig.ThresholdSettings ts, boolean higherBetter, boolean adjustedGood) {
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
    private static float roundStep(float v) {
        return Math.round(v * 10f) / 10f;
    }

    /** Returns true if the click landed on the Back row (caller should set
     *  thresholdPanelOpen = false to return to the stat settings panel this was
     *  opened from — statSettingsStat itself is left set, so it's the settings
     *  panel and not the reorder panel underneath that reappears). */
    public static boolean handleClick(int mx, int my, int menuX, int menuY, int screenW, int screenH,
                                      net.minecraft.client.gui.Font font,
                                      MineTunerConfig.Stat statSettingsStat, MineTunerConfig.ThresholdSettings ts,
                                      MineTunerConfig root) {
        boolean higherBetter = isHigherBetter(statSettingsStat);
        float step = thresholdStep(statSettingsStat);

        int panelH = panelHeight(font);
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);
        int[] rowY = rowOffsets(py, font);

        int ry0 = rowY[TH_USE_CUSTOM];
        int ry1 = rowY[TH_GOOD];
        int ry2 = rowY[TH_WARN];
        int ry3 = rowY[TH_BACK];

        if (PanelChrome.isHoveringRow(mx, my, px, ry0, PANEL_W, ROW_H)) {
            ts.enabled = !ts.enabled;
            root.save();
            return false;
        }

        if (PanelChrome.isHoveringRow(mx, my, px, ry1, PANEL_W / 2, ROW_H)) {
            ts.goodMin = Math.max(0f, roundStep(ts.goodMin - step));
            clampThresholdOrder(ts, higherBetter, true);
            root.save();
            return false;
        } else if (PanelChrome.isHoveringRow(mx, my, px + PANEL_W / 2, ry1, PANEL_W / 2, ROW_H)) {
            ts.goodMin = Math.max(0f, roundStep(ts.goodMin + step));
            clampThresholdOrder(ts, higherBetter, true);
            root.save();
            return false;
        }

        if (PanelChrome.isHoveringRow(mx, my, px, ry2, PANEL_W / 2, ROW_H)) {
            ts.warnMin = Math.max(0f, roundStep(ts.warnMin - step));
            clampThresholdOrder(ts, higherBetter, false);
            root.save();
            return false;
        } else if (PanelChrome.isHoveringRow(mx, my, px + PANEL_W / 2, ry2, PANEL_W / 2, ROW_H)) {
            ts.warnMin = Math.max(0f, roundStep(ts.warnMin + step));
            clampThresholdOrder(ts, higherBetter, false);
            root.save();
            return false;
        }

        return PanelChrome.isHoveringRow(mx, my, px, ry3, PANEL_W, ROW_H);
    }
}
