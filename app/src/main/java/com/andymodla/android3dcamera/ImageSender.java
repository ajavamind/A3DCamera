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
    public ImageSender(Context context) {
    //nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void sendImageUrl(String imageUrl) {
        this.targetImageUrl = imageUrl;
        String ip = "10.0.0.50";   // 3D tablet destination
        int port = 9000;
        try {
            sendUrlToReceiver(ip, port, targetImageUrl);
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
        // Construct the URL for our NanoHTTPD server
        String fullUrl = "http://" + ip + ":" + port + "?imageUrl=" + urlToSend;
        // Creates a new client sharing the same connection pool but with custom timeouts
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
}
