package com.example.udyongbayanihan;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class AdminForgotPassword extends AppCompatActivity {

    private static final String TAG = "AdminForgotPassword";

    private EditText adminUsername, adminOldPassword, adminNewPassword, adminConfirmPassword;
    private MaterialButton btnVerify, btnResetPassword, btnBack;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private String amAccountId;
    private TextView descriptionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_forgot_password);

        // Handle Edge-to-Edge Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        adminUsername = findViewById(R.id.adminUsername);
        adminOldPassword = findViewById(R.id.adminOldPassword);
        adminNewPassword = findViewById(R.id.adminNewPassword);
        adminConfirmPassword = findViewById(R.id.adminConfirmPassword);
        btnVerify = findViewById(R.id.btnVerify);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.adminProgressbar);
        descriptionText = findViewById(R.id.adminResetDescription);

        // Handle Verify Button Click
        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = adminUsername.getText().toString().trim();
                String oldPassword = adminOldPassword.getText().toString().trim();

                if (TextUtils.isEmpty(username)) {
                    adminUsername.setError("Username is required");
                    return;
                }

                if (TextUtils.isEmpty(oldPassword)) {
                    adminOldPassword.setError("Old password is required");
                    return;
                }

                verifyAdminCredentials(username, oldPassword);
            }
        });

        // Handle Reset Password Button Click
        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPassword = adminNewPassword.getText().toString().trim();
                String confirmPassword = adminConfirmPassword.getText().toString().trim();

                if (TextUtils.isEmpty(newPassword)) {
                    adminNewPassword.setError("New password is required");
                    return;
                }

                if (TextUtils.isEmpty(confirmPassword)) {
                    adminConfirmPassword.setError("Please confirm your new password");
                    return;
                }

                if (!newPassword.equals(confirmPassword)) {
                    adminConfirmPassword.setError("Passwords do not match");
                    return;
                }

                resetAdminPassword(newPassword);
            }
        });

        // Handle Back Button Click
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void verifyAdminCredentials(String username, String oldPassword) {
        Log.d(TAG, "Verifying admin credentials: " + username);
        progressBar.setVisibility(View.VISIBLE);
        btnVerify.setVisibility(View.INVISIBLE);

        db.collection("AdminMobileAccount")
                .whereEqualTo("amUsername", username)
                .whereEqualTo("amPassword", oldPassword)
                .whereEqualTo("Status", "Active")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                // Found matching active admin account
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    amAccountId = document.getId();
                                    Log.d(TAG, "Admin account found: " + amAccountId);

                                    // Show password reset fields
                                    adminUsername.setEnabled(false);
                                    adminOldPassword.setEnabled(false);
                                    adminNewPassword.setVisibility(View.VISIBLE);
                                    adminConfirmPassword.setVisibility(View.VISIBLE);
                                    btnVerify.setVisibility(View.GONE);
                                    btnResetPassword.setVisibility(View.VISIBLE);

                                    // Update description text
                                    descriptionText.setText("Please enter and confirm your new password");

                                    Toast.makeText(AdminForgotPassword.this,
                                            "Account verified. Please set your new password",
                                            Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.d(TAG, "No active admin account found with these credentials");
                                Toast.makeText(AdminForgotPassword.this,
                                        "Invalid credentials or inactive account",
                                        Toast.LENGTH_SHORT).show();
                                btnVerify.setVisibility(View.VISIBLE);
                            }
                        } else {
                            Log.e(TAG, "Error checking admin credentials", task.getException());
                            Toast.makeText(AdminForgotPassword.this,
                                    "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            btnVerify.setVisibility(View.VISIBLE);
                        }
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });
    }

    private void resetAdminPassword(String newPassword) {
        Log.d(TAG, "Resetting password for admin account: " + amAccountId);
        progressBar.setVisibility(View.VISIBLE);
        btnResetPassword.setVisibility(View.INVISIBLE);

        db.collection("AdminMobileAccount")
                .document(amAccountId)
                .update("amPassword", newPassword)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d(TAG, "Password successfully updated");
                        Toast.makeText(AdminForgotPassword.this,
                                "Password successfully reset",
                                Toast.LENGTH_SHORT).show();

                        // Navigate back to login
                        startActivity(new Intent(AdminForgotPassword.this, Login.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error updating password", e);
                        Toast.makeText(AdminForgotPassword.this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        btnResetPassword.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });
    }
}