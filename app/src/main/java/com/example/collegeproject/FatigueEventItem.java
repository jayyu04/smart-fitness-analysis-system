package com.example.collegeproject;

/**
 * FatigueEventItem
 * ----------------
 * 封裝 fatigue_events 表的一筆資料。
 */
public class FatigueEventItem {

    private final int    id;
    private final String startTime;     // yyyy-MM-dd HH:mm:ss
    private final String fatigueTime;   // 力竭發生時間 (同上格式)
    private final int    durationSec;   // 持續秒數

    public FatigueEventItem(int id,
                            String startTime,
                            String fatigueTime,
                            int durationSec) {
        this.id          = id;
        this.startTime   = startTime;
        this.fatigueTime = fatigueTime;
        this.durationSec = durationSec;
    }

    /* ---------- Getter ---------- */
    public int    getId()          { return id; }
    public String getStartTime()   { return startTime; }
    public String getFatigueTime() { return fatigueTime; }
    public int    getDurationSec() { return durationSec; }

    /** 方便其它程式呼叫：與 getDurationSec() 同義 */
    public int getDuration() {      // ★ 新增：供 AddPendingDialog 使用
        return durationSec;
    }

    @Override public String toString() {
        return "開始：" + startTime +
                "\n力竭：" + fatigueTime +
                "\n持續：" + durationSec + " 秒";
    }
}
