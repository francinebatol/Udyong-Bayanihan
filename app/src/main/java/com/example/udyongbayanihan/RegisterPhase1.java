package com.example.udyongbayanihan;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RegisterPhase1 extends AppCompatActivity {

    private EditText inputFirstName, inputMiddleName, inputLastName;
    private Button inputDateOfBirth;
    private Spinner genderSpinner;
    private FirebaseFirestore db;
    private RegistrationData registrationData;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_phase1);

        db = FirebaseFirestore.getInstance();
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        inputFirstName = findViewById(R.id.inputFirstName);
        inputMiddleName = findViewById(R.id.inputMiddleName);
        inputLastName = findViewById(R.id.inputLastName);
        genderSpinner = findViewById(R.id.genderSpinner);
        inputDateOfBirth = findViewById(R.id.inputDateofBirth);

        // Check if registration data is already passed from a previous state
        if (getIntent().hasExtra("registrationData")) {
            registrationData = (RegistrationData) getIntent().getSerializableExtra("registrationData");

            // Restore saved values
            if (registrationData.getFirstName() != null) {
                inputFirstName.setText(registrationData.getFirstName());
            }
            if (registrationData.getMiddleName() != null) {
                inputMiddleName.setText(registrationData.getMiddleName());
            }
            if (registrationData.getLastName() != null) {
                inputLastName.setText(registrationData.getLastName());
            }
            if (registrationData.getDateOfBirth() != null) {
                inputDateOfBirth.setText(registrationData.getDateOfBirth());
            }
            if (registrationData.getGender() != null) {
                // Set spinner selection based on gender
                for (int i = 0; i < genderSpinner.getCount(); i++) {
                    if (genderSpinner.getItemAtPosition(i).toString().equals(registrationData.getGender())) {
                        genderSpinner.setSelection(i);
                        break;
                    }
                }
            }
        } else {
            // Initialize new registration data
            registrationData = new RegistrationData();
        }

        // Generate Firestore document IDs for later use
        if (registrationData.getUnameId() == null || registrationData.getUnameId().isEmpty()) {
            registrationData.setUnameId(db.collection("usersName").document().getId());
        }
        if (registrationData.getUotherDetailsId() == null || registrationData.getUotherDetailsId().isEmpty()) {
            registrationData.setUotherDetailsId(db.collection("usersOtherDetails").document().getId());
        }

        // Set up DatePicker for Date of Birth
        inputDateOfBirth.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    RegisterPhase1.this,
                    R.style.CustomDatePicker,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String formattedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        inputDateOfBirth.setText(formattedDate);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        ImageButton nextButton = findViewById(R.id.btnNext);
        nextButton.setOnClickListener(v -> validateAndProceed());
    }

    private void validateAndProceed() {
        // Reset borders to normal first
        resetFieldBorders();

        boolean hasErrors = false;
        String firstName = inputFirstName.getText().toString().trim();
        String middleName = inputMiddleName.getText().toString().trim();
        String lastName = inputLastName.getText().toString().trim();
        String gender = genderSpinner.getSelectedItem().toString();
        String dateOfBirth = inputDateOfBirth.getText().toString().trim();

        // Validate each field separately and set red border if empty
        if (firstName.isEmpty()) {
            inputFirstName.setBackground(getResources().getDrawable(R.drawable.red_border));
            hasErrors = true;
        }

        if (lastName.isEmpty()) {
            inputLastName.setBackground(getResources().getDrawable(R.drawable.red_border));
            hasErrors = true;
        }

        if (gender.equals("Select Gender")) {
            genderSpinner.setBackground(getResources().getDrawable(R.drawable.red_border));
            hasErrors = true;
        }

        if (dateOfBirth.isEmpty()) {
            inputDateOfBirth.setBackground(getResources().getDrawable(R.drawable.red_border));
            hasErrors = true;
        }

        if (hasErrors) {
            Toast.makeText(RegisterPhase1.this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate age from date of birth
        int age;
        try {
            age = calculateAge(dateOfBirth);
            if (age < 0) {
                // Future date selected, show error
                inputDateOfBirth.setBackground(getResources().getDrawable(R.drawable.red_border));
                Toast.makeText(RegisterPhase1.this, "Please enter a valid date of birth", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (ParseException e) {
            inputDateOfBirth.setBackground(getResources().getDrawable(R.drawable.red_border));
            Toast.makeText(RegisterPhase1.this, "Invalid date format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Store data in the registration data object
        registrationData.setFirstName(firstName);
        registrationData.setMiddleName(middleName);
        registrationData.setLastName(lastName);
        registrationData.setGender(gender);
        registrationData.setAge(age);
        registrationData.setDateOfBirth(dateOfBirth);

        // Navigate to next phase with data
        navigateToPhase2();
    }

    /**
     * Calculate age based on date of birth and current date
     * @param dateOfBirthStr Date of birth in format dd/MM/yyyy
     * @return Age in years
     */
    private int calculateAge(String dateOfBirthStr) throws ParseException {
        Date dateOfBirth = dateFormat.parse(dateOfBirthStr);
        Date currentDate = new Date();

        Calendar dobCalendar = Calendar.getInstance();
        Calendar currentCalendar = Calendar.getInstance();

        dobCalendar.setTime(dateOfBirth);
        currentCalendar.setTime(currentDate);

        int age = currentCalendar.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR);

        // Check if birthday has occurred this year
        if (dobCalendar.get(Calendar.MONTH) > currentCalendar.get(Calendar.MONTH) ||
                (dobCalendar.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH) &&
                        dobCalendar.get(Calendar.DAY_OF_MONTH) > currentCalendar.get(Calendar.DAY_OF_MONTH))) {
            age--;
        }

        return age;
    }

    // Method to reset all borders to normal
    private void resetFieldBorders() {
        inputFirstName.setBackground(getResources().getDrawable(R.drawable.border));
        inputMiddleName.setBackground(getResources().getDrawable(R.drawable.border));
        inputLastName.setBackground(getResources().getDrawable(R.drawable.border));
        genderSpinner.setBackground(getResources().getDrawable(R.drawable.border));
        inputDateOfBirth.setBackground(getResources().getDrawable(R.drawable.border));
    }

    private void navigateToPhase2() {
        Intent intent = new Intent(RegisterPhase1.this, RegisterPhase2.class);
        // Pass the registration data to the next phase
        intent.putExtra("registrationData", registrationData);
        startActivity(intent);
    }
}