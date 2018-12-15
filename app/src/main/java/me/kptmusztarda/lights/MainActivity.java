package me.kptmusztarda.lights;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import me.kptmusztarda.handylib.Logger;


public class MainActivity extends Activity {

    private Network net;
    private Bulbs bulbs;

    private final static String TAG = "MAIN";

    private static final String permissions[] = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private void checkPermissions() {
        for(String p : permissions)
            if(ContextCompat.checkSelfPermission(this, p) != 0) {
                ActivityCompat.requestPermissions(this, permissions, 2137);
                break;
            }
    }
    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) android.os.Process.killProcess(android.os.Process.myPid());
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Logger.setDirectory("", "lights_log.txt");

        checkPermissions();

        Button[] switchButtons = new Button[6];
        switchButtons[0] = findViewById(R.id.button_switch_1);
        switchButtons[1] = findViewById(R.id.button_switch_2);
        switchButtons[2] = findViewById(R.id.button_switch_3);
        switchButtons[3] = findViewById(R.id.button_switch_4);
        switchButtons[4] = findViewById(R.id.button_switch_5);
        switchButtons[5] = findViewById(R.id.button_switch_6);
        Button switchAllButton = findViewById(R.id.button_switch_all);

        net = Network.getInstance();
        net.setContext(this);
        net.setStatusTextView(findViewById(R.id.status));
        Bulbs.setViews(this, switchButtons);

        for (int i = 0; i< switchButtons.length; i++) {
            final int x = i;
            switchButtons[i].setOnClickListener(view -> net.send(Bulbs.getSwitchOneString(x)));
        }

        switchAllButton.setOnClickListener(view -> net.send(Bulbs.getSwitchAllString()));

        Intent intent = new Intent(this, BixbyListener.class);
        if(!BixbyListener.isRunning()) startForegroundService(intent);

    }

    @Override
    public void onStart() {
        Logger.log(TAG, "Starting app");
        net.connect();
        net.setMakeToasts(false);
        Bulbs.setUpdateUI(true);
        Bulbs.updateUI();

        Intent intent = new Intent(this, BixbyListener.class);
        if(!BixbyListener.isRunning()) startForegroundService(intent);

        super.onStart();
    }

    @Override
    public void onStop() {
        net.closeSocket();
        net.setMakeToasts(true);
        Bulbs.setUpdateUI(false);
        super.onStop();
    }

}
