package com.jamesgoodhouse.library.camerapreview;

import android.hardware.Camera;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.widget.FrameLayout;
import android.content.Context;
import android.content.pm.PackageManager;

public class CameraActivity extends Activity
{
    private static final String TAG = "CameraActivity";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Camera mCamera           = null;
        CameraPreview mPreview   = null;
        Camera.Parameters params = null;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.lib_activity_camera);

        if (checkCameraHardware(this)) {
            mCamera = getCameraInstance();
            params  = mCamera.getParameters();
        }
        else
            Log.e(TAG, "No camera available, brosive!");

        Log.e(TAG, params.getSupportedFocusModes().toString());
        Log.e(TAG, params.getFocusMode().toString());
        params.setFocusMode("continuous-picture");
        //Log.e(TAG, Camera.Parameters.FOCUS_MODE_AUTO);
        // set camera parameters
        //params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        mCamera.setParameters(params);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }

    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context)
    {
        if (context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            Log.e(TAG, "Yay! You have a camera!!");
            return true;
        } else {
            // no camera on this device
            Log.e(TAG, "Sadness! Your device has no camera. You should think of upgrading your device.");
            return false;
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance()
    {
        Camera c = null;
        try {
            Log.d(TAG, "Attempting to open camera, like a Sir.");
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            Log.e(TAG, "Sorry Sir, I could not access the camera. I swear it's not my fault!!");
        }
        return c; // returns null if camera is unavailable
    }
}