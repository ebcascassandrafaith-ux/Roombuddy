package com.template.browse;

import com.template.model.Listing;
import com.template.model.ListingGroup;
import com.template.util.FxView;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

/** Every listing of one row (e.g. all "Dormitories in Lapasan") in a wrapping grid. */
public class SeeAllController {

    private static final String CARD_FXML = "/com/template/browse/listing_card.fxml";
    private static final double CARD_SIZE = 250;

    @FXML private Label titleLabel;
    @FXML private FlowPane grid;

    private Navigator navigator;

    public void show(ListingGroup group, Navigator navigator) {
        this.navigator = navigator;
        titleLabel.setText(group.getTitle());
        grid.getChildren().clear();
        for (Listing l : group.getListings()) {
            FxView<ListingCardController> card = FxView.load(CARD_FXML);
            card.getController().setData(l, navigator, CARD_SIZE);
            grid.getChildren().add(card.getRoot());
        }
    }

    @FXML
    private void onBack() {
        if (navigator != null) navigator.goHome();
    }
}