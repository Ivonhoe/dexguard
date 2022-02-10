package ivonhoe.android.mine.trace;

import ivonhoe.android.mine.util.Utils;

/**
 * @author Ivonhoe on 11/7/21.
 * @email yangfan3687@163.com
 */
public class LongTimeTrace {

    public static long methodStart() {
        return System.currentTimeMillis();
    }

    public static void methodEnd(boolean mainThreadOnly, long startTime) {
        if (mainThreadOnly && !Utils.isMainThread()) {
            return;
        }

        Throwable throwable = new Throwable();
        throwable.printStackTrace();

    }
}
