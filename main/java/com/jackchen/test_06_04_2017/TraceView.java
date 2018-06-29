package com.jackchen.test_06_04_2017;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.graphics.Canvas;
import android.view.View;

import com.google.android.gms.fit.samples.common.logger.Log;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by jackchen on 2017/6/5.
 */

public class TraceView extends View implements Serializable {

    private static final String TAG = "BasicSensorApi";
    //儲存到JSON樹的形式
    private Mapdata mapdata = new Mapdata();
    //元件
    private int signal = 0;
    private Paint paint, cPaint;
    private Canvas canvas;
    private Bitmap bitmap, scene;
    private Path drawPath, drawNode;
    private int first = 0 ;
    //紀錄
    private  int[][] map = new int[1000][1000];
    //位移計算參數
    private float x = 200f, y = 110f, tmpX, tmpY;
    private double distanceDelta;
    private float stepDelta;
    private double stepLength = 0.5;
    private float[] data = new float[3];


    public TraceView(Context context) {
        this(context, null);
    }

    public TraceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TraceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    public void setName(String n) {
        mapdata.setName(n);
    }

    public  void buildCoordinate(){
        mapdata.buildCoordinate(x, y);
    }

    public void makeNode(){
        drawNode.addCircle(x, y, 10 , Path.Direction.CW);
    }

    public void dataListener(float i_delta, float[] i){
        distanceDelta = i_delta / 3 ;
        data = i;
    }

    public Mapdata generateMapdata(){
        return mapdata;
    }

    public int getNodesAmount(){
        return mapdata.getNodesAmount();
    }

    public float[] getXY(){
        float[] tmp = new float[4];
        tmp[0] = x;
        tmp[1] = y;
        tmp[2] = (float)distanceDelta;
        tmp[3] = data[0];
        return tmp;
    }

    public void pause(){
        signal = 1;
        tmpX = x;
        tmpY = y;
    }

    public void resume(){
        signal = 0;
    }

    public void correctLocation(float a, float b){
        this.x = a;
        this.y = b;
    }

    private void initialize() {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        paint.setStrokeCap(Paint.Cap.ROUND);
        //
        cPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cPaint.setColor(Color.BLUE);
        //
        bitmap = Bitmap.createBitmap(350, 250, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        drawPath = new Path();
        drawNode = new Path();
    }

    private void moveOn(){
        //distanceDelta = stepDelta * stepLength;
        if((data[0] < 45 && data[0] >= 0) || data[0] > 315){//N
            y -= (float)(distanceDelta)*10;
        }
        else if(data[0] >= 225 && data[0] < 315) {//W
            x -= (float)(distanceDelta)*10;
        }
        else if(data[0] >= 45 && data[0] < 135) {//E
            x += (float)(distanceDelta)*10;
        }
        else{//S
            y += (float)(distanceDelta)*10;
        }
        /*
        if((data[0] < 22.5 && data[0] >= 0) || data[0] > 337.5){//N
            y -=  (float)(distanceDelta)*10;
        }
        else if(data[0] >= 22.5 && data[0] < 67.5) {//NE
            y -= (float)(distanceDelta*0.7)*10;
            x += (float)(distanceDelta*0.7)*10;
        }
        else if(data[0] >= 67.5 && data[0] < 112.5) {//E
            x += (float)(distanceDelta)*10;
        }
        else if(data[0] >= 112.5 && data[0] < 157.5) {//SE
            y += (float)(distanceDelta*0.7)*10;
            x += (float)(distanceDelta*0.7)*10;
        }
        else if(data[0] >= 157.5 && data[0] < 202.5){//S
            y += (float)(distanceDelta)*10;
        }
        else if(data[0] >= 205.5 &&  data[0] < 247.5){//SW
            y += (float)(distanceDelta*0.7)*10;
            x -= (float)(distanceDelta*0.7)*10;
        }
        else if(data[0] >= 247.5 && data[0] < 292.5){//W
            x -= (float)(distanceDelta)*10;
        }
        else{//NW
            y -= (float)(distanceDelta*0.7)*10;
            x -= (float)(distanceDelta*0.7)*10;
        }
        */
    }

    private boolean is_limit() {
        return x > 0 && x < 340 && y > 0 && y < 250;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = View.MeasureSpec.getSize(widthMeasureSpec);
        int h = View.MeasureSpec.getSize(heightMeasureSpec);

        //int size = Math.min(w, h);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //
        moveOn();
        //
        if(is_limit())
        {
            drawPath.lineTo(x, y);/*問題*/
            drawNode.lineTo(0, 0);
        }
        if(signal == 1){
            x = tmpX;
            y = tmpY;
        }
        //canvas.drawBitmap(bitmap, x, y, paint);
        canvas.drawBitmap(bitmap, 0, 0, cPaint);
        canvas.drawPath(drawPath, paint);
        canvas.drawPath(drawNode, cPaint);
        //
        distanceDelta = 0;
        //Log.i(TAG, "line to (x, y) = (" + x + ", " + y + ")");

        this.invalidate();
    }
}
