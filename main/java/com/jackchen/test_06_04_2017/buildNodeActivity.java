package com.jackchen.test_06_04_2017;

import android.*;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.fit.samples.common.logger.Log;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static android.Manifest.permission.CAMERA;
import static com.jackchen.test_06_04_2017.R.string.trace;

public class buildNodeActivity extends AppCompatActivity {

    //元件
    private ListView listView;
    private ViewAdapter adapter;
    private Button addItem, complete;
    //資料庫
    // [START declare_database_ref]
    private DatabaseReference mapRef;
    // [END declare_database_ref]
    private ArrayList<String> nData = new ArrayList<String>();
    private ArrayList<String> iData = new ArrayList<String>();
    private int size;
    private String key;
    private boolean is_first = true;
    private int cnt = 0;
    //image file
    private static final int REQUEST_CAPTURE = 0;
    private static final int REQUEST_CAMERA = 1;
    private File currentImageFile;
    private String filepath;
    private String pictureName;
    private Uri imageUri; //圖片路徑
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    android.util.Log.i("OpenCV", "OpenCV loaded successfully");
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    //Log
    public static final String TAG = "BasicSensorsApi";
    private static final String PATH ="File Path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_build_node);

        Bundle bundle = getIntent().getExtras();
        size = bundle.getInt("size");
        key = bundle.getString("key");
        Log.i(TAG, "Get the size = " + size + " and " + " the key =" + key + ".");
        // [START initialize_database_ref]
        mapRef = FirebaseDatabase.getInstance().getReference().child("NodeInfo").child(key);
        // [END initialize_database_ref]
        //開啟相機權限
        int permission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA);
        if(permission != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[] {CAMERA, CAMERA_SERVICE}, REQUEST_CAMERA);
        }

        findViews();
        Log.i(TAG, "findViews");
    }

    private void findViews() {
        listView = (ListView) findViewById(R.id.listView3);
        addItem = (Button) findViewById(R.id.button4);
        addItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cnt < size) {
                    takeAPhoto();//取得該座標點照片
                    final EditText et = new EditText(buildNodeActivity.this);
                    et.setHint("提醒內容");
                    Log.i(TAG, "Open the dialog .");
                    AlertDialog dialog = new AlertDialog.Builder(buildNodeActivity.this).setTitle("請設定您的提醒資訊:").setView(et).
                            setPositiveButton("儲存", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    //紀錄資訊點內容
                                    String notification = et.getText().toString();//地點名稱
                                    //在listView上設定資訊
                                    nData.add(notification);
                                    iData.add(filepath);
                                    if (is_first) {
                                        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                        adapter = new ViewAdapter(nData, iData, inflater, 1);
                                        is_first = false;
                                    } else {
                                        adapter.notifyDataSetChanged();
                                    }
                                    listView.setAdapter(adapter);
                                    // [END_EXCLUDE]*/
                                    cnt++;
                                    arg0.dismiss();
                                }
                            }).setNegativeButton("取消", null).show();
                    dialog.setCancelable(false);
                }
                else {
                    AlertDialog dialog = new AlertDialog.Builder(buildNodeActivity.this).setTitle("您設定的資訊點已超量 !").
                            setPositiveButton("了解", null).show();
                    dialog.setCancelable(false);
                }
            }
        });
        complete = (Button) findViewById(R.id.button6);
        complete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog dialog = new AlertDialog.Builder(buildNodeActivity.this).setTitle("您確定要儲存目前地圖 ?").
                        setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                //存圖資到JSON tree上
                                for(int i=0; i<size; i++){
                                    mapRef.child(String.valueOf(i)).child("notification").setValue(nData.get(i));
                                    mapRef.child(String.valueOf(i)).child("imagefile").setValue(iData.get(i));
                                    Log.i(TAG, "notification : " + nData.get(i) + "imageFile : " + iData.get(i));
                                }
                                //
                                Toast.makeText(buildNodeActivity.this, "儲存地圖", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(buildNodeActivity.this, MainActivity.class);
                                startActivity(intent);
                            }
                        }).setNegativeButton("否",  new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                arg0.dismiss();
                             }
                        }).show();
                dialog.setCancelable(false);
            }
        });
    }

    private void takeAPhoto(){
        //創造儲存路徑
        this.pictureName = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(new Date())  + ".jpg";
        //用時間做照片命名
        currentImageFile = new File(getExternalCacheDir(), pictureName);
        filepath = currentImageFile.getPath();
        android.util.Log.i(PATH, filepath);
        //將File對象轉換為Uri並啟動照相程序
        //進行拍照
        Intent intent = new Intent();
        intent.setAction("android.media.action.IMAGE_CAPTURE");
        intent.addCategory("android.intent.category.DEFAULT");
        //儲存至指定路徑
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {//7.0及以上
            imageUri = FileProvider.getUriForFile(buildNodeActivity.this, "com.jackchen.test_06_04_2017.fileprovider", currentImageFile);

            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri); //指定圖片輸出地址
            ComponentName componentName = intent.resolveActivity(getPackageManager());
            if (componentName != null) {
                startActivityForResult(intent, REQUEST_CAPTURE);
            }
        }
        else {
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(currentImageFile));
            startActivity(intent);
        }
    }
}
