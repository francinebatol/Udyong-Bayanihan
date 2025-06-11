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

public class fragmentInterest2 extends Fragment {

    private InterestsHelper interestsHelper;
    private Button interestElderChildCare, interestCounselingOthers, interestVolunteering, interestCommunityGardening, interestSchoolProfessionalClub, interestMediation, interestGroupExercise, interestPeaceCorps, interestRolePlaying, btnInterestContinue;
    private String uotherDetails;
    private RegistrationData registrationData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_interest2, container, false);

        // Initialize the buttons
        interestElderChildCare = view.findViewById(R.id.interestElderChildCare);
        interestCounselingOthers = view.findViewById(R.id.interestCounselingOthers);
        interestVolunteering = view.findViewById(R.id.interestVolunteering);
        interestCommunityGardening = view.findViewById(R.id.interestCommunityGardening);
        interestSchoolProfessionalClub = view.findViewById(R.id.interestSchoolProfessionalClub);
        interestMediation = view.findViewById(R.id.interestMediation);
        interestGroupExercise = view.findViewById(R.id.interestGroupExercise);
        interestPeaceCorps = view.findViewById(R.id.interestPeaceCorps);
        interestRolePlaying = view.findViewById(R.id.interestRolePlaying);
        btnInterestContinue = view.findViewById(R.id.btnInterestContinue);

        // Initialize InterestsHelper and userId
        FragmentActivity activity = requireActivity();
        if (activity instanceof Interests) {
            interestsHelper = ((Interests) activity).getInterestsHelper();
            uotherDetails = ((Interests) activity).getUsersOtherDetails(); // Get userId from Interests activity
            registrationData = ((Interests) activity).getRegistrationData(); // Get registration data
        }

        // Set initial state for buttons
        updateButtonState(interestElderChildCare, "Elder/Child Care");
        updateButtonState(interestCounselingOthers, "Counseling Others");
        updateButtonState(interestVolunteering, "Volunteering");
        updateButtonState(interestCommunityGardening, "Community Gardening");
        updateButtonState(interestSchoolProfessionalClub, "School/Professional Club");
        updateButtonState(interestMediation, "Mediation");
        updateButtonState(interestGroupExercise, "Group Exercise");
        updateButtonState(interestPeaceCorps, "Peace Corps");
        updateButtonState(interestRolePlaying, "Role Playing");

        // Set the click listener to toggle the button's state and color
        interestElderChildCare.setOnClickListener(v -> toggleInterest(interestElderChildCare, "Elder/Child Care"));
        interestCounselingOthers.setOnClickListener(v -> toggleInterest(interestCounselingOthers, "Counseling Others"));
        interestVolunteering.setOnClickListener(v -> toggleInterest(interestVolunteering, "Volunteering"));
        interestCommunityGardening.setOnClickListener(v -> toggleInterest(interestCommunityGardening, "Community Gardening"));
        interestSchoolProfessionalClub.setOnClickListener(v -> toggleInterest(interestSchoolProfessionalClub, "School/Professional Club"));
        interestMediation.setOnClickListener(v -> toggleInterest(interestMediation, "Mediation"));
        interestGroupExercise.setOnClickListener(v -> toggleInterest(interestGroupExercise, "Group Exercise"));
        interestPeaceCorps.setOnClickListener(v -> toggleInterest(interestPeaceCorps, "Peace Corps"));
        interestRolePlaying.setOnClickListener(v -> toggleInterest(interestRolePlaying, "Role Playing"));

        // Set up continue button listener
        btnInterestContinue.setOnClickListener(v -> {
            if (interestsHelper != null) {
                if (registrationData != null) {
                    // Get skills from the Skills activity through parent activity
                    Interests interestsActivity = (Interests) activity;
                    Set<String> skills = interestsActivity.getSkillsFromPreviousScreen();

                    // Log the skills being passed
                    Log.d("fragmentInterest2", "Skills being passed to saveAllUserData: " + skills);

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