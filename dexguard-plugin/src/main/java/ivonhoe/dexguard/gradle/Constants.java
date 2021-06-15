package ivonhoe.dexguard.gradle;

import org.apache.tools.ant.taskdefs.condition.Os;

import java.io.File;

public class Constants {

    public static final String PACKAGE_NAME = "ivonhoe/dexguard/java";
    public static final String CLASS_EXIST_NAME = "ivonhoe/dexguard/java/Exist";
    public static final String MAP_SEPARATOR = ":";

    public static String getPathSeparator() {
        return Os.isFamily(Os.FAMILY_WINDOWS) ? "\\" : File.separator;
    }
}
