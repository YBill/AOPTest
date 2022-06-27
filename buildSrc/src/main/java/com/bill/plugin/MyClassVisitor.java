package com.bill.plugin;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * author ywb
 * date 2022/6/21
 * desc
 */
public class MyClassVisitor extends ClassVisitor {

    public MyClassVisitor(int api) {
        super(api);
    }

    public MyClassVisitor(int api, ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        // 访问类时会调用该方法
        System.out.println("==== visit: name = " + name);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        // 访问类中的方法时会调用visitMethod
        // ==== visitMethod: name = <init> // 代表构造方法
        System.out.println("==== visitMethod: name = " + name);
        if ("onCreate".equals(name)) {
            MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new MyAdviceAdapter(Opcodes.ASM5, methodVisitor, access, name, descriptor);
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }
}
