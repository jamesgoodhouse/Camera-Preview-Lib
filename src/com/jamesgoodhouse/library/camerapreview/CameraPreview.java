// https://github.com/pikanji/CameraPreviewSample/blob/master/src/net/pikanji/camerapreviewsample/CameraPreview.java

package com.jamesgoodhouse.library.camerapreview;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback
{
    private final String LOG_TAG = this.getClass().getSimpleName();

    private int orientation = getResources().getConfiguration().orientation;
    private boolean portrait;
    private Camera mCamera;
    private Parameters mCameraParams;
    private SurfaceHolder mHolder;
    protected List<Camera.Size> mPreviewSizeList;
    protected List<Camera.Size> mPictureSizeList;
    protected Camera.Size mPreviewSize;
    protected Camera.Size mPictureSize;
    private int[] pixels;

    @SuppressWarnings("deprecation")
    public CameraPreview(Context context) {
        super(context);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            mCamera = Camera.open(0);
        } else {
            mCamera = Camera.open();
        }
        mCameraParams = mCamera.getParameters();
        mPreviewSizeList = mCameraParams.getSupportedPreviewSizes();
        mPictureSizeList = mCameraParams.getSupportedPictureSizes();

        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            Log.d(LOG_TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    @SuppressLint({ "InlinedApi", "NewApi" })
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        // make sure preview surface exists
        if (mHolder.getSurface() == null) {
            Log.e(LOG_TAG, "No preview surface");
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error stopping preview: " + e.getMessage());
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // determine if portrait or not and set camera orientation
        if (orientation == 1) {
            portrait = true;
            mCamera.setDisplayOrientation(90);
        } else {
            portrait = false;
            mCamera.setDisplayOrientation(0);
        }

        mPreviewSize = getOptimalPreviewSize(mPictureSizeList, width, height);
        //mPictureSize = determinePictureSize(mPreviewSize);

        //Log.v(LOG_TAG, mPictureSize.width + "x" + mPictureSize.height);

        //pixels = new int[mPreviewSize.width * mPreviewSize.height];

        mCameraParams.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        //mCameraParams.setPictureSize(mPictureSize.width, mPictureSize.height);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mCameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else {
            mCameraParams.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        if (mCameraParams.getMaxNumMeteringAreas() > 0){ // check that metering areas are supported
            List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();

            Rect areaRect1 = new Rect(-100, -100, 100, 100);    // specify an area in center of image
            meteringAreas.add(new Camera.Area(areaRect1, 600)); // set weight to 60%
            Rect areaRect2 = new Rect(800, -1000, 1000, -800);  // specify an area in upper right of image
            meteringAreas.add(new Camera.Area(areaRect2, 400)); // set weight to 40%
            mCameraParams.setMeteringAreas(meteringAreas);
        }

        mCameraParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(mCameraParams);

        // start preview with new settings
        try {
            //mCamera.setPreviewCallback(this);
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(LOG_TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(LOG_TAG, "Surface has been destroyed");
        closeCamera();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();
            float touchMajor = event.getTouchMajor();
            float touchMinor = event.getTouchMinor();

            Rect touchRect = new Rect( (int) (x - touchMajor / 2),
                                       (int) (y - touchMinor / 2),
                                       (int) (x + touchMajor / 2),
                                       (int) (y + touchMinor / 2) );

            Log.v(LOG_TAG, touchRect.flattenToString());
            //((AndroidCamera) getContext()).touchFocus(touchRect);
        }

        return true;
    }

    void closeCamera() {
        Log.v(LOG_TAG, "Closing camera");
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    protected Camera.Size determinePictureSize(Camera.Size previewSize) {
        Camera.Size retSize = null;
        for (Camera.Size size : mPictureSizeList) {
            if (size.equals(previewSize)) {
                return size;
            }
        }

        Log.v(LOG_TAG, "Same picture size not found.");

        // if the preview size is not supported as a picture size
        float reqRatio = ((float) previewSize.width) / previewSize.height;
        float curRatio, deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE;
        for (Camera.Size size : mPictureSizeList) {
            curRatio = ((float) size.width) / size.height;
            deltaRatio = Math.abs(reqRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
        }

        return retSize;
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int width, int height) {
        // bail if not sizes given
        if (sizes == null) {
            Log.e(LOG_TAG, "No sizes given");
            return null;
        }

        final double ASPECT_TOLERANCE = 0.05;
        double minDiff = Double.MAX_VALUE;
        double targetRatio;
        int targetHeight;
        Size optimalSize = null;
        int w;
        int h;

        // when in portrait, width/height need to be swapped
        if (portrait) {
            w = height;
            h = width;
        } else {
            w = width;
            h = height;
        }

        Log.v(LOG_TAG, "Desired Preview Size - " + w + " x " +  h);

        targetHeight = h;
        targetRatio = (double) w/h;

        Log.v(LOG_TAG, "Listing all supported preview sizes");
        // Find size
        for (Size size : sizes) {
            Log.v(LOG_TAG, "  " + size.width + " x " + size.height);
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        Log.v(LOG_TAG, "Optimal Preview Size - " + optimalSize.width + " x " + optimalSize.height);
        return optimalSize;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //decodeYUV420SP(pixels, data, previewSize.width, previewSize.height);
    }

    private void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0)
                    r = 0;
                else if (r > 262143)
                   r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                   g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                   b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }
}