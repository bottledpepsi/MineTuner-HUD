package bottled.minetuner.gui.panel;

import bottled.minetuner.core.EditBuffer;
import bottled.minetuner.gui.render.PanelChrome;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import static bottled.minetuner.gui.render.PanelChrome.*;


public final class RenameBoxPanel {

    private RenameBoxPanel() {
    }

    /** Blink period for the caret, in ms — half on, half off. */
    private static final long CARET_BLINK_MS = 530;

    /**
     * Renders the box with a blinking caret at the buffer's cursor position,
     * clipped so a long name scrolls with the caret kept visible.
     *
     * @param errorHint optional inline error line (e.g. a rejected theme name);
     *                  grows the box by one row when present so the failure is
     *                  visible instead of the commit silently doing nothing.
     */
    public static void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                              int menuX, int menuY, int screenW, int screenH,
                              EditBuffer buffer, String errorHint) {
        String prompt = "§e" + I18n.get("gui.minetuner.rename.prompt");
        int panelW = PANEL_W + 40;
        int panelH = PANEL_PAD * 2 + ROW_H * 2 + 2 + (errorHint != null ? ROW_H : 0);
        int px = PanelChrome.clampX(menuX, panelW, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);

        PanelChrome.drawBackground(g, px, py, panelW, panelH);
        g.text(font, prompt, px + PANEL_PAD, py + PANEL_PAD, 0xFFFFFFFF, false);
        drawBufferWithCaret(g, font, buffer, px + PANEL_PAD, py + PANEL_PAD + ROW_H + 2, panelW - PANEL_PAD * 2);
        if (errorHint != null) {
            g.text(font, errorHint, px + PANEL_PAD, py + PANEL_PAD + ROW_H * 2 + 2, 0xFFFFFFFF, false);
        }
    }

    /** Draws left+caret+right around the cursor, trimming from the left first (then
     *  the right) so the caret can never be pushed out of the box by long text.
     *  Package-private: TemplateListPanel's edit box shares it. */
    static void drawBufferWithCaret(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                                    EditBuffer buffer, int x, int y, int maxWidth) {
        String left = buffer.textBeforeCursor();
        String right = buffer.textAfterCursor();
        boolean blink = (System.currentTimeMillis() / CARET_BLINK_MS) % 2 == 0;
        String caret = blink ? "§7|" : "";

        // Trim whole code points from the left, then from the right, until it fits.
        while (!left.isEmpty() && font.width(left + caret + right) > maxWidth) {
            left = left.substring(left.offsetByCodePoints(0, 1));
        }
        while (!right.isEmpty() && font.width(left + caret + right) > maxWidth) {
            right = right.substring(0, right.offsetByCodePoints(right.length(), -1));
        }

        g.text(font, left + caret, x, y, 0xFFFFFFFF, false);
        if (!right.isEmpty()) {
            g.text(font, right, x + font.width(left + caret), y, 0xFFFFFFFF, false);
        }
    }
}
