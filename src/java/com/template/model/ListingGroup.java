package com.template.model;

import java.util.ArrayList;
import java.util.List;

/** One row on the home screen, e.g. "Dormitories in Lapasan". */
public class ListingGroup {

    private final String propertyType;  // 'boarding_house' | 'dormitory'
    private final String barangay;
    private final List<Listing> listings = new ArrayList<>();

    public ListingGroup(String propertyType, String barangay) {
        this.propertyType = propertyType;
        this.barangay = barangay;
    }

    public String getPropertyType() { return propertyType; }

    public String getBarangay() { return barangay; }

    public List<Listing> getListings() { return listings; }

    /** "Dormitories in Lapasan" / "Boarding Houses in Carmen" */
    public String getTitle() {
        String type = "dormitory".equals(propertyType) ? "Dormitories" : "Boarding Houses";
        return type + " in " + barangay;
    }
}