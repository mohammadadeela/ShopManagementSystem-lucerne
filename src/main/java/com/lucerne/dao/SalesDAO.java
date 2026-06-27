package com.lucerne.dao;

import com.lucerne.app.AppSession;
import com.lucerne.config.DatabaseConnection;
import com.lucerne.model.Role;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/** Parameterized sales queries with role and branch isolation. */
public final class SalesDAO {
    public QueryDAO.QueryResult search(SalesFilter filter, int limit, int offset) throws SQLException {
        QuerySpec spec = buildWhere(filter);
        String select = """
                SELECT s.SaleID AS `Sale ID`,s.ReceiptNumber AS Receipt,s.SaleDate AS Date,
                       b.Name AS Branch,u.FullName AS Cashier,COALESCE(c.FullName,'Walk-in') AS Customer,
                       s.GrossAmount AS `Gross Sales`,s.DiscountAmount AS Discounts,s.NetAmount AS Revenue,
                       s.CostAmount AS COGS,s.GrossProfit AS `Gross Profit`,s.PaymentMethod AS Payment,s.Status
                FROM sales s
                JOIN branches b ON b.BranchID=s.BranchID
                JOIN users u ON u.UserID=s.CashierUserID
                LEFT JOIN customers c ON c.CustomerID=s.CustomerID
                """ + spec.where();
        return new QueryDAO().query(select + " ORDER BY s.SaleDate DESC,s.SaleID DESC",
                "SELECT COUNT(*) FROM sales s LEFT JOIN customers c ON c.CustomerID=s.CustomerID " + spec.where(),
                spec.parameters(), limit, offset);
    }

    public Summary summary(SalesFilter filter) throws SQLException {
        QuerySpec spec = buildWhere(filter);
        String sql = """
                SELECT COALESCE(SUM(CASE WHEN s.Status<>'CANCELLED' THEN s.NetAmount ELSE 0 END),0),
                       SUM(CASE WHEN s.Status<>'CANCELLED' THEN 1 ELSE 0 END),
                       COALESCE(AVG(CASE WHEN s.Status<>'CANCELLED' THEN s.NetAmount END),0),
                       COALESCE(SUM(CASE WHEN s.Status<>'CANCELLED' THEN s.DiscountAmount ELSE 0 END),0),
                       COALESCE(SUM(CASE WHEN s.Status<>'CANCELLED' THEN s.CostAmount ELSE 0 END),0),
                       COALESCE(SUM(CASE WHEN s.Status<>'CANCELLED' THEN s.GrossProfit ELSE 0 END),0),
                       COALESCE(SUM(CASE WHEN s.Status='CANCELLED' THEN s.NetAmount ELSE 0 END),0)
                FROM sales s LEFT JOIN customers c ON c.CustomerID=s.CustomerID
                """ + spec.where();
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            QueryDAO.bind(statement, spec.parameters());
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return new Summary(result.getBigDecimal(1), result.getInt(2), result.getBigDecimal(3),
                        result.getBigDecimal(4), result.getBigDecimal(5), result.getBigDecimal(6), result.getBigDecimal(7));
            }
        }
    }

    public List<ChartPoint> paymentSummary(SalesFilter filter) throws SQLException {
        QuerySpec spec = buildWhere(filter);
        String sql = "SELECT s.PaymentMethod,COALESCE(SUM(s.NetAmount),0) FROM sales s LEFT JOIN customers c ON c.CustomerID=s.CustomerID "
                + spec.where() + " AND s.Status<>'CANCELLED' GROUP BY s.PaymentMethod ORDER BY 2 DESC";
        List<ChartPoint> values = new ArrayList<>();
        try (Connection connection = DatabaseConnection.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            QueryDAO.bind(statement, spec.parameters());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) values.add(new ChartPoint(result.getString(1), result.getBigDecimal(2)));
            }
        }
        return values;
    }

    public SaleDetails details(long saleId) throws SQLException {
        Map<String,Object> header;
        List<Map<String,Object>> items;
        try (Connection connection = DatabaseConnection.open()) {
            String headerSql = """
                    SELECT s.SaleID,s.ReceiptNumber,s.SaleDate,b.Name Branch,u.FullName Cashier,
                           COALESCE(c.FullName,'Walk-in') Customer,c.Phone CustomerPhone,
                           s.GrossAmount,s.DiscountAmount,s.NetAmount,s.CostAmount,s.GrossProfit,
                           s.PaymentMethod,s.Status,p.PaidAmount,p.ChangeAmount,p.ReferenceNumber
                    FROM sales s JOIN branches b ON b.BranchID=s.BranchID
                    JOIN users u ON u.UserID=s.CashierUserID LEFT JOIN customers c ON c.CustomerID=s.CustomerID
                    LEFT JOIN payments p ON p.SaleID=s.SaleID WHERE s.SaleID=?
                    """;
            try (PreparedStatement statement = connection.prepareStatement(headerSql)) {
                statement.setLong(1, saleId);
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) throw new SQLException("Sale was not found.");
                    header = row(result);
                }
            }
            String itemSql = """
                    SELECT si.SaleItemID AS `Item ID`,p.Name Product,sz.SizeValue Size,co.ColorName Color,
                           si.Quantity,si.ReturnedQuantity AS Returned,si.UnitPrice AS `Unit Price`,
                           si.DiscountAmount AS Discount,si.LineTotal AS `Line Total`,si.CostAtSale AS `Cost At Sale`
                    FROM sale_items si JOIN product_variants pv ON pv.VariantID=si.VariantID
                    JOIN products p ON p.ProductID=pv.ProductID JOIN sizes sz ON sz.SizeID=pv.SizeID
                    JOIN colors co ON co.ColorID=pv.ColorID WHERE si.SaleID=? ORDER BY si.SaleItemID
                    """;
            items = query(connection, itemSql, saleId);
        }
        return new SaleDetails(header, items);
    }

    public List<QueryDAO.Option> branches() throws SQLException {
        if (fixedBranch() != null)
            return new QueryDAO().options("SELECT BranchID,Name FROM branches WHERE BranchID=?", List.of(fixedBranch()));
        return new QueryDAO().options("SELECT BranchID,Name FROM branches WHERE IsActive=1 ORDER BY Name", List.of());
    }

    public List<QueryDAO.Option> cashiers() throws SQLException {
        String sql = "SELECT DISTINCT u.UserID,u.FullName FROM users u JOIN employees e ON e.UserID=u.UserID JOIN roles r ON r.RoleID=u.RoleID WHERE r.RoleName='CASHIER'";
        List<Object> p = new ArrayList<>();
        if (fixedBranch() != null) { sql += " AND e.BranchID=?"; p.add(fixedBranch()); }
        return new QueryDAO().options(sql + " ORDER BY u.FullName", p);
    }

    public List<QueryDAO.Option> customers() throws SQLException {
        return new QueryDAO().options("SELECT CustomerID,FullName FROM customers WHERE IsActive=1 ORDER BY FullName LIMIT 2000", List.of());
    }

    private QuerySpec buildWhere(SalesFilter filter) {
        List<String> where = new ArrayList<>();
        List<Object> p = new ArrayList<>();
        where.add("1=1");
        if (filter.keyword() != null && !filter.keyword().isBlank()) {
            where.add("(s.ReceiptNumber LIKE ? OR c.FullName LIKE ? OR CAST(s.SaleID AS CHAR)=?)");
            String like = "%" + filter.keyword().trim() + "%";
            p.add(like); p.add(like); p.add(filter.keyword().trim());
        }
        if (filter.from() != null) { where.add("DATE(s.SaleDate)>=?"); p.add(filter.from()); }
        if (filter.to() != null) { where.add("DATE(s.SaleDate)<=?"); p.add(filter.to()); }
        Integer branch = fixedBranch() != null ? fixedBranch() : filter.branchId();
        if (branch != null) { where.add("s.BranchID=?"); p.add(branch); }
        if (filter.cashierId() != null) { where.add("s.CashierUserID=?"); p.add(filter.cashierId()); }
        if (filter.customerId() != null) { where.add("s.CustomerID=?"); p.add(filter.customerId()); }
        if (filter.paymentMethod() != null) { where.add("s.PaymentMethod=?"); p.add(filter.paymentMethod()); }
        if (filter.status() != null) { where.add("s.Status=?"); p.add(filter.status()); }
        if (filter.minimum() != null) { where.add("s.NetAmount>=?"); p.add(filter.minimum()); }
        if (filter.maximum() != null) { where.add("s.NetAmount<=?"); p.add(filter.maximum()); }
        if (filter.discounted() != null) where.add(filter.discounted() ? "s.DiscountAmount>0" : "s.DiscountAmount=0");
        if (AppSession.current().role() == Role.CASHIER) { where.add("s.CashierUserID=?"); p.add(AppSession.current().userId()); }
        if (AppSession.current().role() == Role.CUSTOMER && AppSession.current().customerId() != null) {
            where.add("s.CustomerID=?"); p.add(AppSession.current().customerId());
        }
        return new QuerySpec(" WHERE " + String.join(" AND ", where), List.copyOf(p));
    }

    private Integer fixedBranch() {
        Role role = AppSession.current().role();
        return (role == Role.MANAGER || role == Role.CASHIER) && !AppSession.hasPermission("VIEW_ALL_BRANCHES")
                ? AppSession.current().branchId() : null;
    }

    private static List<Map<String,Object>> query(Connection connection, String sql, Object parameter) throws SQLException {
        List<Map<String,Object>> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, parameter);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) rows.add(row(result));
            }
        }
        return rows;
    }

    private static Map<String,Object> row(ResultSet result) throws SQLException {
        LinkedHashMap<String,Object> row = new LinkedHashMap<>();
        ResultSetMetaData metadata = result.getMetaData();
        for (int i=1;i<=metadata.getColumnCount();i++) row.put(metadata.getColumnLabel(i), result.getObject(i));
        return row;
    }

    private record QuerySpec(String where, List<Object> parameters) { }
    public record SalesFilter(String keyword, LocalDate from, LocalDate to, Integer branchId, Integer cashierId,
                              Integer customerId, String paymentMethod, String status, BigDecimal minimum,
                              BigDecimal maximum, Boolean discounted) { }
    public record Summary(BigDecimal revenue, int transactions, BigDecimal average, BigDecimal discounts,
                          BigDecimal cogs, BigDecimal grossProfit, BigDecimal cancelledValue) { }
    public record ChartPoint(String label, BigDecimal value) { }
    public record SaleDetails(Map<String,Object> header, List<Map<String,Object>> items) { }
}
