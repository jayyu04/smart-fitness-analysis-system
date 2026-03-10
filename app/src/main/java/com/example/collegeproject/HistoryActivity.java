package com.example.collegeproject;

import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;

public class HistoryActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private CalendarView calendarView;
    private HistoryAdapter historyAdapter;
    private RecyclerView rvHistory;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new DatabaseHelper(this);
        calendarView = findViewById(R.id.calendarView);
        rvHistory = findViewById(R.id.rvHistory);
        btnBack = findViewById(R.id.btnBack);

        // 設置 RecyclerView
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new HistoryAdapter(new ArrayList<>(), dbHelper);
        rvHistory.setAdapter(historyAdapter);

        // 預設載入「今天」的紀錄
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // 月份從0開始
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        String today = String.format("%04d-%02d-%02d", year, month, day);
        loadHistoryByDate(today);

        // 監聽日曆
        calendarView.setOnDateChangeListener((view, year1, month1, dayOfMonth) -> {
            String selectedDate = String.format("%04d-%02d-%02d", year1, (month1 + 1), dayOfMonth);
            loadHistoryByDate(selectedDate);
        });

        // 左滑刪除：用 ItemTouchHelper
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false; // 不做拖曳
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                // 左滑 => 刪除
                if (direction == ItemTouchHelper.LEFT) {
                    int position = viewHolder.getAdapterPosition();
                    historyAdapter.deleteItem(position);
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(rvHistory);

        // 返回
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadHistoryByDate(String date) {
        // 從 DB 拿該日紀錄
        // DatabaseHelper 提供 getRecordsByDate(date)
        ArrayList<RecordItem> recordItems = dbHelper.getRecordsByDateObjects(date);
        // 用 adapter 更新
        historyAdapter.setData(recordItems);
    }
}
