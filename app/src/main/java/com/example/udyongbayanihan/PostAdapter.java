package com.example.udyongbayanihan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.text.LineBreaker;
import android.text.SpannableString;
import android.text.style.LeadingMarginSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.MyViewHolder> {

    Context context;
    ArrayList<Post> list;
    VolunteerCountListener volunteerCountListener;
    private int participantsJoined = 0;
    private String userId;
    private String userVerificationStatus;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Date formatter
    private final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

    public PostAdapter(Context context, ArrayList<Post> list, VolunteerCountListener listener, String userId) {
        this.context = context;
        this.list = list;
        this.volunteerCountListener = listener;
        this.userId = userId;
        this.userVerificationStatus = "Unverified"; // Default value
    }

    public PostAdapter(Context context, ArrayList<Post> list, VolunteerCountListener listener, String userId, String verificationStatus) {
        this.context = context;
        this.list = list;
        this.volunteerCountListener = listener;
        this.userId = userId;
        this.userVerificationStatus = verificationStatus;
    }

    public void updateVerificationStatus(String status) {
        this.userVerificationStatus = status;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.post, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Post post = list.get(position);
        if (post == null) return;

        holder.organization.setText(post.getOrganizations());
        holder.headCoordinator.setText(post.getHeadCoordinator());
        holder.skills.setText(post.getEventSkills() != null ?
                String.join(", ", post.getEventSkills()) : "No Skills Required");

        holder.nameOfEvent.setText(post.getNameOfEvent());
        holder.volunteersNeeded.setText(String.format("%d/%d",
                post.getParticipantsJoined(),
                post.getVolunteerNeeded()));  // Ensure volunteerNeeded is used here
        holder.typeOfEvent.setText(post.getTypeOfEvent());
        holder.address.setText(post.getBarangay() != null ? post.getBarangay() : "No Barangay Specified");
        if (post.getDate() != null) {
            holder.date.setText(sdf.format(post.getDate().toDate()));
        }
        // Set up first-line indention for caption
        int indentSize = 80; // Adjust as needed
        SpannableString spannableCaption = new SpannableString(post.getCaption());
        spannableCaption.setSpan(new LeadingMarginSpan.Standard(indentSize, 0), 0, spannableCaption.length(), 0);
        holder.caption.setText(spannableCaption);
        holder.caption.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD);

        // Check if user has already joined this event
        String userId = ((Home) context).userId;
        checkUserJoinStatus(userId, post.getEventId(), holder.joinEvent, post);

        List<String> imageUrls = post.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
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

    private void checkUserJoinStatus(String userId, String eventId, Button joinButton, Post post) {
        // First check if the user has joined the event
        db.collection("UserJoinEvents")
                .document(userId + "_" + eventId)
                .get()
                .addOnSuccessListener(joinDoc -> {
                    if (joinDoc.exists()) {
                        // User has joined the event
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
                                                        } else {
                                                            // Show Answer Feedback button
                                                            joinButton.setText("Answer Feedback");
                                                            joinButton.setEnabled(true);
                                                            // Set to yellow color for feedback
                                                            joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.yellow));
                                                            joinButton.setOnClickListener(v -> {
                                                                if (context instanceof Home) {
                                                                    Intent intent = new Intent(context, Feedback.class);
                                                                    intent.putExtra("userId", userId);
                                                                    intent.putExtra("eventId", eventId);
                                                                    intent.putExtra("eventName", post.getNameOfEvent());

                                                                    // Use startActivityForResult instead of startActivity
                                                                    ((Home) context).startActivityForResult(intent, Home.FEEDBACK_REQUEST_CODE);
                                                                    // Store this eventId temporarily to identify which button to update
                                                                    ((Home) context).setCurrentFeedbackEventId(eventId);
                                                                }
                                                            });
                                                        }
                                                    });
                                        } else {
                                            // Check event date status
                                            handleEventDateCheck(post, joinButton, userId, eventId);
                                        }
                                    }
                                });
                    } else {
                        // User hasn't joined the event
                        handleNonJoinedUserState(post, joinButton, userId, eventId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error checking join status: " + e.getMessage());
                    joinButton.setText("Join Event");
                    joinButton.setEnabled(true);
                    // Keep default green color for Join Event
                    joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.green));
                });
    }

    private void handleEventDateCheck(Post post, Button joinButton, String userId, String eventId) {
        if (post.getDate() != null) {
            Calendar eventDate = Calendar.getInstance();
            eventDate.setTime(post.getDate().toDate());

            Calendar weekAfterEvent = Calendar.getInstance();
            weekAfterEvent.setTime(post.getDate().toDate());
            weekAfterEvent.add(Calendar.DAY_OF_YEAR, 7);

            Calendar now = Calendar.getInstance();

            // Set time to beginning of day for comparison
            Calendar eventDay = Calendar.getInstance();
            eventDay.setTime(post.getDate().toDate());
            eventDay.set(Calendar.HOUR_OF_DAY, 0);
            eventDay.set(Calendar.MINUTE, 0);
            eventDay.set(Calendar.SECOND, 0);

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);

            if (now.after(weekAfterEvent)) {
                joinButton.setText("Event Ended");
                joinButton.setEnabled(false);
                // Set to gray color for ended events
                joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray));
            } else if (now.after(eventDate)) {
                joinButton.setText("Event Ended");
                joinButton.setEnabled(false);
                // Set to gray color for ended events
                joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray));
            } else if (eventDay.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    eventDay.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                // Event is today
                joinButton.setText("Ongoing");
                joinButton.setEnabled(false);
                // Set to yellow color for ongoing events
                joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.yellow));
            } else {
                joinButton.setText("Already Joined");
                joinButton.setEnabled(false);
                // Set to blue color for already joined
                joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.blue));
            }
        }
    }

    private void handleNonJoinedUserState(Post post, Button joinButton, String userId, String eventId) {
        if (post.getDate() != null) {
            Calendar eventDate = Calendar.getInstance();
            eventDate.setTime(post.getDate().toDate());

            // Set time to beginning of day for comparison
            Calendar eventDay = Calendar.getInstance();
            eventDay.setTime(post.getDate().toDate());
            eventDay.set(Calendar.HOUR_OF_DAY, 0);
            eventDay.set(Calendar.MINUTE, 0);
            eventDay.set(Calendar.SECOND, 0);

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);

            Calendar now = Calendar.getInstance();

            if (now.after(eventDate)) {
                joinButton.setText("Event Ended");
                joinButton.setEnabled(false);
                // Set to gray color for ended events
                joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray));
            } else if (eventDay.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    eventDay.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                // Event is today
                joinButton.setText("Ongoing");
                joinButton.setEnabled(false);
                // Set to yellow color for ongoing events
                joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.yellow));
            } else {
                if (post.getParticipantsJoined() >= post.getVolunteerNeeded()) {
                    joinButton.setText("Full");
                    joinButton.setEnabled(false);
                    // Set to gray color for full events
                    joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray));
                } else {
                    if (userVerificationStatus.equals("Unverified")) {
                        joinButton.setText("Verification Required");
                        joinButton.setEnabled(true);
                        // Set to gray color for verification required
                        joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray));
                        joinButton.setOnClickListener(v -> {
                            new AlertDialog.Builder(context)
                                    .setTitle("Verification Required")
                                    .setMessage("Your account is not yet verified. Please wait for admin verification before joining events.")
                                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                    .show();
                        });
                    } else {
                        joinButton.setText("Join Event");
                        joinButton.setEnabled(true);
                        // Keep default green color for Join Event
                        joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.green));
                        joinButton.setOnClickListener(v -> {
                            if (context instanceof Home) {
                                joinButton.setEnabled(false); // Prevent double clicks
                                ((Home) context).joinEvent(post.getEventId(), post.getNameOfEvent());
                            }
                        });
                    }
                }
            }
        }
    }

    private void checkPostFeedbackStatus(String eventId, String userId, Button joinButton, Post post) {
        db.collection("EventInformation")
                .document(eventId)
                .get()
                .addOnSuccessListener(eventInfoDoc -> {
                    if (eventInfoDoc.exists()) {
                        Boolean postFeedback = eventInfoDoc.getBoolean("postFeedback");
                        if (Boolean.TRUE.equals(postFeedback)) {
                            joinButton.setText("Answer Feedback");
                            joinButton.setEnabled(true);
                            // Set to yellow color for feedback
                            joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.yellow));
                            joinButton.setOnClickListener(v -> {
                                if (context instanceof Home) {
                                    Intent intent = new Intent(context, Feedback.class);
                                    intent.putExtra("userId", userId);
                                    intent.putExtra("eventId", eventId);
                                    intent.putExtra("eventName", post.getNameOfEvent());

                                    // Use startActivityForResult instead of startActivity
                                    ((Home) context).startActivityForResult(intent, Home.FEEDBACK_REQUEST_CODE);
                                    // Store this eventId temporarily to identify which button to update
                                    ((Home) context).setCurrentFeedbackEventId(eventId);
                                }
                            });
                        } else {
                            joinButton.setText("Event Ended");
                            joinButton.setEnabled(false);
                            // Set to gray color for ended events
                            joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray));
                        }
                    }
                });
    }

    private void updateJoinStatus(String userId, String eventId, boolean isJoined) {
        Map<String, Object> userJoin = new HashMap<>();
        userJoin.put("userId", userId);
        userJoin.put("eventId", eventId);
        userJoin.put("isJoined", isJoined);

        db.collection("UserJoinEvents")
                .document(userId + "_" + eventId) // Unique document ID for each user-event pair
                .set(userJoin)
                .addOnSuccessListener(aVoid -> Log.d("FirestoreSuccess", "Join status updated"))
                .addOnFailureListener(e -> Log.e("FirestoreError", "Error updating join status: " + e.getMessage()));
    }

    public interface VolunteerCountListener {
        void getVolunteersJoined(String eventName, CountCallback callback);
    }

    public interface CountCallback {
        void onCountFetched(int count);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        public Button joinEvent;
        public TextView nameOfEvent, typeOfEvent, organization, address, date, headCoordinator, skills, caption, volunteersNeeded;
        public ImageView poster;
        public RecyclerView posterRecyclerView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

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
    public void updateVolunteersCount(String eventName, String updatedCount) {
        try {
            for (int i = 0; i < list.size(); i++) {
                Post post = list.get(i);
                if (post.getNameOfEvent().equals(eventName)) {
                    String[] parts = updatedCount.split("/");
                    if (parts.length == 2) {
                        int currentCount = Integer.parseInt(parts[0].trim());
                        int neededCount = Integer.parseInt(parts[1].trim());
                        post.setParticipantsJoined(currentCount);
                        post.setVolunteerNeeded(neededCount);
                        notifyItemChanged(i);
                    }
                    break;
                }
            }
        } catch (NumberFormatException e) {
            Log.e("PostAdapter", "Error parsing volunteer counts: " + e.getMessage());
        }
    }

    private void setupJoinStatusListener(String userId, String eventId, Button joinButton, Post post) {
        db.collection("UserJoinEvents")
                .document(userId + "_" + eventId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.e("FirestoreError", "Error listening to join status: " + e.getMessage());
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        joinButton.setText("Already Joined");
                        joinButton.setEnabled(false);
                    } else {
                        if (post.getParticipantsJoined() >= post.getVolunteerNeeded()) {
                            joinButton.setText("Full");
                            joinButton.setEnabled(false);
                        } else {
                            if (userVerificationStatus.equals("Unverified")) {
                                joinButton.setText("Verification Required");
                                joinButton.setEnabled(true);
                            } else {
                                joinButton.setText("Join Event");
                                joinButton.setEnabled(true);
                            }
                        }
                    }
                });
    }
}