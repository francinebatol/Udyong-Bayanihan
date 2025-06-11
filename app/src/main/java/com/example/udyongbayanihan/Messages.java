package com.example.udyongbayanihan;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Messages extends AppCompatActivity {
    private static final String TAG = "Messages";

    private FirebaseFirestore db;
    private String userId, uaddressId, unameId, uotherDetails, userType;
    private ImageButton imgbtnCreateMessage, navHome, navCommunity, navProfile, navEventCalendar, navNotifications;
    private RecyclerView chatPreviewsRecyclerView;
    private ChatPreviewAdapter chatPreviewAdapter;
    private List<ChatPreviewModel> chatPreviews;
    private BottomNavigation bottomNavigation;
    private NotificationBadgeManager notificationBadgeManager;
    private ListenerRegistration unreadNotificationsListener;
    private RelativeLayout notificationsBadgeLayout;
    private TextView tvNoMessages; // Add this TextView reference

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh notification badge count for non-admin users
        if (!"admin".equals(userType) && notificationBadgeManager != null && userId != null && notificationsBadgeLayout != null) {
            notificationBadgeManager.startListeningForUnreadNotifications(
                    userId, count -> {
                        runOnUiThread(() -> {
                            notificationBadgeManager.updateBadgeCount(notificationsBadgeLayout, count);
                        });
                    });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        db = FirebaseFirestore.getInstance();

        Bundle userData = UserDataHelper.extractUserData(getIntent());
        userId = userData.getString("userId");
        userType = userData.getString("userType");

        Log.d(TAG, "Messages activity started with userId: " + userId + ", userType: " + userType);

        if (userId != null && userType != null) {
            NotificationWorkManager.scheduleMessageChecks(this, userId, userType);
        } else {
            Log.e(TAG, "userId or userType is null!");
            Toast.makeText(this, "Error: User information not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (userId == null || userType == null) {
            Log.e(TAG, "No user data available!");
            Toast.makeText(this, "Error: User information not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        NotificationWorkManager.scheduleMessageChecks(this, userId, userType);

        uaddressId = getIntent().getStringExtra("uaddressId");
        unameId = getIntent().getStringExtra("unameId");
        uotherDetails = getIntent().getStringExtra("uotherDetails");

        Log.d(TAG, "UserId: " + userId + ", UserType: " + userType);
        chatPreviews = new ArrayList<>();

        // Initialize the notification badge manager
        notificationBadgeManager = NotificationBadgeManager.getInstance();

        initializeViews();
        loadChatPreviews();

        // Mark all chats as read when opening the Messages activity
        markAllChatsAsRead();
    }

    private void initializeViews() {
        try {
            imgbtnCreateMessage = findViewById(R.id.imgbtnCreateMessage);
            chatPreviewsRecyclerView = findViewById(R.id.chatPreviewsRecyclerView);
            navHome = findViewById(R.id.navHome);
            navCommunity = findViewById(R.id.navCommunity);
            navProfile = findViewById(R.id.navProfile);
            navEventCalendar = findViewById(R.id.navEventCalendar);
            navNotifications = findViewById(R.id.navNotifications);
            tvNoMessages = findViewById(R.id.tvNoMessages); // Initialize the "No messages yet" TextView

            chatPreviewAdapter = new ChatPreviewAdapter(chatPreviews, this, userId, userType);
            chatPreviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            chatPreviewsRecyclerView.setAdapter(chatPreviewAdapter);

            Log.d(TAG, "Views initialized successfully");

            setupClickListeners();
            setupBottomNavigation();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage());
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupNotificationBadge() {
        // Only setup notification badge for non-admin users
        if (!"admin".equals(userType) && bottomNavigation != null && navNotifications != null) {
            // Add notification badge to the notifications button
            notificationsBadgeLayout = bottomNavigation.addNotificationBadge(navNotifications, this);

            // Start listening for notification updates
            unreadNotificationsListener = notificationBadgeManager.startListeningForUnreadNotifications(
                    userId, count -> {
                        runOnUiThread(() -> {
                            if (notificationsBadgeLayout != null) {
                                notificationBadgeManager.updateBadgeCount(notificationsBadgeLayout, count);
                            }
                        });
                    });
        }
    }

    private void markAllChatsAsRead() {
        Map<String, String> participantMap = new HashMap<>();
        participantMap.put("id", userId);
        participantMap.put("type", userType);

        db.collection("chats")
                .whereArrayContains("participants", participantMap)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot chatDoc : queryDocumentSnapshots) {
                            // Update the lastReadBy field for this user
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("lastReadBy." + userId, Timestamp.now());

                            chatDoc.getReference().update(updates)
                                    .addOnSuccessListener(aVoid ->
                                            Log.d(TAG, "Chat marked as read: " + chatDoc.getId()))
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "Error marking chat as read: " + e.getMessage()));
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Error getting chats: " + e.getMessage()));
    }

    private void loadChatPreviews() {
        Log.d(TAG, "Loading chat previews for user: " + userId + " with type: " + userType);

        Map<String, String> participantMap = new HashMap<>();
        participantMap.put("id", userId);
        participantMap.put("type", userType);

        db.collection("chats")
                .whereArrayContains("participants", participantMap)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to chat previews: ", error);
                        Toast.makeText(Messages.this, "Error loading chat previews: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value == null) {
                        Log.d(TAG, "No chat previews found");
                        showNoMessagesView(true);
                        return;
                    }

                    Log.d(TAG, "Number of chat documents found: " + value.size());
                    chatPreviews.clear();

                    // Check if there are any documents and show/hide the "No messages yet" view accordingly
                    if (value.isEmpty()) {
                        showNoMessagesView(true);
                    } else {
                        showNoMessagesView(false);
                    }

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        String chatId = doc.getId();
                        Log.d(TAG, "Processing chat: " + chatId);

                        String lastMessage = doc.getString("lastMessage");
                        Date timestamp = doc.getDate("timestamp");
                        List<Map<String, String>> participants = (List<Map<String, String>>) doc.get("participants");

                        if (participants == null) {
                            Log.e(TAG, "Participants list is null for chat: " + chatId);
                            continue;
                        }

                        // Find the other participant
                        for (Map<String, String> participant : participants) {
                            String participantId = participant.get("id");
                            String participantType = participant.get("type");

                            if (!participantId.equals(userId)) {
                                Log.d(TAG, "Found other participant: " + participantId + " of type: " + participantType);
                                // Load all chats for admin users
                                if (userType.equals("admin") || participantType.equals("admin")) {
                                    fetchUserName(participantId, participantType, chatId, lastMessage, timestamp, participants);
                                } else if (userType.equals("user")) {
                                    // For regular users, load both user and admin chats
                                    fetchUserName(participantId, participantType, chatId, lastMessage, timestamp, participants);
                                }
                                break;
                            }
                        }
                    }
                });
    }

    // Helper method to show/hide the "No messages yet" view
    private void showNoMessagesView(boolean show) {
        runOnUiThread(() -> {
            if (show) {
                tvNoMessages.setVisibility(View.VISIBLE);
                chatPreviewsRecyclerView.setVisibility(View.GONE);
            } else {
                tvNoMessages.setVisibility(View.GONE);
                chatPreviewsRecyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void fetchUserName(String otherUserId, String otherUserType, String chatId,
                               String lastMessage, Date timestamp,
                               List<Map<String, String>> participants) {
        // Determine which collection to query based on user type
        String collection = otherUserType.equals("admin") ? "AMNameDetails" : "usersName";
        String firstNameField = otherUserType.equals("admin") ? "amFirstName" : "firstName";
        String lastNameField = otherUserType.equals("admin") ? "amLastName" : "lastName";
        String userIdField = otherUserType.equals("admin") ? "amAccountId" : "userId";

        Log.d(TAG, "Fetching user name from collection: " + collection + " for user: " + otherUserId);

        db.collection(collection)
                .whereEqualTo(userIdField, otherUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                        String firstName = userDoc.getString(firstNameField);
                        String lastName = userDoc.getString(lastNameField);
                        String fullName = firstName + " " + lastName;

                        Log.d(TAG, "Found user name: " + fullName);

                        // Get the chat document to check for unread messages
                        db.collection("chats").document(chatId).get()
                                .addOnSuccessListener(chatDoc -> {
                                    // Check if there are unread messages
                                    boolean hasUnreadMessages = false;
                                    Map<String, Object> lastReadBy = (Map<String, Object>) chatDoc.get("lastReadBy");
                                    Map<String, Object> lastMessageMetadata =
                                            (Map<String, Object>) chatDoc.get("lastMessageMetadata");
                                    if (lastReadBy != null) {
                                        // If lastReadBy doesn't contain the current user's ID or
                                        // the last read timestamp is before the last message timestamp
                                        com.google.firebase.Timestamp lastReadTimestamp = null;
                                        Object lastReadObj = lastReadBy.get(userId);

                                        if (lastReadObj instanceof com.google.firebase.Timestamp) {
                                            lastReadTimestamp = (com.google.firebase.Timestamp) lastReadObj;
                                        }

                                        hasUnreadMessages = lastReadTimestamp == null ||
                                                (timestamp != null && lastReadTimestamp.toDate().before(timestamp));
                                    } else {
                                        // If lastReadBy field doesn't exist, consider it unread
                                        hasUnreadMessages = true;
                                    }

                                    // Create chat preview with or without admin position
                                    if (otherUserType.equals("admin")) {
                                        createChatPreviewWithAdminDetails(
                                                otherUserId, chatId, lastMessage, timestamp, participants,
                                                fullName, lastReadBy, lastMessageMetadata, hasUnreadMessages
                                        );
                                    } else {
                                        // Regular user, no need to fetch position
                                        ChatPreviewModel preview = new ChatPreviewModel(
                                                chatId,
                                                lastMessage,
                                                timestamp,
                                                participants,
                                                lastReadBy,
                                                lastMessageMetadata,
                                                fullName,
                                                otherUserId,
                                                otherUserType,
                                                hasUnreadMessages
                                        );

                                        int index = findChatPreviewIndex(chatId);
                                        if (index != -1) {
                                            chatPreviews.set(index, preview);
                                        } else {
                                            chatPreviews.add(preview);
                                        }

                                        // Sort and update UI
                                        sortChatPreviews();
                                        runOnUiThread(() -> {
                                            chatPreviewAdapter.notifyDataSetChanged();
                                            // Make sure to hide the "No messages" view when we have messages
                                            if (!chatPreviews.isEmpty()) {
                                                showNoMessagesView(false);
                                            }
                                        });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error fetching chat document: ", e);
                                    // Create preview without lastReadBy
                                    ChatPreviewModel preview = new ChatPreviewModel(
                                            chatId,
                                            lastMessage,
                                            timestamp,
                                            participants,
                                            null,  // No lastReadBy available in error case
                                            null,  // No lastMessageMetadata in error case
                                            fullName,
                                            otherUserId,
                                            otherUserType,
                                            true  // Consider unread in error case
                                    );

                                    int index = findChatPreviewIndex(chatId);
                                    if (index != -1) {
                                        chatPreviews.set(index, preview);
                                    } else {
                                        chatPreviews.add(preview);
                                    }

                                    // Sort chat previews by timestamp (newest first)
                                    sortChatPreviews();

                                    runOnUiThread(() -> {
                                        chatPreviewAdapter.notifyDataSetChanged();
                                        Log.d(TAG, "Updated chat previews. Count: " + chatPreviews.size());
                                        // Make sure to hide the "No messages" view when we have messages
                                        if (!chatPreviews.isEmpty()) {
                                            showNoMessagesView(false);
                                        }
                                    });
                                });
                    } else {
                        Log.e(TAG, "User document not found for ID: " + otherUserId);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching user name: ", e));
    }

    private void createChatPreviewWithAdminDetails(
            String adminId, String chatId, String lastMessage, Date timestamp,
            List<Map<String, String>> participants, String fullName,
            Map<String, Object> lastReadBy, Map<String, Object> lastMessageMetadata,
            boolean hasUnreadMessages) {

        // First create the preview with the basic name
        ChatPreviewModel preview = new ChatPreviewModel(
                chatId,
                lastMessage,
                timestamp,
                participants,
                lastReadBy,
                lastMessageMetadata,
                fullName,
                adminId,
                "admin",
                hasUnreadMessages
        );

        // Add to the list
        int index = findChatPreviewIndex(chatId);
        if (index != -1) {
            chatPreviews.set(index, preview);
        } else {
            chatPreviews.add(preview);
        }

        // Sort and update UI - the admin position will be added in the adapter
        sortChatPreviews();
        runOnUiThread(() -> {
            chatPreviewAdapter.notifyDataSetChanged();
            Log.d(TAG, "Updated chat previews with admin. Count: " + chatPreviews.size());
            // Make sure to hide the "No messages" view when we have messages
            if (!chatPreviews.isEmpty()) {
                showNoMessagesView(false);
            }
        });
    }

    // New method to sort chat previews by timestamp (newest first)
    private void sortChatPreviews() {
        Collections.sort(chatPreviews, new Comparator<ChatPreviewModel>() {
            @Override
            public int compare(ChatPreviewModel a, ChatPreviewModel b) {
                // Handle null timestamps
                if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
                if (a.getTimestamp() == null) return 1; // Null timestamps go to the end
                if (b.getTimestamp() == null) return -1;

                // Sort in descending order (newest first)
                return b.getTimestamp().compareTo(a.getTimestamp());
            }
        });
        Log.d(TAG, "Chat previews sorted by timestamp (newest first)");
    }

    private int findChatPreviewIndex(String chatId) {
        for (int i = 0; i < chatPreviews.size(); i++) {
            if (chatPreviews.get(i).getChatId().equals(chatId)) {
                return i;
            }
        }
        return -1;
    }

    private void setupClickListeners() {
        imgbtnCreateMessage.setOnClickListener(v -> {
            Intent intent = new Intent(Messages.this, SearchMessage.class);
            intent.putExtra("userId", userId);
            intent.putExtra("userType", userType);
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        if ("admin".equals(userType)) {
            findViewById(R.id.containerNavigation).setVisibility(View.GONE);
        } else {
            bottomNavigation = new BottomNavigation(userId, uaddressId, unameId, uotherDetails);
            bottomNavigation.setupBottomNavigation(navHome, navCommunity, navProfile,
                    navEventCalendar, navNotifications, this);

            // Set up the notification badge
            setupNotificationBadge();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up resources
        if (unreadNotificationsListener != null) {
            unreadNotificationsListener.remove();
            unreadNotificationsListener = null;
        }

        if (bottomNavigation != null) {
            bottomNavigation.cleanup();
        }

        if (notificationBadgeManager != null && userId != null) {
            notificationBadgeManager.stopListeningForUser(userId);
        }
    }
}