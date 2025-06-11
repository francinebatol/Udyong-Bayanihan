package com.example.udyongbayanihan;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Home extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private RecyclerView recyclerView;
    private ArrayList<Post> posts;
    private PostAdapter adapter;
    private FirebaseFirestore db;
    public String userId, uaddressId, currentFeedbackEventId, unameId, uotherDetails, selectedEventName, eventName;
    private TextView noPostsText;
    private ImageButton imgbtnMessages, navHome, navCommunity, navProfile, navEventCalendar, navNotifications, imgbtnSearch;
    private String userVerificationStatus = "Unverified"; // Default to unverified
    private boolean dataLoaded = false;
    public static final int FEEDBACK_REQUEST_CODE = 2001; // Unique request code for feedback
    private RelativeLayout messagesBadgeLayout;
    private RelativeLayout notificationsBadgeLayout;
    private ListenerRegistration unreadMessagesListener;
    private ListenerRegistration unreadNotificationsListener;
    private MessageBadgeManager badgeManager;
    private NotificationBadgeManager notificationBadgeManager;
    private BottomNavigation bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        db = FirebaseFirestore.getInstance();

        // Retrieve primary keys from Intent
        userId = getIntent().getStringExtra("userId");
        uaddressId = getIntent().getStringExtra("uaddressId");
        unameId = getIntent().getStringExtra("unameId");
        uotherDetails = getIntent().getStringExtra("uotherDetails");
        selectedEventName = getIntent().getStringExtra("eventName");
        eventName = getIntent().getStringExtra("eventName");

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Error: User ID not passed!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Fetch them if they're not provided
        if (uotherDetails == null || uaddressId == null || unameId == null) {
            fetchUserData(userId);
            return;
        } else {
            initializeUI();
        }
    }

    public void setCurrentFeedbackEventId(String eventId) {
        this.currentFeedbackEventId = eventId;
    }

    private void fetchUserData(String userId) {
        Log.d(TAG, "Fetching missing user data for userId: " + userId);

        // Create a counter to track completion of all queries
        final int[] completedQueries = {0};
        final int totalQueries = 3; // We have 3 queries to complete

        // Fetch uotherDetails if missing
        if (uotherDetails == null) {
            db.collection("usersOtherDetails")
                    .whereEqualTo("userId", userId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            uotherDetails = querySnapshot.getDocuments().get(0).getId();
                            Log.d(TAG, "Fetched uotherDetails: " + uotherDetails);
                        } else {
                            Log.e(TAG, "No usersOtherDetails found for userId: " + userId);
                        }

                        completedQueries[0]++;
                        checkAllQueriesCompleted(completedQueries[0], totalQueries);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching usersOtherDetails", e);
                        completedQueries[0]++;
                        checkAllQueriesCompleted(completedQueries[0], totalQueries);
                    });
        } else {
            completedQueries[0]++;
        }

        // Fetch uaddressId if missing
        if (uaddressId == null) {
            db.collection("usersAddress")
                    .whereEqualTo("userId", userId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            uaddressId = querySnapshot.getDocuments().get(0).getId();
                            Log.d(TAG, "Fetched uaddressId: " + uaddressId);
                        } else {
                            Log.e(TAG, "No usersAddress found for userId: " + userId);
                        }

                        completedQueries[0]++;
                        checkAllQueriesCompleted(completedQueries[0], totalQueries);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching usersAddress", e);
                        completedQueries[0]++;
                        checkAllQueriesCompleted(completedQueries[0], totalQueries);
                    });
        } else {
            completedQueries[0]++;
        }

        // Fetch unameId if missing
        if (unameId == null) {
            db.collection("usersName")
                    .whereEqualTo("userId", userId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            unameId = querySnapshot.getDocuments().get(0).getId();
                            Log.d(TAG, "Fetched unameId: " + unameId);
                        } else {
                            Log.e(TAG, "No usersName found for userId: " + userId);
                        }

                        completedQueries[0]++;
                        checkAllQueriesCompleted(completedQueries[0], totalQueries);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching usersName", e);
                        completedQueries[0]++;
                        checkAllQueriesCompleted(completedQueries[0], totalQueries);
                    });
        } else {
            completedQueries[0]++;
        }
    }

    /**
     * Checks if all queries are completed and initializes UI if they are
     */
    private void checkAllQueriesCompleted(int completed, int total) {
        if (completed >= total) {
            // Now check if we have all the required data
            if (uotherDetails == null || uaddressId == null || unameId == null) {
                // Still missing some data, show error and finish activity
                Log.e(TAG, "Failed to fetch all required data. uotherDetails: " + uotherDetails +
                        ", uaddressId: " + uaddressId + ", unameId: " + unameId);
                Toast.makeText(this, "Error: Could not load user data. Please log in again.", Toast.LENGTH_LONG).show();
                finish();
            } else {
                // We have all the data, so continue with initialization
                Log.d(TAG, "All user data fetched successfully");
                runOnUiThread(this::initializeUI);
            }
        }
    }

    /**
     * Initializes the UI elements and loads data
     */
    private void initializeUI() {
        // Check user verification status
        checkUserVerificationStatus();

        ensureCurrentDeviceUserId();

        recyclerView = findViewById(R.id.postList);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        badgeManager = MessageBadgeManager.getInstance();
        notificationBadgeManager = NotificationBadgeManager.getInstance();

        bottomNavigation = new BottomNavigation(userId, uaddressId, unameId, uotherDetails);

        posts = new ArrayList<>();
        adapter = new PostAdapter(this, posts, (eventName, callback) -> {
            db.collection("UserJoinEvents")
                    .whereEqualTo("eventName", eventName)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        int count = queryDocumentSnapshots.size();
                        callback.onCountFetched(count);
                    })
                    .addOnFailureListener(e -> {
                        callback.onCountFetched(0);
                        Log.e("FirestoreError", "Failed to fetch volunteers count for event: " + eventName, e);
                    });
        }, userId, userVerificationStatus); // Pass userId and verification status here
        recyclerView.setAdapter(adapter);

        loadEventsFromFirestore();

        // Initialize UI buttons
        imgbtnMessages = findViewById(R.id.imgbtnMessages);
        imgbtnSearch = findViewById(R.id.imgbtnSearch);
        navHome = findViewById(R.id.navHome);
        navCommunity = findViewById(R.id.navCommunity);
        navProfile = findViewById(R.id.navProfile);
        navEventCalendar = findViewById(R.id.navEventCalendar);
        navNotifications = findViewById(R.id.navNotifications);
        noPostsText = findViewById(R.id.noPostsText);

        bottomNavigation.setupBottomNavigation(navHome, navCommunity, navProfile, navEventCalendar, navNotifications, this);

        messagesBadgeLayout = bottomNavigation.setupMessageBadge(imgbtnMessages, this);

        unreadMessagesListener = badgeManager.startListeningForUnreadMessages(
                userId, "user", count -> {
                    runOnUiThread(() -> {
                        badgeManager.updateBadgeCount(messagesBadgeLayout, count);
                    });
                });

        ImageButton baseMessageButton = messagesBadgeLayout.findViewById(R.id.imgbtnBase);
        baseMessageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, Messages.class);
            Bundle userDetails = new Bundle();
            userDetails.putString("uaddressId", uaddressId);
            userDetails.putString("unameId", unameId);
            userDetails.putString("uotherDetails", uotherDetails);
            UserDataHelper.passUserData(intent, userId, "user", userDetails);
            startActivity(intent);
        });

        imgbtnMessages.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, Messages.class);
            Bundle userDetails = new Bundle();
            userDetails.putString("uaddressId", uaddressId);
            userDetails.putString("unameId", unameId);
            userDetails.putString("uotherDetails", uotherDetails);
            UserDataHelper.passUserData(intent, userId, "user", userDetails);
            startActivity(intent);
        });

        imgbtnSearch = findViewById(R.id.imgbtnSearch);

        imgbtnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(Home.this, SearchEvent.class);
            startActivityForResult(intent, 1001); // Use a unique request code
        });

        // Initialize bottom navigation buttons
        navHome = findViewById(R.id.navHome);
        navCommunity = findViewById(R.id.navCommunity);
        navProfile = findViewById(R.id.navProfile);
        navEventCalendar = findViewById(R.id.navEventCalendar);
        navNotifications = findViewById(R.id.navNotifications);
        noPostsText = findViewById(R.id.noPostsText);

        // Save the data to shared preferences for next time
        saveUserDataToPreferences();

        setupNotificationBadge();
    }

    /**
     * Saves the current user data to SharedPreferences to prevent this issue in the future
     */
    private void saveUserDataToPreferences() {
        android.content.SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();

        editor.putString("userId", userId);
        editor.putString("uaddressId", uaddressId);
        editor.putString("unameId", unameId);
        editor.putString("uotherDetails", uotherDetails);
        editor.putString("userType", "user");

        editor.apply();
        Log.d(TAG, "Saved user data to SharedPreferences");
    }

    /**
     * Ensures that the current device user ID is properly set for notifications
     */
    private void ensureCurrentDeviceUserId() {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "Cannot set current device user ID: userId is null or empty");
            return;
        }

        SharedPreferences notificationPrefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE);
        String currentDeviceUserId = notificationPrefs.getString("current_device_user_id", null);

        // Set or update the current device user ID if needed
        if (currentDeviceUserId == null || !currentDeviceUserId.equals(userId)) {
            notificationPrefs.edit().putString("current_device_user_id", userId).apply();
            Log.d(TAG, "Updated current device user ID: " + userId);
        } else {
            Log.d(TAG, "Current device user ID already set correctly: " + currentDeviceUserId);
        }
    }

    private void checkUserVerificationStatus() {
        if (userId != null) {
            db.collection("usersAccount")
                    .document(userId)
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                            Log.e("Home", "Listen for verification status failed", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            String newStatus = documentSnapshot.getString("usersStatus");
                            if (newStatus != null) {
                                // Get the previous known status from SharedPreferences
                                SharedPreferences prefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE);
                                String lastKnownStatus = prefs.getString("last_verification_status_" + userId, null);

                                // Update the status in the adapter
                                userVerificationStatus = newStatus;
                                if (adapter != null) {
                                    adapter.updateVerificationStatus(userVerificationStatus);
                                }

                                // If status changed and it's not the first time we're seeing it
                                if (lastKnownStatus != null && !lastKnownStatus.equals(newStatus)) {
                                    Log.d("Home", "Verification status changed from " + lastKnownStatus + " to " + newStatus);

                                    // Save the new status immediately
                                    prefs.edit().putString("last_verification_status_" + userId, newStatus).apply();

                                    // Skip notification if status is "Unverified"
                                    if ("Unverified".equals(newStatus)) {
                                        Log.d("Home", "Skipping notification for Unverified status");
                                        return;
                                    }

                                    // Create the appropriate notification
                                    String message;
                                    if ("Verified".equals(newStatus)) {
                                        message = "Your account is now verified! You can now join events.";
                                    } else {
                                        message = "Your account verification status has been updated to: " + newStatus;
                                    }

                                    // Create unique notification ID to prevent duplicates
                                    String notificationId = userId + "_verification_" + System.currentTimeMillis();

                                    // Check for existing similar notifications first
                                    db.collection("Notifications")
                                            .whereEqualTo("userId", userId)
                                            .whereEqualTo("type", "verification_status")
                                            .whereEqualTo("status", newStatus)
                                            .get()
                                            .addOnSuccessListener(querySnapshot -> {
                                                if (querySnapshot.isEmpty()) {
                                                    // Create notification in Firestore
                                                    Notification notification = new Notification(userId, "verification_status", newStatus, message);

                                                    // Add to Firestore with explicit ID to prevent duplicates
                                                    db.collection("Notifications")
                                                            .document(notificationId)
                                                            .set(notification)
                                                            .addOnSuccessListener(aVoid -> {
                                                                Log.d("Home", "Verification notification created in database");

                                                                // Show notification directly (will appear in foreground)
                                                                NotificationHelper notificationHelper = new NotificationHelper(Home.this);
                                                                notificationHelper.showVerificationNotification(
                                                                        userId,
                                                                        "Account Verification Update",
                                                                        message,
                                                                        newStatus
                                                                );
                                                            })
                                                            .addOnFailureListener(err ->
                                                                    Log.e("Home", "Error creating verification notification", err));
                                                } else {
                                                    Log.d("Home", "Similar verification notification already exists - not creating duplicate");

                                                    // Still show system notification
                                                    NotificationHelper notificationHelper = new NotificationHelper(Home.this);
                                                    notificationHelper.showVerificationNotification(
                                                            userId,
                                                            "Account Verification Update",
                                                            message,
                                                            newStatus
                                                    );
                                                }
                                            })
                                            .addOnFailureListener(err ->
                                                    Log.e("Home", "Error checking for existing notifications", err));
                                } else if (lastKnownStatus == null) {
                                    // First time we're seeing a status, just save it
                                    prefs.edit().putString("last_verification_status_" + userId, newStatus).apply();
                                    Log.d("Home", "Initial verification status set to: " + newStatus);
                                }
                            }
                        }
                    });
        }
    }

    @Override
    protected void onResume() {
        ensureCurrentDeviceUserId();

        super.onResume();

        // Refresh notification badge count
        if (notificationBadgeManager != null && userId != null) {
            notificationBadgeManager.startListeningForUnreadNotifications(
                    userId, count -> {
                        runOnUiThread(() -> {
                            if (notificationsBadgeLayout != null) {
                                notificationBadgeManager.updateBadgeCount(notificationsBadgeLayout, count);
                            }
                        });
                    });
        }

        // Trigger an immediate verification status check
        if (userId != null) {
            NotificationWorkManager.checkAndNotifyVerificationStatus(this, userId);

            // Also refresh the UI to reflect the latest verification status
            checkUserVerificationStatus();
        }
    }

    private void loadEventsFromFirestore() {
        if (uotherDetails == null) {
            Log.e(TAG, "Cannot load events: uotherDetails is null");
            noPostsText.setVisibility(View.VISIBLE);
            return;
        }

        db.collection("usersOtherDetails").document(uotherDetails).get()
                .addOnSuccessListener(otherDetailsSnapshot -> {
                    if (otherDetailsSnapshot.exists()) {
                        List<String> userSkills = (List<String>) otherDetailsSnapshot.get("skills");
                        if (userSkills == null) userSkills = new ArrayList<>();

                        List<String> finalUserSkills = userSkills;

                        if (uaddressId == null) {
                            Log.e(TAG, "Cannot load events: uaddressId is null");
                            noPostsText.setVisibility(View.VISIBLE);
                            return;
                        }

                        db.collection("usersAddress").document(uaddressId).get()
                                .addOnSuccessListener(addressSnapshot -> {
                                    if (addressSnapshot.exists()) {
                                        String userBarangay = addressSnapshot.getString("barangay");

                                        db.collection("EventInformation")
                                                .whereEqualTo("status", "Accepted")
                                                .get()
                                                .addOnSuccessListener(eventInfoSnapshots -> {
                                                    if (!eventInfoSnapshots.isEmpty()) {
                                                        posts.clear();

                                                        for (QueryDocumentSnapshot eventInfoDoc : eventInfoSnapshots) {
                                                            String eventId = eventInfoDoc.getId();
                                                            EventInformation eventInfo = eventInfoDoc.toObject(EventInformation.class);
                                                            setupEventListener(eventId);

                                                            // Get real-time participant count
                                                            db.collection("UserJoinEvents")
                                                                    .whereEqualTo("eventId", eventId)
                                                                    .get()
                                                                    .addOnSuccessListener(joinSnapshots -> {
                                                                        final long participantsJoined = joinSnapshots.size();

                                                                        db.collection("EventDetails")
                                                                                .whereEqualTo("eventId", eventId)
                                                                                .get()
                                                                                .addOnSuccessListener(eventDetailsSnapshots -> {
                                                                                    if (!eventDetailsSnapshots.isEmpty()) {
                                                                                        for (QueryDocumentSnapshot eventDetailsDoc : eventDetailsSnapshots) {
                                                                                            EventDetails eventDetails = eventDetailsDoc.toObject(EventDetails.class);

                                                                                            Long volunteerNeeded = eventDetailsDoc.getLong("volunteerNeeded");
                                                                                            if (volunteerNeeded == null)
                                                                                                volunteerNeeded = 0L;
                                                                                            final long finalVolunteerNeeded = volunteerNeeded;

                                                                                            Post post = new Post();
                                                                                            post.setEventId(eventId);
                                                                                            post.setOrganizations(eventInfo.getOrganizations());
                                                                                            post.setHeadCoordinator(eventInfo.getHeadCoordinator());
                                                                                            post.setEventSkills(eventInfo.getEventSkills());
                                                                                            post.setStatus(eventInfo.getStatus());
                                                                                            post.setParticipantsJoined((int) participantsJoined);
                                                                                            post.setVolunteerNeeded((int) finalVolunteerNeeded);
                                                                                            post.setNameOfEvent(eventDetails.getNameOfEvent());
                                                                                            post.setTypeOfEvent(eventDetails.getTypeOfEvent());
                                                                                            post.setDate(eventDetails.getDate());
                                                                                            post.setCaption(eventDetails.getCaption());
                                                                                            post.setImageUrls(eventDetails.getImageUrls());
                                                                                            post.setBarangay(eventDetails.getBarangay());
                                                                                            post.setPostFeedback(eventInfo.isPostFeedback());

                                                                                            posts.add(post);
                                                                                        }
                                                                                    } else {
                                                                                        Log.d("EventsDebug", "No EventDetails found for event ID: " + eventId);
                                                                                    }

                                                                                    if (!posts.isEmpty()) {
                                                                                        posts = (ArrayList<Post>) EventSorter.sortEvents(posts, userBarangay, finalUserSkills);

                                                                                        Log.d("EventsDebug", "Posts found, updating UI after sorting.");
                                                                                        noPostsText.setVisibility(View.GONE);
                                                                                        adapter.notifyDataSetChanged();

                                                                                        // Scroll to the event if it matches the selectedEventName
                                                                                        String selectedEventName = getIntent().getStringExtra("eventName");
                                                                                        if (selectedEventName != null) {
                                                                                            scrollToEvent(selectedEventName);
                                                                                        }
                                                                                    } else {
                                                                                        Log.d("EventsDebug", "No posts found after retrieving data.");
                                                                                        noPostsText.setVisibility(View.VISIBLE);
                                                                                    }
                                                                                })
                                                                                .addOnFailureListener(e -> Log.e("EventsDebug", "Error fetching event details: " + e.getMessage()));
                                                                    })
                                                                    .addOnFailureListener(e -> Log.e("EventsDebug", "Error fetching join count: " + e.getMessage()));
                                                        }
                                                    } else {
                                                        Log.d("EventsDebug", "No EventInformation found with status 'Accepted'.");
                                                        noPostsText.setVisibility(View.VISIBLE);
                                                        Toast.makeText(Home.this, "No accepted events found.", Toast.LENGTH_SHORT).show();
                                                    }
                                                })
                                                .addOnFailureListener(e -> Log.e("EventsDebug", "Error fetching event information: " + e.getMessage()));
                                    }
                                })
                                .addOnFailureListener(e -> Log.e("EventsDebug", "Error fetching address: " + e.getMessage()));
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.d("EventsDebug", "No other details found for user.");
                        noPostsText.setVisibility(View.VISIBLE);
                        Toast.makeText(Home.this, "User details not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Log.e("EventsDebug", "Error fetching other details: " + e.getMessage()));
    }

    private void scrollToEvent(String eventName) {
        boolean eventFound = false; // Flag to track if the event is found
        for (int i = 0; i < posts.size(); i++) {
            if (posts.get(i).getNameOfEvent().equalsIgnoreCase(eventName)) {
                recyclerView.scrollToPosition(i);
                eventFound = true; // Mark as found
                break; // Exit loop since the event is found
            }
        }
    }

    public void joinEvent(String eventId, String eventName) {
        String userId = this.userId;

        // First check if user is verified
        if (userVerificationStatus.equals("Unverified")) {
            // Show verification required dialog
            new AlertDialog.Builder(this)
                    .setTitle("Verification Required")
                    .setMessage("Your account is not yet verified. Please wait for admin verification before joining events.")
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }

        // Start by disabling the join button immediately to prevent double clicks
        updateJoinButtonText(eventName, "Joining...");

        // Check if the user already joined this event
        db.collection("UserJoinEvents")
                .document(userId + "_" + eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d("JoinEvent", "User already joined the event.");
                        Toast.makeText(this, "You have already joined this event.", Toast.LENGTH_SHORT).show();
                        updateJoinButtonText(eventName, "Already Joined");
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
                                                    if (volunteerNeeded == null)
                                                        volunteerNeeded = 0L;

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

                                                            // Send immediate notification via work manager
                                                            NotificationWorkManager.notifyEventJoined(
                                                                    this, userId, eventId, eventName, barangay,
                                                                    uaddressId, unameId, uotherDetails
                                                            );

                                                            // Update UI
                                                            updateParticipantsCount(eventName, currentParticipants + 1, finalVolunteerNeeded);
                                                            updateJoinButtonText(eventName, "Already Joined");
                                                            Toast.makeText(this, "You have successfully joined the event!", Toast.LENGTH_SHORT).show();
                                                        }).addOnFailureListener(e -> {
                                                            Log.e("JoinEventError", "Transaction failed: " + e.getMessage());
                                                            updateJoinButtonText(eventName, "Join Event");
                                                            Toast.makeText(this, "Failed to join event. Please try again.", Toast.LENGTH_SHORT).show();
                                                        });
                                                    } else {
                                                        updateJoinButtonText(eventName, "Full");
                                                        Toast.makeText(this, "This event is already full.", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("JoinEventError", "Error checking participants: " + e.getMessage());
                                    updateJoinButtonText(eventName, "Join Event");
                                    Toast.makeText(this, "Failed to join event. Please try again.", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("JoinEventError", "Error checking join status: " + e.getMessage());
                    updateJoinButtonText(eventName, "Join Event");
                    Toast.makeText(this, "Failed to join event. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateJoinButtonText(String eventName, String text) {
        for (int i = 0; i < posts.size(); i++) {
            Post post = posts.get(i);
            if (post.getNameOfEvent().equals(eventName)) {
                post.setJoinButtonText(text);
                adapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void updateParticipantsCount(String eventName, long newCount, long volunteerNeeded) {
        String updatedCount = newCount + "/" + volunteerNeeded;

        if (adapter != null) {
            adapter.updateVolunteersCount(eventName, updatedCount);
        }
    }

    private void createJoinEventNotification(String eventId, String eventName, String barangay) {
        // Save timestamp of this notification creation to prevent duplicates
        android.content.SharedPreferences prefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE);
        long currentTime = System.currentTimeMillis();
        prefs.edit().putLong("last_join_notification_" + eventId + "_" + userId, currentTime).apply();

        String confirmationId = userId + "_" + eventId + "_event_confirmation";
        Notification confirmationNotification = new Notification(
                userId, eventId, eventName, barangay, "event_confirmation");

        if (userId != null && eventId != null && eventName != null && barangay != null) {
            // This handles the confirmation notification - both direct and through WorkManager
            // Direct notification will be shown by NotificationWorkManager.notifyEventJoined
            NotificationWorkManager.notifyEventJoined(
                    this, userId, eventId, eventName, barangay,
                    uaddressId, unameId, uotherDetails
            );

            // Save the join confirmation notification to Firestore
            db.collection("Notifications")
                    .document(confirmationId)
                    .set(confirmationNotification)
                    .addOnSuccessListener(aVoid -> {
                        Log.d("Notification", "Event join notification created with ID: " + confirmationId);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Notification", "Error creating join notification", e);
                    });

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
                                                    Log.d("Notification", "Upcoming notification created with ID: " + upcomingId))
                                            .addOnFailureListener(e ->
                                                    Log.e("Notification", "Error creating upcoming notification", e));
                                }
                            }
                        }
                    });
        }
    }

    private void setupEventListener(String eventId) {
        db.collection("EventDetails")
                .document(eventId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.w("FirestoreError", "Listen failed.", e);
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        Long participantsJoined = snapshot.getLong("participantsJoined");
                        Long volunteerNeeded = snapshot.getLong("volunteerNeeded");
                        if (participantsJoined != null && volunteerNeeded != null) {
                            for (Post post : posts) {
                                if (post.getEventId().equals(eventId)) {
                                    post.setParticipantsJoined(participantsJoined.intValue());
                                    post.setVolunteerNeeded(volunteerNeeded.intValue());
                                    adapter.notifyDataSetChanged();
                                    break;
                                }
                            }
                        }
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            // Get the selected event name
            String selectedEventName = data.getStringExtra("eventName");
            if (selectedEventName != null) {
                // Scroll to the event in the RecyclerView
                scrollToEvent(selectedEventName);
            }
        }
        // Handle the result from the Feedback activity
        else if (requestCode == FEEDBACK_REQUEST_CODE && resultCode == RESULT_OK) {
            // User has successfully completed the feedback
            if (currentFeedbackEventId != null) {
                // Find the post with this eventId and update its button
                updateFeedbackButtonStatus(currentFeedbackEventId);
                currentFeedbackEventId = null; // Reset the ID
            }
        }
    }

    private void updateFeedbackButtonStatus(String eventId) {
        // Find the post with this eventId
        for (int i = 0; i < posts.size(); i++) {
            Post post = posts.get(i);
            if (post.getEventId() != null && post.getEventId().equals(eventId)) {
                // Update the join button text in the adapter
                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                if (layoutManager instanceof LinearLayoutManager) {
                    LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
                    int firstVisible = linearLayoutManager.findFirstVisibleItemPosition();
                    int lastVisible = linearLayoutManager.findLastVisibleItemPosition();

                    // Check if the item is currently visible
                    if (i >= firstVisible && i <= lastVisible) {
                        // Item is visible, directly update the button
                        View view = recyclerView.getChildAt(i - firstVisible);
                        if (view != null) {
                            Button joinButton = view.findViewById(R.id.joinEvent);
                            joinButton.setText("Event Ended");
                            // Use setBackgroundTintList instead of setBackgroundColor for consistent styling
                            joinButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.gray));
                            joinButton.setEnabled(false);
                        }
                    }
                }

                // Update the Post object's button text
                post.setJoinButtonText("Event Ended");

                // Notify adapter to update this item
                adapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void setupNotificationBadge() {
        // We'll use the BottomNavigation class's method instead of direct XML
        notificationsBadgeLayout = bottomNavigation.addNotificationBadge(navNotifications, this);

        // Listen for notification updates
        if (notificationBadgeManager == null) {
            notificationBadgeManager = NotificationBadgeManager.getInstance();
        }

        unreadNotificationsListener = notificationBadgeManager.startListeningForUnreadNotifications(
                userId, count -> {
                    runOnUiThread(() -> {
                        if (notificationsBadgeLayout != null) {
                            notificationBadgeManager.updateBadgeCount(notificationsBadgeLayout, count);
                        }
                    });
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (bottomNavigation != null) {
            bottomNavigation.cleanup();
        }

        if (notificationBadgeManager != null) {
            notificationBadgeManager.stopListeningForUser(userId);
        }
    }
}