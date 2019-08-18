package com.example.cameracrop;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import com.example.cameracrop.util.CameraUtils;
import com.example.cameracrop.util.ImageUtils;
import com.example.cameracrop.util.NV21ToBitmap;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import me.pqpo.smartcropperlib.SmartCropper;

public class CameraActivity extends Activity implements CameraCallback {
    @BindView(R.id.fl_camera)
    FrameLayout mFrameLayout;
    @BindView(R.id.border_camera)
    BorderView mBorder;

    HuaweiCropBean mHuaweiCropBean = new HuaweiCropBean();
    private NV21ToBitmap mNv21ToBitmap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_camera);
        ButterKnife.bind(this);
        initPermission();
        mNv21ToBitmap = new NV21ToBitmap(CameraActivity.this);
    }

    private void initPermission() {
        if (AndPermission.hasPermissions(this, Permission.CAMERA)) {
            initView();
        } else {
            AndPermission.with(this)
                    .runtime()
                    .permission(Permission.CAMERA)
                    .onGranted(data -> initView())
                    .onDenied(data -> {

                    })
                    .start();
        }
    }


    /**
     * 初始化View
     */
    public void initView() {

        //初始化Camera的预览布局
        CameraSurfaceView cameraSurfaceView = new CameraSurfaceView(this);
        cameraSurfaceView.setPreviewCallback(this);
        mFrameLayout.addView(cameraSurfaceView);
    }

    /**
     * 关闭相机和传感器
     */
    private void stopCameraAndSensor() {
        CameraUtils.stopPreview();
        if (mBorder != null) {
            mBorder.restore();
        }
    }

    /**
     * 开启相机和传感器
     */
    private void startCameraAndSensor() {
        CameraUtils.startPreview();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        CameraUtils.releaseCamera();
        CameraUtils.setIsSafeTakePicture(true);
    }

    @OnClick(R.id.fl_camera)
    public void lookFocus() {
        if (!CameraUtils.getFocusMode())
            CameraUtils.startFocus();
    }


    /**
     * camera 每一帧回调
     *
     * @param data
     * @param camera
     */
    @SuppressLint("CheckResult")
    @Override
    public void callback(byte[] data, Camera camera) {
        //SmartCropper
        Flowable.create((FlowableOnSubscribe<HuaweiCropBean>) emitter -> {
//            long l1 = System.nanoTime();
            Camera.Size previewSize = CameraUtils.getPreviewSize();
            if (previewSize != null) {
                Bitmap byteArrayBitmap = mNv21ToBitmap.nv21ToBitmap(data, previewSize.width, previewSize.height);
                Bitmap bitmap = ImageUtils.getRotatedBitmap(byteArrayBitmap, 90);

                mHuaweiCropBean.setPoint(SmartCropper.scan(bitmap));
                mHuaweiCropBean.setBitmap(bitmap);
                mHuaweiCropBean.setBlurry(SmartCropper.getBlurry(bitmap));
                emitter.onNext(mHuaweiCropBean);
            }
            long l2 = System.nanoTime();
//            Log.e("时间差   " , (l2 - l1) / 1000 / 1000 + "  清晰度 " + mHuaweiCropBean.getBlurry());
        }, BackpressureStrategy.LATEST)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(huaweiCropBean -> {
                    mBorder.setPoint(huaweiCropBean.getPoint(), huaweiCropBean.getBitmap());
                    camera.addCallbackBuffer(data);
                });
    }
}
