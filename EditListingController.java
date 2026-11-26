package com.template.controllers;

import com.template.model.AdminListing;
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
        String name = nameField.getText().trim();
        String barangay = barangayField.getText().trim();
        String address = addressField.getText().trim();

        if (name.isEmpty() || barangay.isEmpty() || address.isEmpty()) {
            return "Property name, barangay, and address are required.";
        }
        // The columns are VARCHAR(100), VARCHAR(100) and VARCHAR(255): longer text would make the save fail
        if (name.length() > 100 || barangay.length() > 100) {
            return "Property name and barangay must be 100 characters or fewer.";
        }
        if (address.length() > 255) {
            return "Address must be 255 characters or fewer.";
        }
        if (typeBox.getValue() == null || roomBox.getValue() == null) {
            return "Please choose a property type and a room type.";
        }
        try {
            double price = Double.parseDouble(priceField.getText().trim());
            if (price < 0 || price > 99999999.99 || Double.isNaN(price)) {
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
