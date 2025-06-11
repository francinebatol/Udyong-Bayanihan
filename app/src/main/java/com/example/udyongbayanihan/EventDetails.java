package com.example.udyongbayanihan;

import com.google.firebase.Timestamp;

import java.util.List;

public class EventDetails {
    private String eventId; // Foreign Key
    private String nameOfEvent;
    private String typeOfEvent;
    private String barangay;
    private Timestamp date;
    private int volunteerNeeded, participantsJoined;
    private String caption;
    private List<String> imageUrls;

    public String getBarangay() {
        return barangay;
    }

    public void setParticipantsJoined(int participantsJoined) {
        this.participantsJoined = participantsJoined;
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getNameOfEvent() {
        return nameOfEvent;
    }

    public void setNameOfEvent(String nameOfEvent) {
        this.nameOfEvent = nameOfEvent;
    }

    public String getTypeOfEvent() {
        return typeOfEvent;
    }

    public void setTypeOfEvent(String typeOfEvent) {
        this.typeOfEvent = typeOfEvent;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public int getVolunteerNeeded() {
        return volunteerNeeded;
    }

    public void setVolunteerNeeded(int volunteerNeeded) {
        this.volunteerNeeded = volunteerNeeded;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public int getParticipantsJoined() {
        return participantsJoined;
    }
}
