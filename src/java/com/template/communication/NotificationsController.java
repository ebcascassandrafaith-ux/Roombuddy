package com.template.communication;

import com.template.browse.Navigator;
import com.template.dao.CommunicationDAO;
import com.template.model.Notification;
import com.template.util.Async;
import com.template.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class NotificationsController {
    @FXML private VBox notificationsBox;
    @FXML private Button markAllButton;

    private final CommunicationDAO dao = new CommunicationDAO();
    private Navigator navigator;

    public void show(Navigator navigator) {
        this.navigator = navigator;
        reload();
    }

    @FXML
    private void onBack() {
        if (navigator != null) navigator.goBack();
    }

    @FXML
    private void onMarkAllRead() {
        markAllButton.setDisable(true);
        Async.run(() -> {
            dao.markAllNotificationsRead(Session.getUserId());
            return dao.findNotifications(Session.getUserId());
        }, notifications -> {
            markAllButton.setDisable(false);
            render(notifications);
        }, err -> {
            markAllButton.setDisable(false);
            showError(err);
        });
    }

    private void reload() {
        Async.run(() -> dao.findNotifications(Session.getUserId()), this::render, this::showError);
    }

    private void render(List<Notification> notifications) {
        notificationsBox.getChildren().clear();
        if (notifications.isEmpty()) {
            Label empty = new Label("No notifications.");
            empty.getStyleClass().add("muted-text");
            notificationsBox.getChildren().add(empty);
            return;
        }

        for (Notification notification : notifications) {
            VBox card = new VBox(4);
            card.getStyleClass().addAll("notification-card",
                    notification.isRead() ? "notification-read" : "notification-unread");

            Label message = new Label(notification.getMessage());
            message.getStyleClass().add("notification-message");
            Label meta = new Label((notification.isRead() ? "Read" : "Unread")
                    + (notification.getCreatedAt() == null ? ""
                    : " - " + notification.getCreatedAt().format(
                            DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))));
            meta.getStyleClass().add("notification-meta");
            card.getChildren().addAll(message, meta);
            notificationsBox.getChildren().add(card);
        }
    }

    private void showError(Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("RoomBuddy");
        alert.setHeaderText("Notifications");
        alert.setContentText(error.getMessage());
        alert.show();
    }
}
