package me.kptmusztarda.lights;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import me.kptmusztarda.handylib.Logger;
import me.kptmusztarda.handylib.Utilities;

public class BroadcastReceiverService extends Service {


    private final String TAG = "BroadcastReceiverService";

    private static final String ACTION_CLICK = "me.kptmusztarda.lights.CLICK";
    private static final String ACTION_DOUBLE_CLICK = "me.kptmusztarda.lights.DOUBLE_CLICK";
    private static final String ACTION_HOLD = "me.kptmusztarda.lights.HOLD";
    private static final String ACTION_ACTION_DOUBLE_CLICK_HOLD = "me.kptmusztarda.lights.DOUBLE_CLICK_HOLD";
    private static boolean isRunning;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.log(TAG, "Broadcast received! " + intent.getAction());
            switch (intent.getAction()) {
                case ACTION_CLICK:

                    break;
                case ACTION_DOUBLE_CLICK:
                    action();
                    break;

                case ACTION_HOLD:

                    break;
                case ACTION_ACTION_DOUBLE_CLICK_HOLD:

                    break;
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Logger.log(TAG, "onStartCommand");
        isRunning = true;

        registerReceiver(broadcastReceiver, new IntentFilter(ACTION_CLICK));
        registerReceiver(broadcastReceiver, new IntentFilter(ACTION_DOUBLE_CLICK));
        registerReceiver(broadcastReceiver, new IntentFilter(ACTION_HOLD));
        registerReceiver(broadcastReceiver, new IntentFilter(ACTION_ACTION_DOUBLE_CLICK_HOLD));

        startForeground();

        return super.onStartCommand(intent, flags, startId);
    }

    void action() {
        Logger.log(TAG, "Action!");
        RequestQueue.add(new StringRequest(Request.Method.GET,
                "http://192.168.0.131:2138/webapp/switch?id=all&state=toggle",
                response -> {
                    Bulbs.setStatus(response);
                    Utilities.runOnUiThread(() -> Toast.makeText(App.get().getApplicationContext(), "Lights are " + (Bulbs.isAtLeastOneOn() ? "on" : "off"), Toast.LENGTH_SHORT).show());
                },
                error -> Logger.log(TAG, "Nie dziorgo: " + error)
        ));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.log(TAG, "onDestroy");
        unregisterReceiver(broadcastReceiver);
        isRunning = false;
    }

    public static boolean isRunning() {
        return isRunning;
    }

    private void startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String NOTIFICATION_CHANNEL_ID = getPackageName();
            String channelName = "My Background Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle("App is running in background")
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();
            startForeground(2, notification);
        } else {
            startForeground(1, new Notification());
        }
    }

}
