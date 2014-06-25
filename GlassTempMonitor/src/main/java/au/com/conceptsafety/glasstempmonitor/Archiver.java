package au.com.conceptsafety.glasstempmonitor;

import android.util.Log;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by matt on 25/06/2014.
 *
 * Handles archiving of data out to a file at regular intervals.
 */
public class Archiver {
    private static final String TAG = "Archiver";
    private List<String> lines;
    private String filePath;
    private Timer timer;
    private TimerTask saveTask;

    public Archiver(String filePath) {
        lines = new LinkedList<>();
        timer = new Timer();
        this.filePath = filePath;
        saveTask = null;

        setArchiveInterval(10000);
    }

    public void setArchiveInterval(long milliseconds) {
        if (saveTask != null) {
            saveTask.cancel();
        }

        saveTask = new TimerTask() {
            @Override
            public void run() {
                saveLines();
            }
        };

        timer.schedule(saveTask, milliseconds, milliseconds);
    }

    public void close() {
        // make sure we don't try and save anymore
        timer.cancel();
        timer = null;

        // make sure we're all saved
        saveLines();

        // and clear everything
        lines = null;
    }

    private synchronized void saveLines() {
        Log.d(TAG, "archiving lines...");

        try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(filePath, true)))) {
            for (String line : lines) {
                out.println(line);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        lines.clear();
    }

    public void postLine(String line) {
        if (lines == null) {
            throw new RuntimeException("archiver is closed");
        }

        lines.add(line);
    }
}
