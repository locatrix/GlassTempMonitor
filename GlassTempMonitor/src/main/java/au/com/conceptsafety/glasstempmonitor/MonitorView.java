package au.com.conceptsafety.glasstempmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by matt on 18/06/2014.
 */
public class MonitorView extends FrameLayout {
    private static final String TAG = "MonitorView";
    private static final long VISIBLE_UPDATE_DELAY_MILLIS = 1000;
    private static final long ACTIVE_UPDATE_DELAY_MILLIS = 2000;
    private static final long IDLE_UPDATE_DELAY_MILLIS = 7500;
    private static final String CPU_TEMP_PATH = "/sys/devices/platform/notle_pcb_sensor.0/temperature";
    private static final String BATTERY_TEMP_PATH = "/sys/devices/platform/omap_i2c.1/i2c-1/1-0055/power_supply/bq27520-0/temp";
    private TextView cpuText;
    private TextView batteryText;
    private LineGraphView graph;
    private Listener listener;
    private Handler handler;
    private PowerManager powerManager;
    private LinkedList<LineGraphView.Reading> cpuReadings = new LinkedList<>();
    private LinkedList<LineGraphView.Reading> batteryReadings = new LinkedList<>();
    private double lowestReading = 20; // reasonable enough defaults
    private double highestReading = 45;
    private Archiver archiver;

    public static interface Listener {
        void tempUpdated();
    }

    public MonitorView(Context context) {
        super(context);

        powerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);

        LayoutInflater.from(context).inflate(R.layout.card_monitor, this);
        cpuText = (TextView)findViewById(R.id.cpuTemp);
        batteryText = (TextView)findViewById(R.id.batteryTemp);
        graph = (LineGraphView)findViewById(R.id.lineGraph);

        List<Iterable<LineGraphView.Reading>> sources = new ArrayList<>();
        sources.add(cpuReadings);
        sources.add(batteryReadings);

        graph.setSources(sources);

        // set up archiving of this session
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File readingsDir = new File(dir, "GlassTempMonitor");
        File archive = new File(readingsDir, dateFormat.format(new Date()) + "_temps.csv");
        archive.getParentFile().mkdirs();
        try {
            archive.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        archiver = new Archiver(archive.getPath());
        archiver.postLine("unixtime,cpu,battery");

        handler = new Handler(context.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                takeReading();
                handler.postDelayed(this, getUpdateDelay());
            }
        }, getUpdateDelay());
    }

    private void trimReadings(LinkedList<LineGraphView.Reading> readings, double min) {
        while (readings.getFirst().x < min) {
            readings.removeFirst();
        }
    }

    public void takeReading() {
        double cpuTemp = getCPUTemperature();
        double batteryTemp = getBatteryTemperature();
        double time = ((double)System.currentTimeMillis()) / 1000.0;
        lowestReading = Math.min(Math.min(lowestReading, cpuTemp), batteryTemp);
        highestReading = Math.max(Math.max(highestReading, cpuTemp), batteryTemp);

        String readingLine = String.format("%.2f,%.2f,%.2f", time, cpuTemp, batteryTemp);
        Log.d(TAG, "reading: " + readingLine);
        archiver.postLine(readingLine);

        cpuReadings.add(new LineGraphView.Reading(time, cpuTemp));
        batteryReadings.add(new LineGraphView.Reading(time, batteryTemp));
        trimReadings(cpuReadings, time - 180);
        trimReadings(batteryReadings, time - 180);

        graph.setBounds(time - 180, time, lowestReading, highestReading);
        graph.invalidate();

        cpuText.setText(formatDegrees(cpuTemp));
        batteryText.setText(formatDegrees(batteryTemp));

        if (listener != null) {
            listener.tempUpdated();
        }
    }

    public void setListener(final Listener listener) {
        Log.d(TAG, "setting listener " + listener);
        this.listener = listener;
    }

    public void stopMonitoring() {
        Log.d(TAG, "stopping monitoring");
        handler.removeCallbacksAndMessages(null);

        // ensure the full temp archive is written
        archiver.close();
    }

    private String readFirstLine(String path) throws IOException {
        BufferedReader br = null;

        try {
            File file = new File(path);
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            return br.readLine();
        } catch (IOException e) {
            throw e;
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }

    private String formatDegrees(double degrees) {
        return String.format("%.0f", degrees);
    }

    private double getCPUTemperature() {
        try {
            String reading = readFirstLine(CPU_TEMP_PATH);
            double milliDegrees = Integer.parseInt(reading);

            return milliDegrees / 1000.0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private double getBatteryTemperature() {
        try {
            String reading = readFirstLine(BATTERY_TEMP_PATH);
            double deciDegrees = Integer.parseInt(reading);

            return deciDegrees / 10.0;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long getUpdateDelay() {
        if (listener != null && powerManager.isScreenOn()) {
            return VISIBLE_UPDATE_DELAY_MILLIS;
        } else if (powerManager.isScreenOn()) {
            return ACTIVE_UPDATE_DELAY_MILLIS;
        } else {
            return IDLE_UPDATE_DELAY_MILLIS;
        }
    }
}
