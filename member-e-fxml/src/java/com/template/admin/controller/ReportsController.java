package com.template.admin.controller;

import com.template.admin.dao.ReportDAO;
import com.template.admin.model.Report;
import com.template.admin.util.AdminUi;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

/** Screen: admin_reports.fxml. Table: reports. */
public class ReportsController extends AdminScreenController {

    @FXML private TableView<Report> table;

    private final ReportDAO dao = new ReportDAO();

    @FXML
    private void initialize() {
        loadData();
    }

    private void loadData() {
        table.setItems(FXCollections.observableArrayList(dao.getAll()));
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
