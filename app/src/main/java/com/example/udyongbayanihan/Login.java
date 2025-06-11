package com.example.udyongbayanihan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class Login extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText inputUsername, inputPassword;
    private ImageView imgViewPassword;
    private boolean isPasswordVisible = false;
    private View mainContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        100);
            }
        }

        // Hide the main content until we determine if we need to show login
        mainContent = findViewById(R.id.main);
        if (mainContent != null) {
            mainContent.setVisibility(View.INVISIBLE);
        }

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check for existing sessions
        checkExistingSessions();

        requestBatteryOptimizationExemption();

        inputPassword = findViewById(R.id.inputPassword);
        imgViewPassword = findViewById(R.id.imgViewPassword);

        viewPassword();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now show notifications
            } else {
                // Permission denied, you might want to inform the user
                Toast.makeText(this, "Notification permission is required for event updates",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void viewPassword() {
        imgViewPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    // Hide Password
                    inputPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                } else {
                    // Show Password
                    inputPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }
                isPasswordVisible = !isPasswordVisible;

                // Keep cursor at the end of text
                inputPassword.setSelection(inputPassword.getText().length());
            }
        });
    }

    private void checkCredentials() {
        String username = inputUsername.getText().toString().trim();
        String password = inputPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(Login.this, "Please enter both username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // First check admin credentials
        db.collection("AdminMobileAccount")
                .whereEqualTo("amUsername", username)
                .whereEqualTo("amPassword", password)
                .get()
                .addOnCompleteListener(adminTask -> {
                    if (adminTask.isSuccessful() && !adminTask.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : adminTask.getResult()) {
                            String amAccountId = document.getString("amAccountId");

                            // Save admin session
                            SharedPreferences adminPrefs = getSharedPreferences("AdminSession", MODE_PRIVATE);
                            SharedPreferences.Editor editor = adminPrefs.edit();
                            editor.putString("amAccountId", amAccountId);
                            editor.putString("amUsername", document.getString("amUsername"));
                            editor.putString("amEmail", document.getString("amEmail"));
                            editor.apply();

                            // Store the current device user ID for notification filtering
                            SharedPreferences notificationPrefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE);
                            notificationPrefs.edit().putString("current_device_user_id", amAccountId).apply();
                            Log.d("Login", "Set current device user ID for notifications (admin): " + amAccountId);

                            // Schedule message checks for admin
                            NotificationWorkManager.scheduleMessageChecks(this, amAccountId, "admin");

                            // Fetch related details and proceed to admin menu
                            fetchAdminDetails(amAccountId, adminDetails -> {
                                Intent intent = new Intent(Login.this, AdminMainMenu.class);
                                intent.putExtra("amAccountId", amAccountId);
                                intent.putExtra("amUsername", document.getString("amUsername"));
                                intent.putExtra("amEmail", document.getString("amEmail"));

                                Bundle amDetails = new Bundle();
                                for (Map.Entry<String, Object> entry : adminDetails.entrySet()) {
                                    if (entry.getValue() instanceof String) {
                                        amDetails.putString(entry.getKey(), (String) entry.getValue());
                                    } else if (entry.getValue() instanceof Long) {
                                        amDetails.putLong(entry.getKey(), (Long) entry.getValue());
                                    } else if (entry.getValue() instanceof Integer) {
                                        amDetails.putInt(entry.getKey(), (Integer) entry.getValue());
                                    }
                                }
                                intent.putExtra("amDetails", amDetails);

                                startActivity(intent);
                                finish();
                            });
                            return;
                        }
                    } else {
                        // If not admin, check regular user credentials
                        checkRegularUserCredentials(username, password);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(Login.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void checkRegularUserCredentials(String username, String password) {
        db.collection("usersAccount")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(userTask -> {
                    if (userTask.isSuccessful() && !userTask.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : userTask.getResult()) {
                            String email = document.getString("email");
                            String userId = document.getId();
                            fetchAdditionalIds(userId, ids -> authenticateUser(email, password, Home.class, ids));
                            return;
                        }
                    } else {
                        Toast.makeText(Login.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(Login.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void fetchAdditionalIds(String userId, FetchIdsCallback callback) {
        Map<String, String> ids = new HashMap<>();
        ids.put("userId", userId);

        db.collection("usersAddress").whereEqualTo("userId", userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                ids.put("uaddressId", task.getResult().getDocuments().get(0).getId());
            }

            db.collection("usersName").whereEqualTo("userId", userId).get().addOnCompleteListener(task2 -> {
                if (task2.isSuccessful() && !task2.getResult().isEmpty()) {
                    ids.put("unameId", task2.getResult().getDocuments().get(0).getId());
                }
                db.collection("usersOtherDetails").whereEqualTo("userId", userId).get().addOnCompleteListener(task3 -> {
                    if (task3.isSuccessful() && !task3.getResult().isEmpty()) {
                        ids.put("uotherDetails", task3.getResult().getDocuments().get(0).getId());
                    }
                    callback.onIdsFetched(ids);
                });
            });
        });
    }

    private void fetchAdminDetails(String amAccountId, OnCompleteListener<Map<String, Object>> callback) {
        Map<String, Object> adminDetails = new HashMap<>();

        db.collection("AMNameDetails")
                .whereEqualTo("amAccountId", amAccountId)
                .get()
                .addOnCompleteListener(nameTask -> {
                    if (nameTask.isSuccessful() && !nameTask.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot nameDoc : nameTask.getResult()) {
                            adminDetails.put("amFirstName", nameDoc.getString("amFirstName"));
                            adminDetails.put("amMiddleName", nameDoc.getString("amMiddleName"));
                            adminDetails.put("amLastName", nameDoc.getString("amLastName"));
                        }
                    }
                    db.collection("AMOtherDetails")
                            .whereEqualTo("amAccountId", amAccountId)
                            .get()
                            .addOnCompleteListener(otherTask -> {
                                if (otherTask.isSuccessful() && !otherTask.getResult().isEmpty()) {
                                    for (QueryDocumentSnapshot otherDoc : otherTask.getResult()) {
                                        adminDetails.put("amPhoneNo", otherDoc.getLong("amPhoneNo"));
                                        adminDetails.put("position", otherDoc.getString("position"));
                                    }
                                }
                                db.collection("AMAddressDetails")
                                        .whereEqualTo("amAccountId", amAccountId)
                                        .get()
                                        .addOnCompleteListener(addressTask -> {
                                            if (addressTask.isSuccessful() && !addressTask.getResult().isEmpty()) {
                                                for (QueryDocumentSnapshot addressDoc : addressTask.getResult()) {
                                                    adminDetails.put("amHouseNo", addressDoc.getLong("amHouseNo"));
                                                    adminDetails.put("amStreet", addressDoc.getString("amStreet"));
                                                    adminDetails.put("amBarangay", addressDoc.getString("amBarangay"));
                                                    adminDetails.put("amMunicipality", addressDoc.getString("amMunicipality"));
                                                }
                                            }
                                            callback.onComplete(adminDetails);
                                        });
                            });
                });
    }
    private interface FetchIdsCallback {
        void onIdsFetched(Map<String, String> ids);
    }

    public interface OnCompleteListener<T> {
        void onComplete(T result);
    }

    private void authenticateUser(String email, String password, Class<?> targetActivity, Map<String, String> ids) {
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser firebaseUser = mAuth.getCurrentUser();
                if (firebaseUser != null && firebaseUser.isEmailVerified()) {
                    // Save session data
                    SharedPreferences prefs = getSharedPreferences("UserSession", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("userId", ids.get("userId"));
                    editor.putString("userType", "user");
                    for (Map.Entry<String, String> entry : ids.entrySet()) {
                        editor.putString(entry.getKey(), entry.getValue());
                    }
                    editor.apply();

                    SharedPreferences notificationPrefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE);
                    notificationPrefs.edit().putString("current_device_user_id", ids.get("userId")).apply();
                    Log.d("Login", "Set current device user ID for notifications (user): " + ids.get("userId"));

                    // Schedule message checks for regular user
                    NotificationWorkManager.scheduleMessageChecks(this, ids.get("userId"), "user");

                    // Schedule verification status checks for user using the enhanced worker
                    // This will run both periodic checks (every 5 minutes) and an immediate check
                    NotificationWorkManager.scheduleVerificationStatusChecks(this, ids.get("userId"));

                    // Check for the upcoming event notification
                    NotificationWorkManager.scheduleDailyEventChecks(this);

                    // Perform an immediate check for verification status changes
                    NotificationWorkManager.checkAndNotifyVerificationStatus(this, ids.get("userId"));

                    Intent intent = new Intent(Login.this, targetActivity);
                    for (Map.Entry<String, String> entry : ids.entrySet()) {
                        intent.putExtra(entry.getKey(), entry.getValue());
                    }
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(Login.this, "Please verify your email before logging in.", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                }
            } else {
                Toast.makeText(Login.this, "Incorrect username or password.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkExistingSessions() {
        // First check for admin session
        SharedPreferences adminPrefs = getSharedPreferences("AdminSession", MODE_PRIVATE);
        String amAccountId = adminPrefs.getString("amAccountId", null);

        if (amAccountId != null) {
            // Admin is logged in, save admin session with userType
            SharedPreferences userPrefs = getSharedPreferences("UserSession", MODE_PRIVATE);
            SharedPreferences.Editor editor = userPrefs.edit();
            editor.putString("userId", amAccountId);
            editor.putString("userType", "admin");
            editor.apply();

            SharedPreferences notificationPrefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE);
            notificationPrefs.edit().putString("current_device_user_id", amAccountId).apply();
            Log.d("Login", "Set current device user ID for existing admin session: " + amAccountId);

            Intent serviceIntent = new Intent(Login.this, MessageNotificationWorker.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            // Fetch admin details and redirect
            fetchAdminDetails(amAccountId, adminDetails -> {
                Intent intent = new Intent(Login.this, AdminMainMenu.class);
                intent.putExtra("amAccountId", amAccountId);
                intent.putExtra("amUsername", adminPrefs.getString("amUsername", ""));
                intent.putExtra("amEmail", adminPrefs.getString("amEmail", ""));

                Bundle amDetails = new Bundle();
                for (Map.Entry<String, Object> entry : adminDetails.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        amDetails.putString(entry.getKey(), (String) entry.getValue());
                    } else if (entry.getValue() instanceof Long) {
                        amDetails.putLong(entry.getKey(), (Long) entry.getValue());
                    } else if (entry.getValue() instanceof Integer) {
                        amDetails.putInt(entry.getKey(), (Integer) entry.getValue());
                    }
                }
                intent.putExtra("amDetails", amDetails);

                startActivity(intent);
                finish();
            });
            return;
        }

        // If no admin session, check for regular user session
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && currentUser.isEmailVerified()) {
            SharedPreferences userPrefs = getSharedPreferences("UserSession", MODE_PRIVATE);
            String userId = userPrefs.getString("userId", null);
            String userType = userPrefs.getString("userType", null);

            if (userId != null) {
                // Ensure userType is set for existing sessions
                if (userType == null) {
                    SharedPreferences.Editor editor = userPrefs.edit();
                    editor.putString("userType", "user");
                    editor.apply();
                    userType = "user";
                }


                SharedPreferences notificationPrefs = getSharedPreferences("NotificationPrefs", MODE_PRIVATE);
                notificationPrefs.edit().putString("current_device_user_id", userId).apply();
                Log.d("Login", "Set current device user ID for existing user session: " + userId);

                // Schedule message checks
                NotificationWorkManager.scheduleMessageChecks(this, userId, userType);

                // For regular users, schedule verification checks with higher frequency
                if ("user".equals(userType)) {
                    // This schedules both periodic (every 5 min) and immediate verification checks
                    NotificationWorkManager.scheduleVerificationStatusChecks(this, userId);
                }

                // Schedule and check for the upcoming event
                NotificationWorkManager.scheduleDailyEventChecks(this);

                // Perform an immediate check for verification status changes
                if ("user".equals(userType)) {
                    NotificationWorkManager.checkAndNotifyVerificationStatus(this, userId);
                }

                Intent intent = new Intent(Login.this, Home.class);
                Map<String, ?> allPrefs = userPrefs.getAll();
                for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
                    intent.putExtra(entry.getKey(), entry.getValue().toString());
                }
                startActivity(intent);
                finish();
                return;
            }
        }

        // If we reach here, no valid session exists, so initialize the login UI
        initializeLoginUI();
    }

    private void initializeLoginUI() {
        // Make main content visible
        if (mainContent != null) {
            mainContent.setVisibility(View.VISIBLE);
        }

        // Initialize all UI elements
        inputUsername = findViewById(R.id.inputUsername);
        inputPassword = findViewById(R.id.inputPassword);

        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);
        TextView txtForgotPassword = findViewById(R.id.txtForgotPassword);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnLogin.setOnClickListener(v -> {
            btnLogin.setBackgroundColor(ContextCompat.getColor(Login.this, R.color.white));
            btnLogin.setTextColor(ContextCompat.getColor(Login.this, R.color.dark_green));

            new Handler().postDelayed(() -> {
                btnLogin.setBackgroundColor(ContextCompat.getColor(Login.this, R.color.dark_green));
                btnLogin.setTextColor(ContextCompat.getColor(Login.this, R.color.white));
            }, 600);
            checkCredentials();
        });

        btnCreateAccount.setOnClickListener(v -> startActivity(new Intent(Login.this, RegisterPhase1.class)));
        txtForgotPassword.setOnClickListener(v -> startActivity(new Intent(Login.this, ForgetPassword.class)));
    }

    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        100);
            }
        }    }
}
