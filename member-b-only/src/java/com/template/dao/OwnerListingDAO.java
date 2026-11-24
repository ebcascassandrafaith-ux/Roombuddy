package com.template.dao;

import com.template.DBConnection;
import com.template.model.OwnerListing;

import java.sql.*;
import java.util.*;

/**
 * Member B: everything the landlord does with their own listings.
 * Written against database/roombuddy.sql (listings, listing_photos, listing_amenities,
 * listing_documents, amenities). Every write is guarded with owner_id so nobody can touch another owner's listing.
 */
public class OwnerListingDAO {

    // ------------------------------------------------------------------ reading

    public List<OwnerListing> findByOwner(int ownerId) throws SQLException {
        List<OwnerListing> out = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM listings WHERE owner_id = ? ORDER BY created_at DESC, listing_id DESC")) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
            for (OwnerListing l : out) attachDetails(con, l);
        }
        return out;
    }

    /** The owner's listing with photos, amenities and documents, or null. */
    public OwnerListing findById(int listingId, int ownerId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM listings WHERE listing_id = ? AND owner_id = ?")) {
            ps.setInt(1, listingId);
            ps.setInt(2, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                OwnerListing l = map(rs);
                attachDetails(con, l);
                return l;
            }
        }
    }

    /** Amenity names exactly as stored in the `amenities` table (so the form never drifts from the DB). */
    public List<String> amenityNames() throws SQLException {
        List<String> out = new ArrayList<>();
        try (Connection con = DBConnection.getConnection(); Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT amenity_name FROM amenities ORDER BY amenity_id")) {
            while (rs.next()) out.add(rs.getString(1));
        }
        return out;
    }

    /** Flowchart: "Is ID verified?" Landlords must have a verified ID before posting. */
    public boolean isIdVerified(int userId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT is_id_verified FROM users WHERE user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) != 0; }
        }
    }

    // ------------------------------------------------------------------ writing

    /** Saves a new listing with status 'pending' (waiting for admin approval). */
    public int insert(OwnerListing l, List<String> photoPaths, List<OwnerListing.Doc> docs) throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO listings (owner_id, property_name, property_type, description, barangay, address, "
                      + "price, room_type, house_rules, status) VALUES (?,?,?,?,?,?,?,?,?, 'pending')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, l.getOwnerId());
                    bindFields(ps, l, 2);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) { rs.next(); id = rs.getInt(1); }
                }
                writeAmenities(con, id, l.getAmenities());
                writePhotos(con, id, photoPaths);
                writeDocuments(con, id, docs);
                con.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /**
     * Saves edits to an existing listing.
     * @param photoPaths null = leave the photos as they are
     * @param docs       null = leave the documents as they are
     * @param resetToPending true when price, address, photos or documents changed (or a rejected listing is resubmitted)
     */
    public void update(OwnerListing l, List<String> photoPaths, List<OwnerListing.Doc> docs, boolean resetToPending)
            throws SQLException {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                String sql = "UPDATE listings SET property_name=?, property_type=?, description=?, barangay=?, address=?, "
                        + "price=?, room_type=?, house_rules=?"
                        + (resetToPending ? ", status='pending', rejection_reason=NULL, reviewed_by=NULL, reviewed_at=NULL" : "")
                        + " WHERE listing_id=? AND owner_id=?";
                int changed;
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    bindFields(ps, l, 1);
                    ps.setInt(9, l.getId());
                    ps.setInt(10, l.getOwnerId());
                    changed = ps.executeUpdate();
                }
                if (changed == 0) throw new SQLException("Listing not found or not yours.");
                run(con, "DELETE FROM listing_amenities WHERE listing_id=?", l.getId());
                writeAmenities(con, l.getId(), l.getAmenities());
                if (photoPaths != null) {
                    run(con, "DELETE FROM listing_photos WHERE listing_id=?", l.getId());
                    writePhotos(con, l.getId(), photoPaths);
                }
                if (docs != null) {
                    run(con, "DELETE FROM listing_documents WHERE listing_id=?", l.getId());
                    writeDocuments(con, l.getId(), docs);
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Permanently removes the listing (photos, amenities, documents, saved hearts cascade in the DB). */
    public void delete(int listingId, int ownerId) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM listings WHERE listing_id=? AND owner_id=?")) {
            ps.setInt(1, listingId);
            ps.setInt(2, ownerId);
            ps.executeUpdate();
        }
    }

    public void setOccupied(int listingId, int ownerId, boolean occupied) throws SQLException {
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("UPDATE listings SET is_occupied=? WHERE listing_id=? AND owner_id=?")) {
            ps.setInt(1, occupied ? 1 : 0);
            ps.setInt(2, listingId);
            ps.setInt(3, ownerId);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ internals

    /** Binds the 8 editable columns starting at parameter index `start`. */
    private void bindFields(PreparedStatement ps, OwnerListing l, int start) throws SQLException {
        ps.setString(start, l.getPropertyName());
        ps.setString(start + 1, l.getPropertyType());
        ps.setString(start + 2, l.getDescription());
        ps.setString(start + 3, l.getBarangay());
        ps.setString(start + 4, l.getAddress());
        ps.setDouble(start + 5, l.getPrice());
        ps.setString(start + 6, l.getRoomType());
        ps.setString(start + 7, l.getHouseRules());
    }

    private OwnerListing map(ResultSet rs) throws SQLException {
        OwnerListing l = new OwnerListing();
        l.setId(rs.getInt("listing_id"));
        l.setOwnerId(rs.getInt("owner_id"));
        l.setPropertyName(rs.getString("property_name"));
        l.setPropertyType(rs.getString("property_type"));
        l.setDescription(rs.getString("description"));
        l.setBarangay(rs.getString("barangay"));
        l.setAddress(rs.getString("address"));
        l.setPrice(rs.getDouble("price"));
        l.setRoomType(rs.getString("room_type"));
        l.setHouseRules(rs.getString("house_rules"));
        l.setStatus(rs.getString("status"));
        l.setRejectionReason(rs.getString("rejection_reason"));
        l.setOccupied(rs.getInt("is_occupied") != 0);
        return l;
    }

    private void attachDetails(Connection con, OwnerListing l) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT file_path FROM listing_photos WHERE listing_id=? ORDER BY is_cover DESC, photo_id")) {
            ps.setInt(1, l.getId());
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) l.getPhotos().add(rs.getString(1)); }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT a.amenity_name, la.extra_cost FROM listing_amenities la "
              + "JOIN amenities a ON a.amenity_id = la.amenity_id WHERE la.listing_id=? ORDER BY la.listing_amenity_id")) {
            ps.setInt(1, l.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double cost = rs.getDouble(2);
                    l.getAmenities().put(rs.getString(1), rs.wasNull() ? null : cost);
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT document_type, file_path FROM listing_documents WHERE listing_id=? ORDER BY document_id")) {
            ps.setInt(1, l.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) l.getDocuments().add(new OwnerListing.Doc(rs.getString(1), rs.getString(2)));
            }
        }
    }

    private void writeAmenities(Connection con, int listingId, Map<String, Double> amenities) throws SQLException {
        Map<String, Integer> ids = new HashMap<>();
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT amenity_id, amenity_name FROM amenities")) {
            while (rs.next()) ids.put(rs.getString(2), rs.getInt(1));
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO listing_amenities (listing_id, amenity_id, extra_cost) VALUES (?,?,?)")) {
            for (Map.Entry<String, Double> e : amenities.entrySet()) {
                Integer amenityId = ids.get(e.getKey());
                if (amenityId == null) continue;
                ps.setInt(1, listingId);
                ps.setInt(2, amenityId);
                if (e.getValue() == null || e.getValue() <= 0) ps.setNull(3, Types.DECIMAL);
                else ps.setDouble(3, e.getValue());
                ps.executeUpdate();
            }
        }
    }

    private void writePhotos(Connection con, int listingId, List<String> paths) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO listing_photos (listing_id, file_path, is_cover) VALUES (?,?,?)")) {
            for (int i = 0; i < paths.size(); i++) {
                ps.setInt(1, listingId);
                ps.setString(2, paths.get(i));
                ps.setInt(3, i == 0 ? 1 : 0);
                ps.executeUpdate();
            }
        }
    }

    private void writeDocuments(Connection con, int listingId, List<OwnerListing.Doc> docs) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO listing_documents (listing_id, document_type, file_path) VALUES (?,?,?)")) {
            for (OwnerListing.Doc d : docs) {
                ps.setInt(1, listingId);
                ps.setString(2, d.type);
                ps.setString(3, d.path);
                ps.executeUpdate();
            }
        }
    }

    private static void run(Connection con, String sql, int id) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
