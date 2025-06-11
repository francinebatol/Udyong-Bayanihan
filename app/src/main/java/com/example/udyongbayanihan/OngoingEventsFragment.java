package com.example.udyongbayanihan;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class OngoingEventsFragment extends Fragment {

    private static final String TAG = "OngoingEventsFragment";

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private ProgressBar progressBar;
    private UserEventsAdapter adapter;
    private ArrayList<Post> ongoingEvents;
    private FirebaseFirestore db;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events, container, false);

        recyclerView = view.findViewById(R.id.eventsRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        ongoingEvents = new ArrayList<>();
        adapter = new UserEventsAdapter(getContext(), ongoingEvents, false); // "false" means not showing feedback button
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // Get user ID from arguments
        if (getArguments() != null) {
            userId = getArguments().getString("userId");
        }

        if (userId != null) {
            loadOngoingEvents();
        } else {
            showEmptyState("User ID not found");
        }

        return view;
    }

    private void loadOngoingEvents() {
        progressBar.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);

        // Get current date for comparison
        Date currentDate = new Date();

        // First get all events that the user has joined
        db.collection("UserJoinEvents")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(joinedSnap -> {
                    if (joinedSnap.isEmpty()) {
                        showEmptyState("You haven't joined any events yet");
                        return;
                    }

                    // List to hold all joined event IDs
                    List<String> joinedEventIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : joinedSnap) {
                        joinedEventIds.add(doc.getString("eventId"));
                    }

                    // Now fetch each event detail to determine if it's ongoing
                    for (String eventId : joinedEventIds) {
                        db.collection("EventDetails")
                                .document(eventId)
                                .get()
                                .addOnSuccessListener(eventDoc -> {
                                    if (eventDoc.exists()) {
                                        Timestamp eventDate = eventDoc.getTimestamp("date");

                                        // Skip events without dates
                                        if (eventDate == null) return;

                                        // Check if event is in the future (ongoing)
                                        if (eventDate.toDate().after(currentDate)) {
                                            // Fetch associated EventInformation
                                            db.collection("EventInformation")
                                                    .document(eventId)
                                                    .get()
                                                    .addOnSuccessListener(eventInfoDoc -> {
                                                        if (eventInfoDoc.exists()) {
                                                            // Get real-time participant count
                                                            db.collection("UserJoinEvents")
                                                                    .whereEqualTo("eventId", eventId)
                                                                    .get()
                                                                    .addOnSuccessListener(joinSnapshots -> {
                                                                        final long participantsJoined = joinSnapshots.size();

                                                                        // Create a Post object
                                                                        Post post = createPostFromDocuments(eventDoc, eventInfoDoc, participantsJoined);

                                                                        if (post != null) {
                                                                            ongoingEvents.add(post);

                                                                            // Sort events by date (nearest first)
                                                                            sortEventsByDate();

                                                                            // Update UI
                                                                            updateUI();
                                                                        }
                                                                    });
                                                        }
                                                    });
                                        }
                                    }
                                });
                    }

                    // After fetching all events, check if we found any ongoing events
                    // This needs to be delayed to ensure we've processed all events
                    recyclerView.postDelayed(() -> {
                        if (ongoingEvents.isEmpty()) {
                            showEmptyState("No ongoing events found");
                        }
                        progressBar.setVisibility(View.GONE);
                    }, 2000); // 2-second delay to allow fetches to complete
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching joined events", e);
                    showEmptyState("Error loading events");
                });
    }

    private Post createPostFromDocuments(DocumentSnapshot eventDoc, DocumentSnapshot eventInfoDoc, long participantsJoined) {
        try {
            Post post = new Post();
            post.setEventId(eventDoc.getId());
            post.setNameOfEvent(eventDoc.getString("nameOfEvent"));
            post.setTypeOfEvent(eventDoc.getString("typeOfEvent"));
            post.setDate(eventDoc.getTimestamp("date"));
            post.setCaption(eventDoc.getString("caption"));
            post.setBarangay(eventDoc.getString("barangay"));
            post.setImageUrls((List<String>) eventDoc.get("imageUrls"));

            // Data from EventInformation
            post.setOrganizations(eventInfoDoc.getString("organizations"));
            post.setHeadCoordinator(eventInfoDoc.getString("headCoordinator"));
            post.setEventSkills((List<String>) eventInfoDoc.get("eventSkills"));
            post.setStatus(eventInfoDoc.getString("status"));

            // Set volunteers count
            Long volunteerNeeded = eventDoc.getLong("volunteerNeeded");
            post.setVolunteerNeeded(volunteerNeeded != null ? volunteerNeeded.intValue() : 0);
            post.setParticipantsJoined((int) participantsJoined);

            // Set button text to "Already Joined"
            post.setJoinButtonText("Already Joined");

            return post;
        } catch (Exception e) {
            Log.e(TAG, "Error creating post object", e);
            return null;
        }
    }

    private void sortEventsByDate() {
        Collections.sort(ongoingEvents, (event1, event2) -> {
            Date date1 = event1.getDate() != null ? event1.getDate().toDate() : new Date(Long.MAX_VALUE);
            Date date2 = event2.getDate() != null ? event2.getDate().toDate() : new Date(Long.MAX_VALUE);
            return date1.compareTo(date2);
        });
    }

    private void updateUI() {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (ongoingEvents.isEmpty()) {
                showEmptyState("No ongoing events found");
            } else {
                emptyStateText.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();
            }
            progressBar.setVisibility(View.GONE);
        });
    }

    private void showEmptyState(String message) {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            emptyStateText.setText(message);
            emptyStateText.setVisibility(View.VISIBLE);
        });
    }

    public void refreshEvents() {
        ongoingEvents.clear();
        adapter.notifyDataSetChanged();
        loadOngoingEvents();
    }
}