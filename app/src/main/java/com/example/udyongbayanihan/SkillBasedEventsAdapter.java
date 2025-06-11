package com.example.udyongbayanihan;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.graphics.text.LineBreaker;
import android.text.SpannableString;
import android.text.style.LeadingMarginSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class SkillBasedEventsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_EVENT = 0;
    private static final int TYPE_SKILL_POST = 1;
    private Context context;
    private List<EventModel> eventList;
    private String userId;
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat;
    private RecyclerView recyclerView;
    private String userVerificationStatus = "Unverified"; // Default to unverified
    private String uaddressId = "";
    private String unameId = "";
    private String uotherDetails = "";

    private static final String TAG = "SkillBasedAdapter";

    public SkillBasedEventsAdapter(Context context, List<EventModel> eventList, String userId) {
        this.context = context;
        this.eventList = eventList;
        this.userId = userId;
        this.db = FirebaseFirestore.getInstance();
        this.dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

        // Get additional user data if context is SkillsBasedEvents
        if (context instanceof SkillsBasedEvents) {
            unameId = ((SkillsBasedEvents) context).getIntent().getStringExtra("unameId");
            uaddressId = ((SkillsBasedEvents) context).getIntent().getStringExtra("uaddressId");
            uotherDetails = ((SkillsBasedEvents) context).getIntent().getStringExtra("uotherDetails");
        }

        // Check user verification status when adapter is created
        checkUserVerificationStatus();
    }

    private void checkUserVerificationStatus() {
        if (userId != null) {
            db.collection("usersAccount")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String status = documentSnapshot.getString("usersStatus");
                            if (status != null) {
                                userVerificationStatus = status;
                                notifyDataSetChanged(); // Update all items to reflect new status
                                Log.d(TAG, "User verification status: " + userVerificationStatus);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking verification status: " + e.getMessage());
                    });
        }
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;

        // Register for comment count updates
        LocalBroadcastManager.getInstance(context).registerReceiver(
                commentUpdateReceiver,
                new IntentFilter("COMMENT_COUNT_UPDATE")
        );
    }

    @Override
    public int getItemViewType(int position) {
        return eventList.get(position).isEvent() ? TYPE_EVENT : TYPE_SKILL_POST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SKILL_POST) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
            return new SkillPostViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.post, parent, false);
            return new EventViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        EventModel item = eventList.get(position);

        if (getItemViewType(position) == TYPE_SKILL_POST) {
            SkillPostViewHolder skillHolder = (SkillPostViewHolder) holder;
            bindSkillPost(skillHolder, item);
        } else {
            EventViewHolder eventHolder = (EventViewHolder) holder;
            bindEventPost(eventHolder, item);
        }
    }

    private void bindSkillPost(SkillPostViewHolder holder, EventModel post) {
        holder.adminName.setText(post.getAdminName());
        holder.position.setText(post.getPosition());
        holder.postContent.setText(post.getPostContent());
        if (post.getTimestamp() != null) {
            holder.timestamp.setText(dateFormat.format(post.getTimestamp()));
        }

        // Set up initial comments count
        db.collection("CommunityComments")
                .whereEqualTo("postId", post.getPostId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int commentCount = queryDocumentSnapshots.size();
                    holder.textCommentsNumber.setText(String.valueOf(commentCount));
                })
                .addOnFailureListener(e -> {
                    Log.e("CommentsError", "Error loading comments", e);
                    holder.textCommentsNumber.setText("0");
                });

        // Update click listener to use containerComment instead of textCommentsNumber
        holder.containerComment.setOnClickListener(v -> {
            String skillName = ((SkillsBasedEvents) context).getIntent().getStringExtra("skillName");
            if (skillName != null) {
                Intent intent = new Intent(context, SkillsBasedComments.class);
                intent.putExtra("postId", post.getPostId());
                intent.putExtra("skill", skillName);
                intent.putExtra("userId", userId);
                context.startActivity(intent);
            }
        });

        checkIfPostLiked(holder, post);
        holder.imgbtnLike.setOnClickListener(v -> toggleLike(post, holder));

        // Likes number click to view users who liked the post
        holder.textLikesNumber.setOnClickListener(v -> {
            Intent intent = new Intent(context, LikesDetails.class);
            intent.putExtra("postId", post.getPostId());
            context.startActivity(intent);
        });
    }

    private void bindEventPost(EventViewHolder holder, EventModel event) {
        if (event == null) return;

        // Organization
        holder.organization.setText(event.getOrganization() != null ?
                event.getOrganization() : "Not specified");

        // Event Name and Type
        holder.nameOfEvent.setText(event.getName() != null ?
                event.getName() : "Untitled Event");
        holder.typeOfEvent.setText(event.getType() != null ?
                event.getType() : "Not specified");

        // Date
        if (event.getDate() != null) {
            holder.date.setText(dateFormat.format(event.getDate()));
        } else {
            holder.date.setText("Date not set");
        }

        // Address
        holder.address.setText(event.getAddress() != null ?
                event.getAddress() : "Address not specified");

        // Head Coordinator
        holder.headCoordinator.setText(event.getHeadCoordinator() != null ?
                event.getHeadCoordinator() : "Not assigned");

        // Skills
        if (event.getEventSkills() != null && !event.getEventSkills().isEmpty()) {
            holder.skills.setText(String.join(", ", event.getEventSkills()));
        } else {
            holder.skills.setText("No skills specified");
        }

        // Volunteers Needed
        // Corrected to show volunteers joined / needed format
        Long volunteersJoined = 0L;
        Long volunteersNeeded = event.getVolunteersNeeded() != null ? event.getVolunteersNeeded() : 0L;

        db.collection("UserJoinEvents")
                .whereEqualTo("eventId", event.getEventId())
                .get()
                .addOnSuccessListener(joinSnapshots -> {
                    int participantsJoined = joinSnapshots.size();
                    holder.volunteersNeeded.setText(String.format("%d/%d", participantsJoined, volunteersNeeded));
                })
                .addOnFailureListener(e -> {
                    // Fallback if query fails
                    holder.volunteersNeeded.setText(String.format("0/%d", volunteersNeeded));
                });

        // Caption with indentation and justification like PostAdapter and CommunityAdapter
        if (event.getCaption() != null) {
            int indentSize = 80; // Consistent with other adapters
            SpannableString spannableCaption = new SpannableString(event.getCaption());
            spannableCaption.setSpan(new LeadingMarginSpan.Standard(indentSize, 0), 0, spannableCaption.length(), 0);
            holder.caption.setText(spannableCaption);
            holder.caption.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD);
        } else {
            holder.caption.setText("No caption");
        }

        // Handle multiple images
        setupImages(holder, event);

        // Setup join button with proper state
        checkUserJoinStatus(userId, event.getEventId(), holder.joinEvent, event);
    }

    private void checkUserJoinStatus(String userId, String eventId, Button joinButton, EventModel event) {
        Log.d(TAG, "Checking join status for event: " + eventId);

        // First check if the user has joined the event
        db.collection("UserJoinEvents")
                .document(userId + "_" + eventId)
                .get()
                .addOnSuccessListener(joinDoc -> {
                    if (joinDoc.exists()) {
                        // User has already joined the event
                        Log.d(TAG, "User has already joined the event");

                        // Check if the event has feedback posted
                        db.collection("EventInformation")
                                .document(eventId)
                                .get()
                                .addOnSuccessListener(eventInfoDoc -> {
                                    if (eventInfoDoc.exists()) {
                                        Boolean postFeedback = eventInfoDoc.getBoolean("postFeedback");

                                        if (Boolean.TRUE.equals(postFeedback)) {
                                            // Check if user has already submitted feedback
                                            db.collection("Feedback")
                                                    .document(userId + "_" + eventId)
                                                    .get()
                                                    .addOnSuccessListener(feedbackDoc -> {
                                                        if (feedbackDoc.exists()) {
                                                            // User has already submitted feedback
                                                            joinButton.setText("Event Ended");
                                                            joinButton.setEnabled(false);
                                                            // Set to gray color for ended events
                                                            joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray));
                                                            Log.d(TAG, "User has submitted feedback");
                                                        } else {
                                                            // Show Answer Feedback button
                                                            joinButton.setText("Answer Feedback");
                                                            joinButton.setEnabled(true);
                                                            // Set to yellow color for feedback
                                                            joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.yellow));
                                                            Log.d(TAG, "User needs to submit feedback");

                                                            joinButton.setOnClickListener(v -> {
                                                                Intent intent = new Intent(context, Feedback.class);
                                                                intent.putExtra("userId", userId);
                                                                intent.putExtra("eventId", eventId);
                                                                intent.putExtra("eventName", event.getName());
                                                                context.startActivity(intent);
                                                            });
                                                        }
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e(TAG, "Error checking feedback: " + e.getMessage());
                                                        joinButton.setText("Already Joined");
                                                        joinButton.setEnabled(false);
                                                        // Set to blue color for already joined
                                                        joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.blue));
                                                    });
                                        } else {
                                            // No feedback required, check event date status
                                            handleEventDateCheck(event, joinButton);
                                        }
                                    } else {
                                        joinButton.setText("Already Joined");
                                        joinButton.setEnabled(false);
                                        // Set to blue color for already joined
                                        joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.blue));
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error checking event info: " + e.getMessage());
                                    joinButton.setText("Already Joined");
                                    joinButton.setEnabled(false);
                                    // Set to blue color for already joined
                                    joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.blue));
                                });
                    } else {
                        // User hasn't joined the event
                        handleNonJoinedUserState(event, joinButton, userId, eventId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking join status: " + e.getMessage());
                    joinButton.setText("Join Event");
                    joinButton.setEnabled(true);
                    // Keep default green color for Join Event
                    joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.green));
                });
    }

    private void handleEventDateCheck(EventModel event, Button joinButton) {
        if (event.getDate() != null) {
            Calendar eventDate = Calendar.getInstance();
            eventDate.setTime(event.getDate());

            Calendar now = Calendar.getInstance();

            // Set time to beginning of day for comparison
            Calendar eventDay = Calendar.getInstance();
            eventDay.setTime(event.getDate());
            eventDay.set(Calendar.HOUR_OF_DAY, 0);
            eventDay.set(Calendar.MINUTE, 0);
            eventDay.set(Calendar.SECOND, 0);

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);

            if (now.after(eventDate)) {
                // Event date has passed
                joinButton.setText("Event Ended");
                joinButton.setEnabled(false);
                // Set to gray color for ended events
                joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray));
                Log.d(TAG, "Event has ended");
            } else if (eventDay.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    eventDay.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                // Event is today
                joinButton.setText("Ongoing");
                joinButton.setEnabled(false);
                // Set to yellow color for ongoing events
                joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.yellow));
                Log.d(TAG, "Event is ongoing today");
            } else {
                // Event is in the future and user is already joined
                joinButton.setText("Already Joined");
                joinButton.setEnabled(false);
                // Set to blue color for already joined
                joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.blue));
                Log.d(TAG, "User already joined future event");
            }
        } else {
            // No date set
            joinButton.setText("Already Joined");
            joinButton.setEnabled(false);
            // Set to blue color for already joined
            joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.blue));
        }
    }

    private void handleNonJoinedUserState(EventModel event, Button joinButton, String userId, String eventId) {
        if (event.getDate() != null) {
            Calendar eventDate = Calendar.getInstance();
            eventDate.setTime(event.getDate());

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);

            Calendar eventDay = Calendar.getInstance();
            eventDay.setTime(event.getDate());
            eventDay.set(Calendar.HOUR_OF_DAY, 0);
            eventDay.set(Calendar.MINUTE, 0);
            eventDay.set(Calendar.SECOND, 0);

            Calendar now = Calendar.getInstance();

            if (now.after(eventDate)) {
                // Event is in the past
                joinButton.setText("Event Ended");
                joinButton.setEnabled(false);
                // Set to gray color for ended events
                joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray));
                Log.d(TAG, "Event has ended, user did not join");
            } else if (eventDay.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    eventDay.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                // Event is today
                joinButton.setText("Ongoing");
                joinButton.setEnabled(false);
                // Set to yellow color for ongoing events
                joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.yellow));
                Log.d(TAG, "Event is ongoing today, user did not join");
            } else {
                // Event is in the future
                Long volunteersNeeded = event.getVolunteersNeeded();
                // Check volunteers count
                db.collection("UserJoinEvents")
                        .whereEqualTo("eventId", eventId)
                        .get()
                        .addOnSuccessListener(joinSnapshots -> {
                            int participantsJoined = joinSnapshots.size();

                            if (volunteersNeeded != null && participantsJoined >= volunteersNeeded) {
                                // Event is full
                                joinButton.setText("Full");
                                joinButton.setEnabled(false);
                                // Set to gray color for full events
                                joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray));
                                Log.d(TAG, "Event is full");
                            } else {
                                // Check user verification status
                                if (userVerificationStatus.equals("Unverified")) {
                                    joinButton.setText("Verification Required");
                                    joinButton.setEnabled(true);
                                    // Set to gray color for verification required
                                    joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray));
                                    Log.d(TAG, "User needs verification");

                                    joinButton.setOnClickListener(v -> {
                                        new AlertDialog.Builder(context)
                                                .setTitle("Verification Required")
                                                .setMessage("Your account is not yet verified. Please wait for admin verification before joining events.")
                                                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                                .show();
                                    });
                                } else {
                                    // User can join the event
                                    joinButton.setText("Join Event");
                                    joinButton.setEnabled(true);
                                    // Set to green color for Join Event
                                    joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.green));
                                    Log.d(TAG, "User can join event");

                                    joinButton.setOnClickListener(v -> {
                                        joinButton.setEnabled(false); // Prevent double clicks
                                        joinButton.setText("Joining...");
                                        joinEvent(event.getEventId(), event.getName());
                                    });
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error checking participants: " + e.getMessage());
                            joinButton.setText("Join Event");
                            joinButton.setEnabled(true);
                            // Set to green color for Join Event
                            joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.green));
                        });
            }
        } else {
            // No date set
            joinButton.setText("Join Event");
            joinButton.setEnabled(true);
            // Set to green color for Join Event
            joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.green));
        }
    }

    // Method to handle the joining of an event
    private void joinEvent(String eventId, String eventName) {
        Log.d(TAG, "Joining event: " + eventId + " - " + eventName);

        // Check if the user already joined this event
        db.collection("UserJoinEvents")
                .document(userId + "_" + eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "User already joined the event.");
                        Toast.makeText(context, "You have already joined this event.", Toast.LENGTH_SHORT).show();
                        notifyDataSetChanged(); // Refresh to show already joined status
                    } else {
                        // Get the current count of participants first
                        db.collection("UserJoinEvents")
                                .whereEqualTo("eventId", eventId)
                                .get()
                                .addOnSuccessListener(joinSnapshots -> {
                                    int currentParticipants = joinSnapshots.size();

                                    // Check event details and volunteer limit
                                    db.collection("EventDetails")
                                            .document(eventId)
                                            .get()
                                            .addOnSuccessListener(eventDoc -> {
                                                if (eventDoc.exists()) {
                                                    Long volunteerNeeded = eventDoc.getLong("volunteerNeeded");
                                                    if (volunteerNeeded == null) volunteerNeeded = 0L;

                                                    if (currentParticipants < volunteerNeeded) {
                                                        // Prepare join data
                                                        Map<String, Object> joinData = new HashMap<>();
                                                        joinData.put("userId", userId);
                                                        joinData.put("eventId", eventId);
                                                        joinData.put("eventName", eventName);
                                                        joinData.put("timestamp", FieldValue.serverTimestamp());

                                                        // Use a transaction to ensure atomic updates
                                                        Long finalVolunteerNeeded = volunteerNeeded;
                                                        db.runTransaction(transaction -> {
                                                            // Add user to UserJoinEvents
                                                            transaction.set(
                                                                    db.collection("UserJoinEvents").document(userId + "_" + eventId),
                                                                    joinData
                                                            );

                                                            // Update participant count in both collections
                                                            transaction.update(
                                                                    db.collection("EventDetails").document(eventId),
                                                                    "participantsJoined", currentParticipants + 1
                                                            );
                                                            transaction.update(
                                                                    db.collection("EventInformation").document(eventId),
                                                                    "participantsJoined", currentParticipants + 1
                                                            );

                                                            return null;
                                                        }).addOnSuccessListener(aVoid -> {
                                                            // Get barangay info for notification
                                                            String barangay = eventDoc.getString("barangay");
                                                            createJoinEventNotification(eventId, eventName, barangay);

                                                            Toast.makeText(context, "You have successfully joined the event!", Toast.LENGTH_SHORT).show();
                                                            notifyDataSetChanged(); // Refresh to show already joined status
                                                        }).addOnFailureListener(e -> {
                                                            Log.e(TAG, "Transaction failed: " + e.getMessage());
                                                            Toast.makeText(context, "Failed to join event. Please try again.", Toast.LENGTH_SHORT).show();
                                                            notifyDataSetChanged(); // Refresh UI
                                                        });
                                                    } else {
                                                        Toast.makeText(context, "This event is already full.", Toast.LENGTH_SHORT).show();
                                                        notifyDataSetChanged(); // Refresh to show full status
                                                    }
                                                }
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error checking participants: " + e.getMessage());
                                    Toast.makeText(context, "Failed to join event. Please try again.", Toast.LENGTH_SHORT).show();
                                    notifyDataSetChanged(); // Refresh UI
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking join status: " + e.getMessage());
                    Toast.makeText(context, "Failed to join event. Please try again.", Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged(); // Refresh UI
                });
    }

    // Updated notification method to use Notification class and NotificationWorkManager
    private void createJoinEventNotification(String eventId, String eventName, String barangay) {
        if (userId != null && eventId != null && eventName != null && barangay != null) {
            // Create notification object using the Notification class with "event_confirmation" type
            Notification notification = new Notification(userId, eventId, eventName, barangay, "event_confirmation");
            String notificationId = userId + "_" + eventId + "_event_confirmation";

            db.collection("Notifications")
                    .document(notificationId)
                    .set(notification)
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "Join notification created with ID: " + notificationId))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Error creating join notification", e));

            // Send immediate notification via work manager
            NotificationWorkManager.notifyEventJoined(
                    context, userId, eventId, eventName, barangay,
                    uaddressId, unameId, uotherDetails
            );

            // Check if the event is tomorrow and create upcoming notification
            db.collection("EventDetails")
                    .document(eventId)
                    .get()
                    .addOnSuccessListener(eventDoc -> {
                        if (eventDoc.exists()) {
                            Timestamp eventDate = eventDoc.getTimestamp("date");
                            if (eventDate != null) {
                                Calendar calendar = Calendar.getInstance();
                                calendar.add(Calendar.DAY_OF_YEAR, 1);
                                calendar.set(Calendar.HOUR_OF_DAY, 0);
                                calendar.set(Calendar.MINUTE, 0);
                                calendar.set(Calendar.SECOND, 0);
                                Date tomorrowStart = calendar.getTime();

                                calendar.set(Calendar.HOUR_OF_DAY, 23);
                                calendar.set(Calendar.MINUTE, 59);
                                calendar.set(Calendar.SECOND, 59);
                                Date tomorrowEnd = calendar.getTime();

                                Date eventDateTime = eventDate.toDate();
                                if (eventDateTime.after(tomorrowStart) && eventDateTime.before(tomorrowEnd)) {
                                    // Create upcoming event notification
                                    String upcomingId = userId + "_" + eventId + "_upcoming_event";
                                    Notification upcomingNotification = new Notification(
                                            userId, eventId, eventName, barangay, "upcoming_event");

                                    db.collection("Notifications")
                                            .document(upcomingId)
                                            .set(upcomingNotification)
                                            .addOnSuccessListener(aVoid2 ->
                                                    Log.d(TAG, "Upcoming notification created with ID: " + upcomingId))
                                            .addOnFailureListener(e ->
                                                    Log.e(TAG, "Error creating upcoming notification", e));
                                }
                            }
                        }
                    });
        }
    }

    private void setupImages(EventViewHolder holder, EventModel event) {
        List<String> imageUrls = new ArrayList<>();

        // Handle the case where imageUrl might be different formats
        if (event.getImageUrls() != null && !event.getImageUrls().isEmpty()) {
            // If event has a getImageUrls method that returns a list (like in other adapters)
            imageUrls.addAll(event.getImageUrls());
        } else if (event.getImageUrl() instanceof String) {
            // Single image URL
            String imageUrl = (String) event.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                imageUrls.add(imageUrl);
            }
        } else if (event.getImageUrl() instanceof List) {
            // Multiple image URLs
            imageUrls.addAll((List<String>) event.getImageUrl());
        }

        if (!imageUrls.isEmpty()) {
            holder.posterRecyclerView.setVisibility(View.VISIBLE);

            // Set up horizontal layout manager
            LinearLayoutManager layoutManager = new LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL,
                    false
            );
            holder.posterRecyclerView.setLayoutManager(layoutManager);

            // Create and set adapter
            ImagesAdapter imageAdapter = new ImagesAdapter(context, imageUrls);
            holder.posterRecyclerView.setAdapter(imageAdapter);

            // Add item decoration for spacing if needed
            int spacingInPixels = context.getResources().getDimensionPixelSize(R.dimen.image_spacing);
            holder.posterRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                           @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                    outRect.right = spacingInPixels;
                }
            });
        } else {
            holder.posterRecyclerView.setVisibility(View.GONE);
        }
    }

    public void updateCommentsCount(String postId, int count) {
        for (int i = 0; i < eventList.size(); i++) {
            EventModel event = eventList.get(i);
            if (!event.isEvent() && event.getPostId() != null && event.getPostId().equals(postId)) {
                RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(i);
                if (holder instanceof SkillPostViewHolder) {
                    SkillPostViewHolder postHolder = (SkillPostViewHolder) holder;
                    postHolder.textCommentsNumber.setText(String.valueOf(count));
                }
                break;
            }
        }
    }

    private BroadcastReceiver commentUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String postId = intent.getStringExtra("postId");
            int commentsCount = intent.getIntExtra("commentsCount", 0);
            updateCommentsCount(postId, commentsCount);
        }
    };

    private void checkIfPostLiked(SkillPostViewHolder holder, EventModel post) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("CommunityLikes")
                .whereEqualTo("postId", post.getPostId())
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    boolean isLiked = !queryDocumentSnapshots.isEmpty();
                    holder.imgbtnLike.setImageResource(
                            isLiked ? R.drawable.liked : R.drawable.like
                    );

                    // Fetch and set total likes count
                    fetchLikesCount(holder, post.getPostId());
                });
    }

    private void fetchLikesCount(SkillPostViewHolder holder, String postId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("CommunityLikes")
                .whereEqualTo("postId", postId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int likesCount = queryDocumentSnapshots.size();
                    holder.textLikesNumber.setText(String.valueOf(likesCount));
                });
    }

    private void toggleLike(EventModel post, SkillPostViewHolder holder) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Check if the post is already liked
        db.collection("CommunityLikes")
                .whereEqualTo("postId", post.getPostId())
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Post is not liked, so like it
                        likePost(post, holder);
                    } else {
                        // Post is already liked, so unlike it
                        unlikePost(post, holder, queryDocumentSnapshots.getDocuments().get(0).getId());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error checking like status", Toast.LENGTH_SHORT).show();
                });
    }

    private void likePost(EventModel post, SkillPostViewHolder holder) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create a like document
        Map<String, Object> likeData = new HashMap<>();
        likeData.put("userId", userId);
        likeData.put("postId", post.getPostId());
        likeData.put("timestamp", Timestamp.now());

        db.collection("CommunityLikes")
                .add(likeData)
                .addOnSuccessListener(documentReference -> {
                    // Update UI to show liked state
                    holder.imgbtnLike.setImageResource(R.drawable.liked);

                    // Refresh likes count
                    fetchLikesCount(holder, post.getPostId());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to like post", Toast.LENGTH_SHORT).show();
                });
    }

    private void unlikePost(EventModel post, SkillPostViewHolder holder, String likeDocumentId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Delete the like document
        db.collection("CommunityLikes")
                .document(likeDocumentId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Update UI to show unliked state
                    holder.imgbtnLike.setImageResource(R.drawable.like);

                    // Refresh likes count
                    fetchLikesCount(holder, post.getPostId());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to unlike post", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return eventList != null ? eventList.size() : 0;
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView nameOfEvent, typeOfEvent, organization, address, date;
        TextView headCoordinator, skills, caption, volunteersNeeded;
        Button joinEvent;
        RecyclerView posterRecyclerView;
        ImageView imgUserPost;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            imgUserPost = itemView.findViewById(R.id.imgUserPost);
            nameOfEvent = itemView.findViewById(R.id.nameOfEvent);
            typeOfEvent = itemView.findViewById(R.id.typeOfEvent);
            organization = itemView.findViewById(R.id.organization);
            address = itemView.findViewById(R.id.address);
            date = itemView.findViewById(R.id.date);
            headCoordinator = itemView.findViewById(R.id.headCoordinator);
            skills = itemView.findViewById(R.id.skills);
            caption = itemView.findViewById(R.id.caption);
            volunteersNeeded = itemView.findViewById(R.id.volunteersNeeded);
            posterRecyclerView = itemView.findViewById(R.id.posterRecyclerView);
            joinEvent = itemView.findViewById(R.id.joinEvent);
        }
    }

    static class SkillPostViewHolder extends RecyclerView.ViewHolder {
        TextView adminName, position, postContent, timestamp, textCommentsNumber, textLikesNumber;
        ImageButton imgbtnComment, imgbtnLike;
        RelativeLayout containerComment;

        SkillPostViewHolder(@NonNull View itemView) {
            super(itemView);
            adminName = itemView.findViewById(R.id.adminName);
            position = itemView.findViewById(R.id.position);
            postContent = itemView.findViewById(R.id.postContent);
            timestamp = itemView.findViewById(R.id.timestamp);
            textCommentsNumber = itemView.findViewById(R.id.textCommentsNumber);
            textLikesNumber = itemView.findViewById(R.id.textLikesNumber);
            imgbtnLike = itemView.findViewById(R.id.imgbtnLike);
            containerComment = itemView.findViewById(R.id.containerComment);
        }
    }

    // Method to allow external updates to verification status
    public void updateVerificationStatus(String status) {
        this.userVerificationStatus = status;
        notifyDataSetChanged();
    }
}