package com.template.owner;

import com.template.Session;
import com.template.dao.OwnerListingDAO;
import com.template.model.OwnerListing;
import com.template.util.Async;
import com.template.util.Images;
import com.template.util.UploadStore;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

/**
 * Member B: "Post a listing" and "Edit listing" form (listing_form.fxml), shown in its own window.
 *
 * Rules (from the flowcharts / task list):
 *  - required: name, types, barangay, address, price, description, house rules,
 *    at least 3 photos and at least one property document
 *  - a new listing is saved as PENDING (admin approval)
 *  - editing price, address, photos or documents sends the listing back to PENDING
 *    (a rejected listing is also resubmitted as PENDING)
 */
public class ListingFormController {

    /** A photo or document in the form: either already stored (path) or newly chosen (file). */
    private static final class Entry {
        String existingPath;   // set for items that are already saved
        File newFile;          // set for items chosen in this session
        String docType;        // documents only (DB value)

        String key() { return existingPath != null ? existingPath : "NEW:" + newFile.getAbsolutePath(); }
    }

    @FXML private Label formTitle;
    @FXML private Label editNote;
    @FXML private TextField nameField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private ComboBox<String> roomCombo;
    @FXML private TextField barangayField;
    @FXML private TextField priceField;
    @FXML private TextField addressField;
    @FXML private TextArea descriptionArea;
    @FXML private FlowPane amenityPane;
    @FXML private TextArea rulesArea;
    @FXML private Label photoHint;
    @FXML private FlowPane photoPane;
    @FXML private VBox docsBox;
    @FXML private Label errorLabel;
    @FXML private Button submitButton;

    private final OwnerListingDAO dao = new OwnerListingDAO();
    private final Map<String, CheckBox> amenityChecks = new LinkedHashMap<>();
    private final Map<String, TextField> amenityCosts = new LinkedHashMap<>();
    private final List<Entry> photos = new ArrayList<>();
    private final List<Entry> docs = new ArrayList<>();

    private Stage stage;
    private OwnerListing existing;      // null = posting a new listing
    private boolean saved;
    private List<String> originalPhotoKeys = List.of();
    private List<String> originalDocKeys = List.of();

    /**
     * Opens the form and waits until it is closed.
     * @param existing null to post a new listing, or the listing to edit
     * @return true if something was saved
     */
    public static boolean open(Window owner, OwnerListing existing) {
        try {
            FXMLLoader loader = new FXMLLoader(ListingFormController.class.getResource("/com/template/owner/listing_form.fxml"));
            Parent root = loader.load();
            ListingFormController c = loader.getController();

            Stage st = new Stage();
            st.initOwner(owner);
            st.initModality(Modality.APPLICATION_MODAL);
            st.setTitle(existing == null ? "Post a listing" : "Edit listing");
            Scene scene = new Scene(root, 760, 820);
            scene.getStylesheets().add(ListingFormController.class.getResource("/com/template/style.css").toExternalForm());
            st.setScene(scene);

            c.init(st, existing);
            st.showAndWait();
            return c.saved;
        } catch (IOException e) {
            throw new UncheckedIOException("Could not open the listing form", e);
        }
    }

    // ------------------------------------------------------------------ setup

    @FXML
    private void initialize() {
        typeCombo.getItems().setAll(OwnerOptions.propertyTypeLabels());
        roomCombo.getItems().setAll(OwnerOptions.roomTypeLabels());
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void init(Stage stage, OwnerListing existing) {
        this.stage = stage;
        this.existing = existing;

        if (existing != null) {
            formTitle.setText("Edit listing");
            submitButton.setText("Save changes");
            boolean rejected = OwnerListing.REJECTED.equals(existing.getStatus());
            editNote.setText(rejected
                    ? "This listing was rejected. Fix the problems and save to resubmit it for approval."
                    : "Changing the price, address, photos or documents sends the listing back to admin approval.");
            nameField.setText(existing.getPropertyName());
            typeCombo.setValue(OwnerOptions.labelOf(OwnerOptions.PROPERTY_TYPES, existing.getPropertyType()));
            roomCombo.setValue(OwnerOptions.labelOf(OwnerOptions.ROOM_TYPES, existing.getRoomType()));
            barangayField.setText(existing.getBarangay());
            priceField.setText(String.format(Locale.US, "%.2f", existing.getPrice()));
            addressField.setText(existing.getAddress());
            descriptionArea.setText(existing.getDescription());
            rulesArea.setText(existing.getHouseRules());

            for (String p : existing.getPhotos()) {
                Entry e = new Entry();
                e.existingPath = p;
                photos.add(e);
            }
            for (OwnerListing.Doc d : existing.getDocuments()) {
                Entry e = new Entry();
                e.existingPath = d.path;
                e.docType = d.type;
                docs.add(e);
            }
        } else {
            editNote.setVisible(false);
            editNote.setManaged(false);
        }
        originalPhotoKeys = keysOf(photos);
        originalDocKeys = keysOf(docs);
        renderPhotos();
        renderDocs();

        // Amenity names come straight from the DB so they always match.
        Async.run(dao::amenityNames, this::buildAmenities, OwnerUi::error);
    }

    private void buildAmenities(List<String> names) {
        amenityPane.getChildren().clear();
        for (String name : names) {
            CheckBox box = new CheckBox(name);
            TextField cost = new TextField();
            cost.setPromptText("extra ₱");
            cost.setPrefWidth(80);
            cost.setDisable(true);
            box.selectedProperty().addListener((o, was, is) -> cost.setDisable(!is));
            amenityChecks.put(name, box);
            amenityCosts.put(name, cost);
            amenityPane.getChildren().add(new HBox(6, box, cost));
        }
        if (existing != null) {
            for (Map.Entry<String, Double> a : existing.getAmenities().entrySet()) {
                CheckBox box = amenityChecks.get(a.getKey());
                if (box == null) continue;
                box.setSelected(true);
                if (a.getValue() != null) amenityCosts.get(a.getKey()).setText(String.format(Locale.US, "%.0f", a.getValue()));
            }
        }
    }

    // ------------------------------------------------------------------ photos

    @FXML
    private void onAddPhotos() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose photos (JPG or PNG, max 5 MB each)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png"));
        List<File> files = fc.showOpenMultipleDialog(stage);
        if (files == null) return;
        for (File f : files) {
            String problem = UploadStore.validate(f, UploadStore.IMAGES);   // file format + size limit
            if (problem != null) { OwnerUi.error(problem); return; }
        }
        if (photos.size() + files.size() > OwnerOptions.MAX_PHOTOS) {
            OwnerUi.error("You can have at most " + OwnerOptions.MAX_PHOTOS + " photos.");
            return;
        }
        for (File f : files) {
            Entry e = new Entry();
            e.newFile = f;
            photos.add(e);
        }
        renderPhotos();
    }

    private void renderPhotos() {
        photoPane.getChildren().clear();
        for (int i = 0; i < photos.size(); i++) {
            final Entry entry = photos.get(i);
            final int index = i;

            ImageView iv = new ImageView();
            Image img = Images.load(entry.existingPath != null ? entry.existingPath : entry.newFile.getAbsolutePath(), 220, 160);
            iv.setImage(img);
            iv.setFitWidth(110);
            iv.setFitHeight(80);
            iv.setPreserveRatio(true);
            StackPane thumb = new StackPane(iv);
            thumb.getStyleClass().add("photo-thumb-box");
            thumb.setMinSize(110, 80);

            Node coverControl;
            if (i == 0) {
                Label cover = new Label("★ Cover");
                cover.getStyleClass().add("muted");
                coverControl = cover;
            } else {
                Button makeCover = new Button("Make cover");
                makeCover.getStyleClass().add("ghost-button");
                makeCover.setOnAction(ev -> { photos.remove(entry); photos.add(0, entry); renderPhotos(); });
                coverControl = makeCover;
            }
            Button remove = new Button("✕");
            remove.getStyleClass().add("ghost-button");
            remove.setOnAction(ev -> { photos.remove(index); renderPhotos(); });

            Region grow = new Region();
            HBox.setHgrow(grow, Priority.ALWAYS);
            photoPane.getChildren().add(new VBox(4, thumb, new HBox(4, coverControl, grow, remove)));
        }
        int n = photos.size();
        photoHint.setText(n + " photo(s). Minimum " + OwnerOptions.MIN_PHOTOS + ", maximum " + OwnerOptions.MAX_PHOTOS
                + ". The first photo is the cover shown to renters.");
    }

    // ------------------------------------------------------------------ documents

    @FXML
    private void onAddDocument() {
        ChoiceDialog<String> typeDialog = new ChoiceDialog<>(OwnerOptions.DOCUMENT_TYPES.keySet().iterator().next(),
                OwnerOptions.DOCUMENT_TYPES.keySet());
        typeDialog.setTitle("Document type");
        typeDialog.setHeaderText(null);
        typeDialog.setContentText("What kind of document is it?");
        Optional<String> type = typeDialog.showAndWait();
        if (type.isEmpty()) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Choose the document (PDF, JPG or PNG, max 5 MB)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.jpg", "*.jpeg", "*.png"));
        File f = fc.showOpenDialog(stage);
        if (f == null) return;
        String problem = UploadStore.validate(f, UploadStore.DOCUMENTS);
        if (problem != null) { OwnerUi.error(problem); return; }

        Entry e = new Entry();
        e.newFile = f;
        e.docType = OwnerOptions.DOCUMENT_TYPES.get(type.get());
        docs.add(e);
        renderDocs();
    }

    private void renderDocs() {
        docsBox.getChildren().clear();
        if (docs.isEmpty()) {
            Label none = new Label("No documents added yet.");
            none.getStyleClass().add("muted");
            docsBox.getChildren().add(none);
            return;
        }
        for (Entry entry : docs) {
            String fileName = entry.newFile != null ? entry.newFile.getName() : new File(entry.existingPath).getName();
            Label text = new Label(OwnerOptions.labelOf(OwnerOptions.DOCUMENT_TYPES, entry.docType) + "  ·  " + fileName);
            Region grow = new Region();
            HBox.setHgrow(grow, Priority.ALWAYS);
            Button remove = new Button("Remove");
            remove.getStyleClass().add("ghost-button");
            remove.setOnAction(ev -> { docs.remove(entry); renderDocs(); });
            HBox row = new HBox(8, text, grow, remove);
            row.getStyleClass().add("doc-row");
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            docsBox.getChildren().add(row);
        }
    }

    // ------------------------------------------------------------------ submit

    @FXML
    private void onCancel() { stage.close(); }

    @FXML
    private void onSubmit() {
        // 1. validate and highlight the missing fields
        clearMarks();
        List<String> missing = new ArrayList<>();
        check(nameField, !blank(nameField.getText()), "property name", missing);
        check(typeCombo, typeCombo.getValue() != null, "property type", missing);
        check(roomCombo, roomCombo.getValue() != null, "room type", missing);
        check(barangayField, !blank(barangayField.getText()), "barangay", missing);
        check(addressField, !blank(addressField.getText()), "address", missing);
        check(descriptionArea, !blank(descriptionArea.getText()), "description", missing);
        check(rulesArea, !blank(rulesArea.getText()), "house rules", missing);

        double price = 0;
        boolean priceOk;
        try {
            price = Double.parseDouble(priceField.getText().trim().replace(",", ""));
            priceOk = price > 0;
        } catch (RuntimeException e) {
            priceOk = false;
        }
        check(priceField, priceOk, "a price above zero", missing);

        if (photos.size() < OwnerOptions.MIN_PHOTOS) {
            photoPane.getStyleClass().add("field-error");
            missing.add("at least " + OwnerOptions.MIN_PHOTOS + " photos");
        }
        if (docs.isEmpty()) {
            docsBox.getStyleClass().add("field-error");
            missing.add("at least one property document");
        }
        if (!missing.isEmpty()) {
            showError("Please fill in: " + String.join(", ", missing) + ".");
            return;
        }

        // 2. read the amenities (name -> extra cost)
        Map<String, Double> amenities = new LinkedHashMap<>();
        for (Map.Entry<String, CheckBox> e : amenityChecks.entrySet()) {
            if (!e.getValue().isSelected()) continue;
            String costText = amenityCosts.get(e.getKey()).getText();
            Double cost = null;
            if (!blank(costText)) {
                try {
                    cost = Double.parseDouble(costText.trim());
                } catch (NumberFormatException ex) {
                    amenityCosts.get(e.getKey()).getStyleClass().add("field-error");
                    showError("The extra cost for " + e.getKey() + " must be a number.");
                    return;
                }
            }
            amenities.put(e.getKey(), cost);
        }

        // 3. fill the listing object
        final boolean isNew = existing == null;
        final OwnerListing l = isNew ? new OwnerListing() : existing;
        final String newAddress = addressField.getText().trim();
        boolean priceChanged = !isNew && Math.abs(existing.getPrice() - price) > 0.001;
        boolean addressChanged = !isNew && !Objects.equals(existing.getAddress(), newAddress);

        l.setOwnerId(Session.getCurrentUserId());
        l.setPropertyName(nameField.getText().trim());
        l.setPropertyType(OwnerOptions.PROPERTY_TYPES.get(typeCombo.getValue()));
        l.setRoomType(OwnerOptions.ROOM_TYPES.get(roomCombo.getValue()));
        l.setBarangay(barangayField.getText().trim());
        l.setAddress(newAddress);
        l.setPrice(price);
        l.setDescription(descriptionArea.getText().trim());
        l.setHouseRules(rulesArea.getText().trim());
        l.getAmenities().clear();
        l.getAmenities().putAll(amenities);

        final boolean photosChanged = !keysOf(photos).equals(originalPhotoKeys);
        final boolean docsChanged = !keysOf(docs).equals(originalDocKeys);
        // Flowchart: "Is any critical info updated?" (price, address, photos, documents) -> back to pending.
        final boolean resetToPending = !isNew && (priceChanged || addressChanged || photosChanged || docsChanged
                || OwnerListing.REJECTED.equals(existing.getStatus()));

        final List<Entry> photoSnapshot = new ArrayList<>(photos);
        final List<Entry> docSnapshot = new ArrayList<>(docs);

        submitButton.setDisable(true);
        // 4. save off the UI thread (copies files, then writes to the database)
        Async.run(() -> {
            List<String> photoPaths = null;
            if (isNew || photosChanged) {
                photoPaths = new ArrayList<>();
                for (Entry e : photoSnapshot)
                    photoPaths.add(e.existingPath != null ? e.existingPath : UploadStore.store(e.newFile, "listings"));
            }
            List<OwnerListing.Doc> docList = null;
            if (isNew || docsChanged) {
                docList = new ArrayList<>();
                for (Entry e : docSnapshot)
                    docList.add(new OwnerListing.Doc(e.docType,
                            e.existingPath != null ? e.existingPath : UploadStore.store(e.newFile, "documents")));
            }
            if (isNew) dao.insert(l, photoPaths, docList);
            else dao.update(l, photoPaths, docList, resetToPending);
            return resetToPending;
        }, reset -> {
            saved = true;
            stage.close();
            if (isNew) OwnerUi.info("Listing submitted. It is pending admin approval.");
            else if (reset) OwnerUi.info("Changes saved. The listing is pending admin approval again.");
            else OwnerUi.info("Changes saved.");
        }, err -> {
            submitButton.setDisable(false);
            OwnerUi.error(err);
        });
    }

    // ------------------------------------------------------------------ helpers

    private void check(Control field, boolean ok, String label, List<String> missing) {
        if (ok) return;
        field.getStyleClass().add("field-error");
        missing.add(label);
    }

    private void clearMarks() {
        for (Node n : new Node[]{nameField, typeCombo, roomCombo, barangayField, priceField, addressField,
                descriptionArea, rulesArea, photoPane, docsBox}) {
            n.getStyleClass().remove("field-error");
        }
        for (TextField t : amenityCosts.values()) t.getStyleClass().remove("field-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private static boolean blank(String s) { return s == null || s.trim().isEmpty(); }

    private static List<String> keysOf(List<Entry> entries) {
        List<String> keys = new ArrayList<>();
        for (Entry e : entries) keys.add(e.key());
        return keys;
    }
}
