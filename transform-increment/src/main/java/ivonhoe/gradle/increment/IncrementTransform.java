package ivonhoe.gradle.increment;

import com.android.SdkConstants;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import ivonhoe.gradle.increment.util.Logger;

/**
 * @author Ivonhoe on 11/10/21.
 * @email yangfan3687@163.com
 */
public class IncrementTransform {

    private List<Future> futures = new LinkedList<>();
    private Map<File, File> dirInputOutMap = new ConcurrentHashMap<>();
    private Map<File, File> jarInputOutMap = new ConcurrentHashMap<>();

    public void onTransform(ExecutorService executor, TransformInvocation invocation, boolean isIncremental) throws ExecutionException, InterruptedException {
        Logger.debug("isIncremental:" + isIncremental);

        traverseDirectoryInput(executor, invocation, isIncremental);
        traverseJarInput(executor, invocation, isIncremental);

        for (Future future : futures) {
            future.get();
        }
        futures.clear();

        Iterator<File> iterator = dirInputOutMap.keySet().iterator();
        while (iterator.hasNext()) {
            File keyFile = iterator.next();
            File valueFile = dirInputOutMap.get(keyFile);

            Logger.debug("dir input output:%s %s", keyFile.getAbsoluteFile(), valueFile.getAbsoluteFile());
        }

        iterator = jarInputOutMap.keySet().iterator();
        while (iterator.hasNext()) {
            File keyFile = iterator.next();
            File valueFile = jarInputOutMap.get(keyFile);

            Logger.debug("jar input output:%s %s", keyFile.getAbsoluteFile(), valueFile.getAbsoluteFile());
        }
    }

    private void traverseDirectoryInput(ExecutorService executor, TransformInvocation invocation, boolean isIncremental) {
        for (TransformInput input : invocation.getInputs()) {
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        File srcFile = directoryInput.getFile();
                        File destFile = invocation.getOutputProvider().getContentLocation(
                                directoryInput.getName(), directoryInput.getContentTypes(),
                                directoryInput.getScopes(), Format.DIRECTORY);

                        final String inputFullPath = srcFile.getAbsolutePath();
                        final String outputFullPath = destFile.getAbsolutePath();

                        if (isIncremental) {
                            Map<File, Status> fileStatusMap = directoryInput.getChangedFiles();
                            final Map<File, Status> outChangedFiles = new HashMap<>();

                            for (Map.Entry<File, Status> entry : fileStatusMap.entrySet()) {
                                final Status status = entry.getValue();
                                final File changedFileInput = entry.getKey();

                                final String changedFileInputFullPath = changedFileInput.getAbsolutePath();
                                final File changedFileOutput = new File(changedFileInputFullPath.replace(inputFullPath, outputFullPath));

                                if (status == Status.ADDED || status == Status.CHANGED) {
                                    dirInputOutMap.put(changedFileInput, changedFileOutput);
                                } else if (status == Status.REMOVED) {
                                    changedFileOutput.delete();
                                }
                                outChangedFiles.put(changedFileOutput, status);
                            }
                        } else {
                            dirInputOutMap.put(srcFile, destFile);
                        }
                    }
                };

                futures.add(executor.submit(runnable));
            }
        }
    }

    private void traverseJarInput(ExecutorService executor, TransformInvocation invocation, boolean isIncremental) {
        for (TransformInput input : invocation.getInputs()) {
            for (JarInput jarInput : input.getJarInputs()) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        File srcFile = jarInput.getFile();
                        File outputFile = invocation.getOutputProvider().getContentLocation(jarInput.getName(),
                                jarInput.getContentTypes(),
                                jarInput.getScopes(),
                                Format.JAR);

                        if (isIncremental) {
                            if (jarInput.getStatus() == Status.ADDED || jarInput.getStatus() == Status.CHANGED) {
                                jarInputOutMap.put(srcFile, outputFile);
                            } else if (jarInput.getStatus() == Status.REMOVED) {
                                outputFile.delete();
                            }
                        } else {
                            jarInputOutMap.put(srcFile, outputFile);
                        }
                    }
                };

                futures.add(executor.submit(runnable));
            }
        }
    }

    private void traverseFolder(File rootFile) {
        if (rootFile != null && rootFile.exists()) {
            File[] files = rootFile.listFiles();
            if (files == null || files.length == 0) {
                Logger.debug("file empty!");
            } else {
                for (File innerFile : files) {
                    if (shouldProcess(innerFile.getAbsolutePath())) {
                        String clazzAbsolutePath = innerFile.getAbsolutePath();
                        Logger.debug("className:" + clazzAbsolutePath);
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
