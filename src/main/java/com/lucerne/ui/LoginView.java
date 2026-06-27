package com.lucerne.ui;

import com.lucerne.config.ApplicationConfig;
import com.lucerne.config.DatabaseConnection;
import com.lucerne.service.AuthenticationService;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public final class LoginView {
    private final AuthenticationService authenticationService;
    private final Consumer<AuthenticationService.LoginResult> onSuccess;
    private final Preferences preferences=Preferences.userNodeForPackage(LoginView.class);
    public LoginView(AuthenticationService service,Consumer<AuthenticationService.LoginResult> onSuccess){this.authenticationService=service;this.onSuccess=onSuccess;}
    public Parent create(){
        BorderPane root=new BorderPane();root.getStyleClass().add("login-root");VBox brand=new VBox(18);brand.getStyleClass().add("login-brand-panel");brand.setPadding(new Insets(64));brand.setAlignment(Pos.CENTER_LEFT);Label logo=new Label("L");logo.getStyleClass().add("login-logo");Label name=new Label(ApplicationConfig.APP_NAME);name.getStyleClass().add("login-brand-title");Label slogan=new Label("Professional boutique operations, inventory, sales and analytics in one secure desktop system.");slogan.getStyleClass().add("login-brand-copy");slogan.setWrapText(true);brand.getChildren().addAll(logo,name,slogan);root.setLeft(brand);
        VBox form=new VBox(14);form.getStyleClass().add("login-form");form.setPadding(new Insets(60));form.setMaxWidth(470);Label welcome=new Label("Welcome back");welcome.getStyleClass().add("login-title");Label helper=new Label("Use your assigned account. Your role is detected automatically.");helper.getStyleClass().add("page-description");helper.setWrapText(true);TextField username=new TextField(preferences.get("rememberedUsername",""));username.setPromptText("Username");PasswordField password=new PasswordField();password.setPromptText("Password");TextField visiblePassword=new TextField();visiblePassword.setPromptText("Password");visiblePassword.setVisible(false);visiblePassword.setManaged(false);visiblePassword.textProperty().bindBidirectional(password.textProperty());CheckBox show=new CheckBox("Show password");show.selectedProperty().addListener((o,a,b)->{password.setVisible(!b);password.setManaged(!b);visiblePassword.setVisible(b);visiblePassword.setManaged(b);if(b)visiblePassword.requestFocus();else password.requestFocus();});CheckBox remember=new CheckBox("Remember username on this computer");remember.setSelected(!username.getText().isBlank());Label message=new Label();message.getStyleClass().add("login-message");message.setWrapText(true);ProgressIndicator progress=new ProgressIndicator();progress.setMaxSize(24,24);progress.setVisible(false);Button login=new Button("Sign in securely");login.getStyleClass().add("primary-button");login.setMaxWidth(Double.MAX_VALUE);Label connection=new Label("Checking MySQL connection…");connection.getStyleClass().add("secondary-text");HBox action=new HBox(10,login,progress);action.setAlignment(Pos.CENTER_LEFT);HBox.setHgrow(login,Priority.ALWAYS);form.getChildren().addAll(welcome,helper,new Label("Username"),username,new Label("Password"),password,visiblePassword,show,remember,message,action,new Separator(),connection);StackPane formWrap=new StackPane(form);formWrap.setPadding(new Insets(30));root.setCenter(formWrap);
        Runnable submit=()->{message.setText("");login.setDisable(true);progress.setVisible(true);String user=username.getText();String pass=password.getText();Task<AuthenticationService.LoginResult> task=new Task<>(){protected AuthenticationService.LoginResult call(){return authenticationService.login(user,pass);}};task.setOnSucceeded(e->{login.setDisable(false);progress.setVisible(false);var result=task.getValue();if(result.successful()){if(remember.isSelected())preferences.put("rememberedUsername",user);else preferences.remove("rememberedUsername");onSuccess.accept(result);}else{message.setText(result.message());password.clear();}});task.setOnFailed(e->{login.setDisable(false);progress.setVisible(false);message.setText("Login failed unexpectedly. Check application logs and MySQL.");});Thread t=new Thread(task,"login");t.setDaemon(true);t.start();};login.setOnAction(e->submit.run());password.setOnAction(e->submit.run());visiblePassword.setOnAction(e->submit.run());username.setOnAction(e->password.requestFocus());
        Task<DatabaseConnection.ConnectionStatus> check=new Task<>(){protected DatabaseConnection.ConnectionStatus call(){return DatabaseConnection.test();}};check.setOnSucceeded(e->{var result=check.getValue();connection.setText(result.connected()?"MySQL connected · "+result.responseMillis()+" ms":"MySQL unavailable · "+result.message());connection.getStyleClass().add(result.connected()?"connection-ok":"connection-error");});Thread ct=new Thread(check,"connection-check");ct.setDaemon(true);ct.start();return root;
    }
}
