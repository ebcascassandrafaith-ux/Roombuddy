package com.template.owner;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/** Member B: tiny dialog helpers. */
final class OwnerUi {

    private OwnerUi() {}

    static void info(String message) { show(Alert.AlertType.INFORMATION, message); }

    static void error(String message) { show(Alert.AlertType.ERROR, message); }

    static void error(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null) root = root.getCause();
        error("Something went wrong: " + root.getMessage());
    }

    static boolean confirm(String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        a.setTitle("Please confirm");
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    private static void show(Alert.AlertType type, String message) {
        Alert a = new Alert(type);
        a.setHeaderText(null);
        a.setTitle("RoomBuddy");
        a.setContentText(message);
        a.getDialogPane().setMinWidth(380);
        a.showAndWait();
    }
}
