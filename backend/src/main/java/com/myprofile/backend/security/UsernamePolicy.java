package com.myprofile.backend.security;

import java.util.Set;
import java.util.regex.Pattern;

public final class UsernamePolicy {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9][a-z0-9-]{2,29}$");
    private static final Set<String> RESERVED = Set.of(
            "admin", "api", "app", "login", "privacy", "terms", "delete-account",
            "help", "support", "settings", "www", "myprofile");

    private UsernamePolicy() {
    }

    public static boolean isValid(String name) {
        return name != null && PATTERN.matcher(name).matches() && !RESERVED.contains(name);
    }
}