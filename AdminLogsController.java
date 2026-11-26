package com.template.controllers;

import com.template.dao.AdminLogDAO;
import com.template.model.AdminLog;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;

/** Screen: admin_logs.fxml. Table: admin_logs (read only). */
public class AdminLogsController extends AdminScreenController {

    @FXML private TableView<AdminLog> table;

    private final AdminLogDAO dao = new AdminLogDAO();

    @FXML
    private void initialize() {
        loadData();
    }

    private void loadData() {
        table.setItems(FXCollections.observableArrayList(dao.getAll()));
    }

    @FXML
    private void onRefresh() {
        loadData();
    }
}
