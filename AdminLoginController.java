package com.template.controllers;

import com.template.User;
import com.template.dao.UserDAO;
import com.template.model.AdminUser;
import com.template.util.AdminUi;
import com.template.util.AdminWindow;
import com.template.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Screen: admin_login.fxml.
 * Admin-only login for testing the admin module on its own (run AdminLauncher).
 * In the full app, Test1's shared login opens the dashboard for users whose role is 'admin'
 * (see the comment in AdminWindow), so this screen is not used there.
 */
public class AdminLoginController {

    // These fields are filled in by JavaFX: the field name must equal fx:id in the FXML.
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    // Runs when "Log in" is clicked, or Enter is pressed in the password box (onAction="#onLogin").
    @FXML
    private void onLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        if (email.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please fill in all fields.");
            return;
        }

        // The database check (BCrypt) happens inside UserDAO; it only accepts ACTIVE admins.
        AdminUser admin = new UserDAO().loginAdmin(email, password);
        if (admin == null) {
            messageLabel.setText("Invalid admin credentials.");
            return;
        }

        // Same Session as the main app: it stores a com.template.User
        User user = new User(admin.getUserId(), admin.getFullName(), admin.getEmail(),
                admin.getContactNumber(), admin.getRole(), admin.getStatus());
        Session.login(user, "Yes".equals(admin.getIdVerified()));

        if (!AdminWindow.openDashboard()) {
            AdminUi.error("Could not open the dashboard.");
        }
    }
}
