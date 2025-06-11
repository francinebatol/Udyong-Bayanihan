package com.example.udyongbayanihan;

public class FeedbackCommentModel {
    private String commentType;
    private String commentText;
    private String userIdentifier;
    private String userId; // Added to track which user this comment belongs to

    public FeedbackCommentModel(String commentType, String commentText, String userIdentifier) {
        this.commentType = commentType;
        this.commentText = commentText;
        this.userIdentifier = userIdentifier;
        this.userId = null;
    }

    // New constructor with userId
    public FeedbackCommentModel(String commentType, String commentText, String userIdentifier, String userId) {
        this.commentType = commentType;
        this.commentText = commentText;
        this.userIdentifier = userIdentifier;
        this.userId = userId;
    }

    public String getCommentType() {
        return commentType;
    }

    public String getCommentText() {
        return commentText;
    }

    public String getUserIdentifier() {
        return userIdentifier;
    }

    public void setUserIdentifier(String userIdentifier) {
        this.userIdentifier = userIdentifier;
    }

    public String getUserId() {
        return userId;
    }
}