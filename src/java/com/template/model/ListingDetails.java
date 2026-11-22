package com.template.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/** Everything the details page needs: the card fields (from Listing) plus photos, amenities and the host. */
public class ListingDetails extends Listing {

    private String description;
    private String roomType;        // 'single' | 'double' | 'shared' | 'bedspace'
    private String houseRules;      // free text, one rule per line
    private String status;          // 'pending' | 'verified' | 'rejected' | 'hidden'
    private boolean occupied;

    private final List<String> photos = new ArrayList<>();      // cover photo first
    private final List<Amenity> amenities = new ArrayList<>();

    private String hostName;
    private String hostPhoto;
    private boolean hostVerified;
    private LocalDate hostSince;

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public String getHouseRules() { return houseRules; }
    public void setHouseRules(String houseRules) { this.houseRules = houseRules; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isOccupied() { return occupied; }
    public void setOccupied(boolean occupied) { this.occupied = occupied; }

    public List<String> getPhotos() { return photos; }

    public List<Amenity> getAmenities() { return amenities; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }

    public String getHostPhoto() { return hostPhoto; }
    public void setHostPhoto(String hostPhoto) { this.hostPhoto = hostPhoto; }

    public boolean isHostVerified() { return hostVerified; }
    public void setHostVerified(boolean hostVerified) { this.hostVerified = hostVerified; }

    public LocalDate getHostSince() { return hostSince; }
    public void setHostSince(LocalDate hostSince) { this.hostSince = hostSince; }

    /** Only verified, not-occupied listings may be shown to renters (flowchart: "This listing is no longer available"). */
    public boolean isAvailable() {
        return "verified".equals(status) && !occupied;
    }

    /** "Single room in Lapasan" */
    public String getHeadline() {
        String type;
        if (roomType == null) {
            type = "Room";
        } else {
            switch (roomType) {
                case "single":   type = "Single room"; break;
                case "double":   type = "Double room"; break;
                case "shared":   type = "Shared room"; break;
                case "bedspace": type = "Bedspace";    break;
                default:         type = "Room";
            }
        }
        return type + " in " + getBarangay();
    }

    /** "Dormitory" / "Boarding house" */
    public String getPropertyTypeText() {
        return "dormitory".equals(getPropertyType()) ? "Dormitory" : "Boarding house";
    }

    /** "2 years of hosting" / "New host" */
    public String getHostingText() {
        if (hostSince == null) return "";
        int years = Period.between(hostSince, LocalDate.now()).getYears();
        if (years < 1) return "New host";
        return years + (years == 1 ? " year of hosting" : " years of hosting");
    }

    /** The house rules split into separate lines (empty lines removed). */
    public List<String> getHouseRuleLines() {
        List<String> lines = new ArrayList<>();
        if (houseRules == null) return lines;
        for (String line : houseRules.split("\\r?\\n")) {
            String t = line.trim();
            if (!t.isEmpty()) lines.add(t);
        }
        return lines;
    }
}
