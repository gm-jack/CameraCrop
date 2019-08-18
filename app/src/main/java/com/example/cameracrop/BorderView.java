package com.example.cameracrop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.cameracrop.util.SpUtil;


/**
 * 渲染边框
 */
public class BorderView extends View {
    private final String takePicture;
    private Paint textPaint;
    private Context mContext;
    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private Paint mPaint = new Paint();
    private Bitmap mBitmap;
    private Point[] mCropPoints;
    private float mMatriX = 1;
    private float mMatriY = 1;
    private Path mPointLinePath = new Path();
    private Matrix mMatrix;
    private boolean isCanPicture = false;

    public BorderView(Context context) {
        this(context, null);
    }

    public BorderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(Color.argb(70, 255, 0, 0));
//        mPaint.setStrokeWidth(SpUtil.dip2px(context,2));
        mPaint.setStyle(Paint.Style.FILL);

        //初始化画笔
        textPaint = new Paint();
        textPaint.setTextSize(SpUtil.dip2px(context, 18));
        textPaint.setColor(Color.WHITE);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);

        mMatrix = new Matrix();

        takePicture = "拍摄中";
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    public void setBitmap(@NonNull Bitmap bitmap) {
        mBitmap = bitmap;
        postInvalidate();
    }

    public void setPoint(Point[] cropPoint, Bitmap bitmap) {
        if (cropPoint != null && cropPoint.length == 4 && cropPoint[0] != null && cropPoint[1] != null && cropPoint[2] != null && cropPoint[3] != null) {
//            checkCanPicture(cropPoint);
            this.mCropPoints = cropPoint;
        }
        mBitmap = bitmap;
        if (bitmap != null) {
            mMatriX = getWidth() / (float) bitmap.getWidth();
            mMatriY = getHeight() / (float) bitmap.getHeight();
        }
        postInvalidate();
    }

//    private void checkCanPicture(Point[] cropPoint) {
//        if(crop)
//    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        if (mBitmap != null && !mBitmap.isRecycled())
//            canvas.drawBitmap(mBitmap, mMatrix, mPaint);
        if (mCropPoints != null) {
            canvas.drawPath(resetPointPath(canvas), mPaint);
            if (mBitmap != null && !mBitmap.isRecycled()) {
                mBitmap.recycle();
            }
        }
    }

    private Path resetPointPath(Canvas canvas) {
        mPointLinePath.reset();
        Point lt = mCropPoints[0];
        Point rt = mCropPoints[1];
        Point rb = mCropPoints[2];
        Point lb = mCropPoints[3];
        float ltx = lt.x * mMatriX;
        float lty = lt.y * mMatriY;
        float rtx = rt.x * mMatriX;
        float rty = rt.y * mMatriY;
        float rbx = rb.x * mMatriX;
        float rby = rb.y * mMatriY;
        float lbx = lb.x * mMatriX;
        float lby = lb.y * mMatriY;
//        ltx = Math.min(ltx, lbx);
//        lbx = ltx;
//        lty = Math.min(lty, rty);
//        rty = lty;
//        lby = Math.max(lby, rby);
//        rby = lby;
//        rbx = Math.max(rbx, rtx);
//        rtx = rbx;

        if (GetIntersect(ltx, lty, rtx, rty, rbx, rby, lbx, lby) == 1 && duijiao(ltx, lty, rtx, rty, rbx, rby, lbx, lby) == 1) {
            isCanPicture = true;
            canvas.drawText("请保持设备平稳", (rtx - ltx) / 2 + ltx, (lby - lty) / 2 + lty, textPaint);
            mPointLinePath.moveTo(ltx, lty);
            mPointLinePath.lineTo(rtx, rty);
            mPointLinePath.lineTo(rbx, rby);
            mPointLinePath.lineTo(lbx, lby);
        } else {
            isCanPicture = false;
        }
        mPointLinePath.close();
        return mPointLinePath;
    }

    /**
     * 是否检测到文档
     *
     * @return
     */
    public boolean isPointShow() {
        if (mCropPoints != null) {
            Point lt = mCropPoints[0];
            Point rt = mCropPoints[1];
            Point rb = mCropPoints[2];
            Point lb = mCropPoints[3];
            if (lt != null && rt != null && rb != null && lb != null) {
                if (lt.x != 0 && lt.y != 0 && rt.x != 0 && rt.y != 0 && rb.x != 0 && rb.y != 0 && lb.x != 0 && lb.y != 0) {
                    return true;
                }
            }
        }
        Toast.makeText(mContext, "未识别到文档", Toast.LENGTH_SHORT).show();
        return false;
    }

    /**
     * 检测是否可以拍摄
     *
     * @return
     */
    public boolean isCanPicture() {
        return isCanPicture;
    }

    public void setCanPicture(boolean canPicture) {
        isCanPicture = canPicture;
    }

    /**
     * 判断对角线是否相交
     *
     * @param ltx
     * @param lty
     * @param rtx
     * @param rty
     * @param rbx
     * @param rby
     * @param lbx
     * @param lby
     * @return
     */
    private int duijiao(float ltx, float lty, float rtx, float rty, float rbx, float rby,
                        float lbx, float lby) {
        //L1: a1x+b1y=c1
        float a1 = rby - lty;
        float b1 = ltx - rbx;
        float c1 = ltx * rby - rbx * lty;
        //L2: a2x+b2y=c2
        float a2 = lby - rty;
        float b2 = rtx - lbx;
        float c2 = rtx * lby - lbx * rty;

        float detab = a1 * b2 - a2 * b1;
        if (detab == 0) {
            float r;
            if (a2 != 0) r = a1 / a2;
            else r = b1 / b2;

            if (c1 == 0 && c2 == 0) return 0;
            if (r == c1 / c2) return 0;
            else return 0;
        }
        return 1;
    }

    /**
     * 判断对角角度是否符合
     *
     * @param ltx
     * @param lty
     * @param rtx
     * @param rty
     * @param rbx
     * @param rby
     * @param lbx
     * @param lby
     * @return
     */
    private int GetIntersect(float ltx, float lty, float rtx, float rty, float rbx, float rby,
                             float lbx, float lby) {
        if (getAngle(rtx - ltx, rty - lty, lbx - ltx, lby - lty)
                && getAngle(rtx - rbx, rty - rby, lbx - rbx, lby - rby))
            return 1;
        return 0;
    }

    /**
     * 获取两条边的角度
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    private boolean getAngle(float x1, float y1, float x2, float y2) {
        float dot = x1 * x2 + y1 * y2;
        float det = x1 * y2 - y1 * x2;
        double angle = (Math.atan2(det, dot) / Math.PI * 180 + 360) % 360;
        if (angle > 180) {
            angle -= 180;
        }
        return angle > 75 && angle < 105;
    }

    public void restore() {
        mCropPoints = null;
        mBitmap = null;
        isCanPicture = false;
        postInvalidate();
    }
}
