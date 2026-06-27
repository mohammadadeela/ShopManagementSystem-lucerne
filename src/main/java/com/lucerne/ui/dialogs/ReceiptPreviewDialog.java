package com.lucerne.ui.dialogs;

import com.lucerne.util.AlertUtil;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class ReceiptPreviewDialog {
    private ReceiptPreviewDialog() { }

    public static void show(Node owner, String receipt, String suggestedName) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Receipt preview");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        TextArea area = new TextArea(receipt);
        area.setEditable(false);
        area.setWrapText(false);
        area.setPrefSize(620, 620);
        Button save = new Button("Save printable text");
        save.getStyleClass().add("primary-button");
        save.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save receipt");
            chooser.setInitialFileName(suggestedName == null || suggestedName.isBlank() ? "receipt.txt" : suggestedName);
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text receipt", "*.txt"));
            var file = chooser.showSaveDialog(owner.getScene().getWindow());
            if (file == null) return;
            try {
                Files.writeString(file.toPath(), receipt, StandardCharsets.UTF_8);
                AlertUtil.info("Receipt saved", file.toString());
            } catch (Exception exception) {
                AlertUtil.error("Save failed", "The receipt file could not be written.");
            }
        });
        dialog.getDialogPane().setContent(new VBox(10, area, save));
        dialog.showAndWait();
    }
}
