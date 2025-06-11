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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class ForgetPassword extends AppCompatActivity {

    EditText forgotEmail;
    MaterialButton btnReset, btnBack;
    ProgressBar progressBar;
    String strEmail;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    TextView adminForgotLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        // Handle Edge-to-Edge Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialization
        forgotEmail = findViewById(R.id.forgotEmail);
        btnReset = findViewById(R.id.setNewPassword);
        btnBack = findViewById(R.id.btnBack);
        adminForgotLink = findViewById(R.id.adminForgotLink);
        progressBar = findViewById(R.id.forgetPasswordProgressbar);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Reset Password Button Action
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                strEmail = forgotEmail.getText().toString().trim();
                if (!TextUtils.isEmpty(strEmail)) {
                    checkEmailAndResetPassword();
                } else {
                    forgotEmail.setError("Email field should not be empty");
                }
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        adminForgotLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ForgetPassword.this, AdminForgotPassword.class));
            }
        });
    }

    private void checkEmailAndResetPassword() {
        Log.d("ForgetPassword", "Check if email exists in the database: " + strEmail);
        progressBar.setVisibility(View.VISIBLE);
        btnReset.setVisibility(View.INVISIBLE);

        // Check if the email exists in Firestore
        db.collection("usersAccount") // collection where user data is stored
                .whereEqualTo("email", strEmail)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Email exists in the database, proceed with password reset
                            sendPasswordResetEmail();
                        } else {
                            // Email not found in the database
                            Log.d("ForgetPassword", "Email not found in Firestore.");
                            Toast.makeText(ForgetPassword.this, "Email not registered", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.INVISIBLE);
                            btnReset.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("ForgetPassword", "Error checking email in Firestore: " + e.getMessage());
                        Toast.makeText(ForgetPassword.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.INVISIBLE);
                        btnReset.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void sendPasswordResetEmail() {
        Log.d("ForgetPassword", "Sending password reset email to: " + strEmail);

        mAuth.sendPasswordResetEmail(strEmail)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Log.d("ForgetPassword", "Password reset email sent successfully.");

                        // Set "password_reset_required" flag in Firestore
                        db.collection("usersAccount")
                                .whereEqualTo("email", strEmail)
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                        doc.getReference().update("password_reset_required", true);
                                    }
                                });

                        Toast.makeText(ForgetPassword.this, "Reset password link has been sent to your email", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(ForgetPassword.this, Login.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    // Log and show the exact error message for debugging
                    Log.e("ForgetPassword", "Error sending reset email: " + e.getMessage());
                    Toast.makeText(ForgetPassword.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.INVISIBLE);
                    btnReset.setVisibility(View.VISIBLE);
                });
    }
}
