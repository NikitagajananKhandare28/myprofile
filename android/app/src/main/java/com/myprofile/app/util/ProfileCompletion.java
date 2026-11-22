package com.myprofile.app.util;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

public final class ProfileCompletion {

    private ProfileCompletion() {
    }

    public static int percent(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return 0;
        }
        int total = 0;
        if (hasText(snapshot.getString("displayName"))) {
            total += 15;
        }
        if (hasText(snapshot.getString("profileType"))) {
            total += 15;
        }
        if (hasText(snapshot.getString("city"))) {
            total += 10;
        }
        if (hasText(snapshot.getString("bio"))) {
            total += 15;
        }
        if (hasText(snapshot.getString("aboutMe"))) {
            total += 25;
        }
        Object skills = snapshot.get("skills");
        if (skills instanceof List && !((List<?>) skills).isEmpty()) {
            total += 20;
        }
        return total;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
