package bottled.mtss.gui.panel;

import bottled.mtss.gui.render.PanelChrome;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import static bottled.mtss.gui.render.PanelChrome.*;


public final class RenameBoxPanel {

    private RenameBoxPanel() {
    }

    public static void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                              int menuX, int menuY, int screenW, int screenH, String renameBuffer) {
        String prompt = "§e" + I18n.get("gui.mtss.rename.prompt");
        String display = renameBuffer + "§7|";
        int panelW = PANEL_W + 40;
        int panelH = PANEL_PAD * 2 + ROW_H * 2 + 2;
        int px = PanelChrome.clampX(menuX, panelW, screenW);
        int py = PanelChrome.clampY(menuY, panelH, screenH);

        PanelChrome.drawBackground(g, px, py, panelW, panelH);
        g.text(font, prompt, px + PANEL_PAD, py + PANEL_PAD, 0xFFFFFFFF, false);
        g.text(font, display, px + PANEL_PAD, py + PANEL_PAD + ROW_H + 2, 0xFFFFFFFF, false);
    }
}
