package com.example.udyongbayanihan;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
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
import java.util.Map;

public class EventDatePicker extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView eventDetails;
    private TextView selectedDateText;
    private TextView selectedTimeText;
    private LinearLayout confirmationContainer;
    private Button btnYes, btnNo, btnSelectTime;
    private ImageButton backButton;

    private HashMap<String, List<EventInfo>> eventsMap; // Maps dates to lists of events with status info
    private FirebaseFirestore db;
    private String selectedDate;
    private String selectedTime = "00:00"; // Default time
    private HashSet<CalendarDay> acceptedEventDays;
    private HashSet<CalendarDay> pendingEventDays;
    private Calendar selectedCalendar;
    private int selectedHour = 0;
    private int selectedMinute = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_date_picker);

        // Initialize UI components
        calendarView = findViewById(R.id.calendarView);
        eventDetails = findViewById(R.id.eventDetails);
        confirmationContainer = findViewById(R.id.confirmationContainer);
        selectedDateText = findViewById(R.id.selectedDateText);
        btnYes = findViewById(R.id.btnYes);
        btnNo = findViewById(R.id.btnNo);
        backButton = findViewById(R.id.imgbtnBack);

        // Add button for time selection
        btnSelectTime = findViewById(R.id.btnSelectTime);
        selectedTimeText = findViewById(R.id.selectedTimeText);

        db = FirebaseFirestore.getInstance();
        eventsMap = new HashMap<>();
        acceptedEventDays = new HashSet<>();
        pendingEventDays = new HashSet<>();

        // Fetch all events from Firestore
        fetchAllEvents();

        // Set up time selection button
        btnSelectTime.setOnClickListener(v -> showTimePickerDialog());

        // Set up calendar date change listener
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            if (selected) {
                // Create Calendar for selected date
                selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(date.getYear(), date.getMonth() - 1, date.getDay());

                // Get current date for comparison
                Calendar currentCalendar = Calendar.getInstance();
                // Reset time portion to compare dates only
                currentCalendar.set(Calendar.HOUR_OF_DAY, 0);
                currentCalendar.set(Calendar.MINUTE, 0);
                currentCalendar.set(Calendar.SECOND, 0);
                currentCalendar.set(Calendar.MILLISECOND, 0);

                // Check if selected date is before current date
                if (selectedCalendar.before(currentCalendar)) {
                    Toast.makeText(EventDatePicker.this,
                            "Cannot select a date before today.",
                            Toast.LENGTH_SHORT).show();
                    // Don't proceed with date selection
                    return;
                }

                // Continue with date selection as before
                selectedDate = formatDate(selectedCalendar.get(Calendar.YEAR),
                        selectedCalendar.get(Calendar.MONTH) + 1,
                        selectedCalendar.get(Calendar.DAY_OF_MONTH));

                // Show confirmation container
                confirmationContainer.setVisibility(View.VISIBLE);
                selectedDateText.setText("Date: " + selectedDate);
                selectedTimeText.setText("Time: " + selectedTime);

                // Display events for the selected date
                if (eventsMap.containsKey(selectedDate)) {
                    List<EventInfo> events = eventsMap.get(selectedDate);
                    displayEvents(events);
                } else {
                    eventDetails.setText("No events on this date.");
                }
            }
        });

        // Set up confirmation buttons
        btnYes.setOnClickListener(v -> {
            // Send the selected date and time back to AddPendingEvent
            Intent resultIntent = new Intent();
            resultIntent.putExtra("selectedDate", selectedDate);
            resultIntent.putExtra("selectedTime", selectedTime);

            // Create a timestamp
            selectedCalendar.set(Calendar.HOUR_OF_DAY, selectedHour);
            selectedCalendar.set(Calendar.MINUTE, selectedMinute);
            resultIntent.putExtra("timestamp", selectedCalendar.getTimeInMillis());

            setResult(RESULT_OK, resultIntent);
            finish();
        });

        btnNo.setOnClickListener(v -> {
            // Hide confirmation and reset selection
            confirmationContainer.setVisibility(View.GONE);
        });

        // Set up back button
        backButton.setOnClickListener(v -> finish());
    }

    private void showTimePickerDialog() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minute) -> {
                    selectedHour = hourOfDay;
                    selectedMinute = minute;

                    // Format time
                    selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    selectedTimeText.setText("Time: " + selectedTime);
                },
                selectedHour, selectedMinute, false); // false = 12 hour format with AM/PM

        timePickerDialog.setTitle("Select Event Time");
        timePickerDialog.show();
    }

    private void fetchAllEvents() {
        // First, get all EventDetails
        Map<String, String> eventDetailsMap = new HashMap<>(); // Maps eventId to nameOfEvent
        Map<String, Date> eventDatesMap = new HashMap<>(); // Maps eventId to date

        db.collection("EventDetails")
                .get()
                .addOnSuccessListener(detailsSnapshots -> {
                    if (detailsSnapshots.isEmpty()) {
                        Toast.makeText(this, "No events found.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Store event details
                    for (QueryDocumentSnapshot document : detailsSnapshots) {
                        String eventId = document.getString("eventId");
                        String eventName = document.getString("nameOfEvent");
                        Timestamp timestamp = document.getTimestamp("date");

                        if (eventId != null && eventName != null && timestamp != null) {
                            eventDetailsMap.put(eventId, eventName);
                            eventDatesMap.put(eventId, timestamp.toDate());
                        }
                    }

                    // Now get EventInformation to check status
                    db.collection("EventInformation")
                            .get()
                            .addOnSuccessListener(infoSnapshots -> {
                                for (QueryDocumentSnapshot document : infoSnapshots) {
                                    String eventId = document.getString("eventId");
                                    String status = document.getString("status");

                                    // Only process Accepted or Pending events
                                    if (eventId != null && status != null &&
                                            (status.equals("Accepted") || status.equals("Pending"))) {

                                        // If we have details for this event
                                        if (eventDetailsMap.containsKey(eventId) && eventDatesMap.containsKey(eventId)) {
                                            String eventName = eventDetailsMap.get(eventId);
                                            Date eventDate = eventDatesMap.get(eventId);

                                            // Format date for eventsMap
                                            String formattedDate = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault()).format(eventDate);

                                            // Add the event to the corresponding date in the map
                                            eventsMap.putIfAbsent(formattedDate, new ArrayList<>());
                                            eventsMap.get(formattedDate).add(new EventInfo(eventName, status));

                                            // Add date to appropriate eventDays set for decoration
                                            Calendar calendar = Calendar.getInstance();
                                            calendar.setTime(eventDate);
                                            CalendarDay calendarDay = CalendarDay.from(
                                                    calendar.get(Calendar.YEAR),
                                                    calendar.get(Calendar.MONTH) + 1,
                                                    calendar.get(Calendar.DAY_OF_MONTH)
                                            );

                                            if (status.equals("Accepted")) {
                                                acceptedEventDays.add(calendarDay);
                                            } else if (status.equals("Pending")) {
                                                pendingEventDays.add(calendarDay);
                                            }
                                        }
                                    }
                                }

                                // Apply decorators to the calendar
                                if (!acceptedEventDays.isEmpty()) {
                                    calendarView.addDecorator(new EventDecorator(
                                            ContextCompat.getColor(this, R.color.event_marker_color),
                                            acceptedEventDays));
                                }

                                if (!pendingEventDays.isEmpty()) {
                                    calendarView.addDecorator(new EventDecorator(
                                            ContextCompat.getColor(this, R.color.yellow),
                                            pendingEventDays));
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to load event status: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load events: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void displayEvents(List<EventInfo> events) {
        StringBuilder eventsString = new StringBuilder("Events on this date:\n\n");
        for (EventInfo event : events) {
            String statusIndicator = event.getStatus().equals("Pending") ? "⚠️ (Pending)" : "✓ (Accepted)";
            eventsString.append("• ").append(event.getName()).append(" ").append(statusIndicator).append("\n");
        }
        eventDetails.setText(eventsString.toString());
    }

    private String formatDate(int year, int month, int day) {
        return String.format(Locale.getDefault(), "%02d-%02d-%04d", month, day, year);
    }

    /**
     * Class to hold event information including status
     */
    private static class EventInfo {
        private final String name;
        private final String status;

        public EventInfo(String name, String status) {
            this.name = name;
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public String getStatus() {
            return status;
        }
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