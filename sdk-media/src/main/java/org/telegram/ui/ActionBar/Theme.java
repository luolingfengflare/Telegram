package org.telegram.ui.ActionBar;

public final class Theme {
    private Theme() {}

    /** Color key for outgoing-message loader spinner. */
    public static final int key_chat_outLoader = 1;

    /** Color key for incoming-message loader spinner. */
    public static final int key_chat_inLoader = 2;

    /**
     * Resource provider passed by callers that want to override default colors.
     * AudioVisualizerDrawable consults this if non-null, otherwise falls back to
     * {@link Theme#getColor(int)}.
     */
    public interface ResourcesProvider {
        int getColor(int key);
    }

    /** Default palette returns Material Blue tones. */
    public static int getColor(int key) {
        switch (key) {
            case key_chat_outLoader: return 0xFF2196F3; // Material Blue 500
            case key_chat_inLoader:  return 0xFF1976D2; // Material Blue 700
            default:                 return 0xFF888888; // medium gray
        }
    }

    /** Provider-aware accessor. Falls through to {@link #getColor(int)} if provider is null. */
    public static int getColor(int key, ResourcesProvider provider) {
        if (provider != null) return provider.getColor(key);
        return getColor(key);
    }
}
