package me.kptmusztarda.lights;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.widget.Toast;

import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import me.kptmusztarda.handylib.Logger;
import me.kptmusztarda.handylib.Utilities;

class RequestQueue {

    private static final String TAG = "RequestQueue";


    private static final String networks[] = {"dlink-74A1", "dlink-74A1-5GHz", "ASUS", "ASUS_5GHz"};

    private static Context context = App.get().getApplicationContext();
    private static com.android.volley.RequestQueue instance = Volley.newRequestQueue(context);

    static com.android.volley.RequestQueue getInstance() {
        return instance;
    }

    static void add(StringRequest stringRequest) {

        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String connectedNetworkSSID = mWifiManager.getConnectionInfo().getSSID();

        boolean b = false;

        for(String network : networks)
            if(network.equals(connectedNetworkSSID.substring(1, connectedNetworkSSID.length() - 1))) {
                b = true;
                break;
            }

        if(b)
            getInstance().add(stringRequest);
        else
            Utilities.runOnUiThread(() -> Toast.makeText(context, "Not connected to home Wi-Fi", Toast.LENGTH_SHORT).show());

    }
}
