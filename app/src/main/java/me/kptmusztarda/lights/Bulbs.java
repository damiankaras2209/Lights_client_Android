package me.kptmusztarda.lights;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.Button;

import java.io.StringWriter;

import me.kptmusztarda.handylib.Logger;
import me.kptmusztarda.handylib.Utilities;

class Bulbs {

    private static int status[] = new int[6];
    private static int WAIT = 75;
    private static Button buttonViews[];
    private static Context context;
    private static boolean updateUI;
    private static final String TAG = "Bulbs";

//    Bulbs(int bulbsCount) {
//        status = new int[bulbsCount];
//    }

    static void setStatus(String str) {
        //Logger.log(TAG, "Sent to setStatus(): '" + str + "'");
        for(int i=0; i<status.length; i++) {
            final int stat = Integer.parseInt(Character.toString(str.charAt(i*2+1)));
            status[i] = stat;
        }
        updateUI();
    }

    static void updateUI() {
        if(updateUI) {
            for(int i=0; i<status.length; i++) {
                int j =i;
                Utilities.runOnUiThread(() -> {
                    buttonViews[j].setBackgroundColor(status[j] == 1 ? context.getColor(R.color.colorOn) : context.getColor(R.color.colorOff));
                });
            }
        }
    }

    static void setViews(Context ctx, Button buttons[]) {
        buttonViews = buttons;
        context = ctx;
    }

    static void setUpdateUI(boolean b) {
        updateUI = b;
    }

    @SuppressLint("DefaultLocale")
    static String getSwitchOneString(int ind) {
        return String.format("S%d%d", ind, Math.abs(status[ind] - 1));
    }

    @SuppressLint("DefaultLocale")
    private static String getSwitchAllString(boolean b) {
        StringWriter writer = new StringWriter();
        int val = b ? 1 : 0;
        for(int i=0; i<status.length; i++) {
            writer.write(String.format("S%d%d", i, val));
            if(i < status.length - 1) writer.write(String.format(",W%d,", WAIT));
        }

        return writer.toString();
    }

    @SuppressLint("DefaultLocale")
    static String getSwitchAllString() {
        boolean at_least_one_on = true;
        for (int stat : status) {
            if (stat == 0) at_least_one_on = false;
        }
        int val = at_least_one_on ? 0 : 1;

        StringWriter writer = new StringWriter();
        for(int i=0; i<status.length; i++) {
            writer.write(String.format("S%d%d", i, val));
            if(i < status.length - 1) writer.write(String.format(",W%d,", WAIT));
        }

        return writer.toString();
    }

    @SuppressLint("DefaultLocale")
    static String getBixbyString() {

        if(isAtLeastOneOn()) {
            return getSwitchAllString(false);
        } else {
            StringWriter writer = new StringWriter();
            for (int i = 0; i < status.length; i += 2) {
                writer.write(String.format("S%d%d", i, 1));
                Logger.log(TAG, "i: " + i + " , status.length: " + status.length);
                if (i+2<status.length) writer.write(String.format(",W%d,", WAIT));
            }
            return writer.toString();
        }
    }

    static boolean isAtLeastOneOn() {
        boolean at_least_one_on = false;
        for (int stat : status) {
//            Logger.log(TAG, "Status: " + stat);
            if (stat == 1) at_least_one_on = true;
        }
        return at_least_one_on;
    }

}
