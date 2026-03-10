package com.example.collegeproject;

public class RecordItem {
    private int id;
    private String exerciseName;
    private double weight;
    private int sets;
    private int reps;
    private int restTime;
    private int totalTime;
    private String completionTime;

    public RecordItem(int id,
                      String exerciseName,
                      double weight,
                      int sets,
                      int reps,
                      int restTime,
                      int totalTime,
                      String completionTime) {
        this.id = id;
        this.exerciseName = exerciseName;
        this.weight = weight;
        this.sets = sets;
        this.reps = reps;
        this.restTime = restTime;
        this.totalTime = totalTime;
        this.completionTime = completionTime;
    }

    public int getId() { return id; }
    public String getExerciseName() { return exerciseName; }
    public double getWeight() { return weight; }
    public int getSets() { return sets; }
    public int getReps() { return reps; }
    public int getRestTime() { return restTime; }
    public int getTotalTime() { return totalTime; }
    public String getCompletionTime() { return completionTime; }
}

