package com.myprofile.app.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public final class UsernameUtils {

    private static final Set<String> RESERVED = new HashSet<>(Arrays.asList(
            "admin", "api", "app", "login", "privacy", "terms", "delete-account",
            "help", "support", "settings", "www", "myprofile"));

    private static final Random RANDOM = new Random();

    private UsernameUtils() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        s = s.replaceAll("[^a-z0-9-]+", "-");
        s = s.replaceAll("-{2,}", "-");
        s = s.replaceAll("^-+|-+$", "");
        if (s.length() > 30) {
            s = s.substring(0, 30).replaceAll("-+$", "");
        }
        return s;
    }

    public static boolean isValid(String name) {
        return name != null
                && name.matches("^[a-z0-9][a-z0-9-]{2,29}$")
                && !RESERVED.contains(name);
    }

    public static String suggestFrom(String displayName) {
        String base = normalize(displayName);
        if (base.length() > 24) {
            base = base.substring(0, 24).replaceAll("-+$", "");
        }
        if (base.length() < 3) {
            base = "profile-" + (100 + RANDOM.nextInt(900));
        }
        return base;
    }

    public static List<String> alternatives(String base) {
        String trimmed = base.length() > 26 ? base.substring(0, 26).replaceAll("-+$", "") : base;
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            list.add(trimmed + "-" + (10 + RANDOM.nextInt(90)));
        }
        return list;
    }
}