package com.template.communication;

import com.template.browse.Navigator;
import com.template.dao.CommunicationDAO;
import com.template.model.Notification;
import com.template.util.Async;
import com.template.util.Session;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
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

            // Clicking a notification opens the screen it is about (chat, viewing schedule, my listings).
            if (target(notification) != null) {
                Label open = new Label("Click to open");
                open.getStyleClass().add("notification-meta");
                card.getChildren().add(open);
                card.setCursor(Cursor.HAND);
                card.setOnMouseClicked(e -> onOpen(notification));
            }
            notificationsBox.getChildren().add(card);
        }
    }

    /** Which screen a notification leads to, or null when there is nothing to open. */
    private static String target(Notification n) {
        if (n.getType() == null) return null;
        switch (n.getType()) {
            case "new_message":      return "messages";
            case "viewing_request":
            case "viewing_update":   return "viewings";
            case "listing_approved":
            case "listing_rejected": return "listings";
            default:                 return null;     // id_approved / id_rejected: nothing to open yet
        }
    }

    /** Marks the notification as read, then goes to the chat / schedule / listings it is about. */
    private void onOpen(Notification n) {
        final int userId = Session.getUserId();
        Async.run(() -> {
            if (!n.isRead()) dao.markNotificationRead(n.getNotificationId(), userId);
            return true;
        }, ok -> goTo(n), err -> goTo(n));
    }

    private void goTo(Notification n) {
        if (navigator == null) return;
        String target = target(n);
        if (target == null) return;
        switch (target) {
            case "messages":
                // related_id is the conversation id; without it just show the inbox
                if (n.getRelatedId() != null) navigator.openConversation(n.getRelatedId());
                else navigator.showMessages();
                break;
            case "viewings":
                navigator.showViewings();
                break;
            default:
                navigator.showMyListings();
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