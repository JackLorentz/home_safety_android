package com.jackchen.test_06_04_2017;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private Button createNewMap;
    private Button tracing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();

    }

    private void findViews(){
        tracing = (Button)findViewById(R.id.button);
        tracing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SelectMapActivity.class);
                startActivity(intent);
            }
        });
        createNewMap = (Button)findViewById(R.id.button2);
        createNewMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText et=new EditText(MainActivity.this);
                et.setHint("地圖名稱");
                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this).setTitle("請命名您的地圖:").setView(et).
                        setPositiveButton("確定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1){
                                String name = et.getText().toString();//地點名稱
                                Intent intent = new Intent(MainActivity.this, CreateMapActivity.class);
                                intent.putExtra("MapName", name);
                                startActivity(intent);
                            }
                        }).setNegativeButton("取消",null).show();
                dialog.setCancelable(false);
            }
        });
    }
}

