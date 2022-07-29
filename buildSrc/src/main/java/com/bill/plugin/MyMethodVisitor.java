package com.bill.plugin;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * author ywb
 * date 2022/7/28
 * desc 继承MethodVisitor在方法中插入代码，最好继承AdviceAdapter
 */
public class MyMethodVisitor extends MethodVisitor implements Opcodes {

    private final String mClassName;

    public MyMethodVisitor(int api, MethodVisitor methodVisitor, String className) {
        super(api, methodVisitor);
        this.mClassName = className;
    }

    /**
     * 方法访问之前调用
     */
    @Override
    public void visitCode() {
        super.visitCode();
        System.out.println("==== MethodVisitor visitCode");
    }

    /**
     * 这个方法的目的是在onResume方法结束的地方插入如下代码
     * Toast.makeText(this.getApplicationContext(), "Hello", 0).show();
     */
    @Override
    public void visitInsn(int opcode) {
        System.out.println("==== MethodVisitor visitInsn ---> opcode = " + opcode);
        // 判断操作符是不是方法返回
        if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) || opcode == Opcodes.ATHROW) {
            mv.visitVarInsn(ALOAD, 0);
            // mClassName 当前类，也就是this（com/bill/aoptest/MainActivity）
            mv.visitMethodInsn(INVOKEVIRTUAL, mClassName, "getApplicationContext", "()Landroid/content/Context;", false);
            mv.visitLdcInsn("Hello");
            mv.visitInsn(ICONST_0);
            mv.visitMethodInsn(INVOKESTATIC, "android/widget/Toast", "makeText", "(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "android/widget/Toast", "show", "()V", false);
        }
        super.visitInsn(opcode);
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        System.out.println("==== MethodVisitor visitMaxs ---> maxStack = " + maxStack + ", maxLocals = " + maxLocals);
        super.visitMaxs(maxStack, maxLocals);
    }
}
