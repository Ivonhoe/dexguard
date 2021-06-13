package ivonhoe.dexguard.gradle;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import ivonhoe.dexguard.gradle.utils.Logger;

import static org.objectweb.asm.Opcodes.*;

class DexGuardProcessor {

    public void processExistClass(String dir) {
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

    public void processJar(File jarFile, Map<String, String> map, File dest) {
        if (jarFile != null) {
            File optJar = new File(jarFile.getParent(), jarFile.getName() + ".opt");

            JarFile file = null;
            JarOutputStream jarOutputStream = null;
            try {
                file = new JarFile(jarFile);
                Enumeration enumeration = file.entries();
                jarOutputStream = new JarOutputStream(new FileOutputStream(optJar));

                while (enumeration.hasMoreElements()) {
                    JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                    String entryName = jarEntry.getName();
                    ZipEntry zipEntry = new ZipEntry(entryName);

                    InputStream inputStream = file.getInputStream(jarEntry);
                    jarOutputStream.putNextEntry(zipEntry);

                    String className = entryName.replace(File.separator, ".").replace(".class", "");
                    boolean should = shouldProcessClassInJar(entryName);
                    if (should && map != null && map.containsKey(className)) {
                        String methodName = map.get(className);
                        byte[] bytes = referHackWhenInit(inputStream, methodName);

                        if (bytes != null) {
                            jarOutputStream.write(bytes);
                        }
                    } else {
                        jarOutputStream.write(IOUtils.toByteArray(inputStream));
                    }
                    jarOutputStream.closeEntry();
                }

                Logger.d("Copying ${optJar.absolutePath} to ${dest.absolutePath}");

                FileUtils.copyFile(optJar, dest);
                if (optJar.exists()) {
                    optJar.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (file != null) {
                        file.close();
                    }
                    if (jarOutputStream != null) {
                        jarOutputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class InjectCassVisitor extends ClassVisitor {

        private String methodName;

        InjectCassVisitor(int i, ClassVisitor classVisitor, String method) {
            super(i, classVisitor);

            this.methodName = method;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {

            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            mv = new MethodVisitor(ASM6, mv) {

                @Override
                public void visitCode() {
                    // 在方法体开始调用时
                    if (name.equals(methodName)) {
                        mv.visitMethodInsn(INVOKESTATIC, Constants.CLASS_EXIST_NAME, "a",
                                "()Z", false);
                        mv.visitMethodInsn(INVOKESTATIC, Constants.CLASS_EXIST_NAME, "b",
                                "(I)V", false);
                    }
                    super.visitCode();
                }

                @Override
                public void visitMaxs(int maxStack, int maxLocal) {
                    if (name.equals(methodName)) {
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
    private byte[] referHackWhenInit(InputStream inputStream, String methodName) {
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

    public byte[] processClass(File file, String method) {
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
