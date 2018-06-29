package com.jackchen.test_06_04_2017;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.android.gms.fit.samples.common.logger.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingActivity extends AppCompatActivity {

    //元件
    private ListView listView;
    private ViewAdapter adapter;
    //圖資
    private String mapKey;
    private ArrayList<NodeInfo> nodes = new ArrayList<NodeInfo>();
    private ArrayList<String> notificationData = new ArrayList<String>();
    private ArrayList<String> coordinationData = new ArrayList<String>();
    private List<Map<String, String>> items = new ArrayList<Map<String, String>>();
    //資料庫
    private DatabaseReference NodeInfoReference;
    private ValueEventListener mNodeInfoListener;
    private boolean is_first = true;
    private ValueEventListener NodeInfoListener = new ValueEventListener() {
        //存取圖資
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            for(DataSnapshot nodeSnapshot : dataSnapshot.getChildren()){
                NodeInfo node = nodeSnapshot.getValue(NodeInfo.class);
                nodes.add(node);
                //
                notificationData.add(node.notification);
                int x = Integer.parseInt(node.coordination) / 1000;
                int y = Integer.parseInt(node.coordination) % 1000;
                coordinationData.add("(" + x + " , " + y + ")");
            }
            // [START_EXCLUDE]
            //在listView上設定資訊
            if(is_first){
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                adapter = new ViewAdapter(coordinationData, notificationData, inflater, 2);
                listView.setAdapter(adapter);
                android.util.Log.i(TAG, "Adapter set");
                //listView.setOnItemClickListener(onClickListView);
                // [END_EXCLUDE]*/
                is_first = false;
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
    //Log
    private static final String DB = "FireBase";
    private static final String TAG = "BasicSensorApi";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        Intent intent = this.getIntent();
        mapKey = intent.getStringExtra("key");
        android.util.Log.i(TAG, "Get the map's key: " + mapKey);
        // Initialize Database
        NodeInfoReference = FirebaseDatabase.getInstance().getReference().child("NodeInfo").child(mapKey);
        android.util.Log.i(TAG, " Initialize Database");
        // Add value event listener to the post
        // [START post_value_event_listener]
        NodeInfoReference.addValueEventListener(NodeInfoListener);
        // [END post_value_event_listener]
        // Keep copy of post listener so we can remove it when app stops
        mNodeInfoListener = NodeInfoListener;
        //
        findViews();
    }

    private void findViews(){
        listView = (ListView) findViewById(R.id.listview2);
        this.registerForContextMenu(listView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        super.onCreateContextMenu(menu, v, menuInfo);
        if(v == listView){
            menu.setHeaderIcon(R.mipmap.ic_launcher);//ICON設定
            menu.setHeaderTitle("請選擇:");
            menu.add(0, 0, 0, "設定提醒內容");
            menu.add(0, 1, 0, "刪除");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case 0:
                final EditText et=new EditText(SettingActivity.this);
                et.setHint(nodes.get(menuInfo.position).notification);
                AlertDialog dialog = new AlertDialog.Builder(SettingActivity.this).setTitle("請設定您的提醒內容:").setView(et).
                        setPositiveButton("修改", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1){
                                String notification = et.getText().toString();//提醒內容
                                NodeInfoReference.child(String.valueOf(menuInfo.position)).child("notification").setValue(notification);
                                //更新listView項目,以免舊的選項殘留
                                int d = menuInfo.position;
                                notificationData.remove(d);
                                coordinationData.remove(d);
                                notificationData.clear();
                                coordinationData.clear();
                                
                                adapter.notifyDataSetChanged();
                                android.util.Log.i(TAG, "Adapter changed");
                                arg0.dismiss();
                            }
                        }).setNegativeButton("取消",null).show();
                dialog.setCancelable(false);
                break;
            case 1:
                AlertDialog dialog2 = new AlertDialog.Builder(SettingActivity.this).setTitle("您確定要刪除該提醒位置嗎?").
                        setPositiveButton("是", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1){
                                NodeInfoReference.child(String.valueOf(menuInfo.position)).removeValue();
                                nodes.remove(menuInfo.position);
                                //更新listView項目,以免舊的選項殘留
                                int d = menuInfo.position;
                                notificationData.remove(d);
                                coordinationData.remove(d);
                                adapter.notifyDataSetChanged();
                                arg0.dismiss();
                            }
                        }).setNegativeButton("否",null).show();
                dialog2.setCancelable(false);
                break;
            default:    break;
        }
        return super.onContextItemSelected(item);
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
    protected void onDestroy(){
        super.onDestroy();
        unregisterForContextMenu(listView);
    }
}
