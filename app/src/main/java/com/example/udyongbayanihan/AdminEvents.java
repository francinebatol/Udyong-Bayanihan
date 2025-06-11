package com.example.udyongbayanihan;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class AdminEvents extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminEventsAdapter adapter;
    private ArrayList<Post> eventList;
    private FirebaseFirestore db;
    private String amAccountId, amUsername, amEmail, amDetails;
    private TextView noEventsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_events);

        initializeVariables();
        validateInputs();
        Log.d("AdminEvents", "Current amAccountId: " + amAccountId);
        recyclerView = findViewById(R.id.recyclerView);
        setupRecyclerView();
        loadAdminEvents();
    }

    private void initializeVariables() {
        db = FirebaseFirestore.getInstance();
        eventList = new ArrayList<>();

        // Get admin account details from intent
        amAccountId = getIntent().getStringExtra("amAccountId");
        amUsername = getIntent().getStringExtra("amUsername");
        amEmail = getIntent().getStringExtra("amEmail");
        amDetails = getIntent().getStringExtra("amDetails");

        recyclerView = findViewById(R.id.postList);
        noEventsText = findViewById(R.id.noEventsText);
    }

    private void validateInputs() {
        if (amAccountId == null || amAccountId.isEmpty()) {
            Toast.makeText(this, "Error: Admin Account ID not passed!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupRecyclerView() {
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminEventsAdapter(this, eventList);
        recyclerView.setAdapter(adapter);
    }

    private void loadAdminEvents() {
        db.collection("EventInformation")
                .whereEqualTo("amAccountId", amAccountId)
                .get()
                .addOnSuccessListener(eventInfoSnapshots -> {
                    if (!eventInfoSnapshots.isEmpty()) {
                        processEventInformation(eventInfoSnapshots);
                    } else {
                        showNoEventsMessage("No events found for this admin");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("AdminEvents", "Error loading events: " + e.getMessage());
                    showNoEventsMessage("Failed to load events");
                });
    }

    private void processEventInformation(QuerySnapshot eventInfoSnapshots) {
        Log.d("AdminEvents", "Found " + eventInfoSnapshots.size() + " events for admin");
        eventList.clear();
        int totalEvents = eventInfoSnapshots.size();
        final int[] processedEvents = {0};

        if (totalEvents == 0) {
            updateUI();
            return;
        }

        for (QueryDocumentSnapshot eventInfoDoc : eventInfoSnapshots) {
            EventInformation eventInfo = eventInfoDoc.toObject(EventInformation.class);
            String eventId = eventInfo.getEventId();

            // Fetch corresponding event details
            db.collection("EventDetails")
                    .whereEqualTo("eventId", eventId)
                    .get()
                    .addOnSuccessListener(eventDetailsSnapshots -> {
                        Log.d("AdminEvents", "Found " + eventDetailsSnapshots.size()
                                + " details for event " + eventId);

                        if (!eventDetailsSnapshots.isEmpty()) {
                            QueryDocumentSnapshot eventDetailsDoc =
                                    (QueryDocumentSnapshot) eventDetailsSnapshots.getDocuments().get(0);
                            EventDetails eventDetails = eventDetailsDoc.toObject(EventDetails.class);

                            // Create post object combining both information
                            Post post = new Post();
                            post.setEventId(eventId);
                            post.setAmAccountId(eventInfo.getAmAccountId());
                            post.setOrganizations(eventInfo.getOrganizations());
                            post.setHeadCoordinator(eventInfo.getHeadCoordinator());
                            post.setEventSkills(eventInfo.getEventSkills());
                            post.setStatus(eventInfo.getStatus());

                            post.setNameOfEvent(eventDetails.getNameOfEvent());
                            post.setTypeOfEvent(eventDetails.getTypeOfEvent());
                            post.setDate(eventDetails.getDate());
                            post.setVolunteerNeeded(eventDetails.getVolunteerNeeded());
                            post.setParticipantsJoined(eventDetails.getParticipantsJoined());
                            post.setCaption(eventDetails.getCaption());
                            post.setImageUrls(eventDetails.getImageUrls());
                            post.setBarangay(eventDetails.getBarangay());

                            eventList.add(post);
                        }

                        processedEvents[0]++;
                        if (processedEvents[0] == totalEvents) {
                            updateUI();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("AdminEvents", "Error loading event details: " + e.getMessage());
                        processedEvents[0]++;
                        if (processedEvents[0] == totalEvents) {
                            updateUI();
                        }
                    });
        }
    }

    private void updateUI() {
        if (!eventList.isEmpty()) {
            recyclerView.setVisibility(View.VISIBLE);
            noEventsText.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        } else {
            showNoEventsMessage("No events available");
        }
    }

    private void showNoEventsMessage(String message) {
        noEventsText.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        noEventsText.setText(message);
    }
}