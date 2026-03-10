package com.example.collegeproject;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

/**
 * FatigueService  (Android 11 版)
 * ------------------------------
 * • 前景常駐掃描單一 HM-10，連線後把資料交給 FatigueMonitor
 * • 權限不足時自動 stopSelf()
 * • 連線成功要求 HIGH Connection Priority
 */
public class FatigueService extends Service implements BleManagerCallback {

    /* ===== 常數 ===== */
    private static final String TAG        = "FatigueService";
    private static final String CH_ID      = "fatigue_channel";
    private static final int    NOTI_ID    = 101;
    private static final String TARGET_MAC = "5C:9F:A7:7A:10:B2";   // ← 換成你的 HM-10 MAC

    /* ===== BLE ===== */
    private BluetoothAdapter   btAdapter;
    private BluetoothLeScanner bleScanner;
    private BleManager         ble;

    /* ---------- 只接受目標 MAC ---------- */
    private final ScanCallback scanCb = new ScanCallback() {
        @Override public void onScanResult(int c, ScanResult r) {
            BluetoothDevice dev = r.getDevice();
            if (dev == null || !TARGET_MAC.equals(dev.getAddress())) return;

            stopBleScan();                                  // 停止掃描
            ble.connectDevice(getApplicationContext(), dev); // 連線
        }
    };

    /* ========== Service lifecycle ========== */
    @Override public void onCreate() {
        super.onCreate();

        /* 1. 權限檢查（只檢 ACCESS_FINE_LOCATION） */
        if (!hasBlePermission()) {
            Log.w(TAG, "缺少 BLE 權限，服務結束");
            stopSelf();
            return;
        }

        /* 2. 啟用 FatigueMonitor */
        FatigueMonitor.getInstance().init(this);
        FatigueMonitor.getInstance().setEnabled(true);

        /* 3. 建立 BleManager，開始掃描 */
        ble        = new BleManager(this);
        btAdapter  = BluetoothAdapter.getDefaultAdapter();
        bleScanner = (btAdapter != null) ? btAdapter.getBluetoothLeScanner() : null;

        startBleScan();

        /* 4. 前景通知 */
        createChannel();
        Notification n = new NotificationCompat.Builder(this, CH_ID)
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentTitle("Fatigue Monitor 正在運行")
                .setContentText("肌電監測已啟用")
                .setOngoing(true)
                .build();
        startForeground(NOTI_ID, n);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        stopBleScan();
        if (ble != null && ble.isConnected()) ble.disconnect();
        FatigueMonitor.getInstance().setEnabled(false);
    }

    /* ========== BleManagerCallback ========== */
    @Override public void onConnected(BluetoothDevice d) {
        Log.d(TAG, "已連線: " + d.getAddress());
        if (ble != null) ble.setHighConnectionPriority();
    }

    @Override public void onDisconnected(BluetoothDevice d) {
        Log.d(TAG, "斷線，重新掃描...");
        startBleScan();
    }

    @Override public void onDataReceived(BluetoothDevice d, byte[] data) {
        FatigueMonitor.getInstance().onDataReceived(d, data);
    }

    /* ========== 掃描控制 ========== */
    private void startBleScan() {
        if (bleScanner == null) return;
        try { bleScanner.startScan(scanCb); }
        catch (SecurityException ignore) {}
    }

    private void stopBleScan() {
        if (bleScanner == null) return;
        try { bleScanner.stopScan(scanCb); }
        catch (SecurityException ignore) {}
    }

    /* ========== 權限檢查 (Android 11 以下只需要 FINE_LOCATION) ========== */
    private boolean hasBlePermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /* ========== 通知渠道 ========== */
    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CH_ID, "Fatigue Monitor", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    /* ========== Binder ========== */
    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
