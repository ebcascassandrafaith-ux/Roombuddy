package com.template.controllers;

import com.template.dao.AdminListingDAO;
import com.template.model.AdminListing;
import com.template.util.AdminUi;
import com.template.util.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

/** Screen: admin_review_listing.fxml. Tables: listings and the tables around it. */
public class ReviewListingController extends AdminScreenController {

    @FXML private TableView<AdminListing> table;
    @FXML private TextArea detailsArea;

    private final AdminListingDAO dao = new AdminListingDAO();

    @FXML
    private void initialize() {
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) ->
                detailsArea.setText(newValue == null ? "" : dao.getDetailsText(newValue)));
        loadData();
    }

    private void loadData() {
        table.setItems(FXCollections.observableArrayList(dao.getByStatus("pending")));
        detailsArea.clear();
    }

    /** Flowchart: "View listing details and property documents". */
    @FXML
    private void onViewFiles() {
        AdminListing l = table.getSelectionModel().getSelectedItem();
        if (l == null) {
            AdminUi.error("Please select a listing first.");
            return;
        }
        AdminUi.showFiles("Photos and documents: " + l.getPropertyName(), dao.getFiles(l.getListingId()));
    }

    @FXML
    private void onApprove() {
        AdminListing l = table.getSelectionModel().getSelectedItem();
        if (l == null) {
            AdminUi.error("Please select a listing first.");
            return;
        }
        if (!AdminUi.confirm("Approve \"" + l.getPropertyName() + "\" and make it public?")) {
            return;
        }
        if (dao.approve(l, Session.getUserId())) {
            AdminUi.info("Listing approved. The owner has been notified.");
        } else {
            AdminUi.error("Could not approve. It may already be reviewed. Check the console for details.");
        }
        loadData();
    }

    @FXML
    private void onReject() {
        AdminListing l = table.getSelectionModel().getSelectedItem();
        if (l == null) {
            AdminUi.error("Please select a listing first.");
            return;
        }
        String reason = AdminUi.askReason("Reject \"" + l.getPropertyName() + "\"");
        if (reason == null) {
            return;   // cancelled
        }
        if (dao.reject(l, Session.getUserId(), reason)) {
            AdminUi.info("Listing rejected. The owner has been notified.");
        } else {
            AdminUi.error("Could not reject. It may already be reviewed. Check the console for details.");
        }
        loadData();
    }
}
