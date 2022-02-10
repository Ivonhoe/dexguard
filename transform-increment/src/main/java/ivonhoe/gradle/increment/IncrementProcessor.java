package ivonhoe.gradle.increment;

import com.android.SdkConstants;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import ivonhoe.gradle.increment.util.IOUtil;
import ivonhoe.gradle.increment.util.Logger;
import ivonhoe.gradle.increment.util.ReflectUtil;

/**
 * @author Ivonhoe on 11/10/21.
 * @email yangfan3687@163.com
 */
public class IncrementProcessor {


    private List<Future> futures = new LinkedList<>();
    private Map<File, File> dirInputOutMap = new ConcurrentHashMap<>();
    private Map<File, File> jarInputOutMap = new ConcurrentHashMap<>();
    private ExecutorService executor;
    private IIncrementTransform transform;

    public IncrementProcessor(ExecutorService executor, IIncrementTransform transform) {
        this.executor = executor;
        this.transform = transform;
    }

    public void onTransform(TransformInvocation invocation, boolean isIncremental) throws ExecutionException, InterruptedException {
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
            traceSourceFile(transform, keyFile, valueFile, isIncremental);
        }

        iterator = jarInputOutMap.keySet().iterator();
        while (iterator.hasNext()) {
            File keyFile = iterator.next();
            File valueFile = jarInputOutMap.get(keyFile);

            Logger.debug("jar input output:%s %s", keyFile.getAbsoluteFile(), valueFile.getAbsoluteFile());
            traceJarFile(transform, keyFile, valueFile, isIncremental);
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

    private void traceSourceFile(IIncrementTransform transform, File input, File output, boolean isIncremental) {
        ArrayList<File> classFileList = new ArrayList<>();
        if (input.isDirectory()) {
            listClassFiles(classFileList, input);
        } else {
            classFileList.add(input);
        }

        for (File classFile : classFileList) {
            InputStream is = null;
            FileOutputStream os = null;
            try {
                final String changedFileInputFullPath = classFile.getAbsolutePath();
                final File changedFileOutput = new File(changedFileInputFullPath.replace(input.getAbsolutePath(), output.getAbsolutePath()));
                if (!changedFileOutput.exists()) {
                    changedFileOutput.getParentFile().mkdirs();
                }
                changedFileOutput.createNewFile();

                if (transform.isNeedTraceFile(classFile.getName())) {
                    is = new FileInputStream(classFile);
//                    ClassReader classReader = new ClassReader(is);
//                    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
//                    ClassVisitor classVisitor = new TraceClassAdapter(Opcodes.ASM5, classWriter);
//                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
//                    is.close();

                    if (output.isDirectory()) {
                        os = new FileOutputStream(changedFileOutput);
                    } else {
                        os = new FileOutputStream(output);
                    }
                    os.write(transform.onInputTransform(IOUtil.toByteArray(is), isIncremental, null));
                    os.close();
                } else {
                    IOUtil.copyFileUsingStream(classFile, changedFileOutput);
                }
            } catch (Exception e) {
                Logger.error("[innerTraceMethodFromSrc] input:%s e:%s", input.getName(), e);
                try {
                    Files.copy(input.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            } finally {
                try {
                    is.close();
                    os.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    private void traceJarFile(IIncrementTransform transform, File input, File output, boolean isIncremental) {
        ZipOutputStream zipOutputStream = null;
        ZipFile zipFile = null;
        try {
            zipOutputStream = new ZipOutputStream(new FileOutputStream(output));
            zipFile = new ZipFile(input);
            Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry zipEntry = enumeration.nextElement();
                String zipEntryName = zipEntry.getName();
                if (transform.isNeedTraceFile(zipEntryName)) {
                    InputStream inputStream = zipFile.getInputStream(zipEntry);
//                    ClassReader classReader = new ClassReader(inputStream);
//                    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
//                    ClassVisitor classVisitor = new TraceClassAdapter(Opcodes.ASM5, classWriter);
//                    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
                    byte[] data = transform.onInputTransform(IOUtil.toByteArray(inputStream), isIncremental, null);
                    InputStream byteArrayInputStream = new ByteArrayInputStream(data);
                    ZipEntry newZipEntry = new ZipEntry(zipEntryName);
                    IOUtil.addZipEntry(zipOutputStream, newZipEntry, byteArrayInputStream);
                } else {
                    InputStream inputStream = zipFile.getInputStream(zipEntry);
                    ZipEntry newZipEntry = new ZipEntry(zipEntryName);
                    IOUtil.addZipEntry(zipOutputStream, newZipEntry, inputStream);
                }
            }
        } catch (Exception e) {
            Logger.error("[innerTraceMethodFromJar] input:%s output:%s e:%s", input.getName(), output, e);
            try {
                Files.copy(input.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                if (zipOutputStream != null) {
                    zipOutputStream.finish();
                    zipOutputStream.flush();
                    zipOutputStream.close();
                }
                if (zipFile != null) {
                    zipFile.close();
                }
            } catch (Exception e) {
                Logger.error("close stream err!");
            }
        }
    }

    private void listClassFiles(ArrayList<File> classFiles, File folder) {
        File[] files = folder.listFiles();
        if (null == files) {
            Logger.error("[listClassFiles] files is null! %s", folder.getAbsolutePath());
            return;
        }
        for (File file : files) {
            if (file == null) {
                continue;
            }
            if (file.isDirectory()) {
                listClassFiles(classFiles, file);
            } else {
                if (null != file && file.isFile()) {
                    classFiles.add(file);
                }

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

    private void replaceFile(QualifiedContent input, File newFile) {
        final Field fileField;
        try {
            fileField = ReflectUtil.getDeclaredFieldRecursive(input.getClass(), "file");
            fileField.set(input, newFile);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void replaceChangedFile(DirectoryInput dirInput, Map<File, Status> changedFiles) {
        final Field changedFilesField;
        try {
            changedFilesField = ReflectUtil.getDeclaredFieldRecursive(dirInput.getClass(), "changedFiles");
            changedFilesField.set(dirInput, changedFiles);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private boolean shouldProcessEntry(String entryName) {
        return shouldProcess(entryName);
    }

    private boolean shouldProcess(String classPath) {
        return classPath.endsWith(SdkConstants.DOT_CLASS);
    }
}
