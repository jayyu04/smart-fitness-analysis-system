package com.example.collegeproject;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.media.SoundPool;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * FatigueMonitor 2.4
 * ------------------
 * • 採 50 Hz 取樣，RMS 平滑窗 200 ms，MVC 標定 8 s
 * • 疲勞判定改「連續 FATIGUE_DETECT_MS 時間低於 0.6」，
 *   與取樣率解耦，穩定度更高
 * • 冷卻時間、嗶聲、DB 寫入均沿用原邏輯
 */
public class FatigueMonitor implements BleManagerCallback {

    private static final String TAG = "FatigueMonitor";

    /* ===== 參數 ===== */
    private static final int    SAMPLE_RATE_HZ    = 50;
    private static final int    DC_WINDOW_MS      = 1000;
    private static final int    RMS_WINDOW_MS     = 200;     // 200 ms ≈ 10 筆
    private static final int    MVC_COLLECT_MS    = 5000;    // 校準5 秒
    private static final double START_THRESHOLD   = 0.8;
    private static final double FATIGUE_THRESHOLD = 0.6;
    private static final long   FATIGUE_DETECT_MS = 600;     // 0.6 s
    private static final long   COOLDOWN_MS       = 10_000;

    /* ===== 緩衝長度 ===== */
    private static final int DC_BUF_LEN  = DC_WINDOW_MS  * SAMPLE_RATE_HZ / 1000;
    private static final int RMS_BUF_LEN = RMS_WINDOW_MS * SAMPLE_RATE_HZ / 1000;
    private static final int MVC_MAX_LEN = MVC_COLLECT_MS * SAMPLE_RATE_HZ / 1000;

    /* ===== Singleton ===== */
    private static FatigueMonitor instance;
    public static synchronized FatigueMonitor getInstance() {
        if (instance == null) instance = new FatigueMonitor();
        return instance;
    }
    private FatigueMonitor() {}

    /* ===== Listener ===== */
    public interface FatigueListener {
        void onRmsUpdated(double norm);
        void onFatigue();
    }
    private final CopyOnWriteArrayList<FatigueListener> listeners = new CopyOnWriteArrayList<>();
    public void addListener   (FatigueListener l){ if(!listeners.contains(l)) listeners.add(l); }
    public void removeListener(FatigueListener l){ listeners.remove(l); }

    /* ===== App、DB、Sound ===== */
    private Context appCtx;
    private DatabaseHelper db;
    private SoundPool sp;
    private int beepId;

    /* DB 寫入執行緒 */
    private final HandlerThread ioThread = new HandlerThread("fatigue-io");
    private Handler ioHandler;

    public void init(Context c) {
        if (appCtx != null) return;                   // 只需 init 一次
        appCtx = c.getApplicationContext();
        db     = new DatabaseHelper(appCtx);

        ioThread.start();
        ioHandler = new Handler(ioThread.getLooper());

        sp = new SoundPool.Builder().setMaxStreams(1).build();
        beepId = sp.load(appCtx, R.raw.beep, 1);
    }

    /* ===== 狀態 ===== */
    private volatile boolean enabled = false;
    private boolean isContracting = false;

    /* DC-offset */
    private final int[] dcBuf = new int[DC_BUF_LEN];
    private int  dcIdx = 0;
    private long dcSum = 0;
    private long totalSamples = 0;

    /* RMS */
    private final long[] rmsBuf = new long[RMS_BUF_LEN];
    private int  rmsIdx = 0;
    private long rmsSumSq = 0;
    private int  rmsCount = 0;

    /* MVC */
    private double mvcBaseline = Double.NaN;
    private double mvcPeakRms  = 0;
    private int    mvcCounter  = 0;

    /* 疲勞 */
    private long belowTs     = 0;   // <0.6 區間起點
    private long lastFatigue = 0;
    private long startTsMs   = 0;

    /* ===== 公開 API ===== */
    public void setEnabled(boolean on) {
        if (on && !enabled) {          // 第一次開啟
            resetBuffers();
            startTsMs = System.currentTimeMillis();
        } else if (!on && enabled) {   // 關閉
            resetBuffers();
        }
        enabled = on;
        Log.d(TAG, on ? "Monitor ENABLED" : "Monitor DISABLED");
    }
    public boolean isEnabled() { return enabled; }

    /** 若想手動重新校準 MVC，可呼叫 */
    public void forceRecalibrate() { resetBuffers(); }

    public double getMvcBaseline(){ return mvcBaseline; }

    /* ===== 核心回調 ===== */
    @Override public void onConnected   (BluetoothDevice d){ /* no-op */ }
    @Override public void onDisconnected(BluetoothDevice d){ /* no-op */ }

    @Override
    public void onDataReceived(BluetoothDevice device, byte[] data) {
        long now = SystemClock.elapsedRealtime();          // 取當前時間
        if (!enabled) return;

        /* ---------- 0. 解析 ADC 值 ---------- */
        int raw;
        try { raw = Integer.parseInt(new String(data).trim()); }
        catch (NumberFormatException e) { return; }

        /* ---------- 1. DC-offset ---------- */
        dcSum        -= dcBuf[dcIdx];
        dcBuf[dcIdx] = raw;
        dcSum        += raw;
        dcIdx        = (dcIdx + 1) % DC_BUF_LEN;
        totalSamples++;
        int filled   = (int) Math.min(totalSamples, DC_BUF_LEN);
        int centered = raw - (int) (dcSum / filled);

        /* ---------- 2. RMS ---------- */
        long sq = 1L * centered * centered;
        if (rmsCount == RMS_BUF_LEN) rmsSumSq -= rmsBuf[rmsIdx];
        else                         rmsCount++;
        rmsBuf[rmsIdx] = sq;
        rmsSumSq      += sq;
        rmsIdx         = (rmsIdx + 1) % RMS_BUF_LEN;
        double rms     = Math.sqrt((double) rmsSumSq / rmsCount);

        /* ---------- 3. MVC 標定 ---------- */
        if (Double.isNaN(mvcBaseline)) {
            if (rms > mvcPeakRms) mvcPeakRms = rms;
            if (++mvcCounter >= MVC_MAX_LEN) {
                mvcBaseline = mvcPeakRms;
                Log.d(TAG, "MVC baseline = " + mvcBaseline);
            }
            return;
        }

        /* ---------- 4. Norm ---------- */
        double norm = rms / mvcBaseline;

        /* ---------- 5. 收縮啟動 ---------- */
        if (!isContracting && norm >= START_THRESHOLD) {
            isContracting = true;
            belowTs = 0;
        }

        /* ---------- 6. 疲勞判定（時間制） ---------- */
        if (isContracting) {
            if (norm < FATIGUE_THRESHOLD) {
                if (belowTs == 0) belowTs = now;               // 開始計時
                if (now - belowTs >= FATIGUE_DETECT_MS &&
                        now - lastFatigue >= COOLDOWN_MS) {
                    triggerFatigue(now);
                    isContracting = false;
                    belowTs = 0;
                }
            } else {
                belowTs = 0;  // 強度回升，計時重置
            }
        }

        /* ---------- 7. 通知 UI ---------- */
        for (FatigueListener l : listeners) l.onRmsUpdated(norm);
    }

    /* ===== 私有工具 ===== */
    private void resetBuffers() {
        dcIdx = 0; dcSum = 0; totalSamples = 0;
        rmsIdx = 0; rmsSumSq = 0; rmsCount = 0;
        mvcBaseline = Double.NaN; mvcPeakRms = 0; mvcCounter = 0;
        isContracting = false;
        belowTs = 0;
    }

    /** 封裝疲勞觸發（更新 lastFatigue + 嗶聲 + DB） */
    private void triggerFatigue(long now){
        lastFatigue = now;
        onFatigueDetected();
    }

    private void onFatigueDetected() {
        sp.play(beepId, 1, 1, 1, 0, 1);   // 嗶聲

        long now = System.currentTimeMillis();
        int durS = (int) ((now - startTsMs) / 1000);

        ioHandler.post(() -> {            // 非同步寫 DB
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            db.insertFatigueEvent(fmt.format(new Date(startTsMs)),
                    fmt.format(new Date(now)), durS);
        });

        for (FatigueListener l : listeners) l.onFatigue();
        startTsMs = now;
    }
}
