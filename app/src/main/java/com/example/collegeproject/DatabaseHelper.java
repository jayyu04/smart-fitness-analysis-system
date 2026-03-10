package com.example.collegeproject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * DatabaseHelper
 * --------------
 * - v3: participants 新增 gender
 * - v4: 新增 cloud_pending 表 (雲端同步)
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "fitness.db";
    private static final int    DATABASE_VERSION = 4;   // ← 提升

    /* ===== exercise_records ===== */
    public static final String TABLE_EXERCISE           = "exercise_records";
    public static final String COLUMN_ID                = "id";
    public static final String COLUMN_EXERCISE_NAME     = "exercise_name";
    public static final String COLUMN_WEIGHT            = "weight";
    public static final String COLUMN_SETS              = "sets";
    public static final String COLUMN_REPS              = "reps";
    public static final String COLUMN_REST_TIME         = "rest_time";
    public static final String COLUMN_TOTAL_TIME        = "total_time";
    public static final String COLUMN_COMPLETION_TIME   = "completion_time";

    /* ===== settings ===== */
    public static final String TABLE_SETTINGS           = "settings";
    public static final String COLUMN_SETTING_ID        = "id";
    public static final String COLUMN_SETTING_WEIGHT    = "weight";
    public static final String COLUMN_SETTING_SETS      = "sets";
    public static final String COLUMN_SETTING_REPS      = "reps";
    public static final String COLUMN_SETTING_REST_TIME = "rest_time";

    /* ===== participants ===== */
    public static final String TABLE_PARTICIPANT        = "participants";
    public static final String COLUMN_PARTICIPANT_ID    = "pid";
    public static final String COLUMN_PARTICIPANT_NAME  = "name";
    public static final String COLUMN_PARTICIPANT_HEIGHT= "height";
    public static final String COLUMN_PARTICIPANT_WEIGHT= "p_weight";
    public static final String COLUMN_PARTICIPANT_AGE   = "age";
    public static final String COLUMN_PARTICIPANT_GENDER= "gender";

    /* ===== fatigue_events ===== */
    public static final String TABLE_FATIGUE            = "fatigue_events";
    public static final String COLUMN_FATIGUE_ID        = "fid";
    public static final String COLUMN_FATIGUE_START     = "start_time";
    public static final String COLUMN_FATIGUE_END       = "end_time";
    public static final String COLUMN_FATIGUE_DURATION  = "duration";

    /* ===== cloud_pending (NEW) ===== */
    public static final String TABLE_CLOUD              = "cloud_pending";
    public static final String COLUMN_CLOUD_ID          = "cid";
    public static final String COLUMN_CLOUD_PARTICIPANT_ID   = "participant_id";
    public static final String COLUMN_CLOUD_PARTICIPANT_NAME = "participant_name";
    public static final String COLUMN_CLOUD_HEIGHT      = "height_cm";
    public static final String COLUMN_CLOUD_WEIGHT      = "weight_kg";
    public static final String COLUMN_CLOUD_AGE         = "age";
    public static final String COLUMN_CLOUD_GENDER      = "gender";
    public static final String COLUMN_CLOUD_EXERCISE_ID = "exercise_id";
    public static final String COLUMN_CLOUD_EXERCISE_NAME = "exercise_name";
    public static final String COLUMN_CLOUD_EXERCISE_WEIGHT = "exercise_weight";
    public static final String COLUMN_CLOUD_SETS        = "sets";
    public static final String COLUMN_CLOUD_REPS        = "reps";
    public static final String COLUMN_CLOUD_TOTAL_SEC   = "total_seconds";
    public static final String COLUMN_CLOUD_FATIGUE_ID  = "fatigue_id";
    public static final String COLUMN_CLOUD_FATIGUE_SEC = "fatigue_seconds";
    public static final String COLUMN_CLOUD_EXERCISE_DATE = "exercise_date";
    public static final String COLUMN_CLOUD_UPLOADED    = "uploaded";     // 0/1

    public DatabaseHelper(Context ctx) { super(ctx, DATABASE_NAME, null, DATABASE_VERSION); }

    /* ---------------- onCreate ---------------- */
    @Override public void onCreate(SQLiteDatabase db) {

        db.execSQL(
                "CREATE TABLE " + TABLE_EXERCISE + " (" +
                        COLUMN_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_EXERCISE_NAME+" TEXT, " +
                        COLUMN_WEIGHT+" REAL, " +
                        COLUMN_SETS+" INTEGER, " +
                        COLUMN_REPS+" INTEGER, " +
                        COLUMN_REST_TIME+" INTEGER, " +
                        COLUMN_TOTAL_TIME+" INTEGER, " +
                        COLUMN_COMPLETION_TIME+" TEXT)"
        );

        db.execSQL(
                "CREATE TABLE " + TABLE_SETTINGS + " (" +
                        COLUMN_SETTING_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_SETTING_WEIGHT+" REAL, " +
                        COLUMN_SETTING_SETS+" INTEGER, " +
                        COLUMN_SETTING_REPS+" INTEGER, " +
                        COLUMN_SETTING_REST_TIME+" INTEGER)"
        );
        ContentValues def = new ContentValues();
        def.put(COLUMN_SETTING_WEIGHT, 20.0);
        def.put(COLUMN_SETTING_SETS,   3);
        def.put(COLUMN_SETTING_REPS,   10);
        def.put(COLUMN_SETTING_REST_TIME, 60);
        db.insert(TABLE_SETTINGS,null,def);

        db.execSQL(
                "CREATE TABLE " + TABLE_PARTICIPANT + " (" +
                        COLUMN_PARTICIPANT_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_PARTICIPANT_NAME+" TEXT, " +
                        COLUMN_PARTICIPANT_HEIGHT+" REAL, " +
                        COLUMN_PARTICIPANT_WEIGHT+" REAL, " +
                        COLUMN_PARTICIPANT_AGE+" INTEGER, " +
                        COLUMN_PARTICIPANT_GENDER+" TEXT)"
        );

        db.execSQL(
                "CREATE TABLE " + TABLE_FATIGUE + " (" +
                        COLUMN_FATIGUE_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_FATIGUE_START+" TEXT, " +
                        COLUMN_FATIGUE_END+" TEXT, " +
                        COLUMN_FATIGUE_DURATION+" INTEGER)"
        );

        db.execSQL(
                "CREATE TABLE " + TABLE_CLOUD + " (" +
                        COLUMN_CLOUD_ID+" INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        COLUMN_CLOUD_PARTICIPANT_ID+" INTEGER, " +
                        COLUMN_CLOUD_PARTICIPANT_NAME+" TEXT, " +
                        COLUMN_CLOUD_HEIGHT+" REAL, " +
                        COLUMN_CLOUD_WEIGHT+" REAL, " +
                        COLUMN_CLOUD_AGE+" INTEGER, " +
                        COLUMN_CLOUD_GENDER+" TEXT, " +
                        COLUMN_CLOUD_EXERCISE_ID+" INTEGER, " +
                        COLUMN_CLOUD_EXERCISE_NAME+" TEXT, " +
                        COLUMN_CLOUD_EXERCISE_WEIGHT+" REAL, " +
                        COLUMN_CLOUD_SETS+" INTEGER, " +
                        COLUMN_CLOUD_REPS+" INTEGER, " +
                        COLUMN_CLOUD_TOTAL_SEC+" INTEGER, " +
                        COLUMN_CLOUD_FATIGUE_ID+" INTEGER, " +
                        COLUMN_CLOUD_FATIGUE_SEC+" INTEGER, " +
                        COLUMN_CLOUD_EXERCISE_DATE+" TEXT, " +
                        COLUMN_CLOUD_UPLOADED+" INTEGER DEFAULT 0)"
        );
    }

    /* ---------------- onUpgrade ---------------- */
    @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV){
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_EXERCISE);
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_SETTINGS);
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_PARTICIPANT);
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_FATIGUE);
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_CLOUD);
        onCreate(db);
    }

    /* ===== Settings CRUD ===== */
    public void saveSettings(double w,int s,int r,int rest){
        SQLiteDatabase db=getWritableDatabase();
        ContentValues v=new ContentValues();
        v.put(COLUMN_SETTING_WEIGHT,w);
        v.put(COLUMN_SETTING_SETS,s);
        v.put(COLUMN_SETTING_REPS,r);
        v.put(COLUMN_SETTING_REST_TIME,rest);
        db.update(TABLE_SETTINGS,v,COLUMN_SETTING_ID+"=1",null);
        db.close();
    }
    public Cursor getSettings(){
        return getReadableDatabase().rawQuery(
                "SELECT * FROM "+TABLE_SETTINGS+" WHERE "+COLUMN_SETTING_ID+"=1",null);
    }

    /* ===== Exercise Records CRUD ===== */
    public void insertRecord(String name,double w,int sets,int reps,int rest,int total){
        SQLiteDatabase db=getWritableDatabase();
        ContentValues v=new ContentValues();
        v.put(COLUMN_EXERCISE_NAME,name);
        v.put(COLUMN_WEIGHT,w);
        v.put(COLUMN_SETS,sets);
        v.put(COLUMN_REPS,reps);
        v.put(COLUMN_REST_TIME,rest);
        v.put(COLUMN_TOTAL_TIME,total);
        v.put(COLUMN_COMPLETION_TIME,new SimpleDateFormat("yyyy-MM-dd HH:mm",
                Locale.getDefault()).format(new Date()));
        db.insert(TABLE_EXERCISE,null,v); db.close();
    }
    public ArrayList<String> getAllRecords(){
        ArrayList<String> list=new ArrayList<>();
        SQLiteDatabase db=getReadableDatabase();
        Cursor c=db.rawQuery("SELECT * FROM "+TABLE_EXERCISE+
                " ORDER BY "+COLUMN_ID+" DESC",null);
        while(c.moveToNext()){
            list.add("運動："+c.getString(1)
                    +" | 重量："+c.getDouble(2)+"kg"
                    +" | 組數："+c.getInt(3)
                    +" | 次數："+c.getInt(4)
                    +" | 休息："+c.getInt(5)+"s"
                    +" | 總時間："+c.getInt(6)+"s"
                    +" | 完成時間："+c.getString(7));
        }
        c.close(); db.close(); return list;
    }
    public ArrayList<String> getRecordsByDate(String date){
        ArrayList<String> list=new ArrayList<>();
        SQLiteDatabase db=getReadableDatabase();
        Cursor c=db.rawQuery("SELECT * FROM "+TABLE_EXERCISE+
                        " WHERE "+COLUMN_COMPLETION_TIME+" LIKE ?",
                new String[]{date+"%"});
        while(c.moveToNext()){
            list.add("運動："+c.getString(1)
                    +" | 重量："+c.getDouble(2)+"kg"
                    +" | 組數："+c.getInt(3)
                    +" | 次數："+c.getInt(4)
                    +" | 休息："+c.getInt(5)+"s"
                    +" | 總時間："+c.getInt(6)+"s"
                    +" | 完成時間："+c.getString(7));
        }
        c.close(); db.close(); return list;
    }
    public ArrayList<RecordItem> getRecordsByDateObjects(String date){
        ArrayList<RecordItem> list=new ArrayList<>();
        SQLiteDatabase db=getReadableDatabase();
        Cursor c=db.rawQuery("SELECT * FROM "+TABLE_EXERCISE+
                        " WHERE "+COLUMN_COMPLETION_TIME+" LIKE ? ORDER BY "+COLUMN_ID+" DESC",
                new String[]{date+"%"});
        while(c.moveToNext()){
            list.add(new RecordItem(
                    c.getInt(0),c.getString(1),c.getDouble(2),
                    c.getInt(3),c.getInt(4),c.getInt(5),
                    c.getInt(6),c.getString(7)
            ));
        }
        c.close(); db.close(); return list;
    }
    public void deleteRecordById(int id){
        SQLiteDatabase db=getWritableDatabase();
        db.delete(TABLE_EXERCISE,COLUMN_ID+"=?",new String[]{String.valueOf(id)});
        db.close();
    }

    /* ===== Participants CRUD ===== */
    public void insertParticipant(String name,double h,double w,int age,String g){
        SQLiteDatabase db=getWritableDatabase();
        ContentValues v=new ContentValues();
        v.put(COLUMN_PARTICIPANT_NAME,name);
        v.put(COLUMN_PARTICIPANT_HEIGHT,h);
        v.put(COLUMN_PARTICIPANT_WEIGHT,w);
        v.put(COLUMN_PARTICIPANT_AGE,age);
        v.put(COLUMN_PARTICIPANT_GENDER,g);
        db.insert(TABLE_PARTICIPANT,null,v); db.close();
    }
    public ArrayList<ParticipantItem> getAllParticipants(){
        ArrayList<ParticipantItem> list=new ArrayList<>();
        SQLiteDatabase db=getReadableDatabase();
        Cursor c=db.rawQuery("SELECT * FROM "+TABLE_PARTICIPANT+
                " ORDER BY "+COLUMN_PARTICIPANT_ID+" DESC",null);
        while(c.moveToNext()){
            list.add(new ParticipantItem(
                    c.getInt(0),c.getString(1),c.getDouble(2),
                    c.getDouble(3),c.getInt(4),c.getString(5)
            ));
        }
        c.close(); db.close(); return list;
    }
    public void deleteParticipantById(int pid){
        SQLiteDatabase db=getWritableDatabase();
        db.delete(TABLE_PARTICIPANT,COLUMN_PARTICIPANT_ID+"=?",
                new String[]{String.valueOf(pid)});
        db.close();
    }

    /* ===== Fatigue Events CRUD ===== */
    public void insertFatigueEvent(String start,String end,int dur){
        SQLiteDatabase db=getWritableDatabase();
        ContentValues v=new ContentValues();
        v.put(COLUMN_FATIGUE_START,start);
        v.put(COLUMN_FATIGUE_END,end);
        v.put(COLUMN_FATIGUE_DURATION,dur);
        db.insert(TABLE_FATIGUE,null,v); db.close();
    }
    public ArrayList<FatigueEventItem> getAllFatigueEvents(){
        ArrayList<FatigueEventItem> list=new ArrayList<>();
        SQLiteDatabase db=getReadableDatabase();
        Cursor c=db.rawQuery("SELECT * FROM "+TABLE_FATIGUE+
                " ORDER BY "+COLUMN_FATIGUE_ID+" DESC",null);
        while(c.moveToNext()){
            list.add(new FatigueEventItem(
                    c.getInt(0),c.getString(1),
                    c.getString(2),c.getInt(3)
            ));
        }
        c.close(); db.close(); return list;
    }
    public void deleteFatigueEventById(int fid){
        SQLiteDatabase db=getWritableDatabase();
        db.delete(TABLE_FATIGUE,COLUMN_FATIGUE_ID+"=?",
                new String[]{String.valueOf(fid)});
        db.close();
    }

    /* ===== Cloud Pending CRUD (NEW) ===== */
    public long insertPending(ContentValues v){
        SQLiteDatabase db=getWritableDatabase();
        long id=db.insert(TABLE_CLOUD,null,v);
        db.close(); return id;
    }
    public ArrayList<CloudPendingItem> getAllPending(boolean onlyUnsent){
        ArrayList<CloudPendingItem> list=new ArrayList<>();
        String where=onlyUnsent?"WHERE "+COLUMN_CLOUD_UPLOADED+"=0":"";
        SQLiteDatabase db=getReadableDatabase();
        Cursor c=db.rawQuery("SELECT * FROM "+TABLE_CLOUD+" "+where+
                " ORDER BY "+COLUMN_CLOUD_ID+" DESC",null);
        while(c.moveToNext()){
            list.add(new CloudPendingItem(
                    c.getInt(0),c.getInt(1),c.getString(2),
                    c.getDouble(3),c.getDouble(4),c.getInt(5),c.getString(6),
                    c.getInt(7),c.getString(8),c.getDouble(9),
                    c.getInt(10),c.getInt(11),c.getInt(12),
                    c.isNull(13)?-1:c.getInt(13),      // fatigue_id
                    c.isNull(14)?-1:c.getInt(14),      // fatigue_sec
                    c.getString(15),c.getInt(16)
            ));
        }
        c.close(); db.close(); return list;
    }
    public void markUploaded(int cid){
        SQLiteDatabase db=getWritableDatabase();
        ContentValues v=new ContentValues();
        v.put(COLUMN_CLOUD_UPLOADED,1);
        db.update(TABLE_CLOUD,v,COLUMN_CLOUD_ID+"=?",
                new String[]{String.valueOf(cid)});
        db.close();
    }
    public void deletePendingById(int cid){
        SQLiteDatabase db=getWritableDatabase();
        db.delete(TABLE_CLOUD,COLUMN_CLOUD_ID+"=?",
                new String[]{String.valueOf(cid)});
        db.close();
    }
}
