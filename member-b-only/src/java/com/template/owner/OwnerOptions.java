package com.template.owner;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Member B: dropdown values for the post-listing form, with the DB values behind each label. */
public final class OwnerOptions {

    private OwnerOptions() {}

    public static final int MIN_PHOTOS = 3;
    public static final int MAX_PHOTOS = 10;

    /** label -> value stored in listings.room_type */
    public static final Map<String, String> ROOM_TYPES = new LinkedHashMap<>();
    /** label -> value stored in listings.property_type */
    public static final Map<String, String> PROPERTY_TYPES = new LinkedHashMap<>();
    /** label -> value stored in listing_documents.document_type */
    public static final Map<String, String> DOCUMENT_TYPES = new LinkedHashMap<>();

    static {
        ROOM_TYPES.put("Single room", "single");
        ROOM_TYPES.put("Double room", "double");
        ROOM_TYPES.put("Shared room", "shared");
        ROOM_TYPES.put("Bedspace", "bedspace");

        PROPERTY_TYPES.put("Boarding house", "boarding_house");
        PROPERTY_TYPES.put("Dormitory", "dormitory");

        DOCUMENT_TYPES.put("Land title", "land_title");
        DOCUMENT_TYPES.put("Business permit", "business_permit");
        DOCUMENT_TYPES.put("Barangay clearance", "barangay_clearance");
        DOCUMENT_TYPES.put("Other", "other");
    }

    public static List<String> roomTypeLabels() { return List.copyOf(ROOM_TYPES.keySet()); }

    public static List<String> propertyTypeLabels() { return List.copyOf(PROPERTY_TYPES.keySet()); }

    /** Reverse lookup: DB value -> label (falls back to the raw value). */
    public static String labelOf(Map<String, String> map, String dbValue) {
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getValue().equals(dbValue)) return e.getKey();
        }
        return dbValue;
    }
}
