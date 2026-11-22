package com.template.browse;

import com.template.model.SearchCriteria;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The row of chips under the search bar: Price, Location, Type, Amenities (Figma).
 * Each chip opens a small popup. Whenever a filter changes, onChange runs so the results refresh.
 */
public class FilterBar extends HBox {

    private final Runnable onChange;

    private final Button priceChip = chip("\u20B1 Price");
    private final Button locationChip = chip("Location");
    private final Button typeChip = chip("Type");
    private final Button amenitiesChip = chip("Amenities");
    private final Button clearChip = chip("Clear filters");

    // the current filter values
    private Double minPrice;
    private Double maxPrice;
    private String barangay;
    private String roomType;
    private final Set<String> amenities = new LinkedHashSet<>();

    // what can be picked (loaded from the database by MainController)
    private List<String> barangayOptions = new ArrayList<>();
    private List<String> amenityOptions = new ArrayList<>();

    private ContextMenu openMenu;

    public FilterBar(Runnable onChange) {
        this.onChange = onChange;
        getStyleClass().add("filter-bar");
        setAlignment(Pos.CENTER);
        setSpacing(10);
        setMaxWidth(Region.USE_PREF_SIZE);

        clearChip.getStyleClass().add("filter-clear");
        priceChip.setOnAction(e -> showPriceMenu());
        locationChip.setOnAction(e -> showLocationMenu());
        typeChip.setOnAction(e -> showTypeMenu());
        amenitiesChip.setOnAction(e -> showAmenitiesMenu());
        clearChip.setOnAction(e -> {
            reset();
            onChange.run();
        });

        getChildren().addAll(priceChip, locationChip, typeChip, amenitiesChip, clearChip);
        refreshChips();
    }

    // ------------------------------------------------------------------ used by MainController

    public void setBarangayOptions(List<String> options) { this.barangayOptions = options; }

    public void setAmenityOptions(List<String> options) { this.amenityOptions = options; }

    /** Copies the chosen filters into the search. */
    public void applyTo(SearchCriteria c) {
        c.setMinPrice(minPrice);
        c.setMaxPrice(maxPrice);
        c.setBarangay(barangay);
        c.setRoomType(roomType);
        c.getAmenities().clear();
        c.getAmenities().addAll(amenities);
    }

    /** Clears every filter. Does not run onChange. */
    public void reset() {
        minPrice = null;
        maxPrice = null;
        barangay = null;
        roomType = null;
        amenities.clear();
        refreshChips();
    }

    public boolean hasFilters() {
        return minPrice != null || maxPrice != null || barangay != null || roomType != null || !amenities.isEmpty();
    }

    // ------------------------------------------------------------------ the four popups

    private void showPriceMenu() {
        TextField min = priceField("Min", minPrice);
        TextField max = priceField("Max", maxPrice);
        Label error = new Label();
        error.getStyleClass().add("filter-error");
        setShown(error, false);

        Button apply = new Button("Apply");
        apply.getStyleClass().add("primary-button");
        Button clear = new Button("Clear");
        clear.getStyleClass().add("secondary-button");

        ContextMenu menu = new ContextMenu();
        apply.setOnAction(e -> {
            try {
                Double lo = parsePrice(min.getText());
                Double hi = parsePrice(max.getText());
                if (lo != null && hi != null && lo > hi) {
                    showError(error, "Minimum must not be higher than maximum.");
                    return;
                }
                minPrice = lo;
                maxPrice = hi;
                menu.hide();
                changed();
            } catch (NumberFormatException ex) {
                showError(error, "Enter numbers only, for example 3500.");
            }
        });
        min.setOnAction(e -> apply.fire());
        max.setOnAction(e -> apply.fire());
        clear.setOnAction(e -> {
            minPrice = null;
            maxPrice = null;
            menu.hide();
            changed();
        });

        Label title = new Label("Price per month");
        title.getStyleClass().add("filter-popup-title");
        HBox fields = new HBox(8, min, new Label("to"), max);
        fields.setAlignment(Pos.CENTER_LEFT);
        HBox buttons = new HBox(8, clear, apply);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox(10, title, fields, error, buttons);
        box.getStyleClass().add("filter-popup");
        menu.getItems().add(new CustomMenuItem(box, false));   // false = do not close when clicked inside
        open(menu, priceChip);
    }

    private void showLocationMenu() {
        ContextMenu menu = new ContextMenu();
        ToggleGroup group = new ToggleGroup();

        RadioMenuItem any = new RadioMenuItem("Any location");
        any.setToggleGroup(group);
        any.setSelected(barangay == null);
        any.setOnAction(e -> {
            barangay = null;
            changed();
        });
        menu.getItems().add(any);

        for (String b : barangayOptions) {
            RadioMenuItem item = new RadioMenuItem(b);
            item.setToggleGroup(group);
            item.setSelected(b.equals(barangay));
            item.setOnAction(e -> {
                barangay = b;
                changed();
            });
            menu.getItems().add(item);
        }
        open(menu, locationChip);
    }

    private void showTypeMenu() {
        ContextMenu menu = new ContextMenu();
        ToggleGroup group = new ToggleGroup();

        RadioMenuItem any = new RadioMenuItem("Any type");
        any.setToggleGroup(group);
        any.setSelected(roomType == null);
        any.setOnAction(e -> {
            roomType = null;
            changed();
        });
        menu.getItems().add(any);

        for (Map.Entry<String, String> type : SearchCriteria.ROOM_TYPES.entrySet()) {
            RadioMenuItem item = new RadioMenuItem(type.getValue());
            item.setToggleGroup(group);
            item.setSelected(type.getKey().equals(roomType));
            item.setOnAction(e -> {
                roomType = type.getKey();
                changed();
            });
            menu.getItems().add(item);
        }
        open(menu, typeChip);
    }

    private void showAmenitiesMenu() {
        Label title = new Label("Must have all of these");
        title.getStyleClass().add("filter-popup-title");

        VBox list = new VBox(8);
        List<CheckBox> boxes = new ArrayList<>();
        for (String name : amenityOptions) {
            CheckBox cb = new CheckBox(name);
            cb.setSelected(amenities.contains(name));
            boxes.add(cb);
            list.getChildren().add(cb);
        }
        if (boxes.isEmpty()) list.getChildren().add(new Label("No amenities available yet."));

        Button apply = new Button("Apply");
        apply.getStyleClass().add("primary-button");
        Button clear = new Button("Clear");
        clear.getStyleClass().add("secondary-button");
        HBox buttons = new HBox(8, clear, apply);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        ContextMenu menu = new ContextMenu();
        apply.setOnAction(e -> {
            amenities.clear();
            for (CheckBox cb : boxes) {
                if (cb.isSelected()) amenities.add(cb.getText());
            }
            menu.hide();
            changed();
        });
        clear.setOnAction(e -> {
            amenities.clear();
            menu.hide();
            changed();
        });

        VBox box = new VBox(10, title, list, buttons);
        box.getStyleClass().add("filter-popup");
        menu.getItems().add(new CustomMenuItem(box, false));
        open(menu, amenitiesChip);
    }

    // ------------------------------------------------------------------ helpers

    private void changed() {
        refreshChips();
        onChange.run();
    }

    private void open(ContextMenu menu, Button anchor) {
        if (openMenu != null) openMenu.hide();
        openMenu = menu;
        menu.show(anchor, Side.BOTTOM, 0, 6);
    }

    /** Updates each chip's text and highlights the chips that have a filter set. */
    private void refreshChips() {
        boolean priceSet = minPrice != null || maxPrice != null;
        priceChip.setText(priceSet ? SearchCriteria.priceRangeText(minPrice, maxPrice) : "\u20B1 Price");
        setActive(priceChip, priceSet);

        locationChip.setText(barangay == null ? "Location" : barangay);
        setActive(locationChip, barangay != null);

        typeChip.setText(roomType == null ? "Type" : SearchCriteria.ROOM_TYPES.getOrDefault(roomType, roomType));
        setActive(typeChip, roomType != null);

        amenitiesChip.setText(amenities.isEmpty() ? "Amenities" : "Amenities (" + amenities.size() + ")");
        setActive(amenitiesChip, !amenities.isEmpty());

        setShown(clearChip, hasFilters());
    }

    private static Button chip(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("filter-chip");
        return b;
    }

    private static void setActive(Button chip, boolean active) {
        chip.getStyleClass().remove("filter-chip-active");
        if (active) chip.getStyleClass().add("filter-chip-active");
    }

    private static TextField priceField(String prompt, Double value) {
        TextField f = new TextField(value == null ? "" : String.format("%.0f", value));
        f.setPromptText(prompt);
        f.setPrefColumnCount(7);
        f.getStyleClass().add("filter-field");
        return f;
    }

    /** "3,500" / "₱3500" / "" -> number or null. Throws NumberFormatException for anything else. */
    private static Double parsePrice(String text) {
        String t = text == null ? "" : text.replace("\u20B1", "").replace(",", "").trim();
        if (t.isEmpty()) return null;
        double v = Double.parseDouble(t);
        if (v < 0 || Double.isNaN(v) || Double.isInfinite(v)) throw new NumberFormatException("bad price");
        return v;
    }

    private static void showError(Label label, String text) {
        label.setText(text);
        setShown(label, true);
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }
}
