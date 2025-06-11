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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminSeeComments extends AppCompatActivity {
    private static final String TAG = "AdminSeeComments";

    private FirebaseFirestore db;
    private List<Comment> commentsList;
    private AdminCommentsAdapter commentsAdapter;
    private TextView originalPostAdminName, originalPostPosition,
            originalPostContent, originalPostTimestamp;
    private RecyclerView commentsRecyclerView;
    private EditText editTextComment;
    private ImageButton buttonSendComment, btnBack;
    private SimpleDateFormat dateFormat;
    private String adminId;
    private String postId;
    private String adminName;
    private ListenerRegistration commentsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_see_comments);

        // Initialize Firebase and views
        db = FirebaseFirestore.getInstance();
        dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());

        // Get intent extras
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");
        adminId = intent.getStringExtra("adminId");
        adminName = intent.getStringExtra("adminName");
        String position = intent.getStringExtra("position");
        String content = intent.getStringExtra("content");
        Timestamp timestamp = null;
        if (intent.hasExtra("timestampMillis")) {
            long timestampMillis = intent.getLongExtra("timestampMillis", 0);
            if (timestampMillis > 0) {
                timestamp = new Timestamp(new Date(timestampMillis));
            }
        }

        Log.d(TAG, "postId: " + postId);
        Log.d(TAG, "adminId: " + adminId);
        Log.d(TAG, "adminName: " + adminName);

        if (postId == null || adminId == null) {
            Toast.makeText(this, "Error: Missing required data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();

        // Set post details directly from intent
        originalPostAdminName.setText(adminName);
        originalPostPosition.setText(position);
        originalPostContent.setText(content);
        if (timestamp != null) {
            originalPostTimestamp.setText(dateFormat.format(timestamp.toDate()));
        }

        setupRecyclerView();
        setupCommentInput();
        setupCommentsListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commentsListener != null) {
            commentsListener.remove();
        }
    }

    private void initializeViews() {
        originalPostAdminName = findViewById(R.id.originalPostAdminName);
        originalPostPosition = findViewById(R.id.originalPostPosition);
        originalPostContent = findViewById(R.id.originalPostContent);
        originalPostTimestamp = findViewById(R.id.originalPostTimestamp);
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        editTextComment = findViewById(R.id.editTextComment);
        buttonSendComment = findViewById(R.id.buttonSendComment);
        btnBack = findViewById(R.id.btnBack);

        // Set up back button
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        commentsList = new ArrayList<>();
        commentsAdapter = new AdminCommentsAdapter(this, commentsList);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsRecyclerView.setAdapter(commentsAdapter);
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
                        for (QueryDocumentSnapshot document : value) {
                            Comment comment = document.toObject(Comment.class);
                            commentsList.add(comment);
                        }
                        // Sort comments (pinned first)
                        commentsAdapter.sortComments();

                        if (commentsList.size() > 0) {
                            commentsRecyclerView.smoothScrollToPosition(commentsList.size() - 1);
                        }
                    }
                });
    }

    private void addComment(String commentText) {
        Log.d(TAG, "Adding comment: " + commentText);
        if (adminId == null || adminId.isEmpty()) {
            Log.e(TAG, "adminId is null or empty");
            Toast.makeText(this, "Error: Admin ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get admin name and position for the comment
        db.collection("AMNameDetails")
                .whereEqualTo("amAccountId", adminId)
                .get()
                .addOnSuccessListener(nameSnapshot -> {
                    if (!nameSnapshot.isEmpty()) {
                        DocumentSnapshot document = nameSnapshot.getDocuments().get(0);
                        String firstName = document.getString("amFirstName");
                        String lastName = document.getString("amLastName");
                        String fullName = firstName + " " + lastName;

                        db.collection("AMOtherDetails")
                                .whereEqualTo("amAccountId", adminId)
                                .get()
                                .addOnSuccessListener(otherSnapshot -> {
                                    String position = "Admin";
                                    if (!otherSnapshot.isEmpty()) {
                                        DocumentSnapshot otherDoc = otherSnapshot.getDocuments().get(0);
                                        position = otherDoc.getString("position");
                                    }

                                    // Create comment with admin information
                                    String commentId = db.collection("CommunityComments").document().getId();
                                    Comment comment = new Comment(
                                            commentId,
                                            postId,
                                            adminId,
                                            fullName,
                                            commentText,
                                            Timestamp.now()
                                    );
                                    // Admin comments are distinguished by fullName and the fact they're from an admin

                                    db.collection("CommunityComments")
                                            .document(commentId)
                                            .set(comment)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d(TAG, "Comment added successfully");
                                                Toast.makeText(AdminSeeComments.this, "Comment added successfully", Toast.LENGTH_SHORT).show();
                                                editTextComment.setText("");
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Error adding comment", e);
                                                Toast.makeText(AdminSeeComments.this, "Error adding comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error fetching admin details", e);
                                    Toast.makeText(AdminSeeComments.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Log.e(TAG, "No admin document found for adminId: " + adminId);
                        Toast.makeText(AdminSeeComments.this, "Error: Admin data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching admin data", e);
                    Toast.makeText(AdminSeeComments.this, "Error fetching admin data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}