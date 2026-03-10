package com.example.collegeproject;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Point;
import android.media.Image;
import android.media.projection.MediaProjectionManager; // ★新增
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;                     // ★新增
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;

import java.text.SimpleDateFormat;                     // （舊 import 保留）
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import android.widget.Toast;


/**
 * CameraFirstActivity
 * -------------------
 * （原始註解保留）
 */
public class CameraFirstActivity extends AppCompatActivity implements ImageAnalysis.Analyzer {

    /* ---------- UI ---------- */
    private PreviewView previewView;
    private ImageButton flipCamera, btnEnd;
    private PoseOverlayView poseOverlayView;

    private TextView fpsTextView;
    private TextView tvSelectedPose, tvWeight, tvSets, tvReps, tvRestTime;
    private TextView tvCurrentSets, tvCurrentReps;

    private ProgressBar angleProgressBar;
    private TextView tvRestCountDown;

    /* <<< 新增：入場提示欄位 >>> */
    private TextView tvEntryHint;
    private CountDownTimer entryHintTimer;
    private static final int ENTRY_HINT_DURATION = 4000;
    private static final int ENTRY_HINT_INTERVAL = 1000;
    private boolean entryHintShown = false;
    /* -------------------------- */

    /* ---------- CameraX & ML Kit ---------- */
    private ImageAnalysis imageAnalysis;
    private PoseDetector poseDetector;
    private int cameraFacing = CameraSelector.LENS_FACING_BACK;

    /* ---------- 參數 ---------- */
    private double weight = 20.0;
    private int sets = 3;
    private int reps = 10;
    private int restTime = 60;

    private String selectedPose = "unknown";

    /* ---------- FPS ---------- */
    private int frameCount = 0;
    private long lastFpsTime = 0L;
    private int currentFps = 0;

    /* ---------- 動作計數 & 時間 ---------- */
    private MotionRecognizer motionRecognizer;

    private boolean hasStarted = false;
    private long totalStartTime = 0L;
    private long totalEndTime   = 0L;

    private boolean isResting   = false;
    private long restStartTime  = 0L;
    private long accumulatedRestTime = 0L;
    private CountDownTimer restCountDown;

    /* ---------- ★螢幕錄影相關 ---------- */
    private static final int REQUEST_SCREEN_CAPTURE = 1000;
    private MediaProjectionManager projectionManager;
    private boolean recordingEnabled = false;
    /* ------------------------------------- */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_first);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /* ---------- 綁定 UI ---------- */
        previewView      = findViewById(R.id.cameraPreview);
        flipCamera       = findViewById(R.id.flipCamera);
        btnEnd           = findViewById(R.id.btnEnd);
        poseOverlayView  = findViewById(R.id.poseOverlay);
        fpsTextView      = findViewById(R.id.fpsTextView);

        tvSelectedPose   = findViewById(R.id.tvSelectedPose);
        tvWeight         = findViewById(R.id.tvWeight);
        tvSets           = findViewById(R.id.tvSets);
        tvReps           = findViewById(R.id.tvReps);
        tvRestTime       = findViewById(R.id.tvRestTime);
        tvCurrentSets    = findViewById(R.id.tvCurrentSets);
        tvCurrentReps    = findViewById(R.id.tvCurrentReps);

        tvRestCountDown  = findViewById(R.id.tvRestCountDown);
        tvRestCountDown.setVisibility(View.GONE);

        angleProgressBar = findViewById(R.id.angleProgressBar);
        angleProgressBar.setMax(100);
        angleProgressBar.setProgress(0);

        /* <<< 新增：入場提示綁定並顯示 >>> */
        tvEntryHint = findViewById(R.id.tvEntryHint);
        showEntryHint();
        /* ----------------------------------- */

        /* ---------- 接收 Intent ---------- */
        Intent intent = getIntent();
        String poseExtra = intent.getStringExtra("selectedPose");
        if (poseExtra != null) selectedPose = poseExtra;
        recordingEnabled = intent.getBooleanExtra("recordingEnabled", false); // ★新增

        /* ---------- 讀取設定 ---------- */
        loadSettingsFromDB();

        tvSelectedPose.setText("動作: " + selectedPose);
        tvWeight.setText("重量: " + weight + " kg");
        tvSets.setText("組數: " + sets);
        tvReps.setText("次數: " + reps);
        tvRestTime.setText("休息時間: " + restTime + " 秒");
        tvCurrentSets.setText("目前組數(已完成): 0");
        tvCurrentReps.setText("本組次數: 0");

        motionRecognizer = new MotionRecognizer(selectedPose, sets, reps);

        AccuratePoseDetectorOptions options = new AccuratePoseDetectorOptions.Builder()
                .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE).build();
        poseDetector = PoseDetection.getClient(options);

        /* ---------- ★錄影權限請求 ---------- */
        projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (recordingEnabled) {
            Intent capIntent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(capIntent, REQUEST_SCREEN_CAPTURE);
        }
        /* ----------------------------------- */

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        } else startCamera();

        flipCamera.setOnClickListener(v -> {
            cameraFacing = (cameraFacing == CameraSelector.LENS_FACING_BACK)
                    ? CameraSelector.LENS_FACING_FRONT
                    : CameraSelector.LENS_FACING_BACK;
            startCamera();
        });

        btnEnd.setOnClickListener(v -> finishWorkout());

        lastFpsTime = System.currentTimeMillis();
    }

    /* <<< 新增：入場提示方法 >>> */
    private void showEntryHint() {
        if (entryHintShown) return;
        entryHintShown = true;
        runOnUiThread(() -> {
            tvEntryHint.setText("請面向鏡頭並保持右手入鏡");
            tvEntryHint.setVisibility(View.VISIBLE);
        });
        entryHintTimer = new CountDownTimer(ENTRY_HINT_DURATION, ENTRY_HINT_INTERVAL) {
            @Override public void onTick(long ms) {
                long sec = ms / 1000;
                runOnUiThread(() ->
                        tvEntryHint.setText("請面向鏡頭並保持右半身入鏡，誤遮擋偵測點\n開始於: " + sec + " 秒"));
            }
            @Override public void onFinish() {
                runOnUiThread(() -> tvEntryHint.setVisibility(View.GONE));
            }
        }.start();
    }
    /* ------------------------------ */

    private void loadSettingsFromDB() {
        DatabaseHelper db = new DatabaseHelper(this);
        Cursor c = db.getSettings();
        if (c != null && c.moveToFirst()) {
            weight   = c.getDouble(1);
            sets     = c.getInt(2);
            reps     = c.getInt(3);
            restTime = c.getInt(4);
        }
        if (c != null) c.close();
        Log.d("CameraFirst", "DB => weight=" + weight + ", sets=" +
                sets + ", reps=" + reps + ", restTime=" + restTime);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                provider.unbindAll();

                CameraSelector selector = new CameraSelector.Builder()
                        .requireLensFacing(cameraFacing).build();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(
                        ContextCompat.getMainExecutor(this), this);

                provider.bindToLifecycle(this, selector, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @androidx.camera.core.ExperimentalGetImage
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        if (isFinishing()) { imageProxy.close(); return; }

        frameCount++;
        long now = System.currentTimeMillis();
        if (now - lastFpsTime >= 1000) {
            currentFps = frameCount;
            frameCount = 0;
            lastFpsTime = now;
            runOnUiThread(() -> fpsTextView.setText("FPS: " + currentFps));
        }

        int w = imageProxy.getWidth(), h = imageProxy.getHeight();
        Image media = imageProxy.getImage();
        if (media == null) { imageProxy.close(); return; }

        InputImage img = InputImage.fromMediaImage(
                media, imageProxy.getImageInfo().getRotationDegrees());

        poseDetector.process(img)
                .addOnSuccessListener(p -> {
                    processPose(p, w, h); imageProxy.close();
                })
                .addOnFailureListener(e -> {
                    Log.e("CameraFirst", "Pose fail", e); imageProxy.close();
                });
    }

    private void processPose(Pose pose, int imgW, int imgH) {
        Display d = getWindowManager().getDefaultDisplay();
        Point size = new Point(); d.getRealSize(size);
        int screenW = size.x, screenH = size.y;
        boolean front = (cameraFacing == CameraSelector.LENS_FACING_FRONT);

        poseOverlayView.updatePose(pose, screenW, screenH, imgW, imgH, front);

        int oldRep = motionRecognizer.getCurrentRepCount();
        motionRecognizer.updatePose(pose);
        int curRep = motionRecognizer.getCurrentRepCount();
        int curSet = motionRecognizer.getCurrentSetCount();
        int angle = motionRecognizer.getAngleProgress();

        runOnUiThread(() -> {
            tvCurrentSets.setText("目前組數(已完成): " + curSet);
            tvCurrentReps.setText("本組次數: " + curRep);
            angleProgressBar.setProgress(angle);
        });

        if (!hasStarted && curRep > 0) {
            hasStarted = true; totalStartTime = System.currentTimeMillis();
        }

        if (isResting && curRep > 0) {
            cancelRestCountdown();
            long real = System.currentTimeMillis() - restStartTime;
            accumulatedRestTime += real; isResting = false;
        }

        if (oldRep == reps - 1 && curRep == 0) {
            if (curSet < sets) startRestCountdown();
            else finishWorkout();
        }
    }

    private void startRestCountdown() {
        isResting = true; restStartTime = System.currentTimeMillis();
        runOnUiThread(() -> {
            tvRestCountDown.setVisibility(View.VISIBLE);
            tvRestCountDown.setTextSize(64f);
            tvRestCountDown.setText(String.valueOf(restTime));
        });

        restCountDown = new CountDownTimer(restTime * 1000L, 1000) {
            @Override public void onTick(long ms) {
                long s = ms / 1000;
                runOnUiThread(() -> tvRestCountDown.setText(String.valueOf(s)));
            }
            @Override public void onFinish() {
                isResting = false;
                long real = System.currentTimeMillis() - restStartTime;
                accumulatedRestTime += real;
                runOnUiThread(() -> tvRestCountDown.setVisibility(View.GONE));
            }
        }.start();
    }

    private void cancelRestCountdown() {
        if (restCountDown != null) { restCountDown.cancel(); restCountDown = null; }
        runOnUiThread(() -> tvRestCountDown.setVisibility(View.GONE));
    }

    /* ---------- ★錄影授權回傳 ---------- */
    @Override
    protected void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQUEST_SCREEN_CAPTURE) {
            if (res == RESULT_OK && data != null) {
                Intent svc = new Intent(this, ScreenRecordService.class)
                        .setAction(ScreenRecordService.ACTION_START)
                        .putExtra("code", res)
                        .putExtra("data", data);
                ContextCompat.startForegroundService(this, svc);
                Toast.makeText(this, "螢幕錄影已開始", Toast.LENGTH_SHORT).show();
            } else {
                recordingEnabled = false;
                Toast.makeText(this, "未授權螢幕錄影", Toast.LENGTH_SHORT).show();
            }
        }
    }
    /* ----------------------------------- */

    private void finishWorkout() {
        if (hasStarted) totalEndTime = System.currentTimeMillis();

        /* ---------- ★停止錄影 ---------- */
        if (recordingEnabled) {
            startService(new Intent(this, ScreenRecordService.class)
                    .setAction(ScreenRecordService.ACTION_STOP));
            Toast.makeText(this, "螢幕錄影已儲存", Toast.LENGTH_SHORT).show();
        }
        /* -------------------------------- */

        DatabaseHelper db = new DatabaseHelper(this);
        int setDone = motionRecognizer.getCurrentSetCount();
        int restSec = (int)(accumulatedRestTime / 1000);
        int totalSec = hasStarted ? (int)((totalEndTime - totalStartTime) / 1000) : 0;

        db.insertRecord(selectedPose, weight, setDone, reps, restSec, totalSec);

        Intent i = new Intent(this, HistoryActivity.class);
        startActivity(i); finish();
    }

    /* <<< 修改：釋放入場提示計時器 >>> */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (entryHintTimer != null) { entryHintTimer.cancel(); entryHintTimer = null; }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}
