package com.lucerne.ui;

import com.lucerne.app.AppSession;
import com.lucerne.model.UserAccount;
import com.lucerne.service.PasswordService;
import com.lucerne.ui.components.LoadingPane;
import com.lucerne.util.AlertUtil;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** Safe self-service profile page. It never exposes system configuration to ordinary users. */
public final class ProfileView extends StackPane {
    private final LoadingPane loading = new LoadingPane();

    public ProfileView() {
        getChildren().addAll(build(), loading);
    }

    private VBox build() {
        UserAccount user = AppSession.current();
        VBox root = new VBox(18);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("page-root");

        Label title = new Label("My Profile");
        title.getStyleClass().add("page-title");
        Label description = new Label("Review your assigned account and securely change your password.");
        description.getStyleClass().add("page-description");

        GridPane identity = new GridPane();
        identity.setHgap(18);
        identity.setVgap(12);
        identity.setPadding(new Insets(18));
        addIdentity(identity, 0, "Full name", user.fullName());
        addIdentity(identity, 1, "Username", user.username());
        addIdentity(identity, 2, "Role", user.role().name());
        addIdentity(identity, 3, "Employee ID", user.employeeId());
        addIdentity(identity, 4, "Customer ID", user.customerId());
        addIdentity(identity, 5, "Branch ID", user.branchId());
        addIdentity(identity, 6, "Warehouse ID", user.warehouseId());
        addIdentity(identity, 7, "Login time", user.loginTime());
        VBox identityCard = new VBox(10, new Label("Account assignment"), identity);
        identityCard.getStyleClass().add("content-card");
        identityCard.setPadding(new Insets(14));

        PasswordField current = new PasswordField();
        PasswordField replacement = new PasswordField();
        PasswordField confirmation = new PasswordField();
        current.setPromptText("Current password");
        replacement.setPromptText("At least 8 characters");
        confirmation.setPromptText("Repeat new password");
        GridPane passwordGrid = new GridPane();
        passwordGrid.setHgap(18);
        passwordGrid.setVgap(12);
        passwordGrid.setPadding(new Insets(18));
        passwordGrid.addRow(0, new Label("Current password"), current);
        passwordGrid.addRow(1, new Label("New password"), replacement);
        passwordGrid.addRow(2, new Label("Confirm password"), confirmation);
        Button change = new Button("Change password");
        change.getStyleClass().add("primary-button");
        change.setOnAction(event -> changePassword(current, replacement, confirmation));
        VBox passwordCard = new VBox(10, new Label("Security"), passwordGrid, change);
        passwordCard.getStyleClass().add("content-card");
        passwordCard.setPadding(new Insets(14));

        root.getChildren().addAll(title, description, identityCard, passwordCard);
        return root;
    }

    private static void addIdentity(GridPane grid, int row, String label, Object value) {
        Label key = new Label(label);
        key.getStyleClass().add("detail-label");
        grid.addRow(row, key, new Label(value == null ? "Not assigned" : String.valueOf(value)));
    }

    private void changePassword(PasswordField current, PasswordField replacement, PasswordField confirmation) {
        if (current.getText().isBlank() || replacement.getText().isBlank()) {
            AlertUtil.warning("Missing password", "Enter your current and new passwords.");
            return;
        }
        if (!replacement.getText().equals(confirmation.getText())) {
            AlertUtil.warning("Passwords do not match", "Repeat the same new password in both fields.");
            return;
        }
        loading.show("Changing password…");
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                new PasswordService().changeWithCurrent(AppSession.current().userId(), current.getText(), replacement.getText());
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            loading.hide();
            current.clear(); replacement.clear(); confirmation.clear();
            AlertUtil.info("Password changed", "Your password was updated securely.");
        });
        task.setOnFailed(event -> {
            loading.hide();
            String message = task.getException() == null ? "The password could not be changed." : task.getException().getMessage();
            AlertUtil.error("Password not changed", message);
        });
        Thread thread = new Thread(task, "profile-password-change");
        thread.setDaemon(true);
        thread.start();
    }
}
