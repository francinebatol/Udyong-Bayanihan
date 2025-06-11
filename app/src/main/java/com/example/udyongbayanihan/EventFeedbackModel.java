package com.example.udyongbayanihan;

public class EventFeedbackModel {
    private String eventId;
    private String nameOfEvent;

    public EventFeedbackModel(String eventId, String nameOfEvent) {
        this.eventId = eventId;
        this.nameOfEvent = nameOfEvent;
    }

    public String getEventId() {
        return eventId;
    }

    public String getNameOfEvent() {
        return nameOfEvent;
    }
}