package com.example.collegeproject;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Locale;

public class FatigueEventActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private ArrayList<FatigueEventItem> eventList;
    private ArrayAdapter<String> adapter;

    private EditText etStart;
    private EditText etEnd;
    private EditText etDuration;
    private Button btnAddEvent;
    private Button btnBack;
    private ListView lvEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fatigue_event);

        // 1. 取得 UI 元件
        //etStart     = findViewById(R.id.etFatigueStart);
        //etEnd       = findViewById(R.id.etFatigueEnd);
        //etDuration  = findViewById(R.id.etFatigueDuration);
       // btnAddEvent = findViewById(R.id.btnAddFatigueEvent);
        btnBack     = findViewById(R.id.btnBackToHome);
        lvEvents    = findViewById(R.id.lvFatigueEvents);

        // 2. 初始化資料庫與清單、Adapter
        db = new DatabaseHelper(this);
        eventList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                new ArrayList<>());
        lvEvents.setAdapter(adapter);

        // 3. 載入既有事件
        loadEvents();
/*
        // 4. 新增事件按鈕
        btnAddEvent.setOnClickListener(v -> {
            String s = etStart.getText().toString().trim();
            String e = etEnd.getText().toString().trim();
            String d = etDuration.getText().toString().trim();
            if (s.isEmpty() || e.isEmpty() || d.isEmpty()) {
                // 這裡可加 Toast 提示使用者
                return;
            }
            int durationSec = Integer.parseInt(d);
            db.insertFatigueEvent(s, e, durationSec);
            etStart.setText("");
            etEnd.setText("");
            etDuration.setText("");
            loadEvents();
        });
*/
        // 5. 返回按鈕
        btnBack.setOnClickListener(v -> onBackPressed());

        // 6. 長按刪除
        lvEvents.setOnItemLongClickListener((parent, view, position, id) -> {
            FatigueEventItem item = eventList.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("刪除確認")
                    .setMessage("確定要刪除這筆事件？")
                    .setPositiveButton("刪除", (dlg, which) -> {
                        db.deleteFatigueEventById(item.getId());
                        loadEvents();
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });
    }

    /** 重新從資料庫讀取並顯示列表 **/
    private void loadEvents() {
        eventList = db.getAllFatigueEvents();
        adapter.clear();
        for (FatigueEventItem e : eventList) {
            String line = String.format(Locale.getDefault(),
                    "ID:%d | 開始:%s | 結束:%s | 持續:%d秒",
                    e.getId(), e.getStartTime(), e.getFatigueTime(), e.getDurationSec());
            adapter.add(line);
        }
        adapter.notifyDataSetChanged();
    }
}
