package com.example.udyongbayanihan;

import java.util.Date;

public class MessageModel {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String senderType;    // user or admin
    private String receiverType;  // user or admin
    private String message;
    private Date timestamp;
    private boolean read;

    public MessageModel() {
        // Required empty constructor for Firebase
    }

    public MessageModel(String messageId, String senderId, String receiverId,
                        String senderType, String receiverType, String message,
                        Date timestamp, boolean read) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderType = senderType;
        this.receiverType = receiverType;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read;
    }

    // Getters
    public String getMessageId() { return messageId; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getSenderType() { return senderType; }
    public String getReceiverType() { return receiverType; }
    public String getMessage() { return message; }
    public Date getTimestamp() { return timestamp; }
    public boolean isRead() { return read; }
}