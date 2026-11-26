package com.template.dao;

import com.template.AppConfig;
import com.template.DBConnection;
import com.template.model.Conversation;
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
    private static final List<DemoConversation> DEMO_CONVERSATIONS = new ArrayList<>();

    /** A chat in demo mode (no database). */
    private static final class DemoConversation {
        final int id;
        final int userOne;
        final int userTwo;
        final int listingId;

        DemoConversation(int id, int userOne, int userTwo, int listingId) {
            this.id = id;
            this.userOne = userOne;
            this.userTwo = userTwo;
            this.listingId = listingId;
        }
    }

    public int getOrCreateConversation(int renterId, int landlordId, int listingId) throws SQLException {
        if (AppConfig.DEMO_MODE) {
            synchronized (DEMO_CONVERSATIONS) {
                for (DemoConversation c : DEMO_CONVERSATIONS) {
                    boolean samePair = (c.userOne == renterId && c.userTwo == landlordId)
                            || (c.userOne == landlordId && c.userTwo == renterId);
                    if (c.listingId == listingId && samePair) return c.id;
                }
                DemoConversation created = new DemoConversation(DEMO_CONVERSATIONS.size() + 1, renterId, landlordId, listingId);
                DEMO_CONVERSATIONS.add(created);
                return created.id;
            }
        }

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
            addDemoNotification(receiverId, "new_message", "New message from User #" + senderId, conversationId);
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
            String senderName = "someone";
            try (PreparedStatement ps = con.prepareStatement("SELECT full_name FROM users WHERE user_id = ?")) {
                ps.setInt(1, senderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getString(1) != null) senderName = rs.getString(1);
                }
            }
            // related_id = the conversation, so clicking the notification can open exactly this chat
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO notifications (user_id, type, message, related_id, is_read) "
                            + "VALUES (?, 'new_message', ?, ?, 0)")) {
                ps.setInt(1, receiverId);
                ps.setString(2, "New message from " + senderName);
                ps.setInt(3, conversationId);
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

    // ------------------------------------------------------------------ inbox (Messages screen)

    /** Every chat the user is in (renter or owner) that has at least one message, newest first. */
    public List<Conversation> findConversations(int userId) throws SQLException {
        if (AppConfig.DEMO_MODE) {
            List<Conversation> result = new ArrayList<>();
            for (DemoConversation c : DEMO_CONVERSATIONS) {
                if (c.userOne != userId && c.userTwo != userId) continue;
                Conversation conversation = demoConversation(c, userId);
                if (conversation.getLastSentAt() != null) result.add(conversation);
            }
            result.sort((a, b) -> b.getLastSentAt().compareTo(a.getLastSentAt()));
            return result;
        }
        try (Connection con = DBConnection.getConnection()) {
            return queryConversations(con, userId, 0);
        }
    }

    /** One chat, or null when it does not exist or the user is not part of it. */
    public Conversation findConversation(int conversationId, int userId) throws SQLException {
        if (AppConfig.DEMO_MODE) {
            for (DemoConversation c : DEMO_CONVERSATIONS) {
                if (c.id == conversationId && (c.userOne == userId || c.userTwo == userId)) {
                    return demoConversation(c, userId);
                }
            }
            return null;
        }
        try (Connection con = DBConnection.getConnection()) {
            List<Conversation> found = queryConversations(con, userId, conversationId);
            return found.isEmpty() ? null : found.get(0);
        }
    }

    /** @param onlyConversationId 0 = all of the user's chats, otherwise just that one */
    private static List<Conversation> queryConversations(Connection con, int userId, int onlyConversationId)
            throws SQLException {
        String sql = "SELECT c.conversation_id, c.listing_id, l.property_name, u.user_id AS other_id, u.full_name, "
                + "  (SELECT m.message_text FROM messages m WHERE m.conversation_id = c.conversation_id "
                + "     ORDER BY m.sent_at DESC, m.message_id DESC LIMIT 1) AS last_text, "
                + "  (SELECT MAX(m.sent_at) FROM messages m WHERE m.conversation_id = c.conversation_id) AS last_at, "
                + "  (SELECT COUNT(*) FROM messages m WHERE m.conversation_id = c.conversation_id "
                + "     AND m.receiver_id = ? AND m.status <> 'read') AS unread "
                + "FROM conversations c "
                + "JOIN users u ON u.user_id = IF(c.user_one_id = ?, c.user_two_id, c.user_one_id) "
                + "LEFT JOIN listings l ON l.listing_id = c.listing_id "
                + "WHERE (c.user_one_id = ? OR c.user_two_id = ?) "
                + (onlyConversationId > 0 ? "AND c.conversation_id = ? "
                : "AND EXISTS (SELECT 1 FROM messages m WHERE m.conversation_id = c.conversation_id) ")
                + "ORDER BY last_at DESC, c.conversation_id DESC";

        List<Conversation> result = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, userId);
            ps.setInt(4, userId);
            if (onlyConversationId > 0) ps.setInt(5, onlyConversationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Conversation c = new Conversation();
                    c.setConversationId(rs.getInt("conversation_id"));
                    c.setListingId(rs.getInt("listing_id"));
                    c.setListingName(rs.getString("property_name"));
                    c.setOtherUserId(rs.getInt("other_id"));
                    c.setOtherUserName(rs.getString("full_name"));
                    c.setLastMessage(rs.getString("last_text"));
                    Timestamp lastAt = rs.getTimestamp("last_at");
                    if (lastAt != null) c.setLastSentAt(lastAt.toLocalDateTime());
                    c.setUnreadCount(rs.getInt("unread"));
                    result.add(c);
                }
            }
        }
        return result;
    }

    private static Conversation demoConversation(DemoConversation c, int userId) {
        Conversation result = new Conversation();
        int other = c.userOne == userId ? c.userTwo : c.userOne;
        result.setConversationId(c.id);
        result.setOtherUserId(other);
        result.setOtherUserName("User #" + other);
        result.setListingId(c.listingId);
        for (com.template.model.Listing l : SampleData.listings()) {
            if (l.getListingId() == c.listingId) result.setListingName(l.getPropertyName());
        }
        for (Message m : DEMO_MESSAGES) {
            if (m.getConversationId() != c.id) continue;
            result.setLastMessage(m.getText());
            result.setLastSentAt(m.getSentAt());
            if (m.getReceiverId() == userId && !"read".equals(m.getStatus())) {
                result.setUnreadCount(result.getUnreadCount() + 1);
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ viewing schedule (all of a user's requests)

    /** Every viewing request the user is part of (as renter or as owner), with listing and people names. */
    public List<ViewingSchedule> findViewingSchedulesForUser(int userId) throws SQLException {
        if (AppConfig.DEMO_MODE) {
            List<ViewingSchedule> result = new ArrayList<>();
            for (ViewingSchedule schedule : DEMO_SCHEDULES) {
                if (schedule.getRenterId() != userId && schedule.getLandlordId() != userId) continue;
                schedule.setRenterName("User #" + schedule.getRenterId());
                schedule.setLandlordName("User #" + schedule.getLandlordId());
                for (com.template.model.Listing l : SampleData.listings()) {
                    if (l.getListingId() == schedule.getListingId()) schedule.setListingName(l.getPropertyName());
                }
                result.add(schedule);
            }
            result.sort((a, b) -> b.getScheduledAt().compareTo(a.getScheduledAt()));
            return result;
        }

        List<ViewingSchedule> result = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT v.viewing_id, v.listing_id, v.renter_id, v.landlord_id, v.scheduled_at, v.proposed_by, "
                             + "       v.status, v.created_at, l.property_name, ru.full_name AS renter_name, "
                             + "       lu.full_name AS landlord_name "
                             + "FROM viewing_schedules v "
                             + "JOIN listings l ON l.listing_id = v.listing_id "
                             + "JOIN users ru ON ru.user_id = v.renter_id "
                             + "JOIN users lu ON lu.user_id = v.landlord_id "
                             + "WHERE v.renter_id = ? OR v.landlord_id = ? "
                             + "ORDER BY v.scheduled_at DESC")) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);
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
                    schedule.setListingName(rs.getString("property_name"));
                    schedule.setRenterName(rs.getString("renter_name"));
                    schedule.setLandlordName(rs.getString("landlord_name"));
                    result.add(schedule);
                }
            }
        }
        return result;
    }

    // ------------------------------------------------------------------ one notification

    /** Marks a single notification as read (used when the user clicks it). */
    public void markNotificationRead(int notificationId, int userId) throws SQLException {
        if (AppConfig.DEMO_MODE) {
            for (Notification notification : DEMO_NOTIFICATIONS) {
                if (notification.getNotificationId() == notificationId && notification.getUserId() == userId) {
                    notification.setRead(true);
                }
            }
            return;
        }
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE notifications SET is_read = 1 WHERE notification_id = ? AND user_id = ?")) {
            ps.setInt(1, notificationId);
            ps.setInt(2, userId);
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