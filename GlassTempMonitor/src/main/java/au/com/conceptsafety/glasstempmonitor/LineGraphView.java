package au.com.conceptsafety.glasstempmonitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by matt on 18/06/2014.
 */
public class LineGraphView extends View {
    private static final String TAG = "LineGraphView";

    public static class Reading {
        public double x, y;

        public Reading(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    // maps values from (minIn, maxIn) to (minOut, maxOut)
    private class Transformer {
        private double minIn, minOut, maxIn, maxOut;

        public Transformer(double minIn, double maxIn, double minOut, double maxOut) {
            this.minIn = minIn;
            this.maxIn = maxIn;
            this.minOut = minOut;
            this.maxOut = maxOut;
        }

        public double transform(double value) {
            // compute where we are in the given in bounds
            double amtIn = value - minIn;
            double fracIn = amtIn/(maxIn - minIn);

            // and lerp that to the out bounds
            return fracIn * (maxOut - minOut) + minOut;
        }
    }

    private double minX = 0, maxX = 1, minY = 0, maxY = 1;
    private Iterable<Iterable<Reading>> sources = null;

    public LineGraphView(Context context) {
        super(context);
    }

    public LineGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LineGraphView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setSources(Iterable<Iterable<Reading>> sources) {
        this.sources = sources;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // build transformers
        Transformer xt = new Transformer(minX, maxX, 0, getWidth());
        Transformer yt = new Transformer(minY, maxY, getHeight(), 0); // flipped Y axis

        // and render each source
        for (Iterable<Reading> source : sources) {
            boolean first = true;
            float x1 = 0, y1 = 0;
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(5.0f);
            paint.setStyle(Paint.Style.STROKE);

            for (Reading reading : source) {
                if (first) {
                    first = false;
                    x1 = (float)xt.transform(reading.x);
                    y1 = (float)yt.transform(reading.y);
                    continue;
                }

                float x2 = (float)xt.transform(reading.x);
                float y2 = (float)yt.transform(reading.y);

                canvas.drawLine(x1, y1, x2, y2, paint);

                x1 = x2;
                y1 = y2;
            }
        }
    }

    // automatic Y bounds (in the future)
    public void setBounds(double minX, double maxX) {
        double newMinY = 0;
        double newMaxY = 60; // TODO: actually calculate dynamic bounds

        setBounds(minX, maxX, newMinY, newMaxY);
    }

    public void setBounds(double minX, double maxX, double minY, double maxY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;

        invalidate();
    }
}
