package com.template.browse;

import com.template.dao.ListingDAO;
import com.template.model.Listing;
import com.template.model.SearchCriteria;
import com.template.util.Async;
import com.template.util.FxView;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;

import java.util.List;

/**
 * The results of a search (flowchart: "Is there matching listings?").
 * Yes: the matching cards.  No: "No listings match your search".
 * MainController calls search(...) every time the keywords or a filter changes.
 */
public class SearchResultsController {

    private static final String CARD_FXML = "/com/template/browse/listing_card.fxml";
    private static final double CARD_SIZE = 250;

    @FXML private ScrollPane pageScroll;
    @FXML private Label titleLabel;
    @FXML private Label summaryLabel;
    @FXML private Label statusLabel;
    @FXML private FlowPane grid;

    private final ListingDAO listingDAO = new ListingDAO();
    private Navigator navigator;
    private int searchCounter = 0;   // lets us ignore an old, slower search that finishes late

    public void search(SearchCriteria criteria, Navigator navigator) {
        this.navigator = navigator;
        final int ticket = ++searchCounter;

        titleLabel.setText(criteria.isEmpty() ? "All listings" : "Search results");
        summaryLabel.setText(criteria.describe());
        grid.getChildren().clear();
        showStatus("Searching...");

        Async.run(() -> listingDAO.search(criteria),
                results -> {
                    if (ticket == searchCounter) showResults(criteria, results);
                },
                err -> {
                    if (ticket == searchCounter) {
                        showStatus("Could not search. Check your database connection.\n(" + err.getMessage() + ")");
                    }
                });
    }

    private void showResults(SearchCriteria criteria, List<Listing> results) {
        grid.getChildren().clear();
        if (results.isEmpty()) {
            // Flowchart: Display "No listings match your search"
            showStatus("No listings match your search.");
            return;
        }
        hideStatus();

        String count = results.size() + (results.size() == 1 ? " listing found" : " listings found");
        String summary = criteria.describe();
        summaryLabel.setText(summary.isEmpty() ? count : count + "  \u2022  " + summary);

        for (Listing l : results) {
            FxView<ListingCardController> card = FxView.load(CARD_FXML);
            card.getController().setData(l, navigator, CARD_SIZE);
            grid.getChildren().add(card.getRoot());
        }
        pageScroll.setVvalue(0);
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
