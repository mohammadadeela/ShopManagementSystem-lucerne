package com.lucerne.service;

import com.lucerne.dao.AuthDAO;
import com.lucerne.model.UserAccount;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;
import com.lucerne.util.LoggerUtil;

public final class AuthenticationService {
    private final AuthDAO authDAO = new AuthDAO();
    private final SettingsService settingsService = new SettingsService();
    private String activeSessionId;

    public LoginResult login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank())
            return LoginResult.failure("Enter both username and password.");
        try {
            AuthDAO.LoginRecord record = authDAO.findByUsername(username);
            if (record == null) {
                authDAO.recordUnknownLogin(username);
                return LoginResult.failure("The username or password is incorrect.");
            }
            if (!record.active()) return LoginResult.failure("This account is inactive. Contact the owner.");
            if (record.accountExpiresAt() != null && record.accountExpiresAt().isBefore(LocalDateTime.now()))
                return LoginResult.failure("This account has expired.");
            if (record.lockedUntil() != null && record.lockedUntil().isAfter(LocalDateTime.now()))
                return LoginResult.failure("This account is temporarily locked until " + record.lockedUntil().withSecond(0).withNano(0) + ".");

            boolean valid = verifyPassword(password, record);
            if (!valid) {
                int maximumAttempts = Math.max(3, settingsService.getInt("maximum_login_attempts", 5));
                int lockMinutes = Math.max(1, settingsService.getInt("login_lock_minutes", 15));
                int attempts = authDAO.recordFailedLogin(record, maximumAttempts, lockMinutes);
                return LoginResult.failure(attempts >= maximumAttempts
                        ? "Too many failed attempts. The account is locked for " + lockMinutes + " minutes."
                        : "The username or password is incorrect. Attempt " + attempts + " of " + maximumAttempts + ".");
            }
            if ((record.passwordHash() == null || record.passwordHash().isBlank()) && record.legacyPassword() != null) {
                authDAO.migrateLegacyPassword(record.userId(), BCrypt.hashpw(password, BCrypt.gensalt(12)));
            }
            activeSessionId = UUID.randomUUID().toString();
            authDAO.recordSuccessfulLogin(record, activeSessionId);
            UserAccount user = authDAO.toUserAccount(record, authDAO.loadPermissions(record.roleId(), record.userId()));
            return LoginResult.success(user, activeSessionId);
        } catch (SQLException exception) {
            LoggerUtil.warning(AuthenticationService.class, "Login database operation failed", exception);
            return LoginResult.failure("Login could not be completed because the database is unavailable.");
        }
    }

    private boolean verifyPassword(String supplied, AuthDAO.LoginRecord record) {
        if (record.passwordHash() != null && record.passwordHash().startsWith("$2")) {
            try { return BCrypt.checkpw(supplied, record.passwordHash()); }
            catch (IllegalArgumentException ignored) { return false; }
        }
        return record.legacyPassword() != null && record.legacyPassword().equals(supplied);
    }

    public void logout(UserAccount account) {
        if (account == null || activeSessionId == null) return;
        try { authDAO.logout(account.userId(), account.username(), activeSessionId); }
        catch (SQLException exception) { LoggerUtil.warning(AuthenticationService.class, "Logout audit could not be stored", exception); }
        activeSessionId = null;
    }

    public record LoginResult(boolean successful, String message, UserAccount user, String sessionId) {
        static LoginResult success(UserAccount user, String sessionId) { return new LoginResult(true, "Welcome", user, sessionId); }
        static LoginResult failure(String message) { return new LoginResult(false, message, null, null); }
    }
}
