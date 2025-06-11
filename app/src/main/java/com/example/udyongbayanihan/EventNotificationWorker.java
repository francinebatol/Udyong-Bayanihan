package com.example.udyongbayanihan;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventNotificationWorker extends Worker {
    private static final String TAG = "EventNotificationWorker";
    private static final String CHANNEL_ID = "upcoming_events";
    private final FirebaseFirestore db;
    private String userId, uaddressId, unameId, uotherDetails;

    public EventNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        db = FirebaseFirestore.getInstance();

        // Get user info from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        uaddressId = prefs.getString("uaddressId", null);
        unameId = prefs.getString("unameId", null);
        uotherDetails = prefs.getString("uotherDetails", null);

        createNotificationChannel();
    }

    @NonNull
    @Override
    public Result doWork() {
        if (userId == null || userId.isEmpty()) {
            Log.d(TAG, "No user logged in, stopping worker");
            return Result.failure();
        }

        Log.d(TAG, "Starting notification check for userId: " + userId);

        try {
            // Use CountDownLatch to wait for Firebase operations to complete
            CountDownLatch latch = new CountDownLatch(1);
            AtomicBoolean workSuccess = new AtomicBoolean(true);

            checkUpcomingEvents(() -> {
                workSuccess.set(true);
                latch.countDown();
            }, e -> {
                Log.e(TAG, "Error checking upcoming events", e);
                workSuccess.set(false);
                latch.countDown();
            });

            // Wait for Firebase operations to complete (with timeout)
            latch.await(30, TimeUnit.SECONDS);

            return workSuccess.get() ? Result.success() : Result.retry();
        } catch (Exception e) {
            Log.e(TAG, "Error in doWork", e);
            return Result.retry();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);

            // Event notifications channel with enhanced settings
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

            // Enhanced settings for better visibility
            eventChannel.setBypassDnd(true);

            // Set custom sound
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            eventChannel.setSound(soundUri, audioAttributes);

            // Prevent notification throttling on Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                eventChannel.setAllowBubbles(true);
            }

            manager.createNotificationChannel(eventChannel);
            Log.d(TAG, "Created enhanced notification channel for event reminders");
        }
    }

    private void checkUpcomingEvents(Runnable onSuccess, OnFailureListener onFailure) {
        // Get tomorrow's date in the device's timezone
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        Date tomorrowStart = calendar.getTime();

        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date tomorrowEnd = calendar.getTime();

        Log.d(TAG, "Checking for events scheduled for tomorrow between " + tomorrowStart + " and " + tomorrowEnd);

        // Query UserJoinEvents
        db.collection("UserJoinEvents")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(joinSnapshots -> {
                    if (joinSnapshots.isEmpty()) {
                        Log.d(TAG, "No joined events found for user: " + userId);
                        onSuccess.run();
                        return;
                    }

                    final int[] pendingChecks = {joinSnapshots.size()};

                    // Helper method for decrementing counter
                    final Runnable decrementCounter = () -> {
                        pendingChecks[0]--;
                        if (pendingChecks[0] <= 0) {
                            Log.d(TAG, "All event checks complete");
                            onSuccess.run();
                        }
                    };

                    for (DocumentSnapshot joinDoc : joinSnapshots) {
                        String eventId = joinDoc.getString("eventId");

                        if (eventId != null) {
                            db.collection("EventDetails")
                                    .document(eventId)
                                    .get()
                                    .addOnSuccessListener(eventDoc -> {
                                        if (eventDoc.exists()) {
                                            Timestamp eventTimestamp = eventDoc.getTimestamp("date");
                                            String eventName = eventDoc.getString("nameOfEvent");
                                            String barangay = eventDoc.getString("barangay");

                                            if (eventTimestamp != null && eventName != null && barangay != null) {
                                                Date eventDate = eventTimestamp.toDate();

                                                // Check if event is scheduled for tomorrow
                                                boolean isTomorrow = isDateTomorrow(eventDate, tomorrowStart, tomorrowEnd);

                                                if (isTomorrow) {
                                                    Log.d(TAG, "Found event scheduled for tomorrow: " + eventName);
                                                    // Generate unique notification ID
                                                    String upcomingId = userId + "_" + eventId + "_upcoming_event";

                                                    // Check if we've already sent this notification
                                                    db.collection("Notifications")
                                                            .document(upcomingId)
                                                            .get()
                                                            .addOnSuccessListener(notificationDoc -> {
                                                                if (!notificationDoc.exists()) {
                                                                    // Create a new notification if it doesn't exist
                                                                    createUpcomingEventNotification(eventId, eventName, barangay);
                                                                } else {
                                                                    Log.d(TAG, "Notification already exists for upcoming event: " + eventName);
                                                                }

                                                                decrementCounter.run();
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Log.e(TAG, "Error checking notification existence", e);
                                                                decrementCounter.run();
                                                            });
                                                } else {
                                                    Log.d(TAG, "Event not scheduled for tomorrow: " + eventName + ", date: " + eventDate);
                                                    decrementCounter.run();
                                                }
                                            } else {
                                                Log.d(TAG, "Event missing required fields");
                                                decrementCounter.run();
                                            }
                                        } else {
                                            Log.d(TAG, "Event document doesn't exist for ID: " + eventId);
                                            decrementCounter.run();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error retrieving event details", e);
                                        onFailure.accept(e);
                                    });
                        } else {
                            Log.d(TAG, "Event ID is null in UserJoinEvents document");
                            decrementCounter.run();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error querying UserJoinEvents", e);
                    onFailure.accept(e);
                });
    }

    private boolean isDateTomorrow(Date eventDate, Date tomorrowStart, Date tomorrowEnd) {
        // Use calendar objects instead for more reliable comparison
        Calendar eventCal = Calendar.getInstance();
        eventCal.setTime(eventDate);

        Calendar tomorrowCal = Calendar.getInstance();
        tomorrowCal.add(Calendar.DAY_OF_YEAR, 1);

        return eventCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                eventCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR);
    }

    private void createUpcomingEventNotification(String eventId, String eventName, String barangay) {
        String notificationId = userId + "_" + eventId + "_upcoming_event";

        // Create notification object with current timestamp
        Notification notification = new Notification(
                userId,
                eventId,
                eventName,
                barangay,
                "upcoming_event"
        );

        // Save notification with specific ID
        db.collection("Notifications")
                .document(notificationId)
                .set(notification)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Created upcoming notification: " + notificationId);
                    // Send system notification
                    showSystemNotification(eventId, eventName, barangay, "upcoming_event");
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error creating notification", e));
    }

    private void showSystemNotification(String eventId, String eventName, String barangay, String type) {
        Log.d(TAG, "showSystemNotification() called for event: " + eventName);
        NotificationManager notificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            String title, content;
            if ("upcoming_event".equals(type)) {
                title = "Event Tomorrow: " + eventName;
                content = "Remember to attend tomorrow's event in Barangay " + barangay;
            } else {
                title = "Event Joined: " + eventName;
                content = "You have successfully joined the event in Barangay " + barangay;
            }

            // Create an explicit intent for the Notifications activity
            Intent notificationIntent = new Intent(getApplicationContext(), Notifications.class);

            // Use a different action for each notification to ensure it's unique
            notificationIntent.setAction("NOTIFICATION_" + type + "_" + System.currentTimeMillis());

            // Add FLAG_ACTIVITY_CLEAR_TASK to ensure we're not stacking activities
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // Add user data to the intent
            notificationIntent.putExtra("userId", userId);
            notificationIntent.putExtra("uaddressId", uaddressId);
            notificationIntent.putExtra("unameId", unameId);
            notificationIntent.putExtra("uotherDetails", uotherDetails);
            notificationIntent.putExtra("eventId", eventId);
            notificationIntent.putExtra("eventName", eventName);
            notificationIntent.putExtra("notificationType", type);

            // Create a unique request code
            int requestCode = (eventName + type + System.currentTimeMillis()).hashCode();

            // Create PendingIntent for notification tap
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    getApplicationContext(),
                    requestCode,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Create a separate full screen intent to ensure notification shows in foreground
            PendingIntent fullScreenIntent = PendingIntent.getActivity(
                    getApplicationContext(),
                    requestCode + 1, // Different request code
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Get default notification sound
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            // Create notification with enhanced visibility settings
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.udyongbayanihan_logo)
                    .setContentTitle(title)
                    .setContentText(content)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                    .setPriority(NotificationCompat.PRIORITY_MAX) // Maximum priority
                    .setCategory(NotificationCompat.CATEGORY_EVENT)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setSound(soundUri)
                    .setVibrate(new long[]{0, 500, 250, 500}) // Stronger vibration pattern
                    .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound, vibration, etc.
                    .setFullScreenIntent(fullScreenIntent, true); // Force heads-up notification

            // For upcoming event notifications (reminders), make them more persistent
            if ("upcoming_event".equals(type)) {
                // Make it ongoing initially to ensure it appears in the drawer
                builder.setOngoing(true);
            }

            // Add event details button for upcoming events
            if ("upcoming_event".equals(type)) {
                // Create intent for viewing event details
                Intent detailsIntent = new Intent(getApplicationContext(), Notifications.class);
                detailsIntent.setAction("VIEW_EVENT_DETAILS_" + eventId + "_" + System.currentTimeMillis());
                detailsIntent.putExtra("userId", userId);
                detailsIntent.putExtra("eventId", eventId);
                detailsIntent.putExtra("eventName", eventName);
                detailsIntent.putExtra("notificationType", "upcoming_event");
                detailsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                PendingIntent detailsPendingIntent = PendingIntent.getActivity(
                        getApplicationContext(),
                        requestCode + 2, // Different request code
                        detailsIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                builder.addAction(
                        R.drawable.udyongbayanihan_logo,
                        "View Details",
                        detailsPendingIntent
                );
            }

            // Use a unique notification ID based on event name and type
            int notificationId = (eventName + type).hashCode();

            // Show the notification
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notification sent with ID: " + notificationId);

            // For upcoming events, remove the "ongoing" status after a short delay
            // This ensures it's initially shown prominently but doesn't stay persistent forever
            if ("upcoming_event".equals(type)) {
                new android.os.Handler(getApplicationContext().getMainLooper()).postDelayed(() -> {
                    try {
                        // Update notification to no longer be ongoing
                        builder.setOngoing(false);
                        notificationManager.notify(notificationId, builder.build());
                        Log.d(TAG, "Updated notification to non-ongoing state");
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating notification status", e);
                    }
                }, 5000); // 5 seconds delay
            }
        }
    }

    interface OnFailureListener {
        void accept(Exception e);
    }
}