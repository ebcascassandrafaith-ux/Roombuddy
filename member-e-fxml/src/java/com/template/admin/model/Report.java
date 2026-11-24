package com.template.admin.model;

/** One row of reports (plus the names of the people and listing involved). */
public class Report {
    private final int reportId;
    private final String reporterName;
    private final String reportedUser;
    private final String listingName;
    private final String reason;
    private final String status;
    private final String createdAt;

    public Report(int reportId, String reporterName, String reportedUser, String listingName,
                  String reason, String status, String createdAt) {
        this.reportId = reportId;
        this.reporterName = reporterName;
        this.reportedUser = reportedUser;
        this.listingName = listingName;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getReportId() { return reportId; }
    public String getReporterName() { return reporterName; }
    public String getReportedUser() { return reportedUser; }
    public String getListingName() { return listingName; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}
