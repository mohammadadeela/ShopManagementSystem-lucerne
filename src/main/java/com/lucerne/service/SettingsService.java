package com.lucerne.service;

import com.lucerne.dao.QueryDAO;
import java.sql.SQLException;
import java.util.List;

public final class SettingsService {
    private final QueryDAO dao = new QueryDAO();
    public String get(String key, String fallback) {
        try {
            Object value = dao.scalar("SELECT SettingValue FROM system_settings WHERE SettingKey=?", List.of(key));
            return value == null ? fallback : value.toString();
        } catch (SQLException exception) { return fallback; }
    }
    public int getInt(String key, int fallback) {
        try { return Integer.parseInt(get(key, String.valueOf(fallback))); }
        catch (NumberFormatException exception) { return fallback; }
    }
}
