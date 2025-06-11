package com.example.udyongbayanihan;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EditSkills extends AppCompatActivity {

    private FirebaseFirestore db;
    private HashMap<String, Boolean> skillStates = new HashMap<>();
    private String userId, uotherDetails;
    private String[] allSkills = {
            "Dedication", "Patience", "Reliability", "Participation", "Decision Making",
            "Coaching", "Mentoring", "Planning", "Training", "Critical Thinking",
            "Data Gathering", "Determination", "Research", "Flexibility", "Adaptability",
            "Conflict Resolution", "Resilience", "Emotional Intelligence", "Empathy", "Design",
            "Innovation", "Exploration and Discovery", "Visual Thinking", "Mindfulness",
            "Process Analysis", "Scenario Planning", "Troubleshooting"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_skills);

        db = FirebaseFirestore.getInstance();

        // Get the userId passed from ViewProfile
        userId = getIntent().getStringExtra("userId");
        uotherDetails = getIntent().getStringExtra("uotherDetails");

        if (userId == null) {
            Toast.makeText(this, "User ID is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        GridLayout gridLayout = findViewById(R.id.gridLayout);

        // Fetch and display user's skills
        fetchUserSkills(gridLayout);

        // Save button functionality
        Button saveButton = findViewById(R.id.btnSave);
        saveButton.setOnClickListener(v -> saveSkills());
    }

    private void fetchUserSkills(GridLayout gridLayout) {
        gridLayout.removeAllViews(); // Prevent duplication

        db.collection("usersOtherDetails").document(uotherDetails).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> userSkillsFromDB = (List<String>) documentSnapshot.get("skills");

                        for (String skill : allSkills) {
                            Button skillButton = new Button(this);
                            skillButton.setText(skill);

                            if (userSkillsFromDB != null && userSkillsFromDB.contains(skill)) {
                                skillButton.setBackgroundColor(Color.parseColor("#006400")); // dark green
                                skillButton.setTextColor(Color.WHITE);
                                skillStates.put(skill, true);
                            } else {
                                skillButton.setBackgroundResource(R.drawable.rounded_button);
                                skillButton.setTextColor(Color.BLACK);
                                skillStates.put(skill, false);
                            }

                            skillButton.setOnClickListener(v -> toggleSkillState(skillButton, skill));
                            gridLayout.addView(skillButton);
                        }
                    } else {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch user skills: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void toggleSkillState(Button button, String skill) {
        boolean isSelected = skillStates.get(skill);
        skillStates.put(skill, !isSelected);

        if (!isSelected) {
            button.setBackgroundColor(Color.parseColor("#006400")); // dark green
            button.setTextColor(Color.WHITE);
        } else {
            button.setBackgroundResource(R.drawable.rounded_button);
            button.setTextColor(Color.BLACK);
        }
    }

    private void saveSkills() {
        List<String> selectedSkills = new ArrayList<>();
        for (String skill : skillStates.keySet()) {
            if (skillStates.get(skill)) {
                selectedSkills.add(skill);
            }
        }

        if (selectedSkills.isEmpty()) {
            Toast.makeText(this, "Please select at least one skill.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use uotherDetails as document ID instead of userId
        db.collection("usersOtherDetails").document(uotherDetails)
                .update("skills", selectedSkills)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Skills updated successfully!", Toast.LENGTH_SHORT).show();

                    // Redirect back to ViewProfile
                    Intent intent = new Intent(EditSkills.this, ViewProfile.class);
                    intent.putExtra("userId", userId);
                    intent.putExtra("uotherDetails", uotherDetails);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save skills: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
