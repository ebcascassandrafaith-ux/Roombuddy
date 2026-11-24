package com.template.admin.controller;

import com.template.admin.model.AdminListing;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Form: admin_edit_listing.fxml. It is shown inside a dialog by ManageListingsController.
 * Only the fields the admin may edit are here.
 */
public class EditListingController {

    @FXML private TextField nameField;
    @FXML private ComboBox<String> typeBox;
    @FXML private TextArea descArea;
    @FXML private TextField barangayField;
    @FXML private TextField addressField;
    @FXML private TextField priceField;
    @FXML private ComboBox<String> roomBox;
    @FXML private TextArea rulesArea;

    @FXML
    private void initialize() {
        // Values match the enums in the listings table
        typeBox.getItems().addAll("boarding_house", "dormitory");
        roomBox.getItems().addAll("single", "double", "shared", "bedspace");
    }

    /** Puts the listing's current values into the form. */
    public void fill(AdminListing l) {
        nameField.setText(l.getPropertyName());
        typeBox.setValue(l.getPropertyType());
        descArea.setText(l.getDescription() == null ? "" : l.getDescription());
        barangayField.setText(l.getBarangay());
        addressField.setText(l.getAddress());
        priceField.setText(l.getPriceText());
        roomBox.setValue(l.getRoomType());
        rulesArea.setText(l.getHouseRules() == null ? "" : l.getHouseRules());
    }

    /** @return null when everything is valid, otherwise the message to show */
    public String validate() {
        if (nameField.getText().trim().isEmpty() || barangayField.getText().trim().isEmpty()
                || addressField.getText().trim().isEmpty()) {
            return "Property name, barangay, and address are required.";
        }
        try {
            if (Double.parseDouble(priceField.getText().trim()) < 0) {
                return "Price must be a valid positive number.";
            }
        } catch (NumberFormatException ex) {
            return "Price must be a valid positive number.";
        }
        return null;
    }

    /** Copies the form values into the listing object (call only after validate() returned null). */
    public void applyTo(AdminListing l) {
        l.setPropertyName(nameField.getText().trim());
        l.setPropertyType(typeBox.getValue());
        l.setDescription(descArea.getText().trim());
        l.setBarangay(barangayField.getText().trim());
        l.setAddress(addressField.getText().trim());
        l.setPrice(Double.parseDouble(priceField.getText().trim()));
        l.setRoomType(roomBox.getValue());
        l.setHouseRules(rulesArea.getText().trim());
    }
}
