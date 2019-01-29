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
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import me.kptmusztarda.handylib.Logger;

public class BroadcastReceiverService extends Service {

    private static final String ACTION = "me.kptmusztarda.lights.CLICK";
    private final String TAG = "BroadcastReceiverService";
    private static boolean isRunning;
    private Network net;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.log(TAG, "Broadcast received!");
            action();
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

        net = Network.getInstance();
        net.setContext(this);

        registerReceiver(broadcastReceiver, new IntentFilter(ACTION));

        startForeground();

        return super.onStartCommand(intent, flags, startId);
    }

    void action() {
        Logger.log(TAG, "Action!");
        net.send(Bulbs.STRING_SWITCH_HALF, 0, true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.log(TAG, "onDestroy");
        unregisterReceiver(broadcastReceiver);
//        net.closeSocket();
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
