package com.template.admin.util;

import com.template.util.Images;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Optional;

/** Small helpers shared by all admin controllers: pop-ups and the image preview. */
public final class AdminUi {

    private AdminUi() {}

    public static void info(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message);
        a.setHeaderText(null);
        a.showAndWait();
    }

    public static void error(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message);
        a.setHeaderText(null);
        a.showAndWait();
    }

    /** Yes/No question. Returns true only if the admin clicks Yes. */
    public static boolean confirm(String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        a.setTitle("Please confirm");
        a.setHeaderText(null);
        Optional<ButtonType> result = a.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    /** Asks for a reason. Returns null if cancelled or left empty. */
    public static String askReason(String header) {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Reason required");
        d.setHeaderText(header);
        d.setContentText("Reason:");
        Optional<String> result = d.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String text = result.get().trim();
            if (text.length() > 200) {
                text = text.substring(0, 200);   // database columns hold at most 255 characters
            }
            return text;
        }
        return null;
    }

    /** Shows the picture if it can be loaded (uses the project's Images helper), otherwise the path as text. */
    public static Node imagePreview(String path) {
        Image img = Images.load(path, 300, 200);
        if (img != null) {
            ImageView view = new ImageView(img);
            view.setFitWidth(300);
            view.setPreserveRatio(true);
            return view;
        }
        Label label = new Label("Image not found:\n" + path);
        label.setWrapText(true);
        label.setMaxWidth(300);
        return label;
    }
}
