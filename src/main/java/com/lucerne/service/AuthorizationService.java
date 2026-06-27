package com.lucerne.service;

import com.lucerne.app.AppSession;
import com.lucerne.model.Role;

public final class AuthorizationService {
    private AuthorizationService() { }
    public static void require(String permission) {
        if (!AppSession.hasPermission(permission)) throw new SecurityException("You are not authorized to perform this action.");
    }
    public static boolean isOwnerOrAdmin() {
        Role role = AppSession.current().role();
        return role == Role.OWNER || role == Role.ADMIN;
    }
}
