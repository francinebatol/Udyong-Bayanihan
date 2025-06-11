package com.example.udyongbayanihan;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LikesDetails extends AppCompatActivity {
    private RecyclerView likesRecyclerView;
    private TextView noLikesText, titleLikes;
    private FirebaseFirestore db;
    private LikesAdapter likesAdapter;
    private List<LikeUser> likeUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_likes_details);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        likesRecyclerView = findViewById(R.id.likesRecyclerView);
        noLikesText = findViewById(R.id.noLikesText);
        titleLikes = findViewById(R.id.titleLikes);

        // Get postId from intent
        String postId = getIntent().getStringExtra("postId");

        // Setup RecyclerView
        likeUsers = new ArrayList<>();
        likesAdapter = new LikesAdapter(this, likeUsers);
        likesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        likesRecyclerView.setAdapter(likesAdapter);

        // Fetch likes
        fetchLikesForPost(postId);
    }

    private void fetchLikesForPost(String postId) {
        // First, fetch all users who liked the post
        db.collection("CommunityLikes")
                .whereEqualTo("postId", postId)
                .get()
                .addOnSuccessListener(likeSnapshots -> {
                    if (likeSnapshots.isEmpty()) {
                        // No likes
                        noLikesText.setVisibility(View.VISIBLE);
                        likesRecyclerView.setVisibility(View.GONE);
                        return;
                    }

                    // Fetch user details for each like
                    db.collection("usersName")
                            .whereIn("userId", likeSnapshots.getDocuments().stream()
                                    .map(doc -> doc.getString("userId"))
                                    .collect(Collectors.toList()))
                            .get()
                            .addOnSuccessListener(userSnapshots -> {
                                likeUsers.clear();
                                for (QueryDocumentSnapshot userDoc : userSnapshots) {
                                    String userId = userDoc.getString("userId");
                                    String firstName = userDoc.getString("firstName");
                                    String lastName = userDoc.getString("lastName");

                                    // Fetch profile picture instead of address
                                    fetchUserProfilePicture(userId, firstName, lastName);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("LikesError", "Error fetching user details", e);
                                Toast.makeText(this, "Error loading likes", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("LikesError", "Error fetching likes", e);
                    Toast.makeText(this, "Error loading likes", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchUserProfilePicture(String userId, String firstName, String lastName) {
        Log.d("LikesDebug", "Fetching profile picture for user: " + userId);

        // Query by userId field instead of using document ID
        db.collection("usersOtherDetails")
                .whereEqualTo("userId", userId)  // Changed from document(userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Get the first matching document
                        Log.d("LikesDebug", "Found document for user: " + userId);
                        String profilePictureUrl = querySnapshot.getDocuments().get(0).getString("profilePictureUrl");
                        Log.d("LikesDebug", "Profile URL: " + (profilePictureUrl != null ? profilePictureUrl : "null"));

                        String fullName = firstName + " " + lastName;
                        LikeUser likeUser = new LikeUser(userId, fullName, profilePictureUrl);
                        likeUsers.add(likeUser);
                    } else {
                        Log.d("LikesDebug", "No document found for user: " + userId);
                        // Add user without profile picture
                        String fullName = firstName + " " + lastName;
                        LikeUser likeUser = new LikeUser(userId, fullName, null);
                        likeUsers.add(likeUser);
                    }

                    // Update adapter when user is fetched
                    likesAdapter.notifyDataSetChanged();

                    // Update UI visibility
                    noLikesText.setVisibility(likeUsers.isEmpty() ? View.VISIBLE : View.GONE);
                    likesRecyclerView.setVisibility(likeUsers.isEmpty() ? View.GONE : View.VISIBLE);
                    titleLikes.setText("People Who Liked This Post (" + likeUsers.size() + ")");
                })
                .addOnFailureListener(e -> {
                    Log.e("LikesError", "Error fetching user profile picture for " + userId, e);

                    // Still add the user to the list even if we couldn't get their profile picture
                    String fullName = firstName + " " + lastName;
                    LikeUser likeUser = new LikeUser(userId, fullName, null);
                    likeUsers.add(likeUser);

                    // Update UI
                    likesAdapter.notifyDataSetChanged();
                    noLikesText.setVisibility(likeUsers.isEmpty() ? View.VISIBLE : View.GONE);
                    likesRecyclerView.setVisibility(likeUsers.isEmpty() ? View.GONE : View.VISIBLE);
                    titleLikes.setText("People Who Liked This Post (" + likeUsers.size() + ")");
                });
    }
}