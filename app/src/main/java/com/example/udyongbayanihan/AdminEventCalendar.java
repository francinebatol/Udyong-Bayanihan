package com.example.udyongbayanihan;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class AdminEventCalendar extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView eventDetails;
    private HashMap<String, List<Event>> eventsMap;
    private List<Event> selectedEvents;
    private FirebaseFirestore db;
    private ImageButton backButton;
    private String amAccountId; // Current admin ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_event_calendar);

        calendarView = findViewById(R.id.calendarView);
        eventDetails = findViewById(R.id.eventDetails);
        backButton = findViewById(R.id.imgbtnBack);

        db = FirebaseFirestore.getInstance();
        eventsMap = new HashMap<>();

        // Get the admin ID from intent
        amAccountId = getIntent().getStringExtra("amAccountId");
        if (amAccountId == null || amAccountId.isEmpty()) {
            Toast.makeText(this, "Error: Admin ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Setup the calendar date selection
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(date.getYear(), date.getMonth() - 1, date.getDay());
            String selectedDate = formatDate(calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH));

            if (eventsMap.containsKey(selectedDate)) {
                selectedEvents = eventsMap.get(selectedDate);
                displayEventDetails(selectedEvents);
            } else {
                selectedEvents = null;
                eventDetails.setText(String.format("No events found for the selected date."));
            }
        });

        // Fetch events from Firestore
        fetchEventsFromFirestore();

        backButton.setOnClickListener(v -> {
            finish(); // Go back to previous activity
        });
    }

    private void fetchEventsFromFirestore() {
        // For Admin, fetch all events
        db.collection("EventDetails")
                .get()
                .addOnSuccessListener(eventDetailsQuery -> {
                    if (eventDetailsQuery.isEmpty()) {
                        Toast.makeText(this, "No events found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                    HashSet<CalendarDay> adminEventDays = new HashSet<>();  // Admin's own events
                    HashSet<CalendarDay> otherEventDays = new HashSet<>();  // Other admin's events
                    List<String> eventIds = new ArrayList<>();

                    // Create a map to store eventIds for later use
                    HashMap<String, QueryDocumentSnapshot> eventDetailsMap = new HashMap<>();

                    for (QueryDocumentSnapshot eventDetailsDoc : eventDetailsQuery) {
                        String eventId = eventDetailsDoc.getString("eventId");
                        if (eventId != null) {
                            eventIds.add(eventId);
                            eventDetailsMap.put(eventId, eventDetailsDoc);
                        }
                    }

                    // If no events found
                    if (eventIds.isEmpty()) {
                        Toast.makeText(AdminEventCalendar.this, "No events found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Fetch corresponding event information
                    db.collection("EventInformation")
                            .get()  // Get all events for admin view
                            .addOnSuccessListener(eventInfoQuery -> {
                                for (QueryDocumentSnapshot eventInfoDoc : eventInfoQuery) {
                                    String eventId = eventInfoDoc.getString("eventId");
                                    String headCoordinator = eventInfoDoc.getString("headCoordinator");
                                    String organization = eventInfoDoc.getString("organizations");
                                    // Try different field names that might store the admin ID
                                    String createdBy = eventInfoDoc.getString("createdBy");
                                    if (createdBy == null) {
                                        createdBy = eventInfoDoc.getString("adminId");
                                    }
                                    if (createdBy == null) {
                                        createdBy = eventInfoDoc.getString("amAccountId");
                                    }
                                    // Default to a non-matching string if no admin field is found
                                    if (createdBy == null) {
                                        createdBy = "unknown_admin";
                                    }

                                    if (eventId != null && eventDetailsMap.containsKey(eventId)) {
                                        QueryDocumentSnapshot eventDetailsDoc = eventDetailsMap.get(eventId);
                                        String nameOfEvent = eventDetailsDoc.getString("nameOfEvent");
                                        Timestamp timestamp = eventDetailsDoc.getTimestamp("date");

                                        if (nameOfEvent != null && headCoordinator != null &&
                                                organization != null && timestamp != null) {
                                            Date date = timestamp.toDate();
                                            String formattedDate = dateFormat.format(date);

                                            // Add to event days set for decoration
                                            Calendar calendar = Calendar.getInstance();
                                            calendar.setTime(date);
                                            CalendarDay calendarDay = CalendarDay.from(
                                                    calendar.get(Calendar.YEAR),
                                                    calendar.get(Calendar.MONTH) + 1,
                                                    calendar.get(Calendar.DAY_OF_MONTH)
                                            );

                                            // Add to appropriate set based on admin ownership
                                            // For now, just mark all events with a single color
                                            // until we find the right field for ownership
                                            if (createdBy.equals(amAccountId)) {
                                                adminEventDays.add(calendarDay);
                                            } else {
                                                otherEventDays.add(calendarDay);
                                            }

                                            // Create Event object with admin flag
                                            Event event = new Event(nameOfEvent, headCoordinator, organization);
                                            event.setAdminOwned(createdBy.equals(amAccountId));

                                            // Add the event to the map for the corresponding date
                                            if (!eventsMap.containsKey(formattedDate)) {
                                                eventsMap.put(formattedDate, new ArrayList<>());
                                            }
                                            eventsMap.get(formattedDate).add(event);
                                        }
                                    }
                                }

                                // For debugging, log the number of events found
                                int totalEvents = 0;
                                for (List<Event> events : eventsMap.values()) {
                                    totalEvents += events.size();
                                }

                                Toast.makeText(AdminEventCalendar.this,
                                        "Found " + totalEvents + " events",
                                        Toast.LENGTH_SHORT).show();

                                // After all events are loaded, mark dates on the calendar with appropriate colors
                                if (!adminEventDays.isEmpty()) {
                                    markEventDates(adminEventDays, ContextCompat.getColor(this, R.color.event_marker_color));
                                }

                                if (!otherEventDays.isEmpty()) {
                                    markEventDates(otherEventDays, ContextCompat.getColor(this, R.color.neutral_yellow));
                                }

                                // If no events were loaded, display all events with a single color
                                if (adminEventDays.isEmpty() && otherEventDays.isEmpty() && totalEvents > 0) {
                                    HashSet<CalendarDay> allEventDays = new HashSet<>();
                                    for (String dateKey : eventsMap.keySet()) {
                                        try {
                                            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
                                            Date eventDate = sdf.parse(dateKey);
                                            Calendar calendar = Calendar.getInstance();
                                            calendar.setTime(eventDate);
                                            allEventDays.add(CalendarDay.from(
                                                    calendar.get(Calendar.YEAR),
                                                    calendar.get(Calendar.MONTH) + 1,
                                                    calendar.get(Calendar.DAY_OF_MONTH)
                                            ));
                                        } catch (Exception e) {
                                            // Skip invalid date format
                                        }
                                    }
                                    if (!allEventDays.isEmpty()) {
                                        markEventDates(allEventDays, ContextCompat.getColor(this, R.color.event_marker_color));
                                    }
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(AdminEventCalendar.this, "Error loading EventInformation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminEventCalendar.this, "Error loading EventDetails: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Mark dates that have events with specific color
    private void markEventDates(HashSet<CalendarDay> eventDays, int color) {
        if (eventDays.isEmpty()) {
            return;
        }

        // Add the event decorator to the calendar with specific color
        calendarView.addDecorator(new EventDecorator(color, eventDays));
    }

    // Format the date as MM/dd/yyyy
    private String formatDate(int year, int month, int day) {
        return String.format(Locale.getDefault(), "%02d/%02d/%04d", month, day, year);
    }

    // Display the event details in the TextView
    private void displayEventDetails(List<Event> events) {
        StringBuilder details = new StringBuilder();
        for (Event event : events) {
            String ownership = event.isAdminOwned() ? "[Your Event]" : "[Other Admin's Event]";
            details.append(String.format(
                    Locale.getDefault(),
                    "%s\nEvent Title: %s\nHead Coordinator: %s\nOrganization: %s\n\n",
                    ownership,
                    event.getTitle(),
                    event.getHeadCoordinator(),
                    event.getOrganizer()
            ));
        }
        eventDetails.setText(details.toString().trim());
    }

    /**
     * Decorator class for events in the calendar
     */
    private static class EventDecorator implements DayViewDecorator {
        private final int color;
        private final HashSet<CalendarDay> eventDays;

        public EventDecorator(int color, HashSet<CalendarDay> eventDays) {
            this.color = color;
            this.eventDays = eventDays;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return eventDays.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new DotSpan(8, color)); // Adds a colored dot under the date
        }
    }
}