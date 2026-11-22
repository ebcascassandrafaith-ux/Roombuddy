package com.template.browse;

import com.template.dao.SavedListingDAO;
import com.template.model.Listing;
import com.template.util.Async;
import com.template.util.FxView;
import com.template.util.Session;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.List;

/**
 * The saved / bookmarked listings of the logged-in user.
 * Flowchart: Found saved listings? Yes: list them.  No: "You haven't saved any listings yet."
 * Click a card to open its details. The bookmark button on a card removes it from the list.
 */
public class SavedListingsController {

    private static final String CARD_FXML = "/com/template/browse/listing_card.fxml";
    private static final double CARD_SIZE = 250;
    private static final String EMPTY_TEXT = "You haven't saved any listings yet.";

    @FXML private ScrollPane pageScroll;
    @FXML private Label summaryLabel;
    @FXML private Label statusLabel;
    @FXML private FlowPane grid;

    private final SavedListingDAO savedDAO = new SavedListingDAO();
    private Navigator navigator;
    private int loadCounter = 0;   // lets us ignore an old, slower load that finishes late

    /** Called by MainController every time the Saved screen is opened. */
    public void show(Navigator navigator) {
        this.navigator = navigator;
        reload();
    }

    /** Asks the database again. MainController also calls this when you come back from a details page. */
    public void reload() {
        if (!Session.isLoggedIn()) {
            grid.getChildren().clear();
            summaryLabel.setText("");
            showStatus("Log in to see your saved listings.");
            return;
        }
        final int userId = Session.getUserId();
        final int ticket = ++loadCounter;
        if (grid.getChildren().isEmpty()) showStatus("Loading saved listings...");

        Async.run(() -> savedDAO.findSavedListings(userId),
                list -> {
                    if (ticket == loadCounter) showList(list);
                },
                err -> {
                    if (ticket == loadCounter) {
                        showStatus("Could not load saved listings. Check your database connection.\n("
                                + err.getMessage() + ")");
                    }
                });
    }

    private void showList(List<Listing> listings) {
        grid.getChildren().clear();
        if (listings.isEmpty()) {
            showEmpty();
            return;
        }
        hideStatus();
        for (Listing l : listings) {
            grid.getChildren().add(buildCell(l));
        }
        updateSummary();
    }

    /** One card with a small bookmark button in its top-right corner. */
    private StackPane buildCell(Listing listing) {
        FxView<ListingCardController> card = FxView.load(CARD_FXML);
        card.getController().setData(listing, navigator, CARD_SIZE);

        Region icon = new Region();
        icon.getStyleClass().addAll("icon", "icon-bookmark-filled");
        Button remove = new Button();
        remove.getStyleClass().add("circle-button");
        remove.setGraphic(icon);
        remove.setTooltip(new Tooltip("Remove from saved"));

        StackPane cell = new StackPane(card.getRoot(), remove);
        StackPane.setAlignment(card.getRoot(), Pos.TOP_LEFT);
        StackPane.setAlignment(remove, Pos.TOP_RIGHT);
        StackPane.setMargin(remove, new Insets(10, 10, 0, 0));

        remove.setOnAction(e -> removeFromSaved(listing, cell, remove));
        return cell;
    }

    private void removeFromSaved(Listing listing, StackPane cell, Button button) {
        final int userId = Session.getUserId();
        button.setDisable(true);
        Async.run(() -> {
                    savedDAO.unsave(userId, listing.getListingId());
                    return Boolean.TRUE;
                },
                done -> {
                    grid.getChildren().remove(cell);
                    if (grid.getChildren().isEmpty()) showEmpty();
                    else updateSummary();
                },
                err -> button.setDisable(false));
    }

    private void showEmpty() {
        summaryLabel.setText("");
        showStatus(EMPTY_TEXT);
    }

    private void updateSummary() {
        int n = grid.getChildren().size();
        summaryLabel.setText(n + (n == 1 ? " saved listing" : " saved listings"));
    }

    @FXML
    private void onBack() {
        if (navigator != null) navigator.goBack();
    }

    private void showStatus(String text) {
        statusLabel.setText(text);
        setShown(statusLabel, true);
    }

    private void hideStatus() {
        setShown(statusLabel, false);
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }
}
