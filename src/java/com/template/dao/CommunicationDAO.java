package com.template.dao;

import com.template.AppConfig;
import com.template.DBConnection;
import com.template.model.Message;
import com.template.model.Notification;
import com.template.model.ViewingSchedule;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CommunicationDAO {

    private static final List<Message> DEMO_MESSAGES = new ArrayList<>();
    private static final List<ViewingSchedule> DEMO_SCHEDULES = new ArrayList<>();
    private static final List<Notification> DEMO_NOTIFICATIONS = new ArrayList<>();
    private static final AtomicInteger NEXT_SCHEDULE_ID = new AtomicInteger(100);
    private static final AtomicInteger NEXT_NOTIFICATION_ID = new AtomicInteger(200);

    public int getOrCreateConversation(int renterId, int landlordId, int listingId) throws SQLException {
        if (AppConfig.DEMO_MODE) return 1;

        String find = "SELECT conversation_id FROM conversations "
                + "WHERE listing_id = ? AND ((user_one_id = ? AND user_two_id = ?) OR (user_one_id = ? AND user_two_id = ?)) "
                + "ORDER BY conversation_id DESC LIMIT 1";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(find)) {
            ps.setInt(1, listingId);
            ps.setInt(2, renterId);
            ps.setInt(3, landlordId);
            ps.setInt(4, landlordId);
            ps.setInt(5, renterId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO conversations (user_one_id, user_two_id, listing_id) VALUES (?, ?, ?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, renterId);
            ps.setInt(2, landlordId);
            ps.setInt(3, listingId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        throw new SQLException("Could not create conversation.");
    }

    public List<Message> findMessages(int conversationId) throws SQLException {
        if (AppConfig.DEMO_MODE) {
            List<Message> result = new ArrayList<>();
            for (Message message : DEMO_MESSAGES) {
                if (message.getConversationId() == conversationId) result.add(message);
            }
            return result;
        }

        List<Message> result = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT message_id, conversation_id, sender_id, receiver_id, message_text, status, sent_at "
                             + "FROM messages WHERE conversation_id = ? ORDER BY sent_at ASC, message_id ASC")) {
            ps.setInt(1, conversationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) result.add(mapMessage(rs));
            }
        }
        return result;
    }

    public void sendMessage(int conversationId, int senderId, int receiverId, int listingId, String text) throws SQLException {
        if (text == null || text.trim().isEmpty()) throw new SQLException("Message cannot be empty.");

        if (AppConfig.DEMO_MODE) {
            Message message = new Message();
            message.setMessageId(DEMO_MESSAGES.size() + 1);
            message.setConversationId(conversationId);
            message.setSenderId(senderId);
            message.setReceiverId(receiverId);
            message.setText(text.trim());
            message.setStatus("sent");
            message.setSentAt(LocalDateTime.now());
            DEMO_MESSAGES.add(message);
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO messages (conversation_id, sender_id, receiver_id, message_text, status) "
                            + "VALUES (?, ?, ?, ?, 'sent')")) {
                ps.setInt(1, conversationId);
                ps.setInt(2, senderId);
                ps.setInt(3, receiverId);
                ps.setString(4, text.trim());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO notifications (user_id, type, message, related_id, is_read) "
                            + "VALUES (?, 'new_message', ?, ?, 0)")) {
                ps.setInt(1, receiverId);
                ps.setString(2, "You have a new message");
                ps.setInt(3, listingId);
                ps.executeUpdate();
            }
        }
    }

    public void markConversationRead(int conversationId, int userId) throws SQLException {
        if (AppConfig.DEMO_MODE) {
            for (Message message : DEMO_MESSAGES) {
                if (message.getConversationId() == conversationId && message.getReceiverId() == userId) {
                    message.setStatus("read");
                }
            }
            return;
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE messages SET status = 'read' WHERE conversation_id = ? AND receiver_id = ?")) {
            ps.setInt(1, conversationId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public List<Notification> findNotifications(int userId) throws SQLException {
        if (AppConfig.DEMO_MODE) {
            List<Notification> result = new ArrayList<>();
            for (Notification notification : DEMO_NOTIFICATIONS) {
                if (notification.getUserId() == userId) result.add(notification);
            }
            return result;
        }

        List<Notification> result = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT notification_id, type, message, related_id, is_read, created_at "
                             + "FROM notifications WHERE user_id = ? ORDER BY created_at DESC, notification_id DESC")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Notification notification = new Notification();
                    notification.setNotificationId(rs.getInt("notification_id"));
                    notification.setType(rs.getString("type"));
                    notification.setMessage(rs.getString("message"));
                    int related = rs.getInt("related_id");
                    notification.setRelatedId(rs.wasNull() ? null : related);
                    notification.setRead(rs.getInt("is_read") == 1);
                    Timestamp created = rs.getTimestamp("created_at");
                    if (created != null) notification.setCreatedAt(created.toLocalDateTime());
                    result.add(notification);
                }
            }
        }
        return result;
    }

    public void markAllNotificationsRead(int userId) throws SQLException {
        if (AppConfig.DEMO_MODE) {
            for (Notification notification : DEMO_NOTIFICATIONS) {
                if (notification.getUserId() == userId) notification.setRead(true);
            }
            return;
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE notifications SET is_read = 1 WHERE user_id = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public ViewingSchedule createViewingRequest(int listingId, int renterId, int landlordId,
                                                LocalDateTime scheduledAt) throws SQLException {
        if (scheduledAt == null || scheduledAt.isBefore(LocalDateTime.now())) {
            throw new SQLException("Choose a future date and time.");
        }

        if (AppConfig.DEMO_MODE) {
            ViewingSchedule schedule = new ViewingSchedule();
            schedule.setViewingId(NEXT_SCHEDULE_ID.getAndIncrement());
            schedule.setListingId(listingId);
            schedule.setRenterId(renterId);
            schedule.setLandlordId(landlordId);
            schedule.setScheduledAt(scheduledAt);
            schedule.setProposedBy(renterId);
            schedule.setStatus("pending");
            schedule.setCreatedAt(LocalDateTime.now());
            DEMO_SCHEDULES.add(schedule);
            addDemoNotification(landlordId, "viewing_request",
                    "You have a new viewing request", schedule.getViewingId());
            return schedule;
        }

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO viewing_schedules "
                             + "(listing_id, renter_id, landlord_id, scheduled_at, proposed_by, status) "
                             + "VALUES (?, ?, ?, ?, ?, 'pending')",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, listingId);
            ps.setInt(2, renterId);
            ps.setInt(3, landlordId);
            ps.setTimestamp(4, Timestamp.valueOf(scheduledAt));
            ps.setInt(5, renterId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Could not create viewing request.");
                ViewingSchedule schedule = new ViewingSchedule();
                schedule.setViewingId(keys.getInt(1));
                schedule.setListingId(listingId);
                schedule.setRenterId(renterId);
                schedule.setLandlordId(landlordId);
                schedule.setScheduledAt(scheduledAt);
                schedule.setProposedBy(renterId);
                schedule.setStatus("pending");
                schedule.setCreatedAt(LocalDateTime.now());

                try (PreparedStatement notice = con.prepareStatement(
                        "INSERT INTO notifications (user_id, type, message, related_id, is_read) "
                                + "VALUES (?, 'viewing_request', ?, ?, 0)")) {
                    notice.setInt(1, landlordId);
                    notice.setString(2, "You have a new viewing request");
                    notice.setInt(3, schedule.getViewingId());
                    notice.executeUpdate();
                }
                return schedule;
            }
        }
    }

    public List<ViewingSchedule> findViewingSchedules(int listingId, int userId) throws SQLException {
        if (AppConfig.DEMO_MODE) {
            List<ViewingSchedule> result = new ArrayList<>();
            for (ViewingSchedule schedule : DEMO_SCHEDULES) {
                if (schedule.getListingId() == listingId
                        && (schedule.getRenterId() == userId || schedule.getLandlordId() == userId)) {
                    result.add(schedule);
                }
            }
            return result;
        }

        List<ViewingSchedule> result = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT viewing_id, listing_id, renter_id, landlord_id, scheduled_at, proposed_by, status, created_at "
                             + "FROM viewing_schedules WHERE listing_id = ? "
                             + "AND (renter_id = ? OR landlord_id = ?) ORDER BY scheduled_at DESC")) {
            ps.setInt(1, listingId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ViewingSchedule schedule = new ViewingSchedule();
                    schedule.setViewingId(rs.getInt("viewing_id"));
                    schedule.setListingId(rs.getInt("listing_id"));
                    schedule.setRenterId(rs.getInt("renter_id"));
                    schedule.setLandlordId(rs.getInt("landlord_id"));
                    Timestamp scheduled = rs.getTimestamp("scheduled_at");
                    if (scheduled != null) schedule.setScheduledAt(scheduled.toLocalDateTime());
                    schedule.setProposedBy(rs.getInt("proposed_by"));
                    schedule.setStatus(rs.getString("status"));
                    Timestamp created = rs.getTimestamp("created_at");
                    if (created != null) schedule.setCreatedAt(created.toLocalDateTime());
                    result.add(schedule);
                }
            }
        }
        return result;
    }

    public void updateViewingStatus(int viewingId, int receiverId, String status,
                                    LocalDateTime proposedTime) throws SQLException {
        if (!"confirmed".equals(status) && !"declined".equals(status)
                && !"reschedule_proposed".equals(status)) {
            throw new SQLException("Invalid viewing status.");
        }
        if ("reschedule_proposed".equals(status)
                && (proposedTime == null || proposedTime.isBefore(LocalDateTime.now()))) {
            throw new SQLException("Choose a future date and time.");
        }

        if (AppConfig.DEMO_MODE) {
            ViewingSchedule found = null;
            for (ViewingSchedule schedule : DEMO_SCHEDULES) {
                if (schedule.getViewingId() == viewingId) {
                    found = schedule;
                    break;
                }
            }
            if (found == null) throw new SQLException("Viewing request not found.");
            found.setStatus(status);
            if (proposedTime != null) found.setScheduledAt(proposedTime);
            found.setProposedBy(receiverId);
            int notifyUser = receiverId == found.getRenterId() ? found.getLandlordId() : found.getRenterId();
            addDemoNotification(notifyUser, "viewing_update",
                    "A viewing request was " + status.replace('_', ' '), viewingId);
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE viewing_schedules SET status = ?, scheduled_at = COALESCE(?, scheduled_at), proposed_by = ? "
                                + "WHERE viewing_id = ?")) {
                    ps.setString(1, status);
                    if ("reschedule_proposed".equals(status) && proposedTime != null) {
                        ps.setTimestamp(2, Timestamp.valueOf(proposedTime));
                    } else {
                        ps.setTimestamp(2, null);
                    }
                    ps.setInt(3, receiverId);
                    ps.setInt(4, viewingId);
                    if (ps.executeUpdate() == 0) throw new SQLException("Viewing request not found.");
                }

                int notifyUser = 0;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT renter_id, landlord_id FROM viewing_schedules WHERE viewing_id = ?")) {
                    ps.setInt(1, viewingId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int renterId = rs.getInt("renter_id");
                            int landlordId = rs.getInt("landlord_id");
                            notifyUser = receiverId == renterId ? landlordId : renterId;
                        }
                    }
                }
                if (notifyUser > 0) {
                    try (PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO notifications (user_id, type, message, related_id, is_read) "
                                    + "VALUES (?, 'viewing_update', ?, ?, 0)")) {
                        ps.setInt(1, notifyUser);
                        ps.setString(2, "A viewing request was " + status.replace('_', ' '));
                        ps.setInt(3, viewingId);
                        ps.executeUpdate();
                    }
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    private static void addDemoNotification(int userId, String type, String message, int relatedId) {
        Notification notification = new Notification();
        notification.setNotificationId(NEXT_NOTIFICATION_ID.getAndIncrement());
        notification.setUserId(userId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setRelatedId(relatedId);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        DEMO_NOTIFICATIONS.add(notification);
    }

    private static Message mapMessage(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setMessageId(rs.getInt("message_id"));
        message.setConversationId(rs.getInt("conversation_id"));
        message.setSenderId(rs.getInt("sender_id"));
        message.setReceiverId(rs.getInt("receiver_id"));
        message.setText(rs.getString("message_text"));
        message.setStatus(rs.getString("status"));
        Timestamp sent = rs.getTimestamp("sent_at");
        if (sent != null) message.setSentAt(sent.toLocalDateTime());
        return message;
    }
}
