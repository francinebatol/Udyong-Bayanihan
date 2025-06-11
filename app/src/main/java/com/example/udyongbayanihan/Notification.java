package com.example.udyongbayanihan;

import com.google.firebase.Timestamp;

public class Notification {
    private String id;
    private String userId;
    private String eventId;
    private String eventName;
    private String barangay;
    private String type;
    private Timestamp timestamp;
    private boolean read;
    // Fields for verification status
    private String status;
    private String message;
    private String reason; // Added to match web app's field

    // Fields for skill group notifications
    private String skillName;
    private String requestStatus; // "APPROVED" or "REJECTED"

    // Field to store current verification status (not from Firestore)
    private String currentUserStatus;

    // Constants for notification types
    public static final String TYPE_EVENT_CONFIRMATION = "event_confirmation";
    public static final String TYPE_UPCOMING_EVENT = "upcoming_event";
    public static final String TYPE_VERIFICATION_STATUS = "verification_status";
    public static final String TYPE_USER_VERIFICATION = "user_verification";
    public static final String TYPE_FEEDBACK_REQUEST = "feedback_request";
    public static final String TYPE_SKILL_GROUP = "skill_group_request";

    public Notification() {
        // Required for Firestore
    }

    // Constructor for event notifications
    public Notification(String userId, String eventId, String eventName, String barangay, String type) {
        this.id = userId + "_" + eventId + "_" + type; // Generate ID based on components
        this.userId = userId;
        this.eventId = eventId;
        this.eventName = eventName;
        this.barangay = barangay;
        this.type = type;
        this.timestamp = Timestamp.now();
        this.read = false;
    }

    // Constructor for verification status notifications
    public Notification(String userId, String type, String status, String message) {
        this.id = userId + "_verification_" + status + "_" + Timestamp.now().getSeconds();
        this.userId = userId;
        this.type = type;
        this.status = status;
        this.message = message;
        this.timestamp = Timestamp.now();
        this.read = false;
    }

    // Factory method for creating feedback request notifications
    public static Notification createFeedbackNotification(String userId, String eventId, String eventName, String barangay) {
        // Create a new notification instance
        Notification notification = new Notification();

        // Set all the required fields
        notification.setUserId(userId);
        notification.setEventId(eventId);
        notification.setEventName(eventName);
        notification.setBarangay(barangay);
        notification.setType("feedback_request");
        notification.setTimestamp(Timestamp.now());
        notification.setRead(false);

        // Create a deterministic ID that will be the same every time for this user-event pair
        // This prevents duplicate notifications and makes it easy to check if one already exists
        notification.setId("feedback_" + eventId + "_" + userId);

        // Set a standard message for the notification
        String message = "Please provide your feedback for the event: " + eventName;
        if (barangay != null && !barangay.isEmpty()) {
            message += " in Barangay " + barangay;
        }
        notification.setMessage(message);

        return notification;
    }

    // Factory method for creating skill group request notifications
    public static Notification createSkillGroupNotification(String userId, String skillName, String requestStatus) {
        // Create a new notification instance
        Notification notification = new Notification();

        // Set all the required fields
        notification.setUserId(userId);
        notification.setSkillName(skillName);
        notification.setRequestStatus(requestStatus);
        notification.setType(TYPE_SKILL_GROUP);
        notification.setTimestamp(Timestamp.now());
        notification.setRead(false);

        // Create a deterministic ID that will be the same every time for this user-skill-status combination
        // This prevents duplicate notifications
        notification.setId("skill_" + skillName + "_" + userId + "_" + requestStatus);

        // Set a standard message for the notification
        String message;
        if ("APPROVED".equals(requestStatus)) {
            message = "Your request to join the " + skillName + " skill group has been approved!";
            notification.setStatus("approved");
        } else {
            message = "Your request to join the " + skillName + " skill group has been rejected.";
            notification.setStatus("rejected");
        }
        notification.setMessage(message);

        return notification;
    }

    // Standard getter and setter methods
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getBarangay() {
        return barangay;
    }

    public void setBarangay(String barangay) {
        this.barangay = barangay;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCurrentUserStatus() {
        return currentUserStatus;
    }

    public void setCurrentUserStatus(String currentUserStatus) {
        this.currentUserStatus = currentUserStatus;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    /**
     * Check if this is a verification notification
     */
    public boolean isVerificationNotification() {
        return "user_verification".equals(type) || "verification_status".equals(type);
    }

    /**
     * Check if this is an event notification
     */
    public boolean isEventNotification() {
        return "event_confirmation".equals(type) || "upcoming_event".equals(type);
    }

    /**
     * Check if this is a feedback request notification
     */
    public boolean isFeedbackNotification() {
        return "feedback_request".equals(type);
    }

    /**
     * Check if this is a skill group notification
     */
    public boolean isSkillGroupNotification() {
        return TYPE_SKILL_GROUP.equals(type);
    }
}