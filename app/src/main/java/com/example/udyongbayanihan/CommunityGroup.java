package com.example.udyongbayanihan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class CommunityGroup extends AppCompatActivity {
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private CommunityGroupAdapter adapter;
    private List<CommunityGroupModel> groupList;
    private Button btnViewAllSkills;
    private ImageButton imgbtnMessages, btnEnterGroupBarangay;
    private String userId, uaddressId, unameId, uotherDetails;
    private ImageButton navHome, navCommunity, navProfile, navEventCalendar, navNotifications;
    private BottomNavigation bottomNavigation;
    private MessageBadgeManager badgeManager;
    private NotificationBadgeManager notificationBadgeManager;
    private ListenerRegistration unreadMessagesListener;
    private ListenerRegistration unreadNotificationsListener;
    private RelativeLayout messagesBadgeLayout;
    private RelativeLayout notificationsBadgeLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_group);

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("userId");

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Error: User ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupRecyclerView();
        fetchUserData();

        // Move this line after initializeViews so we have references to the UI elements
        imgbtnMessages = findViewById(R.id.imgbtnMessages);

        // Initialize badge managers
        badgeManager = MessageBadgeManager.getInstance();
        notificationBadgeManager = NotificationBadgeManager.getInstance();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerCommunityGroups);
        btnEnterGroupBarangay = findViewById(R.id.btnEnterGroupBarangay);
        navHome = findViewById(R.id.navHome);
        navCommunity = findViewById(R.id.navCommunity);
        navProfile = findViewById(R.id.navProfile);
        navEventCalendar = findViewById(R.id.navEventCalendar);
        navNotifications = findViewById(R.id.navNotifications);

        btnEnterGroupBarangay.setOnClickListener(v -> {
            if (uaddressId == null || unameId == null || uotherDetails == null) {
                Toast.makeText(this, "Please wait while loading user data...", Toast.LENGTH_SHORT).show();
                return;
            }
            launchLocationBasedActivity();
        });

        btnViewAllSkills = findViewById(R.id.btnViewAllSkills);
        btnViewAllSkills.setOnClickListener(v -> {
            if (userId != null) {
                Intent intent = new Intent(CommunityGroup.this, AllSkillGroupsActivity.class);
                intent.putExtra("userId", userId);
                startActivity(intent);
            }
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        groupList = new ArrayList<>();
    }

    private void setupMessagesButton() {
        // Set up the badge layout using the BottomNavigation helper
        messagesBadgeLayout = bottomNavigation.setupMessageBadge(imgbtnMessages, this);

        // Start listening for unread messages
        unreadMessagesListener = badgeManager.startListeningForUnreadMessages(
                userId, "user", count -> {
                    runOnUiThread(() -> {
                        badgeManager.updateBadgeCount(messagesBadgeLayout, count);
                    });
                });

        // Set click listener on the button inside the badge layout
        ImageButton baseMessageButton = messagesBadgeLayout.findViewById(R.id.imgbtnBase);
        baseMessageButton.setOnClickListener(v -> {
            if (uaddressId == null || unameId == null || uotherDetails == null) {
                Toast.makeText(this, "Please wait while loading user data...", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(CommunityGroup.this, Messages.class);
            Bundle userDetails = new Bundle();
            userDetails.putString("uaddressId", uaddressId);
            userDetails.putString("unameId", unameId);
            userDetails.putString("uotherDetails", uotherDetails);
            UserDataHelper.passUserData(intent, userId, "user", userDetails);
            startActivity(intent);
        });
    }

    private void setupNotificationBadge() {
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

    private void setupNavigationAfterDataLoad() {
        // Only set up navigation after all data is loaded
        bottomNavigation = new BottomNavigation(userId, uaddressId, unameId, uotherDetails);
        bottomNavigation.setupBottomNavigation(navHome, navCommunity, navProfile, navEventCalendar, navNotifications, this);

        // Set up the messages button after bottom navigation is initialized
        setupMessagesButton();

        // Set up the notification badge
        setupNotificationBadge();
    }

    private void fetchUserData() {
        db.collection("usersOtherDetails")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        uotherDetails = querySnapshot.getDocuments().get(0).getId();
                        List<String> userSkills = (List<String>) querySnapshot.getDocuments().get(0).get("skills");
                        fetchUserAddress(userSkills);
                    } else {
                        Toast.makeText(this, "User details not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error fetching user details: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private void fetchUserAddress(List<String> userSkills) {
        db.collection("usersAddress")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        uaddressId = querySnapshot.getDocuments().get(0).getId();
                        fetchUsername(userSkills);
                    } else {
                        Toast.makeText(this, "User address not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error fetching address: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private void fetchUsername(List<String> userSkills) {
        db.collection("usersName")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        unameId = querySnapshot.getDocuments().get(0).getId();
                        // Initialize adapter and setup navigation only after all data is fetched
                        initializeAdapter(userSkills);
                        setupNavigationAfterDataLoad();
                    } else {
                        Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error fetching username: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private void initializeAdapter(List<String> userSkills) {
        adapter = new CommunityGroupAdapter(
                this,
                groupList,
                userId,
                uaddressId,
                unameId,
                uotherDetails
        );
        recyclerView.setAdapter(adapter);
        updateGroupList(userSkills);
    }

    private void updateGroupList(List<String> userSkills) {
        if (userSkills != null && !userSkills.isEmpty()) {
            groupList.clear();
            for (String skill : userSkills) {
                groupList.add(new CommunityGroupModel(skill));
            }
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        } else {
            Toast.makeText(this, "No skills found for user", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchLocationBasedActivity() {
        Intent intent = new Intent(CommunityGroup.this, CommunityGroupLocationBased.class);
        intent.putExtra("userId", userId);
        intent.putExtra("uaddressId", uaddressId);
        intent.putExtra("unameId", unameId);
        intent.putExtra("uotherDetails", uotherDetails);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clean up resources
        if (unreadMessagesListener != null) {
            unreadMessagesListener.remove();
            unreadMessagesListener = null;
        }

        if (unreadNotificationsListener != null) {
            unreadNotificationsListener.remove();
            unreadNotificationsListener = null;
        }

        if (bottomNavigation != null) {
            bottomNavigation.cleanup();
        }

        if (notificationBadgeManager != null) {
            notificationBadgeManager.stopListeningForUser(userId);
        }
    }
}