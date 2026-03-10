package com.example.collegeproject;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * FatigueSettingActivity
 * ----------------------
 * • 使用者在此頁手動「開始 / 關閉」力竭偵測
 *   └ 開啟流程：權限檢查 → 6 s 校準倒數 → startForegroundService(FatigueService)
 *   └ 關閉流程：stopService(FatigueService)
 * • 其餘功能：藍牙設定、裝置測試、事件列表、RMS 圖表
 */
public class FatigueSettingActivity extends AppCompatActivity {

    /* ===== 常量 ===== */
    private static final int  REQ_FINE_LOC = 880;
    private static final int  CALI_SECONDS = 6;
    private static final long INTERVAL_MS  = 1_000;

    /* ===== UI ===== */
    private Button  btnToggle;
    private TextView tvHint;
    private CountDownTimer timer;

    /* ===== 生命週期 ===== */
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fatigue_setting);

        /* FatigueMonitor 只需 init 一次，確保 DB / SoundPool 就緒 */
        FatigueMonitor.getInstance().init(getApplicationContext());

        /* 綁定按鈕 */
        btnToggle       = findViewById(R.id.btnToggleFatigue);
        tvHint          = findViewById(R.id.tvCalibrateHint);
        tvHint.setVisibility(TextView.GONE);

        findViewById(R.id.btnBluetooth )
                .setOnClickListener(v -> startActivity(new Intent(this, BluetoothActivity.class)));
        findViewById(R.id.btnDeviceTest)
                .setOnClickListener(v -> startActivity(new Intent(this, DeviceTestActivity.class)));
        findViewById(R.id.btnViewFatigueEvents)
                .setOnClickListener(v -> startActivity(new Intent(this, FatigueEventActivity.class)));
        findViewById(R.id.btnViewRmsChart)
                .setOnClickListener(v -> startActivity(new Intent(this, RmsChartActivity.class)));
        findViewById(R.id.btnBack)
                .setOnClickListener(v -> finish());

        /* 切換監測 */
        updateBtnText(isServiceRunning());
        btnToggle.setOnClickListener(v -> onToggleClicked());
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (timer != null) timer.cancel();
    }

    /* ====== 切換邏輯 ====== */
    private void onToggleClicked() {

        if (isServiceRunning()) {                         // —— 關閉 —— //
            if (timer != null) { timer.cancel(); timer = null; }
            stopService(new Intent(this, FatigueService.class));
            tvHint.setVisibility(TextView.GONE);
            Toast.makeText(this,"疲勞偵測已關閉",Toast.LENGTH_SHORT).show();
            updateBtnText(false);
            return;
        }

        /* —— 尚未運行 → 權限檢查 —— */
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                    REQ_FINE_LOC);
            return;
        }
        /* 權限 OK → 進入倒數 */
        startCalibrationCountDown();
    }

    /* ====== 倒數校準 & 啟動服務 ====== */
    private void startCalibrationCountDown() {
        tvHint.setVisibility(TextView.VISIBLE);
        timer = new CountDownTimer(CALI_SECONDS * 1000L, INTERVAL_MS) {
            @Override public void onTick(long ms) {
                tvHint.setText("請盡全力等長收縮\n" + (ms / 1000) + " 秒");
            }
            @Override public void onFinish() {
                tvHint.setVisibility(TextView.GONE);
                ContextCompat.startForegroundService(
                        FatigueSettingActivity.this,
                        new Intent(FatigueSettingActivity.this, FatigueService.class));
                Toast.makeText(FatigueSettingActivity.this,
                        "疲勞偵測已啟動",Toast.LENGTH_SHORT).show();
                updateBtnText(true);
            }
        }.start();
    }

    /* ====== 權限回覆 ====== */
    @Override public void onRequestPermissionsResult(
            int req, @NonNull String[] perms, @NonNull int[] results) {

        super.onRequestPermissionsResult(req, perms, results);
        if (req == REQ_FINE_LOC &&
                results.length > 0 &&
                results[0] == PackageManager.PERMISSION_GRANTED) {

            startCalibrationCountDown();
        } else {
            Toast.makeText(this,
                    "未授權定位，無法啟動監測",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /* ====== 工具函式 ====== */
    private boolean isServiceRunning() {
        android.app.ActivityManager am =
                (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningServiceInfo s
                : am.getRunningServices(Integer.MAX_VALUE)) {
            if (FatigueService.class.getName().equals(s.service.getClassName()))
                return true;
        }
        return false;
    }
    private void updateBtnText(boolean on) {
        btnToggle.setText(on ? "關閉疲勞偵測" : "開始疲勞偵測");
    }
}
