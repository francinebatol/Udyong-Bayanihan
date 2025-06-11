package com.example.udyongbayanihan;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.BuildConfig;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminUserPendingRequest extends AppCompatActivity {
    private static final String TAG = "AdminPendingRequest";
    private String amAccountId;
    private Bundle amDetails;
    private List<String> amSkills = new ArrayList<>();
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private PendingRequestsTabAdapter tabAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_user_pending_request);

        // Get admin details from intent
        amAccountId = getIntent().getStringExtra("amAccountId");
        amDetails = getIntent().getBundleExtra("amDetails");

        if (amAccountId == null) {
            Toast.makeText(this, "Error: Admin ID not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Admin ID: " + amAccountId);

        // Initialize views
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        // Fetch admin skills from Firestore - this calls the method defined below
        fetchAdminSkills();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Fetch admin skills from Firestore
    private void fetchAdminSkills() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Show a progress dialog
        View rootView = findViewById(R.id.main);
        ProgressBar progressBar = new ProgressBar(this);
        progressBar.setId(View.generateViewId());

        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
                );

        progressBar.setLayoutParams(params);
        ((androidx.constraintlayout.widget.ConstraintLayout) rootView).addView(progressBar);

        params.topToTop = rootView.getId();
        params.bottomToBottom = rootView.getId();
        params.startToStart = rootView.getId();
        params.endToEnd = rootView.getId();
        progressBar.setLayoutParams(params);

        Log.d(TAG, "Fetching admin skills for account ID: " + amAccountId);

        // First run the debugger to check all admin-related collections
        AdminFirestoreDebugger.debugAdminAccount(this, amAccountId);

        // Query AMOtherDetails collection for the admin's skills
        db.collection("AMOtherDetails")
                .whereEqualTo("amAccountid", amAccountId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Remove progress dialog
                    ((androidx.constraintlayout.widget.ConstraintLayout) rootView).removeView(progressBar);

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.e(TAG, "No document found for admin ID: " + amAccountId);

                        // Try with different case for the field name, since Firebase is case-sensitive
                        db.collection("AMOtherDetails")
                                .whereEqualTo("amAccountId", amAccountId)  // Try capitalized "Id"
                                .get()
                                .addOnSuccessListener(altQuerySnapshot -> {
                                    if (!altQuerySnapshot.isEmpty()) {
                                        Log.d(TAG, "Found document with capitalized 'amAccountId'");
                                        processSkillsDocument(altQuerySnapshot.getDocuments().get(0));
                                    } else {
                                        // If still not found, check if we have any hard-coded skills for testing
                                        Log.e(TAG, "No document found with either amAccountid or amAccountId");
                                        Toast.makeText(this, "Admin details not found in database", Toast.LENGTH_SHORT).show();

                                        // For testing purposes, add some default skills
                                        if (BuildConfig.DEBUG) {
                                            amSkills.add("Leadership");
                                            amSkills.add("Research");
                                            amSkills.add("Communication");
                                            Log.d(TAG, "Added default debug skills: " + amSkills);
                                        } else {
                                            amSkills.add("No Skills");
                                        }

                                        setupViewPager();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error in second attempt to fetch admin skills", e);
                                    amSkills.add("No Skills");
                                    setupViewPager();
                                });
                    } else {
                        // Process the document found
                        processSkillsDocument(queryDocumentSnapshots.getDocuments().get(0));
                    }
                })
                .addOnFailureListener(e -> {
                    // Remove progress dialog
                    ((androidx.constraintlayout.widget.ConstraintLayout) rootView).removeView(progressBar);

                    Log.e(TAG, "Error fetching admin skills", e);
                    Toast.makeText(this, "Error fetching skills: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    amSkills.add("No Skills");
                    setupViewPager();
                });
    }

    // Setup ViewPager after fetching skills
    private void setupViewPager() {
        // Setup ViewPager with TabLayout
        tabAdapter = new PendingRequestsTabAdapter(this, amSkills, amAccountId);
        viewPager.setAdapter(tabAdapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(amSkills.get(position));
        }).attach();
    }

    private void processSkillsDocument(DocumentSnapshot document) {
        // Log all fields in the document for debugging
        Log.d(TAG, "Document fields:");
        for (String field : document.getData().keySet()) {
            Log.d(TAG, field + " = " + document.get(field));
        }

        // Get the skills array field
        Object skillsObj = document.get("amSkills");

        if (skillsObj instanceof List) {
            // Cast to List and get skills
            List<String> fetchedSkills = (List<String>) skillsObj;
            Log.d(TAG, "Fetched skills: " + fetchedSkills);

            if (fetchedSkills != null && !fetchedSkills.isEmpty()) {
                amSkills.addAll(fetchedSkills);

                // Limit to 3 skills as specified
                if (amSkills.size() > 3) {
                    amSkills = amSkills.subList(0, 3);
                }
            } else {
                Log.e(TAG, "Admin skills array is empty");
                amSkills.add("No Skills");
            }
        } else {
            // Try to find the skills field with a different name
            boolean foundSkills = false;
            for (String field : document.getData().keySet()) {
                if (field.toLowerCase().contains("skill")) {
                    Object value = document.get(field);
                    if (value instanceof List) {
                        List<String> foundSkillsList = (List<String>) value;
                        Log.d(TAG, "Found skills with field name: " + field + " = " + foundSkillsList);
                        amSkills.addAll(foundSkillsList);
                        foundSkills = true;
                        break;
                    }
                }
            }

            if (!foundSkills) {
                Log.e(TAG, "Admin skills not found in any field");
                amSkills.add("No Skills");
            }
        }

        Log.d(TAG, "Final admin skills list: " + amSkills);
        setupViewPager();
    }
}