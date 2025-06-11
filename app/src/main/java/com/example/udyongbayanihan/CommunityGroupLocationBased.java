package com.example.udyongbayanihan;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import com.example.udyongbayanihan.Notification;
import com.example.udyongbayanihan.NotificationWorkManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommunityGroupLocationBased extends AppCompatActivity {

    RecyclerView recyclerView;
    private ArrayList<Post> posts;
    private CommunityAdapter adapter;
    private FirebaseFirestore db;
    private String userId, uaddressId, unameId, uotherDetails;
    private TextView noPostsText;
    private Set<String> loadedEventIds; // To track loaded events and avoid duplicates

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh comments count for all posts
        for (Post post : posts) {
            if (post.isCommunityPost() && post.getPostId() != null) {
                refreshCommentsCount(post.getPostId());
            }
        }
    }

    private void refreshCommentsCount(String postId) {
        db.collection("CommunityComments")
                .whereEqualTo("postId", postId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int commentCount = queryDocumentSnapshots.size();
                    updateCommentsCount(postId, commentCount);
                })
                .addOnFailureListener(e -> {
                    Log.e("CommentsError", "Error loading comments count", e);
                });
    }

    private void updateCommentsCount(String postId, int count) {
        if (adapter != null) {
            adapter.updateCommentsCount(postId, count);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_group_location_based);

        initializeVariables();
        validateInputs();
        setupRecyclerView();
        loadEventsFromFirestore();
    }

    private void initializeVariables() {
        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("userId");
        uaddressId = getIntent().getStringExtra("uaddressId");
        unameId = getIntent().getStringExtra("unameId");
        uotherDetails = getIntent().getStringExtra("uotherDetails");

        recyclerView = findViewById(R.id.postList);
        noPostsText = findViewById(R.id.noPostsText);
        loadedEventIds = new HashSet<>(); // Initialize the set to track loaded events
    }

    private void validateInputs() {
        if (userId == null || userId.isEmpty()) {
            showErrorAndFinish("Error: User ID not passed!");
            return;
        }
        if (uotherDetails == null) {
            showErrorAndFinish("Error: Other details not provided!");
            return;
        }
        if (uaddressId == null || uaddressId.isEmpty()) {
            showErrorAndFinish("Error: Address ID not passed!");
            return;
        }
        if (unameId == null || unameId.isEmpty()) {
            showErrorAndFinish("Error: Username ID not passed!");
        }
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void setupRecyclerView() {
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        posts = new ArrayList<>();
        adapter = new CommunityAdapter(this, posts, (eventName, callback) -> {
            fetchVolunteersCount(eventName, callback);
        }, userId);
        recyclerView.setAdapter(adapter);

        // Start listening for comment updates for existing posts
        for (Post post : posts) {
            if (post.isCommunityPost() && post.getPostId() != null) {
                adapter.startListeningForCommentUpdates(post.getPostId());
            }
        }
    }

    private void fetchVolunteersCount(String eventName, CommunityAdapter.CountCallback callback) {
        db.collection("UserJoinEvents")
                .whereEqualTo("eventName", eventName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    callback.onCountFetched(queryDocumentSnapshots.size());
                })
                .addOnFailureListener(e -> {
                    callback.onCountFetched(0);
                    Log.e("FirestoreError", "Failed to fetch volunteers count", e);
                });
    }

    private void loadEventsFromFirestore() {
        // First get the user's barangay
        db.collection("usersAddress").document(uaddressId)
                .get()
                .addOnSuccessListener(addressSnapshot -> {
                    if (addressSnapshot.exists()) {
                        String userBarangay = addressSnapshot.getString("barangay");
                        if (userBarangay != null) {
                            userBarangay = userBarangay.trim();

                            // Create a counter to track when both loads are complete
                            final int[] completionCounter = {0};
                            final int TOTAL_LOADS = 2; // We have two loads: community posts and events

                            // Load community posts
                            loadCommunityPosts(userBarangay, () -> {
                                completionCounter[0]++;
                                if (completionCounter[0] == TOTAL_LOADS) {
                                    sortAndUpdateUI();
                                }
                            });

                            // Load events
                            loadEventsByBarangay(userBarangay, () -> {
                                completionCounter[0]++;
                                if (completionCounter[0] == TOTAL_LOADS) {
                                    sortAndUpdateUI();
                                }
                            });
                        } else {
                            showNoPostsMessage("There is no post in your barangay");
                        }
                    } else {
                        showNoPostsMessage("User address not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error fetching user address", e);
                    showNoPostsMessage("Error loading user address");
                });
    }

    // Add an interface for completion callbacks
    private interface LoadCompletionCallback {
        void onLoadComplete();
    }

    private void loadCommunityPosts(String userBarangay, LoadCompletionCallback callback) {
        // Check if userBarangay is not null or empty before proceeding
        if (userBarangay == null || userBarangay.trim().isEmpty()) {
            Log.e("FirestoreError", "User barangay is null or empty");
            callback.onLoadComplete(); // Call callback to prevent getting stuck
            return;
        }

        // First, check if the document exists before trying to access its subcollection
        db.collection("CommunityGroups").document(userBarangay).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Document exists, now it's safe to query the subcollection
                        db.collection("CommunityGroups")
                                .document(userBarangay)
                                .collection("Posts")
                                .orderBy("timestamp", Query.Direction.DESCENDING)  // Sort by newest first
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                        Post communityPost = new Post();
                                        communityPost.setPostId(document.getId());
                                        communityPost.setCommunityPost(true);
                                        communityPost.setAdminName(document.getString("adminName"));
                                        communityPost.setPosition(document.getString("position"));
                                        communityPost.setPostContent(document.getString("postContent"));
                                        communityPost.setPostTimestamp(document.getTimestamp("timestamp"));

                                        // Set a standardized date field for sorting
                                        if (communityPost.getPostTimestamp() != null) {
                                            communityPost.setDate(communityPost.getPostTimestamp());
                                        }

                                        posts.add(communityPost);
                                    }

                                    // Call the completion callback
                                    callback.onLoadComplete();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FirestoreError", "Error loading community posts", e);
                                    callback.onLoadComplete(); // Still call callback on failure
                                });
                    } else {
                        Log.d("FirestoreError", "Barangay document " + userBarangay + " does not exist in CommunityGroups");
                        callback.onLoadComplete(); // Document doesn't exist, still call callback
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error checking if barangay document exists", e);
                    callback.onLoadComplete(); // Still call callback on failure
                });
    }

    private void loadEventsByBarangay(String userBarangay, LoadCompletionCallback callback) {
        db.collection("EventInformation")
                .whereEqualTo("status", "Accepted")
                .get()
                .addOnSuccessListener(eventInfoSnapshots -> {
                    final int[] completedEvents = {0};
                    final int totalEvents = eventInfoSnapshots.size();

                    if (totalEvents == 0) {
                        // No events found
                        callback.onLoadComplete();
                        return;
                    }

                    for (QueryDocumentSnapshot eventInfoDoc : eventInfoSnapshots) {
                        String eventId = eventInfoDoc.getId();
                        EventInformation eventInfo = eventInfoDoc.toObject(EventInformation.class);

                        db.collection("EventDetails")
                                .whereEqualTo("eventId", eventId)
                                .whereEqualTo("barangay", userBarangay)
                                .get()
                                .addOnSuccessListener(eventDetailsSnapshots -> {
                                    for (QueryDocumentSnapshot eventDetailsDoc : eventDetailsSnapshots) {
                                        // Check if we've already loaded this event
                                        if (!loadedEventIds.contains(eventId)) {
                                            loadedEventIds.add(eventId);

                                            EventDetails eventDetails = eventDetailsDoc.toObject(EventDetails.class);
                                            Post post = createPostFromDetails(eventId, eventInfo, eventDetails);
                                            posts.add(post);
                                        }
                                    }

                                    completedEvents[0]++;
                                    if (completedEvents[0] >= totalEvents) {
                                        callback.onLoadComplete();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FirestoreError", "Error fetching event details", e);
                                    completedEvents[0]++;
                                    if (completedEvents[0] >= totalEvents) {
                                        callback.onLoadComplete();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Error fetching events", e);
                    callback.onLoadComplete();
                });
    }

    private Post createPostFromDetails(String eventId, EventInformation eventInfo, EventDetails eventDetails) {
        Post post = new Post();
        post.setEventId(eventId);
        post.setOrganizations(eventInfo.getOrganizations());
        post.setHeadCoordinator(eventInfo.getHeadCoordinator());
        post.setEventSkills(eventInfo.getEventSkills());
        post.setStatus(eventInfo.getStatus());
        post.setNameOfEvent(eventDetails.getNameOfEvent());
        post.setTypeOfEvent(eventDetails.getTypeOfEvent());
        post.setDate(eventDetails.getDate());
        post.setCaption(eventDetails.getCaption());
        post.setImageUrls(eventDetails.getImageUrls());
        post.setBarangay(eventDetails.getBarangay());
        post.setParticipantsJoined(eventDetails.getParticipantsJoined());
        post.setVolunteerNeeded(eventDetails.getVolunteerNeeded());
        post.setCommunityPost(false); // This is an event, not a community post

        return post;
    }

    private void sortAndUpdateUI() {
        // Sort posts by date (newest first)
        Collections.sort(posts, (p1, p2) -> {
            // Handle null dates
            if (p1.getDate() == null && p2.getDate() == null) return 0;
            if (p1.getDate() == null) return 1; // null dates go last
            if (p2.getDate() == null) return -1;

            // Compare dates (reverse order)
            return p2.getDate().compareTo(p1.getDate());
        });

        updateUIWithPosts();
    }

    private void updateUIWithPosts() {
        if (!posts.isEmpty()) {
            noPostsText.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
        } else {
            showNoPostsMessage("No posts or events found in your barangay");
        }
    }

    private void showNoPostsMessage(String message) {
        noPostsText.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void joinEvent(String eventId, String eventName) {
        String userId = this.userId; // Get the current user's ID
        final String TAG = "CommunityGroup";

        // Check user verification status first
        db.collection("usersAccount")
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String userStatus = userDoc.getString("usersStatus");

                        if (!"Verified".equals(userStatus)) {
                            // User is not verified
                            Log.d(TAG, "User is not verified, cannot join event");
                            new AlertDialog.Builder(this)
                                    .setTitle("Verification Required")
                                    .setMessage("Your account is not yet verified. Please wait for admin verification before joining events.")
                                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                                    .show();
                            refreshAdapter(); // Refresh to show correct button state
                            return;
                        }

                        // Continue with joining process if user is verified
                        // Check if the user has already joined
                        db.collection("UserJoinEvents")
                                .document(userId + "_" + eventId)
                                .get()
                                .addOnSuccessListener(joinDoc -> {
                                    if (joinDoc.exists()) {
                                        // User has already joined
                                        Log.d(TAG, "User already joined the event");
                                        Toast.makeText(this, "You have already joined this event.", Toast.LENGTH_SHORT).show();
                                        refreshAdapter();
                                        return;
                                    }

                                    // Check current participant count
                                    db.collection("UserJoinEvents")
                                            .whereEqualTo("eventId", eventId)
                                            .get()
                                            .addOnSuccessListener(joinSnapshots -> {
                                                int currentParticipants = joinSnapshots.size();

                                                // Check event capacity
                                                db.collection("EventDetails")
                                                        .document(eventId)
                                                        .get()
                                                        .addOnSuccessListener(eventDoc -> {
                                                            if (eventDoc.exists()) {
                                                                Long volunteerNeeded = eventDoc.getLong("volunteerNeeded");
                                                                if (volunteerNeeded == null) volunteerNeeded = 0L;

                                                                if (currentParticipants >= volunteerNeeded) {
                                                                    // Event is full
                                                                    Log.d(TAG, "Event is already full");
                                                                    Toast.makeText(this, "This event is already full.", Toast.LENGTH_SHORT).show();
                                                                    refreshAdapter();
                                                                    return;
                                                                }

                                                                // Prepare join data
                                                                Map<String, Object> joinData = new HashMap<>();
                                                                joinData.put("userId", userId);
                                                                joinData.put("eventId", eventId);
                                                                joinData.put("eventName", eventName);
                                                                joinData.put("timestamp", FieldValue.serverTimestamp());

                                                                // Use transaction to ensure atomic updates
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
                                                                    // Get barangay for notification
                                                                    String barangay = eventDoc.getString("barangay");
                                                                    createJoinEventNotification(eventId, eventName, barangay);

                                                                    Log.d(TAG, "Successfully joined event: " + eventId);
                                                                    Toast.makeText(this, "You have successfully joined the event!", Toast.LENGTH_SHORT).show();
                                                                    refreshAdapter();
                                                                }).addOnFailureListener(e -> {
                                                                    Log.e(TAG, "Transaction failed: " + e.getMessage());
                                                                    Toast.makeText(this, "Failed to join event. Please try again.", Toast.LENGTH_SHORT).show();
                                                                    refreshAdapter();
                                                                });
                                                            }
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e(TAG, "Error fetching event details: " + e.getMessage());
                                                            Toast.makeText(this, "Failed to join event. Please try again.", Toast.LENGTH_SHORT).show();
                                                            refreshAdapter();
                                                        });
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Error checking participants: " + e.getMessage());
                                                Toast.makeText(this, "Failed to join event. Please try again.", Toast.LENGTH_SHORT).show();
                                                refreshAdapter();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error checking join status: " + e.getMessage());
                                    Toast.makeText(this, "Failed to join event. Please try again.", Toast.LENGTH_SHORT).show();
                                    refreshAdapter();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user verification: " + e.getMessage());
                    Toast.makeText(this, "Failed to verify your account status. Please try again.", Toast.LENGTH_SHORT).show();
                    refreshAdapter();
                });
    }

    private void createJoinEventNotification(String eventId, String eventName, String barangay) {
        if (userId != null && eventId != null && eventName != null && barangay != null) {
            // Create notification document with "event_confirmation" type to match NotificationAdapter
            Notification notification = new Notification(userId, eventId, eventName, barangay, "event_confirmation");
            String notificationId = userId + "_" + eventId + "_event_confirmation";

            db.collection("Notifications")
                    .document(notificationId)
                    .set(notification)
                    .addOnSuccessListener(aVoid ->
                            Log.d("Notification", "Join notification created with ID: " + notificationId))
                    .addOnFailureListener(e ->
                            Log.e("Notification", "Error creating join notification", e));

            // Send immediate notification via work manager (same as Home activity)
            NotificationWorkManager.notifyEventJoined(
                    this, userId, eventId, eventName, barangay,
                    uaddressId, unameId, uotherDetails
            );

            // Check if the event is tomorrow and create upcoming notificatio and won't duplicate with the one from Home activity
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
                                                    Log.d("Notification", "Upcoming notification created with ID: " + upcomingId))
                                            .addOnFailureListener(e ->
                                                    Log.e("Notification", "Error creating upcoming notification", e));
                                }
                            }
                        }
                    });
        }
    }

    private void refreshAdapter() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Dismiss any open dialogs to prevent window leaks
        if (isFinishing()) {
            // Find and dismiss any open dialogs
            for (android.app.Fragment fragment : getFragmentManager().getFragments()) {
                if (fragment instanceof android.app.DialogFragment) {
                    ((android.app.DialogFragment) fragment).dismissAllowingStateLoss();
                }
            }
        }
    }

    public void toggleLikeForPost(Post post, CommunityAdapter.CommunityPostViewHolder holder) {
        String likeDocId = userId + "_" + post.getPostId();

        if (post.isLikedByCurrentUser()) {
            // Unlike the post
            db.collection("CommunityLikes")
                    .document(likeDocId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Update UI
                        post.setLikedByCurrentUser(false);
                        fetchUpdatedLikesCount(post, holder);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("LikeError", "Error unliking post", e);
                    });
        } else {
            // Like the post
            Map<String, Object> likeData = new HashMap<>();
            likeData.put("userId", userId);
            likeData.put("postId", post.getPostId());
            likeData.put("timestamp", FieldValue.serverTimestamp());

            db.collection("CommunityLikes")
                    .document(likeDocId)
                    .set(likeData)
                    .addOnSuccessListener(aVoid -> {
                        // Update UI
                        post.setLikedByCurrentUser(true);
                        fetchUpdatedLikesCount(post, holder);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("LikeError", "Error liking post", e);
                    });
        }
    }

    private void fetchUpdatedLikesCount(Post post, CommunityAdapter.CommunityPostViewHolder holder) {
        db.collection("CommunityLikes")
                .whereEqualTo("postId", post.getPostId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int likesCount = queryDocumentSnapshots.size();
                    post.setLikesCount(likesCount);

                    // Update UI on main thread
                    runOnUiThread(() -> {
                        holder.textLikesNumber.setText(String.valueOf(likesCount));
                        holder.imgbtnLike.setImageResource(
                                post.isLikedByCurrentUser() ? R.drawable.liked : R.drawable.like
                        );
                    });
                });
    }
}