package com.template.browse;

import com.template.util.Images;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/** A rounded photo tile that crops the picture to fill it (or shows "No photo"). Used by the property and room pages. */
final class PhotoTile extends StackPane {

    private final ImageView view = new ImageView();
    private final Rectangle clip = new Rectangle();
    private final Label noPhoto = new Label("No photo");
    private final Label more = new Label();
    private final Image image;

    PhotoTile(String path) {
        getStyleClass().add("gallery-tile");
        noPhoto.getStyleClass().add("no-photo-text");
        more.getStyleClass().add("gallery-more");
        more.setVisible(false);

        view.setPreserveRatio(false);
        clip.setArcWidth(36);
        clip.setArcHeight(36);

        image = Images.load(path, 1400, 1400);
        noPhoto.setVisible(image == null);
        view.setVisible(image != null);

        getChildren().addAll(noPhoto, view, more);
        StackPane.setAlignment(more, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(more, new Insets(0, 12, 12, 0));
        setClip(clip);
    }

    /** Shows "+3 more" on this tile when there are extra photos. */
    void setMoreCount(int extra) {
        more.setText("+" + extra + " more");
        more.setVisible(true);
    }

    /** Sets the tile to exactly w x h pixels and crops the photo to fit. */
    void fit(double w, double h) {
        setMinSize(w, h);
        setPrefSize(w, h);
        setMaxSize(w, h);
        clip.setWidth(w);
        clip.setHeight(h);
        view.setFitWidth(w);
        view.setFitHeight(h);
        if (image != null) Images.showCover(view, image, w, h);
    }
}
