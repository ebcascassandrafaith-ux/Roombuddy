package com.template.controllers;

import com.template.dao.ReportDAO;
import com.template.model.Report;
import com.template.util.AdminUi;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

/** Screen: admin_reports.fxml. Table: reports. */
public class ReportsController extends AdminScreenController {

    @FXML private TableView<Report> table;
    @FXML private TextArea detailsArea;

    private final ReportDAO dao = new ReportDAO();

    @FXML
    private void initialize() {
        // The reason can be long, so the full text is shown under the table for the selected report
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) ->
                detailsArea.setText(newValue == null ? "" : describe(newValue)));
        loadData();
    }

    private void loadData() {
        table.setItems(FXCollections.observableArrayList(dao.getAll()));
        detailsArea.clear();
    }

    private String describe(Report r) {
        return "Reported by: " + r.getReporterName()
                + "\nReported user: " + r.getReportedUser()
                + "\nListing: " + r.getListingName()
                + "\nStatus: " + r.getStatus() + "    Date: " + r.getCreatedAt()
                + "\n\nReason:\n" + r.getReason();
    }

    @FXML
    private void onResolve() {
        Report r = table.getSelectionModel().getSelectedItem();
        if (r == null) {
            AdminUi.error("Please select a report first.");
            return;
        }
        if (r.getStatus().equals("resolved")) {
            AdminUi.info("This report is already resolved.");
            return;
        }
        if (dao.markResolved(r.getReportId())) {
            AdminUi.info("Report marked as resolved.");
            loadData();
        } else {
            AdminUi.error("Could not update the report. Check the console for details.");
        }
    }
}
