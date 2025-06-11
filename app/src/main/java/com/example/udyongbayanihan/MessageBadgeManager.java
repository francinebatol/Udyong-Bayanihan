package com.example.udyongbayanihan;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility class to manage message badges that display the number of unread messages
 */
public class MessageBadgeManager {
    private static final String TAG = "MessageBadgeManager";
    private static MessageBadgeManager instance;

    private FirebaseFirestore db;
    private ListenerRegistration chatListener;
    private Map<String, Integer> unreadCountCache = new HashMap<>();

    private MessageBadgeManager() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Get the singleton instance of MessageBadgeManager
     */
    public static synchronized MessageBadgeManager getInstance() {
        if (instance == null) {
            instance = new MessageBadgeManager();
        }
        return instance;
    }

    /**
     * Setup a badge on an ImageButton
     * @param originalButton The original ImageButton
     * @param activity The activity context
     * @return A RelativeLayout containing the button and badge
     */
    public RelativeLayout setupBadgeView(ImageButton originalButton, Activity activity) {
        // Inflate the badge layout
        LayoutInflater inflater = LayoutInflater.from(activity);
        RelativeLayout badgeLayout = (RelativeLayout) inflater.inflate(
                R.layout.badge_layout, null);

        // Get the ImageButton from the badge layout
        ImageButton imgbtnBase = badgeLayout.findViewById(R.id.imgbtnBase);

        // Copy attributes from original button to new button
        imgbtnBase.setImageDrawable(originalButton.getDrawable());
        imgbtnBase.setBackground(originalButton.getBackground());
        imgbtnBase.setScaleType(originalButton.getScaleType());
        imgbtnBase.setContentDescription(originalButton.getContentDescription());

        // Copy layout parameters
        ViewGroup.LayoutParams lp = originalButton.getLayoutParams();
        badgeLayout.setLayoutParams(lp);

        // NOTE: We can't copy the click listener directly as there's no getter for it
        // The caller will need to set up the click listener on the returned layout

        // Replace the original button with the badge layout
        ViewGroup parent = (ViewGroup) originalButton.getParent();
        int index = parent.indexOfChild(originalButton);
        parent.removeView(originalButton);
        parent.addView(badgeLayout, index);

        return badgeLayout;
    }

    /**
     * Update the badge count
     * @param badgeLayout The RelativeLayout containing the badge
     * @param count The count to display
     */
    public void updateBadgeCount(RelativeLayout badgeLayout, int count) {
        TextView tvBadge = badgeLayout.findViewById(R.id.tvBadge);

        if (count > 0) {
            tvBadge.setVisibility(View.VISIBLE);

            // Display "99+" if count exceeds 99
            String displayText = (count > 99) ? "99+" : String.valueOf(count);
            tvBadge.setText(displayText);

            // Adjust size if needed
            if (count > 9) {
                tvBadge.setTextSize(8);
            } else {
                tvBadge.setTextSize(10);
            }
        } else {
            tvBadge.setVisibility(View.GONE);
        }
    }

    /**
     * Start listening for unread messages for a user
     * @param userId The user ID
     * @param userType The user type (user or admin)
     * @param callback Callback to be called when unread count changes
     * @return ListenerRegistration that can be used to stop listening
     */
    public ListenerRegistration startListeningForUnreadMessages(
            String userId, String userType, UnreadCountCallback callback) {

        Map<String, String> participantMap = new HashMap<>();
        participantMap.put("id", userId);
        participantMap.put("type", userType);

        if (chatListener != null) {
            chatListener.remove();
        }

        chatListener = db.collection("chats")
                .whereArrayContains("participants", participantMap)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for chats: ", error);
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        callback.onUnreadCountUpdated(0);
                        return;
                    }

                    int totalUnreadCount = 0;
                    List<DocumentSnapshot> chatDocs = new ArrayList<>();

                    // First pass: collect all chat documents
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        chatDocs.add(doc);
                    }

                    // Second pass: count unread messages
                    for (DocumentSnapshot doc : chatDocs) {
                        // Check if the message is unread
                        Map<String, Object> lastReadBy = (Map<String, Object>) doc.get("lastReadBy");
                        Date messageTimestamp = doc.getDate("timestamp");
                        Map<String, Object> lastMessageMetadata =
                                (Map<String, Object>) doc.get("lastMessageMetadata");

                        // Skip if user is the sender of the last message
                        if (lastMessageMetadata != null &&
                                userId.equals(lastMessageMetadata.get("senderId"))) {
                            continue;
                        }

                        // Check if message is unread
                        boolean isUnread = isMessageUnread(lastReadBy, messageTimestamp, userId);

                        if (isUnread) {
                            totalUnreadCount++;
                        }
                    }

                    // Cache the count and notify
                    unreadCountCache.put(userId, totalUnreadCount);
                    callback.onUnreadCountUpdated(totalUnreadCount);
                });

        return chatListener;
    }

    /**
     * Stop listening for unread messages
     */
    public void stopListening() {
        if (chatListener != null) {
            chatListener.remove();
            chatListener = null;
        }
    }

    /**
     * Check if a message is unread by a user
     */
    private boolean isMessageUnread(Map<String, Object> lastReadBy, Date messageTimestamp, String userId) {
        if (lastReadBy == null || messageTimestamp == null) {
            return true;
        }

        Object lastReadObj = lastReadBy.get(userId);
        if (!(lastReadObj instanceof Timestamp)) {
            return true;
        }

        Timestamp lastRead = (Timestamp) lastReadObj;
        return lastRead.toDate().before(messageTimestamp);
    }

    /**
     * Get the cached unread count for a user
     * @param userId The user ID
     * @return The unread count, or 0 if not cached
     */
    public int getCachedUnreadCount(String userId) {
        return unreadCountCache.getOrDefault(userId, 0);
    }

    /**
     * Interface for unread count updates
     */
    public interface UnreadCountCallback {
        void onUnreadCountUpdated(int count);
    }

    /**
     * Refresh the unread count for a specific user
     *
     * @param userId The user ID
     * @param userType The user type
     * @param callback Callback to update the badge display
     */
    public void refreshUnreadCount(String userId, String userType, UnreadCountCallback callback) {
        Map<String, String> participantMap = new HashMap<>();
        participantMap.put("id", userId);
        participantMap.put("type", userType);

        db.collection("chats")
                .whereArrayContains("participants", participantMap)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        callback.onUnreadCountUpdated(0);
                        unreadCountCache.put(userId, 0);
                        return;
                    }

                    int totalUnread = 0;
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Map<String, Object> lastReadBy = (Map<String, Object>) doc.get("lastReadBy");
                        Date messageTimestamp = doc.getDate("timestamp");
                        Map<String, Object> lastMessageMetadata =
                                (Map<String, Object>) doc.get("lastMessageMetadata");

                        // Skip if user is the sender of the last message
                        if (lastMessageMetadata != null &&
                                userId.equals(lastMessageMetadata.get("senderId"))) {
                            continue;
                        }

                        // Check if message is unread
                        boolean isUnread = isMessageUnread(lastReadBy, messageTimestamp, userId);
                        if (isUnread) {
                            totalUnread++;
                        }
                    }

                    unreadCountCache.put(userId, totalUnread);
                    callback.onUnreadCountUpdated(totalUnread);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error refreshing unread count", e));
    }
}