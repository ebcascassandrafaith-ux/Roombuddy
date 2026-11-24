package com.template.admin.dao;

import com.template.DBConnection;
import com.template.admin.model.AdminListing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tables: listings (+ users for the owner's name), listing_amenities + amenities,
 * listing_photos, listing_documents, notifications, admin_logs.
 * (Named AdminListingDAO so it is not confused with the browse side's ListingDAO.)
 */
public class AdminListingDAO {

    private static final String SELECT_LISTINGS =
            "SELECT l.listing_id, l.owner_id, u.full_name AS owner_name, l.property_name, "
          + "l.property_type, l.description, l.barangay, l.address, l.price, l.room_type, "
          + "l.house_rules, l.status, l.rejection_reason, l.is_occupied, l.created_at "
          + "FROM listings l JOIN users u ON l.owner_id = u.user_id ";

    public List<AdminListing> getByStatus(String status) {
        return query(SELECT_LISTINGS + "WHERE l.status = ? ORDER BY l.created_at", status);
    }

    public List<AdminListing> getAll() {
        return query(SELECT_LISTINGS + "ORDER BY l.listing_id", null);
    }

    private List<AdminListing> query(String sql, String statusParam) {
        List<AdminListing> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (statusParam != null) {
                ps.setString(1, statusParam);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new AdminListing(
                            rs.getInt("listing_id"), rs.getInt("owner_id"),
                            rs.getString("owner_name"), rs.getString("property_name"),
                            rs.getString("property_type"), rs.getString("description"),
                            rs.getString("barangay"), rs.getString("address"),
                            rs.getDouble("price"), rs.getString("room_type"),
                            rs.getString("house_rules"), rs.getString("status"),
                            rs.getString("rejection_reason"),
                            rs.getInt("is_occupied") == 1 ? "Yes" : "No",
                            rs.getString("created_at")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM listings WHERE status = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
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

    public int countByOwner(int ownerId) {
        String sql = "SELECT COUNT(*) FROM listings WHERE owner_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
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

    public boolean approve(AdminListing l, int adminId) {
        return review(l, adminId, "verified", null);
    }

    public boolean reject(AdminListing l, int adminId, String reason) {
        return review(l, adminId, "rejected", reason);
    }

    /** One transaction: update the listing, notify the owner, write the admin log. */
    private boolean review(AdminListing l, int adminId, String newStatus, String reason) {
        boolean approved = newStatus.equals("verified");
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql = "UPDATE listings SET status = ?, rejection_reason = ?, "
                           + "reviewed_by = ?, reviewed_at = NOW() "
                           + "WHERE listing_id = ? AND status = 'pending'";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, newStatus);
                    ps.setString(2, reason);   // null when approved
                    ps.setInt(3, adminId);
                    ps.setInt(4, l.getListingId());
                    if (ps.executeUpdate() == 0) {
                        throw new SQLException("This listing was already reviewed.");
                    }
                }
                if (approved) {
                    new NotificationDAO().add(conn, l.getOwnerId(), "listing_approved",
                            "Your listing is Approved and now live!", l.getListingId());
                    new AdminLogDAO().add(conn, adminId, "approve_listing", "listing",
                            l.getListingId(), l.getPropertyName());
                } else {
                    new NotificationDAO().add(conn, l.getOwnerId(), "listing_rejected",
                            "Your listing was rejected: " + reason, l.getListingId());
                    new AdminLogDAO().add(conn, adminId, "reject_listing", "listing",
                            l.getListingId(), reason);
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

    /** UPDATE: the admin edits a listing. */
    public boolean update(AdminListing l, int adminId) {
        String sql = "UPDATE listings SET property_name = ?, property_type = ?, description = ?, "
                   + "barangay = ?, address = ?, price = ?, room_type = ?, house_rules = ? "
                   + "WHERE listing_id = ?";
        Object[] params = { l.getPropertyName(), l.getPropertyType(), l.getDescription(),
                l.getBarangay(), l.getAddress(), l.getPrice(), l.getRoomType(),
                l.getHouseRules(), l.getListingId() };
        return runWithLog(sql, params, adminId, "edit_listing", l);
    }

    /** Unpublish = set status to 'hidden'. */
    public boolean unpublish(AdminListing l, int adminId) {
        String sql = "UPDATE listings SET status = 'hidden' WHERE listing_id = ?";
        return runWithLog(sql, new Object[] { l.getListingId() }, adminId, "unpublish_listing", l);
    }

    /** DELETE: photos, documents and amenities of the listing go too (ON DELETE CASCADE). */
    public boolean delete(AdminListing l, int adminId) {
        String sql = "DELETE FROM listings WHERE listing_id = ?";
        return runWithLog(sql, new Object[] { l.getListingId() }, adminId, "delete_listing", l);
    }

    /**
     * Runs one SQL statement and writes an admin log, in one transaction.
     * The loop fills each ? in the SQL with the matching value from "params".
     */
    private boolean runWithLog(String sql, Object[] params, int adminId, String action, AdminListing l) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (int i = 0; i < params.length; i++) {
                        ps.setObject(i + 1, params[i]);
                    }
                    ps.executeUpdate();
                }
                new AdminLogDAO().add(conn, adminId, action, "listing",
                        l.getListingId(), l.getPropertyName());
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

    /** Builds the text for the "details" box: listing info + amenities + photo and document paths. */
    public String getDetailsText(AdminListing l) {
        StringBuilder sb = new StringBuilder();
        sb.append("Property: ").append(l.getPropertyName()).append("\n");
        sb.append("Owner: ").append(l.getOwnerName()).append("\n");
        sb.append("Type: ").append(l.getPropertyType())
          .append("    Room type: ").append(l.getRoomType()).append("\n");
        sb.append("Price: ").append(l.getPriceText()).append("\n");
        sb.append("Barangay: ").append(l.getBarangay()).append("\n");
        sb.append("Address: ").append(l.getAddress()).append("\n");
        sb.append("Description: ").append(show(l.getDescription())).append("\n");
        sb.append("House rules: ").append(show(l.getHouseRules())).append("\n");
        if (l.getRejectionReason() != null) {
            sb.append("Rejection reason: ").append(l.getRejectionReason()).append("\n");
        }

        sb.append("\nAmenities:\n");
        appendList(sb, getAmenities(l.getListingId()));
        sb.append("\nPhotos:\n");
        appendList(sb, getPhotos(l.getListingId()));
        sb.append("\nDocuments:\n");
        appendList(sb, getDocuments(l.getListingId()));
        return sb.toString();
    }

    private String show(String s) {
        return (s == null || s.isEmpty()) ? "-" : s;
    }

    private void appendList(StringBuilder sb, List<String> items) {
        if (items.isEmpty()) {
            sb.append("  (none)\n");
        }
        for (String item : items) {
            sb.append("  - ").append(item).append("\n");
        }
    }

    private List<String> getAmenities(int listingId) {
        String sql = "SELECT a.amenity_name, la.extra_cost FROM listing_amenities la "
                   + "JOIN amenities a ON la.amenity_id = a.amenity_id WHERE la.listing_id = ?";
        List<String> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, listingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String text = rs.getString("amenity_name");
                    if (rs.getString("extra_cost") != null) {
                        text += " (extra cost: " + rs.getString("extra_cost") + ")";
                    }
                    list.add(text);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private List<String> getPhotos(int listingId) {
        String sql = "SELECT file_path, is_cover FROM listing_photos WHERE listing_id = ?";
        List<String> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, listingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String text = rs.getString("file_path");
                    if (rs.getInt("is_cover") == 1) {
                        text += "  [cover]";
                    }
                    list.add(text);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private List<String> getDocuments(int listingId) {
        String sql = "SELECT document_type, file_path FROM listing_documents WHERE listing_id = ?";
        List<String> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, listingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("document_type") + ": " + rs.getString("file_path"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
