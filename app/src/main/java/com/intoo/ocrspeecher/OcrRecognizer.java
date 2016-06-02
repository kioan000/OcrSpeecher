package com.intoo.ocrspeecher;


import android.graphics.Bitmap;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.ThreadFactory;


/**
 * Created by kioan on 04/04/15.
 */
public class OcrRecognizer implements ThreadFactory
{
    private TessBaseAPI baseApiInstance;
    private String DATA_PATH;
    private String CURRENT_LANGUAGE;
    private ArrayList<Thread> ThreadList;

    private Thread Launcher;
    private Thread OCRProvider;
    private Thread ImageProcesser;
    private Thread ImageWarper;
    private Thread Binarizer;
    private Thread Translator;
    private String OCRText;
    private String FROM;
    private String TO;
    //TODO add the APIKEY from Google
    private static String APIKEY = "AIzaSyAdpXTVRDzjUDjwD9__AWmeP-n_z95-_mM";

    /**
     * OcrRecognizer class constructor
     * @param dataPath the Path of data files used by Tesseract
     * @param language the language to read
     */
    public OcrRecognizer(String dataPath, String language) {
        CURRENT_LANGUAGE=language;
        DATA_PATH = dataPath;
        FROM= "it";
        TO="eng";
        ThreadList = new ArrayList<>();
    }

    //Synchronized getters
    private synchronized Thread getLauncher(){return Launcher;}
    public synchronized TessBaseAPI getTessBase() {return baseApiInstance;}
    private synchronized Thread getOCRProvider() {return OCRProvider;}
    private synchronized Thread getBinarizer() {return Binarizer;}
    private synchronized Thread getImageProcesser() {return ImageProcesser;}
    private synchronized Thread getImageWarper(){return ImageWarper;}
    public synchronized Thread getImageBinarizer() {return Binarizer;}
    public synchronized Thread getTranslator() {return Translator;}


    /**
     * Build a new Thread and handles exception if are still running
     * @param r The runnable with Thread Logic
     * @return R encapsulated in a Thread
     * @throws RuntimeException
     */
    @Override
    public Thread newThread(Runnable r) throws RuntimeException
    {
        //Throws exception if OCR is in progress
        if (r.getClass()==OCRProvider.class){
            if ((getOCRProvider()!=null)&&(getOCRProvider().isAlive())) {
                throw new RuntimeException("OCRProvider is alive");}
            OCRProvider = new Thread(r);
            OCRProvider.setPriority(Thread.MAX_PRIORITY);
            return OCRProvider;
        }
        if (r.getClass()==ImageProcesser.class)
        {
            if ((getImageProcesser()!=null)&&(getImageProcesser().isAlive())) {
                throw new RuntimeException(("Preprocessor is alive"));
            }
            ImageProcesser = new Thread(r);
            ImageProcesser.setPriority(Thread.MAX_PRIORITY);
            return ImageProcesser;
        }
        if (r.getClass() == ImageWarper.class)
        {
            if ((getImageWarper()!=null)&&(getImageWarper().isAlive())) {
                throw new RuntimeException(("Warper is alive"));
            }
            ImageWarper = new Thread(r);
            ImageWarper.setPriority(Thread.MAX_PRIORITY);
            return ImageWarper;
        }
        if (r.getClass() == ImageBinarizer.class)
        {
            if ((getImageBinarizer()!=null)&&(getImageBinarizer().isAlive()))
            {
                throw new RuntimeException(("Binarizer is alive"));
            }
            Binarizer = new Thread(r);
            Binarizer.setPriority(Thread.MAX_PRIORITY);
            return Binarizer;
        }
        if (r.getClass() == Launcher.class)
        {
            if ((getLauncher()!=null)&&(getLauncher().isAlive()))
            {
                throw new RuntimeException((("Launcher is alive")));
            }
            Launcher = new Thread(r);
            Launcher.setPriority(Thread.MAX_PRIORITY);
            return Launcher;
        }
        if (r.getClass()==Translator.class)
        {
            if ((getTranslator()!=null)&&(getTranslator().isAlive())) {
                throw new RuntimeException(("Preprocessor is alive"));
            }
            Translator = new Thread(r);
            Translator.setPriority(Thread.MAX_PRIORITY);
            return Translator;
        }
        else{ //Nothing should come here
            Log.d("Unpredicted thread:",r.toString());
            Thread t = new Thread(r);
            return t;
        }
    }

    /**
     * Change the Tesseract instance reference
     * @param tess the TessBaseAPI instance to use since now
     */
    private synchronized void setTessBase(TessBaseAPI tess) {baseApiInstance = tess;}

    /**
     * Change the language data for Tesseract OCR
     * @param language the language string
     * @return always true. Used by radio button
     */
    public boolean changeLanguage(String language) {
        CURRENT_LANGUAGE = language;
        return true;
    }

    /**
     * Edit language configuration for the translator
     * @param from the language to translate from
     * @param to the language to translate to
     * @return always true (used to set radio button properly)
     */
    public boolean changeTranslatorLanguage(String from, String to) {
        FROM = from;
        TO = to;
        return true;
    }

    /**
     * Method that handles Tesseract initialization
     * @param l the listener containing the Progress notification
     * @return true if the launcher Thread have been started correctly
     */
    public boolean initTesseract(OcrRecognizerListener l){
        if ((getOCRProvider()!= null)&&(getOCRProvider().isAlive()))
            try {
                getOCRProvider().join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return false;}
        return launchTesseract(l);}

    /**
     * Method that handles Tesseract inizialitazion
     * @param l the listener containing the Progress notification
     * @return true if the launcher Thread have been started correctly
     */
    private boolean launchTesseract(OcrRecognizerListener l) {
        Launcher r = new Launcher(l);
        try{
            newThread(r).start();
            return true;}
        catch (RuntimeException e){
            Log.d("launchTesseract",e.getMessage());
            return false;
        }
    }

    /**
     * Method that handles the OCR of Tesseract
     * @param image the image to analyze
     * @param listener the listener at the end of OCRProvider Thread
     * @return true if the OCR Thread have been started correctly
     */
    public boolean launchOCR(Bitmap image, OcrRecognizerListener listener) {
        OCRProvider r = new OCRProvider(listener, image);
        try {
            newThread(r).start();
            return true;
        }
        catch (RuntimeException e)
        {
            Log.e("launchOCR", e.getMessage());
            return false;
        }
    }

    /**
     * Method that handles the image perspective correction
     * @param corners the point array of the perspective box used to warp the image
     * @param image the bitmap image to warp
     * @param listener the listener at the end of warper thread
     * @return true if the Warper Thread have been started correctly
     */
    public boolean warpImage(Point[] corners, Bitmap image, OcrRecognizerListener listener) {
        ImageWarper iw = new ImageWarper(corners, listener, image);
        try {
            newThread(iw).start();
            return true;
        }
        catch (RuntimeException e)
        {
            Log.e("launchPreProcess",e.getMessage());
            return false;
        }
    }

    /**
     * Method that handles the image edges analysis to find a square label in it
     * @param image the image to analyze
     * @param listener the listener at the end of pre-processer thread
     * @return true if the pre-processer Thread have been started correctly
     */
    public boolean preProcessImage(Bitmap image, OcrRecognizerListener listener) {
        ImageProcesser ip = new ImageProcesser(listener,image);
        try {
            newThread(ip).start();
            return true;
        }
        catch (RuntimeException e)
        {
            Log.e("launchPreProcess",e.getMessage());
            return false;
        }
    }

    /**
     * Method that handles the image color binarization.
     * @param bitmap the bitmap to binarize
     * @param listener the listener at the end of image binarization
     * @return true if the binarizer Thread have been started correctly
     */
    public boolean binarize(Bitmap bitmap, OcrRecognizerListener listener) {
        ImageBinarizer ib = new ImageBinarizer(listener, bitmap);
        try {
            newThread(ib).start();
            return true;
        }
        catch (RuntimeException e)
        {
            Log.e("launchPreProcess",e.getMessage());
            return false;
        }
    }

    /**
     * Method that handles the comunication with Google Translation API Servers
     * @param text the text to translate
     * @param l the listener at the end of translation
     * @return true if the translator Thread have been started correctly
     */
    public boolean translate (String text, OcrRecognizerListener l) {
        Translator t = new Translator(text,FROM,TO,l);
        try{
            newThread(t).start();
            return true;
        }
        catch (RuntimeException e) {
            Log.e("launchPreProcess", e.getMessage());
            return false;
        }
    }

    //NOT USED
    //Intersection function to find all the insersecting points of lines in the image. It's part of
    //automatic warping box finding feature
    private Point computeIntersect(double[] a, double[] b) {
        int x1 = (int) a[0], y1 = (int) a[1], x2 = (int) a[2], y2 = (int) a[3];
        int x3 = (int) b[0], y3 = (int) b[1], x4 = (int) b[2], y4 = (int) b[3];

        float d = ((float) (x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4));
        if (d >= 0) {
            Point pt = new Point();
            pt.x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
            pt.y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
            return pt;
        } else
            return new Point(-1, -1);
    }

    /**
     * Safetely ends all running things and clears up tesseract datas
     */
    public void dispose() {
        try {
            if ((getOCRProvider() != null) && (getOCRProvider().isAlive()))
                getOCRProvider().join();
            if ((getLauncher() != null) && (getLauncher().isAlive()))
                getLauncher().join();
            if ((getTranslator() != null) && (getTranslator().isAlive()))
                getTranslator().join();
            if ((getBinarizer() != null) && (getBinarizer().isAlive()))
                getBinarizer().join();
            if ((getImageWarper() != null) && (getImageWarper().isAlive()))
                getImageWarper().join();
            if ((getImageProcesser() != null) && (getImageProcesser().isAlive()))
                getImageProcesser().join();
            if (baseApiInstance!= null)
                baseApiInstance.clear();
        }
        catch (Exception e) {
            Log.e("Failed at dispose:", e.getMessage());
        }
    }

    /**
     * Returns the language that this ocr istance is set to read
     * @return ita or eng strings
     */
    public String getLanguage() {
        return CURRENT_LANGUAGE;
    }


    /**
     * Various logic threads used by the class
     */
    private class Launcher implements Runnable{
        private OcrRecognizerListener l;
        public Launcher(OcrRecognizerListener listener)
        {
            l=listener;
        }
        @Override
        public void run() {
            //Check if there is an OCR instance running
            if ((getOCRProvider()!=null)&&(getOCRProvider().isAlive()))
            {
                try {
                    getOCRProvider().join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (getTessBase()!=null)
                    getTessBase().end();
            }
            setTessBase(new TessBaseAPI(l));
            getTessBase().init(DATA_PATH, CURRENT_LANGUAGE);
            l = null;
        }
    }

    private class ImageWarper implements Runnable {
        Point[] corners;
        OcrRecognizerListener listener;
        Bitmap OCRImage;
        int boundH, boundW;
        public ImageWarper(Point[] corners, OcrRecognizerListener listener, Bitmap OCRImage)
        {
            this.corners= corners;
            this.listener = listener;
            this.OCRImage = OCRImage;
            //Tryng to approximate a squared dimension
            int maxW = Integer.MIN_VALUE,maxH = Integer.MIN_VALUE;
            int minW = Integer.MAX_VALUE,minH = Integer.MAX_VALUE;
            for (int i = 0; i<corners.length;i++)
            {
                maxH = (int) Math.max(maxH,corners[i].y);
                minH = (int) Math.min(minH,corners[i].y);
                maxW = (int) Math.max(maxW,corners[i].x);
                minW = (int) Math.min(minW,corners[i].x);
            }
            boundH = maxH - minH;
            boundW = maxW - minW;
        }

        @Override
        public void run()
        {
            //TODO warp image with opencv
            // Define the destination image
            Mat quad ;

            quad = Mat.zeros(boundH,boundW, CvType.CV_32FC2);
            Point[]  quad_pts = new Point[4];
            // Corners of the destination image (clockwise mode)
            /*quad_pts[0]= new Point(0,0);
            quad_pts[1] = new Point(quad.cols(),0);
            quad_pts[2] = new Point(quad.cols(),quad.rows());
            quad_pts[3] = new Point(0,quad.rows());*/
            //reverse clockwise
            quad_pts[0]= new Point(0,0);
            quad_pts[1] = new Point(0,quad.rows());
            quad_pts[2] = new Point(quad.cols(),quad.rows());
            quad_pts[3] = new Point(quad.cols(),0);

            // Get transformation matrix
            Mat transmtx = Imgproc.getPerspectiveTransform(new MatOfPoint2f(corners),new MatOfPoint2f(quad_pts));

            // Apply perspective transformation
            Mat src = new Mat();
            Utils.bitmapToMat(OCRImage, src);

            Imgproc.warpPerspective(src, quad, transmtx, quad.size());
            OCRImage.recycle();
            OCRImage = null;
            int w = quad.width(), h = quad.height();
            Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
            OCRImage = Bitmap.createBitmap(w, h, conf); // this creates a MUTABLE bitmap
            Utils.matToBitmap(quad, OCRImage);
            listener.endWarpListener(OCRImage);
            listener = null;
            return;
        }
    }

    private class ImageBinarizer implements Runnable {
        private OcrRecognizerListener l;
        private Bitmap OCRImage;
        public ImageBinarizer(OcrRecognizerListener listener, Bitmap image)
        {
            this.l = listener;
            this.OCRImage = image;
        }


        @Override
        public void run()
        {
            Mat greyscalemat = new Mat();
            Mat binarymat = new Mat();
            Mat originalmat = new Mat();
            Utils.bitmapToMat(OCRImage, originalmat);
            //Produce a greyscale mat
            Imgproc.cvtColor(originalmat, greyscalemat, Imgproc.COLOR_RGB2GRAY);

            //Binarize the greyscale mat with OTSU algoritm to choose the best threshold value
            Imgproc.threshold(greyscalemat, binarymat, 0, 255, Imgproc.THRESH_OTSU);

            Utils.matToBitmap(binarymat, OCRImage);
            //Utils.matToBitmap(binarymat, OCRImage);
            l.endPreprocessingListener(OCRImage, null);
            l = null;
            return;
        }
    }

    private class ImageProcesser implements Runnable {
        private OcrRecognizerListener l;
        private Bitmap OCRImage;
        public ImageProcesser(OcrRecognizerListener listener, Bitmap image)
        {
            this.l = listener;
            this.OCRImage = image;
        }


        @Override
        public void run()
        {
            Mat greyscalemat = new Mat();
            Mat binarymat = new Mat();
            Mat originalmat = new Mat();
            Utils.bitmapToMat(OCRImage,originalmat);
            //Produce a greyscale mat
            Imgproc.cvtColor(originalmat, greyscalemat, Imgproc.COLOR_RGB2GRAY);

            //Gaussian blur the greyscaled image
            Imgproc.GaussianBlur(greyscalemat,greyscalemat,new Size(5,5),0);
            //Binarize the greyscale mat with OTSU algoritm to choose the best threshold value
            Imgproc.threshold(greyscalemat,binarymat,0,255,Imgproc.THRESH_OTSU);
            //Find Image Contours
            Vector<MatOfPoint> contours = new Vector<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(binarymat,contours,hierarchy,Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

            Imgproc.drawContours(originalmat,contours,-1,new Scalar(255,255,255),10);
            Utils.matToBitmap(originalmat, OCRImage);
            //Find the biggest square in the image
            MatOfPoint2f biggest = null;
            double max_area = 0;
            for (int i= 0; i< contours.size();i++)
            {
                MatOfPoint2f pointmat = new MatOfPoint2f(contours.get(i).toArray());
                MatOfPoint2f approx = new MatOfPoint2f();
                double area = Imgproc.contourArea(contours.get(i));
                if (area > 100)
                {
                    double peri = Imgproc.arcLength(pointmat,true);
                    Imgproc.approxPolyDP(pointmat,approx,0.02*peri,true);
                    if ((area > max_area)&&(approx.cols()==4))
                    {
                        biggest = approx;
                        max_area = area;
                    }
                }
            }
            if (biggest!=null)
            {
                Vector<android.graphics.Point> points = new Vector<>();
                for(int i = 0; i<biggest.cols(); i++){
                    double[] vec = biggest.get(0, i);
                    double x1 = vec[0];
                    double y1 = vec[1];
                    Point pt = new Point(x1,y1);
                    android.graphics.Point apt = new android.graphics.Point((int)x1,(int)y1);
                    points.add(apt);
                    Core.circle(binarymat,pt,70,new Scalar(123,45,233));
                    Utils.matToBitmap(binarymat, OCRImage);
                    l.endPreprocessingListener(OCRImage, (android.graphics.Point[]) points.toArray());
                    l = null;
                    return;
                }
            }
            Log.d("ImageProcesser","No quad found!");
            //Utils.matToBitmap(binarymat, OCRImage);
            l.endPreprocessingListener(OCRImage,null);
            l = null;
            return;
        }
    }

    private class OCRProvider implements Runnable {
        private OcrRecognizerListener l;
        private Bitmap OCRImage;
        public OCRProvider(OcrRecognizerListener listener, Bitmap image)
        {
            l = listener;
            OCRImage = image;
        }
        @Override
        public void run()
        {
            try{
                if (getLauncher()!=null && getLauncher().isAlive())
                    getLauncher().join();
                getTessBase().setImage(OCRImage);
                OCRText = getTessBase().getUTF8Text();
                OCRImage.recycle();
                OCRImage = null;
                l.endOCRListener(OCRText);
                notifyAll();
            }
            catch (Exception e) //Catch interrupted exception by Tesseract re-initialization
            {
                Log.e("OCRProvider",e.getMessage());
            }
            l = null;
            return;

        }
    }

    private class Translator implements Runnable {

        String text,from,to;
        OcrRecognizerListener listener;
        public Translator(String text, String from, String to, OcrRecognizerListener listener)
        {
            this.listener = listener;
            this.text = text;
            this.from = from;
            this.to = to;
        }
        @Override
        public void run()
        {
            GoogleTranslator gt = new GoogleTranslator(APIKEY);
            listener.endTranslatorListener(gt.translate(text,from,to));
            listener=null;
        }
    }

}


