package com.template.model;

/** One row of admin_logs (plus the admin's name). */
public class AdminLog {
    private final int logId;
    private final String adminName;
    private final String action;
    private final String targetType;
    private final int targetId;
    private final String remarks;
    private final String createdAt;

    public AdminLog(int logId, String adminName, String action, String targetType,
                    int targetId, String remarks, String createdAt) {
        this.logId = logId;
        this.adminName = adminName;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.remarks = remarks;
        this.createdAt = createdAt;
    }

    public int getLogId() { return logId; }
    public String getAdminName() { return adminName; }
    public String getAction() { return action; }
    public String getTargetType() { return targetType; }
    public int getTargetId() { return targetId; }
    public String getRemarks() { return remarks; }
    public String getCreatedAt() { return createdAt; }
}