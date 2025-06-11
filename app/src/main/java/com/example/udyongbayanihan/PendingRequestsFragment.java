package com.example.udyongbayanihan;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PendingRequestsFragment extends Fragment {
    private static final String TAG = "PendingRequestsFragment";
    private static final String ARG_SKILL = "skill";
    private static final String ARG_ADMIN_ID = "adminId";

    private String skill;
    private String adminId;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private PendingRequestsAdapter adapter;
    private List<PendingRequestModel> pendingRequests;
    private ProgressBar progressBar;
    private TextView emptyTextView;

    public static PendingRequestsFragment newInstance(String skill, String adminId) {
        PendingRequestsFragment fragment = new PendingRequestsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SKILL, skill);
        args.putString(ARG_ADMIN_ID, adminId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            skill = getArguments().getString(ARG_SKILL);
            adminId = getArguments().getString(ARG_ADMIN_ID);
        }
        db = FirebaseFirestore.getInstance();
        pendingRequests = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pending_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerPendingRequests);
        progressBar = view.findViewById(R.id.progressBar);
        emptyTextView = view.findViewById(R.id.emptyTextView);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PendingRequestsAdapter(getContext(), pendingRequests, this::refreshRequests);
        recyclerView.setAdapter(adapter);

        fetchPendingRequests();
    }

    private void fetchPendingRequests() {
        progressBar.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);
        pendingRequests.clear();

        // Skip fetching if this is a placeholder tab
        if (skill.equals("No Skills")) {
            progressBar.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.VISIBLE);
            emptyTextView.setText("No skills assigned to this admin account");
            return;
        }

        Log.d(TAG, "Fetching pending requests for skill: " + skill);

        // Check if the skill exists in CommunityGroupSkills first
        db.collection("SkillGroupRequests")
                .whereEqualTo("skillName", skill)
                .whereEqualTo("status", "PENDING")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);

                    if (queryDocumentSnapshots.isEmpty()) {
                        // Now check if the skill exists in CommunityGroupSkills
                        db.collection("CommunityGroupSkills").document(skill).get()
                                .addOnSuccessListener(skillDoc -> {
                                    if (skillDoc.exists()) {
                                        emptyTextView.setVisibility(View.VISIBLE);
                                        emptyTextView.setText("No pending requests for " + skill);
                                    } else {
                                        emptyTextView.setVisibility(View.VISIBLE);
                                        emptyTextView.setText("Skill '" + skill + "' not found in database");
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    emptyTextView.setVisibility(View.VISIBLE);
                                    emptyTextView.setText("Error checking skill: " + e.getMessage());
                                });
                        return;
                    }

                    // Log the number of requests found for debugging
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " pending requests for skill: " + skill);

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String requestId = document.getId();
                        String userId = document.getString("userId");
                        String skillName = document.getString("skillName");

                        // Log each request ID and user ID for debugging
                        Log.d(TAG, "Processing request ID: " + requestId + " for user ID: " + userId);

                        // Fetch user details
                        fetchUserDetails(requestId, userId, skillName);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    emptyTextView.setVisibility(View.VISIBLE);
                    emptyTextView.setText("Error fetching requests: " + e.getMessage());
                    Log.e(TAG, "Error fetching pending requests", e);
                });
    }

    private void fetchPendingRequestsForSkill() {
        db.collection("SkillGroupRequests")
                .whereEqualTo("skillName", skill)
                .whereEqualTo("status", "PENDING")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);

                    if (queryDocumentSnapshots.isEmpty()) {
                        emptyTextView.setVisibility(View.VISIBLE);
                        emptyTextView.setText("No pending requests for " + skill);
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String requestId = document.getId();
                        String userId = document.getString("userId");
                        String skillName = document.getString("skillName");

                        // Fetch user details
                        fetchUserDetails(requestId, userId, skillName);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    emptyTextView.setVisibility(View.VISIBLE);
                    emptyTextView.setText("Error fetching requests: " + e.getMessage());
                    Log.e(TAG, "Error fetching pending requests", e);
                });
    }

    private void fetchUserDetails(String requestId, String userId, String skillName) {
        Log.d(TAG, "Fetching user details for userId: " + userId);

        // Get username and email from usersAccount collection
        // Note: In this collection, userId is the document ID rather than a field
        db.collection("usersAccount")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "User account document exists for userId: " + userId);

                        String username = documentSnapshot.getString("username");
                        String email = documentSnapshot.getString("email");

                        // Add null checks for username and email
                        if (username == null) {
                            username = "Unknown User";
                            Log.w(TAG, "Username is null for userId: " + userId);
                        }

                        if (email == null) {
                            email = "No email";
                            Log.w(TAG, "Email is null for userId: " + userId);
                        }

                        PendingRequestModel request = new PendingRequestModel(
                                requestId,
                                userId,
                                username,
                                email,
                                skillName
                        );

                        pendingRequests.add(request);
                        Log.d(TAG, "Added request to list: " + username + " for skill: " + skillName);

                        adapter.notifyDataSetChanged();

                        if (pendingRequests.size() > 0) {
                            emptyTextView.setVisibility(View.GONE);
                        }
                    } else {
                        Log.e(TAG, "User account document does not exist for userId: " + userId);

                        // Add the request even without user details
                        PendingRequestModel request = new PendingRequestModel(
                                requestId,
                                userId,
                                "Unknown User",
                                "No email available",
                                skillName
                        );

                        pendingRequests.add(request);
                        adapter.notifyDataSetChanged();

                        if (pendingRequests.size() > 0) {
                            emptyTextView.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user account details for userId: " + userId, e);

                    // Add the request even when there's an error
                    PendingRequestModel request = new PendingRequestModel(
                            requestId,
                            userId,
                            "Error Loading User",
                            "Error loading email",
                            skillName
                    );

                    pendingRequests.add(request);
                    adapter.notifyDataSetChanged();

                    if (pendingRequests.size() > 0) {
                        emptyTextView.setVisibility(View.GONE);
                    }
                });
    }

    public void refreshRequests() {
        fetchPendingRequests();
    }
}