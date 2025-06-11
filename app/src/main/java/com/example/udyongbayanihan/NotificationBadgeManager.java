package com.example.udyongbayanihan;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility class to manage notification badges that display the number of unread notifications
 */
public class NotificationBadgeManager {
    private static final String TAG = "NotificationBadgeManager";
    private static NotificationBadgeManager instance;

    private FirebaseFirestore db;
    private Map<String, ListenerRegistration> notificationListeners = new HashMap<>();
    private Map<String, Integer> unreadCountCache = new HashMap<>();
    private Map<String, TextView> badgeViews = new HashMap<>();

    private NotificationBadgeManager() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized NotificationBadgeManager getInstance() {
        if (instance == null) {
            instance = new NotificationBadgeManager();
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
        if (originalButton == null || activity == null) {
            Log.e(TAG, "Cannot setup badge: button or activity is null");
            return null;
        }

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

        // Copy layout parameters if available
        ViewGroup.LayoutParams lp = originalButton.getLayoutParams();
        if (lp != null) {
            badgeLayout.setLayoutParams(lp);
        }

        // Check if the original button has a parent before trying to replace it
        ViewGroup parent = (ViewGroup) originalButton.getParent();
        if (parent != null) {
            int index = parent.indexOfChild(originalButton);
            parent.removeView(originalButton);
            parent.addView(badgeLayout, index);
        } else {
            // Log that we couldn't replace the button but still return the badge layout
            Log.w(TAG, "Button parent is null, couldn't replace in view hierarchy");
        }

        return badgeLayout;
    }

    /**
     * Update the badge count
     * @param badgeLayout The RelativeLayout containing the badge
     * @param count The count to display
     */
    public void updateBadgeCount(RelativeLayout badgeLayout, int count) {
        if (badgeLayout == null) return;

        TextView tvBadge = badgeLayout.findViewById(R.id.tvBadge);
        if (tvBadge == null) return;

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
     * Start listening for unread notifications for a user, including verification notifications
     *
     * @param userId   The user ID
     * @param callback Callback to be called when unread count changes
     * @return ListenerRegistration that can be used to stop listening
     */
    public ListenerRegistration startListeningForUnreadNotifications(String userId, UnreadCountCallback callback) {
        // Remove any existing listener for this user
        if (notificationListeners.containsKey(userId)) {
            notificationListeners.get(userId).remove();
        }

        // Create a new listener for unread notifications, including verification status notifications
        ListenerRegistration listener = db.collection("Notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)  // Only listen for unread notifications
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for notifications: ", error);
                        return;
                    }

                    if (snapshots == null) {
                        callback.onUnreadCountUpdated(0);
                        return;
                    }

                    int unreadCount = snapshots.size();
                    // DON'T mark as read here, just update the count
                    unreadCountCache.put(userId, unreadCount);
                    callback.onUnreadCountUpdated(unreadCount);

                    // Log the count for debugging
                    Log.d(TAG, "Unread notifications count for user " + userId + ": " + unreadCount);
                });

        notificationListeners.put(userId, listener);
        return listener;
    }

    /**
     * Specifically listen for unread verification notifications
     *
     * @param userId The user ID
     * @param callback Callback to be called when verification notification count changes
     * @return ListenerRegistration that can be used to stop listening
     */
    public ListenerRegistration startListeningForVerificationNotifications(String userId, UnreadCountCallback callback) {
        String listenerKey = userId + "_verification";

        // Remove any existing listener for this user's verification notifications
        if (notificationListeners.containsKey(listenerKey)) {
            notificationListeners.get(listenerKey).remove();
        }

        // Create a new listener for unread verification notifications
        ListenerRegistration listener = db.collection("Notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .whereIn("type", Arrays.asList("verification_status", "user_verification"))
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for verification notifications: ", error);
                        return;
                    }

                    if (snapshots == null) {
                        callback.onUnreadCountUpdated(0);
                        return;
                    }

                    int unreadCount = snapshots.size();
                    callback.onUnreadCountUpdated(unreadCount);

                    // Log the count for debugging
                    Log.d(TAG, "Unread verification notifications for user " + userId + ": " + unreadCount);
                });

        notificationListeners.put(listenerKey, listener);
        return listener;
    }

    /**
     * Stop listening for unread notifications for a user
     * @param userId The user ID
     */
    public void stopListeningForUser(String userId) {
        if (notificationListeners.containsKey(userId)) {
            notificationListeners.get(userId).remove();
            notificationListeners.remove(userId);
        }

        // Also remove verification listener if it exists
        String verificationKey = userId + "_verification";
        if (notificationListeners.containsKey(verificationKey)) {
            notificationListeners.get(verificationKey).remove();
            notificationListeners.remove(verificationKey);
        }
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
     * Reset unread count for a user (when they leave the Notifications activity)
     * @param userId The user ID
     */
    public void resetUnreadCount(String userId) {
        if (userId == null) return;

        // Reset the local cache immediately
        unreadCountCache.put(userId, 0);

        // Log the reset action for debugging
        Log.d(TAG, "Resetting unread notification count for user " + userId);

        // Find all badge views for this user and update them to show zero
        TextView badgeView = badgeViews.get(userId);
        if (badgeView != null) {
            badgeView.post(() -> {
                badgeView.setVisibility(View.GONE);
            });
        }

        // Mark all notifications as read in Firestore
        db.collection("Notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Found " + task.getResult().size() + " unread notifications for user " + userId);

                        // Only proceed if there are actually unread notifications
                        if (task.getResult().size() > 0) {
                            // Actually update the documents to be read=true
                            for (DocumentSnapshot doc : task.getResult()) {
                                db.collection("Notifications")
                                        .document(doc.getId())
                                        .update("read", true)
                                        .addOnSuccessListener(aVoid ->
                                                Log.d(TAG, "Marked notification " + doc.getId() + " as read"))
                                        .addOnFailureListener(e ->
                                                Log.e(TAG, "Failed to mark notification as read", e));
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query unread notifications", e);
                });
    }

    /**
     * Reset only verification notification counts
     * @param userId The user ID
     */
    public void resetVerificationNotificationCount(String userId) {
        if (userId == null) return;

        // Mark verification notifications as read in Firestore
        db.collection("Notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .whereIn("type", Arrays.asList("verification_status", "user_verification"))
                .get()
                .addOnSuccessListener(documents -> {
                    Log.d(TAG, "Found " + documents.size() + " unread verification notifications for user " + userId);
                    // Update the documents to be read=true
                    for (DocumentSnapshot doc : documents) {
                        db.collection("Notifications")
                                .document(doc.getId())
                                .update("read", true)
                                .addOnSuccessListener(aVoid ->
                                        Log.d(TAG, "Marked verification notification " + doc.getId() + " as read"))
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "Failed to mark verification notification as read", e));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query unread verification notifications", e);
                });
    }

    /**
     * Cleanup all listeners
     */
    public void cleanup() {
        for (ListenerRegistration listener : notificationListeners.values()) {
            if (listener != null) {
                listener.remove();
            }
        }
        notificationListeners.clear();
    }

    /**
     * Interface for unread count updates
     */
    public interface UnreadCountCallback {
        void onUnreadCountUpdated(int count);
    }
}