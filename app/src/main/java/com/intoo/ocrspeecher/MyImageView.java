package com.intoo.ocrspeecher;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * Created by kioan on 28/04/15.
 * ImageView with warp-box drawing capabilities
 */
public class MyImageView extends ImageView implements View.OnTouchListener, View.OnDragListener {

    private Rect bounding_rect=null;
    private Point[] points = new Point[4];
    private boolean isSelection = false;
    //point1 and point 3 are of same group and same as point 2 and point4
    int groupId = -1;
    // array that holds the balls
    private ArrayList<ColorBall> colorballs = new ArrayList<ColorBall>();
    // variable to know what ball is being dragged
    private int balID = 0;
    Paint paint;


    public MyImageView(Context context) {
        super(context);
        setWillNotDraw(false);
        setOnTouchListener(this);

        paint = new Paint();
        setFocusable(true); // necessary for getting the touch events

    }

    public MyImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setOnTouchListener(this);

        paint = new Paint();
        setFocusable(true); // necessary for getting the touch events

    }

    public MyImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setWillNotDraw(false);
        setOnTouchListener(this);

        paint = new Paint();
        setFocusable(true); // necessary for getting the touch events
    }

    /**
     * Draw default warp square in case of failure for automatic detection
     * @param forced redraw default square even if the square have been already initialized before
     */
    public void putDefaultSquare(boolean forced)
    {
        if ((forced )|| (points[0] == null)) {
            int Y = getDrawable().getMinimumHeight()/4;
            int X = getDrawable().getMinimumWidth()/4;
            //initialize rectangle.
            points[0] = new Point();
            points[0].x = X-60;
            points[0].y = Y-60;

            points[1] = new Point();
            points[1].x = X-60;
            points[1].y = Y + 60;

            points[2] = new Point();
            points[2].x = X + 60;
            points[2].y = Y + 60;

            points[3] = new Point();
            points[3].x = X + 60;
            points[3].y = Y - 60;

            balID = 2;
            groupId = 1;
            isSelection = true;
            int count = 0;
            for (Point pt : points) {
                colorballs.add(new ColorBall(getContext(), R.drawable.circle, pt, count));
                count++;}
            invalidate();
        }
    }


    @Override
    public void setImageAlpha(int alpha) {
        super.setImageAlpha(alpha);
    }



    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        int eventaction = event.getAction();

        int X = (int) event.getX();
        int Y = (int) event.getY();

        switch (eventaction) {
            // touch down so check if the finger is on a ball
            case MotionEvent.ACTION_DOWN:
                Log.d("OnTouch","Action down");
                if (points[0] == null) {
                    //initialize rectangle.
                    points[0] = new Point();
                    points[0].x = X;
                    points[0].y = Y;

                    points[1] = new Point();
                    points[1].x = X;
                    points[1].y = Y + 60;

                    points[2] = new Point();
                    points[2].x = X + 60;
                    points[2].y = Y + 60;

                    points[3] = new Point();
                    points[3].x = X +60;
                    points[3].y = Y;

                    balID = 2;
                    groupId = 1;
                    isSelection = true;
                    // declare each point with the ColorBall class
                    int count = 0;
                    for (Point pt : points) {
                        colorballs.add(new ColorBall(getContext(), R.drawable.circle, pt, count));
                        count++;
                    }
                } else {
                    //resize rectangle
                    balID = -1;
                    groupId = -1;
                    for (int i = colorballs.size()-1; i>=0; i--) {
                        ColorBall ball = colorballs.get(i);
                        // check if inside the bounds of the ball (circle)
                        // get the center for the ball
                        int centerX = ball.getX() + ball.getWidthOfBall();
                        int centerY = ball.getY() + ball.getHeightOfBall();
                        paint.setColor(Color.CYAN);
                        // calculate the radius from the touch to the center of the ball
                        double radCircle = Math
                                .sqrt((double) (((centerX - X) * (centerX - X)) + (centerY - Y)
                                        * (centerY - Y)));

                        if (radCircle < (1.5*ball.getWidthOfBall())) {

                            balID = ball.getID();
                            if (balID == 1 || balID == 3) {
                                groupId = 2;
                            } else {
                                groupId = 1;
                            }
                            invalidate();
                            break;
                        }
                        invalidate();
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE: // touch drag with the ball

                Log.d("OnTouch","Action move");
                if (balID > -1) {
                    // move the balls the same as the finger
                    colorballs.get(balID).setX(X);
                    colorballs.get(balID).setY(Y);
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                Log.d("OnTouch","Action up");
                // touch drop - check if the balls are still in the view
                Rect bounds = new Rect();
                getDrawingRect(bounds);
                if (balID > -1){
                    boolean invalid = false;
                    if (X < bounds.left){X= bounds.left; invalid =true;}
                    if (X > bounds.right){X = bounds.right; invalid = true;}
                    if (Y < bounds.top){Y = bounds.top; invalid = true;}
                    if (Y > bounds.bottom){Y= bounds.bottom-30; invalid = true;}
                    if (invalid) {
                        colorballs.get(balID).setX(X);
                        colorballs.get(balID).setY(Y);
                        invalidate();}}
                break;}
        // redraw the canvas
        invalidate();
        return true;
    }

    public boolean isSelection()
    {return isSelection;}


    private int previewBitmapWidth = 0;
    private int previewBitmapHeight = 0;
    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        if (bm!=null){
            previewBitmapHeight = bm.getHeight();
            previewBitmapWidth = bm.getWidth();
        }
    }

    public org.opencv.core.Point[] getCorners()
    {
        if (isSelection)
        {
            // Get rectangle of the bitmap (drawable) drawn in the imageView.
            RectF bitmapRect = new RectF();
            bitmapRect.right = getDrawable().getIntrinsicWidth();
            bitmapRect.bottom = getDrawable().getIntrinsicHeight();

            // Translate and scale the bitmapRect according to the imageview's scale-type
            Matrix m = getImageMatrix();
            m.mapRect(bitmapRect);

            // Get the width of the image as shown on the screen:
            float drawableBitmapWidth = bitmapRect.width();
            float drawableBitmapHeight = bitmapRect.height();
            ///////////
            //Get the margin
            int leftMargin = (int) ((getWidth() - drawableBitmapWidth)/2);

            //Re-Calculate the bounding box coordinates for the original bitmap
            org.opencv.core.Point[] bounds = new org.opencv.core.Point[points.length];
            for (int p=0; p< colorballs.size() ; p++)
            {
                int pX = colorballs.get(p).getX() + (colorballs.get(p).getWidthOfBall()/2);
                int pY = colorballs.get(p).getY() + (colorballs.get(p).getHeightOfBall()/2);
                double x = (Math.min(Math.max( (pX - leftMargin), 0), drawableBitmapWidth) * Utility.originalWidth)/drawableBitmapWidth;
                double y = (Math.min(Math.max( pY, 0), drawableBitmapHeight) * Utility.originalHeight)/drawableBitmapHeight;
                bounds[p] = new org.opencv.core.Point(x, y);
            }
            return bounds;
        }
        else
            return null;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        if(points[3]==null) //point4 null when user did not touch and move on screen.
            return;
        int left, top, right, bottom;
        left = points[0].x;
        top = points[0].y;
        right = points[0].x;
        bottom = points[0].y;
        for (int i = 1; i < points.length; i++) {
            left = left > points[i].x ? points[i].x:left;
            top = top > points[i].y ? points[i].y:top;
            right = right < points[i].x ? points[i].x:right;
            bottom = bottom < points[i].y ? points[i].y:bottom;
        }
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStrokeJoin(Paint.Join.ROUND);
        /*paint.setStrokeWidth(5);

        //draw stroke
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#AADB1255"));
        paint.setStrokeWidth(2);



        /*canvas.drawRect(
                left + colorballs.get(0).getWidthOfBall() / 2,
                top + colorballs.get(0).getWidthOfBall() / 2,
                right + colorballs.get(2).getWidthOfBall() / 2,
                bottom + colorballs.get(2).getWidthOfBall() / 2, paint);*/
        //fill the rectangle

        Path path = new Path();
        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        paint.setColor(Color.parseColor("#55DB1255"));
        paint.setStrokeWidth(0);
        //Draw the bounding box
        path.moveTo(points[0].x + colorballs.get(0).getWidthOfBall()/2, points[0].y + colorballs.get(0).getHeightOfBall()/2); // used for first point
        path.lineTo(points[1].x + colorballs.get(1).getWidthOfBall()/2, points[1].y + colorballs.get(1).getHeightOfBall()/2);
        path.lineTo(points[2].x + colorballs.get(2).getWidthOfBall()/2, points[2].y + colorballs.get(2).getHeightOfBall()/2);
        path.lineTo(points[3].x + colorballs.get(3).getWidthOfBall()/2, points[3].y + colorballs.get(3).getHeightOfBall()/2);
        path.lineTo(points[0].x + colorballs.get(0).getWidthOfBall()/2, points[0].y + colorballs.get(0).getHeightOfBall()/2);
        canvas.drawPath(path, paint);

        // draw the balls on the canvas
        paint.setColor(Color.BLUE);
        paint.setTextSize(18);
        paint.setStrokeWidth(0);
        for (int i =0; i < colorballs.size(); i ++) {
            ColorBall ball = colorballs.get(i);
            canvas.drawBitmap(ball.getBitmap(), ball.getX(), ball.getY(),
                    paint);

            canvas.drawText("" + (i+1), ball.getX(), ball.getY(), paint);
        }
    }

    @Override
    public boolean onDrag(View v, DragEvent event)
    {
        return false;
    }


    /**
     * Inner class ColorBall
     */
    private class ColorBall
    {
        Bitmap bitmap;
        Context mContext;
        Point point;
        int id;


        public ColorBall(Context context, int resourceId, Point point, int count) {
            id = count;
            bitmap = BitmapFactory.decodeResource(context.getResources(),
                    resourceId);
            mContext = context;
            this.point = point;
        }

        public int getWidthOfBall() {
            return bitmap.getWidth();
        }

        public int getHeightOfBall() {
            return bitmap.getHeight();
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public int getX() {
            return point.x;
        }

        public int getY() {
            return point.y;
        }

        public int getID() {
            return id;
        }

        public void setX(int x) {
            point.x = x;
        }

        public void setY(int y) {
            point.y = y;
        }
    }

}
