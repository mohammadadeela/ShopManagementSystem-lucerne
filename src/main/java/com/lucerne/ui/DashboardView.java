package com.lucerne.ui;

import com.lucerne.app.AppSession;
import com.lucerne.dao.DashboardDAO;
import com.lucerne.dao.QueryDAO;
import com.lucerne.ui.components.LoadingPane;
import com.lucerne.ui.components.MetricCard;
import com.lucerne.util.AlertUtil;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class DashboardView extends StackPane {
    private final DashboardDAO dao=new DashboardDAO();
    private final DatePicker fromDate=new DatePicker(LocalDate.now().withDayOfMonth(1));
    private final DatePicker toDate=new DatePicker(LocalDate.now());
    private final ComboBox<QueryDAO.Option> branch=new ComboBox<>();
    private final FlowPane metrics=new FlowPane(14,14);
    private final LineChart<String,Number> trend=new LineChart<>(new CategoryAxis(),new NumberAxis());
    private final BarChart<String,Number> ranking=new BarChart<>(new CategoryAxis(),new NumberAxis());
    private final PieChart distribution=new PieChart();
    private final TableView<Map<String,Object>> recent=new TableView<>();
    private final LoadingPane loading=new LoadingPane();
    private final Label lastRefresh=new Label("Not refreshed yet");

    public DashboardView(){
        getChildren().addAll(build(),loading);loadBranches();load();
    }
    private Node build(){
        VBox root=new VBox(18);root.getStyleClass().add("page-root");root.setPadding(new Insets(24));
        Label title=new Label("Dashboard");title.getStyleClass().add("page-title");
        Label description=new Label("Live performance, financial health, operational warnings and recent activity.");description.getStyleClass().add("page-description");
        Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);
        Button refresh=new Button("Refresh dashboard");refresh.getStyleClass().add("primary-button");refresh.setOnAction(e->load());
        HBox heading=new HBox(10,new VBox(4,title,description),spacer,lastRefresh,refresh);heading.setAlignment(Pos.CENTER_LEFT);
        FlowPane filters=new FlowPane(10,10,new Label("From"),fromDate,new Label("To"),toDate,new Label("Branch"),branch);filters.getStyleClass().add("filter-panel");filters.setPadding(new Insets(12));
        fromDate.setOnAction(e->load());toDate.setOnAction(e->load());branch.setOnAction(e->load());
        metrics.setPrefWrapLength(1050);
        configureCharts();
        HBox charts=new HBox(16,card(trend,"Revenue trend"),card(ranking,"Top products"),card(distribution,"Payment / status mix"));
        for(Node node:charts.getChildren())HBox.setHgrow(node,Priority.ALWAYS);
        VBox tableCard=new VBox(10);tableCard.getStyleClass().add("content-card");tableCard.setPadding(new Insets(14));Label recentTitle=new Label("Recent activity");recentTitle.getStyleClass().add("section-title");recent.setPlaceholder(new Label("No recent activity for this period."));VBox.setVgrow(recent,Priority.ALWAYS);tableCard.getChildren().addAll(recentTitle,recent);
        root.getChildren().addAll(heading,filters,metrics,charts,tableCard);VBox.setVgrow(tableCard,Priority.ALWAYS);return root;
    }
    private VBox card(Node chart,String title){VBox box=new VBox(8);box.getStyleClass().add("content-card");box.setPadding(new Insets(12));Label label=new Label(title);label.getStyleClass().add("section-title");box.getChildren().addAll(label,chart);VBox.setVgrow(chart,Priority.ALWAYS);box.setMinWidth(280);box.setPrefHeight(330);return box;}
    private void configureCharts(){trend.setLegendVisible(false);trend.setAnimated(false);trend.getXAxis().setLabel("Date");trend.getYAxis().setLabel("Value");ranking.setLegendVisible(false);ranking.setAnimated(false);ranking.getYAxis().setLabel("Value");distribution.setLegendSide(javafx.geometry.Side.BOTTOM);distribution.setLabelsVisible(true);}
    private void loadBranches(){branch.getItems().add(new QueryDAO.Option(null,"All branches"));branch.getSelectionModel().selectFirst();if(!AppSession.hasPermission("VIEW_ALL_BRANCHES")) {branch.setDisable(true);return;}Task<List<QueryDAO.Option>> task=new Task<>(){protected List<QueryDAO.Option> call()throws Exception{return new QueryDAO().options("SELECT BranchID,Name FROM branches WHERE IsActive=1 ORDER BY Name",List.of());}};task.setOnSucceeded(e->branch.getItems().addAll(task.getValue()));Thread t=new Thread(task,"dashboard-branches");t.setDaemon(true);t.start();}
    private void load(){if(fromDate.getValue()!=null&&toDate.getValue()!=null&&fromDate.getValue().isAfter(toDate.getValue())){AlertUtil.warning("Invalid period","The start date cannot be after the end date.");return;}Integer branchId=branch.getValue()==null||branch.getValue().id()==null?null:((Number)branch.getValue().id()).intValue();loading.show("Calculating dashboard metrics…");Task<DashboardDAO.DashboardData> task=new Task<>(){protected DashboardDAO.DashboardData call()throws Exception{return dao.load(AppSession.current(),fromDate.getValue(),toDate.getValue(),branchId);}};task.setOnSucceeded(e->{render(task.getValue());loading.hide();lastRefresh.setText("Updated "+java.time.LocalTime.now().withSecond(0).withNano(0));});task.setOnFailed(e->{loading.hide();AlertUtil.error("Dashboard unavailable","The dashboard could not query MySQL. Import lucerne_demo_final.sql and verify database.properties.");});Thread t=new Thread(task,"dashboard-load");t.setDaemon(true);t.start();}
    private void render(DashboardDAO.DashboardData data){metrics.getChildren().clear();for(DashboardDAO.Metric item:data.metrics()){MetricCard card=new MetricCard(item.title());card.setValue(format(item.value()));card.setSubtitle(item.subtitle());card.setPrefWidth(220);metrics.getChildren().add(card);}trend.getData().clear();XYChart.Series<String,Number> series=new XYChart.Series<>();for(var p:data.trend())series.getData().add(new XYChart.Data<>(p.label(),p.value()));trend.getData().add(series);ranking.getData().clear();XYChart.Series<String,Number> bars=new XYChart.Series<>();for(var p:data.ranking())bars.getData().add(new XYChart.Data<>(p.label(),p.value()));ranking.getData().add(bars);distribution.getData().clear();for(var p:data.distribution())distribution.getData().add(new PieChart.Data(p.label(),p.value().doubleValue()));renderTable(data.recent());}
    private void renderTable(List<Map<String,Object>> rows){recent.getColumns().clear();if(rows.isEmpty()){recent.getItems().clear();return;}for(String column:rows.getFirst().keySet()){TableColumn<Map<String,Object>,Object> c=new TableColumn<>(column);c.setCellValueFactory(v->new ReadOnlyObjectWrapper<>(v.getValue().get(column)));c.setPrefWidth(140);recent.getColumns().add(c);}recent.getItems().setAll(rows);}
    private String format(BigDecimal value){return String.format("%,.2f",value);}
}
