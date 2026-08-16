package bottled.minetuner.hud;

import bottled.minetuner.config.MineTunerConfig;
import bottled.minetuner.hud.LineCache.GraphEntry;
import bottled.minetuner.hud.LineCache.RowKind;
import bottled.minetuner.sample.SamplingDriver;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

/** Renders the live stat overlay using the same panel geometry as the editor preview. */
public class MineTunerRenderer {

    public static void drawRows(GuiGraphicsExtractor graphics, Font font,
                                LineCache cache, int baseX, int baseY, boolean shadow) {
        drawRows(graphics, font, cache, baseX, baseY, shadow, -1, false,
                HudPanelChrome.PADDING_X, HudPanelChrome.PADDING_Y);
    }

    /**
     * Draws relative to a panel's top-left corner. Keeping padding local means
     * scaled panels have exactly the same composition as their 1x counterpart.
     */
    public static void drawRows(GuiGraphicsExtractor graphics, Font font, LineCache cache,
                                int baseX, int baseY, boolean shadow, int listId, boolean animate,
                                int paddingX, int paddingY) {
        int lineH = font.lineHeight + HudPanelChrome.ROW_GAP;
        int px = HudPanelChrome.paddingX(paddingX);
        int py = HudPanelChrome.paddingY(paddingY);
        int contentX = baseX + px;
        int contentW = HudPanelChrome.contentWidth(cache.boxW(font, px), px);
        int textIdx = 0;
        int graphIdx = 0;
        int cursorY = baseY + py;
        long now = animate ? System.nanoTime() : 0L;

        for (RowKind kind : cache.rowKinds()) {
            if (kind == RowKind.TEXT) {
                List<TemplateEngine.ColoredRun> runs = cache.runs().get(textIdx);
                float pulse = animate ? HudMotion.pulseFor(listId, textIdx, runs, now) : 0f;
//                int settleY = cursorY - Math.round(pulse);
                drawColoredRuns(graphics, font, runs, contentX, cursorY, shadow, pulse);
                textIdx++;
                cursorY += lineH;
            } else {
                GraphEntry entry = cache.graphEntries().get(graphIdx++);
                GraphRenderer.drawGraph(graphics, font, entry, contentX, cursorY, contentW, entry.style().height);
                cursorY += entry.style().height + 1;
            }
        }
    }

    private static void drawColoredRuns(GuiGraphicsExtractor graphics, Font font,
                                        List<TemplateEngine.ColoredRun> runs, int x, int y,
                                        boolean shadow, float pulse) {
        int cursorX = x;
        for (TemplateEngine.ColoredRun run : runs) {
            int color = ColorMath.blend(run.color(), 0xFFFFFFFF, pulse * 0.18f);
            graphics.text(font, run.text(), cursorX, y, color, shadow);
            cursorX += font.width(run.text());
        }
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!MineTunerConfig.getInstance().overlayEnabled || mc.getDebugOverlay().showDebugScreen()
                || mc.getConnection() == null || mc.gui.screen() instanceof bottled.minetuner.gui.MineTunerGuiScreen) return;

        LineCache.tickCache();
        SamplingDriver.sampleAll();

        MineTunerConfig root = MineTunerConfig.getInstance();
        Font font = mc.font;
        for (MineTunerConfig.StatListConfig listCfg : root.lists) {
            LineCache cache = LineCache.getCachedLines(listCfg);
            if (cache.rowKinds().isEmpty()) continue;

            float scale = listCfg.textScale <= 0f ? 1f : listCfg.textScale;
            int boxW = Math.round(cache.boxW(font, listCfg.paddingX) * scale);
            int boxH = Math.round(cache.boxH(font, listCfg.paddingY) * scale);
            int[] pos = ListPositioner.getPosition(listCfg, graphics.guiWidth(), graphics.guiHeight(), boxW, boxH);
            int x = pos[0];
            int y = pos[1];

            if (listCfg.showBackground) {
                HudPanelChrome.drawPanel(graphics, x, y, boxW, boxH,
                        listCfg.overrideFillColor, listCfg.overrideOutlineColor);
            }

            if (scale == 1f) {
                drawRows(graphics, font, cache, x, y, listCfg.textShadow, listCfg.id, true, listCfg.paddingX, listCfg.paddingY);
            } else {
                var matrices = graphics.pose();
                matrices.pushMatrix();
                matrices.translate(x, y);
                matrices.scale(scale, scale);
                drawRows(graphics, font, cache, 0, 0, listCfg.textShadow, listCfg.id, true, listCfg.paddingX, listCfg.paddingY);
                matrices.popMatrix();
            }
        }
    }
}
