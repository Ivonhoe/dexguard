package ivonhoe.android.mine.trace;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Trace;


public class SystemTrace {

    private static final String TAG = "TraceTag";

    /**
     * hook method when it's called in.
     *
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void begin(String name) {
        Trace.beginSection(name);
    }

    /**
     * hook method when it's called out.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static void end() {
        Trace.endSection();
    }
}
