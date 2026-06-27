package com.lucerne.service;

import com.lucerne.config.DatabaseConnection;
import com.lucerne.dao.AuthDAO;
import com.lucerne.app.AppSession;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

public final class PasswordService {
    private final SettingsService settings = new SettingsService();

    public void change(int userId, String password) throws SQLException {
        validateNewPassword(password);
        String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE users SET PasswordHash=?,LegacyPassword=NULL,PasswordChangeRequired=0,UpdatedAt=NOW() WHERE UserID=?")) {
                statement.setString(1, hash);
                statement.setInt(2, userId);
                if (statement.executeUpdate() != 1) throw new SQLException("Account was not found.");
                AuthDAO.insertAudit(connection, userId, AppSession.isLoggedIn() ? AppSession.current().username() : null,
                        "PASSWORD_CHANGE", "USER", userId, "Password changed", true, null);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof SQLException sql) throw sql;
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void changeWithCurrent(int userId, String currentPassword, String newPassword) throws SQLException {
        validateNewPassword(newPassword);
        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                String hash;
                String legacy;
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT PasswordHash,LegacyPassword FROM users WHERE UserID=? AND IsActive=1 FOR UPDATE")) {
                    select.setInt(1, userId);
                    try (ResultSet result = select.executeQuery()) {
                        if (!result.next()) throw new IllegalArgumentException("The active account was not found.");
                        hash = result.getString(1);
                        legacy = result.getString(2);
                    }
                }
                boolean verified = hash != null && hash.startsWith("$2")
                        ? BCrypt.checkpw(currentPassword, hash)
                        : legacy != null && legacy.equals(currentPassword);
                if (!verified) throw new IllegalArgumentException("The current password is incorrect.");
                String replacement = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE users SET PasswordHash=?,LegacyPassword=NULL,PasswordChangeRequired=0,UpdatedAt=NOW() WHERE UserID=?")) {
                    update.setString(1, replacement);
                    update.setInt(2, userId);
                    update.executeUpdate();
                }
                AuthDAO.insertAudit(connection, userId, AppSession.current().username(), "PASSWORD_CHANGE",
                        "USER", userId, "User changed own password", true, null);
                connection.commit();
            } catch (IllegalArgumentException exception) {
                connection.rollback();
                throw exception;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private void validateNewPassword(String password) {
        int minimum = Math.max(8, settings.getInt("password_minimum_length", 8));
        if (password == null || password.length() < minimum)
            throw new IllegalArgumentException("Password must contain at least " + minimum + " characters.");
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*"))
            throw new IllegalArgumentException("Password must contain at least one letter and one number.");
    }
}
