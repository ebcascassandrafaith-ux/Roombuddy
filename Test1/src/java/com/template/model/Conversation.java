package com.template.model;

import java.time.LocalDateTime;

/** One chat between two users about one listing, as shown in the Messages inbox. */
public class Conversation {
    private int conversationId;
    private int otherUserId;          // the person you are talking to (not you)
    private String otherUserName;
    private int listingId;            // 0 when the listing was deleted
    private String listingName;
    private String lastMessage;
    private LocalDateTime lastSentAt;
    private int unreadCount;          // messages sent to you that you have not opened yet

    public int getConversationId() { return conversationId; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }
    public int getOtherUserId() { return otherUserId; }
    public void setOtherUserId(int otherUserId) { this.otherUserId = otherUserId; }
    public String getOtherUserName() { return otherUserName; }
    public void setOtherUserName(String otherUserName) { this.otherUserName = otherUserName; }
    public int getListingId() { return listingId; }
    public void setListingId(int listingId) { this.listingId = listingId; }
    public String getListingName() { return listingName; }
    public void setListingName(String listingName) { this.listingName = listingName; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public LocalDateTime getLastSentAt() { return lastSentAt; }
    public void setLastSentAt(LocalDateTime lastSentAt) { this.lastSentAt = lastSentAt; }
    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
}
