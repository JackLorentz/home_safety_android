package com.jackchen.test_06_04_2017;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import org.opencv.android.Utils;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Created by jackchen on 2017/7/31.
 */

public class ORBprocessing {
    private Bitmap bmpObjToRecognize, bmpScene, bmpMatchedScene;
    private double minDistance, maxDistance;
    private Scalar RED = new Scalar(255,0,0);
    private Scalar GREEN = new Scalar(0,255,0);
    private int matchesFound;

    public Bitmap getObjToRecognize() {
        return bmpObjToRecognize;
    }

    public void setObjToRecognize(Bitmap bmpObjToRecognize) {
        this.bmpObjToRecognize = bmpObjToRecognize;
    }

    public Bitmap getScene() {
        return bmpScene;
    }

    public void setScene(Bitmap bmpScene) {
        this.bmpScene = bmpScene;
    }

    public double getMinDistance() {
        return minDistance;
    }

    public void setMinDistance(double minDistance) {
        this.minDistance = minDistance;
    }

    public double getMaxDistance() {
        return maxDistance;
    }

    public void setMaxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
    }

    public void RGB2HSV(String filepath, File addr){
        Bitmap source = BitmapFactory.decodeFile(filepath);
        int width = source.getWidth();
        int height = source.getHeight();
        int[] pixels = new int[width * height];
        float[] hsv = new float[3];

        source.getPixels(pixels, 0, width, 0, 0, width, height);

        for(int i=0; i<pixels.length; i++) {
            Color.colorToHSV(pixels[i], hsv);
            pixels[i] = Color.HSVToColor(hsv);
        }
        Bitmap result = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);
        //
        if(addr.exists()){
            addr.delete();
        }
        FileOutputStream out;
        try{
            out = new FileOutputStream(addr);
            if(result.compress(Bitmap.CompressFormat.PNG, 100, out));
            out.close();
        }
        catch (FileNotFoundException e){
            e.printStackTrace();
        }
        catch (IOException e){
            e.printStackTrace();
        }
    }

    public int detectObject() {
        //Declaration
        Mat mObjectMat = new Mat();
        Mat mSceneMat = new Mat();
        MatOfDMatch matches = new MatOfDMatch();
        List<DMatch> matchesList;
        LinkedList<DMatch> good_matches = new LinkedList<>();
        MatOfDMatch gm = new MatOfDMatch();
        LinkedList<Point> objList = new LinkedList<>();
        LinkedList<Point> sceneList = new LinkedList<>();
        MatOfPoint2f obj = new MatOfPoint2f();
        MatOfPoint2f scene = new MatOfPoint2f();

        MatOfKeyPoint keypoints_object = new MatOfKeyPoint();
        MatOfKeyPoint keypoints_scene = new MatOfKeyPoint();
        Mat descriptors_object = new Mat();
        Mat descriptors_scene = new Mat();

        //Bitmap to Mat
        Utils.bitmapToMat(bmpObjToRecognize, mObjectMat);
        Utils.bitmapToMat(bmpScene, mSceneMat);
        Mat img3 = mSceneMat.clone();

        //Use the FeatureDetector interface in order to find interest points/keypoints in an image.
        FeatureDetector fd = FeatureDetector.create(FeatureDetector.ORB);
        fd.detect(mObjectMat, keypoints_object );
        fd.detect(mSceneMat, keypoints_scene );

        //DescriptorExtractor
        //A descriptor extractor is an algorithm that generates a description of a keypoint that
        // makes this keypoint recognizable by a matcher. Famous descriptors are SIFT, FREAK...
        DescriptorExtractor extractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        extractor.compute(mObjectMat, keypoints_object, descriptors_object );
        extractor.compute(mSceneMat, keypoints_scene, descriptors_scene );

        //DescriptorMatcher
        //Use a DescriptorMatcher for matching keypoint descriptors.
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        matcher.match( descriptors_object, descriptors_scene, matches);

        //Calculate max and min distances between keypoints
        matchesList = matches.toList();
        for( int i = 0; i < descriptors_object.rows(); i++ )
        {
            Double dist = (double) matchesList.get(i).distance;
            if( dist < minDistance ) minDistance = dist;
            if( dist > maxDistance ) maxDistance = dist;
        }

        ////Draw only good matches
        for(int i = 0; i < descriptors_object.rows(); i++){
            if(matchesList.get(i).distance < 3*minDistance){
                good_matches.addLast(matchesList.get(i));
            }
        }
        gm.fromList(good_matches);
        matchesFound = good_matches.size();

        return matchesFound;
    }
}
