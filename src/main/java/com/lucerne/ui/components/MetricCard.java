package com.lucerne.ui.components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class MetricCard extends VBox {
    private final Label value = new Label("—");
    private final Label subtitle = new Label();
    public MetricCard(String title) {
        getStyleClass().add("metric-card"); setPadding(new Insets(16)); setSpacing(5);
        Label heading = new Label(title); heading.getStyleClass().add("metric-title");
        value.getStyleClass().add("metric-value"); subtitle.getStyleClass().add("metric-subtitle");
        getChildren().addAll(heading, value, subtitle);
    }
    public void setValue(String text) { value.setText(text); }
    public void setSubtitle(String text) { subtitle.setText(text == null ? "" : text); }
}
