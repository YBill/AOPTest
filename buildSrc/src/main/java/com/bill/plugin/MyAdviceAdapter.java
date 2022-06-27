package com.bill.plugin;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * author ywb
 * date 2022/6/21
 * desc MethodVisitor子类，处理具体方法的逻辑
 */
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
}
