package com.example.udyongbayanihan;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.flexbox.FlexboxLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ViewProfile extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextView firstName, middleName, lastName, userAge, phoneNumber, usernameTextView, houseno, street, barangay, birthday, gender, userStatusText, btnViewUserEvents;
    private ImageButton editViewProfile, editInterests;
    private ImageView logout, imgProfilePic;
    private String userId, uaddressId, unameId, uotherDetails;
    private ImageButton navHome, navCommunity, navProfile, navEventCalendar, navNotifications, imgbtnMessages;
    private LinearLayout userStatusContainer;
    private MessageBadgeManager badgeManager;
    private NotificationBadgeManager notificationBadgeManager;
    private ListenerRegistration unreadMessagesListener;
    private ListenerRegistration unreadNotificationsListener;
    private RelativeLayout messagesBadgeLayout;
    private RelativeLayout notificationsBadgeLayout;
    private BottomNavigation bottomNavigation;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Retrieve primary keys from Intent
        userId = getIntent().getStringExtra("userId");
        uaddressId = getIntent().getStringExtra("uaddressId");
        unameId = getIntent().getStringExtra("unameId");
        uotherDetails = getIntent().getStringExtra("uotherDetails");

        // Validate that userId is not null
        if (userId == null) {
            Toast.makeText(this, "User ID is missing", Toast.LENGTH_SHORT).show();
            finish(); // End the activity to prevent further errors
            return;
        }

        logout = findViewById(R.id.logout);
        imgProfilePic = findViewById(R.id.imgProfilePic);

        // Set logout click listener
        logout.setOnClickListener(v -> {
            new AlertDialog.Builder(ViewProfile.this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Get userId before clearing preferences
                        SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                        String userId = prefs.getString("userId", null);

                        // Cancel message checks if userId exists
                        if (userId != null) {
                            NotificationWorkManager.cancelMessageChecks(ViewProfile.this, userId);
                        }

                        // Stop notification service
                        Intent serviceIntent = new Intent(ViewProfile.this, NotificationWorkManager.class);
                        stopService(serviceIntent);

                        // Clear session data
                        prefs.edit().clear().apply();

                        // Sign out from Firebase
                        FirebaseAuth.getInstance().signOut();

                        Intent logoutIntent = new Intent(ViewProfile.this, Login.class);
                        logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(logoutIntent);
                        finish();
                    })
                    .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                    .show();
        });

        // Initialize TextViews
        firstName = findViewById(R.id.firstName);
        middleName = findViewById(R.id.middleName);
        lastName = findViewById(R.id.lastName);
        userAge = findViewById(R.id.userAge);
        phoneNumber = findViewById(R.id.phoneNumber);
        usernameTextView = findViewById(R.id.usernameTextView);
        gender = findViewById(R.id.gender);
        houseno = findViewById(R.id.houseno);
        street = findViewById(R.id.street);
        barangay = findViewById(R.id.barangay);
        editViewProfile = findViewById(R.id.editViewProfile);
        birthday = findViewById(R.id.birthday);
        editInterests = findViewById(R.id.editInterests);
        btnViewUserEvents = findViewById(R.id.btnViewUserEvents);
        userStatusText = findViewById(R.id.userStatusText);
        userStatusContainer = findViewById(R.id.userStatusContainer);

        // Initialize bottom navigation buttons
        navHome = findViewById(R.id.navHome);
        navCommunity = findViewById(R.id.navCommunity);
        navProfile = findViewById(R.id.navProfile);
        navEventCalendar = findViewById(R.id.navEventCalendar);
        navNotifications = findViewById(R.id.navNotifications);

        // Initialize badge managers
        badgeManager = MessageBadgeManager.getInstance();
        notificationBadgeManager = NotificationBadgeManager.getInstance();

        // Initialize BottomNavigation class with the necessary data
        bottomNavigation = new BottomNavigation(userId, uaddressId, unameId, uotherDetails);
        bottomNavigation.setupBottomNavigation(navHome, navCommunity, navProfile, navEventCalendar, navNotifications, this);

        editInterests.setOnClickListener(v -> {
            Intent intent = new Intent(ViewProfile.this, EditInterests.class);
            intent.putExtra("uotherDetails", uotherDetails);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        btnViewUserEvents.setOnClickListener(v -> {
            Intent intent = new Intent(ViewProfile.this, UserEvents.class);
            intent.putExtra("userId", userId);
            intent.putExtra("uaddressId", uaddressId);
            intent.putExtra("unameId", unameId);
            intent.putExtra("uotherDetails", uotherDetails);
            startActivity(intent);
        });

        imgbtnMessages = findViewById(R.id.imgbtnMessages);

        // Set up message badge
        setupMessagesBadge();

        // Set up notification badge
        setupNotificationBadge();

        // Fetch and display user data from Firestore
        loadUserProfile(userId);
        loadUsername(userId);
        loadUserStatus(userId);
        fetchAndDisplayUserDetails(userId);
        loadProfilePicture(userId);

        // Set click listener to redirect to EditViewProfile with user data
        editViewProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ViewProfile.this, EditViewProfile.class);
            intent.putExtra("userId", userId);
            intent.putExtra("firstName", firstName.getText().toString());
            intent.putExtra("middleName", middleName.getText().toString());
            intent.putExtra("lastName", lastName.getText().toString());
            intent.putExtra("age", userAge.getText().toString());
            intent.putExtra("phoneNo", phoneNumber.getText().toString());
            intent.putExtra("gender", gender.getText().toString());
            intent.putExtra("houseNo", houseno.getText().toString());
            intent.putExtra("street", street.getText().toString());
            intent.putExtra("barangay", barangay.getText().toString());
            intent.putExtra("dateOfBirth", birthday.getText().toString());
            intent.putExtra("username", usernameTextView.getText().toString());
            startActivityForResult(intent, 1);
        });
    }

    private void setupMessagesBadge() {
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
            Intent intent = new Intent(ViewProfile.this, Messages.class);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getBooleanExtra("refresh", false)) {
            loadUserProfile(userId); // Refresh user profile
            loadProfilePicture(userId); // Reload profile picture too
            loadUserStatus(userId);
        }
    }

    // New method to load profile picture
    private void loadProfilePicture(String userId) {
        db.collection("usersOtherDetails")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String profilePictureUrl = document.getString("profilePictureUrl");

                        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                            // Use Glide with circleCrop() to make the image circular
                            Glide.with(this)
                                    .load(profilePictureUrl)
                                    .apply(RequestOptions.circleCropTransform()) // This makes the image circular
                                    .placeholder(R.drawable.user)
                                    .error(R.drawable.user)
                                    .into(imgProfilePic);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewProfile", "Error loading profile picture", e);
                });
    }
    private void loadUserProfile(String userId) {
        db.collection("usersName")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("ViewProfile", "Query successful. Number of documents found: " + queryDocumentSnapshots.size());

                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        Log.d("ViewProfile", "Document found: " + document.getData());

                        // Load string fields
                        firstName.setText(document.getString("firstName"));
                        middleName.setText(document.getString("middleName"));
                        lastName.setText(document.getString("lastName"));

                        // Load other details
                        loadAdditionalUserDetails(userId);
                    } else {
                        Log.d("ViewProfile", "No matching documents found for userId: " + userId);
                        Toast.makeText(ViewProfile.this, "User profile not found for userId: " + userId, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ViewProfile.this, "Failed to reload profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ViewProfile", "Error loading user profile", e);
                });
    }

    private void loadUserStatus(String userId) {
        db.collection("usersAccount")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String status = document.getString("usersStatus");

                        if (status != null) {
                            userStatusText.setText(status);

                            // Style based on verification status
                            if (status.equals("Verified")) {
                                userStatusContainer.setBackgroundResource(R.drawable.verified_status_bg);
                                userStatusText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_verified, 0, 0, 0);
                                userStatusText.setTextColor(getResources().getColor(R.color.white));
                            } else {
                                userStatusContainer.setBackgroundResource(R.drawable.unverified_status_bg);
                                userStatusText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_unverified, 0, 0, 0);
                                userStatusText.setTextColor(getResources().getColor(R.color.white));
                            }
                        } else {
                            userStatusText.setText("Unverified");
                            userStatusContainer.setBackgroundResource(R.drawable.unverified_status_bg);
                            userStatusText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_unverified, 0, 0, 0);
                            userStatusText.setTextColor(getResources().getColor(R.color.white));
                        }
                    } else {
                        Toast.makeText(ViewProfile.this, "User status not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ViewProfile.this, "Failed to load status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ViewProfile", "Error loading user status", e);
                });
    }

    private void loadAdditionalUserDetails(String userId) {
        db.collection("usersOtherDetails")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);

                        // Load age
                        Long ageValue = document.getLong("age");
                        if (ageValue != null) {
                            userAge.setText(String.valueOf(ageValue));
                        }

                        // Load phone number
                        Long phoneValue = document.getLong("phoneNo");
                        if (phoneValue != null) {
                            phoneNumber.setText(String.valueOf(phoneValue));
                        }

                        // Load gender
                        gender.setText(document.getString("gender"));

                        // Load date of birth
                        birthday.setText(document.getString("dateOfBirth"));

                        // Fetch address
                        loadUserAddress(userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ViewProfile.this, "Failed to load additional details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ViewProfile", "Error loading additional details", e);
                });
    }

    private void loadUserAddress(String userId) {
        db.collection("usersAddress")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);

                        // Load house number
                        Long houseValue = document.getLong("houseNo");
                        if (houseValue != null) {
                            houseno.setText(String.valueOf(houseValue));
                        }

                        // Load street
                        street.setText(document.getString("street"));

                        // Handle barangay or otherBarangay with municipality
                        String barangayValue = document.getString("barangay");
                        String otherBarangayValue = document.getString("otherBarangay");
                        String municipalityValue = document.getString("municipality");

                        if (barangayValue != null && !barangayValue.isEmpty()) {
                            // Use regular barangay if available
                            barangay.setText(barangayValue);
                        } else if (otherBarangayValue != null && !otherBarangayValue.isEmpty()) {
                            // Use otherBarangay and municipality if available
                            if (municipalityValue != null && !municipalityValue.isEmpty()) {
                                barangay.setText(otherBarangayValue + ", " + municipalityValue);
                            } else {
                                barangay.setText(otherBarangayValue);
                            }
                        } else {
                            // Set empty text if neither is available
                            barangay.setText("");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ViewProfile.this, "Failed to load address: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ViewProfile", "Error loading address", e);
                });
    }

    private void loadUsername(String userId) {
        db.collection("usersAccount")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String username = document.getString("username");
                        if (username != null) {
                            usernameTextView.setText(username);
                        } else {
                            Toast.makeText(ViewProfile.this, "Username not found for userId: " + userId, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ViewProfile.this, "No account found for userId: " + userId, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ViewProfile.this, "Failed to load username: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ViewProfile", "Error loading username", e);
                });
    }

    private void fetchAndDisplayUserDetails(String userId) {
        // Change from document(userId) to using whereEqualTo
        db.collection("usersOtherDetails")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Get the first matching document
                        DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
                        Log.d("FirestoreData", "Document Data: " + documentSnapshot.getData());

                        // Make sure to cast to List<String>
                        @SuppressWarnings("unchecked")
                        List<String> skills = (List<String>) documentSnapshot.get("skills");
                        @SuppressWarnings("unchecked")
                        List<String> interests = (List<String>) documentSnapshot.get("interests");

                        displaySkills(skills);
                        displayInterests(interests);
                    } else {
                        Log.d("ViewProfile", "Document not found");
                        Toast.makeText(this, "User details not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewProfile", "Error fetching user details", e);
                    Toast.makeText(this, "Error fetching details", Toast.LENGTH_SHORT).show();
                });
    }
    private void displaySkills(List<String> skills) {
        FlexboxLayout skillsContainer = findViewById(R.id.containerSkills);
        skillsContainer.removeAllViews(); // Clear previous views

        if (skills != null && !skills.isEmpty()) {
            for (String skill : skills) {
                TextView skillTextView = new TextView(this);
                skillTextView.setText(skill);
                skillTextView.setTextColor(getResources().getColor(R.color.white));
                skillTextView.setTextSize(14);
                skillTextView.setPadding(15, 10, 15, 10);
                skillTextView.setBackground(getResources().getDrawable(R.drawable.border3));

                FlexboxLayout.LayoutParams layoutParams = new FlexboxLayout.LayoutParams(
                        FlexboxLayout.LayoutParams.WRAP_CONTENT,
                        FlexboxLayout.LayoutParams.WRAP_CONTENT
                );
                layoutParams.setMargins(8, 8, 8, 8);
                skillTextView.setLayoutParams(layoutParams);

                skillsContainer.addView(skillTextView);
            }
        } else {
            Toast.makeText(this, "No skills found", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayInterests(List<String> interests) {
        FlexboxLayout interestsContainer = findViewById(R.id.containerInterests);
        interestsContainer.removeAllViews(); // Clear previous views

        if (interests != null && !interests.isEmpty()) {
            for (String interest : interests) {
                TextView interestTextView = new TextView(this);
                interestTextView.setText(interest);
                interestTextView.setTextColor(getResources().getColor(R.color.white));
                interestTextView.setTextSize(14);
                interestTextView.setPadding(15, 10, 15, 10);
                interestTextView.setBackground(getResources().getDrawable(R.drawable.border3));

                FlexboxLayout.LayoutParams layoutParams = new FlexboxLayout.LayoutParams(
                        FlexboxLayout.LayoutParams.WRAP_CONTENT,
                        FlexboxLayout.LayoutParams.WRAP_CONTENT
                );
                layoutParams.setMargins(8, 8, 8, 8);
                interestTextView.setLayoutParams(layoutParams);

                interestsContainer.addView(interestTextView);
            }
        } else {
            Toast.makeText(this, "No interests found", Toast.LENGTH_SHORT).show();
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