package com.myprofile.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Builds the public copy of a profile from private data. Pure logic with no
 * Firebase calls, so every privacy rule can be unit tested.
 */
public final class SnapshotBuilder {

    public static final String STORAGE_PREFIX = "https://firebasestorage.googleapis.com/";

    private static final Pattern PHONE = Pattern.compile("^[+]?[0-9]{7,15}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Set<String> PLATFORMS = Set.of(
            "Instagram", "YouTube", "Facebook", "LinkedIn", "Twitter / X",
            "GitHub", "Website", "Other");

    public record Input(
            Map<String, Object> profile,
            Map<String, Object> privacy,
            Map<String, Object> contact,
            boolean minor,
            List<Map<String, Object>> education,
            List<Map<String, Object>> experience,
            List<Map<String, Object>> services,
            List<Map<String, Object>> achievements,
            List<Map<String, Object>> socialLinks,
            List<Map<String, Object>> photos) {
    }

    private SnapshotBuilder() {
    }

    public static Map<String, Object> build(Input in) {
        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> p = nz(in.profile());

        out.put("ownerUid", text(p, "ownerUid", 128));
        out.put("displayName", text(p, "displayName", 60));
        out.put("profileType", text(p, "profileType", 40));
        out.put("bio", text(p, "bio", 300));

        String city = text(p, "city", 60);
        if (isPublic(in.privacy(), "city", false) && !city.isEmpty()) {
            out.put("city", city);
        }
        String about = text(p, "aboutMe", 1000);
        if (isPublic(in.privacy(), "about", true) && !about.isEmpty()) {
            out.put("aboutMe", about);
        }

        if (isPublic(in.privacy(), "skills", true) && p.get("skills") instanceof List<?> raw) {
            List<String> skills = new ArrayList<>();
            for (Object item : raw) {
                if (item instanceof String s && skills.size() < 20) {
                    String cleaned = clean(s, 30);
                    if (!cleaned.isEmpty()) {
                        skills.add(cleaned);
                    }
                }
            }
            if (!skills.isEmpty()) {
                out.put("skills", skills);
            }
        }

        String photoUrl = text(p, "photoUrl", 600);
        if (photoUrl.startsWith(STORAGE_PREFIX)) {
            out.put("photoUrl", photoUrl);
        }

        put(out, "education", isPublic(in.privacy(), "education", true),
                items(in.education(), 20, "qualification", "institution", "year"));
        put(out, "experience", isPublic(in.privacy(), "experience", true),
                items(in.experience(), 20, "role", "organization", "duration", "description"));
        put(out, "services", isPublic(in.privacy(), "services", true),
                items(in.services(), 20, "name", "price", "description"));
        put(out, "achievements", isPublic(in.privacy(), "achievements", true),
                items(in.achievements(), 20, "title", "date", "description"));
        put(out, "socialLinks", true, socialLinks(in.socialLinks()));
        put(out, "gallery", true, gallery(in.photos()));

        if (!in.minor() && in.contact() != null) {
            Map<String, Object> contact = contact(in.contact());
            if (!contact.isEmpty()) {
                out.put("contact", contact);
            }
        }
        return out;
    }

    private static Map<String, Object> contact(Map<String, Object> c) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String key : List.of("phone", "whatsapp", "email", "location")) {
            if (!isPublic(c, key, false)) {
                continue;
            }
            String value = text(c, key, key.equals("location") ? 100 : 100);
            boolean ok = switch (key) {
                case "phone", "whatsapp" -> PHONE.matcher(value).matches();
                case "email" -> EMAIL.matcher(value).matches();
                default -> !value.isEmpty();
            };
            if (ok) {
                out.put(key, value);
            }
        }
        return out;
    }

    private static List<Map<String, Object>> socialLinks(List<Map<String, Object>> docs) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> d : nl(docs)) {
            if (list.size() >= 20 || !"public".equals(d.get("visibility"))) {
                continue;
            }
            String platform = text(d, "platform", 30);
            String url = text(d, "url", 300);
            if (PLATFORMS.contains(platform) && url.startsWith("https://") && url.length() > 8) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("platform", platform);
                m.put("url", url);
                list.add(m);
            }
        }
        return list;
    }

    private static List<Map<String, Object>> gallery(List<Map<String, Object>> docs) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> d : nl(docs)) {
            if (list.size() >= 12) {
                break;
            }
            String url = text(d, "url", 600);
            if (url.startsWith(STORAGE_PREFIX)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("url", url);
                m.put("caption", text(d, "caption", 100));
                list.add(m);
            }
        }
        return list;
    }

    private static List<Map<String, Object>> items(List<Map<String, Object>> docs, int max,
                                                   String... keys) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> d : nl(docs)) {
            if (list.size() >= max) {
                break;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            for (String key : keys) {
                m.put(key, text(d, key, key.equals("description") ? 500 : 100));
            }
            list.add(m);
        }
        return list;
    }

    private static void put(Map<String, Object> out, String key, boolean allowed,
                            List<Map<String, Object>> list) {
        if (allowed && !list.isEmpty()) {
            out.put(key, list);
        }
    }

    static boolean isPublic(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map == null ? null : map.get(key + "Visibility");
        if (value instanceof String s) {
            return "public".equals(s);
        }
        return defaultValue;
    }

    private static String text(Map<String, Object> map, String key, int max) {
        Object value = map.get(key);
        return value instanceof String s ? clean(s, max) : "";
    }

    public static String clean(String value, int max) {
        if (value == null) {
            return "";
        }
        String s = value.replaceAll("[\\p{Cntrl}&&[^\\n\\r\\t]]", "").trim();
        return s.length() > max ? s.substring(0, max).trim() : s;
    }

    private static Map<String, Object> nz(Map<String, Object> m) {
        return m == null ? Map.of() : m;
    }

    private static List<Map<String, Object>> nl(List<Map<String, Object>> l) {
        return l == null ? List.of() : l;
    }
}