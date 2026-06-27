package com.lucerne.ui;

import com.lucerne.app.AppSession;
import com.lucerne.config.ApplicationConfig;
import com.lucerne.dao.QueryDAO;
import com.lucerne.model.Role;
import com.lucerne.ui.components.LoadingPane;
import com.lucerne.ui.components.MetricCard;
import com.lucerne.util.AlertUtil;
import com.lucerne.util.ExportUtil;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

public final class DataPage extends BorderPane {
    private final ModuleDefinition module;
    private final QueryDAO dao = new QueryDAO();
    private final TableView<Map<String,Object>> table = new TableView<>();
    private final TextField search = new TextField();
    private final DatePicker fromDate = new DatePicker();
    private final DatePicker toDate = new DatePicker();
    private final ComboBox<String> status = new ComboBox<>();
    private final ComboBox<QueryDAO.Option> branch = new ComboBox<>();
    private final TextField minAmount = new TextField();
    private final TextField maxAmount = new TextField();
    private final ComboBox<Integer> pageSize = new ComboBox<>();
    private final Label matchCount = new Label("0 matching records");
    private final Label pageLabel = new Label("Page 1");
    private final Button previous = new Button("Previous");
    private final Button next = new Button("Next");
    private final LoadingPane loading = new LoadingPane();
    private final MetricCard totalRecords = new MetricCard("Matching records");
    private final MetricCard filteredValue = new MetricCard("Visible value");
    private final MetricCard currentPeriod = new MetricCard("Date range");
    private List<Map<String,Object>> currentRows = List.of();
    private List<String> currentColumns = List.of();
    private int currentPage = 0;
    private int totalRows = 0;

    public DataPage(ModuleDefinition module) {
        this.module = Objects.requireNonNull(module);
        getStyleClass().add("page-root"); setPadding(new Insets(0));
        search.setPromptText("Search by keyword or ID"); search.setPrefWidth(260);
        fromDate.setPromptText("From date"); toDate.setPromptText("To date");
        status.getItems().add("All"); status.getItems().addAll(module.statuses()); status.getSelectionModel().selectFirst();
        branch.setPromptText("All branches");
        minAmount.setPromptText("Minimum amount"); maxAmount.setPromptText("Maximum amount");
        pageSize.getItems().addAll(10,25,50,100); pageSize.setValue(ApplicationConfig.DEFAULT_PAGE_SIZE);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No records match the selected filters."));
        table.setRowFactory(view -> {
            TableRow<Map<String,Object>> row = new TableRow<>();
            row.setOnMouseClicked(event -> { if (event.getClickCount()==2 && !row.isEmpty()) showDetails(row.getItem()); });
            return row;
        });
        StackPane center = new StackPane(buildContent(), loading);
        setCenter(center);
        loadBranches(); loadData();
    }

    private Node buildContent() {
        VBox root = new VBox(18); root.setPadding(new Insets(24));
        Label title = new Label(module.title()); title.getStyleClass().add("page-title");
        Label description = new Label(module.description()); description.getStyleClass().add("page-description"); description.setWrapText(true);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button add = new Button(actionLabel()); add.getStyleClass().add("primary-button"); add.setOnAction(e -> performPrimaryAction());
        add.setVisible(hasPrimaryAction()); add.setManaged(hasPrimaryAction());
        Button details = new Button("View details"); details.setOnAction(e -> selectedDetails());
        Button export = new Button("Export filtered"); export.setOnAction(e -> exportRows());
        Button refresh = new Button("Refresh"); refresh.setOnAction(e -> loadData());
        HBox heading = new HBox(10, new VBox(4,title,description), spacer, add, details, export, refresh); heading.setAlignment(Pos.CENTER_LEFT);

        HBox cards = new HBox(14,totalRecords,filteredValue,currentPeriod);
        HBox.setHgrow(totalRecords,Priority.ALWAYS); HBox.setHgrow(filteredValue,Priority.ALWAYS); HBox.setHgrow(currentPeriod,Priority.ALWAYS);
        totalRecords.setMaxWidth(Double.MAX_VALUE); filteredValue.setMaxWidth(Double.MAX_VALUE); currentPeriod.setMaxWidth(Double.MAX_VALUE);

        FlowPane filters = new FlowPane(10,10); filters.getStyleClass().add("filter-panel"); filters.setPadding(new Insets(14));
        filters.getChildren().add(search);
        if(module.dateColumn()!=null){filters.getChildren().addAll(fromDate,toDate);}
        if(module.statusColumn()!=null){status.setPrefWidth(170); filters.getChildren().add(status);}
        if(module.branchColumn()!=null && canChooseBranch()){branch.setPrefWidth(180); filters.getChildren().add(branch);}
        if(module.amountColumn()!=null){minAmount.setPrefWidth(140); maxAmount.setPrefWidth(140); filters.getChildren().addAll(minAmount,maxAmount);}
        Button apply = new Button("Apply filters"); apply.getStyleClass().add("primary-button"); apply.setOnAction(e -> {currentPage=0; loadData();});
        Button clear = new Button("Clear"); clear.setOnAction(e -> clearFilters());
        filters.getChildren().addAll(new Label("Rows:"),pageSize,apply,clear);
        search.setOnAction(e -> {currentPage=0; loadData();});

        VBox tableCard = new VBox(10); tableCard.getStyleClass().add("content-card"); tableCard.setPadding(new Insets(14));
        HBox tableHeader = new HBox(matchCount); tableHeader.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(table,Priority.ALWAYS); tableCard.getChildren().addAll(tableHeader,table);

        previous.setOnAction(e -> {if(currentPage>0){currentPage--; loadData();}});
        next.setOnAction(e -> {if((currentPage+1)*pageSize.getValue()<totalRows){currentPage++; loadData();}});
        Region pageSpacer = new Region(); HBox.setHgrow(pageSpacer,Priority.ALWAYS);
        HBox pagination = new HBox(10,matchCount,pageSpacer,previous,pageLabel,next); pagination.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().addAll(heading,cards,filters,tableCard,pagination); VBox.setVgrow(tableCard,Priority.ALWAYS);
        return root;
    }

    private boolean canChooseBranch(){return AppSession.current().role()==Role.OWNER || AppSession.current().role()==Role.ADMIN || AppSession.hasPermission("VIEW_ALL_BRANCHES");}

    private void loadBranches() {
        if(module.branchColumn()==null || !canChooseBranch()) return;
        Task<List<QueryDAO.Option>> task = new Task<>() { protected List<QueryDAO.Option> call() throws Exception {
            return dao.options("SELECT BranchID, Name FROM branches WHERE IsActive=1 ORDER BY Name",List.of()); }};
        task.setOnSucceeded(e -> { branch.getItems().setAll(new QueryDAO.Option(null,"All branches")); branch.getItems().addAll(task.getValue()); branch.getSelectionModel().selectFirst(); });
        Thread thread=new Thread(task,"load-branches");thread.setDaemon(true);thread.start();
    }

    private void loadData() {
        QueryParts parts;
        try { parts=buildQuery(); }
        catch (IllegalArgumentException exception){AlertUtil.warning("Invalid filters",exception.getMessage()); return;}
        loading.show("Loading " + module.title().toLowerCase() + "…");
        Task<QueryDAO.QueryResult> task = new Task<>() { protected QueryDAO.QueryResult call() throws Exception {
            return dao.query(parts.selectSql(),parts.countSql(),parts.parameters(),pageSize.getValue(),currentPage*pageSize.getValue()); }};
        task.setOnSucceeded(e -> {render(task.getValue()); loading.hide();});
        task.setOnFailed(e -> {loading.hide(); AlertUtil.error("Could not load data","The database query failed. Check MySQL and the imported final schema.");});
        Thread thread = new Thread(task,"load-"+module.key()); thread.setDaemon(true); thread.start();
    }

    private QueryParts buildQuery() {
        List<String> where = new ArrayList<>(); List<String> having = new ArrayList<>(); List<Object> parameters = new ArrayList<>();
        String keyword = search.getText()==null?"":search.getText().trim();
        if(!keyword.isBlank()){
            List<String> expressions=new ArrayList<>();
            for(String column:module.searchColumns()){expressions.add(column+" LIKE ?"); parameters.add("%"+keyword+"%");}
            if(keyword.matches("\\d+") && module.idColumn()!=null){expressions.add(module.idColumn()+"=?"); parameters.add(Integer.parseInt(keyword));}
            if(!expressions.isEmpty()) where.add("("+String.join(" OR ",expressions)+")");
        }
        if(module.dateColumn()!=null){
            if(fromDate.getValue()!=null){where.add("DATE("+module.dateColumn()+")>=?");parameters.add(fromDate.getValue());}
            if(toDate.getValue()!=null){where.add("DATE("+module.dateColumn()+")<=?");parameters.add(toDate.getValue());}
            if(fromDate.getValue()!=null && toDate.getValue()!=null && fromDate.getValue().isAfter(toDate.getValue())) throw new IllegalArgumentException("The start date cannot be after the end date.");
        }
        if(module.statusColumn()!=null && status.getValue()!=null && !"All".equals(status.getValue())){where.add(module.statusColumn()+"=?");parameters.add(status.getValue());}
        if(module.branchColumn()!=null){
            Integer branchId=scopeBranchId();
            if(branchId!=null){where.add(module.branchColumn()+"=?");parameters.add(branchId);}
            else if(canChooseBranch() && branch.getValue()!=null && branch.getValue().id()!=null){where.add(module.branchColumn()+"=?");parameters.add(branch.getValue().id());}
        }
        if(module.customerColumn()!=null && AppSession.current().role()==Role.CUSTOMER && AppSession.current().customerId()!=null){where.add(module.customerColumn()+"=?");parameters.add(AppSession.current().customerId());}
        if(module.key().equals("notifications")){where.add("(n.UserID IS NULL OR n.UserID=?)");parameters.add(AppSession.current().userId());}
        addAmountFilter(minAmount.getText(),true,where,having,parameters);
        addAmountFilter(maxAmount.getText(),false,where,having,parameters);
        String sql=injectConditions(module.selectSql(),where,having)+" ORDER BY "+module.defaultOrder();
        String count="SELECT COUNT(*) FROM ("+injectConditions(module.selectSql(),where,having)+") counted";
        return new QueryParts(sql,count,parameters);
    }

    private void addAmountFilter(String raw, boolean minimum, List<String> where, List<String> having, List<Object> params){
        if(module.amountColumn()==null || raw==null || raw.isBlank()) return;
        try {
            BigDecimal value=new BigDecimal(raw.trim()); if(value.signum()<0) throw new NumberFormatException();
            String condition=module.amountColumn()+(minimum?">=":"<=")+"?";
            if(module.amountColumn().toUpperCase().contains("SUM(")) having.add(condition); else where.add(condition);
            params.add(value);
        } catch(NumberFormatException exception){throw new IllegalArgumentException("Amounts must be valid non-negative numbers.");}
    }

    private String injectConditions(String base,List<String> where,List<String> having){
        String upper=base.toUpperCase(Locale.ROOT); int group=upper.indexOf(" GROUP BY ");
        String before=group>=0?base.substring(0,group):base; String after=group>=0?base.substring(group):"";
        if(!where.isEmpty()) before += (before.toUpperCase(Locale.ROOT).contains(" WHERE ")?" AND ":" WHERE ")+String.join(" AND ",where);
        String result=before+after;
        if(!having.isEmpty()) result += " HAVING "+String.join(" AND ",having);
        return result;
    }

    private Integer scopeBranchId(){
        Role role=AppSession.current().role();
        if((role==Role.MANAGER || role==Role.CASHIER) && AppSession.current().branchId()!=null) return AppSession.current().branchId();
        return null;
    }

    private void render(QueryDAO.QueryResult result){
        currentRows=result.rows(); currentColumns=result.columns(); totalRows=result.total();
        table.getColumns().clear();
        for(String column:currentColumns){
            TableColumn<Map<String,Object>,Object> tableColumn=new TableColumn<>(column);
            tableColumn.setCellValueFactory(data -> new ReadOnlyObjectWrapper<>(data.getValue().get(column)));
            tableColumn.setCellFactory(c -> new TableCell<>(){ protected void updateItem(Object item,boolean empty){super.updateItem(item,empty); if(empty||item==null){setText(null);setGraphic(null);getStyleClass().remove("status-cell");}else{setText(format(item)); if(column.toLowerCase().contains("status")||column.equalsIgnoreCase("active")){getStyleClass().add("status-cell");}}}});
            tableColumn.setPrefWidth(Math.max(110,Math.min(240,column.length()*12.0))); table.getColumns().add(tableColumn);
        }
        table.getItems().setAll(currentRows); matchCount.setText(totalRows+" matching records");
        totalRecords.setValue(String.format("%,d",totalRows)); totalRecords.setSubtitle("Filtered database records");
        BigDecimal sum=BigDecimal.ZERO;
        if(module.amountColumn()!=null){
            for(Map<String,Object> row:currentRows) for(Object value:row.values()) if(value instanceof BigDecimal decimal){sum=sum.add(decimal);break;}
            filteredValue.setValue(String.format("%,.2f",sum)); filteredValue.setSubtitle("Current page monetary total");
        } else { filteredValue.setValue(String.valueOf(currentRows.size())); filteredValue.setSubtitle("Records on this page"); }
        String range=(fromDate.getValue()==null?"Any":fromDate.getValue().toString())+" → "+(toDate.getValue()==null?"Today":toDate.getValue().toString());
        currentPeriod.setValue(range); currentPeriod.setSubtitle("Applied reporting period");
        int pages=Math.max(1,(int)Math.ceil(totalRows/(double)pageSize.getValue())); pageLabel.setText("Page "+(currentPage+1)+" of "+pages);
        previous.setDisable(currentPage==0); next.setDisable((currentPage+1)*pageSize.getValue()>=totalRows);
    }

    private String format(Object value){
        if(value instanceof BigDecimal decimal)return String.format("%,.2f",decimal);
        if(value instanceof Number number && !(value instanceof Integer) && !(value instanceof Long))return String.format("%,.2f",number.doubleValue());
        return String.valueOf(value);
    }

    private void clearFilters(){search.clear();fromDate.setValue(null);toDate.setValue(null);status.getSelectionModel().selectFirst();if(!branch.getItems().isEmpty())branch.getSelectionModel().selectFirst();minAmount.clear();maxAmount.clear();currentPage=0;loadData();}
    private void selectedDetails(){Map<String,Object> row=table.getSelectionModel().getSelectedItem();if(row==null){AlertUtil.warning("Select a record","Choose a table row first.");return;}showDetails(row);}
    private void showDetails(Map<String,Object> row){
        Dialog<Void> dialog=new Dialog<>();dialog.setTitle(module.title()+" details");dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        GridPane grid=new GridPane();grid.setHgap(14);grid.setVgap(10);grid.setPadding(new Insets(18));int index=0;
        for(var entry:row.entrySet()){Label key=new Label(entry.getKey());key.getStyleClass().add("detail-label");Label value=new Label(format(entry.getValue()));value.setWrapText(true);grid.addRow(index++,key,value);}
        ScrollPane scroll=new ScrollPane(grid);scroll.setFitToWidth(true);scroll.setPrefViewportWidth(650);scroll.setPrefViewportHeight(480);dialog.getDialogPane().setContent(scroll);dialog.showAndWait();
    }

    private void exportRows(){
        QueryParts parts;
        try{parts=buildQuery();}catch(IllegalArgumentException exception){AlertUtil.warning("Invalid filters",exception.getMessage());return;}
        Path path=ExportUtil.chooseCsv(getScene().getWindow(),module.key()+"-filtered.csv");if(path==null)return;
        loading.show("Exporting all filtered records…");
        Task<List<Map<String,Object>>> task=new Task<>(){protected List<Map<String,Object>> call()throws Exception{return dao.queryAll(parts.selectSql(),parts.parameters());}};
        task.setOnSucceeded(event->{loading.hide();List<Map<String,Object>> rows=task.getValue();if(rows.isEmpty()){AlertUtil.warning("Nothing to export","The selected filters returned no records.");return;}try{List<String> columns=new ArrayList<>(rows.getFirst().keySet());ExportUtil.writeCsv(path,columns,rows);AlertUtil.info("Export complete",rows.size()+" filtered records were exported to:\n"+path);}catch(Exception exception){AlertUtil.error("Export failed","The CSV file could not be written.");}});
        task.setOnFailed(event->{loading.hide();AlertUtil.error("Export failed","The complete filtered dataset could not be loaded.");});
        Thread thread=new Thread(task,"export-"+module.key());thread.setDaemon(true);thread.start();
    }

    private boolean hasPrimaryAction(){return Set.of("customers","employees","users","expenses","suppliers","purchase_orders","daily_closing","stock_requests","orders","notifications").contains(module.key());}
    private String actionLabel(){return switch(module.key()){case "notifications"->"Mark selected read";case "orders"->"Update status";case "stock_requests"->"Review request";default->"Add new";};}
    private void performPrimaryAction(){
        switch(module.key()){
            case "customers" -> addCustomer(); case "employees" -> addEmployee(); case "users" -> addUser();
            case "expenses" -> addExpense(); case "suppliers" -> addSupplier(); case "purchase_orders" -> addPurchaseOrder();
            case "daily_closing" -> addClosing(); case "stock_requests" -> reviewRequest(); case "orders" -> updateOrder();
            case "notifications" -> markNotificationRead(); default -> AlertUtil.info("Action","Use the record details and related workflow page.");
        }
    }

    private Map<String,String> inputDialog(String title,String... fields){
        Dialog<Map<String,String>> dialog=new Dialog<>();dialog.setTitle(title);dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);
        GridPane grid=new GridPane();grid.setHgap(12);grid.setVgap(10);grid.setPadding(new Insets(18));Map<String,TextField> controls=new LinkedHashMap<>();
        for(int i=0;i<fields.length;i++){TextField input=new TextField();input.setPromptText(fields[i]);controls.put(fields[i],input);grid.addRow(i,new Label(fields[i]),input);}
        dialog.getDialogPane().setContent(grid);dialog.setResultConverter(button -> {if(button!=ButtonType.OK)return null;Map<String,String> result=new LinkedHashMap<>();controls.forEach((k,v)->result.put(k,v.getText().trim()));return result;});
        return dialog.showAndWait().orElse(null);
    }

    private void addCustomer(){Map<String,String> v=inputDialog("Create customer","Full name*","Phone*","Email");if(v==null)return;execute("INSERT INTO customers(FullName,Phone,Email,RegisteredAt,IsActive,CreatedAt) VALUES(?,?,?,CURDATE(),1,NOW())",List.of(v.get("Full name*"),v.get("Phone*"),v.get("Email")),"Customer created");}
    private void addEmployee(){Map<String,String> v=inputDialog("Create employee","Full name*","Phone","Email","Job title*","Salary");if(v==null)return;try{execute("INSERT INTO employees(FullName,Phone,Email,JobTitle,Salary,HireDate,IsActive,CreatedAt) VALUES(?,?,?,?,?,CURDATE(),1,NOW())",List.of(v.get("Full name*"),v.get("Phone"),v.get("Email"),v.get("Job title*"),new BigDecimal(v.get("Salary"))),"Employee created");}catch(Exception e){AlertUtil.warning("Invalid salary","Enter a valid salary.");}}
    private void addUser(){Map<String,String> v=inputDialog("Create secure user","Full name*","Username*","Password*","Role (OWNER/ADMIN/MANAGER/CASHIER/WAREHOUSE/CUSTOMER)*");if(v==null)return;try{String hash=org.mindrot.jbcrypt.BCrypt.hashpw(v.get("Password*"),org.mindrot.jbcrypt.BCrypt.gensalt(12));execute("INSERT INTO users(FullName,Username,PasswordHash,RoleID,IsActive,PasswordChangeRequired,CreatedAt,CreatedBy) SELECT ?,?,?,RoleID,1,1,NOW(),? FROM roles WHERE RoleName=?",List.of(v.get("Full name*"),v.get("Username*"),hash,AppSession.current().userId(),v.get("Role (OWNER/ADMIN/MANAGER/CASHIER/WAREHOUSE/CUSTOMER)*").toUpperCase()),"User created");}catch(Exception e){AlertUtil.error("User not created","Check that the username is unique and role is valid.");}}
    private void addExpense(){Map<String,String> v=inputDialog("Record expense","Category*","Description*","Amount*","Payment method");if(v==null)return;try{execute("INSERT INTO expenses(ExpenseDate,Category,Description,Amount,PaymentMethod,Status,RecordedBy,CreatedAt) VALUES(CURDATE(),?,?,?,?, 'PENDING',?,NOW())",List.of(v.get("Category*"),v.get("Description*"),new BigDecimal(v.get("Amount*")),v.get("Payment method"),AppSession.current().userId()),"Expense recorded");}catch(Exception e){AlertUtil.warning("Invalid amount","Enter a valid amount.");}}
    private void addSupplier(){Map<String,String> v=inputDialog("Create supplier","Supplier code*","Supplier name*","Contact person","Phone","Email");if(v==null)return;execute("INSERT INTO suppliers(SupplierCode,SupplierName,ContactPerson,Phone,Email,IsActive,CreatedAt) VALUES(?,?,?,?,?,1,NOW())",new ArrayList<>(v.values()),"Supplier created");}
    private void addPurchaseOrder(){Map<String,String> v=inputDialog("Create purchase order","Supplier ID*","Warehouse ID*","Expected date (YYYY-MM-DD)","Notes");if(v==null)return;try{execute("INSERT INTO purchase_orders(PONumber,SupplierID,WarehouseID,OrderDate,ExpectedDate,TotalCost,Status,Notes,CreatedBy,CreatedAt) VALUES(CONCAT('PO-',DATE_FORMAT(NOW(),'%Y%m%d'),'-',LPAD(FLOOR(RAND()*9999),4,'0')),?,?,CURDATE(),?,0,'DRAFT',?,?,NOW())",List.of(Integer.parseInt(v.get("Supplier ID*")),Integer.parseInt(v.get("Warehouse ID*")),LocalDate.parse(v.get("Expected date (YYYY-MM-DD)")),v.get("Notes"),AppSession.current().userId()),"Purchase order created");}catch(Exception e){AlertUtil.warning("Invalid values","Use valid supplier, warehouse and date values.");}}
    private void addClosing(){Map<String,String> v=inputDialog("Create daily closing","Opening cash*","Expected cash*","Actual cash*","Notes");if(v==null)return;try{BigDecimal opening=new BigDecimal(v.get("Opening cash*")),expected=new BigDecimal(v.get("Expected cash*")),actual=new BigDecimal(v.get("Actual cash*"));Integer branchId=AppSession.current().branchId();if(branchId==null)throw new IllegalArgumentException();execute("INSERT INTO daily_closings(ClosingDate,BranchID,CashierUserID,ShiftStart,ShiftEnd,OpeningCash,ExpectedCash,ActualCash,DifferenceAmount,CashSales,CardSales,Refunds,Status,Notes,CreatedAt) VALUES(CURDATE(),?,?,NOW(),NOW(),?,?,?,? - ?,0,0,0,'PENDING',?,NOW())",List.of(branchId,AppSession.current().userId(),opening,expected,actual,actual,expected,v.get("Notes")),"Daily closing submitted");}catch(Exception e){AlertUtil.warning("Cannot close shift","Only an assigned cashier can close a shift, and amounts must be valid.");}}
    private void reviewRequest(){Map<String,Object> row=table.getSelectionModel().getSelectedItem();if(row==null){AlertUtil.warning("Select a request","Choose a stock request first.");return;}String choice=new ChoiceDialog<>("APPROVED",List.of("APPROVED","PARTIALLY_APPROVED","REJECTED","FULFILLED")).showAndWait().orElse(null);if(choice==null)return;execute("UPDATE stock_requests SET Status=?,ApprovedBy=?,ApprovedAt=NOW() WHERE RequestID=?",List.of(choice,AppSession.current().userId(),numberId(row)),"Request updated");}
    private void updateOrder(){Map<String,Object> row=table.getSelectionModel().getSelectedItem();if(row==null){AlertUtil.warning("Select an order","Choose an order first.");return;}String choice=new ChoiceDialog<>("CONFIRMED",List.of("CONFIRMED","PROCESSING","READY","DELIVERED","CANCELLED","RETURNED")).showAndWait().orElse(null);if(choice==null)return;execute("UPDATE online_orders SET Status=?,UpdatedAt=NOW() WHERE OrderID=?",List.of(choice,numberId(row)),"Order status updated");}
    private void markNotificationRead(){Map<String,Object> row=table.getSelectionModel().getSelectedItem();if(row==null){AlertUtil.warning("Select a notification","Choose a notification first.");return;}execute("UPDATE notifications SET IsRead=1,ReadAt=NOW() WHERE NotificationID=? AND (UserID IS NULL OR UserID=?)",List.of(numberId(row),AppSession.current().userId()),"Notification marked as read");}
    private int numberId(Map<String,Object> row){Object value=row.values().iterator().next();return value instanceof Number n?n.intValue():Integer.parseInt(value.toString());}
    private void execute(String sql,List<?> values,String success){
        Task<Integer> task=new Task<>(){protected Integer call() throws Exception{return dao.update(sql,new ArrayList<>(values));}};loading.show("Saving changes…");
        task.setOnSucceeded(e->{loading.hide();if(task.getValue()>0){AlertUtil.info("Saved",success);loadData();}else AlertUtil.warning("No change","The database did not update any record.");});
        task.setOnFailed(e->{loading.hide();AlertUtil.error("Save failed","The record could not be saved. Check required values and database constraints.");});
        Thread thread=new Thread(task,"save-"+module.key());thread.setDaemon(true);thread.start();
    }

    private record QueryParts(String selectSql,String countSql,List<Object> parameters){}
}
