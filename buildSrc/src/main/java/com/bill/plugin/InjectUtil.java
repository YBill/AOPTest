package com.bill.plugin;

import com.android.build.gradle.AppExtension;

import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.LocalVariableAttribute;
import javassist.bytecode.MethodInfo;

/**
 * Created by Bill on 2022/5/4.
 */

class InjectUtil {

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

    /**
     * 方法中添加判空逻辑
     */
    public static byte[] checkJarMethodParamsNull(String jarFilePath) throws NotFoundException, IOException, CannotCompileException {
        ClassPool classPool = ClassPool.getDefault();

        // 导入jar包，要不找不到Calculation类
        classPool.appendClassPath(jarFilePath);

        CtClass ctClass = classPool.getCtClass("com.bill.calculation.Calculation");
        if (ctClass.isFrozen()) {
            ctClass.defrost();
        }

        CtMethod getLengthMethod = ctClass.getDeclaredMethod("getLength");

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

    /**
     * 获取方法的参数名
     */
    public static String[] getMethodVariableName(CtMethod cm) {
        try {
            MethodInfo methodInfo = cm.getMethodInfo();
            CodeAttribute codeAttribute = methodInfo.getCodeAttribute();
            String[] paramNames = new String[cm.getParameterTypes().length];
            LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
            if (attr != null) {
                int pos = Modifier.isStatic(cm.getModifiers()) ? 0 : 1;
                for (int i = 0; i < paramNames.length; i++) {
                    paramNames[i] = attr.variableName(i + pos);
                }
                return paramNames;
            }
        } catch (Exception e) {
            System.out.println("---- getMethodVariableName fail " + e);
        }
        return null;
    }


    /**
     * 获取类名，如：MainActivity
     */
    private static String getSimpleClassName(String filePath) {
        return filePath.substring(filePath.lastIndexOf("/") + 1).replace(".class", "");
    }

    /**
     * 获取 android.jar 的路径
     */
    private static String getAndroidSDKPath(Project project) {
        AppExtension extension = project.getExtensions().findByType(AppExtension.class);
        if (extension != null) {
            return extension.getBootClasspath().get(0).getAbsolutePath();
        }
        return null;
    }

}
