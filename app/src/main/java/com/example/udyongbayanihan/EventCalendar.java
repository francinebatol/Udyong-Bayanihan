package com.example.udyongbayanihan;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
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

public class EventCalendar extends AppCompatActivity {

    private MaterialCalendarView calendarView;
    private TextView eventDetails;
    private HashMap<String, List<Event>> eventsMap;
    private List<Event> selectedEvents;
    private FirebaseFirestore db;
    private String userId, uaddressId, unameId, uotherDetails;
    private ImageButton imgbtnMessages, navHome, navCommunity, navProfile, navEventCalendar, navNotifications;
    private MessageBadgeManager badgeManager;
    private NotificationBadgeManager notificationBadgeManager;
    private ListenerRegistration unreadMessagesListener;
    private ListenerRegistration unreadNotificationsListener;
    private RelativeLayout messagesBadgeLayout;
    private RelativeLayout notificationsBadgeLayout;
    private BottomNavigation bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_calendar);

        calendarView = findViewById(R.id.calendarView);
        eventDetails = findViewById(R.id.eventDetails);

        db = FirebaseFirestore.getInstance();
        eventsMap = new HashMap<>();

        // Get the user ID passed from the previous activity
        userId = getIntent().getStringExtra("userId");
        uaddressId = getIntent().getStringExtra("uaddressId");
        unameId = getIntent().getStringExtra("unameId");
        uotherDetails = getIntent().getStringExtra("uotherDetails");
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Error: User ID not passed!", Toast.LENGTH_SHORT).show();
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

        eventDetails.setOnClickListener(v -> {
            if (selectedEvents != null && !selectedEvents.isEmpty()) {
                showPopup(selectedEvents);
            } else {
                Toast.makeText(EventCalendar.this, "No event selected", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize badge managers
        badgeManager = MessageBadgeManager.getInstance();
        notificationBadgeManager = NotificationBadgeManager.getInstance();

        // Initialize bottom navigation buttons
        navHome = findViewById(R.id.navHome);
        navCommunity = findViewById(R.id.navCommunity);
        navProfile = findViewById(R.id.navProfile);
        navEventCalendar = findViewById(R.id.navEventCalendar);
        navNotifications = findViewById(R.id.navNotifications);

        // Initialize BottomNavigation class with the necessary data
        bottomNavigation = new BottomNavigation(userId, uaddressId, unameId, uotherDetails);
        bottomNavigation.setupBottomNavigation(navHome, navCommunity, navProfile, navEventCalendar, navNotifications, this);

        imgbtnMessages = findViewById(R.id.imgbtnMessages);

        // Set up the message badge
        setupMessagesBadge();

        // Set up the notification badge
        setupNotificationBadge();
    }

    private void setupMessagesBadge() {
        // Set up the badge layout
        messagesBadgeLayout = bottomNavigation.setupMessageBadge(imgbtnMessages, this);

        // Start listening for unread messages
        unreadMessagesListener = badgeManager.startListeningForUnreadMessages(
                userId, "user", count -> {
                    runOnUiThread(() -> {
                        badgeManager.updateBadgeCount(messagesBadgeLayout, count);
                    });
                });

        // Set click listener on the button inside the badge layout
        ImageButton baseMessageButton = messagesBadgeLayout.findViewById(R.id.imgbtnBase);
        baseMessageButton.setOnClickListener(v -> {
            Intent intent = new Intent(EventCalendar.this, Messages.class);
            Bundle userDetails = new Bundle();
            userDetails.putString("uaddressId", uaddressId);
            userDetails.putString("unameId", unameId);
            userDetails.putString("uotherDetails", uotherDetails);
            UserDataHelper.passUserData(intent, userId, "user", userDetails);
            startActivity(intent);
        });
    }

    private void setupNotificationBadge() {
        // Add notification badge to the notifications button
        notificationsBadgeLayout = bottomNavigation.addNotificationBadge(navNotifications, this);

        // Start listening for notification updates
        unreadNotificationsListener = notificationBadgeManager.startListeningForUnreadNotifications(
                userId, count -> {
                    runOnUiThread(() -> {
                        if (notificationsBadgeLayout != null) {
                            notificationBadgeManager.updateBadgeCount(notificationsBadgeLayout, count);
                        }
                    });
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh notification badge count
        if (notificationBadgeManager != null && userId != null) {
            notificationBadgeManager.startListeningForUnreadNotifications(
                    userId, count -> {
                        runOnUiThread(() -> {
                            if (notificationsBadgeLayout != null) {
                                notificationBadgeManager.updateBadgeCount(notificationsBadgeLayout, count);
                            }
                        });
                    });
        }
    }

    private void fetchEventsFromFirestore() {
        // First, query EventInformation for accepted events
        db.collection("EventInformation")
                .whereEqualTo("status", "Accepted")  // Only get events with "Accepted" status
                .get()
                .addOnSuccessListener(eventInfoQuery -> {
                    // Create a list of accepted event IDs
                    List<String> acceptedEventIds = new ArrayList<>();
                    HashMap<String, QueryDocumentSnapshot> eventInfoMap = new HashMap<>();

                    for (QueryDocumentSnapshot infoDoc : eventInfoQuery) {
                        String eventId = infoDoc.getString("eventId");
                        if (eventId != null) {
                            acceptedEventIds.add(eventId);
                            eventInfoMap.put(eventId, infoDoc);
                        }
                    }

                    // If there are no accepted events, show message and return
                    if (acceptedEventIds.isEmpty()) {
                        Toast.makeText(EventCalendar.this, "No accepted events found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Process events in batches to avoid Firestore whereIn() limitation
                    fetchEventDetailsInBatches(acceptedEventIds, eventInfoMap);
                })
                .addOnFailureListener(e -> {
                    Log.e("EventCalendar", "Error loading EventInformation: " + e.getMessage(), e);
                    Toast.makeText(EventCalendar.this, "Error loading EventInformation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchEventDetailsInBatches(List<String> acceptedEventIds, HashMap<String, QueryDocumentSnapshot> eventInfoMap) {
        // Used to collect all event days for decoration
        final HashSet<CalendarDay> allEventDays = new HashSet<>();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

        // Calculate total batches based on batches of 30 (Firestore's actual limit)
        final int BATCH_SIZE = 30;
        final int totalBatches = (int) Math.ceil(acceptedEventIds.size() / (double)BATCH_SIZE);
        final int[] completedBatches = {0}; // Using array to modify in lambda

        // Process eventIds in batches of 30 (Firestore whereIn() limit)
        for (int i = 0; i < acceptedEventIds.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, acceptedEventIds.size());
            List<String> batch = acceptedEventIds.subList(i, end);

            // Log batch info for debugging
            Log.d("EventCalendar", "Processing batch " + (completedBatches[0] + 1) +
                    " of " + totalBatches + " with " + batch.size() + " events");

            db.collection("EventDetails")
                    .whereIn("eventId", batch)
                    .get()
                    .addOnSuccessListener(eventDetailsQuery -> {
                        Log.d("EventCalendar", "Successfully retrieved batch with " +
                                eventDetailsQuery.size() + " event details");

                        for (QueryDocumentSnapshot eventDetailsDoc : eventDetailsQuery) {
                            String eventId = eventDetailsDoc.getString("eventId");
                            String nameOfEvent = eventDetailsDoc.getString("nameOfEvent");
                            Timestamp timestamp = eventDetailsDoc.getTimestamp("date");

                            if (eventId != null && eventInfoMap.containsKey(eventId)) {
                                // Retrieve corresponding EventInformation
                                QueryDocumentSnapshot eventInfoDoc = eventInfoMap.get(eventId);
                                String headCoordinator = eventInfoDoc.getString("headCoordinator");
                                String organization = eventInfoDoc.getString("organizations");

                                if (nameOfEvent != null && headCoordinator != null && organization != null && timestamp != null) {
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
                                    allEventDays.add(calendarDay);

                                    // Create Event object
                                    Event event = new Event(nameOfEvent, headCoordinator, organization);

                                    // Add the event to the map for the corresponding date
                                    if (!eventsMap.containsKey(formattedDate)) {
                                        eventsMap.put(formattedDate, new ArrayList<>());
                                    }
                                    eventsMap.get(formattedDate).add(event);
                                }
                            }
                        }

                        // Keep track of completed batches and update UI when all batches are done
                        completedBatches[0]++;
                        Log.d("EventCalendar", "Completed " + completedBatches[0] + " of " + totalBatches + " batches");

                        if (completedBatches[0] >= totalBatches) {
                            // All batches are complete, update the calendar UI
                            Log.d("EventCalendar", "All batches completed. Marking " + allEventDays.size() + " days on calendar");
                            runOnUiThread(() -> markEventDates(allEventDays));
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("EventCalendar", "Error loading EventDetails batch: " + e.getMessage(), e);
                        Toast.makeText(EventCalendar.this, "Error loading event batch: " + e.getMessage(), Toast.LENGTH_SHORT).show();

                        // Even if one batch fails, increment counter to avoid deadlock
                        completedBatches[0]++;
                    });
        }
    }

    // New method to mark dates that have events
    private void markEventDates(HashSet<CalendarDay> eventDays) {
        if (eventDays.isEmpty()) {
            return;
        }

        // Add the event decorator to the calendar
        calendarView.addDecorator(new EventDecorator(
                ContextCompat.getColor(this, R.color.event_marker_color),
                eventDays
        ));
    }

    // Format the date as MM/dd/yyyy
    private String formatDate(int year, int month, int day) {
        return String.format(Locale.getDefault(), "%02d/%02d/%04d", month, day, year);
    }

    // Display the event details in the TextView
    private void displayEventDetails(List<Event> events) {
        StringBuilder details = new StringBuilder();
        for (Event event : events) {
            details.append(String.format(
                    Locale.getDefault(),
                    "Event Title: %s\nHead Coordinator: %s\nOrganization: %s\n\n",
                    event.getTitle(),
                    event.getHeadCoordinator(),
                    event.getOrganizer()
            ));
        }
        eventDetails.setText(details.toString().trim());
    }

    // Show a popup dialog with event details and options to select an event
    private void showPopup(List<Event> events) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select an Event");

        String[] eventTitles = new String[events.size()];
        for (int i = 0; i < events.size(); i++) {
            eventTitles[i] = events.get(i).getTitle();
        }

        builder.setItems(eventTitles, (dialog, which) -> {
            Event selectedEvent = events.get(which);
            Intent intent = new Intent(EventCalendar.this, Home.class);
            intent.putExtra("eventName", selectedEvent.getTitle());
            intent.putExtra("userId", userId);
            intent.putExtra("uotherDetails", uotherDetails);
            intent.putExtra("uaddressId", uaddressId);
            intent.putExtra("unameId", unameId);
            startActivity(intent);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up resources
        if (unreadMessagesListener != null) {
            unreadMessagesListener.remove();
            unreadMessagesListener = null;
        }

        if (unreadNotificationsListener != null) {
            unreadNotificationsListener.remove();
            unreadNotificationsListener = null;
        }

        if (bottomNavigation != null) {
            bottomNavigation.cleanup();
        }

        if (notificationBadgeManager != null) {
            notificationBadgeManager.stopListeningForUser(userId);
        }
    }
}