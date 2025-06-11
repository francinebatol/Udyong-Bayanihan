package com.example.udyongbayanihan;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Worker class that periodically checks for events with postFeedback=true
 * whose notifications haven't been sent yet
 */
public class FeedbackNotificationWorker extends Worker {
    private static final String TAG = "FeedbackNotifWorker";
    private static final String FEEDBACK_CHECK_WORK = "feedback_notification_work";
    private final Context context;
    private final FirebaseFirestore db;

    public FeedbackNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting feedback notification check - this is now a backup method only");

        // First, get all admin IDs from AdminMobileAccount collection
        Set<String> allAdminIds = new HashSet<>();

        try {
            // Query for all admin accounts
            db.collection("AdminMobileAccount")
                    .get()
                    .addOnSuccessListener(adminDocs -> {
                        for (DocumentSnapshot adminDoc : adminDocs) {
                            String adminId = adminDoc.getString("amAccountId");
                            if (adminId != null && !adminId.isEmpty()) {
                                allAdminIds.add(adminId);
                            }
                        }

                        Log.d(TAG, "Found " + allAdminIds.size() + " admin IDs to exclude from notifications");

                        // Now proceed with the event processing with our list of admins
                        processEventsForFeedback(allAdminIds);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to fetch admin accounts: " + e.getMessage());
                        // Proceed with empty admin list as fallback
                        processEventsForFeedback(allAdminIds);
                    });

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Exception in feedback notification check", e);
            return Result.retry();
        }
    }

    private void processEventsForFeedback(Set<String> adminIds) {
        // Get the current device user ID
        SharedPreferences prefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        String currentDeviceUserId = prefs.getString("current_device_user_id", null);

        if (currentDeviceUserId == null) {
            Log.e(TAG, "Cannot determine current device user ID, aborting feedback notification check");
            return;
        }

        Log.d(TAG, "Current device user ID: " + currentDeviceUserId);

        // Use the admin IDs to filter notifications
        db.collection("EventInformation")
                .whereEqualTo("postFeedback", true)
                .whereEqualTo("feedbackNotificationsSent", false)
                .get()
                .addOnSuccessListener(eventInfoDocs -> {
                    Log.d(TAG, "Found " + eventInfoDocs.size() + " events with postFeedback=true needing notifications");

                    for (QueryDocumentSnapshot eventInfoDoc : eventInfoDocs) {
                        String eventId = eventInfoDoc.getId();

                        // Get the event details to get the event name
                        db.collection("EventDetails")
                                .whereEqualTo("eventId", eventId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(eventDetailsDocs -> {
                                    if (!eventDetailsDocs.isEmpty()) {
                                        DocumentSnapshot detailsDoc = eventDetailsDocs.getDocuments().get(0);
                                        String eventName = detailsDoc.getString("nameOfEvent");
                                        String barangay = detailsDoc.getString("barangay");

                                        if (eventName != null) {
                                            // Find users who joined this event
                                            db.collection("UserJoinEvents")
                                                    .whereEqualTo("eventName", eventName)
                                                    .get()
                                                    .addOnSuccessListener(userJoinDocs -> {
                                                        Log.d(TAG, "Found " + userJoinDocs.size() + " users who joined event: " + eventName);

                                                        List<String> processedUsers = new ArrayList<>();
                                                        int adminsSkipped = 0;
                                                        boolean foundCurrentUser = false;

                                                        for (QueryDocumentSnapshot userJoinDoc : userJoinDocs) {
                                                            String userId = userJoinDoc.getString("userId");

                                                            // Skip invalid users
                                                            if (userId == null || userId.isEmpty()) {
                                                                continue;
                                                            }

                                                            // Skip admins based on our complete list
                                                            if (adminIds.contains(userId)) {
                                                                Log.d(TAG, "⚠️ Skipping admin user: " + userId);
                                                                adminsSkipped++;
                                                                continue;
                                                            }

                                                            // Skip already processed users
                                                            if (processedUsers.contains(userId)) {
                                                                Log.d(TAG, "Skipping duplicate user: " + userId);
                                                                continue;
                                                            }

                                                            processedUsers.add(userId);

                                                            // Check if this is the current device user
                                                            if (userId.equals(currentDeviceUserId)) {
                                                                foundCurrentUser = true;
                                                                Log.d(TAG, "Found current device user among event participants: " + userId);
                                                            }

                                                            // Store notification in Firestore for all relevant users
                                                            String notificationId = "feedback_" + eventId + "_" + userId;

                                                            // Check if notification already exists
                                                            db.collection("Notifications")
                                                                    .document(notificationId)
                                                                    .get()
                                                                    .addOnSuccessListener(notificationDoc -> {
                                                                        if (notificationDoc.exists()) {
                                                                            Log.d(TAG, "Notification already exists for user: " + userId);
                                                                        } else {
                                                                            // Do one final check to confirm this is not an admin
                                                                            db.collection("AdminMobileAccount")
                                                                                    .whereEqualTo("amAccountId", userId)
                                                                                    .get()
                                                                                    .addOnSuccessListener(adminCheckDocs -> {
                                                                                        if (adminCheckDocs.isEmpty()) {
                                                                                            // Create the Firestore notification entry
                                                                                            createFirestoreNotification(userId, eventId, eventName, barangay, notificationId);

                                                                                            // Only send system notification if this is the current device user
                                                                                            if (userId.equals(currentDeviceUserId)) {
                                                                                                sendFeedbackNotification(userId, eventId, eventName, barangay);
                                                                                                Log.d(TAG, "✓ Creating feedback notification for CURRENT DEVICE USER: " + userId);
                                                                                            } else {
                                                                                                Log.d(TAG, "Created Firestore notification for user: " + userId +
                                                                                                        " (system notification skipped - not current device user)");
                                                                                            }
                                                                                        } else {
                                                                                            Log.d(TAG, "🛑 Final check prevented notification to admin: " + userId);
                                                                                        }
                                                                                    });
                                                                        }
                                                                    });
                                                        }

                                                        Log.d(TAG, "Summary: " + userJoinDocs.size() + " total participants, " +
                                                                adminsSkipped + " admins skipped, " +
                                                                processedUsers.size() + " notifications processed, " +
                                                                "current device user found: " + foundCurrentUser);

                                                        // Mark this event as having had notifications sent
                                                        Map<String, Object> updates = new HashMap<>();
                                                        updates.put("feedbackNotificationsSent", true);

                                                        db.collection("EventInformation")
                                                                .document(eventId)
                                                                .update(updates);
                                                    });
                                        }
                                    }
                                });
                    }
                });
    }

    private void createFirestoreNotification(String userId, String eventId, String eventName, String barangay, String notificationId) {
        // Create notification in Firestore with explicit ID
        Notification notification = Notification.createFeedbackNotification(userId, eventId, eventName, barangay);
        notification.setRead(false);

        // Use document ID that is the same as notification.getId() to prevent duplicates
        db.collection("Notifications")
                .document(notificationId)
                .set(notification)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Feedback notification created in Firestore: " + notificationId);
                });
    }

    private void sendFeedbackNotification(String userId, String eventId, String eventName, String barangay) {
        // Get current admin ID from SharedPreferences
        SharedPreferences adminPrefs = context.getSharedPreferences("AdminSession", Context.MODE_PRIVATE);
        String currentAdminId = adminPrefs.getString("amAccountId", null);

        // Skip if this is the current admin
        if (currentAdminId != null && userId.equals(currentAdminId)) {
            Log.d(TAG, "🛑 BLOCKED notification to current admin: " + userId);
            return;
        }

        // Double-check if this is an admin user by querying AdminMobileAccount
        db.collection("AdminMobileAccount")
                .whereEqualTo("amAccountId", userId)
                .get()
                .addOnSuccessListener(adminDocs -> {
                    if (!adminDocs.isEmpty()) {
                        Log.d(TAG, "🛑 BLOCKED notification to admin: " + userId);
                        return;
                    }

                    // Finally check usersAccount to ensure this is a legitimate user
                    db.collection("usersAccount")
                            .document(userId)
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                if (userDoc.exists()) {
                                    String uaddressId = userDoc.getString("uaddressId");
                                    String unameId = userDoc.getString("unameId");
                                    String uotherDetails = userDoc.getString("uotherDetails");

                                    // Send system notification
                                    NotificationHelper notificationHelper = new NotificationHelper(context);
                                    notificationHelper.showFeedbackRequestNotification(
                                            userId, eventId, eventName, barangay, uaddressId, unameId, uotherDetails);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error checking user in usersAccount: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking if user is admin: " + e.getMessage());
                });
    }

    /**
     * Schedule periodic feedback notification checks
     * This is now a backup method as the primary notification method is in AcceptedEventsAdapter
     */
    public static void scheduleFeedbackNotificationChecks(Context context) {
        try {
            // Create network constraints
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            // Check every hour instead of every 15 minutes since this is now a backup
            PeriodicWorkRequest feedbackCheckRequest = new PeriodicWorkRequest.Builder(
                    FeedbackNotificationWorker.class,
                    1, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                    .build();

            // Enqueue the work, replacing any existing one
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    FEEDBACK_CHECK_WORK,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    feedbackCheckRequest);

            Log.d(TAG, "Scheduled feedback notification checks (backup method)");
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule feedback notification checks", e);
        }
    }
}