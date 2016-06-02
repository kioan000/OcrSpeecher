package com.intoo.ocrspeecher;

import android.graphics.Bitmap;
import android.graphics.Point;

import com.googlecode.tesseract.android.TessBaseAPI;


/**
 * Created by kioan on 21/04/15.
 * OCRRecognizer listeners interface
 */
public interface OcrRecognizerListener extends TessBaseAPI.ProgressNotifier
{
    /**
     * At the end of OCR process
     * @param text the read text
     */
    void endOCRListener(String text);

    /**
     * At the end of first Bitmap analysis
     * @param preprocessed a bitmap with edge analysis performed
     * @param square a warp square-candidate
     */
    void endPreprocessingListener(Bitmap preprocessed, Point[] square);

    /**
     * TessBaseAPI listener implementation
     * @param progressValues
     */
    void onProgressValues(TessBaseAPI.ProgressValues progressValues);

    /**
     * At the end of perspective correction
     * @param warped warped image
     */
    void endWarpListener(Bitmap warped);

    /**
     * At the end of translation process
     * @param translated the translated content
     */
    void endTranslatorListener(String translated);
}
