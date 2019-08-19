package me.kptmusztarda.lights;

import android.content.Context;
import android.drm.DrmStore;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import me.kptmusztarda.handylib.Utilities;

class Bulbs {

    private static final String TAG = "Bulbs";

    private static int status[] = new int[6];
    private static int WAIT = 75;
    private static Button buttonViews[];
    private static TextView statusView;
    private static boolean updateUI;

    static void setStatus(String str) {
        for(int i=0; i<status.length; i++) {
            final int stat = Integer.parseInt(Character.toString(str.charAt(i*2+1)));
            status[i] = stat;
        }
        updateUI();
    }

    static void updateUI() {
        if(updateUI) {
            for(int i=0; i<status.length; i++) {
                int j=i;
                int color = 0;
                Context context = App.get().getApplicationContext();
                switch (status[j]) {
                    case 0: color = context.getColor(R.color.colorOff); break;
                    case 1: color = context.getColor(R.color.colorOn); break;
                    case 2: color = context.getColor(R.color.colorBasic); break;
                }
                int finalColor = color;
                Utilities.runOnUiThread(() -> {
                    buttonViews[j].setBackgroundColor(finalColor);
                });
            }
            Utilities.runOnUiThread(() -> {
                statusView.setText(new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(new Date()));
            });
        }
    }

    static void setViews(Button buttons[], TextView tv) {
        buttonViews = buttons;
        statusView = tv;
    }

    static void setUpdateUI(boolean b) {
        updateUI = b;
    }


    static boolean isAtLeastOneOn() {
        boolean at_least_one_on = false;
        for (int stat : status) {
            if (stat == 1) at_least_one_on = true;
        }
        return at_least_one_on;
    }

}
