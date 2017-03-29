package ivonhoe.dexguard.gradle.utils

public class Logger {

    private static final boolean LOG_W = true;
    private static final boolean LOG_D = true;

    public static void d(String msg) {
        if (LOG_D) {
            println(msg)
        }
    }

    public static void d(String tag, String msg) {
        if (LOG_D) {
            println(tag + "," + msg)
        }
    }

    public static void w(String msg) {
        if (LOG_W) {
            println(msg)
        }
    }
}