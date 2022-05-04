# 字节码插桩

本例子使用的技术如下：

- Android 项目语言：Java
- 生成 Gradle 插件方式：buildSrc
- Gradle 插件语言：Java
- 插桩工具：Javassist
- 插桩中定位代码方式：通过包名+类名定位（写死的不灵活）

下面例子完成了字节码插桩，注意完成了以下两个功能：

- 修改第三方 jar 中的内容
- 在 Activity 的 onCreate 方法中插入 Toast

#### 修改第三方 jar 中的内容的例子

有一个 jar，里面就一个类，一个方法，获取字符串长度，内容如下，但是方法没有判空，str 为空时会崩溃，下面通过字节码插桩添加判空逻辑：

```
public class Calculation {

    public static int getLength(String str) {
        return str.length();
    }

}
```

生成 jar 的方式很多，本例子中通过 calculation 这个 module，通过上传本地 Maven 的方式生成 jar，生成到当前 Module 下的 repo目录下，然后就这个 Module 就没用了，将生成的 jar 拷贝到 app 的 libs 下，并在 gradle 中添加依赖

##### 1、新建 Android 项目，然后在 MainActivity 中添加一个 Button，点击后调用 jar 包中的代码

```
    public void handleShowLength(View view) {
        String str = null;

        // 这个方法里要插入判空代码，防止崩溃
        int length = Calculation.getLength(str);

        Toast.makeText(getApplicationContext(), String.valueOf(length), Toast.LENGTH_SHORT).show();

    }
```


##### 2、创建 buildSrc 插件，并配置好插件

##### 3、创建 Transform 类，这里名为 MyTransform，主要代码如下：

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
    // 处理输入的Jar包
    private void handleJarInputs(TransformInvocation transformInvocation, Collection<JarInput> jarInputs) {
        for (JarInput jarInput : jarInputs) {
            String absolutePath = jarInput.getFile().getAbsolutePath();
            System.out.println(">>>> jar input file path: " + absolutePath);
            File contentLocation = transformInvocation.getOutputProvider().getContentLocation(jarInput.getName(),
                    jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
            try {
                // 匹配要修复的jar包
                if (absolutePath.endsWith("calculation-1.0.0.jar")) {
                    // 原始的jar包
                    JarFile jarFile = new JarFile(absolutePath);
                    // 处理后的jar包路径
                    String tmpJarFilePath = jarInput.getFile().getParent() + File.separator + jarInput.getFile().getName() + "_tmp.jar";
                    File tmpJarFile = new File(tmpJarFilePath);
                    JarOutputStream jos = new JarOutputStream(new FileOutputStream(tmpJarFile));
                    System.out.println("---- origin jar file path: " + jarInput.getFile().getAbsolutePath());
                    System.out.println("---- tmp jar file path: " + tmpJarFilePath);
                    Enumeration<JarEntry> entries = jarFile.entries();
                    // 遍历jar包中的文件，找到需要修改的class文件
                    while (entries.hasMoreElements()) {
                        JarEntry jarEntry = entries.nextElement();
                        String name = jarEntry.getName();
                        jos.putNextEntry(new ZipEntry(name));
                        InputStream is = jarFile.getInputStream(jarEntry);
                        // 匹配到有问题的class文件
                        if ("com/bill/calculation/Calculation.class".equals(name)) {
                            // 处理有问题的class文件并将新的数据写入到新jar包中
                            jos.write(InjectUtil.checkJarMethodParamsNull(absolutePath));
                        } else {
                            // 没有问题的直接写入到新的jar包中
                            jos.write(IOUtils.toByteArray(is));
                        }
                        jos.closeEntry();
                    }
                    // 关闭IO流
                    jos.close();
                    jarFile.close();
                    // 拷贝新的Jar文件
                    System.out.println("---- copy to dest: " + contentLocation.getAbsolutePath());
                    FileUtils.copyFile(tmpJarFile, contentLocation);
                    // 删除临时文件
                    System.out.println("---- tmpJarFile: " + tmpJarFile.getAbsolutePath());
                    tmpJarFile.delete();
                } else {
                    FileUtils.copyFile(jarInput.getFile(), contentLocation);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
```

```
    public static byte[] checkJarMethodParamsNull(String jarFilePath) throws NotFoundException, IOException, CannotCompileException {
        ClassPool classPool = ClassPool.getDefault();

        // 导入jar包，要不找不到Calculation类
        classPool.appendClassPath(jarFilePath);

        CtClass ctClass = classPool.getCtClass("com.bill.calculation.Calculation");
        if (ctClass.isFrozen()) {
            ctClass.defrost();
        }

        CtMethod getLengthMethod = ctClass.getDeclaredMethod("getLength");

        // 获取参数名称
        String[] args = getMethodVariableName(getLengthMethod);

        String insertCode = String.format(Locale.getDefault(),
                "if(%s==null)return 0;",
                args[0]);
        System.out.println("---- insertCode：" + insertCode);

        getLengthMethod.insertBefore(insertCode);

        byte[] bytes = ctClass.toBytecode();
        ctClass.detach();

        return bytes;

    }
```

##### 通过上面的步骤就编写完成，点击就可以运行了，点击 Button 可以正常 Toast，没有崩溃，说明我们判空逻辑成功了，反编译代码查看：

```
// from jadx
public class Calculation {
    public static int getLength(String str) {
        if (str == null) {
            return 0;
        }
        return str.length();
    }
}
```

#### 在 Activity 的 onCreate 方法中插入 Toast

##### 1、接着上面的例子，主要看第三步代码：

```
    // 处理输入的目录
    private void handleDirInputs(TransformInvocation transformInvocation, Collection<DirectoryInput> directoryInputs) {
        for (DirectoryInput directoryInput : directoryInputs) {
            String absolutePath = directoryInput.getFile().getAbsolutePath();
            System.out.println(">>>> directory input file path: " + absolutePath);
            // 处理class文件
            InjectUtil.inject(mProject, absolutePath);
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
    public static void inject(Project project, String dirPath) {
        System.out.println("==== Android SDK Path : " + getAndroidSDKPath(project));
        realInject(getAndroidSDKPath(project), dirPath, dirPath);
    }

    /**
     * 遍历目录，是文件则调用doInjection注入，是目录则递归调用inject方法
     */
    private static void realInject(String androidSDKPath, String originalPath, String dirPath) {
        File f = new File(dirPath);
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (File file : files) {
                realInject(androidSDKPath, originalPath, file.getAbsolutePath());
            }
        } else {
            doInjection(androidSDKPath, originalPath, dirPath);
        }
    }

    private static void doInjection(String androidSDKPath, String originalPath, String filePath) {
        if (filePath == null || filePath.length() == 0
                || filePath.trim().length() == 0
                || !filePath.endsWith(".class")) {
            return;
        }

        addToast(androidSDKPath, originalPath, filePath);
    }

    /**
     * 使用Javassist操作Class字节码，在所有Activity的onCreate方法中插入Toast
     */
    private static void addToast(String androidSDKPath, String originalPath, String filePath) {
        System.out.println("==== filePath = " + filePath);

        try {
            ClassPool classPool = new ClassPool();

            classPool.appendClassPath(androidSDKPath); // 导入 android.jar
            classPool.appendClassPath(originalPath); // 导入当前类的路径，要不找不到，比如MainActivity
            classPool.importPackage("android.widget.Toast"); // 导入包

            if (filePath.contains("MainActivity")) {
                CtClass ctClass = classPool.getCtClass("com.bill.aoptest.MainActivity");
                if (ctClass.isFrozen()) {
                    ctClass.defrost();
                }
                // 获取Activity中的onCreate方法
                CtMethod onCreate = ctClass.getDeclaredMethod("onCreate");
                // 要插入的代码，Toast内容为当前类名
                String insertCode = String.format(Locale.getDefault(),
                        "Toast.makeText(this, \"%s\", Toast.LENGTH_SHORT).show();",
                        getSimpleClassName(filePath));
                System.out.println("==== insertCode：" + insertCode);
                // 在onCreate方法结尾处插入上面的Toast代码
                onCreate.insertAfter(insertCode);
                // 写回原来的目录下，覆盖原来的class文件
                ctClass.writeFile(originalPath);
                ctClass.detach();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
```

##### 点击 Run 跑起来进入项目就 Toast 了，说明我们在 MainActivity 中插入 Toast 成功了，反编译代码查看：

```
    // from jadx
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(2131361820);
        Object var3 = null;
        Toast.makeText(this, "MainActivity", 0).show();
    }
```