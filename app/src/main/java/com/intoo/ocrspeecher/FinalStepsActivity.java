package com.intoo.ocrspeecher;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.googlecode.tesseract.android.TessBaseAPI;


public class FinalStepsActivity extends ActionBarActivity implements OcrRecognizerListener
{

    private OcrRecognizer ocr;
    private ImageView imageView;
    private ProgressBar progressBar;
    private MenuItem action;
    private Menu menu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final_steps);
        ocr = Utility.ocr;
        imageView = (ImageView) findViewById(R.id.imageView);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        imageView.setImageBitmap(Utility.warpedBitmap);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    @Override
    protected void onPause() {
        super.onPause();
        imageView.setImageBitmap(null);
        System.gc();
    }

    @Override
    protected void onResume() {

        super.onResume();
        if (Utility.warpedBitmap == null)
            Utility.warpedBitmap = Utility.loadWarpedImageFromStorage();
        ocr.binarize(Utility.warpedBitmap, this);
        if (action!=null)
            action.setEnabled(false);
        if (menu!=null){
            if (ocr.getLanguage() == getResources().getString(R.string.lang_italian))
                    (menu.findItem(R.id.language_ita)).setChecked(true);
                else
                    menu.findItem(R.id.language_eng).setChecked(true);
            }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_final_steps, menu);
        action = menu.findItem(R.id.action_ocr);
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
                item.setChecked(ocr.changeLanguage("ita"));
                return true ;
            }
            case R.id.language_eng: {
                item.setChecked(ocr.changeLanguage("eng"));
                return true;
            }
            case R.id.action_ocr: {
                //TODO launch ocr scan
                Intent intent = new Intent(getApplicationContext(), TextActivity.class);
                startActivity(intent);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
//NOT USED
    @Override
    public void endOCRListener(String text) {
        ///Do nothing here
    }

    @Override
    public void endPreprocessingListener(final Bitmap preprocessed, Point[] square)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run(){
                imageView.setImageBitmap(preprocessed);
                Utility.warpedBitmap = preprocessed;
                progressBar.setIndeterminate(false);
                if (action!=null)
                    action.setEnabled(true);
            }});
    }
//NOT USED
    @Override
    public void onProgressValues(TessBaseAPI.ProgressValues progressValues) {

    }
//NOT USED
    @Override
    public void endWarpListener(Bitmap warped) {

    }
//NOT USED
    @Override
    public void endTranslatorListener(String translated) {

    }
}
