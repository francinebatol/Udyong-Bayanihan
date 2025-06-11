package com.example.udyongbayanihan;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import com.google.android.material.tabs.TabLayout;

import java.util.HashSet;
import java.util.Set;

public class Interests extends AppCompatActivity {

    private TabLayout tabLayoutInterests;
    private ViewPager viewPagerInterests;
    private InterestsHelper interestsHelper;
    private String uotherDetails;
    private RegistrationData registrationData;
    private Set<String> skillsFromPreviousScreen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_interests);

        // Initialize InterestsHelper
        interestsHelper = new InterestsHelper();
        skillsFromPreviousScreen = new HashSet<>(); // Initialize skills set

        // Retrieve registration data from intent
        if (getIntent().hasExtra("registrationData")) {
            registrationData = (RegistrationData) getIntent().getSerializableExtra("registrationData");
            if (registrationData != null) {
                uotherDetails = registrationData.getUotherDetailsId();
                Log.d("Interests", "Received registration data and uotherDetails: " + uotherDetails);
            } else {
                // Fallback to get only usersOtherDetails
                uotherDetails = getIntent().getStringExtra("usersOtherDetails");
                Log.d("Interests", "Registration data null, falling back to: " + uotherDetails);
            }
        } else {
            // Fallback to get only usersOtherDetails
            uotherDetails = getIntent().getStringExtra("usersOtherDetails");
            Log.d("Interests", "No registration data, using only: " + uotherDetails);
        }

        // Retrieve skills from intent
        if (getIntent().hasExtra("selectedSkills")) {
            String[] skillsArray = getIntent().getStringArrayExtra("selectedSkills");
            if (skillsArray != null) {
                for (String skill : skillsArray) {
                    skillsFromPreviousScreen.add(skill);
                }
                Log.d("Interests", "Received skills: " + skillsFromPreviousScreen);
            } else {
                Log.e("Interests", "selectedSkills array is null");
            }
        } else {
            Log.e("Interests", "No selectedSkills extra in intent");
        }

        // Check if we have the necessary user details
        if (uotherDetails == null || uotherDetails.isEmpty()) {
            // Show error dialog instead of crashing
            new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("User details are missing. Please go back and try again.")
                    .setPositiveButton("Go Back", (dialog, which) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }
        Log.d("Interests", "uotherDetails: " + uotherDetails);

        // Set up views and adapter
        tabLayoutInterests = findViewById(R.id.tabLayoutInterests);
        viewPagerInterests = findViewById(R.id.viewPagerInterests);
        tabLayoutInterests.setupWithViewPager(viewPagerInterests);

        InterestsAdapter vpAdapter = new InterestsAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        vpAdapter.addFragment(new fragmentInterest1(), "COMMUNICATION");
        vpAdapter.addFragment(new fragmentInterest2(), "INTERPERSONAL");
        vpAdapter.addFragment(new fragmentInterest3(), "RESEARCH");
        vpAdapter.addFragment(new fragmentInterest4(), "CREATIVITY");

        viewPagerInterests.setAdapter(vpAdapter);
    }

    // Expose InterestsHelper to fragments
    public InterestsHelper getInterestsHelper() {
        return interestsHelper;
    }

    public String getUsersOtherDetails() {
        return uotherDetails;
    }

    public RegistrationData getRegistrationData() {
        return registrationData;
    }

    public Set<String> getSkillsFromPreviousScreen() {
        return skillsFromPreviousScreen;
    }
}