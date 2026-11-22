package com.template.browse;

import com.template.dao.ListingDAO;
import com.template.model.ListingGroup;
import com.template.util.Async;
import com.template.util.FxView;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;

import java.util.List;

/** The home screen. Loads the grouped listings and stacks one ListingSectionController per row. */
public class HomeController {

    private static final String SECTION_FXML = "/com/template/browse/listing_section.fxml";

    @FXML private ScrollPane pageScroll;
    @FXML private VBox sectionsBox;
    @FXML private Label statusLabel;

    private final ListingDAO listingDAO = new ListingDAO();
    private Navigator navigator;

    @FXML
    private void initialize() {
        // Lets the mouse wheel scroll the page even when the mouse is over a sideways-scrolling row.
        sectionsBox.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (Math.abs(e.getDeltaY()) <= Math.abs(e.getDeltaX())) return;
            double hidden = sectionsBox.getHeight() - pageScroll.getViewportBounds().getHeight();
            if (hidden > 0) {
                double v = pageScroll.getVvalue() - e.getDeltaY() / hidden;
                pageScroll.setVvalue(Math.max(0, Math.min(1, v)));
            }
            e.consume();
        });
    }

    /** Called once by MainController. */
    public void start(Navigator navigator) {
        this.navigator = navigator;
        load();
    }

    public void load() {
        showStatus("Loading listings...");
        Async.run(listingDAO::findHomeGroups,
                this::showGroups,
                err -> showStatus("Could not load listings. Check your database connection.\n(" + err.getMessage() + ")"));
    }

    private void showGroups(List<ListingGroup> groups) {
        sectionsBox.getChildren().clear();
        if (groups.isEmpty()) {
            showStatus("No verified listings yet.");
            return;
        }
        hideStatus();
        for (ListingGroup group : groups) {
            FxView<ListingSectionController> section = FxView.load(SECTION_FXML);
            section.getController().setData(group, navigator);
            sectionsBox.getChildren().add(section.getRoot());
        }
        pageScroll.setVvalue(0);
    }

    private void showStatus(String text) {
        statusLabel.setText(text);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void hideStatus() {
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }
}