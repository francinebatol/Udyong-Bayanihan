package com.example.udyongbayanihan;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.Set;

public class SkillsHelper {
    private final Set<String> selectedSkills = new HashSet<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void toggleSkill(String skill) {
        if (selectedSkills.contains(skill)) {
            selectedSkills.remove(skill);
        } else {
            selectedSkills.add(skill);
        }
    }

    public boolean hasSkill(String skill) {
        return selectedSkills.contains(skill);
    }

    public Set<String> getSelectedSkills() {
        return selectedSkills;
    }

    // This method is now only for logging purposes, no Firestore interactions
    public void saveSkills(String uotherDetails, Context context) {
        // Just log the selected skills, don't attempt to access Firestore
        Log.d("SkillsHelper", "Skills selected: " + selectedSkills);
        Log.d("SkillsHelper", "Skills will be saved during final registration step");
    }
}