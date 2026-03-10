package com.example.collegeproject;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class InstructionActivity extends AppCompatActivity {

    private ImageView ivPoseImage;
    private TextView tvPoseIntro;
    private Button btnStartTraining, btnBack;

    private String selectedPose;
    private int reps;
    private boolean recordingEnabled;  // 新增：錄影狀態

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instruction);

        ivPoseImage       = findViewById(R.id.ivPoseImage);
        tvPoseIntro       = findViewById(R.id.tvPoseIntro);
        btnStartTraining  = findViewById(R.id.btnStartTraining);
        btnBack           = findViewById(R.id.btnBack);

        // 讀取參數
        Intent intent = getIntent();
        selectedPose     = intent.getStringExtra("selectedPose");
        reps             = intent.getIntExtra("reps", 0);
        recordingEnabled = intent.getBooleanExtra("recordingEnabled", false);

        // 根據 selectedPose 顯示文字與圖片
        switch (selectedPose) {
            case "curl":
                ivPoseImage.setImageResource(R.drawable.img_curl);
                tvPoseIntro.setText(
                        "二頭彎舉（Bicep Curl）說明：\n" +
                                "1. 站立，雙腳與肩同寬，手臂自然下垂，手持啞鈴。\n" +
                                "2. 保持肘部貼近身體，上臂不動，僅前臂向上彎舉。\n" +
                                "3. 彎舉至最高點後，慢慢放下回到起始位置。\n" +
                                "建議次數：" + reps + " 下／組"
                );
                break;
            case "raise":
                ivPoseImage.setImageResource(R.drawable.img_raise);
                tvPoseIntro.setText(
                        "側平舉（Lateral Raise）說明：\n" +
                                "1. 站立，雙腳與肩同寬，手持啞鈴於身側。\n" +
                                "2. 手臂微彎，將啞鈴平舉至與肩同高。\n" +
                                "3. 停留一秒後，慢慢放下回到起始位置。\n" +
                                "建議次數：" + reps + " 下／組"
                );
                break;
            case "press":
                ivPoseImage.setImageResource(R.drawable.img_press);
                tvPoseIntro.setText(
                        "肩推（Shoulder Press）說明：\n" +
                                "1. 站立或坐姿，雙手持啞鈴於肩上方。\n" +
                                "2. 手肘略微向前，向上推舉啞鈴至手臂伸直。\n" +
                                "3. 慢慢放下回到起始位置。\n" +
                                "建議次數：" + reps + " 下／組"
                );
                break;
            default:
                ivPoseImage.setImageResource(android.R.color.transparent);
                tvPoseIntro.setText("未知動作");
        }

        // 「開始訓練」→ 進入 CameraFirstActivity，並傳入錄影狀態
        btnStartTraining.setOnClickListener(v -> {
            Intent i = new Intent(InstructionActivity.this, CameraFirstActivity.class);
            i.putExtra("selectedPose", selectedPose);
            i.putExtra("reps", reps);
            i.putExtra("recordingEnabled", recordingEnabled);
            startActivity(i);
            finish();
        });

        // 「返回」→ 回到動作選擇
        btnBack.setOnClickListener(v -> finish());
    }
}