package com.example.collegeproject;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class BluetoothActivity extends AppCompatActivity implements BleManagerCallback {

    private static final String TAG = "BluetoothActivity";
    private static final String TARGET_MAC = "F0:B5:D1:AB:0E:73";   // ★ 你的 HM-10 MAC
    private static final int REQUEST_ENABLE_BT        = 1;
    private static final int REQUEST_BLE_PERMISSIONS  = 2;

    public static BleManager bleManager;

    private BluetoothAdapter      bluetoothAdapter;
    private BluetoothLeScanner    bleScanner;

    private Button   btnScan, btnBack;
    private ListView lvDevices;

    private final ArrayList<BluetoothDevice> bleDeviceList      = new ArrayList<>();
    private final ArrayList<String>          bleDeviceNameList  = new ArrayList<>();
    private ArrayAdapter<String>             adapter;

    /* -------------------------------------------------- */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        btnScan    = findViewById(R.id.btnScan);
        btnBack    = findViewById(R.id.btnBack);
        lvDevices  = findViewById(R.id.lvDevices);

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                bleDeviceNameList);
        lvDevices.setAdapter(adapter);

        BluetoothManager manager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) bluetoothAdapter = manager.getAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "此裝置不支援藍牙", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        /* 建立或重用 BleManager */
        if (bleManager == null) bleManager = new BleManager(this);
        else                     bleManager.setCallback(this);

        /* Button */
        btnScan.setOnClickListener(v -> startScanBle());
        btnBack.setOnClickListener(v -> finish());

        /* List item click */
        lvDevices.setOnItemClickListener((parent, view, position, id) -> {
            stopScanBle();   // ★ 連線前一定先停掃描

            BluetoothDevice device = bleDeviceList.get(position);

            if (bleManager.isConnected()) {
                if (bleManager.getConnectedDevice() != null
                        && bleManager.getConnectedDevice().equals(device)) {
                    Toast.makeText(this, "斷線中...", Toast.LENGTH_SHORT).show();
                    bleManager.disconnect();
                } else {
                    bleManager.disconnect();
                    Toast.makeText(this, "連線到新裝置...", Toast.LENGTH_SHORT).show();
                    bleManager.connectDevice(this, device);
                }
            } else {
                bleManager.connectDevice(this, device);
            }
        });
    }

    /* -------------------------------------------------- */
    private void startScanBle() {
        if (!checkPermissions()) return;

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }

        stopScanBle();                   // ★ 先關掉任何舊掃描

        bleDeviceList.clear();
        bleDeviceNameList.clear();

        /* 已連線裝置 → 顯示在最上方 */
        if (bleManager.isConnected()) {
            BluetoothDevice dev = bleManager.getConnectedDevice();
            if (dev != null) {
                String name = (dev.getName() == null) ? "未知裝置" : dev.getName();
                bleDeviceList.add(0, dev);
                bleDeviceNameList.add(0, "[已連線] " + name + " | " + dev.getAddress());
            }
        }
        adapter.notifyDataSetChanged();

        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) {
            Toast.makeText(this, "BLE不支援或錯誤", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "開始掃描...", Toast.LENGTH_SHORT).show();
        bleScanner.startScan(scanCallback);
    }

    /* ★ 集中管理掃描停止 */
    private void stopScanBle() {
        if (bleScanner != null) {
            bleScanner.stopScan(scanCallback);
        }
    }

    /* -------------------------------------------------- */
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device == null) return;

            /* 只保留目標 MAC */
            if (!TARGET_MAC.equals(device.getAddress())) return;

            if (!bleDeviceList.contains(device)) {
                String name = (device.getName() == null) ? "未知裝置" : device.getName();
                bleDeviceList.add(device);
                bleDeviceNameList.add(name + " | " + device.getAddress());
                runOnUiThread(() -> adapter.notifyDataSetChanged());
            }
        }
    };

    /* -------------------------------------------------- */
    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                    != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_BLE_PERMISSIONS);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_BLE_PERMISSIONS);
                return false;
            }
        }
        return true;
    }

    /* -------------------------------------------------- */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLE_PERMISSIONS) startScanBle();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) startScanBle();
    }

    /* ★ 生命週期：離開畫面時停止掃描 */
    @Override
    protected void onPause() {
        super.onPause();
        stopScanBle();
    }

    /* -------------------------------------------------- */
    /* BleManagerCallback 實作 */

    @Override
    public void onConnected(BluetoothDevice device) {
        runOnUiThread(() -> {
            Toast.makeText(this,
                    "已連線: " + (device.getName() == null ? "未知裝置" : device.getName()),
                    Toast.LENGTH_SHORT).show();

            bleDeviceList.remove(device);
            for (int i = 0; i < bleDeviceNameList.size(); i++) {
                if (bleDeviceNameList.get(i).contains(device.getAddress())) {
                    bleDeviceNameList.remove(i);
                    break;
                }
            }

            String name = (device.getName() == null) ? "未知裝置" : device.getName();
            bleDeviceList.add(0, device);
            bleDeviceNameList.add(0, "[已連線] " + name + " | " + device.getAddress());
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onDisconnected(BluetoothDevice device) {
        runOnUiThread(() -> {
            Toast.makeText(this,
                    "斷線: " + (device.getName() == null ? "未知裝置" : device.getName()),
                    Toast.LENGTH_SHORT).show();

            bleDeviceList.remove(device);
            for (int i = 0; i < bleDeviceNameList.size(); i++) {
                if (bleDeviceNameList.get(i).contains(device.getAddress())) {
                    bleDeviceNameList.remove(i);
                    break;
                }
            }
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onDataReceived(BluetoothDevice device, byte[] data) {
        Log.d(TAG, "onDataReceived: " + new String(data));
        /* 交給 FatigueMonitor 處理即可 */
    }
}
