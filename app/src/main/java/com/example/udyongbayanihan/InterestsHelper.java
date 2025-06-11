package com.example.udyongbayanihan;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InterestsHelper {
    private final Set<String> selectedInterests = new HashSet<>();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void toggleInterest(String interest) {
        if (selectedInterests.contains(interest)) {
            selectedInterests.remove(interest);
        } else {
            selectedInterests.add(interest);
        }
    }

    public boolean hasInterest(String interest) {
        return selectedInterests.contains(interest);
    }

    public Set<String> getSelectedInterests() {
        return selectedInterests;
    }

    // This is only used to temporarily store interests locally, not saving to Firestore yet
    public void saveInterests(String uotherDetails, Context context) {
        // Just log the selected interests, don't save to Firestore yet
        Log.d("InterestsHelper", "Interests selected: " + selectedInterests);
        Log.d("InterestsHelper", "Interests will be saved during final registration step");
    }

    // This is the main method that saves all user data to Firestore
    public void saveAllUserData(RegistrationData registrationData, Set<String> skills, Context context) {
        if (registrationData == null) {
            Log.e("InterestsHelper", "Cannot save data: registrationData is null");
            Toast.makeText(context, "Error: Missing registration data", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = registrationData.getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.e("InterestsHelper", "Cannot save data: userId is null or empty");
            Toast.makeText(context, "Error: Missing user ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("InterestsHelper", "Saving all user data for userId: " + userId);
        Log.d("InterestsHelper", "Selected interests: " + selectedInterests);
        Log.d("InterestsHelper", "Skills from previous screen: " + skills);

        WriteBatch batch = db.batch();

        try {
            // 1. Create account data with verification status
            Map<String, Object> accountData = new HashMap<>();
            accountData.put("username", registrationData.getUsername());
            accountData.put("email", registrationData.getEmail());
            accountData.put("userId", userId);
            accountData.put("usersStatus", "Unverified");

            // 2. Create name data
            Map<String, Object> nameData = new HashMap<>();
            nameData.put("firstName", registrationData.getFirstName());
            nameData.put("middleName", registrationData.getMiddleName());
            nameData.put("lastName", registrationData.getLastName());
            nameData.put("userId", userId);

            // 3. Create other details data
            Map<String, Object> otherDetailsData = new HashMap<>();
            otherDetailsData.put("age", registrationData.getAge());
            otherDetailsData.put("gender", registrationData.getGender());
            otherDetailsData.put("dateOfBirth", registrationData.getDateOfBirth());
            otherDetailsData.put("phoneNo", registrationData.getPhoneNo());
            otherDetailsData.put("idPicture", registrationData.getIdPictureUrl());
            otherDetailsData.put("userId", userId);
            otherDetailsData.put("interests", new ArrayList<>(selectedInterests));
            otherDetailsData.put("skills", new ArrayList<>(skills)); // Add skills from SkillsHelper

            // 4. Create address data
            Map<String, Object> addressData = new HashMap<>();
            if (registrationData.getHouseNo() != null && registrationData.getHouseNo() > 0) {
                addressData.put("houseNo", registrationData.getHouseNo());
            }
            addressData.put("street", registrationData.getStreet());
            addressData.put("userId", userId);

            // Handle barangay selection
            if (!registrationData.getBarangay().equals("Others")) {
                addressData.put("barangay", registrationData.getBarangay());
                addressData.put("municipality", "Orion");
            } else {
                addressData.put("otherBarangay", registrationData.getOtherBarangay());
                addressData.put("municipality", registrationData.getMunicipality());
            }

            // Add all operations to the batch
            batch.set(db.collection("usersAccount").document(userId), accountData);
            batch.set(db.collection("usersName").document(registrationData.getUnameId()), nameData);
            batch.set(db.collection("usersOtherDetails").document(registrationData.getUotherDetailsId()), otherDetailsData);
            batch.set(db.collection("usersAddress").document(registrationData.getUaddressId()), addressData);

            // Execute the batch (all operations succeed or fail together)
            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        Log.d("InterestsHelper", "All user data saved successfully");
                        Toast.makeText(context, "Account created successfully", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("InterestsHelper", "Error saving user data", e);
                        Toast.makeText(context, "Error creating account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            Log.e("InterestsHelper", "Error preparing batch updates", e);
            Toast.makeText(context, "Error preparing data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}