RoomBuddy admin module (Member E) - how it fits with Test1
============================================================

RUN IT ON ITS OWN (testing)
  1. XAMPP: start MySQL. Import roombuddy.sql (+ admin_sample_data.sql) into the "roombuddy" database.
  2. Libraries (File > Project Structure > Libraries): JavaFX (lib2), mysql-connector-j, jbcrypt.
  3. Copy the "uploads" folder (sample ID images, listing photos) into this project's root folder.
  4. Run "Admin Run" (class com.template.util.AdminLauncher).
  5. Log in with an account whose role is 'admin' (sample: admin@example.com / admin123).

PUT IT INTO Test1 (the full app)
  A. Copy these folders from untitled6/src/java/com/template/ into Test1's src/java/com/template/:
       controllers/   dao/ (AdminListingDAO, AdminLogDAO, IdVerificationDAO, NotificationDAO, ReportDAO, UserDAO)
       model/ (AdminListing, AdminLog, AdminUser, IdVerification, Report)
       util/ (AdminApp, AdminLauncher, AdminWindow, AdminUi)
     (User.java, util/Session.java and util/FxView.java are identical copies of Test1's own files - skip them.)
  B. Copy src/resources/com/template/admin.css and the whole src/resources/com/template/admin/ folder.
  C. In Test1's LoginController.handleLogin(), right after  Session.login(user, verified);  add:

        if ("admin".equals(user.getRole())) {
            Stage loginStage = (Stage) txtEmail.getScene().getWindow();
            if (AdminWindow.openDashboard(loginStage::show)) {   // Logout shows the login window again
                loginStage.hide();
            }
            return;
        }

     and add the imports  com.template.util.AdminWindow  and  javafx.stage.Stage.
