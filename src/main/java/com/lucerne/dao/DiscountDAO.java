package com.lucerne.dao;

import com.lucerne.app.AppSession;
import com.lucerne.config.DatabaseConnection;
import com.lucerne.service.AuthorizationService;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DiscountDAO {
    public List<Map<String,Object>> search(String keyword,String state,LocalDate from,LocalDate to) throws SQLException {
        StringBuilder sql=new StringBuilder("""
                SELECT DiscountID AS `Discount ID`,Code,Description,Percentage,FixedAmount AS `Fixed Amount`,
                       MinimumPurchase AS `Minimum Purchase`,MaximumDiscount AS `Maximum Discount`,
                       StartDate AS `Start Date`,EndDate AS `End Date`,IsActive AS Active,
                       CASE WHEN IsActive=0 THEN 'INACTIVE' WHEN CURDATE()<StartDate THEN 'SCHEDULED' WHEN CURDATE()>EndDate THEN 'EXPIRED' ELSE 'CURRENT' END AS Status
                FROM discounts WHERE 1=1
                """);
        List<Object> params=new ArrayList<>();
        if(keyword!=null&&!keyword.isBlank()){sql.append(" AND (Code LIKE ? OR Description LIKE ?)");params.add("%"+keyword.trim()+"%");params.add("%"+keyword.trim()+"%");}
        if("ACTIVE".equals(state)){sql.append(" AND IsActive=1");}else if("INACTIVE".equals(state)){sql.append(" AND IsActive=0");}else if("CURRENT".equals(state)){sql.append(" AND IsActive=1 AND CURDATE() BETWEEN StartDate AND EndDate");}else if("EXPIRED".equals(state)){sql.append(" AND EndDate<CURDATE()");}
        if(from!=null){sql.append(" AND EndDate>=?");params.add(from);}if(to!=null){sql.append(" AND StartDate<=?");params.add(to);}
        sql.append(" ORDER BY IsActive DESC,EndDate DESC,Code");
        return new QueryDAO().queryAll(sql.toString(),params);
    }

    public void save(Integer id,DiscountForm form) throws SQLException {
        AuthorizationService.require("MANAGE_DISCOUNTS");
        boolean creating=id==null;
        try(Connection connection=DatabaseConnection.open()){
            connection.setAutoCommit(false);
            try{
                if(creating){try(PreparedStatement statement=connection.prepareStatement("""
                        INSERT INTO discounts(Code,Description,Percentage,FixedAmount,MinimumPurchase,MaximumDiscount,StartDate,EndDate,IsActive,CreatedBy,CreatedAt)
                        VALUES(?,?,?,?,?,?,?,?,1,?,NOW())
                        """,Statement.RETURN_GENERATED_KEYS)){bind(statement,form);statement.setInt(9,AppSession.current().userId());statement.executeUpdate();try(ResultSet keys=statement.getGeneratedKeys()){keys.next();id=keys.getInt(1);}}}
                else{try(PreparedStatement statement=connection.prepareStatement("""
                        UPDATE discounts SET Code=?,Description=?,Percentage=?,FixedAmount=?,MinimumPurchase=?,MaximumDiscount=?,StartDate=?,EndDate=? WHERE DiscountID=?
                        """)){bind(statement,form);statement.setInt(9,id);statement.executeUpdate();}}
                AuthDAO.insertAudit(connection,AppSession.current().userId(),AppSession.current().username(),creating?"DISCOUNT_CREATE":"DISCOUNT_UPDATE","DISCOUNT",id,"Saved discount "+form.code(),true,AppSession.sessionIdentifier());
                connection.commit();
            }catch(Exception exception){connection.rollback();if(exception instanceof SQLException sqlException)throw sqlException;if(exception instanceof RuntimeException runtimeException)throw runtimeException;throw new SQLException(exception);}finally{connection.setAutoCommit(true);}
        }
    }

    public void setActive(int id,boolean active) throws SQLException {
        AuthorizationService.require("MANAGE_DISCOUNTS");
        try(Connection connection=DatabaseConnection.open()){
            connection.setAutoCommit(false);
            try(PreparedStatement statement=connection.prepareStatement("UPDATE discounts SET IsActive=? WHERE DiscountID=?")){
                statement.setBoolean(1,active);statement.setInt(2,id);statement.executeUpdate();
                AuthDAO.insertAudit(connection,AppSession.current().userId(),AppSession.current().username(),active?"DISCOUNT_ACTIVATE":"DISCOUNT_DEACTIVATE","DISCOUNT",id,"Changed discount active state",true,AppSession.sessionIdentifier());
                connection.commit();
            }catch(SQLException exception){connection.rollback();throw exception;}finally{connection.setAutoCommit(true);}
        }
    }

    private static void bind(PreparedStatement statement,DiscountForm form)throws SQLException{
        statement.setString(1,form.code());statement.setString(2,form.description());statement.setBigDecimal(3,form.percentage());statement.setBigDecimal(4,form.fixedAmount());statement.setBigDecimal(5,form.minimumPurchase());if(form.maximumDiscount()==null)statement.setNull(6,Types.DECIMAL);else statement.setBigDecimal(6,form.maximumDiscount());statement.setDate(7,Date.valueOf(form.startDate()));statement.setDate(8,Date.valueOf(form.endDate()));
    }

    public record DiscountForm(String code,String description,BigDecimal percentage,BigDecimal fixedAmount,BigDecimal minimumPurchase,BigDecimal maximumDiscount,LocalDate startDate,LocalDate endDate){}
}
