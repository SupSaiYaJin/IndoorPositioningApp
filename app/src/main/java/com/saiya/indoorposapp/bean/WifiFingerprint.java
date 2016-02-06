package com.saiya.indoorposapp.bean;

/**
 * 封装WiFi指纹信息的类,包括MAC地址与RSSI
 */
public class WifiFingerprint {
    private final String mac;
    private final float rssi;
    public WifiFingerprint(String mac, float rssi) {
        this.mac = mac;
        this.rssi = rssi;
    }

    public String getMac() {
        return mac;
    }

    public float getRssi() {
        return rssi;
    }
}
