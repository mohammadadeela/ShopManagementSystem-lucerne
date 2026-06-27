package com.lucerne.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtil {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private DateUtil() { }
    public static String format(LocalDate value) { return value == null ? "—" : DATE.format(value); }
    public static String format(LocalDateTime value) { return value == null ? "—" : DATE_TIME.format(value); }
}
