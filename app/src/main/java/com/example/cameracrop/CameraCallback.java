package com.example.cameracrop;

import android.hardware.Camera;

public interface CameraCallback {

    void callback(byte[] data, Camera camera);
}
