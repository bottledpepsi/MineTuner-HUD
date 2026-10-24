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
    // Navigation/editing keys for the caret-aware text fields.
    public static final int LEFT = 263;
    public static final int RIGHT = 262;
    public static final int UP = 265;
    public static final int DOWN = 264;
    public static final int HOME = 268;
    public static final int END = 269;
    public static final int DELETE = 261;
    // Letters (for Ctrl+A/C/V/X shortcuts).
    public static final int A = 65;
    public static final int C = 67;
    public static final int V = 86;
    public static final int X = 88;

    /** GLFW modifier bitmask: the Control key. */
    public static final int MOD_CONTROL = 0x2;
    /** GLFW modifier bitmask: either Shift key. */
    public static final int MOD_SHIFT = 0x1;

    private Keys() {
    }

    public static boolean isEnter(int keyCode) {
        return keyCode == ENTER || keyCode == KP_ENTER;
    }
}
