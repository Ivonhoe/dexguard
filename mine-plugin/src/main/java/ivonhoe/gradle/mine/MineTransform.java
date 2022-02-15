package ivonhoe.gradle.mine;

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;

import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ivonhoe.gradle.increment.IIncrementTransform;
import ivonhoe.gradle.increment.IncrementProcessor;
import ivonhoe.gradle.increment.util._Constants;
import ivonhoe.gradle.mine.extension.MineExtension;
import ivonhoe.gradle.mine.model.TraceMethod;

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
        return "mineTransform";
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
        ClassReader classReader = new ClassReader(inputBytes);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassVisitor classVisitor = new TraceClassAdapter(Opcodes.ASM5, classWriter);
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
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

    private class TraceClassAdapter extends ClassVisitor {

        private String className;
        private boolean isABSClass = false;
        private boolean hasWindowFocusMethod = false;
        private boolean isActivityOrSubClass;
        private boolean isNeedTrace;

        TraceClassAdapter(int i, ClassVisitor classVisitor) {
            super(i, classVisitor);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            super.visit(version, access, name, signature, superName, interfaces);
            this.className = name;
            this.isActivityOrSubClass = true;//isActivityOrSubClass(className, collectedClassExtendMap);
            this.isNeedTrace = true;// MethodCollector.isNeedTrace(configuration, className, mappingCollector);
            if ((access & Opcodes.ACC_ABSTRACT) > 0 || (access & Opcodes.ACC_INTERFACE) > 0) {
                this.isABSClass = true;
            }
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {
            if (isABSClass) {
                return super.visitMethod(access, name, desc, signature, exceptions);
            } else {
                if (!hasWindowFocusMethod) {
//                    hasWindowFocusMethod = MethodCollector.isWindowFocusChangeMethod(name, desc);
                }
                MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
                return new TraceMethodAdapter(api, methodVisitor, access, name, desc, this.className,
                        hasWindowFocusMethod, isActivityOrSubClass, isNeedTrace);
            }
        }

        @Override
        public void visitEnd() {
            if (!hasWindowFocusMethod && isActivityOrSubClass && isNeedTrace) {
//                insertWindowFocusChangeMethod(cv, className);
            }
            super.visitEnd();
        }
    }

    private class TraceMethodAdapter extends AdviceAdapter {

        private final String methodName;
        private final String name;
        private final String className;
        private final boolean hasWindowFocusMethod;
        private final boolean isNeedTrace;
        private final boolean isActivityOrSubClass;

        protected TraceMethodAdapter(int api, MethodVisitor mv, int access, String name, String desc, String className,
                                     boolean hasWindowFocusMethod, boolean isActivityOrSubClass, boolean isNeedTrace) {
            super(api, mv, access, name, desc);
            TraceMethod traceMethod = TraceMethod.create(0, access, className, name, desc);
            this.methodName = traceMethod.getMethodName();
            this.hasWindowFocusMethod = hasWindowFocusMethod;
            this.className = className;
            this.name = name;
            this.isActivityOrSubClass = isActivityOrSubClass;
            this.isNeedTrace = isNeedTrace;
        }

        @Override
        protected void onMethodEnter() {
            // 插入systrace的调用
            String sectionName = methodName;
            int length = sectionName.length();
            if (length > Constants.MAX_SECTION_NAME_LEN) {
                // 先去掉参数
                int parmIndex = sectionName.indexOf('(');
                sectionName = sectionName.substring(0, parmIndex);
                // 如果依然更大，直接裁剪
                length = sectionName.length();
                if (length > Constants.MAX_SECTION_NAME_LEN) {
                    sectionName = sectionName.substring(length - Constants.MAX_SECTION_NAME_LEN);
                }
            }
            mv.visitLdcInsn(sectionName);
            mv.visitMethodInsn(INVOKESTATIC, Constants.TRACE_SYSTRACE_CLASS, "begin", "(Ljava/lang/String;)V", false);
        }

        // pass jni method trace, because its super method can cover it
        /*@Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            String nativeMethodName = owner.replace("/", ".") + "." + name;
            TraceMethod traceMethod = collectedMethodMap.get(nativeMethodName);
            if (traceMethod != null && traceMethod.isNativeMethod()) {
                traceMethodCount.incrementAndGet();
                mv.visitLdcInsn(traceMethod.id);
                mv.visitMethodInsn(INVOKESTATIC, TraceBuildConstants.MATRIX_TRACE_CLASS, "i", "(I)V", false);
            }
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (traceMethod != null && traceMethod.isNativeMethod()) {
                traceMethodCount.incrementAndGet();
                mv.visitLdcInsn(traceMethod.id);
                mv.visitMethodInsn(INVOKESTATIC, TraceBuildConstants.MATRIX_TRACE_CLASS, "o", "(I)V", false);
            }
        }*/

        @Override
        protected void onMethodExit(int opcode) {
            // 插入systrace的调用
            mv.visitMethodInsn(INVOKESTATIC, Constants.TRACE_SYSTRACE_CLASS, "end", "()V", false);
        }
    }
}
