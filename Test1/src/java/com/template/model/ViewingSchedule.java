package com.template.model;

import java.time.LocalDateTime;

public class ViewingSchedule {
    private int viewingId;
    private int listingId;
    private int renterId;
    private int landlordId;
    private LocalDateTime scheduledAt;
    private int proposedBy;
    private String status;
    private LocalDateTime createdAt;
    // filled only by CommunicationDAO.findViewingSchedulesForUser (for the schedule list)
    private String listingName;
    private String renterName;
    private String landlordName;

    public int getViewingId() { return viewingId; }
    public void setViewingId(int viewingId) { this.viewingId = viewingId; }
    public int getListingId() { return listingId; }
    public void setListingId(int listingId) { this.listingId = listingId; }
    public int getRenterId() { return renterId; }
    public void setRenterId(int renterId) { this.renterId = renterId; }
    public int getLandlordId() { return landlordId; }
    public void setLandlordId(int landlordId) { this.landlordId = landlordId; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public int getProposedBy() { return proposedBy; }
    public void setProposedBy(int proposedBy) { this.proposedBy = proposedBy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getListingName() { return listingName; }
    public void setListingName(String listingName) { this.listingName = listingName; }
    public String getRenterName() { return renterName; }
    public void setRenterName(String renterName) { this.renterName = renterName; }
    public String getLandlordName() { return landlordName; }
    public void setLandlordName(String landlordName) { this.landlordName = landlordName; }
}
