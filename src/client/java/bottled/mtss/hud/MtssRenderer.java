package bottled.mtss.hud;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.hud.LineCache.GraphEntry;
import bottled.mtss.hud.LineCache.RowKind;
import bottled.mtss.sample.SamplingDriver;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;


public class MtssRenderer {

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();

        if (!MtssConfig.getInstance().overlayEnabled) return;
        if (mc.getDebugOverlay().showDebugScreen()) return;
        if (mc.getConnection() == null) return;
        if (mc.gui.screen() instanceof bottled.mtss.gui.MtssGuiScreen) return;

        // Advance the frame cache so getCachedLines() is fresh this frame
        LineCache.tickCache();

        // ── Data collection ──────────────────────────────────────────────────
        // Each raw value is pulled in by its own StatSource (bottled.mtss.sample),
        // registered in SourceRegistry and run here at its declared cadence.
        // See the design doc for the full acquisition-side pipeline.
        SamplingDriver.sampleAll();

        // ── Render each stat list (uses frame cache) ─────────────────────────
        MtssConfig root = MtssConfig.getInstance();
        var font = mc.font;

        for (MtssConfig.StatListConfig listCfg : root.lists) {
            LineCache cache = LineCache.getCachedLines(listCfg);
            if (cache.rowKinds().isEmpty()) continue;

            float scale = listCfg.textScale <= 0f ? 1f : listCfg.textScale;
            int unscaledW = cache.boxW(font);
            int unscaledH = cache.boxH(font);

            // Position math is in screen-pixel space, so use the scaled box size —
            // otherwise a scaled-up list could overlap the screen edge or other lists.
            int boxW = Math.round(unscaledW * scale);
            int boxH = Math.round(unscaledH * scale);

            int[] pos = ListPositioner.getPosition(listCfg, graphics.guiWidth(), graphics.guiHeight(), boxW, boxH);
            int x = pos[0], y = pos[1];

            if (listCfg.showBackground) {
                graphics.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0x90000000);
            }
            boolean shadow = listCfg.textShadow;

            if (scale == 1f) {
                drawRows(graphics, font, cache, x + 2, y + 2, shadow);
            } else {
                // Translate to (x, y), scale, then draw at unscaled local offsets.
                var matrices = graphics.pose();
                matrices.pushMatrix();
                matrices.translate(x, y);
                matrices.scale(scale, scale);
                drawRows(graphics, font, cache, 0, 0, shadow);
                matrices.popMatrix();
            }
        }
    }


    public static void drawRows(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font,
                                 LineCache cache, int baseX, int baseY, boolean shadow) {
        int lineH = font.lineHeight + 1;
        // Graphs stretch to the box's actual content width — which may be wider
        // than a graph's configured width if a label needed extra room.
        int contentW = cache.boxW(font) - 4;
        int textIdx = 0, graphIdx = 0;
        int cursorY = baseY;
        for (RowKind kind : cache.rowKinds()) {
            if (kind == RowKind.TEXT) {
                drawColoredRuns(graphics, font, cache.runs().get(textIdx), baseX, cursorY, shadow);
                textIdx++;
                cursorY += lineH;
            } else {
                GraphEntry entry = cache.graphEntries().get(graphIdx);
                GraphRenderer.drawGraph(graphics, font, entry, baseX, cursorY, contentW, entry.style().height);
                graphIdx++;
                cursorY += entry.style().height + 1;
            }
        }
    }

    /**
     * Draws one text row as a sequence of colored runs left-to-right, each
     * positioned after the previous run's rendered width — the general form
     * of what a single {@code graphics.text(...)} call did before inline
     * template coloring existed. The overwhelming majority of rows (every
     * classic-mode row, and any template row with no {@code color=}
     * modifier) are still exactly one run, so this is one draw call in
     * those cases, same as before.
     */
    private static void drawColoredRuns(GuiGraphicsExtractor graphics, net.minecraft.client.gui.Font font,
                                        List<TemplateEngine.ColoredRun> runs, int x, int y, boolean shadow) {
        int cursorX = x;
        for (TemplateEngine.ColoredRun run : runs) {
            graphics.text(font, run.text(), cursorX, y, run.color(), shadow);
            cursorX += font.width(run.text());
        }
    }
}
