package org.telegram.messenger;

public final class LiteMode {
    private LiteMode() {}

    /** Flag constant used by AudioVisualizerDrawable. Numeric value irrelevant for the stub. */
    public static final int FLAG_CHAT_BACKGROUND = 1;

    /** Default: animation enabled. The sample app can shadow this if it wants opt-out. */
    public static boolean isEnabled(int flag) {
        return true;
    }
}
