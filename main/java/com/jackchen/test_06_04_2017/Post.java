package com.jackchen.test_06_04_2017;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jackchen on 2017/9/12.
 */

public class Post {
    public String name;
    public String time;
    public String map;
    public String key;

    public Post(){

    }

    public Post(String name, String time, String map, String key){
        this.name = name;
        this.time = time;
        this.map = map;
        this.key = key;
    }

    @Exclude
    public Map<String, Object> toMap(){
        HashMap<String, Object> result = new HashMap<>();
        result.put("name", name);
        result.put("time", time);
        result.put("map", map);
        result.put("key", key);

        return result;
    }
}
