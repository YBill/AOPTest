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

    public void handleShowLength(View view) {
        String str = null;

        // 这个方法里要插入判空代码
        int length = Calculation.getLength(str);

        Toast.makeText(getApplicationContext(), String.valueOf(length), Toast.LENGTH_SHORT).show();
    }

    /**
     * 下面两个方法没啥用，主要是通过 ASM Bytecode Viewer 插件看看 Java 代码用 ASM 该咋写
     */

    private void toast() {
        Toast.makeText(getApplicationContext(), "Hello", Toast.LENGTH_SHORT).show();
    }

    private void log() {
        Log.e("Bill", "Hello");
    }

}