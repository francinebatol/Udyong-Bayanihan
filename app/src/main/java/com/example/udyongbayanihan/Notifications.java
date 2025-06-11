package com.example.udyongbayanihan;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class Notifications extends AppCompatActivity {
    private static final String TAG = "Notifications";

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private String userId, uaddressId, unameId, uotherDetails;
    private String selectedEventName;
    private ImageButton imgbtnMessages, navHome, navCommunity, navProfile, navEventCalendar, navNotifications;
    private MessageBadgeManager badgeManager;
    private NotificationBadgeManager notificationBadgeManager;
    private ListenerRegistration unreadMessagesListener;
    private ListenerRegistration unreadNotificationsListener;
    private RelativeLayout messagesBadgeLayout;
    private RelativeLayout notificationsBadgeLayout;
    private BottomNavigation bottomNavigation;
    private TextView emptyNotificationsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notifications);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("userId");
        uaddressId = getIntent().getStringExtra("uaddressId");
        unameId = getIntent().getStringExtra("unameId");
        uotherDetails = getIntent().getStringExtra("uotherDetails");

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Error: User ID not passed!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize badge managers
        badgeManager = MessageBadgeManager.getInstance();
        notificationBadgeManager = NotificationBadgeManager.getInstance();

        // Initialize UI elements
        imgbtnMessages = findViewById(R.id.imgbtnMessages);
        emptyNotificationsText = findViewById(R.id.emptyNotificationsText);

        // Initialize bottom navigation buttons
        navHome = findViewById(R.id.navHome);
        navCommunity = findViewById(R.id.navCommunity);
        navProfile = findViewById(R.id.navProfile);
        navEventCalendar = findViewById(R.id.navEventCalendar);
        navNotifications = findViewById(R.id.navNotifications);

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.notificationRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(this, notificationList);
        recyclerView.setAdapter(adapter);

        // Initialize BottomNavigation class with the necessary data
        bottomNavigation = new BottomNavigation(userId, uaddressId, unameId, uotherDetails);
        bottomNavigation.setupBottomNavigation(navHome, navCommunity, navProfile, navEventCalendar, navNotifications, this);

        // Set up the message badge
        setupMessagesBadge();

        // Set up notification badge
        setupNotificationBadge();

        // Reset notification badge count when this activity is opened
        notificationBadgeManager.resetUnreadCount(userId);

        // Load user verification status
        fetchVerificationStatus();

        // Load notifications from Firestore
        loadNotifications();

        // Check if this activity was launched from a verification notification
        checkForVerificationDeepLink();
    }

    private void fetchVerificationStatus() {
        db.collection("usersAccount")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("usersStatus");
                        if (status != null) {
                            // Update the adapter with current verification status
                            adapter.setUserVerificationStatus(status);
                            Log.d(TAG, "Fetched and set user verification status: " + status);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching verification status", e);
                });
    }

    private void setupNotificationBadge() {
        // Add notification badge to the notifications button
        notificationsBadgeLayout = bottomNavigation.addNotificationBadge(navNotifications, this);

        // In Notifications activity, we want to force the badge to be hidden (count = 0)
        unreadNotificationsListener = notificationBadgeManager.startListeningForUnreadNotifications(
                userId, count -> {
                    runOnUiThread(() -> {
                        if (notificationsBadgeLayout != null) {
                            // Always force the badge to show 0 in this activity
                            notificationBadgeManager.updateBadgeCount(notificationsBadgeLayout, 0);
                        }
                    });
                });

        // Immediately mark all notifications as read upon entering this screen
        notificationBadgeManager.resetUnreadCount(userId);
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
            Intent intent = new Intent(Notifications.this, Messages.class);
            Bundle userDetails = new Bundle();
            userDetails.putString("uaddressId", uaddressId);
            userDetails.putString("unameId", unameId);
            userDetails.putString("uotherDetails", uotherDetails);
            UserDataHelper.passUserData(intent, userId, "user", userDetails);
            startActivity(intent);
        });
    }

    private void loadNotifications() {
        Log.d(TAG, "Loading notifications for user: " + userId);

        db.collection("Notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading notifications", error);
                        return;
                    }

                    notificationList.clear();
                    if (value != null) {
                        Log.d(TAG, "Retrieved " + value.size() + " notifications");
                        for (QueryDocumentSnapshot doc : value) {
                            Notification notification = doc.toObject(Notification.class);
                            notification.setId(doc.getId());
                            notificationList.add(notification);
                        }

                        // Show or hide the empty text based on notification count
                        updateEmptyView();

                        adapter.notifyDataSetChanged();

                    } else {
                        // If value is null, show empty view
                        updateEmptyView();
                    }
                });
    }

    private void markNotificationsAsVisuallyRead() {
        notificationBadgeManager.updateBadgeCount(notificationsBadgeLayout, 0);
    }

    private void markNotificationsAsReadInDatabase() {
        Log.d(TAG, "Marking all notifications as read in database");

        for (Notification notification : notificationList) {
            if (!notification.isRead()) {
                db.collection("Notifications")
                        .document(notification.getId())
                        .update("read", true)
                        .addOnSuccessListener(aVoid ->
                                Log.d(TAG, "Marked notification " + notification.getId() + " as read"))
                        .addOnFailureListener(e ->
                                Log.e(TAG, "Error marking notification as read", e));
            }
        }

        // Update badge in other activities
        notificationBadgeManager.resetUnreadCount(userId);
    }

    private void updateEmptyView() {
        if (notificationList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyNotificationsText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyNotificationsText.setVisibility(View.GONE);
        }
    }

    private void markNotificationsAsRead(List<Notification> notifications) {
        Log.d(TAG, "Marking " + notifications.size() + " notifications as read");

        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                db.collection("Notifications")
                        .document(notification.getId())
                        .update("read", true)
                        .addOnSuccessListener(aVoid ->
                                Log.d(TAG, "Marked notification " + notification.getId() + " as read"))
                        .addOnFailureListener(e ->
                                Log.e(TAG, "Error marking notification as read", e));

                // Update the local notification object as well
                notification.setRead(true);
            }
        }

        // Also explicitly call resetUnreadCount to ensure badge in other activities gets updated
        notificationBadgeManager.resetUnreadCount(userId);
    }

    private void checkForVerificationDeepLink() {
        // Check if we have a specific notification to display from a deep link
        String notificationId = getIntent().getStringExtra("notificationId");
        String notificationType = getIntent().getStringExtra("notificationType");

        if (notificationId != null) {
            db.collection("Notifications")
                    .document(notificationId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Notification notification = documentSnapshot.toObject(Notification.class);
                            if (notification != null) {
                                if ("verification_status".equals(notification.getType()) || "user_verification".equals(notification.getType())) {
                                    // Scroll to this notification in the list
                                    scrollToNotificationType("verification");
                                } else if ("feedback_request".equals(notification.getType())) {
                                    // For feedback notifications, open the Feedback activity
                                    openFeedbackActivity(notification);
                                }
                            }
                        }
                    });
        }
        // If notificationType is specified
        else if (notificationType != null) {
            if ("verification_status".equals(notificationType)) {
                // Scroll to the first verification notification
                scrollToNotificationType("verification");
            } else if ("feedback_request".equals(notificationType)) {
                // Scroll to the first feedback notification
                scrollToNotificationType("feedback_request");
            }
        }
    }

    private void scrollToNotificationType(String type) {
        for (int i = 0; i < notificationList.size(); i++) {
            Notification notification = notificationList.get(i);
            String notificationType = notification.getType();

            if ((type.equals("verification") &&
                    (notificationType != null && (notificationType.equals("verification_status") ||
                            notificationType.equals("user_verification")))) ||
                    (type.equals(notificationType))) {
                recyclerView.scrollToPosition(i);
                break;
            }
        }
    }

    private void openFeedbackActivity(Notification notification) {
        // Launch the Feedback activity
        Intent intent = new Intent(this, Feedback.class);
        intent.putExtra("userId", notification.getUserId());
        intent.putExtra("eventId", notification.getEventId());
        intent.putExtra("eventName", notification.getEventName());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Only reset the badge count (without marking as read in the database)
        markNotificationsAsVisuallyRead();

        // Refresh verification status
        fetchVerificationStatus();

        // Check for empty notifications
        updateEmptyView();
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

    @Override
    protected void onStop() {
        super.onStop();

        // Mark all notifications as read when the user leaves the activity
        markNotificationsAsReadInDatabase();
    }
}