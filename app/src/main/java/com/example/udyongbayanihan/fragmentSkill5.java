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

public class fragmentSkill5 extends Fragment {

    private SkillsHelper skillsHelper; // Instance of SkillsHelper
    private Button skillBrainstorming, skillDesign, skillInnovation, skillExplorationAndDiscovery, skillVisualThinking, btnSkillContinue;
    private String uotherDetails;
    private RegistrationData registrationData;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_skill5, container, false);

        // Initialize the button
        skillBrainstorming = view.findViewById(R.id.skillBrainstorming);
        skillDesign = view.findViewById(R.id.skillDesign);
        skillInnovation = view.findViewById(R.id.skillInnovation);
        skillExplorationAndDiscovery = view.findViewById(R.id.skillExplorationAndDiscovery);
        skillVisualThinking = view.findViewById(R.id.skillVisualThinking);
        btnSkillContinue = view.findViewById(R.id.btnSkillContinue);

        // Initialize SkillsHelper and userId
        FragmentActivity activity = requireActivity();
        if (activity instanceof Skills) {
            skillsHelper = ((Skills) activity).getSkillsHelper();
            uotherDetails = ((Skills) activity).getUsersOtherDetails(); // Get userId from Skills activity
            registrationData = ((Skills) activity).getRegistrationData(); // Get registration data
        }

        // Set initial state for buttons
        updateButtonState(skillBrainstorming, "Brainstorming");
        updateButtonState(skillDesign, "Design");
        updateButtonState(skillInnovation, "Innovation");
        updateButtonState(skillExplorationAndDiscovery, "Exploration and Discovery");
        updateButtonState(skillVisualThinking, "Visual Thinking");

        // Set the click listener to toggle the button's state and color
        skillBrainstorming.setOnClickListener(v -> toggleSkill(skillBrainstorming, "Brainstorming"));
        skillDesign.setOnClickListener(v -> toggleSkill(skillDesign, "Design"));
        skillInnovation.setOnClickListener(v -> toggleSkill(skillInnovation, "Innovation"));
        skillExplorationAndDiscovery.setOnClickListener(v -> toggleSkill(skillExplorationAndDiscovery, "Exploration and Discovery"));
        skillVisualThinking.setOnClickListener(v -> toggleSkill(skillVisualThinking, "Visual Thinking"));

        // Set up continue button listener
        btnSkillContinue.setOnClickListener(v -> {
            if (skillsHelper != null) {
                // Locally store skills (no Firestore save yet)
                skillsHelper.saveSkills(uotherDetails, requireContext());

                // Log skills that are selected
                Log.d("fragmentSkill5", "Selected skills: " + skillsHelper.getSelectedSkills());

                // Navigate with proper validation
                Intent intent = new Intent(activity, Interests.class);  // Use the existing activity variable

                if (registrationData != null) {
                    intent.putExtra("registrationData", registrationData);
                }

                intent.putExtra("usersOtherDetails", uotherDetails);

                // Convert skills set to array for passing
                Set<String> selectedSkills = skillsHelper.getSelectedSkills();
                Log.d("fragmentSkill5", "Passing " + selectedSkills.size() + " skills to Interests activity");
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