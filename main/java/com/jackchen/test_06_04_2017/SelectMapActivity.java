package com.jackchen.test_06_04_2017;


import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.fit.samples.common.logger.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;
import java.util.HashMap;



public class SelectMapActivity extends AppCompatActivity {
    //元件
    private ListView listView;
    private ViewAdapter adapter;
    //資料庫
    private static final String TAG = "BasicSensorApi";
    private DatabaseReference trackReference, nodeInfoReference;
    private ValueEventListener mTrackListener;
    private HashMap<String, String> mapFile = new HashMap<>();
    private HashMap<String, String> keys = new HashMap<>();
    private ArrayList<String> nameData = new ArrayList<String>();
    private ArrayList<String> timeData = new ArrayList<String>();
    private boolean is_first = true;
    private ValueEventListener trackListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            // Get Post object and use the values to update the UI
            for(DataSnapshot postSnapshot : dataSnapshot.getChildren()){
                Post post = postSnapshot.getValue(Post.class);
                nameData.add(post.name);
                timeData.add(post.time);
                mapFile.put(post.name, post.map);
                keys.put(post.name, post.key);
            }
            // [START_EXCLUDE]
            //在listView上設定資訊
            if(is_first){
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                adapter = new ViewAdapter(nameData, timeData, inflater, 1);
                listView.setAdapter(adapter);
                // [END_EXCLUDE]*/
                is_first = false;
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            Log.w(TAG, "loadTraceView:onCancelled", databaseError.toException());
            // [START_EXCLUDE]
            Toast.makeText(SelectMapActivity.this, "Failed to load traceView.", Toast.LENGTH_SHORT).show();
            // [END_EXCLUDE]
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_map);
        // Initialize Database
        trackReference = FirebaseDatabase.getInstance().getReference().child("Track");
        nodeInfoReference = FirebaseDatabase.getInstance().getReference().child("NodeInfo");
        // Add value event listener to the post
        // [START post_value_event_listener]
        trackReference.addValueEventListener(trackListener);
        // [END post_value_event_listener]

        // Keep copy of post listener so we can remove it when app stops
        mTrackListener = trackListener;
        //初始化元件
        findViews();
    }

    private void findViews() {
        listView = (ListView) findViewById(R.id.listview);
        this.registerForContextMenu(listView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
        super.onCreateContextMenu(menu, v, menuInfo);
        if(v == listView){
            menu.setHeaderIcon(R.mipmap.ic_launcher);//ICON設定
            menu.setHeaderTitle("請選擇:");
            menu.add(0, 0, 0, "開始追蹤");
            menu.add(0, 1, 0, "設定提醒內容");
            menu.add(0, 2, 0, "刪除");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case 0:
                Intent intent = new Intent(SelectMapActivity.this, TracingActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("key", keys.get((String)adapter.getItem(menuInfo.position)));
                bundle.putString("map", mapFile.get((String)adapter.getItem(menuInfo.position)));
                intent.putExtras(bundle);
                startActivity(intent);
                break;
            case 1:
                Intent intent2 = new Intent(SelectMapActivity.this, SettingActivity.class);
                intent2.putExtra("key", keys.get((String)adapter.getItem(menuInfo.position)));
                startActivity(intent2);
                break;
            case 2:
                nodeInfoReference.child(keys.get(nameData.get(menuInfo.position))).removeValue();
                trackReference.child(keys.get(nameData.get(menuInfo.position))).removeValue();
                //更新listView項目,以免舊的選項殘留
                //arraylist的index歸零以免繼續用原來指標位置增加
                int d = menuInfo.position;
                nameData.remove(d);
                timeData.remove(d);
                adapter.notifyDataSetChanged();
                break;
            default:    break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Remove post value event listener
        if (mTrackListener != null) {
            trackReference.removeEventListener(mTrackListener);
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterForContextMenu(listView);
    }

}
