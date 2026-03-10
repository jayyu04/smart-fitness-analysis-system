package com.example.collegeproject;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

/** App 首頁：七大入口（含雲端平台），不再自動啟動 FatigueService */
public class HomeActivity extends AppCompatActivity {

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button btnParticipants   = findViewById(R.id.btnParticipants);
        Button btnSetting        = findViewById(R.id.btnSetting);
        Button btnTraining       = findViewById(R.id.btnTraining);
        Button btnHistory        = findViewById(R.id.btnHistory);
        Button btnFatigue        = findViewById(R.id.btnFatigue);
        Button btnCloudPending   = findViewById(R.id.btnCloud);            // 原本的 CloudPending
        Button btnCloudPlatform  = findViewById(R.id.btnCloudPlatform);    // ★ 新增：雲端平台

        btnParticipants.setOnClickListener(v ->
                startActivity(new Intent(this, ParticipantActivity.class)));

        btnSetting.setOnClickListener(v ->
                startActivity(new Intent(this, SettingActivity.class)));

        btnTraining.setOnClickListener(v ->
                startActivity(new Intent(this, MovementSelectActivity.class)));

        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, HistoryActivity.class)));

        btnFatigue.setOnClickListener(v ->
                startActivity(new Intent(this, FatigueSettingActivity.class)));

        btnCloudPending.setOnClickListener(v ->
                startActivity(new Intent(this, CloudPendingActivity.class)));

        /* ★ 雲端平台：開啟瀏覽器 */
        btnCloudPlatform.setOnClickListener(v -> {
            Uri uri = Uri.parse("http://100.83.47.21/fitness_web/index.html");
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        });
    }
}
