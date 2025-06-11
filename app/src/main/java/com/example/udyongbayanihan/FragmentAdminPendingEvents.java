package com.example.udyongbayanihan;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.bumptech.glide.Glide;
import androidx.viewpager2.widget.ViewPager2;
import android.widget.ImageView;


public class FragmentAdminPendingEvents extends Fragment {
    private RecyclerView recyclerView;
    private PendingEventsAdapter adapter;
    private List<EventModel> eventsList;
    private FirebaseFirestore db;
    private String currentAdminId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_pending_events, container, false);

        if (getActivity() != null) {
            currentAdminId = getActivity().getIntent().getStringExtra("amAccountId");
        }

        System.out.println("Current Admin ID in Fragment: " + currentAdminId);

        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recyclerViewPendingEvents);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventsList = new ArrayList<>();
        adapter = new PendingEventsAdapter(eventsList);
        recyclerView.setAdapter(adapter);

        loadPendingEvents();
        return view;
    }

    private void loadPendingEvents() {
        // Query EventInformation for events with matching amAccountId and Pending status
        db.collection("EventInformation")
                .whereEqualTo("amAccountId", currentAdminId)  // Changed from amAccountid to match your data
                .whereEqualTo("status", "Pending")
                .get()
                .addOnSuccessListener(eventInfoDocs -> {
                    System.out.println("Found " + eventInfoDocs.size() + " pending events for admin: " + currentAdminId);

                    for (QueryDocumentSnapshot eventInfoDoc : eventInfoDocs) {
                        String eventId = eventInfoDoc.getString("eventId");
                        System.out.println("Processing event with ID: " + eventId);

                        // Get the corresponding event details
                        db.collection("EventDetails")
                                .whereEqualTo("eventId", eventId)
                                .get()
                                .addOnSuccessListener(eventDetailsDocs -> {
                                    for (QueryDocumentSnapshot detailsDoc : eventDetailsDocs) {
                                        System.out.println("Found matching event details for event: " + eventId);

                                        EventModel event = new EventModel(
                                                eventId,
                                                detailsDoc.getString("nameOfEvent"),
                                                detailsDoc.getString("typeOfEvent"),
                                                detailsDoc.getString("caption"),
                                                (List<String>) detailsDoc.get("imageUrls"),
                                                detailsDoc.getLong("volunteerNeeded"),
                                                "Pending",
                                                eventInfoDoc.getString("organizations"),
                                                detailsDoc.getTimestamp("date").toDate(),
                                                detailsDoc.getString("barangay"),
                                                eventInfoDoc.getString("headCoordinator"),
                                                (List<String>) eventInfoDoc.get("eventSkills")
                                        );

                                        eventsList.add(event);
                                        adapter.notifyDataSetChanged();

                                        System.out.println("Added event to list: " + event.getName());
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    System.out.println("Error getting EventDetails: " + e.getMessage());
                                });
                    }

                    if (eventInfoDocs.isEmpty()) {
                        System.out.println("No pending events found for admin: " + currentAdminId);
                    }
                })
                .addOnFailureListener(e -> {
                    System.out.println("Error getting EventInformation: " + e.getMessage());
                });
    }
}