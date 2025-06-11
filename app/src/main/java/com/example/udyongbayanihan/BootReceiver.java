package com.example.udyongbayanihan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "BootReceiver.onReceive called with action: " + intent.getAction());

        if (intent.getAction() != null &&
                (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) ||
                        intent.getAction().equals(Intent.ACTION_MY_PACKAGE_REPLACED) ||
                        intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED))) {

            // Retrieve user session data
            SharedPreferences prefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
            String userId = prefs.getString("userId", null);
            String userType = prefs.getString("userType", null);

            // Also retrieve and store the current device user ID for notification filtering
            SharedPreferences notificationPrefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
            notificationPrefs.edit().putString("current_device_user_id", userId).apply();
            Log.d(TAG, "Stored current device user ID: " + userId + " for notification filtering");

            if (userId != null && userType != null) {
                Log.d(TAG, "Found active user session for user: " + userId + ", type: " + userType);
                Log.d(TAG, "Scheduling notifications after device boot...");

                // Schedule all types of notifications
                NotificationWorkManager.scheduleDailyEventChecks(context);
                NotificationWorkManager.scheduleMessageChecks(context, userId, userType);

                // For regular users, schedule verification status checks
                if ("user".equals(userType)) {
                    // Schedule verification checks at higher frequency
                    NotificationWorkManager.scheduleVerificationStatusChecks(context, userId);

                    // Also trigger an immediate check
                    NotificationWorkManager.checkAndNotifyVerificationStatus(context, userId);

                    // For regular users, also schedule feedback notifications
                    NotificationWorkManager.scheduleFeedbackNotificationChecks(context);
                }

                Log.d(TAG, "All notification services scheduled successfully after boot");
            } else {
                Log.d(TAG, "No active user session found after device boot");
            }
        }
    }
}