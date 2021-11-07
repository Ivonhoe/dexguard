package ivonhoe.gradle.mine;

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.gradle.api.Project;

import java.io.IOException;
import java.util.Set;

import ivonhoe.gradle.mine.bean.MineExtension;

/**
 * @author Ivonhoe on 11/7/21.
 * @email yangfan3687@163.com
 */
public class MineTransform extends Transform {

    private Project mProject;
    private MineExtension mMineExtension;

    public MineTransform(Project project) {
        this.mProject = project;
        mMineExtension = mProject.getExtensions().create("mine", MineExtension.class);
    }

    @Override
    public String getName() {
        return "mine";
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
//        if (transformInvocation == null || transformInvocation.getInputs() == null) {
//            return;
//        }
//
//        String destDir = null;
//        for (TransformInput input : transformInvocation.getInputs()) {
//            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
//                File destFile = getOutputDirectoryDestFile(directoryInput, transformInvocation);
//                destDir = destFile.getAbsolutePath();
//
//                /*
//                  遍历文件夹,进行字节码注入
//                 */
//                File directoryFile = directoryInput.getFile();
//                String buildTypes = directoryFile.getName();
//                String productFlavors = directoryFile.getParentFile().getName();
//                traverseFolder(mProject, directoryFile, guardMethodMap, buildTypes, productFlavors);
//                Logger.d("Copying ${directoryInput.name} to ${dest.absolutePath}");
//
//                /*
//                 * 处理完后拷到目标文件
//                 */
//                FileUtils.copyDirectory(directoryInput.getFile(), destFile);
//            }
//
//            for (JarInput jarInput : input.getJarInputs()) {
//                File destFile = getOutputJarDestFile(jarInput, transformInvocation);
//                File jarInputFile = jarInput.getFile();
//                /*
//                 * 处理jar进行字节码注入
//                 */
//                if (mDexGuardProcessor.shouldProcessJar(jarInputFile)) {
//                    mDexGuardProcessor.processJar(jarInput, destFile, guardMethodMap);
//                } else {
//                    FileUtils.copyFile(jarInputFile, destFile);
//                    Logger.d("Copying ${jarInput.file.absolutePath} to ${dest.absolutePath}");
//                }
//            }
//        }
    }
}
