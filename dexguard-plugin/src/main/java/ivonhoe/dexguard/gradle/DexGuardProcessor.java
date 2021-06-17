package ivonhoe.dexguard.gradle;

import com.android.build.api.transform.JarInput;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import ivonhoe.dexguard.gradle.utils.Logger;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM6;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.RETURN;

class DexGuardProcessor {

    public void processExistClass(String dir) {
        if (dir == null) {
            return;
        }
        String classPath = dir + "/ivonhoe/dexguard/java/Exist.class";
        File file = new File(classPath);
        file.getParentFile().mkdirs();

        ClassWriter cw = new ClassWriter(0);
        FieldVisitor fv;
        MethodVisitor mv;
        AnnotationVisitor av0;

        cw.visit(51, ACC_PUBLIC + ACC_SUPER, "ivonhoe/dexguard/java/Exist", null, "java/lang/Object", null);

        cw.visitSource("Exist.java", null);

        mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(7, l0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLocalVariable("this", "Livonhoe/dexguard/java/Exist;", null, l0, l1, 0);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "a", "()Z", null, null);
        mv.visitCode();
        l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(10, l0);
        mv.visitInsn(ICONST_0);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "b", "(I)V", null, null);
        mv.visitCode();
        l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(14, l0);
        mv.visitInsn(RETURN);
        l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLocalVariable("test", "I", null, l0, l1, 0);
        mv.visitMaxs(0, 1);
        mv.visitEnd();

        cw.visitEnd();

        // 获取生成的class文件对应的二进制流
        byte[] code = cw.toByteArray();

        //将二进制流写到本地磁盘上
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(classPath);
            fos.write(code);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 遍历jar包检查是否是需要保护的方法
     *
     * @param jarInput 输入的jar文件
     * @param destFile 输出的jar文件
     * @param map      需要保护的方法信息
     */
    public void processJar(JarInput jarInput, File destFile, Map<String, List<String>> map) {
        if (jarInput == null || jarInput.getFile() == null || jarInput.getFile().length() == 0) {
            return;
        }
        JarFile jarInputFile = null;
        JarOutputStream jarOutputStream = null;
        try {
            // 构建 JarFile 文件
            jarInputFile = new JarFile(jarInput.getFile());
            jarOutputStream = new JarOutputStream(new FileOutputStream(destFile));

            Enumeration enumeration = jarInputFile.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement();

                String entryName = jarEntry.getName();
                JarEntry outputJarEntry = new JarEntry(entryName);

                InputStream inputStream = jarInputFile.getInputStream(jarEntry);
                jarOutputStream.putNextEntry(outputJarEntry);

                String className = entryName.replace(File.separator, ".").replace(".class", "");
                boolean should = shouldProcessClassInJar(entryName);
                if (should && map != null && map.containsKey(className)) {
                    List<String> methodName = map.get(className);
                    byte[] bytes = referHackWhenInit(inputStream, methodName);

                    if (bytes != null) {
                        jarOutputStream.write(bytes);
                    }
                } else {
                    jarOutputStream.write(IOUtils.toByteArray(inputStream));
                }
                jarOutputStream.flush();
                jarOutputStream.closeEntry();
            }

            Logger.d("Copying ${optJar.absolutePath} to ${dest.absolutePath}");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (jarInputFile != null) {
                    jarInputFile.close();
                }
                if (jarOutputStream != null) {
                    jarOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class InjectCassVisitor extends ClassVisitor {

        private List<String> methodNameList;

        InjectCassVisitor(int i, ClassVisitor classVisitor, List<String> method) {
            super(i, classVisitor);

            this.methodNameList = method;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {

            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            mv = new MethodVisitor(ASM6, mv) {

                boolean withMethodGuardAnnotation = false;

                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (desc != null && desc.equals(Constants.ANNOTATION_METHOD_GUARD)) {
                        withMethodGuardAnnotation = true;
                    }

                    return super.visitAnnotation(desc, visible);
                }

                @Override
                public void visitCode() {
                    // 在方法体开始调用时
                    if (withMethodGuardAnnotation || (methodNameList != null && methodNameList.contains(name))) {
                        mv.visitMethodInsn(INVOKESTATIC, Constants.CLASS_EXIST_NAME, "a", "()Z", false);
                        mv.visitMethodInsn(INVOKESTATIC, Constants.CLASS_EXIST_NAME, "b", "(I)V", false);
                    }
                    super.visitCode();
                }

                @Override
                public void visitMaxs(int maxStack, int maxLocal) {
                    if (withMethodGuardAnnotation || (methodNameList != null && methodNameList.contains(name))) {
                        super.visitMaxs(maxStack + 1, maxLocal);
                    } else {
                        super.visitMaxs(maxStack, maxLocal);
                    }
                }
            };
            return mv;
        }
    }

    //refer hack class when object init
    private byte[] referHackWhenInit(InputStream inputStream, List<String> methodName) {
        ClassReader cr = null;
        try {
            cr = new ClassReader(inputStream);
            ClassWriter cw = new ClassWriter(cr, 0);

            ClassVisitor cv = new InjectCassVisitor(ASM6, cw, methodName);
            cr.accept(cv, 0);
            return cw.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean shouldProcessClass(String path) {
        if (path.startsWith("android")) {
            return false;
        }
        return path.endsWith(".class") && !path.contains("/R\\$") && !path.endsWith("/R.class") &&
                !path.endsWith("/BuildConfig.class");
    }

    public boolean shouldProcessJar(File input) {
        if (input != null && input.isFile()) {
            String path = input.getAbsolutePath();
            return path.endsWith(".jar");
        }

        return false;
    }

    private boolean shouldProcessClassInJar(String entryName) {
        return entryName.endsWith(".class");
    }

    /**
     * 对单个class文件进行处理
     */
    public byte[] processClass(File file, List<String> method) {
        File optClass = new File(file.getParent(), file.getName() + ".opt");

        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        byte[] bytes = null;
        try {
            inputStream = new FileInputStream(file);
            outputStream = new FileOutputStream(optClass);

            bytes = referHackWhenInit(inputStream, method);
            if (bytes != null) {
                outputStream.write(bytes);
            }

            if (file.exists()) {
                file.delete();
            }
            optClass.renameTo(file);
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return bytes;
    }
}
