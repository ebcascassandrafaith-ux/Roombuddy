package com.template.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** One room of a house (table `rooms`). A house (`listings`) has one or more rooms, each with its own price. */
public class Room {

    private int roomId;                 // 0 = a "virtual" room made from an old listing that has no rows in `rooms`
    private int listingId;
    private String roomName;            // "Room 1 - Dodong"
    private String description;
    private double price;
    private String roomType;            // 'single' | 'double' | 'shared' | 'bedspace'
    private int advanceMonths = 1;
    private int depositMonths = 1;
    private boolean occupied;
    private String coverPhoto;          // first photo, shown on the room card
    private final List<String> photos = new ArrayList<>();

    public int getRoomId() { return roomId; }
    public void setRoomId(int roomId) { this.roomId = roomId; }

    public int getListingId() { return listingId; }
    public void setListingId(int listingId) { this.listingId = listingId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public int getAdvanceMonths() { return advanceMonths; }
    public void setAdvanceMonths(int advanceMonths) { this.advanceMonths = advanceMonths; }

    public int getDepositMonths() { return depositMonths; }
    public void setDepositMonths(int depositMonths) { this.depositMonths = depositMonths; }

    public boolean isOccupied() { return occupied; }
    public void setOccupied(boolean occupied) { this.occupied = occupied; }
    public boolean isAvailable() { return !occupied; }

    public String getCoverPhoto() { return coverPhoto; }
    public void setCoverPhoto(String coverPhoto) { this.coverPhoto = coverPhoto; }

    public List<String> getPhotos() { return photos; }

    /** "₱3,500" */
    public String getPriceText() {
        return String.format(Locale.US, "\u20B1%,.0f", price);
    }

    /** "Single room" */
    public String getRoomTypeText() {
        if (roomType == null) return "Room";
        switch (roomType) {
            case "single":   return "Single room";
            case "double":   return "Double room";
            case "shared":   return "Shared room";
            case "bedspace": return "Bedspace";
            default:         return "Room";
        }
    }

    /**
     * Listings made before the `rooms` table existed (or by a form that does not add rooms yet) have no rows
     * in `rooms`. They still work: the house itself is shown as one room.
     */
    public static Room fromListing(ListingDetails d) {
        Room r = new Room();
        r.roomId = 0;
        r.listingId = d.getListingId();
        r.roomName = "Room 1";
        r.price = d.getPrice();
        r.roomType = d.getRoomType();
        r.occupied = d.isOccupied();
        r.photos.addAll(d.getPhotos());
        r.coverPhoto = d.getPhotos().isEmpty() ? null : d.getPhotos().get(0);
        return r;
    }
}
