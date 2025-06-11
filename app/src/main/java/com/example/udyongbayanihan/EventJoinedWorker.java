package com.example.udyongbayanihan;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.FirebaseFirestore;

public class EventJoinedWorker extends Worker {
    private static final String TAG = "EventJoinedWorker";
    private static final String CHANNEL_ID = "upcoming_events";
    private final FirebaseFirestore db;
    private String userId, uaddressId, unameId, uotherDetails;

    public EventJoinedWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        db = FirebaseFirestore.getInstance();

        // Get user info from SharedPreferences as backup
        SharedPreferences prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        uaddressId = prefs.getString("uaddressId", null);
        unameId = prefs.getString("unameId", null);
        uotherDetails = prefs.getString("uotherDetails", null);

        // Try to get from input data first
        String inputUserId = getInputData().getString("userId");
        if (inputUserId != null && !inputUserId.isEmpty()) {
            userId = inputUserId;
            uaddressId = getInputData().getString("uaddressId");
            unameId = getInputData().getString("unameId");
            uotherDetails = getInputData().getString("uotherDetails");
        }

        createNotificationChannel();
    }

    @NonNull
    @Override
    public Result doWork() {
        if (userId == null || userId.isEmpty()) {
            Log.d(TAG, "No user ID provided for event join notification");
            return Result.failure();
        }

        String eventId = getInputData().getString("eventId");
        String eventName = getInputData().getString("eventName");
        String barangay = getInputData().getString("barangay");

        if (eventId == null || eventName == null || barangay == null) {
            Log.e(TAG, "Missing required event data");
            return Result.failure();
        }

        try {
            // Check if the notification has already been shown in the last minute
            // This prevents duplicate notifications from both direct call and worker
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(
                    "NotificationPrefs", Context.MODE_PRIVATE);
            long lastShownTime = prefs.getLong("last_join_notification_" + eventId + "_" + userId, 0);
            long currentTime = System.currentTimeMillis();

            // If notification was shown in the last minute, skip showing it again
            if (currentTime - lastShownTime > 60000) { // 60 seconds
                // Show system notification
                showSystemNotification(eventName, barangay, "event_confirmation");

                // Log that notification was shown
                Log.d(TAG, "System notification displayed for event: " + eventName);

                // Save timestamp of notification
                prefs.edit().putLong("last_join_notification_" + eventId + "_" + userId, currentTime).apply();
            } else {
                Log.d(TAG, "Skipping duplicate notification for event: " + eventName + " (shown " +
                        ((currentTime - lastShownTime) / 1000) + " seconds ago)");
            }

            // AFTER showing notification, update Firestore (don't wait for completion)
            String notificationId = userId + "_" + eventId + "_event_confirmation";
            Notification notification = new Notification(userId, eventId, eventName, barangay, "event_confirmation");

            db.collection("Notifications")
                    .document(notificationId)
                    .set(notification)
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "Created Firestore notification: " + notificationId))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error creating Firestore notification", e));

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error in event joined worker", e);
            return Result.retry();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);

            // Event notifications channel
            NotificationChannel eventChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Upcoming Events",
                    NotificationManager.IMPORTANCE_HIGH
            );
            eventChannel.setDescription("Notifications for upcoming volunteer events");
            eventChannel.enableLights(true);
            eventChannel.enableVibration(true);
            eventChannel.setVibrationPattern(new long[]{0, 250, 250, 250});
            eventChannel.setShowBadge(true);
            eventChannel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            // Force notifications to appear when app is in foreground
            eventChannel.setBypassDnd(true);

            manager.createNotificationChannel(eventChannel);
        }
    }

    private void showSystemNotification(String eventName, String barangay, String type) {
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            String title = "Event Joined: " + eventName;
            String content = "You have successfully joined the event in Barangay " + barangay;

            // Create an explicit intent for the Notifications activity
            Intent notificationIntent = new Intent(getApplicationContext(), Notifications.class);
            notificationIntent.setAction("NOTIFICATION_" + type + "_" + System.currentTimeMillis());
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // Add user data to the intent
            notificationIntent.putExtra("userId", userId);
            notificationIntent.putExtra("uaddressId", uaddressId);
            notificationIntent.putExtra("unameId", unameId);
            notificationIntent.putExtra("uotherDetails", uotherDetails);

            int requestCode = (eventName + type + System.currentTimeMillis()).hashCode();

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    getApplicationContext(),
                    requestCode,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Create a full screen intent to ensure notification shows in foreground
            PendingIntent fullScreenIntent = PendingIntent.getActivity(
                    getApplicationContext(),
                    requestCode + 1, // Different request code
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.udyongbayanihan_logo)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_MAX) // Maximum priority
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setFullScreenIntent(fullScreenIntent, true) // Force heads-up notification
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            // Use a unique notification ID
            int notificationId = (eventName + type).hashCode();
            notificationManager.notify(notificationId, builder.build());
        }
    }
}