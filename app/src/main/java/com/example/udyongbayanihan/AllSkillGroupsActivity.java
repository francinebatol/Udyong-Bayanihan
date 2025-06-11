package com.example.udyongbayanihan;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.CollectionReference;
import java.util.ArrayList;
import java.util.List;

public class AllSkillGroupsActivity extends AppCompatActivity {
    private static final String TAG = "AllSkillGroupsActivity";
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private AllSkillGroupsAdapter adapter;
    private List<SkillGroupModel> skillGroups;
    private String userId;
    private List<String> userSkills;
    private ProgressBar progressBar;
    private TextView emptyTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_skill_groups);

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("userId");

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(this, "Error: User ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "User ID: " + userId);
        initializeViews();
        fetchUserSkills();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerAllSkillGroups);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        skillGroups = new ArrayList<>();
        adapter = new AllSkillGroupsAdapter(this, skillGroups, userId);
        recyclerView.setAdapter(adapter);
    }

    private void fetchUserSkills() {
        progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Fetching user skills...");

        db.collection("usersOtherDetails")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        userSkills = (List<String>) querySnapshot.getDocuments().get(0).get("skills");
                        if (userSkills == null) {
                            userSkills = new ArrayList<>();
                        }
                        Log.d(TAG, "User skills found: " + userSkills.toString());
                        fetchSkillGroups();
                    } else {
                        Log.d(TAG, "No user details found");
                        progressBar.setVisibility(View.GONE);
                        userSkills = new ArrayList<>();
                        fetchSkillGroups();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user skills: " + e.getMessage());
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error fetching user skills: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchSkillGroups() {
        Log.d(TAG, "Starting to fetch skill groups...");
        progressBar.setVisibility(View.VISIBLE);

        // Get reference to the CommunityGroupSkills collection
        CollectionReference skillsRef = db.collection("CommunityGroupSkills");

        // Log the collection path to verify
        Log.d(TAG, "Accessing collection: " + skillsRef.getPath());

        skillsRef.get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Successfully connected to Firestore");
                    Log.d(TAG, "Raw documents count: " + querySnapshot.size());

                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "Query snapshot is empty");
                        progressBar.setVisibility(View.GONE);
                        emptyTextView.setVisibility(View.VISIBLE);
                        emptyTextView.setText("No skill groups available");
                        return;
                    }

                    skillGroups.clear();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String skillName = document.getId();
                        Log.d(TAG, "Processing skill group: " + skillName);

                        // Only add skills that the user doesn't already have
                        if (userSkills == null || !userSkills.contains(skillName)) {
                            Log.d(TAG, "Adding skill to list: " + skillName);
                            skillGroups.add(new SkillGroupModel(skillName, false));
                        } else {
                            Log.d(TAG, "Skipping skill (user already has it): " + skillName);
                        }
                    }

                    Log.d(TAG, "Total skill groups added: " + skillGroups.size());

                    if (skillGroups.isEmpty()) {
                        emptyTextView.setVisibility(View.VISIBLE);
                        emptyTextView.setText("You have joined all available skill groups");
                    } else {
                        emptyTextView.setVisibility(View.GONE);
                    }

                    adapter.notifyDataSetChanged();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching skill groups: " + e.getMessage());
                    Log.e(TAG, "Error details: ", e);
                    progressBar.setVisibility(View.GONE);
                    emptyTextView.setVisibility(View.VISIBLE);
                    emptyTextView.setText("Error fetching skill groups");
                    Toast.makeText(this, "Error fetching skill groups: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}