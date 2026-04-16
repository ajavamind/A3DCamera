package com.andymodla.android3dcamera;

/**
 * A3DCamera app
 * Copyright 2025-2026, Andy Modla All Rights Reserved
 * <p>
 * This code has video implementation left in for reference.
 * Video remote control is not used or tested here.
 */

import java.net.DatagramSocket;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.Log;
import android.content.Context;

import netP5.Bytes;
import netP5.NetListener;
import netP5.NetMessage;
import netP5.NetStatus;
import netP5.UdpClient;
import netP5.UdpServer1;

import com.andymodla.android3dcamera.camera.Camera3D;

public class UdpRemoteControl {
    private static final String TAG = "UdpRemoteControl";


    private boolean isVideo = false;
    // UDP Server variables
    private volatile UdpServer1 udpServer;    // Broadcast receiver
    private int udpPort = 8000;  // Broadcast port
    public static String httpUrl = "";
    public static String sCount = ""; // Photo counter received from Broadcast message
    private static final int MAXPARAM_SIZE = 16;
    private Context context;
    private Camera3D camera;
    private boolean isUdpTransmitter = false;

    // Transmitter section variables
    private static final int doubleTriggerDelay = 100;
    UdpClient udpClient;

    int photoIndex = 0;  // next photo index for filename
    int videoIndex = 0;  // next video index for filename
    boolean useTimeStamp = true;
    String numberFilename = ""; // last used number filename
    String datetimeFilename = ""; // last used date_time filename
    String lastFilename = ""; // last used filename

    static final int SAME = 0;
    static final int UPDATE = 1;
    static final int NEXT = 2;
    static final int PHOTO_MODE = 0;
    static final int VIDEO_MODE = 1;
    int mode = PHOTO_MODE;
    boolean connected;
    String broadcastIpAddress;
    boolean focus = false;
    // Define an Executor at the class level (to reuse it)
    private ExecutorService executorService;

    //
    // constructor
    public UdpRemoteControl(Context context) {
        this.context = context;
    }

    /**
     * UDP server transmitter or receiver setup
     * Receiver create the UDP server to listen for incoming broadcast messages,
     * create a listener for the server.
     * A listener will receive NetMessages which contain camera commands.
     * NetListener is an interface and requires methods netEvent
     * and netStatus.
     */
    public void setUdpTransmitter(Camera3D camera, String hostIpAddress) {
        this.camera = camera;
        isUdpTransmitter = true;
        if (MyDebug.LOG) Log.d(TAG, "setUdpTransmitter " + hostIpAddress);
        executorService = Executors.newSingleThreadExecutor();
        //if (isUdpTransmitter) {
            // extract local network broadcast address from host IP address
            broadcastIpAddress = hostIpAddress.substring(0, hostIpAddress.lastIndexOf(".")) + ".255";
            udpClient = null;
            try {
                udpClient = new UdpClient(broadcastIpAddress, udpPort);  // from netP5.* library
                Log.d(TAG, "UdpClient " + broadcastIpAddress);
            } catch (Exception e) {
                Log.d(TAG, "Wifi problem");
                udpClient = null;
            }
            connected = false;
            if (udpClient != null) {
                connected = true;
                Log.d(TAG, "Wifi connected " + broadcastIpAddress);
            } else {
                Log.d(TAG, "Wifi not connected " + broadcastIpAddress);
            }
        //}
    }

    public void setUdpReceiver(Camera3D camera, String hostIpAddress) {
        if (udpServer == null) {
            // first create listener for UDP messages
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
                                ((MainActivity) context).runOnUiThread(new Runnable() {
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
                            ((MainActivity) context).runOnUiThread(new Runnable() {
                                public void run() {
                                    ((MainActivity) context).capturePhoto();
                                }
                            });
                        } else {
                            ToastHelper.showToast(context, "Remote Not In Photo Mode");
                        }
                    } else if (command.startsWith("V")) { // record/stop video
                        sCount = getParam(data);
                        if (MyDebug.LOG) Log.d(TAG, "remote record video " + sCount);
                        if (isVideo) {
                            ((MainActivity) context).runOnUiThread(new Runnable() {
                                public void run() {
                                    if (isVideoRecording()) {
                                        stopVideo(true);
                                    } else {
                                        ((MainActivity) context).capturePhoto();
                                    }
                                }
                            });
                        } else {
                            ToastHelper.showToast(context, "Remote Not In Video Mode");
                        }
                    } else if (command.startsWith("P")) { // pause
                        if (MyDebug.LOG) Log.d(TAG, "remote pause Video ");
                        if (isVideo) {
                            ((MainActivity) context).runOnUiThread(new Runnable() {
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

            // Now create UDP server for receiving Broadcast messages with listener created above
            if (udpServer == null) {
                udpServer = new UdpServer1(udpListener, udpPort);
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

    // stop UDP server and remove listeners for broadcast message reception
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
        stopUDP();  // transmitter section
    }


    /*=====================================================
     * Receiver section
     *
     */

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

    /*=====================================================
     * Transmitter section
     *
     */



    void updatePhotoIndex() {
        photoIndex++;
        if (photoIndex > 9999) {
            photoIndex = 1;
        }
    }

    void updateVideoIndex() {
        videoIndex++;
        if (videoIndex > 9999) {
            videoIndex = 1;
        }
    }

    /**
     * get filename for Open Camera Remote
     * param 0 update: SAME, UPDATE, NEXT
     * param 1 mode
     */
    String getFilename(int update, int mode) {
        String fn = "";
        if (useTimeStamp) {
            if (update == UPDATE || update == NEXT) {
                fn = getDateTime();
                datetimeFilename = fn;
            } else {  // SAME
                fn = datetimeFilename;
            }
        } else {
            if (mode == PHOTO_MODE) {
                if (update == SAME) {
                    fn = number(photoIndex);
                    numberFilename = fn;
                } else if (update == UPDATE) {
                    updatePhotoIndex();
                    fn = number(photoIndex);
                    numberFilename = fn;
                } else { // NEXT
                    fn = number(photoIndex + 1);
                }
            } else {
                if (update == SAME) {
                    fn = number(videoIndex);
                    numberFilename = fn;
                } else if (update == UPDATE) {
                    updateVideoIndex();
                    fn = number(videoIndex);
                    numberFilename = fn;
                } else {  // NEXT
                    fn = number(videoIndex + 1);
                }
            }
        }
        lastFilename = fn;
        return fn;
    }

    boolean isActive() {
        if (udpClient != null) {
            return true;
        }
        return false;
    }

    void stopUDP() {
        if (udpClient != null) {
            DatagramSocket ds = udpClient.socket();
            if (ds != null) {
                ds.close();
                ds.disconnect();
            }
            udpClient = null;
        }
    }

    void focusPush() {
        if (udpClient != null) {
            udpClient.send("F");
        }
    }

    void focusRelease() {
        focus = false;
        if (udpClient != null) {
            udpClient.send("R");
        }
    }

    void sendFocusReleasePush() {
        //  When you need to call the network method:
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // This runs on a background thread
                focusRelease();
                focusPush();
            }
        });
    }

        void shutterPush() {
        if (udpClient != null) {
            udpClient.send("S" + getFilename(UPDATE, PHOTO_MODE));
        }
    }

    void shutterRelease() {
        if (udpClient != null) {
            udpClient.send("R");
        }
    }

    void record() {
        if (udpClient != null) {
            udpClient.send("V" + getFilename(UPDATE, VIDEO_MODE));
        }
    }

    void cameraOk() {
        if (udpClient != null) {
            udpClient.send("P"); // pause in video mode
        }
    }

    void shutterPushRelease() {
        if (udpClient != null) {
            udpClient.send("S" + getFilename(UPDATE, PHOTO_MODE));
            udpClient.send("R");
        }
    }
    void sendShutterPushRelease() {
        //  When you need to call the network method:
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // This runs on a background thread
                shutterPushRelease();
            }
        });
    }

    void takePhoto(boolean doubleTrigger, boolean shutter) {
//        if (doubleTrigger) {
//            focusPush();
//            shutterPush();
//            delay(doubleTriggerDelay);
//            if (shutter) {
//                shutterPush();
//            } else {
//                shutterPushRelease();
//            }
//        } else {
        if (udpClient != null) {
            if (shutter) {
                udpClient.send("S" + getFilename(UPDATE, PHOTO_MODE));
            } else {
                udpClient.send("C" + getFilename(UPDATE, PHOTO_MODE));
            }
        }
        //}
    }

    private String getDateTime() {
        String timestamp = camera.getTimestamp();
        if (timestamp == null) timestamp = "";
        return timestamp;
    }

    // Add leading zeroes to number
    String number(int index) {
        // fix size of index number at 4 characters long
        if (index == 0)
            return "";
        else if (index < 10)
            return ("000" + String.valueOf(index));
        else if (index < 100)
            return ("00" + String.valueOf(index));
        else if (index < 1000)
            return ("0" + String.valueOf(index));
        return String.valueOf(index);
    }


}
