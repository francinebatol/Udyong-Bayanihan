package com.example.udyongbayanihan;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
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

public class AdminLikesDetails extends AppCompatActivity {
    private static final String TAG = "AdminLikesDetails";

    private RecyclerView likesRecyclerView;
    private TextView noLikesText, titleLikes;
    private ImageButton btnBack;
    private FirebaseFirestore db;
    private LikesAdapter likesAdapter;
    private List<LikeUser> likeUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_likes_details);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize views
        likesRecyclerView = findViewById(R.id.likesRecyclerView);
        noLikesText = findViewById(R.id.noLikesText);
        titleLikes = findViewById(R.id.titleLikes);
        btnBack = findViewById(R.id.btnBack);

        // Set up back button
        btnBack.setOnClickListener(v -> finish());

        // Get postId from intent
        String postId = getIntent().getStringExtra("postId");
        String adminName = getIntent().getStringExtra("adminName");

        if (postId == null) {
            Toast.makeText(this, "Error: Post ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set title
        titleLikes.setText("People Who Liked " + (adminName != null ? adminName + "'s" : "This") + " Post");

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
                        titleLikes.setText(titleLikes.getText() + " (0)");
                        return;
                    }

                    List<String> userIds = likeSnapshots.getDocuments().stream()
                            .map(doc -> doc.getString("userId"))
                            .collect(Collectors.toList());

                    // Fetch user details for each like
                    db.collection("usersName")
                            .whereIn("userId", userIds)
                            .get()
                            .addOnSuccessListener(userSnapshots -> {
                                for (QueryDocumentSnapshot userDoc : userSnapshots) {
                                    String userId = userDoc.getString("userId");
                                    String firstName = userDoc.getString("firstName");
                                    String lastName = userDoc.getString("lastName");

                                    // Fetch user address
                                    fetchUserAddress(userId, firstName, lastName);
                                }

                                // Update title with count
                                titleLikes.setText(titleLikes.getText() + " (" + userIds.size() + ")");
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error fetching user details", e);
                                Toast.makeText(this, "Error loading likes", Toast.LENGTH_SHORT).show();
                                titleLikes.setText(titleLikes.getText() + " (" + userIds.size() + ")");
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching likes", e);
                    Toast.makeText(this, "Error loading likes", Toast.LENGTH_SHORT).show();
                    titleLikes.setText(titleLikes.getText() + " (0)");
                });
    }

    private void fetchUserAddress(String userId, String firstName, String lastName) {
        db.collection("usersAddress")
                .document(userId)
                .get()
                .addOnSuccessListener(addressSnapshot -> {
                    String houseNo = addressSnapshot.getString("houseNo");
                    String street = addressSnapshot.getString("street");
                    String barangay = addressSnapshot.getString("barangay");

                    String fullName = firstName + " " + lastName;
                    String address = (houseNo != null ? houseNo + " " : "") +
                            (street != null ? street + ", " : "") +
                            (barangay != null ? barangay : "");

                    LikeUser likeUser = new LikeUser(userId, fullName, address);
                    likeUsers.add(likeUser);

                    // Update adapter when all users are fetched
                    likesAdapter.notifyDataSetChanged();

                    // Update UI visibility
                    noLikesText.setVisibility(likeUsers.isEmpty() ? View.VISIBLE : View.GONE);
                    likesRecyclerView.setVisibility(likeUsers.isEmpty() ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user address", e);
                });
    }
}