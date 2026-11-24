package com.template.admin.model;

/** One row of the users table, as the admin screens need it. */
public class AdminUser {
    private final int userId;
    private final String fullName;
    private final String email;
    private final String contactNumber;
    private final String role;
    private final String status;
    private final String idVerified;   // "Yes" or "No"
    private final String createdAt;

    public AdminUser(int userId, String fullName, String email, String contactNumber,
                     String role, String status, String idVerified, String createdAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.contactNumber = contactNumber;
        this.role = role;
        this.status = status;
        this.idVerified = idVerified;
        this.createdAt = createdAt;
    }

    public int getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getContactNumber() { return contactNumber; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public String getIdVerified() { return idVerified; }
    public String getCreatedAt() { return createdAt; }
}
