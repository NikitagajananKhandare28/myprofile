package com.myprofile.app.util;

public final class ProfileTypes {

    public static final String[] LABELS = {
        "Personal",
        "Student",
        "Sportsperson",
        "Teacher / Tutor",
        "Professional",
        "Business",
        "Freelancer / Creator",
        "Service Provider",
        "Artist / Other"
    };

    private ProfileTypes() {
    }

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        for (String label : LABELS) {
            if (label.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
