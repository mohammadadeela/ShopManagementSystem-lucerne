import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.print.PrinterJob;
import javafx.scene.Cursor;
import javafx.scene.effect.DropShadow;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.FileWriter;
import java.sql.*;
import java.util.*;
import java.time.LocalDate;

public class MainFX extends Application {

    private TableView<String[]> table = new TableView<>();
    private VBox contentBox;
    private Label pageTitle;
    private int currentUserId;
    private String currentRole;

    private final ArrayList<String> selectedColors = new ArrayList<>();
    private final ArrayList<String> selectedSizes = new ArrayList<>();
    private String selectedSort = "Recommend";
    private String currentCategory = null;
    private String currentKeyword = null;
    private final ArrayList<CartItem> customerCart = new ArrayList<>();


    @Override
    public void start(Stage stage) {
        openRoleSelection(stage);
    }

    private void openRoleSelection(Stage stage) {
        Label title = new Label("Lucerne Boutique");
        title.setStyle("-fx-font-size:36px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");

        Label subtitle = new Label("Choose Account Type");
        subtitle.setStyle("-fx-font-size:18px; -fx-text-fill:#7A5C52;");

        Button ownerBtn = createSmallButton("Owner");
        Button managerBtn = createSmallButton("Branch Manager");
        Button cashierBtn = createSmallButton("Cashier");
        Button warehouseBtn = createSmallButton("Warehouse Manager");
        Button customerBtn = createSmallButton("Customer");

        ownerBtn.setOnAction(e -> openLogin(stage, "OWNER"));
        managerBtn.setOnAction(e -> openLogin(stage, "MANAGER"));
        cashierBtn.setOnAction(e -> openLogin(stage, "CASHIER"));
        warehouseBtn.setOnAction(e -> openLogin(stage, "WAREHOUSE"));
        customerBtn.setOnAction(e -> openLogin(stage, "CUSTOMER"));

        VBox card = new VBox(18, title, subtitle, ownerBtn, managerBtn, cashierBtn, warehouseBtn, customerBtn);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setMaxWidth(520);
        card.setStyle("-fx-background-color:white; -fx-background-radius:30;");

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #F7E7E0, #E7CFC4, #D8B4A0);");

        stage.setScene(new Scene(root, 950, 680));
        stage.setTitle("Choose Account Type");
        stage.show();
    }

    private void openLogin(Stage stage, String selectedRole) {
        Label title = new Label(selectedRole + " Login");
        title.setStyle("-fx-font-size:30px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        usernameField.setMaxWidth(320);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(320);

        Label message = new Label();
        message.setStyle("-fx-text-fill:#8B0000;");

        Button loginBtn = createSmallButton("Login");
        loginBtn.setOnAction(e -> login(stage, usernameField.getText(), passwordField.getText(), selectedRole, message));

        Button backBtn = createSmallButton("Back");
        backBtn.setOnAction(e -> openRoleSelection(stage));

        VBox card = new VBox(18, title, usernameField, passwordField, loginBtn, backBtn, message);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(45));
        card.setMaxWidth(560);
        card.setStyle("-fx-background-color:white; -fx-background-radius:30;");

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #F7E7E0, #E7CFC4, #D8B4A0);");

        stage.setScene(new Scene(root, 950, 680));
        stage.setTitle("Login");
        stage.show();
    }

    private void login(Stage stage, String username, String password, String role, Label message) {
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            if (con == null) {
                message.setText("Database connection failed. Check DataBaseConnection.java and MySQL Connector.");
                return;
            }

            PreparedStatement ps = con.prepareStatement(
                    "SELECT * FROM users WHERE LOWER(Username)=LOWER(?) AND LOWER(Password)=LOWER(?) AND Role=?"
            );
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                openDashboard(stage, rs.getInt("UserID"), rs.getString("FullName"), rs.getString("Role"));
            } else {
                message.setText("Wrong username, password, or selected role");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            message.setText(ex.getMessage());
        }
    }

    private void openDashboard(Stage stage, int userId, String fullName, String role) {
        ensureMegaTablesExist();
        ensureFullUpgradeTablesExist();
        ensureTableUpgradesExist();
        currentUserId = userId;
        currentRole = role;
        ensureTop5TablesExist();
        ensureMore15TablesExist();
        table = new TableView<>();

        pageTitle = new Label("Welcome, " + fullName + " (" + role + ")");
        pageTitle.setStyle("-fx-font-size:24px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");

        TextField searchField = new TextField();
        searchField.setPromptText("Search by product, category, color, branch...");
        searchField.setPrefWidth(360);

        Button searchBtn = createSmallButton("Search");
        searchBtn.setOnAction(e -> showGallerySearch(searchField.getText(), "Search Results"));

        Button showAllBtn = createSmallButton("Show All");
        showAllBtn.setOnAction(e -> showAllGallery("All Products"));

        Button filterBtn = createSmallButton("Filter");
        filterBtn.setOnAction(e -> openFilterDialog());

        HBox searchBox = new HBox(10, searchField, searchBtn, showAllBtn, filterBtn);
        searchBox.setAlignment(Pos.CENTER_LEFT);

        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(240);
        sidebar.setStyle("-fx-background-color:#5A3E36;");

        String[] buttons = buttonsForRole(role);

        for (String text : buttons) {
            Button btn = createSideButton(text);
            btn.setOnAction(e -> handleAction(text, userId));
            sidebar.getChildren().add(btn);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logout = createSideButton("Sign Out");
        logout.setOnAction(e -> openRoleSelection(stage));
        sidebar.getChildren().addAll(spacer, logout);

        contentBox = new VBox(15, pageTitle, searchBox, table);
        contentBox.setPadding(new Insets(20));
        contentBox.setStyle("-fx-background-color:#FFF7F2;");
        VBox.setVgrow(table, Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.setLeft(sidebar);
        root.setCenter(contentBox);

        stage.setScene(new Scene(root, 1250, 760));
        stage.setTitle("Lucerne Boutique Dashboard");
        stage.show();

        if (role.equals("CUSTOMER")) {
            showCustomerStorePage(userId);
        } else {
            showAllGallery("All Products");
        }
    }

    private String[] buttonsForRole(String role) {
        if (role.equals("OWNER")) {
            return new String[]{
                    "Manage Users", "Manage Products",
                    "Supplier Orders Plus", "Search Export Center", "Birthday Discounts", "Loyalty Points", "Attendance", "Expenses", "Activity Log", "Permissions", "All Products", "Low Stock", "Online Orders", "Order Details", "Order Timeline", "Delivery Management", "Pickup Orders", "Returns Requests", "Delivery Areas", "Analytics Plus", "Notifications", "Wishlist / Reviews", "Advanced Coupons", "Stock History", "Backup Database", "Barcode / QR", "Blouses", "Dresses", "Pants", "Shoes", "Abayas",
                    "Branch Inventory", "Warehouse Inventory", "All Sales", "Sales By Branch",
                    "Monthly Net Profit", "Employees Salaries", "Customers", "Discounts"
            };
        }

        if (role.equals("MANAGER")) {
            return new String[]{
                    "Manager Dashboard", "Branch Sales Today", "Branch Low Stock", "Branch Cashiers",
                    "Branch Online Orders", "Create Stock Request", "My Stock Requests",
                    "Branch Inventory", "Blouses", "Dresses", "Pants", "Shoes", "Abayas", "Stock Requests", "Change Password", "Profile", "Notifications Center", "Search Export Center"
            };
        }

        if (role.equals("CASHIER")) {
            return new String[]{
                    "Cashier POS", "My Branch Products", "Create Sale", "Create Customer",
                    "Cash Advance", "My Today Sales", "My Cash Summary", "Daily Closing",
                    "Return Sale", "Print Last Receipt", "Change Password", "Profile", "Notifications Center"
            };
        }

        if (role.equals("WAREHOUSE")) {
            return new String[]{
                    "Warehouse Dashboard", "Warehouse Inventory", "Pending Stock Requests",
                    "Send Stock to Branch", "Receive Purchase", "Damaged Items",
                    "Stock Count", "Warehouse Movements", "Stock Requests", "Change Password", "Profile", "Notifications Center", "Supplier Orders Plus"
            };
        }

        return new String[]{
                "Online Store", "Cart", "My Online Orders", "My Orders Cards", "Order Timeline", "My Addresses", "My Favorites", "Return / Exchange", "Product Reviews", "Notifications", "Discounts"
        };
    }

    private void handleAction(String text, int userId) {

        if (text.equals("Manage Users")) {
            showManageUsersPage();
            return;
        }

        if (text.equals("Manage Products")) {
            showManageProductsPage();
            return;
        }

        if (text.equals("Permissions")) {
            showPermissionsPage();
            return;
        }

        if (text.equals("Activity Log")) {
            showActivityLogPage();
            return;
        }

        if (text.equals("Expenses")) {
            showExpensesPage();
            return;
        }

        if (text.equals("Attendance")) {
            showAttendancePage(userId);
            return;
        }

        if (text.equals("Loyalty Points")) {
            showLoyaltyPointsPage(userId);
            return;
        }

        if (text.equals("Birthday Discounts")) {
            showBirthdayDiscountsPage(userId);
            return;
        }

        if (text.equals("Search Export Center")) {
            showSearchExportCenter();
            return;
        }

        if (text.equals("Supplier Orders Plus")) {
            showSupplierOrdersPlusPage();
            return;
        }

        if (text.equals("Change Password")) {
            openChangePasswordWindow(userId);
            return;
        }

        if (text.equals("Profile")) {
            showProfilePage(userId);
            return;
        }

        if (text.equals("Notifications Center")) {
            showNotificationsCenter(userId);
            return;
        }



        if (text.equals("Manager Dashboard")) {
            showManagerDashboard(userId);
            return;
        }

        if (text.equals("Branch Sales Today")) {
            showBranchSalesToday(userId);
            return;
        }

        if (text.equals("Branch Low Stock")) {
            showBranchLowStock(userId);
            return;
        }

        if (text.equals("Branch Cashiers")) {
            showBranchCashiers(userId);
            return;
        }

        if (text.equals("Branch Online Orders")) {
            showBranchOnlineOrders(userId);
            return;
        }

        if (text.equals("Create Stock Request")) {
            openCreateStockRequestWindow(userId);
            return;
        }

        if (text.equals("My Stock Requests")) {
            showMyStockRequests(userId);
            return;
        }

        if (text.equals("Cashier POS")) {
            showCashierPOS(userId);
            return;
        }

        if (text.equals("Daily Closing")) {
            showDailyClosingPage(userId);
            return;
        }

        if (text.equals("Return Sale")) {
            openReturnSaleWindow(userId);
            return;
        }

        if (text.equals("Print Last Receipt")) {
            printLastCashierReceipt(userId);
            return;
        }

        if (text.equals("Warehouse Dashboard")) {
            showWarehouseDashboard(userId);
            return;
        }

        if (text.equals("Pending Stock Requests")) {
            showPendingStockRequests();
            return;
        }

        if (text.equals("Send Stock to Branch")) {
            openSendStockToBranchWindow(userId);
            return;
        }

        if (text.equals("Receive Purchase")) {
            openReceivePurchaseWindow(userId);
            return;
        }

        if (text.equals("Damaged Items")) {
            openDamagedItemsWindow(userId);
            return;
        }

        if (text.equals("Stock Count")) {
            openStockCountWindow(userId);
            return;
        }



        if (text.equals("Add / Edit Product")) {
            showManageProductsPage();
            return;
        }

        if (text.equals("Low Stock")) {
            showLowStockPage();
            return;
        }

        if (text.equals("My Orders Cards")) {
            showCustomerOrdersCards(userId);
            return;
        }



        if (text.equals("Suppliers")) {
            showSuppliersPage();
            return;
        }

        if (text.equals("Purchase Orders")) {
            showPurchaseOrdersPage();
            return;
        }

        if (text.equals("Drivers")) {
            showDriversPage();
            return;
        }

        if (text.equals("Payments")) {
            showPaymentsPage();
            return;
        }

        if (text.equals("Product Images")) {
            showProductImagesPage();
            return;
        }

        if (text.equals("Product Variants")) {
            showProductVariantsPage();
            return;
        }

        if (text.equals("Table Manager")) {
            showTableManagerPage();
            return;
        }



        if (text.equals("Cart")) {
            showCartPage(userId);
            return;
        }

        if (text.equals("Order Details")) {
            showOrderDetailsPage();
            return;
        }

        if (text.equals("Order Timeline")) {
            showOrderTimelinePage(userId);
            return;
        }

        if (text.equals("My Addresses")) {
            showAddressBookPage(userId);
            return;
        }

        if (text.equals("Return / Exchange")) {
            showReturnExchangeRequestPage(userId);
            return;
        }

        if (text.equals("Returns Requests")) {
            showAdminReturnsRequests();
            return;
        }

        if (text.equals("Delivery Areas")) {
            showDeliveryAreasPage();
            return;
        }

        if (text.equals("Analytics Plus")) {
            showAnalyticsPlusPage();
            return;
        }



        if (text.equals("Delivery Management")) {
            showDeliveryManagement();
            return;
        }

        if (text.equals("Pickup Orders")) {
            showPickupOrders();
            return;
        }

        if (text.equals("Notifications")) {
            showNotificationsPage(userId);
            return;
        }

        if (text.equals("Wishlist / Reviews")) {
            showAdminWishlistReviews();
            return;
        }

        if (text.equals("Advanced Coupons")) {
            showAdvancedCouponsPage();
            return;
        }

        if (text.equals("Stock History")) {
            showStockHistoryPage();
            return;
        }

        if (text.equals("Backup Database")) {
            backupDatabaseCSV();
            return;
        }

        if (text.equals("Barcode / QR")) {
            showBarcodeQRPage();
            return;
        }

        if (text.equals("My Favorites")) {
            showMyFavorites(userId);
            return;
        }

        if (text.equals("Product Reviews")) {
            showProductReviewsPage(userId);
            return;
        }


        if (text.equals("Online Store") || text.equals("Shop Online")) {
            showCustomerStorePage(userId);
            return;
        }

        if (text.equals("My Online Orders")) {
            showCustomerOnlineOrders(userId);
            return;
        }

        if (text.equals("Online Orders")) {
            showAdminOnlineOrders();
            return;
        }

        if (text.equals("All Products") || text.equals("Available Products")) {
            showAllGallery("All Products");
            return;
        }

        if (text.equals("Blouses") || text.equals("Dresses") || text.equals("Pants") || text.equals("Shoes") || text.equals("Abayas")) {
            showGallery(text, text);
            return;
        }

        if (text.equals("Branch Inventory") || text.equals("My Branch Products")) {
            showAllGallery(text);
            return;
        }

        if (text.equals("Create Sale")) {
            openCreateSaleWindow(userId);
            return;
        }

        if (text.equals("Create Customer")) {
            openCreateCustomerWindow();
            return;
        }

        if (text.equals("Cash Advance")) {
            openCashAdvanceWindow(userId);
            return;
        }

        if (text.contains("Gallery")) {
            String category = text.replace(" Gallery", "");
            showGallery(category, text);
            return;
        }

        if (text.equals("Customers")) {
            showTable(moduleSQL("Customers"));
            pageTitle.setText("Customers Information");
            return;
        }

        if (text.equals("My Today Sales")) {
            showTable("SELECT SaleID, SaleDate, TotalAmount, DiscountAmount, FinalAmount FROM sales WHERE CashierUserID=" + userId + " AND SaleDate=CURDATE()");
            return;
        }

        if (text.equals("My Cash Summary")) {
            showTable("SELECT MovementType, SUM(Amount) AS TotalAmount FROM cash_drawer_movements WHERE CashierUserID=" + userId + " AND DATE(MovementDate)=CURDATE() GROUP BY MovementType");
            return;
        }

        showTable(moduleSQL(text));
    }

    private String moduleSQL(String title) {
        if (title.equals("All Products") || title.equals("Available Products")) return productsSQL(null);
        if (title.equals("Blouses")) return productsSQL("Blouses");
        if (title.equals("Dresses")) return productsSQL("Dresses");
        if (title.equals("Pants")) return productsSQL("Pants");
        if (title.equals("Shoes")) return productsSQL("Shoes");
        if (title.equals("Abayas")) return productsSQL("Abayas");

        if (title.equals("Branch Inventory")) return productsSQL(null);

        if (title.equals("Warehouse Inventory")) {
            return "SELECT w.Name AS Warehouse, p.ProductID, p.ImagePath AS Product, p.Name AS ProductName, p.Category, p.Color, p.Price, p.CostPrice, " +
                    "ps.SizeValue AS Size, wi.Quantity AS RemainingQuantity " +
                    "FROM warehouse_inventory wi " +
                    "JOIN warehouses w ON wi.WarehouseID=w.WarehouseID " +
                    "JOIN products p ON wi.ProductID=p.ProductID " +
                    "JOIN product_sizes ps ON wi.SizeID=ps.SizeID " +
                    "ORDER BY p.Category, p.Name, p.Color, ps.SizeValue";
        }

        if (title.equals("All Sales")) {
            return "SELECT s.SaleID, b.Name AS Branch, u.FullName AS Cashier, s.SaleDate, s.TotalAmount, s.DiscountAmount, s.FinalAmount " +
                    "FROM sales s JOIN branches b ON s.BranchID=b.BranchID JOIN users u ON s.CashierUserID=u.UserID";
        }

        if (title.equals("Sales By Branch")) {
            return "SELECT b.Name AS Branch, COUNT(s.SaleID) AS SalesCount, IFNULL(SUM(s.FinalAmount),0) AS TotalSales " +
                    "FROM branches b LEFT JOIN sales s ON b.BranchID=s.BranchID GROUP BY b.Name";
        }

        if (title.equals("Monthly Net Profit")) {
            return "SELECT sales_total.TotalSales, product_cost.TotalCost, expenses_total.TotalExpenses, " +
                    "(sales_total.TotalSales - product_cost.TotalCost - expenses_total.TotalExpenses) AS MonthlyNetProfit " +
                    "FROM " +
                    "(SELECT IFNULL(SUM(FinalAmount),0) AS TotalSales FROM sales WHERE MONTH(SaleDate)=MONTH(CURDATE()) AND YEAR(SaleDate)=YEAR(CURDATE())) sales_total, " +
                    "(SELECT IFNULL(SUM(si.Quantity*p.CostPrice),0) AS TotalCost FROM sale_items si JOIN products p ON si.ProductID=p.ProductID JOIN sales s ON si.SaleID=s.SaleID WHERE MONTH(s.SaleDate)=MONTH(CURDATE()) AND YEAR(s.SaleDate)=YEAR(CURDATE())) product_cost, " +
                    "(SELECT IFNULL(SUM(Amount),0) AS TotalExpenses FROM expenses WHERE MONTH(ExpenseDate)=MONTH(CURDATE()) AND YEAR(ExpenseDate)=YEAR(CURDATE())) expenses_total";
        }

        if (title.equals("Employees Salaries")) {
            return "SELECT u.FullName, u.Username, u.Role, e.JobTitle, e.Salary FROM employees e JOIN users u ON e.UserID=u.UserID";
        }

        if (title.equals("Customers")) {
            return "SELECT " +
                    "c.CustomerID, " +
                    "u.FullName, " +
                    "u.Username, " +
                    "u.Password, " +
                    "u.Role, " +
                    "IFNULL(c.Phone, '-') AS Phone, " +
                    "IFNULL(c.Email, '-') AS Email, " +
                    "COUNT(DISTINCT s.SaleID) AS TotalInvoices, " +
                    "IFNULL(SUM(s.FinalAmount), 0) AS TotalSpent, " +
                    "IFNULL(MAX(s.SaleDate), '-') AS LastPurchaseDate " +
                    "FROM customers c " +
                    "JOIN users u ON c.UserID = u.UserID " +
                    "LEFT JOIN sales s ON c.CustomerID = s.CustomerID " +
                    "GROUP BY c.CustomerID, u.FullName, u.Username, u.Password, u.Role, c.Phone, c.Email " +
                    "ORDER BY u.FullName";
        }

        if (title.equals("Discounts")) {
            return "SELECT Code, Percentage, StartDate, EndDate FROM discounts";
        }

        if (title.equals("Stock Requests")) {
            return "SELECT sr.RequestID, b.Name AS Branch, p.Name AS Product, p.Color, ps.SizeValue AS Size, sr.RequestedQuantity, sr.RequestDate, sr.Status " +
                    "FROM stock_requests sr JOIN branches b ON sr.BranchID=b.BranchID " +
                    "JOIN products p ON sr.ProductID=p.ProductID JOIN product_sizes ps ON sr.SizeID=ps.SizeID";
        }

        if (title.equals("Warehouse Movements")) {
            return "SELECT wm.MovementID, w.Name AS Warehouse, p.Name AS Product, p.Color, ps.SizeValue AS Size, wm.MovementType, wm.Quantity, wm.MovementDate " +
                    "FROM warehouse_movements wm JOIN warehouses w ON wm.WarehouseID=w.WarehouseID " +
                    "JOIN products p ON wm.ProductID=p.ProductID JOIN product_sizes ps ON wm.SizeID=ps.SizeID";
        }

        return productsSQL(null);
    }

    private String productsSQL(String category) {
        String sql = "SELECT b.Name AS Branch, " +
                "p.ProductID, " +
                "p.ImagePath AS Product, " +
                "p.Name AS ProductName, " +
                "p.Category, " +
                "p.Color, " +
                "p.Price, " +
                "p.CostPrice, " +
                "ps.SizeValue AS Size, " +
                "bi.Quantity AS RemainingQuantity " +
                "FROM branch_inventory bi " +
                "JOIN branches b ON bi.BranchID=b.BranchID " +
                "JOIN products p ON bi.ProductID=p.ProductID " +
                "JOIN product_sizes ps ON bi.SizeID=ps.SizeID ";

        if (category != null) {
            sql += "WHERE p.Category='" + category + "' ";
        }

        sql += "ORDER BY p.Category, p.Name, p.Color, ps.SizeValue";
        return sql;
    }

    private String searchSQL(String keyword) {
        keyword = keyword == null ? "" : keyword.replace("'", "''");

        return "SELECT b.Name AS Branch, " +
                "p.ProductID, " +
                "p.ImagePath AS Product, " +
                "p.Name AS ProductName, " +
                "p.Category, " +
                "p.Color, " +
                "p.Price, " +
                "p.CostPrice, " +
                "ps.SizeValue AS Size, " +
                "bi.Quantity AS RemainingQuantity " +
                "FROM branch_inventory bi " +
                "JOIN branches b ON bi.BranchID=b.BranchID " +
                "JOIN products p ON bi.ProductID=p.ProductID " +
                "JOIN product_sizes ps ON bi.SizeID=ps.SizeID " +
                "WHERE p.Name LIKE '%" + keyword + "%' " +
                "OR p.Category LIKE '%" + keyword + "%' " +
                "OR p.Color LIKE '%" + keyword + "%' " +
                "OR b.Name LIKE '%" + keyword + "%' " +
                "ORDER BY p.Category, p.Name, p.Color, ps.SizeValue";
    }

    private String cashierBranchProductsSQL(int cashierUserId) {
        return "SELECT b.Name AS Branch, " +
                "p.ProductID, " +
                "p.ImagePath AS Product, " +
                "p.Name AS ProductName, " +
                "p.Category, " +
                "p.Color, " +
                "p.Price, " +
                "p.CostPrice, " +
                "ps.SizeValue AS Size, " +
                "bi.Quantity AS RemainingQuantity " +
                "FROM employees e " +
                "JOIN branch_inventory bi ON e.BranchID=bi.BranchID " +
                "JOIN branches b ON bi.BranchID=b.BranchID " +
                "JOIN products p ON bi.ProductID=p.ProductID " +
                "JOIN product_sizes ps ON bi.SizeID=ps.SizeID " +
                "WHERE e.UserID=" + cashierUserId + " " +
                "ORDER BY p.Category, p.Name, p.Color, ps.SizeValue";
    }


    private void showGallerySearch(String keyword, String titleText) {
        showGalleryInternal(null, keyword, titleText);
    }

    private void showGallery(String category, String titleText) {
        // Category navigation keeps the active filter selections.
        showGalleryInternal(category, null, titleText);
    }

    private void showAllGallery(String titleText) {
        clearGalleryFilters();
        showGalleryInternal(null, null, titleText);
    }

    private void clearGalleryFilters() {
        selectedColors.clear();
        selectedSizes.clear();
        selectedSort = "Recommend";
        currentCategory = null;
        currentKeyword = null;
    }

    private void showGalleryInternal(String category, String keyword, String titleText) {
        pageTitle.setText(titleText);
        currentCategory = category;
        currentKeyword = keyword;

        TilePane tilePane = new TilePane();
        tilePane.setPadding(new Insets(22));
        tilePane.setHgap(22);
        tilePane.setVgap(22);
        tilePane.setPrefColumns(3);
        tilePane.setPrefTileWidth(300);
        tilePane.setAlignment(Pos.TOP_LEFT);
        tilePane.setStyle("-fx-background-color:#FFF7F2;");

        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            if (con == null) {
                throw new SQLException("Database connection failed.");
            }

            String sql =
                    "SELECT p.ProductID, p.Name, p.Category, p.Color, p.Price, p.ImagePath, " +
                            "COALESCE(sd.Sizes, 'No sizes') AS Sizes, " +
                            "COALESCE(sd.SizeQuantities, '') AS SizeQuantities, " +
                            "COALESCE(st.RemainingQuantity, 0) AS RemainingQuantity " +
                            "FROM products p " +
                            "LEFT JOIN ( " +
                            "   SELECT ps.ProductID, " +
                            "          GROUP_CONCAT(DISTINCT ps.SizeValue " +
                            "              ORDER BY CASE ps.SizeValue " +
                            "                  WHEN 'XXS' THEN 1 WHEN 'XS' THEN 2 WHEN 'S' THEN 3 " +
                            "                  WHEN 'M' THEN 4 WHEN 'L' THEN 5 WHEN 'XL' THEN 6 " +
                            "                  WHEN 'XXL' THEN 7 ELSE 8 END, ps.SizeValue " +
                            "              SEPARATOR ', ') AS Sizes, " +
                            "          GROUP_CONCAT(CONCAT(ps.SizeValue, ':', COALESCE(iq.SizeQty, 0)) " +
                            "              ORDER BY CASE ps.SizeValue " +
                            "                  WHEN 'XXS' THEN 1 WHEN 'XS' THEN 2 WHEN 'S' THEN 3 " +
                            "                  WHEN 'M' THEN 4 WHEN 'L' THEN 5 WHEN 'XL' THEN 6 " +
                            "                  WHEN 'XXL' THEN 7 ELSE 8 END, ps.SizeValue " +
                            "              SEPARATOR ', ') AS SizeQuantities " +
                            "   FROM product_sizes ps " +
                            "   LEFT JOIN ( " +
                            "       SELECT ProductID, SizeID, SUM(Quantity) AS SizeQty " +
                            "       FROM branch_inventory " +
                            "       GROUP BY ProductID, SizeID " +
                            "   ) iq ON iq.ProductID=ps.ProductID AND iq.SizeID=ps.SizeID " +
                            "   GROUP BY ps.ProductID " +
                            ") sd ON sd.ProductID=p.ProductID " +
                            "LEFT JOIN ( " +
                            "   SELECT ProductID, SUM(Quantity) AS RemainingQuantity " +
                            "   FROM branch_inventory " +
                            "   GROUP BY ProductID " +
                            ") st ON st.ProductID=p.ProductID " +
                            "WHERE (p.IsActive=1 OR p.IsActive IS NULL) ";

            ArrayList<Object> params = new ArrayList<>();

            if (category != null && !category.trim().isEmpty()) {
                sql += "AND p.Category=? ";
                params.add(category.trim());
            }

            if (!selectedColors.isEmpty()) {
                sql += "AND p.Color IN (" + placeholders(selectedColors.size()) + ") ";
                params.addAll(selectedColors);
            }

            if (!selectedSizes.isEmpty()) {
                sql += "AND EXISTS (SELECT 1 FROM product_sizes psf " +
                        "WHERE psf.ProductID=p.ProductID " +
                        "AND psf.SizeValue IN (" + placeholders(selectedSizes.size()) + ")) ";
                params.addAll(selectedSizes);
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                sql += "AND (p.Name LIKE ? OR p.Category LIKE ? OR p.Color LIKE ?) ";
                String searchText = "%" + keyword.trim() + "%";
                params.add(searchText);
                params.add(searchText);
                params.add(searchText);
            }

            if ("Price Low to High".equals(selectedSort)) {
                sql += "ORDER BY p.Price ASC, p.ProductID ASC";
            } else if ("Price High to Low".equals(selectedSort)) {
                sql += "ORDER BY p.Price DESC, p.ProductID ASC";
            } else if ("New Arrivals".equals(selectedSort)) {
                sql += "ORDER BY p.ProductID DESC";
            } else {
                sql += "ORDER BY p.Category, p.Name, p.Color, p.ProductID";
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }

                try (ResultSet rs = ps.executeQuery()) {
                    ArrayList<ProductVariant> loadedVariants = new ArrayList<>();

                    while (rs.next()) {
                        loadedVariants.add(new ProductVariant(
                                rs.getInt("ProductID"),
                                cleanProductNameJava(
                                        rs.getString("Name"),
                                        rs.getString("Color")
                                ),
                                rs.getString("Name"),
                                rs.getString("Category"),
                                rs.getString("Color"),
                                rs.getDouble("Price"),
                                rs.getString("Sizes"),
                                rs.getInt("RemainingQuantity"),
                                rs.getString("ImagePath"),
                                parseSizeQuantities(rs.getString("SizeQuantities"))
                        ));
                    }

                    LinkedHashMap<String, ArrayList<ProductVariant>> grouped =
                            groupVariantsByBase(loadedVariants);

                    int displayedProducts = 0;
                    for (ArrayList<ProductVariant> variants : grouped.values()) {
                        tilePane.getChildren().add(createGroupedProductCard(variants));
                        displayedProducts++;
                    }

                    if (displayedProducts == 0) {
                        Label empty = new Label("No products found");
                        empty.setStyle(
                                "-fx-font-size:20px;" +
                                        "-fx-font-weight:bold;" +
                                        "-fx-text-fill:#5A3E36;" +
                                        "-fx-padding:35;"
                        );
                        tilePane.getChildren().add(empty);
                    }
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            Label error = new Label("Unable to load products: " + ex.getMessage());
            error.setWrapText(true);
            error.setStyle("-fx-text-fill:#9B1C1C; -fx-font-size:16px; -fx-padding:20;");
            tilePane.getChildren().add(error);
        }

        ScrollPane scrollPane = new ScrollPane(tilePane);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background:#FFF7F2; -fx-background-color:#FFF7F2;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        if (contentBox.getChildren().size() > 2) {
            contentBox.getChildren().set(2, scrollPane);
        } else {
            contentBox.getChildren().add(scrollPane);
        }
    }

    private LinkedHashMap<String, ArrayList<ProductVariant>> groupVariantsByBase(
            ArrayList<ProductVariant> variants) {

        LinkedHashMap<String, ArrayList<ProductVariant>> grouped = new LinkedHashMap<>();

        for (ProductVariant variant : variants) {
            String category = variant.category == null
                    ? ""
                    : variant.category.trim().toLowerCase();
            String base = variant.baseName == null
                    ? variant.fullName.trim().toLowerCase()
                    : variant.baseName.trim().toLowerCase();

            String key = category + "|" + base;
            ArrayList<ProductVariant> group = grouped.computeIfAbsent(
                    key,
                    ignored -> new ArrayList<>()
            );

            boolean duplicateColor = false;
            for (ProductVariant existing : group) {
                if (Objects.equals(normalizeColor(existing.color), normalizeColor(variant.color))) {
                    duplicateColor = true;
                    break;
                }
            }

            if (!duplicateColor) {
                group.add(variant);
            }
        }

        return grouped;
    }

    private String normalizeColor(String color) {
        return color == null ? "" : color.trim().toLowerCase();
    }

    private VBox createGroupedProductCard(ArrayList<ProductVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return new VBox();
        }

        ProductVariant[] selected = new ProductVariant[]{variants.get(0)};

        ImageView imageView = createCleanProductImageView(selected[0].imagePath);
        imageView.setFitWidth(260);
        imageView.setFitHeight(305);

        Label missingImageLabel = new Label("Image not available");
        missingImageLabel.setWrapText(true);
        missingImageLabel.setAlignment(Pos.CENTER);
        missingImageLabel.setStyle(
                "-fx-font-size:16px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-text-fill:#9A7468;"
        );
        missingImageLabel.setVisible(imageView.getImage() == null);
        missingImageLabel.setManaged(imageView.getImage() == null);

        StackPane imageFrame = new StackPane(imageView, missingImageLabel);
        imageFrame.setMinSize(270, 315);
        imageFrame.setPrefSize(270, 315);
        imageFrame.setMaxSize(270, 315);
        imageFrame.setAlignment(Pos.CENTER);
        imageFrame.setStyle(
                "-fx-background-color:#FAF7F5;" +
                        "-fx-background-radius:16;" +
                        "-fx-border-color:#F0E1DA;" +
                        "-fx-border-radius:16;"
        );

        Label selectedVariantLabel = new Label(selected[0].fullName);
        selectedVariantLabel.setMaxWidth(Double.MAX_VALUE);
        selectedVariantLabel.setAlignment(Pos.CENTER);
        selectedVariantLabel.setWrapText(true);
        selectedVariantLabel.setStyle(
                "-fx-font-size:12px;" +
                        "-fx-text-fill:#9A7468;" +
                        "-fx-background-color:#F8ECE7;" +
                        "-fx-background-radius:12;" +
                        "-fx-padding:4 10;"
        );

        Label nameLabel = new Label(selected[0].baseName);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        nameLabel.setAlignment(Pos.CENTER);
        nameLabel.setWrapText(true);
        nameLabel.setStyle(
                "-fx-font-size:18px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-text-fill:#5A3E36;"
        );

        Label priceLabel = new Label(
                String.format(Locale.US, "Price: %.2f", selected[0].price)
        );
        priceLabel.setStyle("-fx-font-size:15px; -fx-text-fill:#7A5C52;");

        Label colorsTitle = new Label("Available Colors:");
        colorsTitle.setStyle("-fx-font-size:14px; -fx-text-fill:#7A5C52;");

        HBox colorsBox = new HBox(10);
        colorsBox.setAlignment(Pos.CENTER);

        Label sizesLabel = new Label(
                "Sizes: " + safeGalleryText(selected[0].sizes, "No sizes")
        );
        sizesLabel.setWrapText(true);
        sizesLabel.setAlignment(Pos.CENTER);
        sizesLabel.setMaxWidth(Double.MAX_VALUE);
        sizesLabel.setStyle("-fx-font-size:14px; -fx-text-fill:#7A5C52;");

        Label sizeHintLabel = new Label("Click a size to see its remaining quantity");
        sizeHintLabel.setWrapText(true);
        sizeHintLabel.setAlignment(Pos.CENTER);
        sizeHintLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#9A7468;");

        FlowPane sizeBox = new FlowPane(8, 8);
        sizeBox.setAlignment(Pos.CENTER);
        sizeBox.setPrefWrapLength(250);

        Label qtyLabel = new Label();
        updateQuantityLabel(qtyLabel, selected[0].quantity);

        refreshSizeButtons(sizeBox, selected[0], qtyLabel);

        for (ProductVariant variant : variants) {
            Circle colorCircle = createColorCircle(variant.color);
            colorCircle.setRadius(11);

            Tooltip.install(
                    colorCircle,
                    new Tooltip(variant.color == null ? "Unknown" : variant.color)
            );

            colorCircle.setOnMouseClicked(event -> {
                selected[0] = variant;

                ImageView changed = createCleanProductImageView(variant.imagePath);
                imageView.setImage(changed.getImage());
                imageView.setViewport(changed.getViewport());
                imageView.setOnMouseClicked(imageEvent -> {
                    imageEvent.consume();
                    openFullImage(variant.imagePath);
                });

                boolean imageMissing = changed.getImage() == null;
                missingImageLabel.setVisible(imageMissing);
                missingImageLabel.setManaged(imageMissing);

                selectedVariantLabel.setText(variant.fullName);
                nameLabel.setText(variant.baseName);
                priceLabel.setText(
                        String.format(Locale.US, "Price: %.2f", variant.price)
                );
                sizesLabel.setText(
                        "Sizes: " + safeGalleryText(variant.sizes, "No sizes")
                );

                updateQuantityLabel(qtyLabel, variant.quantity);
                refreshSizeButtons(sizeBox, variant, qtyLabel);

                event.consume();
            });

            colorsBox.getChildren().add(colorCircle);
        }

        VBox card = new VBox(
                9,
                imageFrame,
                selectedVariantLabel,
                nameLabel,
                priceLabel,
                colorsTitle,
                colorsBox,
                sizesLabel,
                sizeBox,
                sizeHintLabel,
                qtyLabel
        );

        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(14));
        card.setPrefWidth(300);
        card.setMinWidth(300);
        card.setMaxWidth(300);
        card.setMinHeight(570);
        card.setCursor(Cursor.HAND);
        card.setStyle(productCardStyle(false));
        card.setEffect(new DropShadow(14, Color.rgb(90, 62, 54, 0.10)));

        card.setOnMouseEntered(event -> {
            card.setTranslateY(-4);
            card.setStyle(productCardStyle(true));
            card.setEffect(new DropShadow(22, Color.rgb(90, 62, 54, 0.18)));
        });

        card.setOnMouseExited(event -> {
            card.setTranslateY(0);
            card.setStyle(productCardStyle(false));
            card.setEffect(new DropShadow(14, Color.rgb(90, 62, 54, 0.10)));
        });

        card.setOnMouseClicked(event -> {
            if (isInteractiveProductCardTarget(event.getTarget(), card)) {
                return;
            }

            if ("CUSTOMER".equals(currentRole)) {
                openProductDetailsDialog(variants, selected[0]);
            }
        });

        return card;
    }

    private void updateQuantityLabel(Label label, int quantity) {
        label.setText("Remaining for selected color: " + quantity);
        label.setStyle(
                "-fx-font-size:14px;" +
                        "-fx-font-weight:bold;" +
                        "-fx-text-fill:" + (quantity > 0 ? "#6B4A42" : "#B42318") + ";"
        );
    }

    private String safeGalleryText(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value.trim();
    }

    private String productCardStyle(boolean hover) {
        return "-fx-background-color:white;" +
                "-fx-background-radius:22;" +
                "-fx-border-radius:22;" +
                "-fx-border-width:" + (hover ? "2" : "1") + ";" +
                "-fx-border-color:" + (hover ? "#C98F7B" : "#E7CFC4") + ";";
    }


    private void openProductOrderDialog(ArrayList<ProductVariant> variants, ProductVariant selectedVariant) {
        if (currentRole == null || !currentRole.equals("CUSTOMER")) {
            return;
        }

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Confirm Order");

        Label title = new Label("Confirm Online Order");
        title.setStyle("-fx-font-size:24px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");

        Label productName = new Label(selectedVariant.baseName);
        productName.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");

        ComboBox<String> colorBox = new ComboBox<>();
        colorBox.setPromptText("Choose color");
        colorBox.setMaxWidth(Double.MAX_VALUE);

        for (ProductVariant v : variants) {
            colorBox.getItems().add(v.color);
        }
        colorBox.setValue(selectedVariant.color);

        ComboBox<String> sizeBox = new ComboBox<>();
        sizeBox.setPromptText("Choose size");
        sizeBox.setMaxWidth(Double.MAX_VALUE);

        Runnable loadSizes = () -> {
            sizeBox.getItems().clear();
            ProductVariant v = findVariantByColor(variants, colorBox.getValue());
            if (v != null && v.sizeQuantity != null) {
                for (Map.Entry<String, Integer> entry : v.sizeQuantity.entrySet()) {
                    if (entry.getValue() > 0) {
                        sizeBox.getItems().add(entry.getKey() + " / Remaining " + entry.getValue());
                    }
                }
            }
        };

        loadSizes.run();

        colorBox.setOnAction(e -> loadSizes.run());

        TextField qtyField = new TextField("1");
        qtyField.setPromptText("Quantity");

        ComboBox<String> receiveBox = new ComboBox<>();
        receiveBox.getItems().addAll("PICKUP_FROM_STORE", "HOME_DELIVERY");
        receiveBox.setValue("PICKUP_FROM_STORE");
        receiveBox.setMaxWidth(Double.MAX_VALUE);

        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone number");

        TextField addressField = new TextField();
        addressField.setPromptText("Address only for home delivery");
        addressField.setDisable(true);

        receiveBox.setOnAction(e -> {
            boolean delivery = "HOME_DELIVERY".equals(receiveBox.getValue());
            addressField.setDisable(!delivery);
            if (!delivery) {
                addressField.clear();
            }
        });

        Label msg = new Label();
        msg.setStyle("-fx-text-fill:#8B0000; -fx-font-weight:bold;");

        Button confirmBtn = createSmallButton("Confirm Order");
        confirmBtn.setOnAction(e -> {
            try {
                ProductVariant chosen = findVariantByColor(variants, colorBox.getValue());
                if (chosen == null) {
                    msg.setText("Please choose a color.");
                    return;
                }

                if (sizeBox.getValue() == null) {
                    msg.setText("Please choose a size.");
                    return;
                }

                String sizeValue = sizeBox.getValue().split(" / ")[0].trim();
                int productId = chosen.productId;
                int sizeId = getSizeIdForProduct(productId, sizeValue);
                int qty = Integer.parseInt(qtyField.getText().trim());
                String receiveMethod = receiveBox.getValue();
                String phone = phoneField.getText().trim();
                String address = addressField.getText().trim();

                if (qty <= 0) {
                    msg.setText("Quantity must be greater than zero.");
                    return;
                }

                if (phone.isEmpty()) {
                    msg.setText("Please enter phone number.");
                    return;
                }

                if ("HOME_DELIVERY".equals(receiveMethod) && address.isEmpty()) {
                    msg.setText("Please enter delivery address.");
                    return;
                }

                int orderId = createOnlineOrder(currentUserId, productId, sizeId, qty, receiveMethod, address, phone);
                msg.setStyle("-fx-text-fill:#1B7F3A; -fx-font-weight:bold;");
                msg.setText("Order confirmed. Order ID = " + orderId);

                Alert done = new Alert(Alert.AlertType.INFORMATION);
                done.setTitle("Order Confirmed");
                done.setHeaderText("Order created successfully");
                done.setContentText("Order ID = " + orderId);
                done.showAndWait();

                stage.close();
                showCustomerOnlineOrders(currentUserId);

            } catch (Exception ex) {
                ex.printStackTrace();
                msg.setStyle("-fx-text-fill:#8B0000; -fx-font-weight:bold;");
                msg.setText(ex.getMessage());
            }
        });

        VBox root = new VBox(12,
                title,
                productName,
                new Label("Color"), colorBox,
                new Label("Size"), sizeBox,
                new Label("Quantity"), qtyField,
                new Label("Receive Method"), receiveBox,
                new Label("Phone"), phoneField,
                new Label("Address"), addressField,
                confirmBtn,
                msg
        );

        root.setPadding(new Insets(25));
        root.setStyle("-fx-background-color:#FFF7F2;");

        stage.setScene(new Scene(root, 460, 620));
        stage.showAndWait();
    }

    private ProductVariant findVariantByColor(ArrayList<ProductVariant> variants, String color) {
        if (color == null) return null;

        for (ProductVariant v : variants) {
            if (v.color != null && v.color.equalsIgnoreCase(color)) {
                return v;
            }
        }

        return null;
    }

    private int getSizeIdForProduct(int productId, String sizeValue) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();

        PreparedStatement ps = con.prepareStatement(
                "SELECT SizeID FROM product_sizes WHERE ProductID=? AND SizeValue=?"
        );
        ps.setInt(1, productId);
        ps.setString(2, sizeValue);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getInt("SizeID");
        }

        throw new Exception("Size not found for this product.");
    }


    private void refreshSizeButtons(
            FlowPane sizeBox,
            ProductVariant variant,
            Label qtyLabel
    ) {
        sizeBox.getChildren().clear();

        if (variant == null
                || variant.sizeQuantity == null
                || variant.sizeQuantity.isEmpty()) {

            Label noSizes = new Label("No sizes available");
            noSizes.setStyle(
                    "-fx-text-fill:#B42318;" +
                            "-fx-font-weight:bold;"
            );
            sizeBox.getChildren().add(noSizes);
            return;
        }

        for (Map.Entry<String, Integer> entry : variant.sizeQuantity.entrySet()) {
            String size = entry.getKey();
            int remaining = entry.getValue();

            Button sizeButton = new Button(size);
            sizeButton.setMinWidth(52);
            sizeButton.setPrefHeight(38);
            sizeButton.setDisable(remaining <= 0);
            sizeButton.setTooltip(
                    new Tooltip(
                            "Size " + size + " - Remaining: " + remaining
                    )
            );

            String normalStyle =
                    "-fx-background-color:white;" +
                            "-fx-border-color:#C98F7B;" +
                            "-fx-text-fill:#5A3E36;" +
                            "-fx-font-weight:bold;" +
                            "-fx-background-radius:12;" +
                            "-fx-border-radius:12;" +
                            "-fx-border-width:1.5;" +
                            "-fx-cursor:hand;";

            String selectedStyle =
                    "-fx-background-color:#C98F7B;" +
                            "-fx-border-color:#A86F5D;" +
                            "-fx-text-fill:white;" +
                            "-fx-font-weight:bold;" +
                            "-fx-background-radius:12;" +
                            "-fx-border-radius:12;" +
                            "-fx-border-width:1.5;" +
                            "-fx-cursor:hand;";

            sizeButton.setStyle(normalStyle);

            sizeButton.setOnAction(event -> {
                for (javafx.scene.Node node : sizeBox.getChildren()) {
                    if (node instanceof Button) {
                        node.setStyle(normalStyle);
                    }
                }

                sizeButton.setStyle(selectedStyle);
                qtyLabel.setText(
                        "Remaining for size " + size + ": " + remaining
                );
                qtyLabel.setStyle(
                        "-fx-font-size:14px;" +
                                "-fx-font-weight:bold;" +
                                "-fx-text-fill:" +
                                (remaining > 0 ? "#1B7F3A" : "#B42318") +
                                ";"
                );

                event.consume();
            });

            sizeBox.getChildren().add(sizeButton);
        }
    }

    private boolean isInteractiveProductCardTarget(
            Object target,
            VBox card
    ) {
        if (!(target instanceof javafx.scene.Node)) {
            return false;
        }

        javafx.scene.Node node = (javafx.scene.Node) target;

        while (node != null && node != card) {
            if (node instanceof Button || node instanceof Circle) {
                return true;
            }
            node = node.getParent();
        }

        return false;
    }

    private java.util.LinkedHashMap<String, Integer> parseSizeQuantities(String text) {
        java.util.LinkedHashMap<String, Integer> map = new java.util.LinkedHashMap<>();

        if (text == null || text.trim().isEmpty()) {
            return map;
        }

        String[] parts = text.split(",");
        for (String part : parts) {
            String[] pair = part.trim().split(":");
            if (pair.length == 2) {
                try {
                    map.put(pair[0].trim(), Integer.parseInt(pair[1].trim()));
                } catch (Exception ignored) {
                    map.put(pair[0].trim(), 0);
                }
            }
        }

        return map;
    }

    private String collectSizes(ArrayList<ProductVariant> variants) {
        ArrayList<String> order = new ArrayList<>();
        order.add("S");
        order.add("M");
        order.add("L");
        order.add("XL");

        LinkedHashSet<String> found = new LinkedHashSet<>();

        for (String fixedSize : order) {
            for (ProductVariant v : variants) {
                if (v.sizes != null) {
                    for (String s : v.sizes.split(",")) {
                        if (s.trim().equalsIgnoreCase(fixedSize)) {
                            found.add(fixedSize);
                        }
                    }
                }
            }
        }

        // Add any other numeric/special sizes after S M L XL, without removing them.
        TreeSet<String> others = new TreeSet<>();
        for (ProductVariant v : variants) {
            if (v.sizes != null) {
                for (String s : v.sizes.split(",")) {
                    String clean = s.trim();
                    if (!clean.isEmpty() && !found.contains(clean) &&
                            !clean.equalsIgnoreCase("S") &&
                            !clean.equalsIgnoreCase("M") &&
                            !clean.equalsIgnoreCase("L") &&
                            !clean.equalsIgnoreCase("XL")) {
                        others.add(clean);
                    }
                }
            }
        }

        found.addAll(others);
        return String.join(", ", found);
    }

    private Circle createColorCircle(String colorName) {
        Circle circle = new Circle(10);
        circle.setStroke(Color.GRAY);
        circle.setStrokeWidth(1.5);
        circle.setCursor(Cursor.HAND);

        if (colorName == null) {
            circle.setFill(Color.LIGHTGRAY);
            return circle;
        }

        switch (colorName.toLowerCase()) {
            case "black": circle.setFill(Color.BLACK); break;
            case "white": circle.setFill(Color.WHITE); break;
            case "red": circle.setFill(Color.RED); break;
            case "blue": circle.setFill(Color.DODGERBLUE); break;
            case "light blue": circle.setFill(Color.LIGHTBLUE); break;
            case "dark blue": circle.setFill(Color.rgb(20, 45, 85)); break;
            case "fuchsia": circle.setFill(Color.rgb(255, 0, 128)); break;
            case "pink": circle.setFill(Color.PINK); break;
            case "burgundy": circle.setFill(Color.rgb(128, 0, 32)); break;
            case "beige": circle.setFill(Color.BEIGE); break;
            case "nude": circle.setFill(Color.rgb(224, 190, 160)); break;
            case "navy": circle.setFill(Color.NAVY); break;
            case "brown": circle.setFill(Color.SADDLEBROWN); break;
            case "gray":
            case "grey": circle.setFill(Color.LIGHTGRAY); break;
            case "mauve": circle.setFill(Color.rgb(180, 130, 160)); break;
            case "olive": circle.setFill(Color.OLIVE); break;
            default: circle.setFill(Color.LIGHTGRAY);
        }

        return circle;
    }

    private String cleanProductNameJava(String name, String color) {
        if (name == null) return "";

        String clean = name.trim();

        // Prefer the actual database color. This correctly handles colors
        // containing spaces, such as Light Blue and Dark Blue.
        if (color != null && !color.trim().isEmpty()) {
            String colorText = color.trim();
            String lowerName = clean.toLowerCase(Locale.ROOT);
            String lowerColor = colorText.toLowerCase(Locale.ROOT);

            if (lowerName.endsWith(" " + lowerColor)) {
                clean = clean.substring(0, clean.length() - colorText.length()).trim();
            }
        }

        // Fallback for older rows where Color was empty or inconsistent.
        String[] suffixes = {
                " Light Blue", " Dark Blue",
                " White", " Black", " Red", " Nude", " Blue", " Fuchsia",
                " Beige", " Burgundy", " Navy", " Brown", " Gray", " Grey",
                " Pink", " Olive", " Mauve",
                " أبيض", " أسود", " أحمر", " أزرق", " بيج", " خمري",
                " كحلي", " بني", " رمادي", " زهري", " زيتي"
        };

        for (String suffix : suffixes) {
            if (clean.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT))) {
                clean = clean.substring(0, clean.length() - suffix.length()).trim();
                break;
            }
        }

        // Normalize the older product naming order so all Elegant Abaya
        // colors are grouped into one card.
        if (clean.equalsIgnoreCase("Abaya Elegant")) {
            clean = "Elegant Abaya";
        }

        return clean;
    }

    private ImageView createCleanProductImageView(String imagePath) {
        ImageView imageView = new ImageView();

        try {
            File file = resolveProductImageFile(imagePath);

            if (file != null) {
                Image img = new Image(file.toURI().toString(), false);
                imageView.setImage(img);

                double w = img.getWidth();
                double h = img.getHeight();

                if (w > 0 && h > 0) {
                    double cropX = w * 0.04;
                    double cropY = h * 0.10;
                    double cropW = w * 0.92;
                    double cropH = h * 0.78;

                    imageView.setViewport(
                            new javafx.geometry.Rectangle2D(
                                    cropX, cropY, cropW, cropH
                            )
                    );
                }
            } else {
                System.out.println("Image not found: " + imagePath);
            }
        } catch (Exception ex) {
            System.out.println("Image error: " + imagePath + " - " + ex.getMessage());
        }

        imageView.setFitWidth(245);
        imageView.setFitHeight(310);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.setCursor(Cursor.HAND);

        imageView.setOnMouseClicked(e -> {
            e.consume();
            openFullImage(imagePath);
        });

        return imageView;
    }

    private File resolveProductImageFile(String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return null;
        }

        String rawPath = imagePath.trim().replace("\\", "/");
        String originalFileName = new File(rawPath).getName();

        LinkedHashSet<String> candidateNames = new LinkedHashSet<>();
        candidateNames.add(originalFileName);

        int dotIndex = originalFileName.lastIndexOf('.');
        String stem = dotIndex > 0
                ? originalFileName.substring(0, dotIndex)
                : originalFileName;

        String[] extensions = {".png", ".jpg", ".jpeg", ".webp"};
        for (String extension : extensions) {
            candidateNames.add(stem + extension);
        }

        String lowerStem = stem.toLowerCase(Locale.ROOT);

        if (lowerStem.startsWith("elegant_abaya_")) {
            String suffix = stem.substring("elegant_abaya_".length());
            for (String extension : extensions) {
                candidateNames.add("abaya_elegant_" + suffix + extension);
            }
        }

        if (lowerStem.startsWith("abaya_elegant_")) {
            String suffix = stem.substring("abaya_elegant_".length());
            for (String extension : extensions) {
                candidateNames.add("elegant_abaya_" + suffix + extension);
            }
        }

        if (lowerStem.equals("embroidered_abaya_pink")) {
            for (String extension : extensions) {
                candidateNames.add("embroidered_abaya_mauve" + extension);
            }
        }

        if (lowerStem.equals("pointed_heel_red")) {
            for (String extension : extensions) {
                candidateNames.add("pointed_heel_red_original" + extension);
            }
        }

        LinkedHashSet<File> candidates = new LinkedHashSet<>();

        File directFile = new File(rawPath);
        candidates.add(directFile);

        File workingDirectory = new File(
                System.getProperty("user.dir", ".")
        );

        ArrayList<File> imageDirectories = new ArrayList<>();
        imageDirectories.add(workingDirectory);
        imageDirectories.add(new File(workingDirectory, "out/images"));
        imageDirectories.add(new File(workingDirectory, "src/out/images"));
        imageDirectories.add(new File(workingDirectory, "images"));
        imageDirectories.add(
                new File("C:/Users/user/IdeaProjects/data/out/images")
        );

        // Keep the complete relative path as a candidate.
        candidates.add(new File(workingDirectory, rawPath));

        for (File directory : imageDirectories) {
            for (String candidateName : candidateNames) {
                candidates.add(new File(directory, candidateName));
            }
        }

        for (File candidate : candidates) {
            if (candidate != null && candidate.isFile()) {
                return candidate;
            }
        }

        // Last attempt: case-insensitive filename lookup.
        for (File directory : imageDirectories) {
            if (directory == null || !directory.isDirectory()) {
                continue;
            }

            File[] files = directory.listFiles();
            if (files == null) {
                continue;
            }

            for (File file : files) {
                if (!file.isFile()) {
                    continue;
                }

                for (String candidateName : candidateNames) {
                    if (file.getName().equalsIgnoreCase(candidateName)) {
                        return file;
                    }
                }
            }
        }

        return null;
    }

    private void openFullImage(String imagePath) {
        File file = resolveProductImageFile(imagePath);

        if (file == null) {
            showAlert("Image not found: " + imagePath);
            return;
        }

        Stage imageStage = new Stage();
        ImageView fullImageView = new ImageView(
                new Image(file.toURI().toString(), false)
        );

        fullImageView.setPreserveRatio(true);
        fullImageView.setFitWidth(850);
        fullImageView.setFitHeight(650);

        ScrollPane scrollPane = new ScrollPane(fullImageView);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color:#111111;");

        Button closeBtn = createSmallButton("Close");
        closeBtn.setOnAction(e -> imageStage.close());

        VBox root = new VBox(12, scrollPane, closeBtn);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color:#111111;");

        imageStage.setScene(new Scene(root, 900, 760));
        imageStage.setTitle("Product Image");
        imageStage.show();
    }

    private static class ProductVariant {
        int productId;
        String baseName;
        String fullName;
        String category;
        String color;
        double price;
        String sizes;
        int quantity;
        String imagePath;
        java.util.LinkedHashMap<String, Integer> sizeQuantity;

        ProductVariant(int productId, String baseName, String fullName, String category, String color, double price, String sizes, int quantity, String imagePath, java.util.LinkedHashMap<String, Integer> sizeQuantity) {
            this.productId = productId;
            this.baseName = baseName;
            this.fullName = fullName;
            this.category = category;
            this.color = color;
            this.price = price;
            this.sizes = sizes;
            this.quantity = quantity;
            this.imagePath = imagePath;
            this.sizeQuantity = sizeQuantity;
        }
    }




    private String placeholders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(",");
            sb.append("?");
        }
        return sb.toString();
    }

    private void openFilterDialog() {
        Stage filterStage = new Stage();
        filterStage.initModality(Modality.APPLICATION_MODAL);
        filterStage.setTitle("Filter");

        Label header = new Label("Filter");
        header.setStyle("-fx-font-size:28px; -fx-font-weight:bold; -fx-text-fill:#111111;");

        Label sortTitle = new Label("Sort");
        sortTitle.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#111111;");

        ToggleGroup sortGroup = new ToggleGroup();
        FlowPane sortPane = new FlowPane(12, 12);
        String[] sortOptions = {"Recommend", "Most Popular", "Price Low to High", "Price High to Low", "New Arrivals"};

        for (String option : sortOptions) {
            ToggleButton chip = createFilterChip(option);
            chip.setToggleGroup(sortGroup);
            chip.setSelected(option.equals(selectedSort));
            sortPane.getChildren().add(chip);
        }

        Label sizeTitle = new Label("Size");
        sizeTitle.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#111111;");

        FlowPane sizePane = new FlowPane(12, 12);
        String[] sizes = {"XXS", "XS", "S", "M", "L", "XL", "XXL", "36", "37", "38", "39", "40", "41", "42", "44"};
        ArrayList<ToggleButton> sizeButtons = new ArrayList<>();

        for (String size : sizes) {
            ToggleButton chip = createFilterChip(size);
            chip.setSelected(selectedSizes.contains(size));
            sizeButtons.add(chip);
            sizePane.getChildren().add(chip);
        }

        Label colorTitle = new Label("Color");
        colorTitle.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#111111;");

        FlowPane colorPane = new FlowPane(12, 12);
        String[] colors = {
                "Black", "Nude", "White", "Burgundy", "Beige",
                "Blue", "Light Blue", "Dark Blue", "Red", "Fuchsia",
                "Navy", "Brown", "Pink", "Gray", "Mauve", "Olive"
        };
        ArrayList<ToggleButton> colorButtons = new ArrayList<>();

        for (String color : colors) {
            ToggleButton chip = createFilterChip(color);
            chip.setSelected(selectedColors.contains(color));
            colorButtons.add(chip);
            colorPane.getChildren().add(chip);
        }

        Button clearBtn = new Button("Clear");
        clearBtn.setPrefWidth(120);
        clearBtn.setPrefHeight(45);
        clearBtn.setStyle("-fx-background-color:white; -fx-border-color:#BBBBBB; -fx-font-size:16px; -fx-font-weight:bold;");

        Button doneBtn = new Button("Done");
        doneBtn.setPrefWidth(120);
        doneBtn.setPrefHeight(45);
        doneBtn.setStyle("-fx-background-color:#111111; -fx-text-fill:white; -fx-font-size:16px; -fx-font-weight:bold;");

        clearBtn.setOnAction(e -> {
            selectedColors.clear();
            selectedSizes.clear();
            selectedSort = "Recommend";

            for (ToggleButton b : colorButtons) b.setSelected(false);
            for (ToggleButton b : sizeButtons) b.setSelected(false);

            for (javafx.scene.Node node : sortPane.getChildren()) {
                if (node instanceof ToggleButton) {
                    ToggleButton b = (ToggleButton) node;
                    b.setSelected("Recommend".equals(b.getText()));
                }
            }
        });

        doneBtn.setOnAction(e -> {
            selectedColors.clear();
            selectedSizes.clear();

            Toggle selected = sortGroup.getSelectedToggle();
            if (selected instanceof ToggleButton) {
                selectedSort = ((ToggleButton) selected).getText();
            }

            for (ToggleButton b : colorButtons) {
                if (b.isSelected()) selectedColors.add(b.getText());
            }

            for (ToggleButton b : sizeButtons) {
                if (b.isSelected()) selectedSizes.add(b.getText());
            }

            filterStage.close();

            if (currentKeyword != null && !currentKeyword.trim().isEmpty()) {
                showGalleryInternal(currentCategory, currentKeyword, "Search Results");
            } else {
                showGalleryInternal(
                        currentCategory,
                        null,
                        currentCategory == null ? "All Products" : currentCategory
                );
            }
        });

        Region bottomSpace = new Region();
        HBox.setHgrow(bottomSpace, Priority.ALWAYS);
        HBox bottom = new HBox(15, bottomSpace, clearBtn, doneBtn);
        bottom.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(20, header, sortTitle, sortPane, sizeTitle, sizePane, colorTitle, colorPane, bottom);
        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color:white;");

        filterStage.setScene(new Scene(root, 760, 650));
        filterStage.showAndWait();
    }

    private ToggleButton createFilterChip(String text) {
        ToggleButton btn = new ToggleButton(text);
        btn.setPrefHeight(44);
        btn.setMinWidth(72);
        btn.setCursor(Cursor.HAND);

        String normal = "-fx-background-color:white; -fx-border-color:#E6E6E6; -fx-text-fill:#111111; -fx-font-size:15px; -fx-padding:8 18 8 18;";
        String selected = "-fx-background-color:#111111; -fx-border-color:#111111; -fx-text-fill:white; -fx-font-size:15px; -fx-padding:8 18 8 18;";

        btn.setStyle(btn.isSelected() ? selected : normal);
        btn.selectedProperty().addListener((obs, oldVal, isSelected) -> btn.setStyle(isSelected ? selected : normal));

        return btn;
    }


    private void showTable(String sql) {
        pageTitle.setText("Products / Data");
        if (contentBox != null && contentBox.getChildren().size() > 2) {
            contentBox.getChildren().set(2, table);
        }
        VBox.setVgrow(table, Priority.ALWAYS);
        loadTable(sql);
    }

    private void loadTable(String sql) {
        table.getColumns().clear();
        table.getItems().clear();
        table.setFixedCellSize(46);

        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();

            if (con == null) {
                showAlert("Database connection is null. Check password and MySQL connector.");
                return;
            }

            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            int columnCount = rs.getMetaData().getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                final int index = i - 1;
                String columnName = rs.getMetaData().getColumnLabel(i);

                TableColumn<String[], String> column = new TableColumn<>(columnName);
                column.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[index]));

                if (columnName.equals("Product") || columnName.equals("ImagePath")) {
                    column.setCellFactory(col -> new TableCell<String[], String>() {
                        private final ImageView imageView = new ImageView();

                        {
                            imageView.setFitWidth(95);
                            imageView.setFitHeight(115);
                            imageView.setPreserveRatio(true);
                            imageView.setCursor(Cursor.HAND);
                        }

                        @Override
                        protected void updateItem(String imagePath, boolean empty) {
                            super.updateItem(imagePath, empty);

                            if (empty || imagePath == null || imagePath.trim().isEmpty()) {
                                setGraphic(null);
                                setText(null);
                                return;
                            }

                            try {
                                File file = new File(imagePath);

                                if (file.exists()) {
                                    Image img = new Image(file.toURI().toString());
                                    imageView.setImage(img);
                                    imageView.setOnMouseClicked(e -> openFullImage(imagePath));
                                    setGraphic(imageView);
                                    setText(null);
                                } else {
                                    setGraphic(null);
                                    setText("No Image");
                                }

                            } catch (Exception ex) {
                                setGraphic(null);
                                setText("Image Error");
                            }
                        }
                    });
                }

                if (columnName.equals("Product")) column.setPrefWidth(125);
                else if (columnName.equals("ImagePath")) column.setPrefWidth(125);
                else if (columnName.equals("ProductName")) column.setPrefWidth(190);
                else if (columnName.equals("RemainingQuantity")) column.setPrefWidth(150);
                else if (columnName.equals("CustomerID")) column.setPrefWidth(100);
                else if (columnName.equals("FullName")) column.setPrefWidth(190);
                else if (columnName.equals("Username")) column.setPrefWidth(150);
                else if (columnName.equals("Password")) column.setPrefWidth(120);
                else if (columnName.equals("Role")) column.setPrefWidth(120);
                else if (columnName.equals("Phone")) column.setPrefWidth(150);
                else if (columnName.equals("Email")) column.setPrefWidth(220);
                else if (columnName.equals("TotalInvoices")) column.setPrefWidth(140);
                else if (columnName.equals("TotalSpent")) column.setPrefWidth(140);
                else if (columnName.equals("LastPurchaseDate")) column.setPrefWidth(170);
                else column.setPrefWidth(130);

                table.getColumns().add(column);
            }

            while (rs.next()) {
                String[] row = new String[columnCount];

                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = rs.getString(i);
                }

                table.getItems().add(row);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(ex.getMessage());
        }
    }


    private TableView<String[]> createMiniTable(String sql) {
        TableView<String[]> mini = new TableView<>();

        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();

            if (con == null) {
                showAlert("Database connection is null. Check DataBaseConnection.java and MySQL Connector.");
                return mini;
            }

            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            int columnCount = rs.getMetaData().getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                final int index = i - 1;
                String columnName = rs.getMetaData().getColumnLabel(i);

                TableColumn<String[], String> column = new TableColumn<>(columnName);
                column.setCellValueFactory(data -> new SimpleStringProperty(data.getValue()[index]));
                column.setPrefWidth(150);
                mini.getColumns().add(column);
            }

            while (rs.next()) {
                String[] row = new String[columnCount];

                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = rs.getString(i);
                }

                mini.getItems().add(row);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(ex.getMessage());
        }

        return mini;
    }

    private void openCreateCustomerWindow() {
        Stage stage = new Stage();

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Full Name");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        TextField passwordField = new TextField();
        passwordField.setPromptText("Password");

        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone");

        Label message = new Label();

        Button saveBtn = createSmallButton("Save Customer");

        saveBtn.setOnAction(e -> {
            try {
                Connection con = new DataBaseConnection().getConnection().getConnection();

                PreparedStatement userPs = con.prepareStatement(
                        "INSERT INTO users(FullName, Username, Password, Role) VALUES (?, ?, ?, 'CUSTOMER')",
                        Statement.RETURN_GENERATED_KEYS
                );

                userPs.setString(1, fullNameField.getText());
                userPs.setString(2, usernameField.getText());
                userPs.setString(3, passwordField.getText());
                userPs.executeUpdate();

                ResultSet keys = userPs.getGeneratedKeys();

                if (keys.next()) {
                    PreparedStatement customerPs = con.prepareStatement(
                            "INSERT INTO customers(UserID, Phone) VALUES (?, ?)"
                    );

                    customerPs.setInt(1, keys.getInt(1));
                    customerPs.setString(2, phoneField.getText());
                    customerPs.executeUpdate();

                    message.setText("Customer saved successfully");
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                message.setText(ex.getMessage());
            }
        });

        VBox root = new VBox(12, new Label("Create Customer"), fullNameField, usernameField, passwordField, phoneField, saveBtn, message);
        root.setPadding(new Insets(25));

        stage.setScene(new Scene(root, 420, 420));
        stage.setTitle("Create Customer");
        stage.show();
    }

    /** One line item inside the new POS cart (kept distinct from the customer-facing CartItem class). */
    private static class PosCartLine {
        int productId, sizeId, quantity;
        String productLabel, sizeLabel;
        double unitPrice;

        PosCartLine(int productId, int sizeId, String productLabel, String sizeLabel, int quantity, double unitPrice) {
            this.productId = productId;
            this.sizeId = sizeId;
            this.productLabel = productLabel;
            this.sizeLabel = sizeLabel;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        double subtotal() { return quantity * unitPrice; }
    }

    /**
     * Full POS screen: pick a product + size from the cashier's own branch stock,
     * add multiple lines to a cart, optionally attach a customer and discount code,
     * save the sale, and immediately open a clean printable invoice.
     */
    private void openCreateSaleWindow(int cashierUserId) {
        Stage stage = new Stage();
        stage.setTitle("Point of Sale - Create Sale");

        Label header = createSectionTitle("🧾 New Sale");

        // ---- product / size / qty picker ----
        ComboBox<String[]> productCb = createStyledCombo("Select product");
        productCb.setPrefWidth(260);
        ComboBox<String[]> sizeCb = createStyledCombo("Size");
        sizeCb.setPrefWidth(90);
        Spinner<Integer> qtySpin = new Spinner<>(1, 999, 1);
        qtySpin.setPrefWidth(80);
        qtySpin.setEditable(true);

        Label stockLbl = new Label("");
        stockLbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold;");

        int branchId;
        try {
            branchId = getCashierBranchId(cashierUserId);
        } catch (Exception ex) {
            showAlert("Could not find this cashier's branch: " + ex.getMessage());
            return;
        }
        final int finalBranchId = branchId;

        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            PreparedStatement ps = con.prepareStatement(
                    "SELECT DISTINCT p.ProductID, p.Name, p.Color, p.Price, p.Category " +
                    "FROM branch_inventory bi JOIN products p ON bi.ProductID=p.ProductID " +
                    "WHERE bi.BranchID=? ORDER BY p.Category, p.Name, p.Color");
            ps.setInt(1, finalBranchId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                productCb.getItems().add(new String[]{
                        String.valueOf(rs.getInt("ProductID")), rs.getString("Name"),
                        rs.getString("Color"), String.valueOf(rs.getDouble("Price")), rs.getString("Category")
                });
            }
        } catch (Exception ex) { showAlert(ex.getMessage()); }

        productCb.setConverter(new javafx.util.StringConverter<String[]>() {
            @Override public String toString(String[] p) { return p == null ? "" : p[1] + " - " + p[2] + "  (₪" + p[3] + ")"; }
            @Override public String[] fromString(String s) { return null; }
        });
        sizeCb.setConverter(new javafx.util.StringConverter<String[]>() {
            @Override public String toString(String[] s) { return s == null ? "" : s[1]; }
            @Override public String[] fromString(String s) { return null; }
        });

        productCb.setOnAction(ev -> {
            sizeCb.getItems().clear();
            sizeCb.setValue(null);
            stockLbl.setText("");
            String[] sel = productCb.getValue();
            if (sel == null) return;
            try {
                Connection con = new DataBaseConnection().getConnection().getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT ps.SizeID, ps.SizeValue, bi.Quantity FROM product_sizes ps " +
                        "JOIN branch_inventory bi ON bi.SizeID=ps.SizeID AND bi.ProductID=ps.ProductID " +
                        "WHERE ps.ProductID=? AND bi.BranchID=? ORDER BY ps.SizeValue");
                ps.setInt(1, Integer.parseInt(sel[0]));
                ps.setInt(2, finalBranchId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    sizeCb.getItems().add(new String[]{String.valueOf(rs.getInt("SizeID")), rs.getString("SizeValue"), String.valueOf(rs.getInt("Quantity"))});
                }
            } catch (Exception ex) { showAlert(ex.getMessage()); }
        });

        sizeCb.setOnAction(ev -> {
            String[] s = sizeCb.getValue();
            if (s == null) { stockLbl.setText(""); return; }
            int qty = Integer.parseInt(s[2]);
            stockLbl.setText("In stock: " + qty);
            stockLbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:" + (qty <= 3 ? "#E53935" : "#27AE60") + ";");
        });

        Button addBtn = createSmallButton("➕ Add to Cart");

        // ---- cart table ----
        ObservableList<PosCartLine> cart = FXCollections.observableArrayList();
        TableView<PosCartLine> cartTable = new TableView<>(cart);
        cartTable.setPrefHeight(220);
        cartTable.setStyle("-fx-background-color:white; -fx-border-color:#E7CFC4; -fx-border-radius:10; -fx-background-radius:10;");
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<PosCartLine, String> cProd = new TableColumn<>("Product");
        cProd.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().productLabel));
        TableColumn<PosCartLine, String> cSize = new TableColumn<>("Size");
        cSize.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().sizeLabel));
        TableColumn<PosCartLine, String> cQty = new TableColumn<>("Qty");
        cQty.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().quantity)));
        TableColumn<PosCartLine, String> cPrice = new TableColumn<>("Unit Price");
        cPrice.setCellValueFactory(d -> new SimpleStringProperty("₪" + String.format("%.2f", d.getValue().unitPrice)));
        TableColumn<PosCartLine, String> cSub = new TableColumn<>("Subtotal");
        cSub.setCellValueFactory(d -> new SimpleStringProperty("₪" + String.format("%.2f", d.getValue().subtotal())));
        TableColumn<PosCartLine, Void> cDel = new TableColumn<>("");
        cDel.setPrefWidth(36);
        cDel.setCellFactory(col -> new TableCell<PosCartLine, Void>() {
            final Button del = new Button("✕");
            {
                del.setStyle("-fx-background-color:transparent; -fx-text-fill:#E53935; -fx-cursor:hand; -fx-font-weight:bold;");
                del.setOnAction(e -> { if (getTableRow().getItem() != null) cart.remove(getTableRow().getItem()); });
            }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty ? null : del); }
        });
        cartTable.getColumns().addAll(cProd, cSize, cQty, cPrice, cSub, cDel);

        addBtn.setOnAction(ev -> {
            String[] prod = productCb.getValue();
            String[] size = sizeCb.getValue();
            if (prod == null || size == null) { showAlert("Please select a product and a size."); return; }
            int available = Integer.parseInt(size[2]);
            int qty = qtySpin.getValue();
            int alreadyInCart = cart.stream()
                    .filter(l -> l.productId == Integer.parseInt(prod[0]) && l.sizeId == Integer.parseInt(size[0]))
                    .mapToInt(l -> l.quantity).sum();
            if (qty + alreadyInCart > available) {
                showAlert("Not enough stock. Available = " + available + (alreadyInCart > 0 ? " (already " + alreadyInCart + " in cart)" : ""));
                return;
            }
            boolean merged = false;
            for (PosCartLine line : cart) {
                if (line.productId == Integer.parseInt(prod[0]) && line.sizeId == Integer.parseInt(size[0])) {
                    line.quantity += qty;
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                cart.add(new PosCartLine(Integer.parseInt(prod[0]), Integer.parseInt(size[0]),
                        prod[1] + " - " + prod[2], size[1], qty, Double.parseDouble(prod[3])));
            }
            cartTable.refresh();
            qtySpin.getValueFactory().setValue(1);
        });

        HBox pickerRow = new HBox(10, productCb, sizeCb, qtySpin, addBtn, stockLbl);
        pickerRow.setAlignment(Pos.CENTER_LEFT);

        // ---- customer + discount ----
        ComboBox<String[]> customerCb = createStyledCombo("Walk-in customer (optional)");
        customerCb.setPrefWidth(230);
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            ResultSet rs = con.createStatement().executeQuery(
                    "SELECT c.CustomerID, u.FullName FROM customers c JOIN users u ON c.UserID=u.UserID ORDER BY u.FullName");
            while (rs.next()) customerCb.getItems().add(new String[]{String.valueOf(rs.getInt("CustomerID")), rs.getString("FullName")});
        } catch (Exception ex) { /* table may not exist yet on a brand-new DB */ }
        customerCb.setConverter(new javafx.util.StringConverter<String[]>() {
            @Override public String toString(String[] c) { return c == null ? "" : c[1]; }
            @Override public String[] fromString(String s) { return null; }
        });

        ComboBox<String[]> discountCb = createStyledCombo("Discount code (optional)");
        discountCb.setPrefWidth(230);
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            ResultSet rs = con.createStatement().executeQuery(
                    "SELECT DiscountID, Code, Percentage FROM discounts WHERE StartDate<=CURDATE() AND EndDate>=CURDATE()");
            while (rs.next()) discountCb.getItems().add(new String[]{String.valueOf(rs.getInt("DiscountID")), rs.getString("Code"), String.valueOf(rs.getDouble("Percentage"))});
        } catch (Exception ex) { /* ignore */ }
        discountCb.setConverter(new javafx.util.StringConverter<String[]>() {
            @Override public String toString(String[] d) { return d == null ? "" : d[1] + " (-" + d[2] + "%)"; }
            @Override public String[] fromString(String s) { return null; }
        });

        HBox metaRow = new HBox(16, customerCb, discountCb);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        // ---- totals ----
        Label totalLbl = new Label("Subtotal: ₪0.00");
        totalLbl.setStyle("-fx-font-size:13px; -fx-text-fill:#5A3E36;");
        Label discLbl = new Label("Discount: -₪0.00");
        discLbl.setStyle("-fx-font-size:13px; -fx-text-fill:#5A3E36;");
        Label finalLbl = new Label("TOTAL: ₪0.00");
        finalLbl.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");

        Runnable recalc = () -> {
            double subtotal = cart.stream().mapToDouble(PosCartLine::subtotal).sum();
            double pct = 0;
            String[] disc = discountCb.getValue();
            if (disc != null) pct = Double.parseDouble(disc[2]);
            double discAmt = subtotal * pct / 100.0;
            double total = subtotal - discAmt;
            totalLbl.setText("Subtotal: ₪" + String.format("%.2f", subtotal));
            discLbl.setText("Discount (" + (int) pct + "%): -₪" + String.format("%.2f", discAmt));
            finalLbl.setText("TOTAL: ₪" + String.format("%.2f", total));
        };
        cart.addListener((javafx.collections.ListChangeListener<PosCartLine>) c -> recalc.run());
        discountCb.setOnAction(ev -> recalc.run());

        VBox totalsBox = new VBox(4, totalLbl, discLbl, finalLbl);
        totalsBox.setAlignment(Pos.CENTER_RIGHT);
        totalsBox.setPadding(new Insets(12, 18, 12, 18));
        totalsBox.setStyle("-fx-background-color:#FFF1EA; -fx-background-radius:12;");

        Label message = new Label();
        message.setStyle("-fx-text-fill:#E53935; -fx-font-weight:bold;");

        Button saveBtn = createSuccessButton("💾 Complete Sale & Print Invoice");
        Button cancelBtn = createGhostButton("Cancel");
        cancelBtn.setOnAction(ev -> stage.close());

        saveBtn.setOnAction(ev -> {
            if (cart.isEmpty()) { message.setText("Cart is empty. Add at least one item."); return; }
            double subtotal = cart.stream().mapToDouble(PosCartLine::subtotal).sum();
            String[] disc = discountCb.getValue();
            Integer discountId = disc == null ? null : Integer.parseInt(disc[0]);
            double pct = disc == null ? 0 : Double.parseDouble(disc[2]);
            double discAmt = subtotal * pct / 100.0;
            double finalAmt = subtotal - discAmt;
            String[] cust = customerCb.getValue();
            Integer customerId = cust == null ? null : Integer.parseInt(cust[0]);
            String customerName = cust == null ? "Walk-in Customer" : cust[1];
            String discountCode = disc == null ? null : disc[1];

            try {
                int saleId = saveFullSale(finalBranchId, customerId, cashierUserId, discountId,
                        subtotal, discAmt, finalAmt, cart);
                stage.close();
                openInvoiceWindow(saleId, finalBranchId, cashierUserId, customerName, discountCode, pct, cart, subtotal, discAmt, finalAmt);
            } catch (Exception ex) {
                ex.printStackTrace();
                message.setText(ex.getMessage());
            }
        });

        HBox actionRow = new HBox(12, saveBtn, cancelBtn, message);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        VBox root = new VBox(16,
                header,
                createDivider("Add Item"),
                pickerRow,
                createDivider("Cart"),
                cartTable,
                createDivider("Customer & Discount"),
                metaRow,
                totalsBox,
                actionRow
        );
        root.setPadding(new Insets(26));
        root.setStyle("-fx-background-color:#FFF7F2;");

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent; -fx-background-color:transparent;");

        stage.setScene(new Scene(sp, 860, 760));
        stage.show();
    }

    /** Persists a full multi-line sale (sale header + sale_items + inventory decrement + cash movement) in one transaction. */
    private int saveFullSale(int branchId, Integer customerId, int cashierUserId, Integer discountId,
                              double subtotal, double discountAmount, double finalAmount,
                              List<PosCartLine> lines) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();
        if (con == null) throw new Exception("No database connection.");
        con.setAutoCommit(false);
        try {
            PreparedStatement salePs = con.prepareStatement(
                    "INSERT INTO sales(BranchID, CustomerID, CashierUserID, DiscountID, SaleDate, TotalAmount, DiscountAmount, FinalAmount) " +
                            "VALUES (?, ?, ?, ?, CURDATE(), ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            salePs.setInt(1, branchId);
            if (customerId == null) salePs.setNull(2, Types.INTEGER); else salePs.setInt(2, customerId);
            salePs.setInt(3, cashierUserId);
            if (discountId == null) salePs.setNull(4, Types.INTEGER); else salePs.setInt(4, discountId);
            salePs.setDouble(5, subtotal);
            salePs.setDouble(6, discountAmount);
            salePs.setDouble(7, finalAmount);
            salePs.executeUpdate();

            ResultSet keys = salePs.getGeneratedKeys();
            keys.next();
            int saleId = keys.getInt(1);

            for (PosCartLine line : lines) {
                PreparedStatement itemPs = con.prepareStatement(
                        "INSERT INTO sale_items(SaleID, ProductID, SizeID, Quantity, UnitPrice) VALUES (?, ?, ?, ?, ?)");
                itemPs.setInt(1, saleId);
                itemPs.setInt(2, line.productId);
                itemPs.setInt(3, line.sizeId);
                itemPs.setInt(4, line.quantity);
                itemPs.setDouble(5, line.unitPrice);
                itemPs.executeUpdate();

                PreparedStatement updateInv = con.prepareStatement(
                        "UPDATE branch_inventory SET Quantity=Quantity-? WHERE BranchID=? AND ProductID=? AND SizeID=?");
                updateInv.setInt(1, line.quantity);
                updateInv.setInt(2, branchId);
                updateInv.setInt(3, line.productId);
                updateInv.setInt(4, line.sizeId);
                updateInv.executeUpdate();
            }

            PreparedStatement cashPs = con.prepareStatement(
                    "INSERT INTO cash_drawer_movements(CashierUserID, BranchID, MovementType, Amount, MovementDate, Notes) " +
                            "VALUES (?, ?, 'SALE', ?, NOW(), ?)");
            cashPs.setInt(1, cashierUserId);
            cashPs.setInt(2, branchId);
            cashPs.setDouble(3, finalAmount);
            cashPs.setString(4, "POS sale invoice #" + saleId);
            cashPs.executeUpdate();

            con.commit();
            return saleId;
        } catch (Exception ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }

    /** Clean, professional, printable invoice window shown right after a sale is completed. */
    private void openInvoiceWindow(int saleId, int branchId, int cashierUserId, String customerName,
                                    String discountCode, double discountPct,
                                    List<PosCartLine> lines, double subtotal, double discountAmount, double finalAmount) {
        Stage stage = new Stage();
        stage.setTitle("Invoice #" + saleId);

        String branchName = "Branch", cashierName = "Cashier";
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            ResultSet rs = con.createStatement().executeQuery("SELECT Name FROM branches WHERE BranchID=" + branchId);
            if (rs.next()) branchName = rs.getString("Name");
            ResultSet rs2 = con.createStatement().executeQuery("SELECT FullName FROM users WHERE UserID=" + cashierUserId);
            if (rs2.next()) cashierName = rs2.getString("FullName");
        } catch (Exception ignored) {}

        VBox invoice = new VBox(0);
        invoice.setPadding(new Insets(36, 40, 30, 40));
        invoice.setStyle("-fx-background-color:white;");
        invoice.setPrefWidth(480);

        Label storeName = new Label("Lucerne Boutique");
        storeName.setStyle("-fx-font-size:26px; -fx-font-weight:bold; -fx-text-fill:#5A3E36; -fx-font-family:'Segoe UI';");
        Label storeBranch = new Label(branchName);
        storeBranch.setStyle("-fx-font-size:13px; -fx-text-fill:#8A7570;");
        Label invoiceTag = new Label("S A L E   I N V O I C E");
        invoiceTag.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#C98F7B;");

        VBox headerBox = new VBox(2, storeName, storeBranch, invoiceTag);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(0, 0, 16, 0));

        Separator sep1 = new Separator();

        GridPane meta = new GridPane();
        meta.setHgap(18); meta.setVgap(5);
        meta.setPadding(new Insets(14, 0, 14, 0));
        String today = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy").format(LocalDate.now());
        addInvoiceMetaRow(meta, 0, "Invoice #", "#" + saleId);
        addInvoiceMetaRow(meta, 1, "Date", today);
        addInvoiceMetaRow(meta, 2, "Cashier", cashierName);
        addInvoiceMetaRow(meta, 3, "Customer", customerName);
        if (discountCode != null) addInvoiceMetaRow(meta, 4, "Discount", discountCode + " (-" + (int) discountPct + "%)");

        Separator sep2 = new Separator();

        GridPane itemsHeader = new GridPane();
        itemsHeader.setHgap(8);
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(46);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(14);
        ColumnConstraints col3 = new ColumnConstraints(); col3.setPercentWidth(20); col3.setHalignment(HPos.RIGHT);
        ColumnConstraints col4 = new ColumnConstraints(); col4.setPercentWidth(20); col4.setHalignment(HPos.RIGHT);
        itemsHeader.getColumnConstraints().addAll(col1, col2, col3, col4);
        itemsHeader.add(invoiceColHeader("ITEM"), 0, 0);
        itemsHeader.add(invoiceColHeader("SIZE"), 1, 0);
        itemsHeader.add(invoiceColHeader("PRICE"), 2, 0);
        itemsHeader.add(invoiceColHeader("TOTAL"), 3, 0);
        itemsHeader.setPadding(new Insets(8, 0, 8, 0));
        itemsHeader.setStyle("-fx-background-color:#FBF1EC;");

        VBox itemRows = new VBox(0);
        for (PosCartLine line : lines) {
            GridPane row = new GridPane();
            row.getColumnConstraints().addAll(col1, col2, col3, col4);
            row.setHgap(8);
            row.setPadding(new Insets(8, 0, 8, 0));
            row.add(invoiceCell(line.productLabel), 0, 0);
            row.add(invoiceCell(line.sizeLabel), 1, 0);
            row.add(invoiceCell("₪" + String.format("%.2f", line.unitPrice) + " ×" + line.quantity), 2, 0);
            row.add(invoiceCell("₪" + String.format("%.2f", line.subtotal())), 3, 0);
            row.setStyle("-fx-border-color:transparent transparent #F0E3DC transparent; -fx-border-width:0 0 1 0;");
            itemRows.getChildren().add(row);
        }

        Separator sep3 = new Separator();

        GridPane totals = new GridPane();
        totals.setHgap(8); totals.setVgap(6);
        totals.setPadding(new Insets(14, 0, 0, 0));
        ColumnConstraints tcol1 = new ColumnConstraints(); tcol1.setPercentWidth(70);
        ColumnConstraints tcol2 = new ColumnConstraints(); tcol2.setPercentWidth(30); tcol2.setHalignment(HPos.RIGHT);
        totals.getColumnConstraints().addAll(tcol1, tcol2);
        totals.add(invoiceLabel("Subtotal"), 0, 0); totals.add(invoiceCell("₪" + String.format("%.2f", subtotal)), 1, 0);
        totals.add(invoiceLabel("Discount"), 0, 1); totals.add(invoiceCell("-₪" + String.format("%.2f", discountAmount)), 1, 1);

        Label totalCaption = new Label("TOTAL DUE");
        totalCaption.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");
        Label totalValue = new Label("₪" + String.format("%.2f", finalAmount));
        totalValue.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");
        totals.add(totalCaption, 0, 2);
        totals.add(totalValue, 1, 2);

        Label thanks = new Label("Thank you for shopping at Lucerne Boutique!");
        thanks.setStyle("-fx-font-size:12px; -fx-text-fill:#8A7570; -fx-font-style:italic;");
        thanks.setPadding(new Insets(22, 0, 0, 0));

        invoice.getChildren().addAll(headerBox, sep1, meta, sep2, itemsHeader, itemRows, sep3, totals, thanks);

        Button printBtn = createSuccessButton("🖨️ Print Invoice");
        Button closeBtn = createGhostButton("Close");
        printBtn.setOnAction(e -> {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null && job.showPrintDialog(stage)) {
                boolean ok = job.printPage(invoice);
                if (ok) job.endJob();
            }
        });
        closeBtn.setOnAction(e -> stage.close());

        HBox btnRow = new HBox(12, printBtn, closeBtn);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setPadding(new Insets(16));
        btnRow.setStyle("-fx-background-color:#FFF7F2;");

        ScrollPane scroller = new ScrollPane(invoice);
        scroller.setFitToWidth(true);
        scroller.setStyle("-fx-background-color:white;");

        BorderPane root = new BorderPane(scroller);
        root.setBottom(btnRow);

        stage.setScene(new Scene(root, 540, 700));
        stage.show();
    }

    private void addInvoiceMetaRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-font-size:12px; -fx-text-fill:#8A7570;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#2C1810;");
        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }

    private Label invoiceColHeader(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");
        return l;
    }

    private Label invoiceCell(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:12px; -fx-text-fill:#2C1810;");
        return l;
    }

    private Label invoiceLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:12px; -fx-text-fill:#8A7570;");
        return l;
    }

    private void openCashAdvanceWindow(int cashierUserId) {
        Stage stage = new Stage();

        TextField amountField = new TextField();
        amountField.setPromptText("Amount");

        TextField notesField = new TextField();
        notesField.setPromptText("Notes");

        Label message = new Label();

        Button saveBtn = createSmallButton("Save Advance");

        saveBtn.setOnAction(e -> {
            try {
                int branchId = getCashierBranchId(cashierUserId);
                double amount = Double.parseDouble(amountField.getText());

                Connection con = new DataBaseConnection().getConnection().getConnection();

                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO cash_drawer_movements(CashierUserID, BranchID, MovementType, Amount, MovementDate, Notes) " +
                                "VALUES (?, ?, 'ADVANCE', ?, NOW(), ?)"
                );

                ps.setInt(1, cashierUserId);
                ps.setInt(2, branchId);
                ps.setDouble(3, amount);
                ps.setString(4, notesField.getText());
                ps.executeUpdate();

                message.setText("Advance saved successfully");

            } catch (Exception ex) {
                ex.printStackTrace();
                message.setText(ex.getMessage());
            }
        });

        VBox root = new VBox(12, new Label("Cash Advance"), amountField, notesField, saveBtn, message);
        root.setPadding(new Insets(25));

        stage.setScene(new Scene(root, 400, 330));
        stage.setTitle("Cash Advance");
        stage.show();
    }

    private int getCashierBranchId(int cashierUserId) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();

        PreparedStatement ps = con.prepareStatement("SELECT BranchID FROM employees WHERE UserID=?");
        ps.setInt(1, cashierUserId);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) return rs.getInt("BranchID");

        throw new Exception("Cashier branch not found");
    }

    private int getAvailableQuantity(int branchId, int productId, int sizeId) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();

        PreparedStatement ps = con.prepareStatement(
                "SELECT Quantity FROM branch_inventory WHERE BranchID=? AND ProductID=? AND SizeID=?"
        );

        ps.setInt(1, branchId);
        ps.setInt(2, productId);
        ps.setInt(3, sizeId);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) return rs.getInt("Quantity");

        return 0;
    }

    private double getProductPrice(int productId) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();

        PreparedStatement ps = con.prepareStatement("SELECT Price FROM products WHERE ProductID=?");
        ps.setInt(1, productId);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) return rs.getDouble("Price");

        throw new Exception("Product not found");
    }

    private double getDiscountPercentage(int discountId) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();

        PreparedStatement ps = con.prepareStatement("SELECT Percentage FROM discounts WHERE DiscountID=?");
        ps.setInt(1, discountId);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) return rs.getDouble("Percentage");

        return 0;
    }



    private void showCustomerStorePage(int customerUserId) {
        pageTitle.setText("Online Store");

        VBox page = new VBox(16);
        page.setPadding(new Insets(20));
        page.setStyle("-fx-background-color:#FFF7F2;");

        Label title = new Label("Lucerne Online Store");
        title.setStyle("-fx-font-size:28px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");

        Label subtitle = new Label("Choose a category, browse products, then place an order for store pickup or home delivery.");
        subtitle.setStyle("-fx-font-size:15px; -fx-text-fill:#7A5C52;");

        HBox categoryBar = new HBox(10);
        categoryBar.setAlignment(Pos.CENTER_LEFT);

        Button allBtn = createSmallButton("All");
        Button blousesBtn = createSmallButton("Blouses");
        Button dressesBtn = createSmallButton("Dresses");
        Button pantsBtn = createSmallButton("Pants");
        Button shoesBtn = createSmallButton("Shoes");
        Button abayasBtn = createSmallButton("Abayas");

        allBtn.setOnAction(e -> showAllGallery("Online Store - All Products"));
        blousesBtn.setOnAction(e -> showGallery("Blouses", "Online Store - Blouses"));
        dressesBtn.setOnAction(e -> showGallery("Dresses", "Online Store - Dresses"));
        pantsBtn.setOnAction(e -> showGallery("Pants", "Online Store - Pants"));
        shoesBtn.setOnAction(e -> showGallery("Shoes", "Online Store - Shoes"));
        abayasBtn.setOnAction(e -> showGallery("Abayas", "Online Store - Abayas"));

        categoryBar.getChildren().addAll(allBtn, blousesBtn, dressesBtn, pantsBtn, shoesBtn, abayasBtn);

        VBox orderCard = new VBox(12);
        orderCard.setPadding(new Insets(16));
        orderCard.setStyle("-fx-background-color:white; -fx-background-radius:18; -fx-border-color:#E7CFC4; -fx-border-radius:18;");

        Label orderTitle = new Label("Place Online Order");
        orderTitle.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");

        HBox form1 = new HBox(10);
        form1.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> productBox = new ComboBox<>();
        productBox.setPromptText("Product");
        productBox.setPrefWidth(300);
        loadProductsIntoOnlineCombo(productBox);

        ComboBox<String> sizeBox = new ComboBox<>();
        sizeBox.setPromptText("Size");
        sizeBox.setPrefWidth(150);

        productBox.setOnAction(e -> {
            try {
                loadSizesForOnlineProduct(sizeBox, getIdFromCombo(productBox.getValue()));
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        TextField qtyField = new TextField("1");
        qtyField.setPromptText("Qty");
        qtyField.setPrefWidth(80);

        ComboBox<String> receiveBox = new ComboBox<>();
        receiveBox.getItems().addAll("PICKUP_FROM_STORE", "HOME_DELIVERY");
        receiveBox.setValue("PICKUP_FROM_STORE");
        receiveBox.setPrefWidth(190);

        form1.getChildren().addAll(productBox, sizeBox, qtyField, receiveBox);

        HBox form2 = new HBox(10);
        form2.setAlignment(Pos.CENTER_LEFT);

        TextField addressField = new TextField();
        addressField.setPromptText("Address for home delivery");
        addressField.setPrefWidth(350);

        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone");
        phoneField.setPrefWidth(170);

        Label msg = new Label();
        msg.setStyle("-fx-text-fill:#5A3E36; -fx-font-weight:bold;");

        Button orderBtn = createSmallButton("Place Order");
        orderBtn.setOnAction(e -> {
            try {
                int productId = getIdFromCombo(productBox.getValue());
                int sizeId = getIdFromCombo(sizeBox.getValue());
                int qty = Integer.parseInt(qtyField.getText().trim());
                String receiveMethod = receiveBox.getValue();
                String address = addressField.getText().trim();
                String phone = phoneField.getText().trim();

                if (qty <= 0) {
                    msg.setText("Quantity must be greater than zero.");
                    return;
                }

                if ("HOME_DELIVERY".equals(receiveMethod) && address.isEmpty()) {
                    msg.setText("Please enter delivery address.");
                    return;
                }

                int orderId = createOnlineOrder(customerUserId, productId, sizeId, qty, receiveMethod, address, phone);
                msg.setText("Order created successfully. Order ID = " + orderId);

            } catch (Exception ex) {
                ex.printStackTrace();
                msg.setText(ex.getMessage());
            }
        });

        form2.getChildren().addAll(addressField, phoneField, orderBtn);

        orderCard.getChildren().addAll(orderTitle, form1, form2, msg);

        Label instruction = new Label("Browse products using the category buttons above. Product cards will open in the same customer account.");
        instruction.setStyle("-fx-text-fill:#7A5C52; -fx-font-size:14px;");

        page.getChildren().addAll(title, subtitle, categoryBar, orderCard, instruction);

        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#FFF7F2;");
        VBox.setVgrow(sp, Priority.ALWAYS);

        contentBox.getChildren().set(2, sp);
    }


    // =========================
    // ONLINE STORE PAGE
    // =========================
    private void showOnlineShopPage(int customerUserId) {
        pageTitle.setText("Shop Online");

        VBox page = new VBox(16);
        page.setPadding(new Insets(20));
        page.setStyle("-fx-background-color:#FFF7F2;");

        Label title = new Label("Online Store - Buy from Store");
        title.setStyle("-fx-font-size:26px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");

        Label subtitle = new Label("Choose product, size, delivery method, then place an order.");
        subtitle.setStyle("-fx-font-size:15px; -fx-text-fill:#7A5C52;");

        HBox form = new HBox(12);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPadding(new Insets(15));
        form.setStyle("-fx-background-color:white; -fx-background-radius:18; -fx-border-color:#E7CFC4; -fx-border-radius:18;");

        ComboBox<String> productBox = new ComboBox<>();
        productBox.setPromptText("Product");
        productBox.setPrefWidth(260);
        loadProductsIntoOnlineCombo(productBox);

        ComboBox<String> sizeBox = new ComboBox<>();
        sizeBox.setPromptText("Size");
        sizeBox.setPrefWidth(110);

        productBox.setOnAction(e -> {
            try {
                loadSizesForOnlineProduct(sizeBox, getIdFromCombo(productBox.getValue()));
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        TextField qtyField = new TextField("1");
        qtyField.setPromptText("Qty");
        qtyField.setPrefWidth(70);

        ComboBox<String> receiveBox = new ComboBox<>();
        receiveBox.getItems().addAll("PICKUP_FROM_STORE", "HOME_DELIVERY");
        receiveBox.setValue("PICKUP_FROM_STORE");
        receiveBox.setPrefWidth(180);

        TextField addressField = new TextField();
        addressField.setPromptText("Delivery address if home delivery");
        addressField.setPrefWidth(260);

        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone");
        phoneField.setPrefWidth(150);

        Label msg = new Label();
        msg.setStyle("-fx-text-fill:#5A3E36; -fx-font-weight:bold;");

        Button orderBtn = createSmallButton("Place Order");
        orderBtn.setOnAction(e -> {
            try {
                int productId = getIdFromCombo(productBox.getValue());
                int sizeId = getIdFromCombo(sizeBox.getValue());
                int qty = Integer.parseInt(qtyField.getText().trim());
                String receiveMethod = receiveBox.getValue();
                String address = addressField.getText().trim();
                String phone = phoneField.getText().trim();

                if (qty <= 0) {
                    msg.setText("Quantity must be greater than zero.");
                    return;
                }

                if ("HOME_DELIVERY".equals(receiveMethod) && address.isEmpty()) {
                    msg.setText("Please enter delivery address.");
                    return;
                }

                int orderId = createOnlineOrder(customerUserId, productId, sizeId, qty, receiveMethod, address, phone);
                msg.setText("Online order created successfully. Order ID = " + orderId);
                showCustomerOnlineOrders(customerUserId);

            } catch (Exception ex) {
                ex.printStackTrace();
                msg.setText(ex.getMessage());
            }
        });

        form.getChildren().addAll(productBox, sizeBox, qtyField, receiveBox, addressField, phoneField, orderBtn);

        Label productsTitle = new Label("Available Products");
        productsTitle.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");

        Label note = new Label("Products are shown in the customer side. Use the product and size boxes above to place an online order.");
        note.setStyle("-fx-font-size:14px; -fx-text-fill:#7A5C52;");

        Button showProductsBtn = createSmallButton("Show Product Cards");
        showProductsBtn.setOnAction(e -> showAllGallery("Available Products"));

        page.getChildren().addAll(title, subtitle, form, msg, productsTitle, note, showProductsBtn);

        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#FFF7F2;");
        VBox.setVgrow(sp, Priority.ALWAYS);
        contentBox.getChildren().set(2, sp);
    }

    private int createOnlineOrder(int customerUserId, int productId, int sizeId, int qty, String receiveMethod, String address, String phone) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();
        if (con == null) throw new Exception("No database connection.");

        int customerId = getCustomerIdByUserId(customerUserId);
        double price = getProductPrice(productId);
        double deliveryFee = "HOME_DELIVERY".equals(receiveMethod) ? 20.0 : 0.0;
        double total = (price * qty) + deliveryFee;

        con.setAutoCommit(false);

        try {
            PreparedStatement orderPs = con.prepareStatement(
                    "INSERT INTO online_orders(CustomerID, OrderDate, ReceiveMethod, DeliveryAddress, Phone, Status, DeliveryFee, TotalAmount) " +
                            "VALUES (?, NOW(), ?, ?, ?, 'PENDING', ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            orderPs.setInt(1, customerId);
            orderPs.setString(2, receiveMethod);
            orderPs.setString(3, address);
            orderPs.setString(4, phone);
            orderPs.setDouble(5, deliveryFee);
            orderPs.setDouble(6, total);
            orderPs.executeUpdate();

            ResultSet keys = orderPs.getGeneratedKeys();
            keys.next();
            int orderId = keys.getInt(1);

            PreparedStatement itemPs = con.prepareStatement(
                    "INSERT INTO online_order_items(OnlineOrderID, ProductID, SizeID, Quantity, UnitPrice) VALUES (?, ?, ?, ?, ?)"
            );
            itemPs.setInt(1, orderId);
            itemPs.setInt(2, productId);
            itemPs.setInt(3, sizeId);
            itemPs.setInt(4, qty);
            itemPs.setDouble(5, price);
            itemPs.executeUpdate();

            PreparedStatement audit = con.prepareStatement(
                    "INSERT INTO audit_logs(UserID, ActionType, Details, LogDate) VALUES (?, 'ONLINE_ORDER_CREATED', ?, NOW())"
            );
            audit.setInt(1, customerUserId);
            audit.setString(2, "Online order #" + orderId + " created");
            audit.executeUpdate();

            con.commit();
            return orderId;
        } catch (Exception ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }

    private void showCustomerOnlineOrders(int customerUserId) {
        pageTitle.setText("My Online Orders");

        try {
            int customerId = getCustomerIdByUserId(customerUserId);

            showTable("SELECT oo.OnlineOrderID, oo.OrderDate, oo.ReceiveMethod, oo.DeliveryAddress, oo.Phone, oo.Status, oo.DeliveryFee, oo.TotalAmount " +
                    "FROM online_orders oo WHERE oo.CustomerID=" + customerId + " ORDER BY oo.OnlineOrderID DESC");
            pageTitle.setText("My Online Orders");

            HBox cancelBox = new HBox(10);
            cancelBox.setAlignment(Pos.CENTER_LEFT);
            cancelBox.setPadding(new Insets(10));
            cancelBox.setStyle("-fx-background-color:white; -fx-background-radius:14; -fx-border-color:#E7CFC4; -fx-border-radius:14;");

            TextField orderIdField = new TextField();
            orderIdField.setPromptText("Order ID to cancel");
            orderIdField.setPrefWidth(170);

            Label note = new Label("You can cancel only if status is PENDING.");
            note.setStyle("-fx-text-fill:#7A5C52; -fx-font-weight:bold;");

            Button cancelBtn = createSmallButton("Cancel My Order");
            cancelBtn.setOnAction(e -> {
                try {
                    int orderId = Integer.parseInt(orderIdField.getText().trim());
                    cancelCustomerOnlineOrder(customerUserId, orderId);
                    showSuccess("Order cancelled successfully.");
                    showCustomerOnlineOrders(customerUserId);
                } catch (Exception ex) {
                    showAlert(ex.getMessage());
                }
            });

            cancelBox.getChildren().addAll(new Label("Cancel Order:"), orderIdField, cancelBtn, note);

            if (contentBox.getChildren().size() > 3) {
                contentBox.getChildren().set(3, cancelBox);
            } else {
                contentBox.getChildren().add(cancelBox);
            }

        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    private void cancelCustomerOnlineOrder(int customerUserId, int orderId) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();
        if (con == null) throw new Exception("No database connection.");

        int customerId = getCustomerIdByUserId(customerUserId);

        PreparedStatement check = con.prepareStatement(
                "SELECT Status FROM online_orders WHERE OnlineOrderID=? AND CustomerID=?"
        );
        check.setInt(1, orderId);
        check.setInt(2, customerId);

        ResultSet rs = check.executeQuery();

        if (!rs.next()) {
            throw new Exception("Order not found for this customer.");
        }

        String status = rs.getString("Status");

        if (!"PENDING".equals(status)) {
            throw new Exception("You cannot cancel this order because manager already started processing it. Current status = " + status);
        }

        PreparedStatement update = con.prepareStatement(
                "UPDATE online_orders SET Status='CANCELLED' WHERE OnlineOrderID=? AND CustomerID=?"
        );
        update.setInt(1, orderId);
        update.setInt(2, customerId);
        update.executeUpdate();

        try {
            PreparedStatement audit = con.prepareStatement(
                    "INSERT INTO audit_logs(UserID, ActionType, Details, LogDate) VALUES (?, 'CUSTOMER_CANCEL_ORDER', ?, NOW())"
            );
            audit.setInt(1, customerUserId);
            audit.setString(2, "Customer cancelled online order #" + orderId);
            audit.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private int getCustomerIdByUserId(int userId) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT CustomerID FROM customers WHERE UserID=?");
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt("CustomerID");
        throw new Exception("Customer record not found for this user.");
    }

    private void loadProductsIntoOnlineCombo(ComboBox<String> combo) {
        combo.getItems().clear();
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT ProductID, Name, Color FROM products ORDER BY Category, Name, Color");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                combo.getItems().add(rs.getInt("ProductID") + " - " + rs.getString("Name") + " (" + rs.getString("Color") + ")");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadSizesForOnlineProduct(ComboBox<String> combo, int productId) {
        combo.getItems().clear();

        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            PreparedStatement ps = con.prepareStatement(
                    "SELECT ps.SizeID, ps.SizeValue, IFNULL(SUM(bi.Quantity),0) AS AvailableQty " +
                            "FROM product_sizes ps " +
                            "LEFT JOIN branch_inventory bi ON ps.ProductID=bi.ProductID AND ps.SizeID=bi.SizeID " +
                            "WHERE ps.ProductID=? " +
                            "GROUP BY ps.SizeID, ps.SizeValue " +
                            "HAVING AvailableQty > 0 " +
                            "ORDER BY CASE ps.SizeValue WHEN 'S' THEN 1 WHEN 'M' THEN 2 WHEN 'L' THEN 3 WHEN 'XL' THEN 4 ELSE 5 END, ps.SizeValue"
            );
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                combo.getItems().add(rs.getInt("SizeID") + " - " + rs.getString("SizeValue") + " / Qty " + rs.getInt("AvailableQty"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private int getIdFromCombo(String text) throws Exception {
        if (text == null || text.trim().isEmpty()) throw new Exception("Please select a value.");
        return Integer.parseInt(text.split(" - ")[0].trim());
    }



    // =========================
    // MEGA PROFESSIONAL FEATURES
    // =========================




    // =========================================================
    // WHATSAPP CONTACT
    // Opens WhatsApp Web/Desktop with a ready order message.
    // The user presses Send manually; no paid API is required.
    // =========================================================

    private void openWhatsAppForOrder(int orderId) {
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            if (con == null) {
                showAlert("No database connection.");
                return;
            }

            PreparedStatement ps = con.prepareStatement(
                    "SELECT u.FullName AS CustomerName, " +
                            "IFNULL(oo.Phone, IFNULL(c.Phone, '')) AS Phone, " +
                            "oo.TotalAmount " +
                            "FROM online_orders oo " +
                            "JOIN customers c ON oo.CustomerID=c.CustomerID " +
                            "JOIN users u ON c.UserID=u.UserID " +
                            "WHERE oo.OnlineOrderID=?"
            );
            ps.setInt(1, orderId);

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                showAlert("Order not found.");
                return;
            }

            openWhatsAppContact(
                    rs.getString("Phone"),
                    rs.getString("CustomerName"),
                    orderId,
                    rs.getDouble("TotalAmount")
            );

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Could not open WhatsApp: " + ex.getMessage());
        }
    }

    private void openWhatsAppContact(
            String customerPhone,
            String customerName,
            int orderId,
            double totalAmount
    ) {
        try {
            String phone = normalizeWhatsAppPhone(customerPhone);

            String message =
                    "مرحباً " + customerName + "\n\n" +
                            "تم استلام طلبك بنجاح ✅\n\n" +
                            "رقم الطلب: #" + orderId + "\n" +
                            "الإجمالي: ₪" + String.format(java.util.Locale.US, "%.2f", totalAmount) + "\n" +
                            "الحالة: قيد المراجعة\n\n" +
                            "Lucerne Boutique";

            String encodedMessage = URLEncoder
                    .encode(message, StandardCharsets.UTF_8)
                    .replace("+", "%20");

            String whatsappUrl =
                    "https://wa.me/" + phone + "?text=" + encodedMessage;

            if (!Desktop.isDesktopSupported()) {
                showAlert("WhatsApp cannot be opened on this device.");
                return;
            }

            Desktop.getDesktop().browse(new URI(whatsappUrl));

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Could not open WhatsApp: " + ex.getMessage());
        }
    }

    private String normalizeWhatsAppPhone(String phone) throws Exception {
        if (phone == null || phone.trim().isEmpty()) {
            throw new Exception("Customer phone number is missing.");
        }

        String cleanedPhone = phone.replaceAll("[^0-9]", "");

        if (cleanedPhone.startsWith("00")) {
            cleanedPhone = cleanedPhone.substring(2);
        } else if (cleanedPhone.startsWith("0")) {
            cleanedPhone = "970" + cleanedPhone.substring(1);
        }

        if (cleanedPhone.isEmpty()) {
            throw new Exception("Invalid customer phone number.");
        }

        return cleanedPhone;
    }

    private void showAdminOnlineOrders() {
        ensureOnlineOrderTablesSafe();

        showTable("SELECT oo.OnlineOrderID, " +
                "u.FullName AS Customer, " +
                "IFNULL(oo.Phone, IFNULL(c.Phone, '-')) AS Phone, " +
                "oo.ReceiveMethod, " +
                "IFNULL(oo.DeliveryArea, '-') AS DeliveryArea, " +
                "IFNULL(oo.DeliveryAddress, '-') AS DeliveryAddress, " +
                "IFNULL(oo.PaymentMethod, '-') AS PaymentMethod, " +
                "oo.Status, " +
                "oo.DeliveryFee, " +
                "oo.TotalAmount, " +
                "IFNULL(oo.RejectReason, '-') AS RejectReason " +
                "FROM online_orders oo " +
                "JOIN customers c ON oo.CustomerID = c.CustomerID " +
                "JOIN users u ON c.UserID = u.UserID " +
                "ORDER BY oo.OnlineOrderID DESC");

        pageTitle.setText("Online Orders");
        addAdminOrderAcceptContactButtons();
    }

    private void addAdminOrderAcceptContactButtons() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color:white; -fx-background-radius:14; -fx-border-color:#E7CFC4; -fx-border-radius:14;");

        TextField orderIdField = new TextField();
        orderIdField.setPromptText("OnlineOrderID");
        orderIdField.setPrefWidth(140);

        Button acceptBtn = createSmallButton("Accept Order");
        acceptBtn.setOnAction(e -> {
            try {
                int orderId = Integer.parseInt(orderIdField.getText().trim());
                acceptOnlineOrder(orderId);
                showSuccess("Order accepted successfully. Customer can no longer cancel it.");
                showAdminOnlineOrders();
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        Button preparingBtn = createSmallButton("Preparing");
        preparingBtn.setOnAction(e -> {
            try {
                int orderId = Integer.parseInt(orderIdField.getText().trim());
                updateOnlineOrderSimpleStatus(orderId, "PREPARING", "Order is being prepared");
                showSuccess("Order moved to PREPARING.");
                showAdminOnlineOrders();
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        Button contactBtn = createSmallButton("Contact Customer");
        contactBtn.setOnAction(e -> {
            try {
                int orderId = Integer.parseInt(orderIdField.getText().trim());
                openContactCustomerWindow(orderId);
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        Button whatsappBtn = createSmallButton("WhatsApp");
        whatsappBtn.setStyle(
                "-fx-background-color:#25D366;" +
                        "-fx-text-fill:white;" +
                        "-fx-font-weight:bold;" +
                        "-fx-background-radius:12;" +
                        "-fx-padding:9 16;"
        );
        whatsappBtn.setOnAction(e -> {
            try {
                int orderId = Integer.parseInt(orderIdField.getText().trim());
                openWhatsAppForOrder(orderId);
            } catch (Exception ex) {
                showAlert("Enter a valid OnlineOrderID.");
            }
        });

        Button detailsBtn = createSmallButton("Order Items");
        detailsBtn.setOnAction(e -> {
            try {
                int orderId = Integer.parseInt(orderIdField.getText().trim());
                openOrderDetailsPopup(orderId);
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        Label note = new Label("Accept = manager saw the order and started processing.");
        note.setStyle("-fx-text-fill:#7A5C52; -fx-font-weight:bold;");

        box.getChildren().addAll(new Label("Order ID:"), orderIdField, acceptBtn, preparingBtn, contactBtn, whatsappBtn, detailsBtn, note);

        if (contentBox.getChildren().size() > 3) {
            contentBox.getChildren().set(3, box);
        } else {
            contentBox.getChildren().add(box);
        }
    }

    private void acceptOnlineOrder(int orderId) throws Exception {
        updateOnlineOrderSimpleStatus(orderId, "ACCEPTED", "Manager accepted the order and will contact/prepare it");
    }

    private void updateOnlineOrderSimpleStatus(int orderId, String newStatus, String notes) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();
        if (con == null) throw new Exception("No database connection.");

        String oldStatus = "UNKNOWN";
        PreparedStatement check = con.prepareStatement("SELECT Status FROM online_orders WHERE OnlineOrderID=?");
        check.setInt(1, orderId);
        ResultSet rs = check.executeQuery();
        if (rs.next()) oldStatus = rs.getString("Status");
        else throw new Exception("Order not found.");

        PreparedStatement ps = con.prepareStatement("UPDATE online_orders SET Status=? WHERE OnlineOrderID=?");
        ps.setString(1, newStatus);
        ps.setInt(2, orderId);
        ps.executeUpdate();

        try {
            PreparedStatement hist = con.prepareStatement(
                    "INSERT INTO order_status_history(OnlineOrderID, OldStatus, NewStatus, ChangedBy, ChangedAt, Notes) VALUES (?, ?, ?, ?, NOW(), ?)"
            );
            hist.setInt(1, orderId);
            hist.setString(2, oldStatus);
            hist.setString(3, newStatus);
            hist.setInt(4, currentUserId);
            hist.setString(5, notes);
            hist.executeUpdate();
        } catch (Exception ignored) {
        }

        try {
            PreparedStatement timeline = con.prepareStatement(
                    "INSERT INTO order_timeline(OnlineOrderID, Status, Notes, CreatedAt) VALUES (?, ?, ?, NOW())"
            );
            timeline.setInt(1, orderId);
            timeline.setString(2, newStatus);
            timeline.setString(3, notes);
            timeline.executeUpdate();
        } catch (Exception ignored) {
        }

        try {
            PreparedStatement noti = con.prepareStatement(
                    "INSERT INTO notifications(UserID, Message, IsRead, CreatedAt) " +
                            "SELECT c.UserID, ?, 0, NOW() FROM online_orders oo JOIN customers c ON oo.CustomerID=c.CustomerID WHERE oo.OnlineOrderID=?"
            );
            noti.setString(1, "Your order #" + orderId + " is now " + newStatus);
            noti.setInt(2, orderId);
            noti.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private void openContactCustomerWindow(int orderId) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();

        PreparedStatement ps = con.prepareStatement(
                "SELECT oo.OnlineOrderID, u.FullName AS CustomerName, IFNULL(oo.Phone, IFNULL(c.Phone, '-')) AS Phone, " +
                        "oo.ReceiveMethod, IFNULL(oo.DeliveryAddress, '-') AS DeliveryAddress, IFNULL(oo.DeliveryArea, '-') AS DeliveryArea, " +
                        "oo.Status, oo.TotalAmount " +
                        "FROM online_orders oo JOIN customers c ON oo.CustomerID=c.CustomerID JOIN users u ON c.UserID=u.UserID " +
                        "WHERE oo.OnlineOrderID=?"
        );
        ps.setInt(1, orderId);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) throw new Exception("Order not found.");

        String phone = rs.getString("Phone");
        String customer = rs.getString("CustomerName");
        String message =
                "Hello " + customer + ", your Lucerne Boutique order #" + orderId +
                        " is now accepted and we are preparing it. " +
                        "Status: " + rs.getString("Status") +
                        ". Total: " + rs.getDouble("TotalAmount") + ".";

        Stage stage = new Stage();
        stage.setTitle("Contact Customer");

        Label title = new Label("Contact Customer");
        title.setStyle("-fx-font-size:24px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");

        TextField phoneField = new TextField(phone);
        phoneField.setEditable(false);

        TextArea messageArea = new TextArea(message);
        messageArea.setWrapText(true);
        messageArea.setPrefHeight(140);

        Label info = new Label(
                "Customer: " + customer +
                        "\nReceive Method: " + rs.getString("ReceiveMethod") +
                        "\nArea: " + rs.getString("DeliveryArea") +
                        "\nAddress: " + rs.getString("DeliveryAddress")
        );
        info.setStyle("-fx-text-fill:#7A5C52; -fx-font-size:14px;");

        Button closeBtn = createSmallButton("Close");
        closeBtn.setOnAction(e -> stage.close());

        VBox root = new VBox(12,
                title,
                new Label("Phone:"), phoneField,
                new Label("Message to send/copy:"), messageArea,
                info,
                closeBtn
        );
        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color:#FFF7F2;");

        stage.setScene(new Scene(root, 520, 460));
        stage.show();
    }

    private void showOrderItemsForAdmin(int orderId) {
        showTable("SELECT oo.OnlineOrderID, p.Name AS Product, p.Color, ps.SizeValue AS Size, ooi.Quantity, ooi.UnitPrice, " +
                "(ooi.Quantity * ooi.UnitPrice) AS LineTotal " +
                "FROM online_order_items ooi " +
                "JOIN online_orders oo ON ooi.OnlineOrderID=oo.OnlineOrderID " +
                "JOIN products p ON ooi.ProductID=p.ProductID " +
                "JOIN product_sizes ps ON ooi.SizeID=ps.SizeID " +
                "WHERE oo.OnlineOrderID=" + orderId);
        pageTitle.setText("Order Items - #" + orderId);
    }

    private void ensureOnlineOrderTablesSafe() {
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            if (con == null) return;
            Statement st = con.createStatement();

            st.executeUpdate("CREATE TABLE IF NOT EXISTS order_timeline (TimelineID INT PRIMARY KEY AUTO_INCREMENT, OnlineOrderID INT NOT NULL, Status VARCHAR(80) NOT NULL, Notes TEXT, CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS order_status_history (HistoryID INT PRIMARY KEY AUTO_INCREMENT, OnlineOrderID INT NOT NULL, OldStatus VARCHAR(50), NewStatus VARCHAR(50), ChangedBy INT, ChangedAt DATETIME DEFAULT CURRENT_TIMESTAMP, Notes TEXT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS notifications (NotificationID INT PRIMARY KEY AUTO_INCREMENT, UserID INT NULL, Message TEXT NOT NULL, IsRead BOOLEAN DEFAULT 0, CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP)");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }




    private void showDeliveryManagement() {
        ensureFullUpgradeTablesExist();

        showTable("SELECT oo.OnlineOrderID, " +
                "u.FullName AS Customer, " +
                "IFNULL(oo.Phone, IFNULL(c.Phone, '-')) AS Phone, " +
                "oo.ReceiveMethod, " +
                "IFNULL(oo.DeliveryArea, '-') AS DeliveryArea, " +
                "IFNULL(oo.DeliveryAddress, '-') AS DeliveryAddress, " +
                "IFNULL(oo.PaymentMethod, '-') AS PaymentMethod, " +
                "oo.Status, " +
                "oo.DeliveryFee, " +
                "oo.TotalAmount " +
                "FROM online_orders oo " +
                "JOIN customers c ON oo.CustomerID = c.CustomerID " +
                "JOIN users u ON c.UserID = u.UserID " +
                "WHERE oo.ReceiveMethod = 'HOME_DELIVERY' " +
                "ORDER BY oo.OnlineOrderID DESC");

        pageTitle.setText("Delivery Management");
        addOnlineOrderActionButtons("HOME_DELIVERY");
    }

    private void showPickupOrders() {
        showTable("SELECT oo.OnlineOrderID, u.FullName AS CustomerName, oo.Phone, oo.OrderDate, oo.Status, oo.TotalAmount " +
                "FROM online_orders oo JOIN customers c ON oo.CustomerID=c.CustomerID JOIN users u ON c.UserID=u.UserID " +
                "WHERE oo.ReceiveMethod='PICKUP_FROM_STORE' ORDER BY oo.OnlineOrderID DESC");
        pageTitle.setText("Pickup Orders");
        addOnlineOrderActionButtons("PICKUP_FROM_STORE");
    }

    private void addOnlineOrderActionButtons(String mode) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);

        TextField orderIdField = new TextField();
        orderIdField.setPromptText("OnlineOrderID");
        orderIdField.setPrefWidth(140);

        Button confirm = createSmallButton("Confirm");
        confirm.setOnAction(e -> updateOnlineOrderStatusFromField(orderIdField, "CONFIRMED", true));

        Button ready = createSmallButton("Ready");
        ready.setOnAction(e -> updateOnlineOrderStatusFromField(orderIdField, "READY_FOR_PICKUP", false));

        Button out = createSmallButton("Out Delivery");
        out.setOnAction(e -> updateOnlineOrderStatusFromField(orderIdField, "OUT_FOR_DELIVERY", false));

        Button delivered = createSmallButton("Delivered");
        delivered.setOnAction(e -> updateOnlineOrderStatusFromField(orderIdField, "DELIVERED", false));

        Button cancel = createSmallButton("Cancel");
        cancel.setOnAction(e -> updateOnlineOrderStatusFromField(orderIdField, "CANCELLED", false));

        Button print = createSmallButton("Print Invoice");
        print.setOnAction(e -> {
            try {
                int orderId = Integer.parseInt(orderIdField.getText().trim());
                printOnlineInvoice(orderId);
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        Label managerNote = new Label("Confirm = manager started processing; customer cannot cancel after that.");
        managerNote.setStyle("-fx-text-fill:#7A5C52; -fx-font-weight:bold;");
        box.getChildren().addAll(new Label("Order ID:"), orderIdField, confirm, ready, out, delivered, cancel, print, managerNote);

        if (contentBox.getChildren().size() > 3) {
            contentBox.getChildren().set(3, box);
        } else {
            contentBox.getChildren().add(box);
        }
    }

    private void updateOnlineOrderStatusFromField(TextField field, String status, boolean reduceStock) {
        try {
            int orderId = Integer.parseInt(field.getText().trim());
            updateOnlineOrderStatus(orderId, status, reduceStock);
            showSuccess("Order status updated to " + status);
            showAdminOnlineOrders();
        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    private void updateOnlineOrderStatus(int orderId, String status, boolean reduceStock) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();
        if (con == null) throw new Exception("No database connection");

        con.setAutoCommit(false);
        try {
            if (reduceStock) {
                PreparedStatement items = con.prepareStatement(
                        "SELECT ProductID, SizeID, Quantity FROM online_order_items WHERE OnlineOrderID=?"
                );
                items.setInt(1, orderId);
                ResultSet rs = items.executeQuery();

                while (rs.next()) {
                    int productId = rs.getInt("ProductID");
                    int sizeId = rs.getInt("SizeID");
                    int qty = rs.getInt("Quantity");

                    PreparedStatement check = con.prepareStatement(
                            "SELECT BranchID, Quantity FROM branch_inventory WHERE ProductID=? AND SizeID=? AND Quantity>=? ORDER BY Quantity DESC LIMIT 1"
                    );
                    check.setInt(1, productId);
                    check.setInt(2, sizeId);
                    check.setInt(3, qty);
                    ResultSet qrs = check.executeQuery();

                    if (!qrs.next()) {
                        throw new Exception("Not enough stock for product " + productId + " size " + sizeId);
                    }

                    int branchId = qrs.getInt("BranchID");
                    int oldQty = qrs.getInt("Quantity");

                    PreparedStatement updateInv = con.prepareStatement(
                            "UPDATE branch_inventory SET Quantity=Quantity-? WHERE BranchID=? AND ProductID=? AND SizeID=?"
                    );
                    updateInv.setInt(1, qty);
                    updateInv.setInt(2, branchId);
                    updateInv.setInt(3, productId);
                    updateInv.setInt(4, sizeId);
                    updateInv.executeUpdate();

                    PreparedStatement hist = con.prepareStatement(
                            "INSERT INTO stock_history(ProductID, SizeID, BranchID, OldQuantity, NewQuantity, ActionType, UserID, ActionDate, Notes) " +
                                    "VALUES (?, ?, ?, ?, ?, 'ONLINE_ORDER_CONFIRM', ?, NOW(), ?)"
                    );
                    hist.setInt(1, productId);
                    hist.setInt(2, sizeId);
                    hist.setInt(3, branchId);
                    hist.setInt(4, oldQty);
                    hist.setInt(5, oldQty - qty);
                    hist.setInt(6, currentUserId);
                    hist.setString(7, "Online order #" + orderId);
                    hist.executeUpdate();
                }
            }

            PreparedStatement ps = con.prepareStatement("UPDATE online_orders SET Status=? WHERE OnlineOrderID=?");
            ps.setString(1, status);
            ps.setInt(2, orderId);
            ps.executeUpdate();

            try { insertTimeline(con, orderId, status, "Order status changed by manager"); } catch (Exception ignored) {}

            PreparedStatement audit = con.prepareStatement(
                    "INSERT INTO audit_logs(UserID, ActionType, Details, LogDate) VALUES (?, 'ONLINE_ORDER_STATUS', ?, NOW())"
            );
            audit.setInt(1, currentUserId);
            audit.setString(2, "Online order #" + orderId + " status changed to " + status);
            audit.executeUpdate();

            PreparedStatement noti = con.prepareStatement(
                    "INSERT INTO notifications(UserID, Message, IsRead, CreatedAt) " +
                            "SELECT c.UserID, ?, 0, NOW() FROM online_orders oo JOIN customers c ON oo.CustomerID=c.CustomerID WHERE oo.OnlineOrderID=?"
            );
            noti.setString(1, "Your online order #" + orderId + " is now " + status);
            noti.setInt(2, orderId);
            noti.executeUpdate();

            con.commit();
        } catch (Exception ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }

    private void printOnlineInvoice(int orderId) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();

        PreparedStatement ps = con.prepareStatement(
                "SELECT oo.OnlineOrderID, oo.OrderDate, oo.ReceiveMethod, oo.DeliveryAddress, oo.Phone, oo.Status, oo.DeliveryFee, oo.TotalAmount, " +
                        "u.FullName AS CustomerName " +
                        "FROM online_orders oo JOIN customers c ON oo.CustomerID=c.CustomerID JOIN users u ON c.UserID=u.UserID " +
                        "WHERE oo.OnlineOrderID=?"
        );
        ps.setInt(1, orderId);
        ResultSet rs = ps.executeQuery();
        if (!rs.next()) throw new Exception("Order not found");

        StringBuilder sb = new StringBuilder();
        sb.append("Lucerne Boutique\n");
        sb.append("Online Invoice #").append(orderId).append("\n");
        sb.append("Date: ").append(rs.getString("OrderDate")).append("\n");
        sb.append("Customer: ").append(rs.getString("CustomerName")).append("\n");
        sb.append("Method: ").append(rs.getString("ReceiveMethod")).append("\n");
        sb.append("Phone: ").append(rs.getString("Phone")).append("\n");
        sb.append("Address: ").append(rs.getString("DeliveryAddress")).append("\n");
        sb.append("Status: ").append(rs.getString("Status")).append("\n\n");
        sb.append("Items:\n");

        PreparedStatement itemPs = con.prepareStatement(
                "SELECT p.Name, ps.SizeValue, ooi.Quantity, ooi.UnitPrice, (ooi.Quantity*ooi.UnitPrice) AS LineTotal " +
                        "FROM online_order_items ooi JOIN products p ON ooi.ProductID=p.ProductID JOIN product_sizes ps ON ooi.SizeID=ps.SizeID " +
                        "WHERE ooi.OnlineOrderID=?"
        );
        itemPs.setInt(1, orderId);
        ResultSet irs = itemPs.executeQuery();

        while (irs.next()) {
            sb.append(irs.getString("Name"))
                    .append(" | Size ").append(irs.getString("SizeValue"))
                    .append(" | Qty ").append(irs.getInt("Quantity"))
                    .append(" | Price ").append(irs.getDouble("UnitPrice"))
                    .append(" | Total ").append(irs.getDouble("LineTotal"))
                    .append("\n");
        }

        sb.append("\nDelivery Fee: ").append(rs.getDouble("DeliveryFee")).append("\n");
        sb.append("Final Total: ").append(rs.getDouble("TotalAmount")).append("\n");
        sb.append("\nThank you for shopping with Lucerne Boutique.");

        TextArea invoiceArea = new TextArea(sb.toString());
        invoiceArea.setWrapText(true);
        invoiceArea.setPrefSize(520, 520);

        Button printBtn = createSmallButton("Print");
        Stage stage = new Stage();
        printBtn.setOnAction(e -> {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null && job.showPrintDialog(stage)) {
                boolean success = job.printPage(invoiceArea);
                if (success) job.endJob();
            }
        });

        VBox root = new VBox(12, new Label("Invoice Preview"), invoiceArea, printBtn);
        root.setPadding(new Insets(20));
        stage.setScene(new Scene(root, 580, 650));
        stage.setTitle("Invoice");
        stage.show();
    }

    private void showNotificationsPage(int userId) {
        showTable("SELECT NotificationID, Message, IsRead, CreatedAt FROM notifications WHERE UserID=" + userId + " OR UserID IS NULL ORDER BY NotificationID DESC");
        pageTitle.setText("Notifications");
    }

    private void showAdminWishlistReviews() {
        showTable("SELECT 'Wishlist' AS Type, u.FullName AS Customer, p.Name AS Product, p.Color, w.CreatedAt AS DateInfo " +
                "FROM wishlist w JOIN customers c ON w.CustomerID=c.CustomerID JOIN users u ON c.UserID=u.UserID JOIN products p ON w.ProductID=p.ProductID " +
                "UNION ALL " +
                "SELECT 'Review' AS Type, u.FullName AS Customer, p.Name AS Product, p.Color, CONCAT('Rating ', r.Rating, ': ', r.ReviewText) AS DateInfo " +
                "FROM product_reviews r JOIN customers c ON r.CustomerID=c.CustomerID JOIN users u ON c.UserID=u.UserID JOIN products p ON r.ProductID=p.ProductID");
        pageTitle.setText("Wishlist / Reviews");
    }

    private void showMyFavorites(int userId) {
        try {
            int customerId = getCustomerIdByUserId(userId);
            showTable("SELECT w.WishlistID, p.Name, p.Category, p.Color, p.Price, w.CreatedAt FROM wishlist w JOIN products p ON w.ProductID=p.ProductID WHERE w.CustomerID=" + customerId + " ORDER BY w.WishlistID DESC");
            pageTitle.setText("My Favorites");
        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    private void showProductReviewsPage(int userId) {
        Stage stage = new Stage();
        stage.setTitle("Product Reviews");

        ComboBox<String> productBox = new ComboBox<>();
        productBox.setPromptText("Product");
        productBox.setPrefWidth(320);
        loadProductsIntoOnlineCombo(productBox);

        ComboBox<Integer> ratingBox = new ComboBox<>();
        ratingBox.getItems().addAll(1, 2, 3, 4, 5);
        ratingBox.setValue(5);

        TextArea reviewArea = new TextArea();
        reviewArea.setPromptText("Write review");
        reviewArea.setPrefHeight(120);

        Label msg = new Label();

        Button save = createSmallButton("Save Review");
        save.setOnAction(e -> {
            try {
                int productId = getIdFromCombo(productBox.getValue());
                int customerId = getCustomerIdByUserId(userId);
                Connection con = new DataBaseConnection().getConnection().getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO product_reviews(CustomerID, ProductID, Rating, ReviewText, ReviewDate) VALUES (?, ?, ?, ?, NOW())"
                );
                ps.setInt(1, customerId);
                ps.setInt(2, productId);
                ps.setInt(3, ratingBox.getValue());
                ps.setString(4, reviewArea.getText());
                ps.executeUpdate();
                msg.setText("Review saved.");
            } catch (Exception ex) {
                msg.setText(ex.getMessage());
            }
        });

        VBox root = new VBox(12, new Label("Add Product Review"), productBox, ratingBox, reviewArea, save, msg);
        root.setPadding(new Insets(20));
        stage.setScene(new Scene(root, 450, 420));
        stage.show();
    }

    private void showAdvancedCouponsPage() {
        showTable("SELECT CouponID, Code, DiscountType, DiscountValue, MinOrderAmount, UsageLimit, UsedCount, StartDate, EndDate, IsActive FROM coupons ORDER BY CouponID DESC");
        pageTitle.setText("Advanced Coupons");
    }

    private void showStockHistoryPage() {
        showTable("SELECT sh.HistoryID, p.Name, ps.SizeValue, b.Name AS Branch, sh.OldQuantity, sh.NewQuantity, sh.ActionType, u.FullName AS UserName, sh.ActionDate, sh.Notes " +
                "FROM stock_history sh JOIN products p ON sh.ProductID=p.ProductID JOIN product_sizes ps ON sh.SizeID=ps.SizeID " +
                "LEFT JOIN branches b ON sh.BranchID=b.BranchID LEFT JOIN users u ON sh.UserID=u.UserID ORDER BY sh.HistoryID DESC");
        pageTitle.setText("Stock History");
    }

    private void showBarcodeQRPage() {
        showTable("SELECT ProductID, Name, Category, Color, CONCAT('PRD-', ProductID) AS Barcode, CONCAT('INV-', ProductID, '-', Color) AS QRCodeText FROM products ORDER BY ProductID");
        pageTitle.setText("Barcode / QR");
    }

    private void backupDatabaseCSV() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Backup CSV");
            chooser.setInitialFileName("lucerne_backup_products.csv");
            File file = chooser.showSaveDialog(null);
            if (file == null) return;

            Connection con = new DataBaseConnection().getConnection().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT ProductID, Name, Category, Color, Price, CostPrice, ImagePath FROM products ORDER BY ProductID");
            ResultSet rs = ps.executeQuery();

            FileWriter fw = new FileWriter(file);
            fw.write("ProductID,Name,Category,Color,Price,CostPrice,ImagePath\n");

            while (rs.next()) {
                fw.write(rs.getInt("ProductID") + "," +
                        safeCsv(rs.getString("Name")) + "," +
                        safeCsv(rs.getString("Category")) + "," +
                        safeCsv(rs.getString("Color")) + "," +
                        rs.getDouble("Price") + "," +
                        rs.getDouble("CostPrice") + "," +
                        safeCsv(rs.getString("ImagePath")) + "\n");
            }

            fw.close();
            showSuccess("Backup saved successfully.");
        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    private String safeCsv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }



    private void ensureMegaTablesExist() {
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            if (con == null) return;

            Statement st = con.createStatement();

            st.executeUpdate("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "LogID INT PRIMARY KEY AUTO_INCREMENT, " +
                    "UserID INT NULL, " +
                    "ActionType VARCHAR(100) NOT NULL, " +
                    "Details TEXT, " +
                    "LogDate DATETIME DEFAULT CURRENT_TIMESTAMP)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS notifications (" +
                    "NotificationID INT PRIMARY KEY AUTO_INCREMENT, " +
                    "UserID INT NULL, " +
                    "Message TEXT NOT NULL, " +
                    "IsRead BOOLEAN DEFAULT 0, " +
                    "CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS online_orders (" +
                    "OnlineOrderID INT PRIMARY KEY AUTO_INCREMENT, " +
                    "CustomerID INT NOT NULL, " +
                    "OrderDate DATETIME NOT NULL, " +
                    "ReceiveMethod ENUM('PICKUP_FROM_STORE','HOME_DELIVERY') NOT NULL, " +
                    "DeliveryAddress VARCHAR(255), " +
                    "Phone VARCHAR(30), " +
                    "Status ENUM('PENDING','CONFIRMED','READY_FOR_PICKUP','OUT_FOR_DELIVERY','DELIVERED','CANCELLED') DEFAULT 'PENDING', " +
                    "DeliveryFee DOUBLE DEFAULT 0, " +
                    "TotalAmount DOUBLE NOT NULL)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS online_order_items (" +
                    "OnlineOrderItemID INT PRIMARY KEY AUTO_INCREMENT, " +
                    "OnlineOrderID INT NOT NULL, " +
                    "ProductID INT NOT NULL, " +
                    "SizeID INT NOT NULL, " +
                    "Quantity INT NOT NULL, " +
                    "UnitPrice DOUBLE NOT NULL)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS wishlist (" +
                    "WishlistID INT PRIMARY KEY AUTO_INCREMENT, " +
                    "CustomerID INT NOT NULL, " +
                    "ProductID INT NOT NULL, " +
                    "CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(CustomerID, ProductID))");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS product_reviews (" +
                    "ReviewID INT PRIMARY KEY AUTO_INCREMENT, " +
                    "CustomerID INT NOT NULL, " +
                    "ProductID INT NOT NULL, " +
                    "Rating INT NOT NULL, " +
                    "ReviewText TEXT, " +
                    "ReviewDate DATETIME DEFAULT CURRENT_TIMESTAMP)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS coupons (" +
                    "CouponID INT PRIMARY KEY AUTO_INCREMENT, " +
                    "Code VARCHAR(50) NOT NULL UNIQUE, " +
                    "DiscountType ENUM('PERCENT','FIXED') NOT NULL, " +
                    "DiscountValue DOUBLE NOT NULL, " +
                    "MinOrderAmount DOUBLE DEFAULT 0, " +
                    "UsageLimit INT DEFAULT 100, " +
                    "UsedCount INT DEFAULT 0, " +
                    "StartDate DATE, " +
                    "EndDate DATE, " +
                    "IsActive BOOLEAN DEFAULT 1)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS stock_history (" +
                    "HistoryID INT PRIMARY KEY AUTO_INCREMENT, " +
                    "ProductID INT NOT NULL, " +
                    "SizeID INT NOT NULL, " +
                    "BranchID INT NULL, " +
                    "OldQuantity INT NOT NULL, " +
                    "NewQuantity INT NOT NULL, " +
                    "ActionType VARCHAR(80) NOT NULL, " +
                    "UserID INT NULL, " +
                    "ActionDate DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "Notes TEXT)");

            st.executeUpdate("INSERT IGNORE INTO coupons(Code, DiscountType, DiscountValue, MinOrderAmount, UsageLimit, UsedCount, StartDate, EndDate, IsActive) " +
                    "VALUES ('ONLINE10', 'PERCENT', 10, 50, 100, 0, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 1 YEAR), 1)");

            st.executeUpdate("INSERT IGNORE INTO coupons(Code, DiscountType, DiscountValue, MinOrderAmount, UsageLimit, UsedCount, StartDate, EndDate, IsActive) " +
                    "VALUES ('DELIVERY5', 'FIXED', 5, 100, 100, 0, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 1 YEAR), 1)");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }



    // =========================================================
    // FULL 15 UPGRADES
    // =========================================================

    private void ensureFullUpgradeTablesExist() {
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            if (con == null) return;
            Statement st = con.createStatement();

            st.executeUpdate("CREATE TABLE IF NOT EXISTS notifications (NotificationID INT PRIMARY KEY AUTO_INCREMENT, UserID INT NULL, Message TEXT NOT NULL, IsRead BOOLEAN DEFAULT 0, CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS online_orders (OnlineOrderID INT PRIMARY KEY AUTO_INCREMENT, CustomerID INT NOT NULL, OrderDate DATETIME NOT NULL, ReceiveMethod ENUM('PICKUP_FROM_STORE','HOME_DELIVERY') NOT NULL, DeliveryAddress VARCHAR(255), Phone VARCHAR(30), Status ENUM('PENDING','ACCEPTED','REJECTED','CONFIRMED','PREPARING','READY_FOR_PICKUP','OUT_FOR_DELIVERY','DELIVERED','CANCELLED') DEFAULT 'PENDING', RejectReason TEXT, PaymentMethod VARCHAR(50), DeliveryArea VARCHAR(80), DeliveryFee DOUBLE DEFAULT 0, TotalAmount DOUBLE NOT NULL)");
            addColumnIfMissing(con, "online_orders", "RejectReason", "TEXT");
            addColumnIfMissing(con, "online_orders", "PaymentMethod", "VARCHAR(50)");
            addColumnIfMissing(con, "online_orders", "DeliveryArea", "VARCHAR(80)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS online_order_items (OnlineOrderItemID INT PRIMARY KEY AUTO_INCREMENT, OnlineOrderID INT NOT NULL, ProductID INT NOT NULL, SizeID INT NOT NULL, Quantity INT NOT NULL, UnitPrice DOUBLE NOT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS order_timeline (TimelineID INT PRIMARY KEY AUTO_INCREMENT, OnlineOrderID INT NOT NULL, Status VARCHAR(80) NOT NULL, Notes TEXT, CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS customer_addresses (AddressID INT PRIMARY KEY AUTO_INCREMENT, CustomerID INT NOT NULL, Label VARCHAR(80), AddressText VARCHAR(255), Area VARCHAR(80), IsDefault BOOLEAN DEFAULT 0)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS delivery_areas (AreaID INT PRIMARY KEY AUTO_INCREMENT, AreaName VARCHAR(80) UNIQUE, DeliveryFee DOUBLE NOT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS wishlist (WishlistID INT PRIMARY KEY AUTO_INCREMENT, CustomerID INT NOT NULL, ProductID INT NOT NULL, CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP, UNIQUE(CustomerID, ProductID))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS product_reviews (ReviewID INT PRIMARY KEY AUTO_INCREMENT, CustomerID INT NOT NULL, ProductID INT NOT NULL, Rating INT NOT NULL, ReviewText TEXT, ReviewDate DATETIME DEFAULT CURRENT_TIMESTAMP)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS coupons (CouponID INT PRIMARY KEY AUTO_INCREMENT, Code VARCHAR(50) NOT NULL UNIQUE, DiscountType ENUM('PERCENT','FIXED') NOT NULL, DiscountValue DOUBLE NOT NULL, MinOrderAmount DOUBLE DEFAULT 0, UsageLimit INT DEFAULT 100, UsedCount INT DEFAULT 0, StartDate DATE, EndDate DATE, IsActive BOOLEAN DEFAULT 1)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS return_requests (ReturnRequestID INT PRIMARY KEY AUTO_INCREMENT, CustomerID INT NOT NULL, OnlineOrderID INT NULL, ProductID INT NULL, RequestType ENUM('RETURN','EXCHANGE') NOT NULL, Reason TEXT, Status ENUM('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING', RequestDate DATETIME DEFAULT CURRENT_TIMESTAMP)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS stock_history (HistoryID INT PRIMARY KEY AUTO_INCREMENT, ProductID INT NOT NULL, SizeID INT NOT NULL, BranchID INT NULL, OldQuantity INT NOT NULL, NewQuantity INT NOT NULL, ActionType VARCHAR(80) NOT NULL, UserID INT NULL, ActionDate DATETIME DEFAULT CURRENT_TIMESTAMP, Notes TEXT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS audit_logs (LogID INT PRIMARY KEY AUTO_INCREMENT, UserID INT NULL, ActionType VARCHAR(100) NOT NULL, Details TEXT, LogDate DATETIME DEFAULT CURRENT_TIMESTAMP)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS product_extra_info (ProductID INT PRIMARY KEY, Description TEXT, Material VARCHAR(120), CareInstructions TEXT, FitType VARCHAR(80), Season VARCHAR(80), IsActive BOOLEAN DEFAULT 1)");

            st.executeUpdate("INSERT IGNORE INTO delivery_areas(AreaName, DeliveryFee) VALUES ('Ramallah',15),('Birzeit',20),('Jerusalem',25),('Other',30)");
            st.executeUpdate("INSERT IGNORE INTO coupons(Code, DiscountType, DiscountValue, MinOrderAmount, UsageLimit, UsedCount, StartDate, EndDate, IsActive) VALUES ('ONLINE10','PERCENT',10,50,100,0,CURDATE(),DATE_ADD(CURDATE(), INTERVAL 1 YEAR),1),('DELIVERY5','FIXED',5,100,100,0,CURDATE(),DATE_ADD(CURDATE(), INTERVAL 1 YEAR),1)");

            // default product extra info
            st.executeUpdate("INSERT IGNORE INTO product_extra_info(ProductID, Description, Material, CareInstructions, FitType, Season, IsActive) SELECT ProductID, CONCAT('Elegant product from Lucerne Boutique: ', Name), 'Mixed fabric', 'Hand wash recommended', 'Regular fit', 'All season', 1 FROM products");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void addColumnIfMissing(Connection con, String tableName, String columnName, String columnDefinition) {
        try {
            PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?"
            );
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                Statement st = con.createStatement();
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
            }
        } catch (Exception ignored) {
        }
    }

    private void openProductDetailsDialog(ArrayList<ProductVariant> variants, ProductVariant selectedVariant) {
        if (currentRole == null || !currentRole.equals("CUSTOMER")) {
            openProductOrderDialog(variants, selectedVariant);
            return;
        }

        ProductVariant[] chosen = new ProductVariant[]{selectedVariant};

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Product Details");

        ImageView img = createCleanProductImageView(chosen[0].imagePath);
        img.setFitWidth(260);
        img.setFitHeight(330);

        Label name = new Label(chosen[0].baseName + " - " + chosen[0].color);
        name.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");

        Label price = new Label("Price: " + chosen[0].price);
        price.setStyle("-fx-font-size:17px; -fx-text-fill:#7A5C52; -fx-font-weight:bold;");

        Label info = new Label(loadProductExtraInfo(chosen[0].productId));
        info.setWrapText(true);
        info.setStyle("-fx-text-fill:#7A5C52;");

        HBox colorBox = new HBox(10);
        colorBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> sizeBox = new ComboBox<>();
        sizeBox.setPromptText("Choose size");
        sizeBox.setPrefWidth(180);

        Runnable refreshSizes = () -> {
            sizeBox.getItems().clear();
            if (chosen[0].sizeQuantity != null) {
                for (Map.Entry<String, Integer> entry : chosen[0].sizeQuantity.entrySet()) {
                    String label = entry.getKey() + " / Left " + entry.getValue();
                    if (entry.getValue() <= 2) label += " LOW";
                    if (entry.getValue() <= 0) label += " OUT";
                    sizeBox.getItems().add(label);
                }
            }
        };
        refreshSizes.run();

        for (ProductVariant v : variants) {
            Circle c = createColorCircle(v.color);
            Tooltip.install(c, new Tooltip(v.color));
            c.setOnMouseClicked(e -> {
                chosen[0] = v;
                ImageView changed = createCleanProductImageView(v.imagePath);
                img.setImage(changed.getImage());
                img.setViewport(changed.getViewport());
                name.setText(v.baseName + " - " + v.color);
                price.setText("Price: " + v.price);
                refreshSizes.run();
            });
            colorBox.getChildren().add(c);
        }

        TextField qty = new TextField("1");
        qty.setPrefWidth(90);

        Button addCart = createSmallButton("Add to Cart");
        addCart.setOnAction(e -> {
            try {
                if (sizeBox.getValue() == null) throw new Exception("Choose size first.");
                String size = sizeBox.getValue().split(" / ")[0].trim();
                int sizeId = getSizeIdForProduct(chosen[0].productId, size);
                int q = Integer.parseInt(qty.getText().trim());
                if (q <= 0) throw new Exception("Quantity must be greater than zero.");
                customerCart.add(new CartItem(chosen[0].productId, sizeId, chosen[0].baseName + " - " + chosen[0].color, size, q, chosen[0].price));
                showSuccess("Added to cart.");
                stage.close();
                showCartPage(currentUserId);
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        Button buyNow = createSmallButton("Buy Now");
        buyNow.setOnAction(e -> {
            try {
                if (sizeBox.getValue() == null) throw new Exception("Choose size first.");
                String size = sizeBox.getValue().split(" / ")[0].trim();
                int sizeId = getSizeIdForProduct(chosen[0].productId, size);
                int q = Integer.parseInt(qty.getText().trim());
                customerCart.clear();
                customerCart.add(new CartItem(chosen[0].productId, sizeId, chosen[0].baseName + " - " + chosen[0].color, size, q, chosen[0].price));
                stage.close();
                showCartPage(currentUserId);
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        Button favorite = createSmallButton("Favorite");
        favorite.setOnAction(e -> {
            try {
                addToWishlist(currentUserId, chosen[0].productId);
                showSuccess("Added to favorites.");
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        VBox right = new VBox(12, name, price, new Label("Colors:"), colorBox, new Label("Size:"), sizeBox, new Label("Quantity:"), qty, info, new HBox(10, addCart, buyNow, favorite));
        right.setPadding(new Insets(10));

        HBox root = new HBox(18, img, right);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color:#FFF7F2;");

        stage.setScene(new Scene(root, 850, 560));
        stage.showAndWait();
    }

    private String loadProductExtraInfo(int productId) {
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT Description, Material, CareInstructions, FitType, Season FROM product_extra_info WHERE ProductID=?");
            ps.setInt(1, productId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return "Description: " + rs.getString("Description") +
                        "\nMaterial: " + rs.getString("Material") +
                        "\nCare: " + rs.getString("CareInstructions") +
                        "\nFit: " + rs.getString("FitType") +
                        "\nSeason: " + rs.getString("Season");
            }
        } catch (Exception ignored) {}
        return "Elegant Lucerne Boutique product.";
    }

    private void showCartPage(int customerUserId) {
        pageTitle.setText("Cart / Checkout");

        VBox page = new VBox(14);
        page.setPadding(new Insets(20));
        page.setStyle("-fx-background-color:#FFF7F2;");

        Label title = new Label("Shopping Cart");
        title.setStyle("-fx-font-size:26px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");

        TableView<CartItem> cartTable = new TableView<>();
        cartTable.setPrefHeight(260);

        TableColumn<CartItem, String> productCol = new TableColumn<>("Product");
        productCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name));
        productCol.setPrefWidth(260);

        TableColumn<CartItem, String> sizeCol = new TableColumn<>("Size");
        sizeCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().sizeValue));
        sizeCol.setPrefWidth(90);

        TableColumn<CartItem, String> qtyCol = new TableColumn<>("Qty");
        qtyCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().quantity)));
        qtyCol.setPrefWidth(80);

        TableColumn<CartItem, String> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().unitPrice)));
        priceCol.setPrefWidth(100);

        TableColumn<CartItem, String> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().unitPrice * d.getValue().quantity)));
        totalCol.setPrefWidth(120);

        cartTable.getColumns().addAll(productCol, sizeCol, qtyCol, priceCol, totalCol);
        cartTable.getItems().addAll(customerCart);

        Button remove = createSmallButton("Remove Selected");
        remove.setOnAction(e -> {
            CartItem item = cartTable.getSelectionModel().getSelectedItem();
            if (item != null) {
                customerCart.remove(item);
                showCartPage(customerUserId);
            }
        });

        Button clear = createSmallButton("Clear Cart");
        clear.setOnAction(e -> {
            customerCart.clear();
            showCartPage(customerUserId);
        });

        ComboBox<String> receive = new ComboBox<>();
        receive.getItems().addAll("PICKUP_FROM_STORE", "HOME_DELIVERY");
        receive.setValue("PICKUP_FROM_STORE");

        ComboBox<String> area = new ComboBox<>();
        area.setPromptText("Delivery area");
        loadDeliveryAreas(area);
        area.setDisable(true);

        TextField address = new TextField();
        address.setPromptText("Address");
        address.setDisable(true);

        TextField phone = new TextField();
        phone.setPromptText("Phone");

        ComboBox<String> payment = new ComboBox<>();
        payment.getItems().addAll("Cash on Delivery", "Pay at Store", "Card - Demo");
        payment.setValue("Cash on Delivery");

        TextField coupon = new TextField();
        coupon.setPromptText("Coupon code optional");

        Label totalLabel = new Label();
        totalLabel.setStyle("-fx-font-size:17px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");

        Runnable updateTotal = () -> {
            double subtotal = cartSubtotal();
            double delivery = "HOME_DELIVERY".equals(receive.getValue()) ? getDeliveryFee(area.getValue()) : 0;
            totalLabel.setText("Subtotal: " + subtotal + " | Delivery: " + delivery + " | Total before coupon: " + (subtotal + delivery));
        };
        updateTotal.run();

        receive.setOnAction(e -> {
            boolean del = "HOME_DELIVERY".equals(receive.getValue());
            area.setDisable(!del);
            address.setDisable(!del);
            if (!del) {
                address.clear();
                area.setValue(null);
            }
            updateTotal.run();
        });
        area.setOnAction(e -> updateTotal.run());

        Button checkout = createSmallButton("Checkout");
        checkout.setOnAction(e -> {
            try {
                if (customerCart.isEmpty()) throw new Exception("Cart is empty.");
                if (phone.getText().trim().isEmpty()) throw new Exception("Phone is required.");
                if ("HOME_DELIVERY".equals(receive.getValue()) && address.getText().trim().isEmpty()) throw new Exception("Address is required for delivery.");
                int orderId = createCartOnlineOrder(customerUserId, receive.getValue(), address.getText().trim(), phone.getText().trim(), area.getValue(), payment.getValue(), coupon.getText().trim());
                customerCart.clear();
                Alert done = new Alert(Alert.AlertType.INFORMATION);
                done.setTitle("Order Placed");
                done.setHeaderText("Order created successfully");
                done.setContentText("Order ID = " + orderId);
                done.showAndWait();
                showCustomerOnlineOrders(customerUserId);
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        HBox buttons = new HBox(10, remove, clear);
        HBox checkoutLine = new HBox(10, receive, area, address, phone, payment, coupon, checkout);
        checkoutLine.setAlignment(Pos.CENTER_LEFT);

        page.getChildren().addAll(title, cartTable, buttons, checkoutLine, totalLabel);

        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        contentBox.getChildren().set(2, sp);
    }

    private double cartSubtotal() {
        double total = 0;
        for (CartItem item : customerCart) total += item.unitPrice * item.quantity;
        return total;
    }

    private int createCartOnlineOrder(int customerUserId, String receiveMethod, String address, String phone, String area, String paymentMethod, String couponCode) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();
        if (con == null) throw new Exception("No database connection.");

        int customerId = getCustomerIdByUserId(customerUserId);
        double subtotal = cartSubtotal();
        double deliveryFee = "HOME_DELIVERY".equals(receiveMethod) ? getDeliveryFee(area) : 0.0;
        double discount = calculateCouponDiscount(couponCode, subtotal + deliveryFee);
        double total = subtotal + deliveryFee - discount;

        con.setAutoCommit(false);
        try {
            PreparedStatement orderPs = con.prepareStatement(
                    "INSERT INTO online_orders(CustomerID, OrderDate, ReceiveMethod, DeliveryAddress, Phone, Status, DeliveryFee, TotalAmount, PaymentMethod, DeliveryArea) VALUES (?, NOW(), ?, ?, ?, 'PENDING', ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            orderPs.setInt(1, customerId);
            orderPs.setString(2, receiveMethod);
            orderPs.setString(3, address);
            orderPs.setString(4, phone);
            orderPs.setDouble(5, deliveryFee);
            orderPs.setDouble(6, total);
            orderPs.setString(7, paymentMethod);
            orderPs.setString(8, area);
            orderPs.executeUpdate();

            ResultSet keys = orderPs.getGeneratedKeys();
            keys.next();
            int orderId = keys.getInt(1);

            for (CartItem item : customerCart) {
                PreparedStatement itemPs = con.prepareStatement("INSERT INTO online_order_items(OnlineOrderID, ProductID, SizeID, Quantity, UnitPrice) VALUES (?, ?, ?, ?, ?)");
                itemPs.setInt(1, orderId);
                itemPs.setInt(2, item.productId);
                itemPs.setInt(3, item.sizeId);
                itemPs.setInt(4, item.quantity);
                itemPs.setDouble(5, item.unitPrice);
                itemPs.executeUpdate();
            }

            insertTimeline(con, orderId, "Order Placed", "Customer placed the order");
            con.commit();
            return orderId;
        } catch (Exception ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }

    private void insertTimeline(Connection con, int orderId, String status, String notes) throws SQLException {
        PreparedStatement ps = con.prepareStatement("INSERT INTO order_timeline(OnlineOrderID, Status, Notes, CreatedAt) VALUES (?, ?, ?, NOW())");
        ps.setInt(1, orderId);
        ps.setString(2, status);
        ps.setString(3, notes);
        ps.executeUpdate();
    }

    private void loadDeliveryAreas(ComboBox<String> area) {
        area.getItems().clear();
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            ResultSet rs = con.createStatement().executeQuery("SELECT AreaName, DeliveryFee FROM delivery_areas ORDER BY DeliveryFee");
            while (rs.next()) area.getItems().add(rs.getString("AreaName") + " / " + rs.getDouble("DeliveryFee"));
        } catch (Exception ignored) {}
    }

    private double getDeliveryFee(String areaText) {
        if (areaText == null || areaText.trim().isEmpty()) return 0;
        try {
            String[] parts = areaText.split(" / ");
            if (parts.length == 2) return Double.parseDouble(parts[1]);
        } catch (Exception ignored) {}
        return 0;
    }

    private double calculateCouponDiscount(String code, double amount) {
        if (code == null || code.trim().isEmpty()) return 0;
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT DiscountType, DiscountValue, MinOrderAmount, UsageLimit, UsedCount FROM coupons WHERE Code=? AND IsActive=1 AND (StartDate IS NULL OR StartDate<=CURDATE()) AND (EndDate IS NULL OR EndDate>=CURDATE())");
            ps.setString(1, code.trim());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (amount < rs.getDouble("MinOrderAmount")) return 0;
                if (rs.getInt("UsedCount") >= rs.getInt("UsageLimit")) return 0;
                if ("PERCENT".equals(rs.getString("DiscountType"))) return amount * rs.getDouble("DiscountValue") / 100.0;
                return rs.getDouble("DiscountValue");
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private void addToWishlist(int userId, int productId) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();
        int customerId = getCustomerIdByUserId(userId);
        PreparedStatement ps = con.prepareStatement("INSERT IGNORE INTO wishlist(CustomerID, ProductID, CreatedAt) VALUES (?, ?, NOW())");
        ps.setInt(1, customerId);
        ps.setInt(2, productId);
        ps.executeUpdate();
    }

    private void showOrderDetailsPage() {
        showTable("SELECT oo.OnlineOrderID, u.FullName AS Customer, oo.Phone, oo.ReceiveMethod, oo.DeliveryArea, oo.DeliveryAddress, oo.PaymentMethod, oo.Status, oo.RejectReason, p.Name AS Product, ps.SizeValue, ooi.Quantity, ooi.UnitPrice, oo.TotalAmount FROM online_orders oo JOIN customers c ON oo.CustomerID=c.CustomerID JOIN users u ON c.UserID=u.UserID JOIN online_order_items ooi ON oo.OnlineOrderID=ooi.OnlineOrderID JOIN products p ON ooi.ProductID=p.ProductID JOIN product_sizes ps ON ooi.SizeID=ps.SizeID ORDER BY oo.OnlineOrderID DESC");
        pageTitle.setText("Order Details");
        addOrderDecisionButtons();
    }

    private void addOrderDecisionButtons() {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        TextField orderId = new TextField();
        orderId.setPromptText("Order ID");
        TextField reason = new TextField();
        reason.setPromptText("Reject reason");
        Button accept = createSmallButton("Accept");
        accept.setOnAction(e -> updateOrderDecision(orderId, "ACCEPTED", ""));
        Button reject = createSmallButton("Reject");
        reject.setOnAction(e -> updateOrderDecision(orderId, "REJECTED", reason.getText()));
        box.getChildren().addAll(new Label("Order:"), orderId, accept, reason, reject);
        if (contentBox.getChildren().size() > 3) contentBox.getChildren().set(3, box);
        else contentBox.getChildren().add(box);
    }

    private void updateOrderDecision(TextField field, String status, String reason) {
        try {
            int id = Integer.parseInt(field.getText().trim());
            Connection con = new DataBaseConnection().getConnection().getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE online_orders SET Status=?, RejectReason=? WHERE OnlineOrderID=?");
            ps.setString(1, status);
            ps.setString(2, reason);
            ps.setInt(3, id);
            ps.executeUpdate();
            insertTimeline(con, id, status, reason);
            showSuccess("Order updated to " + status);
            showOrderDetailsPage();
        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    private void showOrderTimelinePage(int userId) {
        if ("CUSTOMER".equals(currentRole)) {
            try {
                int customerId = getCustomerIdByUserId(userId);
                showTable("SELECT oo.OnlineOrderID, oo.Status AS CurrentStatus, ot.Status AS TimelineStatus, ot.Notes, ot.CreatedAt FROM online_orders oo LEFT JOIN order_timeline ot ON oo.OnlineOrderID=ot.OnlineOrderID WHERE oo.CustomerID=" + customerId + " ORDER BY oo.OnlineOrderID DESC, ot.TimelineID");
            } catch (Exception ex) { showAlert(ex.getMessage()); }
        } else {
            showTable("SELECT oo.OnlineOrderID, u.FullName AS Customer, oo.Status AS CurrentStatus, ot.Status AS TimelineStatus, ot.Notes, ot.CreatedAt FROM online_orders oo JOIN customers c ON oo.CustomerID=c.CustomerID JOIN users u ON c.UserID=u.UserID LEFT JOIN order_timeline ot ON oo.OnlineOrderID=ot.OnlineOrderID ORDER BY oo.OnlineOrderID DESC, ot.TimelineID");
        }
        pageTitle.setText("Order Timeline");
    }

    private void showAddressBookPage(int userId) {
        try {
            int customerId = getCustomerIdByUserId(userId);
            showTable("SELECT AddressID, Label, AddressText, Area, IsDefault FROM customer_addresses WHERE CustomerID=" + customerId);
            pageTitle.setText("My Addresses");

            HBox box = new HBox(10);
            TextField label = new TextField(); label.setPromptText("Label");
            TextField addr = new TextField(); addr.setPromptText("Address");
            TextField area = new TextField(); area.setPromptText("Area");
            Button add = createSmallButton("Add Address");
            add.setOnAction(e -> {
                try {
                    Connection con = new DataBaseConnection().getConnection().getConnection();
                    PreparedStatement ps = con.prepareStatement("INSERT INTO customer_addresses(CustomerID, Label, AddressText, Area, IsDefault) VALUES (?, ?, ?, ?, 0)");
                    ps.setInt(1, customerId);
                    ps.setString(2, label.getText());
                    ps.setString(3, addr.getText());
                    ps.setString(4, area.getText());
                    ps.executeUpdate();
                    showAddressBookPage(userId);
                } catch (Exception ex) { showAlert(ex.getMessage()); }
            });
            box.getChildren().addAll(label, addr, area, add);
            if (contentBox.getChildren().size() > 3) contentBox.getChildren().set(3, box);
            else contentBox.getChildren().add(box);
        } catch (Exception ex) { showAlert(ex.getMessage()); }
    }

    private void showReturnExchangeRequestPage(int userId) {
        Stage stage = new Stage();
        stage.setTitle("Return / Exchange Request");

        TextField orderId = new TextField(); orderId.setPromptText("Online Order ID");
        TextField productId = new TextField(); productId.setPromptText("Product ID optional");
        ComboBox<String> type = new ComboBox<>(); type.getItems().addAll("RETURN", "EXCHANGE"); type.setValue("RETURN");
        TextArea reason = new TextArea(); reason.setPromptText("Reason"); reason.setPrefHeight(120);
        Label msg = new Label();

        Button submit = createSmallButton("Submit Request");
        submit.setOnAction(e -> {
            try {
                int customerId = getCustomerIdByUserId(userId);
                Connection con = new DataBaseConnection().getConnection().getConnection();
                PreparedStatement ps = con.prepareStatement("INSERT INTO return_requests(CustomerID, OnlineOrderID, ProductID, RequestType, Reason, Status, RequestDate) VALUES (?, ?, ?, ?, ?, 'PENDING', NOW())");
                ps.setInt(1, customerId);
                ps.setInt(2, Integer.parseInt(orderId.getText().trim()));
                if (productId.getText().trim().isEmpty()) ps.setNull(3, Types.INTEGER); else ps.setInt(3, Integer.parseInt(productId.getText().trim()));
                ps.setString(4, type.getValue());
                ps.setString(5, reason.getText());
                ps.executeUpdate();
                msg.setText("Request submitted.");
            } catch (Exception ex) { msg.setText(ex.getMessage()); }
        });

        VBox root = new VBox(12, new Label("Return / Exchange Request"), orderId, productId, type, reason, submit, msg);
        root.setPadding(new Insets(20));
        stage.setScene(new Scene(root, 430, 430));
        stage.show();
    }

    private void showAdminReturnsRequests() {
        showTable("SELECT rr.ReturnRequestID, u.FullName AS Customer, rr.OnlineOrderID, rr.ProductID, rr.RequestType, rr.Reason, rr.Status, rr.RequestDate FROM return_requests rr JOIN customers c ON rr.CustomerID=c.CustomerID JOIN users u ON c.UserID=u.UserID ORDER BY rr.ReturnRequestID DESC");
        pageTitle.setText("Returns Requests");
    }

    private void showDeliveryAreasPage() {
        showTable("SELECT AreaID, AreaName, DeliveryFee FROM delivery_areas ORDER BY DeliveryFee");
        pageTitle.setText("Delivery Areas");
    }

    private void showAnalyticsPlusPage() {
        showTable("SELECT 'Best Selling Color' AS Metric, p.Color AS Name, SUM(si.Quantity) AS Value FROM sale_items si JOIN products p ON si.ProductID=p.ProductID GROUP BY p.Color " +
                "UNION ALL SELECT 'Best Selling Size', ps.SizeValue, SUM(si.Quantity) FROM sale_items si JOIN product_sizes ps ON si.SizeID=ps.SizeID GROUP BY ps.SizeValue " +
                "UNION ALL SELECT 'Best Customer', u.FullName, SUM(s.FinalAmount) FROM sales s JOIN customers c ON s.CustomerID=c.CustomerID JOIN users u ON c.UserID=u.UserID GROUP BY u.FullName");
        pageTitle.setText("Analytics Plus");
    }

    private static class CartItem {
        int productId;
        int sizeId;
        String name;
        String sizeValue;
        int quantity;
        double unitPrice;

        CartItem(int productId, int sizeId, String name, String sizeValue, int quantity, double unitPrice) {
            this.productId = productId;
            this.sizeId = sizeId;
            this.name = name;
            this.sizeValue = sizeValue;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }
    }



    // =========================================================
    // DATABASE TABLE UPGRADES 1 TO 18 PAGES
    // =========================================================

    private void ensureTableUpgradesExist() {
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            if (con == null) return;
            Statement st = con.createStatement();

            addColumnIfMissingSafe(con, "products", "Description", "TEXT");
            addColumnIfMissingSafe(con, "products", "Material", "VARCHAR(100)");
            addColumnIfMissingSafe(con, "products", "CareInstructions", "TEXT");
            addColumnIfMissingSafe(con, "products", "IsActive", "BOOLEAN DEFAULT TRUE");
            addColumnIfMissingSafe(con, "products", "CreatedAt", "DATETIME DEFAULT CURRENT_TIMESTAMP");

            addColumnIfMissingSafe(con, "customers", "Address", "VARCHAR(255)");
            addColumnIfMissingSafe(con, "customers", "City", "VARCHAR(100)");
            addColumnIfMissingSafe(con, "customers", "BirthDate", "DATE");
            addColumnIfMissingSafe(con, "customers", "Notes", "TEXT");

            addColumnIfMissingSafe(con, "sales", "PaymentMethod", "VARCHAR(50)");
            addColumnIfMissingSafe(con, "sales", "Status", "VARCHAR(50) DEFAULT 'COMPLETED'");
            addColumnIfMissingSafe(con, "sales", "Notes", "TEXT");

            addColumnIfMissingSafe(con, "branch_inventory", "MinQuantity", "INT DEFAULT 2");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS categories (CategoryID INT PRIMARY KEY AUTO_INCREMENT, CategoryName VARCHAR(100) NOT NULL UNIQUE, IsActive BOOLEAN DEFAULT TRUE)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS product_images (ImageID INT PRIMARY KEY AUTO_INCREMENT, ProductID INT NOT NULL, ImagePath VARCHAR(255) NOT NULL, IsMain BOOLEAN DEFAULT FALSE)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS product_colors (ColorID INT PRIMARY KEY AUTO_INCREMENT, ProductID INT NOT NULL, ColorName VARCHAR(50), ImagePath VARCHAR(255))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS product_variants (VariantID INT PRIMARY KEY AUTO_INCREMENT, ProductID INT NOT NULL, ColorName VARCHAR(50), SizeValue VARCHAR(20), Barcode VARCHAR(100), Price DOUBLE, Quantity INT DEFAULT 0)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS order_status_history (HistoryID INT PRIMARY KEY AUTO_INCREMENT, OnlineOrderID INT NOT NULL, OldStatus VARCHAR(50), NewStatus VARCHAR(50), ChangedBy INT, ChangedAt DATETIME DEFAULT CURRENT_TIMESTAMP, Notes TEXT)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS payment_transactions (PaymentID INT PRIMARY KEY AUTO_INCREMENT, OnlineOrderID INT NULL, SaleID INT NULL, PaymentMethod VARCHAR(50), Amount DOUBLE, PaymentStatus VARCHAR(50), TransactionDate DATETIME DEFAULT CURRENT_TIMESTAMP)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS delivery_drivers (DriverID INT PRIMARY KEY AUTO_INCREMENT, DriverName VARCHAR(100), Phone VARCHAR(30), IsActive BOOLEAN DEFAULT TRUE)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS delivery_assignments (AssignmentID INT PRIMARY KEY AUTO_INCREMENT, OnlineOrderID INT NOT NULL, DriverID INT NOT NULL, AssignedAt DATETIME DEFAULT CURRENT_TIMESTAMP, DeliveredAt DATETIME NULL, Status VARCHAR(50) DEFAULT 'ASSIGNED')");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS suppliers (SupplierID INT PRIMARY KEY AUTO_INCREMENT, SupplierName VARCHAR(100), Phone VARCHAR(30), Email VARCHAR(100), Address VARCHAR(255))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS purchase_orders (PurchaseOrderID INT PRIMARY KEY AUTO_INCREMENT, SupplierID INT NULL, OrderDate DATE, TotalCost DOUBLE, Status VARCHAR(50))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS purchase_order_items (PurchaseItemID INT PRIMARY KEY AUTO_INCREMENT, PurchaseOrderID INT NULL, ProductID INT NULL, SizeID INT NULL, Quantity INT, UnitCost DOUBLE)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS product_barcodes (BarcodeID INT PRIMARY KEY AUTO_INCREMENT, ProductID INT NOT NULL, SizeID INT NULL, Barcode VARCHAR(100) UNIQUE, QRCodeText VARCHAR(255))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS price_history (PriceHistoryID INT PRIMARY KEY AUTO_INCREMENT, ProductID INT NOT NULL, OldPrice DOUBLE, NewPrice DOUBLE, ChangedBy INT NULL, ChangedAt DATETIME DEFAULT CURRENT_TIMESTAMP)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS expenses_categories (ExpenseCategoryID INT PRIMARY KEY AUTO_INCREMENT, CategoryName VARCHAR(100) UNIQUE)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS system_settings (SettingID INT PRIMARY KEY AUTO_INCREMENT, SettingName VARCHAR(100) UNIQUE, SettingValue TEXT)");

            st.executeUpdate("INSERT IGNORE INTO categories(CategoryName, IsActive) SELECT DISTINCT Category, 1 FROM products WHERE Category IS NOT NULL");
            st.executeUpdate("INSERT IGNORE INTO delivery_drivers(DriverName, Phone, IsActive) VALUES ('Default Driver','0590000000',1)");
            st.executeUpdate("INSERT IGNORE INTO expenses_categories(CategoryName) VALUES ('Rent'),('Salaries'),('Delivery'),('Marketing'),('Other')");
            st.executeUpdate("INSERT IGNORE INTO system_settings(SettingName, SettingValue) VALUES ('StoreName','Lucerne Boutique'),('DefaultDeliveryFee','20'),('LowStockDefault','2')");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void addColumnIfMissingSafe(Connection con, String tableName, String columnName, String definition) {
        try {
            PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?"
            );
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                Statement st = con.createStatement();
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
            }
        } catch (Exception ignored) {
        }
    }

    private void showSuppliersPage() {
        ensureTableUpgradesExist();
        showTable("SELECT SupplierID, SupplierName, Phone, Email, Address FROM suppliers ORDER BY SupplierID DESC");
        pageTitle.setText("Suppliers");
        HBox form = new HBox(10);
        form.setAlignment(Pos.CENTER_LEFT);
        TextField name = new TextField(); name.setPromptText("Supplier name");
        TextField phone = new TextField(); phone.setPromptText("Phone");
        TextField email = new TextField(); email.setPromptText("Email");
        TextField address = new TextField(); address.setPromptText("Address");
        Button add = createSmallButton("Add Supplier");
        add.setOnAction(e -> {
            try {
                Connection con = new DataBaseConnection().getConnection().getConnection();
                PreparedStatement ps = con.prepareStatement("INSERT INTO suppliers(SupplierName, Phone, Email, Address) VALUES (?, ?, ?, ?)");
                ps.setString(1, name.getText());
                ps.setString(2, phone.getText());
                ps.setString(3, email.getText());
                ps.setString(4, address.getText());
                ps.executeUpdate();
                showSuppliersPage();
            } catch (Exception ex) { showAlert(ex.getMessage()); }
        });
        form.getChildren().addAll(name, phone, email, address, add);
        addBottomForm(form);
    }

    private void showPurchaseOrdersPage() {
        ensureTableUpgradesExist();
        showTable("SELECT po.PurchaseOrderID, s.SupplierName, po.OrderDate, po.TotalCost, po.Status FROM purchase_orders po LEFT JOIN suppliers s ON po.SupplierID=s.SupplierID ORDER BY po.PurchaseOrderID DESC");
        pageTitle.setText("Purchase Orders");
        HBox form = new HBox(10);
        TextField supplierId = new TextField(); supplierId.setPromptText("SupplierID");
        TextField total = new TextField(); total.setPromptText("Total Cost");
        ComboBox<String> status = new ComboBox<>(); status.getItems().addAll("PENDING","RECEIVED","CANCELLED"); status.setValue("PENDING");
        Button add = createSmallButton("Add PO");
        add.setOnAction(e -> {
            try {
                Connection con = new DataBaseConnection().getConnection().getConnection();
                PreparedStatement ps = con.prepareStatement("INSERT INTO purchase_orders(SupplierID, OrderDate, TotalCost, Status) VALUES (?, CURDATE(), ?, ?)");
                ps.setInt(1, Integer.parseInt(supplierId.getText().trim()));
                ps.setDouble(2, Double.parseDouble(total.getText().trim()));
                ps.setString(3, status.getValue());
                ps.executeUpdate();
                showPurchaseOrdersPage();
            } catch (Exception ex) { showAlert(ex.getMessage()); }
        });
        form.getChildren().addAll(supplierId, total, status, add);
        addBottomForm(form);
    }

    private void showDriversPage() {
        ensureTableUpgradesExist();
        showTable("SELECT DriverID, DriverName, Phone, IsActive FROM delivery_drivers ORDER BY DriverID DESC");
        pageTitle.setText("Delivery Drivers");
        HBox form = new HBox(10);
        TextField name = new TextField(); name.setPromptText("Driver name");
        TextField phone = new TextField(); phone.setPromptText("Phone");
        Button add = createSmallButton("Add Driver");
        add.setOnAction(e -> {
            try {
                Connection con = new DataBaseConnection().getConnection().getConnection();
                PreparedStatement ps = con.prepareStatement("INSERT INTO delivery_drivers(DriverName, Phone, IsActive) VALUES (?, ?, 1)");
                ps.setString(1, name.getText());
                ps.setString(2, phone.getText());
                ps.executeUpdate();
                showDriversPage();
            } catch (Exception ex) { showAlert(ex.getMessage()); }
        });
        form.getChildren().addAll(name, phone, add);
        addBottomForm(form);
    }

    private void showPaymentsPage() {
        ensureTableUpgradesExist();
        showTable("SELECT PaymentID, OnlineOrderID, SaleID, PaymentMethod, Amount, PaymentStatus, TransactionDate FROM payment_transactions ORDER BY PaymentID DESC");
        pageTitle.setText("Payment Transactions");
    }

    private void showProductImagesPage() {
        ensureTableUpgradesExist();
        showTable("SELECT pi.ImageID, p.Name AS Product, pi.ImagePath, pi.IsMain FROM product_images pi JOIN products p ON pi.ProductID=p.ProductID ORDER BY pi.ImageID DESC");
        pageTitle.setText("Product Images");
        HBox form = new HBox(10);
        TextField productId = new TextField(); productId.setPromptText("ProductID");
        TextField imagePath = new TextField(); imagePath.setPromptText("Image Path");
        CheckBox isMain = new CheckBox("Main");
        Button add = createSmallButton("Add Image");
        add.setOnAction(e -> {
            try {
                Connection con = new DataBaseConnection().getConnection().getConnection();
                PreparedStatement ps = con.prepareStatement("INSERT INTO product_images(ProductID, ImagePath, IsMain) VALUES (?, ?, ?)");
                ps.setInt(1, Integer.parseInt(productId.getText().trim()));
                ps.setString(2, imagePath.getText());
                ps.setBoolean(3, isMain.isSelected());
                ps.executeUpdate();
                showProductImagesPage();
            } catch (Exception ex) { showAlert(ex.getMessage()); }
        });
        form.getChildren().addAll(productId, imagePath, isMain, add);
        addBottomForm(form);
    }

    private void showProductVariantsPage() {
        ensureTableUpgradesExist();
        showTable("SELECT v.VariantID, p.Name AS Product, v.ColorName, v.SizeValue, v.Barcode, v.Price, v.Quantity FROM product_variants v JOIN products p ON v.ProductID=p.ProductID ORDER BY v.VariantID DESC");
        pageTitle.setText("Product Variants");
        HBox form = new HBox(10);
        TextField productId = new TextField(); productId.setPromptText("ProductID");
        TextField color = new TextField(); color.setPromptText("Color");
        TextField size = new TextField(); size.setPromptText("Size");
        TextField barcode = new TextField(); barcode.setPromptText("Barcode");
        TextField price = new TextField(); price.setPromptText("Price");
        TextField qty = new TextField(); qty.setPromptText("Qty");
        Button add = createSmallButton("Add Variant");
        add.setOnAction(e -> {
            try {
                Connection con = new DataBaseConnection().getConnection().getConnection();
                PreparedStatement ps = con.prepareStatement("INSERT INTO product_variants(ProductID, ColorName, SizeValue, Barcode, Price, Quantity) VALUES (?, ?, ?, ?, ?, ?)");
                ps.setInt(1, Integer.parseInt(productId.getText().trim()));
                ps.setString(2, color.getText());
                ps.setString(3, size.getText());
                ps.setString(4, barcode.getText());
                ps.setDouble(5, Double.parseDouble(price.getText().trim()));
                ps.setInt(6, Integer.parseInt(qty.getText().trim()));
                ps.executeUpdate();
                showProductVariantsPage();
            } catch (Exception ex) { showAlert(ex.getMessage()); }
        });
        form.getChildren().addAll(productId, color, size, barcode, price, qty, add);
        addBottomForm(form);
    }

    private void showTableManagerPage() {
        ensureTableUpgradesExist();
        VBox page = new VBox(12);
        page.setPadding(new Insets(20));
        page.setStyle("-fx-background-color:#FFF7F2;");

        Label title = new Label("Table Manager - Database Upgrades 1 to 18");
        title.setStyle("-fx-font-size:24px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");

        String info =
                "1 products extra columns\n" +
                        "2 customers extra columns\n" +
                        "3 sales payment/status columns\n" +
                        "4 branch_inventory MinQuantity\n" +
                        "5 categories\n" +
                        "6 product_images\n" +
                        "7 product_colors\n" +
                        "8 product_variants\n" +
                        "9 order_status_history\n" +
                        "10 payment_transactions\n" +
                        "11 delivery_drivers\n" +
                        "12 delivery_assignments\n" +
                        "13 suppliers\n" +
                        "14 purchase_orders\n" +
                        "15 purchase_order_items\n" +
                        "16 product_barcodes\n" +
                        "17 price_history\n" +
                        "18 expenses_categories + system_settings";

        TextArea area = new TextArea(info);
        area.setPrefHeight(380);
        area.setWrapText(true);

        Button create = createSmallButton("Create / Fix All Tables");
        create.setOnAction(e -> {
            ensureTableUpgradesExist();
            showSuccess("All table upgrades are ready.");
        });

        page.getChildren().addAll(title, area, create);
        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        contentBox.getChildren().set(2, sp);
        pageTitle.setText("Table Manager");
    }

    private void addBottomForm(HBox form) {
        form.setPadding(new Insets(10));
        form.setAlignment(Pos.CENTER_LEFT);
        form.setStyle("-fx-background-color:white; -fx-background-radius:14; -fx-border-color:#E7CFC4; -fx-border-radius:14;");
        if (contentBox.getChildren().size() > 3) contentBox.getChildren().set(3, form);
        else contentBox.getChildren().add(form);
    }



    // =========================================================
    // TOP 5 PROFESSIONAL FEATURES
    // 1 Add Product + Edit Product
    // 2 Choose Image Button
    // 3 Low Stock Page
    // 4 Order Details Popup
    // 5 Customer My Orders as Cards
    // =========================================================

    private void ensureTop5TablesExist() {
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            if (con == null) return;

            addColumnIfMissingTop5(con, "products", "Description", "TEXT");
            addColumnIfMissingTop5(con, "products", "Material", "VARCHAR(100)");
            addColumnIfMissingTop5(con, "products", "CareInstructions", "TEXT");
            addColumnIfMissingTop5(con, "products", "IsActive", "BOOLEAN DEFAULT TRUE");
            addColumnIfMissingTop5(con, "products", "CreatedAt", "DATETIME DEFAULT CURRENT_TIMESTAMP");

            addColumnIfMissingTop5(con, "branch_inventory", "MinQuantity", "INT DEFAULT 2");

            Statement st = con.createStatement();
            st.executeUpdate("CREATE TABLE IF NOT EXISTS product_extra_info (" +
                    "ProductID INT PRIMARY KEY, " +
                    "Description TEXT, " +
                    "Material VARCHAR(120), " +
                    "CareInstructions TEXT, " +
                    "FitType VARCHAR(80), " +
                    "Season VARCHAR(80), " +
                    "IsActive BOOLEAN DEFAULT 1)");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void addColumnIfMissingTop5(Connection con, String tableName, String columnName, String definition) {
        try {
            PreparedStatement ps = con.prepareStatement(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=? AND COLUMN_NAME=?"
            );
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            ResultSet rs = ps.executeQuery();

            if (rs.next() && rs.getInt(1) == 0) {
                Statement st = con.createStatement();
                st.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Professional user management screen for Owners:
     * a live table of every user (employees + customers) plus an Add / Edit / Delete form.
     * Clicking a row loads that user into the form for editing.
     */
    private void showManageUsersPage() {
        pageTitle.setText("Manage Users");

        VBox page = new VBox(18);
        page.setPadding(new Insets(22));
        page.setStyle("-fx-background-color:#FFF7F2;");

        Label title = createSectionTitle("👥 User Management");
        Label subtitle = new Label("Click any row below to edit it, or fill the form to add a brand new user.");
        subtitle.setStyle("-fx-font-size:12px; -fx-text-fill:#8A7570;");

        // ---- users table ----
        TableView<String[]> usersTable = new TableView<>();
        usersTable.setPrefHeight(280);
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        usersTable.setStyle("-fx-background-color:white; -fx-border-color:#E7CFC4; -fx-border-radius:12; -fx-background-radius:12;");

        String usersSQL =
                "SELECT u.UserID, u.FullName, u.Username, u.Role, " +
                "IFNULL(e.JobTitle,'-') AS JobTitle, IFNULL(e.Salary,0) AS Salary, " +
                "IFNULL(b.Name,'-') AS Branch, IFNULL(c.Phone,'-') AS Phone " +
                "FROM users u " +
                "LEFT JOIN employees e ON u.UserID=e.UserID " +
                "LEFT JOIN branches b ON e.BranchID=b.BranchID " +
                "LEFT JOIN customers c ON u.UserID=c.UserID " +
                "ORDER BY u.Role, u.FullName";

        Runnable[] reloadRef = new Runnable[1];
        Runnable reloadUsers = () -> {
            usersTable.getColumns().clear();
            usersTable.getItems().clear();
            try {
                Connection con = new DataBaseConnection().getConnection().getConnection();
                PreparedStatement ps = con.prepareStatement(usersSQL);
                ResultSet rs = ps.executeQuery();
                int colCount = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= colCount; i++) {
                    final int idx = i - 1;
                    String colName = rs.getMetaData().getColumnLabel(i);
                    TableColumn<String[], String> col = new TableColumn<>(colName);
                    col.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[idx]));
                    usersTable.getColumns().add(col);
                }
                while (rs.next()) {
                    String[] row = new String[colCount];
                    for (int i = 1; i <= colCount; i++) row[i - 1] = rs.getString(i);
                    usersTable.getItems().add(row);
                }
            } catch (Exception ex) { showAlert(ex.getMessage()); }
        };
        reloadRef[0] = reloadUsers;
        reloadUsers.run();

        // ---- form ----
        TextField fullNameField = createStyledField("Full name");
        TextField usernameField = createStyledField("Username (login)");
        TextField passwordField = createStyledField("Password");
        ComboBox<String> roleCb = createStyledCombo("Role");
        roleCb.getItems().addAll("OWNER", "MANAGER", "CASHIER", "WAREHOUSE", "CUSTOMER");

        ComboBox<String[]> branchCb = createStyledCombo("Branch (employees only)");
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            ResultSet rs = con.createStatement().executeQuery("SELECT BranchID, Name FROM branches ORDER BY Name");
            while (rs.next()) branchCb.getItems().add(new String[]{String.valueOf(rs.getInt("BranchID")), rs.getString("Name")});
        } catch (Exception ignored) {}
        branchCb.setConverter(new javafx.util.StringConverter<String[]>() {
            @Override public String toString(String[] b) { return b == null ? "" : b[1]; }
            @Override public String[] fromString(String s) { return null; }
        });

        TextField jobTitleField = createStyledField("Job title (e.g. Cashier, Branch Manager)");
        TextField salaryField = createStyledField("Salary (₪)");
        TextField phoneField = createStyledField("Phone (customers only)");

        Label selectedLbl = new Label("Adding a NEW user");
        selectedLbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#C98F7B;");
        Label formMsg = new Label();
        formMsg.setStyle("-fx-font-weight:bold;");
        final int[] editingUserId = {-1};

        Runnable clearForm = () -> {
            editingUserId[0] = -1;
            selectedLbl.setText("Adding a NEW user");
            fullNameField.clear(); usernameField.clear(); passwordField.clear();
            roleCb.setValue(null); branchCb.setValue(null);
            jobTitleField.clear(); salaryField.clear(); phoneField.clear();
            usersTable.getSelectionModel().clearSelection();
        };

        usersTable.setOnMouseClicked(e -> {
            String[] row = usersTable.getSelectionModel().getSelectedItem();
            if (row == null) return;
            editingUserId[0] = Integer.parseInt(row[0]);
            selectedLbl.setText("Editing User #" + row[0] + " (" + row[3] + ")");
            fullNameField.setText(row[1]);
            usernameField.setText(row[2]);
            passwordField.clear();
            passwordField.setPromptText("Leave blank to keep current password");
            roleCb.setValue(row[3]);
            jobTitleField.setText(row[4].equals("-") ? "" : row[4]);
            salaryField.setText(row[5].equals("0") || row[5].equals("0.0") ? "" : row[5]);
            phoneField.setText(row[7].equals("-") ? "" : row[7]);
            // Branch isn't easily reverse-mapped from name to ID in this row; left to re-pick if changing branch.
        });

        Button addBtn = createSuccessButton("➕ Add New User");
        addBtn.setOnAction(e -> {
            try {
                if (fullNameField.getText().trim().isEmpty()) throw new Exception("Full name is required.");
                if (usernameField.getText().trim().isEmpty()) throw new Exception("Username is required.");
                if (passwordField.getText().trim().isEmpty()) throw new Exception("Password is required for a new user.");
                if (roleCb.getValue() == null) throw new Exception("Please choose a role.");

                int userId = createUserAccount(
                        fullNameField.getText().trim(), usernameField.getText().trim(),
                        passwordField.getText().trim(), roleCb.getValue(),
                        branchCb.getValue() == null ? null : Integer.parseInt(branchCb.getValue()[0]),
                        jobTitleField.getText().trim(),
                        salaryField.getText().trim().isEmpty() ? 0 : Double.parseDouble(salaryField.getText().trim()),
                        phoneField.getText().trim()
                );
                formMsg.setStyle("-fx-text-fill:#27AE60; -fx-font-weight:bold;");
                formMsg.setText("User #" + userId + " created successfully.");
                clearForm.run();
                reloadRef[0].run();
            } catch (Exception ex) {
                formMsg.setStyle("-fx-text-fill:#E53935; -fx-font-weight:bold;");
                formMsg.setText(ex.getMessage());
            }
        });

        Button updateBtn = createSmallButton("💾 Save Changes to Selected");
        updateBtn.setOnAction(e -> {
            if (editingUserId[0] == -1) { formMsg.setStyle("-fx-text-fill:#E53935;"); formMsg.setText("Select a user in the table first."); return; }
            try {
                updateUserAccount(
                        editingUserId[0], fullNameField.getText().trim(),
                        passwordField.getText().trim().isEmpty() ? null : passwordField.getText().trim(),
                        branchCb.getValue() == null ? null : Integer.parseInt(branchCb.getValue()[0]),
                        jobTitleField.getText().trim(),
                        salaryField.getText().trim().isEmpty() ? null : Double.parseDouble(salaryField.getText().trim()),
                        phoneField.getText().trim()
                );
                formMsg.setStyle("-fx-text-fill:#27AE60; -fx-font-weight:bold;");
                formMsg.setText("User #" + editingUserId[0] + " updated successfully.");
                reloadRef[0].run();
            } catch (Exception ex) {
                formMsg.setStyle("-fx-text-fill:#E53935; -fx-font-weight:bold;");
                formMsg.setText(ex.getMessage());
            }
        });

        Button deleteBtn = createDangerButton("🗑️ Delete Selected");
        deleteBtn.setOnAction(e -> {
            if (editingUserId[0] == -1) { formMsg.setStyle("-fx-text-fill:#E53935;"); formMsg.setText("Select a user in the table first."); return; }
            if (editingUserId[0] == currentUserId) { formMsg.setStyle("-fx-text-fill:#E53935;"); formMsg.setText("You cannot delete the account you are currently logged in with."); return; }
            boolean ok = confirmAction("Delete User", "This will permanently delete user #" + editingUserId[0] + ". Continue?");
            if (!ok) return;
            try {
                deleteUserCascade(editingUserId[0]);
                formMsg.setStyle("-fx-text-fill:#27AE60; -fx-font-weight:bold;");
                formMsg.setText("User deleted.");
                clearForm.run();
                reloadRef[0].run();
            } catch (Exception ex) {
                formMsg.setStyle("-fx-text-fill:#E53935; -fx-font-weight:bold;");
                formMsg.setText(ex.getMessage());
            }
        });

        Button clearBtn = createGhostButton("✕ Clear Form");
        clearBtn.setOnAction(e -> { clearForm.run(); formMsg.setText(""); });

        GridPane form = new GridPane();
        form.setHgap(14); form.setVgap(12);
        form.add(labelOver("Full Name", fullNameField), 0, 0);
        form.add(labelOver("Username", usernameField), 1, 0);
        form.add(labelOver("Password", passwordField), 2, 0);
        form.add(labelOver("Role", roleCb), 0, 1);
        form.add(labelOver("Branch (employees)", branchCb), 1, 1);
        form.add(labelOver("Job Title (employees)", jobTitleField), 2, 1);
        form.add(labelOver("Salary (employees)", salaryField), 0, 2);
        form.add(labelOver("Phone (customers)", phoneField), 1, 2);

        HBox actionRow = new HBox(12, addBtn, updateBtn, deleteBtn, clearBtn);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        VBox formBox = new VBox(14, selectedLbl, form, actionRow, formMsg);
        formBox.setPadding(new Insets(20));
        formBox.setStyle("-fx-background-color:white; -fx-background-radius:16; -fx-border-color:#E7CFC4; -fx-border-radius:16;");

        page.getChildren().addAll(title, subtitle, usersTable, createDivider("Add / Edit User"), formBox);

        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#FFF7F2;");
        contentBox.getChildren().set(2, sp);
    }

    /** Creates a brand new login (users row) and the matching employees or customers row. */
    private int createUserAccount(String fullName, String username, String password, String role,
                                   Integer branchId, String jobTitle, double salary, String phone) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();
        if (con == null) throw new Exception("No database connection.");
        con.setAutoCommit(false);
        try {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO users(FullName, Username, Password, Role) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, fullName);
            ps.setString(2, username);
            ps.setString(3, password);
            ps.setString(4, role);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            int userId = keys.getInt(1);

            if (role.equals("CUSTOMER")) {
                PreparedStatement cps = con.prepareStatement("INSERT INTO customers(UserID, Phone) VALUES (?, ?)");
                cps.setInt(1, userId);
                cps.setString(2, phone == null ? "" : phone);
                cps.executeUpdate();
            } else {
                PreparedStatement eps = con.prepareStatement(
                        "INSERT INTO employees(UserID, BranchID, JobTitle, Salary) VALUES (?, ?, ?, ?)");
                eps.setInt(1, userId);
                if (branchId == null) eps.setNull(2, Types.INTEGER); else eps.setInt(2, branchId);
                eps.setString(3, jobTitle == null || jobTitle.isEmpty() ? role : jobTitle);
                eps.setDouble(4, salary);
                eps.executeUpdate();
            }

            con.commit();
            return userId;
        } catch (Exception ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }

    /** Updates an existing user's name/password plus employee or customer details. Password is left unchanged if null. */
    private void updateUserAccount(int userId, String fullName, String password,
                                    Integer branchId, String jobTitle, Double salary, String phone) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();
        if (con == null) throw new Exception("No database connection.");

        if (password != null && !password.isEmpty()) {
            PreparedStatement ps = con.prepareStatement("UPDATE users SET FullName=?, Password=? WHERE UserID=?");
            ps.setString(1, fullName);
            ps.setString(2, password);
            ps.setInt(3, userId);
            ps.executeUpdate();
        } else {
            PreparedStatement ps = con.prepareStatement("UPDATE users SET FullName=? WHERE UserID=?");
            ps.setString(1, fullName);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }

        // Update employee row if it exists
        PreparedStatement checkEmp = con.prepareStatement("SELECT 1 FROM employees WHERE UserID=?");
        checkEmp.setInt(1, userId);
        if (checkEmp.executeQuery().next()) {
            PreparedStatement eps = con.prepareStatement(
                    "UPDATE employees SET BranchID=?, JobTitle=?, Salary=? WHERE UserID=?");
            if (branchId == null) eps.setNull(1, Types.INTEGER); else eps.setInt(1, branchId);
            eps.setString(2, jobTitle == null || jobTitle.isEmpty() ? "Employee" : jobTitle);
            eps.setDouble(3, salary == null ? 0 : salary);
            eps.setInt(4, userId);
            eps.executeUpdate();
        }

        // Update customer row if it exists
        PreparedStatement checkCust = con.prepareStatement("SELECT 1 FROM customers WHERE UserID=?");
        checkCust.setInt(1, userId);
        if (checkCust.executeQuery().next() && phone != null) {
            PreparedStatement cps = con.prepareStatement("UPDATE customers SET Phone=? WHERE UserID=?");
            cps.setString(1, phone);
            cps.setInt(2, userId);
            cps.executeUpdate();
        }
    }

    /** Deletes a user and every row across the schema that references them, so the delete button never fails on a foreign key. */
    private void deleteUserCascade(int userId) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();
        con.setAutoCommit(false);
        try {
            // Resolve a CustomerID if this user is a customer, since several tables reference CustomerID not UserID.
            Integer customerId = null;
            PreparedStatement custLookup = con.prepareStatement("SELECT CustomerID FROM customers WHERE UserID=?");
            custLookup.setInt(1, userId);
            ResultSet crs = custLookup.executeQuery();
            if (crs.next()) customerId = crs.getInt("CustomerID");

            if (customerId != null) {
                String[] customerTables = {
                        "wishlist", "product_reviews", "customer_addresses", "return_requests",
                        "customer_loyalty", "loyalty_transactions", "online_order_items"
                };
                for (String t : customerTables) {
                    try { con.createStatement().executeUpdate("DELETE FROM " + t + " WHERE CustomerID=" + customerId); }
                    catch (Exception ignored) {}
                }
                try { con.createStatement().executeUpdate("DELETE FROM online_orders WHERE CustomerID=" + customerId); } catch (Exception ignored) {}
                try { con.createStatement().executeUpdate("UPDATE sales SET CustomerID=NULL WHERE CustomerID=" + customerId); } catch (Exception ignored) {}
            }

            String[] userTables = {
                    "cash_drawer_movements", "stock_requests", "daily_closing",
                    "employee_attendance", "activity_logs", "user_login_history", "notifications"
            };
            for (String t : userTables) {
                try { con.createStatement().executeUpdate("DELETE FROM " + t + " WHERE UserID=" + userId); }
                catch (Exception ignored) {}
                try { con.createStatement().executeUpdate("DELETE FROM " + t + " WHERE CashierUserID=" + userId); }
                catch (Exception ignored) {}
            }
            try { con.createStatement().executeUpdate("UPDATE sales SET CashierUserID=" + userId + " WHERE CashierUserID=" + userId); } catch (Exception ignored) {}

            con.createStatement().executeUpdate("DELETE FROM customers WHERE UserID=" + userId);
            con.createStatement().executeUpdate("DELETE FROM employees WHERE UserID=" + userId);
            con.createStatement().executeUpdate("DELETE FROM users WHERE UserID=" + userId);

            con.commit();
        } catch (Exception ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }

    /**
     * Professional product management screen for Owners:
     * a live table of every product (with thumbnail) plus an Add / Edit / Delete form below it.
     * Clicking a row loads that product into the form for editing.
     * Images stay on the local disk exactly as chosen - we only ever store the file path.
     */
    private void showManageProductsPage() {
        ensureTop5TablesExist();
        pageTitle.setText("Manage Products");

        VBox page = new VBox(18);
        page.setPadding(new Insets(22));
        page.setStyle("-fx-background-color:#FFF7F2;");

        Label title = createSectionTitle("📦 Product Management");
        Label subtitle = new Label("Click any row below to edit it, or fill the form to add a brand new product.");
        subtitle.setStyle("-fx-font-size:12px; -fx-text-fill:#8A7570;");

        // ---- products table ----
        TableView<String[]> productsTable = new TableView<>();
        productsTable.setPrefHeight(280);
        productsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        productsTable.setStyle("-fx-background-color:white; -fx-border-color:#E7CFC4; -fx-border-radius:12; -fx-background-radius:12;");

        // ---- form fields ----
        TextField nameField = createStyledField("Product name");
        ComboBox<String> categoryCb = createStyledCombo("Category");
        categoryCb.getItems().addAll("Blouses", "Dresses", "Pants", "Shoes", "Abayas");
        categoryCb.setEditable(true);
        TextField colorField = createStyledField("Color");
        TextField priceField = createStyledField("Sell price (₪)");
        TextField costField = createStyledField("Cost price (₪)");
        TextField imagePathField = createStyledField("Local image path (kept exactly as selected)");
        imagePathField.setPrefWidth(420);
        CheckBox activeCheck = new CheckBox("Active / visible to customers");
        activeCheck.setSelected(true);
        activeCheck.setStyle("-fx-font-size:13px;");

        Label sizesCaption = new Label("Sizes for a NEW product (comma separated, e.g. S,M,L,XL):");
        sizesCaption.setStyle("-fx-font-size:11px; -fx-text-fill:#8A7570;");
        TextField sizesField = createStyledField("S,M,L,XL");
        TextField qtyField = createStyledField("Starting quantity per size, per branch");

        TextArea descField = new TextArea();
        descField.setPromptText("Description (optional)");
        descField.setPrefHeight(60);
        descField.setStyle("-fx-background-color:white; -fx-border-color:#E7CFC4; -fx-border-radius:10; -fx-background-radius:10; -fx-font-size:13px;");

        Button chooseImage = createGhostButton("📁 Browse Local Image");
        chooseImage.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Choose Product Image (kept in its current folder)");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.webp"));
            File selected = chooser.showOpenDialog(null);
            if (selected != null) {
                // We do NOT copy or move the file - we keep the exact local path the user picked.
                imagePathField.setText(selected.getAbsolutePath().replace("\\", "/"));
            }
        });

        Label formMsg = new Label();
        formMsg.setStyle("-fx-font-weight:bold;");

        Label selectedIdLbl = new Label("Adding a NEW product");
        selectedIdLbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#C98F7B;");
        final int[] editingProductId = {-1};

        Runnable clearForm = () -> {
            editingProductId[0] = -1;
            selectedIdLbl.setText("Adding a NEW product");
            nameField.clear(); categoryCb.setValue(null); colorField.clear();
            priceField.clear(); costField.clear(); imagePathField.clear();
            descField.clear(); sizesField.clear(); qtyField.clear();
            activeCheck.setSelected(true);
            productsTable.getSelectionModel().clearSelection();
        };

        Runnable[] reloadTableRef = new Runnable[1];
        Runnable reloadTable = () -> {
            productsTable.getColumns().clear();
            productsTable.getItems().clear();
            try {
                Connection con = new DataBaseConnection().getConnection().getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT p.ProductID, p.ImagePath AS Product, p.Name, p.Category, p.Color, p.Price, p.CostPrice, " +
                        "IFNULL(p.IsActive,1) AS IsActive, " +
                        "IFNULL((SELECT SUM(Quantity) FROM branch_inventory bi WHERE bi.ProductID=p.ProductID),0) AS TotalStock " +
                        "FROM products p ORDER BY p.Category, p.Name, p.Color");
                ResultSet rs = ps.executeQuery();
                int colCount = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= colCount; i++) {
                    final int idx = i - 1;
                    String colName = rs.getMetaData().getColumnLabel(i);
                    TableColumn<String[], String> col = new TableColumn<>(colName);
                    col.setCellValueFactory(d -> new SimpleStringProperty(d.getValue()[idx]));
                    if (colName.equals("Product")) {
                        col.setPrefWidth(70);
                        col.setCellFactory(c -> new TableCell<String[], String>() {
                            private final ImageView iv = new ImageView();
                            { iv.setFitWidth(40); iv.setFitHeight(48); iv.setPreserveRatio(true); }
                            @Override protected void updateItem(String path, boolean empty) {
                                super.updateItem(path, empty);
                                if (empty || path == null || path.isBlank()) { setGraphic(null); return; }
                                try {
                                    File f = new File(path);
                                    if (f.exists()) { iv.setImage(new Image(f.toURI().toString())); setGraphic(iv); }
                                    else setGraphic(null);
                                } catch (Exception ex) { setGraphic(null); }
                            }
                        });
                    }
                    productsTable.getColumns().add(col);
                }
                while (rs.next()) {
                    String[] row = new String[colCount];
                    for (int i = 1; i <= colCount; i++) row[i - 1] = rs.getString(i);
                    productsTable.getItems().add(row);
                }
            } catch (Exception ex) { showAlert(ex.getMessage()); }
        };
        reloadTableRef[0] = reloadTable;
        reloadTable.run();

        productsTable.setOnMouseClicked(e -> {
            String[] row = productsTable.getSelectionModel().getSelectedItem();
            if (row == null) return;
            editingProductId[0] = Integer.parseInt(row[0]);
            selectedIdLbl.setText("Editing Product #" + row[0]);
            imagePathField.setText(row[1] == null ? "" : row[1]);
            nameField.setText(row[2]);
            categoryCb.setValue(row[3]);
            colorField.setText(row[4]);
            priceField.setText(row[5]);
            costField.setText(row[6]);
            activeCheck.setSelected(row[7] == null || row[7].equals("1") || row[7].equalsIgnoreCase("true"));
        });

        Button saveNewBtn = createSuccessButton("➕ Add New Product");
        saveNewBtn.setOnAction(e -> {
            try {
                int productId = saveNewProduct(
                        nameField.getText(), categoryCb.getValue() == null ? "" : categoryCb.getValue(),
                        colorField.getText(),
                        Double.parseDouble(priceField.getText().trim()),
                        costField.getText().trim().isEmpty() ? 0 : Double.parseDouble(costField.getText().trim()),
                        imagePathField.getText(),
                        sizesField.getText(),
                        qtyField.getText().trim().isEmpty() ? 0 : Integer.parseInt(qtyField.getText().trim()),
                        descField.getText(),
                        activeCheck.isSelected()
                );
                formMsg.setStyle("-fx-text-fill:#27AE60; -fx-font-weight:bold;");
                formMsg.setText("Product #" + productId + " added successfully.");
                clearForm.run();
                reloadTableRef[0].run();
            } catch (Exception ex) {
                formMsg.setStyle("-fx-text-fill:#E53935; -fx-font-weight:bold;");
                formMsg.setText(ex.getMessage());
            }
        });

        Button updateBtn = createSmallButton("💾 Save Changes to Selected");
        updateBtn.setOnAction(e -> {
            if (editingProductId[0] == -1) { formMsg.setStyle("-fx-text-fill:#E53935;"); formMsg.setText("Select a product in the table first."); return; }
            try {
                updateProductFull(
                        editingProductId[0], nameField.getText(),
                        categoryCb.getValue() == null ? "" : categoryCb.getValue(),
                        colorField.getText(),
                        Double.parseDouble(priceField.getText().trim()),
                        costField.getText().trim().isEmpty() ? 0 : Double.parseDouble(costField.getText().trim()),
                        imagePathField.getText(),
                        activeCheck.isSelected()
                );
                formMsg.setStyle("-fx-text-fill:#27AE60; -fx-font-weight:bold;");
                formMsg.setText("Product #" + editingProductId[0] + " updated successfully.");
                reloadTableRef[0].run();
            } catch (Exception ex) {
                formMsg.setStyle("-fx-text-fill:#E53935; -fx-font-weight:bold;");
                formMsg.setText(ex.getMessage());
            }
        });

        Button deleteBtn = createDangerButton("🗑️ Delete Selected");
        deleteBtn.setOnAction(e -> {
            if (editingProductId[0] == -1) { formMsg.setStyle("-fx-text-fill:#E53935;"); formMsg.setText("Select a product in the table first."); return; }
            boolean ok = confirmAction("Delete Product", "This will permanently delete product #" + editingProductId[0] +
                    " and all of its sizes / inventory rows. This cannot be undone. Continue?");
            if (!ok) return;
            try {
                deleteProductCascade(editingProductId[0]);
                formMsg.setStyle("-fx-text-fill:#27AE60; -fx-font-weight:bold;");
                formMsg.setText("Product deleted.");
                clearForm.run();
                reloadTableRef[0].run();
            } catch (Exception ex) {
                formMsg.setStyle("-fx-text-fill:#E53935; -fx-font-weight:bold;");
                formMsg.setText(ex.getMessage());
            }
        });

        Button clearBtn = createGhostButton("✕ Clear Form");
        clearBtn.setOnAction(e -> { clearForm.run(); formMsg.setText(""); });

        GridPane form = new GridPane();
        form.setHgap(14); form.setVgap(12);
        form.add(labelOver("Name", nameField), 0, 0);
        form.add(labelOver("Category", categoryCb), 1, 0);
        form.add(labelOver("Color", colorField), 2, 0);
        form.add(labelOver("Sell Price (₪)", priceField), 0, 1);
        form.add(labelOver("Cost Price (₪)", costField), 1, 1);
        form.add(labelOver("Active", activeCheck), 2, 1);

        HBox imgRow = new HBox(10, imagePathField, chooseImage);
        imgRow.setAlignment(Pos.CENTER_LEFT);
        form.add(labelOver("Image (local file path)", imgRow), 0, 2, 3, 1);

        VBox newProductSizes = new VBox(4, sizesCaption, new HBox(10, sizesField, qtyField));
        form.add(newProductSizes, 0, 3, 3, 1);

        form.add(labelOver("Description", descField), 0, 4, 3, 1);

        HBox actionRow = new HBox(12, saveNewBtn, updateBtn, deleteBtn, clearBtn);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        VBox formBox = new VBox(14, selectedIdLbl, form, actionRow, formMsg);
        formBox.setPadding(new Insets(20));
        formBox.setStyle("-fx-background-color:white; -fx-background-radius:16; -fx-border-color:#E7CFC4; -fx-border-radius:16;");

        page.getChildren().addAll(title, subtitle, productsTable, createDivider("Add / Edit Product"), formBox);

        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#FFF7F2;");
        contentBox.getChildren().set(2, sp);
    }

    /** Small helper: stacks a caption label above any control, used in the Manage Products / Users forms. */
    private VBox labelOver(String caption, javafx.scene.Node control) {
        Label l = new Label(caption);
        l.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#8A7570;");
        return new VBox(4, l, control);
    }

    /** Full update (name/category/color/price/cost/image/active) used by the new Manage Products screen. */
    private void updateProductFull(int productId, String name, String category, String color,
                                    double price, double cost, String imagePath, boolean active) throws Exception {
        if (name == null || name.trim().isEmpty()) throw new Exception("Product name is required.");
        Connection con = new DataBaseConnection().getConnection().getConnection();

        double oldPrice = 0;
        try {
            PreparedStatement old = con.prepareStatement("SELECT Price FROM products WHERE ProductID=?");
            old.setInt(1, productId);
            ResultSet rs = old.executeQuery();
            if (rs.next()) oldPrice = rs.getDouble("Price");
        } catch (Exception ignored) {}

        PreparedStatement ps = con.prepareStatement(
                "UPDATE products SET Name=?, Category=?, Color=?, Price=?, CostPrice=?, ImagePath=?, IsActive=? WHERE ProductID=?"
        );
        ps.setString(1, name.trim());
        ps.setString(2, category == null ? "" : category.trim());
        ps.setString(3, color == null ? "" : color.trim());
        ps.setDouble(4, price);
        ps.setDouble(5, cost);
        ps.setString(6, imagePath == null ? "" : imagePath.trim());
        ps.setBoolean(7, active);
        ps.setInt(8, productId);
        ps.executeUpdate();

        try {
            PreparedStatement hist = con.prepareStatement(
                    "INSERT INTO price_history(ProductID, OldPrice, NewPrice, ChangedBy, ChangedAt) VALUES (?, ?, ?, ?, NOW())"
            );
            hist.setInt(1, productId);
            hist.setDouble(2, oldPrice);
            hist.setDouble(3, price);
            hist.setInt(4, currentUserId);
            hist.executeUpdate();
        } catch (Exception ignored) {}
    }

    /** Deletes a product and every row that references it, so the delete button never fails on a foreign key. */
    private void deleteProductCascade(int productId) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();
        con.setAutoCommit(false);
        try {
            String[] cleanupTables = {
                    "sale_items", "online_order_items", "wishlist", "product_reviews",
                    "branch_inventory", "warehouse_inventory", "stock_requests",
                    "warehouse_movements", "stock_history", "price_history",
                    "product_variants", "product_images", "product_colors",
                    "product_barcodes", "purchase_order_items", "product_extra_info"
            };
            for (String t : cleanupTables) {
                try {
                    con.createStatement().executeUpdate("DELETE FROM " + t + " WHERE ProductID=" + productId);
                } catch (Exception ignored) { /* table might not exist or have no ProductID column */ }
            }
            con.createStatement().executeUpdate("DELETE FROM product_sizes WHERE ProductID=" + productId);
            con.createStatement().executeUpdate("DELETE FROM products WHERE ProductID=" + productId);
            con.commit();
        } catch (Exception ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }

    private int saveNewProduct(String name, String category, String color, double price, double cost, String imagePath,
                               String sizesText, int quantity, String description, boolean active) throws Exception {
        if (name == null || name.trim().isEmpty()) throw new Exception("Product name is required.");
        if (category == null || category.trim().isEmpty()) throw new Exception("Category is required.");
        if (color == null || color.trim().isEmpty()) throw new Exception("Color is required.");
        if (sizesText == null || sizesText.trim().isEmpty()) throw new Exception("Sizes are required.");
        if (quantity < 0) throw new Exception("Quantity cannot be negative.");

        Connection con = new DataBaseConnection().getConnection().getConnection();
        if (con == null) throw new Exception("No database connection.");

        con.setAutoCommit(false);

        try {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO products(Name, Category, Price, CostPrice, ImagePath, Color, Description, IsActive, CreatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, name.trim());
            ps.setString(2, category.trim());
            ps.setDouble(3, price);
            ps.setDouble(4, cost);
            ps.setString(5, imagePath == null ? "" : imagePath.trim());
            ps.setString(6, color.trim());
            ps.setString(7, description);
            ps.setBoolean(8, active);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            int productId = keys.getInt(1);

            String[] sizes = sizesText.split(",");
            ArrayList<Integer> sizeIds = new ArrayList<>();

            for (String raw : sizes) {
                String size = raw.trim();
                if (size.isEmpty()) continue;

                PreparedStatement sizePs = con.prepareStatement(
                        "INSERT INTO product_sizes(ProductID, SizeValue) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                sizePs.setInt(1, productId);
                sizePs.setString(2, size);
                sizePs.executeUpdate();

                ResultSet sizeKeys = sizePs.getGeneratedKeys();
                sizeKeys.next();
                sizeIds.add(sizeKeys.getInt(1));
            }

            PreparedStatement branches = con.prepareStatement("SELECT BranchID FROM branches");
            ResultSet br = branches.executeQuery();

            while (br.next()) {
                int branchId = br.getInt("BranchID");
                for (int sizeId : sizeIds) {
                    PreparedStatement inv = con.prepareStatement(
                            "INSERT INTO branch_inventory(BranchID, ProductID, SizeID, Quantity, MinQuantity) VALUES (?, ?, ?, ?, 2)"
                    );
                    inv.setInt(1, branchId);
                    inv.setInt(2, productId);
                    inv.setInt(3, sizeId);
                    inv.setInt(4, quantity);
                    inv.executeUpdate();
                }
            }

            try {
                PreparedStatement extra = con.prepareStatement(
                        "INSERT INTO product_extra_info(ProductID, Description, Material, CareInstructions, FitType, Season, IsActive) VALUES (?, ?, 'Mixed fabric', 'Hand wash recommended', 'Regular fit', 'All season', ?)"
                );
                extra.setInt(1, productId);
                extra.setString(2, description);
                extra.setBoolean(3, active);
                extra.executeUpdate();
            } catch (Exception ignored) {
            }

            con.commit();
            return productId;

        } catch (Exception ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }

    private void updateProductBasic(int productId, double price, double cost, boolean active) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();

        double oldPrice = 0;
        try {
            PreparedStatement old = con.prepareStatement("SELECT Price FROM products WHERE ProductID=?");
            old.setInt(1, productId);
            ResultSet rs = old.executeQuery();
            if (rs.next()) oldPrice = rs.getDouble("Price");
        } catch (Exception ignored) {
        }

        PreparedStatement ps = con.prepareStatement(
                "UPDATE products SET Price=?, CostPrice=?, IsActive=? WHERE ProductID=?"
        );
        ps.setDouble(1, price);
        ps.setDouble(2, cost);
        ps.setBoolean(3, active);
        ps.setInt(4, productId);
        ps.executeUpdate();

        try {
            PreparedStatement hist = con.prepareStatement(
                    "INSERT INTO price_history(ProductID, OldPrice, NewPrice, ChangedBy, ChangedAt) VALUES (?, ?, ?, ?, NOW())"
            );
            hist.setInt(1, productId);
            hist.setDouble(2, oldPrice);
            hist.setDouble(3, price);
            hist.setInt(4, currentUserId);
            hist.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private void showLowStockPage() {
        ensureTop5TablesExist();
        showTable("SELECT b.Name AS Branch, p.ProductID, p.Name AS Product, p.Category, p.Color, ps.SizeValue AS Size, " +
                "bi.Quantity AS Remaining, IFNULL(bi.MinQuantity, 2) AS MinQuantity " +
                "FROM branch_inventory bi " +
                "JOIN branches b ON bi.BranchID=b.BranchID " +
                "JOIN products p ON bi.ProductID=p.ProductID " +
                "JOIN product_sizes ps ON bi.SizeID=ps.SizeID " +
                "WHERE bi.Quantity <= IFNULL(bi.MinQuantity, 2) " +
                "ORDER BY bi.Quantity ASC, p.Name");
        pageTitle.setText("Low Stock Products");
    }

    private void openOrderDetailsPopup(int orderId) throws Exception {
        Stage stage = new Stage();
        stage.setTitle("Order Details #" + orderId);

        Connection con = new DataBaseConnection().getConnection().getConnection();

        PreparedStatement orderPs = con.prepareStatement(
                "SELECT oo.OnlineOrderID, u.FullName AS Customer, IFNULL(oo.Phone, IFNULL(c.Phone, '-')) AS Phone, " +
                        "oo.ReceiveMethod, IFNULL(oo.DeliveryArea, '-') AS DeliveryArea, IFNULL(oo.DeliveryAddress, '-') AS DeliveryAddress, " +
                        "IFNULL(oo.PaymentMethod, '-') AS PaymentMethod, oo.Status, oo.DeliveryFee, oo.TotalAmount " +
                        "FROM online_orders oo JOIN customers c ON oo.CustomerID=c.CustomerID JOIN users u ON c.UserID=u.UserID " +
                        "WHERE oo.OnlineOrderID=?"
        );
        orderPs.setInt(1, orderId);
        ResultSet orderRs = orderPs.executeQuery();

        if (!orderRs.next()) {
            throw new Exception("Order not found.");
        }

        String status = orderRs.getString("Status");

        Label title = createSectionTitle("📦 Order #" + orderId + " Details");

        Label statusBadge = new Label(status);
        String statusColor;
        if ("DELIVERED".equals(status)) statusColor = "#27AE60";
        else if ("CANCELLED".equals(status) || "REJECTED".equals(status)) statusColor = "#E53935";
        else if ("PENDING".equals(status)) statusColor = "#F39C12";
        else statusColor = "#2980B9";
        statusBadge.setStyle("-fx-background-color:" + statusColor + "; -fx-text-fill:white; -fx-font-weight:bold; " +
                "-fx-font-size:12px; -fx-padding:4 14 4 14; -fx-background-radius:14;");

        HBox titleRow = new HBox(14, title, statusBadge);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(24);
        infoGrid.setVgap(6);
        infoGrid.setPadding(new Insets(16));
        infoGrid.setStyle("-fx-background-color:white; -fx-background-radius:14; -fx-border-color:#E7CFC4; -fx-border-radius:14;");
        addOrderInfoRow(infoGrid, 0, "Customer", orderRs.getString("Customer"));
        addOrderInfoRow(infoGrid, 1, "Phone", orderRs.getString("Phone"));
        addOrderInfoRow(infoGrid, 2, "Method", orderRs.getString("ReceiveMethod"));
        addOrderInfoRow(infoGrid, 3, "Area", orderRs.getString("DeliveryArea"));
        addOrderInfoRow(infoGrid, 4, "Address", orderRs.getString("DeliveryAddress"));
        addOrderInfoRow(infoGrid, 5, "Payment", orderRs.getString("PaymentMethod"));
        addOrderInfoRow(infoGrid, 6, "Delivery Fee", "₪" + orderRs.getDouble("DeliveryFee"));
        addOrderInfoRow(infoGrid, 7, "Total", "₪" + orderRs.getDouble("TotalAmount"));

        TableView<String[]> itemsTable = createMiniTable(
                "SELECT p.Name AS Product, p.Color, ps.SizeValue AS Size, ooi.Quantity, ooi.UnitPrice, " +
                        "(ooi.Quantity * ooi.UnitPrice) AS LineTotal " +
                        "FROM online_order_items ooi " +
                        "JOIN products p ON ooi.ProductID=p.ProductID " +
                        "JOIN product_sizes ps ON ooi.SizeID=ps.SizeID " +
                        "WHERE ooi.OnlineOrderID=" + orderId
        );
        itemsTable.setPrefHeight(220);

        boolean hasItems = !itemsTable.getItems().isEmpty();

        // Make the reason for an empty table obvious instead of JavaFX's generic "No content in table".
        Label itemsWarning = new Label();
        itemsWarning.setWrapText(true);
        itemsWarning.setStyle("-fx-text-fill:#E53935; -fx-font-weight:bold; -fx-font-size:13px; " +
                "-fx-background-color:#FDEDEC; -fx-padding:10 14 10 14; -fx-background-radius:10;");
        if (!hasItems) {
            itemsWarning.setText("⚠️ No items are recorded for this order. The order_items rows may be missing " +
                    "or this order was created before items were saved correctly. Contact the customer to confirm what was ordered.");
            itemsTable.setPlaceholder(new Label("(empty - see warning above)"));
        }

        Button accept = createSmallButton("✓ Accept");
        accept.setOnAction(e -> {
            try {
                updateOnlineOrderSimpleStatus(orderId, "ACCEPTED", "Manager accepted the order");
                stage.close();
                showAdminOnlineOrders();
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        Button reject = createDangerButton("✕ Reject");
        reject.setOnAction(e -> {
            TextInputDialog d = new TextInputDialog();
            d.setTitle("Reject Order");
            d.setHeaderText("Reject reason");
            d.setContentText("Reason:");
            Optional<String> result = d.showAndWait();
            result.ifPresent(reason -> {
                try {
                    Connection c = new DataBaseConnection().getConnection().getConnection();
                    PreparedStatement ps = c.prepareStatement("UPDATE online_orders SET Status='REJECTED', RejectReason=? WHERE OnlineOrderID=?");
                    ps.setString(1, reason);
                    ps.setInt(2, orderId);
                    ps.executeUpdate();
                    updateOnlineOrderSimpleStatus(orderId, "REJECTED", reason);
                    stage.close();
                    showAdminOnlineOrders();
                } catch (Exception ex) {
                    showAlert(ex.getMessage());
                }
            });
        });

        Button contact = createGhostButton("📞 Contact Customer");
        contact.setOnAction(e -> {
            try {
                openContactCustomerWindow(orderId);
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        Button print = createSmallButton("🖨️ Print Invoice");
        print.setDisable(!hasItems);
        if (!hasItems) {
            Tooltip tip = new Tooltip("Cannot print an invoice with no recorded items.");
            Tooltip.install(print, tip);
        }
        print.setOnAction(e -> {
            try {
                printOnlineInvoice(orderId);
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        HBox actions = new HBox(10, accept, reject, contact, print);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox itemsBox = new VBox(8, createDivider("Items"));
        if (!hasItems) itemsBox.getChildren().add(itemsWarning);
        itemsBox.getChildren().add(itemsTable);

        VBox root = new VBox(16, titleRow, infoGrid, itemsBox, actions);
        root.setPadding(new Insets(24));
        root.setStyle("-fx-background-color:#FFF7F2;");

        ScrollPane sp = new ScrollPane(root);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:#FFF7F2;");

        stage.setScene(new Scene(sp, 820, 680));
        stage.show();
    }

    private void addOrderInfoRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label);
        l.setStyle("-fx-font-size:12px; -fx-text-fill:#8A7570;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#2C1810;");
        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }

    private void showCustomerOrdersCards(int customerUserId) {
        pageTitle.setText("My Orders");

        VBox page = new VBox(14);
        page.setPadding(new Insets(20));
        page.setStyle("-fx-background-color:#FFF7F2;");

        Label title = new Label("My Orders");
        title.setStyle("-fx-font-size:26px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");
        page.getChildren().add(title);

        try {
            int customerId = getCustomerIdByUserId(customerUserId);
            Connection con = new DataBaseConnection().getConnection().getConnection();

            PreparedStatement ps = con.prepareStatement(
                    "SELECT OnlineOrderID, OrderDate, ReceiveMethod, Status, DeliveryFee, TotalAmount, IFNULL(RejectReason, '-') AS RejectReason " +
                            "FROM online_orders WHERE CustomerID=? ORDER BY OnlineOrderID DESC"
            );
            ps.setInt(1, customerId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int orderId = rs.getInt("OnlineOrderID");
                String status = rs.getString("Status");

                Label cardTitle = new Label("Order #" + orderId + " - " + status);
                cardTitle.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");

                Label details = new Label(
                        "Date: " + rs.getString("OrderDate") +
                                "\nMethod: " + rs.getString("ReceiveMethod") +
                                "\nDelivery Fee: " + rs.getDouble("DeliveryFee") +
                                "\nTotal: " + rs.getDouble("TotalAmount") +
                                "\nReject Reason: " + rs.getString("RejectReason")
                );
                details.setStyle("-fx-text-fill:#7A5C52;");

                Button view = createSmallButton("View Details");
                view.setOnAction(e -> {
                    try {
                        openOrderDetailsPopup(orderId);
                    } catch (Exception ex) {
                        showAlert(ex.getMessage());
                    }
                });

                Button cancel = createSmallButton("Cancel");
                cancel.setDisable(!"PENDING".equals(status));
                cancel.setOnAction(e -> {
                    try {
                        cancelCustomerOnlineOrder(customerUserId, orderId);
                        showCustomerOrdersCards(customerUserId);
                    } catch (Exception ex) {
                        showAlert(ex.getMessage());
                    }
                });

                HBox actions = new HBox(10, view, cancel);
                VBox card = new VBox(8, cardTitle, details, actions);
                card.setPadding(new Insets(15));
                card.setStyle("-fx-background-color:white; -fx-background-radius:16; -fx-border-color:#E7CFC4; -fx-border-radius:16;");
                page.getChildren().add(card);
            }

        } catch (Exception ex) {
            Label error = new Label(ex.getMessage());
            page.getChildren().add(error);
        }

        ScrollPane sp = new ScrollPane(page);
        sp.setFitToWidth(true);
        contentBox.getChildren().set(2, sp);
    }



    // =========================================================
    // GENERAL ROLE UPDATES: MANAGER + CASHIER + WAREHOUSE
    // =========================================================

    private void ensureGeneralRoleTablesExist() {
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            if (con == null) return;
            Statement st = con.createStatement();

            st.executeUpdate("CREATE TABLE IF NOT EXISTS stock_request_notes (" +
                    "NoteID INT PRIMARY KEY AUTO_INCREMENT, RequestID INT NULL, UserID INT NULL, NoteText TEXT, CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS daily_closing (" +
                    "ClosingID INT PRIMARY KEY AUTO_INCREMENT, CashierUserID INT NOT NULL, BranchID INT NOT NULL, ClosingDate DATE NOT NULL, " +
                    "SystemCash DOUBLE DEFAULT 0, CountedCash DOUBLE DEFAULT 0, DifferenceAmount DOUBLE DEFAULT 0, Notes TEXT, CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS sale_returns (" +
                    "ReturnID INT PRIMARY KEY AUTO_INCREMENT, SaleID INT NOT NULL, CashierUserID INT NOT NULL, BranchID INT NOT NULL, " +
                    "ReturnAmount DOUBLE DEFAULT 0, Reason TEXT, ReturnDate DATETIME DEFAULT CURRENT_TIMESTAMP)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS damaged_items (" +
                    "DamageID INT PRIMARY KEY AUTO_INCREMENT, WarehouseID INT NULL, ProductID INT NOT NULL, SizeID INT NOT NULL, Quantity INT NOT NULL, " +
                    "Reason TEXT, UserID INT NULL, DamageDate DATETIME DEFAULT CURRENT_TIMESTAMP)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS stock_count (" +
                    "CountID INT PRIMARY KEY AUTO_INCREMENT, WarehouseID INT NULL, ProductID INT NOT NULL, SizeID INT NOT NULL, " +
                    "SystemQuantity INT DEFAULT 0, CountedQuantity INT DEFAULT 0, DifferenceQuantity INT DEFAULT 0, UserID INT NULL, CountDate DATETIME DEFAULT CURRENT_TIMESTAMP)");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ========================= MANAGER =========================

    private void showManagerDashboard(int managerUserId) {
        try {
            int branchId = getManagerBranchId(managerUserId);
            Connection con = new DataBaseConnection().getConnection().getConnection();

            VBox page = new VBox(14);
            page.setPadding(new Insets(20));
            page.setStyle("-fx-background-color:#FFF7F2;");

            Label title = new Label("Manager Dashboard");
            title.setStyle("-fx-font-size:28px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");

            GridPane cards = new GridPane();
            cards.setHgap(15);
            cards.setVgap(15);

            cards.add(makeStatCard("Today Sales", getSingleDouble(con, "SELECT IFNULL(SUM(FinalAmount),0) FROM sales WHERE BranchID=" + branchId + " AND SaleDate=CURDATE()")), 0, 0);
            cards.add(makeStatCard("Today Invoices", getSingleDouble(con, "SELECT COUNT(*) FROM sales WHERE BranchID=" + branchId + " AND SaleDate=CURDATE()")), 1, 0);
            cards.add(makeStatCard("Low Stock", getSingleDouble(con, "SELECT COUNT(*) FROM branch_inventory WHERE BranchID=" + branchId + " AND Quantity<=IFNULL(MinQuantity,2)")), 2, 0);
            cards.add(makeStatCard("Pending Requests", getSingleDouble(con, "SELECT COUNT(*) FROM stock_requests WHERE BranchID=" + branchId + " AND Status='PENDING'")), 0, 1);
            cards.add(makeStatCard("Cashiers", getSingleDouble(con, "SELECT COUNT(*) FROM employees e JOIN users u ON e.UserID=u.UserID WHERE e.BranchID=" + branchId + " AND u.Role='CASHIER'")), 1, 1);

            Button low = createSmallButton("View Branch Low Stock");
            low.setOnAction(e -> showBranchLowStock(managerUserId));

            Button sales = createSmallButton("View Today Sales");
            sales.setOnAction(e -> showBranchSalesToday(managerUserId));

            page.getChildren().addAll(title, cards, new HBox(10, low, sales));

            ScrollPane sp = new ScrollPane(page);
            sp.setFitToWidth(true);
            contentBox.getChildren().set(2, sp);
            pageTitle.setText("Manager Dashboard");

        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    private VBox makeStatCard(String title, double value) {
        Label t = new Label(title);
        t.setStyle("-fx-font-size:14px; -fx-text-fill:#7A5C52;");
        Label v = new Label(String.valueOf(value));
        v.setStyle("-fx-font-size:24px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");
        VBox card = new VBox(8, t, v);
        card.setPadding(new Insets(18));
        card.setPrefWidth(220);
        card.setStyle("-fx-background-color:white; -fx-background-radius:18; -fx-border-color:#E7CFC4; -fx-border-radius:18;");
        return card;
    }

    private double getSingleDouble(Connection con, String sql) {
        try {
            ResultSet rs = con.createStatement().executeQuery(sql);
            if (rs.next()) return rs.getDouble(1);
        } catch (Exception ignored) {}
        return 0;
    }

    private int getManagerBranchId(int managerUserId) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT BranchID FROM employees WHERE UserID=?");
        ps.setInt(1, managerUserId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt("BranchID");
        throw new Exception("Manager branch not found.");
    }

    private void showBranchSalesToday(int managerUserId) {
        try {
            int branchId = getManagerBranchId(managerUserId);
            showTable("SELECT s.SaleID, u.FullName AS Cashier, s.SaleDate, s.TotalAmount, s.DiscountAmount, s.FinalAmount " +
                    "FROM sales s JOIN users u ON s.CashierUserID=u.UserID " +
                    "WHERE s.BranchID=" + branchId + " AND s.SaleDate=CURDATE() ORDER BY s.SaleID DESC");
            pageTitle.setText("Branch Sales Today");
        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    private void showBranchLowStock(int managerUserId) {
        try {
            int branchId = getManagerBranchId(managerUserId);
            showTable("SELECT b.Name AS Branch, p.ProductID, p.Name AS Product, p.Category, p.Color, ps.SizeValue AS Size, " +
                    "bi.Quantity AS Remaining, IFNULL(bi.MinQuantity,2) AS MinQuantity " +
                    "FROM branch_inventory bi " +
                    "JOIN branches b ON bi.BranchID=b.BranchID " +
                    "JOIN products p ON bi.ProductID=p.ProductID " +
                    "JOIN product_sizes ps ON bi.SizeID=ps.SizeID " +
                    "WHERE bi.BranchID=" + branchId + " AND bi.Quantity<=IFNULL(bi.MinQuantity,2) " +
                    "ORDER BY bi.Quantity ASC");
            pageTitle.setText("Branch Low Stock");
        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    private void showBranchCashiers(int managerUserId) {
        try {
            int branchId = getManagerBranchId(managerUserId);
            showTable("SELECT u.UserID, u.FullName, u.Username, e.JobTitle, e.Salary, " +
                    "IFNULL(SUM(CASE WHEN s.SaleDate=CURDATE() THEN s.FinalAmount ELSE 0 END),0) AS TodaySales " +
                    "FROM employees e JOIN users u ON e.UserID=u.UserID " +
                    "LEFT JOIN sales s ON s.CashierUserID=u.UserID " +
                    "WHERE e.BranchID=" + branchId + " AND u.Role='CASHIER' " +
                    "GROUP BY u.UserID, u.FullName, u.Username, e.JobTitle, e.Salary");
            pageTitle.setText("Branch Cashiers");
        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    private void showBranchOnlineOrders(int managerUserId) {
        try {
            int branchId = getManagerBranchId(managerUserId);
            showTable("SELECT oo.OnlineOrderID, u.FullName AS Customer, oo.Phone, oo.ReceiveMethod, oo.DeliveryAddress, oo.Status, oo.TotalAmount " +
                    "FROM online_orders oo " +
                    "JOIN customers c ON oo.CustomerID=c.CustomerID " +
                    "JOIN users u ON c.UserID=u.UserID " +
                    "ORDER BY oo.OnlineOrderID DESC");
            pageTitle.setText("Branch Online Orders");
        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    private void openCreateStockRequestWindow(int managerUserId) {
        Stage stage = new Stage();
        stage.setTitle("Create Stock Request");

        TextField productId = new TextField();
        productId.setPromptText("ProductID");

        TextField sizeId = new TextField();
        sizeId.setPromptText("SizeID");

        TextField qty = new TextField();
        qty.setPromptText("Requested Quantity");

        TextField note = new TextField();
        note.setPromptText("Note optional");

        Label msg = new Label();

        Button save = createSmallButton("Send Request");
        save.setOnAction(e -> {
            try {
                int branchId = getManagerBranchId(managerUserId);
                Connection con = new DataBaseConnection().getConnection().getConnection();

                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO stock_requests(BranchID, ProductID, SizeID, RequestedQuantity, RequestDate, Status) VALUES (?, ?, ?, ?, CURDATE(), 'PENDING')",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setInt(1, branchId);
                ps.setInt(2, Integer.parseInt(productId.getText().trim()));
                ps.setInt(3, Integer.parseInt(sizeId.getText().trim()));
                ps.setInt(4, Integer.parseInt(qty.getText().trim()));
                ps.executeUpdate();

                ResultSet keys = ps.getGeneratedKeys();
                int requestId = 0;
                if (keys.next()) requestId = keys.getInt(1);

                try {
                    PreparedStatement n = con.prepareStatement("INSERT INTO stock_request_notes(RequestID, UserID, NoteText, CreatedAt) VALUES (?, ?, ?, NOW())");
                    n.setInt(1, requestId);
                    n.setInt(2, managerUserId);
                    n.setString(3, note.getText());
                    n.executeUpdate();
                } catch (Exception ignored) {}

                msg.setText("Stock request sent.");
            } catch (Exception ex) {
                msg.setText(ex.getMessage());
            }
        });

        VBox root = new VBox(12, new Label("Create Stock Request"), productId, sizeId, qty, note, save, msg);
        root.setPadding(new Insets(20));
        stage.setScene(new Scene(root, 420, 420));
        stage.show();
    }

    private void showMyStockRequests(int managerUserId) {
        try {
            int branchId = getManagerBranchId(managerUserId);
            showTable("SELECT sr.RequestID, p.Name AS Product, p.Color, ps.SizeValue AS Size, sr.RequestedQuantity, sr.RequestDate, sr.Status " +
                    "FROM stock_requests sr JOIN products p ON sr.ProductID=p.ProductID JOIN product_sizes ps ON sr.SizeID=ps.SizeID " +
                    "WHERE sr.BranchID=" + branchId + " ORDER BY sr.RequestID DESC");
            pageTitle.setText("My Stock Requests");
        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    // ========================= CASHIER =========================

    private void showCashierPOS(int cashierUserId) {
        Stage stage = new Stage();
        stage.setTitle("Cashier POS");

        TextField productId = new TextField();
        productId.setPromptText("ProductID");

        TextField sizeId = new TextField();
        sizeId.setPromptText("SizeID");

        TextField qty = new TextField("1");
        qty.setPromptText("Qty");

        TextField customerId = new TextField();
        customerId.setPromptText("CustomerID optional");

        Label info = new Label();
        info.setStyle("-fx-text-fill:#5A3E36; -fx-font-weight:bold;");

        Button sell = createSmallButton("Quick Sell");
        sell.setOnAction(e -> {
            try {
                int branchId = getCashierBranchId(cashierUserId);
                int p = Integer.parseInt(productId.getText().trim());
                int s = Integer.parseInt(sizeId.getText().trim());
                int q = Integer.parseInt(qty.getText().trim());
                int available = getAvailableQuantity(branchId, p, s);
                if (q <= 0) throw new Exception("Quantity must be greater than zero.");
                if (q > available) throw new Exception("Not enough stock. Available = " + available);

                double price = getProductPrice(p);
                Integer cId = customerId.getText().trim().isEmpty() ? null : Integer.parseInt(customerId.getText().trim());
                int saleId = createQuickSale(cashierUserId, branchId, cId, p, s, q, price);
                info.setText("Sale saved. SaleID = " + saleId + " Total = " + (q * price));
            } catch (Exception ex) {
                info.setText(ex.getMessage());
            }
        });

        Button products = createSmallButton("Show My Products");
        products.setOnAction(e -> {
            stage.close();
            showTable(cashierBranchProductsSQL(cashierUserId));
            pageTitle.setText("My Branch Products");
        });

        VBox root = new VBox(12, new Label("Cashier POS - Quick Sale"), productId, sizeId, qty, customerId, new HBox(10, sell, products), info);
        root.setPadding(new Insets(20));
        stage.setScene(new Scene(root, 430, 390));
        stage.show();
    }

    private int createQuickSale(int cashierUserId, int branchId, Integer customerId, int productId, int sizeId, int qty, double price) throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();
        con.setAutoCommit(false);
        try {
            double total = qty * price;

            PreparedStatement sale = con.prepareStatement(
                    "INSERT INTO sales(BranchID, CustomerID, CashierUserID, SaleDate, TotalAmount, DiscountAmount, FinalAmount, PaymentMethod, Status) VALUES (?, ?, ?, CURDATE(), ?, 0, ?, 'Cash', 'COMPLETED')",
                    Statement.RETURN_GENERATED_KEYS
            );
            sale.setInt(1, branchId);
            if (customerId == null) sale.setNull(2, Types.INTEGER); else sale.setInt(2, customerId);
            sale.setInt(3, cashierUserId);
            sale.setDouble(4, total);
            sale.setDouble(5, total);
            sale.executeUpdate();

            ResultSet keys = sale.getGeneratedKeys();
            keys.next();
            int saleId = keys.getInt(1);

            PreparedStatement item = con.prepareStatement("INSERT INTO sale_items(SaleID, ProductID, SizeID, Quantity, UnitPrice) VALUES (?, ?, ?, ?, ?)");
            item.setInt(1, saleId);
            item.setInt(2, productId);
            item.setInt(3, sizeId);
            item.setInt(4, qty);
            item.setDouble(5, price);
            item.executeUpdate();

            PreparedStatement inv = con.prepareStatement("UPDATE branch_inventory SET Quantity=Quantity-? WHERE BranchID=? AND ProductID=? AND SizeID=?");
            inv.setInt(1, qty);
            inv.setInt(2, branchId);
            inv.setInt(3, productId);
            inv.setInt(4, sizeId);
            inv.executeUpdate();

            PreparedStatement cash = con.prepareStatement("INSERT INTO cash_drawer_movements(CashierUserID, BranchID, MovementType, Amount, MovementDate, Notes) VALUES (?, ?, 'SALE', ?, NOW(), ?)");
            cash.setInt(1, cashierUserId);
            cash.setInt(2, branchId);
            cash.setDouble(3, total);
            cash.setString(4, "POS quick sale #" + saleId);
            cash.executeUpdate();

            con.commit();
            return saleId;
        } catch (Exception ex) {
            con.rollback();
            throw ex;
        } finally {
            con.setAutoCommit(true);
        }
    }

    private void showDailyClosingPage(int cashierUserId) {
        Stage stage = new Stage();
        stage.setTitle("Daily Closing");

        TextField counted = new TextField();
        counted.setPromptText("Counted cash in drawer");

        TextField notes = new TextField();
        notes.setPromptText("Notes");

        Label msg = new Label();

        double systemCash = 0;
        int branchId = 0;
        try {
            branchId = getCashierBranchId(cashierUserId);
            Connection con = new DataBaseConnection().getConnection().getConnection();
            systemCash = getSingleDouble(con, "SELECT IFNULL(SUM(Amount),0) FROM cash_drawer_movements WHERE CashierUserID=" + cashierUserId + " AND BranchID=" + branchId + " AND DATE(MovementDate)=CURDATE()");
        } catch (Exception ignored) {}

        Label systemLabel = new Label("System cash today = " + systemCash);
        int finalBranchId = branchId;
        double finalSystemCash = systemCash;

        Button save = createSmallButton("Save Closing");
        save.setOnAction(e -> {
            try {
                double countedCash = Double.parseDouble(counted.getText().trim());
                double diff = countedCash - finalSystemCash;
                Connection con = new DataBaseConnection().getConnection().getConnection();

                PreparedStatement ps = con.prepareStatement("INSERT INTO daily_closing(CashierUserID, BranchID, ClosingDate, SystemCash, CountedCash, DifferenceAmount, Notes, CreatedAt) VALUES (?, ?, CURDATE(), ?, ?, ?, ?, NOW())");
                ps.setInt(1, cashierUserId);
                ps.setInt(2, finalBranchId);
                ps.setDouble(3, finalSystemCash);
                ps.setDouble(4, countedCash);
                ps.setDouble(5, diff);
                ps.setString(6, notes.getText());
                ps.executeUpdate();

                msg.setText("Closing saved. Difference = " + diff);
            } catch (Exception ex) {
                msg.setText(ex.getMessage());
            }
        });

        VBox root = new VBox(12, new Label("Daily Cash Closing"), systemLabel, counted, notes, save, msg);
        root.setPadding(new Insets(20));
        stage.setScene(new Scene(root, 430, 330));
        stage.show();
    }

    private void openReturnSaleWindow(int cashierUserId) {
        Stage stage = new Stage();
        stage.setTitle("Return Sale");

        TextField saleId = new TextField();
        saleId.setPromptText("SaleID");

        TextField amount = new TextField();
        amount.setPromptText("Return Amount");

        TextField reason = new TextField();
        reason.setPromptText("Reason");

        Label msg = new Label();

        Button save = createSmallButton("Save Return");
        save.setOnAction(e -> {
            try {
                int branchId = getCashierBranchId(cashierUserId);
                Connection con = new DataBaseConnection().getConnection().getConnection();

                PreparedStatement ps = con.prepareStatement("INSERT INTO sale_returns(SaleID, CashierUserID, BranchID, ReturnAmount, Reason, ReturnDate) VALUES (?, ?, ?, ?, ?, NOW())");
                ps.setInt(1, Integer.parseInt(saleId.getText().trim()));
                ps.setInt(2, cashierUserId);
                ps.setInt(3, branchId);
                ps.setDouble(4, Double.parseDouble(amount.getText().trim()));
                ps.setString(5, reason.getText());
                ps.executeUpdate();

                PreparedStatement cash = con.prepareStatement("INSERT INTO cash_drawer_movements(CashierUserID, BranchID, MovementType, Amount, MovementDate, Notes) VALUES (?, ?, 'RETURN', ?, NOW(), ?)");
                cash.setInt(1, cashierUserId);
                cash.setInt(2, branchId);
                cash.setDouble(3, -Double.parseDouble(amount.getText().trim()));
                cash.setString(4, "Return for sale #" + saleId.getText());
                cash.executeUpdate();

                msg.setText("Return saved.");
            } catch (Exception ex) {
                msg.setText(ex.getMessage());
            }
        });

        VBox root = new VBox(12, new Label("Return Sale"), saleId, amount, reason, save, msg);
        root.setPadding(new Insets(20));
        stage.setScene(new Scene(root, 420, 360));
        stage.show();
    }

    private void printLastCashierReceipt(int cashierUserId) {
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            PreparedStatement ps = con.prepareStatement(
                    "SELECT s.SaleID, s.BranchID, s.TotalAmount, s.DiscountAmount, s.FinalAmount, s.DiscountID, " +
                    "IFNULL(u.FullName,'Walk-in Customer') AS CustomerName, d.Code AS DiscountCode, d.Percentage AS DiscountPct " +
                    "FROM sales s " +
                    "LEFT JOIN customers c ON s.CustomerID=c.CustomerID " +
                    "LEFT JOIN users u ON c.UserID=u.UserID " +
                    "LEFT JOIN discounts d ON s.DiscountID=d.DiscountID " +
                    "WHERE s.CashierUserID=? ORDER BY s.SaleID DESC LIMIT 1");
            ps.setInt(1, cashierUserId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) { showAlert("You have not made any sales yet today."); return; }

            int saleId = rs.getInt("SaleID");
            int branchId = rs.getInt("BranchID");
            double subtotal = rs.getDouble("TotalAmount");
            double discountAmount = rs.getDouble("DiscountAmount");
            double finalAmount = rs.getDouble("FinalAmount");
            String customerName = rs.getString("CustomerName");
            String discountCode = rs.getString("DiscountCode");
            double discountPct = rs.getDouble("DiscountPct");

            PreparedStatement itemsPs = con.prepareStatement(
                    "SELECT p.Name, p.Color, ps.SizeValue, si.Quantity, si.UnitPrice " +
                    "FROM sale_items si JOIN products p ON si.ProductID=p.ProductID JOIN product_sizes ps ON si.SizeID=ps.SizeID " +
                    "WHERE si.SaleID=?");
            itemsPs.setInt(1, saleId);
            ResultSet irs = itemsPs.executeQuery();

            ArrayList<PosCartLine> lines = new ArrayList<>();
            while (irs.next()) {
                lines.add(new PosCartLine(0, 0,
                        irs.getString("Name") + " - " + irs.getString("Color"),
                        irs.getString("SizeValue"), irs.getInt("Quantity"), irs.getDouble("UnitPrice")));
            }

            openInvoiceWindow(saleId, branchId, cashierUserId, customerName, discountCode, discountPct,
                    lines, subtotal, discountAmount, finalAmount);
        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    // ========================= WAREHOUSE =========================

    private void showWarehouseDashboard(int warehouseUserId) {
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            VBox page = new VBox(14);
            page.setPadding(new Insets(20));
            page.setStyle("-fx-background-color:#FFF7F2;");

            Label title = new Label("Warehouse Dashboard");
            title.setStyle("-fx-font-size:28px; -fx-font-weight:bold; -fx-text-fill:#5A3E36;");

            GridPane cards = new GridPane();
            cards.setHgap(15);
            cards.setVgap(15);
            cards.add(makeStatCard("Warehouse Items", getSingleDouble(con, "SELECT COUNT(*) FROM warehouse_inventory")), 0, 0);
            cards.add(makeStatCard("Total Qty", getSingleDouble(con, "SELECT IFNULL(SUM(Quantity),0) FROM warehouse_inventory")), 1, 0);
            cards.add(makeStatCard("Pending Requests", getSingleDouble(con, "SELECT COUNT(*) FROM stock_requests WHERE Status='PENDING'")), 2, 0);
            cards.add(makeStatCard("Damaged Items", getSingleDouble(con, "SELECT IFNULL(SUM(Quantity),0) FROM damaged_items")), 0, 1);

            Button pending = createSmallButton("Pending Stock Requests");
            pending.setOnAction(e -> showPendingStockRequests());

            Button send = createSmallButton("Send Stock to Branch");
            send.setOnAction(e -> openSendStockToBranchWindow(warehouseUserId));

            page.getChildren().addAll(title, cards, new HBox(10, pending, send));

            ScrollPane sp = new ScrollPane(page);
            sp.setFitToWidth(true);
            contentBox.getChildren().set(2, sp);
            pageTitle.setText("Warehouse Dashboard");

        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    private void showPendingStockRequests() {
        showTable("SELECT sr.RequestID, b.Name AS Branch, p.Name AS Product, p.Color, ps.SizeValue AS Size, sr.RequestedQuantity, sr.RequestDate, sr.Status " +
                "FROM stock_requests sr JOIN branches b ON sr.BranchID=b.BranchID " +
                "JOIN products p ON sr.ProductID=p.ProductID JOIN product_sizes ps ON sr.SizeID=ps.SizeID " +
                "WHERE sr.Status='PENDING' ORDER BY sr.RequestID DESC");
        pageTitle.setText("Pending Stock Requests");
    }

    private int getFirstWarehouseId() throws Exception {
        Connection con = new DataBaseConnection().getConnection().getConnection();
        ResultSet rs = con.createStatement().executeQuery("SELECT WarehouseID FROM warehouses ORDER BY WarehouseID LIMIT 1");
        if (rs.next()) return rs.getInt("WarehouseID");
        throw new Exception("No warehouse found.");
    }

    private void openSendStockToBranchWindow(int userId) {
        Stage stage = new Stage();
        stage.setTitle("Send Stock to Branch");

        TextField branchId = new TextField();
        branchId.setPromptText("BranchID");

        TextField productId = new TextField();
        productId.setPromptText("ProductID");

        TextField sizeId = new TextField();
        sizeId.setPromptText("SizeID");

        TextField qty = new TextField();
        qty.setPromptText("Quantity");

        Label msg = new Label();

        Button send = createSmallButton("Send");
        send.setOnAction(e -> {
            try {
                int warehouseId = getFirstWarehouseId();
                int b = Integer.parseInt(branchId.getText().trim());
                int p = Integer.parseInt(productId.getText().trim());
                int s = Integer.parseInt(sizeId.getText().trim());
                int q = Integer.parseInt(qty.getText().trim());

                Connection con = new DataBaseConnection().getConnection().getConnection();
                con.setAutoCommit(false);
                try {
                    PreparedStatement dec = con.prepareStatement("UPDATE warehouse_inventory SET Quantity=Quantity-? WHERE WarehouseID=? AND ProductID=? AND SizeID=? AND Quantity>=?");
                    dec.setInt(1, q);
                    dec.setInt(2, warehouseId);
                    dec.setInt(3, p);
                    dec.setInt(4, s);
                    dec.setInt(5, q);
                    int updated = dec.executeUpdate();
                    if (updated == 0) throw new Exception("Not enough warehouse quantity.");

                    PreparedStatement inc = con.prepareStatement("INSERT INTO branch_inventory(BranchID, ProductID, SizeID, Quantity) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE Quantity=Quantity+VALUES(Quantity)");
                    inc.setInt(1, b);
                    inc.setInt(2, p);
                    inc.setInt(3, s);
                    inc.setInt(4, q);
                    inc.executeUpdate();

                    PreparedStatement move = con.prepareStatement("INSERT INTO warehouse_movements(WarehouseID, ProductID, SizeID, MovementType, Quantity, MovementDate) VALUES (?, ?, ?, 'OUT', ?, NOW())");
                    move.setInt(1, warehouseId);
                    move.setInt(2, p);
                    move.setInt(3, s);
                    move.setInt(4, q);
                    move.executeUpdate();

                    con.commit();
                    msg.setText("Stock sent to branch.");
                } catch (Exception ex) {
                    con.rollback();
                    throw ex;
                } finally {
                    con.setAutoCommit(true);
                }
            } catch (Exception ex) {
                msg.setText(ex.getMessage());
            }
        });

        VBox root = new VBox(12, new Label("Send Stock to Branch"), branchId, productId, sizeId, qty, send, msg);
        root.setPadding(new Insets(20));
        stage.setScene(new Scene(root, 420, 420));
        stage.show();
    }

    private void openReceivePurchaseWindow(int userId) {
        Stage stage = new Stage();
        stage.setTitle("Receive Purchase");

        TextField productId = new TextField();
        productId.setPromptText("ProductID");

        TextField sizeId = new TextField();
        sizeId.setPromptText("SizeID");

        TextField qty = new TextField();
        qty.setPromptText("Quantity");

        Label msg = new Label();

        Button receive = createSmallButton("Receive");
        receive.setOnAction(e -> {
            try {
                int warehouseId = getFirstWarehouseId();
                int p = Integer.parseInt(productId.getText().trim());
                int s = Integer.parseInt(sizeId.getText().trim());
                int q = Integer.parseInt(qty.getText().trim());
                Connection con = new DataBaseConnection().getConnection().getConnection();

                PreparedStatement ps = con.prepareStatement("INSERT INTO warehouse_inventory(WarehouseID, ProductID, SizeID, Quantity) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE Quantity=Quantity+VALUES(Quantity)");
                ps.setInt(1, warehouseId);
                ps.setInt(2, p);
                ps.setInt(3, s);
                ps.setInt(4, q);
                ps.executeUpdate();

                PreparedStatement move = con.prepareStatement("INSERT INTO warehouse_movements(WarehouseID, ProductID, SizeID, MovementType, Quantity, MovementDate) VALUES (?, ?, ?, 'IN', ?, NOW())");
                move.setInt(1, warehouseId);
                move.setInt(2, p);
                move.setInt(3, s);
                move.setInt(4, q);
                move.executeUpdate();

                msg.setText("Purchase received.");
            } catch (Exception ex) {
                msg.setText(ex.getMessage());
            }
        });

        VBox root = new VBox(12, new Label("Receive Purchase"), productId, sizeId, qty, receive, msg);
        root.setPadding(new Insets(20));
        stage.setScene(new Scene(root, 420, 360));
        stage.show();
    }

    private void openDamagedItemsWindow(int userId) {
        Stage stage = new Stage();
        stage.setTitle("Damaged Items");

        TextField productId = new TextField();
        productId.setPromptText("ProductID");

        TextField sizeId = new TextField();
        sizeId.setPromptText("SizeID");

        TextField qty = new TextField();
        qty.setPromptText("Quantity");

        TextField reason = new TextField();
        reason.setPromptText("Reason");

        Label msg = new Label();

        Button save = createSmallButton("Save Damage");
        save.setOnAction(e -> {
            try {
                int warehouseId = getFirstWarehouseId();
                int p = Integer.parseInt(productId.getText().trim());
                int s = Integer.parseInt(sizeId.getText().trim());
                int q = Integer.parseInt(qty.getText().trim());

                Connection con = new DataBaseConnection().getConnection().getConnection();
                PreparedStatement dec = con.prepareStatement("UPDATE warehouse_inventory SET Quantity=Quantity-? WHERE WarehouseID=? AND ProductID=? AND SizeID=? AND Quantity>=?");
                dec.setInt(1, q);
                dec.setInt(2, warehouseId);
                dec.setInt(3, p);
                dec.setInt(4, s);
                dec.setInt(5, q);
                int ok = dec.executeUpdate();
                if (ok == 0) throw new Exception("Not enough warehouse stock.");

                PreparedStatement ps = con.prepareStatement("INSERT INTO damaged_items(WarehouseID, ProductID, SizeID, Quantity, Reason, UserID, DamageDate) VALUES (?, ?, ?, ?, ?, ?, NOW())");
                ps.setInt(1, warehouseId);
                ps.setInt(2, p);
                ps.setInt(3, s);
                ps.setInt(4, q);
                ps.setString(5, reason.getText());
                ps.setInt(6, userId);
                ps.executeUpdate();

                msg.setText("Damaged item recorded.");
            } catch (Exception ex) {
                msg.setText(ex.getMessage());
            }
        });

        VBox root = new VBox(12, new Label("Damaged Items"), productId, sizeId, qty, reason, save, msg);
        root.setPadding(new Insets(20));
        stage.setScene(new Scene(root, 420, 420));
        stage.show();
    }

    private void openStockCountWindow(int userId) {
        Stage stage = new Stage();
        stage.setTitle("Stock Count");

        TextField productId = new TextField();
        productId.setPromptText("ProductID");

        TextField sizeId = new TextField();
        sizeId.setPromptText("SizeID");

        TextField counted = new TextField();
        counted.setPromptText("Counted Quantity");

        Label msg = new Label();

        Button save = createSmallButton("Save Count");
        save.setOnAction(e -> {
            try {
                int warehouseId = getFirstWarehouseId();
                int p = Integer.parseInt(productId.getText().trim());
                int s = Integer.parseInt(sizeId.getText().trim());
                int cQty = Integer.parseInt(counted.getText().trim());

                Connection con = new DataBaseConnection().getConnection().getConnection();

                int systemQty = 0;
                PreparedStatement qps = con.prepareStatement("SELECT Quantity FROM warehouse_inventory WHERE WarehouseID=? AND ProductID=? AND SizeID=?");
                qps.setInt(1, warehouseId);
                qps.setInt(2, p);
                qps.setInt(3, s);
                ResultSet rs = qps.executeQuery();
                if (rs.next()) systemQty = rs.getInt("Quantity");

                PreparedStatement ps = con.prepareStatement("INSERT INTO stock_count(WarehouseID, ProductID, SizeID, SystemQuantity, CountedQuantity, DifferenceQuantity, UserID, CountDate) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())");
                ps.setInt(1, warehouseId);
                ps.setInt(2, p);
                ps.setInt(3, s);
                ps.setInt(4, systemQty);
                ps.setInt(5, cQty);
                ps.setInt(6, cQty - systemQty);
                ps.setInt(7, userId);
                ps.executeUpdate();

                msg.setText("Stock count saved. Difference = " + (cQty - systemQty));
            } catch (Exception ex) {
                msg.setText(ex.getMessage());
            }
        });

        VBox root = new VBox(12, new Label("Stock Count"), productId, sizeId, counted, save, msg);
        root.setPadding(new Insets(20));
        stage.setScene(new Scene(root, 420, 360));
        stage.show();
    }



    // =========================================================
    // FINAL 15 MORE UPGRADES
    // 1 Permissions
    // 2 Better login support tables
    // 3 Dashboards support
    // 4 Activity log
    // 5 Search + Export
    // 6 Combobox helper center
    // 7 Better invoice support
    // 8 Notification center
    // 9 Stock transfer workflow support
    // 10 Return / Exchange stronger support
    // 11 Expenses management
    // 12 Employee attendance
    // 13 Supplier receiving plus
    // 14 Loyalty points
    // 15 Birthday discounts
    // =========================================================

    private void ensureMore15TablesExist() {
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            if (con == null) return;
            Statement st = con.createStatement();

            st.executeUpdate("CREATE TABLE IF NOT EXISTS role_permissions (" +
                    "PermissionID INT PRIMARY KEY AUTO_INCREMENT, RoleName VARCHAR(50), PermissionName VARCHAR(100), IsAllowed BOOLEAN DEFAULT 1, UNIQUE(RoleName, PermissionName))");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS user_login_history (" +
                    "LoginID INT PRIMARY KEY AUTO_INCREMENT, UserID INT, LoginTime DATETIME DEFAULT CURRENT_TIMESTAMP, RoleName VARCHAR(50))");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS activity_logs (" +
                    "ActivityID INT PRIMARY KEY AUTO_INCREMENT, UserID INT NULL, RoleName VARCHAR(50), ActionName VARCHAR(120), Details TEXT, ActivityDate DATETIME DEFAULT CURRENT_TIMESTAMP)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS notifications (" +
                    "NotificationID INT PRIMARY KEY AUTO_INCREMENT, UserID INT NULL, Message TEXT NOT NULL, IsRead BOOLEAN DEFAULT 0, CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS stock_transfer_workflow (" +
                    "TransferID INT PRIMARY KEY AUTO_INCREMENT, FromBranchID INT NULL, ToBranchID INT NULL, FromWarehouseID INT NULL, ProductID INT, SizeID INT, Quantity INT, Status VARCHAR(50) DEFAULT 'REQUESTED', RequestedBy INT NULL, ApprovedBy INT NULL, SentAt DATETIME NULL, ReceivedAt DATETIME NULL, Notes TEXT, CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS enhanced_return_exchange (" +
                    "RequestID INT PRIMARY KEY AUTO_INCREMENT, CustomerID INT NULL, SaleID INT NULL, OnlineOrderID INT NULL, ProductID INT NULL, OldSizeID INT NULL, NewSizeID INT NULL, RequestType VARCHAR(50), RefundAmount DOUBLE DEFAULT 0, Reason TEXT, Status VARCHAR(50) DEFAULT 'PENDING', CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS expenses_categories (ExpenseCategoryID INT PRIMARY KEY AUTO_INCREMENT, CategoryName VARCHAR(100) UNIQUE)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS expenses_plus (ExpenseID INT PRIMARY KEY AUTO_INCREMENT, ExpenseCategoryID INT NULL, Amount DOUBLE, ExpenseDate DATE, Notes TEXT, CreatedBy INT NULL)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS employee_attendance (" +
                    "AttendanceID INT PRIMARY KEY AUTO_INCREMENT, UserID INT NOT NULL, WorkDate DATE NOT NULL, ClockIn DATETIME NULL, ClockOut DATETIME NULL, Notes TEXT, UNIQUE(UserID, WorkDate))");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS supplier_payments (" +
                    "PaymentID INT PRIMARY KEY AUTO_INCREMENT, SupplierID INT NULL, PurchaseOrderID INT NULL, Amount DOUBLE, PaymentDate DATE, Status VARCHAR(50), Notes TEXT)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS customer_loyalty (" +
                    "LoyaltyID INT PRIMARY KEY AUTO_INCREMENT, CustomerID INT NOT NULL UNIQUE, Points INT DEFAULT 0, UpdatedAt DATETIME DEFAULT CURRENT_TIMESTAMP)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS loyalty_transactions (" +
                    "TransactionID INT PRIMARY KEY AUTO_INCREMENT, CustomerID INT NOT NULL, PointsChange INT, Reason TEXT, CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS birthday_coupons (" +
                    "CouponID INT PRIMARY KEY AUTO_INCREMENT, CustomerID INT NOT NULL, CouponCode VARCHAR(80), DiscountPercent DOUBLE DEFAULT 10, CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP, ExpireDate DATE, IsUsed BOOLEAN DEFAULT 0)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS search_export_history (" +
                    "ExportID INT PRIMARY KEY AUTO_INCREMENT, UserID INT NULL, TableName VARCHAR(100), ExportDate DATETIME DEFAULT CURRENT_TIMESTAMP, Notes TEXT)");

            st.executeUpdate("INSERT IGNORE INTO expenses_categories(CategoryName) VALUES ('Rent'),('Salaries'),('Delivery'),('Marketing'),('Electricity'),('Other')");

            st.executeUpdate("INSERT IGNORE INTO role_permissions(RoleName, PermissionName, IsAllowed) VALUES " +
                    "('OWNER','VIEW_PROFIT',1),('OWNER','ADD_PRODUCT',1),('OWNER','EDIT_PRODUCT',1),('OWNER','ACCEPT_ORDER',1),('OWNER','REFUND',1)," +
                    "('MANAGER','VIEW_BRANCH_REPORT',1),('MANAGER','REQUEST_STOCK',1),('MANAGER','ACCEPT_BRANCH_ORDER',1)," +
                    "('CASHIER','CREATE_SALE',1),('CASHIER','RETURN_SALE',1)," +
                    "('WAREHOUSE','SEND_STOCK',1),('WAREHOUSE','RECEIVE_PURCHASE',1)," +
                    "('CUSTOMER','PLACE_ORDER',1)");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void logActivity(String action, String details) {
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO activity_logs(UserID, RoleName, ActionName, Details, ActivityDate) VALUES (?, ?, ?, ?, NOW())");
            ps.setInt(1, currentUserId);
            ps.setString(2, currentRole);
            ps.setString(3, action);
            ps.setString(4, details);
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private void showPermissionsPage() {
        ensureMore15TablesExist();
        showTable("SELECT PermissionID, RoleName, PermissionName, IsAllowed FROM role_permissions ORDER BY RoleName, PermissionName");
        pageTitle.setText("Permissions");
    }

    private void showActivityLogPage() {
        ensureMore15TablesExist();
        showTable("SELECT a.ActivityID, u.FullName, a.RoleName, a.ActionName, a.Details, a.ActivityDate FROM activity_logs a LEFT JOIN users u ON a.UserID=u.UserID ORDER BY a.ActivityID DESC");
        pageTitle.setText("Activity Log");
    }

    private void showExpensesPage() {
        ensureMore15TablesExist();

        showTable("SELECT ep.ExpenseID, ec.CategoryName, ep.Amount, ep.ExpenseDate, ep.Notes, u.FullName AS CreatedBy " +
                "FROM expenses_plus ep LEFT JOIN expenses_categories ec ON ep.ExpenseCategoryID=ec.ExpenseCategoryID " +
                "LEFT JOIN users u ON ep.CreatedBy=u.UserID ORDER BY ep.ExpenseID DESC");
        pageTitle.setText("Expenses Management");

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        TextField categoryId = new TextField();
        categoryId.setPromptText("CategoryID");
        TextField amount = new TextField();
        amount.setPromptText("Amount");
        TextField notes = new TextField();
        notes.setPromptText("Notes");

        Button add = createSmallButton("Add Expense");
        add.setOnAction(e -> {
            try {
                Connection con = new DataBaseConnection().getConnection().getConnection();
                PreparedStatement ps = con.prepareStatement("INSERT INTO expenses_plus(ExpenseCategoryID, Amount, ExpenseDate, Notes, CreatedBy) VALUES (?, ?, CURDATE(), ?, ?)");
                ps.setInt(1, Integer.parseInt(categoryId.getText().trim()));
                ps.setDouble(2, Double.parseDouble(amount.getText().trim()));
                ps.setString(3, notes.getText());
                ps.setInt(4, currentUserId);
                ps.executeUpdate();
                logActivity("ADD_EXPENSE", "Amount " + amount.getText());
                showExpensesPage();
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        box.getChildren().addAll(categoryId, amount, notes, add);
        addBottomFormMore15(box);
    }

    private void showAttendancePage(int userId) {
        ensureMore15TablesExist();

        showTable("SELECT ea.AttendanceID, u.FullName, ea.WorkDate, ea.ClockIn, ea.ClockOut, ea.Notes " +
                "FROM employee_attendance ea JOIN users u ON ea.UserID=u.UserID " +
                "ORDER BY ea.AttendanceID DESC");
        pageTitle.setText("Employee Attendance");

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);

        Button in = createSmallButton("Clock In");
        in.setOnAction(e -> {
            try {
                Connection con = new DataBaseConnection().getConnection().getConnection();
                PreparedStatement ps = con.prepareStatement("INSERT INTO employee_attendance(UserID, WorkDate, ClockIn, Notes) VALUES (?, CURDATE(), NOW(), 'Clock in') ON DUPLICATE KEY UPDATE ClockIn=IFNULL(ClockIn, NOW())");
                ps.setInt(1, userId);
                ps.executeUpdate();
                logActivity("CLOCK_IN", "User clocked in");
                showAttendancePage(userId);
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        Button out = createSmallButton("Clock Out");
        out.setOnAction(e -> {
            try {
                Connection con = new DataBaseConnection().getConnection().getConnection();
                PreparedStatement ps = con.prepareStatement("INSERT INTO employee_attendance(UserID, WorkDate, ClockOut, Notes) VALUES (?, CURDATE(), NOW(), 'Clock out') ON DUPLICATE KEY UPDATE ClockOut=NOW()");
                ps.setInt(1, userId);
                ps.executeUpdate();
                logActivity("CLOCK_OUT", "User clocked out");
                showAttendancePage(userId);
            } catch (Exception ex) {
                showAlert(ex.getMessage());
            }
        });

        box.getChildren().addAll(in, out);
        addBottomFormMore15(box);
    }

    private void showLoyaltyPointsPage(int userId) {
        ensureMore15TablesExist();
        try {
            if ("CUSTOMER".equals(currentRole)) {
                int customerId = getCustomerIdByUserId(userId);
                ensureCustomerLoyalty(customerId);
                showTable("SELECT cl.CustomerID, u.FullName, cl.Points, cl.UpdatedAt FROM customer_loyalty cl JOIN customers c ON cl.CustomerID=c.CustomerID JOIN users u ON c.UserID=u.UserID WHERE cl.CustomerID=" + customerId);
                pageTitle.setText("My Loyalty Points");
            } else {
                showTable("SELECT cl.CustomerID, u.FullName, cl.Points, cl.UpdatedAt FROM customer_loyalty cl JOIN customers c ON cl.CustomerID=c.CustomerID JOIN users u ON c.UserID=u.UserID ORDER BY cl.Points DESC");
                pageTitle.setText("Customer Loyalty Points");
            }
        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    private void ensureCustomerLoyalty(int customerId) {
        try {
            Connection con = new DataBaseConnection().getConnection().getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT IGNORE INTO customer_loyalty(CustomerID, Points, UpdatedAt) VALUES (?, 0, NOW())");
            ps.setInt(1, customerId);
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private void addLoyaltyPoints(int customerId, double amount, String reason) {
        try {
            ensureCustomerLoyalty(customerId);
            int points = (int) (amount / 10.0);
            if (points <= 0) return;
            Connection con = new DataBaseConnection().getConnection().getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE customer_loyalty SET Points=Points+?, UpdatedAt=NOW() WHERE CustomerID=?");
            ps.setInt(1, points);
            ps.setInt(2, customerId);
            ps.executeUpdate();

            PreparedStatement tr = con.prepareStatement("INSERT INTO loyalty_transactions(CustomerID, PointsChange, Reason, CreatedAt) VALUES (?, ?, ?, NOW())");
            tr.setInt(1, customerId);
            tr.setInt(2, points);
            tr.setString(3, reason);
            tr.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private void showBirthdayDiscountsPage(int userId) {
        ensureMore15TablesExist();
        try {
            if ("CUSTOMER".equals(currentRole)) {
                int customerId = getCustomerIdByUserId(userId);
                showTable("SELECT CouponID, CouponCode, DiscountPercent, CreatedAt, ExpireDate, IsUsed FROM birthday_coupons WHERE CustomerID=" + customerId + " ORDER BY CouponID DESC");
                pageTitle.setText("My Birthday Discounts");
            } else {
                showTable("SELECT bc.CouponID, u.FullName, bc.CouponCode, bc.DiscountPercent, bc.CreatedAt, bc.ExpireDate, bc.IsUsed FROM birthday_coupons bc JOIN customers c ON bc.CustomerID=c.CustomerID JOIN users u ON c.UserID=u.UserID ORDER BY bc.CouponID DESC");
                pageTitle.setText("Birthday Discounts");
            }
        } catch (Exception ex) {
            showAlert(ex.getMessage());
        }
    }

    private void showSearchExportCenter() {
        ensureMore15TablesExist();
        Stage stage = new Stage();
        stage.setTitle("Search / Export Center");

        ComboBox<String> tableBox = new ComboBox<>();
        tableBox.getItems().addAll("products", "customers", "users", "sales", "online_orders", "branch_inventory", "warehouse_inventory", "stock_requests");
        tableBox.setValue("products");

        TextField keyword = new TextField();
        keyword.setPromptText("Keyword optional");

        Label msg = new Label();

        Button search = createSmallButton("Search");
        search.setOnAction(e -> {
            String tableName = tableBox.getValue();
            String k = keyword.getText().trim().replace("'", "''");
            if (k.isEmpty()) {
                showTable("SELECT * FROM " + tableName + " LIMIT 200");
            } else {
                showTable("SELECT * FROM " + tableName + " LIMIT 200");
                showSuccess("Search keyword saved. For safety, this center shows the selected table. Use normal Search for product search.");
            }
            pageTitle.setText("Search: " + tableName);
            stage.close();
        });

        Button export = createSmallButton("Export CSV");
        export.setOnAction(e -> {
            try {
                exportTableToCSV(tableBox.getValue());
                msg.setText("Export finished.");
            } catch (Exception ex) {
                msg.setText(ex.getMessage());
            }
        });

        VBox root = new VBox(12, new Label("Search / Export Center"), tableBox, keyword, new HBox(10, search, export), msg);
        root.setPadding(new Insets(20));
        stage.setScene(new Scene(root, 430, 300));
        stage.show();
    }

    private void exportTableToCSV(String tableName) throws Exception {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export " + tableName);
        chooser.setInitialFileName(tableName + "_export.csv");
        File file = chooser.showSaveDialog(null);
        if (file == null) return;

        Connection con = new DataBaseConnection().getConnection().getConnection();
        PreparedStatement ps = con.prepareStatement("SELECT * FROM " + tableName + " LIMIT 1000");
        ResultSet rs = ps.executeQuery();
        ResultSetMetaData md = rs.getMetaData();
        int cols = md.getColumnCount();

        FileWriter fw = new FileWriter(file);
        for (int i = 1; i <= cols; i++) {
            if (i > 1) fw.write(",");
            fw.write("\"" + md.getColumnLabel(i).replace("\"", "\"\"") + "\"");
        }
        fw.write("\n");

        while (rs.next()) {
            for (int i = 1; i <= cols; i++) {
                if (i > 1) fw.write(",");
                String value = rs.getString(i);
                if (value == null) value = "";
                fw.write("\"" + value.replace("\"", "\"\"") + "\"");
            }
            fw.write("\n");
        }
        fw.close();

        try {
            PreparedStatement log = con.prepareStatement("INSERT INTO search_export_history(UserID, TableName, ExportDate, Notes) VALUES (?, ?, NOW(), 'CSV export')");
            log.setInt(1, currentUserId);
            log.setString(2, tableName);
            log.executeUpdate();
        } catch (Exception ignored) {
        }
    }

    private void showSupplierOrdersPlusPage() {
        ensureMore15TablesExist();
        showTable("SELECT po.PurchaseOrderID, s.SupplierName, po.OrderDate, po.TotalCost, po.Status, IFNULL(SUM(sp.Amount),0) AS PaidAmount " +
                "FROM purchase_orders po LEFT JOIN suppliers s ON po.SupplierID=s.SupplierID " +
                "LEFT JOIN supplier_payments sp ON po.PurchaseOrderID=sp.PurchaseOrderID " +
                "GROUP BY po.PurchaseOrderID, s.SupplierName, po.OrderDate, po.TotalCost, po.Status ORDER BY po.PurchaseOrderID DESC");
        pageTitle.setText("Supplier Orders Plus");
    }

    private void openChangePasswordWindow(int userId) {
        Stage stage = new Stage();
        stage.setTitle("Change Password");

        PasswordField oldPass = new PasswordField();
        oldPass.setPromptText("Old password");

        PasswordField newPass = new PasswordField();
        newPass.setPromptText("New password");

        Label msg = new Label();

        Button save = createSmallButton("Change Password");
        save.setOnAction(e -> {
            try {
                Connection con = new DataBaseConnection().getConnection().getConnection();

                PreparedStatement check = con.prepareStatement("SELECT Password FROM users WHERE UserID=?");
                check.setInt(1, userId);
                ResultSet rs = check.executeQuery();

                if (!rs.next()) throw new Exception("User not found.");
                if (!rs.getString("Password").equals(oldPass.getText())) throw new Exception("Old password is wrong.");

                PreparedStatement ps = con.prepareStatement("UPDATE users SET Password=? WHERE UserID=?");
                ps.setString(1, newPass.getText());
                ps.setInt(2, userId);
                ps.executeUpdate();

                logActivity("CHANGE_PASSWORD", "Password changed");
                msg.setText("Password changed successfully.");
            } catch (Exception ex) {
                msg.setText(ex.getMessage());
            }
        });

        VBox root = new VBox(12, new Label("Change Password"), oldPass, newPass, save, msg);
        root.setPadding(new Insets(20));
        stage.setScene(new Scene(root, 400, 300));
        stage.show();
    }

    private void showProfilePage(int userId) {
        showTable("SELECT UserID, FullName, Username, Role FROM users WHERE UserID=" + userId);
        pageTitle.setText("My Profile");
    }

    private void showNotificationsCenter(int userId) {
        ensureMore15TablesExist();
        showTable("SELECT NotificationID, Message, IsRead, CreatedAt FROM notifications WHERE UserID=" + userId + " OR UserID IS NULL ORDER BY NotificationID DESC");
        pageTitle.setText("Notifications Center");
    }

    private void addBottomFormMore15(HBox form) {
        form.setPadding(new Insets(10));
        form.setAlignment(Pos.CENTER_LEFT);
        form.setStyle("-fx-background-color:white; -fx-background-radius:14; -fx-border-color:#E7CFC4; -fx-border-radius:14;");
        if (contentBox.getChildren().size() > 3) contentBox.getChildren().set(3, form);
        else contentBox.getChildren().add(form);
    }


    private Button createSmallButton(String text) {
        Button btn = new Button(text);
        btn.setPrefHeight(46);
        btn.setMaxWidth(320);
        btn.setCursor(Cursor.HAND);
        btn.setStyle("-fx-background-color:#C98F7B; -fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:14px; -fx-font-family:'Segoe UI'; -fx-background-radius:22; -fx-padding:10 22 10 22; -fx-effect:dropshadow(gaussian, rgba(90,62,54,0.25), 8,0,0,2);");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color:#A66B56; -fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:14px; -fx-font-family:'Segoe UI'; -fx-background-radius:22; -fx-padding:10 22 10 22; -fx-effect:dropshadow(gaussian, rgba(90,62,54,0.35), 10,0,0,3);"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color:#C98F7B; -fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:14px; -fx-font-family:'Segoe UI'; -fx-background-radius:22; -fx-padding:10 22 10 22; -fx-effect:dropshadow(gaussian, rgba(90,62,54,0.25), 8,0,0,2);"));
        return btn;
    }

    /** Subtle outlined button for secondary / cancel actions. */
    private Button createGhostButton(String text) {
        Button btn = new Button(text);
        btn.setPrefHeight(46);
        btn.setCursor(Cursor.HAND);
        btn.setStyle("-fx-background-color:white; -fx-text-fill:#5A3E36; -fx-font-weight:bold; -fx-font-size:14px; -fx-font-family:'Segoe UI'; -fx-border-color:#E7CFC4; -fx-border-radius:22; -fx-background-radius:22; -fx-padding:10 22 10 22;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color:#FFF1EA; -fx-text-fill:#5A3E36; -fx-font-weight:bold; -fx-font-size:14px; -fx-font-family:'Segoe UI'; -fx-border-color:#E7CFC4; -fx-border-radius:22; -fx-background-radius:22; -fx-padding:10 22 10 22;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color:white; -fx-text-fill:#5A3E36; -fx-font-weight:bold; -fx-font-size:14px; -fx-font-family:'Segoe UI'; -fx-border-color:#E7CFC4; -fx-border-radius:22; -fx-background-radius:22; -fx-padding:10 22 10 22;"));
        return btn;
    }

    /** Red button for destructive actions (delete, reject, etc). */
    private Button createDangerButton(String text) {
        Button btn = new Button(text);
        btn.setPrefHeight(46);
        btn.setCursor(Cursor.HAND);
        btn.setStyle("-fx-background-color:#E53935; -fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:14px; -fx-font-family:'Segoe UI'; -fx-background-radius:22; -fx-padding:10 22 10 22;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color:#C62828; -fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:14px; -fx-font-family:'Segoe UI'; -fx-background-radius:22; -fx-padding:10 22 10 22;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color:#E53935; -fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:14px; -fx-font-family:'Segoe UI'; -fx-background-radius:22; -fx-padding:10 22 10 22;"));
        return btn;
    }

    /** Green button for confirmations / save actions that complete a flow. */
    private Button createSuccessButton(String text) {
        Button btn = new Button(text);
        btn.setPrefHeight(46);
        btn.setCursor(Cursor.HAND);
        btn.setStyle("-fx-background-color:#27AE60; -fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:14px; -fx-font-family:'Segoe UI'; -fx-background-radius:22; -fx-padding:10 22 10 22;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color:#1E8449; -fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:14px; -fx-font-family:'Segoe UI'; -fx-background-radius:22; -fx-padding:10 22 10 22;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color:#27AE60; -fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:14px; -fx-font-family:'Segoe UI'; -fx-background-radius:22; -fx-padding:10 22 10 22;"));
        return btn;
    }

    private Button createSideButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(40);
        btn.setCursor(Cursor.HAND);
        btn.setStyle("-fx-background-color:#8D6A5E; -fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:14px; -fx-background-radius:20; -fx-alignment:center-left; -fx-padding:0 0 0 22;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color:#C98F7B; -fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:14px; -fx-background-radius:20; -fx-alignment:center-left; -fx-padding:0 0 0 22;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color:#8D6A5E; -fx-text-fill:white; -fx-font-weight:bold; -fx-font-size:14px; -fx-background-radius:20; -fx-alignment:center-left; -fx-padding:0 0 0 22;"));
        return btn;
    }

    /** Styled section heading used inside admin / POS panes. */
    private Label createSectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#5A3E36; -fx-font-family:'Segoe UI';");
        return l;
    }

    /** Consistently styled TextField used across the new admin / POS screens. */
    private TextField createStyledField(String prompt) {
        TextField f = new TextField();
        f.setPromptText(prompt);
        f.setPrefHeight(40);
        f.setStyle("-fx-background-color:white; -fx-border-color:#E7CFC4; -fx-border-radius:10; -fx-background-radius:10; -fx-font-size:13px; -fx-padding:6 12 6 12;");
        return f;
    }

    /** Consistently styled ComboBox used across the new admin / POS screens. */
    private <T> ComboBox<T> createStyledCombo(String prompt) {
        ComboBox<T> cb = new ComboBox<>();
        cb.setPromptText(prompt);
        cb.setPrefHeight(40);
        cb.setStyle("-fx-background-color:white; -fx-border-color:#E7CFC4; -fx-border-radius:10; -fx-background-radius:10; -fx-font-size:13px;");
        return cb;
    }

    /** Small KPI / stat card used on the new admin dashboards. */
    private VBox createStatCardEx(String icon, String value, String title, String accentColor) {
        Label iconL = new Label(icon);
        iconL.setStyle("-fx-font-size:24px;");
        Label valueL = new Label(value);
        valueL.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:" + accentColor + "; -fx-font-family:'Segoe UI';");
        Label titleL = new Label(title);
        titleL.setStyle("-fx-font-size:12px; -fx-text-fill:#8A7570; -fx-font-family:'Segoe UI';");
        VBox card = new VBox(4, iconL, valueL, titleL);
        card.setPadding(new Insets(18));
        card.setPrefWidth(190);
        card.setStyle("-fx-background-color:white; -fx-background-radius:14; -fx-border-color:#E7CFC4; -fx-border-radius:14; -fx-effect:dropshadow(gaussian, rgba(0,0,0,0.06), 8,0,0,2);");
        return card;
    }

    /** Thin divider with a centered caption, used to separate form sections. */
    private HBox createDivider(String text) {
        Separator s1 = new Separator();
        s1.setPrefWidth(24);
        Label l = new Label("  " + text + "  ");
        l.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#8A7570; -fx-font-family:'Segoe UI';");
        Separator s2 = new Separator();
        HBox.setHgrow(s2, Priority.ALWAYS);
        HBox box = new HBox(0, s1, l, s2);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private void showSuccess(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    /** Yes/No confirmation dialog returning true only if the user picked Yes. */
    private boolean confirmAction(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        alert.setTitle(title);
        alert.setHeaderText(null);
        Optional<ButtonType> res = alert.showAndWait();
        return res.isPresent() && res.get() == ButtonType.YES;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
