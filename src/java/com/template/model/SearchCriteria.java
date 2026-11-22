package com.template.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** What the renter typed and picked: keywords plus the filters Location, Price Range, Room Type, Amenities. */
public class SearchCriteria {

    /** room_type value in the database -> text shown to the user. */
    public static final Map<String, String> ROOM_TYPES = new LinkedHashMap<>();
    static {
        ROOM_TYPES.put("single", "Single room");
        ROOM_TYPES.put("double", "Double room");
        ROOM_TYPES.put("shared", "Shared room");
        ROOM_TYPES.put("bedspace", "Bedspace");
    }

    private String keyword = "";
    private Double minPrice;              // null = no minimum
    private Double maxPrice;              // null = no maximum
    private String barangay;              // null = any location
    private String roomType;              // null = any type
    private final List<String> amenities = new ArrayList<>();   // listing must have ALL of these

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword == null ? "" : keyword.trim(); }

    public Double getMinPrice() { return minPrice; }
    public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }

    public Double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }

    public String getBarangay() { return barangay; }
    public void setBarangay(String barangay) { this.barangay = barangay; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public List<String> getAmenities() { return amenities; }

    /** The keyword split into words, e.g. "casa lapasan" -> ["casa", "lapasan"]. */
    public List<String> getKeywordWords() {
        List<String> words = new ArrayList<>();
        for (String w : keyword.split("\\s+")) {
            if (!w.isEmpty()) words.add(w);
        }
        return words;
    }

    public boolean hasFilters() {
        return minPrice != null || maxPrice != null || barangay != null || roomType != null || !amenities.isEmpty();
    }

    /** True when nothing was typed or picked: the page then shows every verified listing, newest first. */
    public boolean isEmpty() {
        return keyword.isEmpty() && !hasFilters();
    }

    /** "₱2,000 – ₱5,000" / "₱2,000 and up" / "Up to ₱5,000" */
    public static String priceRangeText(Double min, Double max) {
        if (min != null && max != null) return money(min) + " \u2013 " + money(max);
        if (min != null) return money(min) + " and up";
        if (max != null) return "Up to " + money(max);
        return "";
    }

    private static String money(double v) {
        return String.format(Locale.US, "\u20B1%,.0f", v);
    }

    /** A one-line summary of the search, shown under the results title. */
    public String describe() {
        List<String> parts = new ArrayList<>();
        if (!keyword.isEmpty()) parts.add("\u201C" + keyword + "\u201D");
        if (minPrice != null || maxPrice != null) parts.add(priceRangeText(minPrice, maxPrice));
        if (barangay != null) parts.add(barangay);
        if (roomType != null) parts.add(ROOM_TYPES.getOrDefault(roomType, roomType));
        if (!amenities.isEmpty()) parts.add(String.join(", ", amenities));
        return String.join("  \u2022  ", parts);
    }
}
