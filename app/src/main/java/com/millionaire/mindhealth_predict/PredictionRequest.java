package com.millionaire.mindhealth_predict;

public class PredictionRequest {
    // ⚠️ CRITICAL: These must match your exact Python Pydantic Model names [1]
    private int age;
    private String gender;
    private String country;
    private String academic_level;
    private String most_used_platform; //  Changed from platform to most_used_platform [1]
    private double avg_daily_usage_hours; //  Changed from screen_time to avg_daily_usage_hours [1]
    private int daily_unlocks;
    private double study_hours;
    private String stress_level;

    public PredictionRequest(int age, String gender, String country, String academic_level,
                             String most_used_platform, double avg_daily_usage_hours,
                             int daily_unlocks, double study_hours, String stress_level) {
        this.age = age;
        this.gender = gender;
        this.country = country;
        this.academic_level = academic_level;
        this.most_used_platform = most_used_platform;
        this.avg_daily_usage_hours = avg_daily_usage_hours;
        this.daily_unlocks = daily_unlocks;
        this.study_hours = study_hours;
        this.stress_level = stress_level;
    }
}
