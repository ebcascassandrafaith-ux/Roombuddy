package com.template.dao;

import com.template.AppConfig;
import com.template.DBConnection;
import com.template.model.Listing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Bookmarks. Uses the `saved_listings` table. */
public class SavedListingDAO {

    // Used only while AppConfig.DEMO_MODE is true. Keeps the order things were saved in.
    private static final Set<String> DEMO_SAVED = new LinkedHashSet<>();

    public boolean isSaved(int userId, int listingId) throws SQLException {
        if (AppConfig.DEMO_MODE) return DEMO_SAVED.contains(userId + ":" + listingId);
        try (Connection con = DBConnection.getConnection()) {
            return exists(con, userId, listingId);
        }
    }

    /**
     * Flowchart: "add/remove list from Saved_Listings and update icon to fill & unfill".
     * @return true if the listing is saved after the call, false if it was removed
     */
    public boolean toggle(int userId, int listingId) throws SQLException {
        if (AppConfig.DEMO_MODE) {
            String key = userId + ":" + listingId;
            if (DEMO_SAVED.remove(key)) return false;
            DEMO_SAVED.add(key);
            return true;
        }
        try (Connection con = DBConnection.getConnection()) {
            if (exists(con, userId, listingId)) {
                delete(con, userId, listingId);
                return false;
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO saved_listings (user_id, listing_id) VALUES (?, ?)")) {
                ps.setInt(1, userId);
                ps.setInt(2, listingId);
                ps.executeUpdate();
            }
            return true;
        }
    }

    /** Removes one bookmark (the button on the Saved screen). Does nothing if it was not saved. */
    public void unsave(int userId, int listingId) throws SQLException {
        if (AppConfig.DEMO_MODE) {
            DEMO_SAVED.remove(userId + ":" + listingId);
            return;
        }
        try (Connection con = DBConnection.getConnection()) {
            delete(con, userId, listingId);
        }
    }

    /**
     * Flowchart: "Query DB for saved listings".
     * Newest bookmark first. Listings that were unpublished, rejected or marked occupied are left out
     * ("auto hide ... remove from saved"), so the user never opens a listing that is gone.
     */
    public List<Listing> findSavedListings(int userId) throws SQLException {
        if (AppConfig.DEMO_MODE) {
            List<Listing> all = SampleData.listings();
            List<Listing> result = new ArrayList<>();
            for (String key : DEMO_SAVED) {
                if (!key.startsWith(userId + ":")) continue;
                int id = Integer.parseInt(key.substring(key.indexOf(':') + 1));
                for (Listing l : all) {
                    if (l.getListingId() == id) result.add(l);
                }
            }
            Collections.reverse(result);   // newest bookmark first
            return result;
        }

        String sql =
                "SELECT l.listing_id, l.owner_id, l.property_name, l.property_type, l.barangay, "
                        + "       l.address, l.price, "
                        + "       (SELECT p.file_path FROM listing_photos p "
                        + "         WHERE p.listing_id = l.listing_id "
                        + "         ORDER BY p.is_cover DESC, p.photo_id ASC LIMIT 1) AS cover_photo "
                        + "FROM saved_listings s JOIN listings l ON l.listing_id = s.listing_id "
                        + "WHERE s.user_id = ? AND l.status = 'verified' AND l.is_occupied = 0 "
                        + "ORDER BY s.saved_at DESC, s.saved_id DESC";

        List<Listing> result = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Listing l = new Listing();
                    l.setListingId(rs.getInt("listing_id"));
                    l.setOwnerId(rs.getInt("owner_id"));
                    l.setPropertyName(rs.getString("property_name"));
                    l.setPropertyType(rs.getString("property_type"));
                    l.setBarangay(rs.getString("barangay"));
                    l.setAddress(rs.getString("address"));
                    l.setPrice(rs.getDouble("price"));
                    l.setCoverPhoto(rs.getString("cover_photo"));
                    result.add(l);
                }
            }
        }
        return result;
    }

    private static boolean exists(Connection con, int userId, int listingId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT 1 FROM saved_listings WHERE user_id = ? AND listing_id = ? LIMIT 1")) {
            ps.setInt(1, userId);
            ps.setInt(2, listingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static void delete(Connection con, int userId, int listingId) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "DELETE FROM saved_listings WHERE user_id = ? AND listing_id = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, listingId);
            ps.executeUpdate();
        }
    }
}
