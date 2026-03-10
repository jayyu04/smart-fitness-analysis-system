package com.example.collegeproject;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ScreenRecordService extends Service {

    /* ------- 外部操作指令 ------- */
    public static final String ACTION_START = "START";
    public static final String ACTION_STOP  = "STOP";

    /* ------- 通知 ------- */
    private static final String NOTI_CHANNEL_ID = "rec_channel";
    private static final int    NOTI_ID         = 1;

    /* ------- 錄影元件 ------- */
    private MediaProjection  mediaProjection;
    private MediaRecorder    recorder;
    private android.view.Surface surface;
    private ParcelFileDescriptor pfd;
    private Uri videoUri;

    /* ------- 解析度 (16 的倍數) ------- */
    private static final int WIDTH  = 1088;   // 1080 ➜ 1088 (=16×68)
    private static final int HEIGHT = 1920;   // 1920 已是 16 的倍數
    private int density;

    /* -------------------------------------------------- */
    @Override public int onStartCommand(Intent i, int f, int id) {
        if (i == null) return START_NOT_STICKY;

        switch (i.getAction()) {
            case ACTION_START:
                startForeground(NOTI_ID, buildNotification());
                startProjection(i);
                break;
            case ACTION_STOP:
                stopRecording(); stopSelf(); break;
        }
        return START_NOT_STICKY;
    }

    /* ------------ 建立錄影 ------------ */
    private void startProjection(Intent dataIntent) {
        int code = dataIntent.getIntExtra("code", 0);
        Intent data = dataIntent.getParcelableExtra("data");

        MediaProjectionManager mpm =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mpm.getMediaProjection(code, data);

        density = getResources().getDisplayMetrics().densityDpi;

        /* MediaStore 建立影片 (pending) */
        ContentResolver resolver = getContentResolver();
        String fileName = "REC_" + new SimpleDateFormat(
                "yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".mp4";

        ContentValues v = new ContentValues();
        v.put(android.provider.MediaStore.Video.Media.DISPLAY_NAME, fileName);
        v.put(android.provider.MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        v.put(android.provider.MediaStore.Video.Media.RELATIVE_PATH,
                Environment.DIRECTORY_MOVIES + "/CollegeProject");
        v.put(android.provider.MediaStore.Video.Media.IS_PENDING, 1);

        videoUri = resolver.insert(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI, v);
        if (videoUri == null) { Log.e("ScreenSvc","insert uri null"); stopSelf(); return; }

        try { pfd = resolver.openFileDescriptor(videoUri, "w"); }
        catch (Exception e) { Log.e("ScreenSvc","openFD",e); stopSelf(); return; }

        /* MediaRecorder 設置 */
        recorder = new MediaRecorder();
        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setOutputFile(pfd.getFileDescriptor());
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        recorder.setVideoEncodingBitRate(5_000_000);
        recorder.setVideoFrameRate(30);
        recorder.setVideoSize(WIDTH, HEIGHT);

        try { recorder.prepare(); }
        catch (IOException e) { Log.e("ScreenSvc","prepare",e); stopSelf(); return; }

        surface = recorder.getSurface();

        mediaProjection.createVirtualDisplay(
                "ScreenRec", WIDTH, HEIGHT, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                surface, null, null);
        recorder.start();
    }

    /* ------------ 停止錄影、公開檔案 ------------ */
    private void stopRecording() {
        try { recorder.stop(); } catch (Exception ignore) {}
        if (recorder != null) recorder.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (pfd != null) try { pfd.close(); } catch (Exception ignore) {}

        if (videoUri != null) {
            ContentValues cv = new ContentValues();
            cv.put(android.provider.MediaStore.Video.Media.IS_PENDING, 0);
            getContentResolver().update(videoUri, cv, null, null);
        }
    }

    /* ------------ 前景通知 ------------ */
    private Notification buildNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                nm.getNotificationChannel(NOTI_CHANNEL_ID) == null) {
            nm.createNotificationChannel(new NotificationChannel(
                    NOTI_CHANNEL_ID, "ScreenRecording",
                    NotificationManager.IMPORTANCE_LOW));
        }

        return new NotificationCompat.Builder(this, NOTI_CHANNEL_ID)
                .setContentTitle("螢幕錄影進行中")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build();
    }

    /* -------------------------------------------------- */
    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
