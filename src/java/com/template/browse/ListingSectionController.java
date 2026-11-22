package com.template.browse;

import com.template.model.Listing;
import com.template.model.ListingGroup;
import com.template.util.FxView;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;

import java.util.List;

/** One row of the home screen. The arrow next to the title opens "see all"; the chevron scrolls the row. */
public class ListingSectionController {

    private static final String CARD_FXML = "/com/template/browse/listing_card.fxml";
    private static final double CARD_SIZE = 210;
    private static final double CARD_GAP = 22;
    private static final int MAX_CARDS_IN_ROW = 12;

    @FXML private Label titleLabel;
    @FXML private Button seeAllButton;
    @FXML private Button nextButton;
    @FXML private ScrollPane rowScroll;
    @FXML private HBox row;

    private ListingGroup group;
    private Navigator navigator;

    @FXML
    private void initialize() {
        row.widthProperty().addListener((obs, a, b) -> updateChevron());
        rowScroll.viewportBoundsProperty().addListener((obs, a, b) -> updateChevron());
    }

    public void setData(ListingGroup group, Navigator navigator) {
        this.group = group;
        this.navigator = navigator;

        titleLabel.setText(group.getTitle());
        row.getChildren().clear();

        List<Listing> listings = group.getListings();
        int shown = Math.min(listings.size(), MAX_CARDS_IN_ROW);
        for (int i = 0; i < shown; i++) {
            FxView<ListingCardController> card = FxView.load(CARD_FXML);
            card.getController().setData(listings.get(i), navigator, CARD_SIZE);
            row.getChildren().add(card.getRoot());
        }
    }

    @FXML
    private void onSeeAll() {
        if (navigator != null && group != null) navigator.showSeeAll(group);
    }

    /** Scrolls two cards to the right; at the end it jumps back to the start. */
    @FXML
    private void onNext() {
        double hidden = row.getWidth() - rowScroll.getViewportBounds().getWidth();
        if (hidden <= 1) return;
        double step = (CARD_SIZE + CARD_GAP) * 2 / hidden;
        double h = rowScroll.getHvalue();
        rowScroll.setHvalue(h >= 0.999 ? 0 : Math.min(1, h + step));
    }

    private void updateChevron() {
        boolean overflow = row.getWidth() - rowScroll.getViewportBounds().getWidth() > 1;
        nextButton.setVisible(overflow);
    }
}