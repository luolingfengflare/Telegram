package org.telegram.messenger;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;

import java.util.List;

public final class AndroidUtilities {
    private AndroidUtilities() {}

    /** Display density. Host updates via {@link #setDensity(float)} when display metrics change. */
    public static float density = 1f;

    /** Display refresh rate in Hz. Host updates as needed. */
    public static float screenRefreshRate = 60f;

    /** Shared scratch rect — matches Telegram's AndroidUtilities.rectTmp. */
    public static final RectF rectTmp = new RectF();

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void setDensity(float value) {
        density = value;
    }

    public static int dp(float value) {
        if (value == 0) return 0;
        return (int) Math.ceil(density * value);
    }

    public static void runOnUIThread(Runnable r) {
        MAIN.post(r);
    }

    public static void runOnUIThread(Runnable r, long delayMs) {
        MAIN.postDelayed(r, delayMs);
    }

    public static void recycleBitmaps(List<Bitmap> bitmaps) {
        if (bitmaps == null) return;
        for (Bitmap b : bitmaps) {
            if (b != null && !b.isRecycled()) {
                try { b.recycle(); } catch (Exception ignored) {}
            }
        }
    }
}
