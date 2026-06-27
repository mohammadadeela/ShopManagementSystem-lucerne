package com.lucerne.dao;

import com.lucerne.app.AppSession;
import com.lucerne.config.DatabaseConnection;
import com.lucerne.model.Role;
import com.lucerne.service.AuthorizationService;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Database operations for the owner/admin user-management screen. */
public final class UserAdminDAO {
    private final QueryDAO queryDAO = new QueryDAO();

    public QueryDAO.QueryResult search(UserFilter filter, int limit, int offset) throws SQLException {
        AuthorizationService.require("MANAGE_USERS");
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> parameters = new ArrayList<>();
        if (filter.keyword() != null && !filter.keyword().isBlank()) {
            where.append(" AND (u.Username LIKE ? OR u.FullName LIKE ? OR e.FullName LIKE ? OR c.FullName LIKE ?)");
            String value = "%" + filter.keyword().trim() + "%";
            parameters.add(value); parameters.add(value); parameters.add(value); parameters.add(value);
        }
        if (filter.roleName() != null) { where.append(" AND r.RoleName=?"); parameters.add(filter.roleName()); }
        if (filter.active() != null) { where.append(" AND u.IsActive=?"); parameters.add(filter.active()); }
        if (filter.locked() != null) {
            where.append(filter.locked() ? " AND u.LockedUntil>NOW()" : " AND (u.LockedUntil IS NULL OR u.LockedUntil<=NOW())");
        }
        if (filter.passwordChangeRequired() != null) {
            where.append(" AND u.PasswordChangeRequired=?"); parameters.add(filter.passwordChangeRequired());
        }
        if (filter.neverLoggedIn()) where.append(" AND u.LastLoginAt IS NULL");
        if (filter.branchId() != null) { where.append(" AND e.BranchID=?"); parameters.add(filter.branchId()); }
        if (filter.warehouseId() != null) { where.append(" AND e.WarehouseID=?"); parameters.add(filter.warehouseId()); }
        if (filter.createdFrom() != null) { where.append(" AND DATE(u.CreatedAt)>=?"); parameters.add(filter.createdFrom()); }
        if (filter.createdTo() != null) { where.append(" AND DATE(u.CreatedAt)<=?"); parameters.add(filter.createdTo()); }

        String select = """
                SELECT u.UserID AS `User ID`,u.Username,u.FullName AS `Full Name`,r.RoleName AS Role,
                       COALESCE(e.FullName,'—') AS Employee,COALESCE(c.FullName,'—') AS Customer,
                       COALESCE(b.Name,'—') AS Branch,COALESCE(w.Name,'—') AS Warehouse,
                       CASE WHEN u.IsActive=1 THEN 'ACTIVE' ELSE 'INACTIVE' END AS Status,
                       CASE WHEN u.LockedUntil>NOW() THEN 'LOCKED' ELSE 'UNLOCKED' END AS `Lock Status`,
                       u.FailedLoginCount AS `Failed Attempts`,u.PasswordChangeRequired AS `Password Change`,
                       u.AccountExpiresAt AS `Account Expires`,u.LastLoginAt AS `Last Login`,u.CreatedAt AS Created,u.UpdatedAt AS Updated
                FROM users u JOIN roles r ON r.RoleID=u.RoleID
                LEFT JOIN employees e ON e.UserID=u.UserID LEFT JOIN customers c ON c.UserID=u.UserID
                LEFT JOIN branches b ON b.BranchID=e.BranchID LEFT JOIN warehouses w ON w.WarehouseID=e.WarehouseID
                """ + where;
        return queryDAO.query(select + " ORDER BY u.CreatedAt DESC,u.Username",
                "SELECT COUNT(*) FROM (" + select + ") user_count", parameters, limit, offset);
    }

    public UserDetails find(int userId) throws SQLException {
        AuthorizationService.require("MANAGE_USERS");
        String sql = """
                SELECT u.UserID,u.Username,u.FullName,r.RoleName,u.IsActive,u.PasswordChangeRequired,
                       u.AccountExpiresAt,u.FailedLoginCount,u.LockedUntil,u.LastLoginAt,
                       e.EmployeeID,e.BranchID,e.WarehouseID,c.CustomerID
                FROM users u JOIN roles r ON r.RoleID=u.RoleID
                LEFT JOIN employees e ON e.UserID=u.UserID LEFT JOIN customers c ON c.UserID=u.UserID
                WHERE u.UserID=?
                """;
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return null;
                return new UserDetails(rs.getInt("UserID"),rs.getString("Username"),rs.getString("FullName"),
                        rs.getString("RoleName"),rs.getBoolean("IsActive"),rs.getBoolean("PasswordChangeRequired"),
                        dateTime(rs.getTimestamp("AccountExpiresAt")),rs.getInt("FailedLoginCount"),
                        dateTime(rs.getTimestamp("LockedUntil")),dateTime(rs.getTimestamp("LastLoginAt")),
                        nullableInt(rs,"EmployeeID"),nullableInt(rs,"CustomerID"),nullableInt(rs,"BranchID"),nullableInt(rs,"WarehouseID"));
            }
        }
    }

    public Summary summary() throws SQLException {
        AuthorizationService.require("MANAGE_USERS");
        String sql = """
                SELECT COUNT(*) Total,
                       SUM(IsActive=1) ActiveUsers,SUM(IsActive=0) InactiveUsers,
                       SUM(LockedUntil>NOW()) LockedUsers,SUM(LastLoginAt>=DATE_FORMAT(CURDATE(),'%Y-%m-01')) LoggedThisMonth,
                       SUM(LastLoginAt IS NULL) NeverLoggedIn,SUM(PasswordChangeRequired=1) PasswordChangeUsers
                FROM users
                """;
        try (Connection connection=DatabaseConnection.open(); PreparedStatement statement=connection.prepareStatement(sql);
             ResultSet rs=statement.executeQuery()) {
            rs.next();
            return new Summary(rs.getInt("Total"),rs.getInt("ActiveUsers"),rs.getInt("InactiveUsers"),
                    rs.getInt("LockedUsers"),rs.getInt("LoggedThisMonth"),rs.getInt("NeverLoggedIn"),
                    rs.getInt("PasswordChangeUsers"));
        }
    }

    public List<QueryDAO.Option> roles() throws SQLException {
        return queryDAO.options("SELECT RoleID,RoleName FROM roles ORDER BY FIELD(RoleName,'OWNER','ADMIN','MANAGER','CASHIER','WAREHOUSE','CUSTOMER')",List.of());
    }
    public List<QueryDAO.Option> branches() throws SQLException {
        return queryDAO.options("SELECT BranchID,Name FROM branches WHERE IsActive=1 ORDER BY Name",List.of());
    }
    public List<QueryDAO.Option> warehouses() throws SQLException {
        return queryDAO.options("SELECT WarehouseID,Name FROM warehouses WHERE IsActive=1 ORDER BY Name",List.of());
    }
    public List<QueryDAO.Option> employees() throws SQLException {
        return queryDAO.options("SELECT EmployeeID,CONCAT(FullName,' — ',JobTitle) FROM employees WHERE IsActive=1 ORDER BY FullName",List.of());
    }
    public List<QueryDAO.Option> customers() throws SQLException {
        return queryDAO.options("SELECT CustomerID,CONCAT(FullName,' — ',Phone) FROM customers WHERE IsActive=1 ORDER BY FullName LIMIT 1000",List.of());
    }
    public List<java.util.Map<String,Object>> usersByRole() throws SQLException {
        return queryDAO.queryAll("SELECT r.RoleName Role,COUNT(u.UserID) Users FROM roles r LEFT JOIN users u ON u.RoleID=r.RoleID GROUP BY r.RoleID,r.RoleName ORDER BY Users DESC",List.of());
    }

    public int create(UserForm form) throws SQLException {
        AuthorizationService.require("MANAGE_USERS");
        validateRoleAssignment(form.roleName());
        validateForm(form,true);
        try (Connection connection=DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                int userId;
                String sql="""
                        INSERT INTO users(FullName,Username,PasswordHash,RoleID,IsActive,PasswordChangeRequired,
                                          AccountExpiresAt,CreatedAt,UpdatedAt,CreatedBy,UpdatedBy)
                        SELECT ?,?,?,RoleID,?,?,?,NOW(),NOW(),?,? FROM roles WHERE RoleName=?
                        """;
                try (PreparedStatement statement=connection.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1,form.fullName().trim()); statement.setString(2,form.username().trim());
                    statement.setString(3,BCrypt.hashpw(form.password(),BCrypt.gensalt(12)));
                    statement.setBoolean(4,form.active()); statement.setBoolean(5,true);
                    if(form.accountExpiresAt()==null)statement.setNull(6,Types.TIMESTAMP);else statement.setTimestamp(6,Timestamp.valueOf(form.accountExpiresAt().atStartOfDay()));
                    statement.setInt(7,AppSession.current().userId()); statement.setInt(8,AppSession.current().userId());
                    statement.setString(9,form.roleName());
                    if(statement.executeUpdate()!=1)throw new SQLException("Unknown role.");
                    try(ResultSet keys=statement.getGeneratedKeys()){if(!keys.next())throw new SQLException("No user ID returned.");userId=keys.getInt(1);}
                }
                updateLinks(connection,userId,form);
                AuthDAO.insertAudit(connection,AppSession.current().userId(),AppSession.current().username(),
                        "USER_CREATE","USER",userId,"Created user "+form.username()+" with role "+form.roleName(),true,null);
                connection.commit(); return userId;
            } catch(Exception exception) {
                connection.rollback();
                if(exception instanceof SQLException sqlException)throw sqlException;
                throw new SQLException(exception.getMessage(),exception);
            } finally { connection.setAutoCommit(true); }
        }
    }

    public void update(UserForm form) throws SQLException {
        AuthorizationService.require("MANAGE_USERS");
        if(form.userId()==null)throw new IllegalArgumentException("User ID is required.");
        validateRoleAssignment(form.roleName()); validateForm(form,false);
        try(Connection connection=DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                CurrentState current=lockCurrentState(connection,form.userId());
                if(current==null)throw new SQLException("User no longer exists.");
                enforceOwnerRules(connection,form.userId(),current.roleName(),form.roleName(),form.active());
                String sql="""
                        UPDATE users u JOIN roles r ON r.RoleName=?
                        SET u.FullName=?,u.Username=?,u.RoleID=r.RoleID,u.IsActive=?,u.PasswordChangeRequired=?,
                            u.AccountExpiresAt=?,u.UpdatedBy=?,u.UpdatedAt=NOW()
                        WHERE u.UserID=?
                        """;
                try(PreparedStatement statement=connection.prepareStatement(sql)) {
                    statement.setString(1,form.roleName());statement.setString(2,form.fullName().trim());
                    statement.setString(3,form.username().trim());statement.setBoolean(4,form.active());
                    statement.setBoolean(5,form.forcePasswordChange());
                    if(form.accountExpiresAt()==null)statement.setNull(6,Types.TIMESTAMP);else statement.setTimestamp(6,Timestamp.valueOf(form.accountExpiresAt().atStartOfDay()));
                    statement.setInt(7,AppSession.current().userId());statement.setInt(8,form.userId());statement.executeUpdate();
                }
                updateLinks(connection,form.userId(),form);
                AuthDAO.insertAudit(connection,AppSession.current().userId(),AppSession.current().username(),
                        "USER_UPDATE","USER",form.userId(),"Updated account "+form.username()+"; role "+current.roleName()+" → "+form.roleName(),true,null);
                connection.commit();
            } catch(Exception exception) {
                connection.rollback();
                if(exception instanceof SQLException sqlException)throw sqlException;
                throw new SQLException(exception.getMessage(),exception);
            } finally {connection.setAutoCommit(true);}
        }
    }

    public void setActive(int userId, boolean active) throws SQLException {
        AuthorizationService.require("MANAGE_USERS");
        try(Connection connection=DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                CurrentState current=lockCurrentState(connection,userId);
                if(current==null)throw new SQLException("User no longer exists.");
                enforceOwnerRules(connection,userId,current.roleName(),current.roleName(),active);
                try(PreparedStatement statement=connection.prepareStatement("UPDATE users SET IsActive=?,UpdatedBy=?,UpdatedAt=NOW() WHERE UserID=?")) {
                    statement.setBoolean(1,active);statement.setInt(2,AppSession.current().userId());statement.setInt(3,userId);statement.executeUpdate();
                }
                AuthDAO.insertAudit(connection,AppSession.current().userId(),AppSession.current().username(),
                        active?"USER_ACTIVATE":"USER_DEACTIVATE","USER",userId,active?"Activated account":"Deactivated account",true,null);
                connection.commit();
            } catch(Exception e){connection.rollback();if(e instanceof SQLException x)throw x;throw new SQLException(e.getMessage(),e);}finally{connection.setAutoCommit(true);}
        }
    }

    public void lock(int userId,int minutes) throws SQLException {
        AuthorizationService.require("MANAGE_USERS");
        if(userId==AppSession.current().userId())throw new IllegalArgumentException("You cannot lock your own account.");
        try(Connection c=DatabaseConnection.open()) {c.setAutoCommit(false);try(PreparedStatement s=c.prepareStatement("UPDATE users SET LockedUntil=DATE_ADD(NOW(),INTERVAL ? MINUTE),UpdatedBy=? WHERE UserID=?")){s.setInt(1,minutes);s.setInt(2,AppSession.current().userId());s.setInt(3,userId);s.executeUpdate();AuthDAO.insertAudit(c,AppSession.current().userId(),AppSession.current().username(),"USER_LOCK","USER",userId,"Locked for "+minutes+" minutes",true,null);c.commit();}catch(SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}
    }
    public void unlock(int userId) throws SQLException {
        AuthorizationService.require("MANAGE_USERS");
        try(Connection c=DatabaseConnection.open()) {c.setAutoCommit(false);try(PreparedStatement s=c.prepareStatement("UPDATE users SET LockedUntil=NULL,FailedLoginCount=0,UpdatedBy=? WHERE UserID=?")){s.setInt(1,AppSession.current().userId());s.setInt(2,userId);s.executeUpdate();AuthDAO.insertAudit(c,AppSession.current().userId(),AppSession.current().username(),"USER_UNLOCK","USER",userId,"Unlocked account",true,null);c.commit();}catch(SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}
    }
    public void resetPassword(int userId,String password) throws SQLException {
        AuthorizationService.require("MANAGE_USERS");
        if(password==null||password.length()<8)throw new IllegalArgumentException("Password must contain at least 8 characters.");
        try(Connection c=DatabaseConnection.open()) {c.setAutoCommit(false);try(PreparedStatement s=c.prepareStatement("UPDATE users SET PasswordHash=?,LegacyPassword=NULL,PasswordChangeRequired=1,FailedLoginCount=0,LockedUntil=NULL,UpdatedBy=? WHERE UserID=?")){s.setString(1,BCrypt.hashpw(password,BCrypt.gensalt(12)));s.setInt(2,AppSession.current().userId());s.setInt(3,userId);s.executeUpdate();AuthDAO.insertAudit(c,AppSession.current().userId(),AppSession.current().username(),"PASSWORD_RESET","USER",userId,"Password reset; change required",true,null);c.commit();}catch(SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}
    }

    private void updateLinks(Connection connection,int userId,UserForm form) throws SQLException {
        try(PreparedStatement clearEmployees=connection.prepareStatement("UPDATE employees SET UserID=NULL WHERE UserID=? AND EmployeeID<>COALESCE(?,0)");
            PreparedStatement clearCustomers=connection.prepareStatement("UPDATE customers SET UserID=NULL WHERE UserID=? AND CustomerID<>COALESCE(?,0)")) {
            clearEmployees.setInt(1,userId);if(form.employeeId()==null)clearEmployees.setNull(2,Types.INTEGER);else clearEmployees.setInt(2,form.employeeId());clearEmployees.executeUpdate();
            clearCustomers.setInt(1,userId);if(form.customerId()==null)clearCustomers.setNull(2,Types.INTEGER);else clearCustomers.setInt(2,form.customerId());clearCustomers.executeUpdate();
        }
        if(form.employeeId()!=null){try(PreparedStatement link=connection.prepareStatement("UPDATE employees SET UserID=?,BranchID=?,WarehouseID=?,UpdatedAt=NOW() WHERE EmployeeID=? AND (UserID IS NULL OR UserID=?)")){link.setInt(1,userId);if(form.branchId()==null)link.setNull(2,Types.INTEGER);else link.setInt(2,form.branchId());if(form.warehouseId()==null)link.setNull(3,Types.INTEGER);else link.setInt(3,form.warehouseId());link.setInt(4,form.employeeId());link.setInt(5,userId);if(link.executeUpdate()!=1)throw new SQLException("The employee is linked to another user.");}}
        if(form.customerId()!=null){try(PreparedStatement link=connection.prepareStatement("UPDATE customers SET UserID=?,UpdatedAt=NOW() WHERE CustomerID=? AND (UserID IS NULL OR UserID=?)")){link.setInt(1,userId);link.setInt(2,form.customerId());link.setInt(3,userId);if(link.executeUpdate()!=1)throw new SQLException("The customer is linked to another user.");}}
    }
    private CurrentState lockCurrentState(Connection connection,int userId)throws SQLException{try(PreparedStatement s=connection.prepareStatement("SELECT r.RoleName,u.IsActive FROM users u JOIN roles r ON r.RoleID=u.RoleID WHERE u.UserID=? FOR UPDATE")){s.setInt(1,userId);try(ResultSet r=s.executeQuery()){return r.next()?new CurrentState(r.getString(1),r.getBoolean(2)):null;}}}
    private void enforceOwnerRules(Connection connection,int userId,String oldRole,String newRole,boolean active)throws SQLException{
        if(userId==AppSession.current().userId()&&!active)throw new IllegalArgumentException("You cannot deactivate your own account.");
        if("OWNER".equals(oldRole)&&(!"OWNER".equals(newRole)||!active)){try(PreparedStatement s=connection.prepareStatement("SELECT COUNT(*) FROM users u JOIN roles r ON r.RoleID=u.RoleID WHERE r.RoleName='OWNER' AND u.IsActive=1 AND u.UserID<>?")){s.setInt(1,userId);try(ResultSet r=s.executeQuery()){r.next();if(r.getInt(1)==0)throw new IllegalArgumentException("The last active OWNER cannot be removed or deactivated.");}}}
    }
    private void validateRoleAssignment(String roleName){Role actor=AppSession.current().role();if(actor!=Role.OWNER&&("OWNER".equals(roleName)||"ADMIN".equals(roleName)))throw new SecurityException("Only an OWNER can assign OWNER or ADMIN roles.");}
    private void validateForm(UserForm f,boolean creating){if(f.fullName()==null||f.fullName().trim().length()<2)throw new IllegalArgumentException("Full name is required.");if(f.username()==null||!f.username().trim().matches("[A-Za-z0-9._-]{3,60}"))throw new IllegalArgumentException("Username must contain 3–60 letters, digits, dots, dashes or underscores.");if(creating&&(f.password()==null||f.password().length()<8))throw new IllegalArgumentException("Password must contain at least 8 characters.");if(f.roleName()==null||f.roleName().isBlank())throw new IllegalArgumentException("Role is required.");if(f.branchId()!=null&&f.warehouseId()!=null)throw new IllegalArgumentException("An employee cannot be assigned to a branch and warehouse at the same time.");}
    private static Integer nullableInt(ResultSet rs,String c)throws SQLException{int v=rs.getInt(c);return rs.wasNull()?null:v;}
    private static LocalDateTime dateTime(Timestamp t){return t==null?null:t.toLocalDateTime();}

    private record CurrentState(String roleName,boolean active){}
    public record UserFilter(String keyword,String roleName,Boolean active,Boolean locked,Boolean passwordChangeRequired,
                             boolean neverLoggedIn,Integer branchId,Integer warehouseId,LocalDate createdFrom,LocalDate createdTo){}
    public record UserDetails(int userId,String username,String fullName,String roleName,boolean active,
                              boolean passwordChangeRequired,LocalDateTime accountExpiresAt,int failedLoginCount,
                              LocalDateTime lockedUntil,LocalDateTime lastLoginAt,Integer employeeId,Integer customerId,
                              Integer branchId,Integer warehouseId){}
    public record UserForm(Integer userId,String fullName,String username,String password,String roleName,boolean active,
                           boolean forcePasswordChange,LocalDate accountExpiresAt,Integer employeeId,Integer customerId,
                           Integer branchId,Integer warehouseId){}
    public record Summary(int total,int active,int inactive,int locked,int loggedThisMonth,int neverLoggedIn,int passwordChangeRequired){}
}
