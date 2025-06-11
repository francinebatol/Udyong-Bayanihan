package com.example.udyongbayanihan;

import java.util.Date;
import java.util.List;

public class EventModel {
    private String eventId;
    private String name, nameOfEvent, typeOfEvent;
    private String type;
    private String caption;
    private Object imageUrl;
    private String postId;
    private String adminName;
    private String position;
    private String postContent;
    private Date timestamp;
    private Long volunteersNeeded;
    private String status;
    private String organization;
    private Date date;
    private String address;
    private String headCoordinator;
    private List<String> eventSkills, imageUrls;
    private boolean isEvent;

    public EventModel(String eventId, String name, String type, String caption,
                      List<String> imageUrls, Long volunteersNeeded, String status,
                      String organization, Date date, String address,
                      String headCoordinator, List<String> eventSkills) {
        this.eventId = eventId;
        this.name = name;
        this.type = type;
        this.caption = caption;
        this.imageUrls = imageUrls;
        this.volunteersNeeded = volunteersNeeded;
        this.status = status;
        this.organization = organization;
        this.date = date;
        this.address = address;
        this.headCoordinator = headCoordinator;
        this.eventSkills = eventSkills;
        this.isEvent = true;
    }

    // Constructor for Skill Posts
    public EventModel(String postId, String adminName, String position,
                      String postContent, Date timestamp) {
        this.postId = postId;
        this.adminName = adminName;
        this.position = position;
        this.postContent = postContent;
        this.timestamp = timestamp;
        this.isEvent = false;
    }

    public boolean isEvent() {
        return isEvent;
    }

    // Getters
    public String getEventId() { return eventId; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getCaption() { return caption; }
    public Object getImageUrl() { return imageUrl; }
    public Long getVolunteersNeeded() { return volunteersNeeded; }
    public String getStatus() { return status; }
    public String getOrganization() { return organization; }
    public Date getDate() { return date; }
    public String getAddress() { return address; }
    public String getHeadCoordinator() { return headCoordinator; }
    public List<String> getEventSkills() { return eventSkills; }

    public String getPostId() {
        return postId;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getPosition() {
        return position;
    }

    public String getPostContent() {
        return postContent;
    }

    public Date getTimestamp() {
        return timestamp;
    }
    public List<String> getImageUrls() { return imageUrls; }

    // Setter
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setNameOfEvent(String nameOfEvent) {
        this.nameOfEvent = nameOfEvent;
    }

    public void setTypeOfEvent(String typeOfEvent) {
        this.typeOfEvent = typeOfEvent;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public void setPostContent(String postContent) {
        this.postContent = postContent;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setVolunteersNeeded(Long volunteersNeeded) {
        this.volunteersNeeded = volunteersNeeded;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setHeadCoordinator(String headCoordinator) {
        this.headCoordinator = headCoordinator;
    }

    public void setEventSkills(List<String> eventSkills) {
        this.eventSkills = eventSkills;
    }

    public void setEvent(boolean event) {
        isEvent = event;
    }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
}