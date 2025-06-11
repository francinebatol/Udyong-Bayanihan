package com.example.udyongbayanihan;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterPhase3 extends AppCompatActivity {

    private static final String TAG = "RegisterPhase3";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private EditText inputUsername, inputPassword, inputConfirmPassword, inputEmail;
    private RegistrationData registrationData;
    private boolean verificationEmailSent = false; // Flag to track if verification email was sent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_phase3);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements
        inputUsername = findViewById(R.id.inputUsername);
        inputPassword = findViewById(R.id.inputPassword);
        inputConfirmPassword = findViewById(R.id.inputConfirmPassword);
        inputEmail = findViewById(R.id.inputEmail);

        // Get registration data from Phase 2
        if (getIntent().hasExtra("registrationData")) {
            registrationData = (RegistrationData) getIntent().getSerializableExtra("registrationData");
            if (registrationData == null) {
                Toast.makeText(this, "Error: Missing registration data from previous steps", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            // Restore previous values if they exist
            if (registrationData.getUsername() != null) {
                inputUsername.setText(registrationData.getUsername());
            }
            if (registrationData.getEmail() != null) {
                inputEmail.setText(registrationData.getEmail());

                // Disable email field if we already have a userId (means account was created)
                if (registrationData.getUserId() != null && !registrationData.getUserId().isEmpty()) {
                    inputEmail.setEnabled(false);
                    verificationEmailSent = true; // Assume email was sent if userId exists
                }
            }
            // Don't restore password fields for security reasons
        } else {
            Toast.makeText(this, "Error: Missing registration data from previous steps", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ImageButton btnNext = findViewById(R.id.btnNext);
        btnNext.setOnClickListener(v -> validateAndProceed());
    }

    // Method to reset all borders to normal
    private void resetFieldBorders() {
        inputUsername.setBackground(getResources().getDrawable(R.drawable.border));
        inputEmail.setBackground(getResources().getDrawable(R.drawable.border));
        inputPassword.setBackground(getResources().getDrawable(R.drawable.border));
        inputConfirmPassword.setBackground(getResources().getDrawable(R.drawable.border));
    }

    private void validateAndProceed() {
        // Reset borders to normal first
        resetFieldBorders();

        boolean hasErrors = false;

        String username = inputUsername.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();
        String confirmPassword = inputConfirmPassword.getText().toString().trim();
        String email = inputEmail.getText().toString().trim();

        // Validation with red borders for errors
        if (username.isEmpty()) {
            inputUsername.setBackground(getResources().getDrawable(R.drawable.red_border));
            hasErrors = true;
        }

        if (email.isEmpty()) {
            inputEmail.setBackground(getResources().getDrawable(R.drawable.red_border));
            hasErrors = true;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            inputEmail.setBackground(getResources().getDrawable(R.drawable.red_border));
            hasErrors = true;
        }

        // Only validate password if we don't already have a userId
        if (registrationData.getUserId() == null || registrationData.getUserId().isEmpty()) {
            if (password.isEmpty()) {
                inputPassword.setBackground(getResources().getDrawable(R.drawable.red_border));
                hasErrors = true;
            }

            if (confirmPassword.isEmpty()) {
                inputConfirmPassword.setBackground(getResources().getDrawable(R.drawable.red_border));
                hasErrors = true;
            }

            if (!password.isEmpty() && !confirmPassword.isEmpty() && !password.equals(confirmPassword)) {
                inputPassword.setBackground(getResources().getDrawable(R.drawable.red_border));
                inputConfirmPassword.setBackground(getResources().getDrawable(R.drawable.red_border));
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                hasErrors = true;
            }
        }

        // If there are validation errors, show message and return
        if (hasErrors) {
            Toast.makeText(this, "Please fill in all required fields correctly", Toast.LENGTH_SHORT).show();
            return;
        }

        // Store username and email in registration data
        registrationData.setUsername(username);
        registrationData.setEmail(email);
        if (!password.isEmpty()) {
            registrationData.setPassword(password);
        }

        // If we already have a userId, skip authentication and go to Skills directly
        if (registrationData.getUserId() != null && !registrationData.getUserId().isEmpty()) {
            navigateToSkills();
            return;
        }

        // Check if username exists
        db.collection("usersAccount")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        inputUsername.setBackground(getResources().getDrawable(R.drawable.red_border));
                        Toast.makeText(this, "Username already exists. Please choose another one.", Toast.LENGTH_SHORT).show();
                    } else {
                        createAuthAccount(email, password);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error checking username: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void createAuthAccount(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            registrationData.setUserId(userId);

                            // Send verification email only if not sent already
                            if (!verificationEmailSent) {
                                sendVerificationEmail(user);
                                verificationEmailSent = true;
                            }

                            // Navigate to Skills with all the registration data
                            navigateToSkills();
                        }
                    } else {
                        // Check if the error is because the email is already in use
                        Exception exception = task.getException();
                        if (exception != null && exception.getMessage() != null &&
                                exception.getMessage().contains("email address is already in use")) {

                            // Try to sign in with the provided credentials to get the userId
                            signInWithExistingAccount(email, password);
                        } else {
                            // Handle other Firebase Auth errors with visual feedback
                            String errorMessage = "Authentication failed";
                            if (exception != null) {
                                errorMessage = exception.getMessage();

                                // Highlight the relevant field based on the error
                                if (errorMessage != null) {
                                    if (errorMessage.contains("email") || errorMessage.contains("already in use")) {
                                        inputEmail.setBackground(getResources().getDrawable(R.drawable.red_border));
                                    } else if (errorMessage.contains("password")) {
                                        inputPassword.setBackground(getResources().getDrawable(R.drawable.red_border));
                                    }
                                }
                            }

                            Toast.makeText(this, "Authentication failed: " + errorMessage,
                                    Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Authentication failed", exception);
                        }
                    }
                });
    }

    private void signInWithExistingAccount(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            registrationData.setUserId(userId);
                            verificationEmailSent = true; // Assume email was sent for existing account

                            // Navigate to Skills with the registration data
                            navigateToSkills();
                        }
                    } else {
                        // If signin fails, show error message
                        Toast.makeText(this, "This email is already registered but the password doesn't match.",
                                Toast.LENGTH_SHORT).show();
                        inputEmail.setBackground(getResources().getDrawable(R.drawable.red_border));
                        inputPassword.setBackground(getResources().getDrawable(R.drawable.red_border));
                    }
                });
    }

    private void sendVerificationEmail(FirebaseUser user) {
        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Verification email sent", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Error sending verification email", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToSkills() {
        Intent intent = new Intent(this, Skills.class);
        intent.putExtra("registrationData", registrationData);
        intent.putExtra("userId", registrationData.getUserId());
        intent.putExtra("unameId", registrationData.getUnameId());
        intent.putExtra("uaddressId", registrationData.getUaddressId());
        intent.putExtra("usersOtherDetails", registrationData.getUotherDetailsId());

        Log.d(TAG, "Navigating to Skills with registrationData and usersOtherDetails: " + registrationData.getUotherDetailsId());

        startActivity(intent);
        // Don't finish this activity so user can go back
    }
}