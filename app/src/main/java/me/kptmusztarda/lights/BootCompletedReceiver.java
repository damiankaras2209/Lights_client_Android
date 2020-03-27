package me.kptmusztarda.lights;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import me.kptmusztarda.handylib.Logger;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.setDirectory("", "lights_log.txt");
        Logger.log("BootCompletedReceiver", "Boot completed");
        Intent intent1 = new Intent(context, BroadcastReceiverService.class);
        if(!BroadcastReceiverService.isRunning()) context.startForegroundService(intent1);
    }
}
