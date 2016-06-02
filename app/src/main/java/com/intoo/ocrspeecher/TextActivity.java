package com.intoo.ocrspeecher;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;


public class TextActivity extends ActionBarActivity implements OcrRecognizerListener{

    private OcrRecognizer ocr;
    private TextView ocrView;
    private TextView translationView;
    private ProgressBar progressBar;
    private Menu menu;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);
        ocr = Utility.ocr;
        ocrView = (TextView)findViewById(R.id.textView);
        translationView = (TextView)findViewById(R.id.translationTextView);
        progressBar= (ProgressBar)findViewById(R.id.progressBar);
        ocr.initTesseract(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        ocr.launchOCR(Utility.warpedBitmap, this);
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (Utility.warpedBitmap!= null)
        {
            Utility.warpedBitmap.recycle();
            Utility.warpedBitmap = null;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (Utility.warpedBitmap== null)
        {
            Utility.warpedBitmap = Utility.loadWarpedImageFromStorage();
        }
        if (menu!=null){
        if (ocr.getLanguage() == getResources().getString(R.string.lang_italian))
            (menu.findItem(R.id.language_ita_to_eng)).setChecked(true);
        else
            menu.findItem(R.id.language_eng_to_ita).setChecked(true);}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_text, menu);
        this.menu = menu;
        if (ocr.getLanguage() == getResources().getString(R.string.lang_italian))
            (menu.findItem(R.id.language_ita_to_eng)).setChecked(true);
        else
            menu.findItem(R.id.language_eng_to_ita).setChecked(true);
        return true;
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
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id){
            //Language selection for OCR
            case R.id.language_ita_to_eng:{
                item.setChecked(ocr.changeTranslatorLanguage("it", "en"));
                return true ;
            }
            case R.id.language_eng_to_ita: {
                item.setChecked(ocr.changeTranslatorLanguage("en", "it"));
                return true;
            }
            case R.id.action_translate: {
                ocr.translate(ocrView.getText().toString(),this);
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void endOCRListener(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ocrView.setText(text);
                progressBar.setProgress(progressBar.getMax());
            }
        });
    }

    @Override
    public void onProgressValues(final TessBaseAPI.ProgressValues progressValues) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((ProgressBar)findViewById(R.id.progressBar)).setProgress(progressValues.getPercent());
            }
        });

    }

    @Override
    public void endTranslatorListener(final String translated) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                translationView.setText(translated);
            }
        });

    }

    //Not Used
    @Override
    public void endPreprocessingListener(Bitmap preprocessed, Point[] square) {

    }

    //Not used
    @Override
    public void endWarpListener(Bitmap warped) {

    }
}
