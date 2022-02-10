package ivonhoe.gradle.increment;

import com.android.build.api.transform.Status;

import java.io.File;

/**
 * @author : Ivonhoe
 * @e-mail : yangfan3687@163.com
 * @date : 2022/2/10
 */
public interface IIncrementTransform {

    byte[] onInputTransform(byte[] inputBytes, boolean isIncrement, Status status);

    boolean isNeedTraceFile(String fileName);
}
