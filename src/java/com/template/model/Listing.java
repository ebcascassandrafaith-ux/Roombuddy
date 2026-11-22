package com.template.model;

import java.util.Locale;

/**
 * A listing as shown on a home card. Fields match the `listings` table in roombuddy.sql.
 */
public class Listing {

    private int listingId;
    private int ownerId;
    private String propertyName;   // listings.property_name
    private String propertyType;   // 'boarding_house' | 'dormitory'
    private String barangay;
    private String address;
    private double price;          // per month
    private String coverPhoto;     // listing_photos.file_path of the cover photo, or null

    public int getListingId() { return listingId; }
    public void setListingId(int listingId) { this.listingId = listingId; }

    public int getOwnerId() { return ownerId; }
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }

    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }

    public String getBarangay() { return barangay; }
    public void setBarangay(String barangay) { this.barangay = barangay; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getCoverPhoto() { return coverPhoto; }
    public void setCoverPhoto(String coverPhoto) { this.coverPhoto = coverPhoto; }

    /** "₱5,500 per month" */
    public String getPriceText() {
        return String.format(Locale.US, "\u20B1%,.0f per month", price);
    }
}