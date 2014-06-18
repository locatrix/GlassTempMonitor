package au.com.conceptsafety.glasstempmonitor;

import android.content.Context;
import android.os.Handler;
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
    private static long UPDATE_DELAY_MILLIS = 500;
    private TextView cpuTemp;
    private Listener listener;
    private Handler handler;

    public static interface Listener {
        void tempUpdated();
    }

    public MonitorView(Context context) {
        super(context);

        LayoutInflater.from(context).inflate(R.layout.card_monitor, this);
        cpuTemp = (TextView)findViewById(R.id.cpuTemp);

        handler = new Handler(context.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                cpuTemp.setText(String.format("%.1fC", getCPUTemperature()));
                if (listener != null) {
                    listener.tempUpdated();
                }

                handler.postDelayed(this, UPDATE_DELAY_MILLIS);
            }
        }, UPDATE_DELAY_MILLIS);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
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
}
