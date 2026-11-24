package com.template.admin;

import com.template.admin.dao.UserDAO;
import com.template.admin.util.AdminUi;
import com.template.util.FxView;
import com.template.util.Session;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * The admin module's own window. It does not touch MainController or main.fxml.
 *
 * HOW THE MAIN APP OPENS THE ADMIN MODULE (Member A's login calls this after a successful login
 * whose users.role is 'admin', and after Session.login(...)):
 *
 *     AdminWindow.openDashboard();
 */
public final class AdminWindow {

    // The FXML screens (resources/com/template/admin/). Open them in Scene Builder to restyle.
    public static final String LOGIN            = "/com/template/admin/admin_login.fxml";
    public static final String DASHBOARD        = "/com/template/admin/admin_dashboard.fxml";
    public static final String REVIEW_ID        = "/com/template/admin/admin_review_id.fxml";
    public static final String REVIEW_LISTING   = "/com/template/admin/admin_review_listing.fxml";
    public static final String MANAGE_USERS     = "/com/template/admin/admin_manage_users.fxml";
    public static final String MANAGE_LISTINGS  = "/com/template/admin/admin_manage_listings.fxml";
    public static final String REPORTS          = "/com/template/admin/admin_reports.fxml";
    public static final String LOGS             = "/com/template/admin/admin_logs.fxml";
    public static final String EDIT_LISTING     = "/com/template/admin/admin_edit_listing.fxml";

    private static Stage stage;

    private AdminWindow() {}

    /** Shows the admin login screen (used until Member A's shared login exists, and for testing). */
    public static void openLogin() {
        show(LOGIN);
    }

    /** Shows the dashboard, but only if the user in Session is a real, active admin. */
    public static void openDashboard() {
        if (!new UserDAO().isActiveAdmin(Session.getUserId())) {
            AdminUi.error("This account is not an admin.");
            return;
        }
        show(DASHBOARD);
    }

    /** Loads one FXML screen (using the project's FxView) and shows it in the admin window. */
    public static void show(String fxmlPath) {
        FxView<Object> view = FxView.load(fxmlPath);
        setRoot((Parent) view.getRoot());
    }

    private static void setRoot(Parent root) {
        if (stage == null || !stage.isShowing()) {
            stage = new Stage();
            stage.setTitle("RoomBuddy Admin");
            stage.setScene(new Scene(root, 1100, 650));
            stage.show();
        } else {
            stage.getScene().setRoot(root);   // keep the window, replace what is inside it
            stage.toFront();
        }
    }

    public static void close() {
        if (stage != null) {
            stage.close();
            stage = null;
        }
    }
}
