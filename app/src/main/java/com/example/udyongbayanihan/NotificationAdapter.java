package com.example.udyongbayanihan;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<Notification> notifications;
    private static final int TYPE_EVENT_CONFIRMATION = 1;
    private static final int TYPE_UPCOMING_EVENT = 2;
    private static final int TYPE_VERIFICATION_STATUS = 3;
    private static final int TYPE_FEEDBACK_REQUEST = 4;
    private static final int TYPE_SKILL_GROUP_REQUEST = 5;

    private String userVerificationStatus = null; // Current account status

    public NotificationAdapter(Context context, List<Notification> notifications) {
        this.context = context;
        this.notifications = notifications;
    }

    // Method to set the current verification status
    public void setUserVerificationStatus(String status) {
        this.userVerificationStatus = status;
        notifyDataSetChanged(); // Update all items that might show the status
    }

    @Override
    public int getItemViewType(int position) {
        Notification notification = notifications.get(position);
        String type = notification.getType();

        // Handle potential type naming inconsistencies
        if ("user_verification".equals(type) || "verification_status".equals(type)) {
            return TYPE_VERIFICATION_STATUS;
        } else if ("feedback_request".equals(type)) {
            return TYPE_FEEDBACK_REQUEST;
        } else if (Notification.TYPE_SKILL_GROUP.equals(type)) {
            return TYPE_SKILL_GROUP_REQUEST;
        } else if ("event_confirmation".equals(type)) {
            return TYPE_EVENT_CONFIRMATION;
        } else if ("upcoming_event".equals(type)) {
            return TYPE_UPCOMING_EVENT;
        } else {
            // Default case - use event confirmation layout
            return TYPE_EVENT_CONFIRMATION;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case TYPE_UPCOMING_EVENT:
                view = LayoutInflater.from(context).inflate(R.layout.notification_upcoming_event, parent, false);
                return new EventNotificationViewHolder(view);
            case TYPE_VERIFICATION_STATUS:
                view = LayoutInflater.from(context).inflate(R.layout.notification_verification_status, parent, false);
                return new VerificationStatusViewHolder(view);
            case TYPE_FEEDBACK_REQUEST:
                view = LayoutInflater.from(context).inflate(R.layout.notification_feedback_request, parent, false);
                return new FeedbackRequestViewHolder(view);
            case TYPE_SKILL_GROUP_REQUEST:
                view = LayoutInflater.from(context).inflate(R.layout.notification_skill_group, parent, false);
                return new SkillGroupViewHolder(view);
            case TYPE_EVENT_CONFIRMATION:
            default:
                view = LayoutInflater.from(context).inflate(R.layout.notification_event_confirmation, parent, false);
                return new EventNotificationViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Notification notification = notifications.get(position);

        // Apply bold text style to unread notifications
        applyUnreadStyle(holder, notification);

        if (holder instanceof EventNotificationViewHolder) {
            EventNotificationViewHolder eventHolder = (EventNotificationViewHolder) holder;
            bindEventNotification(eventHolder, notification);
        } else if (holder instanceof VerificationStatusViewHolder) {
            VerificationStatusViewHolder verificationHolder = (VerificationStatusViewHolder) holder;
            bindVerificationNotification(verificationHolder, notification);
        } else if (holder instanceof FeedbackRequestViewHolder) {
            FeedbackRequestViewHolder feedbackHolder = (FeedbackRequestViewHolder) holder;
            bindFeedbackNotification(feedbackHolder, notification);
        } else if (holder instanceof SkillGroupViewHolder) {
            SkillGroupViewHolder skillGroupHolder = (SkillGroupViewHolder) holder;
            bindSkillGroupNotification(skillGroupHolder, notification);
        }
    }

    private void applyUnreadStyle(RecyclerView.ViewHolder holder, Notification notification) {
        // Apply bold text style to unread notifications
        TextView textNotification = null;

        if (holder instanceof EventNotificationViewHolder) {
            EventNotificationViewHolder eventHolder = (EventNotificationViewHolder) holder;
            textNotification = eventHolder.textNotification;
        } else if (holder instanceof VerificationStatusViewHolder) {
            VerificationStatusViewHolder verificationHolder = (VerificationStatusViewHolder) holder;
            textNotification = verificationHolder.textNotification;
        } else if (holder instanceof FeedbackRequestViewHolder) {
            FeedbackRequestViewHolder feedbackHolder = (FeedbackRequestViewHolder) holder;
            textNotification = feedbackHolder.textNotification;
        } else if (holder instanceof SkillGroupViewHolder) {
            SkillGroupViewHolder skillGroupHolder = (SkillGroupViewHolder) holder;
            textNotification = skillGroupHolder.textNotification;
        }

        if (textNotification != null) {
            if (!notification.isRead()) {
                // Bold text for unread notifications
                textNotification.setTypeface(Typeface.DEFAULT_BOLD);

                // Optional: Change background color
                if (holder.itemView.getBackground() != null) {
                    // Apply highlight background to unread notifications
                    holder.itemView.setBackgroundColor(Color.parseColor("#F5F5F5")); // Light gray background
                }
            } else {
                // Normal text for read notifications
                textNotification.setTypeface(Typeface.DEFAULT);

                // Reset background
                holder.itemView.setBackgroundColor(Color.WHITE);
            }
        }
    }

    private void bindEventNotification(EventNotificationViewHolder holder, Notification notification) {
        // Get the event name and barangay from the notification
        String eventName = notification.getEventName();
        String barangay = notification.getBarangay();
        String type = notification.getType();

        // Create the notification text based on the type
        String notificationText;

        if ("event_confirmation".equals(type)) {
            if (eventName != null && barangay != null) {
                notificationText = String.format("You have joined the %s in Barangay %s.",
                        eventName, barangay);
            } else if (eventName != null) {
                notificationText = String.format("You have joined the %s event.", eventName);
            } else if (barangay != null) {
                notificationText = String.format("You have joined an event in Barangay %s.", barangay);
            } else {
                notificationText = "You have joined an event.";
            }

            // Set title if the TextView exists
            if (holder.notificationTitle != null) {
                holder.notificationTitle.setText("Event Confirmation!");
            }
        } else if ("upcoming_event".equals(type)) {
            if (eventName != null && barangay != null) {
                notificationText = String.format("The %s in Barangay %s will be tomorrow. " +
                                "Gather your things and be ready for tomorrow's activity!",
                        eventName, barangay);
            } else if (eventName != null) {
                notificationText = String.format("The %s event will be tomorrow. Be ready!", eventName);
            } else if (barangay != null) {
                notificationText = String.format("An event in Barangay %s will be tomorrow.", barangay);
            } else {
                notificationText = "You have an upcoming event tomorrow. Be ready!";
            }

            // Set title if the TextView exists
            if (holder.notificationTitle != null) {
                holder.notificationTitle.setText("Upcoming Event!");
            }
        } else {
            notificationText = "Event notification";

            // Set title if the TextView exists
            if (holder.notificationTitle != null) {
                holder.notificationTitle.setText("Event Notification");
            }
        }

        holder.textNotification.setText(notificationText);
    }

    private void bindVerificationNotification(VerificationStatusViewHolder holder, Notification notification) {
        // Set the notification title if the TextView exists
        if (holder.notificationTitle != null) {
            holder.notificationTitle.setText("Account Verification");
        }

        // First, get the appropriate message for display
        String message;

        // Check for message or reason field (reason comes from web app)
        if (notification.getMessage() != null && !notification.getMessage().isEmpty()) {
            message = notification.getMessage();
        } else if (notification.getReason() != null && !notification.getReason().isEmpty()) {
            message = notification.getReason();
        } else {
            // Default messages based on status
            String status = notification.getStatus();
            if ("approved".equals(status) || "Verified".equals(status)) {
                message = "Your account is now verified! You can now join the events posted inside the application.";
            } else if ("denied".equals(status) || "Unverified".equals(status)) {
                message = "Your account verification was denied. Please contact support for more information.";
            } else {
                message = "Your account verification status has been updated.";
            }
        }

        holder.textNotification.setText(message);

        // Only show current status if we have it and for verification notifications
        if (userVerificationStatus != null && holder.currentStatusLayout != null && holder.textCurrentStatus != null) {
            holder.currentStatusLayout.setVisibility(View.VISIBLE);

            if ("Verified".equals(userVerificationStatus)) {
                holder.textCurrentStatus.setText("VERIFIED");
                setStatusBackground(holder.textCurrentStatus, "#4CAF50"); // Green
            } else {
                holder.textCurrentStatus.setText("UNVERIFIED");
                setStatusBackground(holder.textCurrentStatus, "#F44336"); // Red
            }
        } else if (holder.currentStatusLayout != null) {
            holder.currentStatusLayout.setVisibility(View.GONE);
        }
    }

    private void bindFeedbackNotification(FeedbackRequestViewHolder holder, Notification notification) {
        // Set the notification title if the TextView exists
        if (holder.notificationTitle != null) {
            holder.notificationTitle.setText("Feedback Request");
        }

        // Get the event name and barangay from the notification
        String eventName = notification.getEventName();
        String barangay = notification.getBarangay();
        String userId = notification.getUserId();
        String eventId = notification.getEventId();

        // Create the notification text
        String notificationText;
        if (eventName != null && barangay != null) {
            notificationText = String.format("Please share your feedback about the %s event held in Barangay %s.",
                    eventName, barangay);
        } else if (eventName != null) {
            notificationText = String.format("Please share your feedback about the %s event.",
                    eventName);
        } else {
            notificationText = "Please share your feedback about an event you attended.";
        }

        holder.textNotification.setText(notificationText);

        // Check if feedback already exists in Firestore before enabling the button
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String feedbackDocId = userId + "_" + eventId;

        db.collection("Feedback")
                .document(feedbackDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Feedback already submitted - disable button and change text
                        holder.btnProvideFeedback.setEnabled(false);
                        holder.btnProvideFeedback.setText("Feedback Submitted");
                        // Change button color to gray to visually indicate it's disabled
                        holder.btnProvideFeedback.setBackgroundTintList(
                                ColorStateList.valueOf(Color.parseColor("#808080")));
                    } else {
                        // No feedback yet - enable button and keep normal styling
                        holder.btnProvideFeedback.setEnabled(true);
                        holder.btnProvideFeedback.setText("Provide Feedback");
                        // Set button color back to green (using the app's color resource)
                        holder.btnProvideFeedback.setBackgroundTintList(
                                ColorStateList.valueOf(ContextCompat.getColor(context, R.color.green)));

                        // Set up the button click listener
                        holder.btnProvideFeedback.setOnClickListener(v -> {
                            // Launch the Feedback activity
                            Intent intent = new Intent(context, Feedback.class);
                            intent.putExtra("userId", notification.getUserId());
                            intent.putExtra("eventId", notification.getEventId());
                            intent.putExtra("eventName", notification.getEventName());
                            intent.putExtra("barangay", notification.getBarangay());
                            context.startActivity(intent);
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    // On error, default to enabling the button
                    holder.btnProvideFeedback.setEnabled(true);
                    holder.btnProvideFeedback.setOnClickListener(v -> {
                        // Launch the Feedback activity
                        Intent intent = new Intent(context, Feedback.class);
                        intent.putExtra("userId", notification.getUserId());
                        intent.putExtra("eventId", notification.getEventId());
                        intent.putExtra("eventName", notification.getEventName());
                        intent.putExtra("barangay", notification.getBarangay());
                        context.startActivity(intent);
                    });
                });
    }

    private void bindSkillGroupNotification(SkillGroupViewHolder holder, Notification notification) {
        // Set the title based on request status if the TextView exists
        String requestStatus = notification.getRequestStatus();

        if (holder.notificationTitle != null) {
            if ("APPROVED".equals(requestStatus)) {
                holder.notificationTitle.setText("Skill Group Approved");
            } else {
                holder.notificationTitle.setText("Skill Group Rejected");
            }
        }

        // Get the skill name and request status from the notification
        String skillName = notification.getSkillName();

        // Create the notification text
        String notificationText;
        if (skillName != null) {
            if ("APPROVED".equals(requestStatus)) {
                notificationText = "Your request to join the " + skillName + " skill group has been approved!";
                holder.textStatus.setText("APPROVED");
                setStatusBackground(holder.textStatus, "#4CAF50"); // Green
            } else {
                notificationText = "Your request to join the " + skillName + " skill group has been rejected.";
                holder.textStatus.setText("REJECTED");
                setStatusBackground(holder.textStatus, "#F44336"); // Red
            }
        } else {
            notificationText = "Skill group request status has been updated.";
            holder.textStatus.setVisibility(View.GONE);
        }

        holder.textNotification.setText(notificationText);

        // Make the status layout visible
        if (holder.statusLayout != null) {
            holder.statusLayout.setVisibility(View.VISIBLE);
        }
    }

    private void setStatusBackground(TextView textView, String colorHex) {
        if (textView == null) return;

        if (textView.getBackground() instanceof GradientDrawable) {
            GradientDrawable drawable = (GradientDrawable) textView.getBackground();
            drawable.setColor(Color.parseColor(colorHex));
        } else {
            // If background is not a GradientDrawable, set text color instead
            textView.setTextColor(Color.parseColor(colorHex));
        }
    }

    @Override
    public int getItemCount() {
        return notifications != null ? notifications.size() : 0;
    }

    static class EventNotificationViewHolder extends RecyclerView.ViewHolder {
        TextView notificationTitle;  // May be null in old layouts
        TextView textNotification;

        EventNotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            notificationTitle = itemView.findViewById(R.id.notificationTitle);  // May be null
            textNotification = itemView.findViewById(R.id.textNotification);
        }
    }

    static class VerificationStatusViewHolder extends RecyclerView.ViewHolder {
        TextView notificationTitle;  // May be null in old layouts
        TextView textNotification;
        TextView textCurrentStatus;
        LinearLayout currentStatusLayout;

        VerificationStatusViewHolder(@NonNull View itemView) {
            super(itemView);
            notificationTitle = itemView.findViewById(R.id.notificationTitle);  // May be null
            textNotification = itemView.findViewById(R.id.textNotification);
            textCurrentStatus = itemView.findViewById(R.id.textCurrentStatus);
            currentStatusLayout = itemView.findViewById(R.id.currentStatusLayout);
        }
    }

    static class FeedbackRequestViewHolder extends RecyclerView.ViewHolder {
        TextView notificationTitle;  // May be null in old layouts
        TextView textNotification;
        Button btnProvideFeedback;

        FeedbackRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            notificationTitle = itemView.findViewById(R.id.notificationTitle);  // May be null
            textNotification = itemView.findViewById(R.id.textNotification);
            btnProvideFeedback = itemView.findViewById(R.id.btnProvideFeedback);
        }
    }

    static class SkillGroupViewHolder extends RecyclerView.ViewHolder {
        TextView notificationTitle;  // May be null in old layouts
        TextView textNotification;
        TextView textStatus;
        LinearLayout statusLayout;

        SkillGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            notificationTitle = itemView.findViewById(R.id.notificationTitle);  // May be null
            textNotification = itemView.findViewById(R.id.textNotification);
            textStatus = itemView.findViewById(R.id.textSkillStatus);
            statusLayout = itemView.findViewById(R.id.skillStatusLayout);
        }
    }

    // Method to update notifications list
    public void updateNotifications(List<Notification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }
}