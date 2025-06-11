package com.example.udyongbayanihan;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class ChatPreviewModel {
    private String chatId;
    private String lastMessage;
    private Date timestamp;
    private List<Map<String, String>> participants;
    private Map<String, Object> lastReadBy, lastMessageMetadata;
    private String otherUserName;
    private String otherUserId;
    private String otherUserType;
    private boolean hasUnreadMessages;

    public ChatPreviewModel() {
        // Required empty constructor for Firebase
    }

    public ChatPreviewModel(String chatId, String lastMessage, Date timestamp,
                            List<Map<String, String>> participants, String otherUserName,
                            String otherUserId, String otherUserType, boolean hasUnreadMessages) {
        this.chatId = chatId;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
        this.participants = participants;
        this.otherUserName = otherUserName;
        this.otherUserId = otherUserId;
        this.otherUserType = otherUserType;
        this.hasUnreadMessages = hasUnreadMessages;
    }

    // Constructor that includes lastReadBy
    public ChatPreviewModel(String chatId, String lastMessage, Date timestamp,
                            List<Map<String, String>> participants, Map<String, Object> lastReadBy,
                            Map<String, Object> lastMessageMetadata,  // Add this parameter
                            String otherUserName, String otherUserId, String otherUserType,
                            boolean hasUnreadMessages) {
        this(chatId, lastMessage, timestamp, participants, otherUserName,
                otherUserId, otherUserType, hasUnreadMessages);
        this.lastReadBy = lastReadBy;
        this.lastMessageMetadata = lastMessageMetadata;  // Add this line
    }

    // Getters
    public String getChatId() { return chatId; }
    public String getLastMessage() { return lastMessage; }
    public Date getTimestamp() { return timestamp; }
    public List<Map<String, String>> getParticipants() { return participants; }
    public String getOtherUserName() { return otherUserName; }
    public String getOtherUserId() { return otherUserId; }
    public String getOtherUserType() { return otherUserType; }
    public Map<String, Object> getLastReadBy() { return lastReadBy; }

    public boolean hasUnreadMessages() {
        return hasUnreadMessages;
    }

    public Map<String, Object> getLastMessageMetadata() { return lastMessageMetadata; }
    public void setLastMessageMetadata(Map<String, Object> lastMessageMetadata) {
        this.lastMessageMetadata = lastMessageMetadata;
    }

    // Setters
    public void setHasUnreadMessages(boolean hasUnreadMessages) {
        this.hasUnreadMessages = hasUnreadMessages;
    }

    public void setLastReadBy(Map<String, Object> lastReadBy) {
        this.lastReadBy = lastReadBy;
    }
}