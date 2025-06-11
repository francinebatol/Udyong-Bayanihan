package com.example.udyongbayanihan;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class AdminJoinedUsersDetails extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_joined_users_details);

        // Find views
        TextView userParticipantName = findViewById(R.id.userParticipantName);
        TextView userParticipantAge = findViewById(R.id.userParticipantAge);
        TextView userParticipantGender = findViewById(R.id.userParticipantGender);
        TextView userParticipantBarangay = findViewById(R.id.userParticipantBarangay);
        TextView userParticipantMunicipality = findViewById(R.id.userParticipantMunicipality);
        TextView userParticipantSkills = findViewById(R.id.userParticipantSkills);

        // Log and get data from Intent
        Intent intent = getIntent();
        Log.d("IntentData", "Received data: " + intent.getExtras());

        String firstName = intent.getStringExtra("firstName");
        String lastName = intent.getStringExtra("lastName");
        long age = intent.getLongExtra("age", -1);
        String gender = intent.getStringExtra("gender");
        String barangay = intent.getStringExtra("barangay");
        String municipality = intent.getStringExtra("municipality");
        ArrayList<String> skills = intent.getStringArrayListExtra("skills");

        // Set data
        userParticipantName.setText(firstName + " " + lastName);
        userParticipantAge.setText(age != -1 ? String.valueOf(age) : "Not specified");
        userParticipantGender.setText(gender != null ? gender : "Gender: Not specified");
        userParticipantBarangay.setText(barangay != null ? barangay : "Barangay: Not specified");
        userParticipantMunicipality.setText(municipality != null ? municipality : "Municipality: Not specified");
        userParticipantSkills.setText(skills != null && !skills.isEmpty() ? String.join(", ", skills) : "Skills: None");
    }
}