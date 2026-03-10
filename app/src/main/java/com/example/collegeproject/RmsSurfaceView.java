package com.example.collegeproject;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayDeque;

/** 超輕量 RMS 折線圖 (0–1) */
public class RmsSurfaceView extends SurfaceView
        implements SurfaceHolder.Callback {

    private static final int MAX_POINTS = 300;
    private final ArrayDeque<Float> points = new ArrayDeque<>();

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint();
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private volatile boolean surfaceReady = false;      // ★ 旗標

    public RmsSurfaceView(Context c, AttributeSet a) {
        super(c, a);
        getHolder().addCallback(this);

        linePaint.setColor(0xFF6200EE);
        linePaint.setStrokeWidth(4f);

        gridPaint.setColor(Color.GRAY);
        gridPaint.setStrokeWidth(2f);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{10,10}, 0));

        textPaint.setColor(0xFF444444);
        textPaint.setTextSize(28f);
    }

    /** 新增一筆資料（0–1） */
    public void addPoint(float v) {
        if (!surfaceReady) return;                     // ★ Surface 不在 → 直接丟棄
        if (points.size() >= MAX_POINTS) points.removeFirst();
        points.addLast(v);
        drawNow();
    }

    /* ============== 繪圖核心 ============== */
    private void drawNow() {
        SurfaceHolder holder = getHolder();
        if (!holder.getSurface().isValid()) return;    // ★ 雙保險

        Canvas c = null;
        try {
            c = holder.lockCanvas();
            if (c == null) return;

            int w = getWidth(), h = getHeight();
            float pad = h * 0.10f, usable = h - 2*pad;

            c.drawColor(Color.WHITE);                 // 1. 背景

            float y100 = pad, y60 = pad + usable*0.4f;
            c.drawLine(0, y100, w, y100, gridPaint);  // 2. 基準線
            c.drawLine(0, y60,  w, y60,  gridPaint);
            c.drawText("100 %", w-90, y100-8, textPaint);
            c.drawText("60 %",  w-90, y60 -8, textPaint);

            if (points.size() > 1) {                  // 3. 折線
                float dx = (float) w / (MAX_POINTS-1);
                float x  = w - dx*points.size();
                float prevY = pad + usable*(1 - points.peekFirst());

                for (float v : points) {
                    float y = pad + usable*(1 - v);
                    c.drawLine(x, prevY, x+dx, y, linePaint);
                    prevY = y; x += dx;
                }
            }

        } catch (Exception e) {                       // ★ 捕捉所有例外
            android.util.Log.w("RmsSurface", "draw error", e);
        } finally {
            if (c != null) holder.unlockCanvasAndPost(c);
        }
    }

    /* ===== SurfaceHolder.Callback ===== */
    @Override public void surfaceCreated (SurfaceHolder h){ surfaceReady = true;  }
    @Override public void surfaceDestroyed(SurfaceHolder h){ surfaceReady = false; }
    @Override public void surfaceChanged  (SurfaceHolder h,int f,int w,int h2){ }
}
