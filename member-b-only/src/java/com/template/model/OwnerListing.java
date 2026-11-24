package com.template.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Member B: a listing as the OWNER sees it (all statuses, documents, extra costs).
 * Mirrors the `listings` table of database/roombuddy.sql.
 * (Member C's Listing class is the renter's read-only view and is left untouched.)
 */
public class OwnerListing {

    public static final String PENDING = "pending";
    public static final String VERIFIED = "verified";
    public static final String REJECTED = "rejected";
    public static final String HIDDEN = "hidden";

    /** One uploaded property document (land title, business permit, ...). */
    public static final class Doc {
        public final String type;   // land_title | business_permit | barangay_clearance | other
        public final String path;

        public Doc(String type, String path) {
            this.type = type;
            this.path = path;
        }
    }

    private int id;
    private int ownerId;
    private String propertyName;
    private String propertyType = "boarding_house";   // boarding_house | dormitory
    private String description;
    private String barangay;
    private String address;
    private double price;
    private String roomType = "single";               // single | double | shared | bedspace
    private String houseRules;
    private String status = PENDING;
    private String rejectionReason;
    private boolean occupied;

    /** amenity name -> extra monthly cost (null = included in the rent). */
    private final Map<String, Double> amenities = new LinkedHashMap<>();
    private final List<String> photos = new ArrayList<>();      // first = cover
    private final List<Doc> documents = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }
    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String v) { this.propertyName = v; }
    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String v) { this.propertyType = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getBarangay() { return barangay; }
    public void setBarangay(String v) { this.barangay = v; }
    public String getAddress() { return address; }
    public void setAddress(String v) { this.address = v; }
    public double getPrice() { return price; }
    public void setPrice(double v) { this.price = v; }
    public String getRoomType() { return roomType; }
    public void setRoomType(String v) { this.roomType = v; }
    public String getHouseRules() { return houseRules; }
    public void setHouseRules(String v) { this.houseRules = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String v) { this.rejectionReason = v; }
    public boolean isOccupied() { return occupied; }
    public void setOccupied(boolean v) { this.occupied = v; }

    public Map<String, Double> getAmenities() { return amenities; }
    public List<String> getPhotos() { return photos; }
    public List<Doc> getDocuments() { return documents; }

    public String getCoverPhoto() { return photos.isEmpty() ? null : photos.get(0); }

    public String getPriceText() { return String.format(Locale.US, "₱%,.0f / month", price); }

    public String getStatusLabel() {
        switch (status) {
            case VERIFIED: return "Verified";
            case REJECTED: return "Rejected";
            case HIDDEN:   return "Hidden";
            default:       return "Pending approval";
        }
    }
}
