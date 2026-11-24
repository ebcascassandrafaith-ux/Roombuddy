# RoomBuddy - Member B (Listings, owner side)

Nothing from Member C (or anyone else) was edited. Member B only ADDS these files, in the same folders as the zip:

    src/java/com/template/owner/       ListingFormController, MyListingsController, MyListingCardController, OwnerOptions, OwnerUi
    src/java/com/template/model/       OwnerListing.java
    src/java/com/template/dao/         OwnerListingDAO.java
    src/java/com/template/util/        UploadStore.java
    src/resources/com/template/owner/  my_listings_view.fxml, my_listing_card.fxml, listing_form.fxml, owner.css

| Part                  | FXML (resources/com/template/owner) | Controller (java/com/template/owner) |
|-----------------------|--------------------------------------|---------------------------------------|
| My Listings screen    | my_listings_view.fxml                | MyListingsController                  |
| One listing row       | my_listing_card.fxml                 | MyListingCardController               |
| Post / Edit form      | listing_form.fxml                    | ListingFormController                 |

Open the three .fxml files in Scene Builder to restyle them (controller class is already set in each).

## What it does (task list for B)
- Post a listing: name, property type, room type, barangay, address, price, description, amenities (+ optional extra cost), house rules,
  3 or more photos (JPG/PNG, max 5 MB, choose the cover), at least one document (land title, business permit, barangay clearance, other).
  Missing fields are highlighted red. Saved with status `pending` -> "Listing submitted, pending admin approval".
- Posting needs a verified ID (`users.is_id_verified`), otherwise "Verify your ID to continue" (Member A's upload flow).
- My Listings: status badge (pending / verified / rejected / hidden), occupied badge, rejection reason shown for rejected listings.
- Edit: price, address, photos or documents changed -> status goes back to `pending`. Editing a rejected listing resubmits it.
- Delete (with confirmation) and Mark occupied / Mark available.
- Every update/delete checks `owner_id`, so one owner can't touch another's listing.

## Hook it into the main window (one-time, done by whoever owns MainController / main.fxml)
1. main.fxml: add a nav button   `<Button fx:id="myListingsNav" text="My Listings" styleClass="nav-button" onAction="#showMyListings"/>`
2. MainController:

        @FXML private Button myListingsNav;   // add 'myListingsNav' to the setActiveNav loop too

        @FXML
        public void showMyListings() {
            FxView<com.template.owner.MyListingsController> v =
                    FxView.load("/com/template/owner/my_listings_view.fxml");
            display(v.getRoot());
            current = Screen.OTHER;            // add OTHER to the Screen enum
            setActiveNav(myListingsNav);
        }

## Database (follows the lead's roombuddy.sql - nobody else has to change anything)
Member B talks to the lead's database `roombuddy` using only its tables and columns (listings, listing_photos, listing_amenities,
listing_documents, amenities, users). It does not create, alter or rename anything. It only needs `DBConnection.getConnection()`:
- Lead already has DBConnection.java -> keep the lead's (member-b-only.zip does NOT include one, so nothing gets overwritten).
- Using the full-project zip -> a default DBConnection.java is included (XAMPP: root / no password / database `roombuddy`).

Setup: start Apache + MySQL in XAMPP, import database/roombuddy.sql in phpMyAdmin, add the MySQL (or MariaDB) JDBC driver jar.
Photos and documents are copied to `uploads/listings/` and `uploads/documents/` (same folder the sample data uses).
Demo owner = user_id 1 (Aling Linda, ID verified) via Session.java, so Post a listing works right away.
Checked: B's queries were run against the roombuddy.sql schema and sample data.

Note for Member C (not changed by B): C's DAOs still use a draft schema (listings.id, title, status APPROVED, bookmarks), so Browse
needs to follow roombuddy.sql too: id->listing_id, title->property_name, location->barangay, rules->house_rules,
APPROVED->verified, bookmarks->saved_listings, listing_amenities.amenity -> join `amenities`.
