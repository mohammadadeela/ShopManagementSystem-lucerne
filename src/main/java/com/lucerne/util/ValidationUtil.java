package com.lucerne.util;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public final class ValidationUtil {
    private static final Pattern EMAIL = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9][0-9 -]{7,17}$");
    private ValidationUtil() { }
    public static boolean required(String value) { return value != null && !value.isBlank(); }
    public static boolean email(String value) { return value == null || value.isBlank() || EMAIL.matcher(value).matches(); }
    public static boolean phone(String value) { return value == null || value.isBlank() || PHONE.matcher(value).matches(); }
    public static BigDecimal positiveMoney(String value) {
        try {
            BigDecimal amount = new BigDecimal(value.trim());
            return amount.signum() >= 0 ? amount : null;
        } catch (Exception exception) { return null; }
    }
}
