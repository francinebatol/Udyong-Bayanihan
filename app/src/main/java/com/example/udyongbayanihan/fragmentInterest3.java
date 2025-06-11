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

public class fragmentInterest3 extends Fragment {

    private InterestsHelper interestsHelper;
    private Button interestPodcasts, interestReading, interestWriting, interestWatchingDocumentaries, interestCollectingData, interestInterviewingPeople, interestMakingInfographics, interestDoingPuzzles, interestTakingOnlineClasses, btnInterestContinue;
    private String uotherDetails;
    private RegistrationData registrationData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_interest3, container, false);

        // Initialize the buttons
        interestPodcasts = view.findViewById(R.id.interestPodcasts);
        interestReading = view.findViewById(R.id.interestReading);
        interestWriting = view.findViewById(R.id.interestWriting);
        interestWatchingDocumentaries = view.findViewById(R.id.interestWatchingDocumentaries);
        interestCollectingData = view.findViewById(R.id.interestCollectingData);
        interestInterviewingPeople = view.findViewById(R.id.interestInterviewingPeople);
        interestMakingInfographics = view.findViewById(R.id.interestMakingInfographics);
        interestDoingPuzzles = view.findViewById(R.id.interestDoingPuzzles);
        interestTakingOnlineClasses = view.findViewById(R.id.interestTakingOnlineClasses);
        btnInterestContinue = view.findViewById(R.id.btnInterestContinue);

        // Initialize InterestsHelper and userId
        FragmentActivity activity = requireActivity();
        if (activity instanceof Interests) {
            interestsHelper = ((Interests) activity).getInterestsHelper();
            uotherDetails = ((Interests) activity).getUsersOtherDetails(); // Get userId from Interests activity
            registrationData = ((Interests) activity).getRegistrationData(); // Get registration data
        }

        // Set initial state for buttons
        updateButtonState(interestPodcasts, "Podcasts");
        updateButtonState(interestReading, "Reading");
        updateButtonState(interestWriting, "Writing");
        updateButtonState(interestWatchingDocumentaries, "Watching Documentaries");
        updateButtonState(interestCollectingData, "Collecting Data");
        updateButtonState(interestInterviewingPeople, "Interviewing People");
        updateButtonState(interestMakingInfographics, "Making Infographics");
        updateButtonState(interestDoingPuzzles, "Doing Puzzles");
        updateButtonState(interestTakingOnlineClasses, "Taking Online Classes");

        // Set the click listener to toggle the button's state and color
        interestPodcasts.setOnClickListener(v -> toggleInterest(interestPodcasts, "Podcasts"));
        interestReading.setOnClickListener(v -> toggleInterest(interestReading, "Reading"));
        interestWriting.setOnClickListener(v -> toggleInterest(interestWriting, "Writing"));
        interestWatchingDocumentaries.setOnClickListener(v -> toggleInterest(interestWatchingDocumentaries, "Watching Documentaries"));
        interestCollectingData.setOnClickListener(v -> toggleInterest(interestCollectingData, "Collecting Data"));
        interestInterviewingPeople.setOnClickListener(v -> toggleInterest(interestInterviewingPeople, "Interviewing People"));
        interestMakingInfographics.setOnClickListener(v -> toggleInterest(interestMakingInfographics, "Making Infographics"));
        interestDoingPuzzles.setOnClickListener(v -> toggleInterest(interestDoingPuzzles, "Doing Puzzles"));
        interestTakingOnlineClasses.setOnClickListener(v -> toggleInterest(interestTakingOnlineClasses, "Taking Online Classes"));

        // Set up continue button listener
        btnInterestContinue.setOnClickListener(v -> {
            if (interestsHelper != null) {
                if (registrationData != null) {
                    // Get skills from the Skills activity through parent activity
                    Interests interestsActivity = (Interests) activity;
                    Set<String> skills = interestsActivity.getSkillsFromPreviousScreen();

                    // Log the skills being passed
                    Log.d("fragmentInterest3", "Skills being passed to saveAllUserData: " + skills);

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