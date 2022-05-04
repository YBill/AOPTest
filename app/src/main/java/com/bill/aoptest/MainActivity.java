package com.bill.aoptest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.bill.calculation.Calculation;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 这里要插入一个 Toast
    }

    public void handleShowLength(View view) {
        String str = null;

        // 这个方法里要插入判空代码
        int length = Calculation.getLength(str);

        Toast.makeText(getApplicationContext(), String.valueOf(length), Toast.LENGTH_SHORT).show();

    }
}