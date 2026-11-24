package com.template.admin.model;

import java.util.Locale;

/** One row of the listings table (plus the owner's name), as the admin screens need it. */
public class AdminListing {
    private final int listingId;
    private final int ownerId;
    private final String ownerName;
    private String propertyName;
    private String propertyType;
    private String description;
    private String barangay;
    private String address;
    private double price;
    private String roomType;
    private String houseRules;
    private final String status;
    private final String rejectionReason;
    private final String occupied;   // "Yes" or "No"
    private final String createdAt;

    public AdminListing(int listingId, int ownerId, String ownerName, String propertyName,
                        String propertyType, String description, String barangay, String address,
                        double price, String roomType, String houseRules, String status,
                        String rejectionReason, String occupied, String createdAt) {
        this.listingId = listingId;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.description = description;
        this.barangay = barangay;
        this.address = address;
        this.price = price;
        this.roomType = roomType;
        this.houseRules = houseRules;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.occupied = occupied;
        this.createdAt = createdAt;
    }

    public int getListingId() { return listingId; }
    public int getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public String getPropertyName() { return propertyName; }
    public String getPropertyType() { return propertyType; }
    public String getDescription() { return description; }
    public String getBarangay() { return barangay; }
    public String getAddress() { return address; }
    public double getPrice() { return price; }
    public String getPriceText() { return String.format(Locale.US, "%.2f", price); }
    public String getRoomType() { return roomType; }
    public String getHouseRules() { return houseRules; }
    public String getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public String getOccupied() { return occupied; }
    public String getCreatedAt() { return createdAt; }

    // Setters only for the fields the admin is allowed to edit
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }
    public void setDescription(String description) { this.description = description; }
    public void setBarangay(String barangay) { this.barangay = barangay; }
    public void setAddress(String address) { this.address = address; }
    public void setPrice(double price) { this.price = price; }
    public void setRoomType(String roomType) { this.roomType = roomType; }
    public void setHouseRules(String houseRules) { this.houseRules = houseRules; }
}
