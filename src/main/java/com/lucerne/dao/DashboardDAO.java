package com.lucerne.dao;

import com.lucerne.config.DatabaseConnection;
import com.lucerne.model.Role;
import com.lucerne.model.UserAccount;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public final class DashboardDAO {
    public DashboardData load(UserAccount user, LocalDate from, LocalDate to, Integer selectedBranch) throws SQLException {
        Integer branch = switch (user.role()) {
            case MANAGER, CASHIER -> user.branchId();
            default -> selectedBranch;
        };
        if (user.role() == Role.WAREHOUSE) return warehouseDashboard(user);
        if (user.role() == Role.CUSTOMER) return customerDashboard(user);
        String branchClause = branch == null ? "" : " AND s.BranchID=?";
        List<Object> parameters = new ArrayList<>(List.of(java.sql.Date.valueOf(from), java.sql.Date.valueOf(to)));
        if (branch != null) parameters.add(branch);
        String cashierClause = user.role() == Role.CASHIER ? " AND s.CashierUserID=?" : "";
        if (user.role() == Role.CASHIER) parameters.add(user.userId());
        String filter = "s.SaleDate>=? AND s.SaleDate<DATE_ADD(?,INTERVAL 1 DAY) AND s.Status<>'CANCELLED'" + branchClause + cashierClause;

        BigDecimal revenue = decimal(scalar("SELECT COALESCE(SUM(s.NetAmount),0) FROM sales s WHERE " + filter, parameters));
        BigDecimal grossProfit = decimal(scalar("SELECT COALESCE(SUM(s.GrossProfit),0) FROM sales s WHERE " + filter, parameters));
        long transactions = number(scalar("SELECT COUNT(*) FROM sales s WHERE " + filter, parameters));
        BigDecimal average = transactions == 0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(transactions),2,java.math.RoundingMode.HALF_UP);
        BigDecimal expenses = user.role() == Role.CASHIER ? BigDecimal.ZERO : loadExpenses(from,to,branch);
        long lowStock = loadLowStock(branch);
        long pendingOrders = loadPendingOrders(branch);
        long customers = number(scalar("SELECT COUNT(*) FROM customers WHERE IsActive=1",List.of()));

        List<Metric> metrics = new ArrayList<>();
        metrics.add(new Metric(user.role()==Role.CASHIER?"My sales":"Net revenue",revenue,"Selected period"));
        metrics.add(new Metric("Transactions",BigDecimal.valueOf(transactions),"Completed and returned sales"));
        metrics.add(new Metric("Average transaction",average,"Revenue ÷ transactions"));
        metrics.add(new Metric("Gross profit",grossProfit,"Revenue minus product cost"));
        metrics.add(new Metric("Expenses",expenses,"Approved expenses"));
        metrics.add(new Metric("Low stock",BigDecimal.valueOf(lowStock),"Requires attention"));
        metrics.add(new Metric("Pending orders",BigDecimal.valueOf(pendingOrders),"Awaiting fulfillment"));
        metrics.add(new Metric("Active customers",BigDecimal.valueOf(customers),"Registered accounts"));

        List<ChartPoint> salesTrend = chart("""
                SELECT DATE(s.SaleDate) Label, COALESCE(SUM(s.NetAmount),0) Value
                FROM sales s WHERE %s GROUP BY DATE(s.SaleDate) ORDER BY DATE(s.SaleDate)
                """.formatted(filter), parameters);
        List<ChartPoint> topProducts = chart("""
                SELECT p.Name Label, SUM(si.Quantity) Value FROM sale_items si
                JOIN sales s ON s.SaleID=si.SaleID JOIN product_variants pv ON pv.VariantID=si.VariantID
                JOIN products p ON p.ProductID=pv.ProductID WHERE %s
                GROUP BY p.ProductID,p.Name ORDER BY Value DESC LIMIT 8
                """.formatted(filter), parameters);
        List<ChartPoint> paymentMethods = chart("""
                SELECT s.PaymentMethod Label, COUNT(*) Value FROM sales s WHERE %s
                GROUP BY s.PaymentMethod ORDER BY Value DESC
                """.formatted(filter), parameters);
        List<Map<String,Object>> recent = rows("""
                SELECT s.ReceiptNumber Receipt, DATE_FORMAT(s.SaleDate,'%d %b %H:%i') Date,
                       u.FullName Cashier, COALESCE(c.FullName,'Walk-in') Customer, s.NetAmount Total, s.Status
                FROM sales s JOIN users u ON u.UserID=s.CashierUserID LEFT JOIN customers c ON c.CustomerID=s.CustomerID
                WHERE %s ORDER BY s.SaleDate DESC LIMIT 10
                """.formatted(filter), parameters);
        return new DashboardData(metrics,salesTrend,topProducts,paymentMethods,recent);
    }

    private DashboardData warehouseDashboard(UserAccount user) throws SQLException {
        Integer warehouseId=user.warehouseId();
        List<Object> p=warehouseId==null?List.of():List.of(warehouseId);
        String filter=warehouseId==null?"":" WHERE wi.WarehouseID=?";
        long products=number(scalar("SELECT COUNT(DISTINCT wi.VariantID) FROM warehouse_inventory wi"+filter,p));
        long quantity=number(scalar("SELECT COALESCE(SUM(wi.Quantity),0) FROM warehouse_inventory wi"+filter,p));
        BigDecimal value=decimal(scalar("SELECT COALESCE(SUM(wi.Quantity*p.CostPrice),0) FROM warehouse_inventory wi JOIN product_variants pv ON pv.VariantID=wi.VariantID JOIN products p ON p.ProductID=pv.ProductID"+filter,p));
        long low=number(scalar("SELECT COUNT(*) FROM warehouse_inventory wi"+(warehouseId==null?" WHERE ":" WHERE wi.WarehouseID=? AND ")+"wi.Quantity<=wi.ReorderLevel",p));
        long pending=number(scalar("SELECT COUNT(*) FROM stock_requests WHERE Status='PENDING'"+(warehouseId==null?"":" AND WarehouseID=?"),p));
        List<Metric> metrics=List.of(new Metric("Warehouse products",BigDecimal.valueOf(products),"Distinct variants"),new Metric("Total quantity",BigDecimal.valueOf(quantity),"Units on hand"),new Metric("Inventory cost",value,"Cost valuation"),new Metric("Low stock",BigDecimal.valueOf(low),"At or below reorder"),new Metric("Pending requests",BigDecimal.valueOf(pending),"Awaiting review"));
        List<ChartPoint> category=chart("SELECT c.CategoryName Label,SUM(wi.Quantity) Value FROM warehouse_inventory wi JOIN product_variants pv ON pv.VariantID=wi.VariantID JOIN products p ON p.ProductID=pv.ProductID JOIN categories c ON c.CategoryID=p.CategoryID"+filter+" GROUP BY c.CategoryID,c.CategoryName ORDER BY Value DESC",p);
        List<ChartPoint> movements=chart("SELECT DATE(sm.MovementDate) Label,SUM(CASE WHEN sm.Direction='IN' THEN sm.Quantity ELSE -sm.Quantity END) Value FROM stock_movements sm WHERE sm.LocationType='WAREHOUSE'"+(warehouseId==null?"":" AND sm.LocationID=?")+" AND sm.MovementDate>=DATE_SUB(CURDATE(),INTERVAL 30 DAY) GROUP BY DATE(sm.MovementDate) ORDER BY Label",p);
        List<Map<String,Object>> recent=rows("SELECT sm.MovementDate Date,p.Name Product,sm.MovementType Type,sm.Direction,sm.Quantity,sm.ReferenceNumber Reference FROM stock_movements sm JOIN product_variants pv ON pv.VariantID=sm.VariantID JOIN products p ON p.ProductID=pv.ProductID WHERE sm.LocationType='WAREHOUSE'"+(warehouseId==null?"":" AND sm.LocationID=?")+" ORDER BY sm.MovementDate DESC LIMIT 10",p);
        return new DashboardData(metrics,movements,category,List.of(),recent);
    }

    private DashboardData customerDashboard(UserAccount user) throws SQLException {
        int customer=user.customerId()==null?-1:user.customerId(); List<Object> p=List.of(customer);
        long current=number(scalar("SELECT COUNT(*) FROM online_orders WHERE CustomerID=? AND Status IN ('PENDING','CONFIRMED','PROCESSING','READY')",p));
        long completed=number(scalar("SELECT COUNT(*) FROM online_orders WHERE CustomerID=? AND Status='DELIVERED'",p));
        long returns=number(scalar("SELECT COUNT(*) FROM return_requests WHERE CustomerID=? AND Status='PENDING'",p));
        long favorites=number(scalar("SELECT COUNT(*) FROM favorites WHERE CustomerID=?",p));
        BigDecimal spending=decimal(scalar("SELECT COALESCE(SUM(NetAmount),0) FROM sales WHERE CustomerID=? AND Status<>'CANCELLED'",p));
        List<Metric> metrics=List.of(new Metric("Current orders",BigDecimal.valueOf(current),"In progress"),new Metric("Completed orders",BigDecimal.valueOf(completed),"Delivered"),new Metric("Pending returns",BigDecimal.valueOf(returns),"Under review"),new Metric("Favorites",BigDecimal.valueOf(favorites),"Saved products"),new Metric("Lifetime spending",spending,"Completed purchases"));
        List<ChartPoint> orderStatus=chart("SELECT Status Label,COUNT(*) Value FROM online_orders WHERE CustomerID=? GROUP BY Status",p);
        List<ChartPoint> purchases=chart("SELECT DATE_FORMAT(SaleDate,'%Y-%m') Label,SUM(NetAmount) Value FROM sales WHERE CustomerID=? AND Status<>'CANCELLED' GROUP BY DATE_FORMAT(SaleDate,'%Y-%m') ORDER BY Label",p);
        List<Map<String,Object>> recent=rows("SELECT OrderNumber,OrderDate,Status,TotalAmount,DeliveryType FROM online_orders WHERE CustomerID=? ORDER BY OrderDate DESC LIMIT 10",p);
        return new DashboardData(metrics,purchases,orderStatus,List.of(),recent);
    }

    private BigDecimal loadExpenses(LocalDate from,LocalDate to,Integer branch) throws SQLException {
        List<Object> p=new ArrayList<>(List.of(java.sql.Date.valueOf(from),java.sql.Date.valueOf(to)));String scope="";if(branch!=null){scope=" AND BranchID=?";p.add(branch);}return decimal(scalar("SELECT COALESCE(SUM(Amount),0) FROM expenses WHERE ExpenseDate>=? AND ExpenseDate<=? AND Status='APPROVED'"+scope,p));
    }
    private long loadLowStock(Integer branch) throws SQLException {List<Object> p=branch==null?List.of():List.of(branch);return number(scalar("SELECT COUNT(*) FROM branch_inventory WHERE Quantity<=ReorderLevel"+(branch==null?"":" AND BranchID=?"),p));}
    private long loadPendingOrders(Integer branch) throws SQLException {List<Object> p=branch==null?List.of():List.of(branch);return number(scalar("SELECT COUNT(*) FROM online_orders WHERE Status IN ('PENDING','CONFIRMED','PROCESSING')"+(branch==null?"":" AND BranchID=?"),p));}
    private Object scalar(String sql,List<Object> parameters) throws SQLException {try(Connection c=DatabaseConnection.open();PreparedStatement s=c.prepareStatement(sql)){QueryDAO.bind(s,parameters);try(ResultSet r=s.executeQuery()){return r.next()?r.getObject(1):null;}}}
    private List<ChartPoint> chart(String sql,List<Object> parameters) throws SQLException {List<ChartPoint> result=new ArrayList<>();try(Connection c=DatabaseConnection.open();PreparedStatement s=c.prepareStatement(sql)){QueryDAO.bind(s,parameters);try(ResultSet r=s.executeQuery()){while(r.next())result.add(new ChartPoint(String.valueOf(r.getObject(1)),decimal(r.getObject(2))));}}return result;}
    private List<Map<String,Object>> rows(String sql,List<Object> parameters) throws SQLException {return new QueryDAO().queryAll(sql,parameters);}
    private static long number(Object value){return value instanceof Number n?n.longValue():0;}
    private static BigDecimal decimal(Object value){if(value instanceof BigDecimal b)return b;if(value instanceof Number n)return BigDecimal.valueOf(n.doubleValue());return BigDecimal.ZERO;}
    public record Metric(String title,BigDecimal value,String subtitle){}
    public record ChartPoint(String label,BigDecimal value){}
    public record DashboardData(List<Metric> metrics,List<ChartPoint> trend,List<ChartPoint> ranking,List<ChartPoint> distribution,List<Map<String,Object>> recent){}
}
