package com.template.dao;

import com.template.AppConfig;
import com.template.DBConnection;
import com.template.model.Room;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Reads the rooms of a house (tables `rooms` and `room_photos`; create them with add_rooms_tables.sql). */
public class RoomDAO {

    // The subselect picks the room's cover photo (is_cover = 1 first, otherwise the oldest photo).
    private static final String ROOM_SELECT =
            "SELECT r.room_id, r.listing_id, r.room_name, r.description, r.price, r.room_type, "
                    + "       r.advance_months, r.deposit_months, r.is_occupied, "
                    + "       (SELECT p.file_path FROM room_photos p WHERE p.room_id = r.room_id "
                    + "         ORDER BY p.is_cover DESC, p.photo_id ASC LIMIT 1) AS cover_photo "
                    + "FROM rooms r ";

    /** Every room of one house, available ones and occupied ones, in the order they were added. */
    public List<Room> findRooms(int listingId) throws SQLException {
        if (AppConfig.DEMO_MODE) return SampleData.rooms(listingId);

        List<Room> rooms = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(ROOM_SELECT + "WHERE r.listing_id = ? ORDER BY r.room_id")) {
            ps.setInt(1, listingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) rooms.add(map(rs));
            }
        }
        return rooms;
    }

    /** One room with all of its photos, or null if it was deleted. */
    public Room findRoom(int roomId) throws SQLException {
        if (AppConfig.DEMO_MODE) return SampleData.room(roomId);

        try (Connection con = DBConnection.getConnection()) {
            Room room;
            try (PreparedStatement ps = con.prepareStatement(ROOM_SELECT + "WHERE r.room_id = ?")) {
                ps.setInt(1, roomId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return null;
                    room = map(rs);
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "SELECT file_path FROM room_photos WHERE room_id = ? ORDER BY is_cover DESC, photo_id ASC")) {
                ps.setInt(1, roomId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) room.getPhotos().add(rs.getString("file_path"));
                }
            }
            return room;
        }
    }

    /**
     * Keeps `listings.price` equal to the cheapest room, so the home cards, search and the price filter keep
     * working. Call it after rooms are added, edited or deleted (Member B's owner form).
     */
    public void syncListingPrice(int listingId) throws SQLException {
        if (AppConfig.DEMO_MODE) return;
        String sql = "UPDATE listings SET price = (SELECT MIN(price) FROM rooms WHERE listing_id = ?) "
                + "WHERE listing_id = ? AND EXISTS (SELECT 1 FROM rooms WHERE listing_id = ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, listingId);
            ps.setInt(2, listingId);
            ps.setInt(3, listingId);
            ps.executeUpdate();
        }
    }

    private static Room map(ResultSet rs) throws SQLException {
        Room r = new Room();
        r.setRoomId(rs.getInt("room_id"));
        r.setListingId(rs.getInt("listing_id"));
        r.setRoomName(rs.getString("room_name"));
        r.setDescription(rs.getString("description"));
        r.setPrice(rs.getDouble("price"));
        r.setRoomType(rs.getString("room_type"));
        r.setAdvanceMonths(rs.getInt("advance_months"));
        r.setDepositMonths(rs.getInt("deposit_months"));
        r.setOccupied(rs.getInt("is_occupied") == 1);
        r.setCoverPhoto(rs.getString("cover_photo"));
        return r;
    }
}
