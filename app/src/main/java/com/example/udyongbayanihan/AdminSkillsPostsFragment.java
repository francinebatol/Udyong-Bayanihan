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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminSkillsPostsFragment extends Fragment {
    private static final String TAG = "SkillsPostsFragment";
    private static final String ARG_ADMIN_ID = "admin_id";
    private static final String ARG_ADMIN_NAME = "admin_name";

    private RecyclerView recyclerView;
    private TextView noPostsText;
    private FirebaseFirestore db;
    private AdminPostAdapter adapter;
    private ArrayList<AdminPostItem> posts;
    private String adminId;
    private String adminName;
    private Map<String, ListenerRegistration> likeListeners = new HashMap<>();
    private Map<String, ListenerRegistration> commentListeners = new HashMap<>();
    // Keep track of posts we've already processed to avoid duplicates
    private Set<String> processedPostIds = new HashSet<>();

    public static AdminSkillsPostsFragment newInstance(String adminId, String adminName) {
        AdminSkillsPostsFragment fragment = new AdminSkillsPostsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ADMIN_ID, adminId);
        args.putString(ARG_ADMIN_NAME, adminName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            adminId = getArguments().getString(ARG_ADMIN_ID);
            adminName = getArguments().getString(ARG_ADMIN_NAME);

            // Debug admin name
            Log.d(TAG, "Admin Name from arguments: '" + adminName + "'");
            Log.d(TAG, "Admin ID from arguments: '" + adminId + "'");
        }
        db = FirebaseFirestore.getInstance();
        posts = new ArrayList<>();
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
        loadSkillsPosts();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload posts when fragment becomes visible again
        if (recyclerView != null) {
            Log.d(TAG, "AdminSkillsPostsFragment onResume - reloading posts");
            loadSkillsPosts();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove listeners to prevent memory leaks
        for (ListenerRegistration registration : likeListeners.values()) {
            registration.remove();
        }
        for (ListenerRegistration registration : commentListeners.values()) {
            registration.remove();
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminPostAdapter(getContext(), posts, adminId);
        recyclerView.setAdapter(adapter);
    }

    private void loadSkillsPosts() {
        // Show loading state
        recyclerView.setVisibility(View.GONE);
        noPostsText.setVisibility(View.GONE);

        // Clear existing posts and listeners
        posts.clear();
        processedPostIds.clear();  // Clear the set of processed post IDs

        for (ListenerRegistration registration : likeListeners.values()) {
            registration.remove();
        }
        likeListeners.clear();

        for (ListenerRegistration registration : commentListeners.values()) {
            registration.remove();
        }
        commentListeners.clear();

        // First, retrieve all available skills from Firestore
        fetchSkillsAndLoadPosts();
    }

    private void fetchSkillsAndLoadPosts() {
        // First, we'll use a default set of skills in case the dynamic fetch fails
        final List<String> defaultSkills = new ArrayList<>();
        defaultSkills.add("Dedication");
        defaultSkills.add("Leadership");
        defaultSkills.add("Teamwork");

        // Try to fetch skills dynamically
        db.collection("SkillCategories")
                .get()
                .addOnSuccessListener(skillsSnapshot -> {
                    List<String> skillsList = new ArrayList<>();

                    if (!skillsSnapshot.isEmpty()) {
                        // If we have skill categories stored, use them
                        for (QueryDocumentSnapshot doc : skillsSnapshot) {
                            String skillName = doc.getId();
                            skillsList.add(skillName);
                        }
                        Log.d(TAG, "Fetched " + skillsList.size() + " skills from database");
                    } else {
                        // Use default skills if none found
                        skillsList = defaultSkills;
                        Log.d(TAG, "Using default skills list");
                    }

                    // Now fetch posts for each skill
                    loadPostsForSkills(skillsList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch skills, using defaults", e);
                    // Use default skills if fetch fails
                    loadPostsForSkills(defaultSkills);
                });
    }

    private void loadPostsForSkills(List<String> skills) {
        Log.d(TAG, "Loading posts for " + skills.size() + " skills");

        // Use AtomicInteger to track completion of queries
        final AtomicInteger pendingQueries = new AtomicInteger(skills.size());
        final Map<String, AdminPostItem> postsMap = new HashMap<>();

        // Load posts from each skill category
        for (String skill : skills) {
            Log.d(TAG, "Fetching posts for skill: " + skill);

            db.collection("CommunityGroupSkills")
                    .document(skill)
                    .collection("Posts")
                    .get()
                    .addOnSuccessListener(skillsSnapshot -> {
                        Log.d(TAG, "Found " + skillsSnapshot.size() + " posts for skill: " + skill);

                        for (QueryDocumentSnapshot document : skillsSnapshot) {
                            String postId = document.getId();
                            String postAdminName = document.getString("adminName");

                            Log.d(TAG, "Post ID: " + postId + ", by: '" + postAdminName + "'");

                            // First check if the post is by this admin, using both name and ID if available
                            boolean isAdminPost = false;

                            // Check by admin name
                            if (postAdminName != null && postAdminName.equals(adminName)) {
                                isAdminPost = true;
                                Log.d(TAG, "Match by name");
                            }
                            // Also check by admin ID if it's stored in the post
                            else if (document.contains("adminId") && adminId.equals(document.getString("adminId"))) {
                                isAdminPost = true;
                                Log.d(TAG, "Match by ID");
                            }
                            // For names that might have formatting differences
                            else if (postAdminName != null && postAdminName.trim().equalsIgnoreCase(adminName.trim())) {
                                isAdminPost = true;
                                Log.d(TAG, "Match by normalized name");
                            }

                            if (isAdminPost) {
                                // Check if we've already seen this post from another skill
                                // or if we've already processed this post ID
                                if (!processedPostIds.contains(postId)) {
                                    processedPostIds.add(postId);  // Mark as processed

                                    // Create a new post item
                                    AdminPostItem post = new AdminPostItem(
                                            postId,
                                            postAdminName,
                                            document.getString("position"),
                                            document.getString("postContent"),
                                            document.getTimestamp("timestamp")
                                    );
                                    post.getSkillsList().add(skill);
                                    postsMap.put(postId, post);
                                    Log.d(TAG, "Added new post: " + postId);
                                } else if (postsMap.containsKey(postId)) {
                                    // Just add this skill to the existing post
                                    AdminPostItem existingPost = postsMap.get(postId);
                                    existingPost.getSkillsList().add(skill);
                                    Log.d(TAG, "Added skill to existing post: " + postId);
                                }
                            }
                        }

                        // Decrement counter and check if we're done
                        if (pendingQueries.decrementAndGet() == 0) {
                            finishLoading(postsMap);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading skill posts for: " + skill, e);

                        // Decrement counter and check if we're done
                        if (pendingQueries.decrementAndGet() == 0) {
                            finishLoading(postsMap);
                        }
                    });
        }
    }

    private void finishLoading(Map<String, AdminPostItem> postsMap) {
        // Add all posts to our ArrayList
        posts.addAll(postsMap.values());
        Log.d(TAG, "Finished loading, found " + posts.size() + " posts total");

        // Sort by timestamp (newest first)
        posts.sort((p1, p2) -> {
            if (p1.getTimestamp() == null && p2.getTimestamp() == null) return 0;
            if (p1.getTimestamp() == null) return 1;
            if (p2.getTimestamp() == null) return -1;
            return p2.getTimestamp().compareTo(p1.getTimestamp());
        });

        // Set up listeners for each post
        for (AdminPostItem post : posts) {
            setupLikesListener(post);
            setupCommentsListener(post);
        }

        // Update UI
        if (posts.isEmpty()) {
            showNoPostsMessage();
        } else {
            if (isAdded()) {  // Check if fragment is still attached to activity
                recyclerView.setVisibility(View.VISIBLE);
                noPostsText.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void setupLikesListener(AdminPostItem post) {
        ListenerRegistration likesListener = db.collection("CommunityLikes")
                .whereEqualTo("postId", post.getPostId())
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Listen failed for likes", error);
                        return;
                    }

                    if (value != null && isAdded()) {
                        post.setLikesCount(value.size());
                        adapter.notifyDataSetChanged();
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

                    if (value != null && isAdded()) {
                        post.setCommentsCount(value.size());
                        adapter.notifyDataSetChanged();
                    }
                });

        commentListeners.put(post.getPostId(), commentsListener);
    }

    private void showNoPostsMessage() {
        if (isAdded()) {  // Check if fragment is still attached to activity
            recyclerView.setVisibility(View.GONE);
            noPostsText.setVisibility(View.VISIBLE);
            noPostsText.setText("No posts in skill categories");
        }
    }
}