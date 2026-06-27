package com.lucerne.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import java.util.Optional;

public final class AlertUtil {
    private AlertUtil() { }
    public static void info(String title, String message) { show(Alert.AlertType.INFORMATION, title, message); }
    public static void warning(String title, String message) { show(Alert.AlertType.WARNING, title, message); }
    public static void error(String title, String message) { show(Alert.AlertType.ERROR, title, message); }
    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.CANCEL, ButtonType.OK);
        alert.setTitle(title); alert.setHeaderText(title);
        Optional<ButtonType> answer = alert.showAndWait();
        return answer.isPresent() && answer.get() == ButtonType.OK;
    }
    private static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title); alert.setHeaderText(title); alert.showAndWait();
    }
}
