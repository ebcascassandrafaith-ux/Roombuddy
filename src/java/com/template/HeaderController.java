package com.template;

import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.function.Consumer;

/** The top bar. MainController plugs in what each button does with the setOnXxx methods. */
public class HeaderController {

    private static final String LOGO_RESOURCE = "/com/template/images/logo.png";

    @FXML private ImageView logoView;
    @FXML private Label logoText;
    @FXML private TextField searchField;
    @FXML private Button menuButton;
    @FXML private HBox filterHost;

    private ContextMenu menu;

    private Runnable logoAction;
    private Runnable bellAction;
    private Runnable hostAction;
    private Runnable profileAction;
    private Runnable savedAction;
    private Runnable settingsAction;
    private Runnable helpAction;
    private Consumer<String> searchAction;

    @FXML
    private void initialize() {
        // If resources/com/template/images/logo.png exists it replaces the "RoomBuddy" text.
        URL logo = HeaderController.class.getResource(LOGO_RESOURCE);
        boolean hasLogo = logo != null;
        if (hasLogo) logoView.setImage(new Image(logo.toExternalForm()));
        logoView.setVisible(hasLogo);
        logoView.setManaged(hasLogo);
        logoText.setVisible(!hasLogo);
        logoText.setManaged(!hasLogo);

        // The filter chips appear as soon as the user clicks into the search bar.
        searchField.focusedProperty().addListener((obs, was, focused) -> {
            if (focused) showFilterBar(true);
        });

        menu = new ContextMenu(
                item("View Profile", () -> run(profileAction)),
                item("Saved Listings", () -> run(savedAction)),
                item("Be a Host", () -> run(hostAction)),
                item("Settings", () -> run(settingsAction)),
                item("Help", () -> run(helpAction)));
    }

    public void setOnLogo(Runnable r) { this.logoAction = r; }
    public void setOnBell(Runnable r) { this.bellAction = r; }
    public void setOnBecomeHost(Runnable r) { this.hostAction = r; }
    public void setOnProfile(Runnable r) { this.profileAction = r; }
    public void setOnSaved(Runnable r) { this.savedAction = r; }
    public void setOnSettings(Runnable r) { this.settingsAction = r; }
    public void setOnHelp(Runnable r) { this.helpAction = r; }
    public void setOnSearch(Consumer<String> c) { this.searchAction = c; }

    /** Puts the filter chips under the search bar. Called once by MainController. */
    public void setFilterBar(Node filterBar) { filterHost.getChildren().setAll(filterBar); }

    public void showFilterBar(boolean show) {
        filterHost.setVisible(show);
        filterHost.setManaged(show);
    }

    public String getSearchText() { return searchField.getText().trim(); }

    public void clearSearch() { searchField.clear(); }

    @FXML
    private void onLogoClicked() {
        run(logoAction);
    }

    @FXML
    private void onBell() {
        run(bellAction);
    }

    @FXML
    private void onBecomeHost() {
        run(hostAction);
    }

    @FXML
    private void onMenu() {
        if (menu.isShowing()) {
            menu.hide();
        } else {
            menu.show(menuButton, Side.BOTTOM, 0, 8);
        }
    }

    @FXML
    private void onSearch() {
        if (searchAction != null) searchAction.accept(searchField.getText().trim());
    }

    private static MenuItem item(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(e -> action.run());
        return item;
    }

    private static void run(Runnable r) {
        if (r != null) r.run();
    }
}