package com.example.collegeproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MovementSelectActivity extends AppCompatActivity {
    private boolean recordingEnabled = false;
    private Button btnRecordToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movement_select);

        Button btnBack = findViewById(R.id.btnBack);
        Button btnCurl = findViewById(R.id.btnCurl);
        Button btnRaise = findViewById(R.id.btnRaise);
        Button btnPress = findViewById(R.id.btnPress);
        btnRecordToggle = findViewById(R.id.btnRecordToggle);

        // 錄影開關
        btnRecordToggle.setText("開啟螢幕錄影");
        btnRecordToggle.setOnClickListener(v -> {
            recordingEnabled = !recordingEnabled;
            if (recordingEnabled) {
                btnRecordToggle.setText("關閉螢幕錄影");
                Toast.makeText(this, "已開啟螢幕錄影", Toast.LENGTH_SHORT).show();
            } else {
                btnRecordToggle.setText("開啟螢幕錄影");
                Toast.makeText(this, "已關閉螢幕錄影", Toast.LENGTH_SHORT).show();
            }
        });

        btnBack.setOnClickListener(v -> finish());

        btnCurl.setOnClickListener(v -> {
            Intent intent = new Intent(this, InstructionActivity.class);
            intent.putExtra("selectedPose", "curl");
            intent.putExtra("reps", 10);
            intent.putExtra("recordingEnabled", recordingEnabled);
            startActivity(intent);
        });

        btnRaise.setOnClickListener(v -> {
            Intent intent = new Intent(this, InstructionActivity.class);
            intent.putExtra("selectedPose", "raise");
            intent.putExtra("reps", 12);
            intent.putExtra("recordingEnabled", recordingEnabled);
            startActivity(intent);
        });

        btnPress.setOnClickListener(v -> {
            Intent intent = new Intent(this, InstructionActivity.class);
            intent.putExtra("selectedPose", "press");
            intent.putExtra("reps", 8);
            intent.putExtra("recordingEnabled", recordingEnabled);
            startActivity(intent);
        });
    }
}