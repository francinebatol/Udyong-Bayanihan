package com.example.udyongbayanihan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class NotificationWorkManager {
    private static final String TAG = "NotificationWorkManager";
    private static final String DAILY_CHECK_WORK = "daily_event_check";
    private static final String EVENT_JOINED_WORK_PREFIX = "event_joined_";
    private static final String MESSAGE_CHECK_WORK_PREFIX = "message_checks_";
    private static final String MESSAGE_CHECK_WORK = "event_check_work";
    private static final String ADMIN_EVENT_STATUS_WORK = "admin_event_status_work";
    private static final String VERIFICATION_STATUS_WORK = "verification_status_work";
    private static final String FEEDBACK_CHECK_WORK = "feedback_check_work";


    // Schedule daily event checks that run every day in the morning
    public static void scheduleDailyEventChecks(Context context) {
        try {
            // Create network constraints
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            // Create a daily work request to run at 8 PM each day
            PeriodicWorkRequest dailyCheckRequest = new PeriodicWorkRequest.Builder(
                    EventNotificationWorker.class, 12, TimeUnit.HOURS) // Check twice a day
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.MINUTES)
                    .build();

            // Enqueue the work, replacing any existing one
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    DAILY_CHECK_WORK,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    dailyCheckRequest);

            Log.d(TAG, "Scheduled daily event check work to run at 8 PM daily");
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule daily event checks", e);
        }
    }

    // Calculate initial delay to run the event check at 8 AM
    private static long calculateInitialDelayForEvents() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY);

        // Target is 8 AM (08:00) instead of 8 PM (20:00)
        if (currentHour >= 8) {
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }

        // Set time to 8 AM
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 8);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);

        long targetTime = calendar.getTimeInMillis();
        long currentTime = System.currentTimeMillis();
        long delay = targetTime - currentTime;

        Log.d(TAG, "Calculated initial delay for event checks: " + delay + "ms");
        Log.d(TAG, "Event checks scheduled to run at: " + calendar.getTime());
        return delay;
    }

    /**
     * Schedule an immediate notification for event joined
     */
    /**
     * Schedule an immediate notification for event joined
     */
    public static void notifyEventJoined(Context context, String userId, String eventId,
                                         String eventName, String barangay,
                                         String uaddressId, String unameId, String uotherDetails) {
        try {
            // IMMEDIATE NOTIFICATION: Show notification directly without waiting for WorkManager
            NotificationHelper notificationHelper = new NotificationHelper(context);
            notificationHelper.showEventJoinedNotification(userId, eventId, eventName, barangay, uaddressId, unameId, uotherDetails);
            Log.d(TAG, "Immediate notification displayed for joined event: " + eventName);

            // Also schedule through WorkManager as a backup
            Data inputData = new Data.Builder()
                    .putString("userId", userId)
                    .putString("uaddressId", uaddressId)
                    .putString("unameId", unameId)
                    .putString("uotherDetails", uotherDetails)
                    .putString("eventId", eventId)
                    .putString("eventName", eventName)
                    .putString("barangay", barangay)
                    .build();

            // Create a one-time work request with IMMEDIATE execution
            OneTimeWorkRequest joinNotificationRequest = new OneTimeWorkRequest.Builder(EventJoinedWorker.class)
                    .setInputData(inputData)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) // Run immediately if possible
                    .build();

            // Unique work name based on user and event
            String uniqueWorkName = EVENT_JOINED_WORK_PREFIX + userId + "_" + eventId;

            // Enqueue the work
            WorkManager.getInstance(context).enqueueUniqueWork(
                    uniqueWorkName,
                    ExistingWorkPolicy.REPLACE,
                    joinNotificationRequest);

            Log.d(TAG, "Scheduled event joined notification for: " + eventName);
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule event joined notification", e);
        }
    }
    /**
     * Calculate initial delay to run the work at 8 AM
     */
    private static long calculateInitialDelay() {
        // Existing implementation unchanged
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY);

        // If it's already past 8 AM, schedule for next day
        if (currentHour >= 8) {
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
        }

        // Set time to 8 AM
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 8);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);

        long targetTime = calendar.getTimeInMillis();
        long currentTime = System.currentTimeMillis();

        return targetTime - currentTime;
    }

    public static void scheduleMessageChecks(Context context, String userId, String userType) {
        if (userId == null || userType == null) {
            Log.e(TAG, "Cannot schedule message checks: userId or userType is null");
            return;
        }

        Log.d(TAG, "Scheduling message checks for user " + userId + " of type " + userType);

        // Create network constraints
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Create input data for the worker
        Data inputData = new Data.Builder()
                .putString("userId", userId)
                .putString("userType", userType)
                .build();

        // Create periodic work request (check every 15 minutes)
        PeriodicWorkRequest messageCheckRequest = new PeriodicWorkRequest.Builder(
                MessageNotificationWorker.class,
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES) // Flex interval for battery optimization
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build();

        // Enqueue unique periodic work
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                MESSAGE_CHECK_WORK + "_" + userId,
                ExistingPeriodicWorkPolicy.UPDATE, // Replace if exists
                messageCheckRequest
        );

        Log.d(TAG, "Message checks scheduled successfully for user: " + userId);

        // Also schedule verification checks for regular users
        if ("user".equals(userType)) {
            scheduleVerificationStatusChecks(context, userId);

            // Schedule feedback notification checks
            scheduleFeedbackNotificationChecks(context);
        }
    }

    // Replace the scheduleVerificationStatusChecks method in NotificationWorkManager.java with this fixed version

    /**
     * Schedule verification status notification checks for a user
     * @param context Application context
     * @param userId ID of the user to monitor verification status for
     */
    public static void scheduleVerificationStatusChecks(Context context, String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot schedule verification checks: userId is null or empty");
            return;
        }

        // Schedule periodic verification checks
        VerificationNotificationWorker.scheduleVerificationChecks(context, userId);

        Log.d(TAG, "Scheduled verification status checks for user: " + userId);
    }

    /**
     * Schedule feedback notification checks
     * @param context Application context
     */
    public static void scheduleFeedbackNotificationChecks(Context context) {
        Log.d(TAG, "Scheduling feedback notification checks");

        // Schedule through the FeedbackNotificationWorker
        FeedbackNotificationWorker.scheduleFeedbackNotificationChecks(context);
    }

    public static void cancelMessageChecks(Context context, String userId) {
        if (userId == null) {
            Log.e(TAG, "Cannot cancel message checks: userId is null");
            return;
        }

        // Cancel the work by unique name
        WorkManager.getInstance(context).cancelUniqueWork(MESSAGE_CHECK_WORK + "_" + userId);
        Log.d(TAG, "Canceled message checks for user: " + userId);
    }

    public static void scheduleAdminEventStatusChecks(Context context, String adminId) {
        if (adminId == null) {
            Log.e(TAG, "Cannot schedule admin event status checks: adminId is null");
            return;
        }

        Log.d(TAG, "Scheduling event status checks for admin: " + adminId);

        // Create network constraints
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Create input data for the worker
        Data inputData = new Data.Builder()
                .putString("adminId", adminId)
                .build();

        // Create periodic work request (check every 15 minutes with 5-minute flex period)
        PeriodicWorkRequest statusCheckRequest = new PeriodicWorkRequest.Builder(
                AdminEventStatusWorker.class,
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES) // Added flex period for battery optimization
                .setConstraints(constraints)
                .setInputData(inputData)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build();

        // Enqueue unique periodic work
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                ADMIN_EVENT_STATUS_WORK + "_" + adminId,
                ExistingPeriodicWorkPolicy.UPDATE,
                statusCheckRequest
        );

        Log.d(TAG, "Event status checks scheduled for admin: " + adminId);
    }

    public static void cancelAdminEventStatusChecks(Context context, String adminId) {
        if (adminId == null) {
            Log.e(TAG, "Cannot cancel admin event status checks: adminId is null");
            return;
        }

        WorkManager.getInstance(context).cancelUniqueWork(ADMIN_EVENT_STATUS_WORK + "_" + adminId);
        Log.d(TAG, "Canceled event status checks for admin: " + adminId);
    }

    /**
     * Create a verification status notification for a user
     * @param context Application context
     * @param userId ID of the user to notify
     * @param status "approved" or "denied"
     * @param message Message to display to the user
     */
    public static void createVerificationStatusNotification(Context context, String userId, String status, String message) {
        try {
            // Create notification document in Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Create notification object
            Notification notification = new Notification(userId, "verification_status", status, message);

            // Add to Firestore
            db.collection("Notifications")
                    .add(notification)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Verification status notification created for user: " + userId);

                        // Also display an Android system notification
                        NotificationHelper notificationHelper = new NotificationHelper(context);
                        String title = "Account Verification";
                        notificationHelper.showVerificationNotification(userId, title, message, status);
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error creating verification notification", e));
        } catch (Exception e) {
            Log.e(TAG, "Failed to create verification status notification", e);
        }
    }

    /**
     * Create a feedback request notification for a user
     * @param context Application context
     * @param userId ID of the user to notify
     * @param eventId ID of the event
     * @param eventName Name of the event
     * @param barangay Barangay where the event was held
     */
    public static void createFeedbackRequestNotification(Context context, String userId, String eventId, String eventName, String barangay, String uaddressId, String unameId, String uotherDetails) {
        try {
            // Get current device user ID from SharedPreferences
            SharedPreferences prefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
            String currentDeviceUserId = prefs.getString("current_device_user_id", null);
            Log.d(TAG, "Current device user ID: " + currentDeviceUserId + ", Target user ID: " + userId);

            // First check if this user is the admin for this event
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("EventInformation")
                    .document(eventId)
                    .get()
                    .addOnSuccessListener(eventDoc -> {
                        if (eventDoc.exists()) {
                            String adminId = eventDoc.getString("amAccountId");

                            // Skip notification if user is the admin
                            if (adminId != null && adminId.equals(userId)) {
                                Log.d(TAG, "Skipping feedback notification for admin: " + userId);
                                return;
                            }

                            // Check if notification already exists to prevent duplicates
                            String notificationId = "feedback_" + eventId + "_" + userId;

                            db.collection("Notifications")
                                    .document(notificationId)
                                    .get()
                                    .addOnSuccessListener(notificationDoc -> {
                                        if (notificationDoc.exists()) {
                                            Log.d(TAG, "Feedback notification already exists for user: " + userId);
                                            return;
                                        }

                                        // Create notification document in Firestore
                                        // Create notification object
                                        Notification notification = Notification.createFeedbackNotification(userId, eventId, eventName, barangay);

                                        // Explicitly set notification as unread
                                        notification.setRead(false);

                                        // Add to Firestore with explicit ID to prevent duplicates
                                        db.collection("Notifications")
                                                .document(notification.getId())
                                                .set(notification)
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d(TAG, "Feedback request notification created for user: " + userId);

                                                    // Only display an Android system notification if this is for the current device user
                                                    if (userId.equals(currentDeviceUserId)) {
                                                        NotificationHelper notificationHelper = new NotificationHelper(context);
                                                        notificationHelper.showFeedbackRequestNotification(userId, eventId, eventName, barangay, uaddressId, unameId, uotherDetails);
                                                        Log.d(TAG, "System notification displayed for CURRENT DEVICE USER: " + userId);
                                                    } else {
                                                        Log.d(TAG, "Firestore notification created for user: " + userId +
                                                                " (system notification skipped - not current device user)");
                                                    }
                                                })
                                                .addOnFailureListener(e ->
                                                        Log.e(TAG, "Error creating feedback notification", e));
                                    });
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error checking admin status", e));
        } catch (Exception e) {
            Log.e(TAG, "Failed to create feedback request notification", e);
        }
    }

    /**
     * Check for verification status changes and create a notification if needed
     * @param context Application context
     * @param userId User ID to check
     */
    public static void checkAndNotifyVerificationStatus(Context context, String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e("NotificationWorkManager", "Cannot check verification status: userId is null or empty");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        NotificationHelper notificationHelper = new NotificationHelper(context);

        // Get the last known status
        SharedPreferences prefs = context.getSharedPreferences(
                "NotificationPrefs", Context.MODE_PRIVATE);
        String lastKnownStatus = prefs.getString("last_verification_status_" + userId, null);

        // Check current status
        db.collection("usersAccount")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String currentStatus = documentSnapshot.getString("usersStatus");

                        // Save the current time as last check time
                        long currentTime = System.currentTimeMillis();
                        prefs.edit().putLong("last_verification_check_" + userId, currentTime).apply();

                        // ONLY notify if status changed (not null and different from last known)
                        if (currentStatus != null && lastKnownStatus != null && !lastKnownStatus.equals(currentStatus)) {
                            // Save the new status
                            prefs.edit().putString("last_verification_status_" + userId, currentStatus).apply();

                            // Skip notification if status is "Unverified"
                            if ("Unverified".equals(currentStatus)) {
                                Log.d("NotificationWorkManager", "Skipping notification for Unverified status");
                                return;
                            }

                            // Determine message
                            String message;
                            if ("Verified".equals(currentStatus)) {
                                message = "Your account has been verified! You can now join events.";
                            } else {
                                message = "Your account verification status has been updated to: " + currentStatus;
                            }

                            // Create unique notification ID to prevent duplicates
                            String notificationId = userId + "_verification_" + System.currentTimeMillis();

                            // First check if a similar notification already exists
                            db.collection("Notifications")
                                    .whereEqualTo("userId", userId)
                                    .whereEqualTo("type", "verification_status")
                                    .whereEqualTo("status", currentStatus)
                                    .get()
                                    .addOnSuccessListener(querySnapshot -> {
                                        if (querySnapshot.isEmpty()) {
                                            // Create notification in Firestore
                                            Notification notification = new Notification(userId, "verification_status", currentStatus, message);

                                            // Add to Firestore with explicit ID to prevent duplicates
                                            db.collection("Notifications")
                                                    .document(notificationId)
                                                    .set(notification)
                                                    .addOnSuccessListener(aVoid -> {
                                                        Log.d(TAG, "Verification status notification created in Firestore");

                                                        // Now show the system notification
                                                        notificationHelper.showVerificationNotification(
                                                                userId,
                                                                "Account Verification Update",
                                                                message,
                                                                currentStatus
                                                        );
                                                    })
                                                    .addOnFailureListener(e ->
                                                            Log.e(TAG, "Error creating verification notification in Firestore", e));
                                        } else {
                                            Log.d(TAG, "Similar verification notification already exists - not creating duplicate");

                                            // Still show the system notification
                                            notificationHelper.showVerificationNotification(
                                                    userId,
                                                    "Account Verification Update",
                                                    message,
                                                    currentStatus
                                            );
                                        }
                                    })
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "Error checking for existing notifications", e));

                            Log.d("NotificationWorkManager", "Sent verification notification for status change: " + currentStatus);
                        } else if (currentStatus != null && lastKnownStatus == null) {
                            // First time we're seeing a status, save it but don't notify
                            prefs.edit().putString("last_verification_status_" + userId, currentStatus).apply();
                            Log.d("NotificationWorkManager", "Initial verification status set to: " + currentStatus);
                        } else {
                            Log.d("NotificationWorkManager", "No change in verification status - skipping notification");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("NotificationWorkManager", "Error checking account status", e);
                });

        // Also check for any unread verification notifications
        db.collection("Notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .whereIn("type", Arrays.asList("verification_status", "user_verification"))
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // We found unread notifications, grab the most recent one
                        DocumentSnapshot latestNotification = null;
                        Timestamp latestTimestamp = null;

                        for (DocumentSnapshot document : task.getResult()) {
                            Notification notification = document.toObject(Notification.class);
                            if (notification != null && notification.getTimestamp() != null) {
                                // Skip notifications with "Unverified" status
                                if ("Unverified".equals(notification.getStatus()) ||
                                        "denied".equals(notification.getStatus())) {
                                    continue;
                                }

                                if (latestTimestamp == null || notification.getTimestamp().compareTo(latestTimestamp) > 0) {
                                    latestNotification = document;
                                    latestTimestamp = notification.getTimestamp();
                                }
                            }
                        }

                        // Process only the most recent unread notification to avoid duplicates
                        if (latestNotification != null) {
                            Notification notification = latestNotification.toObject(Notification.class);
                            if (notification != null) {
                                String status = notification.getStatus();

                                // Skip "Unverified" status notifications
                                if ("Unverified".equals(status) || "denied".equals(status)) {
                                    return;
                                }

                                String message = notification.getMessage();

                                // Default message if none is provided
                                if (message == null || message.isEmpty()) {
                                    if ("approved".equals(status) || "Verified".equals(status)) {
                                        message = "Your account is now verified! You can now join the events posted inside the application.";
                                    } else {
                                        message = "Your account verification status has been updated to: " + status;
                                    }
                                }

                                // Show notification for the latest unread notification
                                notificationHelper.showVerificationNotification(
                                        userId,
                                        "Account Verification",
                                        message,
                                        status
                                );

                                Log.d("NotificationWorkManager", "Processed latest unread verification notification");
                            }
                        }
                    }
                });
    }

    /**
     * Create a skill group request status notification for a user
     * @param context Application context
     * @param userId ID of the user to notify
     * @param skillName Name of the skill group
     * @param requestStatus "APPROVED" or "REJECTED"
     */
    public static void createSkillGroupNotification(Context context, String userId, String skillName, String requestStatus) {
        try {
            // Create notification document in Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Create notification object
            Notification notification = Notification.createSkillGroupNotification(userId, skillName, requestStatus);

            // Add to Firestore
            db.collection("Notifications")
                    .document(notification.getId())
                    .set(notification)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Skill group notification created for user: " + userId);

                        // Also display an Android system notification
                        NotificationHelper notificationHelper = new NotificationHelper(context);
                        notificationHelper.showSkillGroupNotification(userId, skillName, requestStatus);
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error creating skill group notification", e));
        } catch (Exception e) {
            Log.e(TAG, "Failed to create skill group notification", e);
        }
    }
}