package com.template.dao;

import com.template.AppConfig;
import com.template.DBConnection;
import com.template.model.Amenity;
import com.template.model.Listing;
import com.template.model.ListingDetails;
import com.template.model.ListingGroup;
import com.template.model.SearchCriteria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads listings for renters. Only listings with status = 'verified' that are not occupied are shown.
 * Assumes DBConnection has:  public static Connection getConnection() throws SQLException
 */
public class ListingDAO {

    // The subselect picks the cover photo (is_cover = 1 first, otherwise the oldest photo).
    private static final String CARD_SELECT =
            "SELECT l.listing_id, l.owner_id, l.property_name, l.property_type, l.barangay, "
                    + "       l.address, l.price, "
                    + "       (SELECT p.file_path FROM listing_photos p "
                    + "         WHERE p.listing_id = l.listing_id "
                    + "         ORDER BY p.is_cover DESC, p.photo_id ASC LIMIT 1) AS cover_photo "
                    + "FROM listings l ";

    // ------------------------------------------------------------------ home screen (Step 1)

    /** Everything for the home screen, grouped into rows. Biggest rows first. */
    public List<ListingGroup> findHomeGroups() throws SQLException {
        List<Listing> all = AppConfig.DEMO_MODE ? SampleData.listings() : queryVerifiedListings();

        Map<String, ListingGroup> groups = new LinkedHashMap<>();
        for (Listing l : all) {
            String key = l.getPropertyType() + "|" + l.getBarangay();
            ListingGroup g = groups.get(key);
            if (g == null) {
                g = new ListingGroup(l.getPropertyType(), l.getBarangay());
                groups.put(key, g);
            }
            g.getListings().add(l);
        }

        List<ListingGroup> result = new ArrayList<>(groups.values());
        result.sort(Comparator
                .comparingInt((ListingGroup g) -> g.getListings().size()).reversed()
                .thenComparing(ListingGroup::getTitle));
        return result;
    }

    private List<Listing> queryVerifiedListings() throws SQLException {
        String sql = CARD_SELECT
                + "WHERE l.status = 'verified' AND l.is_occupied = 0 "
                + "ORDER BY l.created_at DESC, l.listing_id DESC";

        List<Listing> listings = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) listings.add(mapCard(rs));
        }
        return listings;
    }

    private static Listing mapCard(ResultSet rs) throws SQLException {
        Listing l = new Listing();
        l.setListingId(rs.getInt("listing_id"));
        l.setOwnerId(rs.getInt("owner_id"));
        l.setPropertyName(rs.getString("property_name"));
        l.setPropertyType(rs.getString("property_type"));
        l.setBarangay(rs.getString("barangay"));
        l.setAddress(rs.getString("address"));
        l.setPrice(rs.getDouble("price"));
        l.setCoverPhoto(rs.getString("cover_photo"));
        return l;
    }

    // ------------------------------------------------------------------ details page (Step 3)

    /**
     * One listing with its photos, amenities and host.
     * @return the details, or null if there is no such listing (the page then says "no longer available")
     */
    public ListingDetails findDetails(int listingId) throws SQLException {
        if (AppConfig.DEMO_MODE) return SampleData.details(listingId);

        String sql = "SELECT l.listing_id, l.owner_id, l.property_name, l.property_type, l.description, "
                + "       l.barangay, l.address, l.price, l.room_type, l.house_rules, l.status, l.is_occupied, "
                + "       u.full_name, u.profile_photo, u.is_id_verified, u.created_at AS host_since "
                + "FROM listings l JOIN users u ON u.user_id = l.owner_id "
                + "WHERE l.listing_id = ?";

        try (Connection con = DBConnection.getConnection()) {
            ListingDetails d;
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, listingId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    d = new ListingDetails();
                    d.setListingId(rs.getInt("listing_id"));
                    d.setOwnerId(rs.getInt("owner_id"));
                    d.setPropertyName(rs.getString("property_name"));
                    d.setPropertyType(rs.getString("property_type"));
                    d.setDescription(rs.getString("description"));
                    d.setBarangay(rs.getString("barangay"));
                    d.setAddress(rs.getString("address"));
                    d.setPrice(rs.getDouble("price"));
                    d.setRoomType(rs.getString("room_type"));
                    d.setHouseRules(rs.getString("house_rules"));
                    d.setStatus(rs.getString("status"));
                    d.setOccupied(rs.getInt("is_occupied") == 1);
                    d.setHostName(rs.getString("full_name"));
                    d.setHostPhoto(rs.getString("profile_photo"));
                    d.setHostVerified(rs.getInt("is_id_verified") == 1);
                    Timestamp since = rs.getTimestamp("host_since");
                    if (since != null) d.setHostSince(since.toLocalDateTime().toLocalDate());
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT file_path FROM listing_photos WHERE listing_id = ? ORDER BY is_cover DESC, photo_id ASC")) {
                ps.setInt(1, listingId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) d.getPhotos().add(rs.getString("file_path"));
                }
            }
            if (!d.getPhotos().isEmpty()) d.setCoverPhoto(d.getPhotos().get(0));

            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT a.amenity_name, la.extra_cost FROM listing_amenities la "
                            + "JOIN amenities a ON a.amenity_id = la.amenity_id "
                            + "WHERE la.listing_id = ? ORDER BY a.amenity_name")) {
                ps.setInt(1, listingId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        double cost = rs.getDouble("extra_cost");
                        Double extra = rs.wasNull() ? null : cost;
                        d.getAmenities().add(new Amenity(rs.getString("amenity_name"), extra));
                    }
                }
            }
            return d;
        }
    }

    /** "Explore more options": other available listings, same barangay first. */
    public List<Listing> findMoreOptions(ListingDetails current, int limit) throws SQLException {
        if (AppConfig.DEMO_MODE) return SampleData.moreOptions(current, limit);

        String sql = CARD_SELECT
                + "WHERE l.status = 'verified' AND l.is_occupied = 0 AND l.listing_id <> ? "
                + "ORDER BY (l.barangay = ?) DESC, l.created_at DESC "
                + "LIMIT ?";

        List<Listing> listings = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, current.getListingId());
            ps.setString(2, current.getBarangay());
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) listings.add(mapCard(rs));
            }
        }
        return listings;
    }

    // ------------------------------------------------------------------ search + filters (Step 2)

    /**
     * Flowchart: "query DB with the search + filter parameters".
     * Keywords match the name, barangay or address (every word must match somewhere).
     * Selected amenities must ALL be offered. Newest listings first.
     */
    public List<Listing> search(SearchCriteria c) throws SQLException {
        if (AppConfig.DEMO_MODE) return SampleData.search(c);

        StringBuilder sql = new StringBuilder(CARD_SELECT)
                .append("WHERE l.status = 'verified' AND l.is_occupied = 0 ");
        List<Object> params = new ArrayList<>();

        for (String word : c.getKeywordWords()) {
            sql.append("AND (l.property_name LIKE ? OR l.barangay LIKE ? OR l.address LIKE ?) ");
            String like = "%" + escapeLike(word) + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (c.getMinPrice() != null) {
            sql.append("AND l.price >= ? ");
            params.add(c.getMinPrice());
        }
        if (c.getMaxPrice() != null) {
            sql.append("AND l.price <= ? ");
            params.add(c.getMaxPrice());
        }
        if (c.getBarangay() != null) {
            sql.append("AND l.barangay = ? ");
            params.add(c.getBarangay());
        }
        if (c.getRoomType() != null) {
            sql.append("AND l.room_type = ? ");
            params.add(c.getRoomType());
        }
        for (String amenity : c.getAmenities()) {
            sql.append("AND EXISTS (SELECT 1 FROM listing_amenities la "
                    + "JOIN amenities am ON am.amenity_id = la.amenity_id "
                    + "WHERE la.listing_id = l.listing_id AND am.amenity_name = ?) ");
            params.add(amenity);
        }
        sql.append("ORDER BY l.created_at DESC, l.listing_id DESC");

        List<Listing> listings = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) listings.add(mapCard(rs));
            }
        }
        return listings;
    }

    /** Barangays that currently have an available listing (the Location filter list). */
    public List<String> findBarangays() throws SQLException {
        if (AppConfig.DEMO_MODE) return SampleData.barangays();
        List<String> result = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT DISTINCT barangay FROM listings "
                             + "WHERE status = 'verified' AND is_occupied = 0 ORDER BY barangay");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(rs.getString(1));
        }
        return result;
    }

    /** Every amenity name in the `amenities` table (the Amenities filter list). */
    public List<String> findAmenityNames() throws SQLException {
        if (AppConfig.DEMO_MODE) return SampleData.amenityNames();
        List<String> result = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT amenity_name FROM amenities ORDER BY amenity_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(rs.getString(1));
        }
        return result;
    }

    /** Makes % and _ typed by the user match literally instead of acting as wildcards. */
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
