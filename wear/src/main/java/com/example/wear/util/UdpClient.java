package com.example.wear.util;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UdpClient {

    private static final String TAG = "UdpClient";
    
    // Default UDP properties. It's recommended to broadcast if the target IP is unknown, 
    // or point to the specific PC running the Python script.
    // Replace this IP with the actual IP address of the Python dashboard camera PC!
    public static String TARGET_IP = "10.180.43.2"; 
    public static final int TARGET_PORT = 5005;

    // Use a single thread executor so network operations are queued and don't block
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void sendFallSignal(long eventTimestampMs) {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                DatagramSocket socket = null;
                try {
                    socket = new DatagramSocket();
                    InetAddress address = InetAddress.getByName(TARGET_IP);

                    JSONObject payload = new JSONObject();
                    payload.put("event", "FALL_DETECTED");
                    payload.put("event_timestamp", eventTimestampMs);
                    payload.put("send_timestamp", System.currentTimeMillis());

                    byte[] buf = payload.toString().getBytes();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, address, TARGET_PORT);
                    
                    socket.send(packet);
                    Log.d(TAG, "Sent UDP FALL_DETECTED packet to " + TARGET_IP + ":" + TARGET_PORT);
                    
                } catch (IOException | JSONException e) {
                    Log.e(TAG, "Error sending UDP packet", e);
                } finally {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                }
            }
        });
    }
}
