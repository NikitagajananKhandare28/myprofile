package com.myprofile.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PhoneUtilsTest {

    @Test
    public void normalize_removesSpacesAndHyphens() {
        assertEquals("9371594416", PhoneUtils.normalize("93715 94416"));
        assertEquals("9371594416", PhoneUtils.normalize("937-159-4416"));
    }

    @Test
    public void countryCode_validation() {
        assertTrue(PhoneUtils.isValidCountryCode("+91"));
        assertFalse(PhoneUtils.isValidCountryCode("91"));
        assertFalse(PhoneUtils.isValidCountryCode("+"));
        assertFalse(PhoneUtils.isValidCountryCode(null));
    }

    @Test
    public void indianNumbers_mustBeTenDigitsStartingSixToNine() {
        assertTrue(PhoneUtils.isValidNumber("+91", "9371594416"));
        assertFalse(PhoneUtils.isValidNumber("+91", "1234567890"));
        assertFalse(PhoneUtils.isValidNumber("+91", "937159441"));
    }

    @Test
    public void otherCountries_acceptSixToFourteenDigits() {
        assertTrue(PhoneUtils.isValidNumber("+1", "6505551234"));
        assertFalse(PhoneUtils.isValidNumber("+1", "123"));
    }

    @Test
    public void maskPhone_showsLastFourOnly() {
        assertEquals("******4416", PhoneUtils.maskPhone("+919371594416"));
        assertEquals("", PhoneUtils.maskPhone(null));
    }
}