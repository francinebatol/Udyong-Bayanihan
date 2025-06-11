package com.example.udyongbayanihan;

public class Event {
    private String nameOfEvent;
    private String headCoordinator;
    private String organizations;
    private boolean isAdminOwned; // Flag to indicate if event belongs to current admin

    public Event(String nameOfEvent, String headCoordinator, String organizations) {
        this.nameOfEvent = nameOfEvent;
        this.headCoordinator = headCoordinator;
        this.organizations = organizations;
        this.isAdminOwned = false; // Default to false
    }

    public String getNameOfEvent() {
        return nameOfEvent;
    }

    public String getHeadCoordinator() {
        return headCoordinator;
    }

    public String getOrganizations() {
        return organizations;
    }

    public boolean isAdminOwned() {
        return isAdminOwned;
    }

    public void setAdminOwned(boolean adminOwned) {
        this.isAdminOwned = adminOwned;
    }

    // These methods ensure compatibility with both activities
    public String getTitle() {
        return getNameOfEvent();  // Delegate to getNameOfEvent
    }

    public String getOrganizer() {
        return getOrganizations();  // Delegate to getOrganizations
    }
}