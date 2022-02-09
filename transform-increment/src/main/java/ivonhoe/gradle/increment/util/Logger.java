package ivonhoe.gradle.increment.util;

/**
 * @author Ivonhoe on 11/10/21.
 * @email yangfan3687@163.com
 */
public class Logger {

    private static final String PREFIX = "[" + Constants.NAME + "] ";

    private static boolean sEnableLog = false;
    private static boolean sEnableDebug = true;
    private static boolean sAbortOnError = false;

    public static void setConfig(boolean enableDebug, boolean enableLog, boolean abortError) {
        sEnableDebug = enableDebug;
        sEnableLog = enableLog || enableDebug;
        sAbortOnError = abortError;
    }

    public static void debug(String format, Object... args) {
        if (sEnableDebug) {
            System.out.println(format(format, args));
        }
    }

    public static void info(String format, Object... args) {
        if (sEnableLog) {
            System.out.println(format(format, args));
        }
    }

    public static void warn(String format, Object... args) {
        if (sEnableLog) {
            System.err.println(format(format, args));
        }
    }

    public static void error(String format, Object... args) {
        if (sEnableLog) {
            System.err.println(format(format, args));
        }
    }

    public static void error(Throwable t) {
        if (sEnableLog) {
            t.printStackTrace();
        }
    }

    public static void fatal(Throwable t) {
        if (sAbortOnError) {
            if (t instanceof RuntimeException) {
                throw ((RuntimeException) t);
            } else {
                throw new RuntimeException(t);
            }
        } else {
            t.printStackTrace();
        }
    }

    public static void fatal(String format, Object... args) {
        fatal(new RuntimeException(format(format, args)));
    }

    private static String format(String s, Object... args) {
        return PREFIX + (args.length == 0 ? s : String.format(s, args));
    }
}
