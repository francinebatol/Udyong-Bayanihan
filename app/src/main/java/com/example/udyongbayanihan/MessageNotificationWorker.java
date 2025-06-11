package com.example.udyongbayanihan;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MessageNotificationWorker extends Worker {
    private static final String TAG = "MessageNotificationWorker";
    private final Context context;
    private final FirebaseFirestore db;
    private final NotificationHelper notificationHelper;
    private ListenerRegistration chatListener;
    private final CountDownLatch workCompleteLatch = new CountDownLatch(1);

    public MessageNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.notificationHelper = new NotificationHelper(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        String userId = getInputData().getString("userId");
        String userType = getInputData().getString("userType");

        if (userId == null || userType == null) {
            Log.e(TAG, "Missing user information");
            return Result.failure();
        }

        setupMessageListener(userId, userType);

        try {
            // Wait for some time to allow listener to receive updates
            boolean completed = workCompleteLatch.await(15, TimeUnit.MINUTES);
            if (!completed) {
                Log.d(TAG, "Work period completed normally");
            }
            return Result.success();
        } catch (InterruptedException e) {
            Log.e(TAG, "Work interrupted", e);
            return Result.retry();
        } finally {
            cleanup();
        }
    }

    private void setupMessageListener(String userId, String userType) {
        Map<String, String> participantMap = new HashMap<>();
        participantMap.put("id", userId);
        participantMap.put("type", userType);

        chatListener = db.collection("chats")
                .whereArrayContains("participants", participantMap)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed:", error);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.MODIFIED || dc.getType() == DocumentChange.Type.ADDED) {
                                // Get the timestamp of the message
                                Timestamp messageTimestamp = dc.getDocument().getTimestamp("timestamp");
                                Timestamp currentTimestamp = Timestamp.now();

                                // Only notify if message is recent (within last 10 seconds)
                                // This makes it more likely it's a true new message, not just a read status update
                                if (messageTimestamp != null &&
                                        currentTimestamp.getSeconds() - messageTimestamp.getSeconds() <= 10 &&
                                        dc.getDocument().contains("lastMessage")) {

                                    // Additional check: If this modification is an actual new message
                                    Map<String, Object> lastMessageMetadata =
                                            (Map<String, Object>) dc.getDocument().get("lastMessageMetadata");

                                    // Only process if last message metadata exists and has a sender
                                    if (lastMessageMetadata != null && lastMessageMetadata.containsKey("senderId")) {
                                        processNewMessage(dc.getDocument().getData(), userId, userType);
                                    }
                                }
                            }                        }
                    }
                });
    }

    private void processNewMessage(Map<String, Object> chatData, String userId, String userType) {
        // Get message details
        String lastMessage = (String) chatData.get("lastMessage");

        // Get the last message metadata to check sender
        Map<String, Object> messageMetadata = (Map<String, Object>) chatData.get("lastMessageMetadata");
        if (messageMetadata == null) {
            Log.d(TAG, "No message metadata found, skipping notification");
            return;
        }

        // Extract sender information
        String senderId = (String) messageMetadata.get("senderId");
        String senderType = (String) messageMetadata.get("senderType");

        // Only send notification if the message is from someone else
        if (senderId != null && !senderId.equals(userId)) {
            Log.d(TAG, "New message from " + senderId + " of type " + senderType);

            // Check message timestamp - only notify if very recent (less than 10 seconds old)
            Timestamp messageTimestamp = (Timestamp) chatData.get("timestamp");
            Timestamp currentTimestamp = Timestamp.now();

            if (messageTimestamp != null) {
                long secondsSinceMessage = currentTimestamp.getSeconds() - messageTimestamp.getSeconds();

                // Only notify for new messages, not just status updates
                if (secondsSinceMessage > 10) {
                    Log.d(TAG, "Message is too old (" + secondsSinceMessage + " seconds), likely just a status update");
                    return;
                }
            }

            // Check if the message is already read
            Map<String, Object> lastReadBy = (Map<String, Object>) chatData.get("lastReadBy");
            boolean isRead = false;

            if (lastReadBy != null && messageTimestamp != null) {
                Object lastReadObj = lastReadBy.get(userId);
                if (lastReadObj instanceof Timestamp) {
                    Timestamp lastReadTimestamp = (Timestamp) lastReadObj;
                    isRead = lastReadTimestamp.compareTo(messageTimestamp) >= 0;
                }
            }

            if (!isRead) {
                // Fetch sender name and show notification
                fetchSenderNameAndNotify(senderId, senderType, lastMessage, userId, userType);
            } else {
                Log.d(TAG, "Message already read, skipping notification");
            }
        } else {
            Log.d(TAG, "Message was sent by current user, skipping notification");
        }
    }

    private void fetchSenderNameAndNotify(String senderId, String senderType,
                                          String message, String userId, String userType) {
        String collection = "admin".equals(senderType) ? "AMNameDetails" : "usersName";
        String firstNameField = "admin".equals(senderType) ? "amFirstName" : "firstName";
        String lastNameField = "admin".equals(senderType) ? "amLastName" : "lastName";
        String userIdField = "admin".equals(senderType) ? "amAccountId" : "userId";

        db.collection(collection)
                .whereEqualTo(userIdField, senderId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String firstName = querySnapshot.getDocuments().get(0).getString(firstNameField);
                        String lastName = querySnapshot.getDocuments().get(0).getString(lastNameField);
                        String senderName = firstName + " " + lastName;

                        notificationHelper.showMessageNotification(
                                userId, userType, senderId, senderType, senderName, message);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching sender name", e));
    }

    private void cleanup() {
        if (chatListener != null) {
            chatListener.remove();
            chatListener = null;
        }
    }

    @Override
    public void onStopped() {
        super.onStopped();
        cleanup();
    }
}