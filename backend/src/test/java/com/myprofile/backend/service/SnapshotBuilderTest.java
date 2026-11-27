package com.myprofile.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SnapshotBuilderTest {

    private static final String GOOD_PHOTO = "https://firebasestorage.googleapis.com/v0/b/x/o/a.jpg";

    private static Map<String, Object> m(Object... kv) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return map;
    }

    private static Map<String, Object> profile() {
        return m("ownerUid", "u1", "displayName", "Test User", "profileType", "Student",
                "bio", "Hello", "city", "Pune", "aboutMe", "About me",
                "skills", List.of("Java", "SQL"), "photoUrl", GOOD_PHOTO);
    }

    private static SnapshotBuilder.Input input(Map<String, Object> privacy,
                                               Map<String, Object> contact, boolean minor) {
        return new SnapshotBuilder.Input(profile(), privacy, contact, minor,
                List.of(m("qualification", "BE", "institution", "SPPU", "year", "2026")),
                List.of(), List.of(), List.of(),
                List.of(m("platform", "GitHub", "url", "https://github.com/x", "visibility", "public"),
                        m("platform", "Instagram", "url", "https://instagram.com/x", "visibility", "private"),
                        m("platform", "Website", "url", "http://insecure.example", "visibility", "public")),
                List.of(m("url", GOOD_PHOTO, "caption", "One"),
                        m("url", "https://evil.example/x.jpg", "caption", "Bad")));
    }

    @Test
    void cityHiddenByDefaultOthersShown() {
        Map<String, Object> out = SnapshotBuilder.build(input(Map.of(), Map.of(), false));
        assertFalse(out.containsKey("city"));
        assertEquals("About me", out.get("aboutMe"));
        assertTrue(out.containsKey("skills"));
        assertTrue(out.containsKey("education"));
    }

    @Test
    void privateSectionIsOmitted() {
        Map<String, Object> out = SnapshotBuilder.build(
                input(m("educationVisibility", "private", "cityVisibility", "public"), Map.of(), false));
        assertFalse(out.containsKey("education"));
        assertEquals("Pune", out.get("city"));
    }

    @Test
    void minorNeverGetsContactEvenIfAllPublic() {
        Map<String, Object> contact = m("phone", "+919999900001", "phoneVisibility", "public",
                "email", "a@b.co", "emailVisibility", "public");
        Map<String, Object> out = SnapshotBuilder.build(input(Map.of(), contact, true));
        assertFalse(out.containsKey("contact"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void adultContactOnlyIncludesPublicValidFields() {
        Map<String, Object> contact = m(
                "phone", "+919999900001", "phoneVisibility", "public",
                "email", "a@b.co", "emailVisibility", "private",
                "whatsapp", "not-a-number", "whatsappVisibility", "public");
        Map<String, Object> out = SnapshotBuilder.build(input(Map.of(), contact, false));
        Map<String, Object> c = (Map<String, Object>) out.get("contact");
        assertEquals("+919999900001", c.get("phone"));
        assertFalse(c.containsKey("email"));
        assertFalse(c.containsKey("whatsapp"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void onlyPublicHttpsLinksAreKept() {
        Map<String, Object> out = SnapshotBuilder.build(input(Map.of(), Map.of(), false));
        List<Map<String, Object>> links = (List<Map<String, Object>>) out.get("socialLinks");
        assertEquals(1, links.size());
        assertEquals("GitHub", links.get(0).get("platform"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void photosMustComeFromFirebaseStorage() {
        Map<String, Object> out = SnapshotBuilder.build(input(Map.of(), Map.of(), false));
        assertEquals(GOOD_PHOTO, out.get("photoUrl"));
        assertEquals(1, ((List<Object>) out.get("gallery")).size());

        Map<String, Object> bad = profile();
        bad.put("photoUrl", "https://evil.example/me.jpg");
        SnapshotBuilder.Input in = new SnapshotBuilder.Input(bad, Map.of(), Map.of(), false,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        assertNull(SnapshotBuilder.build(in).get("photoUrl"));
    }

    @Test
    void cleanStripsControlCharactersAndTruncates() {
        assertEquals("ab", SnapshotBuilder.clean("a\u0007b", 10));
        assertEquals("abc", SnapshotBuilder.clean("  abcdef  ", 3));
        assertEquals("", SnapshotBuilder.clean(null, 5));
    }
}