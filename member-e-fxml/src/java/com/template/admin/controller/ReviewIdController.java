package com.template.admin.controller;

import com.template.admin.dao.IdVerificationDAO;
import com.template.admin.model.IdVerification;
import com.template.admin.util.AdminUi;
import com.template.util.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

/** Screen: admin_review_id.fxml. Tables: id_verifications, users, notifications, admin_logs. */
public class ReviewIdController extends AdminScreenController {

    @FXML private TableView<IdVerification> table;
    @FXML private Label infoLabel;
    @FXML private HBox imageBox;

    private final IdVerificationDAO dao = new IdVerificationDAO();

    @FXML
    private void initialize() {
        // When the admin selects a row, show that submission's details
        table.getSelectionModel().selectedItemProperty()
             .addListener((obs, oldValue, newValue) -> showDetails(newValue));
        loadData();
    }

    private void loadData() {
        table.setItems(FXCollections.observableArrayList(dao.getPending()));
        showDetails(null);
    }

    private void showDetails(IdVerification v) {
        imageBox.getChildren().clear();
        if (v == null) {
            infoLabel.setText("Select a submission to see the uploaded ID.");
            return;
        }
        infoLabel.setText("Registered name: " + v.getUserName() + "   |   Email: "
                + v.getEmail() + "   |   ID type: " + v.getIdType());
        imageBox.getChildren().addAll(AdminUi.imagePreview(v.getFrontImagePath()),
                                      AdminUi.imagePreview(v.getBackImagePath()));
    }

    @FXML
    private void onApprove() {
        IdVerification v = table.getSelectionModel().getSelectedItem();
        if (v == null) {
            AdminUi.error("Please select a submission first.");
            return;
        }
        if (!AdminUi.confirm("Approve the ID of " + v.getUserName() + "?")) {
            return;
        }
        if (dao.approve(v, Session.getUserId())) {
            AdminUi.info("ID approved. The user has been notified.");
            loadData();
        } else {
            AdminUi.error("Could not approve. Check the console for details.");
        }
    }

    @FXML
    private void onReject() {
        IdVerification v = table.getSelectionModel().getSelectedItem();
        if (v == null) {
            AdminUi.error("Please select a submission first.");
            return;
        }
        String reason = AdminUi.askReason("Reject the ID of " + v.getUserName());
        if (reason == null) {
            AdminUi.error("A rejection reason is required.");
            return;
        }
        if (dao.reject(v, Session.getUserId(), reason)) {
            AdminUi.info("ID rejected. The user has been notified.");
            loadData();
        } else {
            AdminUi.error("Could not reject. Check the console for details.");
        }
    }
}
