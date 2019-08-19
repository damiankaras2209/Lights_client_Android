package me.kptmusztarda.lights;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;
import com.skydoves.colorpickerview.ActionMode;
import com.skydoves.colorpickerview.ColorPickerView;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;
import com.skydoves.colorpickerview.sliders.AlphaSlideBar;
import com.skydoves.colorpickerview.sliders.BrightnessSlideBar;

import me.kptmusztarda.handylib.Logger;


public class MainActivity extends Activity {

    private Bulbs bulbs;

    private final static String TAG = "MAIN";
    private static final int MIN_HTTP_REQUEST_INTERVAL = 1000;
    private static long lastRequest = 0;

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


    @SuppressLint("ClickableViewAccessibility")
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
        ImageButton refreshButton = findViewById(R.id.refresh_button);

        TextView statusTextView = findViewById(R.id.status);

        Bulbs.setViews(switchButtons, statusTextView);


//        com.android.volley.RequestQueue requestQueue = Volley.newRequestQueue(this);
//        String url ="http://192.168.0.131:2138/webapp/status";
//
//        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
//                response -> {
//                    // Display the first 500 characters of the response string.
//                    System.out.println("Response is: "+ response);
//                }, error -> System.out.println("Nie dziorgo"));
//
//        requestQueue.add(stringRequest);


        for (int i = 0; i< switchButtons.length; i++) {
            int finalI = i;
            switchButtons[i].setOnClickListener(view -> RequestQueue.add(
                    new StringRequest(Request.Method.GET,
                            "http://192.168.0.131:2138/webapp/switch?id=" + finalI + "&state=toggle",
                            Bulbs::setStatus,
                            error -> Logger.log(TAG, "Nie dziorgo: " + error)
                            )));
        }

        switchAllButton.setOnClickListener(view -> RequestQueue.add(
                new StringRequest(Request.Method.GET,
                        "http://192.168.0.131:2138/webapp/switch?id=all&state=toggle",
                        Bulbs::setStatus,
                        error -> Logger.log(TAG, "Nie dziorgo: " + error)
                )));

        refreshButton.setOnClickListener(v -> RequestQueue.add(new StringRequest(Request.Method.GET,
                "http://192.168.0.131:2138/webapp/status",
                Bulbs::setStatus,
                error -> Logger.log(TAG, "Nie dziorgo: " + error)
        )));


        ColorPickerView colorPickerView = findViewById(R.id.colorPickerView);
        colorPickerView.setActionMode(ActionMode.LAST);
        colorPickerView.setColorListener((ColorEnvelopeListener) (color, fromUser) -> {
            if(fromUser) {

                int r = Integer.parseInt(color.getHexCode().substring(2, 4), 16);
                int g = Integer.parseInt(color.getHexCode().substring(4, 6), 16);
                int b = Integer.parseInt(color.getHexCode().substring(6), 16);

                @SuppressLint("DefaultLocale") String json = String.format("{\"m\":3,\"r\":%d,\"g\":%d,\"b\":%d}", r, g, b);

//                System.out.print(json);

                RequestQueue.add(new StringRequest(Request.Method.GET,
                    "http://192.168.0.131:8080/json.htm?type=command&param=setcolbrightnessvalue&idx=1&color=" + json + "&brightness=" + (int)((float)(color.getArgb()[0])/255*100),
                    response -> {},
                    error -> Logger.log(TAG, "Nie dziorgo: " + error)
                ));

                lastRequest = System.currentTimeMillis();
            }
        });

        BrightnessSlideBar brightnessSlideBar = findViewById(R.id.brightnessSlide);
        colorPickerView.attachBrightnessSlider(brightnessSlideBar);

        AlphaSlideBar alphaSlideBar = findViewById(R.id.alphaSlideBar);
        colorPickerView.attachAlphaSlider(alphaSlideBar);

    }

    @Override
    public void onStart() {
        Logger.log(TAG, "Starting app");
        Bulbs.setUpdateUI(true);
        Bulbs.updateUI();

        RequestQueue.add(new StringRequest(Request.Method.GET,
                "http://192.168.0.131:2138/webapp/status",
                Bulbs::setStatus,
                error -> Logger.log(TAG, "Nie dziorgo: " + error)
        ));

        Intent intent = new Intent(this, BroadcastReceiverService.class);
        if(!BroadcastReceiverService.isRunning()) startForegroundService(intent);

        super.onStart();
    }

    @Override
    public void onStop() {
        Bulbs.setUpdateUI(false);
        super.onStop();
    }

}
