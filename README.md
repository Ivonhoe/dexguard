[美团如何防dex2jar](https://ivonhoe.github.io/2017/02/09/%E7%BE%8E%E5%9B%A2%E5%A6%82%E4%BD%95%E9%98%B2dex2jar/)
---

### 如何使用
- 在root project的build.gradle中添加依赖`classpath 'ivonhoe.gradle.dexguard:dexguard-gradle:0.0.2-SNAPSHOT'`

```
buildscript {
    repositories {
        maven { url 'https://raw.githubusercontent.com/Ivonhoe/mvn-repo/master/' }
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:2.3.0'
        classpath 'ivonhoe.gradle.dexguard:dexguard-gradle:0.0.2-SNAPSHOT'
    }
}
```
- 在app项目的build.gradle中添加插件，map.txt中配置需要保护的方法名

```
apply plugin: 'ivonhoe.dexguard'
dexguard {
    guardConfig = "${rootDir}/map.txt"
}
```


### 一、概述
[上一篇](https://ivonhoe.github.io/2017/02/09/%E7%BE%8E%E5%9B%A2%E5%A6%82%E4%BD%95%E9%98%B2dex2jar/)，我大致分析了美团外卖Android客户端在防止其Java代码被dex2jar工具转换而做的防护措施，其实就是借助dex2jar的语法检查机制，将有语法错误的字节码插入到想要保护的Java函数中里面，以达到dex2jar转换出错的目的。这篇文章就大致记录下如何开发Gradle编译插件，在编译过程中实现上述防护思路。

### 二、思路
**先看下Android APK打包流程：**

<!--more-->

![Android apk打包流程](https://ivonhoe.github.io/res/dexguard/dexguard4.jpeg)

Android APK打包流程如上图所示，Java代码先通过Java Compiler生成.class文件，在通过dx工具生成dex文件，最后使用apkbuilder工具完成代码与资源文件的打包，并使用jarsigner签名，最后可能还有使用zipalign对签名后的apk做对齐处理。

如果需要完成对特定函数的代码注入，可以在Java代码编译生成class文件后，在dex文件生成前，针对class字节码进行操作，以本例为例需要动态生成Exsit类文件的字节码(不清楚Exsit的作用可以看[上一篇](https://ivonhoe.github.io/2017/02/13/Android%E5%AE%89%E5%85%A8%E4%B9%8B---%E7%BE%8E%E5%9B%A2%E9%98%B2dex2jar%E5%8E%9F%E7%90%86/)文章)。

```
// 动态生成Exist.class
public class Exist {
    public static boolean a() {
        return false;
    }

    public static void b(int test) {
    }
}
```
动态修改特定方法的字节码，将下列Java代码转换成字节码插入特定的函数中。

```
// 插入到特定的Java函数内
Exist.b(Exist.a());
```

并将修改后的.class文件放入dex打包目录中，完成dex打包，具体流程如下图所示：

![](https://ivonhoe.github.io/res/dexguard/dexguard5.png)

Gradle提供了叫`Transform`的API，允许三方插件在class文件转换为dex文件前操作编译好的class文件，这个API的目标就是简化class文件的自定义的操作而不用对Task进行处理，并且可以更加灵活地进行操作。详细的可以参考[区长的博客](http://blog.csdn.net/sbsujjbcy/article/details/50839263)。

### 四、ASM操作Java字节码
ASM 是一个 Java 字节码操控框架。它能被用来动态生成类或者增强既有类的功能。ASM 可以直
接产生二进制 class 文件，也可以在类被加载入 Java 虚拟机之前动态改变类行为。这里推荐一个IDEA插件:`ASM ByteCode Outline`。可以查看.class文件的字节码，并可以生成成ASM框架代码。安装`ASM Bytecode Outline`插件后，可以在`Intellij IDEA`->`Code`->`Show Bytecode Outline`查看类文件对应个字节码和ASM框架代码，利用ASM框架代码就可以生成相应的.class文件了。

![](https://ivonhoe.github.io/res/dexguard/dexguard3.png)


生成Exist字节码的具体实现，生成Exist.java的构造函数：

```
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
        
```

声明一个函数名为a，返回值为boolean类型的无参函数：

```
mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "a", "()Z", null, null);
mv.visitCode();
l0 = new Label();
mv.visitLabel(l0);
mv.visitLineNumber(10, l0);
mv.visitInsn(ICONST_0);
mv.visitInsn(IRETURN);
mv.visitMaxs(1, 0);
mv.visitEnd();
```

声明一个函数名为b，参数为int型，返回类型为void的函数

```    
MV = CW.VISITmETHOD(acc_public + acc_static, "b", "(i)v", NULL, NULL);
MV.VISITcODE();
L0 = NEW lABEL();
MV.VISITlABEL(L0);
MV.VISITlINEnUMBER(14, L0);
MV.VISITiNSN(return);
L1 = NEW lABEL();
MV.VISITlABEL(L1);
MV.VISITlOCALvARIABLE("TEST", "i", NULL, L0, L1, 0);
MV.VISITmAXS(0, 1);
MV.VISITeND();
```

在指定函数内，插入`Exist.b(Exist.a());`对应的字节码的具体实现，绕过Java编译器的语法检查：

```
//refer hack class when object init
private static byte[] referHackWhenInit(InputStream inputStream, String methodName) {
    ClassReader cr = new ClassReader(inputStream);
    ClassWriter cw = new ClassWriter(cr, 0);

    ClassVisitor cv = new InjectCassVisitor(Opcodes.ASM4, cw, methodName);
    cr.accept(cv, 0);
    return cw.toByteArray();
}
```


```
static class InjectClassVisitor extends ClassVisitor {

        private String methodName;

        InjectClassVisitor(int i, ClassVisitor classVisitor, String method) {
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
```
### 五、源码
详细的Gradle源码和实例可参考[https://github.com/Ivonhoe/dexguard](https://github.com/Ivonhoe/dexguard)

### 六、参考文档

[Android 热修复使用Gradle Plugin1.5改造Nuwa插件](http://blog.csdn.net/sbsujjbcy/article/details/50839263)

[ASM-操作字节码初探](http://www.wangyuwei.me/2017/01/20/ASM-%E6%93%8D%E4%BD%9C%E5%AD%97%E8%8A%82%E7%A0%81%E5%88%9D%E6%8E%A2/)

[手摸手增加字节码往方法体内插代码](https://www.diycode.cc/topics/581)

