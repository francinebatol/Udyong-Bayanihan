package com.example.udyongbayanihan;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminNotification extends AppCompatActivity {
    private static final String TAG = "AdminNotification";
    private static final long LOADING_TIMEOUT = 15000; // 15 seconds timeout

    private String amAccountId;
    private RecyclerView notificationRecyclerView;
    private AdminNotificationAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;
    private FirebaseFirestore db;
    private Handler timeoutHandler = new Handler();
    private Runnable timeoutRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_notification);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get admin ID from intent
        amAccountId = getIntent().getStringExtra("amAccountId");
        if (amAccountId == null) {
            Log.e(TAG, "Admin ID is null");
            finish();
            return;
        }

        // Initialize views
        notificationRecyclerView = findViewById(R.id.notificationRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyView = findViewById(R.id.emptyView);

        // Set up RecyclerView
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminNotificationAdapter();
        notificationRecyclerView.setAdapter(adapter);

        // Set up SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadNotifications);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Schedule notification checks
        NotificationWorkManager.scheduleAdminEventStatusChecks(this, amAccountId);

        // Reset unread count since user is viewing notifications now
        AdminNotificationHelper.resetUnreadCount(this);

        // Load initial notifications
        loadNotifications();
    }

    private void loadNotifications() {
        swipeRefreshLayout.setRefreshing(true);
        List<AdminNotificationItem> notifications = new ArrayList<>();

        // Set a timeout to prevent infinite loading
        timeoutRunnable = () -> {
            if (swipeRefreshLayout.isRefreshing()) {
                Log.e(TAG, "Loading notifications timed out");
                Toast.makeText(AdminNotification.this, "Loading timed out. Please try again.", Toast.LENGTH_SHORT).show();
                swipeRefreshLayout.setRefreshing(false);
                updateUI(notifications);
            }
        };
        timeoutHandler.postDelayed(timeoutRunnable, LOADING_TIMEOUT);

        db.collection("EventInformation")
                .whereEqualTo("amAccountId", amAccountId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            Log.d(TAG, "No event information found for admin: " + amAccountId);
                            runOnUiThread(() -> {
                                timeoutHandler.removeCallbacks(timeoutRunnable);
                                swipeRefreshLayout.setRefreshing(false);
                                updateUI(notifications);
                            });
                            return;
                        }

                        int totalEvents = task.getResult().size();
                        Log.d(TAG, "Found " + totalEvents + " events for admin: " + amAccountId);
                        AtomicInteger processedEvents = new AtomicInteger(0);

                        // Process each event
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String eventId = document.getString("eventId");
                            String status = document.getString("status");
                            Log.d(TAG, "Event: " + eventId + ", Status: " + status);

                            // Mark all events as viewed when user opens the notification screen
                            if (("Accepted".equals(status) || "Rejected".equals(status)) && eventId != null) {
                                // Mark as viewed - this is important for badge counts
                                AdminNotificationHelper.markAsViewed(this, eventId, status);

                                // Ensure it's also marked as notified
                                if (!AdminNotificationHelper.hasBeenNotified(this, eventId, status)) {
                                    Log.d(TAG, "Marking event as notified: " + eventId + " with status: " + status);
                                    AdminNotificationHelper.markAsNotified(this, eventId, status);
                                    AdminNotificationHelper.storeNotificationTime(this, eventId, status, System.currentTimeMillis() / 1000);
                                }
                            }

                            // Continue with normal processing
                            processEvent(document, notifications, processedEvents, totalEvents);
                        }
                    } else {
                        Log.e(TAG, "Error getting event information", task.getException());
                        runOnUiThread(() -> {
                            timeoutHandler.removeCallbacks(timeoutRunnable);
                            swipeRefreshLayout.setRefreshing(false);
                            updateUI(notifications);
                        });
                    }
                });
    }

    private void processEvent(QueryDocumentSnapshot document, List<AdminNotificationItem> notifications,
                              AtomicInteger processedEvents, int totalEvents) {
        String eventId = document.getString("eventId");
        String status = document.getString("status");

        // Get feedback for rejected events
        String feedback = null;
        if ("Rejected".equals(status)) {
            feedback = document.getString("feedback");
            Log.d(TAG, "Found feedback for rejected event " + eventId + ": " + feedback);
        }

        if (("Accepted".equals(status) || "Rejected".equals(status)) && eventId != null) {
            // Note: We include all accepted/rejected events regardless of notification status
            // since we're displaying them in the UI now, not just sending system notifications

            // Store final values for lambda
            final String finalFeedback = feedback;
            final String finalEventId = eventId;
            final String finalStatus = status;

            db.collection("EventDetails")
                    .document(eventId)
                    .get()
                    .addOnSuccessListener(eventDetailsDoc -> {
                        if (eventDetailsDoc.exists()) {
                            String eventName = eventDetailsDoc.getString("nameOfEvent");
                            if (eventName != null) {
                                // Use the stored notification timestamp
                                long notificationTime = AdminNotificationHelper.getNotificationTime(
                                        this, finalEventId, finalStatus);

                                // Create notification item with feedback if available
                                AdminNotificationItem notificationItem;
                                if (finalFeedback != null && !finalFeedback.isEmpty()) {
                                    notificationItem = new AdminNotificationItem(
                                            finalEventId,
                                            eventName,
                                            finalStatus,
                                            notificationTime,
                                            finalFeedback
                                    );
                                } else {
                                    notificationItem = new AdminNotificationItem(
                                            finalEventId,
                                            eventName,
                                            finalStatus,
                                            notificationTime
                                    );
                                }

                                notifications.add(notificationItem);
                                Log.d(TAG, "Added notification for " + eventName);
                            }
                        } else {
                            Log.d(TAG, "Event details not found for event: " + finalEventId);
                        }

                        checkAndUpdateUI(notifications, processedEvents, totalEvents);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting event details: ", e);
                        checkAndUpdateUI(notifications, processedEvents, totalEvents);
                    });
        } else {
            checkAndUpdateUI(notifications, processedEvents, totalEvents);
        }
    }

    private void checkAndUpdateUI(List<AdminNotificationItem> notifications, AtomicInteger processedEvents, int totalEvents) {
        int processed = processedEvents.incrementAndGet();
        Log.d(TAG, "Processed " + processed + " of " + totalEvents + " events");

        if (processed >= totalEvents) {
            runOnUiThread(() -> {
                timeoutHandler.removeCallbacks(timeoutRunnable);
                swipeRefreshLayout.setRefreshing(false);
                updateUI(notifications);

                // Mark all notifications as viewed and reset unread count
                AdminNotificationHelper.markNotificationsAsViewed(this, notifications);
                AdminNotificationHelper.resetUnreadCount(this);
            });
        }
    }

    private void updateUI(List<AdminNotificationItem> notifications) {
        // Sort notifications by timestamp in descending order (newest first)
        Collections.sort(notifications, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

        // Update UI
        if (notifications.isEmpty()) {
            Log.d(TAG, "No notifications to display");
            emptyView.setVisibility(View.VISIBLE);
            notificationRecyclerView.setVisibility(View.GONE);
        } else {
            Log.d(TAG, "Displaying " + notifications.size() + " notifications");
            emptyView.setVisibility(View.GONE);
            notificationRecyclerView.setVisibility(View.VISIBLE);
            adapter.setNotifications(notifications);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh notifications when returning to this screen
        loadNotifications();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeoutHandler.removeCallbacks(timeoutRunnable);
        // Clean up any resources if needed
    }
}