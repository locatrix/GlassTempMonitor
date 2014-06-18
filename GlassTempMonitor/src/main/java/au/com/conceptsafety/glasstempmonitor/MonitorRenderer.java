package au.com.conceptsafety.glasstempmonitor;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;

import com.google.android.glass.timeline.DirectRenderingCallback;

/**
 * Created by matt on 18/06/2014.
 */
public class MonitorRenderer implements DirectRenderingCallback, MonitorView.Listener {
    private static final String TAG = "MonitorRenderer";
    private MonitorView monitorView;
    private SurfaceHolder surfaceHolder;
    private boolean rendering;

    public MonitorRenderer(Context context) {
        monitorView = new MonitorView(context);
    }

    @Override
    public void renderingPaused(SurfaceHolder surfaceHolder, boolean paused) {
        rendering = paused;
        updateRenderingState();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        rendering = true;
        this.surfaceHolder = surfaceHolder;

        updateRenderingState();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        int measuredWidth = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int measuredHeight = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);

        monitorView.measure(measuredWidth, measuredHeight);
        monitorView.layout(0, 0, monitorView.getMeasuredWidth(), monitorView.getMeasuredHeight());
        monitorView.layout(0, 0, monitorView.getMeasuredWidth(), monitorView.getMeasuredHeight());
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = null;
        updateRenderingState();
    }

    private void updateRenderingState() {
        if (surfaceHolder != null && rendering) {
            monitorView.setListener(this);
        } else {
            monitorView.setListener(null);
        }
    }

    public void tempUpdated() {
        if (surfaceHolder != null) {
            draw(monitorView);
        }
    }

    private void draw(View view) {
        Canvas canvas;
        try {
            canvas = surfaceHolder.lockCanvas();
        } catch (Exception e) {
            Log.e(TAG, "Unable to lock canvas: " + e);
            return;
        }

        if (canvas != null) {
            view.draw(canvas);
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }
}
