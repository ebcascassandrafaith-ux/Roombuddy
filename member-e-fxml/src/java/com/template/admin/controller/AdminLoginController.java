package com.template.admin.controller;

import com.template.admin.AdminWindow;
import com.template.admin.dao.UserDAO;
import com.template.admin.model.AdminUser;
import com.template.admin.util.PasswordUtil;
import com.template.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Screen: admin_login.fxml.
 * Temporary admin-only login. When Member A's shared login exists, it should call
 * AdminWindow.openDashboard() for users whose role is 'admin', and this screen is no longer needed.
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
        // Hash what was typed, then compare it with users.password_hash
        AdminUser admin = new UserDAO().loginAdmin(email, PasswordUtil.hash(password));
        if (admin == null) {
            messageLabel.setText("Invalid admin credentials.");
        } else {
            Session.login(admin.getUserId(), true);   // the project's Session: id + "ID verified"
            AdminWindow.openDashboard();
        }
    }
}
