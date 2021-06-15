package ivonhoe.dexguard.gradle;

import com.google.common.collect.ImmutableSet;

import com.android.build.api.transform.Context;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ivonhoe.dexguard.gradle.utils.Logger;
import ivonhoe.dexguard.gradle.utils.MapUtils;

import static com.android.build.api.transform.QualifiedContent.DefaultContentType.CLASSES;

public class DexGuardTransform extends Transform {

    private static final String TAG = "DexguardTransform";

    private Project mProject;

    private DexGuardProcessor mDexGuardProcessor;

    private Map<String, List<String>> guardMethodMap;

    public DexGuardTransform(Project project) {
        mProject = project;
        mDexGuardProcessor = new DexGuardProcessor();
        mProject.getExtensions().create("dexguard", DexGuardExtension.class);
    }

    @Override
    public String getName() {
        return "dexGuard";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        if (transformInvocation == null || transformInvocation.getInputs() == null) {
            return;
        }

        DexGuardExtension guardExtension = (DexGuardExtension) mProject.getExtensions().findByName("dexguard");
        guardMethodMap = MapUtils.parseMap(new File(guardExtension.guardConfig));

        String destDir = null;
        for (TransformInput input : transformInvocation.getInputs()) {
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                File destFile = getOutputDirectoryDestFile(directoryInput, transformInvocation);
                destDir = destFile.getAbsolutePath();

                /*
                  遍历文件夹,进行字节码注入
                 */
                File directoryFile = directoryInput.getFile();
                String buildTypes = directoryFile.getName();
                String productFlavors = directoryFile.getParentFile().getName();
                traverseFolder(mProject, directoryFile, guardMethodMap, buildTypes, productFlavors);
                Logger.d("Copying ${directoryInput.name} to ${dest.absolutePath}");

                /*
                 * 处理完后拷到目标文件
                 */
                FileUtils.copyDirectory(directoryInput.getFile(), destFile);
            }

            for (JarInput jarInput : input.getJarInputs()) {
                File destFile = getOutputJarDestFile(jarInput, transformInvocation);
                File jarInputFile = jarInput.getFile();
                /*
                 * 处理jar进行字节码注入
                 */
                if (mDexGuardProcessor.shouldProcessJar(jarInputFile)) {
                    mDexGuardProcessor.processJar(jarInput, destFile, guardMethodMap);
                } else {
                    FileUtils.copyFile(jarInputFile, destFile);
                    Logger.d("Copying ${jarInput.file.absolutePath} to ${dest.absolutePath}");
                }
            }
        }

        mDexGuardProcessor.processExistClass(destDir);
    }

    private File getOutputDirectoryDestFile(DirectoryInput directoryInput, TransformInvocation transformInvocation) {
        /*
         * 获得产物的目录
         */
        return transformInvocation.getOutputProvider().getContentLocation(directoryInput.getName(),
                directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
    }

    private File getOutputJarDestFile(JarInput jarInput, TransformInvocation transformInvocation) {
        String inputPath = jarInput.getFile().getAbsolutePath();
        String destName = inputPath.substring(inputPath.lastIndexOf(File.separator) + 1);

        /*
         * 重命名输出文件,因为可能同名,会覆盖
         */
        String hexName = DigestUtils.md5Hex(inputPath);
        if (destName.endsWith(".jar")) {
            destName = destName.substring(0, destName.length() - 4);
        }

        /*
         * 获得输出文件
         */
        return transformInvocation.getOutputProvider().getContentLocation(
                destName + "_" + hexName,
                jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
    }

    /**
     * 遍历文件夹进行字节码注入
     */
    private void traverseFolder(Project project, File rootFile, Map<String, List<String>> hashMap,
                                String buildType, String productFlavors) {

        if (rootFile != null && rootFile.exists()) {
            File[] files = rootFile.listFiles();
            if (files == null || files.length == 0) {
                Logger.w("file empty!");
            } else {
                for (File innerFile : files) {
                    if (innerFile.isDirectory()) {
                        traverseFolder(project, innerFile, hashMap, buildType, productFlavors);
                    } else {
                        if (mDexGuardProcessor.shouldProcessClass(innerFile.getAbsolutePath())) {
                            Logger.d(TAG, innerFile.getAbsolutePath());
                            String clazzAbsolutePath = innerFile.getAbsolutePath();
                            String separator = productFlavors + Constants.getPathSeparator() + buildType;
                            int separatorIndex = clazzAbsolutePath.indexOf(separator);
                            int start = separatorIndex + separator.length() + 1;
                            int end = clazzAbsolutePath.length();
                            String classFile = clazzAbsolutePath.substring(start, end);
                            String className = classFile.replace(File.separator, ".").replace(".class", "");
                            if (hashMap != null && hashMap.containsKey(className)) {
                                // 根据配置的指定类的指定方法，插入字节码
                                List<String> methodName = hashMap.get(className);
                                Logger.d(TAG, "className:" + className + " ,method:" + methodName);
                                mDexGuardProcessor.processClass(innerFile, methodName);

                                Logger.d(TAG, "invoked class absolute path:${innerFile.absolutePath}");
                            }
                        } else {
                            Logger.w("不需要处理文件:${innerFile.absolutePath}");
                        }
                    }
                }
            }
        } else {
            assert rootFile != null;
            project.getLogger().warn("rootFile not exists:" + rootFile.getAbsolutePath());
        }
    }
}
