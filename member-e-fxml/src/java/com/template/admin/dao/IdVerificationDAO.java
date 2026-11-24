package com.template.admin.dao;

import com.template.DBConnection;
import com.template.admin.model.IdVerification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Tables: id_verifications, users (is_id_verified), notifications, admin_logs. */
public class IdVerificationDAO {

    public List<IdVerification> getPending() {
        List<IdVerification> list = new ArrayList<>();
        String sql = "SELECT v.verification_id, v.user_id, u.full_name, u.email, v.id_type, "
                   + "v.front_image_path, v.back_image_path, v.status, v.rejection_reason, "
                   + "v.submitted_at "
                   + "FROM id_verifications v JOIN users u ON v.user_id = u.user_id "
                   + "WHERE v.status = 'pending' ORDER BY v.submitted_at";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new IdVerification(
                        rs.getInt("verification_id"), rs.getInt("user_id"),
                        rs.getString("full_name"), rs.getString("email"),
                        rs.getString("id_type"), rs.getString("front_image_path"),
                        rs.getString("back_image_path"), rs.getString("status"),
                        rs.getString("rejection_reason"), rs.getString("submitted_at")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int countPending() {
        String sql = "SELECT COUNT(*) FROM id_verifications WHERE status = 'pending'";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean approve(IdVerification v, int adminId) {
        return review(v, adminId, true, null);
    }

    public boolean reject(IdVerification v, int adminId, String reason) {
        return review(v, adminId, false, reason);
    }

    /**
     * One transaction: update the submission, update the user (if approved),
     * send a notification, and write the admin log. All of it happens, or none of it.
     */
    private boolean review(IdVerification v, int adminId, boolean approved, String reason) {
        String newStatus = approved ? "approved" : "rejected";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql = "UPDATE id_verifications SET status = ?, rejection_reason = ?, "
                           + "reviewed_by = ?, reviewed_at = NOW() "
                           + "WHERE verification_id = ? AND status = 'pending'";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, newStatus);
                    ps.setString(2, reason);   // null when approved
                    ps.setInt(3, adminId);
                    ps.setInt(4, v.getVerificationId());
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("This submission was already reviewed.");
                    }
                }

                NotificationDAO notifications = new NotificationDAO();
                AdminLogDAO logs = new AdminLogDAO();

                if (approved) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE users SET is_id_verified = 1 WHERE user_id = ?")) {
                        ps.setInt(1, v.getUserId());
                        ps.executeUpdate();
                    }
                    notifications.add(conn, v.getUserId(), "id_approved",
                            "Your ID is approved.", v.getVerificationId());
                    logs.add(conn, adminId, "approve_id", "id_verification",
                            v.getVerificationId(), null);
                } else {
                    notifications.add(conn, v.getUserId(), "id_rejected",
                            "Your ID was rejected: " + reason, v.getVerificationId());
                    logs.add(conn, adminId, "reject_id", "id_verification",
                            v.getVerificationId(), reason);
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
