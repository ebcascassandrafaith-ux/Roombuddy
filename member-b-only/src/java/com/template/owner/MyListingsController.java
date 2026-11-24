package com.template.owner;

import com.template.Session;
import com.template.dao.OwnerListingDAO;
import com.template.model.OwnerListing;
import com.template.util.Async;
import com.template.util.FxView;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Member B: "My Listings" screen (my_listings_view.fxml).
 * Shows the logged-in landlord's listings, lets them post a new one, and handles
 * the Edit / Delete / Mark occupied buttons of each card.
 *
 * Load it from the main window like Member C's screens:
 *     FxView<MyListingsController> v = FxView.load("/com/template/owner/my_listings_view.fxml");
 *     contentArea.getChildren().setAll(v.getRoot());
 */
public class MyListingsController implements MyListingCardController.Actions {

    private static final String CARD_FXML = "/com/template/owner/my_listing_card.fxml";

    @FXML private VBox listBox;

    private final OwnerListingDAO dao = new OwnerListingDAO();

    @FXML
    private void initialize() {
        refresh();
    }

    /** Reloads the list from the database. */
    public void refresh() {
        final int userId = Session.getCurrentUserId();
        Async.run(() -> dao.findByOwner(userId), this::render, OwnerUi::error);
    }

    private void render(List<OwnerListing> listings) {
        listBox.getChildren().clear();
        if (listings.isEmpty()) {
            Label empty = new Label("You have not posted any listings yet.\nUse \"Post a listing\" to add your first one.");
            empty.getStyleClass().add("muted");
            listBox.getChildren().add(empty);
            return;
        }
        for (OwnerListing l : listings) {
            FxView<MyListingCardController> card = FxView.load(CARD_FXML);
            card.getController().setListing(l, this);
            listBox.getChildren().add(card.getRoot());
        }
    }

    // ------------------------------------------------------------------ Post a listing

    @FXML
    private void onPostListing() {
        final int userId = Session.getCurrentUserId();
        // Flowchart: "Is ID verified?" -> if not, "Verify ID to continue".
        Async.run(() -> dao.isIdVerified(userId), verified -> {
            if (!verified) {
                OwnerUi.info("Verify your ID to continue.\nUpload your ID from your profile first, then post your listing.");
                return;
            }
            if (ListingFormController.open(listBox.getScene().getWindow(), null)) refresh();
        }, OwnerUi::error);
    }

    // ------------------------------------------------------------------ card actions

    @Override
    public void onEdit(OwnerListing summary) {
        final int userId = Session.getCurrentUserId();
        Async.run(() -> dao.findById(summary.getId(), userId), full -> {
            if (full == null) { refresh(); return; }
            if (ListingFormController.open(listBox.getScene().getWindow(), full)) refresh();
        }, OwnerUi::error);
    }

    @Override
    public void onDelete(OwnerListing l) {
        // Flowchart: confirmation message, then "Listing deleted".
        if (!OwnerUi.confirm("Are you sure you want to delete \"" + l.getPropertyName() + "\"?\nThis cannot be undone.")) return;
        final int userId = Session.getCurrentUserId();
        Async.run(() -> { dao.delete(l.getId(), userId); return true; }, ok -> {
            OwnerUi.info("Listing deleted.");
            refresh();
        }, OwnerUi::error);
    }

    @Override
    public void onToggleOccupied(OwnerListing l) {
        final int userId = Session.getCurrentUserId();
        final boolean newValue = !l.isOccupied();
        Async.run(() -> { dao.setOccupied(l.getId(), userId, newValue); return true; },
                ok -> refresh(), OwnerUi::error);
    }
}
