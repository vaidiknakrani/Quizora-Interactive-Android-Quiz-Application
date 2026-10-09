package com.example.adminquiz.utils;

public final class Categories {
    private Categories() {
    }

    public static final String[] QUIZ_CATEGORIES = {"Java", "Python", "C++", "Android", "DSA", "DBMS",
            "Operating Systems", "Computer Networks", "AI/ML", "Science", "Mathematics", "History",
            "Geography", "General Knowledge"};

    public static final String CUSTOM = "Custom";

    public static String[] aiCategories() {
        String[] all = new String[QUIZ_CATEGORIES.length + 1];
        System.arraycopy(QUIZ_CATEGORIES, 0, all, 0, QUIZ_CATEGORIES.length);
        all[all.length - 1] = CUSTOM;
        return all;
    }

    public static String[] filterCategories() {
        String[] all = new String[QUIZ_CATEGORIES.length + 1];
        all[0] = "All";
        System.arraycopy(QUIZ_CATEGORIES, 0, all, 1, QUIZ_CATEGORIES.length);
        return all;
    }

    public static final String[] DIFFICULTIES = {"Easy", "Medium", "Hard", "Expert"};
    public static final String[] AI_COUNTS = {"5", "10", "15", "20"};
    public static final String[] AI_TYPES = {"MCQ", "True/False", "Mixed"};
    public static final String[] AI_TIMES = {"5 minutes", "10 minutes", "15 minutes", "No Limit"};
}
