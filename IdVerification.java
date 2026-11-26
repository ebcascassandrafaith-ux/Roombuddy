package com.template.model;


/** One row of id_verifications (plus the user's name and email). */
public class IdVerification {
    private final int verificationId;
    private final int userId;
    private final String userName;
    private final String email;
    private final String idType;
    private final String frontImagePath;
    private final String backImagePath;
    private final String status;
    private final String rejectionReason;
    private final String submittedAt;

    public IdVerification(int verificationId, int userId, String userName, String email,
                          String idType, String frontImagePath, String backImagePath,
                          String status, String rejectionReason, String submittedAt) {
        this.verificationId = verificationId;
        this.userId = userId;
        this.userName = userName;
        this.email = email;
        this.idType = idType;
        this.frontImagePath = frontImagePath;
        this.backImagePath = backImagePath;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.submittedAt = submittedAt;
    }

    public int getVerificationId() { return verificationId; }
    public int getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getEmail() { return email; }
    public String getIdType() { return idType; }
    public String getFrontImagePath() { return frontImagePath; }
    public String getBackImagePath() { return backImagePath; }
    public String getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public String getSubmittedAt() { return submittedAt; }
}
