package com.example.udyongbayanihan;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.Nullable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MessageListenerService extends Service {
    private static final String TAG = "MessageListenerService";
    private FirebaseFirestore db;
    private ListenerRegistration chatListener;
    private NotificationHelper notificationHelper;
    private HandlerThread handlerThread;
    private Handler backgroundHandler;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        notificationHelper = new NotificationHelper(this);

        // Create background thread for Firestore operations
        handlerThread = new HandlerThread("MessageListenerThread");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String userId = intent.getStringExtra("userId");
            String userType = intent.getStringExtra("userType");

            if (userId != null && userType != null) {
                setupRealtimeListener(userId, userType);
            }
        }
        return START_STICKY;
    }

    private void setupRealtimeListener(String userId, String userType) {
        // Run Firestore operations on background thread
        backgroundHandler.post(() -> {
            Map<String, String> participantMap = new HashMap<>();
            participantMap.put("id", userId);
            participantMap.put("type", userType);

            // Setup real-time listener
            chatListener = db.collection("chats")
                    .whereArrayContains("participants", participantMap)
                    .addSnapshotListener(executor, (snapshots, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Listen failed:", error);
                            return;
                        }

                        if (snapshots != null && !snapshots.isEmpty()) {
                            processSnapshots(snapshots, userId, userType);
                        }
                    });
        });
    }

    private void processSnapshots(QuerySnapshot snapshots, String userId, String userType) {
        // Process changes in background
        executor.execute(() -> {
            for (DocumentChange change : snapshots.getDocumentChanges()) {
                if (change.getType() == DocumentChange.Type.MODIFIED) {
                    DocumentSnapshot document = change.getDocument();
                    DocumentSnapshot oldDocument = change.getDocument().exists() ?
                            change.getDocument() : null;

                    // Only process if this is a new message, not just a read status update
                    // Check if the modification includes timestamp changes (indicating a new message)
                    if (isNewMessageChange(document)) {
                        processNewMessage(document, userId, userType);
                    }
                }
            }
        });
    }

    private boolean isNewMessageChange(DocumentSnapshot document) {
        // If we have metadata about the last change
        if (document.getMetadata().hasPendingWrites()) {
            return false; // Skip local changes (like marking as read)
        }

        // Get the last modified fields if available
        // In most implementations we can't easily access what fields changed
        // So as a fallback, only process if message is very recent
        Timestamp messageTimestamp = document.getTimestamp("timestamp");
        if (messageTimestamp != null) {
            Timestamp currentTime = Timestamp.now();
            // Only consider changes that happened within the last 10 seconds
            // as potentially new messages (adjust time as needed)
            return (currentTime.getSeconds() - messageTimestamp.getSeconds() <= 10);
        }

        return false;
    }

    private void processNewMessage(DocumentSnapshot document, String userId, String userType) {
        try {
            // Get message data
            String lastMessage = document.getString("lastMessage");
            Map<String, Object> lastReadBy = (Map<String, Object>) document.get("lastReadBy");
            Timestamp messageTimestamp = document.getTimestamp("timestamp");

            // Get the last message metadata to check who sent it
            Map<String, Object> lastMessageMetadata = (Map<String, Object>) document.get("lastMessageMetadata");

            // Only continue if we have metadata and can determine the sender
            if (lastMessageMetadata == null) {
                Log.d(TAG, "No message metadata found, skipping notification");
                return;
            }

            // Extract the sender ID from metadata
            String senderId = (String) lastMessageMetadata.get("senderId");
            String senderType = (String) lastMessageMetadata.get("senderType");

            // Skip notification if current user is the sender
            if (userId.equals(senderId)) {
                Log.d(TAG, "Message was sent by current user, skipping notification");
                return;
            }

            // Check if message is unread - this is needed for UI bold highlighting
            boolean isUnread = checkIfUnread(lastReadBy, messageTimestamp, userId);
            if (!isUnread) {
                Log.d(TAG, "Message is already read, skipping notification");
                return;
            }

            // CHECK FOR RECENCY: Only notify if message is very recent (within last 10 seconds)
            // This prevents notifications when just opening the app or marking messages as read
            Timestamp currentTimestamp = Timestamp.now();
            long secondsElapsed = currentTimestamp.getSeconds() - messageTimestamp.getSeconds();

            if (secondsElapsed > 10) {
                Log.d(TAG, "Message is " + secondsElapsed + " seconds old, too old for notification");
                return; // Skip notification for older messages that are likely just being viewed
            }

            // Proceed with fetching sender info and showing notification
            fetchSenderNameAndNotify(
                    senderId,
                    senderType,
                    lastMessage,
                    userId,
                    userType
            );

        } catch (Exception e) {
            Log.e(TAG, "Error processing message", e);
        }
    }

    private boolean checkIfUnread(Map<String, Object> lastReadBy, Timestamp messageTimestamp, String userId) {
        if (lastReadBy == null || messageTimestamp == null) return true;

        Object lastReadObj = lastReadBy.get(userId);
        if (!(lastReadObj instanceof Timestamp)) return true;

        Timestamp lastReadTimestamp = (Timestamp) lastReadObj;
        return lastReadTimestamp.compareTo(messageTimestamp) < 0;
    }

    private Map<String, String> findOtherParticipant(List<Map<String, String>> participants, String userId) {
        return participants.stream()
                .filter(p -> !userId.equals(p.get("id")))
                .findFirst()
                .orElse(null);
    }

    private void fetchSenderNameAndNotify(String senderId, String senderType,
                                          String message, String userId, String userType) {
        String collection = "admin".equals(senderType) ? "AMNameDetails" : "usersName";
        String firstNameField = "admin".equals(senderType) ? "amFirstName" : "firstName";
        String lastNameField = "admin".equals(senderType) ? "amLastName" : "lastName";
        String userIdField = "admin".equals(senderType) ? "amAccountId" : "userId";

        // Execute Firestore query on background thread
        executor.execute(() -> {
            db.collection(collection)
                    .whereEqualTo(userIdField, senderId)
                    .get()
                    .addOnSuccessListener(executor, querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot senderDoc = querySnapshot.getDocuments().get(0);
                            String senderName = senderDoc.getString(firstNameField) + " " +
                                    senderDoc.getString(lastNameField);

                            // Show notification on main thread
                            new Handler(Looper.getMainLooper()).post(() ->
                                    notificationHelper.showMessageNotification(
                                            userId, userType, senderId, senderType, senderName, message
                                    )
                            );
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error fetching sender name", e));
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (chatListener != null) {
            chatListener.remove();
        }
        if (handlerThread != null) {
            handlerThread.quit();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}