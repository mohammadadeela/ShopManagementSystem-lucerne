package com.lucerne.ui;

import com.lucerne.app.AppSession;
import com.lucerne.dao.QueryDAO;
import com.lucerne.model.Role;
import com.lucerne.ui.components.LoadingPane;
import com.lucerne.util.AlertUtil;
import com.lucerne.util.ExportUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

public final class ReportsView extends StackPane {
    private final QueryDAO dao=new QueryDAO();private final ComboBox<ReportDef> report=new ComboBox<>();private final DatePicker from=new DatePicker(LocalDate.now().withDayOfMonth(1)),to=new DatePicker(LocalDate.now());private final ComboBox<QueryDAO.Option> branch=new ComboBox<>();private final TableView<Map<String,Object>> table=new TableView<>();private final Label count=new Label();private final LoadingPane loading=new LoadingPane();private List<Map<String,Object>> rows=List.of();private List<String> columns=List.of();
    public ReportsView(){report.getItems().addAll(definitions());if(!report.getItems().isEmpty())report.getSelectionModel().selectFirst();getChildren().addAll(build(),loading);loadScopes();if(report.getValue()!=null)run();}
    private VBox build(){VBox root=new VBox(18);root.setPadding(new Insets(24));Label title=new Label("Reports & Analytics");title.getStyleClass().add("page-title");Label desc=new Label("Twenty-two filtered operational and financial reports. Exports include only the visible result set.");desc.getStyleClass().add("page-description");Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);Button run=new Button("Run report");run.getStyleClass().add("primary-button");run.setOnAction(e->run());Button export=new Button("Export CSV");export.setOnAction(e->export());HBox head=new HBox(10,new VBox(4,title,desc),spacer,run,export);head.setAlignment(Pos.CENTER_LEFT);FlowPane filters=new FlowPane(10,10,new Label("Report"),report,new Label("From"),from,new Label("To"),to,new Label(isWarehouseRole()?"Warehouse":"Branch"),branch);filters.getStyleClass().add("filter-panel");filters.setPadding(new Insets(14));report.setPrefWidth(280);branch.setPrefWidth(180);table.setPlaceholder(new Label("Run a report to see results."));VBox card=new VBox(10,count,table);card.getStyleClass().add("content-card");card.setPadding(new Insets(14));VBox.setVgrow(table,Priority.ALWAYS);root.getChildren().addAll(head,filters,card);VBox.setVgrow(card,Priority.ALWAYS);return root;}
    private void loadScopes(){
        Integer assigned=isWarehouseRole()?AppSession.current().warehouseId():AppSession.current().branchId();
        boolean fixed=assigned!=null&&!AppSession.hasPermission("VIEW_ALL_BRANCHES");
        String sql=isWarehouseRole()?"SELECT WarehouseID,Name FROM warehouses WHERE IsActive=1":"SELECT BranchID,Name FROM branches WHERE IsActive=1";
        List<Object> parameters=fixed?List.of(assigned):List.of();
        if(fixed)sql+=" AND "+(isWarehouseRole()?"WarehouseID":"BranchID")+"=?";
        sql+=" ORDER BY Name";
        String finalSql=sql;
        Task<List<QueryDAO.Option>> task=new Task<>(){protected List<QueryDAO.Option> call()throws Exception{return dao.options(finalSql,parameters);}};
        task.setOnSucceeded(event->{
            branch.getItems().clear();
            if(!fixed)branch.getItems().add(new QueryDAO.Option(null,isWarehouseRole()?"All warehouses":"All branches"));
            branch.getItems().addAll(task.getValue());
            if(!branch.getItems().isEmpty())branch.getSelectionModel().selectFirst();
            branch.setDisable(fixed);
        });
        Thread thread=new Thread(task,"report-scopes");thread.setDaemon(true);thread.start();
    }
    private Integer selectedScopeId(){
        Integer assigned=isWarehouseRole()?AppSession.current().warehouseId():AppSession.current().branchId();
        if(assigned!=null&&!AppSession.hasPermission("VIEW_ALL_BRANCHES"))return assigned;
        return branch.getValue()==null||branch.getValue().id()==null?null:((Number)branch.getValue().id()).intValue();
    }
    private boolean isWarehouseRole(){return AppSession.current().role()==Role.WAREHOUSE;}
    private void run(){
        if(report.getValue()==null)return;
        if(from.getValue()==null||to.getValue()==null||from.getValue().isAfter(to.getValue())){AlertUtil.warning("Invalid date range","Choose a valid start and end date.");return;}
        ReportDef def=report.getValue();Integer scopeId=selectedScopeId();List<Object> params=new ArrayList<>();
        for(Param p:def.params()){switch(p){case FROM->params.add(from.getValue());case TO->params.add(to.getValue());case BRANCH,BRANCH_AGAIN->params.add(scopeId);}}
        loading.show("Generating "+def.name().toLowerCase()+"…");Task<List<Map<String,Object>>> task=new Task<>(){protected List<Map<String,Object>> call()throws Exception{return dao.queryAll(def.sql(),params);}};
        task.setOnSucceeded(event->{rows=task.getValue();render();loading.hide();});task.setOnFailed(event->{loading.hide();AlertUtil.error("Report failed","The report query could not be completed.");});Thread thread=new Thread(task,"report-query");thread.setDaemon(true);thread.start();
    }
    private void render(){table.getColumns().clear();if(rows.isEmpty()){columns=List.of();table.getItems().clear();count.setText("0 rows");return;}columns=new ArrayList<>(rows.getFirst().keySet());for(String col:columns){TableColumn<Map<String,Object>,Object> c=new TableColumn<>(col);c.setCellValueFactory(v->new ReadOnlyObjectWrapper<>(v.getValue().get(col)));c.setPrefWidth(Math.max(110,Math.min(220,col.length()*12.0)));table.getColumns().add(c);}table.getItems().setAll(rows);count.setText(rows.size()+" report rows · "+report.getValue().description());}
    private void export(){if(rows.isEmpty()){AlertUtil.warning("Nothing to export","Run a report that returns data first.");return;}Path path=ExportUtil.chooseCsv(getScene().getWindow(),report.getValue().name().toLowerCase().replace(' ','-')+".csv");if(path==null)return;try{ExportUtil.writeCsv(path,columns,rows);AlertUtil.info("Export complete",path.toString());}catch(Exception e){AlertUtil.error("Export failed","Could not write the CSV file.");}}
    private List<ReportDef> definitions(){String scope=" AND (? IS NULL OR s.BranchID=?)";String orderScope=" AND (? IS NULL OR o.BranchID=?)";String expenseScope=" AND (? IS NULL OR e.BranchID=?)";String inventoryScope=isWarehouseRole()?"(? IS NULL OR WarehouseID=?)":"(? IS NULL OR BranchID=?)";String movementScope=isWarehouseRole()?"(? IS NULL OR (sm.LocationType='WAREHOUSE' AND sm.LocationID=?))":"(? IS NULL OR (sm.LocationType='BRANCH' AND sm.LocationID=?))";String requestScope=isWarehouseRole()?"(? IS NULL OR sr.WarehouseID=?)":"(? IS NULL OR sr.BranchID=?)";List<ReportDef> all=List.of(
            r("Sales report","Completed sales and financial components","SELECT s.ReceiptNumber Receipt,s.SaleDate Date,b.Name Branch,u.FullName Cashier,s.GrossAmount `Gross Sales`,s.DiscountAmount Discounts,s.NetAmount Revenue,s.CostAmount COGS,s.GrossProfit `Gross Profit`,s.Status FROM sales s JOIN branches b ON b.BranchID=s.BranchID JOIN users u ON u.UserID=s.CashierUserID WHERE DATE(s.SaleDate) BETWEEN ? AND ?"+scope+" ORDER BY s.SaleDate DESC"),
            r("Transaction report","Receipt and payment analysis","SELECT s.ReceiptNumber Receipt,s.SaleDate Date,s.PaymentMethod Payment,p.PaidAmount Paid,p.ChangeAmount `Change`,s.NetAmount Total,s.Status FROM sales s LEFT JOIN payments p ON p.SaleID=s.SaleID WHERE DATE(s.SaleDate) BETWEEN ? AND ?"+scope+" ORDER BY s.SaleDate DESC"),
            r("Revenue report","Daily gross sales, discounts and net revenue","SELECT DATE(s.SaleDate) Date,SUM(s.GrossAmount) `Gross Sales`,SUM(s.DiscountAmount) Discounts,SUM(s.NetAmount) `Net Revenue` FROM sales s WHERE DATE(s.SaleDate) BETWEEN ? AND ? AND s.Status<>'CANCELLED'"+scope+" GROUP BY DATE(s.SaleDate) ORDER BY Date"),
            r("Gross profit report","Revenue less cost of goods sold","SELECT DATE(s.SaleDate) Date,SUM(s.NetAmount) Revenue,SUM(s.CostAmount) COGS,SUM(s.GrossProfit) `Gross Profit` FROM sales s WHERE DATE(s.SaleDate) BETWEEN ? AND ? AND s.Status<>'CANCELLED'"+scope+" GROUP BY DATE(s.SaleDate) ORDER BY Date"),
            r("Net profit report","Gross profit less approved expenses","SELECT m.Month,m.Revenue,m.COGS,m.GrossProfit,COALESCE(e.Expenses,0) Expenses,(m.GrossProfit-COALESCE(e.Expenses,0)) `Net Profit` FROM v_monthly_gross_profit m LEFT JOIN v_monthly_expenses e ON e.Month=m.Month AND e.BranchID=m.BranchID WHERE STR_TO_DATE(CONCAT(m.Month,'-01'),'%Y-%m-%d') BETWEEN ? AND ? AND (? IS NULL OR m.BranchID=?) ORDER BY m.Month"),
            expense("Expense report","Approved, pending and rejected expenses","SELECT e.ExpenseDate Date,e.Category,e.Description,b.Name Branch,e.Amount,e.PaymentMethod Payment,e.Status FROM expenses e LEFT JOIN branches b ON b.BranchID=e.BranchID WHERE e.ExpenseDate BETWEEN ? AND ?"+expenseScope+" ORDER BY e.ExpenseDate DESC"),
            r("Product performance report","Units, revenue and gross profit per product","SELECT p.SKU,p.Name Product,c.CategoryName Category,SUM(si.Quantity) `Units Sold`,SUM(si.LineTotal) Revenue,SUM(si.LineTotal-si.CostAtSale*si.Quantity) `Gross Profit` FROM sale_items si JOIN sales s ON s.SaleID=si.SaleID JOIN product_variants pv ON pv.VariantID=si.VariantID JOIN products p ON p.ProductID=pv.ProductID JOIN categories c ON c.CategoryID=p.CategoryID WHERE DATE(s.SaleDate) BETWEEN ? AND ? AND s.Status<>'CANCELLED'"+scope+" GROUP BY p.ProductID,p.SKU,p.Name,c.CategoryName ORDER BY Revenue DESC"),
            r("Best-seller report","Ranked products by sold quantity","SELECT ROW_NUMBER() OVER(ORDER BY SUM(si.Quantity) DESC) `Rank`,p.Name Product,c.CategoryName Category,SUM(si.Quantity) `Units Sold`,SUM(si.LineTotal) Revenue,SUM(si.LineTotal-si.CostAtSale*si.Quantity) `Gross Profit` FROM sale_items si JOIN sales s ON s.SaleID=si.SaleID JOIN product_variants pv ON pv.VariantID=si.VariantID JOIN products p ON p.ProductID=pv.ProductID JOIN categories c ON c.CategoryID=p.CategoryID WHERE DATE(s.SaleDate) BETWEEN ? AND ? AND s.Status<>'CANCELLED'"+scope+" GROUP BY p.ProductID,p.Name,c.CategoryName ORDER BY `Units Sold` DESC"),
            inventory("Inventory report","Current quantities and valuations","SELECT LocationType,LocationName,SKU,ProductName Product,CategoryName Category,SizeValue Size,ColorName Color,Quantity,StockStatus Status,CostValue `Cost Value`,RetailValue `Retail Value` FROM v_current_inventory WHERE "+inventoryScope+" ORDER BY LocationName,ProductName"),
            inventory("Low-stock report","Variants at or below reorder level","SELECT LocationType,LocationName,SKU,ProductName Product,SizeValue Size,ColorName Color,Quantity,ReorderLevel,StockStatus Status FROM v_current_inventory WHERE StockStatus IN ('LOW_STOCK','OUT_OF_STOCK') AND "+inventoryScope+" ORDER BY Quantity,ProductName"),
            r("Stock movement report","Inbound and outbound inventory movements","SELECT sm.MovementDate Date,sm.LocationType,sm.LocationID,p.Name Product,sz.SizeValue Size,co.ColorName Color,sm.MovementType Type,sm.Direction,sm.Quantity,sm.ReferenceNumber Reference FROM stock_movements sm JOIN product_variants pv ON pv.VariantID=sm.VariantID JOIN products p ON p.ProductID=pv.ProductID JOIN sizes sz ON sz.SizeID=pv.SizeID JOIN colors co ON co.ColorID=pv.ColorID WHERE DATE(sm.MovementDate) BETWEEN ? AND ? AND "+movementScope+" ORDER BY sm.MovementDate DESC"),
            r("Branch performance report","Revenue, transactions and profit by branch","SELECT b.Name Branch,COUNT(s.SaleID) Transactions,SUM(s.NetAmount) Revenue,SUM(s.GrossProfit) `Gross Profit`,AVG(s.NetAmount) `Average Transaction` FROM sales s JOIN branches b ON b.BranchID=s.BranchID WHERE DATE(s.SaleDate) BETWEEN ? AND ? AND s.Status<>'CANCELLED'"+scope+" GROUP BY b.BranchID,b.Name ORDER BY Revenue DESC"),
            r("Cashier performance report","Cashier transactions, revenue and discounts","SELECT u.FullName Cashier,b.Name Branch,COUNT(s.SaleID) Transactions,SUM(s.NetAmount) Revenue,SUM(s.DiscountAmount) Discounts,AVG(s.NetAmount) Average FROM sales s JOIN users u ON u.UserID=s.CashierUserID JOIN branches b ON b.BranchID=s.BranchID WHERE DATE(s.SaleDate) BETWEEN ? AND ? AND s.Status<>'CANCELLED'"+scope+" GROUP BY u.UserID,u.FullName,b.Name ORDER BY Revenue DESC"),
            r("Employee performance report","Employee-linked sales performance","SELECT e.FullName Employee,e.JobTitle,b.Name Branch,COUNT(s.SaleID) Transactions,COALESCE(SUM(s.NetAmount),0) Revenue FROM employees e LEFT JOIN users u ON u.UserID=e.UserID LEFT JOIN sales s ON s.CashierUserID=u.UserID AND DATE(s.SaleDate) BETWEEN ? AND ? LEFT JOIN branches b ON b.BranchID=e.BranchID WHERE (? IS NULL OR e.BranchID=?) GROUP BY e.EmployeeID,e.FullName,e.JobTitle,b.Name ORDER BY Revenue DESC"),
            r("Customer purchases report","Customer receipt-level purchase history","SELECT c.FullName Customer,c.Phone,s.ReceiptNumber Receipt,s.SaleDate Date,s.NetAmount Total,s.Status FROM sales s JOIN customers c ON c.CustomerID=s.CustomerID WHERE DATE(s.SaleDate) BETWEEN ? AND ?"+scope+" ORDER BY c.FullName,s.SaleDate DESC"),
            r("Customer spending report","Lifetime value within the selected period","SELECT c.FullName Customer,c.Phone,COUNT(s.SaleID) Purchases,SUM(s.NetAmount) Spending,AVG(s.NetAmount) Average,MAX(s.SaleDate) `Last Purchase` FROM sales s JOIN customers c ON c.CustomerID=s.CustomerID WHERE DATE(s.SaleDate) BETWEEN ? AND ? AND s.Status<>'CANCELLED'"+scope+" GROUP BY c.CustomerID,c.FullName,c.Phone ORDER BY Spending DESC"),
            order("Order report","Order status and fulfillment overview","SELECT o.OrderNumber `Order Number`,o.OrderDate Date,c.FullName Customer,b.Name Branch,o.DeliveryType Delivery,o.PaymentStatus `Payment Status`,o.Status,o.TotalAmount Total FROM online_orders o JOIN customers c ON c.CustomerID=o.CustomerID LEFT JOIN branches b ON b.BranchID=o.BranchID WHERE DATE(o.OrderDate) BETWEEN ? AND ?"+orderScope+" ORDER BY o.OrderDate DESC"),
            r("Return report","Return and exchange requests","SELECT rr.RequestNumber Reference,rr.RequestDate Date,s.ReceiptNumber Receipt,c.FullName Customer,rr.RequestType Type,rr.Reason,rr.RefundAmount Amount,rr.Status FROM return_requests rr JOIN sales s ON s.SaleID=rr.SaleID LEFT JOIN customers c ON c.CustomerID=rr.CustomerID WHERE DATE(rr.RequestDate) BETWEEN ? AND ? AND (? IS NULL OR rr.BranchID=?) ORDER BY rr.RequestDate DESC"),
            r("Stock request report","Branch stock requests and approval status","SELECT sr.RequestNumber Reference,sr.RequestDate Date,b.Name Branch,w.Name Warehouse,sr.Priority,sr.Status,SUM(sri.RequestedQuantity) Requested,SUM(sri.ApprovedQuantity) Approved FROM stock_requests sr JOIN branches b ON b.BranchID=sr.BranchID JOIN warehouses w ON w.WarehouseID=sr.WarehouseID LEFT JOIN stock_request_items sri ON sri.RequestID=sr.RequestID WHERE DATE(sr.RequestDate) BETWEEN ? AND ? AND "+requestScope+" GROUP BY sr.RequestID,sr.RequestNumber,sr.RequestDate,b.Name,w.Name,sr.Priority,sr.Status ORDER BY sr.RequestDate DESC"),
            purchase("Purchase order report","Supplier orders and receiving status","SELECT po.PONumber `PO Number`,po.OrderDate Date,s.SupplierName Supplier,w.Name Warehouse,po.ExpectedDate Expected,po.TotalCost Total,po.Status FROM purchase_orders po JOIN suppliers s ON s.SupplierID=po.SupplierID JOIN warehouses w ON w.WarehouseID=po.WarehouseID WHERE po.OrderDate BETWEEN ? AND ? AND (? IS NULL OR po.WarehouseID=?) ORDER BY po.OrderDate DESC"),
            r("Audit report","Security and business action history","SELECT a.ActionAt Date,a.Username,a.ActionCode Action,a.EntityType Entity,a.EntityID `Entity ID`,a.Description,CASE WHEN a.Success=1 THEN 'SUCCESS' ELSE 'FAILURE' END Status FROM audit_logs a WHERE DATE(a.ActionAt) BETWEEN ? AND ? AND (? IS NULL OR a.BranchID=?) ORDER BY a.ActionAt DESC"),
            r("Daily closing report","Cash reconciliation and variance","SELECT dc.ClosingDate Date,b.Name Branch,u.FullName Cashier,dc.ExpectedCash Expected,dc.ActualCash Actual,dc.DifferenceAmount Difference,dc.CashSales `Cash Sales`,dc.CardSales `Card Sales`,dc.Refunds,dc.Status FROM daily_closings dc JOIN branches b ON b.BranchID=dc.BranchID JOIN users u ON u.UserID=dc.CashierUserID WHERE dc.ClosingDate BETWEEN ? AND ? AND (? IS NULL OR dc.BranchID=?) ORDER BY dc.ClosingDate DESC")
        );
        if(isWarehouseRole()){
            Set<String> allowed=Set.of("Inventory report","Low-stock report","Stock movement report","Stock request report","Purchase order report");
            all=all.stream().filter(def->allowed.contains(def.name())).toList();
        }
        if(!AppSession.hasPermission("VIEW_NET_PROFIT"))all=all.stream().filter(def->!def.name().equals("Net profit report")).toList();
        if(!AppSession.hasPermission("VIEW_PRODUCT_COST"))all=all.stream().filter(def->!Set.of("Gross profit report","Product performance report","Best-seller report").contains(def.name())).toList();
        if(!AppSession.hasPermission("VIEW_AUDIT_LOG"))all=all.stream().filter(def->!def.name().equals("Audit report")).toList();
        return all;
    }
    private ReportDef r(String n,String d,String s){return new ReportDef(n,d,s,List.of(Param.FROM,Param.TO,Param.BRANCH,Param.BRANCH_AGAIN));}
    private ReportDef expense(String n,String d,String s){return r(n,d,s);}private ReportDef order(String n,String d,String s){return r(n,d,s);}private ReportDef purchase(String n,String d,String s){return r(n,d,s);}private ReportDef inventory(String n,String d,String s){return new ReportDef(n,d,s,List.of(Param.BRANCH,Param.BRANCH_AGAIN));}
    private enum Param{FROM,TO,BRANCH,BRANCH_AGAIN}
    private record ReportDef(String name,String description,String sql,List<Param> params){public String toString(){return name;}}
}
