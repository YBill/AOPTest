package com.bill.plugin;

import com.android.build.gradle.AppExtension;

import org.gradle.api.Project;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

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

        addLog(androidSDKPath, originalPath, filePath);
    }

    /**
     * 使用ASM操作Class字节码，在所有Activity的onCreate方法中插入Log输入执行时间
     */
    private static void addLog(String androidSDKPath, String originalPath, String filePath) {
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
