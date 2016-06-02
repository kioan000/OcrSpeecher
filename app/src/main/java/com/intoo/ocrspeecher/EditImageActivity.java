package com.intoo.ocrspeecher;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.OpenCVLoader;

/**
 * Activity that shows the photo and allows the drawing of the warp box
 */
public class EditImageActivity extends ActionBarActivity implements OcrRecognizerListener
{
    /* OpenCV static loading*/
    static{
        if(!OpenCVLoader.initDebug())
            Log.e("OpenCVLoader", "Failed loading OpenCV");
        else
            Log.d("OpenCVLoader","OpenCV statically loaded");
    }

    //The logic object
    private OcrRecognizer ocr;
    private MyImageView myImageView;
    private ProgressBar progressBar;
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_image);
        myImageView = (MyImageView) findViewById(R.id.myImageView);
        Utility.loadSmallImageFromStorage(Utility.preview, myImageView.getMaxHeight(), myImageView.getMaxWidth());
        ocr = new OcrRecognizer(getFilesDir().getPath(), getResources().getString(R.string.lang_italian));
        ocr.preProcessImage(Utility.preview, this);
        Utility.ocr = ocr;
        myImageView.setImageBitmap(Utility.preview);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit_image, menu);
        this.menu = menu;
        if (ocr.getLanguage() == getResources().getString(R.string.lang_italian))
            (menu.findItem(R.id.language_ita)).setChecked(true);
        else
            menu.findItem(R.id.language_eng).setChecked(true);
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
            //Language selection for OCR
            case R.id.language_ita:{
                item.setChecked(ocr.changeLanguage(getResources().getString(R.string.lang_italian)));
                return true ;
            }
            case R.id.language_eng: {
                item.setChecked(ocr.changeLanguage(getResources().getString(R.string.lang_english)));
                return true;
            }
            case R.id.action_warp: {
                //TODO launch ocr scan
                org.opencv.core.Point[] corners = myImageView.getCorners();
                if (corners != null)
                    ocr.warpImage(corners, Utility.loadOriginalImageFromStorage(),this);
                item.setEnabled(false);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        myImageView.setImageBitmap(null);
        Utility.preview.recycle();
        Utility.preview = null;
        System.gc();
    }

    @Override
    protected void onResume() {

        super.onResume();
        if (Utility.warpedBitmap != null)
        {
            Utility.warpedBitmap.recycle();
            Utility.warpedBitmap = null;
            System.gc();
        }
        Utility.loadSmallImageFromStorage(Utility.preview, myImageView.getMaxHeight(), myImageView.getMaxWidth());
        ocr.preProcessImage(Utility.preview, this);

        if (menu!=null) {
            if (ocr.getLanguage() == getResources().getString(R.string.lang_italian))
                (menu.findItem(R.id.language_ita)).setChecked(true);
            else
                menu.findItem(R.id.language_eng).setChecked(true);
        }
    }



    //Not used here
    @Override
    public void endOCRListener(final String text) {

    }

    @Override
    public void endPreprocessingListener(final Bitmap preprocessed, final Point[] square)
    {
        Utility.preview = preprocessed;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Log.d("Result ",square.toString());
                myImageView.setImageBitmap(preprocessed);
                myImageView.putDefaultSquare(false);
        }});
    }

    //Not used here
    @Override
    public void onProgressValues(final TessBaseAPI.ProgressValues progressValues) {

    }

    @Override
    public void endWarpListener(Bitmap warped) {
        Utility.warpedBitmap = warped;
        runOnUiThread(new Runnable() {
            @Override
            public void run(){
                Intent intent = new Intent(getApplicationContext(), FinalStepsActivity.class);
                startActivity(intent);
                (menu.findItem(R.id.action_warp)).setEnabled(true);
            }});
        Utility.saveWarpedToInternalStorage(warped, getBaseContext());
    }

    //Not used here
    @Override
    public void endTranslatorListener(String translated) {

    }
    //Not used here
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Utility.ocr.dispose();
    }

}
