package com.template.admin;

import javafx.application.Application;
import javafx.stage.Stage;

/** TEST ONLY: opens the admin login window by itself. The real app does not use this class. */
public class AdminApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        AdminWindow.openLogin();   // AdminWindow makes its own window, so primaryStage stays unused
    }
}
