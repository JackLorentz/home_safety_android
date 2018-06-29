package com.jackchen.test_06_04_2017;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jackchen on 2017/8/24.
 */

public class ViewAdapter extends BaseAdapter {

    private ArrayList<String> Data1 = new ArrayList<String>();
    private ArrayList<String> Data2 = new ArrayList<String>();
    private LayoutInflater inflater;
    private int opt;
    private int indentionBase;

    //優化Listview 避免重新加載
    //這邊宣告你會動到的Item元件
    static class ViewHolder{
        LinearLayout rlBorder;
        TextView Name;
        TextView Time;
    }

    //初始化
    public ViewAdapter(ArrayList<String> Data1, ArrayList<String> Data2, LayoutInflater inflater, int opt){
        this.Data1 = Data1;
        this.Data2 = Data2;
        this.inflater = inflater;
        this.opt = opt;
        indentionBase = 100;
    }

    @Override
    public int getCount() {
        return Data1.size();
    }

    @Override
    public Object getItem(int position) {
        return Data1.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        //當ListView被拖拉時會不斷觸發getView，為了避免重複加載必須加上這個判斷
        if(convertView == null){
            holder = new ViewHolder();
            if(opt == 1){
                convertView = inflater.inflate(R.layout.style_listview, null);
            }
            else{
                convertView = inflater.inflate(R.layout.style_setting_view, null);
            }
            holder.Name = (TextView) convertView.findViewById(R.id.Name);
            holder.Time = (TextView) convertView.findViewById(R.id.Time);
            holder.rlBorder = (LinearLayout) convertView.findViewById(R.id.llBorder);
            convertView.setTag(holder);
        }
        else{
            holder = (ViewHolder) convertView.getTag();
        }
        //不同類型用不同Style的表現方式
        holder.Name.setText(Data1.get(position));
        holder.Time.setText(Data2.get(position));
        //holder.rlBorder.setBackgroundColor(Color.parseColor("#FFDBC9"));

        return convertView;
    }
}
