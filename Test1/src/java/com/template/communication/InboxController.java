package com.template.communication;

import com.template.browse.Navigator;
import com.template.dao.CommunicationDAO;
import com.template.model.Conversation;
import com.template.util.Async;
import com.template.util.Session;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * "Messages": every chat the logged-in user is in (as owner or as renter), newest first.
 * Click one to open it. Same chat screen the renter uses.
 */
public class InboxController {

    @FXML private VBox conversationsBox;

    private final CommunicationDAO dao = new CommunicationDAO();
    private Navigator navigator;

    public void show(Navigator navigator) {
        this.navigator = navigator;
        final int userId = Session.getUserId();
        Async.run(() -> dao.findConversations(userId), this::render, this::showError);
    }

    @FXML
    private void onBack() {
        if (navigator != null) navigator.goBack();
    }

    private void render(List<Conversation> conversations) {
        conversationsBox.getChildren().clear();
        if (conversations.isEmpty()) {
            Label empty = new Label("No messages yet.");
            empty.getStyleClass().add("muted-text");
            conversationsBox.getChildren().add(empty);
            return;
        }

        for (Conversation c : conversations) {
            VBox card = new VBox(4);
            card.getStyleClass().addAll("notification-card",
                    c.getUnreadCount() > 0 ? "notification-unread" : "notification-read");
            card.setCursor(Cursor.HAND);

            String name = c.getOtherUserName() == null ? "Unknown user" : c.getOtherUserName();
            Label title = new Label(c.getUnreadCount() > 0 ? name + "  (" + c.getUnreadCount() + " new)" : name);
            title.getStyleClass().add("notification-message");

            Label about = new Label(c.getListingName() == null ? "Listing no longer available" : c.getListingName());
            about.getStyleClass().add("muted-text");

            String last = c.getLastMessage() == null ? "" : c.getLastMessage().replace('\n', ' ');
            if (last.length() > 90) last = last.substring(0, 90) + "...";
            Label preview = new Label(last);
            preview.setWrapText(true);

            Label when = new Label(c.getLastSentAt() == null ? ""
                    : c.getLastSentAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a")));
            when.getStyleClass().add("notification-meta");

            card.getChildren().addAll(title, about, preview, when);
            final int id = c.getConversationId();
            card.setOnMouseClicked(e -> {
                if (navigator != null) navigator.openConversation(id);
            });
            conversationsBox.getChildren().add(card);
        }
    }

    private void showError(Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("RoomBuddy");
        alert.setHeaderText("Messages");
        alert.setContentText(error.getMessage());
        alert.show();
    }
}
