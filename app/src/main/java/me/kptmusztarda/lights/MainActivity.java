package me.kptmusztarda.lights;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends Activity {

    private Network net;
    private Bulbs bulbs;

    private final static String TAG = "MAIN";

    private void checkPermissions() {
        int permission1 = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int permission2 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permission1 != 0 || permission2 != 0) ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2137);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Logger.setDirectory("", "lights_log.txt");
        Logger.enableLogging(true);

        checkPermissions();

        Button[] switchButtons = new Button[6];
        switchButtons[0] = findViewById(R.id.button_switch_1);
        switchButtons[1] = findViewById(R.id.button_switch_2);
        switchButtons[2] = findViewById(R.id.button_switch_3);
        switchButtons[3] = findViewById(R.id.button_switch_4);
        switchButtons[4] = findViewById(R.id.button_switch_5);
        switchButtons[5] = findViewById(R.id.button_switch_6);
        Button switchAllButton = findViewById(R.id.button_switch_all);

        net = new Network("192.168.0.131", 2137, (TextView)findViewById(R.id.status));
        bulbs = new Bulbs(6);

        for (int i = 0; i< switchButtons.length; i++) {
            final int x = i;
            switchButtons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    net.send(bulbs.getSwitchOneString(x));
                }
            });
        }

        switchAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                net.send(bulbs.getSwitchAllString());
            }
        });

        Thread listener = new Thread(new Runnable() {
            @Override
            public void run() {
                String str = "";
                while (true) {
                    str = net.getReceivedData();
                    if (str.length() > 0) {
                        bulbs.setStatus(str);
                        net.clearReceivedData();
                    }
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        Logger.log(TAG, e);
                    }
                }
            }
        });
        listener.start();

    }

    public void onStart() {
        Logger.space();
        Logger.log(TAG, "Starting app");
        net.connect();
        super.onStart();
    }

    public void onStop() {
        net.closeSocket();
        super.onStop();
    }

}
