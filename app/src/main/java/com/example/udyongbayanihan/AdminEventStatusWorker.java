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

import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminEventStatusWorker extends Worker {
    private static final String TAG = "AdminEventStatusWorker";
    private static final String CHANNEL_ID = "admin_event_status";
    private static final int NOTIFICATION_ID = 1000;

    private final Context context;
    private final FirebaseFirestore db;
    private final String adminId;

    public AdminEventStatusWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.adminId = params.getInputData().getString("adminId");
        createNotificationChannel();
    }

    @NonNull
    @Override
    public Result doWork() {
        if (adminId == null) {
            Log.e(TAG, "Admin ID is null");
            return Result.failure();
        }

        try {
            // Create a TaskCompletionSource to handle async operations
            TaskCompletionSource<Result> taskCompletionSource = new TaskCompletionSource<>();

            // Query EventInformation collection for events created by this admin
            db.collection("EventInformation")
                    .whereEqualTo("amAccountId", adminId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            try {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String eventId = document.getString("eventId");
                                    String status = document.getString("status");

                                    // Check if status is "Accepted" or "Rejected"
                                    if ("Accepted".equals(status) || "Rejected".equals(status)) {
                                        // Check if we've already notified for this event status
                                        if (!AdminNotificationHelper.hasBeenNotified(context, eventId, status)) {
                                            // Get event name from EventDetails collection
                                            db.collection("EventDetails")
                                                    .document(eventId)
                                                    .get()
                                                    .addOnSuccessListener(eventDetailsDoc -> {
                                                        if (eventDetailsDoc.exists()) {
                                                            String eventName = eventDetailsDoc.getString("nameOfEvent");
                                                            if (eventName != null) {
                                                                showNotification(eventName, status, eventId);
                                                                AdminNotificationHelper.markAsNotified(context, eventId, status);
                                                            }
                                                        }                                                    })
                                                    .addOnFailureListener(e ->
                                                            Log.e(TAG, "Error getting event details: ", e));
                                        }
                                    }
                                }
                                taskCompletionSource.setResult(Result.success());
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing documents: ", e);
                                taskCompletionSource.setResult(Result.retry());
                            }
                        } else {
                            Log.e(TAG, "Error getting documents: ", task.getException());
                            taskCompletionSource.setResult(Result.retry());
                        }
                    });

            // Wait for the async operation to complete
            Tasks.await(taskCompletionSource.getTask());
            return taskCompletionSource.getTask().getResult();
        } catch (Exception e) {
            Log.e(TAG, "Error in doWork: ", e);
            return Result.retry();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Event Status Updates",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for event status updates");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void showNotification(String eventName, String status, String eventId) {
        Intent intent = new Intent(context, AdminNotification.class);
        intent.putExtra("amAccountId", adminId);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Store the notification timestamp when it's first created
        long notificationTime = System.currentTimeMillis() / 1000;

        // Format the time for the notification message
        String formattedTime = formatTimestamp(notificationTime);

        String title = status + " Event";
        String message = "Your event '" + eventName + "' has been " + status.toLowerCase() +
                " at " + formattedTime;

        // Store the notification timestamp
        AdminNotificationHelper.storeNotificationTime(context, eventId, status, notificationTime);

        // Add this line to increment the unread notification count
        AdminNotificationHelper.incrementUnreadCount(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.udyongbayanihan_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{100, 200, 300, 400, 500})
                .setContentIntent(pendingIntent)
                // Add a long text style to show the full message with timestamp
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int uniqueNotificationId = NOTIFICATION_ID + (int) (notificationTime % Integer.MAX_VALUE);
        notificationManager.notify(uniqueNotificationId, builder.build());
    }

    // Helper method to format timestamp
    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        Date date = new Date(timestamp * 1000);
        return sdf.format(date);
    }
}