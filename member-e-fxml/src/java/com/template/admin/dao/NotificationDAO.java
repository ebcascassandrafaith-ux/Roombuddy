package com.template.admin.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Table: notifications. Only inserts the notices the admin sends (ID and listing decisions). */
public class NotificationDAO {

    /** type must be one of the notifications.type values, e.g. 'id_approved' or 'listing_rejected'. */
    public void add(Connection conn, int userId, String type, String message,
                    int relatedId) throws SQLException {
        String sql = "INSERT INTO notifications (user_id, type, message, related_id) "
                   + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, type);
            ps.setString(3, message);
            ps.setInt(4, relatedId);
            ps.executeUpdate();
        }
    }
}
