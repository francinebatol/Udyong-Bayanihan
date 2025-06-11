package com.example.udyongbayanihan;

import android.os.Bundle;
import android.util.Log;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminCreatePost extends AppCompatActivity {
    private static final String TAG = "AdminCreatePost";

    private TextView adminFullName, adminPosition;
    private Spinner skillSpinner;
    private EditText postContent;
    private CheckBox postToBarangayCheckBox;
    private Button submitPostButton;
    private ImageButton imgbtnBack;

    private String adminBarangay = null;
    private List<String> adminSkills = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_create_post);

        adminFullName = findViewById(R.id.adminFullName);
        adminPosition = findViewById(R.id.adminPosition);
        skillSpinner = findViewById(R.id.skillSpinner);
        postContent = findViewById(R.id.postContent);
        postToBarangayCheckBox = findViewById(R.id.postToBarangayCheckBox);
        submitPostButton = findViewById(R.id.submitPostButton);
        imgbtnBack = findViewById(R.id.imgbtnBack);

        // Set back button listener
        imgbtnBack.setOnClickListener(v -> finish());

        // Retrieve amAccountId passed from AdminMainMenu
        String amAccountId = getIntent().getStringExtra("amAccountId");

        if (amAccountId != null) {
            fetchAdminDetails(amAccountId);
        } else {
            Toast.makeText(this, "Admin Account ID not provided!", Toast.LENGTH_SHORT).show();
            finish();
        }

        submitPostButton.setOnClickListener(v -> handlePostSubmission(amAccountId));
    }

    // Fetch admin details from Firestore
    private void fetchAdminDetails(String amAccountId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetch admin name
        db.collection("AMNameDetails")
                .whereEqualTo("amAccountId", amAccountId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot nameDetails = querySnapshot.getDocuments().get(0);
                        String amFirstName = nameDetails.getString("amFirstName");
                        String amLastName = nameDetails.getString("amLastName");
                        adminFullName.setText(amFirstName + " " + amLastName);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching admin name details", e);
                });

        // Fetch admin position and skills
        db.collection("AMOtherDetails")
                .whereEqualTo("amAccountId", amAccountId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot otherDetails = querySnapshot.getDocuments().get(0);
                        String position = otherDetails.getString("position");
                        adminPosition.setText(position);

                        // Fetch admin skills from amSkills array field
                        List<String> skills = (List<String>) otherDetails.get("amSkills");
                        if (skills != null && !skills.isEmpty()) {
                            adminSkills.clear();
                            adminSkills.addAll(skills);
                        } else {
                            // Fallback to default skills if admin has no skills in database
                            adminSkills.add("Dedication");
                            adminSkills.add("Leadership");
                            adminSkills.add("Teamwork");
                        }
                        // Populate the spinner with fetched skills
                        populateSkillSpinner();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching admin other details", e);
                });

        // Fetch admin barangay
        db.collection("AMAddressDetails")
                .whereEqualTo("amAccountId", amAccountId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot addressDetails = querySnapshot.getDocuments().get(0);
                        adminBarangay = addressDetails.getString("amBarangay");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching admin address details", e);
                });
    }

    // Populate skill spinner with admin's skills
    private void populateSkillSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, adminSkills);
        skillSpinner.setAdapter(adapter);
    }

    // Handle post submission
// Modify the handlePostSubmission method to set result when done
    private void handlePostSubmission(String amAccountId) {
        // Ensure we have skills to select from
        if (adminSkills.isEmpty() || skillSpinner.getSelectedItem() == null) {
            Toast.makeText(this, "No skill selected!", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedSkill = skillSpinner.getSelectedItem().toString();
        String postText = postContent.getText().toString();

        if (postText.isEmpty()) {
            Toast.makeText(this, "Post content cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Disable submit button to prevent double-posting
        submitPostButton.setEnabled(false);

        // Prepare common post data
        Map<String, Object> postData = new HashMap<>();
        postData.put("adminName", adminFullName.getText().toString());
        postData.put("position", adminPosition.getText().toString());
        postData.put("postContent", postText);
        postData.put("timestamp", FieldValue.serverTimestamp());
        // Add the admin ID - this is critical for consistent filtering
        postData.put("adminId", amAccountId);
        // Add selected skill for easier filtering
        postData.put("skill", selectedSkill);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Counter for async operations
        final int[] pendingOperations = {0};
        final boolean[] hasError = {false};

        // Update the lastUpdated timestamp in the main CommunityGroupSkills document
        pendingOperations[0]++;
        db.collection("CommunityGroupSkills")
                .document(selectedSkill)
                .update("lastUpdated", FieldValue.serverTimestamp())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "CommunityGroupSkills main document timestamp updated");
                    checkCompletion(pendingOperations, hasError);
                })
                .addOnFailureListener(e -> {
                    // If updating fails, the document might not exist yet, so create it
                    Map<String, Object> skillDocData = new HashMap<>();
                    skillDocData.put("lastUpdated", FieldValue.serverTimestamp());

                    db.collection("CommunityGroupSkills")
                            .document(selectedSkill)
                            .set(skillDocData)
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d(TAG, "Created CommunityGroupSkills document with timestamp");
                                checkCompletion(pendingOperations, hasError);
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "Error creating CommunityGroupSkills document", e2);
                                hasError[0] = true;
                                checkCompletion(pendingOperations, hasError);
                            });
                });

        // Post to CommunityGroupSkills subcollection
        pendingOperations[0]++;
        db.collection("CommunityGroupSkills")
                .document(selectedSkill)
                .collection("Posts")
                .add(postData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                    Toast.makeText(this, "Posted in Skills group", Toast.LENGTH_SHORT).show();

                    // Also update the skill document to ensure it exists for future queries
                    Map<String, Object> skillData = new HashMap<>();
                    skillData.put("name", selectedSkill);
                    skillData.put("lastUpdated", FieldValue.serverTimestamp());

                    db.collection("SkillCategories")
                            .document(selectedSkill)
                            .set(skillData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Skill category updated");
                                checkCompletion(pendingOperations, hasError);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error updating skill category", e);
                                hasError[0] = true;
                                checkCompletion(pendingOperations, hasError);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding document", e);
                    Toast.makeText(this, "Failed to post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    hasError[0] = true;
                    checkCompletion(pendingOperations, hasError);
                });

        // Post to CommunityGroups if checkbox is checked
        if (postToBarangayCheckBox.isChecked() && adminBarangay != null) {
            pendingOperations[0]++;
            db.collection("CommunityGroups")
                    .document(adminBarangay)
                    .collection("Posts") // Use a sub-collection for posts under the barangay
                    .add(postData)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Barangay post written with ID: " + documentReference.getId());
                        Toast.makeText(this, "Posted in Barangay group", Toast.LENGTH_SHORT).show();
                        checkCompletion(pendingOperations, hasError);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding barangay post", e);
                        Toast.makeText(this, "Failed to post to barangay: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        hasError[0] = true;
                        checkCompletion(pendingOperations, hasError);
                    });
        }

        // Clear the post content field so user can create another post
        postContent.setText("");
    }

    private void checkCompletion(final int[] pendingOperations, final boolean[] hasError) {
        pendingOperations[0]--;

        if (pendingOperations[0] <= 0) {
            // Re-enable submit button
            submitPostButton.setEnabled(true);

            if (!hasError[0]) {
                // All operations completed successfully
                Toast.makeText(this, "Post created successfully!", Toast.LENGTH_SHORT).show();

                // Set result to indicate success and finish
                setResult(RESULT_OK);
                finish();
            } else {
                // Show error if any operation failed
                Toast.makeText(this, "Some operations failed. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}