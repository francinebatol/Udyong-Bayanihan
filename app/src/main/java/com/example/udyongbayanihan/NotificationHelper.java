package com.example.udyongbayanihan;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "message_notifications";
    private static final String CHANNEL_NAME = "Messages";
    private static final String VERIFICATION_CHANNEL_ID = "verification_notifications";
    private static final String VERIFICATION_CHANNEL_NAME = "Account Verification";
    private static final String FEEDBACK_CHANNEL_ID = "feedback_notifications";
    private static final String FEEDBACK_CHANNEL_NAME = "Event Feedback";
    private static final String SKILL_GROUP_CHANNEL_ID = "skill_group_notifications";
    private static final String SKILL_GROUP_CHANNEL_NAME = "Skill Group Requests";

    private final Context context;
    private final NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Message notifications channel
            NotificationChannel messageChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            messageChannel.setDescription("Notifications for new messages");
            messageChannel.enableVibration(true);
            // Make sure notifications are shown in foreground
            messageChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(messageChannel);

            // Verification notifications channel with enhanced settings for foreground visibility
            NotificationChannel verificationChannel = new NotificationChannel(
                    VERIFICATION_CHANNEL_ID,
                    VERIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            verificationChannel.setDescription("Notifications for account verification status updates");
            verificationChannel.enableVibration(true);
            verificationChannel.setShowBadge(true);

            // Ensure sound plays even if device is in silent mode
            verificationChannel.setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
            );

            // Add lights
            verificationChannel.enableLights(true);
            verificationChannel.setLightColor(Color.BLUE);

            // Make notification bypass Do Not Disturb mode
            verificationChannel.setBypassDnd(true);

            // Set importance to IMPORTANCE_HIGH to show as heads-up notification
            verificationChannel.setImportance(NotificationManager.IMPORTANCE_HIGH);

            // Prevent notification throttling
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                verificationChannel.setAllowBubbles(true);
            }

            notificationManager.createNotificationChannel(verificationChannel);

            // Feedback notifications channel with enhanced settings
            NotificationChannel feedbackChannel = new NotificationChannel(
                    FEEDBACK_CHANNEL_ID,
                    FEEDBACK_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            feedbackChannel.setDescription("Notifications for event feedback requests");
            feedbackChannel.enableVibration(true);
            feedbackChannel.setShowBadge(true);

            // Ensure sound plays even if device is in silent mode
            feedbackChannel.setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
            );

            // Add lights
            feedbackChannel.enableLights(true);
            feedbackChannel.setLightColor(Color.BLUE);

            // Make notification bypass Do Not Disturb mode
            feedbackChannel.setBypassDnd(true);

            // Set importance to IMPORTANCE_HIGH to show as heads-up notification
            feedbackChannel.setImportance(NotificationManager.IMPORTANCE_HIGH);

            // Prevent notification throttling
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                feedbackChannel.setAllowBubbles(true);
            }

            notificationManager.createNotificationChannel(feedbackChannel);

            // Skill Group notifications channel with enhanced settings
            NotificationChannel skillGroupChannel = new NotificationChannel(
                    SKILL_GROUP_CHANNEL_ID,
                    SKILL_GROUP_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            skillGroupChannel.setDescription("Notifications for skill group request status updates");
            skillGroupChannel.enableVibration(true);
            skillGroupChannel.setShowBadge(true);

            // Ensure sound plays even if device is in silent mode
            skillGroupChannel.setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
            );

            // Add lights
            skillGroupChannel.enableLights(true);
            skillGroupChannel.setLightColor(Color.GREEN);

            // Make notification bypass Do Not Disturb mode
            skillGroupChannel.setBypassDnd(true);

            // Set importance to IMPORTANCE_HIGH to show as heads-up notification
            skillGroupChannel.setImportance(NotificationManager.IMPORTANCE_HIGH);

            // Prevent notification throttling
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                skillGroupChannel.setAllowBubbles(true);
            }

            notificationManager.createNotificationChannel(skillGroupChannel);
        }
    }

    public void showMessageNotification(String userId, String userType, String senderId,
                                        String senderType, String senderName, String message) {
        // IMPORTANT: Add this check to skip notifications for own messages
        if (userId.equals(senderId)) {
            // Don't show notification for user's own messages
            return;
        }

        // Create intent for when notification is clicked
        Intent intent = new Intent(context, Chat.class);
        intent.putExtra("userId", userId);
        intent.putExtra("userType", userType);
        intent.putExtra("recipientId", senderId);
        intent.putExtra("recipientType", senderType);
        intent.putExtra("recipientName", senderName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                senderId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.udyongbayanihan_logo)
                .setContentTitle(senderName)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 250, 250, 250})
                // Force notifications to show in the drawer
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Force heads-up notification
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        notificationManager.notify(senderId.hashCode(), builder.build());
    }

    /**
     * Show a notification for account verification status updates with enhanced foreground visibility
     */
    public void showVerificationNotification(String userId, String title, String message, String status) {
        // Skip notifications for "Unverified" status
        if ("Unverified".equals(status) || "denied".equals(status)) {
            Log.d(TAG, "Skipping system notification for Unverified/denied status");
            return;
        }

        // Get current device user's ID from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        String currentDeviceUserId = prefs.getString("current_device_user_id", null);

        // Only show notification if it's for the current device user
        if (currentDeviceUserId == null || !currentDeviceUserId.equals(userId)) {
            Log.d(TAG, "Skipping verification notification for user " + userId +
                    " as it's not for the current device user: " + currentDeviceUserId);
            return;
        }

        // Check if we've shown this exact notification recently (within 5 seconds)
        long lastVerificationTime = prefs.getLong("last_verification_notification_time_" + userId, 0);
        String lastVerificationMessage = prefs.getString("last_verification_notification_message_" + userId, "");
        long currentTime = System.currentTimeMillis();

        // If we've shown the same notification in the last 5 seconds, skip it to prevent duplicates
        if (currentTime - lastVerificationTime < 5000 && message.equals(lastVerificationMessage)) {
            Log.d(TAG, "Skipping duplicate verification notification (shown in last 5 seconds)");
            return;
        }

        // Store this notification's details to prevent immediate duplicates
        prefs.edit()
                .putLong("last_verification_notification_time_" + userId, currentTime)
                .putString("last_verification_notification_message_" + userId, message)
                .apply();

        Log.d(TAG, "Preparing verification notification for user: " + userId);

        // Create intent for when notification is clicked
        Intent intent = new Intent(context, Notifications.class);
        intent.putExtra("userId", userId);
        intent.putExtra("notificationType", "verification_status");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Create unique notification ID for verification
        String notificationId = "verification_" + userId;
        intent.putExtra("notificationId", notificationId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create a full screen intent to ensure the notification shows in foreground
        PendingIntent fullScreenIntent = PendingIntent.getActivity(
                context,
                notificationId.hashCode() + 1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Choose icon and priority based on status
        int smallIcon = R.drawable.udyongbayanihan_logo;
        int priority = NotificationCompat.PRIORITY_MAX; // Use MAX priority for foreground visibility

        // Create the notification builder with enhanced settings for foreground visibility
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, VERIFICATION_CHANNEL_ID)
                .setSmallIcon(smallIcon)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(priority)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 250, 500}) // Stronger vibration pattern
                // Force notifications to show in the drawer
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Add full screen intent to ensure it shows in foreground
                .setFullScreenIntent(fullScreenIntent, true)
                // Force audio and vibration
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        // Set color based on status
        if ("approved".equals(status) || "Verified".equals(status)) {
            builder.setColor(context.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            builder.setColor(context.getResources().getColor(android.R.color.holo_blue_dark));
        }

        // Use a unique notification ID
        int uniqueId = notificationId.hashCode();

        // Check if app is in foreground
        boolean isAppInForeground = isAppInForeground();

        Log.d(TAG, "App in foreground: " + isAppInForeground + " - Will still show verification notification");

        // Make notification temporarily "ongoing" to ensure it shows in foreground
        builder.setOngoing(true);
        notificationManager.notify(uniqueId, builder.build());
        Log.d(TAG, "Showing verification notification for current device user: " + userId);

        // Remove the "ongoing" status after a short delay
        new Handler(context.getMainLooper()).postDelayed(() -> {
            try {
                builder.setOngoing(false);
                notificationManager.notify(uniqueId, builder.build());
            } catch (Exception e) {
                Log.e(TAG, "Error updating notification status: " + e.getMessage());
            }
        }, 3000); // 3 seconds delay
    }

    /**
     * Show a notification for event feedback requests with enhanced foreground visibility
     * Modified to only show notifications for the current device user
     */
    public void showFeedbackRequestNotification(String userId, String eventId, String eventName, String barangay, String uaddressId, String unameId, String uotherDetails) {
        Log.d(TAG, "Processing feedback notification request for user: " + userId + ", event: " + eventName);


        // Get current device user's ID from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        String currentDeviceUserId = prefs.getString("current_device_user_id", null);

        // Only show notification if it's for the current device user
        if (currentDeviceUserId == null) {
            currentDeviceUserId = UserSessionManager.ensureCurrentDeviceUserId(context);
            Log.d(TAG, "Recovered current device user ID via UserSessionManager: " + currentDeviceUserId);
        }

        // If still null, try looking directly at the active sessions
        if (currentDeviceUserId == null) {
            // Try to get user ID from user session
            SharedPreferences userPrefs = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
            String userSessionId = userPrefs.getString("userId", null);

            // Try to get admin ID from admin session
            SharedPreferences adminPrefs = context.getSharedPreferences("AdminSession", Context.MODE_PRIVATE);
            String adminSessionId = adminPrefs.getString("amAccountId", null);

            // Update currentDeviceUserId if we found a session
            if (userSessionId != null) {
                currentDeviceUserId = userSessionId;
                // Save it for future use
                prefs.edit().putString("current_device_user_id", currentDeviceUserId).apply();
                Log.d(TAG, "Recovered user ID from UserSession: " + currentDeviceUserId);
            } else if (adminSessionId != null) {
                currentDeviceUserId = adminSessionId;
                // Save it for future use
                prefs.edit().putString("current_device_user_id", currentDeviceUserId).apply();
                Log.d(TAG, "Recovered user ID from AdminSession: " + currentDeviceUserId);
            }
        }

        if (currentDeviceUserId == null) {
            Log.e(TAG, "⚠️ Cannot determine current device user ID, skipping notification");
            return;
        }

        if (!currentDeviceUserId.equals(userId)) {
            Log.d(TAG, "Skipping feedback notification for user " + userId +
                    " as it's not for the current device user: " + currentDeviceUserId);
            return;
        }

        Log.d(TAG, "✓ Current device user ID matches target user ID: " + userId + ", proceeding with notification");

        // Get current admin ID from SharedPreferences
        SharedPreferences adminPrefs = context.getSharedPreferences("AdminSession", Context.MODE_PRIVATE);
        String currentAdminId = adminPrefs.getString("amAccountId", null);

        SharedPreferences allAdminsPrefs = context.getSharedPreferences("AllAdmins", Context.MODE_PRIVATE);
        String allAdminsString = allAdminsPrefs.getString("adminIds", "");
        boolean isAdmin = false;

        if (!allAdminsString.isEmpty()) {
            String[] adminIds = allAdminsString.split(",");
            for (String adminId : adminIds) {
                if (adminId.equals(userId)) {
                    isAdmin = true;
                    break;
                }
            }
        }

        if (isAdmin || (currentAdminId != null && userId.equals(currentAdminId))) {
            Log.e(TAG, "🛑 BLOCKED feedback notification - this is an admin: " + userId);
            return;
        }

        // If this userId matches current admin ID, don't show notification
        if (currentAdminId != null && userId.equals(currentAdminId)) {
            Log.e(TAG, "🛑 BLOCKED feedback notification - this is the current admin: " + userId);
            return;
        }

        // Check if this user is any admin (not just the current one)
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // First check AdminMobileAccount collection
        db.collection("AdminMobileAccount")
                .whereEqualTo("amAccountId", userId)
                .get()
                .addOnSuccessListener(adminDocs -> {
                    if (!adminDocs.isEmpty()) {
                        Log.e(TAG, "🛑 BLOCKED feedback notification - user is an admin: " + userId);
                        return;
                    }

                    // Now check if user exists in usersAccount collection (must be a regular user)
                    db.collection("usersAccount")
                            .document(userId)
                            .get()
                            .addOnSuccessListener(userDoc -> {
                                if (!userDoc.exists()) {
                                    Log.e(TAG, "🛑 BLOCKED feedback notification - user not found in usersAccount: " + userId);
                                    return;
                                }

                                // This is confirmed to be a regular user, proceed with notification
                                Log.d(TAG, "✓ CONFIRMED showing feedback notification for current device user: " + userId);
                                proceedWithFeedbackNotification(userId, eventId, eventName, barangay, uaddressId, unameId, uotherDetails);
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
     * Show a notification for skill group request status updates
     */
    public void showSkillGroupNotification(String userId, String skillName, String requestStatus) {
        Log.d(TAG, "Processing skill group notification for user: " + userId + ", skill: " + skillName + ", status: " + requestStatus);

        // Get current device user's ID from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        String currentDeviceUserId = prefs.getString("current_device_user_id", null);

        // Only show notification if it's for the current device user
        if (currentDeviceUserId == null) {
            currentDeviceUserId = UserSessionManager.ensureCurrentDeviceUserId(context);
            Log.d(TAG, "Recovered current device user ID via UserSessionManager: " + currentDeviceUserId);
        }

        if (currentDeviceUserId == null) {
            Log.e(TAG, "⚠️ Cannot determine current device user ID, skipping notification");
            return;
        }

        if (!currentDeviceUserId.equals(userId)) {
            Log.d(TAG, "Skipping skill group notification for user " + userId +
                    " as it's not for the current device user: " + currentDeviceUserId);
            return;
        }

        Log.d(TAG, "✓ Current device user ID matches target user ID: " + userId + ", proceeding with notification");

        // Create intent for when notification is clicked
        Intent intent = new Intent(context, Notifications.class);
        intent.putExtra("userId", userId);
        intent.putExtra("notificationType", Notification.TYPE_SKILL_GROUP);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Create unique notification ID for this skill group notification
        String notificationId = "skill_group_" + skillName + "_" + userId;
        intent.putExtra("notificationId", notificationId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create a full screen intent to ensure the notification shows in foreground
        PendingIntent fullScreenIntent = PendingIntent.getActivity(
                context,
                notificationId.hashCode() + 1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Determine message and color based on request status
        String title = "Skill Group Request";
        String message;
        int colorRes;

        if ("APPROVED".equals(requestStatus)) {
            message = "Your request to join the " + skillName + " skill group has been approved!";
            colorRes = android.R.color.holo_green_dark;
        } else {
            message = "Your request to join the " + skillName + " skill group has been rejected.";
            colorRes = android.R.color.holo_red_dark;
        }

        // Create the notification builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, SKILL_GROUP_CHANNEL_ID)
                .setSmallIcon(R.drawable.udyongbayanihan_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(fullScreenIntent, true)
                .setVibrate(new long[]{0, 250, 250, 250})
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setColor(context.getResources().getColor(colorRes));

        // Create a unique notification ID
        int uniqueId = notificationId.hashCode();

        // Make notification temporarily "ongoing" to ensure it shows in foreground
        builder.setOngoing(true);
        notificationManager.notify(uniqueId, builder.build());

        // Remove the "ongoing" status after a short delay
        new Handler(context.getMainLooper()).postDelayed(() -> {
            try {
                builder.setOngoing(false);
                notificationManager.notify(uniqueId, builder.build());
            } catch (Exception e) {
                Log.e(TAG, "Error updating notification status: " + e.getMessage());
            }
        }, 3000);

        Log.d(TAG, "Showing skill group notification for user: " + userId);
    }

    private void proceedWithFeedbackNotification(String userId, String eventId, String eventName, String barangay, String uaddressId, String unameId, String uotherDetails) {
        // Create unique notification ID for feedback
        String notificationId = "feedback_" + eventId + "_" + userId;

        // Create intent for when notification is clicked
        Intent intent = new Intent(context, Feedback.class);
        intent.putExtra("userId", userId);
        intent.putExtra("eventId", eventId);
        intent.putExtra("eventName", eventName);
        intent.putExtra("uaddressId", uaddressId);
        intent.putExtra("unameId", unameId);
        intent.putExtra("uotherDetails", uotherDetails);
        intent.putExtra("notificationType", "feedback_request");
        intent.putExtra("notificationId", notificationId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create a full screen intent to ensure the notification shows in foreground
        PendingIntent fullScreenIntent = PendingIntent.getActivity(
                context,
                notificationId.hashCode() + 1, // Different request code to avoid conflict
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "Feedback Request";
        String message = "Please provide your feedback for the event: " + eventName;
        if (barangay != null && !barangay.isEmpty()) {
            message += " in Barangay " + barangay;
        }

        // Create the notification builder with max priority to ensure it's seen in foreground
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, FEEDBACK_CHANNEL_ID)
                .setSmallIcon(R.drawable.udyongbayanihan_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 250, 500}) // Stronger vibration pattern
                .setColor(context.getResources().getColor(android.R.color.holo_blue_dark))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenIntent, true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        // Add high importance flag to ensure heads-up notification
        builder.setChannelId(FEEDBACK_CHANNEL_ID);

        // Add a button to open feedback directly
        Intent feedbackIntent = new Intent(context, Feedback.class);
        feedbackIntent.putExtra("userId", userId);
        feedbackIntent.putExtra("eventId", eventId);
        feedbackIntent.putExtra("eventName", eventName);
        feedbackIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent feedbackPendingIntent = PendingIntent.getActivity(
                context,
                notificationId.hashCode() + 2,
                feedbackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        builder.addAction(R.drawable.udyongbayanihan_logo, "Submit Feedback", feedbackPendingIntent);

        // Show the notification with a unique ID
        int uniqueId = notificationId.hashCode();

        try {
            // Detect if app is in foreground
            boolean isAppInForeground = isAppInForeground();

            Log.d(TAG, "App in foreground: " + isAppInForeground + " - Showing feedback notification for current device user ID: " + userId);

            // Always show notification regardless of app state
            notificationManager.notify(uniqueId, builder.build());
            Log.d(TAG, "Feedback notification displayed with ID: " + uniqueId);
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification: " + e.getMessage());
        }

        // Make notification briefly "ongoing" to ensure it shows in foreground
        builder.setOngoing(true);
        notificationManager.notify(uniqueId, builder.build());

        // Remove the "ongoing" status after a short delay
        new Handler(context.getMainLooper()).postDelayed(() -> {
            try {
                builder.setOngoing(false);
                notificationManager.notify(uniqueId, builder.build());
            } catch (Exception e) {
                Log.e(TAG, "Error updating notification status: " + e.getMessage());
            }
        }, 3000); // Increased delay to 3 seconds for better visibility
    }

    /**
     * Check if the app is currently in foreground
     * @return true if app is in foreground, false otherwise
     */
    private boolean isAppInForeground() {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) return false;

        // Get package name of this app
        String packageName = context.getPackageName();

        // For newer Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();
            if (appTasks != null && !appTasks.isEmpty()) {
                try {
                    ActivityManager.RecentTaskInfo taskInfo = appTasks.get(0).getTaskInfo();
                    ComponentName componentName = taskInfo.topActivity;
                    return componentName != null && componentName.getPackageName().equals(packageName);
                } catch (Exception e) {
                    Log.e(TAG, "Error checking foreground state: " + e.getMessage());
                }
            }
        }

        // Fallback for older Android versions
        List<ActivityManager.RunningAppProcessInfo> runningProcesses = activityManager.getRunningAppProcesses();
        if (runningProcesses != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(packageName)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Show an immediate notification when a user joins an event
     */
    public void showEventJoinedNotification(String userId, String eventId, String eventName, String barangay,
                                            String uaddressId, String unameId, String uotherDetails) {
        // Get current device user's ID from SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        String currentDeviceUserId = prefs.getString("current_device_user_id", null);

        // Only show notification if it's for the current device user
        if (currentDeviceUserId == null || !currentDeviceUserId.equals(userId)) {
            Log.d(TAG, "Skipping event joined notification for user " + userId +
                    " as it's not for the current device user: " + currentDeviceUserId);
            return;
        }

        // Create intent for when notification is clicked
        Intent notificationIntent = new Intent(context, Notifications.class);
        notificationIntent.setAction("NOTIFICATION_event_confirmation_" + System.currentTimeMillis());
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // Add user data to the intent
        notificationIntent.putExtra("userId", userId);
        notificationIntent.putExtra("uaddressId", uaddressId);
        notificationIntent.putExtra("unameId", unameId);
        notificationIntent.putExtra("uotherDetails", uotherDetails);

        // Create unique request code
        int requestCode = (eventName + "event_confirmation" + System.currentTimeMillis()).hashCode();

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create a full screen pending intent to ensure notification shows in foreground
        PendingIntent fullScreenIntent = PendingIntent.getActivity(
                context,
                requestCode + 1, // Different request code to avoid conflict
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = "Event Joined: " + eventName;
        String content = "You have successfully joined the event in Barangay " + barangay;

        // Create the notification builder with special foreground handling
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.udyongbayanihan_logo)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_MAX) // Use MAX priority for foreground
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                // Force notification to appear in foreground
                .setFullScreenIntent(fullScreenIntent, true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Force vibration and sound
                .setVibrate(new long[]{0, 250, 250, 250})
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                // Add color to make notification more noticeable
                .setColor(context.getResources().getColor(android.R.color.holo_green_light));

        // Use a unique notification ID
        int notificationId = (eventName + "event_confirmation").hashCode();

        // Use FLAG_ONGOING_EVENT to make notification more prominent
        builder.setOngoing(true);

        // Add action button to view details
        Intent viewIntent = new Intent(context, Notifications.class);
        viewIntent.putExtra("userId", userId);
        viewIntent.putExtra("eventId", eventId);
        viewIntent.putExtra("eventName", eventName);
        viewIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent viewPendingIntent = PendingIntent.getActivity(
                context,
                requestCode + 2, // Different request code
                viewIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        builder.addAction(R.drawable.udyongbayanihan_logo, "View Details", viewPendingIntent);

        // Show the notification
        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Showing event joined notification for current device user: " + userId);

        // Remove the "ongoing" status after a short delay
        new Handler(context.getMainLooper()).postDelayed(() -> {
            // Update notification to no longer be ongoing
            builder.setOngoing(false);
            notificationManager.notify(notificationId, builder.build());
        }, 5000); // 5 seconds
    }
}