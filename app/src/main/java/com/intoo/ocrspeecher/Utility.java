package com.intoo.ocrspeecher;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.ImageView;

import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

/**
 * Created by kioan on 11/04/15.
 * Utility class:
 * Functions to handle tessdata files and related stuffs at first boot
 * Geometric functions to find lines intersections
 * Bitmap references and other objects to pass between activities
 * I/O functions directly from Apache Commons code
 */
public class Utility {
    private static AssetManager assets;
    private static String dataName;
    //Used by EditImage to avoid reloading and reprocessing at each resizing...
    public static boolean editImageActivityFirstRun;

    public static OcrRecognizer ocr;
    public static Bitmap preview;
    public static Bitmap warpedBitmap;
    private static String ORIGINAL_PHOTO = "photo.jpg";
    private static String WARPED_PHOTO = "warped.jpg";

    /**
     * @param externalFilesDir Context.getExternalFileDir()
     * @param asset            Context.getAssetManager()
     * @param copyData         data name to copy, "" for all
     * @throws IOException
     */
    public static void doCopyAssets(File externalFilesDir, AssetManager asset, String copyData) {
        assets = asset;
        dataName = copyData;
        externalFilesDir.getPath();
        try {
            doCopy(dataName, externalFilesDir.getPath());
        } catch (IOException e) {
            Log.e("CopyAssets", e.getMessage());
        }
        assets = null;
        dataName = null;

    }

    private static void doCopy(String dirName, String outPath) throws IOException {

        String[] srcFiles = assets.list(dirName);//for directory
        for (String srcFileName : srcFiles) {
            String outFileName = outPath + File.separator + srcFileName;
            String inFileName = dirName + File.separator + srcFileName;
            if (dirName.equals(dataName)) {// for first time
                inFileName = srcFileName;
            }
            try {
                InputStream inputStream = assets.open(inFileName);
                copyAndClose(inputStream, new FileOutputStream(outFileName));
            } catch (IOException e) {//if directory fails exception
                new File(outFileName).mkdir();
                doCopy(inFileName, outFileName);
            }

        }
    }


    /**
     * Apache CommonsIO code
     *
     * @param output
     */
    public static void closeQuietly(OutputStream output) {
        try {
            if (output != null) {
                output.close();
            }
        } catch (IOException ioe) {
            //skip
        }
    }

    public static void closeQuietly(InputStream input) {
        try {
            if (input != null) {
                input.close();
            }
        } catch (IOException ioe) {
            //skip
        }
    }

    public static void copyAndClose(InputStream input, OutputStream output) throws IOException {
        copy(input, output);
        closeQuietly(input);
        closeQuietly(output);
    }

    public static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int n = 0;
        while ((n = input.read(buffer)) != -1) {
            output.write(buffer, 0, n);
        }
    }

    private static String path_original;
    private static String path_warped;
    private static OriginalSaver original_saver;
    private static WarpedSaver warped_saver;

    public static int originalWidth;
    public static int originalHeight;

    public static void saveOriginalToInternalStorage(Bitmap bitmapImage, final Context cw)
    {
        if ((original_saver!=null)&&(original_saver.isAlive()  )){
            try {
                original_saver.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }}
        original_saver = new OriginalSaver(bitmapImage,cw);
        original_saver.setPriority(Thread.MAX_PRIORITY);
        original_saver.start();
    }

    private static class OriginalSaver extends Thread
    {
        Bitmap bitmapImage;
        Context cw;
        public OriginalSaver(Bitmap bitmapImage, Context cw){
            this.bitmapImage = bitmapImage;
            this.cw = cw;
        }

        @Override
        public void run() {
            //Save the original image resolution for ImageView
            originalWidth = bitmapImage.getWidth();
            originalHeight = bitmapImage.getHeight();
            // path to /data/data/yourapp/app_data/imageDir
            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
            path_original = directory.getPath();
            // Create imageDir
            File mypath1=new File(directory,ORIGINAL_PHOTO);

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mypath1);
                // Use the compress method on the BitMap object to write image to the OutputStream
                bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            bitmapImage.recycle();
            bitmapImage = null;
            System.gc();
            return;
        }
    }

    public static void saveWarpedToInternalStorage(Bitmap bitmapImage, Context cw)
    {
        if ((warped_saver!=null)&&(warped_saver.isAlive()  )){
            try {
                warped_saver.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }}
        warped_saver = new WarpedSaver(bitmapImage,cw);
        warped_saver.setPriority(Thread.MAX_PRIORITY);
        warped_saver.start();
    }

    private static class WarpedSaver extends Thread
    {
        Bitmap bitmapImage;
        Context cw;
        public WarpedSaver(Bitmap bitmapImage, Context cw){
            this.bitmapImage = bitmapImage;
            this.cw = cw;
        }

        @Override
        public void run() {
            // path to /data/data/yourapp/app_data/imageDir
            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
            path_warped = directory.getPath();
            // Create imageDir
            File mypath1=new File(directory,WARPED_PHOTO);

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mypath1);
                // Use the compress method on the BitMap object to write image to the OutputStream
                bitmapImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            bitmapImage = null;
            return;
        }
    }

    public static Bitmap loadWarpedImageFromStorage()
    {
        if ((warped_saver!=null)&&(warped_saver.isAlive())){
            try {
                warped_saver.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            }}

            File f=new File(path_warped, WARPED_PHOTO);
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inPurgeable = true;
            bmOptions.inMutable = true;
            return BitmapFactory.decodeFile(f.getPath(),bmOptions);
    }

    public static Bitmap loadOriginalImageFromStorage()
    {
        if ((original_saver!=null)&&(original_saver.isAlive())){
            try {
                original_saver.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            }}

            File f=new File(path_original, ORIGINAL_PHOTO);
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inPurgeable = true;
            bmOptions.inMutable = true;
            return BitmapFactory.decodeFile(f.getPath(),bmOptions);

    }

    /**
     * Resizer bitmap loader
     * @param h height pixels
     * @param w width pixels
     * @return the loaded bitmap
     */
    public static Bitmap loadSmallImageFromStorage(Bitmap b, int h, int w)
    {
        if ((original_saver!=null)&&(original_saver.isAlive())){
            try {
                original_saver.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            }}

        // Get the dimensions of the bitmap
        File f=new File(path_original, ORIGINAL_PHOTO);
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/w, photoH/h);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = 2; //scaleFactor
        bmOptions.inPurgeable = true;
        bmOptions.inMutable = true;

        b = BitmapFactory.decodeFile(f.getPath(), bmOptions);
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(f.getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
        b = rotateBitmap(b, orientation);
        preview = b;
        return b;
    }

    /**
     * Rotate a bitmap with the correct rotation correction
     * @param bitmap the bitmap to rotate
     * @param orientation the exif interface rotation
     * @return
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        }
        catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

}
