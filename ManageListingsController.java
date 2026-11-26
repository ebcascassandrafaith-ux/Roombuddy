package com.template.controllers;

import com.template.dao.AdminListingDAO;
import com.template.model.AdminListing;
import com.template.util.AdminUi;
import com.template.util.AdminWindow;
import com.template.util.FxView;
import com.template.util.Session;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

import java.util.Optional;

/** Screen: admin_manage_listings.fxml. Table: listings. */
public class ManageListingsController extends AdminScreenController {

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
        table.setItems(FXCollections.observableArrayList(dao.getAll()));
        detailsArea.clear();
    }

    private AdminListing getSelected() {
        AdminListing l = table.getSelectionModel().getSelectedItem();
        if (l == null) {
            AdminUi.error("Please select a listing first.");
        }
        return l;
    }

    @FXML
    private void onViewFiles() {
        AdminListing l = getSelected();
        if (l == null) {
            return;
        }
        AdminUi.showFiles("Photos and documents: " + l.getPropertyName(), dao.getFiles(l.getListingId()));
    }

    @FXML
    private void onUnpublish() {
        AdminListing l = getSelected();
        if (l == null) {
            return;
        }
        if (l.getStatus().equals("hidden")) {
            AdminUi.info("This listing is already hidden.");
            return;
        }
        if (AdminUi.confirm("Unpublish \"" + l.getPropertyName() + "\"? It will be hidden from the public.")) {
            if (dao.unpublish(l, Session.getUserId())) {
                AdminUi.info("Listing unpublished.");
                loadData();
            } else {
                AdminUi.error("Could not unpublish. Check the console for details.");
            }
        }
    }

    /** Puts a hidden listing back online. */
    @FXML
    private void onRepublish() {
        AdminListing l = getSelected();
        if (l == null) {
            return;
        }
        if (!l.getStatus().equals("hidden")) {
            AdminUi.info("Only hidden listings can be republished. Pending listings go through Review Listings.");
            return;
        }
        if (AdminUi.confirm("Republish \"" + l.getPropertyName() + "\"? Renters will see it again.")) {
            if (dao.republish(l, Session.getUserId())) {
                AdminUi.info("Listing is live again.");
                loadData();
            } else {
                AdminUi.error("Could not republish. Check the console for details.");
            }
        }
    }

    @FXML
    private void onDelete() {
        AdminListing l = getSelected();
        if (l == null) {
            return;
        }
        if (AdminUi.confirm("Are you sure you want to permanently delete \""
                + l.getPropertyName() + "\"?")) {
            if (dao.delete(l, Session.getUserId())) {
                AdminUi.info("Listing deleted.");
                loadData();
            } else {
                AdminUi.error("Could not delete. Check the console for details.");
            }
        }
    }

    @FXML
    private void onEdit() {
        AdminListing l = getSelected();
        if (l == null) {
            return;
        }

        // Load the edit form (FXML) and put it inside a dialog
        FxView<EditListingController> form = FxView.load(AdminWindow.EDIT_LISTING);
        EditListingController formController = form.getController();
        formController.fill(l);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Listing #" + l.getListingId());
        dialog.getDialogPane().setContent(form.getRoot());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Check the inputs when OK is clicked; consume() keeps the dialog open if something is invalid
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(ActionEvent.ACTION, event -> {
            String problem = formController.validate();
            if (problem != null) {
                AdminUi.error(problem);
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            formController.applyTo(l);
            if (dao.update(l, Session.getUserId())) {
                AdminUi.info("Listing updated successfully.");
            } else {
                AdminUi.error("Could not update the listing. Check the console for details.");
            }
            loadData();
        }
    }
}
