package com.lucerne.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import com.lucerne.util.LoggerUtil;

public final class DatabaseConnection {
    private DatabaseConnection() { }

    public static Connection open() throws SQLException {
        return DriverManager.getConnection(DatabaseConfig.jdbcUrl(), DatabaseConfig.username(), DatabaseConfig.password());
    }

    public static ConnectionStatus test() {
        long started = System.nanoTime();
        try (Connection connection = open()) {
            String version = connection.getMetaData().getDatabaseProductVersion();
            long millis = (System.nanoTime() - started) / 1_000_000;
            return new ConnectionStatus(true, "Connected", version, millis);
        } catch (SQLException exception) {
            LoggerUtil.warning(DatabaseConnection.class, "Database connection test failed", exception);
            return new ConnectionStatus(false, friendlyMessage(exception), "Unknown", 0);
        }
    }

    public static String friendlyMessage(SQLException exception) {
        return switch (exception.getSQLState() == null ? "" : exception.getSQLState()) {
            case "28000" -> "Database username or password is incorrect.";
            case "42000" -> "The database does not exist or the account has no access.";
            default -> "Cannot connect to MySQL. Check that MySQL is running and database.properties is correct.";
        };
    }

    public record ConnectionStatus(boolean connected, String message, String version, long responseMillis) { }
}
