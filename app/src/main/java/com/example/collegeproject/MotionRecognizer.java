package com.example.collegeproject;

import android.util.Log;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

/**
 * MotionRecognizer
 * ----------------
 * - curl / raise / press 計數
 * - angleProgress (0~100) 表示動作進度
 * - raise：肘→肩→髖
 * - press：肩→肘→腕（肘角）
 */
public class MotionRecognizer {

    // ==== 參數 ====
    private final String selectedPose;
    private final int    sets;
    private final int    reps;

    // ==== 內部狀態 ====
    private int     currentRepCount = 0;
    private int     currentSetCount = 0;
    private boolean isUp            = false;
    private int     angleProgress   = 0;   // 0-100

    public MotionRecognizer(String selectedPose, int sets, int reps) {
        this.selectedPose = selectedPose;
        this.sets         = sets;
        this.reps         = reps;
    }

    /* 每偵由 CameraFirstActivity 呼叫 */
    public void updatePose(Pose pose) {

        PoseLandmark rShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER);
        PoseLandmark rElbow    = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW);
        PoseLandmark rWrist    = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST);
        PoseLandmark rHip      = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP);

        switch (selectedPose) {

            /* ────── 1. 二頭彎舉 (curl) ────── */
            case "curl":
                if (rShoulder != null && rElbow != null && rWrist != null) {
                    double angle = getAngle(rShoulder, rElbow, rWrist); // 肘角
                    handleBicepCurl(angle);
                }
                break;

            /* ────── 2. 側平舉 (raise) ──────
             *       肘→肩→髖，量肩外展角
             */
            case "raise":
                if (rElbow != null && rShoulder != null && rHip != null) {
                    double angle = getAngle(rElbow, rShoulder, rHip);
                    handleSideRaise(angle);
                }
                break;

            /* ────── 3. 肩推 (press) ──────
             *       肩→肘→腕，量肘伸展角
             */
            case "press":
                if (rShoulder != null && rElbow != null && rWrist != null) {
                    double angle = getAngle(rShoulder, rElbow, rWrist);
                    handleShoulderPress(angle);
                }
                break;

            default:
                angleProgress = 0;
        }
    }

    // ─────────── Getter ───────────
    public int  getCurrentSetCount() { return currentSetCount; }
    public int  getCurrentRepCount() { return currentRepCount; }
    public boolean isAllSetsDone()   { return currentSetCount >= sets; }
    public int  getAngleProgress()   { return angleProgress; }

    // ─────────── 通用工具 ───────────
    /** 回傳 first-mid-last 夾角 (0-180°) */
    private double getAngle(PoseLandmark first, PoseLandmark mid, PoseLandmark last) {
        double x1 = first.getPosition().x - mid.getPosition().x;
        double y1 = first.getPosition().y - mid.getPosition().y;
        double x2 = last.getPosition().x  - mid.getPosition().x;
        double y2 = last.getPosition().y  - mid.getPosition().y;

        double angle = Math.toDegrees(Math.atan2(y2, x2) - Math.atan2(y1, x1));
        angle = Math.abs(angle);
        return (angle > 180) ? 360.0 - angle : angle;
    }

    /** 將角度映射至 0-100 進度條 */
    private int calcAngleProgress(double angle, double minAngle, double maxAngle) {
        double ratio = (angle - minAngle) / (maxAngle - minAngle);
        ratio = Math.max(0, Math.min(1, ratio));
        return (int) (ratio * 100);
    }

    // ─────────── 動作邏輯 ───────────
    /* 1️⃣ curl：肘角 <50° 下 / >140° 上 */
    private void handleBicepCurl(double angle) {
        angleProgress = calcAngleProgress(angle, 50, 140);
        countLogic(angle, 50, 140, "curl");
    }

    /* 2️⃣ raise：肩外展角 <30° 下 / >90° 上 */
    private void handleSideRaise(double angle) {
        angleProgress = calcAngleProgress(angle, 30, 90);
        countLogic(angle, 30, 90, "raise");
    }

    /* 3️⃣ press：肘角 <100° 下 / >160° 上 */
    private void handleShoulderPress(double angle) {
        angleProgress = calcAngleProgress(angle, 100, 160);
        countLogic(angle, 100, 160, "press");
    }

    /** 共用計數邏輯 (下→上→下 算 1 次) */
    private void countLogic(double angle, double downTh, double upTh, String tag) {
        if (!isUp && angle > upTh) {          // 下 → 上
            isUp = true;
        } else if (isUp && angle < downTh) {  // 上 → 下
            isUp = false;
            currentRepCount++;
            Log.d("MotionRecognizer", tag + " rep=" + currentRepCount);

            if (currentRepCount >= reps) {
                currentSetCount++;
                currentRepCount = 0;
                Log.d("MotionRecognizer", tag + " set completed => set=" + currentSetCount);

                if (currentSetCount >= sets) {
                    Log.d("MotionRecognizer", "all sets done (" + tag + ")!");
                }
            }
        }
    }
}
