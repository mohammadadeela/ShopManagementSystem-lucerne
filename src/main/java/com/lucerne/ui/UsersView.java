package com.lucerne.ui;

import com.lucerne.app.AppSession;
import com.lucerne.config.ApplicationConfig;
import com.lucerne.dao.QueryDAO;
import com.lucerne.dao.UserAdminDAO;
import com.lucerne.ui.components.LoadingPane;
import com.lucerne.ui.components.MetricCard;
import com.lucerne.util.AlertUtil;
import com.lucerne.util.ExportUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Full owner/admin account-management page. */
public final class UsersView extends StackPane {
    private final UserAdminDAO dao = new UserAdminDAO();
    private final TableView<Map<String,Object>> table = new TableView<>();
    private final TextField search = new TextField();
    private final ComboBox<QueryDAO.Option> role = new ComboBox<>();
    private final ComboBox<String> accountStatus = new ComboBox<>();
    private final ComboBox<String> lockStatus = new ComboBox<>();
    private final ComboBox<QueryDAO.Option> branch = new ComboBox<>();
    private final ComboBox<QueryDAO.Option> warehouse = new ComboBox<>();
    private final DatePicker createdFrom = new DatePicker();
    private final DatePicker createdTo = new DatePicker();
    private final CheckBox forceChange = new CheckBox("Password change required");
    private final CheckBox neverLoggedIn = new CheckBox("Never logged in");
    private final ComboBox<Integer> pageSize = new ComboBox<>();
    private final Label matching = new Label("0 matching users");
    private final Label pageLabel = new Label("Page 1");
    private final Button previous = new Button("Previous");
    private final Button next = new Button("Next");
    private final LoadingPane loading = new LoadingPane();
    private final PieChart roleChart = new PieChart();
    private final MetricCard total = card("Total users");
    private final MetricCard active = card("Active users");
    private final MetricCard inactive = card("Inactive users");
    private final MetricCard locked = card("Locked users");
    private final MetricCard monthLogins = card("Logged in this month");
    private final MetricCard never = card("Never logged in");
    private List<Map<String,Object>> rows = List.of();
    private List<String> columns = List.of();
    private int totalRows;
    private int page;

    public UsersView() {
        getChildren().addAll(build(),loading);
        loadLookups();
        load();
    }

    private Node build() {
        VBox root = new VBox(18);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("page-root");

        Label title = new Label("Manage Users"); title.getStyleClass().add("page-title");
        Label description = new Label("Create, secure, assign and audit application accounts without deleting historical identities.");
        description.getStyleClass().add("page-description");
        Region spacer = new Region(); HBox.setHgrow(spacer,Priority.ALWAYS);
        Button create = new Button("Create user"); create.getStyleClass().add("primary-button"); create.setOnAction(e->openForm(null));
        Button edit = new Button("Edit selected"); edit.setOnAction(e->edit());
        Button reset = new Button("Reset password"); reset.setOnAction(e->resetPassword());
        MenuButton security = new MenuButton("Security actions");
        MenuItem activate = new MenuItem("Activate"); activate.setOnAction(e->setActive(true));
        MenuItem deactivate = new MenuItem("Deactivate"); deactivate.setOnAction(e->setActive(false));
        MenuItem lockItem = new MenuItem("Lock 30 minutes"); lockItem.setOnAction(e->lock());
        MenuItem unlockItem = new MenuItem("Unlock"); unlockItem.setOnAction(e->unlock());
        security.getItems().addAll(activate,deactivate,new SeparatorMenuItem(),lockItem,unlockItem);
        Button export = new Button("Export filtered"); export.setOnAction(e->export());
        Button refresh = new Button("Refresh"); refresh.setOnAction(e->load());
        HBox heading = new HBox(10,new VBox(4,title,description),spacer,create,edit,reset,security,export,refresh);
        heading.setAlignment(Pos.CENTER_LEFT);

        FlowPane cards = new FlowPane(12,12,total,active,inactive,locked,monthLogins,never);
        cards.setPrefWrapLength(1200);
        for(Node node:cards.getChildren()) if(node instanceof MetricCard metric)metric.setPrefWidth(190);

        FlowPane filters = new FlowPane(10,10);
        filters.getStyleClass().add("filter-panel"); filters.setPadding(new Insets(14));
        search.setPromptText("Username, name, employee or customer"); search.setPrefWidth(250); search.setOnAction(e->{page=0;load();});
        role.setPromptText("All roles"); accountStatus.getItems().addAll("All accounts","Active","Inactive"); accountStatus.setValue("All accounts");
        lockStatus.getItems().addAll("All lock states","Locked","Unlocked"); lockStatus.setValue("All lock states");
        branch.setPromptText("All branches"); warehouse.setPromptText("All warehouses");
        createdFrom.setPromptText("Created from"); createdTo.setPromptText("Created to");
        pageSize.getItems().addAll(10,25,50,100); pageSize.setValue(ApplicationConfig.DEFAULT_PAGE_SIZE);
        Button apply = new Button("Apply filters"); apply.getStyleClass().add("primary-button"); apply.setOnAction(e->{page=0;load();});
        Button clear = new Button("Clear"); clear.setOnAction(e->clearFilters());
        filters.getChildren().addAll(search,role,accountStatus,lockStatus,branch,warehouse,createdFrom,createdTo,forceChange,neverLoggedIn,new Label("Rows"),pageSize,apply,clear);

        table.setPlaceholder(new Label("No user accounts match the selected filters."));
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setRowFactory(v->{TableRow<Map<String,Object>> row=new TableRow<>();row.setOnMouseClicked(e->{if(e.getClickCount()==2&&!row.isEmpty())openForm(userId(row.getItem()));});return row;});
        VBox tableCard = new VBox(10,matching,table); tableCard.getStyleClass().add("content-card"); tableCard.setPadding(new Insets(14)); VBox.setVgrow(table,Priority.ALWAYS);
        roleChart.setTitle("Users by role"); roleChart.setLegendSide(javafx.geometry.Side.RIGHT); roleChart.setLabelsVisible(true); roleChart.setPrefHeight(280);
        VBox chartCard = new VBox(8,roleChart); chartCard.getStyleClass().add("content-card"); chartCard.setPadding(new Insets(14)); chartCard.setPrefWidth(360);
        HBox content = new HBox(16,tableCard,chartCard); HBox.setHgrow(tableCard,Priority.ALWAYS); VBox.setVgrow(content,Priority.ALWAYS);

        previous.setOnAction(e->{if(page>0){page--;load();}}); next.setOnAction(e->{if((page+1)*pageSize.getValue()<totalRows){page++;load();}});
        Region pageSpacer = new Region(); HBox.setHgrow(pageSpacer,Priority.ALWAYS);
        HBox paging = new HBox(10,matching,pageSpacer,previous,pageLabel,next); paging.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().addAll(heading,cards,filters,content,paging); VBox.setVgrow(content,Priority.ALWAYS);
        return root;
    }

    private static MetricCard card(String title){MetricCard card=new MetricCard(title);card.setValue("—");return card;}

    private void loadLookups() {
        Task<LookupData> task = new Task<>() { protected LookupData call() throws Exception {
            return new LookupData(dao.roles(),dao.branches(),dao.warehouses()); }};
        task.setOnSucceeded(e->{
            role.getItems().setAll(new QueryDAO.Option(null,"All roles")); role.getItems().addAll(task.getValue().roles()); role.getSelectionModel().selectFirst();
            branch.getItems().setAll(new QueryDAO.Option(null,"All branches")); branch.getItems().addAll(task.getValue().branches()); branch.getSelectionModel().selectFirst();
            warehouse.getItems().setAll(new QueryDAO.Option(null,"All warehouses")); warehouse.getItems().addAll(task.getValue().warehouses()); warehouse.getSelectionModel().selectFirst();
        });
        task.setOnFailed(e->AlertUtil.error("Lookups unavailable","Roles and locations could not be loaded."));
        start(task,"user-lookups");
    }

    private void load() {
        if(createdFrom.getValue()!=null&&createdTo.getValue()!=null&&createdFrom.getValue().isAfter(createdTo.getValue())){AlertUtil.warning("Invalid dates","Created-from cannot be after created-to.");return;}
        loading.show("Loading secure account data…");
        UserAdminDAO.UserFilter filter = new UserAdminDAO.UserFilter(search.getText(),optionLabel(role),
                switch(accountStatus.getValue()){case "Active"->true;case "Inactive"->false;default->null;},
                switch(lockStatus.getValue()){case "Locked"->true;case "Unlocked"->false;default->null;},
                forceChange.isSelected()?true:null,neverLoggedIn.isSelected(),optionInt(branch),optionInt(warehouse),createdFrom.getValue(),createdTo.getValue());
        Task<PageData> task = new Task<>() { protected PageData call() throws Exception {
            return new PageData(dao.search(filter,pageSize.getValue(),page*pageSize.getValue()),dao.summary(),dao.usersByRole()); }};
        task.setOnSucceeded(e->{render(task.getValue());loading.hide();});
        task.setOnFailed(e->{loading.hide();AlertUtil.error("Users unavailable","The user-management query failed. Check database setup and permissions.");});
        start(task,"users-load");
    }

    private void render(PageData data) {
        rows=data.result().rows(); columns=data.result().columns(); totalRows=data.result().total();
        table.getColumns().clear();
        for(String column:columns){
            TableColumn<Map<String,Object>,Object> c=new TableColumn<>(column);
            c.setCellValueFactory(v->new ReadOnlyObjectWrapper<>(v.getValue().get(column)));
            c.setPrefWidth(Math.max(105,Math.min(210,column.length()*12)));
            c.setCellFactory(v->new TableCell<>(){protected void updateItem(Object item,boolean empty){super.updateItem(item,empty);setText(empty||item==null?null:String.valueOf(item));if(!empty&&(column.equals("Status")||column.equals("Lock Status")))getStyleClass().add("status-cell");}});
            table.getColumns().add(c);
        }
        table.getItems().setAll(rows); matching.setText(totalRows+" matching users");
        UserAdminDAO.Summary s=data.summary();total.setValue(String.valueOf(s.total()));active.setValue(String.valueOf(s.active()));inactive.setValue(String.valueOf(s.inactive()));locked.setValue(String.valueOf(s.locked()));monthLogins.setValue(String.valueOf(s.loggedThisMonth()));never.setValue(String.valueOf(s.neverLoggedIn()));
        total.setSubtitle(s.passwordChangeRequired()+" require password change");active.setSubtitle("Enabled accounts");inactive.setSubtitle("History preserved");locked.setSubtitle("Temporarily restricted");monthLogins.setSubtitle("Successful login this month");never.setSubtitle("No successful session yet");
        roleChart.getData().clear();for(Map<String,Object> row:data.byRole()){Object count=row.get("Users");if(count instanceof Number n&&n.doubleValue()>0)roleChart.getData().add(new PieChart.Data(String.valueOf(row.get("Role")),n.doubleValue()));}
        int pages=Math.max(1,(int)Math.ceil(totalRows/(double)pageSize.getValue()));pageLabel.setText("Page "+(page+1)+" of "+pages);previous.setDisable(page==0);next.setDisable((page+1)*pageSize.getValue()>=totalRows);
    }

    private void edit(){Map<String,Object> row=table.getSelectionModel().getSelectedItem();if(row==null){AlertUtil.warning("Select a user","Choose an account first.");return;}openForm(userId(row));}
    private int userId(Map<String,Object> row){return ((Number)row.get("User ID")).intValue();}

    private void openForm(Integer id) {
        loading.show(id==null?"Preparing account form…":"Loading account details…");
        Task<FormData> task=new Task<>(){protected FormData call()throws Exception{return new FormData(id==null?null:dao.find(id),dao.roles(),dao.employees(),dao.customers(),dao.branches(),dao.warehouses());}};
        task.setOnSucceeded(e->{loading.hide();showForm(task.getValue());});task.setOnFailed(e->{loading.hide();AlertUtil.error("Form unavailable","User details could not be loaded.");});start(task,"user-form-data");
    }

    private void showForm(FormData data) {
        boolean editing=data.details()!=null;
        Dialog<UserAdminDAO.UserForm> dialog=new Dialog<>();dialog.setTitle(editing?"Edit user":"Create user");dialog.setHeaderText(editing?"Update account identity, role and links.":"Create a secure role-based account.");dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);
        TextField fullName=new TextField(),username=new TextField();PasswordField password=new PasswordField();
        ComboBox<QueryDAO.Option> roleField=new ComboBox<>(),employee=new ComboBox<>(),customer=new ComboBox<>(),branchField=new ComboBox<>(),warehouseField=new ComboBox<>();
        CheckBox activeField=new CheckBox("Account active"),forceField=new CheckBox("Force password change");DatePicker expiry=new DatePicker();
        roleField.getItems().setAll(data.roles());employee.getItems().add(new QueryDAO.Option(null,"No employee link"));employee.getItems().addAll(data.employees());customer.getItems().add(new QueryDAO.Option(null,"No customer link"));customer.getItems().addAll(data.customers());branchField.getItems().add(new QueryDAO.Option(null,"No branch"));branchField.getItems().addAll(data.branches());warehouseField.getItems().add(new QueryDAO.Option(null,"No warehouse"));warehouseField.getItems().addAll(data.warehouses());
        employee.getSelectionModel().selectFirst();customer.getSelectionModel().selectFirst();branchField.getSelectionModel().selectFirst();warehouseField.getSelectionModel().selectFirst();activeField.setSelected(true);forceField.setSelected(true);
        if(editing){UserAdminDAO.UserDetails d=data.details();fullName.setText(d.fullName());username.setText(d.username());selectByLabel(roleField,d.roleName());selectById(employee,d.employeeId());selectById(customer,d.customerId());selectById(branchField,d.branchId());selectById(warehouseField,d.warehouseId());activeField.setSelected(d.active());forceField.setSelected(d.passwordChangeRequired());if(d.accountExpiresAt()!=null)expiry.setValue(d.accountExpiresAt().toLocalDate());password.setDisable(true);password.setPromptText("Use Reset password action");}
        GridPane grid=new GridPane();grid.setHgap(14);grid.setVgap(10);grid.setPadding(new Insets(18));int r=0;
        grid.addRow(r++,new Label("Full name *"),fullName);grid.addRow(r++,new Label("Username *"),username);grid.addRow(r++,new Label(editing?"Password":"Initial password *"),password);grid.addRow(r++,new Label("Role *"),roleField);grid.addRow(r++,new Label("Employee link"),employee);grid.addRow(r++,new Label("Customer link"),customer);grid.addRow(r++,new Label("Branch assignment"),branchField);grid.addRow(r++,new Label("Warehouse assignment"),warehouseField);grid.addRow(r++,new Label("Account expiration"),expiry);grid.addRow(r++,new Label("Security"),new VBox(6,activeField,forceField));
        Label helper=new Label("Branch/warehouse assignment is stored on the linked employee. Only OWNER can assign OWNER or ADMIN.");helper.setWrapText(true);helper.getStyleClass().add("secondary-text");grid.add(helper,0,r,2,1);
        dialog.getDialogPane().setContent(new ScrollPane(grid));dialog.getDialogPane().setPrefWidth(640);
        dialog.setResultConverter(button->{if(button!=ButtonType.OK)return null;return new UserAdminDAO.UserForm(editing?data.details().userId():null,fullName.getText(),username.getText(),editing?null:password.getText(),selectedLabel(roleField),activeField.isSelected(),forceField.isSelected(),expiry.getValue(),optionInt(employee),optionInt(customer),optionInt(branchField),optionInt(warehouseField));});
        dialog.showAndWait().ifPresent(form->save(form,editing));
    }

    private void save(UserAdminDAO.UserForm form,boolean editing){loading.show(editing?"Updating account…":"Creating account…");Task<Void> task=new Task<>(){protected Void call()throws Exception{if(editing)dao.update(form);else dao.create(form);return null;}};task.setOnSucceeded(e->{loading.hide();AlertUtil.info("Account saved",editing?"The user account was updated.":"The user account was created with a forced password change.");load();});task.setOnFailed(e->{loading.hide();String message=task.getException()==null?"The account could not be saved.":task.getException().getMessage();AlertUtil.error("Account not saved",friendly(message));});start(task,"user-save");}
    private void setActive(boolean value){Integer id=selectedId();if(id==null)return;if(!AlertUtil.confirm(value?"Activate account":"Deactivate account",value?"Enable this user account?":"Deactivate this account while preserving its history?"))return;runAction(value?"Activating account…":"Deactivating account…",()->dao.setActive(id,value),"Account status updated.");}
    private void lock(){Integer id=selectedId();if(id==null)return;runAction("Locking account…",()->dao.lock(id,30),"The account is locked for 30 minutes.");}
    private void unlock(){Integer id=selectedId();if(id==null)return;runAction("Unlocking account…",()->dao.unlock(id),"The account is unlocked and failed attempts were cleared.");}
    private void resetPassword(){Integer id=selectedId();if(id==null)return;Dialog<String> d=new Dialog<>();d.setTitle("Reset password");d.setHeaderText("Set a temporary password. The user must change it at next login.");d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);PasswordField p1=new PasswordField(),p2=new PasswordField();p1.setPromptText("At least 8 characters");p2.setPromptText("Repeat password");GridPane g=new GridPane();g.setHgap(12);g.setVgap(10);g.setPadding(new Insets(18));g.addRow(0,new Label("Temporary password"),p1);g.addRow(1,new Label("Confirm"),p2);d.getDialogPane().setContent(g);d.setResultConverter(b->b==ButtonType.OK?p1.getText():null);d.showAndWait().ifPresent(password->{if(password.length()<8||!password.equals(p2.getText())){AlertUtil.warning("Invalid password","Passwords must match and contain at least 8 characters.");return;}runAction("Resetting password…",()->dao.resetPassword(id,password),"Password reset. Change is required at next login.");});}
    private Integer selectedId(){Map<String,Object> row=table.getSelectionModel().getSelectedItem();if(row==null){AlertUtil.warning("Select a user","Choose an account first.");return null;}return userId(row);}
    private void runAction(String progress,SqlRunnable action,String success){loading.show(progress);Task<Void> task=new Task<>(){protected Void call()throws Exception{action.run();return null;}};task.setOnSucceeded(e->{loading.hide();AlertUtil.info("Completed",success);load();});task.setOnFailed(e->{loading.hide();AlertUtil.error("Action failed",friendly(task.getException()==null?null:task.getException().getMessage()));});start(task,"user-action");}

    private void export(){if(rows.isEmpty()){AlertUtil.warning("Nothing to export","No filtered users are visible.");return;}Path path=ExportUtil.chooseCsv(getScene().getWindow(),"users-filtered.csv");if(path==null)return;List<String> safeColumns=columns.stream().filter(c->!c.toLowerCase().contains("password")).toList();List<Map<String,Object>> safeRows=new ArrayList<>();for(Map<String,Object> row:rows){java.util.LinkedHashMap<String,Object> copy=new java.util.LinkedHashMap<>();for(String c:safeColumns)copy.put(c,row.get(c));safeRows.add(copy);}try{ExportUtil.writeCsv(path,safeColumns,safeRows);AlertUtil.info("Export complete","User metadata was exported without password data.");}catch(Exception e){AlertUtil.error("Export failed","The CSV file could not be written.");}}
    private void clearFilters(){search.clear();if(!role.getItems().isEmpty())role.getSelectionModel().selectFirst();accountStatus.setValue("All accounts");lockStatus.setValue("All lock states");if(!branch.getItems().isEmpty())branch.getSelectionModel().selectFirst();if(!warehouse.getItems().isEmpty())warehouse.getSelectionModel().selectFirst();createdFrom.setValue(null);createdTo.setValue(null);forceChange.setSelected(false);neverLoggedIn.setSelected(false);page=0;load();}
    private static String optionLabel(ComboBox<QueryDAO.Option> box){return box.getValue()==null||box.getValue().id()==null?null:box.getValue().label();}
    private static Integer optionInt(ComboBox<QueryDAO.Option> box){return box.getValue()==null||box.getValue().id()==null?null:((Number)box.getValue().id()).intValue();}
    private static String selectedLabel(ComboBox<QueryDAO.Option> box){return box.getValue()==null?null:box.getValue().label();}
    private static void selectById(ComboBox<QueryDAO.Option> box,Integer id){if(id==null){box.getSelectionModel().selectFirst();return;}for(QueryDAO.Option option:box.getItems())if(option.id() instanceof Number n&&n.intValue()==id){box.setValue(option);return;}}
    private static void selectByLabel(ComboBox<QueryDAO.Option> box,String label){for(QueryDAO.Option option:box.getItems())if(option.label().equals(label)){box.setValue(option);return;}}
    private static String friendly(String message){if(message==null||message.isBlank())return "The operation could not be completed.";String lower=message.toLowerCase();if(lower.contains("duplicate")&&lower.contains("username"))return "That username is already in use.";if(lower.contains("duplicate"))return "A unique value is already in use.";return message;}
    private static void start(Task<?> task,String name){Thread thread=new Thread(task,name);thread.setDaemon(true);thread.start();}

    @FunctionalInterface private interface SqlRunnable{void run()throws Exception;}
    private record LookupData(List<QueryDAO.Option> roles,List<QueryDAO.Option> branches,List<QueryDAO.Option> warehouses){}
    private record PageData(QueryDAO.QueryResult result,UserAdminDAO.Summary summary,List<Map<String,Object>> byRole){}
    private record FormData(UserAdminDAO.UserDetails details,List<QueryDAO.Option> roles,List<QueryDAO.Option> employees,List<QueryDAO.Option> customers,List<QueryDAO.Option> branches,List<QueryDAO.Option> warehouses){}
}
