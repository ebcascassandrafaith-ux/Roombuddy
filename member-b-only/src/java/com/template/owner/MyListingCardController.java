package com.template.owner;

import com.template.model.OwnerListing;
import com.template.util.Images;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/** Member B: one row of the My Listings screen (my_listing_card.fxml). */
public class MyListingCardController {

    /** The screen that owns the cards implements this. */
    public interface Actions {
        void onEdit(OwnerListing listing);
        void onDelete(OwnerListing listing);
        void onToggleOccupied(OwnerListing listing);
    }

    @FXML private ImageView photoView;
    @FXML private Label noPhotoLabel;
    @FXML private Label titleLabel;
    @FXML private Label statusBadge;
    @FXML private Label occupiedBadge;
    @FXML private Label metaLabel;
    @FXML private Label priceLabel;
    @FXML private Label noteLabel;
    @FXML private Label rejectionLabel;
    @FXML private Button editButton;
    @FXML private Button occupiedButton;

    private OwnerListing listing;
    private Actions actions;

    public void setListing(OwnerListing listing, Actions actions) {
        this.listing = listing;
        this.actions = actions;

        Image cover = Images.load(listing.getCoverPhoto(), 320, 230);
        photoView.setImage(cover);
        noPhotoLabel.setVisible(cover == null);
        noPhotoLabel.setManaged(cover == null);

        titleLabel.setText(listing.getPropertyName());
        metaLabel.setText(OwnerOptions.labelOf(OwnerOptions.PROPERTY_TYPES, listing.getPropertyType())
                + "  ·  " + OwnerOptions.labelOf(OwnerOptions.ROOM_TYPES, listing.getRoomType())
                + "  ·  " + listing.getBarangay());
        priceLabel.setText(listing.getPriceText());

        statusBadge.setText(listing.getStatusLabel());
        statusBadge.getStyleClass().setAll("badge-status", "badge-status-" + listing.getStatus());

        occupiedBadge.getStyleClass().setAll("badge-status", "badge-status-occupied");
        setShown(occupiedBadge, listing.isOccupied());
        occupiedButton.setText(listing.isOccupied() ? "Mark available" : "Mark occupied");

        String note = null;
        switch (listing.getStatus()) {
            case OwnerListing.PENDING:  note = "Waiting for admin approval. Renters cannot see it yet."; break;
            case OwnerListing.HIDDEN:   note = "Hidden by the admin. Renters cannot see it."; break;
            case OwnerListing.VERIFIED: note = listing.isOccupied()
                    ? "Marked occupied, so it is hidden from search." : "Live: renters can find this listing."; break;
            default: break;
        }
        noteLabel.setText(note);
        setShown(noteLabel, note != null);

        boolean rejected = OwnerListing.REJECTED.equals(listing.getStatus());
        if (rejected) {
            String reason = listing.getRejectionReason();
            rejectionLabel.setText("Rejected: " + (reason == null || reason.isBlank() ? "no reason given" : reason));
        }
        setShown(rejectionLabel, rejected);
        editButton.setText(rejected ? "Edit & resubmit" : "Edit");
    }

    @FXML
    private void onEdit() { if (actions != null) actions.onEdit(listing); }

    @FXML
    private void onToggleOccupied() { if (actions != null) actions.onToggleOccupied(listing); }

    @FXML
    private void onDelete() { if (actions != null) actions.onDelete(listing); }

    private static void setShown(javafx.scene.Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }
}
