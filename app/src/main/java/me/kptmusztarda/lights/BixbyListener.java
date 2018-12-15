package me.kptmusztarda.lights;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import me.kptmusztarda.handylib.Logger;

public class BixbyListener extends Service {

    private final String TAG = "BixbyListener";
    private static boolean isRunning;
    private Network net;
    private long lastClick;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Logger.log(TAG, "onStartCommand");
        isRunning = true;

        net = Network.getInstance();
        net.setContext(this);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 1970);
        Date date = cal.getTime();

        @SuppressLint("SimpleDateFormat")
        Thread bixbyListener = new Thread(() -> {
            try {
//                Process process = Runtime.getRuntime().exec("logcat -e act=com.samsung.android.bixby.agent.action.BixbyVoiceLife cmp=com.samsung.android.bixby.agent/com.samsung.android.bixby.WinkService");
                //Process process = Runtime.getRuntime().exec("logcat -e ^(?=.*?\\bbixby\\b)(?=.*?\\bACTION_DOWN\\b)");
                Process process = Runtime.getRuntime().exec("logcat -e ^(?=.*?\\bBixbyController\\b)(?=.*?\\bKeyEvent\\b)");
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = bufferedReader.readLine()) != null) {

//                    if(!isRunning) break;
//                    System.out.println(line);

                    if(!line.contains("beginning")) {
                        Date logDate = null;
                        try {
//                            Logger.log(TAG, line.substring(0, 17));
                            logDate = new SimpleDateFormat("MM-dd HH:mm:ss.SSS").parse(line.substring(0, 17));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        if(Objects.requireNonNull(logDate).after(date)) {
                            if (line.contains("action = 0")) onBixbyDown();
                            else if(line.contains("action = 1")) onBixbyUp();
                        }
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        bixbyListener.start();

        startForeground();

        return super.onStartCommand(intent, flags, startId);
    }

    void onBixbyDown() {
        Logger.log("Bixby listener", "Action down");

        if(System.currentTimeMillis() - lastClick < 500) net.send(Bulbs.getBixbyString(), false);
        lastClick = System.currentTimeMillis();
    }

    void onBixbyUp() {
        Logger.log("Bixby listener", "Action up");
    }

    void onBixbyDoubleClick() {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.log(TAG, "onDestroy");
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
