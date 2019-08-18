package com.example.cameracrop;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.cameracrop.util.CameraUtils;


public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = CameraSurfaceView.class.getSimpleName();

    private SurfaceHolder mSurfaceHolder;
    private CameraCallback mPreCallback;
    private Context mContext;

    public CameraSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (CameraUtils.getCamera() == null)
            CameraUtils.openFrontalCamera(CameraUtils.DESIRED_PREVIEW_FPS);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        CameraUtils.startPreviewDisplay(holder, mPreCallback);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        CameraUtils.stopPreview();
    }

    private int getDeviceAspectRatio(Activity activity, int width1) {
        int width = activity.getWindow().getDecorView().getWidth();
        int height = activity.getWindow().getDecorView().getHeight();
        return width / width1 * height;
    }

    public void setPreviewCallback(CameraCallback callback) {
        this.mPreCallback = callback;
    }
}