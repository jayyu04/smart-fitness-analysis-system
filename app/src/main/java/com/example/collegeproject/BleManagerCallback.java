package com.example.collegeproject;

import android.bluetooth.BluetoothDevice;

/**
 * BleManagerCallback
 * ------------------
 * 讓 BleManager 能夠回呼「連線/斷線/接收資料」等事件給上層(比如 Activity)。
 */
public interface BleManagerCallback {

    /**
     * 當 BLE 裝置成功連線時
     * @param device 已連線裝置
     */
    void onConnected(BluetoothDevice device);

    /**
     * 當 BLE 裝置斷線時
     * @param device 斷線裝置
     */
    void onDisconnected(BluetoothDevice device);

    /**
     * 當從 BLE 裝置收到資料 (notify/indicate) 時
     * @param device 來自哪個裝置
     * @param data   接收的原始byte陣列
     */
    void onDataReceived(BluetoothDevice device, byte[] data);
}
