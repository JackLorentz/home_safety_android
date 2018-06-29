package com.jackchen.test_06_04_2017;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jackchen on 2017/9/12.
 */

public class NodeInfo {
    public String coordination;
    public String imagefile;
    public String notification;
    private int opt;

    public NodeInfo(){

    }

    public NodeInfo(int coordinate, String imagefile, String notification){
        this.coordination = String.valueOf(coordinate);
        this.imagefile = imagefile;
        this.notification = notification;
        opt = 2;
    }

    public NodeInfo(String notification, String imagefile){
        this.imagefile = imagefile;
        this.notification = notification;
        opt = 1;
    }

    public NodeInfo(int coordinate){
        this.coordination = String.valueOf(coordinate);
        opt = 0;
    }

    @Exclude
    public Map<String, Object> toMap(){
        HashMap<String, Object> result = new HashMap<>();
        switch(opt) {
            case 0:
                result.put("coordination", coordination);
                break;
            case 1:
                result.put("imagefile", imagefile);
                result.put("notification", notification);
                break;
            case 2:
                result.put("coordination", coordination);
                result.put("imagefile", imagefile);
                result.put("notification", notification);
                break;
        }
        return result;
    }
}
