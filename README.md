# 字节码插桩

本例子使用的技术如下：

- Android 项目语言：Java
- 生成 Gradle 插件方式：buildSrc
- Gradle 插件语言：Java
- 插桩工具：ASM
- 插桩中定位代码方式：通过包名+类名定位（写死的不灵活）

下面例子完成了字节码插桩，注意完成了以下两个功能：

- 在 Activity 的 onCreate 方法中插入计算方法执行耗时并输出Log
- 在 Activity 的 onResume 方法中 Toast

##### 0-1、创建 buildSrc 插件，并配置好插件

##### 0-2、创建 Transform 类，这里名为 MyTransform，主要代码如下：

```
    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation);

        if (!transformInvocation.isIncremental()) {
            // 非增量编译，则删除之前的所有输出
            transformInvocation.getOutputProvider().deleteAll();
        }
        // 拿到所有输入
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        if (!inputs.isEmpty()) {
            for (TransformInput input : inputs) {
                // directoryInputs保存的是存放class文件的所有目录，可以通过方法内打印的log查看具体的目录
                Collection<DirectoryInput> directoryInputs = input.getDirectoryInputs();
                handleDirInputs(transformInvocation, directoryInputs);

                // jarInputs保存的是所有依赖的jar包的地址，可以通过方法内打印的log查看具体的jar包路径
                Collection<JarInput> jarInputs = input.getJarInputs();
                handleJarInputs(transformInvocation, jarInputs);
            }
        }

    }
```

```
        // Jar包不处理
        private void handleJarInputs(TransformInvocation transformInvocation, Collection<JarInput> jarInputs) {
            for (JarInput jarInput : jarInputs) {
                String absolutePath = jarInput.getFile().getAbsolutePath();
                System.out.println(">>>> jar input file path: " + absolutePath);
                File contentLocation = transformInvocation.getOutputProvider().getContentLocation(jarInput.getName(),
                        jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                try {
                    FileUtils.copyFile(jarInput.getFile(), contentLocation);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
```

#### 在 Activity 的 onCreate 方法中插入 Log

##### 1、处理输入的目录：

```
    // 处理输入的目录
    private void handleDirInputs(TransformInvocation transformInvocation, Collection<DirectoryInput> directoryInputs) {
        for (DirectoryInput directoryInput : directoryInputs) {
            String absolutePath = directoryInput.getFile().getAbsolutePath();
            System.out.println(">>>> directory input file path: " + absolutePath);
            // 处理class文件
            InjectUtil.inject(absolutePath);
            // 获取目标地址
            File contentLocation = transformInvocation.getOutputProvider().getContentLocation(directoryInput.getName(),
                    directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
            // 拷贝目录
            try {
                FileUtils.copyDirectory(directoryInput.getFile(), contentLocation);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
```

```
        public static void inject(String dirPath) {
            realInject(dirPath);
        }

        /**
         * 遍历目录，是文件则调用doInjection注入，是目录则递归调用inject方法
         */
        private static void realInject(String dirPath) {
            File f = new File(dirPath);
            if (f.isDirectory()) {
                File[] files = f.listFiles();
                for (File file : files) {
                    realInject(file.getAbsolutePath());
                }
            } else {
                doInjection(dirPath);
            }
        }

        private static void doInjection(String filePath) {
            if (filePath == null || filePath.length() == 0
                    || filePath.trim().length() == 0
                    || !filePath.endsWith(".class")) {
                return;
            }

            addLog(filePath);
        }

        private static void addLog(String filePath) {
            System.out.println("==== filePath = " + filePath);

            try {
                if (filePath.contains("MainActivity")) {
                    // 要操作的class源文件，这里换成你本机的路径
                    final String originClzPath = filePath;
                    FileInputStream fis = new FileInputStream(originClzPath);
                    // ClassReader是ASM提供的读取字节码的工具
                    ClassReader classReader = new ClassReader(fis);
                    // ClassWriter是ASM提供的写入字节码的工具
                    ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
                    // 自定义类访问器，在其中完成对某个方法的字节码操作
                    MyClassVisitor myClassVisitor = new MyClassVisitor(Opcodes.ASM5, classWriter);
                    // 调用ClassReader的accept方法开始处理字节码
                    classReader.accept(myClassVisitor, ClassReader.EXPAND_FRAMES);
                    // 操作后的class文件写入到这个文件中，也可以自定义一个文件对比看结果
                    String destPath = filePath;
                    // 通过ClassWriter拿到处理后的字节码对应的字节数组
                    byte[] bytes = classWriter.toByteArray();
                    FileOutputStream fos = new FileOutputStream(destPath);
                    // 写文件
                    fos.write(bytes);
                    // 关闭文件流
                    fos.close();
                    fis.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
```
##### 2、创建ClassVisitor类处理类：

```
public class MyClassVisitor extends ClassVisitor {

    private String mClassName;

    public MyClassVisitor(int api) {
        super(api);
    }

    public MyClassVisitor(int api, ClassVisitor classVisitor) {
        // api：ASM API版本，源码规定只能为4，5，6
        super(api, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.mClassName = name;
        // 访问类时会调用该方法
        System.out.println("==== ClassVisitor visit --> className = " + name);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        // 访问类中的方法时会调用
        // ==== visitMethod: name = <init> // 代表构造方法
        System.out.println("==== ClassVisitor visitMethod ---> methodName = " + name);
        // 下面分别在onCreate和onResume中插入代码，分别使用了继承MyMethodVisitor和AdviceAdapter(继承MyMethodVisitor)来处理
        // 推荐使用继承AdviceAdapter处理
        if ("onCreate".equals(name)) {
            MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new MyAdviceAdapter(Opcodes.ASM5, methodVisitor, access, name, descriptor);
        } else if ("onResume".equals(name)) {
            MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new MyMethodVisitor(Opcodes.ASM5, methodVisitor, mClassName);
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        super.visitEnd();
        // 访问类结束调用
        System.out.println("==== ClassVisitor visitEnd");
    }
}
```

##### 2、创建MethodVisitor类处理方法：

这里分别创建了继承 MethodVisitor 和 AdviceAdapter 分别处理 插入日志和Toast，推荐使用继承AdviceAdapter处理

```
public class MyAdviceAdapter extends AdviceAdapter {

    private int startTimeId;

    protected MyAdviceAdapter(int api, MethodVisitor methodVisitor, int access, String name, String descriptor) {
        super(api, methodVisitor, access, name, descriptor);
    }

    /**
     * 这个方法的目的是为了在onCreate方法开始处插入如下代码：
     * long v = System.currentTimeMillis();
     */
    @Override
    protected void onMethodEnter() {
        super.onMethodEnter();
        System.out.println("==== AdviceAdapter onMethodEnter");
        // 在方法开始处调用
        // 创建一个long类型的本地变量
        startTimeId = newLocal(Type.LONG_TYPE);
        // 调用System.currentTimeMillis()方法
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        // 将上一步中的结果保存到startTimeId指向的long类型变量中（不是保存到startTimeId）
        mv.visitIntInsn(LSTORE, startTimeId);
    }

    /**
     * 这个方法的目的是在onCreate方法结束的地方插入如下代码：
     * long end = System.currentTimeMillis();
     * long x = end - start;
     * System.out.println("execute onCreate() use time: " + x);
     * 或
     * Log.e("Bill", "execute onCreate() use time: " + x);
     */
    @Override
    protected void onMethodExit(int opcode) {
        super.onMethodExit(opcode);
        System.out.println("==== AdviceAdapter onMethodExit");
        // 在方法结束时调用
        // 创建一个long类型的本地变量
        int endTimeId = newLocal(Type.LONG_TYPE);
        // 调用System.currentTimeMillis()方法
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
        // 将上一步中的结果保存到endTimeId指向的long类型变量中（不是保存到endTimeId）
        mv.visitIntInsn(LSTORE, endTimeId);
        // 创建一个long类型的本地变量，deltaTimeId为这个变量的ID
        int deltaTimeId = newLocal(Type.LONG_TYPE);
        // 加载endTimeId指向的long类型的变量
        mv.visitIntInsn(LLOAD, endTimeId);
        // 加载startTimeId指向的long类型变量
        mv.visitIntInsn(LLOAD, startTimeId);
        // 将上面两个变量做减法（endTimeIdVal - startTimeIdVal）
        mv.visitInsn(LSUB);
        // 将减法的结果存在deltaTimeId指向的变量中
        mv.visitIntInsn(LSTORE, deltaTimeId);

        // 1、使用System.out.println()输出
        /*// 调用System静态方法out
        mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");*/

        // 2、使用Log.x输出
        // Log的第一个参数，也就是tag
        mv.visitLdcInsn("Bill");

        // 这个是拼接字符串，作为使用System.out.println()的第一个参数或使用Log.x的第二个参数
        // 创建StringBuilder对象
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        // 复制栈顶数值并将复制值压入栈顶
        mv.visitInsn(DUP);
        // 调用StringBuilder构造方法初始化
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        // 将字符串推到栈顶
        mv.visitLdcInsn("execute onCreate() use time: ");
        // 调用StringBuilder的append方法
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        // 加载deltaTimeId指向的long类型数据
        mv.visitVarInsn(LLOAD, deltaTimeId);
        // 调用StringBuilder的append方法
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false);
        // 调用StringBuilder的toString方法
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);

//        mv.visitLdcInsn("Hello"); // Log的第二个参数
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", "e", "(Ljava/lang/String;Ljava/lang/String;)I", false);
        mv.visitInsn(Opcodes.POP);

        /*// 调用System.out的println方法
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);*/
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        System.out.println("==== AdviceAdapter visitMaxs ---> maxStack = " + maxStack + ", maxLocals = " + maxLocals);
        super.visitMaxs(maxStack, maxLocals);
    }
}
```

##### 点击 Run 跑起来进入项目可以看到在 onCreate 和 onResume 中插入的代码了，反编译代码查看：

```
    // from jadx
    public class MainActivity extends AppCompatActivity {
        public MainActivity() {
        }

        protected void onCreate(Bundle savedInstanceState) {
            long var2 = System.currentTimeMillis();
            super.onCreate(savedInstanceState);
            this.setContentView(2131361820);
            long var4 = System.currentTimeMillis();
            long var6 = var4 - var2;
            Log.e("Bill", "execute onCreate() use time: " + var6);
        }

        protected void onResume() {
            super.onResume();
            Toast.makeText(this.getApplicationContext(), "Hello", 0).show();
        }
    }
```

##### 编写 ASM 代码：

可以先在一个类中写入要实现的功能，然后编译生成 class 文件（在build/intermediates/javac/debug/classes/包名/类名），然后通过 ASM Bytecode Viewer 插件查看，
然后将 ASM 代码拷贝即可