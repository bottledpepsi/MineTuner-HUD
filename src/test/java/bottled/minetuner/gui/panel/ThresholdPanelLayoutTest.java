package bottled.minetuner.gui.panel;

import bottled.minetuner.gui.render.PanelChrome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Layout invariants for the threshold sub-panel. Regression guard for a real
 * overlap bug: the direction-subtitle line and row 0 ("Use custom thresholds")
 * used to draw on top of each other because rows started only 2px below the
 * subtitle's baseline instead of a full ROW_H below it.
 */
class ThresholdPanelLayoutTest {

    private static final int PY = 100;
    private static final int LINE_HEIGHT = 9; // vanilla font.lineHeight at GUI scale 1.
    private static final int PAD = PanelChrome.PANEL_PAD;
    private static final int ROW_H = PanelChrome.ROW_H;

    @Test
    void rowsStartBelowTitleAndSubtitleLines() {
        int[] rowY = ThresholdPanel.rowOffsets(PY, LINE_HEIGHT);
        // Title occupies [PAD, PAD + lineHeight); subtitle [PAD + lineHeight,
        // PAD + 2*lineHeight). Row 0's top must be at or below the subtitle's
        // bottom — previously it started above it.
        assertTrue(rowY[ThresholdPanel.TH_USE_CUSTOM] >= PY + PAD + 2 * LINE_HEIGHT,
                "row 0 must start below the title + subtitle lines");
    }

    @Test
    void panelHeightCoversAllRowsPlusPadding() {
        int panelH = ThresholdPanel.panelHeight(LINE_HEIGHT);
        int[] rowY = ThresholdPanel.rowOffsets(PY, LINE_HEIGHT);
        int lastRowBottom = rowY[ThresholdPanel.TH_BACK] + ROW_H;
        assertTrue(panelH >= lastRowBottom - PY + PAD,
                "panel height must cover the last row plus bottom padding");
    }

    @Test
    void rowsAreSpacedByRowHeightWithoutGapsOrOverlap() {
        int[] rowY = ThresholdPanel.rowOffsets(PY, LINE_HEIGHT);
        assertEquals(ThresholdPanel.TH_COUNT, rowY.length);
        for (int i = 1; i < rowY.length; i++) {
            assertEquals(ROW_H, rowY[i] - rowY[i - 1]);
        }
    }

    @Test
    void subtitleRowHasBreathingRoomBeforeFirstRow() {
        int[] rowY = ThresholdPanel.rowOffsets(PY, LINE_HEIGHT);
        int subtitleBottom = PY + PAD + 2 * LINE_HEIGHT;
        assertTrue(rowY[ThresholdPanel.TH_USE_CUSTOM] >= subtitleBottom,
                "row 0 must not start inside the subtitle line");
    }
}
