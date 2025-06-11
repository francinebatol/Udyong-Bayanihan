package com.example.udyongbayanihan;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SkillsBasedComments extends AppCompatActivity {
    private static final String TAG = "SkillsBasedComments";

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
    private String skillName;
    private ListenerRegistration commentsListener;

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
        skillName = intent.getStringExtra("skill");
        userId = intent.getStringExtra("userId");

        Log.d(TAG, "Received data - postId: " + postId + ", skillName: " + skillName + ", userId: " + userId);

        if (postId == null || skillName == null || userId == null) {
            Toast.makeText(this, "Error: Missing required data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupRecyclerView();
        setupCommentInput();
        retrievePostAndComments();
        setupCommentsListener();
    }

    private void initializeViews() {
        originalPostAdminName = findViewById(R.id.originalPostAdminName);
        originalPostPosition = findViewById(R.id.originalPostPosition);
        originalPostContent = findViewById(R.id.originalPostContent);
        originalPostTimestamp = findViewById(R.id.originalPostTimestamp);
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        editTextComment = findViewById(R.id.editTextComment);
        buttonSendComment = findViewById(R.id.buttonSendComment);

        if (editTextComment == null || buttonSendComment == null) {
            Log.e(TAG, "Failed to initialize views");
            Toast.makeText(this, "Error initializing views", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Views initialized successfully");
    }

    private void setupRecyclerView() {
        commentsList = new ArrayList<>();
        commentsAdapter = new CommentsAdapter(this, commentsList);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsRecyclerView.setAdapter(commentsAdapter);
        Log.d(TAG, "RecyclerView setup complete");
    }

    private void setupCommentInput() {
        buttonSendComment.setOnClickListener(v -> {
            String commentText = editTextComment.getText().toString().trim();
            Log.d(TAG, "Send button clicked, comment text: " + commentText);
            if (!commentText.isEmpty()) {
                addComment(commentText);
            } else {
                Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupCommentsListener() {
        commentsListener = db.collection("CommunityComments")
                .whereEqualTo("postId", postId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed.", error);
                        return;
                    }

                    if (value != null) {
                        commentsList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Comment comment = doc.toObject(Comment.class);
                            commentsList.add(comment);
                        }

                        // Sort comments to show pinned comments at the top
                        commentsAdapter.sortComments();

                        // Update comments count in real-time
                        int commentsCount = commentsList.size();
                        updateCommentsCountInUI(commentsCount);

                        // Broadcast the update to SkillBasedEventsAdapter
                        Intent updateIntent = new Intent("COMMENT_COUNT_UPDATE");
                        updateIntent.putExtra("postId", postId);
                        updateIntent.putExtra("commentsCount", commentsCount);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(updateIntent);

                        if (commentsList.size() > 0) {
                            commentsRecyclerView.smoothScrollToPosition(commentsList.size() - 1);
                        }
                    }
                });
    }

    private void updateCommentsCountInUI(int count) {
        // Get reference to the comments count TextView if you have one in your layout
        TextView commentsCountView = findViewById(R.id.textCommentsNumber);
        if (commentsCountView != null) {
            commentsCountView.setText(String.format("%d Comments", count));
        }

        // Update the title bar if needed
        if (getSupportActionBar() != null) {
            getSupportActionBar().setSubtitle(String.format("%d Comments", count));
        }
    }

    private void retrievePostAndComments() {
        db.collection("CommunityGroupSkills")
                .document(skillName)
                .collection("Posts")
                .document(postId)
                .get()
                .addOnSuccessListener(postSnapshot -> {
                    if (postSnapshot.exists()) {
                        Log.d(TAG, "Post document retrieved successfully");
                        originalPostAdminName.setText(postSnapshot.getString("adminName"));
                        originalPostPosition.setText(postSnapshot.getString("position"));
                        originalPostContent.setText(postSnapshot.getString("postContent"));

                        if (postSnapshot.getTimestamp("timestamp") != null) {
                            originalPostTimestamp.setText(dateFormat.format(
                                    postSnapshot.getTimestamp("timestamp").toDate()));
                        }
                    } else {
                        Log.e(TAG, "Post document does not exist");
                        Toast.makeText(this, "Post not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load post", e);
                    Toast.makeText(this, "Error loading post", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void addComment(String commentText) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "userId is null or empty");
            Toast.makeText(this, "Error: User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("usersName")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) querySnapshot.getDocuments().get(0);
                        String firstName = document.getString("firstName");
                        String lastName = document.getString("lastName");
                        String fullName = firstName + " " + lastName;

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
                                    Log.d(TAG, "Comment added successfully");
                                    Toast.makeText(this, "Comment added successfully", Toast.LENGTH_SHORT).show();
                                    editTextComment.setText("");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error adding comment", e);
                                    Toast.makeText(this, "Error adding comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.e(TAG, "No user document found for userId: " + userId);
                        Toast.makeText(this, "Error: User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user data", e);
                    Toast.makeText(this, "Error fetching user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commentsListener != null) {
            commentsListener.remove();
        }
    }
}