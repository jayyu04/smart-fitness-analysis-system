package com.example.collegeproject;

import android.os.SystemClock;     // ←★ 加這行
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/**
 * RmsChartActivity
 * ----------------
 * • 折線圖即時顯示 RMS 百分比
 * • 右上角文字以 10 fps 更新（減少 UI 負載）
 * • 連續 norm〈0.6 持續 5 次 → 力竭時顯示 Toast
 * • 透過 addListener/removeListener 與 FatigueMonitor 解耦
 */
public class RmsChartActivity extends AppCompatActivity
        implements FatigueMonitor.FatigueListener {

    private static final long UI_INTERVAL_MS = 100;   // 10 次/秒
   //舊 private long lastUiTs = 0L;
    private long lastUiTs = 0;

    private RmsSurfaceView rmsView;
    private TextView       tvRms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rms_surface);

        rmsView = findViewById(R.id.rmsSurface);
        tvRms   = findViewById(R.id.tvRms);

        Button btnBack = findViewById(R.id.btnBackRms);
        btnBack.setOnClickListener(v -> finish());
    }

    /* ====== 生命週期：可見時註冊，隱藏時解除 ====== */
    @Override protected void onStart() {
        super.onStart();
        FatigueMonitor.getInstance().addListener(this);
    }

    @Override protected void onStop() {
        super.onStop();
        FatigueMonitor.getInstance().removeListener(this);
    }

    /* ====== FatigueMonitor callbacks ====== */
    /*舊方法
    @Override public void onRmsUpdated(double rmsNorm) {
        rmsView.addPoint((float) rmsNorm);            // 折線即時點

        long now = System.currentTimeMillis();
        if (now - lastUiTs >= UI_INTERVAL_MS) {       // 節流更新文字
            lastUiTs = now;
            runOnUiThread(() -> tvRms.setText(
                    String.format(Locale.TAIWAN, "目前 RMS：%.0f %%", rmsNorm * 100)));
        }
    }
*/
    @Override public void onRmsUpdated(double rmsNorm) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastUiTs < UI_INTERVAL_MS) return;   // 節流
        lastUiTs = now;

        runOnUiThread(() -> {              // ★★ 全部搬進 UI 執行緒
            rmsView.addPoint((float) rmsNorm);
            tvRms.setText(String.format(Locale.TAIWAN,
                    "目前 RMS：%.0f %%", rmsNorm * 100));
        });
    }

    @Override public void onFatigue() {
        runOnUiThread(() ->
                Toast.makeText(this, "⚠️ 偵測到疲勞！", Toast.LENGTH_SHORT).show());
    }
}
