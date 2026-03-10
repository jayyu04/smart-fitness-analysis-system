package com.example.collegeproject;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class ParticipantActivity extends AppCompatActivity {

    private EditText etName, etHeight, etWeight, etAge;
    private Spinner   spGender;                      // ★ 新增
    private Button btnAddParticipant, btnBackToHome;
    private ListView lvParticipants;

    private DatabaseHelper dbHelper;
    private ArrayList<ParticipantItem> participantList;
    private ArrayList<String> participantStrings;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participants);

        /* 綁定 UI */
        btnBackToHome     = findViewById(R.id.btnBackToHome);
        etName            = findViewById(R.id.etName);
        etHeight          = findViewById(R.id.etHeight);
        etWeight          = findViewById(R.id.etWeight);
        etAge             = findViewById(R.id.etAge);
        spGender          = findViewById(R.id.spGender);          // ★ 新增
        btnAddParticipant = findViewById(R.id.btnAddParticipant);
        lvParticipants    = findViewById(R.id.lvParticipants);

        /* 性別下拉選單 (字串陣列定義在 strings.xml/gender_array) */
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(
                this, R.array.gender_array, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(genderAdapter);

        dbHelper = new DatabaseHelper(this);
        participantList    = new ArrayList<>();
        participantStrings = new ArrayList<>();
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, participantStrings);
        lvParticipants.setAdapter(adapter);

        /* 返回主頁 */
        btnBackToHome.setOnClickListener(v -> finish());

        loadParticipants();

        /* 新增受試者 */
        btnAddParticipant.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String sh   = etHeight.getText().toString().trim();
            String tz   = etWeight.getText().toString().trim();
            String nl   = etAge.getText().toString().trim();
            String gender = spGender.getSelectedItem().toString();      // ★ 取得性別

            if (name.isEmpty() || sh.isEmpty() || tz.isEmpty() || nl.isEmpty()) {
                Toast.makeText(this, "請填寫所有欄位", Toast.LENGTH_SHORT).show();
                return;
            }
            double height = Double.parseDouble(sh);
            double weight = Double.parseDouble(tz);
            int    age    = Integer.parseInt(nl);

            dbHelper.insertParticipant(name, height, weight, age, gender); // ★ 改用新方法
            Toast.makeText(this, "已新增：" + name, Toast.LENGTH_SHORT).show();

            etName.setText(""); etHeight.setText("");
            etWeight.setText(""); etAge.setText("");
            spGender.setSelection(0);
            loadParticipants();
        });

        /* 長按刪除 */
        lvParticipants.setOnItemLongClickListener((parent, view, position, id) -> {
            ParticipantItem p = participantList.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("刪除確認")
                    .setMessage("確定要刪除「" + p.getName() + "」？")
                    .setPositiveButton("刪除", (dlg, which) -> {
                        dbHelper.deleteParticipantById(p.getId());
                        Toast.makeText(this, "已刪除：" + p.getName(), Toast.LENGTH_SHORT).show();
                        loadParticipants();
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });
    }

    private void loadParticipants() {
        participantList.clear();
        participantStrings.clear();
        participantList.addAll(dbHelper.getAllParticipants());
        for (ParticipantItem p : participantList) {
            /* toString() 建議也納入 gender；若未改可自行組字串 */
            participantStrings.add(
                    p.getName() + " | " + p.getGender() + " | " +
                            p.getHeight() + "cm  " + p.getWeight() + "kg  " +
                            p.getAge() + "歲");
        }
        adapter.notifyDataSetChanged();
    }
}
