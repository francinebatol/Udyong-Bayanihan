package com.example.udyongbayanihan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.WorkManager;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class VerificationNotificationWorker extends Worker {
    private static final String TAG = "VerificationWorker";
    private static final String VERIFICATION_WORK_NAME = "verification_status_check";
    private FirebaseFirestore db;
    private NotificationHelper notificationHelper;
    private String userId;

    public VerificationNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        db = FirebaseFirestore.getInstance();
        notificationHelper = new NotificationHelper(context);

        // Get user ID from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userId = prefs.getString("userId", null);

        // Also get from input data if available
        String inputUserId = getInputData().getString("userId");
        if (inputUserId != null && !inputUserId.isEmpty()) {
            userId = inputUserId;
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        if (userId == null || userId.isEmpty()) {
            Log.d(TAG, "No user logged in, stopping worker");
            return Result.failure();
        }

        Log.d(TAG, "Checking for verification status notifications for user: " + userId);

        try {
            // Use CountDownLatch to wait for Firebase operations to complete
            CountDownLatch latch = new CountDownLatch(2); // Wait for both checks to complete
            AtomicBoolean workSuccess = new AtomicBoolean(true);

            // Get the last check timestamp from SharedPreferences
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                    "NotificationPrefs", Context.MODE_PRIVATE);
            long lastCheckTime = prefs.getLong("last_verification_check_" + userId, 0);

            // Save current time as new last check time right away to prevent duplicate notifications
            // if the worker runs multiple times in quick succession
            long currentTime = System.currentTimeMillis();
            prefs.edit().putLong("last_verification_check_" + userId, currentTime).apply();

            Timestamp lastCheckTimestamp = new Timestamp(lastCheckTime / 1000, (int) ((lastCheckTime % 1000) * 1000000));

            // First check for account status changes directly in the usersAccount collection
            checkAccountStatusChanges(userId, lastCheckTimestamp, latch, workSuccess);

            // Check for verification notifications in the Notifications collection
            checkVerificationNotifications(userId, lastCheckTimestamp, latch, workSuccess);

            // Wait for Firebase operations to complete (with timeout)
            boolean completed = latch.await(30, TimeUnit.SECONDS);

            if (!completed) {
                Log.e(TAG, "Timeout waiting for Firebase operations to complete");
                return Result.retry();
            }

            return workSuccess.get() ? Result.success() : Result.retry();
        } catch (Exception e) {
            Log.e(TAG, "Error in verification worker", e);
            return Result.retry();
        }
    }

    private void checkAccountStatusChanges(String userId, Timestamp lastCheckTimestamp,
                                           CountDownLatch latch, AtomicBoolean workSuccess) {
        // Get the last known status
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                "NotificationPrefs", Context.MODE_PRIVATE);
        String lastKnownStatus = prefs.getString("last_verification_status_" + userId, null);

        // Check current status
        db.collection("usersAccount")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String currentStatus = documentSnapshot.getString("usersStatus");

                        Log.d(TAG, "Current verification status: " + currentStatus +
                                ", Last known status: " + lastKnownStatus);

                        // If last known status is null (first check) or different from current, create notification
                        if (currentStatus != null) {
                            boolean isFirstCheck = (lastKnownStatus == null);
                            boolean hasStatusChanged = !isFirstCheck && !lastKnownStatus.equals(currentStatus);

                            // Save the new status regardless
                            prefs.edit().putString("last_verification_status_" + userId, currentStatus).apply();

                            // ONLY show notification if the status has actually changed
                            if (hasStatusChanged) {
                                // Create notification in Firestore and show system notification
                                createVerificationNotification(userId, currentStatus, lastKnownStatus);
                                Log.d(TAG, "Status changed from " + lastKnownStatus + " to " + currentStatus +
                                        " - showing notification");
                            } else {
                                Log.d(TAG, "No change in verification status detected - no notification needed");
                            }
                        }
                    }
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking account status", e);
                    workSuccess.set(false);
                    latch.countDown();
                });
    }

    private void createVerificationNotification(String userId, String newStatus, String oldStatus) {
        // Skip creating notification if the status is "Unverified"
        if ("Unverified".equals(newStatus)) {
            Log.d(TAG, "Skipping notification for Unverified status");

            // Still update the saved status though
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                    "NotificationPrefs", Context.MODE_PRIVATE);
            prefs.edit().putString("last_verification_status_" + userId, newStatus).apply();
            return;
        }

        // Get appropriate message
        String message;
        if ("Verified".equals(newStatus)) {
            message = "Your account has been successfully verified! You can now join the volunteer events";
        } else {
            message = "Your account verification status has been updated to: " + newStatus;
        }

        // Generate a unique ID for the notification to prevent duplicates
        String notificationId = userId + "_verification_" + Timestamp.now().getSeconds();

        // First check for duplicate notifications
        db.collection("Notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "user_verification")
                .whereEqualTo("status", newStatus)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Only create if no similar notification exists
                    if (querySnapshot.isEmpty()) {
                        // Create a notification in Firestore
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("userId", userId);
                        notification.put("type", "user_verification");
                        notification.put("status", newStatus);
                        notification.put("reason", message);
                        notification.put("timestamp", Timestamp.now());
                        notification.put("read", false);

                        // Add to Firestore with the unique ID
                        db.collection("Notifications")
                                .document(notificationId)
                                .set(notification)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Verification notification added to Firestore: " + notificationId);

                                    // Mark that we've created a verification notification for this user
                                    SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                                            "NotificationPrefs", Context.MODE_PRIVATE);
                                    prefs.edit().putBoolean("verification_notification_created_" + userId, true).apply();

                                    // Show the notification regardless of app state
                                    notificationHelper.showVerificationNotification(
                                            userId,
                                            "Account Verification",
                                            message,
                                            newStatus
                                    );
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error adding verification notification to Firestore", e);
                                });
                    } else {
                        Log.d(TAG, "Similar verification notification already exists - not creating duplicate");

                        // Still show the system notification even if we don't create a new Firestore entry
                        notificationHelper.showVerificationNotification(
                                userId,
                                "Account Verification",
                                message,
                                newStatus
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking for existing notifications", e);
                });

        // Log the status change
        Log.d(TAG, "User status changed from " + oldStatus + " to " + newStatus);
    }

    private void checkVerificationNotifications(String userId, Timestamp lastCheckTimestamp,
                                                CountDownLatch latch, AtomicBoolean workSuccess) {
        // Query for unread verification notifications created after the last check
        db.collection("Notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)  // Only check unread notifications
                .whereGreaterThan("timestamp", lastCheckTimestamp) // Only notifications after last check
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshots = task.getResult();
                        if (snapshots != null && !snapshots.isEmpty()) {
                            // Process notifications
                            for (DocumentSnapshot document : snapshots) {
                                Notification notification = document.toObject(Notification.class);

                                // Check if this is a verification notification
                                if (notification != null &&
                                        (notification.getType() != null && (
                                                notification.getType().equals("verification_status") ||
                                                        notification.getType().equals("user_verification")))) {

                                    // Mark that we've found a verification notification for this user
                                    SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                                            "NotificationPrefs", Context.MODE_PRIVATE);
                                    prefs.edit().putBoolean("verification_notification_created_" + userId, true).apply();

                                    // Process new notifications
                                    Log.d(TAG, "Found new verification notification created after last check");
                                    processVerificationNotification(notification);
                                }
                            }
                        }
                        workSuccess.set(true);
                    } else {
                        Log.e(TAG, "Error checking verification notifications", task.getException());
                        workSuccess.set(false);
                    }
                    latch.countDown();
                });
    }

    private void processVerificationNotification(Notification notification) {
        if (notification != null &&
                ("verification_status".equals(notification.getType()) || "user_verification".equals(notification.getType()))) {

            String status = notification.getStatus();

            // Show notifications for any status
            String title = "Account Verification";

            // Determine message (prioritize message, then reason, then fallback to default)
            String message = null;
            if (notification.getMessage() != null && !notification.getMessage().isEmpty()) {
                message = notification.getMessage();
            } else if (notification.getReason() != null && !notification.getReason().isEmpty()) {
                message = notification.getReason();
            }

            // Default message if none provided
            if (message == null || message.isEmpty()) {
                if ("approved".equals(status) || "Verified".equals(status)) {
                    message = "Your account is now verified! You can now join the events posted inside the application.";
                } else if ("denied".equals(status) || "Unverified".equals(status)) {
                    message = "Your account verification was denied. Please contact support for more information.";
                } else {
                    message = "Your account verification status has been updated to: " + status;
                }
            }

            // Show the system notification regardless of app state
            notificationHelper.showVerificationNotification(userId, title, message, status);

            // Log the notification
            Log.d(TAG, "Verification notification processed: " + status + " - " + message);
        }
    }

    /**
     * Schedule periodic verification status checks
     */
    public static void scheduleVerificationChecks(Context context, String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot schedule verification checks: userId is null or empty");
            return;
        }

        // Define network constraints - require network connection
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Create work data with userId
        androidx.work.Data inputData = new androidx.work.Data.Builder()
                .putString("userId", userId)
                .build();

        // Create a periodic work request that runs every 15 minutes
        PeriodicWorkRequest verificationCheckRequest = new PeriodicWorkRequest.Builder(
                VerificationNotificationWorker.class,
                15, TimeUnit.MINUTES,  // Run every 15 minutes
                5, TimeUnit.MINUTES)  // Flex period for battery optimization
                .setConstraints(constraints)
                .setInputData(inputData)
                .build();

        // Schedule unique periodic work
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                VERIFICATION_WORK_NAME + "_" + userId,
                ExistingPeriodicWorkPolicy.UPDATE, // Replace if exists
                verificationCheckRequest
        );

        Log.d(TAG, "Verification status checks scheduled for user: " + userId + " (runs every 15 minutes)");

        // Also run once immediately
        runOneTimeCheck(context, userId);
    }

    /**
     * Run a one-time immediate verification check
     */
    public static void runOneTimeCheck(Context context, String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot run verification check: userId is null or empty");
            return;
        }

        // Define network constraints
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Create work data with userId
        androidx.work.Data inputData = new androidx.work.Data.Builder()
                .putString("userId", userId)
                .build();

        // Create a one-time work request with high priority
        androidx.work.OneTimeWorkRequest immediateCheckRequest =
                new androidx.work.OneTimeWorkRequest.Builder(VerificationNotificationWorker.class)
                        .setConstraints(constraints)
                        .setInputData(inputData)
                        .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build();

        // Enqueue the one-time work
        WorkManager.getInstance(context).enqueue(immediateCheckRequest);

        Log.d(TAG, "Immediate verification check scheduled for user: " + userId);
    }

    /**
     * Cancel scheduled verification checks
     */
    public static void cancelVerificationChecks(Context context, String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot cancel verification checks: userId is null or empty");
            return;
        }

        WorkManager.getInstance(context).cancelUniqueWork(VERIFICATION_WORK_NAME + "_" + userId);
        Log.d(TAG, "Verification status checks canceled for user: " + userId);
    }
}