package com.example.udyongbayanihan;

import com.google.firebase.Timestamp;

public class Comment {
    private String commentId;
    private String postId;
    private String userId;
    private String fullName;
    private String commentText;
    private Timestamp timestamp;
    private boolean pinned;

    // Empty constructor for Firebase
    public Comment() {}

    public Comment(String commentId, String postId, String userId, String fullName, String commentText, Timestamp timestamp) {
        this.commentId = commentId;
        this.postId = postId;
        this.userId = userId;
        this.fullName = fullName;
        this.commentText = commentText;
        this.timestamp = timestamp;
        this.pinned = false;  // Default value
    }

    // Getters and Setters
    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
}