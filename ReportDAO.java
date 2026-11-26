package com.template.dao;

import com.template.DBConnection;
import com.template.model.Report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Table: reports. */
public class ReportDAO {

    /** Open reports first, then newest first. LEFT JOIN keeps reports whose user or listing is empty. */
    public List<Report> getAll() {
        List<Report> list = new ArrayList<>();
        String sql = "SELECT r.report_id, rep.full_name AS reporter_name, "
                + "ru.full_name AS reported_name, l.property_name, r.reason, r.status, r.created_at "
                + "FROM reports r "
                + "JOIN users rep ON r.reporter_id = rep.user_id "
                + "LEFT JOIN users ru ON r.reported_user_id = ru.user_id "
                + "LEFT JOIN listings l ON r.listing_id = l.listing_id "
                + "ORDER BY r.status ASC, r.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Report(rs.getInt("report_id"),
                        rs.getString("reporter_name"),
                        dash(rs.getString("reported_name")),
                        dash(rs.getString("property_name")),
                        rs.getString("reason"), rs.getString("status"),
                        rs.getString("created_at")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private String dash(String s) {
        return s == null ? "-" : s;
    }

    public boolean markResolved(int reportId) {
        String sql = "UPDATE reports SET status = 'resolved' WHERE report_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reportId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int countOpen() {
        String sql = "SELECT COUNT(*) FROM reports WHERE status = 'open'";
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

    public int countAgainstUser(int userId) {
        String sql = "SELECT COUNT(*) FROM reports WHERE reported_user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
