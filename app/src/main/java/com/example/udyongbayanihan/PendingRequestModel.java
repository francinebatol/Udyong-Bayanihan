package com.example.udyongbayanihan;

public class PendingRequestModel {
    private String requestId;
    private String userId;
    private String username;
    private String email;
    private String skillName;

    public PendingRequestModel(String requestId, String userId, String username, String email, String skillName) {
        this.requestId = requestId;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.skillName = skillName;
    }

    // Getters
    public String getRequestId() { return requestId; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getSkillName() { return skillName; }
}