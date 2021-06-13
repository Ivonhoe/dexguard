package ivonhoe.dexguard.gradle;

import com.google.common.collect.ImmutableSet;

import com.android.build.api.transform.Context;
import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformOutputProvider;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import ivonhoe.dexguard.gradle.utils.Logger;

import static com.android.build.api.transform.QualifiedContent.DefaultContentType.CLASSES;

public class DexGuardTransform extends Transform {

    private static final String TAG = "DexguardTransform";

    private final Project mProject;

    private DexGuardProcessor mDexGuardProcessor;

    public static final Set<QualifiedContent.Scope> SCOPE_FULL_PROJECT = ImmutableSet.of(
            QualifiedContent.Scope.PROJECT,
            QualifiedContent.Scope.PROJECT_LOCAL_DEPS,
            QualifiedContent.Scope.SUB_PROJECTS,
            QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS,
            QualifiedContent.Scope.EXTERNAL_LIBRARIES);

    public static final Set<QualifiedContent.ContentType> CONTENT_CLASS = ImmutableSet.of(CLASSES);

    public DexGuardTransform(Project project) {
        mProject = project;
        mDexGuardProcessor = new DexGuardProcessor();
        project.getExtensions().create("dexguard", DexGuardExtension.class);
    }

    @Override
    public String getName() {
        return "dexguard";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return CONTENT_CLASS;
    }

    @Override
    public Set<QualifiedContent.Scope> getScopes() {
        return SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(Context context, Collection<TransformInput> inputs,
                          Collection<TransformInput> referencedInputs,
                          TransformOutputProvider outputProvider,
                          boolean isIncremental) {
        if (inputs.isEmpty()) {
            return;
        }

        String destDir = null;
        for (TransformInput input : inputs) {
            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                /*
                 * 获得产物的目录
                 */
                File dest = outputProvider.getContentLocation(directoryInput.getName(),
                        directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);

                String buildTypes = directoryInput.getFile().getName();
                String productFlavors = directoryInput.getFile().getParentFile().getName();
                /*
                  遍历文件夹,进行字节码注入
                 */
                traverseFolder(mProject, directoryInput.getFile(), null, buildTypes,
                        productFlavors);
                Logger.d("Copying ${directoryInput.name} to ${dest.absolutePath}");
                /*
                 * 处理完后拷到目标文件
                 */
                try {
                    FileUtils.copyDirectory(directoryInput.getFile(), dest);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                destDir = dest.getAbsolutePath();
            }

            for (JarInput jarInput : input.getJarInputs()) {
                String path = jarInput.getFile().getAbsolutePath();
                String destName = path.substring(path.lastIndexOf(File.separator) + 1);
                /*
                 * 重名名输出文件,因为可能同名,会覆盖
                 */
                String hexName = DigestUtils.md5Hex(path);
                if (destName.endsWith(".jar")) {
                    destName = destName.substring(0, destName.length() - 4);
                }

                /*
                 * 获得输出文件
                 */
                File dest = outputProvider.getContentLocation(destName + "_" + hexName,
                        jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);

                /*
                 * 处理jar进行字节码注入
                 */
                if (mDexGuardProcessor.shouldProcessJar(jarInput.getFile())) {
                    mDexGuardProcessor.processJar(jarInput.getFile(), null, dest);
                } else {
                    try {
                        FileUtils.copyFile(jarInput.getFile(), dest);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Logger.d("Copying ${jarInput.file.absolutePath} to ${dest.absolutePath}");
                }
            }
        }

        mDexGuardProcessor.processExistClass(destDir);
    }

    /**
     * 遍历文件夹进行字节码注入
     */
    private void traverseFolder(Project project, File rootFile, Map<String, String> hashMap,
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
                            String separator = productFlavors + getPathSeparator() + buildType;
                            int separatorIndex = clazzAbsolutePath.indexOf(separator);
                            int start = separatorIndex + separator.length() + 1;
                            int end = clazzAbsolutePath.length();
                            String classFile = clazzAbsolutePath.substring(start, end);
                            String className = classFile.replace(File.separator, ".").replace(".class", "");
                            if (hashMap != null && hashMap.containsKey(className)) {
                                // 根据配置的指定类的指定方法，插入字节码
                                String methodName = hashMap.get(className);
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

    private static String getPathSeparator() {
        return Os.isFamily(Os.FAMILY_WINDOWS) ? "\\" : File.separator;
    }
}
