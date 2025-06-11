package com.example.udyongbayanihan;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminMainMenu extends AppCompatActivity {
    private static final String TAG = "AdminMainMenu";

    private String amAccountId, amUsername, amEmail;
    private Bundle amDetails;
    private LinearLayout addPendingEvent, seeMyEvents, adminEventCalendar, seeAdminPost, seeEventFeedbacks, pendingGroupRequest;
    private ImageButton notification, seeMessages;
    private ImageButton adminLogout;
    private TextView notificationBadge;
    private SharedPreferences notificationPrefs;
    private MessageBadgeManager badgeManager;
    private ListenerRegistration unreadMessagesListener;
    private RelativeLayout messagesBadgeLayout;
    private FirebaseFirestore db;
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_main_menu);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Retrieve data passed from Login
        Intent intent = getIntent();
        amAccountId = intent.getStringExtra("amAccountId");
        amUsername = intent.getStringExtra("amUsername");
        amEmail = intent.getStringExtra("amEmail");
        amDetails = intent.getBundleExtra("amDetails");

        NotificationWorkManager.scheduleAdminEventStatusChecks(this, amAccountId);

        // Validate the required data
        if (amAccountId == null || amUsername == null || amEmail == null) {
            // Log details to debug the issue
            Log.d(TAG, "amAccountId: " + amAccountId);
            Log.d(TAG, "amUsername: " + amUsername);
            Log.d(TAG, "amEmail: " + amEmail);
        }

        if (amDetails != null) {
            for (String key : amDetails.keySet()) {
                Log.d(TAG, key + ": " + amDetails.get(key));
            }
        } else {
            Log.d(TAG, "amDetails is null or empty");
        }

        // Extract additional details from amDetails bundle
        String firstName = amDetails.getString("amFirstName", "N/A");
        Log.d(TAG, "amFirstName: " + firstName);
        String middleName = amDetails.getString("amMiddleName", "N/A");
        String lastName = amDetails.getString("amLastName", "N/A");
        String phoneNo = amDetails.getString("amPhoneNo", "N/A");
        String position = amDetails.getString("position", "N/A");
        String houseNo = amDetails.getString("amHouseNo", "N/A");
        String street = amDetails.getString("amStreet", "N/A");
        String barangay = amDetails.getString("amBarangay", "N/A");
        String municipality = amDetails.getString("amMunicipality", "N/A");

        // Display a welcome message
        Toast.makeText(this, "Welcome, " + firstName + " " + lastName + "!", Toast.LENGTH_SHORT).show();

        // Initialize all UI elements
        addPendingEvent = findViewById(R.id.addPendingEvent);
        seeMyEvents = findViewById(R.id.seeMyEvents);
        adminEventCalendar = findViewById(R.id.adminEventCalendar);
        seeMessages = findViewById(R.id.seeMessages);
        notification = findViewById(R.id.notification);
        seeEventFeedbacks = findViewById(R.id.seeEventFeedbacks);
        seeAdminPost = findViewById(R.id.seeAdminPost);
        pendingGroupRequest = findViewById(R.id.pendingGroupRequest);
        adminLogout = findViewById(R.id.adminLogout);
        notificationBadge = findViewById(R.id.notificationBadge);

        setupMessageBadge();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Update notification badge using both methods (for reliability)
        updateNotificationBadge();
        // Also directly check Firestore for new notifications
        checkForNewNotificationsDirectly();

        // Set up the real-time notification listener instead of just checking once
        setupNotificationListener();

        // Redirect to other activities and pass necessary details
        addPendingEvent.setOnClickListener(v -> {
            Intent intentToAddEvent = new Intent(AdminMainMenu.this, AddPendingEvent.class);
            passDetailsToIntent(intentToAddEvent);
            startActivity(intentToAddEvent);
        });

        seeMyEvents.setOnClickListener(v -> {
            Intent intentToSeeEvents = new Intent(AdminMainMenu.this, AdminSeeEvents.class);
            passDetailsToIntent(intentToSeeEvents);
            startActivity(intentToSeeEvents);
        });

        adminEventCalendar.setOnClickListener(v -> {
            Intent intentToCalendar = new Intent(AdminMainMenu.this, AdminEventCalendar.class);
            passDetailsToIntent(intentToCalendar);
            startActivity(intentToCalendar);
        });

        seeEventFeedbacks.setOnClickListener(v -> {
            Intent intentToMessages = new Intent(AdminMainMenu.this, AdminFeedbacks.class);
            // Pass admin data using the helper
            UserDataHelper.passUserData(intentToMessages, amAccountId, "admin", amDetails);
            startActivity(intentToMessages);
        });

        notification.setOnClickListener(v -> {
            // Reset the badge count when notifications are viewed
            updateNotificationCountToZero();

            // Schedule notification checks
            NotificationWorkManager.scheduleAdminEventStatusChecks(this, amAccountId);
            Intent intentToNotification = new Intent(AdminMainMenu.this, AdminNotification.class);
            intentToNotification.putExtra("amAccountId", amAccountId);
            startActivity(intentToNotification);
        });

        seeAdminPost.setOnClickListener(v -> {
            Intent intentToMessages = new Intent(AdminMainMenu.this, AdminPost.class);
            // Pass admin data using the helper
            UserDataHelper.passUserData(intentToMessages, amAccountId, "admin", amDetails);
            startActivity(intentToMessages);
        });

        pendingGroupRequest.setOnClickListener(v -> {
            Intent intentToMessages = new Intent(AdminMainMenu.this, AdminUserPendingRequest.class);
            // Pass admin data using the helper
            UserDataHelper.passUserData(intentToMessages, amAccountId, "admin", amDetails);
            startActivity(intentToMessages);
        });

        adminLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(AdminMainMenu.this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Clear admin session
                        SharedPreferences adminPrefs = getSharedPreferences("AdminSession", MODE_PRIVATE);
                        adminPrefs.edit().clear().apply();

                        // Cancel the notification worker before logging out
                        NotificationWorkManager.cancelAdminEventStatusChecks(AdminMainMenu.this, amAccountId);

                        Intent logoutIntent = new Intent(AdminMainMenu.this, Login.class);
                        logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(logoutIntent);
                        finish();

                        SharedPreferences notificationPrefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE);
                        notificationPrefs.edit().remove("current_device_user_id").apply();
                        Log.d(TAG, "Cleared current device user ID on logout");
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        ensureCurrentDeviceUserId();
    }

    /**
     * Ensures that the current device user ID is properly set for notifications
     */
    private void ensureCurrentDeviceUserId() {
        if (amAccountId == null || amAccountId.isEmpty()) {
            Log.e(TAG, "Cannot set current device user ID: amAccountId is null or empty");
            return;
        }

        SharedPreferences notificationPrefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE);
        String currentDeviceUserId = notificationPrefs.getString("current_device_user_id", null);

        // Set or update the current device user ID if needed
        if (currentDeviceUserId == null || !currentDeviceUserId.equals(amAccountId)) {
            notificationPrefs.edit().putString("current_device_user_id", amAccountId).apply();
            Log.d(TAG, "Updated current device user ID (admin): " + amAccountId);
        } else {
            Log.d(TAG, "Current device user ID already set correctly: " + currentDeviceUserId);
        }
    }

    private void setupNotificationListener() {
        if (amAccountId == null) {
            Log.e(TAG, "Cannot set up notification listener: admin ID is null");
            return;
        }

        // Get a reference to the EventInformation collection
        Query query = db.collection("EventInformation")
                .whereEqualTo("amAccountId", amAccountId);

        // Create a real-time listener
        notificationListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Listen failed for admin notifications", e);
                return;
            }

            if (snapshots == null || snapshots.isEmpty()) {
                Log.d(TAG, "No events found for admin");
                return;
            }

            int notificationCount = 0;

            // Process events and count unread notifications
            for (QueryDocumentSnapshot document : snapshots) {
                String eventId = document.getString("eventId");
                String status = document.getString("status");

                if (eventId != null && ("Accepted".equals(status) || "Rejected".equals(status))) {
                    boolean hasBeenViewed = AdminNotificationHelper.hasBeenViewed(this, eventId, status);

                    // If notification hasn't been viewed, increment count
                    if (!hasBeenViewed) {
                        notificationCount++;

                        // Ensure it's marked as notified so we don't show duplicate Android notifications
                        if (!AdminNotificationHelper.hasBeenNotified(this, eventId, status)) {
                            Log.d(TAG, "Auto-marking notification as notified: " + eventId + ", status: " + status);
                            AdminNotificationHelper.markAsNotified(this, eventId, status);

                            // Store notification time
                            long notificationTime = System.currentTimeMillis() / 1000;
                            AdminNotificationHelper.storeNotificationTime(this, eventId, status, notificationTime);
                        }
                    }
                }
            }

            // Update the badge UI on the main thread
            int finalNotificationCount = notificationCount;
            runOnUiThread(() -> {
                Log.d(TAG, "Updating notification badge, count: " + finalNotificationCount);
                updateNotificationBadgeWithCount(finalNotificationCount);

                // Also update the stored count
                SharedPreferences prefs = getSharedPreferences("AdminNotifications", MODE_PRIVATE);
                prefs.edit().putInt("unread_notification_count", finalNotificationCount).apply();
            });
        });
    }

    private void setupMessageBadge() {
        // Initialize the badge manager
        badgeManager = MessageBadgeManager.getInstance();

        // Set up the badge layout
        messagesBadgeLayout = badgeManager.setupBadgeView(seeMessages, this);

        // Start listening for unread messages with userType "admin"
        unreadMessagesListener = badgeManager.startListeningForUnreadMessages(
                amAccountId, "admin", count -> {
                    runOnUiThread(() -> {
                        badgeManager.updateBadgeCount(messagesBadgeLayout, count);
                    });
                });

        // Set click listener on the button inside the badge layout
        ImageButton baseMessageButton = messagesBadgeLayout.findViewById(R.id.imgbtnBase);
        baseMessageButton.setOnClickListener(v -> {
            Intent intentToMessages = new Intent(AdminMainMenu.this, Messages.class);
            // Pass admin data using the helper
            UserDataHelper.passUserData(intentToMessages, amAccountId, "admin", amDetails);
            startActivity(intentToMessages);
        });
    }

    // Helper method to pass details to intents
    private void passDetailsToIntent(Intent intent) {
        intent.putExtra("amAccountId", amAccountId);
        intent.putExtra("amUsername", amUsername);
        intent.putExtra("amEmail", amEmail);
        intent.putExtra("amDetails", amDetails);
    }

    private void updateNotificationBadge() {
        int count = AdminNotificationHelper.getUnreadCount(this);
        updateNotificationBadgeWithCount(count);
    }

    private void updateNotificationBadgeWithCount(int count) {
        if (count > 0) {
            notificationBadge.setVisibility(View.VISIBLE);
            notificationBadge.setText(String.valueOf(count));
        } else {
            notificationBadge.setVisibility(View.GONE);
        }
    }

    /**
     * Check Firestore directly for new notifications
     * This is kept for backward compatibility but is less important now
     * that we have a real-time listener
     */
    private void checkForNewNotificationsDirectly() {
        AdminNotificationHelper.checkForNewNotifications(
                this,
                amAccountId,
                new AdminNotificationHelper.NotificationCountCallback() {
                    @Override
                    public void onCountReceived(int count) {
                        runOnUiThread(() -> {
                            // Only update if the count is greater than what we have locally
                            int currentCount = AdminNotificationHelper.getUnreadCount(AdminMainMenu.this);
                            if (count > currentCount) {
                                Log.d(TAG, "Direct check found more notifications: " + count);
                                updateNotificationBadgeWithCount(count);

                                // Update the local count to match
                                SharedPreferences prefs = getSharedPreferences(
                                        "AdminNotifications", MODE_PRIVATE);
                                prefs.edit().putInt("unread_notification_count", count).apply();
                            }
                        });
                    }
                }
        );
    }

    private void updateNotificationCountToZero() {
        AdminNotificationHelper.resetUnreadCount(this);
        updateNotificationBadge();
    }

    @Override
    protected void onResume() {
        ensureCurrentDeviceUserId();
        super.onResume();

        // Check if it need to restart the notification listener
        if (notificationListener == null) {
            setupNotificationListener();
        }

        // Refresh the badge when returning to this activity
        updateNotificationBadge();
        checkForNewNotificationsDirectly();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up resources
        if (unreadMessagesListener != null) {
            unreadMessagesListener.remove();
            unreadMessagesListener = null;
        }

        // Clean up notification listener
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }
}