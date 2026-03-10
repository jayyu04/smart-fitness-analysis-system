package com.example.collegeproject;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

/**
 * DeviceTestActivity
 * ------------------
 * 1. 透過 BluetoothActivity.bleManager 取得同一個 BleManager
 * 2. onDataReceived(...) 內顯示終端機訊息、畫 EMG 曲線
 * 3. 同步轉發資料給 FatigueMonitor，避免 callback 被覆蓋
 * 4. onDestroy() 時把 callback 換回 FatigueMonitor
 * 5. 「返回」按鈕結束 Activity
 */
public class DeviceTestActivity extends AppCompatActivity implements BleManagerCallback {

    private static final String TAG = "DeviceTestActivity";

    private LineChart lineChart;
    private TextView  txtConsole;
    private ScrollView scrollView;
    private Button    btnBack;

    private LineDataSet dataSet;
    private LineData    lineData;
    private long startTime; // 起始時間，用於計算 X 軸秒數

    private BleManager bleManager; // 共享的 BleManager

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_test);

        // === 綁定 UI ===
        lineChart  = findViewById(R.id.lineChart);
        txtConsole = findViewById(R.id.txtConsole);
        scrollView = findViewById(R.id.scrollView);
        btnBack    = findViewById(R.id.btnBack);

        initChart();

        // 取得同一個 BleManager
        bleManager = BluetoothActivity.bleManager;
        if (bleManager == null) {
            Log.w(TAG, "bleManager == null，可能尚未建立連線");
        } else {
            /* ★ 將此頁設為 callback（會覆蓋前一個）★ */
            bleManager.setCallback(this);
        }

        startTime = System.currentTimeMillis();
        btnBack.setOnClickListener(v -> finish());
    }

    /* 離開頁面時，把 callback 換回 FatigueMonitor */
    @Override protected void onDestroy() {
        super.onDestroy();
        if (bleManager != null) {
            bleManager.setCallback(FatigueMonitor.getInstance()); // ★ 關鍵：讓 FatigueMonitor 重接管
        }
    }

    /* ---------- 圖表初始化 ---------- */
    private void initChart() {
        dataSet = new LineDataSet(new ArrayList<>(), "EMG 數值");
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);

        lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setDrawLabels(false);

        YAxis y = lineChart.getAxisLeft();
        y.setAxisMinimum(0f);
        y.setAxisMaximum(1500f);
        lineChart.getAxisRight().setEnabled(false);

        lineChart.invalidate();
    }

    private void appendConsole(String s) {
        txtConsole.append(s + "\n");
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void addEntry(int v) {
        float x = (System.currentTimeMillis() - startTime) / 1000f;
        dataSet.addEntry(new Entry(x, v));
        lineData.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.setVisibleXRangeMaximum(50);
        lineChart.moveViewToX(x);
    }

    /* ---------- BleManagerCallback ---------- */
    @Override public void onConnected   (BluetoothDevice d){ runOnUiThread(() -> appendConsole("✅ 已連線: " + d.getName())); }
    @Override public void onDisconnected(BluetoothDevice d){ runOnUiThread(() -> appendConsole("❌ 已斷線: " + d.getName())); }

    @Override
    public void onDataReceived(BluetoothDevice d, byte[] data) {
        String raw = new String(data).trim();
        Log.d(TAG, "onDataReceived: " + raw);

        /* ★ 轉發給 FatigueMonitor，確保其仍收到資料 ★ */
        FatigueMonitor.getInstance().onDataReceived(d, data);

        runOnUiThread(() -> {
            appendConsole("收到: " + raw);
            try {
                int emg = Integer.parseInt(raw);
                addEntry(emg);
            } catch (NumberFormatException e) {
                appendConsole("⚠ 不是數字: " + raw);
            }
        });
    }
}
