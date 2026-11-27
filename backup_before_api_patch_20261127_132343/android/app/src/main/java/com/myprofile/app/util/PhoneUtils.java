package com.myprofile.app.util;

public final class PhoneUtils {

    private PhoneUtils() {
    }

    public static String normalize(String raw) {
        return raw == null ? "" : raw.replaceAll("[\\s-]", "");
    }

    public static boolean isValidCountryCode(String code) {
        return code != null && code.matches("^\\+[1-9]\\d{0,3}$");
    }

    public static boolean isValidNumber(String code, String number) {
        if (number == null) {
            return false;
        }
        if ("+91".equals(code)) {
            return number.matches("^[6-9]\\d{9}$");
        }
        return number.matches("^\\d{6,14}$");
    }

    public static String maskPhone(String e164) {
        if (e164 == null || e164.length() <= 4) {
            return "";
        }
        return "******" + e164.substring(e164.length() - 4);
    }
}
