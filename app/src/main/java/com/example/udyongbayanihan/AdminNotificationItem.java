package com.example.udyongbayanihan;

public class AdminNotificationItem {
    private String eventId;
    private String eventName;
    private String status;
    private long timestamp;
    private String feedback;

    public AdminNotificationItem(String eventId, String eventName, String status, long timestamp) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.status = status;
        this.timestamp = timestamp;
        this.feedback = null;
    }

    public AdminNotificationItem(String eventId, String eventName, String status, long timestamp, String feedback) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.status = status;
        this.timestamp = timestamp;
        this.feedback = feedback;
    }

    // Getters
    public String getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public String getStatus() { return status; }
    public long getTimestamp() { return timestamp; }
    public String getFeedback() { return feedback; }
}