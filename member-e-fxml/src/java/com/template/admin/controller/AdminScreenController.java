package com.template.admin.controller;

import com.template.admin.AdminWindow;

/**
 * Parent of every admin screen controller. It gives each screen the "< Dashboard" button action,
 * so each FXML only needs onAction="#onBack" on its back button.
 */
public abstract class AdminScreenController {

    public void onBack() {
        AdminWindow.show(AdminWindow.DASHBOARD);
    }
}
