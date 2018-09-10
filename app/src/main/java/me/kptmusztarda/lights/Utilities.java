package me.kptmusztarda.lights;

import android.os.Handler;
import android.os.Looper;

public class Utilities {

    public static void runOnUiThread(Runnable runnable){
        final Handler UIHandler = new Handler(Looper.getMainLooper());
        UIHandler .post(runnable);
    }

}


