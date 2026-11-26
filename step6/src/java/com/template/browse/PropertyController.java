package com.template.browse;

import com.template.dao.ListingDAO;
import com.template.dao.RoomDAO;
import com.template.model.Listing;
import com.template.model.ListingDetails;
import com.template.model.Room;
import com.template.util.Async;
import com.template.util.Images;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The page of one HOUSE (Figma "view listing"): the photo collage, the list of its rooms
 * ("Click to view room details") and who hosts it. MainController opens it through Navigator.openListing(...);
 * a room card opens the room page (ListingDetailsController) through Navigator.openRoom(...).
 */
public class PropertyController {

    private static final double GALLERY_HEIGHT = 300;
    private static final double GALLERY_GAP = 12;
    private static final double SIDE_PADDING = 88 + 56;   // page padding (44 + 44) + blue card padding (28 + 28)

    @FXML private ScrollPane pageScroll;
    @FXML private Label statusLabel;
    @FXML private VBox detailsBox;

    @FXML private Label titleLabel;
    @FXML private HBox gallery;
    @FXML private Label headlineLabel;
    @FXML private Label factsLabel;
    @FXML private Label addressLabel;

    @FXML private VBox roomsBox;

    @FXML private Label hostedByLabel;
    @FXML private StackPane hostAvatar;
    @FXML private Label hostLabel;
    @FXML private Label hostBadge;
    @FXML private Label hostingLabel;

    private final ListingDAO listingDAO = new ListingDAO();
    private final RoomDAO roomDAO = new RoomDAO();

    private Navigator navigator;
    private ListingDetails details;

    private final List<PhotoTile> tiles = new ArrayList<>();
    private int galleryMode = 1;     // 1 = one tile, 2 = big + one, 3 = big + two stacked, 4 = big + 2x2
    private double galleryWidth = 0;

    /** What the background thread brings back. */
    private static final class Loaded {
        final ListingDetails details;   // null = house is gone / hidden / fully occupied
        final List<Room> rooms;

        Loaded(ListingDetails details, List<Room> rooms) {
            this.details = details;
            this.rooms = rooms;
        }
    }

    @FXML
    private void initialize() {
        pageScroll.viewportBoundsProperty().addListener((obs, old, now) ->
                layoutGallery(now.getWidth() - SIDE_PADDING));
    }

    /** Called by MainController right after the FXML is loaded. */
    public void show(Listing listing, Navigator navigator) {
        this.navigator = navigator;
        final int listingId = listing.getListingId();

        setShown(detailsBox, false);
        showStatus("Loading listing...");

        Async.run(() -> {
                    ListingDetails d = listingDAO.findDetails(listingId);
                    if (d == null || !d.isAvailable()) {
                        return new Loaded(null, new ArrayList<Room>());
                    }
                    List<Room> rooms = roomDAO.findRooms(listingId);
                    if (rooms.isEmpty()) {
                        // an older listing without rows in `rooms`: show the house as one room
                        rooms = new ArrayList<>();
                        rooms.add(Room.fromListing(d));
                    }
                    return new Loaded(d, rooms);
                },
                this::render,
                err -> showStatus("Could not load this listing. Check your database connection.\n("
                        + err.getMessage() + ")"));
    }

    // ------------------------------------------------------------------ drawing the page

    private void render(Loaded loaded) {
        if (loaded.details == null) {
            showStatus("This listing is no longer available.");   // flowchart wording
            return;
        }
        details = loaded.details;
        setShown(statusLabel, false);
        setShown(detailsBox, true);

        titleLabel.setText(details.getPropertyName());
        headlineLabel.setText(details.getPropertyTypeText() + " in " + details.getBarangay() + ", CDO");
        factsLabel.setText(factsText(loaded.rooms));
        addressLabel.setText(details.getAddress());

        buildGallery(details.getPhotos());
        buildRooms(loaded.rooms);
        buildHost();
        pageScroll.setVvalue(0);
    }

    /** "3 rooms  •  from ₱3,500 per month" */
    private static String factsText(List<Room> rooms) {
        int available = 0;
        double cheapest = Double.MAX_VALUE;
        for (Room r : rooms) {
            if (r.isAvailable()) {
                available++;
                cheapest = Math.min(cheapest, r.getPrice());
            }
        }
        String count = rooms.size() + (rooms.size() == 1 ? " room" : " rooms");
        if (available == 0) return count + "  \u2022  no rooms available right now";
        return count + "  \u2022  from " + String.format(Locale.US, "\u20B1%,.0f", cheapest) + " per month";
    }

    // ---- the blue card's photo collage ----

    private void buildGallery(List<String> photos) {
        gallery.getChildren().clear();
        tiles.clear();
        int n = photos.size();

        if (n <= 1) {
            PhotoTile only = tile(n == 1 ? photos.get(0) : null);
            gallery.getChildren().add(only);
            galleryMode = 1;
        } else if (n == 2) {
            gallery.getChildren().addAll(tile(photos.get(0)), tile(photos.get(1)));
            galleryMode = 2;
        } else if (n <= 4) {                                   // big + two stacked
            PhotoTile big = tile(photos.get(0));
            PhotoTile second = tile(photos.get(1));
            PhotoTile third = tile(photos.get(2));
            if (n == 4) third.setMoreCount(1);
            gallery.getChildren().addAll(big, new VBox(GALLERY_GAP, second, third));
            galleryMode = 3;
        } else {                                               // big + 2 x 2
            PhotoTile big = tile(photos.get(0));
            PhotoTile a = tile(photos.get(1));
            PhotoTile b = tile(photos.get(2));
            PhotoTile c = tile(photos.get(3));
            PhotoTile d = tile(photos.get(4));
            if (n > 5) d.setMoreCount(n - 5);
            gallery.getChildren().addAll(big, new VBox(GALLERY_GAP,
                    new HBox(GALLERY_GAP, a, b), new HBox(GALLERY_GAP, c, d)));
            galleryMode = 4;
        }
        layoutGallery(galleryWidth);
    }

    private PhotoTile tile(String path) {
        PhotoTile t = new PhotoTile(path);
        tiles.add(t);
        return t;
    }

    private void layoutGallery(double total) {
        if (total <= 0) total = 760;                        // window not measured yet
        total = Math.max(560, Math.min(total, 1100));
        galleryWidth = total;
        if (tiles.isEmpty()) return;

        double bigWidth = (total - GALLERY_GAP) * (galleryMode == 4 ? 0.40 : 0.45);
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
            case 3:
                tiles.get(0).fit(bigWidth, GALLERY_HEIGHT);
                tiles.get(1).fit(sideWidth, halfHeight);
                tiles.get(2).fit(sideWidth, halfHeight);
                break;
            default:
                double cell = (sideWidth - GALLERY_GAP) / 2;
                tiles.get(0).fit(bigWidth, GALLERY_HEIGHT);
                for (int i = 1; i < tiles.size(); i++) tiles.get(i).fit(cell, halfHeight);
        }
    }

    // ---- the room cards ----

    private void buildRooms(List<Room> rooms) {
        roomsBox.getChildren().clear();
        for (Room room : rooms) {
            roomsBox.getChildren().add(roomCard(room));
        }
    }

    /** Figma: photo on the left; name, price, AVAILABLE and "Click to view room details" on the right. */
    private Node roomCard(Room room) {
        String photoPath = room.getCoverPhoto() != null ? room.getCoverPhoto() : firstHousePhoto();
        PhotoTile photo = new PhotoTile(photoPath);
        photo.fit(230, 140);

        Label name = new Label(room.getRoomName());
        name.getStyleClass().add("room-name");
        Label price = new Label(room.getPriceText() + " per month");
        price.getStyleClass().add("room-price");
        Label status = new Label(room.isAvailable() ? "AVAILABLE" : "OCCUPIED");
        status.getStyleClass().add(room.isAvailable() ? "available-text" : "occupied-text");

        Button open = new Button(room.isAvailable() ? "Click to view room details" : "Not available");
        open.getStyleClass().add("room-button");
        open.setDisable(!room.isAvailable());
        open.setMaxWidth(Double.MAX_VALUE);
        open.setOnAction(e -> navigator.openRoom(details, room));

        VBox info = new VBox(6, name, price, status, open);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox card = new HBox(26, photo, info);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("room-card");
        if (room.isAvailable()) {
            card.setOnMouseClicked(e -> navigator.openRoom(details, room));
        }
        return card;
    }

    private String firstHousePhoto() {
        return details.getPhotos().isEmpty() ? null : details.getPhotos().get(0);
    }

    // ---- "<house> is hosted by:" ----

    private void buildHost() {
        String name = details.getHostName() == null ? "the host" : details.getHostName();
        hostedByLabel.setText(details.getPropertyName() + " is hosted by:");
        hostLabel.setText(name);
        setShown(hostBadge, details.isHostVerified());
        hostingLabel.setText(details.getHostingText());

        hostAvatar.getChildren().clear();
        Label initial = new Label(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        initial.getStyleClass().add("host-initial");
        hostAvatar.getChildren().add(initial);

        Image photo = Images.load(details.getHostPhoto(), 160, 160);
        if (photo != null) {
            ImageView view = new ImageView();
            view.setFitWidth(56);
            view.setFitHeight(56);
            view.setClip(new Circle(28, 28, 28));
            Images.showSquareCrop(view, photo);
            hostAvatar.getChildren().add(view);
        }
    }

    // ------------------------------------------------------------------ small helpers

    @FXML
    private void onBack() {
        if (navigator != null) navigator.goBack();
    }

    private void showStatus(String text) {
        statusLabel.setText(text);
        setShown(statusLabel, true);
    }

    private static void setShown(Node node, boolean shown) {
        node.setVisible(shown);
        node.setManaged(shown);
    }
}
