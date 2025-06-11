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

public class fragmentSkill1 extends Fragment {

    private SkillsHelper skillsHelper;
    private Button skillCollaboration, skillDedication, skillPatience, skillReliability, skillParticipation, btnSkillContinue;
    private String uotherDetails;
    private RegistrationData registrationData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_skill1, container, false);

        // Initialize the buttons
        skillCollaboration = view.findViewById(R.id.skillCollaboration);
        skillDedication = view.findViewById(R.id.skillDedication);
        skillPatience = view.findViewById(R.id.skillPatience);
        skillReliability = view.findViewById(R.id.skillReliability);
        skillParticipation = view.findViewById(R.id.skillParticipation);
        btnSkillContinue = view.findViewById(R.id.btnSkillContinue);

        // Initialize SkillsHelper
        FragmentActivity activity = requireActivity();
        if (activity instanceof Skills) {
            skillsHelper = ((Skills) activity).getSkillsHelper();
            uotherDetails = ((Skills) activity).getUsersOtherDetails(); // Get uotherDetails from Skills activity
            registrationData = ((Skills) activity).getRegistrationData(); // Get registration data
        }

        // Set initial state for buttons
        updateButtonState(skillCollaboration, "Collaboration");
        updateButtonState(skillDedication, "Dedication");
        updateButtonState(skillPatience, "Patience");
        updateButtonState(skillReliability, "Reliability");
        updateButtonState(skillParticipation, "Participation");

        // Set the click listener to toggle the button's state and color
        skillCollaboration.setOnClickListener(v -> toggleSkill(skillCollaboration, "Collaboration"));
        skillDedication.setOnClickListener(v -> toggleSkill(skillDedication, "Dedication"));
        skillPatience.setOnClickListener(v -> toggleSkill(skillPatience, "Patience"));
        skillReliability.setOnClickListener(v -> toggleSkill(skillReliability, "Reliability"));
        skillParticipation.setOnClickListener(v -> toggleSkill(skillParticipation, "Participation"));

        // Set up continue button listener
        btnSkillContinue.setOnClickListener(v -> {
            if (skillsHelper != null) {
                // Locally store skills (no Firestore save yet)
                skillsHelper.saveSkills(uotherDetails, requireContext());

                // Log skills that are selected
                Log.d("fragmentSkill1", "Selected skills: " + skillsHelper.getSelectedSkills());

                // Navigate with full registration data
                Intent intent = new Intent(activity, Interests.class);

                if (registrationData != null) {
                    intent.putExtra("registrationData", registrationData);
                }

                intent.putExtra("usersOtherDetails", uotherDetails);

                // Convert skills set to array for passing
                Set<String> selectedSkills = skillsHelper.getSelectedSkills();
                Log.d("fragmentSkill1", "Passing " + selectedSkills.size() + " skills to Interests activity");
                String[] skillsArray = selectedSkills.toArray(new String[0]);
                intent.putExtra("selectedSkills", skillsArray);

                startActivity(intent);
            } else {
                Toast.makeText(requireContext(), "Error: Skills helper not initialized", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    // Method to toggle the button state and color
    private void toggleSkill(Button button, String skill) {
        if (skillsHelper != null) {
            skillsHelper.toggleSkill(skill);
            updateButtonState(button, skill);
        }
    }

    // Method to update the button state and color
    private void updateButtonState(Button button, String skill) {
        if (skillsHelper != null) {
            if (skillsHelper.hasSkill(skill)) {
                button.setBackgroundColor(getResources().getColor(R.color.dark_green));
                button.setTextColor(getResources().getColor(R.color.white));
            } else {
                button.setBackgroundColor(getResources().getColor(R.color.white));
                button.setTextColor(getResources().getColor(R.color.black));
            }
        }
    }
}