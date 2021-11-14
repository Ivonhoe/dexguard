package ivonhoe.gradle.increment;

import com.android.SdkConstants;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import ivonhoe.gradle.increment.util.Logger;

/**
 * @author Ivonhoe on 11/10/21.
 * @email yangfan3687@163.com
 */
public class IncrementTransform {

    public void onTransform(TransformInvocation invocation) {
        if (!invocation.isIncremental()) {
            try {
                invocation.getOutputProvider().deleteAll();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (TransformInput input : invocation.getInputs()) {
            input.getJarInputs().forEach(jarInput -> {
                File src = jarInput.getFile();
                File dst = invocation.getOutputProvider().getContentLocation(jarInput.getName(),
                        jarInput.getContentTypes(),
                        jarInput.getScopes(),
                        Format.JAR);

                try {
                    scanJarFile(src);
                    FileUtils.copyFile(src, dst);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            input.getDirectoryInputs().forEach(directoryInput -> {
                File destFile = invocation.getOutputProvider().getContentLocation(
                        directoryInput.getName(), directoryInput.getContentTypes(),
                        directoryInput.getScopes(), Format.DIRECTORY);

                try {
                    traverseFolder(directoryInput.getFile());
                    FileUtils.copyDirectory(directoryInput.getFile(), destFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void traverseFolder(File rootFile) throws IOException {
        if (rootFile != null && rootFile.exists()) {
            File[] files = rootFile.listFiles();
            if (files == null || files.length == 0) {
                Logger.debug("file empty!");
            } else {
                for (File innerFile : files) {
                    if (shouldProcess(innerFile.getAbsolutePath())) {
                        String clazzAbsolutePath = innerFile.getAbsolutePath();
                        Logger.debug("className:" + clazzAbsolutePath);
                        //scanClass(new FileInputStream(innerFile));
                    } else {
                        Logger.debug("不需要处理文件:${innerFile.absolutePath}");
                    }
                }
            }
        } else {
            assert rootFile != null;
            Logger.debug("rootFile not exists:" + rootFile.getAbsolutePath());
        }
    }

    private void scanJarFile(File file) throws IOException {
        JarFile jarFile = new JarFile(file);

        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            InputStream inputStream = jarFile.getInputStream(entry);
            boolean should = shouldProcessEntry(name);
            if (should) {
                //scanClass(inputStream);
            }
        }
    }

    private boolean shouldProcessEntry(String entryName) {
        return shouldProcess(entryName);
    }

    private boolean shouldProcess(String classPath) {
        return classPath.endsWith(SdkConstants.DOT_CLASS);
    }
}
