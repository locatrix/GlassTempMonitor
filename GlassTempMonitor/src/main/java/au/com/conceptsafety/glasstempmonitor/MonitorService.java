package au.com.conceptsafety.glasstempmonitor;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.google.android.glass.timeline.LiveCard;

/**
 * Created by matt on 18/06/2014.
 */
public class MonitorService extends Service {
    private static final String LIVE_CARD_TAG = "temp_monitor";
    private LiveCard liveCard;
    private MonitorRenderer renderer;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (liveCard == null) {
            liveCard = new LiveCard(this, LIVE_CARD_TAG);

            // set up the main renderer
            renderer = new MonitorRenderer(this);
            liveCard.setDirectRenderingEnabled(true).getSurfaceHolder().addCallback(renderer);

            // and also the intent for showing our menu
            Intent menuIntent = new Intent(this, MenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            liveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));

            // and finally attach and reveal it to the user
            liveCard.attach(this);
            liveCard.publish(LiveCard.PublishMode.REVEAL);
        } else {
            liveCard.navigate();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (liveCard != null && liveCard.isPublished()) {
            liveCard.unpublish();
            liveCard = null;
        }

        super.onDestroy();
    }
}
