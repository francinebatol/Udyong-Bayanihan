package com.example.udyongbayanihan;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

/**
 * Helper class to manage admin notifications status and count
 */
public class AdminNotificationHelper {
    private static final String TAG = "AdminNotificationHelper";
    private static final String PREFERENCES_NAME = "AdminNotifications";
    private static final String NOTIFIED_PREFIX = "notified_";
    private static final String VIEWED_PREFIX = "viewed_";
    private static final String TIMESTAMP_PREFIX = "timestamp_";
    private static final String UNREAD_COUNT_KEY = "unread_notification_count";

    /**
     * Check if a notification has been shown already
     */
    public static boolean hasBeenNotified(Context context, String eventId, String status) {
        if (context == null || eventId == null || status == null) {
            return false;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String key = NOTIFIED_PREFIX + eventId + "_" + status;
        return prefs.getBoolean(key, false);
    }

    /**
     * Mark a notification as having been shown to the user
     */
    public static void markAsNotified(Context context, String eventId, String status) {
        if (context == null || eventId == null || status == null) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String key = NOTIFIED_PREFIX + eventId + "_" + status;
        prefs.edit().putBoolean(key, true).apply();

        // Increment the unread count when we show a new notification
        int currentCount = prefs.getInt(UNREAD_COUNT_KEY, 0);
        prefs.edit().putInt(UNREAD_COUNT_KEY, currentCount + 1).apply();

        Log.d(TAG, "Marked notification as notified: " + eventId + ", status: " + status
                + ", new count: " + (currentCount + 1));
    }

    /**
     * Check if a notification has been viewed by the user in the notifications list
     */
    public static boolean hasBeenViewed(Context context, String eventId, String status) {
        if (context == null || eventId == null || status == null) {
            return false;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String key = VIEWED_PREFIX + eventId + "_" + status;
        return prefs.getBoolean(key, false);
    }

    /**
     * Mark a notification as viewed by the user
     */
    public static void markAsViewed(Context context, String eventId, String status) {
        if (context == null || eventId == null || status == null) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String key = VIEWED_PREFIX + eventId + "_" + status;
        prefs.edit().putBoolean(key, true).apply();

        Log.d(TAG, "Marked notification as viewed: " + eventId + ", status: " + status);
    }

    /**
     * Store the timestamp when a notification was created
     */
    public static void storeNotificationTime(Context context, String eventId, String status, long timestamp) {
        if (context == null || eventId == null || status == null) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String key = TIMESTAMP_PREFIX + eventId + "_" + status;
        prefs.edit().putLong(key, timestamp).apply();
    }

    /**
     * Get the timestamp when a notification was created
     */
    public static long getNotificationTime(Context context, String eventId, String status) {
        if (context == null || eventId == null || status == null) {
            return 0;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String key = TIMESTAMP_PREFIX + eventId + "_" + status;
        return prefs.getLong(key, System.currentTimeMillis() / 1000);
    }

    /**
     * Get the count of unread notifications
     */
    public static int getUnreadCount(Context context) {
        if (context == null) {
            return 0;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(UNREAD_COUNT_KEY, 0);
    }

    /**
     * Reset the unread notification count to zero
     */
    public static void resetUnreadCount(Context context) {
        if (context == null) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(UNREAD_COUNT_KEY, 0).apply();
        Log.d(TAG, "Reset unread notification count to 0");
    }

    /**
     * Increment the unread notification count by one
     */
    public static void incrementUnreadCount(Context context) {
        if (context == null) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        int currentCount = prefs.getInt(UNREAD_COUNT_KEY, 0);
        prefs.edit().putInt(UNREAD_COUNT_KEY, currentCount + 1).apply();
        Log.d(TAG, "Incremented unread notification count to " + (currentCount + 1));
    }

    /**
     * Mark a list of notifications as viewed
     */
    public static void markNotificationsAsViewed(Context context, List<AdminNotificationItem> notifications) {
        if (context == null || notifications == null || notifications.isEmpty()) {
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        for (AdminNotificationItem item : notifications) {
            String key = VIEWED_PREFIX + item.getEventId() + "_" + item.getStatus();
            editor.putBoolean(key, true);
            Log.d(TAG, "Marking notification as viewed: " + item.getEventId() + ", status: " + item.getStatus());
        }

        // Commit all changes at once for better performance
        editor.apply();

        // Reset the unread count since user viewed all notifications
        editor.putInt(UNREAD_COUNT_KEY, 0).apply();
        Log.d(TAG, "Reset unread count after viewing " + notifications.size() + " notifications");
    }

    /**
     * Interface for notification count callback
     */
    public interface NotificationCountCallback {
        void onCountReceived(int count);
    }

    /**
     * Check Firestore directly for unread notifications
     */
    public static void checkForNewNotifications(Context context, String adminId, NotificationCountCallback callback) {
        if (context == null || adminId == null || callback == null) {
            if (callback != null) {
                callback.onCountReceived(0);
            }
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("EventInformation")
                .whereEqualTo("amAccountId", adminId)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Error checking for notifications", task.getException());
                        callback.onCountReceived(0);
                        return;
                    }

                    int count = 0;
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String eventId = document.getString("eventId");
                        String status = document.getString("status");

                        if (eventId != null && ("Accepted".equals(status) || "Rejected".equals(status))) {
                            // Count as unread if it's been notified but not viewed
                            if (hasBeenNotified(context, eventId, status) && !hasBeenViewed(context, eventId, status)) {
                                count++;
                            }
                            // Also count as a new notification if it hasn't been notified yet
                            else if (!hasBeenNotified(context, eventId, status)) {
                                // Mark it as notified immediately
                                markAsNotified(context, eventId, status);
                                // Store the notification time
                                storeNotificationTime(context, eventId, status, System.currentTimeMillis() / 1000);
                                count++;
                            }
                        }
                    }

                    Log.d(TAG, "Found " + count + " unread notifications for admin: " + adminId);
                    callback.onCountReceived(count);
                });
    }
}