package com.template.admin.controller;

import com.template.admin.dao.AdminListingDAO;
import com.template.admin.dao.ReportDAO;
import com.template.admin.dao.UserDAO;
import com.template.admin.model.AdminUser;
import com.template.admin.util.AdminUi;
import com.template.util.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

/** Screen: admin_manage_users.fxml. Table: users. */
public class ManageUsersController extends AdminScreenController {

    @FXML private TableView<AdminUser> table;

    private final UserDAO dao = new UserDAO();

    @FXML
    private void initialize() {
        loadData();
    }

    private void loadData() {
        table.setItems(FXCollections.observableArrayList(dao.getAllUsers()));
    }

    /** Returns the selected user, or null (after showing a message). */
    private AdminUser getSelected() {
        AdminUser u = table.getSelectionModel().getSelectedItem();
        if (u == null) {
            AdminUi.error("Please select a user first.");
        }
        return u;
    }

    @FXML
    private void onDetails() {
        AdminUser u = getSelected();
        if (u == null) {
            return;
        }
        int listings = new AdminListingDAO().countByOwner(u.getUserId());
        int reports = new ReportDAO().countAgainstUser(u.getUserId());
        AdminUi.info("Name: " + u.getFullName()
                + "\nEmail: " + u.getEmail()
                + "\nContact: " + u.getContactNumber()
                + "\nRole: " + u.getRole()
                + "\nStatus: " + u.getStatus()
                + "\nID verified: " + u.getIdVerified()
                + "\nListings posted: " + listings
                + "\nReports against this user: " + reports);
    }

    @FXML private void onSuspend() { changeStatus("suspended"); }

    @FXML private void onBan() { changeStatus("banned"); }

    private void changeStatus(String newStatus) {
        AdminUser u = getSelected();
        if (u == null) {
            return;
        }
        if (u.getRole().equals("admin")) {
            AdminUi.error("Admin accounts cannot be suspended, banned, or deleted here.");
            return;
        }
        String reason = AdminUi.askReason("Reason to "
                + (newStatus.equals("banned") ? "ban " : "suspend ") + u.getFullName());
        if (reason == null) {
            AdminUi.error("A reason is required.");
            return;
        }
        if (dao.updateStatus(u.getUserId(), newStatus, Session.getUserId(), reason)) {
            AdminUi.info("User status changed to " + newStatus + ".");
            loadData();
        } else {
            AdminUi.error("Could not update the user. Check the console for details.");
        }
    }

    @FXML
    private void onDelete() {
        AdminUser u = getSelected();
        if (u == null) {
            return;
        }
        if (u.getRole().equals("admin")) {
            AdminUi.error("Admin accounts cannot be suspended, banned, or deleted here.");
            return;
        }
        String reason = AdminUi.askReason("Delete " + u.getFullName()
                + " and all of their listings permanently");
        if (reason == null) {
            AdminUi.error("A reason is required.");
            return;
        }
        if (dao.deleteUser(u.getUserId(), Session.getUserId(), reason)) {
            AdminUi.info("User deleted.");
            loadData();
        } else {
            AdminUi.error("Could not delete the user. Check the console for details.");
        }
    }
}
