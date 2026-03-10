package com.example.collegeproject;

/**
 * CloudPendingItem
 * ----------------
 * 對應 cloud_pending 表的一筆待上傳紀錄。
 * 共 16 欄位；fatigueId / fatigueSec 可能為 -1 表示「無」。
 */
public class CloudPendingItem {

    /* === 基本欄位 (與資料表一一對應) === */
    private final int    id;                 // cid
    private final int    participantId;      // participants.pid
    private final String participantName;
    private final double heightCm;
    private final double weightKg;
    private final int    age;
    private final String gender;

    private final int    exerciseId;         // exercise_records.id
    private final String exerciseName;
    private final double exerciseWeight;
    private final int    sets;
    private final int    reps;
    private final int    totalSeconds;

    private final int    fatigueId;          // -1 代表無
    private final int    fatigueSeconds;     // -1 代表無

    private final String exerciseDate;       // yyyy-MM-dd
    private final int    uploaded;           // 0/1

    /* === 建構子 === */
    public CloudPendingItem(int id,
                            int participantId, String participantName,
                            double heightCm, double weightKg, int age, String gender,
                            int exerciseId, String exerciseName, double exerciseWeight,
                            int sets, int reps, int totalSeconds,
                            int fatigueId, int fatigueSeconds,
                            String exerciseDate, int uploaded) {

        this.id = id;
        this.participantId   = participantId;
        this.participantName = participantName;
        this.heightCm        = heightCm;
        this.weightKg        = weightKg;
        this.age             = age;
        this.gender          = gender;

        this.exerciseId      = exerciseId;
        this.exerciseName    = exerciseName;
        this.exerciseWeight  = exerciseWeight;
        this.sets            = sets;
        this.reps            = reps;
        this.totalSeconds    = totalSeconds;

        this.fatigueId       = fatigueId;
        this.fatigueSeconds  = fatigueSeconds;

        this.exerciseDate    = exerciseDate;
        this.uploaded        = uploaded;
    }

    /* === Getter === */
    public int    getId()             { return id; }
    public int    getParticipantId()  { return participantId; }
    public String getParticipantName(){ return participantName; }
    public double getHeightCm()       { return heightCm; }
    public double getWeightKg()       { return weightKg; }
    public int    getAge()            { return age; }
    public String getGender()         { return gender; }

    public int    getExerciseId()     { return exerciseId; }
    public String getExerciseName()   { return exerciseName; }
    public double getExerciseWeight() { return exerciseWeight; }
    public int    getSets()           { return sets; }
    public int    getReps()           { return reps; }
    public int    getTotalSeconds()   { return totalSeconds; }

    public int    getFatigueId()      { return fatigueId; }
    public int    getFatigueSeconds() { return fatigueSeconds; }

    public String getExerciseDate()   { return exerciseDate; }
    public int    getUploaded()       { return uploaded; }

    /* === 方便 ListView 顯示用 === */
    @Override public String toString() {
        String fat = (fatigueId == -1) ? "無" : fatigueSeconds + "s";
        return participantName + " | " + exerciseName +
                " | " + sets + "x" + reps +
                " | 日期:" + exerciseDate +
                " | 力竭:" + fat +
                (uploaded == 1 ? " | ✔已傳" : " | 未傳");
    }
}
