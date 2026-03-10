package com.example.collegeproject;
// 加在其他 import 下面即可
import android.os.SystemClock;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

/**
 * BleManager
 * ----------
 * 1. 封裝 BLE GATT 連線邏輯 (connect, discoverServices, notify, etc.)
 * 2. setCallback(...) 允許其它頁面動態切換 callback。
 */
public class BleManager {

    private static final String TAG = "BleManager";

    // ★ HM-10 常用 UUID
    private static final UUID UUID_HM10_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_HM10_CHAR    = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_CLIENT_CHAR_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BleManagerCallback callback;

    private BluetoothGatt   bluetoothGatt;
    private BluetoothDevice connectedDevice;
    private boolean         isConnected = false;

    public BleManager(BleManagerCallback callback) {
        this.callback = callback;
    }

    /* 允許動態切換 callback */
    public void setCallback(BleManagerCallback callback) {
        this.callback = callback;
    }

    /* 連線到指定裝置 */
    public void connectDevice(Context context, BluetoothDevice device) {
        if (device == null) {
            Log.w(TAG, "connectDevice: device == null");
            return;
        }
        Log.d(TAG, "connectDevice: " + device.getAddress());

        // 若已有連線，先斷開
        if (bluetoothGatt != null && isConnected) {
            disconnect();
        }

        // 發起 GATT 連線
        bluetoothGatt = device.connectGatt(context, /*autoConnect=*/false, gattCallback);
    }

    /* 斷線 */
    public void disconnect() {
        if (bluetoothGatt != null) {
            Log.d(TAG, "disconnect");
            bluetoothGatt.disconnect();
        }
    }

    /* 寫入字串資料 (HM-10) */
    public void writeData(String data) {
        if (!isConnected || bluetoothGatt == null) return;

        BluetoothGattService service = bluetoothGatt.getService(UUID_HM10_SERVICE);
        if (service == null) {
            Log.w(TAG, "writeData: HM-10 Service not found");
            return;
        }
        BluetoothGattCharacteristic ch = service.getCharacteristic(UUID_HM10_CHAR);
        if (ch == null) {
            Log.w(TAG, "writeData: HM-10 Characteristic not found");
            return;
        }

        ch.setValue(data.getBytes());
        boolean ok = bluetoothGatt.writeCharacteristic(ch);
        Log.d(TAG, "writeData: " + data + ", result=" + ok);
    }

    public boolean isConnected()               { return isConnected; }
    public BluetoothDevice getConnectedDevice(){ return connectedDevice; }

    /* ───────────────── GATT Callback ───────────────── */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "GATT 連線成功，開始 discoverServices");
                    bluetoothGatt   = gatt;
                    connectedDevice = gatt.getDevice();
                    isConnected     = true;

                    /* 提升為 HIGH priority，降低連線間隔 */
                    bluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);

                    if (callback != null && connectedDevice != null) {
                        callback.onConnected(connectedDevice);
                    }
                    bluetoothGatt.discoverServices();

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(TAG, "GATT 已斷線");
                    isConnected = false;
                    if (callback != null && connectedDevice != null) {
                        callback.onDisconnected(connectedDevice);
                    }
                    connectedDevice = null;
                    bluetoothGatt.close();
                    bluetoothGatt = null;
                }

            } else {   // 連線失敗
                Log.d(TAG, "GATT 連線失敗，status = " + status);
                isConnected = false;
                if (callback != null && connectedDevice != null) {
                    callback.onDisconnected(connectedDevice);
                }
                connectedDevice = null;
                gatt.close();
                bluetoothGatt = null;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServicesDiscovered: SUCCESS");
                BluetoothGattService srv = gatt.getService(UUID_HM10_SERVICE);
                if (srv != null) {
                    BluetoothGattCharacteristic ch = srv.getCharacteristic(UUID_HM10_CHAR);
                    if (ch != null) enableNotification(gatt, ch);
                }
            } else {
                Log.w(TAG, "onServicesDiscovered: FAIL status = " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic ch) {
            super.onCharacteristicChanged(gatt, ch);
            if (UUID_HM10_CHAR.equals(ch.getUuid())) {
                byte[] data = ch.getValue();
                Log.d(TAG, "onCharacteristicChanged: " + new String(data));
                if (callback != null && connectedDevice != null) {
                    callback.onDataReceived(connectedDevice, data);
                }
            }
        }
    };

    /** 開啟 notifications */
    private void enableNotification(BluetoothGatt gatt, BluetoothGattCharacteristic ch) {
        boolean ok = gatt.setCharacteristicNotification(ch, true);
        Log.d(TAG, "setCharacteristicNotification: " + ok);

        BluetoothGattDescriptor cccd = ch.getDescriptor(UUID_CLIENT_CHAR_CONFIG);
        if (cccd != null) {
            cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(cccd);
        }
    }

    /* 供外部（例如 Service）手動提升連線優先級 */
    public void setHighConnectionPriority() {
        if (bluetoothGatt != null) {
            bluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        }
    }

    /* 讓外部能直接取得 gatt（若需要更進階操作） */
    public BluetoothGatt getGatt() {
        return bluetoothGatt;
    }
}
