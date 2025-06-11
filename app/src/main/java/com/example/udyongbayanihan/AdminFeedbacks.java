package com.example.udyongbayanihan;

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminFeedbacks extends AppCompatActivity {
    private RecyclerView eventsRecyclerView;
    private EventsForFeedbackAdapter adapter;
    private List<EventFeedbackModel> eventsList;
    private FirebaseFirestore db;
    private String amAccountId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_feedbacks);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get amAccountId from intent
        amAccountId = getIntent().getStringExtra("amAccountId");

        // Initialize RecyclerView
        eventsRecyclerView = findViewById(R.id.eventsForFeedbackList);
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventsList = new ArrayList<>();
        adapter = new EventsForFeedbackAdapter(eventsList);
        eventsRecyclerView.setAdapter(adapter);

        // Fetch events
        fetchEvents();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void fetchEvents() {
        // Query EventInformation to get eventIds for the current admin where postFeedback is true
        db.collection("EventInformation")
                .whereEqualTo("amAccountId", amAccountId)
                .whereEqualTo("postFeedback", true)
                .get()
                .addOnSuccessListener(eventInfoSnapshot -> {
                    List<String> eventIds = new ArrayList<>();
                    for (DocumentSnapshot doc : eventInfoSnapshot.getDocuments()) {
                        eventIds.add(doc.getId());
                    }

                    // If no events found, return early
                    if (eventIds.isEmpty()) {
                        return;
                    }

                    // Use a temporary list to collect all events
                    List<EventFeedbackModel> tempEventsList = new ArrayList<>();

                    // Use AtomicInteger to track completion of all queries
                    AtomicInteger queriesCompleted = new AtomicInteger(0);
                    int totalQueries = eventIds.size();

                    // Then, query EventDetails for those eventIds
                    for (String eventId : eventIds) {
                        db.collection("EventDetails")
                                .whereEqualTo("eventId", eventId)
                                .get()
                                .addOnSuccessListener(eventDetailsSnapshot -> {
                                    for (DocumentSnapshot doc : eventDetailsSnapshot.getDocuments()) {
                                        String nameOfEvent = doc.getString("nameOfEvent");
                                        tempEventsList.add(new EventFeedbackModel(eventId, nameOfEvent));
                                    }

                                    // Check if all queries are completed
                                    if (queriesCompleted.incrementAndGet() == totalQueries) {
                                        // Sort alphabetically by event name
                                        Collections.sort(tempEventsList, new Comparator<EventFeedbackModel>() {
                                            @Override
                                            public int compare(EventFeedbackModel event1, EventFeedbackModel event2) {
                                                return event1.getNameOfEvent().compareToIgnoreCase(event2.getNameOfEvent());
                                            }
                                        });

                                        // Update the main list and notify adapter
                                        eventsList.clear();
                                        eventsList.addAll(tempEventsList);
                                        adapter.notifyDataSetChanged();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    // Handle any errors
                                    e.printStackTrace();

                                    // Still increment the counter to prevent hanging
                                    if (queriesCompleted.incrementAndGet() == totalQueries) {
                                        // Sort and update even if some queries failed
                                        Collections.sort(tempEventsList, new Comparator<EventFeedbackModel>() {
                                            @Override
                                            public int compare(EventFeedbackModel event1, EventFeedbackModel event2) {
                                                return event1.getNameOfEvent().compareToIgnoreCase(event2.getNameOfEvent());
                                            }
                                        });

                                        eventsList.clear();
                                        eventsList.addAll(tempEventsList);
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle any errors
                    e.printStackTrace();
                });
    }
}