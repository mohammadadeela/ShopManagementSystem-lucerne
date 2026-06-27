package com.lucerne.dao;

import com.lucerne.app.AppSession;
import com.lucerne.config.DatabaseConnection;
import com.lucerne.model.Role;
import com.lucerne.service.AuthorizationService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RolePermissionDAO {
    public List<RoleOption> roles() throws SQLException {
        List<RoleOption> roles = new ArrayList<>();
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT RoleID,RoleName,Description FROM roles ORDER BY FIELD(RoleName,'OWNER','ADMIN','MANAGER','CASHIER','WAREHOUSE','CUSTOMER')" );
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) roles.add(new RoleOption(rs.getInt(1), rs.getString(2), rs.getString(3)));
        }
        return roles;
    }

    public List<PermissionChoice> permissions(int roleId) throws SQLException {
        List<PermissionChoice> permissions = new ArrayList<>();
        String sql = """
                SELECT p.PermissionID,p.PermissionCode,p.Description,
                       CASE WHEN rp.PermissionID IS NULL THEN 0 ELSE 1 END Granted
                FROM permissions p
                LEFT JOIN role_permissions rp ON rp.PermissionID=p.PermissionID AND rp.RoleID=?
                ORDER BY p.PermissionCode
                """;
        try (Connection connection = DatabaseConnection.open(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, roleId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) permissions.add(new PermissionChoice(rs.getInt(1),rs.getString(2),rs.getString(3),rs.getBoolean(4)));
            }
        }
        return permissions;
    }

    public void save(RoleOption role, Set<Integer> permissionIds) throws SQLException {
        AuthorizationService.require("MANAGE_ROLES");
        Role current = AppSession.current().role();
        if (current != Role.OWNER && ("OWNER".equals(role.name()) || "ADMIN".equals(role.name()))) {
            throw new SecurityException("Only an owner can modify owner or administrator permissions.");
        }
        if (permissionIds.isEmpty()) throw new IllegalArgumentException("A role must retain at least one permission.");
        try (Connection connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement delete = connection.prepareStatement("DELETE FROM role_permissions WHERE RoleID=?")) {
                    delete.setInt(1, role.id()); delete.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO role_permissions(RoleID,PermissionID) VALUES(?,?)")) {
                    for (Integer permissionId : permissionIds) {
                        insert.setInt(1, role.id()); insert.setInt(2, permissionId); insert.addBatch();
                    }
                    insert.executeBatch();
                }
                AuthDAO.insertAudit(connection,AppSession.current().userId(),AppSession.current().username(),
                        "ROLE_PERMISSIONS_UPDATE","ROLE",role.id(),
                        "Updated permissions for role "+role.name()+" ("+permissionIds.size()+" grants)",true,
                        AppSession.sessionIdentifier());
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof SQLException sqlException) throw sqlException;
                if (exception instanceof RuntimeException runtimeException) throw runtimeException;
                throw new SQLException(exception);
            } finally { connection.setAutoCommit(true); }
        }
    }

    public record RoleOption(int id,String name,String description) {
        @Override public String toString(){return name;}
    }
    public record PermissionChoice(int id,String code,String description,boolean granted) { }
}
