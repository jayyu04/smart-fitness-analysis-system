package com.example.collegeproject;

/**
 * ParticipantItem
 * ---------------
 * 封裝 participants 表中的一筆受試者資料（含性別）
 */
public class ParticipantItem {

    private final int    id;
    private final String name;
    private final double height;   // cm
    private final double weight;   // kg
    private final int    age;      // 歲
    private final String gender;   // "男" / "女" / "其他"

    public ParticipantItem(int id, String name,
                           double height, double weight,
                           int age, String gender) {
        this.id     = id;
        this.name   = name;
        this.height = height;
        this.weight = weight;
        this.age    = age;
        this.gender = gender;
    }

    /* ───────── getters ───────── */
    public int    getId()     { return id; }
    public String getName()   { return name; }
    public double getHeight() { return height; }
    public double getWeight() { return weight; }
    public int    getAge()    { return age; }
    public String getGender() { return gender; }

    @Override
    public String toString() {
        return name
                + " | 性別:" + gender
                + " | 身高:" + height + "cm"
                + " | 體重:" + weight + "kg"
                + " | 年齡:" + age;
    }
}
