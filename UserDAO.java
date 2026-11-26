package com.template.dao;

import com.template.DBConnection;
import com.template.model.AdminUser;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Table: users. */
public class UserDAO {

    private static final String COLUMNS =
            "user_id, full_name, email, contact_number, role, status, is_id_verified, created_at";

    /**
     * Returns the admin if the email and password match an ACTIVE admin, otherwise null.
     * Passwords are stored as BCrypt hashes (the main app's signup uses the same jBCrypt library),
     * so the typed password is checked with BCrypt.checkpw instead of being compared as text.
     */
    public AdminUser loginAdmin(String email, String password) {
        String sql = "SELECT " + COLUMNS + ", password_hash FROM users "
                + "WHERE email = ? AND role = 'admin' AND status = 'active'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && passwordMatches(password, rs.getString("password_hash"))) {
                    return mapUser(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** false for accounts without a password (hash is NULL) or with a hash that is not a BCrypt hash. */
    private boolean passwordMatches(String password, String savedHash) {
        if (savedHash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, savedHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** True only if this user exists, has role 'admin' and status 'active'. */
    public boolean isActiveAdmin(int userId) {
        String sql = "SELECT 1 FROM users WHERE user_id = ? AND role = 'admin' AND status = 'active'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getFullName(int userId) {
        String sql = "SELECT full_name FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("full_name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Admin";
    }

    /** READ all users. */
    public List<AdminUser> getAllUsers() {
        List<AdminUser> list = new ArrayList<>();
        String sql = "SELECT " + COLUMNS + " FROM users ORDER BY user_id";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int countAll() {
        String sql = "SELECT COUNT(*) FROM users";
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

    /** UPDATE. newStatus is 'suspended', 'banned', or 'active' (restore). The reason goes into admin_logs.remarks. */
    public boolean updateStatus(int userId, String newStatus, int adminId, String reason) {
        String action = newStatus.equals("banned") ? "ban_user"
                : newStatus.equals("active") ? "restore_user" : "suspend_user";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE users SET status = ? WHERE user_id = ?")) {
                    ps.setString(1, newStatus);
                    ps.setInt(2, userId);
                    ps.executeUpdate();
                }
                new AdminLogDAO().add(conn, adminId, action, "user", userId, reason);
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

    /** DELETE. The user's listings, IDs, etc. go too (ON DELETE CASCADE in the database). */
    public boolean deleteUser(int userId, int adminId, String reason) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM users WHERE user_id = ?")) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                }
                new AdminLogDAO().add(conn, adminId, "delete_user", "user", userId, reason);
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

    /** Turns the current row of the ResultSet into an AdminUser object. */
    private AdminUser mapUser(ResultSet rs) throws SQLException {
        return new AdminUser(
                rs.getInt("user_id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("contact_number"),
                rs.getString("role"),
                rs.getString("status"),
                rs.getInt("is_id_verified") == 1 ? "Yes" : "No",
                rs.getString("created_at"));
    }
}