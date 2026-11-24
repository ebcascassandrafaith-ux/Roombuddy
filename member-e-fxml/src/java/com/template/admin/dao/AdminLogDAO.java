package com.template.admin.dao;

import com.template.DBConnection;
import com.template.admin.model.AdminLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Table: admin_logs. */
public class AdminLogDAO {

    /** CREATE. Used inside other DAOs' transactions, so it receives their open connection. */
    public void add(Connection conn, int adminId, String action, String targetType,
                    int targetId, String remarks) throws SQLException {
        String sql = "INSERT INTO admin_logs (admin_id, action, target_type, target_id, remarks) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, adminId);
            ps.setString(2, action);
            ps.setString(3, targetType);
            ps.setInt(4, targetId);
            ps.setString(5, remarks);   // may be null
            ps.executeUpdate();
        }
    }

    /** READ. Newest first. */
    public List<AdminLog> getAll() {
        List<AdminLog> list = new ArrayList<>();
        String sql = "SELECT l.log_id, u.full_name, l.action, l.target_type, l.target_id, "
                   + "l.remarks, l.created_at "
                   + "FROM admin_logs l JOIN users u ON l.admin_id = u.user_id "
                   + "ORDER BY l.log_id DESC";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new AdminLog(rs.getInt("log_id"), rs.getString("full_name"),
                        rs.getString("action"), rs.getString("target_type"),
                        rs.getInt("target_id"), rs.getString("remarks"),
                        rs.getString("created_at")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
