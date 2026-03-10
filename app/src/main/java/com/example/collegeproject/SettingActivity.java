package com.example.collegeproject;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.database.Cursor;
import androidx.appcompat.app.AppCompatActivity;

public class SettingActivity extends AppCompatActivity {
    private EditText editWeight, editSets, editReps, editRest;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        dbHelper = new DatabaseHelper(this);
        editWeight = findViewById(R.id.etWeight);
        editSets = findViewById(R.id.etSets);
        editReps = findViewById(R.id.etReps);
        editRest = findViewById(R.id.etRestTime);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnBack = findViewById(R.id.btnBack);

        // **📌 讀取資料庫內的設定，避免閃退**
        loadSettings();

        btnSave.setOnClickListener(v -> {
            try {
                double weight = Double.parseDouble(editWeight.getText().toString());
                int sets = Integer.parseInt(editSets.getText().toString());
                int reps = Integer.parseInt(editReps.getText().toString());
                int restTime = Integer.parseInt(editRest.getText().toString());

                dbHelper.saveSettings(weight, sets, reps, restTime);
                Toast.makeText(SettingActivity.this, "設定已儲存！", Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(SettingActivity.this, "請輸入有效數值", Toast.LENGTH_SHORT).show();
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadSettings() {
        Cursor cursor = dbHelper.getSettings();
        if (cursor.moveToFirst()) {
            editWeight.setText(String.valueOf(cursor.getDouble(1)));
            editSets.setText(String.valueOf(cursor.getInt(2)));
            editReps.setText(String.valueOf(cursor.getInt(3)));
            editRest.setText(String.valueOf(cursor.getInt(4)));
        }
        cursor.close();
    }
}
