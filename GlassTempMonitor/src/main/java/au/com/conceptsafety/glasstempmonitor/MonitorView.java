package au.com.conceptsafety.glasstempmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * Created by matt on 18/06/2014.
 */
public class MonitorView extends FrameLayout {
    private static final String TAG = "MonitorView";
    private static final long VISIBLE_UPDATE_DELAY_MILLIS = 200;
    private static final long ACTIVE_UPDATE_DELAY_MILLIS = 500;
    private static final long IDLE_UPDATE_DELAY_MILLIS = 2000;
    private TextView cpuTemp;
    private Listener listener;
    private Handler handler;
    private PowerManager powerManager;

    public static interface Listener {
        void tempUpdated();
    }

    public MonitorView(Context context) {
        super(context);

        powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);

        LayoutInflater.from(context).inflate(R.layout.card_monitor, this);
        cpuTemp = (TextView)findViewById(R.id.cpuTemp);

        handler = new Handler(context.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                String formatted = String.format("%.1fC", getCPUTemperature());
                cpuTemp.setText(formatted);
                Log.d(TAG, formatted);

                if (listener != null) {
                    listener.tempUpdated();
                }

                handler.postDelayed(this, getUpdateDelay());
            }
        }, getUpdateDelay());
    }

    public void setListener(final Listener listener) {
        Log.d(TAG, "setting listener " + listener);
        this.listener = listener;
    }

    public void stopMonitoring() {
        Log.d(TAG, "stopping monitoring");
        handler.removeCallbacksAndMessages(null);
    }

    private double getCPUTemperature() {
        try {
            File file = new File("/sys/devices/platform/notle_pcb_sensor.0/temperature");
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            double milliDegrees = Integer.parseInt(br.readLine());

            return milliDegrees / 1000.0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long getUpdateDelay() {
        if (listener != null && powerManager.isScreenOn()) {
            Log.d(TAG, "visible updating");
            return VISIBLE_UPDATE_DELAY_MILLIS;
        } else if (powerManager.isScreenOn()) {
            Log.d(TAG, "active updating");
            return ACTIVE_UPDATE_DELAY_MILLIS;
        } else {
            Log.d(TAG, "idle updating");
            return IDLE_UPDATE_DELAY_MILLIS;
        }
    }
}
