package com.example.udyongbayanihan;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class AdminBarangayPostsFragment extends Fragment {
    private static final String TAG = "BarangayPostsFragment";
    private static final String ARG_ADMIN_ID = "admin_id";
    private static final String ARG_ADMIN_NAME = "admin_name";
    private static final String ARG_BARANGAY = "barangay";

    private RecyclerView recyclerView;
    private TextView noPostsText;
    private FirebaseFirestore db;
    private AdminPostAdapter adapter;
    private ArrayList<AdminPostItem> posts;
    private String adminId;
    private String adminName;
    private String barangay;
    private Map<String, ListenerRegistration> likeListeners = new HashMap<>();
    private Map<String, ListenerRegistration> commentListeners = new HashMap<>();

    // Flag to track if we're currently loading data
    private boolean isLoading = false;

    // Flag to track if this instance is active or being replaced
    private boolean isActive = true;

    public static AdminBarangayPostsFragment newInstance(String adminId, String adminName, String barangay) {
        AdminBarangayPostsFragment fragment = new AdminBarangayPostsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ADMIN_ID, adminId);
        args.putString(ARG_ADMIN_NAME, adminName);
        args.putString(ARG_BARANGAY, barangay);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            adminId = getArguments().getString(ARG_ADMIN_ID);
            adminName = getArguments().getString(ARG_ADMIN_NAME);
            barangay = getArguments().getString(ARG_BARANGAY);
        }
        db = FirebaseFirestore.getInstance();
        posts = new ArrayList<>();
        Log.d(TAG, "Fragment instance created: " + this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_posts, container, false);
        recyclerView = view.findViewById(R.id.postsRecyclerView);
        noPostsText = view.findViewById(R.id.noPostsText);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRecyclerView();
        loadBarangayPosts();
    }

    @Override
    public void onStart() {
        super.onStart();
        isActive = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        isActive = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called for fragment: " + this);

        // Remove listeners to prevent memory leaks
        removeAllListeners();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Fragment instance destroyed: " + this);
    }

    private void removeAllListeners() {
        // Clear like listeners
        for (ListenerRegistration registration : likeListeners.values()) {
            registration.remove();
        }
        likeListeners.clear();

        // Clear comment listeners
        for (ListenerRegistration registration : commentListeners.values()) {
            registration.remove();
        }
        commentListeners.clear();
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume called for fragment: " + this);

        // Only reload if we're not already loading and the fragment is properly initialized
        if (recyclerView != null && !isLoading) {
            Log.d(TAG, "AdminBarangayPostsFragment onResume - reloading posts");
            loadBarangayPosts();
        }
    }

    private synchronized void loadBarangayPosts() {
        // Prevent concurrent or duplicate loads
        if (isLoading || !isActive) {
            Log.d(TAG, "Skipping load - already loading or fragment inactive");
            return;
        }

        Log.d(TAG, "Starting loadBarangayPosts for: " + this);
        isLoading = true;

        // Show loading state
        recyclerView.setVisibility(View.GONE);
        noPostsText.setVisibility(View.GONE);

        // Clear existing posts and listeners
        posts.clear();
        removeAllListeners();

        if (barangay == null || barangay.isEmpty()) {
            noPostsText.setText("Your barangay information is not available");
            noPostsText.setVisibility(View.VISIBLE);
            isLoading = false;
            return;
        }

        // Load posts from barangay collection - order by timestamp descending to get newest first
        db.collection("CommunityGroups")
                .document(barangay)
                .collection("Posts")
                .whereEqualTo("adminName", adminName)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(barangaySnapshot -> {
                    processQueryResults(barangaySnapshot);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading barangay posts", e);
                    showNoPostsMessage();
                    isLoading = false;
                });
    }

    private void processQueryResults(QuerySnapshot barangaySnapshot) {
        if (!isActive) {
            Log.d(TAG, "Fragment no longer active, discarding results");
            isLoading = false;
            return;
        }

        Log.d(TAG, "Query returned " + barangaySnapshot.size() + " documents");

        // Use a set to ensure we have no duplicate post IDs
        Set<String> addedPostIds = new HashSet<>();

        for (QueryDocumentSnapshot document : barangaySnapshot) {
            String postId = document.getId();

            // Skip if we already have this post ID
            if (addedPostIds.contains(postId)) {
                Log.d(TAG, "Skipping duplicate post ID: " + postId);
                continue;
            }

            Log.d(TAG, "Processing post with ID: " + postId);
            addedPostIds.add(postId);

            // Create a new post item
            AdminPostItem post = new AdminPostItem(
                    postId,
                    document.getString("adminName"),
                    document.getString("position"),
                    document.getString("postContent"),
                    document.getTimestamp("timestamp")
            );
            post.setBarangay(barangay);
            posts.add(post);

            // Set up real-time listeners for likes and comments
            setupLikesListener(post);
            setupCommentsListener(post);
        }

        // Update UI
        if (posts.isEmpty()) {
            showNoPostsMessage();
        } else {
            Log.d(TAG, "Showing " + posts.size() + " posts in recycler view");
            recyclerView.setVisibility(View.VISIBLE);
            noPostsText.setVisibility(View.GONE);

            // Create a new adapter instead of notifying the existing one
            setupRecyclerView();
        }

        isLoading = false;
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminPostAdapter(getContext(), posts, adminId);
        recyclerView.setAdapter(adapter);
    }

    private void setupLikesListener(AdminPostItem post) {
        ListenerRegistration likesListener = db.collection("CommunityLikes")
                .whereEqualTo("postId", post.getPostId())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed for likes", error);
                        return;
                    }

                    if (value != null && isActive) {
                        post.setLikesCount(value.size());
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    }
                });

        likeListeners.put(post.getPostId(), likesListener);
    }

    private void setupCommentsListener(AdminPostItem post) {
        ListenerRegistration commentsListener = db.collection("CommunityComments")
                .whereEqualTo("postId", post.getPostId())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed for comments", error);
                        return;
                    }

                    if (value != null && isActive) {
                        post.setCommentsCount(value.size());
                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    }
                });

        commentListeners.put(post.getPostId(), commentsListener);
    }

    private void showNoPostsMessage() {
        recyclerView.setVisibility(View.GONE);
        noPostsText.setVisibility(View.VISIBLE);
        noPostsText.setText("No posts in your barangay");
    }
}