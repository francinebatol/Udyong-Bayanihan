package com.example.udyongbayanihan;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SeeComments extends AppCompatActivity {
    private static final String TAG = "SeeComments";

    private FirebaseFirestore db;
    private List<Comment> commentsList;
    private CommentsAdapter commentsAdapter;
    private TextView originalPostAdminName, originalPostPosition,
            originalPostContent, originalPostTimestamp;
    private RecyclerView commentsRecyclerView;
    private EditText editTextComment;
    private ImageButton buttonSendComment;
    private SimpleDateFormat dateFormat;
    private String userId;
    private String postId;
    private String barangayName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_see_comments);

        // Initialize Firebase and views
        db = FirebaseFirestore.getInstance();
        dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());

        // Get intent extras
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");
        barangayName = intent.getStringExtra("barangay");
        userId = intent.getStringExtra("userId");

        Log.d("SeeComments", "postId: " + postId);
        Log.d("SeeComments", "barangayName: " + barangayName);
        Log.d("SeeComments", "userId: " + userId);

        if (postId == null || barangayName == null || userId == null) {
            Toast.makeText(this, "Error: Missing required data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupRecyclerView();
        setupCommentInput();
        retrievePostAndComments();
    }

    private void initializeViews() {
        originalPostAdminName = findViewById(R.id.originalPostAdminName);
        originalPostPosition = findViewById(R.id.originalPostPosition);
        originalPostContent = findViewById(R.id.originalPostContent);
        originalPostTimestamp = findViewById(R.id.originalPostTimestamp);
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        editTextComment = findViewById(R.id.editTextComment);
        buttonSendComment = findViewById(R.id.buttonSendComment);
    }

    private void setupRecyclerView() {
        commentsList = new ArrayList<>();
        commentsAdapter = new CommentsAdapter(this, commentsList);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsRecyclerView.setAdapter(commentsAdapter);
    }

    private void setupCommentInput() {
        buttonSendComment.setOnClickListener(v -> {
            String commentText = editTextComment.getText().toString().trim();
            Log.d("SeeComments", "Send button clicked, comment text: " + commentText);
            if (!commentText.isEmpty()) {
                addComment(commentText);
            } else {
                Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addComment(String commentText) {
        Log.d("SeeComments", "Adding comment: " + commentText);
        if (userId == null || userId.isEmpty()) {
            Log.e("SeeComments", "userId is null or empty");
            Toast.makeText(this, "Error: User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find document where userId field matches
        db.collection("usersName")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Get the first matching document
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        String firstName = document.getString("firstName");
                        String lastName = document.getString("lastName");
                        String fullName = firstName + " " + lastName;
                        Log.d("SeeComments", "User fullName: " + fullName);

                        String commentId = db.collection("CommunityComments").document().getId();
                        Comment comment = new Comment(
                                commentId,
                                postId,
                                userId,
                                fullName,
                                commentText,
                                Timestamp.now()
                        );

                        db.collection("CommunityComments")
                                .document(commentId)
                                .set(comment)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("SeeComments", "Comment added successfully");
                                    Toast.makeText(SeeComments.this, "Comment added successfully", Toast.LENGTH_SHORT).show();
                                    editTextComment.setText("");
                                    fetchComments(postId);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("SeeComments", "Error adding comment", e);
                                    Toast.makeText(SeeComments.this, "Error adding comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.e("SeeComments", "No user document found for userId: " + userId);
                        Toast.makeText(SeeComments.this, "Error: User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SeeComments", "Error fetching user data", e);
                    Toast.makeText(SeeComments.this, "Error fetching user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void retrievePostAndComments() {
        if (postId == null || barangayName == null) {
            Toast.makeText(this, "Error: Missing post details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db.collection("CommunityGroups")
                .document(barangayName)
                .collection("Posts")
                .document(postId)
                .get()
                .addOnSuccessListener(postSnapshot -> {
                    if (postSnapshot.exists()) {
                        originalPostAdminName.setText(postSnapshot.getString("adminName"));
                        originalPostPosition.setText(postSnapshot.getString("position"));
                        originalPostContent.setText(postSnapshot.getString("postContent"));

                        if (postSnapshot.getTimestamp("timestamp") != null) {
                            originalPostTimestamp.setText(dateFormat.format(
                                    postSnapshot.getTimestamp("timestamp").toDate()));
                        }

                        fetchComments(postId);
                    } else {
                        Toast.makeText(this, "Post not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading post", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void fetchComments(String postId) {
        db.collection("CommunityComments")
                .whereEqualTo("postId", postId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    commentsList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Comment comment = document.toObject(Comment.class);
                        commentsList.add(comment);
                    }

                    // Sort comments to show pinned comments at the top
                    commentsAdapter.sortComments();

                    if (commentsList.size() > 0) {
                        commentsRecyclerView.smoothScrollToPosition(commentsList.size() - 1);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading comments: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}