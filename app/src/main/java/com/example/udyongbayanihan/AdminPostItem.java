package com.example.udyongbayanihan;

import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class AdminPostItem {
    private String postId;
    private String adminName;
    private String position;
    private String postContent;
    private Timestamp timestamp;
    private String barangay;
    private List<String> skillsList;
    private int likesCount;
    private int commentsCount;

    // Empty constructor for Firestore
    public AdminPostItem() {
        skillsList = new ArrayList<>();
    }

    public AdminPostItem(String postId, String adminName, String position, String postContent,
                         Timestamp timestamp) {
        this.postId = postId;
        this.adminName = adminName;
        this.position = position;
        this.postContent = postContent;
        this.timestamp = timestamp;
        this.likesCount = 0;
        this.commentsCount = 0;
        this.skillsList = new ArrayList<>();
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getPostContent() {
        return postContent;
    }

    public void setPostContent(String postContent) {
        this.postContent = postContent;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public String getBarangay() {
        return barangay;
    }

    public void setBarangay(String barangay) {
        this.barangay = barangay;
    }

    public List<String> getSkillsList() {
        return skillsList;
    }

    public void setSkillsList(List<String> skillsList) {
        this.skillsList = skillsList;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    public int getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(int commentsCount) {
        this.commentsCount = commentsCount;
    }

    public boolean isPostedInSkill() {
        return skillsList != null && !skillsList.isEmpty();
    }

    public boolean isPostedInBarangay() {
        return barangay != null && !barangay.isEmpty();
    }

    // Generate a formatted location string
    public String getFormattedLocation() {
        StringBuilder location = new StringBuilder("Posted in: ");

        // Add skills information
        if (isPostedInSkill()) {
            location.append("Skill - ");
            for (int i = 0; i < skillsList.size(); i++) {
                location.append(skillsList.get(i));
                if (i < skillsList.size() - 1) {
                    location.append(", ");
                }
            }

            // Add barangay if applicable
            if (isPostedInBarangay()) {
                location.append(" and Barangay - ").append(barangay);
            }
        } else if (isPostedInBarangay()) {
            // Only barangay
            location.append("Barangay - ").append(barangay);
        } else {
            location.append("Unknown location");
        }

        return location.toString();
    }
}