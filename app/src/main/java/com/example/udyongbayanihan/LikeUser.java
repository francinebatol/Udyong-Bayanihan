package com.example.udyongbayanihan;

public class LikeUser {
    private String userId;
    private String fullName;
    private String profilePictureUrl;  // Changed from address to profilePictureUrl

    public LikeUser(String userId, String fullName, String profilePictureUrl) {
        this.userId = userId;
        this.fullName = fullName;
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getProfilePictureUrl() { return profilePictureUrl; }
}