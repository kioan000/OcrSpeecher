package com.intoo.ocrspeecher;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;

import java.io.File;
import java.io.FilenameFilter;


public class LiveCamActivity extends ActionBarActivity implements Camera.ShutterCallback, Camera.PictureCallback {

    private MySurfaceView camera_preview;
    private Thread assets_check ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*Launch the asset loader thread to extract tessdata resources if neeeded*/
        assets_check = new  Thread(new Runnable() {
                @Override
                public void run() {
                    String[] file = getFilesDir().list(new FilenameFilter() {
                        public boolean accept(File directory, String fileName) {
                            return fileName.equals("tessdata");
                        }
                    });
                    if (file.length == 0)
                        Utility.doCopyAssets(getFilesDir(), getAssets(), "");

                }
            });

        assets_check.start();

        //Init del layout della activity live cam
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_cam);
        camera_preview = (MySurfaceView)findViewById(R.id.camera_preview);
        camera_preview.init(getScreenRotationDegree());
    }

    @Override
    protected void onPause() {
        super.onPause();
        camera_preview.dispose();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Heavy memory free-up needed
        camera_preview.init(getScreenRotationDegree());
        if (Utility.preview != null) {
            Utility.preview.recycle();
            Utility.preview = null;
        }
        if (Utility.warpedBitmap != null){
            Utility.warpedBitmap.recycle();
            Utility.warpedBitmap = null;
        }
        if (Utility.ocr != null)
            Utility.ocr.dispose();
        System.gc();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_live_cam, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        switch (id){
            case R.id.action_shoot: {
                camera_preview.getCamera().takePicture(this,null,null,this);
                item.setEnabled(false);
                return true;}
            case R.id.camera_mode_autofocus: {
                if (camera_preview.isMacroMode()) {
                    camera_preview.setMacroMode(false);}
                item.setChecked(true);
                return true;
            }
            case R.id.camera_mode_macro:{
                if (!camera_preview.isMacroMode()) {
                    camera_preview.setMacroMode(true);}
                item.setChecked(true);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Calcola la rotazione dello schermo come somma tra natural rotation del display e rotazione effettiva
     * @return la rotazione in gradi per correggere la fotocamera
     */
    private int getScreenRotationDegree()
    {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break; // Natural orientation
            case Surface.ROTATION_90:
                degrees = 90;
                break; // Landscape left
            case Surface.ROTATION_180:
                degrees = 180;
                break;// Upside down
            case Surface.ROTATION_270:
                degrees = 270;
                break;// Landscape right
        }
        return degrees;
    }

    @Override
    public void onShutter()
    {

    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        camera.stopPreview();
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inPurgeable = true;
        bmOptions.inMutable = true;
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length,bmOptions);
        bitmap.setDensity(300);
        Utility.saveOriginalToInternalStorage(bitmap, this);
        bitmap = null;
        Intent intent = new Intent(this, EditImageActivity.class);
        startActivity(intent);
    }
}
