package com.jackchen.test_06_04_2017;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
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
import com.google.android.gms.fit.samples.common.logger.Log;
import com.google.android.gms.fit.samples.common.logger.LogView;
import com.google.android.gms.fit.samples.common.logger.LogWrapper;
import com.google.android.gms.fit.samples.common.logger.MessageOnlyLogFilter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.text.DecimalFormat;

import static android.Manifest.permission.CAMERA;

public class CreateMapActivity extends AppCompatActivity {

    //Log
    public static final String TAG = "BasicSensorsApi";
    private static final String PATH ="File Path";
    private static final String openCV = "openCV ORB";
    //元件
    private Button draw, record, location;
    private EditText info;
    //fitness api
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
    private boolean is_first = true;
    //旋轉向量
    private final float[] mRotationMatrix = new float[16];
    private final float[] mOrientation = new float[9];
    private float magneticHeading;
    private GeomagneticField mGeomagneticField;
    private float[] data = new float[3];
    //位移
    private float distanceDelta = 0;
    private float stepDelta;
    private int gate = 0;
    //軌跡
    private TraceView trace;
    //資料庫
    // [START declare_database_ref]
    DatabaseReference mapRef;
    // [END declare_database_ref]
    //時間
    private double startTime;
    private double seconds;
    private Handler handler = new Handler();
    private Runnable updateTimer = new Runnable() {
        public void run() {
            double spentTime = System.currentTimeMillis() - startTime;
            //計算目前已過分鐘數
            double minius = (spentTime/1000)/60;
            //計算目前已過秒數
            seconds= (spentTime/1000);
            handler.postDelayed(this, 1000);
            //text.setText("time : "+String.valueOf(timeForm.format(seconds))+"s");
        }
    };
    private SimpleDateFormat now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private String date;
    //image file
    private static final int REQUEST_CAPTURE = 0;
    private static final int REQUEST_CAMERA = 1;
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
    //印出格式
    private DecimalFormat formatter=new DecimalFormat("#.##");
    private DecimalFormat timeForm = new DecimalFormat("#");
    //地圖IO
    private int [][] tempMap;
    private FileWriter mapFile;
    private String mapPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_map);
        // [START initialize_database_ref]
        mapRef = FirebaseDatabase.getInstance().getReference();
        // [END initialize_database_ref]

        //與google API 取得聯繫
        // This method sets up our custom logger, which will print all log messages to the device
        // screen, as well as to adb logcat.
        initializeLogging();

        // When permissions are revoked the app is restarted so onCreate is sufficient to check for
        // permissions core to the Activity's functionality.
        if (!checkPermissions()) {
            requestPermissions();
        }
        //開啟相機權限
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if(permission != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[] {CAMERA, CAMERA_SERVICE}, REQUEST_CAMERA);
        }
        //計時器
        //取得目前時間
        /*startTime = System.currentTimeMillis();
        //設定定時要執行的方法
        handler.removeCallbacks(updateTimer);
        //設定Delay的時間
        handler.postDelayed(updateTimer, 1000);*/
        //
        now.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        date = now.format(new Date());

        findViews();
        //建立地圖名稱
        Intent intent = getIntent();
        String name = intent.getSerializableExtra("MapName").toString();
        trace.setName(name);
        Log.i(TAG, "Get map's name.");

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        rotationVectorSensor=sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    private void findViews() {
        draw = (Button)findViewById(R.id.button3);
        record = (Button)findViewById(R.id.button5);
        location = (Button)findViewById(R.id.button7);
        trace = (TraceView)findViewById(R.id.traceView);
        info = (EditText)findViewById(R.id.editText);
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(gate == 1) {
                    trace.buildCoordinate();
                    //
                    trace.makeNode();//在地圖
                }
            }
        });
        draw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(gate == 0) {
                    buildFitnessClient();//用Fitness API開始追蹤
                    Toast.makeText(CreateMapActivity.this, "開始追蹤", Toast.LENGTH_SHORT).show();
                    gate = 1;
                }
                else if(trace.getNodesAmount() == 0){
                    new AlertDialog.Builder(CreateMapActivity.this).setTitle("您沒有建立任何提醒內容,故無法建立建立地圖!").
                            setPositiveButton("了解", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    arg0.dismiss();
                                }
                            }).show();
                }
                else {
                    unregisterFitnessDataListener();
                    sensorManager.unregisterListener(rotationVectorListener);
                    new AlertDialog.Builder(CreateMapActivity.this).setTitle("您確定完成座標設定,要開始設定提醒資訊了嗎?").
                            setPositiveButton("是", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1){
                                    //將新地圖加入資料庫(Firebase)
                                    Mapdata mapdata = trace.generateMapdata();
                                    //地圖IO
                                    tempMap = mapdata.getMap();
                                    try{
                                        mapPath = getExternalCacheDir() + "/" + mapdata.getName() + "地圖.txt";
                                        mapFile = new FileWriter(mapPath, false);
                                        BufferedWriter bufferedWriter = new BufferedWriter(mapFile);
                                        for(int i=0; i<1000; i++){
                                            for(int j=0; j<1000; j++){
                                                bufferedWriter.write(String.valueOf(tempMap[i][j]));
                                            }
                                            bufferedWriter.newLine();
                                        }
                                        bufferedWriter.close();
                                        Log.i(TAG, "Write into the file : " + mapPath);
                                    }
                                    catch(IOException e){
                                        Log.e(TAG, "exception" , e);
                                    }
                                    //軌跡
                                    Map<String, Object> childUpdates = new HashMap<>();
                                    //填資料
                                    String postKey = mapRef.child("Track").push().getKey();
                                    Post post = new Post(mapdata.getName(), date, mapPath, postKey);
                                    Map<String, Object> postValues = post.toMap();
                                    //在子樹下建立節點
                                    childUpdates.put("/Track/" + postKey , postValues);
                                    //把所有更新結果存進資料庫
                                    //圖資
                                    for(int i=0; i<mapdata.getNodesAmount(); i++){
                                        NodeInfo node = new NodeInfo(mapdata.getCoordination(i), "null", "null");
                                        Map<String, Object> nodeValues = node.toMap();
                                        childUpdates.put("/NodeInfo/" + postKey + "/" + i + "/", nodeValues);
                                    }
                                    mapRef.updateChildren(childUpdates);
                                    //
                                    Intent intent = new Intent(CreateMapActivity.this, buildNodeActivity.class);
                                    Bundle bundle = new Bundle();
                                    bundle.putInt("size", mapdata.getNodesAmount());
                                    bundle.putString("key", postKey);
                                    intent.putExtras(bundle);
                                    startActivity(intent);
                                }
                            }).setNegativeButton("否",null).show();
                    gate = 0;
                }
            }
        });
        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CreateMapActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
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
                                    if (i == ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                        Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                                    } else if (i == ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
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
                                    CreateMapActivity.this.findViewById(R.id.create_map_activity_view),
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
                    Log.i(TAG, "Raw data(step) : " + tmp);
                    distanceDelta = Float.parseFloat(tmp);
                    if(distanceDelta >= 7){
                        distanceDelta = 1;
                    }
                    //Log.i(TAG, "Detected DataPoint field: " + field.getName());
                    //Log.i(TAG, "Detected DataPoint value: " + val);
                    trace.dataListener(distanceDelta, data);
                    //
                    float[] tmpData;
                    tmpData = trace.getXY();
                    Log.i(TAG, "(x , y) = " + "(" + tmpData[0] + " , " + tmpData[1] + ")");
                    Log.i(TAG, "Distance Delta Measurement : " + tmpData[2] + "m");
                    Log.i(TAG, "方位角 : " + tmpData[3]);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQUEST_CAPTURE: // 拍照
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                    //mImageView.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
        }
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
        int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);

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
                            ActivityCompat.requestPermissions(CreateMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(CreateMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
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
        sensorManager.registerListener(rotationVectorListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        Toast.makeText(this, "Register rotationVectorListener", Toast.LENGTH_SHORT).show();
        if (!OpenCVLoader.initDebug()) {
            android.util.Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            android.util.Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterFitnessDataListener();
        sensorManager.unregisterListener(rotationVectorListener);
        Toast.makeText(this, "解除追蹤", Toast.LENGTH_SHORT).show();
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
