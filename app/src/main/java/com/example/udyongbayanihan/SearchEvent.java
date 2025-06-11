package com.example.udyongbayanihan;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchEvent extends AppCompatActivity {

    private static final String TAG = "SearchEvent";
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private SearchEventAdapter adapter;
    private List<Post> eventsList;
    private TextView noResultsText;
    private SearchView searchView;
    private ImageButton imgbtnBack;
    private boolean hasSearched = false; // Flag to track if user has searched

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_event);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        recyclerView = findViewById(R.id.searchResultsList);
        noResultsText = findViewById(R.id.noResultsText);
        searchView = findViewById(R.id.searchView);
        imgbtnBack = findViewById(R.id.imgbtnBack);

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventsList = new ArrayList<>();
        adapter = new SearchEventAdapter(this, eventsList, event -> {
            // Handle event click
            Intent intent = new Intent();
            intent.putExtra("eventName", event.getNameOfEvent());
            setResult(RESULT_OK, intent);
            finish();
        });
        recyclerView.setAdapter(adapter);

        // Set up back button
        imgbtnBack.setOnClickListener(v -> finish());

        // Set up search functionality
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                hasSearched = true; // User has searched
                adapter.filter(query);
                checkNoResults();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!newText.isEmpty()) {
                    hasSearched = true; // User has typed something
                }
                adapter.filter(newText);
                checkNoResults();
                return true;
            }
        });

        // Load events from Firestore
        loadAcceptedEvents();
    }

    private void loadAcceptedEvents() {
        db.collection("EventInformation")
                .whereEqualTo("status", "Accepted")
                .get()
                .addOnSuccessListener(eventInfoSnapshots -> {
                    if (!eventInfoSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot eventInfoDoc : eventInfoSnapshots) {
                            String eventId = eventInfoDoc.getId();

                            // Get event details
                            db.collection("EventDetails")
                                    .whereEqualTo("eventId", eventId)
                                    .get()
                                    .addOnSuccessListener(eventDetailsSnapshots -> {
                                        if (!eventDetailsSnapshots.isEmpty()) {
                                            for (QueryDocumentSnapshot eventDetailsDoc : eventDetailsSnapshots) {
                                                // Create Post object
                                                Post post = new Post();
                                                post.setEventId(eventId);
                                                post.setNameOfEvent(eventDetailsDoc.getString("nameOfEvent"));
                                                post.setTypeOfEvent(eventDetailsDoc.getString("typeOfEvent"));
                                                post.setOrganizations(eventInfoDoc.getString("organizations"));
                                                post.setDate(eventDetailsDoc.getTimestamp("date"));

                                                eventsList.add(post);

                                                // Update adapter with new data
                                                adapter.notifyDataSetChanged();

                                                // Only check for no results if user has searched
                                                if (hasSearched) {
                                                    checkNoResults();
                                                } else {
                                                    // When first loading, always hide the no results message
                                                    noResultsText.setVisibility(View.GONE);
                                                    recyclerView.setVisibility(View.VISIBLE);
                                                }
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e(TAG, "Error loading event details: " + e.getMessage()));
                        }
                    } else {
                        // Only show no results if this is not the initial load
                        if (hasSearched) {
                            noResultsText.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading events: " + e.getMessage());
                    // Only show no results if user has searched
                    if (hasSearched) {
                        noResultsText.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void checkNoResults() {
        if (adapter.getItemCount() == 0 && hasSearched) {
            // Only show no results message if user has actively searched
            noResultsText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noResultsText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}