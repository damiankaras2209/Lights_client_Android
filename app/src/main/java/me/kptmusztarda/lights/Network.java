package me.kptmusztarda.lights;

import android.content.Context;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import me.kptmusztarda.handylib.Logger;
import me.kptmusztarda.handylib.Utilities;

public class Network {

    private final static String TAG = "NETWORK";
    private static Network instance;
    private String ip = "192.168.0.131";
    private int port = 2137;

    private static final int MAX_CONNECTION_ATTEMPTS = 60;

    private static final int DISCONNECTED = -1;
    private static final int CONNECTING = 0;
    private static final int CONNECTED = 1;

    private int status;

    private Socket sock;
    private SocketAddress socketAddress;
    private boolean allowReconnecting = true;

    private final int timeout = 1; // delay between connecting attempts in seconds
    private int connectionAttempt = 0;
    private TextView statusTextView;
    private long lastReceive = System.currentTimeMillis();
    private WifiManager wifiManager;
    private Context context;
    private boolean makeToasts;



    private Network() {
        socketAddress = new InetSocketAddress(ip, port);
        setStatus(DISCONNECTED);
    }

    static {
        instance = new Network();
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setStatusTextView(TextView view) {
        statusTextView = view;
    }

    public void setMakeToasts(boolean b) {
        makeToasts = b;
    }

    public static Network getInstance() {
        return instance;
    }

    void connect() {
        Logger.log(TAG, "connect() status: " + status + " properNetwork: " + Boolean.toString(isConnectedToProperWiFiNetwork()));
        if(status == DISCONNECTED && isConnectedToProperWiFiNetwork()) {
            allowReconnecting = true;
            new Thread(() -> {
                setStatus(CONNECTING);
                //Logger.log(TAG, "Connect thread started");
                while (status == CONNECTING && allowReconnecting && connectionAttempt <= MAX_CONNECTION_ATTEMPTS ) {
                    try {
                        connectionAttempt++;
                        Logger.log(TAG, "Connecting... attempt: " + connectionAttempt);
                        sock = new Socket();
                        sock.connect(socketAddress, 1000);
                        Logger.log(TAG, "Connected");
                        setStatus(CONNECTED);
                        listen();
                    } catch (IOException e) {
                        Logger.log(TAG, "Connetion failed (" + e.getMessage() + "). Retrying in " + timeout + " seconds");
                        try {
                            Thread.sleep(timeout * 1000);
                        } catch (InterruptedException e1) {
                            Logger.log(TAG, e1);
                        }
                    }
                }
                //Logger.log(TAG, "Connect thread finished");
            }).start();
        }
    }

    synchronized void connectAndWait() {
        connect();
        while(status != CONNECTED) {
            try {
                wait(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void send(final String dataOut, boolean closeAfterSend) {
        new Thread(() -> {
            try {
                if(status == CONNECTED) {
                    OutputStreamWriter out = new OutputStreamWriter(sock.getOutputStream());
                    PrintWriter pw = new PrintWriter(out, true);
                    Logger.log(TAG, "Sending: '" + dataOut + "'");
                    pw.print(dataOut);
                    pw.flush();
                    Logger.log(TAG, "CloseAfterSend: " + Boolean.toString(closeAfterSend));
                    if(closeAfterSend) closeSocket();
                } else {
                    Logger.log(TAG, "Sending failed. Not connected");
                    connectAndWait();
                    send(dataOut, closeAfterSend);
                }
            } catch (IOException e) {
                Logger.log(TAG, e);
            }
        }).start();
    }

    void send(final String dataOut) {
        send(dataOut, false);
    }


    private void listen() {
        new Thread(() -> {
            try {
                InputStreamReader in = new InputStreamReader(sock.getInputStream());
                int n;
                char cbuf[] = new char[20];
                while (true) {
                    n = in.read(cbuf, 0, cbuf.length);
                    if(n > 0) {
                        String receivedData = new String(cbuf).substring(0, n);
                        Logger.log(TAG, "Received: '" + receivedData + "'");

                        Bulbs.setStatus(receivedData.substring(0, receivedData.indexOf('/')));

                        boolean isEndOfQuery = Integer.parseInt(receivedData.substring(receivedData.length()-1, receivedData.length())) == 1 ? true : false;
                        //Logger.log(TAG, "showToast?: " + Boolean.toString(showToast));

                        if(makeToasts && isEndOfQuery)
                            Utilities.runOnUiThread(() -> Toast.makeText(context, "Lights are " + (Bulbs.isAtLeastOneOn() ? "on" : "off"), Toast.LENGTH_SHORT).show());

                    } else if(n == -1){
                        setStatus(DISCONNECTED);
                        Logger.log(TAG, "Disconnected");
                        if(status != CONNECTING && allowReconnecting) {
                            Logger.log(TAG, "Reconnecting from listen loop");
                            connect();
                        }
                        throw new IOException("Connection closed");
                    }
                    cbuf = new char[20];
                }
            } catch (IOException e) {
                Logger.log(TAG, "Stopping listening loop (" + e.getMessage() + ")");
                closeSocket();
            }
        }).start();
    }


//    protected String getReceivedData() {
//        return receivedData;
//    }
//
//    protected void clearReceivedData() {
//        receivedData = "";
//    }

    protected boolean isConnectedOrConnecting() {
        return status == CONNECTING || status == CONNECTED;
    }

    private void setStatus(int status) {
        this.status = status;
        updateUI();
        Logger.log(TAG, "New status: " + status);
    }

    private boolean isConnectedToProperWiFiNetwork() {
        String networks[] = {"dlink-74A1", "dlink-74A1-5GHz", "ASUS", "ASUS"};

        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String connectedNetworkSSID = mWifiManager.getConnectionInfo().getSSID();

        for(String network : networks) {
            if(network.equals(connectedNetworkSSID.substring(1, connectedNetworkSSID.length() - 1))) {
                return true;
            }
        }

        if(makeToasts) Toast.makeText(context, "Not connected to a proper Wi-Fi network", Toast.LENGTH_SHORT).show();
        return false;
    }

    private void updateUI() {
        if(statusTextView != null) {
            String statusString = "";
            String statusColor = "#000";
            switch (status) {
                case -1:
                    statusString = "Disconnected";
                    statusColor = "#c62323";
                    break;
                case 0:
                    statusString = "Connecting";
                    statusColor = "#e8c61e";
                    break;
                case 1:
                    statusString = "Connected";
                    statusColor = "#27c449";
                    break;
            }
            final String str = statusString;
            final String col = statusColor;
            Utilities.runOnUiThread(() -> {
                statusTextView.setText(str);
                statusTextView.setTextColor(Color.parseColor(col));
            });
        }
    }

    void closeSocket() {
        if (status == CONNECTED) {
            Logger.log(TAG, "Closing socket");
            setStatus(DISCONNECTED);
            allowReconnecting = false;
            if (sock != null) {
                try {
                    sock.shutdownOutput();
                } catch (IOException e) {
                    Logger.log(TAG, e);
                }
                try {
                    sock.close();
                } catch (IOException e) {
                    Logger.log(TAG, e);
                }
            }
        }
    }

}