package com.myprofile.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class UsernameUtilsTest {

    @Test
    public void normalize_lowercasesAndJoinsWithHyphens() {
        assertEquals("john-doe", UsernameUtils.normalize("John Doe"));
    }

    @Test
    public void normalize_trimsAndCollapsesSeparators() {
        assertEquals("hello-world", UsernameUtils.normalize("  --Hello__World--  "));
    }

    @Test
    public void normalize_nullGivesEmpty() {
        assertEquals("", UsernameUtils.normalize(null));
    }

    @Test
    public void isValid_acceptsNormalNames() {
        assertTrue(UsernameUtils.isValid("abc"));
        assertTrue(UsernameUtils.isValid("nikita-khandare"));
    }

    @Test
    public void isValid_rejectsShortLongAndBadNames() {
        assertFalse(UsernameUtils.isValid("ab"));
        assertFalse(UsernameUtils.isValid("-abc"));
        assertFalse(UsernameUtils.isValid("has space"));
        assertFalse(UsernameUtils.isValid("a".repeat(31)));
        assertTrue(UsernameUtils.isValid("a".repeat(30)));
    }

    @Test
    public void isValid_rejectsReservedNames() {
        assertFalse(UsernameUtils.isValid("admin"));
        assertFalse(UsernameUtils.isValid("delete-account"));
    }

    @Test
    public void suggestFrom_usesTheName() {
        assertEquals("nikita-khandare", UsernameUtils.suggestFrom("Nikita Khandare"));
    }

    @Test
    public void suggestFrom_fallsBackForTinyNames() {
        assertTrue(UsernameUtils.suggestFrom("A").startsWith("profile-"));
    }

    @Test
    public void alternatives_returnsThreeValidNames() {
        List<String> list = UsernameUtils.alternatives("nikita");
        assertEquals(3, list.size());
        for (String name : list) {
            assertTrue(name.startsWith("nikita-"));
            assertTrue(UsernameUtils.isValid(name));
        }
    }
}