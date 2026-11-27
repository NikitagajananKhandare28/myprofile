package com.myprofile.app.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProfileTypesTest {

    @Test
    public void everyListedTypeIsValid() {
        for (String label : ProfileTypes.LABELS) {
            assertTrue(ProfileTypes.isValid(label));
        }
    }

    @Test
    public void unknownOrWrongCaseIsRejected() {
        assertFalse(ProfileTypes.isValid("student"));
        assertFalse(ProfileTypes.isValid("Wizard"));
        assertFalse(ProfileTypes.isValid(null));
    }
}