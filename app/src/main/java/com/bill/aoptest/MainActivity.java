package com.bill.aoptest;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bill.calculation.Calculation;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 这里要插入计算onCreate执行时间的代码
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 这里要插入Toast代码
    }

    public void handleShowLength(View view) {
        String str = null;

        // 这个方法里要插入判空代码
        int length = Calculation.getLength(str);

        Toast.makeText(getApplicationContext(), String.valueOf(length), Toast.LENGTH_SHORT).show();
    }

    /**
     * 下面两个方法没啥用，主要是通过 ASM Bytecode Viewer 插件看看 Java 代码用 ASM 该咋写
     * 可以在java中写一个方法，方法内写要实现的功能代码，然后编译后生成class代码
     * 找到class代码，右键ASM Bytecode Viewer，然后查看 ASMified
     * 可以看到方法是通过{}包裹的，然后找到label0,label0代码块就是具体的ASM代码(有可能不是label0)，
     * label0中从methodVisitor.visitLineNumber(x, label0)开始到label1前的代码就是可用代码
     */

    private void toast() {
        Toast.makeText(getApplicationContext(), "Hello", Toast.LENGTH_SHORT).show();
    }

    private void log() {
        Log.e("Bill", "Hello");
    }

}