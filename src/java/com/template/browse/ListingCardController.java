package com.template.browse;

import com.template.model.Listing;
import com.template.util.Images;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/** One listing card (photo, name, price). Click it to open the listing. */
public class ListingCardController {

    private static final double CORNER = 26;

    @FXML private VBox root;
    @FXML private StackPane photoBox;
    @FXML private Region placeholder;
    @FXML private Label noPhotoLabel;
    @FXML private ImageView photoView;
    @FXML private Label nameLabel;
    @FXML private Label priceLabel;

    private Listing listing;
    private Navigator navigator;

    /** @param size width and height of the square photo in pixels */
    public void setData(Listing listing, Navigator navigator, double size) {
        this.listing = listing;
        this.navigator = navigator;

        nameLabel.setText(listing.getPropertyName());
        priceLabel.setText(listing.getPriceText());

        root.setPrefWidth(size);
        root.setMinWidth(size);
        root.setMaxWidth(size);
        for (Region r : new Region[]{photoBox, placeholder}) {
            r.setPrefSize(size, size);
            r.setMinSize(size, size);
            r.setMaxSize(size, size);
        }
        nameLabel.setMaxWidth(size);
        priceLabel.setMaxWidth(size);

        photoView.setFitWidth(size);
        photoView.setFitHeight(size);
        Rectangle clip = new Rectangle(size, size);
        clip.setArcWidth(CORNER * 2);
        clip.setArcHeight(CORNER * 2);
        photoView.setClip(clip);

        Image image = Images.load(listing.getCoverPhoto(), size * 2, size * 2);
        if (image != null) {
            Images.showSquareCrop(photoView, image);
            noPhotoLabel.setVisible(false);
            image.errorProperty().addListener((obs, was, isError) -> {
                if (isError) {
                    photoView.setImage(null);
                    noPhotoLabel.setVisible(true);
                }
            });
        } else {
            photoView.setImage(null);
            noPhotoLabel.setVisible(true);
        }
    }

    @FXML
    private void onOpen() {
        if (navigator != null && listing != null) navigator.openListing(listing);
    }
}