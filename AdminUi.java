package com.template.util;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.util.Map;
import java.util.Optional;

/** Small pop-ups and previews used by every admin screen, so the controllers stay short. */
public final class AdminUi {

    /** notifications.message and admin_logs.remarks are VARCHAR(255); the text around the reason needs room. */
    public static final int MAX_REASON = 200;

    private AdminUi() {}

    public static void info(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /** @return true if the admin clicked OK */
    public static boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message);
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    /**
     * Asks for a reason (flowchart: "Enter rejection reason", "Confirm action and reason").
     * Keeps asking while the box is empty or too long.
     *
     * @return the reason, or null if the admin cancelled
     */
    public static String askReason(String title) {
        while (true) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Reason required");
            dialog.setHeaderText(title);
            dialog.setContentText("Reason (max " + MAX_REASON + " characters):");
            Optional<String> result = dialog.showAndWait();
            if (result.isEmpty()) {
                return null;                                   // cancelled
            }
            String reason = result.get().trim();
            if (reason.isEmpty()) {
                error("A reason is required.");
            } else if (reason.length() > MAX_REASON) {
                error("The reason is too long (" + reason.length() + " characters). Please keep it under "
                        + MAX_REASON + ".");
            } else {
                return reason;
            }
        }
    }

    /** A picture of an uploaded file, or a short message if the file cannot be found. */
    public static Node imagePreview(String path) {
        VBox box = new VBox(4);
        File file = resolve(path);
        Image image = null;
        if (file.isFile()) {
            Image loaded = new Image(file.toURI().toString(), 640, 420, true, true);
            if (!loaded.isError()) {
                image = loaded;
            }
        }
        if (image == null) {
            Label missing = new Label("Image not found:\n" + path);
            missing.getStyleClass().add("admin-error");
            box.getChildren().add(missing);
        } else {
            ImageView view = new ImageView(image);
            view.setFitWidth(300);
            view.setFitHeight(200);
            view.setPreserveRatio(true);
            box.getChildren().addAll(view, new Label(file.getName()));
        }
        return box;
    }

    /**
     * Shows every photo / document of a listing in a window: pictures are previewed, other files
     * (PDFs) get an "Open file" button that uses the computer's default program.
     *
     * @param files label shown to the admin -> file path stored in the database
     */
    public static void showFiles(String title, Map<String, String> files) {
        VBox list = new VBox(14);
        list.setPadding(new Insets(10));
        if (files.isEmpty()) {
            list.getChildren().add(new Label("This listing has no photos or documents."));
        }
        for (Map.Entry<String, String> entry : files.entrySet()) {
            Label name = new Label(entry.getKey());
            name.setStyle("-fx-font-weight: bold;");
            list.getChildren().add(name);

            File file = resolve(entry.getValue());
            String lower = entry.getValue().toLowerCase();
            boolean isImage = lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
            if (isImage) {
                list.getChildren().add(imagePreview(entry.getValue()));
            } else if (!file.isFile()) {
                Label missing = new Label("File not found: " + entry.getValue());
                missing.getStyleClass().add("admin-error");
                list.getChildren().add(missing);
            }
            if (file.isFile()) {
                Button open = new Button("Open file");
                open.setOnAction(e -> openWithDefaultProgram(file));
                list.getChildren().add(open);
            }
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setPrefSize(520, 480);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private static void openWithDefaultProgram(File file) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                error("This computer cannot open files from the app. File: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            error("Could not open the file: " + file.getAbsolutePath());
        }
    }

    /** Paths in the database are relative to the project folder (for example uploads/ids/x.jpg). */
    private static File resolve(String path) {
        File file = new File(path);
        return file.isAbsolute() ? file : new File(System.getProperty("user.dir"), path);
    }
}
