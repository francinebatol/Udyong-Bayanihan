package com.example.udyongbayanihan;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchMessage extends AppCompatActivity {
    private FirebaseFirestore db;
    private RecyclerView usersList;
    private UserAdapter userAdapter;
    private List<UserModel> users;
    private EditText searchBar;
    private String currentUserId;
    private String currentUserType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_message);

        db = FirebaseFirestore.getInstance();
        users = new ArrayList<>();

        // Get the current user's ID and type from intent
        currentUserId = getIntent().getStringExtra("userId");
        currentUserType = getIntent().getStringExtra("userType");

        // Initialize views
        usersList = findViewById(R.id.usersList);
        searchBar = findViewById(R.id.searchBar);

        // Setup RecyclerView with the correct constructor parameters
        userAdapter = new UserAdapter(users, currentUserId, currentUserType);
        usersList.setLayoutManager(new LinearLayoutManager(this));
        usersList.setAdapter(userAdapter);

        // Load all users initially
        loadUsers("");

        // Setup search functionality
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void loadUsers(String searchQuery) {
        users.clear();
        userAdapter.notifyDataSetChanged();

        List<UserModel> tempUsers = new ArrayList<>();

        // Don't show the current user in the search results
        db.collection("usersName")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String userId = document.getString("userId");

                        // Skip if this is the current user
                        if (userId != null && userId.equals(currentUserId)) {
                            continue;
                        }

                        String firstName = document.getString("firstName");
                        String lastName = document.getString("lastName");

                        if (firstName != null && lastName != null &&
                                (searchQuery.isEmpty() ||
                                        firstName.toLowerCase().contains(searchQuery.toLowerCase()) ||
                                        lastName.toLowerCase().contains(searchQuery.toLowerCase()))) {

                            UserModel user = new UserModel(userId, firstName, lastName, "user");
                            tempUsers.add(user);
                        }
                    }
                    queryAdminUsers(searchQuery, tempUsers);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SearchMessage.this, "Error loading users", Toast.LENGTH_SHORT).show();
                });
    }

    private void queryAdminUsers(String searchQuery, List<UserModel> tempUsers) {
        db.collection("AMNameDetails")
                .get()
                .addOnSuccessListener(adminSnapshots -> {
                    List<UserModel> adminsToCheck = new ArrayList<>();

                    for (QueryDocumentSnapshot document : adminSnapshots) {
                        String adminId = document.getString("amAccountId");

                        // Skip if this is the current user
                        if (adminId != null && adminId.equals(currentUserId)) {
                            continue;
                        }

                        String firstName = document.getString("amFirstName");
                        String lastName = document.getString("amLastName");

                        if (firstName != null && lastName != null &&
                                (searchQuery.isEmpty() ||
                                        firstName.toLowerCase().contains(searchQuery.toLowerCase()) ||
                                        lastName.toLowerCase().contains(searchQuery.toLowerCase()))) {

                            // Store admin info but don't add to result list yet
                            // We'll check their status first
                            UserModel admin = new UserModel(adminId, firstName, lastName, "admin");
                            adminsToCheck.add(admin);
                        }
                    }

                    // Now check status for all potential admin users
                    checkAdminStatus(adminsToCheck, tempUsers);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SearchMessage.this, "Error loading admin users", Toast.LENGTH_SHORT).show();
                    // Still update the UI with regular users if admin loading fails
                    updateUsersList(tempUsers);
                });
    }

    private void checkAdminStatus(List<UserModel> adminsToCheck, List<UserModel> tempUsers) {
        if (adminsToCheck.isEmpty()) {
            // No admins to check, update list with just regular users
            updateUsersList(tempUsers);
            return;
        }

        // Counter to track how many admin status checks have completed
        final int[] completedChecks = {0};
        final int totalAdmins = adminsToCheck.size();

        for (UserModel admin : adminsToCheck) {
            // Directly get the document by ID since amAccountId is the document ID in AdminMobileAccount
            db.collection("AdminMobileAccount")
                    .document(admin.getId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        boolean isActive = true; // Default to active if no status found

                        if (documentSnapshot.exists()) {
                            String status = documentSnapshot.getString("Status");
                            // Only set to inactive if explicitly "Inactive"
                            isActive = status == null || !status.equals("Inactive");
                        }

                        // Add admin to results only if active
                        if (isActive) {
                            tempUsers.add(admin);
                        }

                        // Increment completed checks counter
                        completedChecks[0]++;

                        // Update UI when all checks are complete
                        if (completedChecks[0] >= totalAdmins) {
                            updateUsersList(tempUsers);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Count this as a completed check even on failure
                        completedChecks[0]++;

                        // Update UI when all checks are complete
                        if (completedChecks[0] >= totalAdmins) {
                            updateUsersList(tempUsers);
                        }
                    });
        }
    }

    private void updateUsersList(List<UserModel> tempUsers) {
        users.clear();
        users.addAll(tempUsers);
        userAdapter.notifyDataSetChanged();
    }
}