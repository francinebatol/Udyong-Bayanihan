package com.example.udyongbayanihan;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.util.Log;

import com.jakewharton.threetenabp.AndroidThreeTen;

public class MyApplication extends Application {
    private static final String TAG = "MyApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize ThreeTen library (keep your existing code)
        AndroidThreeTen.init(this);

        // Add notification channel setup
        setupNotificationChannels();

        Log.d(TAG, "Application initialized with notification channels configured");
    }

    private void setupNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (notificationManager == null) {
                return;
            }

            // First, delete any existing channels to reconfigure them
            deleteExistingChannels(notificationManager);

            // Create message notifications channel
            NotificationChannel messageChannel = new NotificationChannel(
                    "message_notifications",
                    "Messages",
                    NotificationManager.IMPORTANCE_HIGH
            );
            configureChannelForMaxVisibility(messageChannel);
            notificationManager.createNotificationChannel(messageChannel);

            // Create verification notifications channel
            NotificationChannel verificationChannel = new NotificationChannel(
                    "verification_notifications",
                    "Account Verification",
                    NotificationManager.IMPORTANCE_HIGH
            );
            configureChannelForMaxVisibility(verificationChannel);
            notificationManager.createNotificationChannel(verificationChannel);

            // Create feedback notifications channel
            NotificationChannel feedbackChannel = new NotificationChannel(
                    "feedback_notifications",
                    "Event Feedback",
                    NotificationManager.IMPORTANCE_HIGH
            );
            configureChannelForMaxVisibility(feedbackChannel);
            notificationManager.createNotificationChannel(feedbackChannel);

            // Create upcoming events channel
            NotificationChannel upcomingEventsChannel = new NotificationChannel(
                    "upcoming_events",
                    "Upcoming Events",
                    NotificationManager.IMPORTANCE_HIGH
            );
            configureChannelForMaxVisibility(upcomingEventsChannel);
            notificationManager.createNotificationChannel(upcomingEventsChannel);

            Log.d(TAG, "Notification channels created with maximum visibility settings");
        }
    }

    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.O)
    private void configureChannelForMaxVisibility(NotificationChannel channel) {
        channel.setDescription("Important notifications from Udyong Bayanihan app");
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 250, 250, 250});
        channel.setShowBadge(true);
        channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        channel.setBypassDnd(true);

        // Enable sound
        channel.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                new AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());

        // Force importance
        channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
    }

    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.O)
    private void deleteExistingChannels(NotificationManager notificationManager) {
        // Delete any existing channels to ensure we create new ones with our settings
        for (NotificationChannel channel : notificationManager.getNotificationChannels()) {
            notificationManager.deleteNotificationChannel(channel.getId());
        }
    }
}