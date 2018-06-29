package com.jackchen.test_06_04_2017;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fit.samples.common.logger.Log;
import com.google.android.gms.fit.samples.common.logger.LogView;
import com.google.android.gms.fit.samples.common.logger.LogWrapper;
import com.google.android.gms.fit.samples.common.logger.MessageOnlyLogFilter;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.fitness.result.DataSourcesResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static android.Manifest.permission.CAMERA;

public class TracingActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    //元件
    private TraceView trace;
    private Button pause;
    private ProgressDialog progressDialog;
    //圖資
    private String mapKey;
    private int[][] map = new int[1000][1000];
    private String mapUrl;
    private ArrayList<NodeInfo> nodes = new ArrayList<NodeInfo>();
    private float x, y;
    private int initialPlace = 0;
    //場景辨識
    private static final int REQUEST_CAPTURE = 0;
    private static final int REQUEST_CAMERA = 1;
    private String text1 = "請按是讓程式藉由拍照幫您判斷位置,否則按否離開回到選單.";
    private String text2 = "您您照的照片並不在資料庫內!請您重拍一張照片或是選擇退出.";
    private int recognized = 0;
    private File currentImageFile;
    private Bitmap object, target;
    private String filepath;
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
    //資料庫
    private DatabaseReference NodeInfoReference;
    private ValueEventListener mNodeInfoListener;
    private ValueEventListener NodeInfoListener = new ValueEventListener() {
        //存取圖資
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            for(DataSnapshot nodeSnapshot : dataSnapshot.getChildren()){
                NodeInfo node = nodeSnapshot.getValue(NodeInfo.class);
                nodes.add(node);
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "loadTraceView:onCancelled", databaseError.toException());
            // [START_EXCLUDE]
            // [END_EXCLUDE]
        }
    };
    // [START auth_variable_references]
    private GoogleApiClient mClient = null;
    // [END auth_variable_references]
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    // [START mListener_variable_reference]
    // Need to hold a reference to this listener, as it's passed into the "unregister"
    // method in order to stop all sensors from sending data to this listener.
    private OnDataPointListener distanceListener;
    // [END mListener_variable_reference]
    //慣性感測器
    private SensorManager sensorManager;
    private Sensor rotationVectorSensor;
    //旋轉向量
    private final float[] mRotationMatrix = new float[16];
    private final float[] mOrientation = new float[9];
    private float magneticHeading;
    private GeomagneticField mGeomagneticField;
    private float[] data = new float[3];
    //位移
    private float distanceDelta = 0;
    //提醒
    private int currentPlace = 0;
    private float[] loc;
    private boolean inner = true;//一開始就在某提醒區域中
    private TextToSpeech tts;
    private class Reminder extends AsyncTask<Void, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            currentPlace =  map[(int)loc[0]][(int)loc[1]] - 1;
            //語音提醒
            CharSequence text = nodes.get(map[(int)loc[0]][(int)loc[1]] - 1).notification;
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "id1");
        }

        @Override
        protected String doInBackground(Void... params) {
            return "OK";
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //校正位置
            Log.i(TAG, nodes.get(map[(int)loc[0]][(int)loc[1]] - 1).notification);
            x = Integer.parseInt(nodes.get(currentPlace).coordination) / 1000;
            y = Integer.parseInt(nodes.get(currentPlace).coordination) % 1000;
            trace.correctLocation(x , y);
            //畫面提醒
            Toast.makeText(TracingActivity.this, nodes.get(currentPlace).notification, Toast.LENGTH_LONG).show();
            inner = true;
        }

    }
    //Log
    private static final String DB = "FireBase";
    private static final String TAG = "BasicSensorApi";
    private static final String openCV = "openCV";
    private static final String TTS = "TextToSpeech";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracing);
        //取得地圖名稱
        Intent intent = this.getIntent();
        mapKey = intent.getStringExtra("key");
        mapUrl = intent.getStringExtra("map");
        android.util.Log.i(TAG, "Get the map's key: " + mapKey);
        // Initialize Database
        NodeInfoReference = FirebaseDatabase.getInstance().getReference().child("NodeInfo").child(mapKey);
        android.util.Log.i(TAG, " Initialize Database");
        //取得地圖
        readMapfile();
        android.util.Log.i(TAG, "Read file from " + mapUrl);
        //開啟相機權限
        int permission = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA);
        if(permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {CAMERA, CAMERA_SERVICE}, REQUEST_CAMERA);
        }
        // Add value event listener to the post
        // [START post_value_event_listener]
        NodeInfoReference.addValueEventListener(NodeInfoListener);
        // [END post_value_event_listener]
        // Keep copy of post listener so we can remove it when app stops
        mNodeInfoListener = NodeInfoListener;
        //初始化元件
        findView();
        //進度條
        progressDialog = new ProgressDialog(TracingActivity.this);
        progressDialog.setMax(100);
        progressDialog.setMessage("辨識中...");
        progressDialog.setTitle("尋找位置");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        //TTS
        tts = new TextToSpeech(this, this);
    }

    @Override
    public void onInit(int status){
        if(status == TextToSpeech.SUCCESS){
            int result = tts.setLanguage(Locale.CHINESE);
            if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                Log.e(TTS, "This Language isn't supported!");
                Toast.makeText(TracingActivity.this, "This Language isn't supported!", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            Log.e(TTS, "Initialization Failed!");
            Toast.makeText(TracingActivity.this, "Initialization Failed!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        //場景辨識
        if(recognized == 0) {
            recognizePlace(text1);
            //與google API 取得聯繫
            // This method sets up our custom logger, which will print all log messages to the device
            // screen, as well as to adb logcat.
            initializeLogging();
            // When permissions are revoked the app is restarted so onCreate is sufficient to check for
            // permissions core to the Activity's functionality.
            if (!checkPermissions()) {
                requestPermissions();
            }
            //
            recognized = 1;
        }
    }

    private void findView(){
        trace = (TraceView)findViewById(R.id.traceView);
        pause = (Button)findViewById(R.id.button8);
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trace.pause();
                AlertDialog dialog = new AlertDialog.Builder(TracingActivity.this).setTitle("暫停追蹤").
                        setPositiveButton("繼續", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1){
                                trace.resume();
                                arg0.dismiss();
                            }
                        }).setNegativeButton("回主選單", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(TracingActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                }).show();
                dialog.setCancelable(false);
            }
        });
    }

    private void recognizePlace(String text){
        AlertDialog dialog = new AlertDialog.Builder(TracingActivity.this).setTitle(text).
                setPositiveButton("是", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1){
                        takeAPhoto();
                        while(target == null){
                            target = BitmapFactory.decodeFile(filepath);
                        }
                        new SceneRecognition().execute();
                    }
                }).setNegativeButton("否", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            unregisterFitnessDataListener();
                            Intent intent = new Intent(TracingActivity.this, SelectMapActivity.class);
                            startActivity(intent);
                        }
                }).show();
        dialog.setCancelable(false);
    }

    private class SceneRecognition extends AsyncTask<Void, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // 在onPreExecute()中我們讓ProgressDialog顯示出來
            progressDialog.show();
        }

        @Override
        protected String doInBackground(Void... params) {
            int min = 1000000, i = 0;
            while(progressDialog.getProgress() <= progressDialog.getMax() && i < nodes.size()){
                Log.i(TAG, "節點數: " + nodes.size());
                object = BitmapFactory.decodeFile(nodes.get(i).imagefile);
                int tmp = min;
                int match = compare();//用ORB比對
                min = Math.min(min, match);//取matchesFound最少的
                Log.i(TAG, nodes.get(i).notification + " comparision matchesFound: " + match);
                if (tmp != min) {
                    initialPlace = i;
                }
                i++;
                publishProgress(Integer.valueOf(100/nodes.size()));
            }
            //統計出閥值篩選非資料庫照片
            if(min > 418){
                return "FAIL";
            }
            else{
                return "OK";
            }
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            progressDialog.incrementProgressBy(values[0]);
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            android.util.Log.i(TAG, "Recognition Completed.");
            if(result.equals("OK")){
                Log.i(openCV, "get the bitmap.");
                Log.i(openCV, nodes.get(initialPlace).notification);
                x = Integer.valueOf(nodes.get(initialPlace).coordination) / 1000;
                y = Integer.valueOf(nodes.get(initialPlace).coordination) % 1000;
                trace.correctLocation(x , y);
                //
                Toast.makeText(TracingActivity.this, nodes.get(initialPlace).notification, Toast.LENGTH_LONG).show();
                //語音提醒
                CharSequence text = nodes.get(initialPlace).notification;
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "id1");
                //
                sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
                sensorManager.registerListener(rotationVectorListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
                //
                buildFitnessClient();
                Toast.makeText(TracingActivity.this, "開始追蹤", Toast.LENGTH_SHORT).show();
                //
                progressDialog.dismiss();
            }
            else if(result.equals("FAIL")){
                progressDialog.dismiss();
                recognizePlace(text2);
            }
        }

    }

    private void takeAPhoto(){
        //創造儲存路徑
        String pictureName = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(new Date())  + ".jpg";
        //用時間做照片命名
        currentImageFile = new File(getExternalCacheDir(), pictureName);
        filepath = currentImageFile.getPath();
        android.util.Log.i(TAG, filepath);
        //將File對象轉換為Uri並啟動照相程序
        //進行拍照
        Intent intent = new Intent();
        intent.setAction("android.media.action.IMAGE_CAPTURE");
        intent.addCategory("android.intent.category.DEFAULT");
        //儲存至指定路徑
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {//7.0及以上
            imageUri = FileProvider.getUriForFile(TracingActivity.this, "com.jackchen.test_06_04_2017.fileprovider", currentImageFile);

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

    private void readMapfile(){
        try{
            FileReader rd = new FileReader(mapUrl);
            BufferedReader br = new BufferedReader(rd);
            /*Scanner runner = new Scanner(br);
            int i = 0;
            while(runner.hasNextLine()){
                for(int j=0; j<1000; j++){
                    map[i][j] = runner.;
                }
                i++;
                if(runner.hasNextLine()){
                    runner.nextLine();
                }
            }
            runner.close();*/
            int ch, i = 0, j = 0;
            while((ch = br.read()) != -1){
                if((char)ch == '\n'){
                    continue;
                }
                if(j == 1000){
                    i++;
                    j = 0;
                }
                map[i][j] = ch - 48;
                j++;
            }
        }
        catch (IOException e){
            android.util.Log.e(TAG, "exception", e);
        }
    }

    private int compare(){
        Log.i(openCV, "In Compare");
        int matchesFound = 0;
        ORBprocessing task = new ORBprocessing();

        if (object != null) {
            Log.i(openCV, "Scaling bitmaps");
            task.setObjToRecognize(object);
            task.setScene(target);
            task.setMinDistance(100);
            task.setMaxDistance(0);
            matchesFound = task.detectObject();
        }
        else {
            Log.i(openCV, "Unable to compare");
        }

        return matchesFound;
    }

    private void buildFitnessClient() {
        if (mClient == null && checkPermissions()) {
            mClient = new GoogleApiClient.Builder(this)
                    .addApi(Fitness.SENSORS_API)
                    .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                    .addConnectionCallbacks(
                            new GoogleApiClient.ConnectionCallbacks() {
                                @Override
                                public void onConnected(Bundle bundle) {
                                    Log.i(TAG, "Connected!!!");
                                    // Now you can make calls to the Fitness APIs.
                                    findFitnessDataSources();
                                }

                                @Override
                                public void onConnectionSuspended(int i) {
                                    // If your connection to the sensor gets lost at some point,
                                    // you'll be able to determine the reason and react to it here.
                                    if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                        Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                                    } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                                    }
                                }
                            }
                    )
                    .enableAutoManage(this, 0, new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult result) {
                            Log.i(TAG, "Google Play services connection failed. Cause: " +
                                    result.toString());
                            Snackbar.make(
                                    TracingActivity.this.findViewById(R.id.create_map_activity_view),
                                    "Exception while connecting to Google Play services: " + result.getErrorMessage(),
                                    Snackbar.LENGTH_INDEFINITE).show();
                        }
                    })
                    .build();
        }
    }
    // [END auth_build_googleapiclient_beginning]

    private void findFitnessDataSources() {
        // [START find_data_sources]
        // Note: Fitness.SensorsApi.findDataSources() requires the ACCESS_FINE_LOCATION permission.
        Fitness.SensorsApi.findDataSources(mClient, new DataSourcesRequest.Builder()
                // At least one datatype must be specified.
                .setDataTypes(DataType.TYPE_STEP_COUNT_DELTA)
                // Can specify whether data type is raw or derived.
                .setDataSourceTypes(DataSource.TYPE_DERIVED)
                .build())
                .setResultCallback(new ResultCallback<DataSourcesResult>() {
                    @Override
                    public void onResult(DataSourcesResult dataSourcesResult) {
                        Log.i(TAG, "Result: " + dataSourcesResult.getStatus().toString());
                        for (DataSource dataSource : dataSourcesResult.getDataSources()) {
                            Log.i(TAG, "Data source found: " + dataSource.toString());
                            Log.i(TAG, "Data Source type: " + dataSource.getDataType().getName());

                            //Let's register a listener to receive Activity data!
                            if (dataSource.getDataType().equals(DataType.TYPE_STEP_COUNT_DELTA) && distanceListener == null) {
                                Log.i(TAG, "Data source for TYPE_STEP_COUNT_DELTA found!  Registering.");
                                registerFitnessDataListener(dataSource, DataType.TYPE_STEP_COUNT_DELTA);
                            }
                        }
                    }
                });
        // [END find_data_sources]
    }

    private void registerFitnessDataListener(DataSource dataSource, DataType dataType) {
        // [START register_data_listener]
        distanceListener = new OnDataPointListener() {
            @Override
            public void onDataPoint(DataPoint dataPoint) {
                for (Field field : dataPoint.getDataType().getFields()) {
                    Value val = dataPoint.getValue(field);
                    String tmp = val.toString();
                    Log.i(TAG, "------------------");
                    Log.i(TAG, "Raw data(step) : " + tmp);
                    distanceDelta = Float.parseFloat(tmp);
                    if(distanceDelta >= 7){
                        distanceDelta = 1;
                    }
                    //Log.i(TAG, "Detected DataPoint field: " + field.getName());
                    //Log.i(TAG, "Detected DataPoint value: " + val);
                    trace.dataListener(distanceDelta, data);
                    //取得(x,y)座標位置
                    loc = trace.getXY();
                    Log.i(TAG, "(x , y) = " + "(" + loc[0] + " , " + loc[1] + ")");
                    Log.i(TAG, "Distance Delta Measurement : " + loc[2] + "m");
                    Log.i(TAG, "方位角 : " + loc[3]);
                    //偵測是否有資訊點
                    Log.i(TAG, "map[" + (int)loc[0] + "][" + (int)loc[1] + "] = " + map[(int)loc[0]][(int)loc[1]]);
                    if(is_boundary((int)loc[0], (int)loc[1])){
                        if(inner){
                            //如果現在裡面(提醒區域),則inner改成false表示要出去了,下次進來要提醒
                            inner = false;
                        }
                        else{
                            //如果在外面表示正要進來提醒區域要提醒
                            new Reminder().execute();
                        }
                    }
                    Log.i(TAG, "------------------");
                }
            }
        };

        Fitness.SensorsApi.add(
                mClient,
                new SensorRequest.Builder()
                        .setDataSource(dataSource) // Optional but recommended for custom data sets.
                        .setDataType(dataType) // Can't be omitted.
                        .setSamplingRate(1, TimeUnit.MILLISECONDS)
                        .build(),
                distanceListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Listener registered!");
                        } else {
                            Log.i(TAG, "Listener not registered.");
                        }
                    }
                });
        // [END register_data_listener]
    }

    private boolean is_boundary(int x, int y){
        return map[x][y] != map[x-1][y] || map[x][y] != map[x][y-1] || map[x][y] != map[x+1][y] || map[x][y] != map[x][y+1] ;
    }

    private void unregisterFitnessDataListener() {
        if (distanceListener == null) {
            // This code only activates one listener at a time.  If there's no listener, there's
            // nothing to unregister.
            return;
        }

        // [START unregister_data_listener]
        // Waiting isn't actually necessary as the unregister call will complete regardless,
        // even if called from within onStop, but a callback can still be added in order to
        // inspect the results.
        Fitness.SensorsApi.remove(
                mClient,
                distanceListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.i(TAG, "Listener was removed!");
                        } else {
                            Log.i(TAG, "Listener was not removed.");
                        }
                    }
                });
        // [END unregister_data_listener]
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_unregister_listener) {
            unregisterFitnessDataListener();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     *  Initialize a custom log class that outputs both to in-app targets and logcat.
     */
    private void initializeLogging() {
        // Wraps Android's native log framework.
        LogWrapper logWrapper = new LogWrapper();
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.setLogNode(logWrapper);
        // Filter strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);
        // On screen logging via a customized TextView.
        LogView logView = (LogView) findViewById(R.id.sample_logview);

        // Fixing this lint errors adds logic without benefit.
        //noinspection AndroidLintDeprecation
        logView.setTextAppearance(this, R.style.Log);

        logView.setBackgroundColor(Color.WHITE);
        msgFilter.setNext(logView);
        Log.i(TAG, "Ready");
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            Snackbar.make(
                    findViewById(R.id.create_map_activity_view),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(TracingActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(TracingActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                buildFitnessClient();
            } else {
                // Permission denied.

                // In this Activity we've chosen to notify the user that they
                // have rejected a core permission for the app since it makes the Activity useless.
                // We're communicating this message in a Snackbar since this is a sample app, but
                // core permissions would typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                Snackbar.make(
                        findViewById(R.id.create_map_activity_view),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            android.util.Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            android.util.Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // Remove post value event listener
        if (mNodeInfoListener != null) {
            NodeInfoReference.removeEventListener(mNodeInfoListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterFitnessDataListener();
        sensorManager.unregisterListener(rotationVectorListener);
        Toast.makeText(this, "解除追蹤", Toast.LENGTH_LONG).show();
    }

    private SensorEventListener rotationVectorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            SensorManager.getRotationMatrixFromVector(mRotationMatrix, sensorEvent.values);//存取原數據
            SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, mRotationMatrix);//座標軸變換
            SensorManager.getOrientation(mRotationMatrix, mOrientation);//存取移動方向
            // Store the pitch (used to display a message indicating that the user's head
            // angle is too steep to produce reliable results.
            magneticHeading = (float) Math.toDegrees(mOrientation[0]);//弧度轉度數
            magneticHeading = MathUtils.mod(computeTrueNorth(magneticHeading), 360.0f);//正北
            //data[6] = sensorEvent.values;
            data[0] = magneticHeading;//方位角
            //Log.i(TAG, "magneticHeading value" + String.valueOf(data[0]));
            data[1] = (float) Math.toDegrees(mOrientation[1]);//傾斜
            data[2] = (float) Math.toDegrees(mOrientation[2]);//滾動
            //DebugLog.show(data[6].length);
            //Log.i(TAG, "方位角: " + String.valueOf(data[0]));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }

        private float computeTrueNorth(float heading) {
            if (mGeomagneticField != null)
            {
                return heading + mGeomagneticField.getDeclination();
            }
            else
            {
                return heading;
            }
        }
    };
}
