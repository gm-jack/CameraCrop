package com.example.cameracrop;

import android.graphics.Bitmap;
import android.graphics.Point;

import java.io.Serializable;

public class HuaweiCropBean implements Serializable {
    private Bitmap bitmap;
    private Point[] point;
    private double blurry;

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Point[] getPoint() {
        return point;
    }

    public void setPoint(Point[] point) {
        this.point = point;
    }

    public double getBlurry() {
        return blurry;
    }

    public void setBlurry(double blurry) {
        this.blurry = blurry;
    }
}
