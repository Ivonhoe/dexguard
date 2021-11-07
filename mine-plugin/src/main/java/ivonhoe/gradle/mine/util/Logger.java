package ivonhoe.gradle.mine.util;

import com.android.ddmlib.Log;

public class Logger {

    private static final String TAG = "dexguard";
    private static final boolean LOG_W = true;
    private static final boolean LOG_D = true;

    public static void d(String msg) {
        if (LOG_D) {
            Log.d(TAG, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (LOG_D) {
            Log.d(tag, msg);
        }
    }

    public static void w(String msg) {
        if (LOG_W) {
            Log.w(TAG, msg);
        }
    }
}