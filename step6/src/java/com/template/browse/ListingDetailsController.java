package com.template.browse;

import com.template.dao.ListingDAO;
import com.template.dao.RoomDAO;
import com.template.dao.SavedListingDAO;
import com.template.model.Amenity;
import com.template.model.Listing;
import com.template.model.ListingDetails;
import com.template.model.Room;
import com.template.util.Async;
import com.template.util.FxView;
import com.template.util.Images;
import com.template.util.Session;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The details page of one ROOM of a house (flowchart: "Display full listing details w/ chat, schedule viewing,
 * bookmark button"). The house page (PropertyController) opens it through Navigator.openRoom(...).
 * Host, amenities and house rules belong to the house; name, price, photos and the advance / deposit belong to the room.
 */
public class ListingDetailsController {

    private static final String CARD_FXML = "/com/template/browse/listing_card.fxml";
    private static final double GALLERY_HEIGHT = 360;
    private static final double GALLERY_GAP = 14;
    private static final double PAGE_SIDE_PADDING = 88;   // 44 + 44, see .page-content in style.css
    private static final double CARD_SIZE = 210;
    private static final int MORE_OPTIONS = 4;

    @FXML private ScrollPane pageScroll;
    @FXML private Label statusLabel;
    @FXML private VBox detailsBox;

    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label addressLabel;
    @FXML private HBox gallery;

    @FXML private StackPane hostAvatar;
    @FXML private Label hostLabel;
    @FXML private Label hostBadge;
    @FXML private Label hostingLabel;
    @FXML private VBox amenitiesBox;
    @FXML private Label descriptionLabel;

    @FXML private Label priceLabel;
    @FXML private Label advanceLabel;
    @FXML private Label advanceUnit;
    @FXML private Label depositLabel;
    @FXML private Label depositUnit;
    @FXML private Label ownerNote;
    @FXML private Button messageButton;
    @FXML private Button viewingButton;
    @FXML private Button saveButton;
    @FXML private Region bookmarkIcon;

    @FXML private VBox rulesBox;
    @FXML private VBox moreSection;
    @FXML private FlowPane moreGrid;
    @FXML private Label toastLabel;

    private final ListingDAO listingDAO = new ListingDAO();
    private final RoomDAO roomDAO = new RoomDAO();
    private final SavedListingDAO savedDAO = new SavedListingDAO();
    private final PauseTransition toastTimer = new PauseTransition(Duration.seconds(2));

    private Navigator navigator;
    private ListingDetails details;   // the house
    private Room room;                 // the room being shown
    private boolean saved;

    private final List<PhotoTile> tiles = new ArrayList<>();
    private int galleryMode = 1;          // 1 = one big tile, 2 = big + one, 3 = big + two stacked
    private double galleryWidth = 0;

    /** What the background thread brings back. */
    private static final class Loaded {
        final ListingDetails details;   // null = house or room is gone / hidden / occupied
        final Room room;
        final boolean saved;
        final List<Listing> more;

        Loaded(ListingDetails details, Room room, boolean saved, List<Listing> more) {
            this.details = details;
            this.room = room;
            this.saved = saved;
            this.more = more;
        }
    }

    @FXML
    private void initialize() {
        toastTimer.setOnFinished(e -> toastLabel.setVisible(false));
        // Keep the photo gallery matched to the window width.
        pageScroll.viewportBoundsProperty().addListener((obs, old, now) ->
                layoutGallery(now.getWidth() - PAGE_SIDE_PADDING));
    }

    /** Called once by MainController right after the FXML is loaded. */
    public void show(Listing listing, Room roomToShow, Navigator navigator) {
        this.navigator = navigator;
        final int listingId = listing.getListingId();
        final int roomId = roomToShow.getRoomId();
        final int userId = Session.getUserId();

        setShown(detailsBox, false);
        showStatus("Loading room...");

        Async.run(() -> {
                    ListingDetails d = listingDAO.findDetails(listingId);
                    if (d == null || !d.isAvailable()) {
                        return new Loaded(null, null, false, new ArrayList<Listing>());
                    }
                    // roomId 0 = an older listing without rows in `rooms`: the house itself is the room
                    Room r = roomId > 0 ? roomDAO.findRoom(roomId) : Room.fromListing(d);
                    if (r == null || !r.isAvailable()) {
                        return new Loaded(null, null, false, new ArrayList<Listing>());
                    }
                    boolean isSaved = Session.isLoggedIn() && savedDAO.isSaved(userId, listingId);
                    return new Loaded(d, r, isSaved, listingDAO.findMoreOptions(d, MORE_OPTIONS));
                },
                this::render,
                err -> showStatus("Could not load this room. Check your database connection.\n("
                        + err.getMessage() + ")"));
    }

    // ------------------------------------------------------------------ drawing the page

    private void render(Loaded loaded) {
        if (loaded.details == null) {
            // Flowchart: "This listing is no longer available"
            showStatus("This room is no longer available.");
            return;
        }
        details = loaded.details;
        room = loaded.room;
        saved = loaded.saved;

        hideStatus();
        setShown(detailsBox, true);

        titleLabel.setText(room.getRoomName());
        subtitleLabel.setText(details.getPropertyName() + "  \u2022  " + room.getRoomTypeText());
        addressLabel.setText(details.getAddress());

        // the room's own photos; if the owner has not added any, the house's photos
        buildGallery(room.getPhotos().isEmpty() ? details.getPhotos() : room.getPhotos());
        buildHost();
        buildAmenities();

        descriptionLabel.setText(descriptionText());

        priceLabel.setText(String.format(Locale.US, "\u20B1%,.0f", room.getPrice()));
        advanceLabel.setText(String.valueOf(room.getAdvanceMonths()));
        advanceUnit.setText(room.getAdvanceMonths() == 1 ? "month advance" : "months advance");
        depositLabel.setText(String.valueOf(room.getDepositMonths()));
        depositUnit.setText(room.getDepositMonths() == 1 ? "month deposit" : "months deposit");

        // You cannot message or book a viewing on your own listing.
        boolean ownListing = Session.isLoggedIn() && Session.getUserId() == details.getOwnerId();
        setShown(messageButton, !ownListing);
        setShown(viewingButton, !ownListing);
        setShown(ownerNote, ownListing);
        updateSaveButton();

        buildRules();
        buildMoreOptions(loaded.more);

        pageScroll.setVvalue(0);
    }

    /** The room's own description first, then the house's description. */
    private String descriptionText() {
        String roomDesc = room.getDescription() == null ? "" : room.getDescription().trim();
        String houseDesc = details.getDescription() == null ? "" : details.getDescription().trim();
        if (roomDesc.isEmpty() && houseDesc.isEmpty()) return "The host has not added a description.";
        if (roomDesc.isEmpty()) return houseDesc;
        if (houseDesc.isEmpty()) return roomDesc;
        return roomDesc + "\n\n" + houseDesc;
    }

    private void buildGallery(List<String> photos) {
        gallery.getChildren().clear();
        tiles.clear();
        int n = photos.size();

        if (n <= 1) {
            PhotoTile only = new PhotoTile(n == 1 ? photos.get(0) : null);
            tiles.add(only);
            gallery.getChildren().add(only);
            galleryMode = 1;
        } else {
            PhotoTile big = new PhotoTile(photos.get(0));
            PhotoTile second = new PhotoTile(photos.get(1));
            tiles.add(big);
            tiles.add(second);
            gallery.getChildren().add(big);
            if (n == 2) {
                gallery.getChildren().add(second);
                galleryMode = 2;
            } else {
                PhotoTile third = new PhotoTile(photos.get(2));
                if (n > 3) third.setMoreCount(n - 3);
                tiles.add(third);
                VBox side = new VBox(GALLERY_GAP, second, third);
                gallery.getChildren().add(side);
                galleryMode = 3;
            }
        }
        layoutGallery(galleryWidth);
    }

    /** Sizes the tiles so the whole gallery is `total` pixels wide. */
    private void layoutGallery(double total) {
        if (total <= 0) total = 800;                       // window not measured yet
        total = Math.max(600, Math.min(total, 1200));
        galleryWidth = total;
        if (tiles.isEmpty()) return;

        double bigWidth = (total - GALLERY_GAP) * 0.45;
        double sideWidth = total - GALLERY_GAP - bigWidth;
        double halfHeight = (GALLERY_HEIGHT - GALLERY_GAP) / 2;

        switch (galleryMode) {
            case 1:
                tiles.get(0).fit(total, GALLERY_HEIGHT);
                break;
            case 2:
                tiles.get(0).fit(bigWidth, GALLERY_HEIGHT);
                tiles.get(1).fit(sideWidth, GALLERY_HEIGHT);
                break;
            default:
                tiles.get(0).fit(bigWidth, GALLERY_HEIGHT);
                tiles.get(1).fit(sideWidth, halfHeight);
                tiles.get(2).fit(sideWidth, halfHeight);
        }
    }

    private void buildHost() {
        String name = details.getHostName() == null ? "the host" : details.getHostName();
        hostLabel.setText("Hosted by " + name);
        setShown(hostBadge, details.isHostVerified());
        hostingLabel.setText(details.getHostingText());

        hostAvatar.getChildren().clear();
        Label initial = new Label(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        initial.getStyleClass().add("host-initial");
        hostAvatar.getChildren().add(initial);

        Image photo = Images.load(details.getHostPhoto(), 120, 120);
        if (photo != null) {
            ImageView view = new ImageView();
            view.setFitWidth(56);
            view.setFitHeight(56);
            view.setClip(new Circle(28, 28, 28));
            Images.showSquareCrop(view, photo);
            hostAvatar.getChildren().add(view);
        }
    }

    private void buildAmenities() {
        amenitiesBox.getChildren().clear();
        if (details.getAmenities().isEmpty()) {
            Label none = new Label("The host has not listed any amenities.");
            none.getStyleClass().add("muted-text");
            amenitiesBox.getChildren().add(none);
            return;
        }
        for (Amenity a : details.getAmenities()) {
            Region icon = new Region();
            icon.getStyleClass().addAll("amenity-icon", iconClassFor(a.getName()));

            Label name = new Label(a.getName());
            name.getStyleClass().add("amenity-name");
            Label cost = new Label(a.getCostText());
            cost.getStyleClass().add("muted-text");

            HBox row = new HBox(14, icon, new VBox(1, name, cost));
            row.setAlignment(Pos.CENTER_LEFT);
            amenitiesBox.getChildren().add(row);
        }
    }

    private static String iconClassFor(String amenityName) {
        String n = amenityName == null ? "" : amenityName.toLowerCase();
        if (n.contains("water")) return "amenity-water";
        if (n.contains("electric")) return "amenity-electric";
        if (n.contains("wifi")) return "amenity-wifi";
        if (n.contains("security") || n.contains("cctv")) return "amenity-security";
        return "amenity-default";
    }

    private void buildRules() {
        rulesBox.getChildren().clear();
        List<String> rules = details.getHouseRuleLines();
        if (rules.isEmpty()) {
            Label none = new Label("The host has not listed any house rules.");
            none.getStyleClass().add("muted-text");
            rulesBox.getChildren().add(none);
            return;
        }
        for (String rule : rules) {
            Label l = new Label("\u2022  " + rule);
            l.setWrapText(true);
            l.getStyleClass().add("body-text");
            rulesBox.getChildren().add(l);
        }
    }

    private void buildMoreOptions(List<Listing> more) {
        moreGrid.getChildren().clear();
        for (Listing l : more) {
            FxView<ListingCardController> card = FxView.load(CARD_FXML);
            card.getController().setData(l, navigator, CARD_SIZE);
            moreGrid.getChildren().add(card.getRoot());
        }
        setShown(moreSection, !more.isEmpty());
    }

    private void updateSaveButton() {
        saveButton.setText(saved ? "Saved" : "Save");
        bookmarkIcon.getStyleClass().removeAll("icon-bookmark", "icon-bookmark-filled");
        bookmarkIcon.getStyleClass().add(saved ? "icon-bookmark-filled" : "icon-bookmark");
    }

    // ------------------------------------------------------------------ button actions

    @FXML
    private void onBack() {
        if (navigator != null) navigator.goBack();
    }

    @FXML
    private void onMoreOptions() {
        if (navigator != null) navigator.goHome();
    }

    /** Flowchart: chat -> "is ID verified?" -> No: "Verify ID to continue" / Yes: proceed. */
    @FXML
    private void onMessageHost() {
        if (details != null && navigator != null && requireVerifiedUser()) {
            navigator.openChat(details);
        }
    }

    @FXML
    private void onScheduleViewing() {
        if (details != null && navigator != null && requireVerifiedUser()) {
            navigator.openScheduleViewing(details);
        }
    }

    /** Flowchart: add/remove from Saved_Listings, fill/unfill the icon, show "Added to / Removed from saved". */
    @FXML
    private void onToggleSave() {
        if (details == null) return;
        if (!Session.isLoggedIn()) {
            info("Please log in", "Log in to save listings.");
            return;
        }
        final int userId = Session.getUserId();
        final int listingId = details.getListingId();

        saveButton.setDisable(true);
        Async.run(() -> savedDAO.toggle(userId, listingId),
                nowSaved -> {
                    saved = nowSaved;
                    updateSaveButton();
                    saveButton.setDisable(false);
                    toast(nowSaved ? "Added to saved listings" : "Removed from saved listings");
                },
                err -> {
                    saveButton.setDisable(false);
                    toast("Could not update saved listings.");
                });
    }

    /** @return true if the user may continue (logged in and ID verified). */
    private boolean requireVerifiedUser() {
        if (!Session.isLoggedIn()) {
            info("Please log in", "Log in to message hosts or schedule a viewing.");
            return false;
        }
        if (!Session.isIdVerified()) {
            ButtonType verify = new ButtonType("Verify ID", ButtonBar.ButtonData.OK_DONE);
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                    "You need a verified ID before you can message hosts or schedule a viewing.",
                    verify, ButtonType.CANCEL);
            alert.setTitle("RoomBuddy");
            alert.setHeaderText("Verify ID to continue");
            alert.showAndWait().ifPresent(choice -> {
                if (choice == verify) navigator.openIdVerification();
            });
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ small helpers

    private void info(String header, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("RoomBuddy");
        alert.setHeaderText(header);
        alert.setContentText(text);
        alert.show();
    }

    private void toast(String text) {
        toastLabel.setText(text);
        toastLabel.setVisible(true);
        toastTimer.playFromStart();
    }

    private void showStatus(String text) {
        statusLabel.setText(text);
        setShown(statusLabel, true);
    }

    private void hideStatus() {
        setShown(statusLabel, false);
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }
}
