package com.bill.plugin;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.utils.FileUtils;

import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * Created by Bill on 2022/5/3.
 */

class MyTransform extends Transform {

    private final Project mProject;

    public MyTransform(Project project) {
        mProject = project;
    }

    @Override
    public String getName() {
        return "MyTrans";
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
        return false;
    }

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

    // 处理输入的Jar包
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


}
