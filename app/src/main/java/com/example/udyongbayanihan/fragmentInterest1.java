package com.example.udyongbayanihan;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.util.Set;

public class fragmentInterest1 extends Fragment {

    private InterestsHelper interestsHelper;
    private Button interestBlogging, interestGivingSpeeches, interestActing, interestHostingParties, interestSocialMedia, interestDebating, interestSinging, interestNetworking, interestImprovisation, interestStorytelling, btnInterestContinue;
    private String uotherDetails;
    private RegistrationData registrationData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_interest1, container, false);

        // Initialize the buttons
        interestBlogging = view.findViewById(R.id.interestBlogging);
        interestGivingSpeeches = view.findViewById(R.id.interestGivingSpeeches);
        interestActing = view.findViewById(R.id.interestActing);
        interestHostingParties = view.findViewById(R.id.interestHostingParties);
        interestSocialMedia = view.findViewById(R.id.interestSocialMedia);
        interestDebating = view.findViewById(R.id.interestDebating);
        interestSinging = view.findViewById(R.id.interestSinging);
        interestNetworking = view.findViewById(R.id.interestNetworking);
        interestImprovisation = view.findViewById(R.id.interestImprovisation);
        interestStorytelling = view.findViewById(R.id.interestStorytelling);
        btnInterestContinue = view.findViewById(R.id.btnInterestContinue);

        // Initialize InterestsHelper and userId
        FragmentActivity activity = requireActivity();
        if (activity instanceof Interests) {
            interestsHelper = ((Interests) activity).getInterestsHelper();
            uotherDetails = ((Interests) activity).getUsersOtherDetails();
            registrationData = ((Interests) activity).getRegistrationData(); // Get registration data
        }

        // Set initial state for buttons
        updateButtonState(interestBlogging, "Blogging");
        updateButtonState(interestGivingSpeeches, "Giving Speeches");
        updateButtonState(interestActing, "Acting");
        updateButtonState(interestHostingParties, "Hosting Parties");
        updateButtonState(interestSocialMedia, "Social Media");
        updateButtonState(interestDebating, "Debating");
        updateButtonState(interestSinging, "Singing");
        updateButtonState(interestNetworking, "Networking");
        updateButtonState(interestImprovisation, "Improvisation");
        updateButtonState(interestStorytelling, "Storytelling");

        // Set the click listener to toggle the button's state and color
        interestBlogging.setOnClickListener(v -> toggleInterest(interestBlogging, "Blogging"));
        interestGivingSpeeches.setOnClickListener(v -> toggleInterest(interestGivingSpeeches, "Giving Speeches"));
        interestActing.setOnClickListener(v -> toggleInterest(interestActing, "Acting"));
        interestHostingParties.setOnClickListener(v -> toggleInterest(interestHostingParties, "Hosting Parties"));
        interestSocialMedia.setOnClickListener(v -> toggleInterest(interestSocialMedia, "Social Media"));
        interestDebating.setOnClickListener(v -> toggleInterest(interestDebating, "Debating"));
        interestSinging.setOnClickListener(v -> toggleInterest(interestSinging, "Singing"));
        interestNetworking.setOnClickListener(v -> toggleInterest(interestNetworking, "Networking"));
        interestImprovisation.setOnClickListener(v -> toggleInterest(interestImprovisation, "Improvisation"));
        interestStorytelling.setOnClickListener(v -> toggleInterest(interestStorytelling, "Storytelling"));

        // Set up continue button listener
        btnInterestContinue.setOnClickListener(v -> {
            if (interestsHelper != null) {
                if (registrationData != null) {
                    // Get skills from the Skills activity through parent activity
                    Interests interestsActivity = (Interests) activity;
                    Set<String> skills = interestsActivity.getSkillsFromPreviousScreen();

                    // Log the skills being passed
                    Log.d("fragmentInterest1", "Skills being passed to saveAllUserData: " + skills);

                    // Save ALL user data to Firestore now (this is the final step)
                    interestsHelper.saveAllUserData(
                            registrationData,
                            skills,
                            requireContext()
                    );
                } else {
                    Toast.makeText(requireContext(), "Error: Missing registration data", Toast.LENGTH_SHORT).show();
                }

                Intent intent = new Intent(activity, Login.class);
                startActivity(intent);
                activity.finish();
            }
        });

        return view;
    }

    // Method to toggle the button state and color
    private void toggleInterest(Button button, String interest) {
        if (interestsHelper != null) {
            interestsHelper.toggleInterest(interest);
            updateButtonState(button, interest);
        }
    }

    // Method to update the button state and color
    private void updateButtonState(Button button, String interest) {
        if (interestsHelper != null) {
            if (interestsHelper.hasInterest(interest)) {
                button.setBackgroundColor(getResources().getColor(R.color.dark_green));
                button.setTextColor(getResources().getColor(R.color.white));
            } else {
                button.setBackgroundColor(getResources().getColor(R.color.white));
                button.setTextColor(getResources().getColor(R.color.black));
            }
        }
    }
}