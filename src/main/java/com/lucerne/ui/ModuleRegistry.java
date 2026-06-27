package com.lucerne.ui;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModuleRegistry {
    private static final Map<String, ModuleDefinition> MODULES = new LinkedHashMap<>();
    static {
        add(ModuleDefinition.builder("sales","Sales & Transactions").permission("VIEW_BRANCH_SALES")
                .description("Search receipts, inspect transaction totals, returns, discounts and payment methods.")
                .sql("""
SELECT s.SaleID AS `Sale ID`, s.ReceiptNumber AS Receipt, DATE_FORMAT(s.SaleDate,'%Y-%m-%d %H:%i') AS `Sale Date`,
                       b.Name AS Branch, u.FullName AS Cashier, COALESCE(c.FullName,'Walk-in') AS Customer,
                       s.GrossAmount AS `Gross Sales`, s.DiscountAmount AS Discounts, s.NetAmount AS Revenue,
                       s.CostAmount AS COGS, s.GrossProfit AS `Gross Profit`, s.PaymentMethod AS Payment, s.Status
                       FROM sales s JOIN branches b ON b.BranchID=s.BranchID JOIN users u ON u.UserID=s.CashierUserID
                       LEFT JOIN customers c ON c.CustomerID=s.CustomerID""")
                .id("s.SaleID").search("s.ReceiptNumber","u.FullName","c.FullName").date("s.SaleDate")
                .status("s.Status","COMPLETED","CANCELLED","PARTIALLY_RETURNED","RETURNED")
                .amount("s.NetAmount").branch("s.BranchID").order("s.SaleDate DESC").build());
        add(ModuleDefinition.builder("inventory","Inventory").permission("MANAGE_INVENTORY")
                .description("Current branch and warehouse quantities with reorder and valuation information.")
                .sql("""
SELECT LocationType AS `Location Type`, LocationName AS Location, ProductID AS `Product ID`, SKU,
                       ProductName AS Product, CategoryName AS Category, SizeValue AS Size, ColorName AS Color,
                       Quantity, ReorderLevel AS `Reorder Level`, StockStatus AS Status,
                       CostValue AS `Cost Value`, RetailValue AS `Retail Value`, LastUpdated AS `Last Updated`, BranchID
                       FROM v_current_inventory""")
                .id("ProductID").search("SKU","ProductName","CategoryName","ColorName","SizeValue","LocationName")
                .date("LastUpdated").status("StockStatus","IN_STOCK","LOW_STOCK","OUT_OF_STOCK","OVERSTOCK")
                .amount("RetailValue").branch("BranchID").order("ProductName ASC").build());
        add(ModuleDefinition.builder("customers","Customers").permission("MANAGE_CUSTOMERS")
                .description("Customer profiles, contact details, registration activity and lifetime spending.")
                .sql("""
SELECT c.CustomerID AS `Customer ID`, c.FullName AS Name, c.Phone, c.Email,
                       DATE_FORMAT(c.RegisteredAt,'%Y-%m-%d') AS Registered, c.IsActive AS Active,
                       COUNT(DISTINCT s.SaleID) AS `Purchase Count`, COALESCE(SUM(CASE WHEN s.Status<>'CANCELLED' THEN s.NetAmount ELSE 0 END),0) AS `Lifetime Spending`,
                       MAX(s.SaleDate) AS `Last Purchase`
                       FROM customers c LEFT JOIN sales s ON s.CustomerID=c.CustomerID
                       GROUP BY c.CustomerID,c.FullName,c.Phone,c.Email,c.RegisteredAt,c.IsActive""")
                .id("c.CustomerID").search("c.FullName","c.Phone","c.Email").date("c.RegisteredAt")
                .status("c.IsActive","1","0").amount("COALESCE(SUM(s.NetAmount),0)").order("c.RegisteredAt DESC").build());
        add(ModuleDefinition.builder("employees","Employees").permission("MANAGE_EMPLOYEES")
                .description("Employees, assignments, job roles, salary access and account linkage.")
                .sql("""
SELECT e.EmployeeID AS `Employee ID`, e.FullName AS Name, e.Phone, e.Email, e.JobTitle AS `Job Title`,
                       r.RoleName AS Role, b.Name AS Branch, w.Name AS Warehouse, e.Salary, e.HireDate AS `Hire Date`, e.IsActive AS Active,
                       u.Username FROM employees e LEFT JOIN users u ON u.UserID=e.UserID LEFT JOIN roles r ON r.RoleID=u.RoleID
                       LEFT JOIN branches b ON b.BranchID=e.BranchID LEFT JOIN warehouses w ON w.WarehouseID=e.WarehouseID""")
                .id("e.EmployeeID").search("e.FullName","e.Phone","e.Email","e.JobTitle","u.Username")
                .date("e.HireDate").status("e.IsActive","1","0").amount("e.Salary").branch("e.BranchID").order("e.FullName ASC").build());
        add(ModuleDefinition.builder("users","Manage Users").permission("MANAGE_USERS")
                .description("Create, secure and administer accounts without deleting historical activity.")
                .sql("""
SELECT u.UserID AS `User ID`, u.Username, u.FullName AS Name, r.RoleName AS Role,
                       b.Name AS Branch, w.Name AS Warehouse, u.IsActive AS Active,
                       CASE WHEN u.LockedUntil>NOW() THEN 'LOCKED' ELSE 'UNLOCKED' END AS Status,
                       u.FailedLoginCount AS `Failed Logins`, u.PasswordChangeRequired AS `Password Change`,
                       u.LastLoginAt AS `Last Login`, u.CreatedAt AS Created
                       FROM users u JOIN roles r ON r.RoleID=u.RoleID
                       LEFT JOIN employees e ON e.UserID=u.UserID LEFT JOIN branches b ON b.BranchID=e.BranchID
                       LEFT JOIN warehouses w ON w.WarehouseID=e.WarehouseID""")
                .id("u.UserID").search("u.Username","u.FullName","r.RoleName")
                .date("u.CreatedAt").status("CASE WHEN u.LockedUntil>NOW() THEN 'LOCKED' ELSE 'UNLOCKED' END","LOCKED","UNLOCKED")
                .branch("e.BranchID").order("u.CreatedAt DESC").build());
        add(ModuleDefinition.builder("orders","Orders").permission("MANAGE_ORDERS")
                .description("Online and assisted orders, payment status, fulfillment and delivery progress.")
                .sql("""
SELECT o.OrderID AS `Order ID`, o.OrderNumber AS `Order Number`, o.OrderDate AS Date,
                       c.FullName AS Customer, b.Name AS Branch, o.OrderType AS Type, o.DeliveryType AS Delivery,
                       o.PaymentStatus AS `Payment Status`, o.Status, o.TotalAmount AS Total, o.AssignedEmployeeID AS `Assigned Employee`
                       FROM online_orders o JOIN customers c ON c.CustomerID=o.CustomerID LEFT JOIN branches b ON b.BranchID=o.BranchID""")
                .id("o.OrderID").search("o.OrderNumber","c.FullName").date("o.OrderDate")
                .status("o.Status","PENDING","CONFIRMED","PROCESSING","READY","DELIVERED","CANCELLED","RETURNED")
                .amount("o.TotalAmount").branch("o.BranchID").customer("o.CustomerID").order("o.OrderDate DESC").build());
        add(ModuleDefinition.builder("expenses","Expenses").permission("MANAGE_EXPENSES")
                .description("Operational expenses, approval workflow, categories and recurring charges.")
                .sql("""
SELECT e.ExpenseID AS `Expense ID`, e.ExpenseDate AS Date, e.Category, e.Description,
                       b.Name AS Branch, u.FullName AS `Recorded By`, e.Amount, e.PaymentMethod AS Payment,
                       e.IsRecurring AS Recurring, e.Status, e.ReceiptPath AS Receipt
                       FROM expenses e LEFT JOIN branches b ON b.BranchID=e.BranchID LEFT JOIN users u ON u.UserID=e.RecordedBy""")
                .id("e.ExpenseID").search("e.Category","e.Description","u.FullName").date("e.ExpenseDate")
                .status("e.Status","PENDING","APPROVED","REJECTED").amount("e.Amount").branch("e.BranchID").order("e.ExpenseDate DESC").build());
        add(ModuleDefinition.builder("returns","Returns & Exchanges").permission("PROCESS_RETURN")
                .description("Return and exchange requests with approval, reasons, refunds and inventory restoration.")
                .sql("""
SELECT rr.ReturnID AS `Request ID`, rr.RequestNumber AS Reference, rr.RequestDate AS Date,
                       s.ReceiptNumber AS Receipt, c.FullName AS Customer, b.Name AS Branch, rr.RequestType AS Type,
                       rr.Reason, rr.ItemCondition AS `Item Condition`, rr.RefundMethod AS `Refund Method`, rr.RefundAmount AS Amount, rr.Status
                       FROM return_requests rr JOIN sales s ON s.SaleID=rr.SaleID LEFT JOIN customers c ON c.CustomerID=rr.CustomerID
                       JOIN branches b ON b.BranchID=rr.BranchID""")
                .id("rr.ReturnID").search("rr.RequestNumber","s.ReceiptNumber","c.FullName","rr.Reason")
                .date("rr.RequestDate").status("rr.Status","PENDING","APPROVED","REJECTED","PROCESSED")
                .amount("rr.RefundAmount").branch("rr.BranchID").customer("rr.CustomerID").order("rr.RequestDate DESC").build());
        add(ModuleDefinition.builder("stock_requests","Stock Requests").permission("APPROVE_STOCK_REQUEST")
                .description("Branch requests, approval history, priorities and fulfillment progress.")
                .sql("""
SELECT sr.RequestID AS `Request ID`, sr.RequestNumber AS Reference, sr.RequestDate AS Date,
                       b.Name AS Branch, w.Name AS Warehouse, u.FullName AS Requester, sr.Priority, sr.Status,
                       COUNT(sri.RequestItemID) AS Items, SUM(sri.RequestedQuantity) AS `Requested Qty`, SUM(sri.ApprovedQuantity) AS `Approved Qty`
                       FROM stock_requests sr JOIN branches b ON b.BranchID=sr.BranchID JOIN warehouses w ON w.WarehouseID=sr.WarehouseID
                       JOIN users u ON u.UserID=sr.RequestedBy LEFT JOIN stock_request_items sri ON sri.RequestID=sr.RequestID
                       GROUP BY sr.RequestID,sr.RequestNumber,sr.RequestDate,b.Name,w.Name,u.FullName,sr.Priority,sr.Status""")
                .id("sr.RequestID").search("sr.RequestNumber","b.Name","w.Name","u.FullName")
                .date("sr.RequestDate").status("sr.Status","PENDING","APPROVED","PARTIALLY_APPROVED","REJECTED","FULFILLED","CANCELLED")
                .branch("sr.BranchID").order("sr.RequestDate DESC").build());
        add(ModuleDefinition.builder("suppliers","Suppliers").permission("MANAGE_SUPPLIERS")
                .description("Supplier contacts, active status, products, order count and purchasing totals.")
                .sql("""
SELECT s.SupplierID AS `Supplier ID`, s.SupplierCode AS Code, s.SupplierName AS Supplier,
                       s.ContactPerson AS Contact, s.Phone, s.Email, s.Address, s.IsActive AS Active,
                       COUNT(po.PurchaseOrderID) AS `Purchase Orders`, COALESCE(SUM(po.TotalCost),0) AS `Purchase Total`, s.CreatedAt AS Created
                       FROM suppliers s LEFT JOIN purchase_orders po ON po.SupplierID=s.SupplierID
                       GROUP BY s.SupplierID,s.SupplierCode,s.SupplierName,s.ContactPerson,s.Phone,s.Email,s.Address,s.IsActive,s.CreatedAt""")
                .id("s.SupplierID").search("s.SupplierCode","s.SupplierName","s.ContactPerson","s.Phone","s.Email")
                .date("s.CreatedAt").status("s.IsActive","1","0").amount("COALESCE(SUM(po.TotalCost),0)").order("s.SupplierName ASC").build());
        add(ModuleDefinition.builder("purchase_orders","Purchase Orders").permission("MANAGE_PURCHASING")
                .description("Create and track supplier purchase orders, approvals, receiving and costs.")
                .sql("""
SELECT po.PurchaseOrderID AS `PO ID`, po.PONumber AS `PO Number`, po.OrderDate AS Date,
                       s.SupplierName AS Supplier, w.Name AS Warehouse, po.ExpectedDate AS Expected,
                       po.TotalCost AS Total, po.Status, po.Notes, u.FullName AS `Created By`
                       FROM purchase_orders po JOIN suppliers s ON s.SupplierID=po.SupplierID JOIN warehouses w ON w.WarehouseID=po.WarehouseID
                       JOIN users u ON u.UserID=po.CreatedBy""")
                .id("po.PurchaseOrderID").search("po.PONumber","s.SupplierName","po.Notes")
                .date("po.OrderDate").status("po.Status","DRAFT","PENDING","APPROVED","PARTIALLY_RECEIVED","RECEIVED","CANCELLED")
                .amount("po.TotalCost").order("po.OrderDate DESC").build());
        add(ModuleDefinition.builder("daily_closing","Daily Closing").permission("CREATE_DAILY_CLOSING")
                .description("Cashier shift reconciliation, cash differences and manager approval.")
                .sql("""
SELECT dc.ClosingID AS `Closing ID`, dc.ClosingDate AS Date, b.Name AS Branch, u.FullName AS Cashier,
                       dc.ShiftStart AS `Shift Start`, dc.ShiftEnd AS `Shift End`, dc.OpeningCash AS Opening,
                       dc.ExpectedCash AS Expected, dc.ActualCash AS Actual, dc.DifferenceAmount AS Difference,
                       dc.CashSales AS `Cash Sales`, dc.CardSales AS `Card Sales`, dc.Refunds, dc.Status, dc.Notes
                       FROM daily_closings dc JOIN branches b ON b.BranchID=dc.BranchID JOIN users u ON u.UserID=dc.CashierUserID""")
                .id("dc.ClosingID").search("u.FullName","dc.Notes").date("dc.ClosingDate")
                .status("dc.Status","PENDING","APPROVED","REJECTED").amount("dc.DifferenceAmount").branch("dc.BranchID").order("dc.ClosingDate DESC").build());
        add(ModuleDefinition.builder("audit","Audit Log").permission("VIEW_AUDIT_LOG")
                .description("Immutable history of security, financial, inventory and administrative activity.")
                .sql("""
SELECT a.AuditID AS `Audit ID`, a.ActionAt AS Date, a.Username, a.ActionCode AS Action,
                       a.EntityType AS Entity, a.EntityID AS `Entity ID`, b.Name AS Branch, a.Description,
                       CASE WHEN a.Success=1 THEN 'SUCCESS' ELSE 'FAILURE' END AS Status
                       FROM audit_logs a LEFT JOIN branches b ON b.BranchID=a.BranchID""")
                .id("a.AuditID").search("a.Username","a.ActionCode","a.EntityType","a.Description")
                .date("a.ActionAt").status("CASE WHEN a.Success=1 THEN 'SUCCESS' ELSE 'FAILURE' END","SUCCESS","FAILURE")
                .branch("a.BranchID").order("a.ActionAt DESC").build());
        add(ModuleDefinition.builder("notifications","Notifications").permission("VIEW_DASHBOARD")
                .description("Operational alerts for stock, orders, returns, account security and closings.")
                .sql("""
SELECT n.NotificationID AS `Notification ID`, n.CreatedAt AS Date, n.Title, n.Message,
                       n.NotificationType AS Type, n.Priority, n.RelatedEntityType AS Entity, n.RelatedEntityID AS `Entity ID`,
                       CASE WHEN n.IsRead=1 THEN 'READ' ELSE 'UNREAD' END AS Status
                       FROM notifications n""")
                .id("n.NotificationID").search("n.Title","n.Message","n.NotificationType")
                .date("n.CreatedAt").status("CASE WHEN n.IsRead=1 THEN 'READ' ELSE 'UNREAD' END","UNREAD","READ")
                .order("n.CreatedAt DESC").build());
    }
    private ModuleRegistry(){ }
    private static void add(ModuleDefinition module){MODULES.put(module.key(),module);}
    public static ModuleDefinition get(String key){return MODULES.get(key);}
    public static Map<String,ModuleDefinition> all(){return Map.copyOf(MODULES);}
}
