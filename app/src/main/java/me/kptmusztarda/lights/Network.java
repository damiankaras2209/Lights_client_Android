package me.kptmusztarda.lights;

import android.graphics.Color;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class Network {

    private Socket sock;
    private SocketAddress socketAddress;
    private String receivedData = "";
    private boolean allowReconnecting = true, isConnecting, isConnected = false;
    private short status = -1; // -1 - disconnected / 0 - connecting / 1 - connected
    private final int timeout = 5;
    private int connectionAttempt = 0;
    private TextView statusTextView;

    private final static String TAG = "NETWORK";

    Network(String ip, int port, TextView statusTextView) {
        this.statusTextView = statusTextView;
        socketAddress = new InetSocketAddress(ip, port);
    }

    protected void connect() {
        allowReconnecting = true;
        if(status != 0) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    status = 0; updateUI();
                    Logger.log(TAG, "Connect thread started");
                    while (status == 0 && allowReconnecting) {
                        try {
                            connectionAttempt++;
                            Logger.log(TAG, "Connecting... attempt: " + connectionAttempt);
                            sock = new Socket();
                            sock.connect(socketAddress, 1000);
                            Logger.log(TAG, "Connected");
                            status = 1; updateUI();
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
                    Logger.log(TAG, "Connect thread finished");
                }
            }).start();
        }
    }

    protected void send(final String dataOut) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(status == 1) {
                        OutputStreamWriter out = new OutputStreamWriter(sock.getOutputStream());
                        PrintWriter pw = new PrintWriter(out, true);
                        Logger.log(TAG, "Sending: '" + dataOut + "'");
                        pw.print(dataOut);
                        pw.flush();
                    } else {
                        Logger.log(TAG, "Sending failed. Not connected");
                    }
                } catch (IOException e) {
                    Logger.log(TAG, e);
                }
            }
        }).start();
    }

    private void listen() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStreamReader in = new InputStreamReader(sock.getInputStream());
                    int n;
                    char cbuf[] = new char[20];
                    while (true) {
                        n = in.read(cbuf, 0, cbuf.length);
                        if(n > 0) {
                            receivedData = new String(cbuf).substring(0, n);
                            Logger.log(TAG, "Received: '" + receivedData + "'");
                        } else if(n == -1){
                            status = -1; updateUI();
                            Logger.log(TAG, "Disconnected");
                            if(status != 0 && allowReconnecting) {
                                Logger.log(TAG, "Reconnecting from listen loop");
                                connect();
                            }
                            throw new IOException("Connection closed");
                        }
                        cbuf = new char[20];
                    }
                } catch (IOException e) {
                    Logger.log(TAG, "Stopping listening loop (" + e.getMessage() + ")");
                }
            }
        }).start();
    }


    protected String getReceivedData() {
        return receivedData;
    }

    protected void clearReceivedData() {
        receivedData = "";
    }

    protected boolean isConnected() {
        return isConnected;
    }

    protected void updateUI() {
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
        Utilities.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusTextView.setText(str);
                statusTextView.setTextColor(Color.parseColor(col));
            }
        });
    }

    protected void closeSocket() {
        try {
            allowReconnecting = false;
            if(sock != null) {
                if (status == 1) sock.shutdownOutput();
                sock.close();
            }
        } catch (IOException e){
            Logger.log(TAG, e);
        }
    }

}