package com.example.collegeproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

/**
 * HistoryAdapter
 * -------------
 * 1. 顯示每筆「運動紀錄」
 * 2. swipe => delete
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private ArrayList<RecordItem> records;
    private DatabaseHelper dbHelper;

    public HistoryAdapter(ArrayList<RecordItem> records, DatabaseHelper dbHelper) {
        this.records = records;
        this.dbHelper = dbHelper;
    }

    public void setData(ArrayList<RecordItem> newRecords) {
        this.records = newRecords;
        notifyDataSetChanged();
    }

    // 左滑刪除
    public void deleteItem(int position) {
        if (position < 0 || position >= records.size()) return;
        RecordItem item = records.get(position);
        int id = item.getId(); // 拿到該筆紀錄的id

        // DB刪除
        dbHelper.deleteRecordById(id);

        // 移除列表
        records.remove(position);
        notifyItemRemoved(position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecordItem item = records.get(position);

        // 顯示文字 (可自訂)
        String info = "ID: " + item.getId()
                + " 運動: " + item.getExerciseName()
                + " 重量: " + item.getWeight()
                + " 組數: " + item.getSets()
                + " 次數: " + item.getReps()
                + " 休息: " + item.getRestTime()
                + " 總秒數: " + item.getTotalTime()
                + " 完成時間: " + item.getCompletionTime();

        holder.txtInfo.setText(info);
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtInfo;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtInfo = itemView.findViewById(android.R.id.text1);
        }
    }
}
