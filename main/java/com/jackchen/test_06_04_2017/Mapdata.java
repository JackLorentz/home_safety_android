package com.jackchen.test_06_04_2017;

import android.support.design.widget.CoordinatorLayout;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * Created by jackchen on 2017/9/7.
 */

public class Mapdata {
    public static final String TAG = "Mapdata: ";
    //欲儲存至雲端資料
    private int mapID;
    private String name;//地圖名稱
    private int[][] tempMap = new int[1000][1000];//位置紀錄;
    private int count = 1;
    private int drawed = 0;
    private String map = "";//地圖(二元形式)

    private class Coordinates{
        public float x,y;//資訊點座標
    }
    private ArrayList<Coordinates> coordinates = new ArrayList<Coordinates>();

    private class Node{
        public String notification;//提醒資訊
        public String imagePath;//資訊點照片
    }//資訊點
    private ArrayList<Node> nodeInfo = new ArrayList<Node>();

    public void setName(String n){
        name = n;
    }

    public void buildCoordinate(float a, float b){
        Coordinates tmp = new Coordinates();
        tmp.x = a;
        tmp.y = b;
        coordinates.add(tmp);
        //
        for(int i=(int)a-10; i<(int)a+10; i++)
        {
            for(int j=(int)b-10; j<(int)b+10; j++)
            {
                tempMap[i][j] = count;
            }
        }
        count++;
    }

    public int getMapID(){
        return mapID;
    }

    public int getNodesAmount(){
        return coordinates.size();
    }

    public String getName(){
        return name;
    }

    public int[][] getMap(){
        /*if(drawed == 0) map = "0";
        mapFormatTransform();*/
        return tempMap;
    }

    public int getCoordination(int i){
        int a,b;
        a = (int)coordinates.get(i).x;
        b = (int)coordinates.get(i).y;
        return a*1000 + b;
    }

    public String getNotification(int i){
        return nodeInfo.get(i).notification;
    }

    public String getImagePath(int i){
        return nodeInfo.get(i).imagePath;
    }

    private void mapFormatTransform(){
        for(int i=0; i<1000; i++){
            for(int j=0; j<1000; j++){
                map = map.concat(String.valueOf(tempMap[i][j]));
                //Log.i(TAG, "Map's Binary String : " + map);
            }
        }
    }
}
