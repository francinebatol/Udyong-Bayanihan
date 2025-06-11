package com.example.udyongbayanihan;

import android.app.Activity;
import android.content.Intent;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EndedEventsFragment extends Fragment implements UserEventsAdapter.OnFeedbackButtonClickListener {

    private static final String TAG = "EndedEventsFragment";

    private RecyclerView recyclerView;
    private TextView emptyStateText;
    private ProgressBar progressBar;
    private UserEventsAdapter adapter;
    private ArrayList<Post> endedEvents;
    private FirebaseFirestore db;
    private String userId, uaddressId, unameId, uotherDetails;
    private Map<String, Boolean> feedbackSubmitted = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_events, container, false);

        recyclerView = view.findViewById(R.id.eventsRecyclerView);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        endedEvents = new ArrayList<>();
        adapter = new UserEventsAdapter(getContext(), endedEvents, true); // "true" means show feedback button when needed
        adapter.setFeedbackButtonClickListener(this);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // Get user ID from arguments
        if (getArguments() != null) {
            userId = getArguments().getString("userId");
            uaddressId = getArguments().getString("uaddressId");
            unameId = getArguments().getString("unameId");
            uotherDetails = getArguments().getString("uotherDetails");
        }

        if (userId != null) {
            loadEndedEvents();
        } else {
            showEmptyState("User ID not found");
        }

        return view;
    }

    private void loadEndedEvents() {
        progressBar.setVisibility(View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);
        endedEvents.clear();

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

                    // Check for feedback submission status
                    checkFeedbackSubmissions(joinedEventIds, () -> {
                        // Now fetch each event detail to determine if it's ended
                        for (String eventId : joinedEventIds) {
                            db.collection("EventDetails")
                                    .document(eventId)
                                    .get()
                                    .addOnSuccessListener(eventDoc -> {
                                        if (eventDoc.exists()) {
                                            Timestamp eventDate = eventDoc.getTimestamp("date");

                                            // Skip events without dates
                                            if (eventDate == null) return;

                                            // Check if event is in the past (ended)
                                            if (eventDate.toDate().before(currentDate)) {
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
                                                                                endedEvents.add(post);

                                                                                // Sort events
                                                                                sortEndedEvents();

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

                        // After fetching all events, check if we found any ended events
                        recyclerView.postDelayed(() -> {
                            if (endedEvents.isEmpty()) {
                                showEmptyState("No ended events found");
                            }
                            progressBar.setVisibility(View.GONE);
                        }, 2000); // 2-second delay to allow fetches to complete
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching joined events", e);
                    showEmptyState("Error loading events");
                });
    }

    private void checkFeedbackSubmissions(List<String> eventIds, Runnable onComplete) {
        feedbackSubmitted.clear();

        if (eventIds.isEmpty()) {
            onComplete.run();
            return;
        }

        final int[] count = {0};

        for (String eventId : eventIds) {
            // Check if feedback was submitted for this event
            db.collection("Feedback")
                    .document(userId + "_" + eventId)
                    .get()
                    .addOnSuccessListener(feedbackDoc -> {
                        feedbackSubmitted.put(eventId, feedbackDoc.exists());
                        count[0]++;

                        if (count[0] >= eventIds.size()) {
                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking feedback submission", e);
                        feedbackSubmitted.put(eventId, false); // Assume not submitted on error
                        count[0]++;

                        if (count[0] >= eventIds.size()) {
                            onComplete.run();
                        }
                    });
        }
    }

    private Post createPostFromDocuments(DocumentSnapshot eventDoc, DocumentSnapshot eventInfoDoc, long participantsJoined) {
        try {
            Post post = new Post();
            String eventId = eventDoc.getId();
            post.setEventId(eventId);
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
            post.setPostFeedback(Boolean.TRUE.equals(eventInfoDoc.getBoolean("postFeedback")));

            // Set volunteers count
            Long volunteerNeeded = eventDoc.getLong("volunteerNeeded");
            post.setVolunteerNeeded(volunteerNeeded != null ? volunteerNeeded.intValue() : 0);
            post.setParticipantsJoined((int) participantsJoined);

            // Set button text based on feedback status
            if (post.isPostFeedback() && !feedbackSubmitted.getOrDefault(eventId, false)) {
                post.setJoinButtonText("Answer Feedback");
            } else {
                post.setJoinButtonText("Event Ended");
            }

            // Assign priority score (needs feedback = 0, has feedback = 1)
            if (post.isPostFeedback() && !feedbackSubmitted.getOrDefault(eventId, false)) {
                post.setPriorityScore(0); // Needs feedback (higher priority)
            } else {
                post.setPriorityScore(1); // Already has feedback or doesn't need it (lower priority)
            }

            return post;
        } catch (Exception e) {
            Log.e(TAG, "Error creating post object", e);
            return null;
        }
    }

    private void sortEndedEvents() {
        Collections.sort(endedEvents, (event1, event2) -> {
            // First sort by priority score (needs feedback at top)
            int priorityComparison = Integer.compare(event1.getPriorityScore(), event2.getPriorityScore());
            if (priorityComparison != 0) {
                return priorityComparison;
            }

            // Then sort by date (most recent events at top) within each priority group
            Date date1 = event1.getDate() != null ? event1.getDate().toDate() : new Date(0);
            Date date2 = event2.getDate() != null ? event2.getDate().toDate() : new Date(0);
            return date2.compareTo(date1); // Reversed comparison for descending order
        });
    }

    private void updateUI() {
        if (getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            if (endedEvents.isEmpty()) {
                showEmptyState("No ended events found");
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

    @Override
    public void onFeedbackButtonClick(String eventId, String eventName) {
        Intent intent = new Intent(getActivity(), Feedback.class);
        intent.putExtra("userId", userId);
        intent.putExtra("eventId", eventId);
        intent.putExtra("eventName", eventName);
        getActivity().startActivityForResult(intent, Home.FEEDBACK_REQUEST_CODE);
    }

    public void refreshEvents() {
        loadEndedEvents();
    }
}