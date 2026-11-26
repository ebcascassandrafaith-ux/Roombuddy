package com.template.util;

import com.template.dao.UserDAO;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.net.URL;

/**
 * The admin module's own window. It does not touch the main RoomBuddy window or Main.
 *
 * HOW THE MAIN APP (Test1) OPENS THE ADMIN MODULE: in LoginController, right after Session.login(user, verified):
 *
 *     if ("admin".equals(user.getRole())) {
 *         Stage loginStage = (Stage) txtEmail.getScene().getWindow();
 *         if (AdminWindow.openDashboard(loginStage::show)) {   // the Logout button shows the login window again
 *             loginStage.hide();
 *         }
 *         return;
 *     }
 *
 * For testing the admin module alone, run AdminLauncher (it opens the admin login screen).
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
    private static Runnable afterLogout;     // what the main app wants to happen after "Logout"
    private static boolean fontsLoaded;

    private AdminWindow() {}

    /** Shows the admin login screen (used for testing the module on its own). */
    public static void openLogin() {
        show(LOGIN);
    }

    /** Shows the dashboard, but only if the user in Session is a real, active admin. */
    public static boolean openDashboard() {
        return openDashboard(null);
    }

    /**
     * @param afterLogoutAction runs when the admin clicks Logout (for example: show the login window again);
     *                          may be null
     * @return true if the dashboard was opened
     */
    public static boolean openDashboard(Runnable afterLogoutAction) {
        if (!new UserDAO().isActiveAdmin(Session.getUserId())) {
            AdminUi.error("This account is not an admin.");
            return false;
        }
        afterLogout = afterLogoutAction;
        show(DASHBOARD);
        return true;
    }

    /** Loads one FXML screen (using the project's FxView) and shows it in the admin window. */
    public static void show(String fxmlPath) {
        // Every screen except the login needs a logged-in, active admin (checked against the database).
        if (!fxmlPath.equals(LOGIN) && !new UserDAO().isActiveAdmin(Session.getUserId())) {
            AdminUi.error("Please log in as an admin first.");
            show(LOGIN);
            return;
        }
        loadFontsOnce();
        FxView<Object> view = FxView.load(fxmlPath);
        setRoot((Parent) view.getRoot());
    }

    /** The Logout button: ends the session, lets the main app react, then closes the admin window. */
    public static void logout() {
        Session.logout();
        Runnable action = afterLogout;
        afterLogout = null;
        if (action != null) {
            action.run();     // run it BEFORE closing, so the app never has "no windows" for a moment
        }
        close();
    }

    public static void close() {
        if (stage != null) {
            stage.close();
            stage = null;
        }
    }

    private static void setRoot(Parent root) {
        if (stage == null || !stage.isShowing()) {
            stage = new Stage();
            stage.setTitle("RoomBuddy Admin");
            stage.setScene(new Scene(root, 1100, 700));
            stage.setMinWidth(900);
            stage.setMinHeight(560);
            stage.show();
        } else {
            stage.getScene().setRoot(root);   // keep the window, replace what is inside it
            stage.toFront();
        }
    }

    /** Same Figma fonts as the main app. Skipped quietly when the font files are not in this project. */
    private static void loadFontsOnce() {
        if (fontsLoaded) {
            return;
        }
        fontsLoaded = true;
        for (String name : new String[] {"CherryBombOne-Regular", "Poppins-ExtraBold", "Poppins-SemiBold"}) {
            URL url = AdminWindow.class.getResource("/fonts/" + name + ".ttf");
            if (url != null) {
                Font.loadFont(url.toExternalForm(), 12);
            }
        }
    }
}
