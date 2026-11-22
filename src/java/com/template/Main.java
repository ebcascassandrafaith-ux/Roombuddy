package com.template;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        loadFonts();

        URL fxml = Main.class.getResource("/com/template/main.fxml");
        if (fxml == null) {
            throw new IllegalStateException("main.fxml not found. Is src/resources marked as Resources Root?");
        }
        Parent root = new FXMLLoader(fxml).load();

        Scene scene = new Scene(root, 1180, 780);
        scene.getStylesheets().add(Main.class.getResource("/com/template/style.css").toExternalForm());

        stage.setTitle("RoomBuddy");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    /** Optional: drop Poppins .ttf files into resources/com/template/fonts/ for the Figma font. */
    private void loadFonts() {
        String[] files = {"Poppins-Regular", "Poppins-Medium", "Poppins-SemiBold", "Poppins-Bold"};
        for (String name : files) {
            URL url = Main.class.getResource("/com/template/fonts/" + name + ".ttf");
            if (url != null) Font.loadFont(url.toExternalForm(), 12);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}