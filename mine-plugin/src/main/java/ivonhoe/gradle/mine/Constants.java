package ivonhoe.gradle.mine;

import org.apache.tools.ant.taskdefs.condition.Os;

import java.io.File;

public class Constants {

    public static final String MAP_SEPARATOR = ":";
    public static final int MAX_SECTION_NAME_LEN = 127;

    public static final String TRACE_SYSTRACE_CLASS = "ivonhoe/android/mine/trace/SystemTrace";

    public static String getPathSeparator() {
        return Os.isFamily(Os.FAMILY_WINDOWS) ? "\\" : File.separator;
    }
}
