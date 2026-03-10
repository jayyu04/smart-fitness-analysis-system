package com.example.collegeproject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

/**
 * PoseOverlayView
 * ---------------
 * - 畫紅點＋綠線骨架
 * - 前鏡頭時左右鏡像
 * - 新增「肩→髖」(腋下到髖關節) 連線
 */
public class PoseOverlayView extends View {

    private Pose   pose;
    private float  scaleX, scaleY;
    private boolean isFrontCamera;
    private int    previewWidth, previewHeight;

    public PoseOverlayView(Context context) { super(context); }
    public PoseOverlayView(Context context, AttributeSet attrs) { super(context, attrs); }

    /** 更新 Pose ＆ 螢幕影像比例，觸發重繪 */
    public void updatePose(Pose pose,
                           int previewWidth,
                           int previewHeight,
                           int imageWidth,
                           int imageHeight,
                           boolean isFrontCamera) {

        this.pose          = pose;
        this.previewWidth  = previewWidth;
        this.previewHeight = previewHeight;
        this.isFrontCamera = isFrontCamera;

        // 相機座標 (imageWidth×imageHeight) → 螢幕座標 (previewWidth×previewHeight)
        scaleX = (float) previewWidth  / imageHeight;
        scaleY = (float) previewHeight / imageWidth;

        invalidate(); // 呼叫 onDraw()
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (pose == null) return;

        // ---- 畫點 ----
        Paint pointPaint = new Paint();
        pointPaint.setColor(Color.RED);
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setStrokeWidth(10f);

        for (PoseLandmark lm : pose.getAllPoseLandmarks()) {
            if (lm == null) continue;
            float x = lm.getPosition().x * scaleX;
            float y = lm.getPosition().y * scaleY;
            if (isFrontCamera) x = previewWidth - x;        // 鏡像
            canvas.drawCircle(x, y, 7, pointPaint);
        }

        // ---- 畫骨架線 ----
        Paint linePaint = new Paint();
        linePaint.setColor(Color.GREEN);
        linePaint.setStrokeWidth(5f);

        // 軀幹
        connect(canvas, linePaint, PoseLandmark.LEFT_SHOULDER,  PoseLandmark.RIGHT_SHOULDER);
        connect(canvas, linePaint, PoseLandmark.LEFT_HIP,       PoseLandmark.RIGHT_HIP);

        // ⭐ 新增左右「肩→髖」連線（腋下到髖）
        connect(canvas, linePaint, PoseLandmark.LEFT_SHOULDER,  PoseLandmark.LEFT_HIP);
        connect(canvas, linePaint, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP);

        // 左手
        connect(canvas, linePaint, PoseLandmark.LEFT_SHOULDER,  PoseLandmark.LEFT_ELBOW);
        connect(canvas, linePaint, PoseLandmark.LEFT_ELBOW,     PoseLandmark.LEFT_WRIST);

        // 右手
        connect(canvas, linePaint, PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW);
        connect(canvas, linePaint, PoseLandmark.RIGHT_ELBOW,    PoseLandmark.RIGHT_WRIST);

        // 左腳
        connect(canvas, linePaint, PoseLandmark.LEFT_HIP,       PoseLandmark.LEFT_KNEE);
        connect(canvas, linePaint, PoseLandmark.LEFT_KNEE,      PoseLandmark.LEFT_ANKLE);

        // 右腳
        connect(canvas, linePaint, PoseLandmark.RIGHT_HIP,      PoseLandmark.RIGHT_KNEE);
        connect(canvas, linePaint, PoseLandmark.RIGHT_KNEE,     PoseLandmark.RIGHT_ANKLE);
    }

    /** 連接兩 Landmark；若任一為 null 則略過 */
    private void connect(Canvas canvas, Paint paint, int startType, int endType) {
        PoseLandmark s = pose.getPoseLandmark(startType);
        PoseLandmark e = pose.getPoseLandmark(endType);
        if (s == null || e == null) return;

        float startX = s.getPosition().x * scaleX;
        float startY = s.getPosition().y * scaleY;
        float endX   = e.getPosition().x * scaleX;
        float endY   = e.getPosition().y * scaleY;

        if (isFrontCamera) {
            startX = previewWidth - startX;
            endX   = previewWidth - endX;
        }
        canvas.drawLine(startX, startY, endX, endY, paint);
    }
}
