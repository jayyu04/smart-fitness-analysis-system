package com.example.collegeproject;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.*;
import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * AddPendingDialog
 * ----------------
 * 三個 Spinner 依序選：受試者、運動紀錄、力竭紀錄(可無)。
 * 整合成 cloud_pending 一筆資料後寫入 DB。
 */
public class AddPendingDialog extends Dialog {

    /* 回呼：新增成功後讓父頁面刷新列表 */
    public interface OnAddedListener { void onAdded(); }

    private final DatabaseHelper db;
    private final OnAddedListener listener;

    private Spinner spParticipant, spExercise, spFatigue;
    private Button  btnOk, btnCancel;

    /* 對應 Spinner 的原始資料 */
    private ArrayList<ParticipantItem>   participants;
    private ArrayList<RecordItem>        exercises;
    private ArrayList<FatigueEventItem>  fatigues;   // index 0 = null (無)

    public AddPendingDialog(@NonNull Context ctx,
                            DatabaseHelper db,
                            OnAddedListener listener) {
        super(ctx);
        this.db = db;
        this.listener = listener;
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_add_pending);

        /* 綁定 UI */
        spParticipant = findViewById(R.id.spParticipant);
        spExercise    = findViewById(R.id.spExercise);
        spFatigue     = findViewById(R.id.spFatigue);
        btnOk         = findViewById(R.id.btnAddOk);
        btnCancel     = findViewById(R.id.btnAddCancel);

        /* 讀資料填入 Spinner */
        loadSpinners();

        btnCancel.setOnClickListener(v -> dismiss());

        btnOk.setOnClickListener(v -> {
            int pPos = spParticipant.getSelectedItemPosition();
            int ePos = spExercise.getSelectedItemPosition();
            int fPos = spFatigue.getSelectedItemPosition();

            if (pPos < 0 || ePos < 0) {
                Toast.makeText(getContext(),
                        "請選擇受試者與運動紀錄", Toast.LENGTH_SHORT).show();
                return;
            }

            ParticipantItem   p = participants.get(pPos);
            RecordItem        r = exercises.get(ePos);
            FatigueEventItem  f = (fPos == 0) ? null : fatigues.get(fPos);

            /* === 整合欄位 === */
            ContentValues cv = new ContentValues();
            cv.put(DatabaseHelper.COLUMN_CLOUD_PARTICIPANT_ID,    p.getId());
            cv.put(DatabaseHelper.COLUMN_CLOUD_PARTICIPANT_NAME,  p.getName());
            cv.put(DatabaseHelper.COLUMN_CLOUD_HEIGHT,            p.getHeight());
            cv.put(DatabaseHelper.COLUMN_CLOUD_WEIGHT,            p.getWeight());
            cv.put(DatabaseHelper.COLUMN_CLOUD_AGE,               p.getAge());
            cv.put(DatabaseHelper.COLUMN_CLOUD_GENDER,            p.getGender());

            cv.put(DatabaseHelper.COLUMN_CLOUD_EXERCISE_ID,       r.getId());
            cv.put(DatabaseHelper.COLUMN_CLOUD_EXERCISE_NAME,     r.getExerciseName());
            cv.put(DatabaseHelper.COLUMN_CLOUD_EXERCISE_WEIGHT,   r.getWeight());
            cv.put(DatabaseHelper.COLUMN_CLOUD_SETS,              r.getSets());
            cv.put(DatabaseHelper.COLUMN_CLOUD_REPS,              r.getReps());
            cv.put(DatabaseHelper.COLUMN_CLOUD_TOTAL_SEC,         r.getTotalTime());

            if (f != null) {
                cv.put(DatabaseHelper.COLUMN_CLOUD_FATIGUE_ID,  f.getId());
                cv.put(DatabaseHelper.COLUMN_CLOUD_FATIGUE_SEC, f.getDuration());
            } else {
                cv.putNull(DatabaseHelper.COLUMN_CLOUD_FATIGUE_ID);
                cv.putNull(DatabaseHelper.COLUMN_CLOUD_FATIGUE_SEC);
            }

            /* 取 yyyy-MM-dd */
            try {
                cv.put(DatabaseHelper.COLUMN_CLOUD_EXERCISE_DATE,
                        r.getCompletionTime().substring(0, 10));
            } catch (Exception ex) {
                cv.put(DatabaseHelper.COLUMN_CLOUD_EXERCISE_DATE, "");
            }

            /* 寫入 cloud_pending */
            long newId = db.insertPending(cv);
            if (newId > 0) {
                Toast.makeText(getContext(),
                        "已新增待上傳資料", Toast.LENGTH_SHORT).show();
                if (listener != null) listener.onAdded();
                dismiss();
            } else {
                Toast.makeText(getContext(),
                        "新增失敗", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** 讀取資料庫，填 Spinner */
    private void loadSpinners() {
        /* 受試者 */
        participants = db.getAllParticipants();
        ArrayList<String> pNames = new ArrayList<>();
        for (ParticipantItem p : participants) pNames.add(p.getName());
        spParticipant.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, pNames));

        /* 運動紀錄（全部，最新→最舊） */
        exercises = db.getRecordsByDateObjects("");
        ArrayList<String> eStrs = new ArrayList<>();
        for (RecordItem r : exercises) {
            eStrs.add(r.getCompletionTime() + " | " +
                    r.getExerciseName()    + " | " +
                    r.getWeight() + "kg");
        }
        spExercise.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, eStrs));

        /* 力竭紀錄 */
        fatigues = db.getAllFatigueEvents();   // 可能為空
        ArrayList<String> fStrs = new ArrayList<>();
        fStrs.add("無");                       // index 0
        fatigues.add(0, null);                 // 讓 index 對齊
        for (int i = 1; i < fatigues.size(); i++) {
            FatigueEventItem f = fatigues.get(i);
            fStrs.add(f.getStartTime() + " | " + f.getDuration() + "s");
        }
        spFatigue.setAdapter(new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, fStrs));
    }
}
