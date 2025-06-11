package com.example.udyongbayanihan;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EditInterests extends AppCompatActivity {

    private FirebaseFirestore db;
    private HashMap<String, Boolean> interestStates = new HashMap<>();
    private String userId, uotherDetails;
    private String[] allInterests = {
            "Blogging", "Giving Speeches", "Acting", "Hosting Parties", "Social Media",
            "Debating", "Singing", "Networking", "Improvisation", "Storytelling",
            "Elder/Child Care", "Counseling Others", "Volunteering", "Community Gardening", "School/Professional Club",
            "Mediation", "Group Exercise", "Peace Corps", "Role Playing", "Podcasts",
            "Reading", "Writing", "Watching Documentaries", "Collecting Data", "Interviewing People",
            "Making Infographics", "Doing Puzzles", "Taking Online Classes", "Photography", "Drawing",
            "Music Composition", "Poetry", "Interior Design", "Scrapbooking", "Crafting",
            "Pottery", "Digital Art"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_interests);

        db = FirebaseFirestore.getInstance();

        // Get the userId passed from ViewProfile
        userId = getIntent().getStringExtra("userId");
        uotherDetails = getIntent().getStringExtra("uotherDetails");

        if (userId == null) {
            Toast.makeText(this, "User ID is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        GridLayout gridLayout = findViewById(R.id.gridLayoutInterests);

        // Fetch and display user's interests
        fetchUserInterests(gridLayout);

        // Save button functionality
        Button saveButton = findViewById(R.id.btnSaveInterests);
        saveButton.setOnClickListener(v -> saveInterests());
    }

    private void fetchUserInterests(GridLayout gridLayout) {
        gridLayout.removeAllViews(); // Prevent duplication

        db.collection("usersOtherDetails").document(uotherDetails).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<String> userInterestsFromDB = (List<String>) documentSnapshot.get("interests");

                        for (String interest : allInterests) {
                            Button interestButton = new Button(this);

                            // Set button text
                            interestButton.setText(interest);
                            interestButton.setAllCaps(false); // Make text sentence case like in the profile

                            // Set button appearance
                            boolean isSelected = userInterestsFromDB != null && userInterestsFromDB.contains(interest);
                            interestStates.put(interest, isSelected);

                            // Configure GridLayout parameters
                            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                            params.width = 0;
                            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
                            params.setMargins(12, 8, 12, 8);

                            // Set button padding for better touch area
                            interestButton.setPadding(24, 16, 24, 16);

                            // Apply appropriate style
                            updateButtonAppearance(interestButton, isSelected);

                            // Set click listener
                            interestButton.setOnClickListener(v -> toggleInterestState(interestButton, interest));

                            // Add button to grid
                            interestButton.setLayoutParams(params);
                            gridLayout.addView(interestButton);
                        }
                    } else {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch user interests: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void toggleInterestState(Button button, String interest) {
        boolean isSelected = interestStates.get(interest);
        boolean newState = !isSelected;
        interestStates.put(interest, newState);
        updateButtonAppearance(button, newState);
    }

    private void updateButtonAppearance(Button button, boolean isSelected) {
        if (isSelected) {
            // Match the exact style from the profile screenshot
            button.setBackgroundResource(R.drawable.interest_selected_button);
            button.setTextColor(Color.WHITE);
        } else {
            button.setBackgroundResource(R.drawable.interest_unselected_button);
            button.setTextColor(ContextCompat.getColor(this, R.color.dark_green));
        }
    }

    private void saveInterests() {
        List<String> selectedInterests = new ArrayList<>();
        for (String interest : interestStates.keySet()) {
            if (interestStates.get(interest)) {
                selectedInterests.add(interest);
            }
        }

        db.collection("usersOtherDetails").document(uotherDetails)
                .update("interests", selectedInterests)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Interests updated successfully!", Toast.LENGTH_SHORT).show();

                    // Redirect back to ViewProfile
                    Intent intent = new Intent(EditInterests.this, ViewProfile.class);
                    intent.putExtra("userId", userId); // Pass the userId back
                    intent.putExtra("uotherDetails", uotherDetails);
                    startActivity(intent);
                    finish(); // End the EditInterests activity
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save interests: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}