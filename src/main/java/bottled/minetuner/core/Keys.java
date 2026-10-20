package bottled.minetuner.core;

/**
 * GLFW key codes used by the editor's text-entry handling (stable GLFW values),
 * named so call sites do not carry magic numbers.
 */
public final class Keys {

    public static final int ESCAPE = 256;
    public static final int ENTER = 257;
    public static final int BACKSPACE = 259;
    public static final int KP_ENTER = 335;

    private Keys() {
    }

    public static boolean isEnter(int keyCode) {
        return keyCode == ENTER || keyCode == KP_ENTER;
    }
}
