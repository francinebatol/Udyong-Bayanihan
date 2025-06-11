package com.example.udyongbayanihan;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateGroup extends AppCompatActivity {

    private FirebaseFirestore db;
    private String orgId;
    private EditText inputGroupName, inputDescription;
    private Button createCommunityGroup;
    private String organizerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_group);

        db = FirebaseFirestore.getInstance();
        orgId = getIntent().getStringExtra("orgId");

        inputGroupName = findViewById(R.id.inputGroupName);
        inputDescription = findViewById(R.id.inputDescription);
        createCommunityGroup = findViewById(R.id.createCommunityGroup);

        // Load organizer data
        loadOrganizerData(orgId);

        createCommunityGroup.setOnClickListener(v -> saveCommunityGroup());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void saveCommunityGroup() {
        String GroupName = inputGroupName.getText().toString().trim();
        String Description = inputDescription.getText().toString().trim();

        if (GroupName.isEmpty() || Description.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("GroupName", GroupName);
        eventData.put("Description", Description);
        eventData.put("organizerName", organizerName); // Add the organizer's name
        eventData.put("organizerId", orgId); // Reference to the logged-in organizer

        // Save to Firestore
        db.collection("CommunityGroup").add(eventData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(CreateGroup.this, "Group created successfully.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CreateGroup.this, "Error creating group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadOrganizerData(String orgId) {
        db.collection("Organizer").document(orgId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        organizerName = documentSnapshot.getString("Name");
                        createCommunityGroup.setEnabled(true); // Enable button after data is loaded
                    } else {
                        Toast.makeText(this, "Organizer data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error retrieving organizer data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}