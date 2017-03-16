package ivonhoe.dexguard.gradle.utils

import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.objectweb.asm.*

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

import static org.objectweb.asm.Opcodes.*


class Processor {

    public static processExistClass(String dir) {
        String classPath = dir + "/ivonhoe/dexguard/java/Exist.class";
        def file = new File(classPath)
        file.getParentFile().mkdirs()

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
        FileOutputStream fos = new FileOutputStream(classPath);
        fos.write(code);
        fos.close();
    }

    public static processJar(File jarFile, Map map, File dest) {
        if (jarFile) {
            def optJar = new File(jarFile.getParent(), jarFile.name + ".opt")

            def file = new JarFile(jarFile);
            Enumeration enumeration = file.entries();
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(optJar));

            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                String entryName = jarEntry.getName();
                ZipEntry zipEntry = new ZipEntry(entryName);

                InputStream inputStream = file.getInputStream(jarEntry);
                jarOutputStream.putNextEntry(zipEntry);

                String className = entryName.replace(File.separator, ".").replace('.class', "");
                boolean should = shouldProcessClassInJar(entryName);
                if (should && map != null && map.keySet().contains(className)) {
                    String methodName = map.get(className);
                    def bytes = referHackWhenInit(inputStream, methodName)

                    if (bytes) {
                        jarOutputStream.write(bytes);
                    }
                } else {
                    jarOutputStream.write(IOUtils.toByteArray(inputStream));
                }
                jarOutputStream.closeEntry();
            }
            jarOutputStream.close();
            file.close();

            Logger.d("Copying ${optJar.absolutePath} to ${dest.absolutePath}")

            FileUtils.copyFile(optJar, dest)
            if (optJar.exists()) {
                optJar.delete()
            }
        }
    }

    static class InjectCassVisitor extends ClassVisitor {

        private String methodName;

        InjectCassVisitor(int i, ClassVisitor classVisitor, String method) {
            super(i, classVisitor)

            this.methodName = method;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc,
                                         String signature, String[] exceptions) {

            MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
            mv = new MethodVisitor(Opcodes.ASM4, mv) {

                @Override
                void visitCode() {
                    // 在方法体开始调用时
                    if (name.equals(methodName)) {
                        mv.visitMethodInsn(INVOKESTATIC, "ivonhoe/dexguard/java/Exist", "a", "()Z", false);
                        mv.visitMethodInsn(INVOKESTATIC, "ivonhoe/dexguard/java/Exist", "b", "(I)V", false);
                    }
                    super.visitCode()
                }

                @Override
                public void visitMaxs(int maxStack, int maxLocal) {
                    if (name.equals(methodName)) {
                        super.visitMaxs(maxStack + 1, maxLocal);
                    } else {
                        super.visitMaxs(maxStack, maxLocal);
                    }
                }
            }
            return mv;
        }
    }

    //refer hack class when object init
    private static byte[] referHackWhenInit(InputStream inputStream, String methodName) {
        ClassReader cr = new ClassReader(inputStream);
        ClassWriter cw = new ClassWriter(cr, 0);

        ClassVisitor cv = new InjectCassVisitor(Opcodes.ASM4, cw, methodName);
        cr.accept(cv, 0);
        return cw.toByteArray();
    }

    public static boolean shouldProcessClass(String path) {
        return path.endsWith(".class") && !path.contains("/R\$") && !path.endsWith("/R.class") &&
                !path.endsWith("/BuildConfig.class")
    }

    public static boolean shouldProcessJar(File input) {
        if (input && input.isFile()) {
            String path = input.absolutePath;
            return path.endsWith('.jar')
        }

        return false
    }

    private static boolean shouldProcessClassInJar(String entryName) {
        return entryName.endsWith(".class");
    }

    public static byte[] processClass(File file, String method) {
        def optClass = new File(file.getParent(), file.name + ".opt")

        FileInputStream inputStream = new FileInputStream(file);
        FileOutputStream outputStream = new FileOutputStream(optClass)

        def bytes = referHackWhenInit(inputStream, method);
        outputStream.write(bytes)
        inputStream.close()
        outputStream.close()
        if (file.exists()) {
            file.delete()
        }
        optClass.renameTo(file)
        return bytes
    }
}
