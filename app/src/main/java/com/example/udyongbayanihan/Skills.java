package com.example.udyongbayanihan;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;

public class Skills extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager viewPager;
    private SkillsHelper skillsHelper;
    private String uotherDetails;
    private RegistrationData registrationData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_skills);

        // Initialize SkillsHelper
        skillsHelper = new SkillsHelper();

        // Get registration data from intent
        if (getIntent().hasExtra("registrationData")) {
            registrationData = (RegistrationData) getIntent().getSerializableExtra("registrationData");
            if (registrationData != null) {
                uotherDetails = registrationData.getUotherDetailsId();
                Log.d("Skills", "Received registration data and uotherDetails: " + uotherDetails);
            } else {
                // Fallback to get only usersOtherDetails
                uotherDetails = getIntent().getStringExtra("usersOtherDetails");
                Log.d("Skills", "Registration data null, falling back to: " + uotherDetails);
            }
        } else {
            // Fallback to get only usersOtherDetails
            uotherDetails = getIntent().getStringExtra("usersOtherDetails");
            Log.d("Skills", "No registration data, using only: " + uotherDetails);
        }

        // Get user details with better error recovery
        if (uotherDetails == null || uotherDetails.isEmpty()) {
            // Try to recover from other sources if possible
            Toast.makeText(this, "Warning: User details missing, some features may not work correctly", Toast.LENGTH_LONG).show();
            Log.e("Skills", "usersOtherDetails is null or empty");
        } else {
            Log.d("Skills", "Successfully retrieving uotherDetails: " + uotherDetails);
        }

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        if (tabLayout == null || viewPager == null) {
            Log.e("Skills", "TabLayout or ViewPager is null. Check XML IDs.");
            return;
        }

        tabLayout.setupWithViewPager(viewPager);

        SkillsAdapter vpAdapter = new SkillsAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        vpAdapter.addFragment(new fragmentSkill1(), "COMMUNICATION");
        vpAdapter.addFragment(new fragmentSkill2(), "LEADERSHIP");
        vpAdapter.addFragment(new fragmentSkill3(), "PROBLEM SOLVING");
        vpAdapter.addFragment(new fragmentSkill4(), "PERSONAL");
        vpAdapter.addFragment(new fragmentSkill5(), "CREATIVITY");
        vpAdapter.addFragment(new fragmentSkill6(), "TEAMWORK");

        viewPager.setAdapter(vpAdapter);
        Log.d("Skills", "ViewPager adapter set successfully.");
    }

    // Expose SkillsHelper to fragments
    public SkillsHelper getSkillsHelper() {
        return skillsHelper;
    }

    public String getUsersOtherDetails() {
        return uotherDetails;
    }

    public RegistrationData getRegistrationData() {
        return registrationData;
    }
}