package com.lucerne.app;

import javafx.application.Application;
import javafx.stage.Stage;
import com.lucerne.util.LoggerUtil;

public final class Main extends Application {
    @Override public void init(){LoggerUtil.configure();LoggerUtil.info(Main.class,"Application starting");}
    @Override public void start(Stage stage){new AppLauncher(stage).showLogin();}
    @Override public void stop(){LoggerUtil.info(Main.class,"Application stopped");}
    public static void main(String[] args){launch(args);}
}
