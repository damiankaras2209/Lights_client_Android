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
import java.util.logging.Handler;

import me.kptmusztarda.handylib.Logger;
import me.kptmusztarda.handylib.Utilities;

enum Status {

    IMPROPER_NETWORK("Improper network", Color.parseColor("#c62323")),
    DISCONNECTED("Disconnected", Color.parseColor("#c62323")),
    CONNECTING("Connecting...", Color.parseColor("#e8c61e")),
    CONNECTED("Connected", Color.parseColor("#27c449"));

    private String str;
    private int color;

    private Status(String str, int color) {
        this.str = str;
        this.color = color;
    }

    public String getString() {
        return str;
    }

    public int getColor() {
        return color;
    }
}

public class Network {

    private final static String TAG = "NETWORK";
    private static Network instance;
    private String ip = "192.168.0.131";
    private int port = 2137;

    private static final int MAX_CONNECTION_ATTEMPTS = 60;
    private static final int SERVER_ANSWER_LENGTH = 14;

    private Status status = Status.DISCONNECTED;

    private Socket sock;
    private SocketAddress socketAddress;
    private boolean allowReconnecting = true;

    private final int timeout = 1; // delay between connecting attempts in seconds
    private int connectionAttempt = 0;
    private long lastServerAnswer;

    private TextView statusTextView;
    private WifiManager wifiManager;
    private Context context;
    private boolean makeToasts;



    private Network() {
        socketAddress = new InetSocketAddress(ip, port);
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> Logger.log(TAG, e));
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
        Logger.log(TAG, "connect()");
        if(!isConnectedOrConnecting()) {
            isConnectedToProperWiFiNetwork();
            if (status.equals(Status.DISCONNECTED)) {
                new Thread(() -> {
                    allowReconnecting = true;
                    connectionAttempt = 0;
                    setStatus(Status.CONNECTING);
                    //Logger.log(TAG, "Connect thread started");
                    while (status.equals(Status.CONNECTING) && allowReconnecting && connectionAttempt <= MAX_CONNECTION_ATTEMPTS) {
                        try {
                            connectionAttempt++;
                            updateUI();
                            Logger.log(TAG, "Connecting... attempt: " + connectionAttempt);
                            sock = new Socket();
                            sock.connect(socketAddress, 1000);
                            Logger.log(TAG, "Connected");
                            setStatus(Status.CONNECTED);
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
    }

    synchronized void connectAndWait() {
        connect();
        while(!status.equals(Status.CONNECTED)) {
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
                if(status.equals(Status.CONNECTED)) {
                    OutputStreamWriter out = new OutputStreamWriter(sock.getOutputStream());
                    PrintWriter pw = new PrintWriter(out, true);
                    Logger.log(TAG, "Sending: '" + dataOut + "'");
                    pw.print(dataOut);
                    pw.flush();
                    Logger.log(TAG, "CloseAfterSend: " + Boolean.toString(closeAfterSend));
                    if(closeAfterSend) closeSocket();
                    else {
                        try {
                            Thread.sleep(100);
                            if(System.currentTimeMillis() - lastServerAnswer > 100) closeSocket();
                        } catch (InterruptedException e1) {
                            Logger.log(TAG, e1);
                        }
                    }
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
                char cbuf[] = new char[SERVER_ANSWER_LENGTH];
                while (true) {
                    n = in.read(cbuf, 0, cbuf.length);
                    if(n > 0) {
                        String receivedData = new String(cbuf).substring(0, n);
                        lastServerAnswer = System.currentTimeMillis();
                        Logger.log(TAG, "Received: '" + receivedData + "'");

                        Bulbs.setStatus(receivedData.substring(0, receivedData.indexOf('/')));

                        boolean isEndOfQuery = Integer.parseInt(receivedData.substring(receivedData.length()-1, receivedData.length())) == 1 ? true : false;
                        //Logger.log(TAG, "showToast?: " + Boolean.toString(showToast));

                        if(makeToasts && isEndOfQuery)
                            Utilities.runOnUiThread(() -> Toast.makeText(context, "Lights are " + (Bulbs.isAtLeastOneOn() ? "on" : "off"), Toast.LENGTH_SHORT).show());

                    } else if(n == -1){
                        setStatus(Status.DISCONNECTED);
                        Logger.log(TAG, "Disconnected");
                        if(!status.equals(Status.CONNECTING) && allowReconnecting) {
                            Logger.log(TAG, "Reconnecting from listen loop");
                            connect();
                        }
                        throw new IOException("Connection closed");
                    }
                    cbuf = new char[SERVER_ANSWER_LENGTH];
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
        return status.equals(Status.CONNECTING) || status.equals(Status.CONNECTED);
    }

    private boolean isConnectedToProperWiFiNetwork() {
        String networks[] = {"dlink-74A1", "dlink-74A1-5GHz", "ASUS", "ASUS_5GHz"};

        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String connectedNetworkSSID = mWifiManager.getConnectionInfo().getSSID();

        for(String network : networks)
            if(network.equals(connectedNetworkSSID.substring(1, connectedNetworkSSID.length() - 1))) {
                setStatus(Status.DISCONNECTED);
                return true;
            }

        if(makeToasts) Utilities.runOnUiThread(() -> Toast.makeText(context, "Not connected to a proper Wi-Fi network", Toast.LENGTH_SHORT).show());
        setStatus(Status.IMPROPER_NETWORK);
        return false;
    }

    private void setStatus(Status status) {
        this.status = status;
        if(!status.equals(Status.CONNECTED)) Bulbs.setStatus("021222324252");
        Logger.log(TAG, "New status: " + status);
        updateUI();
    }

    public void updateUI() {
        if(statusTextView != null) {
            //Logger.log(TAG, "updateUI()");
            Utilities.runOnUiThread(() -> {
                if(status.equals(Status.CONNECTING)) statusTextView.setText(status.getString() + " " + connectionAttempt);
                else statusTextView.setText(status.getString());
                statusTextView.setTextColor(status.getColor());
            });
        }
    }

    void closeSocket() {
        if (status.equals(Status.CONNECTED)) {
            Logger.log(TAG, "Closing socket");
            allowReconnecting = false;
            if (sock != null) {
                try {
                    sock.shutdownOutput();
                } catch (IOException e) {
                    //Logger.log(TAG, e);
                }
                try {
                    sock.close();
                } catch (IOException e) {
                    //Logger.log(TAG, e);
                }
            }
        } else if (status.equals(Status.CONNECTING)) {
            allowReconnecting = false;
        }
        isConnectedToProperWiFiNetwork();
    }

}