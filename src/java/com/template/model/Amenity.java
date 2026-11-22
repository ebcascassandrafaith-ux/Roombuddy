package com.template.model;

import java.util.Locale;

/** One amenity of a listing. Comes from `amenities.amenity_name` + `listing_amenities.extra_cost`. */
public class Amenity {

    private final String name;
    private final Double extraCost;   // null or 0 = included in the rent

    public Amenity(String name, Double extraCost) {
        this.name = name;
        this.extraCost = extraCost;
    }

    public String getName() { return name; }

    public Double getExtraCost() { return extraCost; }

    /** "Included in the rent" or "Extra cost: ₱300" */
    public String getCostText() {
        if (extraCost == null || extraCost <= 0) return "Included in the rent";
        return String.format(Locale.US, "Extra cost: \u20B1%,.0f", extraCost);
    }
}
