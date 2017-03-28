package ivonhoe.dexguard.gradle

import com.android.build.api.transform.*
import com.google.common.collect.Sets
import ivonhoe.dexguard.gradle.utils.Logger
import ivonhoe.dexguard.gradle.utils.MapUtils
import ivonhoe.dexguard.gradle.utils.Processor
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

public class DexguardTransform extends Transform {

    private final Project project;

    public static final Set<QualifiedContent.Scope> SCOPE_FULL_PROJECT = Sets.immutableEnumSet(
            QualifiedContent.Scope.PROJECT,
            QualifiedContent.Scope.PROJECT_LOCAL_DEPS,
            QualifiedContent.Scope.SUB_PROJECTS,
            QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS,
            QualifiedContent.Scope.EXTERNAL_LIBRARIES);

    public static final Set<QualifiedContent.Scope> CONTENT_CLASS = Sets.immutableEnumSet(
            QualifiedContent.DefaultContentType.CLASSES,
    )

    public DexguardTransform(Project target) {
        project = target

        project.extensions.create('dexguard', DexguardExtension)
    }

    @Override
    String getName() {
        return "dexGuard"
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return CONTENT_CLASS
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return SCOPE_FULL_PROJECT
    }

    @Override
    boolean isIncremental() {
        return false;
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs,
                   TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        DexguardExtension extension = project.extensions.findByName("dexguard") as DexguardExtension

        Map hashMap
        def hashFile = new File(extension.guardConfig);
        if (hashFile) {
            hashMap = MapUtils.parseMap(hashFile)
        }

        Logger.d("hashMap:" + hashMap)

        String destDir = null;
        /**
         * 遍历输入文件
         */
        inputs.each { TransformInput input ->

            /**
             * 遍历目录
             */
            input.directoryInputs.each { DirectoryInput directoryInput ->
                /**
                 * 获得产物的目录
                 */
                File dest = outputProvider.getContentLocation(directoryInput.name,
                        directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY);

                String buildTypes = directoryInput.file.name
                String productFlavors = directoryInput.file.parentFile.name
                /**
                 * 遍历文件夹,进行字节码注入
                 */
                traverseFolder(project, directoryInput.file, hashMap, buildTypes, productFlavors)
                Logger.d("Copying ${directoryInput.name} to ${dest.absolutePath}")
                /**
                 * 处理完后拷到目标文件
                 */
                FileUtils.copyDirectory(directoryInput.file, dest);

                destDir = dest.getAbsolutePath();
            }

            /**
             * 遍历jar
             */
            input.jarInputs.each { JarInput jarInput ->

                String path = jarInput.file.absolutePath;
                String destName = path.substring(path.lastIndexOf(File.separator) + 1);
                /**
                 * 重名名输出文件,因为可能同名,会覆盖
                 */
                def hexName = DigestUtils.md5Hex(path);
                if (destName.endsWith(".jar")) {
                    destName = destName.substring(0, destName.length() - 4);
                }

                /**
                 * 获得输出文件
                 */
                File dest = outputProvider.getContentLocation(destName + "_" + hexName,
                        jarInput.contentTypes, jarInput.scopes, Format.JAR);

                /**
                 * 处理jar进行字节码注入
                 */
                if (Processor.shouldProcessJar(jarInput.file)) {
                    Processor.processJar(jarInput.file, hashMap, dest)
                } else {
                    FileUtils.copyFile(jarInput.file, dest);
                    Logger.d("Copying ${jarInput.file.absolutePath} to ${dest.absolutePath}")
                }
            }
        }

        Processor.processExistClass(destDir);
    }

    /**
     * 遍历文件夹进行字节码注入
     * @param project
     * @param rootFile
     * @param destHashFile
     * @param hashMap
     * @param buildType
     * @param productFlavors
     * @param patchDir
     */
    public static void traverseFolder(Project project, File rootFile, Map hashMap,
                                      String buildType, String productFlavors) {

        if (rootFile != null && rootFile.exists()) {
            File[] files = rootFile.listFiles();
            if (files == null || files.length == 0) {
                Logger.w("文件夹是空的!")
                return;
            } else {
                for (File innerFile : files) {
                    if (innerFile.isDirectory()) {
                        Logger.w("不需要处理文件夹:${innerFile.absolutePath},进行递归")
                        traverseFolder(project, innerFile, hashMap, buildType, productFlavors);
                    } else {
                        def classFile = innerFile.absolutePath.split("${productFlavors}/${buildType}/")[1];
                        String className = classFile.replace(File.separator, ".").replace('.class', "");
                        Logger.d("--------" + className)
                        if (Processor.shouldProcessClass(innerFile.absolutePath)) {
                            if (hashMap != null && hashMap.keySet().contains(className)) {
                                // 根据配置的指定类的指定方法，插入字节码
                                String methodName = hashMap.get(className);
                                Logger.d("---method:" + methodName)
                                def bytes = Processor.processClass(innerFile, methodName);

                                Logger.w("需要处理文件:${innerFile.absolutePath}")
                            }
                        } else {
                            Logger.w "不需要处理文件:${innerFile.absolutePath}"
                        }
                    }
                }
            }
        } else {
            project.logger.warn "文件不存在!"
        }
    }
}