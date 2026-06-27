package com.lucerne.app;

import com.lucerne.config.ApplicationConfig;
import com.lucerne.service.AuthenticationService;
import com.lucerne.service.PasswordService;
import com.lucerne.ui.LoginView;
import com.lucerne.ui.MainLayout;
import com.lucerne.util.AlertUtil;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import java.util.Objects;

public final class AppLauncher {
    private final Stage stage;private final AuthenticationService authenticationService=new AuthenticationService();
    public AppLauncher(Stage stage){this.stage=stage;}
    public void showLogin(){Scene scene=new Scene(new LoginView(authenticationService,this::authenticated).create(),1280,780);applyCss(scene);stage.setScene(scene);stage.setTitle(ApplicationConfig.APP_NAME+" · Sign in");stage.setMinWidth(980);stage.setMinHeight(650);stage.centerOnScreen();stage.show();}
    private void authenticated(AuthenticationService.LoginResult result){AppSession.start(result.user(),result.sessionId());if(result.user().passwordChangeRequired()&&!forcePasswordChange()){authenticationService.logout(result.user());AppSession.clear();return;}MainLayout layout=new MainLayout(authenticationService,this::showLogin);Scene scene=new Scene(layout,1440,900);applyCss(scene);stage.setScene(scene);stage.setTitle(ApplicationConfig.APP_NAME+" · "+result.user().role());stage.setMinWidth(ApplicationConfig.MIN_WIDTH);stage.setMinHeight(ApplicationConfig.MIN_HEIGHT);stage.centerOnScreen();}
    private boolean forcePasswordChange(){Dialog<String> d=new Dialog<>();d.setTitle("Password change required");d.setHeaderText("Create a new password before continuing.");d.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL,ButtonType.OK);PasswordField p1=new PasswordField(),p2=new PasswordField();p1.setPromptText("At least 8 characters");p2.setPromptText("Repeat password");GridPane g=new GridPane();g.setHgap(12);g.setVgap(10);g.addRow(0,new Label("New password"),p1);g.addRow(1,new Label("Confirm"),p2);d.getDialogPane().setContent(g);d.setResultConverter(b->b==ButtonType.OK?p1.getText():null);while(true){var result=d.showAndWait();if(result.isEmpty())return false;if(!p1.getText().equals(p2.getText())||p1.getText().length()<8){AlertUtil.warning("Invalid password","Passwords must match and contain at least 8 characters.");continue;}try{new PasswordService().change(AppSession.current().userId(),p1.getText());return true;}catch(Exception e){AlertUtil.error("Password not changed","The database could not save the new password.");return false;}}}
    private void applyCss(Scene scene){scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm());}
}
