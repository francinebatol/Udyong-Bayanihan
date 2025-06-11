package com.example.udyongbayanihan;

import com.google.firebase.Timestamp;

public class CommunityPost extends CommunityItem {
    private String adminName;
    private String position;
    private String postContent;
    private Timestamp timestamp;

    @Override
    public int getType() {
        return TYPE_POST;
    }

    // Add getters and setters
    public String getAdminName() { return adminName; }
    public void setAdminName(String adminName) { this.adminName = adminName; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getPostContent() { return postContent; }
    public void setPostContent(String postContent) { this.postContent = postContent; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
