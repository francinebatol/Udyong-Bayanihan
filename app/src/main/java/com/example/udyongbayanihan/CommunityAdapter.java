package com.example.udyongbayanihan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.text.LineBreaker;
import android.text.SpannableString;
import android.text.style.LeadingMarginSpan;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CommunityAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_EVENT = 0;
    private static final int TYPE_COMMUNITY_POST = 1;
    private Context context;
    private ArrayList<Post> posts;
    private VolunteerCountListener volunteerCountListener;
    private String userId;
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat;
    private RecyclerView recyclerView;
    private String userVerificationStatus = "Unverified"; // Default to unverified

    private static final String TAG = "CommunityAdapter";

    public CommunityAdapter(Context context, ArrayList<Post> posts, VolunteerCountListener listener, String userId) {
        this.context = context;
        this.posts = posts;
        this.volunteerCountListener = listener;
        this.userId = userId;
        this.db = FirebaseFirestore.getInstance();
        this.dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

        // Check user verification status when adapter is created
        checkUserVerificationStatus();
    }

    /**
     * Checks the user's verification status from Firestore
     */
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
                                Log.d(TAG, "User verification status: " + userVerificationStatus);
                                notifyDataSetChanged(); // Refresh all items to reflect new status
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking verification status: " + e.getMessage());
                    });
        }
    }

    @Override
    public int getItemViewType(int position) {
        return posts.get(position).isCommunityPost() ? TYPE_COMMUNITY_POST : TYPE_EVENT;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_COMMUNITY_POST) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
            return new CommunityPostViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.post, parent, false);
            return new CommunityViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Post post = posts.get(position);

        if (getItemViewType(position) == TYPE_COMMUNITY_POST) {
            CommunityPostViewHolder communityHolder = (CommunityPostViewHolder) holder;
            bindCommunityPost(communityHolder, post);
        } else {
            CommunityViewHolder eventHolder = (CommunityViewHolder) holder;
            bindEventPost(eventHolder, post);
        }
    }

    private void bindCommunityPost(CommunityPostViewHolder holder, Post post) {
        holder.adminName.setText(post.getAdminName());
        holder.position.setText(post.getPosition());
        holder.postContent.setText(post.getPostContent());
        if (post.getPostTimestamp() != null) {
            holder.timestamp.setText(dateFormat.format(post.getPostTimestamp().toDate()));
        }

        // Load comment count
        db.collection("CommunityComments")
                .whereEqualTo("postId", post.getPostId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int commentCount = queryDocumentSnapshots.size();
                    holder.textCommentsNumber.setText(String.valueOf(commentCount));
                })
                .addOnFailureListener(e -> {
                    Log.e("CommentsError", "Error loading comments count", e);
                    holder.textCommentsNumber.setText("0");
                });

        // Set click listener on the container view
        holder.containerComment.setOnClickListener(v -> {
            fetchAndPassUserData(post.getPostId());
        });

        fetchLikesForPost(post, holder);

        holder.imgbtnLike.setOnClickListener(v -> {
            toggleLike(post, holder);
        });

        // Setup likes number click listener
        holder.textLikesNumber.setOnClickListener(v -> {
            openLikesDetailsActivity(post);
        });

        // Setup real-time comment listener
        setupCommentListener(post.getPostId(), holder);
    }

    private void setupCommentListener(String postId, CommunityPostViewHolder holder) {
        db.collection("CommunityComments")
                .whereEqualTo("postId", postId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("CommentsError", "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        int commentCount = value.size();
                        holder.textCommentsNumber.setText(String.valueOf(commentCount));
                    }
                });
    }

    private void fetchLikesForPost(Post post, CommunityPostViewHolder holder) {
        db.collection("CommunityLikes")
                .whereEqualTo("postId", post.getPostId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int likesCount = queryDocumentSnapshots.size();
                    post.setLikesCount(likesCount);
                    holder.textLikesNumber.setText(String.valueOf(likesCount));

                    // Check if current user has liked the post
                    boolean isLikedByUser = queryDocumentSnapshots.getDocuments().stream()
                            .anyMatch(doc -> doc.getString("userId").equals(userId));
                    post.setLikedByCurrentUser(isLikedByUser);

                    // Update like button image
                    updateLikeButtonImage(holder, isLikedByUser);
                })
                .addOnFailureListener(e -> {
                    Log.e("LikesError", "Error fetching likes", e);
                });
    }

    private void toggleLike(Post post, CommunityPostViewHolder holder) {
        if (context instanceof CommunityGroupLocationBased) {
            ((CommunityGroupLocationBased) context).toggleLikeForPost(post, holder);
        }
    }

    private void updateLikeButtonImage(CommunityPostViewHolder holder, boolean isLiked) {
        holder.imgbtnLike.setImageResource(
                isLiked ? R.drawable.liked : R.drawable.like
        );
    }

    private void openLikesDetailsActivity(Post post) {
        Intent intent = new Intent(context, LikesDetails.class);
        intent.putExtra("postId", post.getPostId());
        context.startActivity(intent);
    }

    private void fetchAndPassUserData(String postId) {
        db.collection("CommunityGroups")
                .get()
                .addOnSuccessListener(barangaySnapshot -> {
                    // Track if we found the post
                    boolean postFound = false;

                    for (DocumentSnapshot barangayDoc : barangaySnapshot.getDocuments()) {
                        barangayDoc.getReference()
                                .collection("Posts")
                                .document(postId)
                                .get()
                                .addOnSuccessListener(postSnapshot -> {
                                    if (postSnapshot.exists()) {
                                        String barangay = barangayDoc.getId();

                                        // Get user details and launch activity
                                        db.collection("usersName").document(userId).get()
                                                .addOnSuccessListener(nameSnapshot -> {
                                                    String firstName = nameSnapshot.getString("firstName");
                                                    String lastName = nameSnapshot.getString("lastName");
                                                    String fullName = firstName + " " + lastName;

                                                    db.collection("usersAddress").document(userId).get()
                                                            .addOnSuccessListener(addressSnapshot -> {
                                                                String houseNo = addressSnapshot.getString("houseNo");
                                                                String street = addressSnapshot.getString("street");
                                                                String userBarangay = addressSnapshot.getString("barangay");

                                                                Intent intent = new Intent(context, SeeComments.class);
                                                                intent.putExtra("postId", postId);
                                                                intent.putExtra("barangay", barangay);
                                                                intent.putExtra("userId", userId);
                                                                intent.putExtra("fullName", fullName);
                                                                intent.putExtra("address", houseNo + " " + street + ", " + userBarangay);

                                                                context.startActivity(intent);
                                                            });
                                                });
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> Log.e("PostError", "Error finding post", e));
    }

    public void startListeningForCommentUpdates(String postId) {
        if (recyclerView == null) return;

        db.collection("CommunityComments")
                .whereEqualTo("postId", postId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("CommentUpdate", "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        updateCommentsCount(postId, value.size());
                    }
                });
    }

    private void bindEventPost(CommunityViewHolder holder, Post post) {
        if (post == null) return;

        // Set basic post information
        holder.nameOfEvent.setText(post.getNameOfEvent());
        holder.typeOfEvent.setText(post.getTypeOfEvent());
        holder.organization.setText(post.getOrganizations());
        holder.address.setText(post.getBarangay());
        holder.headCoordinator.setText(post.getHeadCoordinator());

        // Set skills
        String skills = post.getEventSkills() != null ?
                String.join(", ", post.getEventSkills()) : "No Skills Required";
        holder.skills.setText(skills);

        // Set date
        if (post.getDate() != null) {
            holder.date.setText(dateFormat.format(post.getDate().toDate()));
        }

        // Set volunteers count
        holder.volunteersNeeded.setText(String.format("%d/%d",
                post.getParticipantsJoined(),
                post.getVolunteerNeeded()));

        // Set up caption with indentation and justification (similar to PostAdapter)
        int indentSize = 80; // Adjust as needed
        SpannableString spannableCaption = new SpannableString(post.getCaption());
        spannableCaption.setSpan(new LeadingMarginSpan.Standard(indentSize, 0), 0, spannableCaption.length(), 0);
        holder.caption.setText(spannableCaption);
        holder.caption.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD);

        // Handle images
        setupImages(holder, post);

        // Check join status with enhanced functionality
        checkUserJoinStatus(holder.joinEvent, post);
    }

    private void setupImages(CommunityViewHolder holder, Post post) {
        if (post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
            holder.posterRecyclerView.setVisibility(View.VISIBLE);
            ImagesAdapter imageAdapter = new ImagesAdapter(context, post.getImageUrls());
            holder.posterRecyclerView.setAdapter(imageAdapter);
        } else {
            holder.posterRecyclerView.setVisibility(View.GONE);
        }
    }

    /**
     * Enhanced method to check user join status with all scenarios covered
     */
    private void checkUserJoinStatus(Button joinButton, Post post) {
        Log.d(TAG, "Checking join status for event: " + post.getEventId());

        // Check if the user has joined the event
        String joinDocId = userId + "_" + post.getEventId();
        db.collection("UserJoinEvents")
                .document(joinDocId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // User has already joined the event
                        Log.d(TAG, "User has already joined the event");

                        // Check if the event has feedback posted
                        db.collection("EventInformation")
                                .document(post.getEventId())
                                .get()
                                .addOnSuccessListener(eventInfoDoc -> {
                                    if (eventInfoDoc.exists()) {
                                        Boolean postFeedback = eventInfoDoc.getBoolean("postFeedback");

                                        if (Boolean.TRUE.equals(postFeedback)) {
                                            // Check if user has already submitted feedback
                                            db.collection("Feedback")
                                                    .document(userId + "_" + post.getEventId())
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
                                                                intent.putExtra("eventId", post.getEventId());
                                                                intent.putExtra("eventName", post.getNameOfEvent());
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
                                            handleEventDateCheck(post, joinButton);
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
                        handleNonJoinedUserState(joinButton, post);
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

    /**
     * Handles button state for events the user has joined, based on date
     */
    private void handleEventDateCheck(Post post, Button joinButton) {
        if (post.getDate() != null) {
            Calendar eventDate = Calendar.getInstance();
            eventDate.setTime(post.getDate().toDate());

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

    /**
     * Handles button state for events the user has not joined
     */
    private void handleNonJoinedUserState(Button joinButton, Post post) {
        if (post.getDate() != null) {
            Calendar eventDate = Calendar.getInstance();
            eventDate.setTime(post.getDate().toDate());

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);

            Calendar eventDay = Calendar.getInstance();
            eventDay.setTime(post.getDate().toDate());
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
                if (post.getParticipantsJoined() >= post.getVolunteerNeeded()) {
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
                        // Keep default green color for Join Event
                        joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.green));
                        Log.d(TAG, "User can join event");

                        joinButton.setOnClickListener(v -> {
                            if (context instanceof CommunityGroupLocationBased) {
                                joinButton.setEnabled(false); // Prevent double clicks
                                joinButton.setText("Joining...");
                                ((CommunityGroupLocationBased) context).joinEvent(
                                        post.getEventId(),
                                        post.getNameOfEvent()
                                );
                                // Button will be updated on the next data refresh
                            }
                        });
                    }
                }
            }
        } else {
            // No date set
            if (post.getParticipantsJoined() >= post.getVolunteerNeeded()) {
                joinButton.setText("Full");
                joinButton.setEnabled(false);
                // Set to gray color for full events
                joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray));
            } else {
                joinButton.setText("Join Event");
                joinButton.setEnabled(true);
                // Keep default green color for Join Event
                joinButton.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.green));

                joinButton.setOnClickListener(v -> {
                    if (context instanceof CommunityGroupLocationBased) {
                        ((CommunityGroupLocationBased) context).joinEvent(
                                post.getEventId(),
                                post.getNameOfEvent()
                        );
                    }
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void updateVolunteersCount(String eventName, String updatedCount) {
        try {
            for (int i = 0; i < posts.size(); i++) {
                Post post = posts.get(i);
                if (post.getNameOfEvent() != null && post.getNameOfEvent().equals(eventName)) {
                    String[] counts = updatedCount.split("/");
                    if (counts.length == 2) {
                        post.setParticipantsJoined(Integer.parseInt(counts[0].trim()));
                        post.setVolunteerNeeded(Integer.parseInt(counts[1].trim()));
                        notifyItemChanged(i);
                    }
                    break;
                }
            }
        } catch (NumberFormatException e) {
            Log.e("CommunityAdapter", "Error parsing volunteer counts", e);
        }
    }

    public interface VolunteerCountListener {
        void getVolunteersJoined(String eventName, CountCallback callback);
    }

    public interface CountCallback {
        void onCountFetched(int count);
    }

    public void updateCommentsCount(String postId, int count) {
        for (int i = 0; i < posts.size(); i++) {
            Post post = posts.get(i);
            if (post.getPostId() != null && post.getPostId().equals(postId)) {
                RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(i);
                if (holder instanceof CommunityPostViewHolder) {
                    CommunityPostViewHolder postHolder = (CommunityPostViewHolder) holder;
                    postHolder.textCommentsNumber.setText(String.valueOf(count));
                }
                break;
            }
        }
    }

    /**
     * Method to allow external updates to verification status
     */
    public void updateVerificationStatus(String status) {
        this.userVerificationStatus = status;
        notifyDataSetChanged();
    }

    static class CommunityViewHolder extends RecyclerView.ViewHolder {
        TextView nameOfEvent, typeOfEvent, organization, address, date;
        TextView headCoordinator, skills, caption, volunteersNeeded;
        Button joinEvent;
        RecyclerView posterRecyclerView;

        CommunityViewHolder(@NonNull View itemView) {
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

    static class CommunityPostViewHolder extends RecyclerView.ViewHolder {
        TextView adminName, position, postContent, timestamp, textCommentsNumber, textLikesNumber;
        ImageButton imgbtnComment, imgbtnLike;
        RelativeLayout containerComment;

        CommunityPostViewHolder(@NonNull View itemView) {
            super(itemView);
            adminName = itemView.findViewById(R.id.adminName);
            position = itemView.findViewById(R.id.position);
            postContent = itemView.findViewById(R.id.postContent);
            timestamp = itemView.findViewById(R.id.timestamp);
            textCommentsNumber = itemView.findViewById(R.id.textCommentsNumber);
            imgbtnComment = itemView.findViewById(R.id.imgbtnComment);
            containerComment = itemView.findViewById(R.id.containerComment);
            imgbtnLike = itemView.findViewById(R.id.imgbtnLike);
            textLikesNumber = itemView.findViewById(R.id.textLikesNumber);
        }
    }
}