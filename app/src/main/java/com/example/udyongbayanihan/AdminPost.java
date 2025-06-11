package com.example.udyongbayanihan;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminPost extends AppCompatActivity {
    private static final String TAG = "AdminPost";

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FloatingActionButton createPostFab;
    private ImageButton btnBack;
    private AdminPostPagerAdapter pagerAdapter;

    private FirebaseFirestore db;
    private String amAccountId;
    private Bundle amDetails;
    private String adminName;
    private String adminBarangay;
    private static final int CREATE_POST_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_post);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get admin info from intent
        amAccountId = getIntent().getStringExtra("amAccountId");
        amDetails = getIntent().getBundleExtra("amDetails");

        if (amAccountId == null) {
            Toast.makeText(this, "Admin ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        createPostFab = findViewById(R.id.createPostFab);
        btnBack = findViewById(R.id.btnBack);

        // Set up back button
        btnBack.setOnClickListener(v -> finish());

        // Set up FAB click listener
        createPostFab.setOnClickListener(v -> {
            Intent intent = new Intent(AdminPost.this, AdminCreatePost.class);
            intent.putExtra("amAccountId", amAccountId);
            intent.putExtra("amDetails", amDetails);
            // Start for result instead of just starting the activity
            startActivityForResult(intent, CREATE_POST_REQUEST_CODE);
        });

        // Load admin details and then setup the tabs
        loadAdminDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh data when returning to this activity
        if (pagerAdapter != null) {
            refreshFragments();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_POST_REQUEST_CODE && resultCode == RESULT_OK) {
            // Post was created successfully, refresh all fragments
            refreshAllFragments();
        }
    }

    private void refreshAllFragments() {
        if (pagerAdapter != null) {
            // Force recreation of all fragments using our custom method
            pagerAdapter.refreshAllItems();

            Log.d(TAG, "Refreshing all fragments");
        }
    }

    private void refreshFragments() {
        // This will trigger onResume in the current fragment
        int currentItem = viewPager.getCurrentItem();

        if (pagerAdapter != null) {
            // Force recreation of the current fragment using our custom method
            pagerAdapter.refreshItem(currentItem);

            // Log that we're refreshing
            Log.d(TAG, "Refreshing fragment at position: " + currentItem);
        }
    }

    private void loadAdminDetails() {
        // Get admin name
        db.collection("AMNameDetails")
                .whereEqualTo("amAccountId", amAccountId)
                .get()
                .addOnSuccessListener(nameSnapshot -> {
                    if (nameSnapshot.isEmpty()) {
                        Log.e(TAG, "Admin name details not found");
                        Toast.makeText(this, "Error: Admin details not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    DocumentSnapshot nameDoc = nameSnapshot.getDocuments().get(0);
                    String amFirstName = nameDoc.getString("amFirstName");
                    String amLastName = nameDoc.getString("amLastName");
                    adminName = amFirstName + " " + amLastName;

                    // Get admin's barangay
                    db.collection("AMAddressDetails")
                            .whereEqualTo("amAccountId", amAccountId)
                            .get()
                            .addOnSuccessListener(addressSnapshot -> {
                                if (!addressSnapshot.isEmpty()) {
                                    DocumentSnapshot addressDoc = addressSnapshot.getDocuments().get(0);
                                    adminBarangay = addressDoc.getString("amBarangay");
                                }

                                // Setup ViewPager and TabLayout now that we have the admin details
                                setupViewPager();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error fetching admin address", e);
                                adminBarangay = null;
                                // Still setup ViewPager without barangay info
                                setupViewPager();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching admin name", e);
                    Toast.makeText(this, "Error: Could not load admin details", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setupViewPager() {
        // Create adapter for ViewPager
        pagerAdapter = new AdminPostPagerAdapter(
                this, amAccountId, adminName, adminBarangay);

        // Set adapter to ViewPager
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    if (position == 0) {
                        tab.setText("Barangay");
                    } else {
                        tab.setText("Skills");
                    }
                }).attach();
    }
}