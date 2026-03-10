package com.example.collegeproject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * CloudPendingActivity
 * --------------------
 * 顯示 cloud_pending，並將 uploaded=0 的資料 POST 到 upload.php。
 */
public class CloudPendingActivity extends AppCompatActivity {

    private Button   btnAdd, btnUpload, btnBack;
    private ListView lvPending;

    private DatabaseHelper db;
    private ArrayList<CloudPendingItem> pendingList;
    private ArrayList<String>           pendingStrs;
    private ArrayAdapter<String>        adapter;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cloud_pending);

        btnAdd    = findViewById(R.id.btnAddPending);
        btnUpload = findViewById(R.id.btnUpload);
        btnBack   = findViewById(R.id.btnBackPending);
        lvPending = findViewById(R.id.lvPending);

        db = new DatabaseHelper(this);
        pendingList = new ArrayList<>();
        pendingStrs = new ArrayList<>();
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, pendingStrs);
        lvPending.setAdapter(adapter);

        loadPending();

        btnAdd.setOnClickListener(v ->
                new AddPendingDialog(this, db, this::loadPending).show()
        );

        btnUpload.setOnClickListener(v -> {
            if (pendingList.isEmpty()) {
                Toast.makeText(this, "沒有待上傳資料", Toast.LENGTH_SHORT).show();
                return;
            }
            new UploadTask().execute();
        });

        btnBack.setOnClickListener(v -> finish());

        lvPending.setOnItemLongClickListener((p, view, pos, id) -> {
            CloudPendingItem item = pendingList.get(pos);
            new AlertDialog.Builder(this)
                    .setTitle("刪除確認")
                    .setMessage("確定刪除該筆紀錄？")
                    .setPositiveButton("刪除", (d, w) -> {
                        db.deletePendingById(item.getId());
                        loadPending();
                    }).setNegativeButton("取消", null).show();
            return true;
        });
    }

    /** 重新撈 cloud_pending → ListView */
    private void loadPending() {
        pendingList.clear();
        pendingStrs.clear();
        pendingList.addAll(db.getAllPending(false));
        for (CloudPendingItem c : pendingList) pendingStrs.add(c.toString());
        adapter.notifyDataSetChanged();
    }

    /* ---------------- 上傳任務 ---------------- */
    private class UploadTask extends AsyncTask<Void, Void, Integer> {
        @Override protected void onPreExecute() {
            Toast.makeText(CloudPendingActivity.this, "開始上傳…", Toast.LENGTH_SHORT).show();
        }

        @Override protected Integer doInBackground(Void... voids) {
            int success = 0;

            ArrayList<CloudPendingItem> list = db.getAllPending(true);
            Log.d("UPLOAD", "Rows to upload = " + list.size());

            /* TODO: 換成自己電腦或伺服器 IP */
            String apiUrl = "http://100.83.47.21/fitness_api/upload.php";

            for (CloudPendingItem item : list) {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(apiUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                    /* --- 組 POST data --- */
                    String postData =
                            "name="            + URLEncoder.encode(item.getParticipantName(), "UTF-8") +
                                    "&height="          + item.getHeightCm() +
                                    "&weight="          + item.getWeightKg() +
                                    "&age="             + item.getAge() +
                                    "&gender="          + URLEncoder.encode(item.getGender(), "UTF-8") +
                                    "&exercise_name="   + URLEncoder.encode(item.getExerciseName(), "UTF-8") +
                                    "&exercise_weight=" + item.getExerciseWeight() +
                                    "&sets="            + item.getSets() +
                                    "&reps="            + item.getReps() +
                                    "&total_time="      + item.getTotalSeconds() +
                                    "&fatigue_sec="     + (item.getFatigueSeconds() <= 0 ? "" : item.getFatigueSeconds()) +
                                    "&date="            + URLEncoder.encode(item.getExerciseDate(), "UTF-8");

                    /* --- 傳送 --- */
                    OutputStream os = conn.getOutputStream();
                    os.write(postData.getBytes());
                    os.flush();
                    os.close();

                    int code = conn.getResponseCode();
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(
                                    code >= 400 ? conn.getErrorStream() : conn.getInputStream()));
                    StringBuilder resp = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) resp.append(line);
                    br.close();

                    Log.d("UPLOAD", "id=" + item.getId() +
                            " HTTP " + code + " resp=" + resp);

                    if (code == 200 && resp.toString().contains("success")) {
                        db.markUploaded(item.getId());
                        success++;
                    }
                } catch (Exception e) {
                    Log.e("UPLOAD", "id=" + item.getId() + " error=" + e);
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
            return success;
        }

        @Override protected void onPostExecute(Integer ok) {
            Toast.makeText(CloudPendingActivity.this,
                    "成功上傳 " + ok + " 筆", Toast.LENGTH_SHORT).show();
            loadPending();
        }
    }
}
