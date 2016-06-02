package com.intoo.ocrspeecher;


import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.lang.reflect.Method;
import java.util.List;




/**
 * Created by kioan on 28/03/15.
 */
public class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback
{
    private static Camera camera;
    private Camera.CameraInfo cameraInfo;
    private SurfaceHolder surfaceHolder;
    private Camera.Size cameraPreviewSize;
    private Camera.Size cameraPictureSize;
    private int degrees;
    private Camera.Parameters cameraParameters;

    private boolean macroMode;


    public void setMacroMode(boolean value) {
        macroMode = value;
            if ((macroMode) && cameraParameters.getSupportedFocusModes().contains("macro"))
                cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
            else /*if (cameraParameters.getSupportedFocusModes().contains("auto"))
                cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            else*/ if (cameraParameters.getSupportedFocusModes().contains("continuos-picture"))
                cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            else if (cameraParameters.getSupportedFocusModes().contains("fixed")) {
                cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
            }
            camera.setParameters(cameraParameters);
            if (!macroMode)
                camera.autoFocus(null);

    }

    public boolean isMacroMode() {

        return macroMode;
    }

    public MySurfaceView(Context c) {

        super(c);
    }

    public MySurfaceView(Context c, AttributeSet as) {
        super(c,as);
    }

    public MySurfaceView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context,attrs,defStyleAttr);
    }
    public void init(int degrees)
    {
        this.macroMode= true; //Default value
        this.degrees = degrees;
        surfaceHolder = this.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        setFocusable(true);
        setFocusableInTouchMode(true);
        try {
            camera = getFirstBackCamera();
            camera.enableShutterSound(true);
        } catch (Exception e) {
            Log.e("Camera instantiation",e.getMessage());
        }

    }

    /**
     * Dispose all the resources handled by this View
     */
    public void dispose()
    {
        if (camera!=null)
            camera.stopPreview();
            camera.release();
    }

    /**
     * Get the first back camera available
     * @return the Camera object
     * @throws Exception if the devices doesn't have a back camera
     */
    private Camera getFirstBackCamera() throws Exception
    {
        Camera.CameraInfo mCandidateInfo = new Camera.CameraInfo();
        for (int i=0; i<Camera.getNumberOfCameras(); i++)
        {
            Camera.getCameraInfo(i, mCandidateInfo);
            if (mCandidateInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
            {
                cameraInfo = mCandidateInfo;
                return Camera.open(i);
            }
        }
        throw new Exception("No back camera found");
    }

    //Catch eventi sulla surface view (da callback)
    //Alla creazione della surface view
    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        try {
            surfaceHolder = this.getHolder();
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

            //Enable Canvas drawing to apply rows and lines
            setWillNotDraw(false);
            //Instantiate camera

        } catch (Exception e) {
            Log.e("surfaceCreated",e.getMessage());
        }

    }

    //Al cambiamento della fotocamera
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        surfaceHolder = holder;

        try {
            //Camera initialization
            cameraParameters = camera.getParameters();

            List<Camera.Size> supportedPreviewSizes = cameraParameters.getSupportedPreviewSizes();
            //Get the bigger camera size
            cameraPreviewSize = supportedPreviewSizes.get(0);
            List<Camera.Size> supportedPictureSizes = cameraParameters.getSupportedPictureSizes();
            //Get the bigger picture size
            cameraPictureSize = supportedPictureSizes.get(supportedPictureSizes.size()-1);

            cameraParameters.setPreviewSize(cameraPreviewSize.width, cameraPreviewSize.height);
            cameraParameters.setPictureSize(cameraPictureSize.width, cameraPictureSize.height);

            for(int i=0;i< cameraParameters.getSupportedFocusModes().size();i++){
            Log.v("", cameraParameters.getSupportedFocusModes().get(i));}

            //Setting macro-focus (if device supported)

            if ((macroMode) && cameraParameters.getSupportedFocusModes().contains("macro"))
                cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
            else /*if (cameraParameters.getSupportedFocusModes().contains("auto"))
                    cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            else */if (cameraParameters.getSupportedFocusModes().contains("continuos-picture"))
                    cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            else if (cameraParameters.getSupportedFocusModes().contains("fixed"))
                    cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);


            //Try to fix camera orientation problems
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            //WARNING: DON'T TOUCH THIS FUCK*** PIECE OF CODE!
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

            int cameraRotationOffset = cameraInfo.orientation;
            // ...
            List<Camera.Size> previewSizes = cameraParameters.getSupportedPreviewSizes();
            Camera.Size previewSize = null;
            float closestRatio = Float.MAX_VALUE;

            int targetPreviewWidth;
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) targetPreviewWidth = getWidth();
            else targetPreviewWidth = getHeight();
            int targetPreviewHeight;
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) targetPreviewHeight = getHeight();
            else targetPreviewHeight = getWidth();
            float targetRatio = targetPreviewWidth / (float) targetPreviewHeight;
            Log.v("", "target size: " + targetPreviewWidth + " / " + targetPreviewHeight + " ratio:" + targetRatio);
            for (Camera.Size candidateSize : previewSizes) {
                float whRatio = candidateSize.width / (float) candidateSize.height;
                if (previewSize == null || Math.abs(targetRatio - whRatio) < Math.abs(targetRatio - closestRatio)) {
                    closestRatio = whRatio;
                    previewSize = candidateSize;
                }
            }

            int displayRotation;
            displayRotation = (cameraRotationOffset - degrees + 360) % 360;

            Log.v("", "rotation cam / phone = displayRotation: " + cameraRotationOffset + " / " + degrees + " = "
                    + displayRotation);

            //cameraParameters.setRotation(displayRotation);
            camera.setDisplayOrientation(displayRotation);

            int rotate = (360 + cameraRotationOffset - degrees) % 360;

            Log.v("", "screenshot rotation: " + cameraRotationOffset + " / " + degrees + " = " + rotate);
            Log.v("", "preview size: " + previewSize.width + " / " + previewSize.height);
            cameraParameters.setPreviewSize(previewSize.width, previewSize.height);
            //cameraParameters.setRotation(rotate);
            //--------------------------------------------------------------------------------------
            //~~~~~~~~~~~~~~~~~~~~~~~~ END OF SH*T ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
            //--------------------------------------------------------------------------------------


            //Set squared camera layout
            //Get container layout and fits the surface in it
            ViewGroup container = (ViewGroup)this.getParent();
            ViewGroup.LayoutParams containerParameters = container.getLayoutParams();


            //int maxSize = Math.min(container.getWidth(),container.getHeight());

            //float w_h_camera_ratio = cameraPreviewSize.width/cameraPreviewSize.height;
            //containerParameters.width = (int) (w_h_camera_ratio*containerParameters.height);

            //container.setLayoutParams(containerParameters);

            //Adapting the container layout to avoid camera stretching
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.getLayoutParams();
            if (cameraPreviewSize.width > cameraPreviewSize.height) //16:9 format
            {
                layoutParams.height = containerParameters.height;
                layoutParams.width = (int)(containerParameters.height * ((float)(cameraPreviewSize.width)/(float)(cameraPreviewSize.height)));
                layoutParams.setMargins((containerParameters.width-layoutParams.width)/2, 0,0,0);
            }
            else //Vertical format
            {
                layoutParams.width = containerParameters.width;
                layoutParams.height = (int)(containerParameters.width * ((float)(cameraPreviewSize.height)/(float)(cameraPreviewSize.width)));
                layoutParams.setMargins(0,(containerParameters.height-layoutParams.height)/2,0,0);

            }

            if ((displayRotation == 90)||(displayRotation == 270))
            {
                int temp = layoutParams.height;
                layoutParams.height = layoutParams.width;
                layoutParams.width = temp;
                temp = layoutParams.leftMargin;
                layoutParams.setMargins(layoutParams.topMargin,temp,0,0);
            }

            this.setLayoutParams(layoutParams);

            camera.setParameters(cameraParameters);
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            if (!macroMode)
                camera.autoFocus(null);
            Log.d("", "preview started");


        }
        catch (Exception e)
        {
            Log.e("startpreview stopped",e.getMessage());
        }
    }

    public Camera getCamera()
    {
        return this.camera;
    }

    //NOT USED ANYMORE
    // My displayOrientation to fix camera
    protected void setDisplayOrientation(Camera camera, int angle){
        Method downPolymorphic;
        try
        {
            downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[] { int.class });
            if (downPolymorphic != null)
                downPolymorphic.invoke(camera, new Object[] { angle });
        }
        catch (Exception e1)
        {
            Log.e("mysurfaceview",e1.getMessage());
        }
    }

    //Alla distruzione del contenitore
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //Libero le risorse occupate (fotocamera)
        camera.release();
    }


    Paint paint = new Paint();

    /**
     * Paint guidelines on canvas
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas)
    {
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();
        int pixelStep = 50;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(128,255,255,255));
        for (int i=canvasHeight; i>=0;)
        {
            canvas.drawLine(0,i,canvasWidth,i,paint);
            i = i - pixelStep;
            Log.d(this.getClass().getName(), "On Draw Called: i:"+i+" width:"+canvasWidth+" height:"+canvasHeight);
        }
    }


}