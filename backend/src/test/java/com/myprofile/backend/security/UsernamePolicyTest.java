package com.myprofile.backend.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UsernamePolicyTest {

    @Test
    void acceptsNormalNames() {
        assertTrue(UsernamePolicy.isValid("nikita-khandare"));
        assertTrue(UsernamePolicy.isValid("abc"));
        assertTrue(UsernamePolicy.isValid("a".repeat(30)));
    }

    @Test
    void rejectsBadNames() {
        assertFalse(UsernamePolicy.isValid(null));
        assertFalse(UsernamePolicy.isValid("ab"));
        assertFalse(UsernamePolicy.isValid("-abc"));
        assertFalse(UsernamePolicy.isValid("Has-Caps"));
        assertFalse(UsernamePolicy.isValid("has space"));
        assertFalse(UsernamePolicy.isValid("a".repeat(31)));
    }

    @Test
    void rejectsReservedNames() {
        assertFalse(UsernamePolicy.isValid("admin"));
        assertFalse(UsernamePolicy.isValid("delete-account"));
    }
}