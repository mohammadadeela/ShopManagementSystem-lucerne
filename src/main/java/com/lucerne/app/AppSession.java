package com.lucerne.app;

import com.lucerne.model.Role;
import com.lucerne.model.UserAccount;
import java.util.Set;

public final class AppSession {
    private static UserAccount current;
    private static String sessionIdentifier;
    private AppSession() { }

    public static void start(UserAccount user) { start(user,null); }
    public static void start(UserAccount user,String sessionId) { current = user; sessionIdentifier=sessionId; }
    public static UserAccount current() {
        if (current == null) throw new IllegalStateException("No active application session");
        return current;
    }
    public static boolean isLoggedIn() { return current != null; }
    public static boolean hasPermission(String permission) {
        return current != null && (current.role() == Role.OWNER || current.permissions().contains(permission));
    }
    public static Set<String> permissions() { return current == null ? Set.of() : current.permissions(); }
    public static String sessionIdentifier(){ return sessionIdentifier; }
    public static void clear() { current = null; sessionIdentifier=null; }
}
