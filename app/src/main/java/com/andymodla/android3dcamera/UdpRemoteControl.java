package com.andymodla.android3dcamera;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Vector;
import android.util.Log;
import android.content.Context;
import netP5.Bytes;
import netP5.NetListener;
import netP5.NetMessage;
import netP5.NetStatus;
import netP5.UdpServer1;

public class UdpRemoteControl {
    private static final String TAG = "UdpRemoteControl";


    private boolean isVideo = false;
    // UDP Server variables
    private volatile UdpServer1 udpServer;    // Broadcast receiver
    private int port = 8000;  // Broadcast port
    public static String httpUrl = "";
    public static String sCount = ""; // Photo counter received from Broadcast message
    private static final int MAXPARAM_SIZE = 16;
    private Context context;


    public UdpRemoteControl(Context context) {
        this.context = context;
    }

    /**
     * UDP server setup
     * Create the UDP server to listen for incoming broadcast messages,
     * create a listener for the server.
     * A listener will receive NetMessages which contain camera commands.
     * NetListener is an interface and requires methods netEvent
     * and netStatus.
     */
    public void setup() {

        if (udpServer == null) {
            // create listener for UDP messages
            NetListener udpListener = new NetListener() {
                public void netEvent(NetMessage m) {
                    if (MyDebug.LOG)
                        Log.d(TAG, "netEvent from ip address=" + m.getDatagramPacket().getAddress());
                    byte[] data = m.getData();
                    byte[] b = new byte[1];
                    b[0] = data[0];
                    String command = Bytes.getAsString(b);
                    if (MyDebug.LOG) Log.d(TAG, "netEvent (UDP Server) command=" + command);
                    if (command.startsWith("F")) {
                        if (!isVideo) {
                            if (!isFocusWaiting()) {
                                if (MyDebug.LOG) {
                                    Log.d(TAG, "remote request focus");
                                }
                                ((MainActivity)context).runOnUiThread(new Runnable() {
                                    public void run() {
                                        requestAutoFocus();
                                    }
                                });
                            }
                        } else {
                            ToastHelper.showToast(context, "Remote Not In Photo Mode");
                        }
                    } else if (command.startsWith("S") || command.startsWith("C")) {
                        sCount = getParam(data);
                        if (MyDebug.LOG) Log.d(TAG, "remote takePicture() " + sCount);
                        if (!isVideo) {
                            ((MainActivity)context).runOnUiThread(new Runnable() {
                                public void run() {
                                    //takePicture(false);
                                }
                            });
                        } else {
                            ToastHelper.showToast(context, "Remote Not In Photo Mode");
                        }
                    } else if (command.startsWith("V")) { // record/stop video
                        sCount = getParam(data);
                        if (MyDebug.LOG) Log.d(TAG, "remote record video " + sCount);
                        if (isVideo) {
                            ((MainActivity)context).runOnUiThread(new Runnable() {
                                public void run() {
                                    if (isVideoRecording()) {
                                        stopVideo(true);
                                    } else {
                                        //takePicture(false);
                                    }
                                }
                            });
                        } else {
                            ToastHelper.showToast(context, "Remote Not In Video Mode");
                        }
                    } else if (command.startsWith("P")) { // pause
                        if (MyDebug.LOG) Log.d(TAG, "remote pause Video ");
                        if (isVideo) {
                            ((MainActivity)context).runOnUiThread(new Runnable() {
                                public void run() {
                                    if (isVideoRecording()) {
                                        pauseVideo();
                                    }
                                }
                            });
                        } else {
                            ToastHelper.showToast(context, "Remote Not In Video Mode");
                        }
                    } else if (command.startsWith("R")) { // reset / information request
                        httpUrl = getHostnameUrl();
                        if (MyDebug.LOG) Log.d(TAG, "Reset information request " + httpUrl);
                        ToastHelper.showToast(context, httpUrl);
                    }
                }

                public void netStatus(NetStatus s) {
                    if (MyDebug.LOG) Log.d(TAG, "netStatus (UDP Server) : " + s);
                }
            };
            // UDP server for receiving Broadcast messages
            if (udpServer == null) {
                udpServer = new UdpServer1(udpListener, port);
                if (udpServer == null) {
                    if (MyDebug.LOG) Log.d(TAG, "UdpServer error");
                    ToastHelper.showToast(context, "Remote Message Server not running");
                } else {
                    if (udpServer.socket() == null) {
                        if (MyDebug.LOG) Log.d(TAG, "UdpServer not connected retry");
                    }
                }
            }
        }
    }


    private String getParam(byte[] data) {
        String param = "";
        byte[] s = new byte[MAXPARAM_SIZE];
        boolean done = false;
        for (int i = 0; i < MAXPARAM_SIZE; i++) {
            byte b = data[i + 1];
            if (done || b == 0 || b == '\r' || b == '\n') {
                s[i] = ' ';
                done = true;
            } else {
                s[i] = data[i + 1];
            }
        }
        param = Bytes.getAsString(s).trim();
        return param;
    }

    public String getHostnameUrl() {
        String hostname = getHostnameAddress();
        //httpUrl = "http://" + hostname + ":" + serverPort +"/";
        httpUrl = "http://" + hostname + "/";
        return httpUrl;
    }

//    public String getHostnameAddress() {
//        WifiManager wifiMan = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        WifiInfo wifiInf = wifiMan.getConnectionInfo();
//        int ipAddress = wifiInf.getIpAddress();
//        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
//        return ip;
//    }

    public String getHostnameAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                    .hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
                String last = null;
                while (niEnum.hasMoreElements()) {
                    NetworkInterface ni = niEnum.nextElement();
                    if (!ni.isLoopback() && !ni.isPointToPoint()) {
                        for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                            if (MyDebug.LOG)
                                Log.d(TAG, "network prefix length=" + interfaceAddress.getNetworkPrefixLength());
                            if (interfaceAddress.getAddress() != null) {
                                if (MyDebug.LOG)
                                    Log.d(TAG, interfaceAddress.getAddress().getHostAddress());
                                last = (interfaceAddress.getAddress().getHostAddress());
                                if (last.matches("\\d*\\.\\d*\\.\\d*\\.\\d*")) {
                                    return last;
                                }
                            }
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            if (MyDebug.LOG) {
                Log.d(TAG, "Socket Exception");
            }
        }
        return null;
    }

    public String getBroadcastAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en
                    .hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces();
                while (niEnum.hasMoreElements()) {
                    NetworkInterface ni = niEnum.nextElement();
                    if (!ni.isLoopback()) {
                        for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                            if (interfaceAddress.getBroadcast() != null) {
                                //println(interfaceAddress.getBroadcast().toString());
                                return (interfaceAddress.getBroadcast().toString().substring(1));
                            }
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            if (MyDebug.LOG) {
                Log.d(TAG, "Socket Exception");
            }
        }
        return null;
    }

    // stop UDP server for broadcast message reception
    void destroy() {
        if (MyDebug.LOG) Log.d(TAG, "Destroy UDP server");
        if (udpServer != null) {
            Vector list = udpServer.getListeners();
            if (MyDebug.LOG) Log.d(TAG, "NetListener size=" + list.size());
            for (int i = 0; i < list.size(); i++) {
                udpServer.removeListener((NetListener) list.get(i));
                if (MyDebug.LOG) Log.d(TAG, "remove NetListener " + list.get(i).toString());
            }
            udpServer.stop();
            udpServer.dispose();
            udpServer = null;
        }
    }

    private void requestAutoFocus() {
    }

    private void stopVideo(boolean b) {
    }

    private boolean isVideoRecording() {
        return false;
    }

    private void pauseVideo() {
    }

    private boolean isFocusWaiting() {
        return false;
    }

}
