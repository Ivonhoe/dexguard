package ivonhoe.gradle.mine;

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.gradle.api.Project;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ivonhoe.gradle.increment.IncrementTransform;
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
        return true;
    }

    private IncrementTransform mIncrementTransform = new IncrementTransform();
    private ExecutorService executor = Executors.newFixedThreadPool(16);

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        final boolean isIncremental = transformInvocation.isIncremental() && this.isIncremental();

        try {
            mIncrementTransform.onTransform(executor, transformInvocation, isIncremental);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}
