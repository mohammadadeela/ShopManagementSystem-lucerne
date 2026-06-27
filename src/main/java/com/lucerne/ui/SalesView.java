package com.lucerne.ui;

import com.lucerne.app.AppSession;
import com.lucerne.config.ApplicationConfig;
import com.lucerne.dao.QueryDAO;
import com.lucerne.dao.SalesDAO;
import com.lucerne.service.ReceiptService;
import com.lucerne.service.SaleService;
import com.lucerne.ui.components.LoadingPane;
import com.lucerne.ui.components.MetricCard;
import com.lucerne.ui.dialogs.ReceiptPreviewDialog;
import com.lucerne.util.AlertUtil;
import com.lucerne.util.ExportUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

/** Dedicated transaction browser with receipt and rollback-protected cancellation workflows. */
public final class SalesView extends StackPane {
    private final SalesDAO dao = new SalesDAO();
    private final SaleService saleService = new SaleService();
    private final ReceiptService receiptService = new ReceiptService();
    private final LoadingPane loading = new LoadingPane();
    private final TableView<Map<String,Object>> table = new TableView<>();
    private final TextField search = new TextField(), minimum = new TextField(), maximum = new TextField();
    private final DatePicker from = new DatePicker(LocalDate.now().withDayOfMonth(1)), to = new DatePicker(LocalDate.now());
    private final ComboBox<QueryDAO.Option> branch = new ComboBox<>(), cashier = new ComboBox<>(), customer = new ComboBox<>();
    private final ComboBox<String> payment = new ComboBox<>(), status = new ComboBox<>(), discount = new ComboBox<>();
    private final ComboBox<Integer> pageSize = new ComboBox<>();
    private final Label matching = new Label(), pageLabel = new Label("Page 1");
    private final Button previous = new Button("Previous"), next = new Button("Next");
    private final MetricCard revenue = metric("Net revenue"), transactions = metric("Transactions"), average = metric("Average transaction"),
            discounts = metric("Discounts"), cogs = metric("COGS"), grossProfit = metric("Gross profit"), cancelled = metric("Cancelled value");
    private final BarChart<String,Number> paymentChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private List<Map<String,Object>> rows = List.of();
    private List<String> columns = List.of();
    private int page, total;

    public SalesView() {
        getChildren().addAll(build(), loading);
        loadLookups();
        load();
    }

    private Node build() {
        VBox root = new VBox(18); root.setPadding(new Insets(24)); root.getStyleClass().add("page-root");
        Label title = new Label("Sales & Transactions"); title.getStyleClass().add("page-title");
        Label description = new Label("Search receipts, inspect items, regenerate receipts and safely cancel eligible sales.");
        description.getStyleClass().add("page-description");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button details = new Button("View details"); details.setOnAction(e -> details());
        Button receipt = new Button("Receipt preview"); receipt.setOnAction(e -> receipt());
        Button cancel = new Button("Cancel sale"); cancel.getStyleClass().add("danger-button"); cancel.setOnAction(e -> cancel());
        cancel.setVisible(AppSession.hasPermission("CANCEL_SALE")); cancel.setManaged(cancel.isVisible());
        Button export = new Button("Export filtered"); export.setOnAction(e -> export());
        Button refresh = new Button("Refresh"); refresh.setOnAction(e -> load());
        HBox heading = new HBox(10, new VBox(4,title,description), spacer, details, receipt, cancel, export, refresh);
        heading.setAlignment(Pos.CENTER_LEFT);

        FlowPane cards = new FlowPane(12,12,revenue,transactions,average,discounts,cogs,grossProfit,cancelled);
        for (Node node : cards.getChildren()) if (node instanceof MetricCard card) card.setPrefWidth(188);
        cogs.setVisible(AppSession.hasPermission("VIEW_PRODUCT_COST")); cogs.setManaged(cogs.isVisible());
        grossProfit.setVisible(AppSession.hasPermission("VIEW_PRODUCT_COST")); grossProfit.setManaged(grossProfit.isVisible());

        FlowPane filters = new FlowPane(10,10); filters.getStyleClass().add("filter-panel"); filters.setPadding(new Insets(14));
        search.setPromptText("Receipt, sale ID or customer"); search.setPrefWidth(220); search.setOnAction(e -> apply());
        branch.setPromptText("All branches"); cashier.setPromptText("All cashiers"); customer.setPromptText("All customers");
        payment.getItems().addAll("All payments","CASH","CARD","MIXED","BANK_TRANSFER"); payment.setValue("All payments");
        status.getItems().addAll("All statuses","COMPLETED","CANCELLED","PARTIALLY_RETURNED","RETURNED"); status.setValue("All statuses");
        discount.getItems().addAll("Any discount","With discount","Without discount"); discount.setValue("Any discount");
        minimum.setPromptText("Minimum total"); maximum.setPromptText("Maximum total");
        pageSize.getItems().addAll(10,25,50,100); pageSize.setValue(ApplicationConfig.DEFAULT_PAGE_SIZE);
        Button apply = new Button("Apply filters"); apply.getStyleClass().add("primary-button"); apply.setOnAction(e -> apply());
        Button clear = new Button("Clear"); clear.setOnAction(e -> clear());
        filters.getChildren().addAll(search,from,to,branch,cashier,customer,payment,status,discount,minimum,maximum,new Label("Rows"),pageSize,apply,clear);

        table.setPlaceholder(new Label("No transactions match these filters."));
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setRowFactory(view -> { TableRow<Map<String,Object>> row = new TableRow<>(); row.setOnMouseClicked(e -> { if(e.getClickCount()==2&&!row.isEmpty()) details(row.getItem()); }); return row; });
        VBox tableCard = new VBox(10,matching,table); tableCard.getStyleClass().add("content-card"); tableCard.setPadding(new Insets(14)); VBox.setVgrow(table, Priority.ALWAYS);

        paymentChart.setTitle("Revenue by payment method"); paymentChart.setLegendVisible(false); paymentChart.setAnimated(false);
        paymentChart.getXAxis().setLabel("Payment method"); paymentChart.getYAxis().setLabel("Revenue"); paymentChart.setPrefHeight(290);
        VBox chartCard = new VBox(paymentChart); chartCard.getStyleClass().add("content-card"); chartCard.setPadding(new Insets(12)); chartCard.setPrefWidth(420);
        HBox content = new HBox(16,tableCard,chartCard); HBox.setHgrow(tableCard,Priority.ALWAYS); VBox.setVgrow(content,Priority.ALWAYS);

        previous.setOnAction(e -> { if(page>0){page--;load();} });
        next.setOnAction(e -> { if((page+1)*pageSize.getValue()<total){page++;load();} });
        Region pageSpacer = new Region(); HBox.setHgrow(pageSpacer,Priority.ALWAYS);
        HBox paging = new HBox(10,matching,pageSpacer,previous,pageLabel,next); paging.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().addAll(heading,cards,filters,content,paging); VBox.setVgrow(content,Priority.ALWAYS);
        return root;
    }

    private static MetricCard metric(String title) { MetricCard card = new MetricCard(title); card.setValue("—"); return card; }

    private void loadLookups() {
        Task<Lookups> task = new Task<>() { @Override protected Lookups call() throws Exception { return new Lookups(dao.branches(),dao.cashiers(),dao.customers()); } };
        task.setOnSucceeded(e -> {
            setOptions(branch,"All branches",task.getValue().branches());
            setOptions(cashier,"All cashiers",task.getValue().cashiers());
            setOptions(customer,"All customers",task.getValue().customers());
            if (AppSession.current().branchId()!=null&&!AppSession.hasPermission("VIEW_ALL_BRANCHES")) branch.setDisable(true);
        });
        task.setOnFailed(e -> AlertUtil.error("Filters unavailable","Sales lookup values could not be loaded."));
        start(task,"sales-lookups");
    }
    private static void setOptions(ComboBox<QueryDAO.Option> box,String all,List<QueryDAO.Option> values){box.getItems().setAll(new QueryDAO.Option(null,all));box.getItems().addAll(values);box.getSelectionModel().selectFirst();}

    private void apply(){page=0;load();}
    private void clear(){search.clear();from.setValue(LocalDate.now().withDayOfMonth(1));to.setValue(LocalDate.now());if(!branch.getItems().isEmpty())branch.getSelectionModel().selectFirst();if(!cashier.getItems().isEmpty())cashier.getSelectionModel().selectFirst();if(!customer.getItems().isEmpty())customer.getSelectionModel().selectFirst();payment.setValue("All payments");status.setValue("All statuses");discount.setValue("Any discount");minimum.clear();maximum.clear();page=0;load();}

    private void load() {
        SalesDAO.SalesFilter filter;
        try { filter=filter(); } catch (IllegalArgumentException exception) { AlertUtil.warning("Invalid filters",exception.getMessage()); return; }
        loading.show("Loading transactions and financial totals…");
        Task<PageData> task = new Task<>() { @Override protected PageData call() throws Exception { return new PageData(dao.search(filter,pageSize.getValue(),page*pageSize.getValue()),dao.summary(filter),dao.paymentSummary(filter)); } };
        task.setOnSucceeded(e -> { render(task.getValue()); loading.hide(); });
        task.setOnFailed(e -> { loading.hide(); AlertUtil.error("Sales unavailable","The filtered sales query could not be completed."); });
        start(task,"sales-load");
    }

    private SalesDAO.SalesFilter filter() {
        if(from.getValue()!=null&&to.getValue()!=null&&from.getValue().isAfter(to.getValue()))throw new IllegalArgumentException("Start date cannot be after end date.");
        BigDecimal min=money(minimum.getText()),max=money(maximum.getText()); if(min!=null&&max!=null&&min.compareTo(max)>0)throw new IllegalArgumentException("Minimum total cannot exceed maximum total.");
        Boolean discounted=switch(discount.getValue()){case "With discount"->true;case "Without discount"->false;default->null;};
        return new SalesDAO.SalesFilter(search.getText(),from.getValue(),to.getValue(),id(branch),id(cashier),id(customer),all(payment,"All payments"),all(status,"All statuses"),min,max,discounted);
    }
    private static Integer id(ComboBox<QueryDAO.Option> box){return box.getValue()==null||box.getValue().id()==null?null:((Number)box.getValue().id()).intValue();}
    private static String all(ComboBox<String> box,String all){return box.getValue()==null||box.getValue().equals(all)?null:box.getValue();}
    private static BigDecimal money(String raw){if(raw==null||raw.isBlank())return null;try{BigDecimal value=new BigDecimal(raw.trim());if(value.signum()<0)throw new NumberFormatException();return value;}catch(NumberFormatException e){throw new IllegalArgumentException("Amount filters must be non-negative numbers.");}}

    private void render(PageData data) {
        rows=data.result().rows();columns=data.result().columns();total=data.result().total();table.getColumns().clear();
        for(String column:columns){if(!AppSession.hasPermission("VIEW_PRODUCT_COST")&&Set.of("COGS","Gross Profit").contains(column))continue;TableColumn<Map<String,Object>,Object> c=new TableColumn<>(column);c.setCellValueFactory(v->new ReadOnlyObjectWrapper<>(v.getValue().get(column)));c.setCellFactory(v->new TableCell<>(){protected void updateItem(Object item,boolean empty){super.updateItem(item,empty);setText(empty||item==null?null:item instanceof BigDecimal b?String.format("%,.2f",b):String.valueOf(item));}});c.setPrefWidth(Math.max(105,Math.min(210,column.length()*12.0)));table.getColumns().add(c);}table.getItems().setAll(rows);
        matching.setText(total+" matching transactions");int pages=Math.max(1,(int)Math.ceil(total/(double)pageSize.getValue()));pageLabel.setText("Page "+(page+1)+" of "+pages);previous.setDisable(page==0);next.setDisable((page+1)*pageSize.getValue()>=total);
        SalesDAO.Summary s=data.summary();revenue.setValue(format(s.revenue()));transactions.setValue(String.format("%,d",s.transactions()));average.setValue(format(s.average()));discounts.setValue(format(s.discounts()));cogs.setValue(format(s.cogs()));grossProfit.setValue(format(s.grossProfit()));cancelled.setValue(format(s.cancelledValue()));
        XYChart.Series<String,Number> series=new XYChart.Series<>();for(SalesDAO.ChartPoint point:data.chart())series.getData().add(new XYChart.Data<>(point.label(),point.value()));paymentChart.getData().clear();paymentChart.getData().add(series);
    }
    private static String format(BigDecimal value){return String.format("%,.2f",value==null?BigDecimal.ZERO:value);}

    private Map<String,Object> selected(){Map<String,Object> row=table.getSelectionModel().getSelectedItem();if(row==null)AlertUtil.warning("Select a sale","Choose a transaction row first.");return row;}
    private long saleId(Map<String,Object> row){return ((Number)row.get("Sale ID")).longValue();}
    private void details(){Map<String,Object> row=selected();if(row!=null)details(row);}
    private void details(Map<String,Object> row){runDetails(saleId(row),false);}
    private void receipt(){Map<String,Object> row=selected();if(row!=null)showReceipt(saleId(row));}

    private void runDetails(long saleId,boolean receiptOnly){loading.show("Loading sale details…");Task<SalesDAO.SaleDetails> task=new Task<>(){@Override protected SalesDAO.SaleDetails call()throws Exception{return dao.details(saleId);}};task.setOnSucceeded(e->{loading.hide();showDetails(task.getValue());});task.setOnFailed(e->{loading.hide();AlertUtil.error("Details unavailable","The sale details could not be loaded.");});start(task,"sale-details");}
    private void showDetails(SalesDAO.SaleDetails details){Dialog<Void> dialog=new Dialog<>();dialog.setTitle("Sale details");dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);GridPane header=new GridPane();header.setHgap(14);header.setVgap(8);int row=0;for(var entry:details.header().entrySet()){Label key=new Label(entry.getKey());key.getStyleClass().add("detail-label");header.addRow(row++,key,new Label(String.valueOf(entry.getValue())));}TableView<Map<String,Object>> items=new TableView<>();if(!details.items().isEmpty())for(String column:details.items().getFirst().keySet()){if(!AppSession.hasPermission("VIEW_PRODUCT_COST")&&column.equals("Cost At Sale"))continue;TableColumn<Map<String,Object>,Object> c=new TableColumn<>(column);c.setCellValueFactory(v->new ReadOnlyObjectWrapper<>(v.getValue().get(column)));c.setPrefWidth(115);items.getColumns().add(c);}items.getItems().setAll(details.items());items.setPrefSize(850,300);VBox content=new VBox(14,header,new Label("Items"),items);content.setPadding(new Insets(12));dialog.getDialogPane().setContent(content);dialog.showAndWait();}

    private void showReceipt(long saleId){loading.show("Preparing receipt…");Task<String> task=new Task<>(){@Override protected String call()throws Exception{return receiptService.render(saleId);}};task.setOnSucceeded(e->{loading.hide();receiptDialog(task.getValue());});task.setOnFailed(e->{loading.hide();AlertUtil.error("Receipt unavailable","The printable receipt could not be generated.");});start(task,"receipt-render");}
    private void receiptDialog(String receipt){ReceiptPreviewDialog.show(this,receipt,"receipt.txt");}

    private void cancel(){Map<String,Object> row=selected();if(row==null)return;if(!"COMPLETED".equals(String.valueOf(row.get("Status")))){AlertUtil.warning("Cannot cancel","Only a completed, non-returned sale can be cancelled.");return;}TextInputDialog reason=new TextInputDialog();reason.setTitle("Cancel sale");reason.setHeaderText("Stock will be restored and the payment will be marked cancelled.");reason.setContentText("Reason:");reason.showAndWait().filter(v->!v.isBlank()).ifPresent(value->{if(!AlertUtil.confirm("Confirm cancellation","Cancel receipt "+row.get("Receipt")+"? This action is audited."))return;loading.show("Cancelling sale and restoring stock…");Task<Void> task=new Task<>(){@Override protected Void call()throws Exception{saleService.cancelSale(saleId(row),value.trim());return null;}};task.setOnSucceeded(e->{loading.hide();AlertUtil.info("Sale cancelled","Stock and transaction status were updated.");load();});task.setOnFailed(e->{loading.hide();AlertUtil.error("Cancellation rolled back",task.getException()==null?"The sale was not changed.":task.getException().getMessage());});start(task,"sale-cancel");});}

    private void export(){SalesDAO.SalesFilter filter;try{filter=filter();}catch(Exception e){AlertUtil.warning("Invalid filters",e.getMessage());return;}Path path=ExportUtil.chooseCsv(getScene().getWindow(),"sales-filtered.csv");if(path==null)return;loading.show("Exporting filtered sales…");Task<QueryDAO.QueryResult> task=new Task<>(){@Override protected QueryDAO.QueryResult call()throws Exception{return dao.search(filter,100000,0);}};task.setOnSucceeded(e->{loading.hide();try{List<String> exportColumns=new ArrayList<>(task.getValue().columns());if(!AppSession.hasPermission("VIEW_PRODUCT_COST"))exportColumns.removeIf(Set.of("COGS","Gross Profit")::contains);ExportUtil.writeCsv(path,exportColumns,task.getValue().rows());AlertUtil.info("Export complete",path.toString());}catch(Exception ex){AlertUtil.error("Export failed","Could not create the CSV file.");}});task.setOnFailed(e->{loading.hide();AlertUtil.error("Export failed","The filtered dataset could not be loaded.");});start(task,"sales-export");}
    private static void start(Task<?> task,String name){Thread thread=new Thread(task,name);thread.setDaemon(true);thread.start();}

    private record Lookups(List<QueryDAO.Option> branches,List<QueryDAO.Option> cashiers,List<QueryDAO.Option> customers){}
    private record PageData(QueryDAO.QueryResult result,SalesDAO.Summary summary,List<SalesDAO.ChartPoint> chart){}
}
