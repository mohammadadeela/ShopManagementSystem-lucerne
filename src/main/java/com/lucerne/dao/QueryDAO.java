package com.lucerne.dao;

import com.lucerne.config.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class QueryDAO {
    public QueryResult query(String selectSql, String countSql, List<Object> parameters, int limit, int offset) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<String> columns = new ArrayList<>();
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(selectSql + " LIMIT ? OFFSET ?")) {
            bind(statement, parameters);
            statement.setInt(parameters.size() + 1, limit);
            statement.setInt(parameters.size() + 2, offset);
            try (ResultSet rs = statement.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                for (int index = 1; index <= meta.getColumnCount(); index++) columns.add(meta.getColumnLabel(index));
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int index = 1; index <= meta.getColumnCount(); index++) row.put(columns.get(index - 1), rs.getObject(index));
                    rows.add(row);
                }
            }
        }
        int total;
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(countSql)) {
            bind(statement, parameters);
            try (ResultSet rs = statement.executeQuery()) { rs.next(); total = rs.getInt(1); }
        }
        return new QueryResult(columns, rows, total);
    }

    public List<Map<String, Object>> queryAll(String sql, List<Object> parameters) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection connection = DatabaseConnection.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet rs = statement.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i=1; i<=meta.getColumnCount(); i++) row.put(meta.getColumnLabel(i), rs.getObject(i));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    public int update(String sql, List<Object> parameters) throws SQLException {
        try (Connection connection = DatabaseConnection.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters); return statement.executeUpdate();
        }
    }

    public List<Option> options(String sql, List<Object> parameters) throws SQLException {
        List<Option> options = new ArrayList<>();
        try (Connection connection = DatabaseConnection.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) options.add(new Option(rs.getObject(1), rs.getString(2)));
            }
        }
        return options;
    }

    public Object scalar(String sql, List<Object> parameters) throws SQLException {
        try (Connection connection = DatabaseConnection.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet rs = statement.executeQuery()) { return rs.next() ? rs.getObject(1) : null; }
        }
    }

    public static void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object value = parameters.get(i);
            if (value instanceof LocalDate date) statement.setDate(i + 1, java.sql.Date.valueOf(date));
            else if (value == null) statement.setNull(i + 1, Types.NULL);
            else statement.setObject(i + 1, value);
        }
    }

    public record QueryResult(List<String> columns, List<Map<String, Object>> rows, int total) { }
    public record Option(Object id, String label) { @Override public String toString() { return label; } }
}
