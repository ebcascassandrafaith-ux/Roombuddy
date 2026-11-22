package com.template.dao;

import com.template.model.Amenity;
import com.template.model.Listing;
import com.template.model.ListingDetails;
import com.template.model.SearchCriteria;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/** Fake listings used while AppConfig.DEMO_MODE is true, so the screens work without a database. */
final class SampleData {

    private SampleData() {}

    static List<Listing> listings() {
        List<Listing> list = new ArrayList<>();
        int id = 1;
        id = add(list, id, "dormitory", "Lapasan", 7, 5500);
        id = add(list, id, "dormitory", "Kauswagan", 5, 4200);
        id = add(list, id, "boarding_house", "Carmen", 4, 3800);
        id = add(list, id, "boarding_house", "Nazareth", 2, 2500);
        return list;
    }

    private static int add(List<Listing> list, int id, String type, String barangay, int count, double basePrice) {
        String[] names = {"Ang Balay ni Libay Libat", "Casa Verde", "Sunrise Residence", "Maria's Place",
                "Blue Door Dorm", "Kabalay Lodge", "The Quiet Corner"};
        for (int i = 0; i < count; i++) {
            Listing l = new Listing();
            l.setListingId(id++);
            l.setOwnerId(1);
            l.setPropertyName(names[i % names.length]);
            l.setPropertyType(type);
            l.setBarangay(barangay);
            l.setAddress("Sample St., " + barangay + ", Cagayan de Oro");
            l.setPrice(basePrice + i * 250);
            list.add(l); // no cover photo: the card shows the "No photo" placeholder
        }
        return id;
    }

    // ------------------------------------------------------------------ details page

    /** @return details for the sample listing with this id, or null if there is none. */
    static ListingDetails details(int listingId) {
        for (Listing l : listings()) {
            if (l.getListingId() != listingId) continue;

            ListingDetails d = new ListingDetails();
            d.setListingId(l.getListingId());
            d.setOwnerId(l.getOwnerId());
            d.setPropertyName(l.getPropertyName());
            d.setPropertyType(l.getPropertyType());
            d.setBarangay(l.getBarangay());
            d.setAddress(l.getAddress());
            d.setPrice(l.getPrice());
            d.setStatus("verified");
            d.setOccupied(false);
            d.setRoomType(listingId % 2 == 0 ? "shared" : "single");
            d.setDescription("A clean and quiet place a short ride from the university. "
                    + "Good for students and working renters. The host lives nearby and responds quickly.");
            d.setHouseRules("No overnight guests\nNo smoking inside the house\nQuiet hours after 10 PM\nCurfew at 11 PM");
            d.getAmenities().add(new Amenity("Water", null));
            d.getAmenities().add(new Amenity("Electricity", 300.0));
            d.getAmenities().add(new Amenity("WiFi", null));
            d.getAmenities().add(new Amenity("CCTV / Security", null));
            d.setHostName("Aling Linda");
            d.setHostVerified(true);
            d.setHostSince(LocalDate.now().minusYears(2));
            // no photos on purpose: the gallery shows "No photo" tiles
            return d;
        }
        return null;
    }

    static List<Listing> moreOptions(ListingDetails current, int limit) {
        List<Listing> result = new ArrayList<>();
        for (Listing l : listings()) {
            if (l.getListingId() != current.getListingId()) result.add(l);
        }
        // same barangay first (sort is stable, so the original order is kept inside each group)
        result.sort(Comparator.comparing((Listing l) -> !l.getBarangay().equals(current.getBarangay())));
        return result.size() > limit ? new ArrayList<>(result.subList(0, limit)) : result;
    }

    // ------------------------------------------------------------------ search + filters (demo)

    static List<Listing> search(SearchCriteria c) {
        List<Listing> out = new ArrayList<>();
        for (Listing l : listings()) {
            if (!matchesKeywords(l, c)) continue;
            if (c.getMinPrice() != null && l.getPrice() < c.getMinPrice()) continue;
            if (c.getMaxPrice() != null && l.getPrice() > c.getMaxPrice()) continue;
            if (c.getBarangay() != null && !c.getBarangay().equals(l.getBarangay())) continue;

            ListingDetails d = details(l.getListingId());
            if (c.getRoomType() != null && !c.getRoomType().equals(d.getRoomType())) continue;
            if (!hasAllAmenities(d, c.getAmenities())) continue;
            out.add(l);
        }
        return out;
    }

    private static boolean matchesKeywords(Listing l, SearchCriteria c) {
        String text = (l.getPropertyName() + " " + l.getBarangay() + " " + l.getAddress()).toLowerCase();
        for (String word : c.getKeywordWords()) {
            if (!text.contains(word.toLowerCase())) return false;
        }
        return true;
    }

    private static boolean hasAllAmenities(ListingDetails d, List<String> wanted) {
        for (String name : wanted) {
            boolean found = false;
            for (Amenity a : d.getAmenities()) {
                if (a.getName().equals(name)) { found = true; break; }
            }
            if (!found) return false;
        }
        return true;
    }

    static List<String> barangays() {
        TreeSet<String> set = new TreeSet<>();
        for (Listing l : listings()) set.add(l.getBarangay());
        return new ArrayList<>(set);
    }

    /** Same names as the `amenities` table in roombuddy.sql. */
    static List<String> amenityNames() {
        return new ArrayList<>(List.of("Air Conditioning", "CCTV / Security", "Electricity", "Kitchen Access",
                "Laundry Area", "Parking", "Private Bathroom", "Shared Bathroom", "Study Area", "Water", "WiFi"));
    }
}
