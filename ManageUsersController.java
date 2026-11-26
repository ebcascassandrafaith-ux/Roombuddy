package com.template.controllers;

import com.template.dao.AdminListingDAO;
import com.template.dao.ReportDAO;
import com.template.dao.UserDAO;
import com.template.model.AdminUser;
import com.template.util.AdminUi;
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

    /** Flowchart: "View profile, listings, ID status, any reports". */
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
                + "\nContact: " + (u.getContactNumber() == null ? "-" : u.getContactNumber())
                + "\nRole: " + u.getRole()
                + "\nStatus: " + u.getStatus()
                + "\nID verified: " + u.getIdVerified()
                + "\nListings posted: " + listings
                + "\nReports against this user: " + reports);
    }

    @FXML private void onSuspend() { changeStatus("suspended"); }

    @FXML private void onBan() { changeStatus("banned"); }

    /** Puts a suspended or banned user back to 'active'. */
    @FXML private void onRestore() { changeStatus("active"); }

    private void changeStatus(String newStatus) {
        AdminUser u = getSelected();
        if (u == null) {
            return;
        }
        if (u.getRole().equals("admin")) {
            AdminUi.error("Admin accounts cannot be suspended, banned, restored, or deleted here.");
            return;
        }
        if (u.getStatus().equals(newStatus)) {
            AdminUi.info("This user is already " + newStatus + ".");
            return;
        }

        String reason;
        if (newStatus.equals("active")) {
            if (!AdminUi.confirm("Restore " + u.getFullName() + "? They will be able to log in again.")) {
                return;
            }
            reason = "Account restored";
        } else {
            reason = AdminUi.askReason("Reason to "
                    + (newStatus.equals("banned") ? "ban " : "suspend ") + u.getFullName());
            if (reason == null) {
                return;   // cancelled
            }
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
            AdminUi.error("Admin accounts cannot be suspended, banned, restored, or deleted here.");
            return;
        }
        String reason = AdminUi.askReason("Delete " + u.getFullName()
                + " and all of their listings permanently");
        if (reason == null) {
            return;   // cancelled
        }
        if (dao.deleteUser(u.getUserId(), Session.getUserId(), reason)) {
            AdminUi.info("User deleted.");
            loadData();
        } else {
            AdminUi.error("Could not delete the user. Check the console for details.");
        }
    }
}
