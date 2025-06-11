package com.example.udyongbayanihan;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import java.util.Set;

public class fragmentSkill6 extends Fragment {

    private SkillsHelper skillsHelper; // Instance of SkillsHelper
    private Button skillBenchmarking, skillMindfulness, skillProcessAnalysis, skillScenarioPlanning, skillTroubleshooting, btnSkillContinue;
    private String uotherDetails;
    private RegistrationData registrationData;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_skill6, container, false);

        // Initialize the button
        skillBenchmarking = view.findViewById(R.id.skillBenchmarking);
        skillMindfulness = view.findViewById(R.id.skillMindfulness);
        skillProcessAnalysis = view.findViewById(R.id.skillProcessAnalysis);
        skillScenarioPlanning = view.findViewById(R.id.skillScenarioPlanning);
        skillTroubleshooting = view.findViewById(R.id.skillTroubleshooting);
        btnSkillContinue = view.findViewById(R.id.btnSkillContinue);

        // Initialize SkillsHelper and userId
        FragmentActivity activity = requireActivity();
        if (activity instanceof Skills) {
            skillsHelper = ((Skills) activity).getSkillsHelper();
            uotherDetails = ((Skills) activity).getUsersOtherDetails(); // Get userId from Skills activity
            registrationData = ((Skills) activity).getRegistrationData(); // Get registration data
        }

        // Set initial state for buttons
        updateButtonState(skillBenchmarking, "Benchmarking");
        updateButtonState(skillMindfulness, "Mindfulness");
        updateButtonState(skillProcessAnalysis, "Process Analysis");
        updateButtonState(skillScenarioPlanning, "Scenario Planning");
        updateButtonState(skillTroubleshooting, "Troubleshooting");

        // Set the click listener to toggle the button's state and color
        skillBenchmarking.setOnClickListener(v -> toggleSkill(skillBenchmarking, "Benchmarking"));
        skillMindfulness.setOnClickListener(v -> toggleSkill(skillMindfulness, "Mindfulness"));
        skillProcessAnalysis.setOnClickListener(v -> toggleSkill(skillProcessAnalysis, "Process Analysis"));
        skillScenarioPlanning.setOnClickListener(v -> toggleSkill(skillScenarioPlanning, "Scenario Planning"));
        skillTroubleshooting.setOnClickListener(v -> toggleSkill(skillTroubleshooting, "Troubleshooting"));

        // Set up continue button listener
        btnSkillContinue.setOnClickListener(v -> {
            if (skillsHelper != null) {
                // Locally store skills (no Firestore save yet)
                skillsHelper.saveSkills(uotherDetails, requireContext());

                // Log skills that are selected
                Log.d("fragmentSkill6", "Selected skills: " + skillsHelper.getSelectedSkills());

                // Navigate with proper validation
                Intent intent = new Intent(activity, Interests.class);  // Use the existing activity variable

                if (registrationData != null) {
                    intent.putExtra("registrationData", registrationData);
                }

                intent.putExtra("usersOtherDetails", uotherDetails);

                // Convert skills set to array for passing
                Set<String> selectedSkills = skillsHelper.getSelectedSkills();
                Log.d("fragmentSkill6", "Passing " + selectedSkills.size() + " skills to Interests activity");
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