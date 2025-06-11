package com.example.udyongbayanihan;

import java.util.List;

public class EventInformation {
    private String eventId; // Primary Key
    private String amAccountId; // Foreign Key
    private String headCoordinator;
    private String organizations;
    private List<String> eventSkills;
    private String status;
    private boolean postFeedback;
    private int volunteerNeeded;

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getAmAccountId() {
        return amAccountId;
    }

    public void setAmAccountId(String amAccountId) {
        this.amAccountId = amAccountId;
    }

    public String getHeadCoordinator() {
        return headCoordinator;
    }

    public void setHeadCoordinator(String headCoordinator) {
        this.headCoordinator = headCoordinator;
    }

    public String getOrganizations() {
        return organizations;
    }

    public void setOrganizations(String organizations) {
        this.organizations = organizations;
    }

    public List<String> getEventSkills() {
        return eventSkills;
    }

    public void setEventSkills(List<String> eventSkills) {
        this.eventSkills = eventSkills;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getVolunteerNeeded() {
        return volunteerNeeded;
    }

    public void setVolunteerNeeded(int volunteerNeeded) {
        this.volunteerNeeded = volunteerNeeded;
    }

    public boolean isPostFeedback() {
        return postFeedback;
    }

    public void setPostFeedback(boolean postFeedback) {
        this.postFeedback = postFeedback;
    }
}
