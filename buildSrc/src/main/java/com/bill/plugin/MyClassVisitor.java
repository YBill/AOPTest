package com.bill.plugin;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * author ywb
 * date 2022/6/21
 * desc ClassVisitor处理类访问的逻辑
 */
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
