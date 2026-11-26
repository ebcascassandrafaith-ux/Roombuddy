package com.template;

import com.template.browse.FilterBar;
import com.template.browse.HomeController;
import com.template.browse.ListingDetailsController;
import com.template.browse.Navigator;
import com.template.browse.PropertyController;
import com.template.browse.SavedListingsController;
import com.template.browse.SearchResultsController;
import com.template.browse.SeeAllController;
import com.template.communication.ChatController;
import com.template.communication.NotificationsController;
import com.template.communication.ScheduleViewingController;
import com.template.owner.MyListingsController;
import com.template.dao.ListingDAO;
import com.template.model.Listing;
import com.template.model.ListingGroup;
import com.template.model.Room;
import com.template.model.SearchCriteria;
import com.template.util.Async;
import com.template.util.FxView;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;
import com.template.util.Session;
import javafx.scene.control.ButtonType;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * The main window. It owns the header and the content area, decides which screen is showing,
 * and decides what every header button does.
 */
public class MainController implements Navigator {

    private static final String HOME_FXML    = "/com/template/browse/home_view.fxml";
    private static final String SEE_ALL_FXML = "/com/template/browse/see_all_view.fxml";
    private static final String PROPERTY_FXML = "/com/template/browse/property_view.fxml";   // one house + its rooms
    private static final String DETAILS_FXML = "/com/template/browse/listing_details_view.fxml";   // one room
    private static final String SEARCH_FXML  = "/com/template/browse/search_results_view.fxml";
    private static final String SAVED_FXML   = "/com/template/browse/saved_listings_view.fxml";
    private static final String CHAT_FXML = "/com/template/communication/chat_view.fxml";
    private static final String SCHEDULE_FXML = "/com/template/communication/schedule_viewing_view.fxml";
    private static final String NOTIFICATIONS_FXML = "/com/template/communication/notification_view.fxml";
    private static final String MY_LISTINGS_FXML = "/com/template/owner/my_listings_view.fxml";

    // injected from <fx:include fx:id="header" .../> in main.fxml  (fx:id + "Controller")
    @FXML private HeaderController headerController;
    @FXML private StackPane contentArea;

    // Home is built once and reused, so it keeps its scroll position when you come back from "see all".
    private FxView<HomeController> homeView;

    private final ListingDAO listingDAO = new ListingDAO();
    private FilterBar filterBar;                          // the Price / Location / Type / Amenities chips
    private FxView<SearchResultsController> searchView;   // built once, reused for every search
    private FxView<SavedListingsController> savedView;    // built once, reloaded each time it opens

    // The screens the user came from, newest on top. goBack() shows the top one again.
    private final Deque<Node> history = new ArrayDeque<>();

    @FXML
    private void initialize() {
        filterBar = new FilterBar(this::runSearch);   // any filter change re-runs the search
        headerController.setFilterBar(filterBar);
        loadFilterOptions();
        wireHeader();
        showHome();
    }

    /** Decide what each header button does. Other members: replace the comingSoon(...) lines with your screen. */
    private void wireHeader() {
        headerController.setOnLogo(this::goHome);
        headerController.setOnSearch(query -> runSearch());
        headerController.setOnBell(this::showNotifications);
        headerController.setOnBecomeHost(this::showMyListings);
        headerController.setOnProfile(() -> comingSoon("Profile", "Member A"));
        headerController.setOnSaved(this::showSaved);
        headerController.setOnSettings(() -> comingSoon("Settings", "Member A"));
        headerController.setOnHelp(() -> comingSoon("Help", "the team"));
        headerController.setOnLogout(this::logout);
    }

    public void showHome() {
        if (homeView == null) {
            homeView = FxView.load(HOME_FXML);
            homeView.getController().start(this);   // first load of listings
        } else {
            homeView.getController().load();        // reload so new, occupied or deleted listings are correct
            loadFilterOptions();                    // reload the Location and Amenities choices too
        }
        history.clear();                            // home is the start: nothing to go back to
        resetSearchUi();
        display(homeView.getRoot());
    }

    @Override
    public void goHome() {
        showHome();
    }

    @Override
    public void showSeeAll(ListingGroup group) {
        FxView<SeeAllController> view = FxView.load(SEE_ALL_FXML);
        view.getController().show(group, this);
        remember();
        display(view.getRoot());
    }

    /** A listing card was clicked: show the house with its rooms (Figma "view listing"). */
    @Override
    public void openListing(Listing listing) {
        FxView<PropertyController> view = FxView.load(PROPERTY_FXML);
        view.getController().show(listing, this);
        remember();
        display(view.getRoot());
    }

    /** "Click to view room details": show one room of the house (Figma "view details"). */
    @Override
    public void openRoom(Listing listing, Room room) {
        FxView<ListingDetailsController> view = FxView.load(DETAILS_FXML);
        view.getController().show(listing, room, this);
        remember();
        display(view.getRoot());
    }

    @Override
    public void goBack() {
        if (history.isEmpty()) {
            goHome();
        } else {
            Node previous = history.pop();
            display(previous);
            if (homeView != null && previous == homeView.getRoot()) resetSearchUi();
            // you may have un-bookmarked something on the details page: refresh the list
            if (savedView != null && previous == savedView.getRoot()) savedView.getController().reload();
        }
    }

    // ---- screens owned by other members: replace each comingSoon(...) line with their screen ----

    @Override
    public void openChat(Listing listing) {
        FxView<ChatController> view = FxView.load(CHAT_FXML);
        view.getController().show(listing, this);
        remember();
        display(view.getRoot());
    }

    @Override
    public void openScheduleViewing(Listing listing) {
        FxView<ScheduleViewingController> view = FxView.load(SCHEDULE_FXML);
        view.getController().show(listing, this);
        remember();
        display(view.getRoot());
    }

    @Override
    public void openIdVerification() {
        comingSoon("ID verification", "Member A");
    }

    // ---- saved / bookmarks (Step 4) ----

    /** Header menu -> "Saved Listings". Flowchart: "Query DB for saved listings". */
    public void showSaved() {
        if (savedView == null) savedView = FxView.load(SAVED_FXML);
        headerController.showFilterBar(false);

        Node root = savedView.getRoot();
        if (!contentArea.getChildren().contains(root)) {
            remember();
            display(root);
        }
        savedView.getController().show(this);
    }

    /** Member D: inbox notifications (new messages and viewing updates). */
    public void showNotifications() {
        FxView<NotificationsController> view = FxView.load(NOTIFICATIONS_FXML);
        view.getController().show(this);
        remember();
        display(view.getRoot());
    }

    /** Member B: the landlord's own listings (Become a Host). */
    public void showMyListings() {
        FxView<MyListingsController> view = FxView.load(MY_LISTINGS_FXML);
        view.getController().setNavigator(this);
        headerController.showFilterBar(false);
        remember();
        display(view.getRoot());
    }

    // ---- search + filters (Step 2) ----

    /**
     * Runs the search with the typed keywords and the chosen filters.
     * Called when the user presses Enter / the search button, and again whenever a filter chip changes.
     */
    private void runSearch() {
        SearchCriteria criteria = new SearchCriteria();
        criteria.setKeyword(headerController.getSearchText());
        filterBar.applyTo(criteria);

        headerController.showFilterBar(true);
        if (searchView == null) searchView = FxView.load(SEARCH_FXML);

        Node root = searchView.getRoot();
        if (!contentArea.getChildren().contains(root)) {   // already on the results: just refresh them
            remember();
            display(root);
        }
        searchView.getController().search(criteria, this);
    }

    /** Fills the Location and Amenities chips with what exists in the database. */
    private void loadFilterOptions() {
        Async.run(listingDAO::findBarangays, filterBar::setBarangayOptions, null);
        Async.run(listingDAO::findAmenityNames, filterBar::setAmenityOptions, null);
    }

    /** Back on the home screen: empty search bar, no filters, chips hidden. */
    private void resetSearchUi() {
        filterBar.reset();
        headerController.clearSearch();
        headerController.showFilterBar(false);
    }

    // Header menu -> "Log Out"
    private void logout()
    {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to log out?");
        confirm.setHeaderText(null);

        if (confirm.showAndWait().get() == ButtonType.OK)
        {
            Session.logout();
            Main.changeScene("login.fxml", 500, 600);
        }
    }

    // ---- helpers ----

    /** Puts the screen that is showing now on the back stack, so goBack() can return to it. */
    private void remember() {
        if (!contentArea.getChildren().isEmpty()) {
            history.push(contentArea.getChildren().get(0));
        }
    }

    private void display(Node view) {
        contentArea.getChildren().setAll(view);
    }

    /** Temporary popup for screens that do not exist yet. */
    private void comingSoon(String feature, String owner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("RoomBuddy");
        alert.setHeaderText(feature);
        alert.setContentText("Not connected yet. This belongs to " + owner + ".");
        alert.show();
    }
}