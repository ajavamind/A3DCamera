package com.andymodla.android3dcamera;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ImageSender {
    private static final String TAG = "A3DCamera";

    //private NsdManager nsdManager;
    //private NsdManager.DiscoveryListener discoveryListener;
    private String targetImageUrl;
    private OkHttpClient httpClient = new OkHttpClient();
    private Parameters parameters;
    private Context context;
    private UdpRemoteControl udpRemoteControl;

    // constructor
    public ImageSender(Context context, Parameters parameters, UdpRemoteControl udpRemoteControl) {
    //nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        this.parameters = parameters;
        this.context = context;
        this.udpRemoteControl = udpRemoteControl;
    }

    public void sendImageUrl(String imageUrl, String ip, int port) {
        this.targetImageUrl = imageUrl;
        //String ip = "10.0.0.50";   // 3D tablet destination
        //String ip = "192.168.8.131";   // 3D tablet IQH3D SKYY
        //String ip = "192.168.8.208";   // 3D tablet Leia 1
        //int port = 9000;
        try {
            if (parameters.getUdpTransmit()){
                sendUrlBroadcast(targetImageUrl);
            } else {
                sendUrlToReceiver(ip, port, targetImageUrl);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //initializeDiscoveryListener();
        //nsdManager.discoverServices("_img-receiver._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

//    private void initializeDiscoveryListener() {
//        discoveryListener = new NsdManager.DiscoveryListener() {
//            @Override
//            public void onServiceFound(NsdServiceInfo serviceInfo) {
//                // Found a service! Now resolve it to get the IP.
//                nsdManager.resolveService(serviceInfo, new NsdManager.ResolveListener() {
//                    @Override
//                    public void onServiceResolved(NsdServiceInfo resolvedService) {
//                        //String ip = resolvedService.getHost().getHostAddress();
//                        //int port = resolvedService.getPort();
//                        String ip = "10.0.0.50";   // 3D tablet
//                        int port = 9000;
//                        sendUrlToReceiver(ip, port, targetImageUrl);
//                    }
//
//                    @Override
//                    public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {}
//                });
//            }
//
//            @Override public void onDiscoveryStarted(String regType) {}
//            @Override public void onServiceLost(NsdServiceInfo serviceInfo) {}
//            @Override public void onDiscoveryStopped(String serviceType) {}
//            @Override public void onStartDiscoveryFailed(String serviceType, int errorCode) { nsdManager.stopServiceDiscovery(this); }
//            @Override public void onStopDiscoveryFailed(String serviceType, int errorCode) { nsdManager.stopServiceDiscovery(this); }
//        };
//    }

    private void sendUrlToReceiver(String ip, int port, String urlToSend) {
        // Construct the URL for our HTTPD server
        String fullUrl = "http://" + ip + ":" + port + "?imageUrl=" + urlToSend;
        // Creates a new client sharing the same connection pool but with custom timeouts
        Log.d(TAG, "sendUrlToReceiver: " + fullUrl);
        //OkHttpClient extendedClient = httpClient.newBuilder")
        OkHttpClient extendedClient = httpClient.newBuilder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .build();

        new Thread(() -> {
            try {
                Request request = new Request.Builder().url(fullUrl).build();
                try (Response response = extendedClient.newCall(request).execute()) {
                    // Success! The receiver now has the intent to download.
                }
            } catch (Exception e) {
                //e.printStackTrace();
                Log.d(TAG, "Exception in sendUrlToReceiver: " + e.getMessage());
            } finally {
                // Stop discovery to save battery once sent
                //nsdManager.stopServiceDiscovery(discoveryListener);
            }
        }).start();
    }

    private void sendUrlBroadcast( String urlToSend) {
        // Construct the URL for our UDP transmitter

        Log.d(TAG, "sendUrlBroadcast: " + urlToSend);
        new Thread(() -> {
            try {
                udpRemoteControl.sendBroadcast(urlToSend);
            } catch (Exception e) {
                //e.printStackTrace();
                Log.d(TAG, "Exception in sendUrlToReceiver: " + e.getMessage());
            } finally {
                // Stop discovery to save battery once sent
                //nsdManager.stopServiceDiscovery(discoveryListener);
            }
        }).start();
    }
}

