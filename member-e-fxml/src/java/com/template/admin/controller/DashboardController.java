package com.template.admin.controller;

import com.template.admin.AdminWindow;
import com.template.admin.dao.AdminListingDAO;
import com.template.admin.dao.IdVerificationDAO;
import com.template.admin.dao.ReportDAO;
import com.template.admin.dao.UserDAO;
import com.template.admin.util.AdminUi;
import com.template.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/** Screen: admin_dashboard.fxml. Shows the counts and the menu buttons. */
public class DashboardController {

    @FXML private Label whoLabel;
    @FXML private Label pendingIdsLabel;
    @FXML private Label pendingListingsLabel;
    @FXML private Label totalUsersLabel;
    @FXML private Label openReportsLabel;

    // initialize() runs automatically right after the FXML is loaded.
    @FXML
    private void initialize() {
        whoLabel.setText("Logged in as: " + new UserDAO().getFullName(Session.getUserId()));
        pendingIdsLabel.setText(String.valueOf(new IdVerificationDAO().countPending()));
        pendingListingsLabel.setText(String.valueOf(new AdminListingDAO().countByStatus("pending")));
        totalUsersLabel.setText(String.valueOf(new UserDAO().countAll()));
        openReportsLabel.setText(String.valueOf(new ReportDAO().countOpen()));
    }

    @FXML private void onReviewIds()      { AdminWindow.show(AdminWindow.REVIEW_ID); }
    @FXML private void onReviewListings() { AdminWindow.show(AdminWindow.REVIEW_LISTING); }
    @FXML private void onManageUsers()    { AdminWindow.show(AdminWindow.MANAGE_USERS); }
    @FXML private void onManageListings() { AdminWindow.show(AdminWindow.MANAGE_LISTINGS); }
    @FXML private void onReports()        { AdminWindow.show(AdminWindow.REPORTS); }
    @FXML private void onLogs()           { AdminWindow.show(AdminWindow.LOGS); }

    @FXML
    private void onLogout() {
        if (AdminUi.confirm("Are you sure you want to log out?")) {
            Session.logout();
            AdminWindow.close();
        }
    }
}
