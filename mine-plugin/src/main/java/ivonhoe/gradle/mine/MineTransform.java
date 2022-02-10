package ivonhoe.gradle.mine;

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.gradle.api.Project;

import java.io.File;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ivonhoe.gradle.increment.IIncrementTransform;
import ivonhoe.gradle.increment.IncrementProcessor;
import ivonhoe.gradle.increment.util._Constants;
import ivonhoe.gradle.mine.bean.MineExtension;

/**
 * @author Ivonhoe on 11/7/21.
 * @email yangfan3687@163.com
 */
public class MineTransform extends Transform implements IIncrementTransform {

    private Project mProject;
    private MineExtension mMineExtension;

    private IncrementProcessor mIncrementProcessor;
    private ExecutorService executor = Executors.newFixedThreadPool(16);

    public MineTransform(Project project) {
        mProject = project;
        mIncrementProcessor = new IncrementProcessor(executor, this);
        mMineExtension = mProject.getExtensions().create("mine", MineExtension.class);
    }

    @Override
    public String getName() {
        return "MineTransform";
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

    @Override
    public void transform(TransformInvocation transformInvocation) throws InterruptedException {
        final boolean isIncremental = transformInvocation.isIncremental() && this.isIncremental();

        try {
            mIncrementProcessor.onTransform(transformInvocation, isIncremental);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] onInputTransform(byte[] inputBytes, boolean isIncrement, Status status) {
        return inputBytes;
    }

    @Override
    public boolean isNeedTraceFile(String fileName) {
        if (fileName.endsWith(".class")) {
            for (String unTraceCls : _Constants.UN_TRACE_CLASS) {
                if (fileName.contains(unTraceCls)) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }
}
