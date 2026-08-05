package bottled.mtss.gui.panel;

import net.minecraft.client.gui.GuiGraphicsExtractor;


public interface GuiPanel {

    /** Draws this panel at its resolved position, given the current mouse position for hover highlighting. */
    void render(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, int mx, int my);

    /** Whether (mx, my) falls inside this panel's bounds. */
    boolean isInside(int mx, int my);

    /** Handles a click at (mx, my), already known to be inside this panel. */
    void handleClick(int mx, int my);
}
