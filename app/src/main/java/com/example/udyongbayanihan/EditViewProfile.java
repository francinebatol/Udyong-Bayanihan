package com.example.udyongbayanihan;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class EditViewProfile extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;

    private EditText editFirstName, editMiddleName, editLastName, editPhoneNumber, editUsername, editHouseno, editStreet, editBarangay, editBirthday;
    private Spinner editGender;
    private Button btnSaveChanges;
    private ImageView profileImageView;
    private ProgressBar imageUploadProgressBar;
    private TextView imageUploadStatus;

    private String userId;
    private String nameDocId, addressDocId, otherDetailsDocId;
    private String currentProfileImageUrl = null;

    private Uri profileImageUri = null;
    private boolean isProfileImageChanged = false;
    private File photoFile;
    private Uri cameraPhotoUri;
    private ImageView idPictureView;
    private ProgressBar idPictureProgressBar;
    private TextView idPictureUploadStatus;
    private Button btnChangeIdPicture;
    private String userStatus;
    private Uri idPictureUri = null;
    private boolean isIdPictureChanged = false;
    private String currentIdPictureUrl = null;
    private Long calculatedAge = null;
    private TextView verifiedUserMessage;

    // Activity result launcher for picking images from gallery
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    profileImageUri = result.getData().getData();
                    displaySelectedImage();
                }
            }
    );

    // Activity result launcher for capturing images with camera
    private final ActivityResultLauncher<Intent> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (cameraPhotoUri != null) {
                        profileImageUri = cameraPhotoUri;
                        displaySelectedImage();
                    } else {
                        // Fallback for camera thumbnail if full image capture fails
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            if (imageBitmap != null) {
                                profileImageView.setImageBitmap(imageBitmap);
                                saveBitmapToCache(imageBitmap);
                                isProfileImageChanged = true;
                            }
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_view_profile);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Retrieve user data from Intent
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
            Toast.makeText(this, "Error: User ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        editFirstName = findViewById(R.id.editFirstName);
        editMiddleName = findViewById(R.id.editMiddleName);
        editLastName = findViewById(R.id.editLastName);
        editPhoneNumber = findViewById(R.id.editPhoneNumber);
        editUsername = findViewById(R.id.editUsername);
        editHouseno = findViewById(R.id.editHouseno);
        editStreet = findViewById(R.id.editStreet);
        editBarangay = findViewById(R.id.editBarangay);
        editBirthday = findViewById(R.id.editBirthday);
        editGender = findViewById(R.id.editGender);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        profileImageView = findViewById(R.id.imageView2);
        idPictureView = findViewById(R.id.idPictureView);
        idPictureProgressBar = findViewById(R.id.idPictureUploadProgressBar);
        idPictureUploadStatus = findViewById(R.id.idPictureUploadStatus);
        btnChangeIdPicture = findViewById(R.id.btnChangeIdPicture);
        verifiedUserMessage = findViewById(R.id.verifiedUserMessage);

        // Set up DatePicker for birthday field
        editBirthday.setOnClickListener(v -> {
            // Only proceed if user status isn't "Verified"
            if (!"Verified".equals(userStatus)) {
                // Get current date or parse existing date
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH);
                int day = calendar.get(Calendar.DAY_OF_MONTH);

                // Parse existing date if available
                String currentDate = editBirthday.getText().toString();
                if (!currentDate.isEmpty()) {
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                        Date date = sdf.parse(currentDate);
                        if (date != null) {
                            calendar.setTime(date);
                            year = calendar.get(Calendar.YEAR);
                            month = calendar.get(Calendar.MONTH);
                            day = calendar.get(Calendar.DAY_OF_MONTH);
                        }
                    } catch (Exception e) {
                        // If parsing fails, use current date
                        e.printStackTrace();
                    }
                }

                // Create DatePickerDialog
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        EditViewProfile.this,
                        (view, selectedYear, selectedMonth, selectedDay) -> {
                            // Format the date
                            Calendar selectedCalendar = Calendar.getInstance();
                            selectedCalendar.set(selectedYear, selectedMonth, selectedDay);
                            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                            String formattedDate = sdf.format(selectedCalendar.getTime());

                            // Set the formatted date to EditText
                            editBirthday.setText(formattedDate);

                            // Calculate age and store in member variable
                            calculatedAge = (long) calculateAge(selectedCalendar.getTime());
                            // No longer setting to editUserAge
                        },
                        year, month, day);

                // Set max date to current date (no future dates)
                datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

                // Show dialog
                datePickerDialog.show();
            } else {
                Toast.makeText(EditViewProfile.this, "Verified users cannot change their birthday information", Toast.LENGTH_SHORT).show();
            }
        });

        btnChangeIdPicture.setOnClickListener(v -> {
            if ("Unverified".equals(userStatus) || "Denied".equals(userStatus)) {
                showIdPictureSourceOptions();
            } else {
                Toast.makeText(EditViewProfile.this, "You can only change ID picture when your status is Unverified or Denied", Toast.LENGTH_SHORT).show();
            }
        });

        // Add progress bar and status text for image upload
        imageUploadProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        imageUploadProgressBar.setVisibility(View.GONE);
        imageUploadStatus = new TextView(this);
        imageUploadStatus.setVisibility(View.GONE);

        // Set up click listener for profile image
        profileImageView.setOnClickListener(v -> showImageSourceOptions());

        // Fetch document IDs and current data
        fetchDocumentIds();

        // Populate fields with existing data from Intent
        populateFieldsFromIntent();

        // Save changes button
        btnSaveChanges.setOnClickListener(v -> saveProfileChanges());

        checkUserStatus();
    }

    private int calculateAge(Date birthDate) {
        Calendar birthCalendar = Calendar.getInstance();
        birthCalendar.setTime(birthDate);

        Calendar currentCalendar = Calendar.getInstance();

        int age = currentCalendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR);

        // Adjust age if birthday hasn't occurred yet this year
        if (currentCalendar.get(Calendar.MONTH) < birthCalendar.get(Calendar.MONTH) ||
                (currentCalendar.get(Calendar.MONTH) == birthCalendar.get(Calendar.MONTH) &&
                        currentCalendar.get(Calendar.DAY_OF_MONTH) < birthCalendar.get(Calendar.DAY_OF_MONTH))) {
            age--;
        }

        return age;
    }

    private void checkUserStatus() {
        db.collection("usersAccount")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        userStatus = document.getString("usersStatus");

                        // Enable/disable ID picture change based on status
                        boolean canChangeIdPicture = "Unverified".equals(userStatus) || "Denied".equals(userStatus);
                        btnChangeIdPicture.setEnabled(canChangeIdPicture);
                        btnChangeIdPicture.setAlpha(canChangeIdPicture ? 1.0f : 0.5f);

                        if (!canChangeIdPicture) {
                            btnChangeIdPicture.setText("ID Picture Cannot Be Changed");
                        }

                        // If user is verified, disable editing of fields except phone number
                        if ("Verified".equals(userStatus)) {
                            disableFieldsForVerifiedUser();
                            verifiedUserMessage.setVisibility(View.VISIBLE);
                        } else {
                            verifiedUserMessage.setVisibility(View.GONE);
                        }

                        // Load the current ID picture
                        loadIdPicture();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditViewProfile.this, "Failed to check status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void disableFieldsForVerifiedUser() {
        // Disable fields that verified users shouldn't be able to edit
        editFirstName.setEnabled(false);
        editMiddleName.setEnabled(false);
        editLastName.setEnabled(false);
        editUsername.setEnabled(false);
        editHouseno.setEnabled(false);
        editStreet.setEnabled(false);
        editBarangay.setEnabled(false);
        editBirthday.setEnabled(false);
        editGender.setEnabled(false);

        // Only phone number and profile picture remain enabled
        editPhoneNumber.setEnabled(true);

        // Apply visual indication for disabled fields
        editFirstName.setBackgroundResource(R.drawable.disabled_edit_background);
        editMiddleName.setBackgroundResource(R.drawable.disabled_edit_background);
        editLastName.setBackgroundResource(R.drawable.disabled_edit_background);
        editUsername.setBackgroundResource(R.drawable.disabled_edit_background);
        editHouseno.setBackgroundResource(R.drawable.disabled_edit_background);
        editStreet.setBackgroundResource(R.drawable.disabled_edit_background);
        editBarangay.setBackgroundResource(R.drawable.disabled_edit_background);
        editBirthday.setBackgroundResource(R.drawable.disabled_edit_background);
        editGender.setBackgroundResource(R.drawable.disabled_edit_background);
    }

    private void loadIdPicture() {
        db.collection("usersOtherDetails")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        currentIdPictureUrl = document.getString("idPicture");

                        // Load ID picture if available
                        if (currentIdPictureUrl != null && !currentIdPictureUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(currentIdPictureUrl)
                                    .placeholder(R.drawable.id_placeholder)
                                    .error(R.drawable.id_placeholder)
                                    .into(idPictureView);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditViewProfile.this, "Failed to load ID picture: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showIdPictureSourceOptions() {
        CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose ID Picture");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Take Photo")) {
                openIdCamera();
            } else if (options[item].equals("Choose from Gallery")) {
                openIdGallery();
            } else if (options[item].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void fetchDocumentIds() {
        // Find nameDocId
        db.collection("usersName")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            nameDocId = document.getId();
                            break;
                        }
                    }
                });

        // Find addressDocId
        db.collection("usersAddress")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            addressDocId = document.getId();
                            break;
                        }
                    }
                });

        // Find otherDetailsDocId and profile picture URL
        db.collection("usersOtherDetails")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            otherDetailsDocId = document.getId();
                            currentProfileImageUrl = document.getString("profilePictureUrl");

                            // Load profile image if available
                            if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(currentProfileImageUrl)
                                        .placeholder(R.drawable.user)
                                        .error(R.drawable.user)
                                        .into(profileImageView);
                            }
                            break;
                        }
                    }
                });
    }

    private void populateFieldsFromIntent() {
        String firstName = getIntent().getStringExtra("firstName");
        String middleName = getIntent().getStringExtra("middleName");
        String lastName = getIntent().getStringExtra("lastName");
        String age = getIntent().getStringExtra("age");
        String phoneNumber = getIntent().getStringExtra("phoneNo");
        String username = getIntent().getStringExtra("username");
        String houseNo = getIntent().getStringExtra("houseNo");
        String street = getIntent().getStringExtra("street");
        String barangay = getIntent().getStringExtra("barangay");
        String birthday = getIntent().getStringExtra("dateOfBirth");
        String gender = getIntent().getStringExtra("gender");

        // Populate fields with existing data
        editFirstName.setText(firstName);
        editMiddleName.setText(middleName);
        editLastName.setText(lastName);
        editPhoneNumber.setText(phoneNumber);
        editUsername.setText(username);
        editHouseno.setText(houseNo);
        editStreet.setText(street);
        editBarangay.setText(barangay);
        editBirthday.setText(birthday);

        if (birthday != null && !birthday.isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                Date date = sdf.parse(birthday);
                if (date != null) {
                    calculatedAge = (long) calculateAge(date);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Set the Spinner to the current gender
        String[] genderOptions = getResources().getStringArray(R.array.gender_options);
        for (int i = 0; i < genderOptions.length; i++) {
            if (genderOptions[i].equals(gender)) {
                editGender.setSelection(i);
                break;
            }
        }
    }

    private void showImageSourceOptions() {
        CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Profile Picture");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Take Photo")) {
                openCamera();
            } else if (options[item].equals("Choose from Gallery")) {
                openGallery();
            } else if (options[item].equals("Cancel")) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                photoFile = createImageFile();
                if (photoFile != null) {
                    cameraPhotoUri = FileProvider.getUriForFile(this,
                            "com.example.udyongbayanihan.fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
                    takePictureLauncher.launch(takePictureIntent);
                }
            } catch (IOException ex) {
                // If file creation failed, just get the thumbnail
                takePictureLauncher.launch(takePictureIntent);
            }
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
    }

    private void saveBitmapToCache(Bitmap bitmap) {
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "temp_profile_" + System.currentTimeMillis() + ".jpg");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
            stream.close();

            profileImageUri = Uri.fromFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save camera image", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void displaySelectedImage() {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), profileImageUri);
            profileImageView.setImageBitmap(bitmap);
            isProfileImageChanged = true;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadProfileImage(final Runnable onSuccess) {
        if (profileImageUri == null) {
            // No image to upload, proceed with next steps
            if (onSuccess != null) {
                onSuccess.run();
            }
            return;
        }

        // Show progress
        imageUploadProgressBar.setVisibility(View.VISIBLE);
        btnSaveChanges.setEnabled(false);

        // Create a unique filename
        String filename = "profile_pictures/" + userId + "_" + UUID.randomUUID().toString();
        StorageReference profilePicRef = storageRef.child(filename);

        try {
            // Compress the image to reduce storage usage
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), profileImageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] data = baos.toByteArray();

            // Upload the file
            UploadTask uploadTask = profilePicRef.putBytes(data);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Get the download URL
                profilePicRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String profilePictureUrl = uri.toString();

                    // Update profile picture URL in Firestore
                    if (otherDetailsDocId != null) {
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("profilePictureUrl", profilePictureUrl);

                        db.collection("usersOtherDetails")
                                .document(otherDetailsDocId)
                                .update(updateData)
                                .addOnSuccessListener(aVoid -> {
                                    imageUploadProgressBar.setVisibility(View.GONE);
                                    btnSaveChanges.setEnabled(true);
                                    currentProfileImageUrl = profilePictureUrl;

                                    // Execute success callback
                                    if (onSuccess != null) {
                                        onSuccess.run();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    imageUploadProgressBar.setVisibility(View.GONE);
                                    btnSaveChanges.setEnabled(true);
                                    Toast.makeText(EditViewProfile.this, "Failed to update profile picture URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        imageUploadProgressBar.setVisibility(View.GONE);
                        btnSaveChanges.setEnabled(true);
                        Toast.makeText(EditViewProfile.this, "Failed to get user details document ID", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    imageUploadProgressBar.setVisibility(View.GONE);
                    btnSaveChanges.setEnabled(true);
                    Toast.makeText(EditViewProfile.this, "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                imageUploadProgressBar.setVisibility(View.GONE);
                btnSaveChanges.setEnabled(true);
                Toast.makeText(EditViewProfile.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }).addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                imageUploadProgressBar.setProgress((int) progress);
            });
        } catch (IOException e) {
            imageUploadProgressBar.setVisibility(View.GONE);
            btnSaveChanges.setEnabled(true);
            Toast.makeText(EditViewProfile.this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfileChanges() {
        // For verified users, only update phone number and profile picture
        if ("Verified".equals(userStatus)) {
            saveVerifiedUserChanges();
        } else {
            saveAllProfileChanges();
        }
    }

    private void saveVerifiedUserChanges() {
        // Retrieve only the allowed values for verified users
        String newPhoneNumberString = editPhoneNumber.getText().toString().trim();

        // Basic validation
        Long newPhoneNumber = null;
        try {
            if (!newPhoneNumberString.isEmpty()) {
                newPhoneNumber = Long.parseLong(newPhoneNumberString);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        Long finalNewPhoneNumber = newPhoneNumber;

        // Check if profile image needs to be uploaded
        if (isProfileImageChanged && profileImageUri != null) {
            uploadProfileImage(() -> {
                // This will run after successful profile image upload
                updateVerifiedUserData(finalNewPhoneNumber);
            });
        } else {
            // No image changes, proceed with data update
            updateVerifiedUserData(newPhoneNumber);
        }
    }

    private void updateVerifiedUserData(Long phoneNumber) {
        // Update only phone number for verified users
        if (otherDetailsDocId != null && phoneNumber != null) {
            Map<String, Object> otherDetailsData = new HashMap<>();
            otherDetailsData.put("phoneNo", phoneNumber);

            db.collection("usersOtherDetails")
                    .document(otherDetailsDocId)
                    .update(otherDetailsData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                        // Return result to ViewProfile
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("refresh", true);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update phone number: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // If no phone number or document ID, just return to previous screen
            // since profile picture would have been updated already if changed
            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
            Intent resultIntent = new Intent();
            resultIntent.putExtra("refresh", true);
            setResult(RESULT_OK, resultIntent);
            finish();
        }
    }

    private void saveAllProfileChanges() {
        // Retrieve updated values
        String newFirstName = editFirstName.getText().toString().trim();
        String newMiddleName = editMiddleName.getText().toString().trim();
        String newLastName = editLastName.getText().toString().trim();
        String newPhoneNumberString = editPhoneNumber.getText().toString().trim();
        String newUsername = editUsername.getText().toString().trim();
        String newHouseNoString = editHouseno.getText().toString().trim();
        String newStreet = editStreet.getText().toString().trim();
        String newBarangay = editBarangay.getText().toString().trim();
        String newBirthday = editBirthday.getText().toString().trim();
        String newGender = editGender.getSelectedItem().toString();

        // Basic validation
        if (newFirstName.isEmpty() || newLastName.isEmpty() || newUsername.isEmpty()) {
            Toast.makeText(this, "First name, last name, and username are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Long newPhoneNumber = null, newHouseNo = null;
        try {
            if (!newPhoneNumberString.isEmpty()) {
                newPhoneNumber = Long.parseLong(newPhoneNumberString);
            }
            if (!newHouseNoString.isEmpty()) {
                newHouseNo = Long.parseLong(newHouseNoString);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers for phone number and house number", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if profile image or ID picture need to be uploaded
        Long finalNewHouseNo = newHouseNo;
        Long finalNewPhoneNumber = newPhoneNumber;

        if ((isProfileImageChanged && profileImageUri != null) || (isIdPictureChanged && idPictureUri != null)) {
            // First upload profile image if changed
            if (isProfileImageChanged && profileImageUri != null) {
                uploadProfileImage(() -> {
                    // Then upload ID picture if changed
                    if (isIdPictureChanged && idPictureUri != null) {
                        uploadIdPicture(() -> {
                            // This will run after successful uploads
                            updateUserData(newFirstName, newMiddleName, newLastName, calculatedAge, finalNewPhoneNumber,
                                    newUsername, finalNewHouseNo, newStreet, newBarangay, newBirthday, newGender);
                        });
                    } else {
                        // Just profile image was changed
                        updateUserData(newFirstName, newMiddleName, newLastName, calculatedAge, finalNewPhoneNumber,
                                newUsername, finalNewHouseNo, newStreet, newBarangay, newBirthday, newGender);
                    }
                });
            } else if (isIdPictureChanged && idPictureUri != null) {
                // Only ID picture was changed
                uploadIdPicture(() -> {
                    updateUserData(newFirstName, newMiddleName, newLastName, calculatedAge, finalNewPhoneNumber,
                            newUsername, finalNewHouseNo, newStreet, newBarangay, newBirthday, newGender);
                });
            }
        } else {
            // No image changes, proceed with data update
            updateUserData(newFirstName, newMiddleName, newLastName, calculatedAge, newPhoneNumber,
                    newUsername, newHouseNo, newStreet, newBarangay, newBirthday, newGender);
        }
    }

    private void updateUserData(String firstName, String middleName, String lastName,
                                Long age, Long phoneNumber, String username,
                                Long houseNo, String street, String barangay,
                                String birthday, String gender) {

        // Update name collection
        if (nameDocId != null) {
            Map<String, Object> nameData = new HashMap<>();
            nameData.put("firstName", firstName);
            nameData.put("middleName", middleName);
            nameData.put("lastName", lastName);

            db.collection("usersName")
                    .document(nameDocId)
                    .update(nameData)
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update name: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

        // Update address collection
        if (addressDocId != null) {
            Map<String, Object> addressData = new HashMap<>();
            if (houseNo != null) {
                addressData.put("houseNo", houseNo);
            }
            addressData.put("street", street);
            addressData.put("barangay", barangay);

            db.collection("usersAddress")
                    .document(addressDocId)
                    .update(addressData)
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update address: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

        // Update other details collection
        if (otherDetailsDocId != null) {
            Map<String, Object> otherDetailsData = new HashMap<>();
            if (age != null) {
                otherDetailsData.put("age", age);
            }
            if (phoneNumber != null) {
                otherDetailsData.put("phoneNo", phoneNumber);
            }
            otherDetailsData.put("gender", gender);
            otherDetailsData.put("dateOfBirth", birthday);

            db.collection("usersOtherDetails")
                    .document(otherDetailsDocId)
                    .update(otherDetailsData)
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update other details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

        // Update username in account collection
        db.collection("usersAccount")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);

                        db.collection("usersAccount")
                                .document(document.getId())
                                .update("username", username)
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to update username: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                });

        // Once all updates are initiated
        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
        Intent resultIntent = new Intent();
        resultIntent.putExtra("refresh", true);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    // Activity result launcher for picking ID images from gallery
    private final ActivityResultLauncher<Intent> pickIdImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    idPictureUri = result.getData().getData();
                    displaySelectedIdImage();
                }
            }
    );

    // Activity result launcher for capturing ID images with camera
    private final ActivityResultLauncher<Intent> takeIdPictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    // Similar to profile picture but for ID
                    if (cameraPhotoUri != null) {
                        idPictureUri = cameraPhotoUri;
                        displaySelectedIdImage();
                    } else {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            if (imageBitmap != null) {
                                idPictureView.setImageBitmap(imageBitmap);
                                saveIdBitmapToCache(imageBitmap);
                                isIdPictureChanged = true;
                            }
                        }
                    }
                }
            }
    );

    private void openIdCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                photoFile = createImageFile();
                if (photoFile != null) {
                    cameraPhotoUri = FileProvider.getUriForFile(this,
                            "com.example.udyongbayanihan.fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
                    takeIdPictureLauncher.launch(takePictureIntent);
                }
            } catch (IOException ex) {
                takeIdPictureLauncher.launch(takePictureIntent);
            }
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openIdGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIdImageLauncher.launch(intent);
    }

    private void displaySelectedIdImage() {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), idPictureUri);
            idPictureView.setImageBitmap(bitmap);
            isIdPictureChanged = true;
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveIdBitmapToCache(Bitmap bitmap) {
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File file = new File(cachePath, "temp_id_" + System.currentTimeMillis() + ".jpg");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
            stream.close();

            idPictureUri = Uri.fromFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save camera image", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadIdPicture(final Runnable onSuccess) {
        if (idPictureUri == null) {
            if (onSuccess != null) {
                onSuccess.run();
            }
            return;
        }

        // Show progress
        idPictureProgressBar.setVisibility(View.VISIBLE);
        btnSaveChanges.setEnabled(false);

        // Create a unique filename
        String filename = "id_pictures/" + userId + "_" + UUID.randomUUID().toString();
        StorageReference idPicRef = storageRef.child(filename);

        try {
            // Compress the image to reduce storage usage
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), idPictureUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] data = baos.toByteArray();

            // Upload the file
            UploadTask uploadTask = idPicRef.putBytes(data);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Get the download URL
                idPicRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String idPictureUrl = uri.toString();

                    // Update ID picture URL in Firestore
                    if (otherDetailsDocId != null) {
                        Map<String, Object> updateData = new HashMap<>();
                        updateData.put("idPicture", idPictureUrl); // Changed from idPictureUrl to idPicture

                        db.collection("usersOtherDetails")
                                .document(otherDetailsDocId)
                                .update(updateData)
                                .addOnSuccessListener(aVoid -> {
                                    idPictureProgressBar.setVisibility(View.GONE);
                                    btnSaveChanges.setEnabled(true);
                                    currentIdPictureUrl = idPictureUrl;

                                    // Reset user status to Unverified when they upload a new ID
                                    updateUserStatusToUnverified();

                                    // Execute success callback
                                    if (onSuccess != null) {
                                        onSuccess.run();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    idPictureProgressBar.setVisibility(View.GONE);
                                    btnSaveChanges.setEnabled(true);
                                    Toast.makeText(EditViewProfile.this, "Failed to update ID picture URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        idPictureProgressBar.setVisibility(View.GONE);
                        btnSaveChanges.setEnabled(true);
                        Toast.makeText(EditViewProfile.this, "Failed to get user details document ID", Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    idPictureProgressBar.setVisibility(View.GONE);
                    btnSaveChanges.setEnabled(true);
                    Toast.makeText(EditViewProfile.this, "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                idPictureProgressBar.setVisibility(View.GONE);
                btnSaveChanges.setEnabled(true);
                Toast.makeText(EditViewProfile.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }).addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                idPictureProgressBar.setProgress((int) progress);
            });
        } catch (IOException e) {
            idPictureProgressBar.setVisibility(View.GONE);
            btnSaveChanges.setEnabled(true);
            Toast.makeText(EditViewProfile.this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUserStatusToUnverified() {
        db.collection("usersAccount")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);

                        db.collection("usersAccount")
                                .document(document.getId())
                                .update("usersStatus", "Unverified")
                                .addOnSuccessListener(aVoid -> {
                                    userStatus = "Unverified";
                                    Toast.makeText(EditViewProfile.this, "Your verification status has been reset to Unverified", Toast.LENGTH_LONG).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(EditViewProfile.this, "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                });
    }
}