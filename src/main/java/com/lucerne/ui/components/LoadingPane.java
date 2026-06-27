package com.lucerne.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class LoadingPane extends StackPane {
    private final VBox content = new VBox(10);
    private final Label message = new Label("Loading data…");
    public LoadingPane() {
        getStyleClass().add("loading-overlay"); content.setAlignment(Pos.CENTER);
        ProgressIndicator progress = new ProgressIndicator(); progress.setMaxSize(48,48);
        content.getChildren().addAll(progress,message); getChildren().add(content); setVisible(false); setManaged(false);
    }
    public void show(String text) { message.setText(text); setVisible(true); setManaged(true); }
    public void hide() { setVisible(false); setManaged(false); }
}
