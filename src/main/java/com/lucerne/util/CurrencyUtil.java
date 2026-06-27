package com.lucerne.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class CurrencyUtil {
    private static final NumberFormat FORMAT = NumberFormat.getCurrencyInstance(Locale.US);
    private CurrencyUtil() { }
    public static String format(BigDecimal value) { return FORMAT.format(value == null ? BigDecimal.ZERO : value); }
    public static String format(double value) { return FORMAT.format(value); }
}
