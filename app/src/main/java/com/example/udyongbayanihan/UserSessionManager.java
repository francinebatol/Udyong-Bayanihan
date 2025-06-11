package com.example.udyongbayanihan;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Utility class to manage user session information across the app
 */
public class UserSessionManager {
    private static final String TAG = "UserSessionManager";

    // SharedPreference Names
    private static final String NOTIFICATION_PREFS = "NotificationPrefs";
    private static final String USER_SESSION = "UserSession";
    private static final String ADMIN_SESSION = "AdminSession";

    // SharedPreference Keys
    private static final String CURRENT_DEVICE_USER_ID = "current_device_user_id";
    private static final String USER_ID = "userId";
    private static final String ADMIN_ID = "amAccountId";

    // Verification Status Related Keys
    private static final String VERIFICATION_STATUS_PREFIX = "last_verification_status_";
    private static final String VERIFICATION_CHECK_PREFIX = "last_verification_check_";
    private static final String VERIFICATION_NOTIFIED_PREFIX = "last_verification_notified_";

    /**
     * Set the current device user ID for notifications
     * @param context Application context
     * @param userId The user ID to set
     * @return true if successful, false otherwise
     */
    public static boolean setCurrentDeviceUserId(Context context, String userId) {
        if (context == null || userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot set current device user ID: context or userId is invalid");
            return false;
        }

        try {
            SharedPreferences prefs = context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE);
            prefs.edit().putString(CURRENT_DEVICE_USER_ID, userId).apply();
            Log.d(TAG, "Set current device user ID: " + userId);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error setting current device user ID", e);
            return false;
        }
    }

    /**
     * Get the current device user ID
     * @param context Application context
     * @return The current device user ID or null if not set
     */
    public static String getCurrentDeviceUserId(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot get current device user ID: context is null");
            return null;
        }

        try {
            SharedPreferences prefs = context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE);
            return prefs.getString(CURRENT_DEVICE_USER_ID, null);
        } catch (Exception e) {
            Log.e(TAG, "Error getting current device user ID", e);
            return null;
        }
    }

    /**
     * Ensures that the current device user ID is set correctly
     * If not set, tries to get it from active user or admin sessions
     * @param context Application context
     * @return The current device user ID or null if it couldn't be set
     */
    public static String ensureCurrentDeviceUserId(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot ensure current device user ID: context is null");
            return null;
        }

        // Check if already set
        String currentId = getCurrentDeviceUserId(context);
        if (currentId != null && !currentId.isEmpty()) {
            return currentId;
        }

        // Try to get from user session
        try {
            SharedPreferences userPrefs = context.getSharedPreferences(USER_SESSION, Context.MODE_PRIVATE);
            String userId = userPrefs.getString(USER_ID, null);
            if (userId != null && !userId.isEmpty()) {
                setCurrentDeviceUserId(context, userId);
                Log.d(TAG, "Set current device user ID from user session: " + userId);
                return userId;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking user session", e);
        }

        // Try to get from admin session
        try {
            SharedPreferences adminPrefs = context.getSharedPreferences(ADMIN_SESSION, Context.MODE_PRIVATE);
            String adminId = adminPrefs.getString(ADMIN_ID, null);
            if (adminId != null && !adminId.isEmpty()) {
                setCurrentDeviceUserId(context, adminId);
                Log.d(TAG, "Set current device user ID from admin session: " + adminId);
                return adminId;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking admin session", e);
        }

        Log.w(TAG, "Could not determine current device user ID from any session");
        return null;
    }

    /**
     * Clear the current device user ID
     * @param context Application context
     */
    public static void clearCurrentDeviceUserId(Context context) {
        if (context == null) {
            Log.e(TAG, "Cannot clear current device user ID: context is null");
            return;
        }

        try {
            SharedPreferences prefs = context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE);
            prefs.edit().remove(CURRENT_DEVICE_USER_ID).apply();
            Log.d(TAG, "Cleared current device user ID");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing current device user ID", e);
        }
    }

    /**
     * Check if the current user is an admin
     * @param context Application context
     * @return true if admin, false if regular user or unknown
     */
    public static boolean isCurrentUserAdmin(Context context) {
        if (context == null) {
            return false;
        }

        try {
            SharedPreferences adminPrefs = context.getSharedPreferences(ADMIN_SESSION, Context.MODE_PRIVATE);
            return adminPrefs.contains(ADMIN_ID);
        } catch (Exception e) {
            Log.e(TAG, "Error checking if current user is admin", e);
            return false;
        }
    }

    /**
     * Updates the stored verification status for a user
     *
     * @param context Application context
     * @param userId User ID to update status for
     * @param status New verification status
     * @return true if the status was updated and changed, false otherwise
     */
    public static boolean updateVerificationStatus(Context context, String userId, String status) {
        if (context == null || userId == null || userId.isEmpty() || status == null) {
            Log.e(TAG, "Cannot update verification status: invalid parameters");
            return false;
        }

        try {
            SharedPreferences prefs = context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE);
            String previousStatus = prefs.getString(VERIFICATION_STATUS_PREFIX + userId, null);

            // Store the new status
            prefs.edit().putString(VERIFICATION_STATUS_PREFIX + userId, status).apply();

            // Also update the last check time
            long currentTime = System.currentTimeMillis();
            prefs.edit().putLong(VERIFICATION_CHECK_PREFIX + userId, currentTime).apply();

            Log.d(TAG, "Updated verification status for user " + userId + ": " + status);

            // Return true if the status changed
            return previousStatus == null || !previousStatus.equals(status);
        } catch (Exception e) {
            Log.e(TAG, "Error updating verification status", e);
            return false;
        }
    }

    /**
     * Gets the last known verification status for a user
     *
     * @param context Application context
     * @param userId User ID to get status for
     * @return The last known verification status or null if not available
     */
    public static String getLastKnownVerificationStatus(Context context, String userId) {
        if (context == null || userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot get verification status: invalid parameters");
            return null;
        }

        try {
            SharedPreferences prefs = context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE);
            return prefs.getString(VERIFICATION_STATUS_PREFIX + userId, null);
        } catch (Exception e) {
            Log.e(TAG, "Error getting verification status", e);
            return null;
        }
    }

    /**
     * Updates the last time a verification notification was shown to the user
     *
     * @param context Application context
     * @param userId User ID to update timestamp for
     * @return true if successful, false otherwise
     */
    public static boolean updateVerificationNotifiedTime(Context context, String userId) {
        if (context == null || userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot update verification notified time: invalid parameters");
            return false;
        }

        try {
            SharedPreferences prefs = context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE);
            prefs.edit().putLong(VERIFICATION_NOTIFIED_PREFIX + userId, System.currentTimeMillis()).apply();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error updating verification notified time", e);
            return false;
        }
    }

    /**
     * Checks if it's time to show a reminder notification for verification status
     *
     * @param context Application context
     * @param userId User ID to check
     * @param intervalMillis Time interval in milliseconds between reminders
     * @return true if enough time has passed since the last notification, false otherwise
     */
    public static boolean shouldShowVerificationReminder(Context context, String userId, long intervalMillis) {
        if (context == null || userId == null || userId.isEmpty()) {
            return false;
        }

        try {
            SharedPreferences prefs = context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE);
            long lastNotified = prefs.getLong(VERIFICATION_NOTIFIED_PREFIX + userId, 0);
            return System.currentTimeMillis() - lastNotified > intervalMillis;
        } catch (Exception e) {
            Log.e(TAG, "Error checking verification reminder time", e);
            return false;
        }
    }

    /**
     * Gets the timestamp of the last verification status check
     *
     * @param context Application context
     * @param userId User ID to get timestamp for
     * @return The timestamp in milliseconds or 0 if not available
     */
    public static long getLastVerificationCheckTime(Context context, String userId) {
        if (context == null || userId == null || userId.isEmpty()) {
            return 0;
        }

        try {
            SharedPreferences prefs = context.getSharedPreferences(NOTIFICATION_PREFS, Context.MODE_PRIVATE);
            return prefs.getLong(VERIFICATION_CHECK_PREFIX + userId, 0);
        } catch (Exception e) {
            Log.e(TAG, "Error getting verification check time", e);
            return 0;
        }
    }
}