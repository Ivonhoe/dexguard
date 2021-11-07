package ivonhoe.andrid.mine.util;

import android.os.Looper;

/**
 * @author Ivonhoeon 11/7/21.
 * @email yangfan3687@163.com
 */
public class Utils {

    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }
}
