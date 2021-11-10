package ivonhoe.gradle.increment.util;

import com.android.SdkConstants;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.TransformOutputProvider;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;

/**
 * @author Ivonhoe on 11/10/21.
 * @email yangfan3687@163.com
 */
public class Utils {

    public static File getHexDest(JarInput jarInput, TransformOutputProvider outputProvider) {
        String destName = jarInput.getFile().getName();
        /* 重名名输出文件,因为可能同名,会覆盖*/
        String hexName = DigestUtils.md5Hex(jarInput.getFile().getAbsolutePath()).substring(0, 8);
        if (destName.endsWith(".jar")) {
            destName = destName.substring(0, destName.length() - 4);
        }
        /*获得输出文件*/
        return outputProvider.getContentLocation(destName + "_" + hexName,
                jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
    }

    /**
     * [prefix]com/xxx/aaa.class --> com/xxx/aaa
     * [prefix]com\xxx\aaa.class --> com\xxx\aaa
     */
    public static String trimName(String s, int start) {
        return s.substring(start, s.length() - SdkConstants.DOT_CLASS.length());
    }
}
