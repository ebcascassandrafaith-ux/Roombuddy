package com.template.communication;

import com.template.browse.Navigator;
import com.template.dao.CommunicationDAO;
import com.template.model.Conversation;
import com.template.model.Listing;
import com.template.model.Message;
import com.template.util.Async;
import com.template.util.Session;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class ChatController {
    @FXML private Label titleLabel;
    @FXML private Label hostLabel;
    @FXML private VBox messagesBox;
    @FXML private ScrollPane messagesScroll;
    @FXML private TextArea messageField;
    @FXML private Button sendButton;

    private final CommunicationDAO dao = new CommunicationDAO();
    private Navigator navigator;
    private int conversationId;
    private int receiverId;      // who your messages go to (the host for a renter, the renter for a host)
    private int listingId;

    /** Renter flow: "Click to message host" on a listing. */
    public void show(Listing listing, Navigator navigator) {
        this.navigator = navigator;
        this.receiverId = listing.getOwnerId();
        this.listingId = listing.getListingId();
        titleLabel.setText("Chat");
        hostLabel.setText("Host: " + "Listing owner #" + listing.getOwnerId());
        messageField.clear();
        messagesBox.getChildren().clear();

        Async.run(() -> dao.getOrCreateConversation(Session.getUserId(), listing.getOwnerId(), listing.getListingId()),
                id -> {
                    conversationId = id;
                    reload();
                },
                this::showError);
    }

    /** Inbox / notification flow (works for the owner and the renter): open an existing chat. */
    public void showConversation(int conversationId, Navigator navigator) {
        this.navigator = navigator;
        this.conversationId = conversationId;
        titleLabel.setText("Chat");
        hostLabel.setText("");
        messageField.clear();
        messagesBox.getChildren().clear();
        sendButton.setDisable(true);

        Async.run(() -> dao.findConversation(conversationId, Session.getUserId()),
                (Conversation c) -> {
                    if (c == null) {
                        showError(new IllegalStateException("This conversation could not be found."));
                        if (navigator != null) navigator.goBack();
                        return;
                    }
                    receiverId = c.getOtherUserId();
                    listingId = c.getListingId();
                    hostLabel.setText(c.getOtherUserName()
                            + (c.getListingName() == null ? "" : "  \u2022  " + c.getListingName()));
                    sendButton.setDisable(false);
                    reload();
                },
                this::showError);
    }

    @FXML
    private void onBack() {
        if (navigator != null) navigator.goBack();
    }

    @FXML
    private void onSend() {
        String text = messageField.getText();
        if (text == null || text.trim().isEmpty()) return;
        sendButton.setDisable(true);

        Async.run(() -> {
            dao.sendMessage(conversationId, Session.getUserId(), receiverId, listingId, text);
            return dao.findMessages(conversationId);
        }, messages -> {
            messageField.clear();
            sendButton.setDisable(false);
            renderMessages(messages);
        }, err -> {
            sendButton.setDisable(false);
            showError(err);
        });
    }

    private void reload() {
        Async.run(() -> {
            dao.markConversationRead(conversationId, Session.getUserId());
            return dao.findMessages(conversationId);
        }, this::renderMessages, this::showError);
    }

    private void renderMessages(List<Message> messages) {
        messagesBox.getChildren().clear();
        for (Message message : messages) {
            Label bubble = new Label(message.getText());
            bubble.getStyleClass().addAll("chat-bubble",
                    message.getSenderId() == Session.getUserId() ? "chat-bubble-me" : "chat-bubble-host");

            HBox row = new HBox(bubble);
            row.setAlignment(message.getSenderId() == Session.getUserId()
                    ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            row.setMaxWidth(Double.MAX_VALUE);
            messagesBox.getChildren().add(row);
        }
        javafx.application.Platform.runLater(() -> messagesScroll.setVvalue(1.0));
    }

    private void showError(Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("RoomBuddy");
        alert.setHeaderText("Could not complete the chat action");
        alert.setContentText(error.getMessage());
        alert.show();
    }
}