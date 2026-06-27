-- Lucerne Boutique Professional Database
-- MySQL 8.0+
-- Fresh development/demo installation. Import into a test database first.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;
DROP DATABASE IF EXISTS lucerne_demo;
CREATE DATABASE lucerne_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lucerne_demo;

CREATE TABLE roles (
 RoleID INT AUTO_INCREMENT PRIMARY KEY, RoleName VARCHAR(30) NOT NULL UNIQUE, Description VARCHAR(255), IsSystemRole BOOLEAN NOT NULL DEFAULT TRUE, CreatedAt TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
CREATE TABLE permissions (
 PermissionID INT AUTO_INCREMENT PRIMARY KEY, PermissionCode VARCHAR(80) NOT NULL UNIQUE, Description VARCHAR(255)
) ENGINE=InnoDB;
CREATE TABLE role_permissions (RoleID INT NOT NULL,PermissionID INT NOT NULL,PRIMARY KEY(RoleID,PermissionID),FOREIGN KEY(RoleID) REFERENCES roles(RoleID),FOREIGN KEY(PermissionID) REFERENCES permissions(PermissionID)) ENGINE=InnoDB;
CREATE TABLE branches (BranchID INT AUTO_INCREMENT PRIMARY KEY,Name VARCHAR(100) NOT NULL UNIQUE,Location VARCHAR(255),Phone VARCHAR(30),IsActive BOOLEAN NOT NULL DEFAULT TRUE,CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) ENGINE=InnoDB;
CREATE TABLE warehouses (WarehouseID INT AUTO_INCREMENT PRIMARY KEY,Name VARCHAR(100) NOT NULL UNIQUE,Location VARCHAR(255),Phone VARCHAR(30),IsActive BOOLEAN NOT NULL DEFAULT TRUE,CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP) ENGINE=InnoDB;
CREATE TABLE users (
 UserID INT AUTO_INCREMENT PRIMARY KEY,FullName VARCHAR(120) NOT NULL,Username VARCHAR(60) NOT NULL UNIQUE,PasswordHash VARCHAR(100),LegacyPassword VARCHAR(255),RoleID INT NOT NULL,IsActive BOOLEAN NOT NULL DEFAULT TRUE,PasswordChangeRequired BOOLEAN NOT NULL DEFAULT FALSE,FailedLoginCount INT NOT NULL DEFAULT 0,LockedUntil DATETIME NULL,AccountExpiresAt DATETIME NULL,LastLoginAt DATETIME NULL,CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,CreatedBy INT NULL,UpdatedBy INT NULL,FOREIGN KEY(RoleID) REFERENCES roles(RoleID),FOREIGN KEY(CreatedBy) REFERENCES users(UserID) ON DELETE SET NULL,FOREIGN KEY(UpdatedBy) REFERENCES users(UserID) ON DELETE SET NULL,INDEX idx_users_role_active(RoleID,IsActive),INDEX idx_users_last_login(LastLoginAt)
) ENGINE=InnoDB;
CREATE TABLE user_permissions (UserID INT NOT NULL,PermissionID INT NOT NULL,IsGranted BOOLEAN NOT NULL DEFAULT TRUE,PRIMARY KEY(UserID,PermissionID),FOREIGN KEY(UserID) REFERENCES users(UserID),FOREIGN KEY(PermissionID) REFERENCES permissions(PermissionID)) ENGINE=InnoDB;
CREATE TABLE employees (
 EmployeeID INT AUTO_INCREMENT PRIMARY KEY,UserID INT NULL UNIQUE,FullName VARCHAR(120) NOT NULL,Phone VARCHAR(30),Email VARCHAR(120),JobTitle VARCHAR(100) NOT NULL,BranchID INT NULL,WarehouseID INT NULL,Salary DECIMAL(12,2) NOT NULL DEFAULT 0,HireDate DATE NOT NULL,IsActive BOOLEAN NOT NULL DEFAULT TRUE,CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,FOREIGN KEY(UserID) REFERENCES users(UserID) ON DELETE SET NULL,FOREIGN KEY(BranchID) REFERENCES branches(BranchID),FOREIGN KEY(WarehouseID) REFERENCES warehouses(WarehouseID),CHECK(Salary>=0),INDEX idx_employee_branch(BranchID,IsActive),INDEX idx_employee_warehouse(WarehouseID,IsActive)
) ENGINE=InnoDB;
CREATE TABLE customers (
 CustomerID INT AUTO_INCREMENT PRIMARY KEY,UserID INT NULL UNIQUE,FullName VARCHAR(120) NOT NULL,Phone VARCHAR(30) NOT NULL,Email VARCHAR(120),RegisteredAt DATE NOT NULL,IsActive BOOLEAN NOT NULL DEFAULT TRUE,VIPStatus VARCHAR(20) NOT NULL DEFAULT 'REGULAR',CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,FOREIGN KEY(UserID) REFERENCES users(UserID) ON DELETE SET NULL,UNIQUE KEY uq_customer_phone(Phone),INDEX idx_customer_name(FullName),INDEX idx_customer_registered(RegisteredAt)
) ENGINE=InnoDB;
CREATE TABLE categories (CategoryID INT AUTO_INCREMENT PRIMARY KEY,CategoryName VARCHAR(80) NOT NULL UNIQUE,Description VARCHAR(255),IsActive BOOLEAN NOT NULL DEFAULT TRUE,CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB;
CREATE TABLE subcategories (SubcategoryID INT AUTO_INCREMENT PRIMARY KEY,CategoryID INT NOT NULL,SubcategoryName VARCHAR(80) NOT NULL,IsActive BOOLEAN NOT NULL DEFAULT TRUE,CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(CategoryID) REFERENCES categories(CategoryID),UNIQUE KEY uq_subcategory(CategoryID,SubcategoryName)) ENGINE=InnoDB;
CREATE TABLE sizes (SizeID INT AUTO_INCREMENT PRIMARY KEY,SizeValue VARCHAR(20) NOT NULL UNIQUE,SortOrder INT NOT NULL DEFAULT 0,IsActive BOOLEAN NOT NULL DEFAULT TRUE) ENGINE=InnoDB;
CREATE TABLE colors (ColorID INT AUTO_INCREMENT PRIMARY KEY,ColorName VARCHAR(40) NOT NULL UNIQUE,HexCode VARCHAR(7),IsActive BOOLEAN NOT NULL DEFAULT TRUE) ENGINE=InnoDB;
CREATE TABLE products (
 ProductID INT AUTO_INCREMENT PRIMARY KEY,SKU VARCHAR(40) NOT NULL UNIQUE,Barcode VARCHAR(60) NULL UNIQUE,Name VARCHAR(140) NOT NULL,CategoryID INT NOT NULL,SubcategoryID INT NULL,SellingPrice DECIMAL(12,2) NOT NULL,CostPrice DECIMAL(12,2) NOT NULL,Description TEXT,Material VARCHAR(120),CareInstructions VARCHAR(255),ImagePath VARCHAR(255),ReorderLevel INT NOT NULL DEFAULT 5,IsActive BOOLEAN NOT NULL DEFAULT TRUE,CreatedBy INT NULL,UpdatedBy INT NULL,CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,FOREIGN KEY(CategoryID) REFERENCES categories(CategoryID),FOREIGN KEY(SubcategoryID) REFERENCES subcategories(SubcategoryID),FOREIGN KEY(CreatedBy) REFERENCES users(UserID) ON DELETE SET NULL,FOREIGN KEY(UpdatedBy) REFERENCES users(UserID) ON DELETE SET NULL,CHECK(SellingPrice>=0),CHECK(CostPrice>=0),CHECK(ReorderLevel>=0),INDEX idx_products_category_active(CategoryID,IsActive),INDEX idx_products_name(Name)
) ENGINE=InnoDB;
CREATE TABLE product_variants (VariantID INT AUTO_INCREMENT PRIMARY KEY,ProductID INT NOT NULL,SizeID INT NOT NULL,ColorID INT NOT NULL,VariantSKU VARCHAR(60) UNIQUE,VariantBarcode VARCHAR(60) UNIQUE,IsActive BOOLEAN NOT NULL DEFAULT TRUE,CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(ProductID) REFERENCES products(ProductID),FOREIGN KEY(SizeID) REFERENCES sizes(SizeID),FOREIGN KEY(ColorID) REFERENCES colors(ColorID),UNIQUE KEY uq_variant(ProductID,SizeID,ColorID),INDEX idx_variant_product(ProductID,IsActive)) ENGINE=InnoDB;
CREATE TABLE branch_inventory (BranchInventoryID BIGINT AUTO_INCREMENT PRIMARY KEY,BranchID INT NOT NULL,VariantID INT NOT NULL,Quantity INT NOT NULL DEFAULT 0,ReorderLevel INT NOT NULL DEFAULT 5,UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,FOREIGN KEY(BranchID) REFERENCES branches(BranchID),FOREIGN KEY(VariantID) REFERENCES product_variants(VariantID),UNIQUE KEY uq_branch_variant(BranchID,VariantID),CHECK(Quantity>=0),INDEX idx_branch_stock(BranchID,Quantity,ReorderLevel)) ENGINE=InnoDB;
CREATE TABLE warehouse_inventory (WarehouseInventoryID BIGINT AUTO_INCREMENT PRIMARY KEY,WarehouseID INT NOT NULL,VariantID INT NOT NULL,Quantity INT NOT NULL DEFAULT 0,ReorderLevel INT NOT NULL DEFAULT 10,UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,FOREIGN KEY(WarehouseID) REFERENCES warehouses(WarehouseID),FOREIGN KEY(VariantID) REFERENCES product_variants(VariantID),UNIQUE KEY uq_warehouse_variant(WarehouseID,VariantID),CHECK(Quantity>=0),INDEX idx_warehouse_stock(WarehouseID,Quantity,ReorderLevel)) ENGINE=InnoDB;
CREATE TABLE price_history (PriceHistoryID BIGINT AUTO_INCREMENT PRIMARY KEY,ProductID INT NOT NULL,OldPrice DECIMAL(12,2),NewPrice DECIMAL(12,2),ChangedBy INT NULL,ChangedAt DATETIME NOT NULL,FOREIGN KEY(ProductID) REFERENCES products(ProductID),FOREIGN KEY(ChangedBy) REFERENCES users(UserID) ON DELETE SET NULL,INDEX idx_price_product_date(ProductID,ChangedAt)) ENGINE=InnoDB;
CREATE TABLE discounts (DiscountID INT AUTO_INCREMENT PRIMARY KEY,Code VARCHAR(40) NOT NULL UNIQUE,Description VARCHAR(255),Percentage DECIMAL(5,2) DEFAULT 0,FixedAmount DECIMAL(12,2) DEFAULT 0,MinimumPurchase DECIMAL(12,2) DEFAULT 0,MaximumDiscount DECIMAL(12,2) NULL,StartDate DATE NOT NULL,EndDate DATE NOT NULL,IsActive BOOLEAN NOT NULL DEFAULT TRUE,CreatedBy INT NULL,CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(CreatedBy) REFERENCES users(UserID) ON DELETE SET NULL,CHECK(Percentage BETWEEN 0 AND 100),CHECK(EndDate>=StartDate)) ENGINE=InnoDB;
CREATE TABLE sales (
 SaleID BIGINT AUTO_INCREMENT PRIMARY KEY,ReceiptNumber VARCHAR(50) NOT NULL UNIQUE,BranchID INT NOT NULL,CustomerID INT NULL,CashierUserID INT NOT NULL,SaleDate DATETIME NOT NULL,GrossAmount DECIMAL(14,2) NOT NULL DEFAULT 0,DiscountID INT NULL,DiscountAmount DECIMAL(14,2) NOT NULL DEFAULT 0,NetAmount DECIMAL(14,2) NOT NULL DEFAULT 0,CostAmount DECIMAL(14,2) NOT NULL DEFAULT 0,GrossProfit DECIMAL(14,2) NOT NULL DEFAULT 0,PaymentMethod VARCHAR(30) NOT NULL,Status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(BranchID) REFERENCES branches(BranchID),FOREIGN KEY(CustomerID) REFERENCES customers(CustomerID),FOREIGN KEY(CashierUserID) REFERENCES users(UserID),FOREIGN KEY(DiscountID) REFERENCES discounts(DiscountID),CHECK(GrossAmount>=0),CHECK(DiscountAmount>=0),CHECK(NetAmount>=0),INDEX idx_sales_date_branch(SaleDate,BranchID),INDEX idx_sales_cashier_date(CashierUserID,SaleDate),INDEX idx_sales_customer(CustomerID,SaleDate),INDEX idx_sales_status(Status)
) ENGINE=InnoDB;
CREATE TABLE sale_items (SaleItemID BIGINT AUTO_INCREMENT PRIMARY KEY,SaleID BIGINT NOT NULL,VariantID INT NOT NULL,Quantity INT NOT NULL,UnitPrice DECIMAL(12,2) NOT NULL,DiscountAmount DECIMAL(12,2) NOT NULL DEFAULT 0,LineTotal DECIMAL(14,2) NOT NULL,CostAtSale DECIMAL(12,2) NOT NULL,ReturnedQuantity INT NOT NULL DEFAULT 0,FOREIGN KEY(SaleID) REFERENCES sales(SaleID),FOREIGN KEY(VariantID) REFERENCES product_variants(VariantID),CHECK(Quantity>0),CHECK(ReturnedQuantity BETWEEN 0 AND Quantity),INDEX idx_sale_items_sale(SaleID),INDEX idx_sale_items_variant(VariantID)) ENGINE=InnoDB;
CREATE TABLE payments (PaymentID BIGINT AUTO_INCREMENT PRIMARY KEY,SaleID BIGINT NOT NULL,PaymentMethod VARCHAR(30) NOT NULL,Amount DECIMAL(14,2) NOT NULL,PaidAmount DECIMAL(14,2) NOT NULL,ChangeAmount DECIMAL(14,2) NOT NULL DEFAULT 0,PaymentDate DATETIME NOT NULL,Status VARCHAR(20) NOT NULL,ReferenceNumber VARCHAR(80),FOREIGN KEY(SaleID) REFERENCES sales(SaleID),INDEX idx_payment_sale(SaleID),INDEX idx_payment_date(PaymentDate)) ENGINE=InnoDB;
CREATE TABLE cash_drawer_movements (MovementID BIGINT AUTO_INCREMENT PRIMARY KEY,CashierUserID INT NOT NULL,BranchID INT NOT NULL,MovementType VARCHAR(30) NOT NULL,Amount DECIMAL(14,2) NOT NULL,MovementDate DATETIME NOT NULL,ReferenceNumber VARCHAR(80),Notes VARCHAR(255),FOREIGN KEY(CashierUserID) REFERENCES users(UserID),FOREIGN KEY(BranchID) REFERENCES branches(BranchID),INDEX idx_cashier_movement(CashierUserID,MovementDate)) ENGINE=InnoDB;
CREATE TABLE online_orders (OrderID BIGINT AUTO_INCREMENT PRIMARY KEY,OrderNumber VARCHAR(50) NOT NULL UNIQUE,CustomerID INT NOT NULL,BranchID INT NULL,OrderDate DATETIME NOT NULL,OrderType VARCHAR(20) NOT NULL DEFAULT 'ONLINE',DeliveryType VARCHAR(20) NOT NULL,DeliveryAddress VARCHAR(255),PaymentStatus VARCHAR(25) NOT NULL DEFAULT 'PENDING',Status VARCHAR(30) NOT NULL DEFAULT 'PENDING',Subtotal DECIMAL(14,2) NOT NULL DEFAULT 0,DiscountAmount DECIMAL(14,2) NOT NULL DEFAULT 0,DeliveryFee DECIMAL(12,2) NOT NULL DEFAULT 0,TotalAmount DECIMAL(14,2) NOT NULL DEFAULT 0,AssignedEmployeeID INT NULL,Notes VARCHAR(255),CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,FOREIGN KEY(CustomerID) REFERENCES customers(CustomerID),FOREIGN KEY(BranchID) REFERENCES branches(BranchID),FOREIGN KEY(AssignedEmployeeID) REFERENCES employees(EmployeeID),INDEX idx_orders_date_status(OrderDate,Status),INDEX idx_orders_branch(BranchID,Status),INDEX idx_orders_customer(CustomerID,OrderDate)) ENGINE=InnoDB;
CREATE TABLE order_items (OrderItemID BIGINT AUTO_INCREMENT PRIMARY KEY,OrderID BIGINT NOT NULL,VariantID INT NOT NULL,Quantity INT NOT NULL,UnitPrice DECIMAL(12,2) NOT NULL,LineTotal DECIMAL(14,2) NOT NULL,FOREIGN KEY(OrderID) REFERENCES online_orders(OrderID),FOREIGN KEY(VariantID) REFERENCES product_variants(VariantID),CHECK(Quantity>0),INDEX idx_order_items_order(OrderID)) ENGINE=InnoDB;
CREATE TABLE order_status_history (HistoryID BIGINT AUTO_INCREMENT PRIMARY KEY,OrderID BIGINT NOT NULL,Status VARCHAR(30) NOT NULL,Notes VARCHAR(255),ChangedBy INT NULL,ChangedAt DATETIME NOT NULL,FOREIGN KEY(OrderID) REFERENCES online_orders(OrderID),FOREIGN KEY(ChangedBy) REFERENCES users(UserID) ON DELETE SET NULL,INDEX idx_order_history(OrderID,ChangedAt)) ENGINE=InnoDB;
CREATE TABLE return_requests (ReturnID BIGINT AUTO_INCREMENT PRIMARY KEY,RequestNumber VARCHAR(50) NOT NULL UNIQUE,SaleID BIGINT NOT NULL,CustomerID INT NULL,BranchID INT NOT NULL,RequestType VARCHAR(20) NOT NULL,RequestDate DATETIME NOT NULL,Reason VARCHAR(255) NOT NULL,ItemCondition VARCHAR(80),RefundMethod VARCHAR(30),RefundAmount DECIMAL(14,2) NOT NULL DEFAULT 0,Status VARCHAR(30) NOT NULL DEFAULT 'PENDING',RequestedBy INT NULL,ApprovedBy INT NULL,ProcessedAt DATETIME NULL,FOREIGN KEY(SaleID) REFERENCES sales(SaleID),FOREIGN KEY(CustomerID) REFERENCES customers(CustomerID),FOREIGN KEY(BranchID) REFERENCES branches(BranchID),FOREIGN KEY(RequestedBy) REFERENCES users(UserID) ON DELETE SET NULL,FOREIGN KEY(ApprovedBy) REFERENCES users(UserID) ON DELETE SET NULL,INDEX idx_returns_date_status(RequestDate,Status),INDEX idx_returns_branch(BranchID,Status)) ENGINE=InnoDB;
CREATE TABLE return_items (ReturnItemID BIGINT AUTO_INCREMENT PRIMARY KEY,ReturnID BIGINT NOT NULL,SaleItemID BIGINT NOT NULL,Quantity INT NOT NULL,RefundAmount DECIMAL(14,2) NOT NULL,RestoreInventory BOOLEAN NOT NULL DEFAULT TRUE,FOREIGN KEY(ReturnID) REFERENCES return_requests(ReturnID),FOREIGN KEY(SaleItemID) REFERENCES sale_items(SaleItemID),CHECK(Quantity>0)) ENGINE=InnoDB;
CREATE TABLE expenses (ExpenseID BIGINT AUTO_INCREMENT PRIMARY KEY,ExpenseDate DATE NOT NULL,Category VARCHAR(80) NOT NULL,Description VARCHAR(255) NOT NULL,BranchID INT NULL,Amount DECIMAL(14,2) NOT NULL,PaymentMethod VARCHAR(30),IsRecurring BOOLEAN NOT NULL DEFAULT FALSE,Status VARCHAR(20) NOT NULL DEFAULT 'PENDING',ReceiptPath VARCHAR(255),RecordedBy INT NOT NULL,ApprovedBy INT NULL,ApprovedAt DATETIME NULL,CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,FOREIGN KEY(BranchID) REFERENCES branches(BranchID),FOREIGN KEY(RecordedBy) REFERENCES users(UserID),FOREIGN KEY(ApprovedBy) REFERENCES users(UserID) ON DELETE SET NULL,CHECK(Amount>=0),INDEX idx_expense_date_branch(ExpenseDate,BranchID),INDEX idx_expense_status(Status)) ENGINE=InnoDB;
CREATE TABLE suppliers (SupplierID INT AUTO_INCREMENT PRIMARY KEY,SupplierCode VARCHAR(30) NOT NULL UNIQUE,SupplierName VARCHAR(120) NOT NULL,ContactPerson VARCHAR(120),Phone VARCHAR(30),Email VARCHAR(120),Address VARCHAR(255),Notes VARCHAR(255),IsActive BOOLEAN NOT NULL DEFAULT TRUE,CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,INDEX idx_supplier_name(SupplierName)) ENGINE=InnoDB;
CREATE TABLE supplier_products (SupplierID INT NOT NULL,ProductID INT NOT NULL,SupplierSKU VARCHAR(60),LastCost DECIMAL(12,2),LeadTimeDays INT,PRIMARY KEY(SupplierID,ProductID),FOREIGN KEY(SupplierID) REFERENCES suppliers(SupplierID),FOREIGN KEY(ProductID) REFERENCES products(ProductID)) ENGINE=InnoDB;
CREATE TABLE purchase_orders (PurchaseOrderID BIGINT AUTO_INCREMENT PRIMARY KEY,PONumber VARCHAR(50) NOT NULL UNIQUE,SupplierID INT NOT NULL,WarehouseID INT NOT NULL,OrderDate DATE NOT NULL,ExpectedDate DATE NULL,TotalCost DECIMAL(14,2) NOT NULL DEFAULT 0,Status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',Notes VARCHAR(255),CreatedBy INT NOT NULL,ApprovedBy INT NULL,CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,FOREIGN KEY(SupplierID) REFERENCES suppliers(SupplierID),FOREIGN KEY(WarehouseID) REFERENCES warehouses(WarehouseID),FOREIGN KEY(CreatedBy) REFERENCES users(UserID),FOREIGN KEY(ApprovedBy) REFERENCES users(UserID) ON DELETE SET NULL,INDEX idx_po_date_status(OrderDate,Status)) ENGINE=InnoDB;
CREATE TABLE purchase_order_items (PurchaseOrderItemID BIGINT AUTO_INCREMENT PRIMARY KEY,PurchaseOrderID BIGINT NOT NULL,VariantID INT NOT NULL,OrderedQuantity INT NOT NULL,ReceivedQuantity INT NOT NULL DEFAULT 0,UnitCost DECIMAL(12,2) NOT NULL,LineTotal DECIMAL(14,2) NOT NULL,FOREIGN KEY(PurchaseOrderID) REFERENCES purchase_orders(PurchaseOrderID),FOREIGN KEY(VariantID) REFERENCES product_variants(VariantID),CHECK(OrderedQuantity>0),CHECK(ReceivedQuantity>=0)) ENGINE=InnoDB;
CREATE TABLE goods_receipts (ReceiptID BIGINT AUTO_INCREMENT PRIMARY KEY,ReceiptNumber VARCHAR(50) NOT NULL UNIQUE,PurchaseOrderID BIGINT NOT NULL,WarehouseID INT NOT NULL,ReceivedDate DATETIME NOT NULL,ReceivedBy INT NOT NULL,Notes VARCHAR(255),FOREIGN KEY(PurchaseOrderID) REFERENCES purchase_orders(PurchaseOrderID),FOREIGN KEY(WarehouseID) REFERENCES warehouses(WarehouseID),FOREIGN KEY(ReceivedBy) REFERENCES users(UserID)) ENGINE=InnoDB;
CREATE TABLE goods_receipt_items (ReceiptItemID BIGINT AUTO_INCREMENT PRIMARY KEY,ReceiptID BIGINT NOT NULL,VariantID INT NOT NULL,ReceivedQuantity INT NOT NULL,DamagedQuantity INT NOT NULL DEFAULT 0,UnitCost DECIMAL(12,2) NOT NULL,FOREIGN KEY(ReceiptID) REFERENCES goods_receipts(ReceiptID),FOREIGN KEY(VariantID) REFERENCES product_variants(VariantID),CHECK(ReceivedQuantity>=0),CHECK(DamagedQuantity>=0)) ENGINE=InnoDB;
CREATE TABLE stock_requests (RequestID BIGINT AUTO_INCREMENT PRIMARY KEY,RequestNumber VARCHAR(50) NOT NULL UNIQUE,BranchID INT NOT NULL,WarehouseID INT NOT NULL,RequestedBy INT NOT NULL,RequestDate DATETIME NOT NULL,Priority VARCHAR(15) NOT NULL DEFAULT 'NORMAL',Status VARCHAR(30) NOT NULL DEFAULT 'PENDING',ApprovedBy INT NULL,ApprovedAt DATETIME NULL,Notes VARCHAR(255),FOREIGN KEY(BranchID) REFERENCES branches(BranchID),FOREIGN KEY(WarehouseID) REFERENCES warehouses(WarehouseID),FOREIGN KEY(RequestedBy) REFERENCES users(UserID),FOREIGN KEY(ApprovedBy) REFERENCES users(UserID) ON DELETE SET NULL,INDEX idx_request_date_status(RequestDate,Status),INDEX idx_request_branch(BranchID,Status)) ENGINE=InnoDB;
CREATE TABLE stock_request_items (RequestItemID BIGINT AUTO_INCREMENT PRIMARY KEY,RequestID BIGINT NOT NULL,VariantID INT NOT NULL,RequestedQuantity INT NOT NULL,ApprovedQuantity INT NOT NULL DEFAULT 0,FulfilledQuantity INT NOT NULL DEFAULT 0,FOREIGN KEY(RequestID) REFERENCES stock_requests(RequestID),FOREIGN KEY(VariantID) REFERENCES product_variants(VariantID),CHECK(RequestedQuantity>0),CHECK(ApprovedQuantity>=0),CHECK(FulfilledQuantity>=0)) ENGINE=InnoDB;
CREATE TABLE stock_transfers (TransferID BIGINT AUTO_INCREMENT PRIMARY KEY,TransferNumber VARCHAR(50) NOT NULL UNIQUE,FromLocationType VARCHAR(20) NOT NULL,FromLocationID INT NOT NULL,ToLocationType VARCHAR(20) NOT NULL,ToLocationID INT NOT NULL,TransferDate DATETIME NOT NULL,Status VARCHAR(30) NOT NULL,CreatedBy INT NOT NULL,ApprovedBy INT NULL,Notes VARCHAR(255),FOREIGN KEY(CreatedBy) REFERENCES users(UserID),FOREIGN KEY(ApprovedBy) REFERENCES users(UserID) ON DELETE SET NULL,INDEX idx_transfer_date(TransferDate,Status)) ENGINE=InnoDB;
CREATE TABLE stock_transfer_items (TransferItemID BIGINT AUTO_INCREMENT PRIMARY KEY,TransferID BIGINT NOT NULL,VariantID INT NOT NULL,Quantity INT NOT NULL,FOREIGN KEY(TransferID) REFERENCES stock_transfers(TransferID),FOREIGN KEY(VariantID) REFERENCES product_variants(VariantID),CHECK(Quantity>0)) ENGINE=InnoDB;
CREATE TABLE stock_movements (StockMovementID BIGINT AUTO_INCREMENT PRIMARY KEY,LocationType VARCHAR(20) NOT NULL,LocationID INT NOT NULL,VariantID INT NOT NULL,MovementType VARCHAR(30) NOT NULL,Direction VARCHAR(5) NOT NULL,Quantity INT NOT NULL,ReferenceType VARCHAR(30),ReferenceID BIGINT,ReferenceNumber VARCHAR(80),MovementDate DATETIME NOT NULL,PerformedBy INT NULL,Notes VARCHAR(255),FOREIGN KEY(VariantID) REFERENCES product_variants(VariantID),FOREIGN KEY(PerformedBy) REFERENCES users(UserID) ON DELETE SET NULL,CHECK(Quantity>0),INDEX idx_stock_movement_date(MovementDate),INDEX idx_stock_movement_location(LocationType,LocationID,MovementDate),INDEX idx_stock_movement_variant(VariantID,MovementDate)) ENGINE=InnoDB;
CREATE TABLE daily_closings (ClosingID BIGINT AUTO_INCREMENT PRIMARY KEY,ClosingDate DATE NOT NULL,BranchID INT NOT NULL,CashierUserID INT NOT NULL,ShiftStart DATETIME NOT NULL,ShiftEnd DATETIME NOT NULL,OpeningCash DECIMAL(14,2) NOT NULL,ExpectedCash DECIMAL(14,2) NOT NULL,ActualCash DECIMAL(14,2) NOT NULL,DifferenceAmount DECIMAL(14,2) NOT NULL,CashSales DECIMAL(14,2) NOT NULL DEFAULT 0,CardSales DECIMAL(14,2) NOT NULL DEFAULT 0,Refunds DECIMAL(14,2) NOT NULL DEFAULT 0,Status VARCHAR(20) NOT NULL DEFAULT 'PENDING',Notes VARCHAR(255),ApprovedBy INT NULL,ApprovedAt DATETIME NULL,CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(BranchID) REFERENCES branches(BranchID),FOREIGN KEY(CashierUserID) REFERENCES users(UserID),FOREIGN KEY(ApprovedBy) REFERENCES users(UserID) ON DELETE SET NULL,UNIQUE KEY uq_closing_shift(CashierUserID,ClosingDate,ShiftStart),INDEX idx_closing_date_branch(ClosingDate,BranchID)) ENGINE=InnoDB;
CREATE TABLE damaged_stock (DamagedID BIGINT AUTO_INCREMENT PRIMARY KEY,LocationType VARCHAR(20),LocationID INT,VariantID INT NOT NULL,Quantity INT NOT NULL,Reason VARCHAR(255),RecordedBy INT NOT NULL,RecordedAt DATETIME NOT NULL,FOREIGN KEY(VariantID) REFERENCES product_variants(VariantID),FOREIGN KEY(RecordedBy) REFERENCES users(UserID)) ENGINE=InnoDB;
CREATE TABLE favorites (FavoriteID BIGINT AUTO_INCREMENT PRIMARY KEY,CustomerID INT NOT NULL,ProductID INT NOT NULL,CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,FOREIGN KEY(CustomerID) REFERENCES customers(CustomerID),FOREIGN KEY(ProductID) REFERENCES products(ProductID),UNIQUE KEY uq_favorite(CustomerID,ProductID)) ENGINE=InnoDB;
CREATE TABLE notifications (NotificationID BIGINT AUTO_INCREMENT PRIMARY KEY,UserID INT NULL,Title VARCHAR(140) NOT NULL,Message VARCHAR(500) NOT NULL,NotificationType VARCHAR(50) NOT NULL,Priority VARCHAR(15) NOT NULL DEFAULT 'NORMAL',RelatedEntityType VARCHAR(50),RelatedEntityID BIGINT,IsRead BOOLEAN NOT NULL DEFAULT FALSE,CreatedAt DATETIME NOT NULL,ReadAt DATETIME NULL,FOREIGN KEY(UserID) REFERENCES users(UserID) ON DELETE CASCADE,INDEX idx_notification_user_read(UserID,IsRead,CreatedAt)) ENGINE=InnoDB;
CREATE TABLE system_settings (SettingKey VARCHAR(80) PRIMARY KEY,SettingValue TEXT NOT NULL,Description VARCHAR(255),UpdatedBy INT NULL,UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,FOREIGN KEY(UpdatedBy) REFERENCES users(UserID) ON DELETE SET NULL) ENGINE=InnoDB;
CREATE TABLE user_sessions (SessionID BIGINT AUTO_INCREMENT PRIMARY KEY,SessionIdentifier VARCHAR(80) NOT NULL UNIQUE,UserID INT NOT NULL,LoginAt DATETIME NOT NULL,LogoutAt DATETIME NULL,IsActive BOOLEAN NOT NULL DEFAULT TRUE,FOREIGN KEY(UserID) REFERENCES users(UserID),INDEX idx_session_user_active(UserID,IsActive)) ENGINE=InnoDB;
CREATE TABLE login_attempts (AttemptID BIGINT AUTO_INCREMENT PRIMARY KEY,UserID INT NULL,UsernameAttempted VARCHAR(60),Success BOOLEAN NOT NULL,FailureReason VARCHAR(120),AttemptedAt DATETIME NOT NULL,FOREIGN KEY(UserID) REFERENCES users(UserID) ON DELETE SET NULL,INDEX idx_login_attempt_date(AttemptedAt),INDEX idx_login_attempt_user(UserID,AttemptedAt)) ENGINE=InnoDB;
CREATE TABLE audit_logs (AuditID BIGINT AUTO_INCREMENT PRIMARY KEY,UserID INT NULL,Username VARCHAR(60),ActionCode VARCHAR(80) NOT NULL,EntityType VARCHAR(50),EntityID BIGINT,OldValue JSON NULL,NewValue JSON NULL,Description VARCHAR(500),ActionAt DATETIME NOT NULL,BranchID INT NULL,SessionIdentifier VARCHAR(80),Success BOOLEAN NOT NULL DEFAULT TRUE,FOREIGN KEY(UserID) REFERENCES users(UserID) ON DELETE SET NULL,FOREIGN KEY(BranchID) REFERENCES branches(BranchID) ON DELETE SET NULL,INDEX idx_audit_date(ActionAt),INDEX idx_audit_user(UserID,ActionAt),INDEX idx_audit_entity(EntityType,EntityID)) ENGINE=InnoDB;
CREATE TABLE backup_history (BackupID BIGINT AUTO_INCREMENT PRIMARY KEY,BackupPath VARCHAR(500) NOT NULL,BackupType VARCHAR(20) NOT NULL,Status VARCHAR(20) NOT NULL,ErrorMessage VARCHAR(500),CreatedBy INT NULL,StartedAt DATETIME NOT NULL,CompletedAt DATETIME NULL,FOREIGN KEY(CreatedBy) REFERENCES users(UserID) ON DELETE SET NULL,INDEX idx_backup_status_date(Status,CompletedAt)) ENGINE=InnoDB;

-- Safe views
CREATE VIEW v_current_inventory AS
SELECT 'BRANCH' LocationType,b.Name LocationName,p.ProductID,pv.VariantID,p.SKU,p.Name ProductName,c.CategoryName,sz.SizeValue,co.ColorName,bi.Quantity,bi.ReorderLevel,
 CASE WHEN bi.Quantity=0 THEN 'OUT_OF_STOCK' WHEN bi.Quantity<=bi.ReorderLevel THEN 'LOW_STOCK' WHEN bi.Quantity>=bi.ReorderLevel*8 THEN 'OVERSTOCK' ELSE 'IN_STOCK' END StockStatus,
 bi.Quantity*p.CostPrice CostValue,bi.Quantity*p.SellingPrice RetailValue,bi.UpdatedAt LastUpdated,b.BranchID,NULL WarehouseID
FROM branch_inventory bi JOIN branches b ON b.BranchID=bi.BranchID JOIN product_variants pv ON pv.VariantID=bi.VariantID JOIN products p ON p.ProductID=pv.ProductID JOIN categories c ON c.CategoryID=p.CategoryID JOIN sizes sz ON sz.SizeID=pv.SizeID JOIN colors co ON co.ColorID=pv.ColorID
UNION ALL
SELECT 'WAREHOUSE',w.Name,p.ProductID,pv.VariantID,p.SKU,p.Name,c.CategoryName,sz.SizeValue,co.ColorName,wi.Quantity,wi.ReorderLevel,
 CASE WHEN wi.Quantity=0 THEN 'OUT_OF_STOCK' WHEN wi.Quantity<=wi.ReorderLevel THEN 'LOW_STOCK' WHEN wi.Quantity>=wi.ReorderLevel*8 THEN 'OVERSTOCK' ELSE 'IN_STOCK' END,
 wi.Quantity*p.CostPrice,wi.Quantity*p.SellingPrice,wi.UpdatedAt,NULL,w.WarehouseID
FROM warehouse_inventory wi JOIN warehouses w ON w.WarehouseID=wi.WarehouseID JOIN product_variants pv ON pv.VariantID=wi.VariantID JOIN products p ON p.ProductID=pv.ProductID JOIN categories c ON c.CategoryID=p.CategoryID JOIN sizes sz ON sz.SizeID=pv.SizeID JOIN colors co ON co.ColorID=pv.ColorID;
CREATE VIEW v_low_stock AS SELECT * FROM v_current_inventory WHERE StockStatus IN ('LOW_STOCK','OUT_OF_STOCK');
CREATE VIEW v_daily_sales AS SELECT DATE(SaleDate) SaleDay,BranchID,COUNT(*) Transactions,SUM(GrossAmount) GrossSales,SUM(DiscountAmount) Discounts,SUM(NetAmount) Revenue,SUM(CostAmount) COGS,SUM(GrossProfit) GrossProfit FROM sales WHERE Status<>'CANCELLED' GROUP BY DATE(SaleDate),BranchID;
CREATE VIEW v_monthly_sales AS SELECT DATE_FORMAT(SaleDate,'%Y-%m') Month,BranchID,COUNT(*) Transactions,SUM(NetAmount) Revenue,SUM(GrossProfit) GrossProfit FROM sales WHERE Status<>'CANCELLED' GROUP BY DATE_FORMAT(SaleDate,'%Y-%m'),BranchID;
CREATE VIEW v_product_performance AS SELECT p.ProductID,p.SKU,p.Name ProductName,c.CategoryName,SUM(si.Quantity-si.ReturnedQuantity) UnitsSold,SUM(si.LineTotal) Revenue,SUM(si.LineTotal-si.CostAtSale*si.Quantity) GrossProfit FROM sale_items si JOIN sales s ON s.SaleID=si.SaleID JOIN product_variants pv ON pv.VariantID=si.VariantID JOIN products p ON p.ProductID=pv.ProductID JOIN categories c ON c.CategoryID=p.CategoryID WHERE s.Status<>'CANCELLED' GROUP BY p.ProductID,p.SKU,p.Name,c.CategoryName;
CREATE VIEW v_branch_performance AS SELECT b.BranchID,b.Name BranchName,COUNT(s.SaleID) Transactions,COALESCE(SUM(s.NetAmount),0) Revenue,COALESCE(SUM(s.GrossProfit),0) GrossProfit FROM branches b LEFT JOIN sales s ON s.BranchID=b.BranchID AND s.Status<>'CANCELLED' GROUP BY b.BranchID,b.Name;
CREATE VIEW v_cashier_performance AS SELECT u.UserID,u.FullName Cashier,e.BranchID,COUNT(s.SaleID) Transactions,COALESCE(SUM(s.NetAmount),0) Revenue,COALESCE(SUM(s.GrossProfit),0) GrossProfit FROM users u JOIN employees e ON e.UserID=u.UserID LEFT JOIN sales s ON s.CashierUserID=u.UserID AND s.Status<>'CANCELLED' GROUP BY u.UserID,u.FullName,e.BranchID;
CREATE VIEW v_customer_statistics AS SELECT c.CustomerID,c.FullName,COUNT(s.SaleID) PurchaseCount,COALESCE(SUM(s.NetAmount),0) LifetimeSpending,MAX(s.SaleDate) LastPurchaseDate FROM customers c LEFT JOIN sales s ON s.CustomerID=c.CustomerID AND s.Status<>'CANCELLED' GROUP BY c.CustomerID,c.FullName;
CREATE VIEW v_order_statistics AS SELECT DATE(OrderDate) OrderDay,BranchID,Status,COUNT(*) OrderCount,SUM(TotalAmount) OrderValue FROM online_orders GROUP BY DATE(OrderDate),BranchID,Status;
CREATE VIEW v_return_statistics AS SELECT DATE(RequestDate) ReturnDay,BranchID,RequestType,Status,COUNT(*) RequestCount,SUM(RefundAmount) RefundTotal FROM return_requests GROUP BY DATE(RequestDate),BranchID,RequestType,Status;
CREATE VIEW v_monthly_gross_profit AS SELECT DATE_FORMAT(SaleDate,'%Y-%m') Month,BranchID,SUM(NetAmount) Revenue,SUM(CostAmount) COGS,SUM(GrossProfit) GrossProfit FROM sales WHERE Status<>'CANCELLED' GROUP BY DATE_FORMAT(SaleDate,'%Y-%m'),BranchID;
CREATE VIEW v_monthly_expenses AS SELECT DATE_FORMAT(ExpenseDate,'%Y-%m') Month,BranchID,SUM(Amount) Expenses FROM expenses WHERE Status='APPROVED' GROUP BY DATE_FORMAT(ExpenseDate,'%Y-%m'),BranchID;
CREATE VIEW v_monthly_net_profit AS SELECT g.Month,g.BranchID,g.Revenue,g.COGS,g.GrossProfit,COALESCE(e.Expenses,0) Expenses,g.GrossProfit-COALESCE(e.Expenses,0) NetProfit FROM v_monthly_gross_profit g LEFT JOIN v_monthly_expenses e ON e.Month=g.Month AND e.BranchID=g.BranchID;

-- Safe triggers: only assign NEW values; no same-table UPDATE and no circular calls.
DELIMITER $$
CREATE TRIGGER trg_users_before_update BEFORE UPDATE ON users FOR EACH ROW BEGIN SET NEW.UpdatedAt=CURRENT_TIMESTAMP; END$$
CREATE TRIGGER trg_products_before_update BEFORE UPDATE ON products FOR EACH ROW BEGIN SET NEW.UpdatedAt=CURRENT_TIMESTAMP; END$$
DELIMITER ;

-- Required security reference data
INSERT INTO roles(RoleName,Description) VALUES
('OWNER','Full business ownership'),('ADMIN','Authorized administration'),('MANAGER','Assigned branch management'),('CASHIER','Point of sale and own shift'),('WAREHOUSE','Warehouse operations'),('CUSTOMER','Personal shopping account');
INSERT INTO permissions(PermissionCode,Description) VALUES
('VIEW_DASHBOARD','View Dashboard'),
('MANAGE_USERS','Manage Users'),
('MANAGE_ROLES','Manage Roles'),
('MANAGE_PRODUCTS','Manage Products'),
('VIEW_PRODUCT_COST','View Product Cost'),
('MANAGE_INVENTORY','Manage Inventory'),
('CREATE_SALE','Create Sale'),
('CANCEL_SALE','Cancel Sale'),
('PROCESS_RETURN','Process Return'),
('APPROVE_RETURN','Approve Return'),
('VIEW_ALL_BRANCHES','View All Branches'),
('VIEW_BRANCH_SALES','View Branch Sales'),
('MANAGE_CUSTOMERS','Manage Customers'),
('MANAGE_EMPLOYEES','Manage Employees'),
('VIEW_SALARIES','View Salaries'),
('MANAGE_EXPENSES','Manage Expenses'),
('APPROVE_EXPENSES','Approve Expenses'),
('MANAGE_DISCOUNTS','Manage Discounts'),
('APPROVE_STOCK_REQUEST','Approve Stock Request'),
('MANAGE_WAREHOUSE','Manage Warehouse'),
('EXPORT_REPORTS','Export Reports'),
('VIEW_NET_PROFIT','View Net Profit'),
('MANAGE_SETTINGS','Manage Settings'),
('VIEW_AUDIT_LOG','View Audit Log'),
('DATABASE_BACKUP','Database Backup'),
('DATABASE_RESTORE','Database Restore'),
('MANAGE_ORDERS','Manage Orders'),
('MANAGE_SUPPLIERS','Manage Suppliers'),
('MANAGE_PURCHASING','Manage Purchasing'),
('CREATE_DAILY_CLOSING','Create Daily Closing');
INSERT INTO role_permissions(RoleID,PermissionID) SELECT r.RoleID,p.PermissionID FROM roles r CROSS JOIN permissions p WHERE r.RoleName='OWNER' AND p.PermissionCode IN ('VIEW_DASHBOARD','MANAGE_USERS','MANAGE_ROLES','MANAGE_PRODUCTS','VIEW_PRODUCT_COST','MANAGE_INVENTORY','CREATE_SALE','CANCEL_SALE','PROCESS_RETURN','APPROVE_RETURN','VIEW_ALL_BRANCHES','VIEW_BRANCH_SALES','MANAGE_CUSTOMERS','MANAGE_EMPLOYEES','VIEW_SALARIES','MANAGE_EXPENSES','APPROVE_EXPENSES','MANAGE_DISCOUNTS','APPROVE_STOCK_REQUEST','MANAGE_WAREHOUSE','EXPORT_REPORTS','VIEW_NET_PROFIT','MANAGE_SETTINGS','VIEW_AUDIT_LOG','DATABASE_BACKUP','DATABASE_RESTORE','MANAGE_ORDERS','MANAGE_SUPPLIERS','MANAGE_PURCHASING','CREATE_DAILY_CLOSING');
INSERT INTO role_permissions(RoleID,PermissionID) SELECT r.RoleID,p.PermissionID FROM roles r CROSS JOIN permissions p WHERE r.RoleName='ADMIN' AND p.PermissionCode IN ('VIEW_DASHBOARD','MANAGE_USERS','MANAGE_ROLES','MANAGE_PRODUCTS','VIEW_PRODUCT_COST','MANAGE_INVENTORY','CREATE_SALE','CANCEL_SALE','PROCESS_RETURN','APPROVE_RETURN','VIEW_ALL_BRANCHES','VIEW_BRANCH_SALES','MANAGE_CUSTOMERS','MANAGE_EMPLOYEES','VIEW_SALARIES','MANAGE_EXPENSES','APPROVE_EXPENSES','MANAGE_DISCOUNTS','APPROVE_STOCK_REQUEST','MANAGE_WAREHOUSE','EXPORT_REPORTS','MANAGE_SETTINGS','VIEW_AUDIT_LOG','DATABASE_BACKUP','DATABASE_RESTORE','MANAGE_ORDERS','MANAGE_SUPPLIERS','MANAGE_PURCHASING','CREATE_DAILY_CLOSING');
INSERT INTO role_permissions(RoleID,PermissionID) SELECT r.RoleID,p.PermissionID FROM roles r CROSS JOIN permissions p WHERE r.RoleName='MANAGER' AND p.PermissionCode IN ('VIEW_DASHBOARD','MANAGE_PRODUCTS','VIEW_PRODUCT_COST','MANAGE_INVENTORY','CANCEL_SALE','PROCESS_RETURN','APPROVE_RETURN','VIEW_BRANCH_SALES','MANAGE_CUSTOMERS','MANAGE_EMPLOYEES','VIEW_SALARIES','MANAGE_EXPENSES','APPROVE_EXPENSES','MANAGE_DISCOUNTS','APPROVE_STOCK_REQUEST','EXPORT_REPORTS','MANAGE_ORDERS','CREATE_DAILY_CLOSING');
INSERT INTO role_permissions(RoleID,PermissionID) SELECT r.RoleID,p.PermissionID FROM roles r CROSS JOIN permissions p WHERE r.RoleName='CASHIER' AND p.PermissionCode IN ('VIEW_DASHBOARD','CREATE_SALE','PROCESS_RETURN','VIEW_BRANCH_SALES','MANAGE_CUSTOMERS','CREATE_DAILY_CLOSING');
INSERT INTO role_permissions(RoleID,PermissionID) SELECT r.RoleID,p.PermissionID FROM roles r CROSS JOIN permissions p WHERE r.RoleName='WAREHOUSE' AND p.PermissionCode IN ('VIEW_DASHBOARD','MANAGE_PRODUCTS','VIEW_PRODUCT_COST','MANAGE_INVENTORY','APPROVE_STOCK_REQUEST','MANAGE_WAREHOUSE','EXPORT_REPORTS','MANAGE_SUPPLIERS','MANAGE_PURCHASING');
INSERT INTO role_permissions(RoleID,PermissionID) SELECT r.RoleID,p.PermissionID FROM roles r CROSS JOIN permissions p WHERE r.RoleName='CUSTOMER' AND p.PermissionCode IN ('VIEW_DASHBOARD','PROCESS_RETURN','MANAGE_ORDERS');

-- Optional realistic demo data. Remove everything below this marker for a clean production database.
INSERT INTO branches(Name,Location,Phone) VALUES ('Ramallah Main','Al-Irsal Street, Ramallah','+97022900001'),('Birzeit','Main Road, Birzeit','+97022900002'),('Nablus','Rafidia, Nablus','+97092900003');
INSERT INTO warehouses(Name,Location,Phone) VALUES ('Central Warehouse','Ramallah Industrial Area','+97022900100'),('North Warehouse','Nablus Industrial Area','+97092900100');
INSERT INTO users(FullName,Username,PasswordHash,RoleID,IsActive,PasswordChangeRequired,CreatedAt) SELECT 'Lucerne Owner','owner','$2a$12$k2faPBffbK2dOpOk3da/Ke8LFcM4AhhpHhHMzhyV5.XtpDQje0mC.',RoleID,1,0,NOW() FROM roles WHERE RoleName='OWNER';
INSERT INTO users(FullName,Username,PasswordHash,RoleID,IsActive,PasswordChangeRequired,CreatedAt) SELECT 'System Administrator','admin','$2a$12$k2faPBffbK2dOpOk3da/Ke8LFcM4AhhpHhHMzhyV5.XtpDQje0mC.',RoleID,1,0,NOW() FROM roles WHERE RoleName='ADMIN';
INSERT INTO users(FullName,Username,PasswordHash,RoleID,IsActive,PasswordChangeRequired,CreatedAt) SELECT 'Rana Manager','manager.ramallah','$2a$12$k2faPBffbK2dOpOk3da/Ke8LFcM4AhhpHhHMzhyV5.XtpDQje0mC.',RoleID,1,0,NOW() FROM roles WHERE RoleName='MANAGER';
INSERT INTO users(FullName,Username,PasswordHash,RoleID,IsActive,PasswordChangeRequired,CreatedAt) SELECT 'Maya Manager','manager.birzeit','$2a$12$k2faPBffbK2dOpOk3da/Ke8LFcM4AhhpHhHMzhyV5.XtpDQje0mC.',RoleID,1,0,NOW() FROM roles WHERE RoleName='MANAGER';
INSERT INTO users(FullName,Username,PasswordHash,RoleID,IsActive,PasswordChangeRequired,CreatedAt) SELECT 'Lina Cashier','cashier.ramallah','$2a$12$k2faPBffbK2dOpOk3da/Ke8LFcM4AhhpHhHMzhyV5.XtpDQje0mC.',RoleID,1,0,NOW() FROM roles WHERE RoleName='CASHIER';
INSERT INTO users(FullName,Username,PasswordHash,RoleID,IsActive,PasswordChangeRequired,CreatedAt) SELECT 'Sara Cashier','cashier.birzeit','$2a$12$k2faPBffbK2dOpOk3da/Ke8LFcM4AhhpHhHMzhyV5.XtpDQje0mC.',RoleID,1,0,NOW() FROM roles WHERE RoleName='CASHIER';
INSERT INTO users(FullName,Username,PasswordHash,RoleID,IsActive,PasswordChangeRequired,CreatedAt) SELECT 'Noor Cashier','cashier.nablus','$2a$12$k2faPBffbK2dOpOk3da/Ke8LFcM4AhhpHhHMzhyV5.XtpDQje0mC.',RoleID,1,0,NOW() FROM roles WHERE RoleName='CASHIER';
INSERT INTO users(FullName,Username,PasswordHash,RoleID,IsActive,PasswordChangeRequired,CreatedAt) SELECT 'Huda Warehouse','warehouse.central','$2a$12$k2faPBffbK2dOpOk3da/Ke8LFcM4AhhpHhHMzhyV5.XtpDQje0mC.',RoleID,1,0,NOW() FROM roles WHERE RoleName='WAREHOUSE';
INSERT INTO users(FullName,Username,PasswordHash,RoleID,IsActive,PasswordChangeRequired,CreatedAt) SELECT 'Demo Customer','customer.demo','$2a$12$k2faPBffbK2dOpOk3da/Ke8LFcM4AhhpHhHMzhyV5.XtpDQje0mC.',RoleID,1,0,NOW() FROM roles WHERE RoleName='CUSTOMER';
INSERT INTO employees(UserID,FullName,Phone,Email,JobTitle,BranchID,WarehouseID,Salary,HireDate) VALUES
(3,'Rana Manager','+970599100001','rana@lucerne.local','Branch Manager',1,NULL,1100,CURDATE()-INTERVAL 900 DAY),
(4,'Maya Manager','+970599100002','maya@lucerne.local','Branch Manager',2,NULL,1050,CURDATE()-INTERVAL 700 DAY),
(5,'Lina Cashier','+970599100003','lina@lucerne.local','Senior Cashier',1,NULL,650,CURDATE()-INTERVAL 500 DAY),
(6,'Sara Cashier','+970599100004','sara@lucerne.local','Cashier',2,NULL,620,CURDATE()-INTERVAL 380 DAY),
(7,'Noor Cashier','+970599100005','noor@lucerne.local','Cashier',3,NULL,620,CURDATE()-INTERVAL 300 DAY),
(8,'Huda Warehouse','+970599100006','huda@lucerne.local','Warehouse Supervisor',NULL,1,800,CURDATE()-INTERVAL 600 DAY),
(NULL,'Dalia Sales','+970599100007','dalia@lucerne.local','Sales Associate',1,NULL,550,CURDATE()-INTERVAL 210 DAY),
(NULL,'Reem Stock','+970599100008','reem@lucerne.local','Stock Controller',NULL,2,700,CURDATE()-INTERVAL 190 DAY);
INSERT INTO customers(UserID,FullName,Phone,Email,RegisteredAt,VIPStatus) VALUES (9,'Demo Customer','+970599200000','customer@example.com',CURDATE()-INTERVAL 300 DAY,'VIP');
INSERT INTO customers(UserID,FullName,Phone,Email,RegisteredAt,VIPStatus) VALUES
(NULL,'Customer 001','+970592000001','customer001@example.com',CURDATE()-INTERVAL 1 DAY,'REGULAR'),
(NULL,'Customer 002','+970592000002','customer002@example.com',CURDATE()-INTERVAL 2 DAY,'REGULAR'),
(NULL,'Customer 003','+970592000003','customer003@example.com',CURDATE()-INTERVAL 3 DAY,'REGULAR'),
(NULL,'Customer 004','+970592000004','customer004@example.com',CURDATE()-INTERVAL 4 DAY,'REGULAR'),
(NULL,'Customer 005','+970592000005','customer005@example.com',CURDATE()-INTERVAL 5 DAY,'REGULAR'),
(NULL,'Customer 006','+970592000006','customer006@example.com',CURDATE()-INTERVAL 6 DAY,'REGULAR'),
(NULL,'Customer 007','+970592000007','customer007@example.com',CURDATE()-INTERVAL 7 DAY,'REGULAR'),
(NULL,'Customer 008','+970592000008','customer008@example.com',CURDATE()-INTERVAL 8 DAY,'REGULAR'),
(NULL,'Customer 009','+970592000009','customer009@example.com',CURDATE()-INTERVAL 9 DAY,'REGULAR'),
(NULL,'Customer 010','+970592000010','customer010@example.com',CURDATE()-INTERVAL 10 DAY,'REGULAR'),
(NULL,'Customer 011','+970592000011','customer011@example.com',CURDATE()-INTERVAL 11 DAY,'REGULAR'),
(NULL,'Customer 012','+970592000012','customer012@example.com',CURDATE()-INTERVAL 12 DAY,'VIP'),
(NULL,'Customer 013','+970592000013','customer013@example.com',CURDATE()-INTERVAL 13 DAY,'REGULAR'),
(NULL,'Customer 014','+970592000014','customer014@example.com',CURDATE()-INTERVAL 14 DAY,'REGULAR'),
(NULL,'Customer 015','+970592000015','customer015@example.com',CURDATE()-INTERVAL 15 DAY,'REGULAR'),
(NULL,'Customer 016','+970592000016','customer016@example.com',CURDATE()-INTERVAL 16 DAY,'REGULAR'),
(NULL,'Customer 017','+970592000017','customer017@example.com',CURDATE()-INTERVAL 17 DAY,'REGULAR'),
(NULL,'Customer 018','+970592000018','customer018@example.com',CURDATE()-INTERVAL 18 DAY,'REGULAR'),
(NULL,'Customer 019','+970592000019','customer019@example.com',CURDATE()-INTERVAL 19 DAY,'REGULAR'),
(NULL,'Customer 020','+970592000020','customer020@example.com',CURDATE()-INTERVAL 20 DAY,'REGULAR'),
(NULL,'Customer 021','+970592000021','customer021@example.com',CURDATE()-INTERVAL 21 DAY,'REGULAR'),
(NULL,'Customer 022','+970592000022','customer022@example.com',CURDATE()-INTERVAL 22 DAY,'REGULAR'),
(NULL,'Customer 023','+970592000023','customer023@example.com',CURDATE()-INTERVAL 23 DAY,'REGULAR'),
(NULL,'Customer 024','+970592000024','customer024@example.com',CURDATE()-INTERVAL 24 DAY,'VIP'),
(NULL,'Customer 025','+970592000025','customer025@example.com',CURDATE()-INTERVAL 25 DAY,'REGULAR'),
(NULL,'Customer 026','+970592000026','customer026@example.com',CURDATE()-INTERVAL 26 DAY,'REGULAR'),
(NULL,'Customer 027','+970592000027','customer027@example.com',CURDATE()-INTERVAL 27 DAY,'REGULAR'),
(NULL,'Customer 028','+970592000028','customer028@example.com',CURDATE()-INTERVAL 28 DAY,'REGULAR'),
(NULL,'Customer 029','+970592000029','customer029@example.com',CURDATE()-INTERVAL 29 DAY,'REGULAR'),
(NULL,'Customer 030','+970592000030','customer030@example.com',CURDATE()-INTERVAL 30 DAY,'REGULAR'),
(NULL,'Customer 031','+970592000031','customer031@example.com',CURDATE()-INTERVAL 31 DAY,'REGULAR'),
(NULL,'Customer 032','+970592000032','customer032@example.com',CURDATE()-INTERVAL 32 DAY,'REGULAR'),
(NULL,'Customer 033','+970592000033','customer033@example.com',CURDATE()-INTERVAL 33 DAY,'REGULAR'),
(NULL,'Customer 034','+970592000034','customer034@example.com',CURDATE()-INTERVAL 34 DAY,'REGULAR'),
(NULL,'Customer 035','+970592000035','customer035@example.com',CURDATE()-INTERVAL 35 DAY,'REGULAR'),
(NULL,'Customer 036','+970592000036','customer036@example.com',CURDATE()-INTERVAL 36 DAY,'VIP'),
(NULL,'Customer 037','+970592000037','customer037@example.com',CURDATE()-INTERVAL 37 DAY,'REGULAR'),
(NULL,'Customer 038','+970592000038','customer038@example.com',CURDATE()-INTERVAL 38 DAY,'REGULAR'),
(NULL,'Customer 039','+970592000039','customer039@example.com',CURDATE()-INTERVAL 39 DAY,'REGULAR'),
(NULL,'Customer 040','+970592000040','customer040@example.com',CURDATE()-INTERVAL 40 DAY,'REGULAR'),
(NULL,'Customer 041','+970592000041','customer041@example.com',CURDATE()-INTERVAL 41 DAY,'REGULAR'),
(NULL,'Customer 042','+970592000042','customer042@example.com',CURDATE()-INTERVAL 42 DAY,'REGULAR'),
(NULL,'Customer 043','+970592000043','customer043@example.com',CURDATE()-INTERVAL 43 DAY,'REGULAR'),
(NULL,'Customer 044','+970592000044','customer044@example.com',CURDATE()-INTERVAL 44 DAY,'REGULAR'),
(NULL,'Customer 045','+970592000045','customer045@example.com',CURDATE()-INTERVAL 45 DAY,'REGULAR'),
(NULL,'Customer 046','+970592000046','customer046@example.com',CURDATE()-INTERVAL 46 DAY,'REGULAR'),
(NULL,'Customer 047','+970592000047','customer047@example.com',CURDATE()-INTERVAL 47 DAY,'REGULAR'),
(NULL,'Customer 048','+970592000048','customer048@example.com',CURDATE()-INTERVAL 48 DAY,'VIP'),
(NULL,'Customer 049','+970592000049','customer049@example.com',CURDATE()-INTERVAL 49 DAY,'REGULAR'),
(NULL,'Customer 050','+970592000050','customer050@example.com',CURDATE()-INTERVAL 50 DAY,'REGULAR'),
(NULL,'Customer 051','+970592000051','customer051@example.com',CURDATE()-INTERVAL 51 DAY,'REGULAR'),
(NULL,'Customer 052','+970592000052','customer052@example.com',CURDATE()-INTERVAL 52 DAY,'REGULAR'),
(NULL,'Customer 053','+970592000053','customer053@example.com',CURDATE()-INTERVAL 53 DAY,'REGULAR'),
(NULL,'Customer 054','+970592000054','customer054@example.com',CURDATE()-INTERVAL 54 DAY,'REGULAR'),
(NULL,'Customer 055','+970592000055','customer055@example.com',CURDATE()-INTERVAL 55 DAY,'REGULAR'),
(NULL,'Customer 056','+970592000056','customer056@example.com',CURDATE()-INTERVAL 56 DAY,'REGULAR'),
(NULL,'Customer 057','+970592000057','customer057@example.com',CURDATE()-INTERVAL 57 DAY,'REGULAR'),
(NULL,'Customer 058','+970592000058','customer058@example.com',CURDATE()-INTERVAL 58 DAY,'REGULAR'),
(NULL,'Customer 059','+970592000059','customer059@example.com',CURDATE()-INTERVAL 59 DAY,'REGULAR'),
(NULL,'Customer 060','+970592000060','customer060@example.com',CURDATE()-INTERVAL 60 DAY,'VIP'),
(NULL,'Customer 061','+970592000061','customer061@example.com',CURDATE()-INTERVAL 61 DAY,'REGULAR'),
(NULL,'Customer 062','+970592000062','customer062@example.com',CURDATE()-INTERVAL 62 DAY,'REGULAR'),
(NULL,'Customer 063','+970592000063','customer063@example.com',CURDATE()-INTERVAL 63 DAY,'REGULAR'),
(NULL,'Customer 064','+970592000064','customer064@example.com',CURDATE()-INTERVAL 64 DAY,'REGULAR'),
(NULL,'Customer 065','+970592000065','customer065@example.com',CURDATE()-INTERVAL 65 DAY,'REGULAR'),
(NULL,'Customer 066','+970592000066','customer066@example.com',CURDATE()-INTERVAL 66 DAY,'REGULAR'),
(NULL,'Customer 067','+970592000067','customer067@example.com',CURDATE()-INTERVAL 67 DAY,'REGULAR'),
(NULL,'Customer 068','+970592000068','customer068@example.com',CURDATE()-INTERVAL 68 DAY,'REGULAR'),
(NULL,'Customer 069','+970592000069','customer069@example.com',CURDATE()-INTERVAL 69 DAY,'REGULAR'),
(NULL,'Customer 070','+970592000070','customer070@example.com',CURDATE()-INTERVAL 70 DAY,'REGULAR'),
(NULL,'Customer 071','+970592000071','customer071@example.com',CURDATE()-INTERVAL 71 DAY,'REGULAR'),
(NULL,'Customer 072','+970592000072','customer072@example.com',CURDATE()-INTERVAL 72 DAY,'VIP'),
(NULL,'Customer 073','+970592000073','customer073@example.com',CURDATE()-INTERVAL 73 DAY,'REGULAR'),
(NULL,'Customer 074','+970592000074','customer074@example.com',CURDATE()-INTERVAL 74 DAY,'REGULAR'),
(NULL,'Customer 075','+970592000075','customer075@example.com',CURDATE()-INTERVAL 75 DAY,'REGULAR'),
(NULL,'Customer 076','+970592000076','customer076@example.com',CURDATE()-INTERVAL 76 DAY,'REGULAR'),
(NULL,'Customer 077','+970592000077','customer077@example.com',CURDATE()-INTERVAL 77 DAY,'REGULAR'),
(NULL,'Customer 078','+970592000078','customer078@example.com',CURDATE()-INTERVAL 78 DAY,'REGULAR'),
(NULL,'Customer 079','+970592000079','customer079@example.com',CURDATE()-INTERVAL 79 DAY,'REGULAR'),
(NULL,'Customer 080','+970592000080','customer080@example.com',CURDATE()-INTERVAL 80 DAY,'REGULAR'),
(NULL,'Customer 081','+970592000081','customer081@example.com',CURDATE()-INTERVAL 81 DAY,'REGULAR'),
(NULL,'Customer 082','+970592000082','customer082@example.com',CURDATE()-INTERVAL 82 DAY,'REGULAR'),
(NULL,'Customer 083','+970592000083','customer083@example.com',CURDATE()-INTERVAL 83 DAY,'REGULAR'),
(NULL,'Customer 084','+970592000084','customer084@example.com',CURDATE()-INTERVAL 84 DAY,'VIP'),
(NULL,'Customer 085','+970592000085','customer085@example.com',CURDATE()-INTERVAL 85 DAY,'REGULAR'),
(NULL,'Customer 086','+970592000086','customer086@example.com',CURDATE()-INTERVAL 86 DAY,'REGULAR'),
(NULL,'Customer 087','+970592000087','customer087@example.com',CURDATE()-INTERVAL 87 DAY,'REGULAR'),
(NULL,'Customer 088','+970592000088','customer088@example.com',CURDATE()-INTERVAL 88 DAY,'REGULAR'),
(NULL,'Customer 089','+970592000089','customer089@example.com',CURDATE()-INTERVAL 89 DAY,'REGULAR'),
(NULL,'Customer 090','+970592000090','customer090@example.com',CURDATE()-INTERVAL 90 DAY,'REGULAR'),
(NULL,'Customer 091','+970592000091','customer091@example.com',CURDATE()-INTERVAL 91 DAY,'REGULAR'),
(NULL,'Customer 092','+970592000092','customer092@example.com',CURDATE()-INTERVAL 92 DAY,'REGULAR'),
(NULL,'Customer 093','+970592000093','customer093@example.com',CURDATE()-INTERVAL 93 DAY,'REGULAR'),
(NULL,'Customer 094','+970592000094','customer094@example.com',CURDATE()-INTERVAL 94 DAY,'REGULAR'),
(NULL,'Customer 095','+970592000095','customer095@example.com',CURDATE()-INTERVAL 95 DAY,'REGULAR'),
(NULL,'Customer 096','+970592000096','customer096@example.com',CURDATE()-INTERVAL 96 DAY,'VIP'),
(NULL,'Customer 097','+970592000097','customer097@example.com',CURDATE()-INTERVAL 97 DAY,'REGULAR'),
(NULL,'Customer 098','+970592000098','customer098@example.com',CURDATE()-INTERVAL 98 DAY,'REGULAR'),
(NULL,'Customer 099','+970592000099','customer099@example.com',CURDATE()-INTERVAL 99 DAY,'REGULAR'),
(NULL,'Customer 100','+970592000100','customer100@example.com',CURDATE()-INTERVAL 100 DAY,'REGULAR'),
(NULL,'Customer 101','+970592000101','customer101@example.com',CURDATE()-INTERVAL 101 DAY,'REGULAR'),
(NULL,'Customer 102','+970592000102','customer102@example.com',CURDATE()-INTERVAL 102 DAY,'REGULAR'),
(NULL,'Customer 103','+970592000103','customer103@example.com',CURDATE()-INTERVAL 103 DAY,'REGULAR'),
(NULL,'Customer 104','+970592000104','customer104@example.com',CURDATE()-INTERVAL 104 DAY,'REGULAR'),
(NULL,'Customer 105','+970592000105','customer105@example.com',CURDATE()-INTERVAL 105 DAY,'REGULAR'),
(NULL,'Customer 106','+970592000106','customer106@example.com',CURDATE()-INTERVAL 106 DAY,'REGULAR'),
(NULL,'Customer 107','+970592000107','customer107@example.com',CURDATE()-INTERVAL 107 DAY,'REGULAR'),
(NULL,'Customer 108','+970592000108','customer108@example.com',CURDATE()-INTERVAL 108 DAY,'VIP'),
(NULL,'Customer 109','+970592000109','customer109@example.com',CURDATE()-INTERVAL 109 DAY,'REGULAR'),
(NULL,'Customer 110','+970592000110','customer110@example.com',CURDATE()-INTERVAL 110 DAY,'REGULAR'),
(NULL,'Customer 111','+970592000111','customer111@example.com',CURDATE()-INTERVAL 111 DAY,'REGULAR'),
(NULL,'Customer 112','+970592000112','customer112@example.com',CURDATE()-INTERVAL 112 DAY,'REGULAR'),
(NULL,'Customer 113','+970592000113','customer113@example.com',CURDATE()-INTERVAL 113 DAY,'REGULAR'),
(NULL,'Customer 114','+970592000114','customer114@example.com',CURDATE()-INTERVAL 114 DAY,'REGULAR'),
(NULL,'Customer 115','+970592000115','customer115@example.com',CURDATE()-INTERVAL 115 DAY,'REGULAR'),
(NULL,'Customer 116','+970592000116','customer116@example.com',CURDATE()-INTERVAL 116 DAY,'REGULAR'),
(NULL,'Customer 117','+970592000117','customer117@example.com',CURDATE()-INTERVAL 117 DAY,'REGULAR'),
(NULL,'Customer 118','+970592000118','customer118@example.com',CURDATE()-INTERVAL 118 DAY,'REGULAR'),
(NULL,'Customer 119','+970592000119','customer119@example.com',CURDATE()-INTERVAL 119 DAY,'REGULAR'),
(NULL,'Customer 120','+970592000120','customer120@example.com',CURDATE()-INTERVAL 120 DAY,'VIP'),
(NULL,'Customer 121','+970592000121','customer121@example.com',CURDATE()-INTERVAL 121 DAY,'REGULAR'),
(NULL,'Customer 122','+970592000122','customer122@example.com',CURDATE()-INTERVAL 122 DAY,'REGULAR'),
(NULL,'Customer 123','+970592000123','customer123@example.com',CURDATE()-INTERVAL 123 DAY,'REGULAR'),
(NULL,'Customer 124','+970592000124','customer124@example.com',CURDATE()-INTERVAL 124 DAY,'REGULAR'),
(NULL,'Customer 125','+970592000125','customer125@example.com',CURDATE()-INTERVAL 125 DAY,'REGULAR'),
(NULL,'Customer 126','+970592000126','customer126@example.com',CURDATE()-INTERVAL 126 DAY,'REGULAR'),
(NULL,'Customer 127','+970592000127','customer127@example.com',CURDATE()-INTERVAL 127 DAY,'REGULAR'),
(NULL,'Customer 128','+970592000128','customer128@example.com',CURDATE()-INTERVAL 128 DAY,'REGULAR'),
(NULL,'Customer 129','+970592000129','customer129@example.com',CURDATE()-INTERVAL 129 DAY,'REGULAR'),
(NULL,'Customer 130','+970592000130','customer130@example.com',CURDATE()-INTERVAL 130 DAY,'REGULAR'),
(NULL,'Customer 131','+970592000131','customer131@example.com',CURDATE()-INTERVAL 131 DAY,'REGULAR'),
(NULL,'Customer 132','+970592000132','customer132@example.com',CURDATE()-INTERVAL 132 DAY,'VIP'),
(NULL,'Customer 133','+970592000133','customer133@example.com',CURDATE()-INTERVAL 133 DAY,'REGULAR'),
(NULL,'Customer 134','+970592000134','customer134@example.com',CURDATE()-INTERVAL 134 DAY,'REGULAR'),
(NULL,'Customer 135','+970592000135','customer135@example.com',CURDATE()-INTERVAL 135 DAY,'REGULAR'),
(NULL,'Customer 136','+970592000136','customer136@example.com',CURDATE()-INTERVAL 136 DAY,'REGULAR'),
(NULL,'Customer 137','+970592000137','customer137@example.com',CURDATE()-INTERVAL 137 DAY,'REGULAR'),
(NULL,'Customer 138','+970592000138','customer138@example.com',CURDATE()-INTERVAL 138 DAY,'REGULAR'),
(NULL,'Customer 139','+970592000139','customer139@example.com',CURDATE()-INTERVAL 139 DAY,'REGULAR'),
(NULL,'Customer 140','+970592000140','customer140@example.com',CURDATE()-INTERVAL 140 DAY,'REGULAR'),
(NULL,'Customer 141','+970592000141','customer141@example.com',CURDATE()-INTERVAL 141 DAY,'REGULAR'),
(NULL,'Customer 142','+970592000142','customer142@example.com',CURDATE()-INTERVAL 142 DAY,'REGULAR'),
(NULL,'Customer 143','+970592000143','customer143@example.com',CURDATE()-INTERVAL 143 DAY,'REGULAR'),
(NULL,'Customer 144','+970592000144','customer144@example.com',CURDATE()-INTERVAL 144 DAY,'VIP'),
(NULL,'Customer 145','+970592000145','customer145@example.com',CURDATE()-INTERVAL 145 DAY,'REGULAR'),
(NULL,'Customer 146','+970592000146','customer146@example.com',CURDATE()-INTERVAL 146 DAY,'REGULAR'),
(NULL,'Customer 147','+970592000147','customer147@example.com',CURDATE()-INTERVAL 147 DAY,'REGULAR'),
(NULL,'Customer 148','+970592000148','customer148@example.com',CURDATE()-INTERVAL 148 DAY,'REGULAR'),
(NULL,'Customer 149','+970592000149','customer149@example.com',CURDATE()-INTERVAL 149 DAY,'REGULAR'),
(NULL,'Customer 150','+970592000150','customer150@example.com',CURDATE()-INTERVAL 150 DAY,'REGULAR'),
(NULL,'Customer 151','+970592000151','customer151@example.com',CURDATE()-INTERVAL 151 DAY,'REGULAR'),
(NULL,'Customer 152','+970592000152','customer152@example.com',CURDATE()-INTERVAL 152 DAY,'REGULAR'),
(NULL,'Customer 153','+970592000153','customer153@example.com',CURDATE()-INTERVAL 153 DAY,'REGULAR'),
(NULL,'Customer 154','+970592000154','customer154@example.com',CURDATE()-INTERVAL 154 DAY,'REGULAR'),
(NULL,'Customer 155','+970592000155','customer155@example.com',CURDATE()-INTERVAL 155 DAY,'REGULAR'),
(NULL,'Customer 156','+970592000156','customer156@example.com',CURDATE()-INTERVAL 156 DAY,'VIP'),
(NULL,'Customer 157','+970592000157','customer157@example.com',CURDATE()-INTERVAL 157 DAY,'REGULAR'),
(NULL,'Customer 158','+970592000158','customer158@example.com',CURDATE()-INTERVAL 158 DAY,'REGULAR'),
(NULL,'Customer 159','+970592000159','customer159@example.com',CURDATE()-INTERVAL 159 DAY,'REGULAR'),
(NULL,'Customer 160','+970592000160','customer160@example.com',CURDATE()-INTERVAL 160 DAY,'REGULAR'),
(NULL,'Customer 161','+970592000161','customer161@example.com',CURDATE()-INTERVAL 161 DAY,'REGULAR'),
(NULL,'Customer 162','+970592000162','customer162@example.com',CURDATE()-INTERVAL 162 DAY,'REGULAR'),
(NULL,'Customer 163','+970592000163','customer163@example.com',CURDATE()-INTERVAL 163 DAY,'REGULAR'),
(NULL,'Customer 164','+970592000164','customer164@example.com',CURDATE()-INTERVAL 164 DAY,'REGULAR'),
(NULL,'Customer 165','+970592000165','customer165@example.com',CURDATE()-INTERVAL 165 DAY,'REGULAR'),
(NULL,'Customer 166','+970592000166','customer166@example.com',CURDATE()-INTERVAL 166 DAY,'REGULAR'),
(NULL,'Customer 167','+970592000167','customer167@example.com',CURDATE()-INTERVAL 167 DAY,'REGULAR'),
(NULL,'Customer 168','+970592000168','customer168@example.com',CURDATE()-INTERVAL 168 DAY,'VIP'),
(NULL,'Customer 169','+970592000169','customer169@example.com',CURDATE()-INTERVAL 169 DAY,'REGULAR'),
(NULL,'Customer 170','+970592000170','customer170@example.com',CURDATE()-INTERVAL 170 DAY,'REGULAR'),
(NULL,'Customer 171','+970592000171','customer171@example.com',CURDATE()-INTERVAL 171 DAY,'REGULAR'),
(NULL,'Customer 172','+970592000172','customer172@example.com',CURDATE()-INTERVAL 172 DAY,'REGULAR'),
(NULL,'Customer 173','+970592000173','customer173@example.com',CURDATE()-INTERVAL 173 DAY,'REGULAR'),
(NULL,'Customer 174','+970592000174','customer174@example.com',CURDATE()-INTERVAL 174 DAY,'REGULAR'),
(NULL,'Customer 175','+970592000175','customer175@example.com',CURDATE()-INTERVAL 175 DAY,'REGULAR'),
(NULL,'Customer 176','+970592000176','customer176@example.com',CURDATE()-INTERVAL 176 DAY,'REGULAR'),
(NULL,'Customer 177','+970592000177','customer177@example.com',CURDATE()-INTERVAL 177 DAY,'REGULAR'),
(NULL,'Customer 178','+970592000178','customer178@example.com',CURDATE()-INTERVAL 178 DAY,'REGULAR'),
(NULL,'Customer 179','+970592000179','customer179@example.com',CURDATE()-INTERVAL 179 DAY,'REGULAR'),
(NULL,'Customer 180','+970592000180','customer180@example.com',CURDATE()-INTERVAL 180 DAY,'VIP'),
(NULL,'Customer 181','+970592000181','customer181@example.com',CURDATE()-INTERVAL 181 DAY,'REGULAR'),
(NULL,'Customer 182','+970592000182','customer182@example.com',CURDATE()-INTERVAL 182 DAY,'REGULAR'),
(NULL,'Customer 183','+970592000183','customer183@example.com',CURDATE()-INTERVAL 183 DAY,'REGULAR'),
(NULL,'Customer 184','+970592000184','customer184@example.com',CURDATE()-INTERVAL 184 DAY,'REGULAR'),
(NULL,'Customer 185','+970592000185','customer185@example.com',CURDATE()-INTERVAL 185 DAY,'REGULAR'),
(NULL,'Customer 186','+970592000186','customer186@example.com',CURDATE()-INTERVAL 186 DAY,'REGULAR'),
(NULL,'Customer 187','+970592000187','customer187@example.com',CURDATE()-INTERVAL 187 DAY,'REGULAR'),
(NULL,'Customer 188','+970592000188','customer188@example.com',CURDATE()-INTERVAL 188 DAY,'REGULAR'),
(NULL,'Customer 189','+970592000189','customer189@example.com',CURDATE()-INTERVAL 189 DAY,'REGULAR'),
(NULL,'Customer 190','+970592000190','customer190@example.com',CURDATE()-INTERVAL 190 DAY,'REGULAR'),
(NULL,'Customer 191','+970592000191','customer191@example.com',CURDATE()-INTERVAL 191 DAY,'REGULAR'),
(NULL,'Customer 192','+970592000192','customer192@example.com',CURDATE()-INTERVAL 192 DAY,'VIP'),
(NULL,'Customer 193','+970592000193','customer193@example.com',CURDATE()-INTERVAL 193 DAY,'REGULAR'),
(NULL,'Customer 194','+970592000194','customer194@example.com',CURDATE()-INTERVAL 194 DAY,'REGULAR'),
(NULL,'Customer 195','+970592000195','customer195@example.com',CURDATE()-INTERVAL 195 DAY,'REGULAR'),
(NULL,'Customer 196','+970592000196','customer196@example.com',CURDATE()-INTERVAL 196 DAY,'REGULAR'),
(NULL,'Customer 197','+970592000197','customer197@example.com',CURDATE()-INTERVAL 197 DAY,'REGULAR'),
(NULL,'Customer 198','+970592000198','customer198@example.com',CURDATE()-INTERVAL 198 DAY,'REGULAR'),
(NULL,'Customer 199','+970592000199','customer199@example.com',CURDATE()-INTERVAL 199 DAY,'REGULAR');
INSERT INTO categories(CategoryName,Description) VALUES
('Blouses','Blouses catalog'),
('Pants','Pants catalog'),
('Dresses','Dresses catalog'),
('Abayas','Abayas catalog'),
('Shoes','Shoes catalog'),
('Outerwear','Outerwear catalog'),
('Sets','Sets catalog'),
('Accessories','Accessories catalog'),
('Knitwear','Knitwear catalog'),
('Skirts','Skirts catalog');
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Casual Blouses' FROM categories WHERE CategoryName='Blouses';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Formal Blouses' FROM categories WHERE CategoryName='Blouses';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Jeans' FROM categories WHERE CategoryName='Pants';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Formal Pants' FROM categories WHERE CategoryName='Pants';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Casual Pants' FROM categories WHERE CategoryName='Pants';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Modest Dresses' FROM categories WHERE CategoryName='Dresses';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Casual Dresses' FROM categories WHERE CategoryName='Dresses';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Formal Dresses' FROM categories WHERE CategoryName='Dresses';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Classic Abayas' FROM categories WHERE CategoryName='Abayas';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Open Abayas' FROM categories WHERE CategoryName='Abayas';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Embroidered Abayas' FROM categories WHERE CategoryName='Abayas';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Heels' FROM categories WHERE CategoryName='Shoes';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Flats' FROM categories WHERE CategoryName='Shoes';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Sneakers' FROM categories WHERE CategoryName='Shoes';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Blazers' FROM categories WHERE CategoryName='Outerwear';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Coats' FROM categories WHERE CategoryName='Outerwear';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Jackets' FROM categories WHERE CategoryName='Outerwear';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Casual Sets' FROM categories WHERE CategoryName='Sets';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Formal Sets' FROM categories WHERE CategoryName='Sets';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Bags' FROM categories WHERE CategoryName='Accessories';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Scarves' FROM categories WHERE CategoryName='Accessories';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Belts' FROM categories WHERE CategoryName='Accessories';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Jewelry' FROM categories WHERE CategoryName='Accessories';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Tops' FROM categories WHERE CategoryName='Knitwear';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Cardigans' FROM categories WHERE CategoryName='Knitwear';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Sweaters' FROM categories WHERE CategoryName='Knitwear';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Maxi Skirts' FROM categories WHERE CategoryName='Skirts';
INSERT INTO subcategories(CategoryID,SubcategoryName) SELECT CategoryID,'Midi Skirts' FROM categories WHERE CategoryName='Skirts';
INSERT INTO sizes(SizeValue,SortOrder) VALUES ('XS',1),('S',2),('M',3),('L',4),('XL',5),('XXL',6),('36',10),('37',11),('38',12),('39',13),('40',14),('ONE SIZE',20);
INSERT INTO colors(ColorName,HexCode) VALUES ('Black','#111111'),('White','#F4F1EA'),('Beige','#D8C3A5'),('Rose','#C98B91'),('Navy','#283A5B'),('Brown','#7A5543'),('Red','#A83D43'),('Green','#60765C'),('Blue','#617F9B'),('Gold','#B89A58');
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0001','6291000000001','Polo Blouse',c.CategoryID,sc.SubcategoryID,35.00,18.00,'Polo Blouse designed for the Lucerne Boutique demo catalog.','Cotton blend','Machine wash cold','images/products/product_001.png',5,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Casual Blouses' WHERE c.CategoryName='Blouses';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0002','6291000000002','Satin Bow Blouse',c.CategoryID,sc.SubcategoryID,44.00,23.00,'Satin Bow Blouse designed for the Lucerne Boutique demo catalog.','Satin','Hand wash','images/products/product_002.png',6,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Formal Blouses' WHERE c.CategoryName='Blouses';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0003','6291000000003','Ruffle Sleeve Blouse',c.CategoryID,sc.SubcategoryID,39.00,20.00,'Ruffle Sleeve Blouse designed for the Lucerne Boutique demo catalog.','Viscose','Gentle wash','images/products/product_003.png',7,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Casual Blouses' WHERE c.CategoryName='Blouses';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0004','6291000000004','Classic White Shirt',c.CategoryID,sc.SubcategoryID,42.00,21.00,'Classic White Shirt designed for the Lucerne Boutique demo catalog.','Cotton','Machine wash','images/products/product_004.png',4,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Formal Blouses' WHERE c.CategoryName='Blouses';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0005','6291000000005','Pleated Chiffon Blouse',c.CategoryID,sc.SubcategoryID,48.00,26.00,'Pleated Chiffon Blouse designed for the Lucerne Boutique demo catalog.','Chiffon','Hand wash','images/products/product_005.png',5,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Formal Blouses' WHERE c.CategoryName='Blouses';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0006','6291000000006','Linen Summer Shirt',c.CategoryID,sc.SubcategoryID,45.00,24.00,'Linen Summer Shirt designed for the Lucerne Boutique demo catalog.','Linen blend','Gentle wash','images/products/product_006.png',6,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Casual Blouses' WHERE c.CategoryName='Blouses';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0007','6291000000007','Wide Leg Jeans',c.CategoryID,sc.SubcategoryID,55.00,30.00,'Wide Leg Jeans designed for the Lucerne Boutique demo catalog.','Denim','Machine wash','images/products/product_007.png',7,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Jeans' WHERE c.CategoryName='Pants';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0008','6291000000008','Tailored Trousers',c.CategoryID,sc.SubcategoryID,58.00,31.00,'Tailored Trousers designed for the Lucerne Boutique demo catalog.','Poly-viscose','Dry clean recommended','images/products/product_008.png',4,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Formal Pants' WHERE c.CategoryName='Pants';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0009','6291000000009','Straight Leg Pants',c.CategoryID,sc.SubcategoryID,49.00,26.00,'Straight Leg Pants designed for the Lucerne Boutique demo catalog.','Cotton twill','Machine wash','images/products/product_009.png',5,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Casual Pants' WHERE c.CategoryName='Pants';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0010','6291000000010','High Waist Jeans',c.CategoryID,sc.SubcategoryID,57.00,31.00,'High Waist Jeans designed for the Lucerne Boutique demo catalog.','Denim','Machine wash','images/products/product_010.png',6,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Jeans' WHERE c.CategoryName='Pants';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0011','6291000000011','Pleated Palazzo Pants',c.CategoryID,sc.SubcategoryID,52.00,28.00,'Pleated Palazzo Pants designed for the Lucerne Boutique demo catalog.','Crepe','Gentle wash','images/products/product_011.png',7,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Casual Pants' WHERE c.CategoryName='Pants';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0012','6291000000012','Classic Black Trousers',c.CategoryID,sc.SubcategoryID,61.00,34.00,'Classic Black Trousers designed for the Lucerne Boutique demo catalog.','Suiting fabric','Dry clean','images/products/product_012.png',4,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Formal Pants' WHERE c.CategoryName='Pants';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0013','6291000000013','Modest Maxi Dress',c.CategoryID,sc.SubcategoryID,89.00,49.00,'Modest Maxi Dress designed for the Lucerne Boutique demo catalog.','Crepe','Hand wash','images/products/product_013.png',5,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Modest Dresses' WHERE c.CategoryName='Dresses';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0014','6291000000014','Floral Midi Dress',c.CategoryID,sc.SubcategoryID,75.00,39.00,'Floral Midi Dress designed for the Lucerne Boutique demo catalog.','Viscose','Gentle wash','images/products/product_014.png',6,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Casual Dresses' WHERE c.CategoryName='Dresses';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0015','6291000000015','Evening Satin Dress',c.CategoryID,sc.SubcategoryID,120.00,68.00,'Evening Satin Dress designed for the Lucerne Boutique demo catalog.','Satin','Dry clean','images/products/product_015.png',7,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Formal Dresses' WHERE c.CategoryName='Dresses';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0016','6291000000016','Belted Shirt Dress',c.CategoryID,sc.SubcategoryID,82.00,44.00,'Belted Shirt Dress designed for the Lucerne Boutique demo catalog.','Cotton blend','Machine wash','images/products/product_016.png',4,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Casual Dresses' WHERE c.CategoryName='Dresses';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0017','6291000000017','Pleated Occasion Dress',c.CategoryID,sc.SubcategoryID,135.00,76.00,'Pleated Occasion Dress designed for the Lucerne Boutique demo catalog.','Chiffon','Dry clean','images/products/product_017.png',5,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Formal Dresses' WHERE c.CategoryName='Dresses';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0018','6291000000018','Embroidered Long Dress',c.CategoryID,sc.SubcategoryID,110.00,60.00,'Embroidered Long Dress designed for the Lucerne Boutique demo catalog.','Crepe','Hand wash','images/products/product_018.png',6,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Modest Dresses' WHERE c.CategoryName='Dresses';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0019','6291000000019','Elegant Abaya',c.CategoryID,sc.SubcategoryID,95.00,52.00,'Elegant Abaya designed for the Lucerne Boutique demo catalog.','Nida fabric','Hand wash','images/products/product_019.png',7,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Classic Abayas' WHERE c.CategoryName='Abayas';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0020','6291000000020','Open Front Abaya',c.CategoryID,sc.SubcategoryID,105.00,58.00,'Open Front Abaya designed for the Lucerne Boutique demo catalog.','Premium crepe','Hand wash','images/products/product_020.png',4,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Open Abayas' WHERE c.CategoryName='Abayas';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0021','6291000000021','Embroidered Abaya',c.CategoryID,sc.SubcategoryID,125.00,72.00,'Embroidered Abaya designed for the Lucerne Boutique demo catalog.','Nida fabric','Dry clean','images/products/product_021.png',5,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Embroidered Abayas' WHERE c.CategoryName='Abayas';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0022','6291000000022','Belted Abaya',c.CategoryID,sc.SubcategoryID,98.00,54.00,'Belted Abaya designed for the Lucerne Boutique demo catalog.','Crepe','Hand wash','images/products/product_022.png',6,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Classic Abayas' WHERE c.CategoryName='Abayas';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0023','6291000000023','Pearl Detail Abaya',c.CategoryID,sc.SubcategoryID,140.00,79.00,'Pearl Detail Abaya designed for the Lucerne Boutique demo catalog.','Premium nida','Dry clean','images/products/product_023.png',7,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Embroidered Abayas' WHERE c.CategoryName='Abayas';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0024','6291000000024','Layered Open Abaya',c.CategoryID,sc.SubcategoryID,118.00,65.00,'Layered Open Abaya designed for the Lucerne Boutique demo catalog.','Chiffon crepe','Hand wash','images/products/product_024.png',4,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Open Abayas' WHERE c.CategoryName='Abayas';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0025','6291000000025','Block Heel Sandal',c.CategoryID,sc.SubcategoryID,68.00,35.00,'Block Heel Sandal designed for the Lucerne Boutique demo catalog.','Vegan leather','Wipe clean','images/products/product_025.png',5,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Heels' WHERE c.CategoryName='Shoes';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0026','6291000000026','Classic Ballerina',c.CategoryID,sc.SubcategoryID,49.00,24.00,'Classic Ballerina designed for the Lucerne Boutique demo catalog.','Vegan leather','Wipe clean','images/products/product_026.png',6,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Flats' WHERE c.CategoryName='Shoes';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0027','6291000000027','Comfort Sneakers',c.CategoryID,sc.SubcategoryID,72.00,38.00,'Comfort Sneakers designed for the Lucerne Boutique demo catalog.','Textile','Wipe clean','images/products/product_027.png',7,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Sneakers' WHERE c.CategoryName='Shoes';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0028','6291000000028','Pointed Court Heel',c.CategoryID,sc.SubcategoryID,79.00,42.00,'Pointed Court Heel designed for the Lucerne Boutique demo catalog.','Vegan leather','Wipe clean','images/products/product_028.png',4,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Heels' WHERE c.CategoryName='Shoes';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0029','6291000000029','Everyday Loafer',c.CategoryID,sc.SubcategoryID,65.00,34.00,'Everyday Loafer designed for the Lucerne Boutique demo catalog.','Vegan leather','Wipe clean','images/products/product_029.png',5,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Flats' WHERE c.CategoryName='Shoes';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0030','6291000000030','Minimal White Sneaker',c.CategoryID,sc.SubcategoryID,76.00,40.00,'Minimal White Sneaker designed for the Lucerne Boutique demo catalog.','Vegan leather','Wipe clean','images/products/product_030.png',6,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Sneakers' WHERE c.CategoryName='Shoes';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0031','6291000000031','Structured Blazer',c.CategoryID,sc.SubcategoryID,92.00,50.00,'Structured Blazer designed for the Lucerne Boutique demo catalog.','Suiting fabric','Dry clean','images/products/product_031.png',7,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Blazers' WHERE c.CategoryName='Outerwear';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0032','6291000000032','Longline Coat',c.CategoryID,sc.SubcategoryID,145.00,82.00,'Longline Coat designed for the Lucerne Boutique demo catalog.','Wool blend','Dry clean','images/products/product_032.png',4,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Coats' WHERE c.CategoryName='Outerwear';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0033','6291000000033','Cropped Jacket',c.CategoryID,sc.SubcategoryID,88.00,47.00,'Cropped Jacket designed for the Lucerne Boutique demo catalog.','Cotton blend','Gentle wash','images/products/product_033.png',5,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Jackets' WHERE c.CategoryName='Outerwear';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0034','6291000000034','Belted Trench Coat',c.CategoryID,sc.SubcategoryID,138.00,78.00,'Belted Trench Coat designed for the Lucerne Boutique demo catalog.','Water-resistant blend','Dry clean','images/products/product_034.png',6,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Coats' WHERE c.CategoryName='Outerwear';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0035','6291000000035','Classic Double Blazer',c.CategoryID,sc.SubcategoryID,105.00,58.00,'Classic Double Blazer designed for the Lucerne Boutique demo catalog.','Suiting fabric','Dry clean','images/products/product_035.png',7,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Blazers' WHERE c.CategoryName='Outerwear';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0036','6291000000036','Denim Jacket',c.CategoryID,sc.SubcategoryID,85.00,45.00,'Denim Jacket designed for the Lucerne Boutique demo catalog.','Denim','Machine wash','images/products/product_036.png',4,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Jackets' WHERE c.CategoryName='Outerwear';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0037','6291000000037','Knit Two Piece Set',c.CategoryID,sc.SubcategoryID,99.00,54.00,'Knit Two Piece Set designed for the Lucerne Boutique demo catalog.','Knit blend','Gentle wash','images/products/product_037.png',5,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Casual Sets' WHERE c.CategoryName='Sets';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0038','6291000000038','Tailored Vest Set',c.CategoryID,sc.SubcategoryID,125.00,70.00,'Tailored Vest Set designed for the Lucerne Boutique demo catalog.','Suiting fabric','Dry clean','images/products/product_038.png',6,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Formal Sets' WHERE c.CategoryName='Sets';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0039','6291000000039','Relaxed Linen Set',c.CategoryID,sc.SubcategoryID,108.00,60.00,'Relaxed Linen Set designed for the Lucerne Boutique demo catalog.','Linen blend','Gentle wash','images/products/product_039.png',7,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Casual Sets' WHERE c.CategoryName='Sets';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0040','6291000000040','Satin Evening Set',c.CategoryID,sc.SubcategoryID,132.00,74.00,'Satin Evening Set designed for the Lucerne Boutique demo catalog.','Satin','Dry clean','images/products/product_040.png',4,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Formal Sets' WHERE c.CategoryName='Sets';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0041','6291000000041','Ribbed Lounge Set',c.CategoryID,sc.SubcategoryID,86.00,45.00,'Ribbed Lounge Set designed for the Lucerne Boutique demo catalog.','Rib knit','Gentle wash','images/products/product_041.png',5,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Casual Sets' WHERE c.CategoryName='Sets';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0042','6291000000042','Monochrome Office Set',c.CategoryID,sc.SubcategoryID,128.00,72.00,'Monochrome Office Set designed for the Lucerne Boutique demo catalog.','Crepe suiting','Dry clean','images/products/product_042.png',6,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Formal Sets' WHERE c.CategoryName='Sets';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0043','6291000000043','Quilted Shoulder Bag',c.CategoryID,sc.SubcategoryID,62.00,31.00,'Quilted Shoulder Bag designed for the Lucerne Boutique demo catalog.','Vegan leather','Wipe clean','images/products/product_043.png',7,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Bags' WHERE c.CategoryName='Accessories';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0044','6291000000044','Mini Crossbody Bag',c.CategoryID,sc.SubcategoryID,48.00,23.00,'Mini Crossbody Bag designed for the Lucerne Boutique demo catalog.','Vegan leather','Wipe clean','images/products/product_044.png',4,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Bags' WHERE c.CategoryName='Accessories';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0045','6291000000045','Silk Feel Scarf',c.CategoryID,sc.SubcategoryID,25.00,10.00,'Silk Feel Scarf designed for the Lucerne Boutique demo catalog.','Poly silk','Hand wash','images/products/product_045.png',5,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Scarves' WHERE c.CategoryName='Accessories';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0046','6291000000046','Statement Belt',c.CategoryID,sc.SubcategoryID,29.00,12.00,'Statement Belt designed for the Lucerne Boutique demo catalog.','Vegan leather','Wipe clean','images/products/product_046.png',6,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Belts' WHERE c.CategoryName='Accessories';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0047','6291000000047','Pearl Evening Clutch',c.CategoryID,sc.SubcategoryID,58.00,28.00,'Pearl Evening Clutch designed for the Lucerne Boutique demo catalog.','Satin and pearls','Spot clean','images/products/product_047.png',7,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Bags' WHERE c.CategoryName='Accessories';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0048','6291000000048','Printed Modest Scarf',c.CategoryID,sc.SubcategoryID,22.00,9.00,'Printed Modest Scarf designed for the Lucerne Boutique demo catalog.','Viscose','Hand wash','images/products/product_048.png',4,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Scarves' WHERE c.CategoryName='Accessories';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0049','6291000000049','Classic Leather Belt',c.CategoryID,sc.SubcategoryID,34.00,16.00,'Classic Leather Belt designed for the Lucerne Boutique demo catalog.','Vegan leather','Wipe clean','images/products/product_049.png',5,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Belts' WHERE c.CategoryName='Accessories';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0050','6291000000050','Gold Tone Necklace',c.CategoryID,sc.SubcategoryID,38.00,17.00,'Gold Tone Necklace designed for the Lucerne Boutique demo catalog.','Stainless steel','Keep dry','images/products/product_050.png',6,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Jewelry' WHERE c.CategoryName='Accessories';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0051','6291000000051','Pearl Drop Earrings',c.CategoryID,sc.SubcategoryID,26.00,11.00,'Pearl Drop Earrings designed for the Lucerne Boutique demo catalog.','Alloy and pearl','Keep dry','images/products/product_051.png',7,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Jewelry' WHERE c.CategoryName='Accessories';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0052','6291000000052','Minimal Bracelet',c.CategoryID,sc.SubcategoryID,24.00,10.00,'Minimal Bracelet designed for the Lucerne Boutique demo catalog.','Stainless steel','Keep dry','images/products/product_052.png',4,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Jewelry' WHERE c.CategoryName='Accessories';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0053','6291000000053','Ribbed Knit Top',c.CategoryID,sc.SubcategoryID,46.00,24.00,'Ribbed Knit Top designed for the Lucerne Boutique demo catalog.','Viscose knit','Gentle wash','images/products/product_053.png',5,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Tops' WHERE c.CategoryName='Knitwear';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0054','6291000000054','Oversized Cardigan',c.CategoryID,sc.SubcategoryID,69.00,37.00,'Oversized Cardigan designed for the Lucerne Boutique demo catalog.','Acrylic blend','Gentle wash','images/products/product_054.png',6,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Cardigans' WHERE c.CategoryName='Knitwear';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0055','6291000000055','Fine Knit Turtleneck',c.CategoryID,sc.SubcategoryID,48.00,25.00,'Fine Knit Turtleneck designed for the Lucerne Boutique demo catalog.','Knit blend','Gentle wash','images/products/product_055.png',7,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Tops' WHERE c.CategoryName='Knitwear';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0056','6291000000056','Button Front Cardigan',c.CategoryID,sc.SubcategoryID,64.00,34.00,'Button Front Cardigan designed for the Lucerne Boutique demo catalog.','Cotton knit','Gentle wash','images/products/product_056.png',4,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Cardigans' WHERE c.CategoryName='Knitwear';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0057','6291000000057','Cable Knit Sweater',c.CategoryID,sc.SubcategoryID,72.00,39.00,'Cable Knit Sweater designed for the Lucerne Boutique demo catalog.','Acrylic wool blend','Hand wash','images/products/product_057.png',5,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Sweaters' WHERE c.CategoryName='Knitwear';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0058','6291000000058','Soft Crewneck Sweater',c.CategoryID,sc.SubcategoryID,66.00,35.00,'Soft Crewneck Sweater designed for the Lucerne Boutique demo catalog.','Viscose knit','Gentle wash','images/products/product_058.png',6,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Sweaters' WHERE c.CategoryName='Knitwear';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0059','6291000000059','Pleated Maxi Skirt',c.CategoryID,sc.SubcategoryID,58.00,31.00,'Pleated Maxi Skirt designed for the Lucerne Boutique demo catalog.','Chiffon','Hand wash','images/products/product_059.png',7,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Maxi Skirts' WHERE c.CategoryName='Skirts';
INSERT INTO products(SKU,Barcode,Name,CategoryID,SubcategoryID,SellingPrice,CostPrice,Description,Material,CareInstructions,ImagePath,ReorderLevel,CreatedBy,UpdatedBy) SELECT 'LUC-0060','6291000000060','A-Line Midi Skirt',c.CategoryID,sc.SubcategoryID,54.00,28.00,'A-Line Midi Skirt designed for the Lucerne Boutique demo catalog.','Crepe','Gentle wash','images/products/product_060.png',4,1,1 FROM categories c JOIN subcategories sc ON sc.CategoryID=c.CategoryID AND sc.SubcategoryName='Midi Skirts' WHERE c.CategoryName='Skirts';

DELIMITER $$
CREATE PROCEDURE sp_seed_variants_inventory()
BEGIN
 DECLARE pid INT DEFAULT 1; DECLARE sid1 INT; DECLARE sid2 INT; DECLARE sid3 INT; DECLARE cid1 INT; DECLARE cid2 INT; DECLARE cid3 INT; DECLARE cat VARCHAR(80);
 WHILE pid<=60 DO
  SELECT c.CategoryName INTO cat FROM products p JOIN categories c ON c.CategoryID=p.CategoryID WHERE p.ProductID=pid;
  IF cat='Shoes' THEN SELECT SizeID INTO sid1 FROM sizes WHERE SizeValue='37'; SELECT SizeID INTO sid2 FROM sizes WHERE SizeValue='38'; SELECT SizeID INTO sid3 FROM sizes WHERE SizeValue='39';
  ELSEIF cat='Accessories' THEN SELECT SizeID INTO sid1 FROM sizes WHERE SizeValue='ONE SIZE'; SET sid2=sid1; SET sid3=sid1;
  ELSE SELECT SizeID INTO sid1 FROM sizes WHERE SizeValue='S'; SELECT SizeID INTO sid2 FROM sizes WHERE SizeValue='M'; SELECT SizeID INTO sid3 FROM sizes WHERE SizeValue='L'; END IF;
  SET cid1=((pid-1) MOD 10)+1; SET cid2=(pid MOD 10)+1; SET cid3=((pid+1) MOD 10)+1;
  INSERT IGNORE INTO product_variants(ProductID,SizeID,ColorID,VariantSKU,VariantBarcode) VALUES
   (pid,sid1,cid1,CONCAT('LUC-',LPAD(pid,4,'0'),'-A'),CONCAT('729',LPAD(pid,7,'0'),'1')),
   (pid,sid2,cid2,CONCAT('LUC-',LPAD(pid,4,'0'),'-B'),CONCAT('729',LPAD(pid,7,'0'),'2')),
   (pid,sid3,cid3,CONCAT('LUC-',LPAD(pid,4,'0'),'-C'),CONCAT('729',LPAD(pid,7,'0'),'3'));
  SET pid=pid+1;
 END WHILE;
 INSERT INTO branch_inventory(BranchID,VariantID,Quantity,ReorderLevel) SELECT b.BranchID,pv.VariantID,3+MOD(pv.VariantID*b.BranchID,28),5 FROM branches b CROSS JOIN product_variants pv;
 UPDATE branch_inventory SET Quantity=1 WHERE MOD(BranchInventoryID,31)=0;
 UPDATE branch_inventory SET Quantity=0 WHERE MOD(BranchInventoryID,47)=0;
 INSERT INTO warehouse_inventory(WarehouseID,VariantID,Quantity,ReorderLevel) SELECT w.WarehouseID,pv.VariantID,18+MOD(pv.VariantID*w.WarehouseID,45),10 FROM warehouses w CROSS JOIN product_variants pv;
END$$
DELIMITER ;
CALL sp_seed_variants_inventory();
DROP PROCEDURE sp_seed_variants_inventory;
INSERT INTO discounts(Code,Description,Percentage,FixedAmount,MinimumPurchase,MaximumDiscount,StartDate,EndDate,CreatedBy) VALUES ('WELCOME10','10 percent welcome discount',10,0,30,25,CURDATE()-INTERVAL 365 DAY,CURDATE()+INTERVAL 365 DAY,1),('VIP15','VIP customer discount',15,0,100,40,CURDATE()-INTERVAL 365 DAY,CURDATE()+INTERVAL 365 DAY,1),('SAVE20','Fixed promotion',0,20,150,20,CURDATE()-INTERVAL 60 DAY,CURDATE()+INTERVAL 120 DAY,1);
INSERT INTO suppliers(SupplierCode,SupplierName,ContactPerson,Phone,Email,Address) VALUES
('SUP-001','Amman Fashion Supply','Layla Haddad','+970599300001','supplier1@example.com','Palestine'),
('SUP-002','Istanbul Modest Wear','Aylin Kaya','+970599300002','supplier2@example.com','Palestine'),
('SUP-003','Hebron Leather Works','Mariam Nasser','+970599300003','supplier3@example.com','Palestine'),
('SUP-004','Nablus Accessories Co.','Ruba Saleh','+970599300004','supplier4@example.com','Palestine'),
('SUP-005','Mediterranean Textiles','Dina Omar','+970599300005','supplier5@example.com','Palestine'),
('SUP-006','Urban Footwear Group','Samar Ali','+970599300006','supplier6@example.com','Palestine');
INSERT INTO supplier_products(SupplierID,ProductID,SupplierSKU,LastCost,LeadTimeDays) SELECT 1+MOD(ProductID-1,6),ProductID,CONCAT('SUPSKU-',LPAD(ProductID,4,'0')),CostPrice,7+MOD(ProductID,14) FROM products;

DELIMITER $$
CREATE PROCEDURE sp_seed_demo_operations(IN p_sales INT)
BEGIN
 DECLARE i INT DEFAULT 1; DECLARE j INT; DECLARE item_count INT; DECLARE sale_id BIGINT; DECLARE variant_id INT; DECLARE qty INT; DECLARE unit_price DECIMAL(12,2); DECLARE unit_cost DECIMAL(12,2); DECLARE gross DECIMAL(14,2); DECLARE cost_total DECIMAL(14,2); DECLARE disc DECIMAL(14,2); DECLARE net DECIMAL(14,2); DECLARE branch_id INT; DECLARE cashier_id INT; DECLARE customer_id INT; DECLARE sale_time DATETIME; DECLARE receipt VARCHAR(50);
 WHILE i<=p_sales DO
  SET branch_id=1+MOD(i,3); SET cashier_id=CASE branch_id WHEN 1 THEN 5 WHEN 2 THEN 6 ELSE 7 END; SET customer_id=1+MOD(i*7,200); SET sale_time=DATE_SUB(NOW(),INTERVAL MOD(i*13,360) DAY)+INTERVAL MOD(i*17,10) HOUR; SET receipt=CONCAT('DEMO-',LPAD(i,7,'0'));
  INSERT INTO sales(ReceiptNumber,BranchID,CustomerID,CashierUserID,SaleDate,GrossAmount,DiscountAmount,NetAmount,CostAmount,GrossProfit,PaymentMethod,Status) VALUES(receipt,branch_id,customer_id,cashier_id,sale_time,0,0,0,0,0,CASE MOD(i,4) WHEN 0 THEN 'CARD' WHEN 1 THEN 'CASH' WHEN 2 THEN 'CASH' ELSE 'BANK_TRANSFER' END,CASE WHEN MOD(i,97)=0 THEN 'CANCELLED' WHEN MOD(i,53)=0 THEN 'PARTIALLY_RETURNED' ELSE 'COMPLETED' END);
  SET sale_id=LAST_INSERT_ID(); SET gross=0; SET cost_total=0; SET item_count=3; SET j=1;
  WHILE j<=item_count DO
   SET variant_id=1+MOD(i*11+j*17,180); SET qty=1+MOD(i+j,3);
   SELECT p.SellingPrice,p.CostPrice INTO unit_price,unit_cost FROM product_variants pv JOIN products p ON p.ProductID=pv.ProductID WHERE pv.VariantID=variant_id;
   INSERT INTO sale_items(SaleID,VariantID,Quantity,UnitPrice,DiscountAmount,LineTotal,CostAtSale) VALUES(sale_id,variant_id,qty,unit_price,0,unit_price*qty,unit_cost);
   SET gross=gross+unit_price*qty; SET cost_total=cost_total+unit_cost*qty; SET j=j+1;
  END WHILE;
  SET disc=CASE WHEN MOD(i,9)=0 THEN ROUND(gross*0.10,2) WHEN MOD(i,17)=0 THEN 20 ELSE 0 END; IF disc>gross THEN SET disc=gross; END IF; SET net=gross-disc;
  UPDATE sales SET GrossAmount=gross,DiscountAmount=disc,NetAmount=net,CostAmount=cost_total,GrossProfit=net-cost_total,DiscountID=CASE WHEN MOD(i,9)=0 THEN 1 WHEN MOD(i,17)=0 THEN 3 ELSE NULL END WHERE SaleID=sale_id;
  INSERT INTO payments(SaleID,PaymentMethod,Amount,PaidAmount,ChangeAmount,PaymentDate,Status) SELECT SaleID,PaymentMethod,NetAmount,CASE WHEN PaymentMethod='CASH' THEN CEIL(NetAmount/10)*10 ELSE NetAmount END,CASE WHEN PaymentMethod='CASH' THEN CEIL(NetAmount/10)*10-NetAmount ELSE 0 END,SaleDate,'COMPLETED' FROM sales WHERE SaleID=sale_id;
  INSERT INTO cash_drawer_movements(CashierUserID,BranchID,MovementType,Amount,MovementDate,ReferenceNumber,Notes) SELECT CashierUserID,BranchID,'SALE',NetAmount,SaleDate,ReceiptNumber,'Demo transaction' FROM sales WHERE SaleID=sale_id AND PaymentMethod='CASH' AND Status<>'CANCELLED';
  SET i=i+1;
 END WHILE;
END$$
DELIMITER ;
CALL sp_seed_demo_operations(10000);
DROP PROCEDURE sp_seed_demo_operations;
INSERT INTO expenses(ExpenseDate,Category,Description,BranchID,Amount,PaymentMethod,IsRecurring,Status,RecordedBy,ApprovedBy) VALUES
(CURDATE()-INTERVAL 1 DAY,'Utilities','Demo utilities expense 1',2,47.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 2 DAY,'Marketing','Demo marketing expense 2',3,64.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 3 DAY,'Transport','Demo transport expense 3',1,81.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 4 DAY,'Maintenance','Demo maintenance expense 4',2,98.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 5 DAY,'Packaging','Demo packaging expense 5',3,115.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 6 DAY,'Internet','Demo internet expense 6',1,132.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 7 DAY,'Cleaning','Demo cleaning expense 7',2,149.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 8 DAY,'Rent','Demo rent expense 8',3,166.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 9 DAY,'Utilities','Demo utilities expense 9',NULL,183.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 10 DAY,'Marketing','Demo marketing expense 10',2,200.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 11 DAY,'Transport','Demo transport expense 11',3,217.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 12 DAY,'Maintenance','Demo maintenance expense 12',1,234.00,'BANK_TRANSFER',1,'APPROVED',1,1),
(CURDATE()-INTERVAL 13 DAY,'Packaging','Demo packaging expense 13',2,251.00,'BANK_TRANSFER',0,'PENDING',1,NULL),
(CURDATE()-INTERVAL 14 DAY,'Internet','Demo internet expense 14',3,268.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 15 DAY,'Cleaning','Demo cleaning expense 15',1,285.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 16 DAY,'Rent','Demo rent expense 16',2,302.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 17 DAY,'Utilities','Demo utilities expense 17',3,319.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 18 DAY,'Marketing','Demo marketing expense 18',NULL,336.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 19 DAY,'Transport','Demo transport expense 19',2,353.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 20 DAY,'Maintenance','Demo maintenance expense 20',3,370.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 21 DAY,'Packaging','Demo packaging expense 21',1,387.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 22 DAY,'Internet','Demo internet expense 22',2,404.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 23 DAY,'Cleaning','Demo cleaning expense 23',3,421.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 24 DAY,'Rent','Demo rent expense 24',1,438.00,'BANK_TRANSFER',1,'APPROVED',1,1),
(CURDATE()-INTERVAL 25 DAY,'Utilities','Demo utilities expense 25',2,455.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 26 DAY,'Marketing','Demo marketing expense 26',3,472.00,'BANK_TRANSFER',0,'PENDING',1,NULL),
(CURDATE()-INTERVAL 27 DAY,'Transport','Demo transport expense 27',NULL,489.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 28 DAY,'Maintenance','Demo maintenance expense 28',2,506.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 29 DAY,'Packaging','Demo packaging expense 29',3,523.00,'BANK_TRANSFER',0,'REJECTED',1,1),
(CURDATE()-INTERVAL 30 DAY,'Internet','Demo internet expense 30',1,540.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 31 DAY,'Cleaning','Demo cleaning expense 31',2,557.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 32 DAY,'Rent','Demo rent expense 32',3,574.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 33 DAY,'Utilities','Demo utilities expense 33',1,591.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 34 DAY,'Marketing','Demo marketing expense 34',2,608.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 35 DAY,'Transport','Demo transport expense 35',3,625.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 36 DAY,'Maintenance','Demo maintenance expense 36',NULL,642.00,'BANK_TRANSFER',1,'APPROVED',1,1),
(CURDATE()-INTERVAL 37 DAY,'Packaging','Demo packaging expense 37',2,659.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 38 DAY,'Internet','Demo internet expense 38',3,676.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 39 DAY,'Cleaning','Demo cleaning expense 39',1,693.00,'BANK_TRANSFER',0,'PENDING',1,NULL),
(CURDATE()-INTERVAL 40 DAY,'Rent','Demo rent expense 40',2,710.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 41 DAY,'Utilities','Demo utilities expense 41',3,727.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 42 DAY,'Marketing','Demo marketing expense 42',1,744.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 43 DAY,'Transport','Demo transport expense 43',2,41.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 44 DAY,'Maintenance','Demo maintenance expense 44',3,58.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 45 DAY,'Packaging','Demo packaging expense 45',NULL,75.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 46 DAY,'Internet','Demo internet expense 46',2,92.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 47 DAY,'Cleaning','Demo cleaning expense 47',3,109.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 48 DAY,'Rent','Demo rent expense 48',1,126.00,'BANK_TRANSFER',1,'APPROVED',1,1),
(CURDATE()-INTERVAL 49 DAY,'Utilities','Demo utilities expense 49',2,143.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 50 DAY,'Marketing','Demo marketing expense 50',3,160.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 51 DAY,'Transport','Demo transport expense 51',1,177.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 52 DAY,'Maintenance','Demo maintenance expense 52',2,194.00,'BANK_TRANSFER',0,'PENDING',1,NULL),
(CURDATE()-INTERVAL 53 DAY,'Packaging','Demo packaging expense 53',3,211.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 54 DAY,'Internet','Demo internet expense 54',NULL,228.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 55 DAY,'Cleaning','Demo cleaning expense 55',2,245.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 56 DAY,'Rent','Demo rent expense 56',3,262.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 57 DAY,'Utilities','Demo utilities expense 57',1,279.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 58 DAY,'Marketing','Demo marketing expense 58',2,296.00,'BANK_TRANSFER',0,'REJECTED',1,1),
(CURDATE()-INTERVAL 59 DAY,'Transport','Demo transport expense 59',3,313.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 60 DAY,'Maintenance','Demo maintenance expense 60',1,330.00,'BANK_TRANSFER',1,'APPROVED',1,1),
(CURDATE()-INTERVAL 61 DAY,'Packaging','Demo packaging expense 61',2,347.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 62 DAY,'Internet','Demo internet expense 62',3,364.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 63 DAY,'Cleaning','Demo cleaning expense 63',NULL,381.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 64 DAY,'Rent','Demo rent expense 64',2,398.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 65 DAY,'Utilities','Demo utilities expense 65',3,415.00,'BANK_TRANSFER',0,'PENDING',1,NULL),
(CURDATE()-INTERVAL 66 DAY,'Marketing','Demo marketing expense 66',1,432.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 67 DAY,'Transport','Demo transport expense 67',2,449.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 68 DAY,'Maintenance','Demo maintenance expense 68',3,466.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 69 DAY,'Packaging','Demo packaging expense 69',1,483.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 70 DAY,'Internet','Demo internet expense 70',2,500.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 71 DAY,'Cleaning','Demo cleaning expense 71',3,517.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 72 DAY,'Rent','Demo rent expense 72',NULL,534.00,'BANK_TRANSFER',1,'APPROVED',1,1),
(CURDATE()-INTERVAL 73 DAY,'Utilities','Demo utilities expense 73',2,551.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 74 DAY,'Marketing','Demo marketing expense 74',3,568.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 75 DAY,'Transport','Demo transport expense 75',1,585.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 76 DAY,'Maintenance','Demo maintenance expense 76',2,602.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 77 DAY,'Packaging','Demo packaging expense 77',3,619.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 78 DAY,'Internet','Demo internet expense 78',1,636.00,'BANK_TRANSFER',0,'PENDING',1,NULL),
(CURDATE()-INTERVAL 79 DAY,'Cleaning','Demo cleaning expense 79',2,653.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 80 DAY,'Rent','Demo rent expense 80',3,670.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 81 DAY,'Utilities','Demo utilities expense 81',NULL,687.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 82 DAY,'Marketing','Demo marketing expense 82',2,704.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 83 DAY,'Transport','Demo transport expense 83',3,721.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 84 DAY,'Maintenance','Demo maintenance expense 84',1,738.00,'BANK_TRANSFER',1,'APPROVED',1,1),
(CURDATE()-INTERVAL 85 DAY,'Packaging','Demo packaging expense 85',2,35.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 86 DAY,'Internet','Demo internet expense 86',3,52.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 87 DAY,'Cleaning','Demo cleaning expense 87',1,69.00,'BANK_TRANSFER',0,'REJECTED',1,1),
(CURDATE()-INTERVAL 88 DAY,'Rent','Demo rent expense 88',2,86.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 89 DAY,'Utilities','Demo utilities expense 89',3,103.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 90 DAY,'Marketing','Demo marketing expense 90',NULL,120.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 91 DAY,'Transport','Demo transport expense 91',2,137.00,'BANK_TRANSFER',0,'PENDING',1,NULL),
(CURDATE()-INTERVAL 92 DAY,'Maintenance','Demo maintenance expense 92',3,154.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 93 DAY,'Packaging','Demo packaging expense 93',1,171.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 94 DAY,'Internet','Demo internet expense 94',2,188.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 95 DAY,'Cleaning','Demo cleaning expense 95',3,205.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 96 DAY,'Rent','Demo rent expense 96',1,222.00,'BANK_TRANSFER',1,'APPROVED',1,1),
(CURDATE()-INTERVAL 97 DAY,'Utilities','Demo utilities expense 97',2,239.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 98 DAY,'Marketing','Demo marketing expense 98',3,256.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 99 DAY,'Transport','Demo transport expense 99',NULL,273.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 100 DAY,'Maintenance','Demo maintenance expense 100',2,290.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 101 DAY,'Packaging','Demo packaging expense 101',3,307.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 102 DAY,'Internet','Demo internet expense 102',1,324.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 103 DAY,'Cleaning','Demo cleaning expense 103',2,341.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 104 DAY,'Rent','Demo rent expense 104',3,358.00,'BANK_TRANSFER',0,'PENDING',1,NULL),
(CURDATE()-INTERVAL 105 DAY,'Utilities','Demo utilities expense 105',1,375.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 106 DAY,'Marketing','Demo marketing expense 106',2,392.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 107 DAY,'Transport','Demo transport expense 107',3,409.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 108 DAY,'Maintenance','Demo maintenance expense 108',NULL,426.00,'BANK_TRANSFER',1,'APPROVED',1,1),
(CURDATE()-INTERVAL 109 DAY,'Packaging','Demo packaging expense 109',2,443.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 110 DAY,'Internet','Demo internet expense 110',3,460.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 111 DAY,'Cleaning','Demo cleaning expense 111',1,477.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 112 DAY,'Rent','Demo rent expense 112',2,494.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 113 DAY,'Utilities','Demo utilities expense 113',3,511.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 114 DAY,'Marketing','Demo marketing expense 114',1,528.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 115 DAY,'Transport','Demo transport expense 115',2,545.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 116 DAY,'Maintenance','Demo maintenance expense 116',3,562.00,'BANK_TRANSFER',0,'REJECTED',1,1),
(CURDATE()-INTERVAL 117 DAY,'Packaging','Demo packaging expense 117',NULL,579.00,'BANK_TRANSFER',0,'PENDING',1,NULL),
(CURDATE()-INTERVAL 118 DAY,'Internet','Demo internet expense 118',2,596.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 119 DAY,'Cleaning','Demo cleaning expense 119',3,613.00,'BANK_TRANSFER',0,'APPROVED',1,1),
(CURDATE()-INTERVAL 120 DAY,'Rent','Demo rent expense 120',1,630.00,'BANK_TRANSFER',1,'APPROVED',1,1);
INSERT INTO online_orders(OrderNumber,CustomerID,BranchID,OrderDate,OrderType,DeliveryType,DeliveryAddress,PaymentStatus,Status,Subtotal,DiscountAmount,DeliveryFee,TotalAmount,AssignedEmployeeID,Notes) VALUES
('ORD-000001',6,2,NOW()-INTERVAL 1 DAY,'ONLINE','DELIVERY','Demo address 1','PENDING','CONFIRMED',68,0,5,73,8,'Demo order'),
('ORD-000002',11,3,NOW()-INTERVAL 2 DAY,'ONLINE','PICKUP','Demo address 2','PENDING','PROCESSING',81,0,0,81,7,'Demo order'),
('ORD-000003',16,1,NOW()-INTERVAL 3 DAY,'ONLINE','DELIVERY','Demo address 3','PAID','READY',94,0,5,99,8,'Demo order'),
('ORD-000004',21,2,NOW()-INTERVAL 4 DAY,'ONLINE','PICKUP','Demo address 4','PAID','DELIVERED',107,0,0,107,7,'Demo order'),
('ORD-000005',26,3,NOW()-INTERVAL 5 DAY,'ONLINE','DELIVERY','Demo address 5','PENDING','CANCELLED',120,0,5,125,8,'Demo order'),
('ORD-000006',31,1,NOW()-INTERVAL 6 DAY,'ONLINE','PICKUP','Demo address 6','PENDING','PENDING',133,0,0,133,7,'Demo order'),
('ORD-000007',36,2,NOW()-INTERVAL 7 DAY,'ONLINE','DELIVERY','Demo address 7','PENDING','CONFIRMED',146,0,5,151,8,'Demo order'),
('ORD-000008',41,3,NOW()-INTERVAL 8 DAY,'ONLINE','PICKUP','Demo address 8','PENDING','PROCESSING',159,0,0,159,7,'Demo order'),
('ORD-000009',46,1,NOW()-INTERVAL 9 DAY,'ONLINE','DELIVERY','Demo address 9','PAID','READY',172,0,5,177,8,'Demo order'),
('ORD-000010',51,2,NOW()-INTERVAL 10 DAY,'ONLINE','PICKUP','Demo address 10','PAID','DELIVERED',185,0,0,185,7,'Demo order'),
('ORD-000011',56,3,NOW()-INTERVAL 11 DAY,'ONLINE','DELIVERY','Demo address 11','PENDING','CANCELLED',198,0,5,203,8,'Demo order'),
('ORD-000012',61,1,NOW()-INTERVAL 12 DAY,'ONLINE','PICKUP','Demo address 12','PENDING','PENDING',211,0,0,211,7,'Demo order'),
('ORD-000013',66,2,NOW()-INTERVAL 13 DAY,'ONLINE','DELIVERY','Demo address 13','PENDING','CONFIRMED',224,0,5,229,8,'Demo order'),
('ORD-000014',71,3,NOW()-INTERVAL 14 DAY,'ONLINE','PICKUP','Demo address 14','PENDING','PROCESSING',237,0,0,237,7,'Demo order'),
('ORD-000015',76,1,NOW()-INTERVAL 15 DAY,'ONLINE','DELIVERY','Demo address 15','PAID','READY',250,0,5,255,8,'Demo order'),
('ORD-000016',81,2,NOW()-INTERVAL 16 DAY,'ONLINE','PICKUP','Demo address 16','PAID','DELIVERED',263,0,0,263,7,'Demo order'),
('ORD-000017',86,3,NOW()-INTERVAL 17 DAY,'ONLINE','DELIVERY','Demo address 17','PENDING','CANCELLED',276,0,5,281,8,'Demo order'),
('ORD-000018',91,1,NOW()-INTERVAL 18 DAY,'ONLINE','PICKUP','Demo address 18','PENDING','PENDING',289,0,0,289,7,'Demo order'),
('ORD-000019',96,2,NOW()-INTERVAL 19 DAY,'ONLINE','DELIVERY','Demo address 19','PENDING','CONFIRMED',302,0,5,307,8,'Demo order'),
('ORD-000020',101,3,NOW()-INTERVAL 20 DAY,'ONLINE','PICKUP','Demo address 20','PENDING','PROCESSING',315,0,0,315,7,'Demo order'),
('ORD-000021',106,1,NOW()-INTERVAL 21 DAY,'ONLINE','DELIVERY','Demo address 21','PAID','READY',328,0,5,333,8,'Demo order'),
('ORD-000022',111,2,NOW()-INTERVAL 22 DAY,'ONLINE','PICKUP','Demo address 22','PAID','DELIVERED',341,0,0,341,7,'Demo order'),
('ORD-000023',116,3,NOW()-INTERVAL 23 DAY,'ONLINE','DELIVERY','Demo address 23','PENDING','CANCELLED',354,0,5,359,8,'Demo order'),
('ORD-000024',121,1,NOW()-INTERVAL 24 DAY,'ONLINE','PICKUP','Demo address 24','PENDING','PENDING',57,0,0,57,7,'Demo order'),
('ORD-000025',126,2,NOW()-INTERVAL 25 DAY,'ONLINE','DELIVERY','Demo address 25','PENDING','CONFIRMED',70,0,5,75,8,'Demo order'),
('ORD-000026',131,3,NOW()-INTERVAL 26 DAY,'ONLINE','PICKUP','Demo address 26','PENDING','PROCESSING',83,0,0,83,7,'Demo order'),
('ORD-000027',136,1,NOW()-INTERVAL 27 DAY,'ONLINE','DELIVERY','Demo address 27','PAID','READY',96,0,5,101,8,'Demo order'),
('ORD-000028',141,2,NOW()-INTERVAL 28 DAY,'ONLINE','PICKUP','Demo address 28','PAID','DELIVERED',109,0,0,109,7,'Demo order'),
('ORD-000029',146,3,NOW()-INTERVAL 29 DAY,'ONLINE','DELIVERY','Demo address 29','PENDING','CANCELLED',122,0,5,127,8,'Demo order'),
('ORD-000030',151,1,NOW()-INTERVAL 30 DAY,'ONLINE','PICKUP','Demo address 30','PENDING','PENDING',135,0,0,135,7,'Demo order'),
('ORD-000031',156,2,NOW()-INTERVAL 31 DAY,'ONLINE','DELIVERY','Demo address 31','PENDING','CONFIRMED',148,0,5,153,8,'Demo order'),
('ORD-000032',161,3,NOW()-INTERVAL 32 DAY,'ONLINE','PICKUP','Demo address 32','PENDING','PROCESSING',161,0,0,161,7,'Demo order'),
('ORD-000033',166,1,NOW()-INTERVAL 33 DAY,'ONLINE','DELIVERY','Demo address 33','PAID','READY',174,0,5,179,8,'Demo order'),
('ORD-000034',171,2,NOW()-INTERVAL 34 DAY,'ONLINE','PICKUP','Demo address 34','PAID','DELIVERED',187,0,0,187,7,'Demo order'),
('ORD-000035',176,3,NOW()-INTERVAL 35 DAY,'ONLINE','DELIVERY','Demo address 35','PENDING','CANCELLED',200,0,5,205,8,'Demo order'),
('ORD-000036',181,1,NOW()-INTERVAL 36 DAY,'ONLINE','PICKUP','Demo address 36','PENDING','PENDING',213,0,0,213,7,'Demo order'),
('ORD-000037',186,2,NOW()-INTERVAL 37 DAY,'ONLINE','DELIVERY','Demo address 37','PENDING','CONFIRMED',226,0,5,231,8,'Demo order'),
('ORD-000038',191,3,NOW()-INTERVAL 38 DAY,'ONLINE','PICKUP','Demo address 38','PENDING','PROCESSING',239,0,0,239,7,'Demo order'),
('ORD-000039',196,1,NOW()-INTERVAL 39 DAY,'ONLINE','DELIVERY','Demo address 39','PAID','READY',252,0,5,257,8,'Demo order'),
('ORD-000040',1,2,NOW()-INTERVAL 40 DAY,'ONLINE','PICKUP','Demo address 40','PAID','DELIVERED',265,0,0,265,7,'Demo order'),
('ORD-000041',6,3,NOW()-INTERVAL 41 DAY,'ONLINE','DELIVERY','Demo address 41','PENDING','CANCELLED',278,0,5,283,8,'Demo order'),
('ORD-000042',11,1,NOW()-INTERVAL 42 DAY,'ONLINE','PICKUP','Demo address 42','PENDING','PENDING',291,0,0,291,7,'Demo order'),
('ORD-000043',16,2,NOW()-INTERVAL 43 DAY,'ONLINE','DELIVERY','Demo address 43','PENDING','CONFIRMED',304,0,5,309,8,'Demo order'),
('ORD-000044',21,3,NOW()-INTERVAL 44 DAY,'ONLINE','PICKUP','Demo address 44','PENDING','PROCESSING',317,0,0,317,7,'Demo order'),
('ORD-000045',26,1,NOW()-INTERVAL 45 DAY,'ONLINE','DELIVERY','Demo address 45','PAID','READY',330,0,5,335,8,'Demo order'),
('ORD-000046',31,2,NOW()-INTERVAL 46 DAY,'ONLINE','PICKUP','Demo address 46','PAID','DELIVERED',343,0,0,343,7,'Demo order'),
('ORD-000047',36,3,NOW()-INTERVAL 47 DAY,'ONLINE','DELIVERY','Demo address 47','PENDING','CANCELLED',356,0,5,361,8,'Demo order'),
('ORD-000048',41,1,NOW()-INTERVAL 48 DAY,'ONLINE','PICKUP','Demo address 48','PENDING','PENDING',59,0,0,59,7,'Demo order'),
('ORD-000049',46,2,NOW()-INTERVAL 49 DAY,'ONLINE','DELIVERY','Demo address 49','PENDING','CONFIRMED',72,0,5,77,8,'Demo order'),
('ORD-000050',51,3,NOW()-INTERVAL 50 DAY,'ONLINE','PICKUP','Demo address 50','PENDING','PROCESSING',85,0,0,85,7,'Demo order'),
('ORD-000051',56,1,NOW()-INTERVAL 51 DAY,'ONLINE','DELIVERY','Demo address 51','PAID','READY',98,0,5,103,8,'Demo order'),
('ORD-000052',61,2,NOW()-INTERVAL 52 DAY,'ONLINE','PICKUP','Demo address 52','PAID','DELIVERED',111,0,0,111,7,'Demo order'),
('ORD-000053',66,3,NOW()-INTERVAL 53 DAY,'ONLINE','DELIVERY','Demo address 53','PENDING','CANCELLED',124,0,5,129,8,'Demo order'),
('ORD-000054',71,1,NOW()-INTERVAL 54 DAY,'ONLINE','PICKUP','Demo address 54','PENDING','PENDING',137,0,0,137,7,'Demo order'),
('ORD-000055',76,2,NOW()-INTERVAL 55 DAY,'ONLINE','DELIVERY','Demo address 55','PENDING','CONFIRMED',150,0,5,155,8,'Demo order'),
('ORD-000056',81,3,NOW()-INTERVAL 56 DAY,'ONLINE','PICKUP','Demo address 56','PENDING','PROCESSING',163,0,0,163,7,'Demo order'),
('ORD-000057',86,1,NOW()-INTERVAL 57 DAY,'ONLINE','DELIVERY','Demo address 57','PAID','READY',176,0,5,181,8,'Demo order'),
('ORD-000058',91,2,NOW()-INTERVAL 58 DAY,'ONLINE','PICKUP','Demo address 58','PAID','DELIVERED',189,0,0,189,7,'Demo order'),
('ORD-000059',96,3,NOW()-INTERVAL 59 DAY,'ONLINE','DELIVERY','Demo address 59','PENDING','CANCELLED',202,0,5,207,8,'Demo order'),
('ORD-000060',101,1,NOW()-INTERVAL 60 DAY,'ONLINE','PICKUP','Demo address 60','PENDING','PENDING',215,0,0,215,7,'Demo order'),
('ORD-000061',106,2,NOW()-INTERVAL 61 DAY,'ONLINE','DELIVERY','Demo address 61','PENDING','CONFIRMED',228,0,5,233,8,'Demo order'),
('ORD-000062',111,3,NOW()-INTERVAL 62 DAY,'ONLINE','PICKUP','Demo address 62','PENDING','PROCESSING',241,0,0,241,7,'Demo order'),
('ORD-000063',116,1,NOW()-INTERVAL 63 DAY,'ONLINE','DELIVERY','Demo address 63','PAID','READY',254,0,5,259,8,'Demo order'),
('ORD-000064',121,2,NOW()-INTERVAL 64 DAY,'ONLINE','PICKUP','Demo address 64','PAID','DELIVERED',267,0,0,267,7,'Demo order'),
('ORD-000065',126,3,NOW()-INTERVAL 65 DAY,'ONLINE','DELIVERY','Demo address 65','PENDING','CANCELLED',280,0,5,285,8,'Demo order'),
('ORD-000066',131,1,NOW()-INTERVAL 66 DAY,'ONLINE','PICKUP','Demo address 66','PENDING','PENDING',293,0,0,293,7,'Demo order'),
('ORD-000067',136,2,NOW()-INTERVAL 67 DAY,'ONLINE','DELIVERY','Demo address 67','PENDING','CONFIRMED',306,0,5,311,8,'Demo order'),
('ORD-000068',141,3,NOW()-INTERVAL 68 DAY,'ONLINE','PICKUP','Demo address 68','PENDING','PROCESSING',319,0,0,319,7,'Demo order'),
('ORD-000069',146,1,NOW()-INTERVAL 69 DAY,'ONLINE','DELIVERY','Demo address 69','PAID','READY',332,0,5,337,8,'Demo order'),
('ORD-000070',151,2,NOW()-INTERVAL 70 DAY,'ONLINE','PICKUP','Demo address 70','PAID','DELIVERED',345,0,0,345,7,'Demo order'),
('ORD-000071',156,3,NOW()-INTERVAL 71 DAY,'ONLINE','DELIVERY','Demo address 71','PENDING','CANCELLED',358,0,5,363,8,'Demo order'),
('ORD-000072',161,1,NOW()-INTERVAL 72 DAY,'ONLINE','PICKUP','Demo address 72','PENDING','PENDING',61,0,0,61,7,'Demo order'),
('ORD-000073',166,2,NOW()-INTERVAL 73 DAY,'ONLINE','DELIVERY','Demo address 73','PENDING','CONFIRMED',74,0,5,79,8,'Demo order'),
('ORD-000074',171,3,NOW()-INTERVAL 74 DAY,'ONLINE','PICKUP','Demo address 74','PENDING','PROCESSING',87,0,0,87,7,'Demo order'),
('ORD-000075',176,1,NOW()-INTERVAL 75 DAY,'ONLINE','DELIVERY','Demo address 75','PAID','READY',100,0,5,105,8,'Demo order'),
('ORD-000076',181,2,NOW()-INTERVAL 76 DAY,'ONLINE','PICKUP','Demo address 76','PAID','DELIVERED',113,0,0,113,7,'Demo order'),
('ORD-000077',186,3,NOW()-INTERVAL 77 DAY,'ONLINE','DELIVERY','Demo address 77','PENDING','CANCELLED',126,0,5,131,8,'Demo order'),
('ORD-000078',191,1,NOW()-INTERVAL 78 DAY,'ONLINE','PICKUP','Demo address 78','PENDING','PENDING',139,0,0,139,7,'Demo order'),
('ORD-000079',196,2,NOW()-INTERVAL 79 DAY,'ONLINE','DELIVERY','Demo address 79','PENDING','CONFIRMED',152,0,5,157,8,'Demo order'),
('ORD-000080',1,3,NOW()-INTERVAL 80 DAY,'ONLINE','PICKUP','Demo address 80','PENDING','PROCESSING',165,0,0,165,7,'Demo order'),
('ORD-000081',6,1,NOW()-INTERVAL 81 DAY,'ONLINE','DELIVERY','Demo address 81','PAID','READY',178,0,5,183,8,'Demo order'),
('ORD-000082',11,2,NOW()-INTERVAL 82 DAY,'ONLINE','PICKUP','Demo address 82','PAID','DELIVERED',191,0,0,191,7,'Demo order'),
('ORD-000083',16,3,NOW()-INTERVAL 83 DAY,'ONLINE','DELIVERY','Demo address 83','PENDING','CANCELLED',204,0,5,209,8,'Demo order'),
('ORD-000084',21,1,NOW()-INTERVAL 84 DAY,'ONLINE','PICKUP','Demo address 84','PENDING','PENDING',217,0,0,217,7,'Demo order'),
('ORD-000085',26,2,NOW()-INTERVAL 85 DAY,'ONLINE','DELIVERY','Demo address 85','PENDING','CONFIRMED',230,0,5,235,8,'Demo order'),
('ORD-000086',31,3,NOW()-INTERVAL 86 DAY,'ONLINE','PICKUP','Demo address 86','PENDING','PROCESSING',243,0,0,243,7,'Demo order'),
('ORD-000087',36,1,NOW()-INTERVAL 87 DAY,'ONLINE','DELIVERY','Demo address 87','PAID','READY',256,0,5,261,8,'Demo order'),
('ORD-000088',41,2,NOW()-INTERVAL 88 DAY,'ONLINE','PICKUP','Demo address 88','PAID','DELIVERED',269,0,0,269,7,'Demo order'),
('ORD-000089',46,3,NOW()-INTERVAL 89 DAY,'ONLINE','DELIVERY','Demo address 89','PENDING','CANCELLED',282,0,5,287,8,'Demo order'),
('ORD-000090',51,1,NOW()-INTERVAL 90 DAY,'ONLINE','PICKUP','Demo address 90','PENDING','PENDING',295,0,0,295,7,'Demo order'),
('ORD-000091',56,2,NOW()-INTERVAL 91 DAY,'ONLINE','DELIVERY','Demo address 91','PENDING','CONFIRMED',308,0,5,313,8,'Demo order'),
('ORD-000092',61,3,NOW()-INTERVAL 92 DAY,'ONLINE','PICKUP','Demo address 92','PENDING','PROCESSING',321,0,0,321,7,'Demo order'),
('ORD-000093',66,1,NOW()-INTERVAL 93 DAY,'ONLINE','DELIVERY','Demo address 93','PAID','READY',334,0,5,339,8,'Demo order'),
('ORD-000094',71,2,NOW()-INTERVAL 94 DAY,'ONLINE','PICKUP','Demo address 94','PAID','DELIVERED',347,0,0,347,7,'Demo order'),
('ORD-000095',76,3,NOW()-INTERVAL 95 DAY,'ONLINE','DELIVERY','Demo address 95','PENDING','CANCELLED',360,0,5,365,8,'Demo order'),
('ORD-000096',81,1,NOW()-INTERVAL 96 DAY,'ONLINE','PICKUP','Demo address 96','PENDING','PENDING',63,0,0,63,7,'Demo order'),
('ORD-000097',86,2,NOW()-INTERVAL 97 DAY,'ONLINE','DELIVERY','Demo address 97','PENDING','CONFIRMED',76,0,5,81,8,'Demo order'),
('ORD-000098',91,3,NOW()-INTERVAL 98 DAY,'ONLINE','PICKUP','Demo address 98','PENDING','PROCESSING',89,0,0,89,7,'Demo order'),
('ORD-000099',96,1,NOW()-INTERVAL 99 DAY,'ONLINE','DELIVERY','Demo address 99','PAID','READY',102,0,5,107,8,'Demo order'),
('ORD-000100',101,2,NOW()-INTERVAL 100 DAY,'ONLINE','PICKUP','Demo address 100','PAID','DELIVERED',115,0,0,115,7,'Demo order'),
('ORD-000101',106,3,NOW()-INTERVAL 101 DAY,'ONLINE','DELIVERY','Demo address 101','PENDING','CANCELLED',128,0,5,133,8,'Demo order'),
('ORD-000102',111,1,NOW()-INTERVAL 102 DAY,'ONLINE','PICKUP','Demo address 102','PENDING','PENDING',141,0,0,141,7,'Demo order'),
('ORD-000103',116,2,NOW()-INTERVAL 103 DAY,'ONLINE','DELIVERY','Demo address 103','PENDING','CONFIRMED',154,0,5,159,8,'Demo order'),
('ORD-000104',121,3,NOW()-INTERVAL 104 DAY,'ONLINE','PICKUP','Demo address 104','PENDING','PROCESSING',167,0,0,167,7,'Demo order'),
('ORD-000105',126,1,NOW()-INTERVAL 105 DAY,'ONLINE','DELIVERY','Demo address 105','PAID','READY',180,0,5,185,8,'Demo order'),
('ORD-000106',131,2,NOW()-INTERVAL 106 DAY,'ONLINE','PICKUP','Demo address 106','PAID','DELIVERED',193,0,0,193,7,'Demo order'),
('ORD-000107',136,3,NOW()-INTERVAL 107 DAY,'ONLINE','DELIVERY','Demo address 107','PENDING','CANCELLED',206,0,5,211,8,'Demo order'),
('ORD-000108',141,1,NOW()-INTERVAL 108 DAY,'ONLINE','PICKUP','Demo address 108','PENDING','PENDING',219,0,0,219,7,'Demo order'),
('ORD-000109',146,2,NOW()-INTERVAL 109 DAY,'ONLINE','DELIVERY','Demo address 109','PENDING','CONFIRMED',232,0,5,237,8,'Demo order'),
('ORD-000110',151,3,NOW()-INTERVAL 110 DAY,'ONLINE','PICKUP','Demo address 110','PENDING','PROCESSING',245,0,0,245,7,'Demo order'),
('ORD-000111',156,1,NOW()-INTERVAL 111 DAY,'ONLINE','DELIVERY','Demo address 111','PAID','READY',258,0,5,263,8,'Demo order'),
('ORD-000112',161,2,NOW()-INTERVAL 112 DAY,'ONLINE','PICKUP','Demo address 112','PAID','DELIVERED',271,0,0,271,7,'Demo order'),
('ORD-000113',166,3,NOW()-INTERVAL 113 DAY,'ONLINE','DELIVERY','Demo address 113','PENDING','CANCELLED',284,0,5,289,8,'Demo order'),
('ORD-000114',171,1,NOW()-INTERVAL 114 DAY,'ONLINE','PICKUP','Demo address 114','PENDING','PENDING',297,0,0,297,7,'Demo order'),
('ORD-000115',176,2,NOW()-INTERVAL 115 DAY,'ONLINE','DELIVERY','Demo address 115','PENDING','CONFIRMED',310,0,5,315,8,'Demo order'),
('ORD-000116',181,3,NOW()-INTERVAL 116 DAY,'ONLINE','PICKUP','Demo address 116','PENDING','PROCESSING',323,0,0,323,7,'Demo order'),
('ORD-000117',186,1,NOW()-INTERVAL 117 DAY,'ONLINE','DELIVERY','Demo address 117','PAID','READY',336,0,5,341,8,'Demo order'),
('ORD-000118',191,2,NOW()-INTERVAL 118 DAY,'ONLINE','PICKUP','Demo address 118','PAID','DELIVERED',349,0,0,349,7,'Demo order'),
('ORD-000119',196,3,NOW()-INTERVAL 119 DAY,'ONLINE','DELIVERY','Demo address 119','PENDING','CANCELLED',362,0,5,367,8,'Demo order'),
('ORD-000120',1,1,NOW()-INTERVAL 120 DAY,'ONLINE','PICKUP','Demo address 120','PENDING','PENDING',65,0,0,65,7,'Demo order'),
('ORD-000121',6,2,NOW()-INTERVAL 121 DAY,'ONLINE','DELIVERY','Demo address 121','PENDING','CONFIRMED',78,0,5,83,8,'Demo order'),
('ORD-000122',11,3,NOW()-INTERVAL 122 DAY,'ONLINE','PICKUP','Demo address 122','PENDING','PROCESSING',91,0,0,91,7,'Demo order'),
('ORD-000123',16,1,NOW()-INTERVAL 123 DAY,'ONLINE','DELIVERY','Demo address 123','PAID','READY',104,0,5,109,8,'Demo order'),
('ORD-000124',21,2,NOW()-INTERVAL 124 DAY,'ONLINE','PICKUP','Demo address 124','PAID','DELIVERED',117,0,0,117,7,'Demo order'),
('ORD-000125',26,3,NOW()-INTERVAL 125 DAY,'ONLINE','DELIVERY','Demo address 125','PENDING','CANCELLED',130,0,5,135,8,'Demo order'),
('ORD-000126',31,1,NOW()-INTERVAL 126 DAY,'ONLINE','PICKUP','Demo address 126','PENDING','PENDING',143,0,0,143,7,'Demo order'),
('ORD-000127',36,2,NOW()-INTERVAL 127 DAY,'ONLINE','DELIVERY','Demo address 127','PENDING','CONFIRMED',156,0,5,161,8,'Demo order'),
('ORD-000128',41,3,NOW()-INTERVAL 128 DAY,'ONLINE','PICKUP','Demo address 128','PENDING','PROCESSING',169,0,0,169,7,'Demo order'),
('ORD-000129',46,1,NOW()-INTERVAL 129 DAY,'ONLINE','DELIVERY','Demo address 129','PAID','READY',182,0,5,187,8,'Demo order'),
('ORD-000130',51,2,NOW()-INTERVAL 130 DAY,'ONLINE','PICKUP','Demo address 130','PAID','DELIVERED',195,0,0,195,7,'Demo order'),
('ORD-000131',56,3,NOW()-INTERVAL 131 DAY,'ONLINE','DELIVERY','Demo address 131','PENDING','CANCELLED',208,0,5,213,8,'Demo order'),
('ORD-000132',61,1,NOW()-INTERVAL 132 DAY,'ONLINE','PICKUP','Demo address 132','PENDING','PENDING',221,0,0,221,7,'Demo order'),
('ORD-000133',66,2,NOW()-INTERVAL 133 DAY,'ONLINE','DELIVERY','Demo address 133','PENDING','CONFIRMED',234,0,5,239,8,'Demo order'),
('ORD-000134',71,3,NOW()-INTERVAL 134 DAY,'ONLINE','PICKUP','Demo address 134','PENDING','PROCESSING',247,0,0,247,7,'Demo order'),
('ORD-000135',76,1,NOW()-INTERVAL 135 DAY,'ONLINE','DELIVERY','Demo address 135','PAID','READY',260,0,5,265,8,'Demo order'),
('ORD-000136',81,2,NOW()-INTERVAL 136 DAY,'ONLINE','PICKUP','Demo address 136','PAID','DELIVERED',273,0,0,273,7,'Demo order'),
('ORD-000137',86,3,NOW()-INTERVAL 137 DAY,'ONLINE','DELIVERY','Demo address 137','PENDING','CANCELLED',286,0,5,291,8,'Demo order'),
('ORD-000138',91,1,NOW()-INTERVAL 138 DAY,'ONLINE','PICKUP','Demo address 138','PENDING','PENDING',299,0,0,299,7,'Demo order'),
('ORD-000139',96,2,NOW()-INTERVAL 139 DAY,'ONLINE','DELIVERY','Demo address 139','PENDING','CONFIRMED',312,0,5,317,8,'Demo order'),
('ORD-000140',101,3,NOW()-INTERVAL 140 DAY,'ONLINE','PICKUP','Demo address 140','PENDING','PROCESSING',325,0,0,325,7,'Demo order'),
('ORD-000141',106,1,NOW()-INTERVAL 141 DAY,'ONLINE','DELIVERY','Demo address 141','PAID','READY',338,0,5,343,8,'Demo order'),
('ORD-000142',111,2,NOW()-INTERVAL 142 DAY,'ONLINE','PICKUP','Demo address 142','PAID','DELIVERED',351,0,0,351,7,'Demo order'),
('ORD-000143',116,3,NOW()-INTERVAL 143 DAY,'ONLINE','DELIVERY','Demo address 143','PENDING','CANCELLED',364,0,5,369,8,'Demo order'),
('ORD-000144',121,1,NOW()-INTERVAL 144 DAY,'ONLINE','PICKUP','Demo address 144','PENDING','PENDING',67,0,0,67,7,'Demo order'),
('ORD-000145',126,2,NOW()-INTERVAL 145 DAY,'ONLINE','DELIVERY','Demo address 145','PENDING','CONFIRMED',80,0,5,85,8,'Demo order'),
('ORD-000146',131,3,NOW()-INTERVAL 146 DAY,'ONLINE','PICKUP','Demo address 146','PENDING','PROCESSING',93,0,0,93,7,'Demo order'),
('ORD-000147',136,1,NOW()-INTERVAL 147 DAY,'ONLINE','DELIVERY','Demo address 147','PAID','READY',106,0,5,111,8,'Demo order'),
('ORD-000148',141,2,NOW()-INTERVAL 148 DAY,'ONLINE','PICKUP','Demo address 148','PAID','DELIVERED',119,0,0,119,7,'Demo order'),
('ORD-000149',146,3,NOW()-INTERVAL 149 DAY,'ONLINE','DELIVERY','Demo address 149','PENDING','CANCELLED',132,0,5,137,8,'Demo order'),
('ORD-000150',151,1,NOW()-INTERVAL 150 DAY,'ONLINE','PICKUP','Demo address 150','PENDING','PENDING',145,0,0,145,7,'Demo order'),
('ORD-000151',156,2,NOW()-INTERVAL 151 DAY,'ONLINE','DELIVERY','Demo address 151','PENDING','CONFIRMED',158,0,5,163,8,'Demo order'),
('ORD-000152',161,3,NOW()-INTERVAL 152 DAY,'ONLINE','PICKUP','Demo address 152','PENDING','PROCESSING',171,0,0,171,7,'Demo order'),
('ORD-000153',166,1,NOW()-INTERVAL 153 DAY,'ONLINE','DELIVERY','Demo address 153','PAID','READY',184,0,5,189,8,'Demo order'),
('ORD-000154',171,2,NOW()-INTERVAL 154 DAY,'ONLINE','PICKUP','Demo address 154','PAID','DELIVERED',197,0,0,197,7,'Demo order'),
('ORD-000155',176,3,NOW()-INTERVAL 155 DAY,'ONLINE','DELIVERY','Demo address 155','PENDING','CANCELLED',210,0,5,215,8,'Demo order'),
('ORD-000156',181,1,NOW()-INTERVAL 156 DAY,'ONLINE','PICKUP','Demo address 156','PENDING','PENDING',223,0,0,223,7,'Demo order'),
('ORD-000157',186,2,NOW()-INTERVAL 157 DAY,'ONLINE','DELIVERY','Demo address 157','PENDING','CONFIRMED',236,0,5,241,8,'Demo order'),
('ORD-000158',191,3,NOW()-INTERVAL 158 DAY,'ONLINE','PICKUP','Demo address 158','PENDING','PROCESSING',249,0,0,249,7,'Demo order'),
('ORD-000159',196,1,NOW()-INTERVAL 159 DAY,'ONLINE','DELIVERY','Demo address 159','PAID','READY',262,0,5,267,8,'Demo order'),
('ORD-000160',1,2,NOW()-INTERVAL 160 DAY,'ONLINE','PICKUP','Demo address 160','PAID','DELIVERED',275,0,0,275,7,'Demo order'),
('ORD-000161',6,3,NOW()-INTERVAL 161 DAY,'ONLINE','DELIVERY','Demo address 161','PENDING','CANCELLED',288,0,5,293,8,'Demo order'),
('ORD-000162',11,1,NOW()-INTERVAL 162 DAY,'ONLINE','PICKUP','Demo address 162','PENDING','PENDING',301,0,0,301,7,'Demo order'),
('ORD-000163',16,2,NOW()-INTERVAL 163 DAY,'ONLINE','DELIVERY','Demo address 163','PENDING','CONFIRMED',314,0,5,319,8,'Demo order'),
('ORD-000164',21,3,NOW()-INTERVAL 164 DAY,'ONLINE','PICKUP','Demo address 164','PENDING','PROCESSING',327,0,0,327,7,'Demo order'),
('ORD-000165',26,1,NOW()-INTERVAL 165 DAY,'ONLINE','DELIVERY','Demo address 165','PAID','READY',340,0,5,345,8,'Demo order'),
('ORD-000166',31,2,NOW()-INTERVAL 166 DAY,'ONLINE','PICKUP','Demo address 166','PAID','DELIVERED',353,0,0,353,7,'Demo order'),
('ORD-000167',36,3,NOW()-INTERVAL 167 DAY,'ONLINE','DELIVERY','Demo address 167','PENDING','CANCELLED',56,0,5,61,8,'Demo order'),
('ORD-000168',41,1,NOW()-INTERVAL 168 DAY,'ONLINE','PICKUP','Demo address 168','PENDING','PENDING',69,0,0,69,7,'Demo order'),
('ORD-000169',46,2,NOW()-INTERVAL 169 DAY,'ONLINE','DELIVERY','Demo address 169','PENDING','CONFIRMED',82,0,5,87,8,'Demo order'),
('ORD-000170',51,3,NOW()-INTERVAL 170 DAY,'ONLINE','PICKUP','Demo address 170','PENDING','PROCESSING',95,0,0,95,7,'Demo order'),
('ORD-000171',56,1,NOW()-INTERVAL 171 DAY,'ONLINE','DELIVERY','Demo address 171','PAID','READY',108,0,5,113,8,'Demo order'),
('ORD-000172',61,2,NOW()-INTERVAL 172 DAY,'ONLINE','PICKUP','Demo address 172','PAID','DELIVERED',121,0,0,121,7,'Demo order'),
('ORD-000173',66,3,NOW()-INTERVAL 173 DAY,'ONLINE','DELIVERY','Demo address 173','PENDING','CANCELLED',134,0,5,139,8,'Demo order'),
('ORD-000174',71,1,NOW()-INTERVAL 174 DAY,'ONLINE','PICKUP','Demo address 174','PENDING','PENDING',147,0,0,147,7,'Demo order'),
('ORD-000175',76,2,NOW()-INTERVAL 175 DAY,'ONLINE','DELIVERY','Demo address 175','PENDING','CONFIRMED',160,0,5,165,8,'Demo order'),
('ORD-000176',81,3,NOW()-INTERVAL 176 DAY,'ONLINE','PICKUP','Demo address 176','PENDING','PROCESSING',173,0,0,173,7,'Demo order'),
('ORD-000177',86,1,NOW()-INTERVAL 177 DAY,'ONLINE','DELIVERY','Demo address 177','PAID','READY',186,0,5,191,8,'Demo order'),
('ORD-000178',91,2,NOW()-INTERVAL 178 DAY,'ONLINE','PICKUP','Demo address 178','PAID','DELIVERED',199,0,0,199,7,'Demo order'),
('ORD-000179',96,3,NOW()-INTERVAL 179 DAY,'ONLINE','DELIVERY','Demo address 179','PENDING','CANCELLED',212,0,5,217,8,'Demo order'),
('ORD-000180',101,1,NOW()-INTERVAL 0 DAY,'ONLINE','PICKUP','Demo address 180','PENDING','PENDING',225,0,0,225,7,'Demo order');
INSERT INTO order_items(OrderID,VariantID,Quantity,UnitPrice,LineTotal) SELECT o.OrderID,1+MOD(o.OrderID*7,180),1+MOD(o.OrderID,3),p.SellingPrice,p.SellingPrice*(1+MOD(o.OrderID,3)) FROM online_orders o JOIN product_variants pv ON pv.VariantID=1+MOD(o.OrderID*7,180) JOIN products p ON p.ProductID=pv.ProductID;
INSERT INTO order_status_history(OrderID,Status,Notes,ChangedBy,ChangedAt) SELECT OrderID,Status,'Seeded order status',1,OrderDate FROM online_orders;
INSERT INTO return_requests(RequestNumber,SaleID,CustomerID,BranchID,RequestType,RequestDate,Reason,ItemCondition,RefundMethod,RefundAmount,Status,RequestedBy,ApprovedBy) VALUES
('RET-000001',27,190,1,'RETURN',NOW()-INTERVAL 3 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',21,'APPROVED',NULL,1),
('RET-000002',44,109,3,'RETURN',NOW()-INTERVAL 6 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',22,'REJECTED',NULL,1),
('RET-000003',61,28,2,'EXCHANGE',NOW()-INTERVAL 9 DAY,'Size or preference change','UNUSED','STORE_CREDIT',23,'PROCESSED',NULL,1),
('RET-000004',78,147,1,'RETURN',NOW()-INTERVAL 12 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',24,'PENDING',NULL,1),
('RET-000005',95,66,3,'RETURN',NOW()-INTERVAL 15 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',25,'APPROVED',NULL,1),
('RET-000006',112,185,2,'EXCHANGE',NOW()-INTERVAL 18 DAY,'Size or preference change','UNUSED','STORE_CREDIT',26,'REJECTED',NULL,1),
('RET-000007',129,104,1,'RETURN',NOW()-INTERVAL 21 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',27,'PROCESSED',NULL,1),
('RET-000008',146,23,3,'RETURN',NOW()-INTERVAL 24 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',28,'PENDING',NULL,1),
('RET-000009',163,142,2,'EXCHANGE',NOW()-INTERVAL 27 DAY,'Size or preference change','UNUSED','STORE_CREDIT',29,'APPROVED',NULL,1),
('RET-000010',180,61,1,'RETURN',NOW()-INTERVAL 30 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',30,'REJECTED',NULL,1),
('RET-000011',197,180,3,'RETURN',NOW()-INTERVAL 33 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',31,'PROCESSED',NULL,1),
('RET-000012',214,99,2,'EXCHANGE',NOW()-INTERVAL 36 DAY,'Size or preference change','UNUSED','STORE_CREDIT',32,'PENDING',NULL,1),
('RET-000013',231,18,1,'RETURN',NOW()-INTERVAL 39 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',33,'APPROVED',NULL,1),
('RET-000014',248,137,3,'RETURN',NOW()-INTERVAL 42 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',34,'REJECTED',NULL,1),
('RET-000015',265,56,2,'EXCHANGE',NOW()-INTERVAL 45 DAY,'Size or preference change','UNUSED','STORE_CREDIT',35,'PROCESSED',NULL,1),
('RET-000016',282,175,1,'RETURN',NOW()-INTERVAL 48 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',36,'PENDING',NULL,1),
('RET-000017',299,94,3,'RETURN',NOW()-INTERVAL 51 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',37,'APPROVED',NULL,1),
('RET-000018',316,13,2,'EXCHANGE',NOW()-INTERVAL 54 DAY,'Size or preference change','UNUSED','STORE_CREDIT',38,'REJECTED',NULL,1),
('RET-000019',333,132,1,'RETURN',NOW()-INTERVAL 57 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',39,'PROCESSED',NULL,1),
('RET-000020',350,51,3,'RETURN',NOW()-INTERVAL 60 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',40,'PENDING',NULL,1),
('RET-000021',367,170,2,'EXCHANGE',NOW()-INTERVAL 63 DAY,'Size or preference change','UNUSED','STORE_CREDIT',41,'APPROVED',NULL,1),
('RET-000022',384,89,1,'RETURN',NOW()-INTERVAL 66 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',42,'REJECTED',NULL,1),
('RET-000023',401,8,3,'RETURN',NOW()-INTERVAL 69 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',43,'PROCESSED',NULL,1),
('RET-000024',418,127,2,'EXCHANGE',NOW()-INTERVAL 72 DAY,'Size or preference change','UNUSED','STORE_CREDIT',44,'PENDING',NULL,1),
('RET-000025',435,46,1,'RETURN',NOW()-INTERVAL 75 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',45,'APPROVED',NULL,1),
('RET-000026',452,165,3,'RETURN',NOW()-INTERVAL 78 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',46,'REJECTED',NULL,1),
('RET-000027',469,84,2,'EXCHANGE',NOW()-INTERVAL 81 DAY,'Size or preference change','UNUSED','STORE_CREDIT',47,'PROCESSED',NULL,1),
('RET-000028',486,3,1,'RETURN',NOW()-INTERVAL 84 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',48,'PENDING',NULL,1),
('RET-000029',503,122,3,'RETURN',NOW()-INTERVAL 87 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',49,'APPROVED',NULL,1),
('RET-000030',520,41,2,'EXCHANGE',NOW()-INTERVAL 90 DAY,'Size or preference change','UNUSED','STORE_CREDIT',50,'REJECTED',NULL,1),
('RET-000031',537,160,1,'RETURN',NOW()-INTERVAL 93 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',51,'PROCESSED',NULL,1),
('RET-000032',554,79,3,'RETURN',NOW()-INTERVAL 96 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',52,'PENDING',NULL,1),
('RET-000033',571,198,2,'EXCHANGE',NOW()-INTERVAL 99 DAY,'Size or preference change','UNUSED','STORE_CREDIT',53,'APPROVED',NULL,1),
('RET-000034',588,117,1,'RETURN',NOW()-INTERVAL 102 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',54,'REJECTED',NULL,1),
('RET-000035',605,36,3,'RETURN',NOW()-INTERVAL 105 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',55,'PROCESSED',NULL,1),
('RET-000036',622,155,2,'EXCHANGE',NOW()-INTERVAL 108 DAY,'Size or preference change','UNUSED','STORE_CREDIT',56,'PENDING',NULL,1),
('RET-000037',639,74,1,'RETURN',NOW()-INTERVAL 111 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',57,'APPROVED',NULL,1),
('RET-000038',656,193,3,'RETURN',NOW()-INTERVAL 114 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',58,'REJECTED',NULL,1),
('RET-000039',673,112,2,'EXCHANGE',NOW()-INTERVAL 117 DAY,'Size or preference change','UNUSED','STORE_CREDIT',59,'PROCESSED',NULL,1),
('RET-000040',690,31,1,'RETURN',NOW()-INTERVAL 120 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',60,'PENDING',NULL,1),
('RET-000041',707,150,3,'RETURN',NOW()-INTERVAL 123 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',61,'APPROVED',NULL,1),
('RET-000042',724,69,2,'EXCHANGE',NOW()-INTERVAL 126 DAY,'Size or preference change','UNUSED','STORE_CREDIT',62,'REJECTED',NULL,1),
('RET-000043',741,188,1,'RETURN',NOW()-INTERVAL 129 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',63,'PROCESSED',NULL,1),
('RET-000044',758,107,3,'RETURN',NOW()-INTERVAL 132 DAY,'Size or preference change','UNUSED','ORIGINAL_PAYMENT',64,'PENDING',NULL,1),
('RET-000045',775,26,2,'EXCHANGE',NOW()-INTERVAL 135 DAY,'Size or preference change','UNUSED','STORE_CREDIT',65,'APPROVED',NULL,1);
INSERT INTO return_items(ReturnID,SaleItemID,Quantity,RefundAmount,RestoreInventory) SELECT rr.ReturnID,(SELECT MIN(si.SaleItemID) FROM sale_items si WHERE si.SaleID=rr.SaleID),1,rr.RefundAmount,1 FROM return_requests rr;
INSERT INTO stock_requests(RequestNumber,BranchID,WarehouseID,RequestedBy,RequestDate,Priority,Status,ApprovedBy,ApprovedAt,Notes) VALUES
('SR-000001',2,2,3,NOW()-INTERVAL 1 DAY,'NORMAL','APPROVED',8,NOW(),'Demo stock request'),
('SR-000002',3,1,4,NOW()-INTERVAL 2 DAY,'HIGH','PARTIALLY_APPROVED',8,NOW(),'Demo stock request'),
('SR-000003',1,2,3,NOW()-INTERVAL 3 DAY,'URGENT','REJECTED',8,NOW(),'Demo stock request'),
('SR-000004',2,1,4,NOW()-INTERVAL 4 DAY,'LOW','FULFILLED',8,NOW(),'Demo stock request'),
('SR-000005',3,2,3,NOW()-INTERVAL 5 DAY,'NORMAL','PENDING',NULL,NULL,'Demo stock request'),
('SR-000006',1,1,4,NOW()-INTERVAL 6 DAY,'HIGH','APPROVED',8,NOW(),'Demo stock request'),
('SR-000007',2,2,3,NOW()-INTERVAL 7 DAY,'URGENT','PARTIALLY_APPROVED',8,NOW(),'Demo stock request'),
('SR-000008',3,1,4,NOW()-INTERVAL 8 DAY,'LOW','REJECTED',8,NOW(),'Demo stock request'),
('SR-000009',1,2,3,NOW()-INTERVAL 9 DAY,'NORMAL','FULFILLED',8,NOW(),'Demo stock request'),
('SR-000010',2,1,4,NOW()-INTERVAL 10 DAY,'HIGH','PENDING',NULL,NULL,'Demo stock request'),
('SR-000011',3,2,3,NOW()-INTERVAL 11 DAY,'URGENT','APPROVED',8,NOW(),'Demo stock request'),
('SR-000012',1,1,4,NOW()-INTERVAL 12 DAY,'LOW','PARTIALLY_APPROVED',8,NOW(),'Demo stock request'),
('SR-000013',2,2,3,NOW()-INTERVAL 13 DAY,'NORMAL','REJECTED',8,NOW(),'Demo stock request'),
('SR-000014',3,1,4,NOW()-INTERVAL 14 DAY,'HIGH','FULFILLED',8,NOW(),'Demo stock request'),
('SR-000015',1,2,3,NOW()-INTERVAL 15 DAY,'URGENT','PENDING',NULL,NULL,'Demo stock request'),
('SR-000016',2,1,4,NOW()-INTERVAL 16 DAY,'LOW','APPROVED',8,NOW(),'Demo stock request'),
('SR-000017',3,2,3,NOW()-INTERVAL 17 DAY,'NORMAL','PARTIALLY_APPROVED',8,NOW(),'Demo stock request'),
('SR-000018',1,1,4,NOW()-INTERVAL 18 DAY,'HIGH','REJECTED',8,NOW(),'Demo stock request'),
('SR-000019',2,2,3,NOW()-INTERVAL 19 DAY,'URGENT','FULFILLED',8,NOW(),'Demo stock request'),
('SR-000020',3,1,4,NOW()-INTERVAL 20 DAY,'LOW','PENDING',NULL,NULL,'Demo stock request'),
('SR-000021',1,2,3,NOW()-INTERVAL 21 DAY,'NORMAL','APPROVED',8,NOW(),'Demo stock request'),
('SR-000022',2,1,4,NOW()-INTERVAL 22 DAY,'HIGH','PARTIALLY_APPROVED',8,NOW(),'Demo stock request'),
('SR-000023',3,2,3,NOW()-INTERVAL 23 DAY,'URGENT','REJECTED',8,NOW(),'Demo stock request'),
('SR-000024',1,1,4,NOW()-INTERVAL 24 DAY,'LOW','FULFILLED',8,NOW(),'Demo stock request'),
('SR-000025',2,2,3,NOW()-INTERVAL 25 DAY,'NORMAL','PENDING',NULL,NULL,'Demo stock request'),
('SR-000026',3,1,4,NOW()-INTERVAL 26 DAY,'HIGH','APPROVED',8,NOW(),'Demo stock request'),
('SR-000027',1,2,3,NOW()-INTERVAL 27 DAY,'URGENT','PARTIALLY_APPROVED',8,NOW(),'Demo stock request'),
('SR-000028',2,1,4,NOW()-INTERVAL 28 DAY,'LOW','REJECTED',8,NOW(),'Demo stock request'),
('SR-000029',3,2,3,NOW()-INTERVAL 29 DAY,'NORMAL','FULFILLED',8,NOW(),'Demo stock request'),
('SR-000030',1,1,4,NOW()-INTERVAL 30 DAY,'HIGH','PENDING',NULL,NULL,'Demo stock request'),
('SR-000031',2,2,3,NOW()-INTERVAL 31 DAY,'URGENT','APPROVED',8,NOW(),'Demo stock request'),
('SR-000032',3,1,4,NOW()-INTERVAL 32 DAY,'LOW','PARTIALLY_APPROVED',8,NOW(),'Demo stock request'),
('SR-000033',1,2,3,NOW()-INTERVAL 33 DAY,'NORMAL','REJECTED',8,NOW(),'Demo stock request'),
('SR-000034',2,1,4,NOW()-INTERVAL 34 DAY,'HIGH','FULFILLED',8,NOW(),'Demo stock request'),
('SR-000035',3,2,3,NOW()-INTERVAL 35 DAY,'URGENT','PENDING',NULL,NULL,'Demo stock request'),
('SR-000036',1,1,4,NOW()-INTERVAL 36 DAY,'LOW','APPROVED',8,NOW(),'Demo stock request'),
('SR-000037',2,2,3,NOW()-INTERVAL 37 DAY,'NORMAL','PARTIALLY_APPROVED',8,NOW(),'Demo stock request'),
('SR-000038',3,1,4,NOW()-INTERVAL 38 DAY,'HIGH','REJECTED',8,NOW(),'Demo stock request'),
('SR-000039',1,2,3,NOW()-INTERVAL 39 DAY,'URGENT','FULFILLED',8,NOW(),'Demo stock request'),
('SR-000040',2,1,4,NOW()-INTERVAL 40 DAY,'LOW','PENDING',NULL,NULL,'Demo stock request'),
('SR-000041',3,2,3,NOW()-INTERVAL 41 DAY,'NORMAL','APPROVED',8,NOW(),'Demo stock request'),
('SR-000042',1,1,4,NOW()-INTERVAL 42 DAY,'HIGH','PARTIALLY_APPROVED',8,NOW(),'Demo stock request'),
('SR-000043',2,2,3,NOW()-INTERVAL 43 DAY,'URGENT','REJECTED',8,NOW(),'Demo stock request'),
('SR-000044',3,1,4,NOW()-INTERVAL 44 DAY,'LOW','FULFILLED',8,NOW(),'Demo stock request'),
('SR-000045',1,2,3,NOW()-INTERVAL 45 DAY,'NORMAL','PENDING',NULL,NULL,'Demo stock request'),
('SR-000046',2,1,4,NOW()-INTERVAL 46 DAY,'HIGH','APPROVED',8,NOW(),'Demo stock request'),
('SR-000047',3,2,3,NOW()-INTERVAL 47 DAY,'URGENT','PARTIALLY_APPROVED',8,NOW(),'Demo stock request'),
('SR-000048',1,1,4,NOW()-INTERVAL 48 DAY,'LOW','REJECTED',8,NOW(),'Demo stock request'),
('SR-000049',2,2,3,NOW()-INTERVAL 49 DAY,'NORMAL','FULFILLED',8,NOW(),'Demo stock request'),
('SR-000050',3,1,4,NOW()-INTERVAL 50 DAY,'HIGH','PENDING',NULL,NULL,'Demo stock request'),
('SR-000051',1,2,3,NOW()-INTERVAL 51 DAY,'URGENT','APPROVED',8,NOW(),'Demo stock request'),
('SR-000052',2,1,4,NOW()-INTERVAL 52 DAY,'LOW','PARTIALLY_APPROVED',8,NOW(),'Demo stock request'),
('SR-000053',3,2,3,NOW()-INTERVAL 53 DAY,'NORMAL','REJECTED',8,NOW(),'Demo stock request'),
('SR-000054',1,1,4,NOW()-INTERVAL 54 DAY,'HIGH','FULFILLED',8,NOW(),'Demo stock request'),
('SR-000055',2,2,3,NOW()-INTERVAL 55 DAY,'URGENT','PENDING',NULL,NULL,'Demo stock request'),
('SR-000056',3,1,4,NOW()-INTERVAL 56 DAY,'LOW','APPROVED',8,NOW(),'Demo stock request'),
('SR-000057',1,2,3,NOW()-INTERVAL 57 DAY,'NORMAL','PARTIALLY_APPROVED',8,NOW(),'Demo stock request'),
('SR-000058',2,1,4,NOW()-INTERVAL 58 DAY,'HIGH','REJECTED',8,NOW(),'Demo stock request'),
('SR-000059',3,2,3,NOW()-INTERVAL 59 DAY,'URGENT','FULFILLED',8,NOW(),'Demo stock request'),
('SR-000060',1,1,4,NOW()-INTERVAL 60 DAY,'LOW','PENDING',NULL,NULL,'Demo stock request'),
('SR-000061',2,2,3,NOW()-INTERVAL 61 DAY,'NORMAL','APPROVED',8,NOW(),'Demo stock request'),
('SR-000062',3,1,4,NOW()-INTERVAL 62 DAY,'HIGH','PARTIALLY_APPROVED',8,NOW(),'Demo stock request'),
('SR-000063',1,2,3,NOW()-INTERVAL 63 DAY,'URGENT','REJECTED',8,NOW(),'Demo stock request'),
('SR-000064',2,1,4,NOW()-INTERVAL 64 DAY,'LOW','FULFILLED',8,NOW(),'Demo stock request'),
('SR-000065',3,2,3,NOW()-INTERVAL 65 DAY,'NORMAL','PENDING',NULL,NULL,'Demo stock request'),
('SR-000066',1,1,4,NOW()-INTERVAL 66 DAY,'HIGH','APPROVED',8,NOW(),'Demo stock request'),
('SR-000067',2,2,3,NOW()-INTERVAL 67 DAY,'URGENT','PARTIALLY_APPROVED',8,NOW(),'Demo stock request'),
('SR-000068',3,1,4,NOW()-INTERVAL 68 DAY,'LOW','REJECTED',8,NOW(),'Demo stock request'),
('SR-000069',1,2,3,NOW()-INTERVAL 69 DAY,'NORMAL','FULFILLED',8,NOW(),'Demo stock request'),
('SR-000070',2,1,4,NOW()-INTERVAL 70 DAY,'HIGH','PENDING',NULL,NULL,'Demo stock request');
INSERT INTO stock_request_items(RequestID,VariantID,RequestedQuantity,ApprovedQuantity,FulfilledQuantity) SELECT RequestID,1+MOD(RequestID*9,180),5+MOD(RequestID,16),CASE WHEN Status IN('APPROVED','FULFILLED') THEN 5+MOD(RequestID,16) WHEN Status='PARTIALLY_APPROVED' THEN 3 ELSE 0 END,CASE WHEN Status='FULFILLED' THEN 5+MOD(RequestID,16) ELSE 0 END FROM stock_requests;
INSERT INTO purchase_orders(PONumber,SupplierID,WarehouseID,OrderDate,ExpectedDate,TotalCost,Status,Notes,CreatedBy,ApprovedBy) VALUES
('PO-000001',2,2,CURDATE()-INTERVAL 7 DAY,DATE_ADD(CURDATE()-INTERVAL 7 DAY, INTERVAL 14 DAY),875,'PENDING','Demo purchase order',1,NULL),
('PO-000002',3,1,CURDATE()-INTERVAL 14 DAY,DATE_ADD(CURDATE()-INTERVAL 14 DAY, INTERVAL 14 DAY),950,'APPROVED','Demo purchase order',1,1),
('PO-000003',4,2,CURDATE()-INTERVAL 21 DAY,DATE_ADD(CURDATE()-INTERVAL 21 DAY, INTERVAL 14 DAY),1025,'PARTIALLY_RECEIVED','Demo purchase order',1,1),
('PO-000004',5,1,CURDATE()-INTERVAL 28 DAY,DATE_ADD(CURDATE()-INTERVAL 28 DAY, INTERVAL 14 DAY),1100,'RECEIVED','Demo purchase order',1,1),
('PO-000005',6,2,CURDATE()-INTERVAL 35 DAY,DATE_ADD(CURDATE()-INTERVAL 35 DAY, INTERVAL 14 DAY),1175,'CANCELLED','Demo purchase order',1,1),
('PO-000006',1,1,CURDATE()-INTERVAL 42 DAY,DATE_ADD(CURDATE()-INTERVAL 42 DAY, INTERVAL 14 DAY),1250,'DRAFT','Demo purchase order',1,NULL),
('PO-000007',2,2,CURDATE()-INTERVAL 49 DAY,DATE_ADD(CURDATE()-INTERVAL 49 DAY, INTERVAL 14 DAY),1325,'PENDING','Demo purchase order',1,NULL),
('PO-000008',3,1,CURDATE()-INTERVAL 56 DAY,DATE_ADD(CURDATE()-INTERVAL 56 DAY, INTERVAL 14 DAY),1400,'APPROVED','Demo purchase order',1,1),
('PO-000009',4,2,CURDATE()-INTERVAL 63 DAY,DATE_ADD(CURDATE()-INTERVAL 63 DAY, INTERVAL 14 DAY),1475,'PARTIALLY_RECEIVED','Demo purchase order',1,1),
('PO-000010',5,1,CURDATE()-INTERVAL 70 DAY,DATE_ADD(CURDATE()-INTERVAL 70 DAY, INTERVAL 14 DAY),1550,'RECEIVED','Demo purchase order',1,1),
('PO-000011',6,2,CURDATE()-INTERVAL 77 DAY,DATE_ADD(CURDATE()-INTERVAL 77 DAY, INTERVAL 14 DAY),1625,'CANCELLED','Demo purchase order',1,1),
('PO-000012',1,1,CURDATE()-INTERVAL 84 DAY,DATE_ADD(CURDATE()-INTERVAL 84 DAY, INTERVAL 14 DAY),1700,'DRAFT','Demo purchase order',1,NULL),
('PO-000013',2,2,CURDATE()-INTERVAL 91 DAY,DATE_ADD(CURDATE()-INTERVAL 91 DAY, INTERVAL 14 DAY),1775,'PENDING','Demo purchase order',1,NULL),
('PO-000014',3,1,CURDATE()-INTERVAL 98 DAY,DATE_ADD(CURDATE()-INTERVAL 98 DAY, INTERVAL 14 DAY),1850,'APPROVED','Demo purchase order',1,1),
('PO-000015',4,2,CURDATE()-INTERVAL 105 DAY,DATE_ADD(CURDATE()-INTERVAL 105 DAY, INTERVAL 14 DAY),1925,'PARTIALLY_RECEIVED','Demo purchase order',1,1),
('PO-000016',5,1,CURDATE()-INTERVAL 112 DAY,DATE_ADD(CURDATE()-INTERVAL 112 DAY, INTERVAL 14 DAY),2000,'RECEIVED','Demo purchase order',1,1),
('PO-000017',6,2,CURDATE()-INTERVAL 119 DAY,DATE_ADD(CURDATE()-INTERVAL 119 DAY, INTERVAL 14 DAY),2075,'CANCELLED','Demo purchase order',1,1),
('PO-000018',1,1,CURDATE()-INTERVAL 126 DAY,DATE_ADD(CURDATE()-INTERVAL 126 DAY, INTERVAL 14 DAY),2150,'DRAFT','Demo purchase order',1,NULL),
('PO-000019',2,2,CURDATE()-INTERVAL 133 DAY,DATE_ADD(CURDATE()-INTERVAL 133 DAY, INTERVAL 14 DAY),2225,'PENDING','Demo purchase order',1,NULL),
('PO-000020',3,1,CURDATE()-INTERVAL 140 DAY,DATE_ADD(CURDATE()-INTERVAL 140 DAY, INTERVAL 14 DAY),2300,'APPROVED','Demo purchase order',1,1),
('PO-000021',4,2,CURDATE()-INTERVAL 147 DAY,DATE_ADD(CURDATE()-INTERVAL 147 DAY, INTERVAL 14 DAY),2375,'PARTIALLY_RECEIVED','Demo purchase order',1,1),
('PO-000022',5,1,CURDATE()-INTERVAL 154 DAY,DATE_ADD(CURDATE()-INTERVAL 154 DAY, INTERVAL 14 DAY),2450,'RECEIVED','Demo purchase order',1,1),
('PO-000023',6,2,CURDATE()-INTERVAL 161 DAY,DATE_ADD(CURDATE()-INTERVAL 161 DAY, INTERVAL 14 DAY),2525,'CANCELLED','Demo purchase order',1,1),
('PO-000024',1,1,CURDATE()-INTERVAL 168 DAY,DATE_ADD(CURDATE()-INTERVAL 168 DAY, INTERVAL 14 DAY),2600,'DRAFT','Demo purchase order',1,NULL),
('PO-000025',2,2,CURDATE()-INTERVAL 175 DAY,DATE_ADD(CURDATE()-INTERVAL 175 DAY, INTERVAL 14 DAY),2675,'PENDING','Demo purchase order',1,NULL),
('PO-000026',3,1,CURDATE()-INTERVAL 182 DAY,DATE_ADD(CURDATE()-INTERVAL 182 DAY, INTERVAL 14 DAY),2750,'APPROVED','Demo purchase order',1,1),
('PO-000027',4,2,CURDATE()-INTERVAL 189 DAY,DATE_ADD(CURDATE()-INTERVAL 189 DAY, INTERVAL 14 DAY),2825,'PARTIALLY_RECEIVED','Demo purchase order',1,1),
('PO-000028',5,1,CURDATE()-INTERVAL 196 DAY,DATE_ADD(CURDATE()-INTERVAL 196 DAY, INTERVAL 14 DAY),2900,'RECEIVED','Demo purchase order',1,1),
('PO-000029',6,2,CURDATE()-INTERVAL 203 DAY,DATE_ADD(CURDATE()-INTERVAL 203 DAY, INTERVAL 14 DAY),2975,'CANCELLED','Demo purchase order',1,1),
('PO-000030',1,1,CURDATE()-INTERVAL 210 DAY,DATE_ADD(CURDATE()-INTERVAL 210 DAY, INTERVAL 14 DAY),3050,'DRAFT','Demo purchase order',1,NULL),
('PO-000031',2,2,CURDATE()-INTERVAL 217 DAY,DATE_ADD(CURDATE()-INTERVAL 217 DAY, INTERVAL 14 DAY),3125,'PENDING','Demo purchase order',1,NULL),
('PO-000032',3,1,CURDATE()-INTERVAL 224 DAY,DATE_ADD(CURDATE()-INTERVAL 224 DAY, INTERVAL 14 DAY),3200,'APPROVED','Demo purchase order',1,1),
('PO-000033',4,2,CURDATE()-INTERVAL 231 DAY,DATE_ADD(CURDATE()-INTERVAL 231 DAY, INTERVAL 14 DAY),3275,'PARTIALLY_RECEIVED','Demo purchase order',1,1),
('PO-000034',5,1,CURDATE()-INTERVAL 238 DAY,DATE_ADD(CURDATE()-INTERVAL 238 DAY, INTERVAL 14 DAY),3350,'RECEIVED','Demo purchase order',1,1),
('PO-000035',6,2,CURDATE()-INTERVAL 245 DAY,DATE_ADD(CURDATE()-INTERVAL 245 DAY, INTERVAL 14 DAY),3425,'CANCELLED','Demo purchase order',1,1),
('PO-000036',1,1,CURDATE()-INTERVAL 252 DAY,DATE_ADD(CURDATE()-INTERVAL 252 DAY, INTERVAL 14 DAY),3500,'DRAFT','Demo purchase order',1,NULL);
INSERT INTO purchase_order_items(PurchaseOrderID,VariantID,OrderedQuantity,ReceivedQuantity,UnitCost,LineTotal) SELECT po.PurchaseOrderID,1+MOD(po.PurchaseOrderID*13,180),20+MOD(po.PurchaseOrderID,30),CASE WHEN po.Status='RECEIVED' THEN 20+MOD(po.PurchaseOrderID,30) WHEN po.Status='PARTIALLY_RECEIVED' THEN 10 ELSE 0 END,p.CostPrice,p.CostPrice*(20+MOD(po.PurchaseOrderID,30)) FROM purchase_orders po JOIN product_variants pv ON pv.VariantID=1+MOD(po.PurchaseOrderID*13,180) JOIN products p ON p.ProductID=pv.ProductID;
INSERT INTO favorites(CustomerID,ProductID) SELECT c.CustomerID,1+MOD(c.CustomerID*7,60) FROM customers c WHERE MOD(c.CustomerID,3)=0;
INSERT INTO notifications(UserID,Title,Message,NotificationType,Priority,RelatedEntityType,RelatedEntityID,IsRead,CreatedAt) VALUES
(2,'Demo notification 1','Pending Order requires attention','PENDING_ORDER','NORMAL','SYSTEM',1,0,NOW()-INTERVAL 5 HOUR),
(3,'Demo notification 2','Pending Return requires attention','PENDING_RETURN','HIGH','SYSTEM',2,0,NOW()-INTERVAL 10 HOUR),
(4,'Demo notification 3','Account Security requires attention','ACCOUNT_SECURITY','URGENT','SYSTEM',3,1,NOW()-INTERVAL 15 HOUR),
(NULL,'Demo notification 4','Daily Closing requires attention','DAILY_CLOSING','LOW','SYSTEM',4,0,NOW()-INTERVAL 20 HOUR),
(6,'Demo notification 5','Low Stock requires attention','LOW_STOCK','NORMAL','SYSTEM',5,0,NOW()-INTERVAL 25 HOUR),
(7,'Demo notification 6','Pending Order requires attention','PENDING_ORDER','HIGH','SYSTEM',6,1,NOW()-INTERVAL 30 HOUR),
(8,'Demo notification 7','Pending Return requires attention','PENDING_RETURN','URGENT','SYSTEM',7,0,NOW()-INTERVAL 35 HOUR),
(NULL,'Demo notification 8','Account Security requires attention','ACCOUNT_SECURITY','LOW','SYSTEM',8,0,NOW()-INTERVAL 40 HOUR),
(1,'Demo notification 9','Daily Closing requires attention','DAILY_CLOSING','NORMAL','SYSTEM',9,1,NOW()-INTERVAL 45 HOUR),
(2,'Demo notification 10','Low Stock requires attention','LOW_STOCK','HIGH','SYSTEM',10,0,NOW()-INTERVAL 50 HOUR),
(3,'Demo notification 11','Pending Order requires attention','PENDING_ORDER','URGENT','SYSTEM',11,0,NOW()-INTERVAL 55 HOUR),
(NULL,'Demo notification 12','Pending Return requires attention','PENDING_RETURN','LOW','SYSTEM',12,1,NOW()-INTERVAL 60 HOUR),
(5,'Demo notification 13','Account Security requires attention','ACCOUNT_SECURITY','NORMAL','SYSTEM',13,0,NOW()-INTERVAL 65 HOUR),
(6,'Demo notification 14','Daily Closing requires attention','DAILY_CLOSING','HIGH','SYSTEM',14,0,NOW()-INTERVAL 70 HOUR),
(7,'Demo notification 15','Low Stock requires attention','LOW_STOCK','URGENT','SYSTEM',15,1,NOW()-INTERVAL 75 HOUR),
(NULL,'Demo notification 16','Pending Order requires attention','PENDING_ORDER','LOW','SYSTEM',16,0,NOW()-INTERVAL 80 HOUR),
(9,'Demo notification 17','Pending Return requires attention','PENDING_RETURN','NORMAL','SYSTEM',17,0,NOW()-INTERVAL 85 HOUR),
(1,'Demo notification 18','Account Security requires attention','ACCOUNT_SECURITY','HIGH','SYSTEM',18,1,NOW()-INTERVAL 90 HOUR),
(2,'Demo notification 19','Daily Closing requires attention','DAILY_CLOSING','URGENT','SYSTEM',19,0,NOW()-INTERVAL 95 HOUR),
(NULL,'Demo notification 20','Low Stock requires attention','LOW_STOCK','LOW','SYSTEM',20,0,NOW()-INTERVAL 100 HOUR),
(4,'Demo notification 21','Pending Order requires attention','PENDING_ORDER','NORMAL','SYSTEM',21,1,NOW()-INTERVAL 105 HOUR),
(5,'Demo notification 22','Pending Return requires attention','PENDING_RETURN','HIGH','SYSTEM',22,0,NOW()-INTERVAL 110 HOUR),
(6,'Demo notification 23','Account Security requires attention','ACCOUNT_SECURITY','URGENT','SYSTEM',23,0,NOW()-INTERVAL 115 HOUR),
(NULL,'Demo notification 24','Daily Closing requires attention','DAILY_CLOSING','LOW','SYSTEM',24,1,NOW()-INTERVAL 120 HOUR),
(8,'Demo notification 25','Low Stock requires attention','LOW_STOCK','NORMAL','SYSTEM',25,0,NOW()-INTERVAL 125 HOUR),
(9,'Demo notification 26','Pending Order requires attention','PENDING_ORDER','HIGH','SYSTEM',26,0,NOW()-INTERVAL 130 HOUR),
(1,'Demo notification 27','Pending Return requires attention','PENDING_RETURN','URGENT','SYSTEM',27,1,NOW()-INTERVAL 135 HOUR),
(NULL,'Demo notification 28','Account Security requires attention','ACCOUNT_SECURITY','LOW','SYSTEM',28,0,NOW()-INTERVAL 140 HOUR),
(3,'Demo notification 29','Daily Closing requires attention','DAILY_CLOSING','NORMAL','SYSTEM',29,0,NOW()-INTERVAL 145 HOUR),
(4,'Demo notification 30','Low Stock requires attention','LOW_STOCK','HIGH','SYSTEM',30,1,NOW()-INTERVAL 150 HOUR),
(5,'Demo notification 31','Pending Order requires attention','PENDING_ORDER','URGENT','SYSTEM',31,0,NOW()-INTERVAL 155 HOUR),
(NULL,'Demo notification 32','Pending Return requires attention','PENDING_RETURN','LOW','SYSTEM',32,0,NOW()-INTERVAL 160 HOUR),
(7,'Demo notification 33','Account Security requires attention','ACCOUNT_SECURITY','NORMAL','SYSTEM',33,1,NOW()-INTERVAL 165 HOUR),
(8,'Demo notification 34','Daily Closing requires attention','DAILY_CLOSING','HIGH','SYSTEM',34,0,NOW()-INTERVAL 170 HOUR),
(9,'Demo notification 35','Low Stock requires attention','LOW_STOCK','URGENT','SYSTEM',35,0,NOW()-INTERVAL 175 HOUR),
(NULL,'Demo notification 36','Pending Order requires attention','PENDING_ORDER','LOW','SYSTEM',36,1,NOW()-INTERVAL 180 HOUR),
(2,'Demo notification 37','Pending Return requires attention','PENDING_RETURN','NORMAL','SYSTEM',37,0,NOW()-INTERVAL 185 HOUR),
(3,'Demo notification 38','Account Security requires attention','ACCOUNT_SECURITY','HIGH','SYSTEM',38,0,NOW()-INTERVAL 190 HOUR),
(4,'Demo notification 39','Daily Closing requires attention','DAILY_CLOSING','URGENT','SYSTEM',39,1,NOW()-INTERVAL 195 HOUR),
(NULL,'Demo notification 40','Low Stock requires attention','LOW_STOCK','LOW','SYSTEM',40,0,NOW()-INTERVAL 200 HOUR),
(6,'Demo notification 41','Pending Order requires attention','PENDING_ORDER','NORMAL','SYSTEM',41,0,NOW()-INTERVAL 205 HOUR),
(7,'Demo notification 42','Pending Return requires attention','PENDING_RETURN','HIGH','SYSTEM',42,1,NOW()-INTERVAL 210 HOUR),
(8,'Demo notification 43','Account Security requires attention','ACCOUNT_SECURITY','URGENT','SYSTEM',43,0,NOW()-INTERVAL 215 HOUR),
(NULL,'Demo notification 44','Daily Closing requires attention','DAILY_CLOSING','LOW','SYSTEM',44,0,NOW()-INTERVAL 220 HOUR),
(1,'Demo notification 45','Low Stock requires attention','LOW_STOCK','NORMAL','SYSTEM',45,1,NOW()-INTERVAL 225 HOUR),
(2,'Demo notification 46','Pending Order requires attention','PENDING_ORDER','HIGH','SYSTEM',46,0,NOW()-INTERVAL 230 HOUR),
(3,'Demo notification 47','Pending Return requires attention','PENDING_RETURN','URGENT','SYSTEM',47,0,NOW()-INTERVAL 235 HOUR),
(NULL,'Demo notification 48','Account Security requires attention','ACCOUNT_SECURITY','LOW','SYSTEM',48,1,NOW()-INTERVAL 240 HOUR),
(5,'Demo notification 49','Daily Closing requires attention','DAILY_CLOSING','NORMAL','SYSTEM',49,0,NOW()-INTERVAL 245 HOUR),
(6,'Demo notification 50','Low Stock requires attention','LOW_STOCK','HIGH','SYSTEM',50,0,NOW()-INTERVAL 250 HOUR),
(7,'Demo notification 51','Pending Order requires attention','PENDING_ORDER','URGENT','SYSTEM',51,1,NOW()-INTERVAL 255 HOUR),
(NULL,'Demo notification 52','Pending Return requires attention','PENDING_RETURN','LOW','SYSTEM',52,0,NOW()-INTERVAL 260 HOUR),
(9,'Demo notification 53','Account Security requires attention','ACCOUNT_SECURITY','NORMAL','SYSTEM',53,0,NOW()-INTERVAL 265 HOUR),
(1,'Demo notification 54','Daily Closing requires attention','DAILY_CLOSING','HIGH','SYSTEM',54,1,NOW()-INTERVAL 270 HOUR),
(2,'Demo notification 55','Low Stock requires attention','LOW_STOCK','URGENT','SYSTEM',55,0,NOW()-INTERVAL 275 HOUR),
(NULL,'Demo notification 56','Pending Order requires attention','PENDING_ORDER','LOW','SYSTEM',56,0,NOW()-INTERVAL 280 HOUR),
(4,'Demo notification 57','Pending Return requires attention','PENDING_RETURN','NORMAL','SYSTEM',57,1,NOW()-INTERVAL 285 HOUR),
(5,'Demo notification 58','Account Security requires attention','ACCOUNT_SECURITY','HIGH','SYSTEM',58,0,NOW()-INTERVAL 290 HOUR),
(6,'Demo notification 59','Daily Closing requires attention','DAILY_CLOSING','URGENT','SYSTEM',59,0,NOW()-INTERVAL 295 HOUR),
(NULL,'Demo notification 60','Low Stock requires attention','LOW_STOCK','LOW','SYSTEM',60,1,NOW()-INTERVAL 300 HOUR);
INSERT INTO system_settings(SettingKey,SettingValue,Description,UpdatedBy) VALUES
('boutique_name','Lucerne Boutique','Application setting',1),
('currency','USD','Application setting',1),
('tax_percentage','0','Application setting',1),
('default_low_stock_level','5','Application setting',1),
('receipt_footer','Thank you for shopping with Lucerne Boutique','Application setting',1),
('company_phone','+97022900001','Application setting',1),
('company_email','info@lucerne-boutique.local','Application setting',1),
('company_address','Ramallah, Palestine','Application setting',1),
('return_period_days','14','Application setting',1),
('invoice_prefix','LC','Application setting',1),
('order_prefix','ORD','Application setting',1),
('stock_request_prefix','SR','Application setting',1),
('maximum_login_attempts','5','Application setting',1),
('login_lock_minutes','15','Application setting',1),
('password_minimum_length','8','Application setting',1),
('default_chart_period','MONTH','Application setting',1),
('date_format','dd MMM yyyy','Application setting',1),
('time_format','HH:mm','Application setting',1);
INSERT INTO daily_closings(ClosingDate,BranchID,CashierUserID,ShiftStart,ShiftEnd,OpeningCash,ExpectedCash,ActualCash,DifferenceAmount,CashSales,CardSales,Refunds,Status,Notes,ApprovedBy) VALUES
(CURDATE()-INTERVAL 1 DAY,2,6,DATE_SUB(NOW(),INTERVAL 1 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 1 DAY),100,437,437,0,240.35,196.65,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 2 DAY,3,7,DATE_SUB(NOW(),INTERVAL 2 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 2 DAY),100,474,479,5,260.70,213.30,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 3 DAY,1,5,DATE_SUB(NOW(),INTERVAL 3 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 3 DAY),100,511,504,-7,281.05,229.95,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 4 DAY,2,6,DATE_SUB(NOW(),INTERVAL 4 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 4 DAY),100,548,551,3,301.40,246.60,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 5 DAY,3,7,DATE_SUB(NOW(),INTERVAL 5 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 5 DAY),100,585,585,0,321.75,263.25,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 6 DAY,1,5,DATE_SUB(NOW(),INTERVAL 6 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 6 DAY),100,622,622,0,342.10,279.90,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 7 DAY,2,6,DATE_SUB(NOW(),INTERVAL 7 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 7 DAY),100,659,664,5,362.45,296.55,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 8 DAY,3,7,DATE_SUB(NOW(),INTERVAL 8 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 8 DAY),100,696,689,-7,382.80,313.20,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 9 DAY,1,5,DATE_SUB(NOW(),INTERVAL 9 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 9 DAY),100,733,736,3,403.15,329.85,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 10 DAY,2,6,DATE_SUB(NOW(),INTERVAL 10 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 10 DAY),100,770,770,0,423.50,346.50,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 11 DAY,3,7,DATE_SUB(NOW(),INTERVAL 11 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 11 DAY),100,807,807,0,443.85,363.15,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 12 DAY,1,5,DATE_SUB(NOW(),INTERVAL 12 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 12 DAY),100,844,849,5,464.20,379.80,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 13 DAY,2,6,DATE_SUB(NOW(),INTERVAL 13 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 13 DAY),100,881,874,-7,484.55,396.45,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 14 DAY,3,7,DATE_SUB(NOW(),INTERVAL 14 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 14 DAY),100,918,921,3,504.90,413.10,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 15 DAY,1,5,DATE_SUB(NOW(),INTERVAL 15 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 15 DAY),100,955,955,0,525.25,429.75,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 16 DAY,2,6,DATE_SUB(NOW(),INTERVAL 16 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 16 DAY),100,992,992,0,545.60,446.40,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 17 DAY,3,7,DATE_SUB(NOW(),INTERVAL 17 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 17 DAY),100,1029,1034,5,565.95,463.05,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 18 DAY,1,5,DATE_SUB(NOW(),INTERVAL 18 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 18 DAY),100,1066,1059,-7,586.30,479.70,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 19 DAY,2,6,DATE_SUB(NOW(),INTERVAL 19 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 19 DAY),100,1103,1106,3,606.65,496.35,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 20 DAY,3,7,DATE_SUB(NOW(),INTERVAL 20 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 20 DAY),100,1140,1140,0,627.00,513.00,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 21 DAY,1,5,DATE_SUB(NOW(),INTERVAL 21 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 21 DAY),100,1177,1177,0,647.35,529.65,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 22 DAY,2,6,DATE_SUB(NOW(),INTERVAL 22 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 22 DAY),100,1214,1219,5,667.70,546.30,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 23 DAY,3,7,DATE_SUB(NOW(),INTERVAL 23 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 23 DAY),100,1251,1244,-7,688.05,562.95,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 24 DAY,1,5,DATE_SUB(NOW(),INTERVAL 24 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 24 DAY),100,1288,1291,3,708.40,579.60,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 25 DAY,2,6,DATE_SUB(NOW(),INTERVAL 25 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 25 DAY),100,425,425,0,233.75,191.25,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 26 DAY,3,7,DATE_SUB(NOW(),INTERVAL 26 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 26 DAY),100,462,462,0,254.10,207.90,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 27 DAY,1,5,DATE_SUB(NOW(),INTERVAL 27 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 27 DAY),100,499,504,5,274.45,224.55,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 28 DAY,2,6,DATE_SUB(NOW(),INTERVAL 28 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 28 DAY),100,536,529,-7,294.80,241.20,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 29 DAY,3,7,DATE_SUB(NOW(),INTERVAL 29 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 29 DAY),100,573,576,3,315.15,257.85,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 30 DAY,1,5,DATE_SUB(NOW(),INTERVAL 30 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 30 DAY),100,610,610,0,335.50,274.50,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 31 DAY,2,6,DATE_SUB(NOW(),INTERVAL 31 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 31 DAY),100,647,647,0,355.85,291.15,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 32 DAY,3,7,DATE_SUB(NOW(),INTERVAL 32 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 32 DAY),100,684,689,5,376.20,307.80,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 33 DAY,1,5,DATE_SUB(NOW(),INTERVAL 33 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 33 DAY),100,721,714,-7,396.55,324.45,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 34 DAY,2,6,DATE_SUB(NOW(),INTERVAL 34 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 34 DAY),100,758,761,3,416.90,341.10,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 35 DAY,3,7,DATE_SUB(NOW(),INTERVAL 35 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 35 DAY),100,795,795,0,437.25,357.75,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 36 DAY,1,5,DATE_SUB(NOW(),INTERVAL 36 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 36 DAY),100,832,832,0,457.60,374.40,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 37 DAY,2,6,DATE_SUB(NOW(),INTERVAL 37 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 37 DAY),100,869,874,5,477.95,391.05,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 38 DAY,3,7,DATE_SUB(NOW(),INTERVAL 38 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 38 DAY),100,906,899,-7,498.30,407.70,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 39 DAY,1,5,DATE_SUB(NOW(),INTERVAL 39 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 39 DAY),100,943,946,3,518.65,424.35,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 40 DAY,2,6,DATE_SUB(NOW(),INTERVAL 40 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 40 DAY),100,980,980,0,539.00,441.00,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 41 DAY,3,7,DATE_SUB(NOW(),INTERVAL 41 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 41 DAY),100,1017,1017,0,559.35,457.65,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 42 DAY,1,5,DATE_SUB(NOW(),INTERVAL 42 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 42 DAY),100,1054,1059,5,579.70,474.30,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 43 DAY,2,6,DATE_SUB(NOW(),INTERVAL 43 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 43 DAY),100,1091,1084,-7,600.05,490.95,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 44 DAY,3,7,DATE_SUB(NOW(),INTERVAL 44 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 44 DAY),100,1128,1131,3,620.40,507.60,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 45 DAY,1,5,DATE_SUB(NOW(),INTERVAL 45 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 45 DAY),100,1165,1165,0,640.75,524.25,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 46 DAY,2,6,DATE_SUB(NOW(),INTERVAL 46 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 46 DAY),100,1202,1202,0,661.10,540.90,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 47 DAY,3,7,DATE_SUB(NOW(),INTERVAL 47 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 47 DAY),100,1239,1244,5,681.45,557.55,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 48 DAY,1,5,DATE_SUB(NOW(),INTERVAL 48 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 48 DAY),100,1276,1269,-7,701.80,574.20,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 49 DAY,2,6,DATE_SUB(NOW(),INTERVAL 49 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 49 DAY),100,413,416,3,227.15,185.85,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 50 DAY,3,7,DATE_SUB(NOW(),INTERVAL 50 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 50 DAY),100,450,450,0,247.50,202.50,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 51 DAY,1,5,DATE_SUB(NOW(),INTERVAL 51 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 51 DAY),100,487,487,0,267.85,219.15,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 52 DAY,2,6,DATE_SUB(NOW(),INTERVAL 52 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 52 DAY),100,524,529,5,288.20,235.80,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 53 DAY,3,7,DATE_SUB(NOW(),INTERVAL 53 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 53 DAY),100,561,554,-7,308.55,252.45,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 54 DAY,1,5,DATE_SUB(NOW(),INTERVAL 54 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 54 DAY),100,598,601,3,328.90,269.10,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 55 DAY,2,6,DATE_SUB(NOW(),INTERVAL 55 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 55 DAY),100,635,635,0,349.25,285.75,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 56 DAY,3,7,DATE_SUB(NOW(),INTERVAL 56 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 56 DAY),100,672,672,0,369.60,302.40,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 57 DAY,1,5,DATE_SUB(NOW(),INTERVAL 57 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 57 DAY),100,709,714,5,389.95,319.05,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 58 DAY,2,6,DATE_SUB(NOW(),INTERVAL 58 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 58 DAY),100,746,739,-7,410.30,335.70,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 59 DAY,3,7,DATE_SUB(NOW(),INTERVAL 59 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 59 DAY),100,783,786,3,430.65,352.35,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 60 DAY,1,5,DATE_SUB(NOW(),INTERVAL 60 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 60 DAY),100,820,820,0,451.00,369.00,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 61 DAY,2,6,DATE_SUB(NOW(),INTERVAL 61 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 61 DAY),100,857,857,0,471.35,385.65,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 62 DAY,3,7,DATE_SUB(NOW(),INTERVAL 62 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 62 DAY),100,894,899,5,491.70,402.30,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 63 DAY,1,5,DATE_SUB(NOW(),INTERVAL 63 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 63 DAY),100,931,924,-7,512.05,418.95,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 64 DAY,2,6,DATE_SUB(NOW(),INTERVAL 64 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 64 DAY),100,968,971,3,532.40,435.60,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 65 DAY,3,7,DATE_SUB(NOW(),INTERVAL 65 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 65 DAY),100,1005,1005,0,552.75,452.25,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 66 DAY,1,5,DATE_SUB(NOW(),INTERVAL 66 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 66 DAY),100,1042,1042,0,573.10,468.90,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 67 DAY,2,6,DATE_SUB(NOW(),INTERVAL 67 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 67 DAY),100,1079,1084,5,593.45,485.55,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 68 DAY,3,7,DATE_SUB(NOW(),INTERVAL 68 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 68 DAY),100,1116,1109,-7,613.80,502.20,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 69 DAY,1,5,DATE_SUB(NOW(),INTERVAL 69 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 69 DAY),100,1153,1156,3,634.15,518.85,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 70 DAY,2,6,DATE_SUB(NOW(),INTERVAL 70 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 70 DAY),100,1190,1190,0,654.50,535.50,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 71 DAY,3,7,DATE_SUB(NOW(),INTERVAL 71 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 71 DAY),100,1227,1227,0,674.85,552.15,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 72 DAY,1,5,DATE_SUB(NOW(),INTERVAL 72 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 72 DAY),100,1264,1269,5,695.20,568.80,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 73 DAY,2,6,DATE_SUB(NOW(),INTERVAL 73 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 73 DAY),100,401,394,-7,220.55,180.45,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 74 DAY,3,7,DATE_SUB(NOW(),INTERVAL 74 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 74 DAY),100,438,441,3,240.90,197.10,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 75 DAY,1,5,DATE_SUB(NOW(),INTERVAL 75 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 75 DAY),100,475,475,0,261.25,213.75,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 76 DAY,2,6,DATE_SUB(NOW(),INTERVAL 76 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 76 DAY),100,512,512,0,281.60,230.40,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 77 DAY,3,7,DATE_SUB(NOW(),INTERVAL 77 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 77 DAY),100,549,554,5,301.95,247.05,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 78 DAY,1,5,DATE_SUB(NOW(),INTERVAL 78 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 78 DAY),100,586,579,-7,322.30,263.70,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 79 DAY,2,6,DATE_SUB(NOW(),INTERVAL 79 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 79 DAY),100,623,626,3,342.65,280.35,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 80 DAY,3,7,DATE_SUB(NOW(),INTERVAL 80 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 80 DAY),100,660,660,0,363.00,297.00,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 81 DAY,1,5,DATE_SUB(NOW(),INTERVAL 81 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 81 DAY),100,697,697,0,383.35,313.65,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 82 DAY,2,6,DATE_SUB(NOW(),INTERVAL 82 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 82 DAY),100,734,739,5,403.70,330.30,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 83 DAY,3,7,DATE_SUB(NOW(),INTERVAL 83 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 83 DAY),100,771,764,-7,424.05,346.95,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 84 DAY,1,5,DATE_SUB(NOW(),INTERVAL 84 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 84 DAY),100,808,811,3,444.40,363.60,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 85 DAY,2,6,DATE_SUB(NOW(),INTERVAL 85 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 85 DAY),100,845,845,0,464.75,380.25,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 86 DAY,3,7,DATE_SUB(NOW(),INTERVAL 86 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 86 DAY),100,882,882,0,485.10,396.90,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 87 DAY,1,5,DATE_SUB(NOW(),INTERVAL 87 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 87 DAY),100,919,924,5,505.45,413.55,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 88 DAY,2,6,DATE_SUB(NOW(),INTERVAL 88 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 88 DAY),100,956,949,-7,525.80,430.20,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 89 DAY,3,7,DATE_SUB(NOW(),INTERVAL 89 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 89 DAY),100,993,996,3,546.15,446.85,0,'APPROVED','Demo closing',3),
(CURDATE()-INTERVAL 90 DAY,1,5,DATE_SUB(NOW(),INTERVAL 90 DAY)-INTERVAL 8 HOUR,DATE_SUB(NOW(),INTERVAL 90 DAY),100,1030,1030,0,566.50,463.50,0,'APPROVED','Demo closing',3);
INSERT INTO stock_movements(LocationType,LocationID,VariantID,MovementType,Direction,Quantity,ReferenceType,ReferenceID,ReferenceNumber,MovementDate,PerformedBy,Notes) VALUES
('BRANCH',2,2,'RECEIPT','IN',2,'DEMO',1,'MOV-000001',NOW()-INTERVAL 1 DAY,3,'Seed movement'),
('BRANCH',3,3,'TRANSFER','OUT',3,'DEMO',2,'MOV-000002',NOW()-INTERVAL 2 DAY,3,'Seed movement'),
('WAREHOUSE',2,4,'RECEIPT','IN',4,'DEMO',3,'MOV-000003',NOW()-INTERVAL 3 DAY,8,'Seed movement'),
('BRANCH',2,5,'TRANSFER','OUT',5,'DEMO',4,'MOV-000004',NOW()-INTERVAL 4 DAY,3,'Seed movement'),
('BRANCH',3,6,'RECEIPT','IN',6,'DEMO',5,'MOV-000005',NOW()-INTERVAL 5 DAY,3,'Seed movement'),
('WAREHOUSE',1,7,'TRANSFER','OUT',7,'DEMO',6,'MOV-000006',NOW()-INTERVAL 6 DAY,8,'Seed movement'),
('BRANCH',2,8,'RECEIPT','IN',8,'DEMO',7,'MOV-000007',NOW()-INTERVAL 7 DAY,3,'Seed movement'),
('BRANCH',3,9,'TRANSFER','OUT',9,'DEMO',8,'MOV-000008',NOW()-INTERVAL 8 DAY,3,'Seed movement'),
('WAREHOUSE',2,10,'RECEIPT','IN',10,'DEMO',9,'MOV-000009',NOW()-INTERVAL 9 DAY,8,'Seed movement'),
('BRANCH',2,11,'TRANSFER','OUT',11,'DEMO',10,'MOV-000010',NOW()-INTERVAL 10 DAY,3,'Seed movement'),
('BRANCH',3,12,'RECEIPT','IN',12,'DEMO',11,'MOV-000011',NOW()-INTERVAL 11 DAY,3,'Seed movement'),
('WAREHOUSE',1,13,'TRANSFER','OUT',13,'DEMO',12,'MOV-000012',NOW()-INTERVAL 12 DAY,8,'Seed movement'),
('BRANCH',2,14,'RECEIPT','IN',14,'DEMO',13,'MOV-000013',NOW()-INTERVAL 13 DAY,3,'Seed movement'),
('BRANCH',3,15,'TRANSFER','OUT',15,'DEMO',14,'MOV-000014',NOW()-INTERVAL 14 DAY,3,'Seed movement'),
('WAREHOUSE',2,16,'RECEIPT','IN',1,'DEMO',15,'MOV-000015',NOW()-INTERVAL 15 DAY,8,'Seed movement'),
('BRANCH',2,17,'TRANSFER','OUT',2,'DEMO',16,'MOV-000016',NOW()-INTERVAL 16 DAY,3,'Seed movement'),
('BRANCH',3,18,'RECEIPT','IN',3,'DEMO',17,'MOV-000017',NOW()-INTERVAL 17 DAY,3,'Seed movement'),
('WAREHOUSE',1,19,'TRANSFER','OUT',4,'DEMO',18,'MOV-000018',NOW()-INTERVAL 18 DAY,8,'Seed movement'),
('BRANCH',2,20,'RECEIPT','IN',5,'DEMO',19,'MOV-000019',NOW()-INTERVAL 19 DAY,3,'Seed movement'),
('BRANCH',3,21,'TRANSFER','OUT',6,'DEMO',20,'MOV-000020',NOW()-INTERVAL 20 DAY,3,'Seed movement'),
('WAREHOUSE',2,22,'RECEIPT','IN',7,'DEMO',21,'MOV-000021',NOW()-INTERVAL 21 DAY,8,'Seed movement'),
('BRANCH',2,23,'TRANSFER','OUT',8,'DEMO',22,'MOV-000022',NOW()-INTERVAL 22 DAY,3,'Seed movement'),
('BRANCH',3,24,'RECEIPT','IN',9,'DEMO',23,'MOV-000023',NOW()-INTERVAL 23 DAY,3,'Seed movement'),
('WAREHOUSE',1,25,'TRANSFER','OUT',10,'DEMO',24,'MOV-000024',NOW()-INTERVAL 24 DAY,8,'Seed movement'),
('BRANCH',2,26,'RECEIPT','IN',11,'DEMO',25,'MOV-000025',NOW()-INTERVAL 25 DAY,3,'Seed movement'),
('BRANCH',3,27,'TRANSFER','OUT',12,'DEMO',26,'MOV-000026',NOW()-INTERVAL 26 DAY,3,'Seed movement'),
('WAREHOUSE',2,28,'RECEIPT','IN',13,'DEMO',27,'MOV-000027',NOW()-INTERVAL 27 DAY,8,'Seed movement'),
('BRANCH',2,29,'TRANSFER','OUT',14,'DEMO',28,'MOV-000028',NOW()-INTERVAL 28 DAY,3,'Seed movement'),
('BRANCH',3,30,'RECEIPT','IN',15,'DEMO',29,'MOV-000029',NOW()-INTERVAL 29 DAY,3,'Seed movement'),
('WAREHOUSE',1,31,'TRANSFER','OUT',1,'DEMO',30,'MOV-000030',NOW()-INTERVAL 30 DAY,8,'Seed movement'),
('BRANCH',2,32,'RECEIPT','IN',2,'DEMO',31,'MOV-000031',NOW()-INTERVAL 31 DAY,3,'Seed movement'),
('BRANCH',3,33,'TRANSFER','OUT',3,'DEMO',32,'MOV-000032',NOW()-INTERVAL 32 DAY,3,'Seed movement'),
('WAREHOUSE',2,34,'RECEIPT','IN',4,'DEMO',33,'MOV-000033',NOW()-INTERVAL 33 DAY,8,'Seed movement'),
('BRANCH',2,35,'TRANSFER','OUT',5,'DEMO',34,'MOV-000034',NOW()-INTERVAL 34 DAY,3,'Seed movement'),
('BRANCH',3,36,'RECEIPT','IN',6,'DEMO',35,'MOV-000035',NOW()-INTERVAL 35 DAY,3,'Seed movement'),
('WAREHOUSE',1,37,'TRANSFER','OUT',7,'DEMO',36,'MOV-000036',NOW()-INTERVAL 36 DAY,8,'Seed movement'),
('BRANCH',2,38,'RECEIPT','IN',8,'DEMO',37,'MOV-000037',NOW()-INTERVAL 37 DAY,3,'Seed movement'),
('BRANCH',3,39,'TRANSFER','OUT',9,'DEMO',38,'MOV-000038',NOW()-INTERVAL 38 DAY,3,'Seed movement'),
('WAREHOUSE',2,40,'RECEIPT','IN',10,'DEMO',39,'MOV-000039',NOW()-INTERVAL 39 DAY,8,'Seed movement'),
('BRANCH',2,41,'TRANSFER','OUT',11,'DEMO',40,'MOV-000040',NOW()-INTERVAL 40 DAY,3,'Seed movement'),
('BRANCH',3,42,'RECEIPT','IN',12,'DEMO',41,'MOV-000041',NOW()-INTERVAL 41 DAY,3,'Seed movement'),
('WAREHOUSE',1,43,'TRANSFER','OUT',13,'DEMO',42,'MOV-000042',NOW()-INTERVAL 42 DAY,8,'Seed movement'),
('BRANCH',2,44,'RECEIPT','IN',14,'DEMO',43,'MOV-000043',NOW()-INTERVAL 43 DAY,3,'Seed movement'),
('BRANCH',3,45,'TRANSFER','OUT',15,'DEMO',44,'MOV-000044',NOW()-INTERVAL 44 DAY,3,'Seed movement'),
('WAREHOUSE',2,46,'RECEIPT','IN',1,'DEMO',45,'MOV-000045',NOW()-INTERVAL 45 DAY,8,'Seed movement'),
('BRANCH',2,47,'TRANSFER','OUT',2,'DEMO',46,'MOV-000046',NOW()-INTERVAL 46 DAY,3,'Seed movement'),
('BRANCH',3,48,'RECEIPT','IN',3,'DEMO',47,'MOV-000047',NOW()-INTERVAL 47 DAY,3,'Seed movement'),
('WAREHOUSE',1,49,'TRANSFER','OUT',4,'DEMO',48,'MOV-000048',NOW()-INTERVAL 48 DAY,8,'Seed movement'),
('BRANCH',2,50,'RECEIPT','IN',5,'DEMO',49,'MOV-000049',NOW()-INTERVAL 49 DAY,3,'Seed movement'),
('BRANCH',3,51,'TRANSFER','OUT',6,'DEMO',50,'MOV-000050',NOW()-INTERVAL 50 DAY,3,'Seed movement'),
('WAREHOUSE',2,52,'RECEIPT','IN',7,'DEMO',51,'MOV-000051',NOW()-INTERVAL 51 DAY,8,'Seed movement'),
('BRANCH',2,53,'TRANSFER','OUT',8,'DEMO',52,'MOV-000052',NOW()-INTERVAL 52 DAY,3,'Seed movement'),
('BRANCH',3,54,'RECEIPT','IN',9,'DEMO',53,'MOV-000053',NOW()-INTERVAL 53 DAY,3,'Seed movement'),
('WAREHOUSE',1,55,'TRANSFER','OUT',10,'DEMO',54,'MOV-000054',NOW()-INTERVAL 54 DAY,8,'Seed movement'),
('BRANCH',2,56,'RECEIPT','IN',11,'DEMO',55,'MOV-000055',NOW()-INTERVAL 55 DAY,3,'Seed movement'),
('BRANCH',3,57,'TRANSFER','OUT',12,'DEMO',56,'MOV-000056',NOW()-INTERVAL 56 DAY,3,'Seed movement'),
('WAREHOUSE',2,58,'RECEIPT','IN',13,'DEMO',57,'MOV-000057',NOW()-INTERVAL 57 DAY,8,'Seed movement'),
('BRANCH',2,59,'TRANSFER','OUT',14,'DEMO',58,'MOV-000058',NOW()-INTERVAL 58 DAY,3,'Seed movement'),
('BRANCH',3,60,'RECEIPT','IN',15,'DEMO',59,'MOV-000059',NOW()-INTERVAL 59 DAY,3,'Seed movement'),
('WAREHOUSE',1,61,'TRANSFER','OUT',1,'DEMO',60,'MOV-000060',NOW()-INTERVAL 60 DAY,8,'Seed movement'),
('BRANCH',2,62,'RECEIPT','IN',2,'DEMO',61,'MOV-000061',NOW()-INTERVAL 61 DAY,3,'Seed movement'),
('BRANCH',3,63,'TRANSFER','OUT',3,'DEMO',62,'MOV-000062',NOW()-INTERVAL 62 DAY,3,'Seed movement'),
('WAREHOUSE',2,64,'RECEIPT','IN',4,'DEMO',63,'MOV-000063',NOW()-INTERVAL 63 DAY,8,'Seed movement'),
('BRANCH',2,65,'TRANSFER','OUT',5,'DEMO',64,'MOV-000064',NOW()-INTERVAL 64 DAY,3,'Seed movement'),
('BRANCH',3,66,'RECEIPT','IN',6,'DEMO',65,'MOV-000065',NOW()-INTERVAL 65 DAY,3,'Seed movement'),
('WAREHOUSE',1,67,'TRANSFER','OUT',7,'DEMO',66,'MOV-000066',NOW()-INTERVAL 66 DAY,8,'Seed movement'),
('BRANCH',2,68,'RECEIPT','IN',8,'DEMO',67,'MOV-000067',NOW()-INTERVAL 67 DAY,3,'Seed movement'),
('BRANCH',3,69,'TRANSFER','OUT',9,'DEMO',68,'MOV-000068',NOW()-INTERVAL 68 DAY,3,'Seed movement'),
('WAREHOUSE',2,70,'RECEIPT','IN',10,'DEMO',69,'MOV-000069',NOW()-INTERVAL 69 DAY,8,'Seed movement'),
('BRANCH',2,71,'TRANSFER','OUT',11,'DEMO',70,'MOV-000070',NOW()-INTERVAL 70 DAY,3,'Seed movement'),
('BRANCH',3,72,'RECEIPT','IN',12,'DEMO',71,'MOV-000071',NOW()-INTERVAL 71 DAY,3,'Seed movement'),
('WAREHOUSE',1,73,'TRANSFER','OUT',13,'DEMO',72,'MOV-000072',NOW()-INTERVAL 72 DAY,8,'Seed movement'),
('BRANCH',2,74,'RECEIPT','IN',14,'DEMO',73,'MOV-000073',NOW()-INTERVAL 73 DAY,3,'Seed movement'),
('BRANCH',3,75,'TRANSFER','OUT',15,'DEMO',74,'MOV-000074',NOW()-INTERVAL 74 DAY,3,'Seed movement'),
('WAREHOUSE',2,76,'RECEIPT','IN',1,'DEMO',75,'MOV-000075',NOW()-INTERVAL 75 DAY,8,'Seed movement'),
('BRANCH',2,77,'TRANSFER','OUT',2,'DEMO',76,'MOV-000076',NOW()-INTERVAL 76 DAY,3,'Seed movement'),
('BRANCH',3,78,'RECEIPT','IN',3,'DEMO',77,'MOV-000077',NOW()-INTERVAL 77 DAY,3,'Seed movement'),
('WAREHOUSE',1,79,'TRANSFER','OUT',4,'DEMO',78,'MOV-000078',NOW()-INTERVAL 78 DAY,8,'Seed movement'),
('BRANCH',2,80,'RECEIPT','IN',5,'DEMO',79,'MOV-000079',NOW()-INTERVAL 79 DAY,3,'Seed movement'),
('BRANCH',3,81,'TRANSFER','OUT',6,'DEMO',80,'MOV-000080',NOW()-INTERVAL 80 DAY,3,'Seed movement'),
('WAREHOUSE',2,82,'RECEIPT','IN',7,'DEMO',81,'MOV-000081',NOW()-INTERVAL 81 DAY,8,'Seed movement'),
('BRANCH',2,83,'TRANSFER','OUT',8,'DEMO',82,'MOV-000082',NOW()-INTERVAL 82 DAY,3,'Seed movement'),
('BRANCH',3,84,'RECEIPT','IN',9,'DEMO',83,'MOV-000083',NOW()-INTERVAL 83 DAY,3,'Seed movement'),
('WAREHOUSE',1,85,'TRANSFER','OUT',10,'DEMO',84,'MOV-000084',NOW()-INTERVAL 84 DAY,8,'Seed movement'),
('BRANCH',2,86,'RECEIPT','IN',11,'DEMO',85,'MOV-000085',NOW()-INTERVAL 85 DAY,3,'Seed movement'),
('BRANCH',3,87,'TRANSFER','OUT',12,'DEMO',86,'MOV-000086',NOW()-INTERVAL 86 DAY,3,'Seed movement'),
('WAREHOUSE',2,88,'RECEIPT','IN',13,'DEMO',87,'MOV-000087',NOW()-INTERVAL 87 DAY,8,'Seed movement'),
('BRANCH',2,89,'TRANSFER','OUT',14,'DEMO',88,'MOV-000088',NOW()-INTERVAL 88 DAY,3,'Seed movement'),
('BRANCH',3,90,'RECEIPT','IN',15,'DEMO',89,'MOV-000089',NOW()-INTERVAL 89 DAY,3,'Seed movement'),
('WAREHOUSE',1,91,'TRANSFER','OUT',1,'DEMO',90,'MOV-000090',NOW()-INTERVAL 90 DAY,8,'Seed movement'),
('BRANCH',2,92,'RECEIPT','IN',2,'DEMO',91,'MOV-000091',NOW()-INTERVAL 91 DAY,3,'Seed movement'),
('BRANCH',3,93,'TRANSFER','OUT',3,'DEMO',92,'MOV-000092',NOW()-INTERVAL 92 DAY,3,'Seed movement'),
('WAREHOUSE',2,94,'RECEIPT','IN',4,'DEMO',93,'MOV-000093',NOW()-INTERVAL 93 DAY,8,'Seed movement'),
('BRANCH',2,95,'TRANSFER','OUT',5,'DEMO',94,'MOV-000094',NOW()-INTERVAL 94 DAY,3,'Seed movement'),
('BRANCH',3,96,'RECEIPT','IN',6,'DEMO',95,'MOV-000095',NOW()-INTERVAL 95 DAY,3,'Seed movement'),
('WAREHOUSE',1,97,'TRANSFER','OUT',7,'DEMO',96,'MOV-000096',NOW()-INTERVAL 96 DAY,8,'Seed movement'),
('BRANCH',2,98,'RECEIPT','IN',8,'DEMO',97,'MOV-000097',NOW()-INTERVAL 97 DAY,3,'Seed movement'),
('BRANCH',3,99,'TRANSFER','OUT',9,'DEMO',98,'MOV-000098',NOW()-INTERVAL 98 DAY,3,'Seed movement'),
('WAREHOUSE',2,100,'RECEIPT','IN',10,'DEMO',99,'MOV-000099',NOW()-INTERVAL 99 DAY,8,'Seed movement'),
('BRANCH',2,101,'TRANSFER','OUT',11,'DEMO',100,'MOV-000100',NOW()-INTERVAL 100 DAY,3,'Seed movement'),
('BRANCH',3,102,'RECEIPT','IN',12,'DEMO',101,'MOV-000101',NOW()-INTERVAL 101 DAY,3,'Seed movement'),
('WAREHOUSE',1,103,'TRANSFER','OUT',13,'DEMO',102,'MOV-000102',NOW()-INTERVAL 102 DAY,8,'Seed movement'),
('BRANCH',2,104,'RECEIPT','IN',14,'DEMO',103,'MOV-000103',NOW()-INTERVAL 103 DAY,3,'Seed movement'),
('BRANCH',3,105,'TRANSFER','OUT',15,'DEMO',104,'MOV-000104',NOW()-INTERVAL 104 DAY,3,'Seed movement'),
('WAREHOUSE',2,106,'RECEIPT','IN',1,'DEMO',105,'MOV-000105',NOW()-INTERVAL 105 DAY,8,'Seed movement'),
('BRANCH',2,107,'TRANSFER','OUT',2,'DEMO',106,'MOV-000106',NOW()-INTERVAL 106 DAY,3,'Seed movement'),
('BRANCH',3,108,'RECEIPT','IN',3,'DEMO',107,'MOV-000107',NOW()-INTERVAL 107 DAY,3,'Seed movement'),
('WAREHOUSE',1,109,'TRANSFER','OUT',4,'DEMO',108,'MOV-000108',NOW()-INTERVAL 108 DAY,8,'Seed movement'),
('BRANCH',2,110,'RECEIPT','IN',5,'DEMO',109,'MOV-000109',NOW()-INTERVAL 109 DAY,3,'Seed movement'),
('BRANCH',3,111,'TRANSFER','OUT',6,'DEMO',110,'MOV-000110',NOW()-INTERVAL 110 DAY,3,'Seed movement'),
('WAREHOUSE',2,112,'RECEIPT','IN',7,'DEMO',111,'MOV-000111',NOW()-INTERVAL 111 DAY,8,'Seed movement'),
('BRANCH',2,113,'TRANSFER','OUT',8,'DEMO',112,'MOV-000112',NOW()-INTERVAL 112 DAY,3,'Seed movement'),
('BRANCH',3,114,'RECEIPT','IN',9,'DEMO',113,'MOV-000113',NOW()-INTERVAL 113 DAY,3,'Seed movement'),
('WAREHOUSE',1,115,'TRANSFER','OUT',10,'DEMO',114,'MOV-000114',NOW()-INTERVAL 114 DAY,8,'Seed movement'),
('BRANCH',2,116,'RECEIPT','IN',11,'DEMO',115,'MOV-000115',NOW()-INTERVAL 115 DAY,3,'Seed movement'),
('BRANCH',3,117,'TRANSFER','OUT',12,'DEMO',116,'MOV-000116',NOW()-INTERVAL 116 DAY,3,'Seed movement'),
('WAREHOUSE',2,118,'RECEIPT','IN',13,'DEMO',117,'MOV-000117',NOW()-INTERVAL 117 DAY,8,'Seed movement'),
('BRANCH',2,119,'TRANSFER','OUT',14,'DEMO',118,'MOV-000118',NOW()-INTERVAL 118 DAY,3,'Seed movement'),
('BRANCH',3,120,'RECEIPT','IN',15,'DEMO',119,'MOV-000119',NOW()-INTERVAL 119 DAY,3,'Seed movement'),
('WAREHOUSE',1,121,'TRANSFER','OUT',1,'DEMO',120,'MOV-000120',NOW()-INTERVAL 0 DAY,8,'Seed movement'),
('BRANCH',2,122,'RECEIPT','IN',2,'DEMO',121,'MOV-000121',NOW()-INTERVAL 1 DAY,3,'Seed movement'),
('BRANCH',3,123,'TRANSFER','OUT',3,'DEMO',122,'MOV-000122',NOW()-INTERVAL 2 DAY,3,'Seed movement'),
('WAREHOUSE',2,124,'RECEIPT','IN',4,'DEMO',123,'MOV-000123',NOW()-INTERVAL 3 DAY,8,'Seed movement'),
('BRANCH',2,125,'TRANSFER','OUT',5,'DEMO',124,'MOV-000124',NOW()-INTERVAL 4 DAY,3,'Seed movement'),
('BRANCH',3,126,'RECEIPT','IN',6,'DEMO',125,'MOV-000125',NOW()-INTERVAL 5 DAY,3,'Seed movement'),
('WAREHOUSE',1,127,'TRANSFER','OUT',7,'DEMO',126,'MOV-000126',NOW()-INTERVAL 6 DAY,8,'Seed movement'),
('BRANCH',2,128,'RECEIPT','IN',8,'DEMO',127,'MOV-000127',NOW()-INTERVAL 7 DAY,3,'Seed movement'),
('BRANCH',3,129,'TRANSFER','OUT',9,'DEMO',128,'MOV-000128',NOW()-INTERVAL 8 DAY,3,'Seed movement'),
('WAREHOUSE',2,130,'RECEIPT','IN',10,'DEMO',129,'MOV-000129',NOW()-INTERVAL 9 DAY,8,'Seed movement'),
('BRANCH',2,131,'TRANSFER','OUT',11,'DEMO',130,'MOV-000130',NOW()-INTERVAL 10 DAY,3,'Seed movement'),
('BRANCH',3,132,'RECEIPT','IN',12,'DEMO',131,'MOV-000131',NOW()-INTERVAL 11 DAY,3,'Seed movement'),
('WAREHOUSE',1,133,'TRANSFER','OUT',13,'DEMO',132,'MOV-000132',NOW()-INTERVAL 12 DAY,8,'Seed movement'),
('BRANCH',2,134,'RECEIPT','IN',14,'DEMO',133,'MOV-000133',NOW()-INTERVAL 13 DAY,3,'Seed movement'),
('BRANCH',3,135,'TRANSFER','OUT',15,'DEMO',134,'MOV-000134',NOW()-INTERVAL 14 DAY,3,'Seed movement'),
('WAREHOUSE',2,136,'RECEIPT','IN',1,'DEMO',135,'MOV-000135',NOW()-INTERVAL 15 DAY,8,'Seed movement'),
('BRANCH',2,137,'TRANSFER','OUT',2,'DEMO',136,'MOV-000136',NOW()-INTERVAL 16 DAY,3,'Seed movement'),
('BRANCH',3,138,'RECEIPT','IN',3,'DEMO',137,'MOV-000137',NOW()-INTERVAL 17 DAY,3,'Seed movement'),
('WAREHOUSE',1,139,'TRANSFER','OUT',4,'DEMO',138,'MOV-000138',NOW()-INTERVAL 18 DAY,8,'Seed movement'),
('BRANCH',2,140,'RECEIPT','IN',5,'DEMO',139,'MOV-000139',NOW()-INTERVAL 19 DAY,3,'Seed movement'),
('BRANCH',3,141,'TRANSFER','OUT',6,'DEMO',140,'MOV-000140',NOW()-INTERVAL 20 DAY,3,'Seed movement'),
('WAREHOUSE',2,142,'RECEIPT','IN',7,'DEMO',141,'MOV-000141',NOW()-INTERVAL 21 DAY,8,'Seed movement'),
('BRANCH',2,143,'TRANSFER','OUT',8,'DEMO',142,'MOV-000142',NOW()-INTERVAL 22 DAY,3,'Seed movement'),
('BRANCH',3,144,'RECEIPT','IN',9,'DEMO',143,'MOV-000143',NOW()-INTERVAL 23 DAY,3,'Seed movement'),
('WAREHOUSE',1,145,'TRANSFER','OUT',10,'DEMO',144,'MOV-000144',NOW()-INTERVAL 24 DAY,8,'Seed movement'),
('BRANCH',2,146,'RECEIPT','IN',11,'DEMO',145,'MOV-000145',NOW()-INTERVAL 25 DAY,3,'Seed movement'),
('BRANCH',3,147,'TRANSFER','OUT',12,'DEMO',146,'MOV-000146',NOW()-INTERVAL 26 DAY,3,'Seed movement'),
('WAREHOUSE',2,148,'RECEIPT','IN',13,'DEMO',147,'MOV-000147',NOW()-INTERVAL 27 DAY,8,'Seed movement'),
('BRANCH',2,149,'TRANSFER','OUT',14,'DEMO',148,'MOV-000148',NOW()-INTERVAL 28 DAY,3,'Seed movement'),
('BRANCH',3,150,'RECEIPT','IN',15,'DEMO',149,'MOV-000149',NOW()-INTERVAL 29 DAY,3,'Seed movement'),
('WAREHOUSE',1,151,'TRANSFER','OUT',1,'DEMO',150,'MOV-000150',NOW()-INTERVAL 30 DAY,8,'Seed movement'),
('BRANCH',2,152,'RECEIPT','IN',2,'DEMO',151,'MOV-000151',NOW()-INTERVAL 31 DAY,3,'Seed movement'),
('BRANCH',3,153,'TRANSFER','OUT',3,'DEMO',152,'MOV-000152',NOW()-INTERVAL 32 DAY,3,'Seed movement'),
('WAREHOUSE',2,154,'RECEIPT','IN',4,'DEMO',153,'MOV-000153',NOW()-INTERVAL 33 DAY,8,'Seed movement'),
('BRANCH',2,155,'TRANSFER','OUT',5,'DEMO',154,'MOV-000154',NOW()-INTERVAL 34 DAY,3,'Seed movement'),
('BRANCH',3,156,'RECEIPT','IN',6,'DEMO',155,'MOV-000155',NOW()-INTERVAL 35 DAY,3,'Seed movement'),
('WAREHOUSE',1,157,'TRANSFER','OUT',7,'DEMO',156,'MOV-000156',NOW()-INTERVAL 36 DAY,8,'Seed movement'),
('BRANCH',2,158,'RECEIPT','IN',8,'DEMO',157,'MOV-000157',NOW()-INTERVAL 37 DAY,3,'Seed movement'),
('BRANCH',3,159,'TRANSFER','OUT',9,'DEMO',158,'MOV-000158',NOW()-INTERVAL 38 DAY,3,'Seed movement'),
('WAREHOUSE',2,160,'RECEIPT','IN',10,'DEMO',159,'MOV-000159',NOW()-INTERVAL 39 DAY,8,'Seed movement'),
('BRANCH',2,161,'TRANSFER','OUT',11,'DEMO',160,'MOV-000160',NOW()-INTERVAL 40 DAY,3,'Seed movement'),
('BRANCH',3,162,'RECEIPT','IN',12,'DEMO',161,'MOV-000161',NOW()-INTERVAL 41 DAY,3,'Seed movement'),
('WAREHOUSE',1,163,'TRANSFER','OUT',13,'DEMO',162,'MOV-000162',NOW()-INTERVAL 42 DAY,8,'Seed movement'),
('BRANCH',2,164,'RECEIPT','IN',14,'DEMO',163,'MOV-000163',NOW()-INTERVAL 43 DAY,3,'Seed movement'),
('BRANCH',3,165,'TRANSFER','OUT',15,'DEMO',164,'MOV-000164',NOW()-INTERVAL 44 DAY,3,'Seed movement'),
('WAREHOUSE',2,166,'RECEIPT','IN',1,'DEMO',165,'MOV-000165',NOW()-INTERVAL 45 DAY,8,'Seed movement'),
('BRANCH',2,167,'TRANSFER','OUT',2,'DEMO',166,'MOV-000166',NOW()-INTERVAL 46 DAY,3,'Seed movement'),
('BRANCH',3,168,'RECEIPT','IN',3,'DEMO',167,'MOV-000167',NOW()-INTERVAL 47 DAY,3,'Seed movement'),
('WAREHOUSE',1,169,'TRANSFER','OUT',4,'DEMO',168,'MOV-000168',NOW()-INTERVAL 48 DAY,8,'Seed movement'),
('BRANCH',2,170,'RECEIPT','IN',5,'DEMO',169,'MOV-000169',NOW()-INTERVAL 49 DAY,3,'Seed movement'),
('BRANCH',3,171,'TRANSFER','OUT',6,'DEMO',170,'MOV-000170',NOW()-INTERVAL 50 DAY,3,'Seed movement'),
('WAREHOUSE',2,172,'RECEIPT','IN',7,'DEMO',171,'MOV-000171',NOW()-INTERVAL 51 DAY,8,'Seed movement'),
('BRANCH',2,173,'TRANSFER','OUT',8,'DEMO',172,'MOV-000172',NOW()-INTERVAL 52 DAY,3,'Seed movement'),
('BRANCH',3,174,'RECEIPT','IN',9,'DEMO',173,'MOV-000173',NOW()-INTERVAL 53 DAY,3,'Seed movement'),
('WAREHOUSE',1,175,'TRANSFER','OUT',10,'DEMO',174,'MOV-000174',NOW()-INTERVAL 54 DAY,8,'Seed movement'),
('BRANCH',2,176,'RECEIPT','IN',11,'DEMO',175,'MOV-000175',NOW()-INTERVAL 55 DAY,3,'Seed movement'),
('BRANCH',3,177,'TRANSFER','OUT',12,'DEMO',176,'MOV-000176',NOW()-INTERVAL 56 DAY,3,'Seed movement'),
('WAREHOUSE',2,178,'RECEIPT','IN',13,'DEMO',177,'MOV-000177',NOW()-INTERVAL 57 DAY,8,'Seed movement'),
('BRANCH',2,179,'TRANSFER','OUT',14,'DEMO',178,'MOV-000178',NOW()-INTERVAL 58 DAY,3,'Seed movement'),
('BRANCH',3,180,'RECEIPT','IN',15,'DEMO',179,'MOV-000179',NOW()-INTERVAL 59 DAY,3,'Seed movement'),
('WAREHOUSE',1,1,'TRANSFER','OUT',1,'DEMO',180,'MOV-000180',NOW()-INTERVAL 60 DAY,8,'Seed movement'),
('BRANCH',2,2,'RECEIPT','IN',2,'DEMO',181,'MOV-000181',NOW()-INTERVAL 61 DAY,3,'Seed movement'),
('BRANCH',3,3,'TRANSFER','OUT',3,'DEMO',182,'MOV-000182',NOW()-INTERVAL 62 DAY,3,'Seed movement'),
('WAREHOUSE',2,4,'RECEIPT','IN',4,'DEMO',183,'MOV-000183',NOW()-INTERVAL 63 DAY,8,'Seed movement'),
('BRANCH',2,5,'TRANSFER','OUT',5,'DEMO',184,'MOV-000184',NOW()-INTERVAL 64 DAY,3,'Seed movement'),
('BRANCH',3,6,'RECEIPT','IN',6,'DEMO',185,'MOV-000185',NOW()-INTERVAL 65 DAY,3,'Seed movement'),
('WAREHOUSE',1,7,'TRANSFER','OUT',7,'DEMO',186,'MOV-000186',NOW()-INTERVAL 66 DAY,8,'Seed movement'),
('BRANCH',2,8,'RECEIPT','IN',8,'DEMO',187,'MOV-000187',NOW()-INTERVAL 67 DAY,3,'Seed movement'),
('BRANCH',3,9,'TRANSFER','OUT',9,'DEMO',188,'MOV-000188',NOW()-INTERVAL 68 DAY,3,'Seed movement'),
('WAREHOUSE',2,10,'RECEIPT','IN',10,'DEMO',189,'MOV-000189',NOW()-INTERVAL 69 DAY,8,'Seed movement'),
('BRANCH',2,11,'TRANSFER','OUT',11,'DEMO',190,'MOV-000190',NOW()-INTERVAL 70 DAY,3,'Seed movement'),
('BRANCH',3,12,'RECEIPT','IN',12,'DEMO',191,'MOV-000191',NOW()-INTERVAL 71 DAY,3,'Seed movement'),
('WAREHOUSE',1,13,'TRANSFER','OUT',13,'DEMO',192,'MOV-000192',NOW()-INTERVAL 72 DAY,8,'Seed movement'),
('BRANCH',2,14,'RECEIPT','IN',14,'DEMO',193,'MOV-000193',NOW()-INTERVAL 73 DAY,3,'Seed movement'),
('BRANCH',3,15,'TRANSFER','OUT',15,'DEMO',194,'MOV-000194',NOW()-INTERVAL 74 DAY,3,'Seed movement'),
('WAREHOUSE',2,16,'RECEIPT','IN',1,'DEMO',195,'MOV-000195',NOW()-INTERVAL 75 DAY,8,'Seed movement'),
('BRANCH',2,17,'TRANSFER','OUT',2,'DEMO',196,'MOV-000196',NOW()-INTERVAL 76 DAY,3,'Seed movement'),
('BRANCH',3,18,'RECEIPT','IN',3,'DEMO',197,'MOV-000197',NOW()-INTERVAL 77 DAY,3,'Seed movement'),
('WAREHOUSE',1,19,'TRANSFER','OUT',4,'DEMO',198,'MOV-000198',NOW()-INTERVAL 78 DAY,8,'Seed movement'),
('BRANCH',2,20,'RECEIPT','IN',5,'DEMO',199,'MOV-000199',NOW()-INTERVAL 79 DAY,3,'Seed movement'),
('BRANCH',3,21,'TRANSFER','OUT',6,'DEMO',200,'MOV-000200',NOW()-INTERVAL 80 DAY,3,'Seed movement'),
('WAREHOUSE',2,22,'RECEIPT','IN',7,'DEMO',201,'MOV-000201',NOW()-INTERVAL 81 DAY,8,'Seed movement'),
('BRANCH',2,23,'TRANSFER','OUT',8,'DEMO',202,'MOV-000202',NOW()-INTERVAL 82 DAY,3,'Seed movement'),
('BRANCH',3,24,'RECEIPT','IN',9,'DEMO',203,'MOV-000203',NOW()-INTERVAL 83 DAY,3,'Seed movement'),
('WAREHOUSE',1,25,'TRANSFER','OUT',10,'DEMO',204,'MOV-000204',NOW()-INTERVAL 84 DAY,8,'Seed movement'),
('BRANCH',2,26,'RECEIPT','IN',11,'DEMO',205,'MOV-000205',NOW()-INTERVAL 85 DAY,3,'Seed movement'),
('BRANCH',3,27,'TRANSFER','OUT',12,'DEMO',206,'MOV-000206',NOW()-INTERVAL 86 DAY,3,'Seed movement'),
('WAREHOUSE',2,28,'RECEIPT','IN',13,'DEMO',207,'MOV-000207',NOW()-INTERVAL 87 DAY,8,'Seed movement'),
('BRANCH',2,29,'TRANSFER','OUT',14,'DEMO',208,'MOV-000208',NOW()-INTERVAL 88 DAY,3,'Seed movement'),
('BRANCH',3,30,'RECEIPT','IN',15,'DEMO',209,'MOV-000209',NOW()-INTERVAL 89 DAY,3,'Seed movement'),
('WAREHOUSE',1,31,'TRANSFER','OUT',1,'DEMO',210,'MOV-000210',NOW()-INTERVAL 90 DAY,8,'Seed movement'),
('BRANCH',2,32,'RECEIPT','IN',2,'DEMO',211,'MOV-000211',NOW()-INTERVAL 91 DAY,3,'Seed movement'),
('BRANCH',3,33,'TRANSFER','OUT',3,'DEMO',212,'MOV-000212',NOW()-INTERVAL 92 DAY,3,'Seed movement'),
('WAREHOUSE',2,34,'RECEIPT','IN',4,'DEMO',213,'MOV-000213',NOW()-INTERVAL 93 DAY,8,'Seed movement'),
('BRANCH',2,35,'TRANSFER','OUT',5,'DEMO',214,'MOV-000214',NOW()-INTERVAL 94 DAY,3,'Seed movement'),
('BRANCH',3,36,'RECEIPT','IN',6,'DEMO',215,'MOV-000215',NOW()-INTERVAL 95 DAY,3,'Seed movement'),
('WAREHOUSE',1,37,'TRANSFER','OUT',7,'DEMO',216,'MOV-000216',NOW()-INTERVAL 96 DAY,8,'Seed movement'),
('BRANCH',2,38,'RECEIPT','IN',8,'DEMO',217,'MOV-000217',NOW()-INTERVAL 97 DAY,3,'Seed movement'),
('BRANCH',3,39,'TRANSFER','OUT',9,'DEMO',218,'MOV-000218',NOW()-INTERVAL 98 DAY,3,'Seed movement'),
('WAREHOUSE',2,40,'RECEIPT','IN',10,'DEMO',219,'MOV-000219',NOW()-INTERVAL 99 DAY,8,'Seed movement'),
('BRANCH',2,41,'TRANSFER','OUT',11,'DEMO',220,'MOV-000220',NOW()-INTERVAL 100 DAY,3,'Seed movement'),
('BRANCH',3,42,'RECEIPT','IN',12,'DEMO',221,'MOV-000221',NOW()-INTERVAL 101 DAY,3,'Seed movement'),
('WAREHOUSE',1,43,'TRANSFER','OUT',13,'DEMO',222,'MOV-000222',NOW()-INTERVAL 102 DAY,8,'Seed movement'),
('BRANCH',2,44,'RECEIPT','IN',14,'DEMO',223,'MOV-000223',NOW()-INTERVAL 103 DAY,3,'Seed movement'),
('BRANCH',3,45,'TRANSFER','OUT',15,'DEMO',224,'MOV-000224',NOW()-INTERVAL 104 DAY,3,'Seed movement'),
('WAREHOUSE',2,46,'RECEIPT','IN',1,'DEMO',225,'MOV-000225',NOW()-INTERVAL 105 DAY,8,'Seed movement'),
('BRANCH',2,47,'TRANSFER','OUT',2,'DEMO',226,'MOV-000226',NOW()-INTERVAL 106 DAY,3,'Seed movement'),
('BRANCH',3,48,'RECEIPT','IN',3,'DEMO',227,'MOV-000227',NOW()-INTERVAL 107 DAY,3,'Seed movement'),
('WAREHOUSE',1,49,'TRANSFER','OUT',4,'DEMO',228,'MOV-000228',NOW()-INTERVAL 108 DAY,8,'Seed movement'),
('BRANCH',2,50,'RECEIPT','IN',5,'DEMO',229,'MOV-000229',NOW()-INTERVAL 109 DAY,3,'Seed movement'),
('BRANCH',3,51,'TRANSFER','OUT',6,'DEMO',230,'MOV-000230',NOW()-INTERVAL 110 DAY,3,'Seed movement'),
('WAREHOUSE',2,52,'RECEIPT','IN',7,'DEMO',231,'MOV-000231',NOW()-INTERVAL 111 DAY,8,'Seed movement'),
('BRANCH',2,53,'TRANSFER','OUT',8,'DEMO',232,'MOV-000232',NOW()-INTERVAL 112 DAY,3,'Seed movement'),
('BRANCH',3,54,'RECEIPT','IN',9,'DEMO',233,'MOV-000233',NOW()-INTERVAL 113 DAY,3,'Seed movement'),
('WAREHOUSE',1,55,'TRANSFER','OUT',10,'DEMO',234,'MOV-000234',NOW()-INTERVAL 114 DAY,8,'Seed movement'),
('BRANCH',2,56,'RECEIPT','IN',11,'DEMO',235,'MOV-000235',NOW()-INTERVAL 115 DAY,3,'Seed movement'),
('BRANCH',3,57,'TRANSFER','OUT',12,'DEMO',236,'MOV-000236',NOW()-INTERVAL 116 DAY,3,'Seed movement'),
('WAREHOUSE',2,58,'RECEIPT','IN',13,'DEMO',237,'MOV-000237',NOW()-INTERVAL 117 DAY,8,'Seed movement'),
('BRANCH',2,59,'TRANSFER','OUT',14,'DEMO',238,'MOV-000238',NOW()-INTERVAL 118 DAY,3,'Seed movement'),
('BRANCH',3,60,'RECEIPT','IN',15,'DEMO',239,'MOV-000239',NOW()-INTERVAL 119 DAY,3,'Seed movement'),
('WAREHOUSE',1,61,'TRANSFER','OUT',1,'DEMO',240,'MOV-000240',NOW()-INTERVAL 0 DAY,8,'Seed movement'),
('BRANCH',2,62,'RECEIPT','IN',2,'DEMO',241,'MOV-000241',NOW()-INTERVAL 1 DAY,3,'Seed movement'),
('BRANCH',3,63,'TRANSFER','OUT',3,'DEMO',242,'MOV-000242',NOW()-INTERVAL 2 DAY,3,'Seed movement'),
('WAREHOUSE',2,64,'RECEIPT','IN',4,'DEMO',243,'MOV-000243',NOW()-INTERVAL 3 DAY,8,'Seed movement'),
('BRANCH',2,65,'TRANSFER','OUT',5,'DEMO',244,'MOV-000244',NOW()-INTERVAL 4 DAY,3,'Seed movement'),
('BRANCH',3,66,'RECEIPT','IN',6,'DEMO',245,'MOV-000245',NOW()-INTERVAL 5 DAY,3,'Seed movement'),
('WAREHOUSE',1,67,'TRANSFER','OUT',7,'DEMO',246,'MOV-000246',NOW()-INTERVAL 6 DAY,8,'Seed movement'),
('BRANCH',2,68,'RECEIPT','IN',8,'DEMO',247,'MOV-000247',NOW()-INTERVAL 7 DAY,3,'Seed movement'),
('BRANCH',3,69,'TRANSFER','OUT',9,'DEMO',248,'MOV-000248',NOW()-INTERVAL 8 DAY,3,'Seed movement'),
('WAREHOUSE',2,70,'RECEIPT','IN',10,'DEMO',249,'MOV-000249',NOW()-INTERVAL 9 DAY,8,'Seed movement'),
('BRANCH',2,71,'TRANSFER','OUT',11,'DEMO',250,'MOV-000250',NOW()-INTERVAL 10 DAY,3,'Seed movement'),
('BRANCH',3,72,'RECEIPT','IN',12,'DEMO',251,'MOV-000251',NOW()-INTERVAL 11 DAY,3,'Seed movement'),
('WAREHOUSE',1,73,'TRANSFER','OUT',13,'DEMO',252,'MOV-000252',NOW()-INTERVAL 12 DAY,8,'Seed movement'),
('BRANCH',2,74,'RECEIPT','IN',14,'DEMO',253,'MOV-000253',NOW()-INTERVAL 13 DAY,3,'Seed movement'),
('BRANCH',3,75,'TRANSFER','OUT',15,'DEMO',254,'MOV-000254',NOW()-INTERVAL 14 DAY,3,'Seed movement'),
('WAREHOUSE',2,76,'RECEIPT','IN',1,'DEMO',255,'MOV-000255',NOW()-INTERVAL 15 DAY,8,'Seed movement'),
('BRANCH',2,77,'TRANSFER','OUT',2,'DEMO',256,'MOV-000256',NOW()-INTERVAL 16 DAY,3,'Seed movement'),
('BRANCH',3,78,'RECEIPT','IN',3,'DEMO',257,'MOV-000257',NOW()-INTERVAL 17 DAY,3,'Seed movement'),
('WAREHOUSE',1,79,'TRANSFER','OUT',4,'DEMO',258,'MOV-000258',NOW()-INTERVAL 18 DAY,8,'Seed movement'),
('BRANCH',2,80,'RECEIPT','IN',5,'DEMO',259,'MOV-000259',NOW()-INTERVAL 19 DAY,3,'Seed movement'),
('BRANCH',3,81,'TRANSFER','OUT',6,'DEMO',260,'MOV-000260',NOW()-INTERVAL 20 DAY,3,'Seed movement'),
('WAREHOUSE',2,82,'RECEIPT','IN',7,'DEMO',261,'MOV-000261',NOW()-INTERVAL 21 DAY,8,'Seed movement'),
('BRANCH',2,83,'TRANSFER','OUT',8,'DEMO',262,'MOV-000262',NOW()-INTERVAL 22 DAY,3,'Seed movement'),
('BRANCH',3,84,'RECEIPT','IN',9,'DEMO',263,'MOV-000263',NOW()-INTERVAL 23 DAY,3,'Seed movement'),
('WAREHOUSE',1,85,'TRANSFER','OUT',10,'DEMO',264,'MOV-000264',NOW()-INTERVAL 24 DAY,8,'Seed movement'),
('BRANCH',2,86,'RECEIPT','IN',11,'DEMO',265,'MOV-000265',NOW()-INTERVAL 25 DAY,3,'Seed movement'),
('BRANCH',3,87,'TRANSFER','OUT',12,'DEMO',266,'MOV-000266',NOW()-INTERVAL 26 DAY,3,'Seed movement'),
('WAREHOUSE',2,88,'RECEIPT','IN',13,'DEMO',267,'MOV-000267',NOW()-INTERVAL 27 DAY,8,'Seed movement'),
('BRANCH',2,89,'TRANSFER','OUT',14,'DEMO',268,'MOV-000268',NOW()-INTERVAL 28 DAY,3,'Seed movement'),
('BRANCH',3,90,'RECEIPT','IN',15,'DEMO',269,'MOV-000269',NOW()-INTERVAL 29 DAY,3,'Seed movement'),
('WAREHOUSE',1,91,'TRANSFER','OUT',1,'DEMO',270,'MOV-000270',NOW()-INTERVAL 30 DAY,8,'Seed movement'),
('BRANCH',2,92,'RECEIPT','IN',2,'DEMO',271,'MOV-000271',NOW()-INTERVAL 31 DAY,3,'Seed movement'),
('BRANCH',3,93,'TRANSFER','OUT',3,'DEMO',272,'MOV-000272',NOW()-INTERVAL 32 DAY,3,'Seed movement'),
('WAREHOUSE',2,94,'RECEIPT','IN',4,'DEMO',273,'MOV-000273',NOW()-INTERVAL 33 DAY,8,'Seed movement'),
('BRANCH',2,95,'TRANSFER','OUT',5,'DEMO',274,'MOV-000274',NOW()-INTERVAL 34 DAY,3,'Seed movement'),
('BRANCH',3,96,'RECEIPT','IN',6,'DEMO',275,'MOV-000275',NOW()-INTERVAL 35 DAY,3,'Seed movement'),
('WAREHOUSE',1,97,'TRANSFER','OUT',7,'DEMO',276,'MOV-000276',NOW()-INTERVAL 36 DAY,8,'Seed movement'),
('BRANCH',2,98,'RECEIPT','IN',8,'DEMO',277,'MOV-000277',NOW()-INTERVAL 37 DAY,3,'Seed movement'),
('BRANCH',3,99,'TRANSFER','OUT',9,'DEMO',278,'MOV-000278',NOW()-INTERVAL 38 DAY,3,'Seed movement'),
('WAREHOUSE',2,100,'RECEIPT','IN',10,'DEMO',279,'MOV-000279',NOW()-INTERVAL 39 DAY,8,'Seed movement'),
('BRANCH',2,101,'TRANSFER','OUT',11,'DEMO',280,'MOV-000280',NOW()-INTERVAL 40 DAY,3,'Seed movement'),
('BRANCH',3,102,'RECEIPT','IN',12,'DEMO',281,'MOV-000281',NOW()-INTERVAL 41 DAY,3,'Seed movement'),
('WAREHOUSE',1,103,'TRANSFER','OUT',13,'DEMO',282,'MOV-000282',NOW()-INTERVAL 42 DAY,8,'Seed movement'),
('BRANCH',2,104,'RECEIPT','IN',14,'DEMO',283,'MOV-000283',NOW()-INTERVAL 43 DAY,3,'Seed movement'),
('BRANCH',3,105,'TRANSFER','OUT',15,'DEMO',284,'MOV-000284',NOW()-INTERVAL 44 DAY,3,'Seed movement'),
('WAREHOUSE',2,106,'RECEIPT','IN',1,'DEMO',285,'MOV-000285',NOW()-INTERVAL 45 DAY,8,'Seed movement'),
('BRANCH',2,107,'TRANSFER','OUT',2,'DEMO',286,'MOV-000286',NOW()-INTERVAL 46 DAY,3,'Seed movement'),
('BRANCH',3,108,'RECEIPT','IN',3,'DEMO',287,'MOV-000287',NOW()-INTERVAL 47 DAY,3,'Seed movement'),
('WAREHOUSE',1,109,'TRANSFER','OUT',4,'DEMO',288,'MOV-000288',NOW()-INTERVAL 48 DAY,8,'Seed movement'),
('BRANCH',2,110,'RECEIPT','IN',5,'DEMO',289,'MOV-000289',NOW()-INTERVAL 49 DAY,3,'Seed movement'),
('BRANCH',3,111,'TRANSFER','OUT',6,'DEMO',290,'MOV-000290',NOW()-INTERVAL 50 DAY,3,'Seed movement'),
('WAREHOUSE',2,112,'RECEIPT','IN',7,'DEMO',291,'MOV-000291',NOW()-INTERVAL 51 DAY,8,'Seed movement'),
('BRANCH',2,113,'TRANSFER','OUT',8,'DEMO',292,'MOV-000292',NOW()-INTERVAL 52 DAY,3,'Seed movement'),
('BRANCH',3,114,'RECEIPT','IN',9,'DEMO',293,'MOV-000293',NOW()-INTERVAL 53 DAY,3,'Seed movement'),
('WAREHOUSE',1,115,'TRANSFER','OUT',10,'DEMO',294,'MOV-000294',NOW()-INTERVAL 54 DAY,8,'Seed movement'),
('BRANCH',2,116,'RECEIPT','IN',11,'DEMO',295,'MOV-000295',NOW()-INTERVAL 55 DAY,3,'Seed movement'),
('BRANCH',3,117,'TRANSFER','OUT',12,'DEMO',296,'MOV-000296',NOW()-INTERVAL 56 DAY,3,'Seed movement'),
('WAREHOUSE',2,118,'RECEIPT','IN',13,'DEMO',297,'MOV-000297',NOW()-INTERVAL 57 DAY,8,'Seed movement'),
('BRANCH',2,119,'TRANSFER','OUT',14,'DEMO',298,'MOV-000298',NOW()-INTERVAL 58 DAY,3,'Seed movement'),
('BRANCH',3,120,'RECEIPT','IN',15,'DEMO',299,'MOV-000299',NOW()-INTERVAL 59 DAY,3,'Seed movement'),
('WAREHOUSE',1,121,'TRANSFER','OUT',1,'DEMO',300,'MOV-000300',NOW()-INTERVAL 60 DAY,8,'Seed movement');
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(2,(SELECT Username FROM users WHERE UserID=2),'PRODUCT_UPDATE','DEMO',1,'Seeded audit event',NOW()-INTERVAL 4 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(3,(SELECT Username FROM users WHERE UserID=3),'SALE_CREATE','DEMO',2,'Seeded audit event',NOW()-INTERVAL 8 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(4,(SELECT Username FROM users WHERE UserID=4),'INVENTORY_ADJUSTMENT','DEMO',3,'Seeded audit event',NOW()-INTERVAL 12 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(5,(SELECT Username FROM users WHERE UserID=5),'REPORT_EXPORT','DEMO',4,'Seeded audit event',NOW()-INTERVAL 16 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(6,(SELECT Username FROM users WHERE UserID=6),'ORDER_STATUS_CHANGE','DEMO',5,'Seeded audit event',NOW()-INTERVAL 20 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(7,(SELECT Username FROM users WHERE UserID=7),'LOGIN_SUCCESS','DEMO',6,'Seeded audit event',NOW()-INTERVAL 24 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(8,(SELECT Username FROM users WHERE UserID=8),'PRODUCT_UPDATE','DEMO',7,'Seeded audit event',NOW()-INTERVAL 28 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(1,(SELECT Username FROM users WHERE UserID=1),'SALE_CREATE','DEMO',8,'Seeded audit event',NOW()-INTERVAL 32 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(2,(SELECT Username FROM users WHERE UserID=2),'INVENTORY_ADJUSTMENT','DEMO',9,'Seeded audit event',NOW()-INTERVAL 36 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(3,(SELECT Username FROM users WHERE UserID=3),'REPORT_EXPORT','DEMO',10,'Seeded audit event',NOW()-INTERVAL 40 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(4,(SELECT Username FROM users WHERE UserID=4),'ORDER_STATUS_CHANGE','DEMO',11,'Seeded audit event',NOW()-INTERVAL 44 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(5,(SELECT Username FROM users WHERE UserID=5),'LOGIN_SUCCESS','DEMO',12,'Seeded audit event',NOW()-INTERVAL 48 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(6,(SELECT Username FROM users WHERE UserID=6),'PRODUCT_UPDATE','DEMO',13,'Seeded audit event',NOW()-INTERVAL 52 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(7,(SELECT Username FROM users WHERE UserID=7),'SALE_CREATE','DEMO',14,'Seeded audit event',NOW()-INTERVAL 56 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(8,(SELECT Username FROM users WHERE UserID=8),'INVENTORY_ADJUSTMENT','DEMO',15,'Seeded audit event',NOW()-INTERVAL 60 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(1,(SELECT Username FROM users WHERE UserID=1),'REPORT_EXPORT','DEMO',16,'Seeded audit event',NOW()-INTERVAL 64 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(2,(SELECT Username FROM users WHERE UserID=2),'ORDER_STATUS_CHANGE','DEMO',17,'Seeded audit event',NOW()-INTERVAL 68 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(3,(SELECT Username FROM users WHERE UserID=3),'LOGIN_SUCCESS','DEMO',18,'Seeded audit event',NOW()-INTERVAL 72 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(4,(SELECT Username FROM users WHERE UserID=4),'PRODUCT_UPDATE','DEMO',19,'Seeded audit event',NOW()-INTERVAL 76 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(5,(SELECT Username FROM users WHERE UserID=5),'SALE_CREATE','DEMO',20,'Seeded audit event',NOW()-INTERVAL 80 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(6,(SELECT Username FROM users WHERE UserID=6),'INVENTORY_ADJUSTMENT','DEMO',21,'Seeded audit event',NOW()-INTERVAL 84 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(7,(SELECT Username FROM users WHERE UserID=7),'REPORT_EXPORT','DEMO',22,'Seeded audit event',NOW()-INTERVAL 88 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(8,(SELECT Username FROM users WHERE UserID=8),'ORDER_STATUS_CHANGE','DEMO',23,'Seeded audit event',NOW()-INTERVAL 92 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(1,(SELECT Username FROM users WHERE UserID=1),'LOGIN_SUCCESS','DEMO',24,'Seeded audit event',NOW()-INTERVAL 96 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(2,(SELECT Username FROM users WHERE UserID=2),'PRODUCT_UPDATE','DEMO',25,'Seeded audit event',NOW()-INTERVAL 100 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(3,(SELECT Username FROM users WHERE UserID=3),'SALE_CREATE','DEMO',26,'Seeded audit event',NOW()-INTERVAL 104 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(4,(SELECT Username FROM users WHERE UserID=4),'INVENTORY_ADJUSTMENT','DEMO',27,'Seeded audit event',NOW()-INTERVAL 108 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(5,(SELECT Username FROM users WHERE UserID=5),'REPORT_EXPORT','DEMO',28,'Seeded audit event',NOW()-INTERVAL 112 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(6,(SELECT Username FROM users WHERE UserID=6),'ORDER_STATUS_CHANGE','DEMO',29,'Seeded audit event',NOW()-INTERVAL 116 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(7,(SELECT Username FROM users WHERE UserID=7),'LOGIN_SUCCESS','DEMO',30,'Seeded audit event',NOW()-INTERVAL 120 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(8,(SELECT Username FROM users WHERE UserID=8),'PRODUCT_UPDATE','DEMO',31,'Seeded audit event',NOW()-INTERVAL 124 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(1,(SELECT Username FROM users WHERE UserID=1),'SALE_CREATE','DEMO',32,'Seeded audit event',NOW()-INTERVAL 128 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(2,(SELECT Username FROM users WHERE UserID=2),'INVENTORY_ADJUSTMENT','DEMO',33,'Seeded audit event',NOW()-INTERVAL 132 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(3,(SELECT Username FROM users WHERE UserID=3),'REPORT_EXPORT','DEMO',34,'Seeded audit event',NOW()-INTERVAL 136 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(4,(SELECT Username FROM users WHERE UserID=4),'ORDER_STATUS_CHANGE','DEMO',35,'Seeded audit event',NOW()-INTERVAL 140 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(5,(SELECT Username FROM users WHERE UserID=5),'LOGIN_SUCCESS','DEMO',36,'Seeded audit event',NOW()-INTERVAL 144 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(6,(SELECT Username FROM users WHERE UserID=6),'PRODUCT_UPDATE','DEMO',37,'Seeded audit event',NOW()-INTERVAL 148 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(7,(SELECT Username FROM users WHERE UserID=7),'SALE_CREATE','DEMO',38,'Seeded audit event',NOW()-INTERVAL 152 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(8,(SELECT Username FROM users WHERE UserID=8),'INVENTORY_ADJUSTMENT','DEMO',39,'Seeded audit event',NOW()-INTERVAL 156 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(1,(SELECT Username FROM users WHERE UserID=1),'REPORT_EXPORT','DEMO',40,'Seeded audit event',NOW()-INTERVAL 160 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(2,(SELECT Username FROM users WHERE UserID=2),'ORDER_STATUS_CHANGE','DEMO',41,'Seeded audit event',NOW()-INTERVAL 164 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(3,(SELECT Username FROM users WHERE UserID=3),'LOGIN_SUCCESS','DEMO',42,'Seeded audit event',NOW()-INTERVAL 168 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(4,(SELECT Username FROM users WHERE UserID=4),'PRODUCT_UPDATE','DEMO',43,'Seeded audit event',NOW()-INTERVAL 172 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(5,(SELECT Username FROM users WHERE UserID=5),'SALE_CREATE','DEMO',44,'Seeded audit event',NOW()-INTERVAL 176 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(6,(SELECT Username FROM users WHERE UserID=6),'INVENTORY_ADJUSTMENT','DEMO',45,'Seeded audit event',NOW()-INTERVAL 180 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(7,(SELECT Username FROM users WHERE UserID=7),'REPORT_EXPORT','DEMO',46,'Seeded audit event',NOW()-INTERVAL 184 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(8,(SELECT Username FROM users WHERE UserID=8),'ORDER_STATUS_CHANGE','DEMO',47,'Seeded audit event',NOW()-INTERVAL 188 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(1,(SELECT Username FROM users WHERE UserID=1),'LOGIN_SUCCESS','DEMO',48,'Seeded audit event',NOW()-INTERVAL 192 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(2,(SELECT Username FROM users WHERE UserID=2),'PRODUCT_UPDATE','DEMO',49,'Seeded audit event',NOW()-INTERVAL 196 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(3,(SELECT Username FROM users WHERE UserID=3),'SALE_CREATE','DEMO',50,'Seeded audit event',NOW()-INTERVAL 200 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(4,(SELECT Username FROM users WHERE UserID=4),'INVENTORY_ADJUSTMENT','DEMO',51,'Seeded audit event',NOW()-INTERVAL 204 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(5,(SELECT Username FROM users WHERE UserID=5),'REPORT_EXPORT','DEMO',52,'Seeded audit event',NOW()-INTERVAL 208 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(6,(SELECT Username FROM users WHERE UserID=6),'ORDER_STATUS_CHANGE','DEMO',53,'Seeded audit event',NOW()-INTERVAL 212 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(7,(SELECT Username FROM users WHERE UserID=7),'LOGIN_SUCCESS','DEMO',54,'Seeded audit event',NOW()-INTERVAL 216 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(8,(SELECT Username FROM users WHERE UserID=8),'PRODUCT_UPDATE','DEMO',55,'Seeded audit event',NOW()-INTERVAL 220 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(1,(SELECT Username FROM users WHERE UserID=1),'SALE_CREATE','DEMO',56,'Seeded audit event',NOW()-INTERVAL 224 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(2,(SELECT Username FROM users WHERE UserID=2),'INVENTORY_ADJUSTMENT','DEMO',57,'Seeded audit event',NOW()-INTERVAL 228 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(3,(SELECT Username FROM users WHERE UserID=3),'REPORT_EXPORT','DEMO',58,'Seeded audit event',NOW()-INTERVAL 232 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(4,(SELECT Username FROM users WHERE UserID=4),'ORDER_STATUS_CHANGE','DEMO',59,'Seeded audit event',NOW()-INTERVAL 236 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(5,(SELECT Username FROM users WHERE UserID=5),'LOGIN_SUCCESS','DEMO',60,'Seeded audit event',NOW()-INTERVAL 240 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(6,(SELECT Username FROM users WHERE UserID=6),'PRODUCT_UPDATE','DEMO',61,'Seeded audit event',NOW()-INTERVAL 244 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(7,(SELECT Username FROM users WHERE UserID=7),'SALE_CREATE','DEMO',62,'Seeded audit event',NOW()-INTERVAL 248 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(8,(SELECT Username FROM users WHERE UserID=8),'INVENTORY_ADJUSTMENT','DEMO',63,'Seeded audit event',NOW()-INTERVAL 252 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(1,(SELECT Username FROM users WHERE UserID=1),'REPORT_EXPORT','DEMO',64,'Seeded audit event',NOW()-INTERVAL 256 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(2,(SELECT Username FROM users WHERE UserID=2),'ORDER_STATUS_CHANGE','DEMO',65,'Seeded audit event',NOW()-INTERVAL 260 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(3,(SELECT Username FROM users WHERE UserID=3),'LOGIN_SUCCESS','DEMO',66,'Seeded audit event',NOW()-INTERVAL 264 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(4,(SELECT Username FROM users WHERE UserID=4),'PRODUCT_UPDATE','DEMO',67,'Seeded audit event',NOW()-INTERVAL 268 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(5,(SELECT Username FROM users WHERE UserID=5),'SALE_CREATE','DEMO',68,'Seeded audit event',NOW()-INTERVAL 272 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(6,(SELECT Username FROM users WHERE UserID=6),'INVENTORY_ADJUSTMENT','DEMO',69,'Seeded audit event',NOW()-INTERVAL 276 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(7,(SELECT Username FROM users WHERE UserID=7),'REPORT_EXPORT','DEMO',70,'Seeded audit event',NOW()-INTERVAL 280 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(8,(SELECT Username FROM users WHERE UserID=8),'ORDER_STATUS_CHANGE','DEMO',71,'Seeded audit event',NOW()-INTERVAL 284 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(1,(SELECT Username FROM users WHERE UserID=1),'LOGIN_SUCCESS','DEMO',72,'Seeded audit event',NOW()-INTERVAL 288 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(2,(SELECT Username FROM users WHERE UserID=2),'PRODUCT_UPDATE','DEMO',73,'Seeded audit event',NOW()-INTERVAL 292 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(3,(SELECT Username FROM users WHERE UserID=3),'SALE_CREATE','DEMO',74,'Seeded audit event',NOW()-INTERVAL 296 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(4,(SELECT Username FROM users WHERE UserID=4),'INVENTORY_ADJUSTMENT','DEMO',75,'Seeded audit event',NOW()-INTERVAL 300 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(5,(SELECT Username FROM users WHERE UserID=5),'REPORT_EXPORT','DEMO',76,'Seeded audit event',NOW()-INTERVAL 304 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(6,(SELECT Username FROM users WHERE UserID=6),'ORDER_STATUS_CHANGE','DEMO',77,'Seeded audit event',NOW()-INTERVAL 308 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(7,(SELECT Username FROM users WHERE UserID=7),'LOGIN_SUCCESS','DEMO',78,'Seeded audit event',NOW()-INTERVAL 312 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(8,(SELECT Username FROM users WHERE UserID=8),'PRODUCT_UPDATE','DEMO',79,'Seeded audit event',NOW()-INTERVAL 316 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(1,(SELECT Username FROM users WHERE UserID=1),'SALE_CREATE','DEMO',80,'Seeded audit event',NOW()-INTERVAL 320 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(2,(SELECT Username FROM users WHERE UserID=2),'INVENTORY_ADJUSTMENT','DEMO',81,'Seeded audit event',NOW()-INTERVAL 324 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(3,(SELECT Username FROM users WHERE UserID=3),'REPORT_EXPORT','DEMO',82,'Seeded audit event',NOW()-INTERVAL 328 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(4,(SELECT Username FROM users WHERE UserID=4),'ORDER_STATUS_CHANGE','DEMO',83,'Seeded audit event',NOW()-INTERVAL 332 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(5,(SELECT Username FROM users WHERE UserID=5),'LOGIN_SUCCESS','DEMO',84,'Seeded audit event',NOW()-INTERVAL 336 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(6,(SELECT Username FROM users WHERE UserID=6),'PRODUCT_UPDATE','DEMO',85,'Seeded audit event',NOW()-INTERVAL 340 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(7,(SELECT Username FROM users WHERE UserID=7),'SALE_CREATE','DEMO',86,'Seeded audit event',NOW()-INTERVAL 344 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(8,(SELECT Username FROM users WHERE UserID=8),'INVENTORY_ADJUSTMENT','DEMO',87,'Seeded audit event',NOW()-INTERVAL 348 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(1,(SELECT Username FROM users WHERE UserID=1),'REPORT_EXPORT','DEMO',88,'Seeded audit event',NOW()-INTERVAL 352 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(2,(SELECT Username FROM users WHERE UserID=2),'ORDER_STATUS_CHANGE','DEMO',89,'Seeded audit event',NOW()-INTERVAL 356 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(3,(SELECT Username FROM users WHERE UserID=3),'LOGIN_SUCCESS','DEMO',90,'Seeded audit event',NOW()-INTERVAL 360 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(4,(SELECT Username FROM users WHERE UserID=4),'PRODUCT_UPDATE','DEMO',91,'Seeded audit event',NOW()-INTERVAL 364 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(5,(SELECT Username FROM users WHERE UserID=5),'SALE_CREATE','DEMO',92,'Seeded audit event',NOW()-INTERVAL 368 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(6,(SELECT Username FROM users WHERE UserID=6),'INVENTORY_ADJUSTMENT','DEMO',93,'Seeded audit event',NOW()-INTERVAL 372 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(7,(SELECT Username FROM users WHERE UserID=7),'REPORT_EXPORT','DEMO',94,'Seeded audit event',NOW()-INTERVAL 376 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(8,(SELECT Username FROM users WHERE UserID=8),'ORDER_STATUS_CHANGE','DEMO',95,'Seeded audit event',NOW()-INTERVAL 380 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(1,(SELECT Username FROM users WHERE UserID=1),'LOGIN_SUCCESS','DEMO',96,'Seeded audit event',NOW()-INTERVAL 384 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(2,(SELECT Username FROM users WHERE UserID=2),'PRODUCT_UPDATE','DEMO',97,'Seeded audit event',NOW()-INTERVAL 388 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(3,(SELECT Username FROM users WHERE UserID=3),'SALE_CREATE','DEMO',98,'Seeded audit event',NOW()-INTERVAL 392 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(4,(SELECT Username FROM users WHERE UserID=4),'INVENTORY_ADJUSTMENT','DEMO',99,'Seeded audit event',NOW()-INTERVAL 396 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(5,(SELECT Username FROM users WHERE UserID=5),'REPORT_EXPORT','DEMO',100,'Seeded audit event',NOW()-INTERVAL 400 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(6,(SELECT Username FROM users WHERE UserID=6),'ORDER_STATUS_CHANGE','DEMO',101,'Seeded audit event',NOW()-INTERVAL 404 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(7,(SELECT Username FROM users WHERE UserID=7),'LOGIN_SUCCESS','DEMO',102,'Seeded audit event',NOW()-INTERVAL 408 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(8,(SELECT Username FROM users WHERE UserID=8),'PRODUCT_UPDATE','DEMO',103,'Seeded audit event',NOW()-INTERVAL 412 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(1,(SELECT Username FROM users WHERE UserID=1),'SALE_CREATE','DEMO',104,'Seeded audit event',NOW()-INTERVAL 416 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(2,(SELECT Username FROM users WHERE UserID=2),'INVENTORY_ADJUSTMENT','DEMO',105,'Seeded audit event',NOW()-INTERVAL 420 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(3,(SELECT Username FROM users WHERE UserID=3),'REPORT_EXPORT','DEMO',106,'Seeded audit event',NOW()-INTERVAL 424 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(4,(SELECT Username FROM users WHERE UserID=4),'ORDER_STATUS_CHANGE','DEMO',107,'Seeded audit event',NOW()-INTERVAL 428 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(5,(SELECT Username FROM users WHERE UserID=5),'LOGIN_SUCCESS','DEMO',108,'Seeded audit event',NOW()-INTERVAL 432 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(6,(SELECT Username FROM users WHERE UserID=6),'PRODUCT_UPDATE','DEMO',109,'Seeded audit event',NOW()-INTERVAL 436 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(7,(SELECT Username FROM users WHERE UserID=7),'SALE_CREATE','DEMO',110,'Seeded audit event',NOW()-INTERVAL 440 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(8,(SELECT Username FROM users WHERE UserID=8),'INVENTORY_ADJUSTMENT','DEMO',111,'Seeded audit event',NOW()-INTERVAL 444 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(1,(SELECT Username FROM users WHERE UserID=1),'REPORT_EXPORT','DEMO',112,'Seeded audit event',NOW()-INTERVAL 448 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(2,(SELECT Username FROM users WHERE UserID=2),'ORDER_STATUS_CHANGE','DEMO',113,'Seeded audit event',NOW()-INTERVAL 452 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(3,(SELECT Username FROM users WHERE UserID=3),'LOGIN_SUCCESS','DEMO',114,'Seeded audit event',NOW()-INTERVAL 456 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(4,(SELECT Username FROM users WHERE UserID=4),'PRODUCT_UPDATE','DEMO',115,'Seeded audit event',NOW()-INTERVAL 460 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(5,(SELECT Username FROM users WHERE UserID=5),'SALE_CREATE','DEMO',116,'Seeded audit event',NOW()-INTERVAL 464 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(6,(SELECT Username FROM users WHERE UserID=6),'INVENTORY_ADJUSTMENT','DEMO',117,'Seeded audit event',NOW()-INTERVAL 468 HOUR,1,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(7,(SELECT Username FROM users WHERE UserID=7),'REPORT_EXPORT','DEMO',118,'Seeded audit event',NOW()-INTERVAL 472 HOUR,2,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(8,(SELECT Username FROM users WHERE UserID=8),'ORDER_STATUS_CHANGE','DEMO',119,'Seeded audit event',NOW()-INTERVAL 476 HOUR,3,1);
INSERT INTO audit_logs(UserID,Username,ActionCode,EntityType,EntityID,Description,ActionAt,BranchID,Success) VALUES(1,(SELECT Username FROM users WHERE UserID=1),'LOGIN_SUCCESS','DEMO',120,'Seeded audit event',NOW()-INTERVAL 480 HOUR,1,1);

SET FOREIGN_KEY_CHECKS=1;
