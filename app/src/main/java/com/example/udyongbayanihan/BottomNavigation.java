package com.example.udyongbayanihan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class BottomNavigation {
    private static final String TAG = "BottomNavigation";
    private final String userId;
    private final String uaddressId;
    private final String unameId;
    private final String uotherDetails;
    private final FirebaseFirestore db;
    private MessageBadgeManager messageBadgeManager;
    private NotificationBadgeManager notificationBadgeManager;
    private ListenerRegistration unreadMessagesListener;
    private ListenerRegistration unreadNotificationsListener;
    private ListenerRegistration unreadVerificationListener;
    private RelativeLayout notificationsBadgeLayout;

    public BottomNavigation(String userId, String uaddressId, String unameId, String uotherDetails) {
        this.userId = userId;
        this.uaddressId = uaddressId;
        this.unameId = unameId;
        this.uotherDetails = uotherDetails;
        this.db = FirebaseFirestore.getInstance();
        this.messageBadgeManager = MessageBadgeManager.getInstance();
        this.notificationBadgeManager = NotificationBadgeManager.getInstance();
    }

    // Initialize navigation buttons and their actions
    public void setupBottomNavigation(ImageButton navHome, ImageButton navCommunity,
                                      ImageButton navProfile, ImageButton navEventCalendar,
                                      ImageButton navNotifications, Context context) {
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(context, Home.class);
            passUserData(intent);
            context.startActivity(intent);
        });

        navCommunity.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommunityGroup.class);
            passUserData(intent);
            context.startActivity(intent);
        });

        navProfile.setOnClickListener(v -> {
            Intent intent = new Intent(context, ViewProfile.class);
            passUserData(intent);
            context.startActivity(intent);
        });

        navEventCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(context, EventCalendar.class);
            passUserData(intent);
            context.startActivity(intent);
        });

        // Set up notification badge using the proper badge system
        if (context instanceof Activity) {
            RelativeLayout badgeLayout = addNotificationBadge(navNotifications, (Activity) context);

            // Set up notification button click listener with badge reset
            navNotifications.setOnClickListener(v -> {
                // If we're not already in the Notifications activity, navigate there
                if (!(context instanceof Notifications)) {
                    // Reset the unread count BEFORE navigating to ensure badge disappears immediately
                    notificationBadgeManager.resetUnreadCount(userId);

                    // Navigate to Notifications activity
                    Intent intent = new Intent(context, Notifications.class);
                    passUserData(intent);
                    context.startActivity(intent);
                }
            });
        } else {
            // Fallback if not in an Activity context
            navNotifications.setOnClickListener(v -> {
                // Reset unread count even in this fallback case
                notificationBadgeManager.resetUnreadCount(userId);

                Intent intent = new Intent(context, Notifications.class);
                passUserData(intent);
                context.startActivity(intent);
            });
        }
    }

    /**
     * Set up the message badge on a message button
     *
     * @param imgbtnMessages The message button to add a badge to
     * @param activity The activity context
     * @return The RelativeLayout containing the badged button
     */
    public RelativeLayout setupMessageBadge(ImageButton imgbtnMessages, Activity activity) {
        if (imgbtnMessages == null || userId == null) {
            return null;
        }

        // Create the badge layout
        RelativeLayout messagesBadgeLayout = messageBadgeManager.setupBadgeView(imgbtnMessages, activity);

        // Set the click listener on the new button inside the badge layout
        ImageButton baseButton = messagesBadgeLayout.findViewById(R.id.imgbtnBase);
        if (baseButton != null) {
            baseButton.setOnClickListener(v -> {
                Intent intent = new Intent(activity, Messages.class);
                Bundle userDetails = new Bundle();
                userDetails.putString("uaddressId", uaddressId);
                userDetails.putString("unameId", unameId);
                userDetails.putString("uotherDetails", uotherDetails);
                UserDataHelper.passUserData(intent, userId, "user", userDetails);
                activity.startActivity(intent);
            });
        }

        // Check if there's an existing cached count and update badge
        int cachedCount = messageBadgeManager.getCachedUnreadCount(userId);
        messageBadgeManager.updateBadgeCount(messagesBadgeLayout, cachedCount);

        // Start listening for unread messages if not already listening
        if (unreadMessagesListener == null) {
            unreadMessagesListener = messageBadgeManager.startListeningForUnreadMessages(
                    userId, "user", count -> {
                        activity.runOnUiThread(() -> {
                            messageBadgeManager.updateBadgeCount(messagesBadgeLayout, count);
                        });
                    });
        }

        return messagesBadgeLayout;
    }

    /**
     * Add a badge overlay to a navigation button without replacing it
     */
    public RelativeLayout addNotificationBadge(ImageButton button, Activity activity) {
        if (button == null || activity == null || userId == null) {
            Log.e("BottomNavigation", "Cannot add badge: button, activity, or userId is null");
            return null;
        }

        // Find the parent ViewGroup
        ViewGroup parent = (ViewGroup) button.getParent();
        if (parent == null) {
            Log.e("BottomNavigation", "Cannot add badge: button parent is null");
            return null;
        }

        // Get the index of the button in its parent
        int index = parent.indexOfChild(button);

        // The layout params of the button - IMPORTANT: we need to preserve weight
        ViewGroup.LayoutParams buttonParams = button.getLayoutParams();
        float weight = 0;
        if (buttonParams instanceof LinearLayout.LayoutParams) {
            weight = ((LinearLayout.LayoutParams) buttonParams).weight;
        }

        // Create a RelativeLayout container to hold both the button and badge
        RelativeLayout container = new RelativeLayout(activity);
        if (buttonParams instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, weight);
            container.setLayoutParams(containerParams);
        } else {
            container.setLayoutParams(buttonParams);
        }

        // Remove the button from its parent
        parent.removeView(button);

        // Add the button to the container with MATCH_PARENT to fill the container
        RelativeLayout.LayoutParams buttonInContainerParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        buttonInContainerParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        button.setLayoutParams(buttonInContainerParams);
        container.addView(button);

        // Inflate badge overlay view
        View badgeOverlay = activity.getLayoutInflater().inflate(R.layout.badge_overlay, null);

        // Get the badge TextView
        TextView badgeView = badgeOverlay.findViewById(R.id.tvBadge);

        // Add the badge overlay to the container
        RelativeLayout.LayoutParams badgeParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        container.addView(badgeOverlay, badgeParams);

        // Add the container back to the parent at the original position
        parent.addView(container, index);

        // Store this for access later
        notificationsBadgeLayout = container;

        // Special handling if we're already in the Notifications activity
        if (activity instanceof Notifications) {
            // Hide the badge immediately if we're in the Notifications activity
            badgeView.setVisibility(View.GONE);
        }

        // Start listening for all unread notifications (including verification notifications)
        unreadNotificationsListener = notificationBadgeManager.startListeningForUnreadNotifications(
                userId, count -> {
                    activity.runOnUiThread(() -> {
                        // If we're in the Notifications activity, always hide the badge
                        if (activity instanceof Notifications) {
                            badgeView.setVisibility(View.GONE);
                        } else {
                            // In other activities, show badge based on count
                            if (count > 0) {
                                badgeView.setVisibility(View.VISIBLE);
                                badgeView.setText(count > 99 ? "99+" : String.valueOf(count));
                            } else {
                                badgeView.setVisibility(View.GONE);
                            }
                        }
                    });
                });

        // Add specific listener for verification notifications to prioritize them
        // This will ensure verification notifications get special treatment if needed
        unreadVerificationListener = notificationBadgeManager.startListeningForVerificationNotifications(
                userId, count -> {
                    if (count > 0 && !(activity instanceof Notifications)) {
                        // Log that we found verification notifications
                        Log.d(TAG, "Found " + count + " unread verification notifications");

                        // We could use this to highlight the notification icon differently
                        // For now, let's keep it simple and just update the existing badge
                        activity.runOnUiThread(() -> {
                            if (badgeView.getVisibility() != View.VISIBLE) {
                                badgeView.setVisibility(View.VISIBLE);
                                badgeView.setText(count > 99 ? "99+" : String.valueOf(count));
                            }
                        });
                    }
                });

        return container;
    }

    /**
     * Hide the notification badge - useful when in Notifications activity
     */
    public void hideNotificationBadge() {
        if (notificationsBadgeLayout != null) {
            TextView badgeView = notificationsBadgeLayout.findViewById(R.id.tvBadge);
            if (badgeView != null) {
                badgeView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Clean up resources when the activity is being destroyed
     */
    public void cleanup() {
        if (unreadMessagesListener != null) {
            unreadMessagesListener.remove();
            unreadMessagesListener = null;
        }

        if (unreadNotificationsListener != null) {
            unreadNotificationsListener.remove();
            unreadNotificationsListener = null;
        }

        if (unreadVerificationListener != null) {
            unreadVerificationListener.remove();
            unreadVerificationListener = null;
        }
    }

    /**
     * Update notification badge counts manually
     * @param count Number to display on badge
     */
    public void updateNotificationBadgeCount(int count) {
        if (notificationsBadgeLayout != null) {
            TextView badgeView = notificationsBadgeLayout.findViewById(R.id.tvBadge);
            if (badgeView != null) {
                if (count > 0) {
                    badgeView.setVisibility(View.VISIBLE);
                    badgeView.setText(count > 99 ? "99+" : String.valueOf(count));
                } else {
                    badgeView.setVisibility(View.GONE);
                }
            }
        }
    }

    // Helper method to pass user-related IDs to an intent
    private void passUserData(Intent intent) {
        intent.putExtra("userId", userId);
        intent.putExtra("uaddressId", uaddressId);
        intent.putExtra("unameId", unameId);
        intent.putExtra("uotherDetails", uotherDetails);
    }
}