package com.example.udyongbayanihan;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.Set;

public class fragmentInterest4 extends Fragment {
    private InterestsHelper interestsHelper;
    private Button interestPhotography, interestDrawing, interestMusicComposition, interestPoetry,
            interestInteriorDesign, interestSewing, interestScarpbooking, interestCrafting,
            interestPottery, interestDigitalArt, interestStorytelling, btnInterestContinue;
    private String uotherDetails;
    private RegistrationData registrationData;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_interest4, container, false);

        // Initialize buttons
        interestPhotography = view.findViewById(R.id.interestPhotography);
        interestDrawing = view.findViewById(R.id.interestDrawing);
        interestMusicComposition = view.findViewById(R.id.interestMusicComposition);
        interestPoetry = view.findViewById(R.id.interestPoetry);
        interestInteriorDesign = view.findViewById(R.id.interestInteriorDesign);
        interestSewing = view.findViewById(R.id.interestSewing);
        interestScarpbooking = view.findViewById(R.id.interestScrapbooking);
        interestCrafting = view.findViewById(R.id.interestCrafting);
        interestPottery = view.findViewById(R.id.interestPottery);
        interestDigitalArt = view.findViewById(R.id.interestDigitalArt);
        interestStorytelling = view.findViewById(R.id.interestStorytelling);
        btnInterestContinue = view.findViewById(R.id.btnInterestContinue);

        // Initialize InterestsHelper and userId
        FragmentActivity activity = requireActivity();
        if (activity instanceof Interests) {
            interestsHelper = ((Interests) activity).getInterestsHelper();
            uotherDetails = ((Interests) activity).getUsersOtherDetails();
            registrationData = ((Interests) activity).getRegistrationData(); // Get registration data
        }

        // Update button states
        updateButtonState(interestPhotography, "Photography");
        updateButtonState(interestDrawing, "Drawing");
        updateButtonState(interestMusicComposition, "Music Composition");
        updateButtonState(interestPoetry, "Poetry");
        updateButtonState(interestInteriorDesign, "Interior Design");
        updateButtonState(interestSewing, "Sewing");
        updateButtonState(interestScarpbooking, "Scrapbooking");
        updateButtonState(interestCrafting, "Crafting");
        updateButtonState(interestPottery, "Pottery");
        updateButtonState(interestDigitalArt, "Digital Art");
        updateButtonState(interestStorytelling, "Storytelling");

        // Set click listeners
        interestPhotography.setOnClickListener(v -> toggleInterest(interestPhotography, "Photography"));
        interestDrawing.setOnClickListener(v -> toggleInterest(interestDrawing, "Drawing"));
        interestMusicComposition.setOnClickListener(v -> toggleInterest(interestMusicComposition, "Music Composition"));
        interestPoetry.setOnClickListener(v -> toggleInterest(interestPoetry, "Poetry"));
        interestInteriorDesign.setOnClickListener(v -> toggleInterest(interestInteriorDesign, "Interior Design"));
        interestSewing.setOnClickListener(v -> toggleInterest(interestSewing, "Sewing"));
        interestScarpbooking.setOnClickListener(v -> toggleInterest(interestScarpbooking, "Scrapbooking"));
        interestCrafting.setOnClickListener(v -> toggleInterest(interestCrafting, "Crafting"));
        interestPottery.setOnClickListener(v -> toggleInterest(interestPottery, "Pottery"));
        interestDigitalArt.setOnClickListener(v -> toggleInterest(interestDigitalArt, "Digital Art"));
        interestStorytelling.setOnClickListener(v -> toggleInterest(interestStorytelling, "Storytelling"));

        // Set up continue button listener
        btnInterestContinue.setOnClickListener(v -> {
            if (interestsHelper != null) {
                if (registrationData != null) {
                    // Get skills from the Skills activity through parent activity
                    Interests interestsActivity = (Interests) activity;
                    Set<String> skills = interestsActivity.getSkillsFromPreviousScreen();

                    // Log the skills being passed
                    Log.d("fragmentInterest4", "Skills being passed to saveAllUserData: " + skills);

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

    private void toggleInterest(Button button, String interest) {
        if (interestsHelper != null) {
            interestsHelper.toggleInterest(interest);
            updateButtonState(button, interest);
        }
    }

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