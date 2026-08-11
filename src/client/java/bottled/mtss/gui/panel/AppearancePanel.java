package bottled.mtss.gui.panel;

import bottled.mtss.config.MtssConfig;
import bottled.mtss.gui.render.PanelChrome;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;

import static bottled.mtss.gui.render.PanelChrome.*;


public final class AppearancePanel {

    public static final int AP_RENAME = 0;
    public static final int AP_BG = 1;
    public static final int AP_SHADOW = 2;
    public static final int AP_COLOR_SCALE = 3;
    public static final int AP_TEMPLATE_MODE = 4;
    public static final int AP_BACK = 5;
    public static final int AP_COUNT = 6;
    private AppearancePanel() {
    }

    public static int panelHeight() {
        return PANEL_PAD * 2 + ROW_H * AP_COUNT;
    }

    public static void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font,
                              int mx, int my, int menuX, int menuY, int screenW, int screenH,
                              MtssConfig.StatListConfig lc) {
        String onOffBg = lc.showBackground ? " §a" + I18n.get("gui.mtss.menu.on")
                : " §c" + I18n.get("gui.mtss.menu.off");
        String onOffShadow = lc.textShadow ? " §a" + I18n.get("gui.mtss.menu.on")
                : " §c" + I18n.get("gui.mtss.menu.off");
        String onOffTemplate = lc.useTemplate ? " §a" + I18n.get("gui.mtss.menu.on")
                : " §c" + I18n.get("gui.mtss.menu.off");

        String[] labels = new String[AP_COUNT];
        labels[AP_RENAME] = "§e" + I18n.get("gui.mtss.menu.rename");
        labels[AP_BG] = "§f" + I18n.get("gui.mtss.menu.background") + onOffBg;
        labels[AP_SHADOW] = "§f" + I18n.get("gui.mtss.menu.shadow") + onOffShadow;
        labels[AP_COLOR_SCALE] = "§f" + I18n.get("gui.mtss.menu.color_scale") + " »";
        labels[AP_TEMPLATE_MODE] = "§f" + I18n.get("gui.mtss.menu.template_mode") + onOffTemplate;
        labels[AP_BACK] = "§7" + I18n.get("gui.mtss.stat_settings.back");

        int px = PanelChrome.clampX(menuX, PANEL_W, screenW);
        int py = PanelChrome.clampY(menuY, panelHeight(), screenH);
        PanelChrome.drawLabelPanel(g, font, labels, mx, my, px, py, PANEL_W);
    }

    public static boolean isInside(int mx, int my, int menuX, int menuY, int screenW, int screenH) {
        int panelH = panelHeight();
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW), py = PanelChrome.clampY(menuY, panelH, screenH);
        return PanelChrome.isInsidePanel(mx, my, px, py, PANEL_W, panelH);
    }

    /** Returns the clicked row index (AP_* constant), or -1 if the click missed. */
    public static int rowAt(int mx, int my, int menuX, int menuY, int screenW, int screenH) {
        int px = PanelChrome.clampX(menuX, PANEL_W, screenW), py = PanelChrome.clampY(menuY, panelHeight(), screenH);
        if (mx < px || mx > px + PANEL_W) return -1;
        int rel = my - (py + PANEL_PAD);
        if (rel < 0 || rel >= ROW_H * AP_COUNT) return -1;
        return rel / ROW_H;
    }
}
