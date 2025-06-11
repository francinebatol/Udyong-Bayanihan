package com.example.udyongbayanihan;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillsBasedEvents extends AppCompatActivity {
    private FirebaseFirestore db;
    private Context context;
    private RecyclerView recyclerView;
    private SkillBasedEventsAdapter adapter;
    private List<EventModel> eventList;
    private String userId;
    private String unameId;
    private TextView tvNoSkillContent;

    @Override
    protected void onCreate(Bundle saveadInstanceState) {
        super.onCreate(saveadInstanceState);
        setContentView(R.layout.activity_skills_based_events);

        db = FirebaseFirestore.getInstance();
        context = this;
        String skillName = getIntent().getStringExtra("skillName");
        userId = getIntent().getStringExtra("userId");
        unameId = getIntent().getStringExtra("unameId");

        // Add debug logs to verify received parameters
        Log.d("SkillsBasedEvents", "Received: skillName=" + skillName + ", userId=" + userId + ", unameId=" + unameId);

        recyclerView = findViewById(R.id.recyclerSkillBasedEvents);
        tvNoSkillContent = findViewById(R.id.tvNoSkillContent);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        eventList = new ArrayList<>();
        adapter = new SkillBasedEventsAdapter(this, eventList, userId);
        recyclerView.setAdapter(adapter);

        if (skillName != null) {
            // Show loading state
            updateContentVisibility(false, true);
            // Fetch content
            fetchAllContent(skillName);
        } else {
            Toast.makeText(this, "Error: Skill name is missing", Toast.LENGTH_SHORT).show();
            Log.e("SkillsBasedEvents", "skillName is null - cannot fetch content");
            // Show empty state since we can't fetch anything
            updateContentVisibility(false, false);
        }
    }

    /**
     * Updates the visibility of RecyclerView and empty state message
     * @param hasContent true if we have content to show, false otherwise
     * @param isLoading true if content is still loading, false otherwise
     */
    private void updateContentVisibility(boolean hasContent, boolean isLoading) {
        runOnUiThread(() -> {
            if (isLoading) {
                // Content is loading, hide both
                recyclerView.setVisibility(View.INVISIBLE);
                tvNoSkillContent.setVisibility(View.GONE);
            } else if (hasContent) {
                // We have content, show recyclerView and hide empty message
                recyclerView.setVisibility(View.VISIBLE);
                tvNoSkillContent.setVisibility(View.GONE);
            } else {
                // No content, hide recyclerView and show empty message
                recyclerView.setVisibility(View.GONE);
                tvNoSkillContent.setVisibility(View.VISIBLE);
            }
        });
    }

    private void fetchAllContent(String skillName) {
        Log.d("SkillsBasedEvents", "Fetching content for skill: " + skillName);

        List<EventModel> tempList = new ArrayList<>();
        Map<String, Date> eventSharedTimes = new HashMap<>();

        // Fetch posts with their timestamps
        db.collection("CommunityGroupSkills")
                .document(skillName)
                .collection("Posts")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("SkillsBasedEvents", "Posts query returned: " + querySnapshot.size() + " documents");
                    if (querySnapshot.isEmpty()) {
                        Log.d("SkillsBasedEvents", "No posts found for skill: " + skillName);
                        // Continue to fetch events even if no posts
                        fetchEventsBySkill(skillName, tempList, eventSharedTimes);
                        return;
                    }

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Log.d("SkillsBasedEvents", "Adding post: " + doc.getId());
                        EventModel skillPost = new EventModel(
                                doc.getId(),
                                doc.getString("adminName"),
                                doc.getString("position"),
                                doc.getString("postContent"),
                                doc.getTimestamp("timestamp") != null ?
                                        doc.getTimestamp("timestamp").toDate() : null
                        );
                        tempList.add(skillPost);
                    }

                    // Fetch events after posts are loaded
                    fetchEventsBySkill(skillName, tempList, eventSharedTimes);
                })
                .addOnFailureListener(e -> {
                    Log.e("SkillsBasedEvents", "Error fetching posts: " + e.getMessage(), e);
                    Toast.makeText(context,
                            "Error fetching skill posts: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    // Even if posts query fails, try to fetch events
                    fetchEventsBySkill(skillName, tempList, eventSharedTimes);
                });
    }

    private void fetchEventsBySkill(String skillName, List<EventModel> tempList, Map<String, Date> eventSharedTimes) {
        Log.d("SkillsBasedEvents", "Fetching events for skill: " + skillName);

        db.collection("CommunityGroupSkills")
                .document(skillName)
                .collection(skillName) // This path might be incorrect
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d("SkillsBasedEvents", "Events query returned: " + querySnapshot.size() + " documents");
                    List<String> eventIds = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String eventId = doc.getString("eventId");
                        Timestamp sharedTimestamp = doc.getTimestamp("timeShared");

                        if (eventId != null && sharedTimestamp != null) {
                            eventIds.add(eventId);
                            eventSharedTimes.put(eventId, sharedTimestamp.toDate());
                            Log.d("SkillsBasedEvents", "Added event ID: " + eventId);
                        }
                    }

                    if (eventIds.isEmpty() && tempList.isEmpty()) {
                        Toast.makeText(context, "No content found for " + skillName, Toast.LENGTH_SHORT).show();
                        Log.w("SkillsBasedEvents", "No events or posts found for skill: " + skillName);
                        // Show empty state
                        updateContentVisibility(false, false);
                        return;
                    }

                    if (eventIds.isEmpty()) {
                        // If we have posts but no events, display the posts
                        Log.d("SkillsBasedEvents", "No events found, displaying only posts");
                        sortAndDisplayContent(tempList);
                        return;
                    }

                    fetchEventDetails(eventIds, eventSharedTimes, tempList);
                })
                .addOnFailureListener(e -> {
                    Log.e("SkillsBasedEvents", "Error fetching events: " + e.getMessage(), e);
                    Toast.makeText(context,
                            "Error fetching events: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    // Even if events query fails, display any posts we found
                    if (!tempList.isEmpty()) {
                        sortAndDisplayContent(tempList);
                    } else {
                        // Show empty state if we have no content at all
                        updateContentVisibility(false, false);
                    }
                });
    }

    private void fetchEventDetails(List<String> eventIds, Map<String, Date> eventSharedTimes, List<EventModel> tempList) {
        Log.d("SkillsBasedEvents", "Starting to fetch details for " + eventIds.size() + " events");

        if (eventIds.isEmpty()) {
            // If no events, sort and display posts only
            Log.d("SkillsBasedEvents", "No events to fetch, displaying posts only");
            sortAndDisplayContent(tempList);
            return;
        }

        // Counter to track completed event processing
        final int[] completedEvents = {0};
        final int[] failedEvents = {0};

        for (String eventId : eventIds) {
            Log.d("SkillsBasedEvents", "Fetching details for event: " + eventId);

            db.collection("EventDetails")
                    .whereEqualTo("eventId", eventId)
                    .get()
                    .addOnSuccessListener(eventDetailsSnapshot -> {
                        if (eventDetailsSnapshot.isEmpty()) {
                            Log.w("SkillsBasedEvents", "No EventDetails found for eventId: " + eventId);
                            failedEvents[0]++;
                            checkAllEventsProcessed(eventIds.size(), completedEvents[0], failedEvents[0], tempList);
                            return;
                        }

                        DocumentSnapshot eventDetails = eventDetailsSnapshot.getDocuments().get(0);
                        Log.d("SkillsBasedEvents", "Found EventDetails for: " + eventId);

                        // Now fetch event information
                        db.collection("EventInformation")
                                .document(eventId)
                                .get()
                                .addOnSuccessListener(eventInfoDoc -> {
                                    if (!eventInfoDoc.exists()) {
                                        Log.w("SkillsBasedEvents", "No EventInformation found for eventId: " + eventId);
                                        failedEvents[0]++;
                                        checkAllEventsProcessed(eventIds.size(), completedEvents[0], failedEvents[0], tempList);
                                        return;
                                    }

                                    Log.d("SkillsBasedEvents", "Found EventInformation for: " + eventId);

                                    try {
                                        // Try to extract image URLs safely with proper type checking
                                        List<String> imageUrls = new ArrayList<>();
                                        Object imageUrlsObj = eventDetails.get("imageUrls");

                                        if (imageUrlsObj instanceof List) {
                                            imageUrls = (List<String>) imageUrlsObj;
                                        } else if (imageUrlsObj instanceof Map) {
                                            Map<String, String> imageUrlsMap = (Map<String, String>) imageUrlsObj;
                                            imageUrls.addAll(imageUrlsMap.values());
                                        }

                                        // Create event model object
                                        EventModel event = new EventModel(
                                                eventId,
                                                eventDetails.getString("nameOfEvent"),
                                                eventDetails.getString("typeOfEvent"),
                                                eventDetails.getString("caption"),
                                                imageUrls,
                                                eventDetails.getLong("volunteerNeeded"),
                                                eventInfoDoc.getString("status"),
                                                eventInfoDoc.getString("organizations"),
                                                eventDetails.getTimestamp("date") != null ?
                                                        eventDetails.getTimestamp("date").toDate() : null,
                                                eventDetails.getString("barangay"),
                                                eventInfoDoc.getString("headCoordinator"),
                                                (List<String>) eventInfoDoc.get("eventSkills")
                                        );

                                        // Set timestamp for sorting
                                        event.setTimestamp(eventSharedTimes.get(eventId));
                                        tempList.add(event);

                                        Log.d("SkillsBasedEvents", "Added event to list: " + event.getName());

                                        // Increment completed events counter
                                        completedEvents[0]++;

                                        // Check if all events are processed
                                        checkAllEventsProcessed(eventIds.size(), completedEvents[0], failedEvents[0], tempList);

                                    } catch (Exception e) {
                                        Log.e("SkillsBasedEvents", "Error creating EventModel: " + e.getMessage(), e);
                                        failedEvents[0]++;
                                        checkAllEventsProcessed(eventIds.size(), completedEvents[0], failedEvents[0], tempList);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("SkillsBasedEvents", "Error fetching EventInformation: " + e.getMessage(), e);
                                    failedEvents[0]++;
                                    checkAllEventsProcessed(eventIds.size(), completedEvents[0], failedEvents[0], tempList);
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("SkillsBasedEvents", "Error fetching EventDetails: " + e.getMessage(), e);
                        failedEvents[0]++;
                        checkAllEventsProcessed(eventIds.size(), completedEvents[0], failedEvents[0], tempList);
                    });
        }
    }

    // Helper method to check if all events are processed
    private void checkAllEventsProcessed(int totalEvents, int completed, int failed, List<EventModel> tempList) {
        Log.d("SkillsBasedEvents", "Events processed: " + completed + " failed: " + failed + " out of " + totalEvents);

        if (completed + failed >= totalEvents) {
            // All events have been processed (successfully or with failure)
            Log.d("SkillsBasedEvents", "All events processed, moving to display");

            // Run on UI thread to ensure UI updates happen safely
            runOnUiThread(() -> {
                sortAndDisplayContent(tempList);
            });
        }
    }

    private void sortAndDisplayContent(List<EventModel> contentList) {
        Log.d("SkillsBasedEvents", "Sorting and displaying content, items: " + contentList.size());

        if (contentList.isEmpty()) {
            Log.w("SkillsBasedEvents", "No content to display");
            updateContentVisibility(false, false);
            return;
        }

        // Sort by timestamp (most recent first)
        Collections.sort(contentList, (item1, item2) -> {
            Date date1 = item1.getTimestamp();
            Date date2 = item2.getTimestamp();

            if (date1 == null && date2 == null) return 0;
            if (date1 == null) return 1;
            if (date2 == null) return -1;

            return date2.compareTo(date1);
        });

        eventList.clear();
        eventList.addAll(contentList);

        // Make sure the adapter is properly set on the RecyclerView
        if (recyclerView.getAdapter() == null) {
            recyclerView.setAdapter(adapter);
        }

        adapter.notifyDataSetChanged();
        Log.d("SkillsBasedEvents", "Updated adapter with " + eventList.size() + " items");

        // Show content view, hide empty state
        updateContentVisibility(true, false);
    }
}