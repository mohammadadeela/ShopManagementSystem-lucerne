package com.lucerne.dao;

import com.lucerne.config.DatabaseConnection;
import com.lucerne.model.Role;
import com.lucerne.model.UserAccount;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public final class AuthDAO {
    public LoginRecord findByUsername(String username) throws SQLException {
        String sql = """
                SELECT u.UserID, u.Username, u.FullName, u.PasswordHash, u.LegacyPassword,
                       u.RoleID, r.RoleName, u.IsActive, u.PasswordChangeRequired,
                       u.FailedLoginCount, u.LockedUntil, u.AccountExpiresAt,
                       e.EmployeeID, e.BranchID, e.WarehouseID, c.CustomerID
                FROM users u
                JOIN roles r ON r.RoleID=u.RoleID
                LEFT JOIN employees e ON e.UserID=u.UserID
                LEFT JOIN customers c ON c.UserID=u.UserID
                WHERE u.Username=?
                LIMIT 1
                """;
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username.trim());
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return null;
                return new LoginRecord(
                        rs.getInt("UserID"), rs.getString("Username"), rs.getString("FullName"),
                        rs.getString("PasswordHash"), rs.getString("LegacyPassword"),
                        rs.getInt("RoleID"), Role.fromDatabase(rs.getString("RoleName")),
                        rs.getBoolean("IsActive"), rs.getBoolean("PasswordChangeRequired"),
                        rs.getInt("FailedLoginCount"), toDateTime(rs.getTimestamp("LockedUntil")),
                        toDateTime(rs.getTimestamp("AccountExpiresAt")),
                        nullableInt(rs, "EmployeeID"), nullableInt(rs, "CustomerID"),
                        nullableInt(rs, "BranchID"), nullableInt(rs, "WarehouseID")
                );
            }
        }
    }

    public Set<String> loadPermissions(int roleId, int userId) throws SQLException {
        String sql = """
                SELECT DISTINCT p.PermissionCode
                FROM permissions p
                LEFT JOIN role_permissions rp ON rp.PermissionID=p.PermissionID AND rp.RoleID=?
                LEFT JOIN user_permissions up ON up.PermissionID=p.PermissionID AND up.UserID=? AND up.IsGranted=1
                WHERE rp.PermissionID IS NOT NULL OR up.PermissionID IS NOT NULL
                """;
        Set<String> permissions = new HashSet<>();
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roleId); statement.setInt(2, userId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) permissions.add(rs.getString(1));
            }
        }
        return Set.copyOf(permissions);
    }

    public void recordSuccessfulLogin(LoginRecord record, String sessionId) throws SQLException {
        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE users SET FailedLoginCount=0, LockedUntil=NULL, LastLoginAt=NOW() WHERE UserID=?")) {
                    update.setInt(1, record.userId()); update.executeUpdate();
                }
                try (PreparedStatement session = connection.prepareStatement(
                        "INSERT INTO user_sessions(SessionIdentifier, UserID, LoginAt, IsActive) VALUES(?,?,NOW(),1)")) {
                    session.setString(1, sessionId); session.setInt(2, record.userId()); session.executeUpdate();
                }
                recordAttempt(connection, record.userId(), record.username(), true, "SUCCESS");
                insertAudit(connection, record.userId(), record.username(), "LOGIN_SUCCESS", "USER", record.userId(), "Successful login", true, sessionId);
                connection.commit();
            } catch (SQLException exception) { connection.rollback(); throw exception; }
            finally { connection.setAutoCommit(true); }
        }
    }

    public void recordUnknownLogin(String username) throws SQLException {
        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                recordAttempt(connection, null, username == null ? "" : username.trim(), false, "UNKNOWN_USERNAME");
                insertAudit(connection, null, username, "LOGIN_FAILURE", "USER", null,
                        "Unknown username login attempt", false, null);
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public int recordFailedLogin(LoginRecord record, int maxAttempts, int lockMinutes) throws SQLException {
        int next = record.failedLoginCount() + 1;
        String sql = next >= maxAttempts
                ? "UPDATE users SET FailedLoginCount=?, LockedUntil=DATE_ADD(NOW(), INTERVAL ? MINUTE) WHERE UserID=?"
                : "UPDATE users SET FailedLoginCount=? WHERE UserID=?";
        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, next);
                if (next >= maxAttempts) { statement.setInt(2, lockMinutes); statement.setInt(3, record.userId()); }
                else statement.setInt(2, record.userId());
                statement.executeUpdate();
                recordAttempt(connection, record.userId(), record.username(), false, next >= maxAttempts ? "ACCOUNT_LOCKED" : "INVALID_PASSWORD");
                insertAudit(connection, record.userId(), record.username(), "LOGIN_FAILURE", "USER", record.userId(), "Invalid password", false, null);
                connection.commit();
            } catch (SQLException exception) { connection.rollback(); throw exception; }
            finally { connection.setAutoCommit(true); }
        }
        return next;
    }

    public void migrateLegacyPassword(int userId, String bcryptHash) throws SQLException {
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE users SET PasswordHash=?, LegacyPassword=NULL, PasswordChangeRequired=1 WHERE UserID=?")) {
            statement.setString(1, bcryptHash); statement.setInt(2, userId); statement.executeUpdate();
        }
    }

    public void logout(int userId, String username, String sessionId) throws SQLException {
        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE user_sessions SET LogoutAt=NOW(), IsActive=0 WHERE SessionIdentifier=? AND UserID=?")) {
                    statement.setString(1, sessionId); statement.setInt(2, userId); statement.executeUpdate();
                }
                insertAudit(connection, userId, username, "LOGOUT", "USER", userId, "Session ended", true, sessionId);
                connection.commit();
            } catch (SQLException exception) { connection.rollback(); throw exception; }
            finally { connection.setAutoCommit(true); }
        }
    }

    private static void recordAttempt(Connection connection, Integer userId, String username, boolean success, String reason) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO login_attempts(UserID, UsernameAttempted, Success, FailureReason, AttemptedAt) VALUES(?,?,?,?,NOW())")) {
            if (userId == null) statement.setNull(1, Types.INTEGER); else statement.setInt(1, userId);
            statement.setString(2, username); statement.setBoolean(3, success); statement.setString(4, reason); statement.executeUpdate();
        }
    }

    public static void insertAudit(Connection connection, Integer userId, String username, String action,
                                   String entityType, Integer entityId, String description, boolean success, String sessionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO audit_logs(UserID, Username, ActionCode, EntityType, EntityID, Description,
                                       ActionAt, SessionIdentifier, Success)
                VALUES(?,?,?,?,?,?,NOW(),?,?)
                """)) {
            if (userId == null) statement.setNull(1, Types.INTEGER); else statement.setInt(1, userId);
            statement.setString(2, username); statement.setString(3, action); statement.setString(4, entityType);
            if (entityId == null) statement.setNull(5, Types.INTEGER); else statement.setInt(5, entityId);
            statement.setString(6, description); statement.setString(7, sessionId); statement.setBoolean(8, success);
            statement.executeUpdate();
        }
    }

    public UserAccount toUserAccount(LoginRecord record, Set<String> permissions) {
        return new UserAccount(record.userId(), record.username(), record.fullName(), record.role(),
                record.employeeId(), record.customerId(), record.branchId(), record.warehouseId(),
                record.active(), record.passwordChangeRequired(), LocalDateTime.now(), permissions);
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column); return rs.wasNull() ? null : value;
    }
    private static LocalDateTime toDateTime(Timestamp value) { return value == null ? null : value.toLocalDateTime(); }

    public record LoginRecord(int userId, String username, String fullName, String passwordHash,
                              String legacyPassword, int roleId, Role role, boolean active,
                              boolean passwordChangeRequired, int failedLoginCount,
                              LocalDateTime lockedUntil, LocalDateTime accountExpiresAt,
                              Integer employeeId, Integer customerId, Integer branchId, Integer warehouseId) { }
}
