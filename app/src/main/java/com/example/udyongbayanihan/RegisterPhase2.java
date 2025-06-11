package com.example.udyongbayanihan;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class RegisterPhase2 extends AppCompatActivity {

    private EditText inputHouseNo, inputStreet, inputPhoneNo, inputOtherBarangay;
    private Spinner inputBarangay, inputMunicipality;
    private FirebaseFirestore db;
    private RegistrationData registrationData;
    private ImageButton nextButton;

    // ID picture components
    private ImageView idPictureImageView;
    private Button uploadIdButton;
    private TextView idUploadStatus;
    private ProgressBar uploadProgressBar;
    private Uri idPictureUri = null;
    private boolean isIdPictureUploaded = false;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private Uri cameraPhotoUri;
    private File photoFile;

    // Loading overlay components
    private FrameLayout loadingOverlay;
    private ProgressBar loadingProgressBar;
    private TextView loadingText;

    // Activity result launcher for picking images from gallery
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    idPictureUri = result.getData().getData();
                    displaySelectedImage();
                }
            }
    );

    // Activity result launcher for capturing images with camera
    private final ActivityResultLauncher<Intent> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    idPictureUri = cameraPhotoUri;
                    displaySelectedImage();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_phase2);

        // Initialize Firestore instance
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();

        // Get registration data from Phase 1
        if (getIntent().hasExtra("registrationData")) {
            registrationData = (RegistrationData) getIntent().getSerializableExtra("registrationData");
            if (registrationData == null) {
                Toast.makeText(this, "Error: Missing registration data from Phase 1", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        } else {
            Toast.makeText(this, "Error: Missing registration data from Phase 1", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Generate address document ID for later use if needed
        if (registrationData.getUaddressId() == null || registrationData.getUaddressId().isEmpty()) {
            registrationData.setUaddressId(db.collection("usersAddress").document().getId());
        }

        // Initialize EditText fields for Phase 2
        inputHouseNo = findViewById(R.id.inputHouseNo);
        inputStreet = findViewById(R.id.inputStreet);
        inputBarangay = findViewById(R.id.inputBarangay);
        inputPhoneNo = findViewById(R.id.inputPhoneNo);
        inputOtherBarangay = findViewById(R.id.inputOtherBarangay);
        inputMunicipality = findViewById(R.id.inputMunicipality);

        // Initialize ID Picture upload components
        idPictureImageView = findViewById(R.id.idPictureImageView);
        uploadIdButton = findViewById(R.id.uploadIdButton);
        idUploadStatus = findViewById(R.id.idUploadStatus);
        uploadProgressBar = findViewById(R.id.uploadProgressBar);

        // Initialize loading overlay
        setupLoadingOverlay();

        // Set initial visibility
        idPictureImageView.setVisibility(View.GONE);
        idUploadStatus.setVisibility(View.GONE);
        uploadProgressBar.setVisibility(View.GONE);

        // Change button text and make it visible
        uploadIdButton.setVisibility(View.VISIBLE);
        uploadIdButton.setText("Select ID Picture");

        // Restore previous values if they exist
        if (registrationData.getHouseNo() != null && registrationData.getHouseNo() > 0) {
            inputHouseNo.setText(String.valueOf(registrationData.getHouseNo()));
        }
        if (registrationData.getStreet() != null) {
            inputStreet.setText(registrationData.getStreet());
        }
        if (registrationData.getPhoneNo() != null && registrationData.getPhoneNo() > 0) {
            inputPhoneNo.setText(String.valueOf(registrationData.getPhoneNo()));
        }
        if (registrationData.getIdPictureUrl() != null && !registrationData.getIdPictureUrl().isEmpty()) {
            // ID picture was already uploaded
            idUploadStatus.setText("ID Picture Uploaded Successfully");
            idUploadStatus.setVisibility(View.VISIBLE);
            isIdPictureUploaded = true;
        }

        // Spinner item selected listener
        inputBarangay.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedBarangay = inputBarangay.getSelectedItem().toString();

                if (selectedBarangay.equals("Others")) {
                    // Show input fields for custom barangay and municipality
                    inputOtherBarangay.setVisibility(View.VISIBLE);
                    inputOtherBarangay.setEnabled(true);

                    // Set layout parameters to push the other fields down
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) inputOtherBarangay.getLayoutParams();
                    params.topMargin = getResources().getDimensionPixelSize(R.dimen.standard_margin); // Define this dimension in dimens.xml
                    inputOtherBarangay.setLayoutParams(params);

                    inputMunicipality.setVisibility(View.VISIBLE);
                    inputMunicipality.setEnabled(true);
                } else if (!selectedBarangay.equals("Select Barangay")) {
                    // Hide other barangay input field
                    inputOtherBarangay.setVisibility(View.GONE);
                    inputOtherBarangay.setEnabled(false);

                    // Auto-select "Orion" as municipality
                    inputMunicipality.setVisibility(View.VISIBLE);
                    inputMunicipality.setEnabled(false);

                    // Find the position of "Orion" in the spinner and set it
                    for (int i = 0; i < inputMunicipality.getCount(); i++) {
                        if (inputMunicipality.getItemAtPosition(i).toString().equals("Orion")) {
                            inputMunicipality.setSelection(i);
                            break;
                        }
                    }
                } else {
                    // For "Select Barangay" - hide other barangay input
                    inputOtherBarangay.setVisibility(View.GONE);
                    inputOtherBarangay.setEnabled(false);

                    // Show municipality spinner but set to default
                    inputMunicipality.setVisibility(View.VISIBLE);
                    inputMunicipality.setEnabled(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // Do nothing
            }
        });

        // Restore barangay selection if exists
        if (registrationData.getBarangay() != null) {
            // Set spinner selection based on barangay
            for (int i = 0; i < inputBarangay.getCount(); i++) {
                if (inputBarangay.getItemAtPosition(i).toString().equals(registrationData.getBarangay())) {
                    inputBarangay.setSelection(i);
                    break;
                }
            }

            // If "Others" was selected, populate the other fields
            if (registrationData.getBarangay().equals("Others")) {
                inputOtherBarangay.setText(registrationData.getOtherBarangay());

                // Set municipality spinner
                for (int i = 0; i < inputMunicipality.getCount(); i++) {
                    if (inputMunicipality.getItemAtPosition(i).toString().equals(registrationData.getMunicipality())) {
                        inputMunicipality.setSelection(i);
                        break;
                    }
                }
            }
        }

        // Initialize the Next button
        nextButton = findViewById(R.id.btnNext);
        nextButton.setOnClickListener(v -> validateAndProceed());

        // Upload ID button click listener - now used only for selecting an image
        uploadIdButton.setOnClickListener(v -> {
            if (idPictureUri != null) {
                // Already have an image selected, ask if they want to select a new one
                new AlertDialog.Builder(this)
                        .setTitle("Change ID Picture")
                        .setMessage("You've already selected an ID picture. Would you like to select a new one?")
                        .setPositiveButton("Yes", (dialog, which) -> showImageSourceOptions())
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .show();
            } else {
                // No image selected yet, show options
                showImageSourceOptions();
            }
        });
    }

    /**
     * Setup the loading overlay for the form
     */
    private void setupLoadingOverlay() {
        // Create loading overlay programmatically
        ConstraintLayout mainLayout = findViewById(R.id.main);

        loadingOverlay = new FrameLayout(this);
        ConstraintLayout.LayoutParams overlayParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
        );
        loadingOverlay.setLayoutParams(overlayParams);
        loadingOverlay.setBackgroundColor(getResources().getColor(android.R.color.black));
        loadingOverlay.setAlpha(0.5f);
        loadingOverlay.setVisibility(View.GONE);
        loadingOverlay.setClickable(true);  // Prevent clicks through overlay

        // Add a loading spinner
        loadingProgressBar = new ProgressBar(this);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        progressParams.gravity = android.view.Gravity.CENTER;
        loadingProgressBar.setLayoutParams(progressParams);

        // Add loading text
        loadingText = new TextView(this);
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.gravity = android.view.Gravity.CENTER;
        textParams.setMargins(0, 150, 0, 0);  // Position below spinner
        loadingText.setLayoutParams(textParams);
        loadingText.setText("Uploading ID Picture...");
        loadingText.setTextColor(getResources().getColor(android.R.color.white));

        // Add views to overlay
        loadingOverlay.addView(loadingProgressBar);
        loadingOverlay.addView(loadingText);

        // Add overlay to main layout
        mainLayout.addView(loadingOverlay);
    }

    /**
     * Disable all input fields during upload
     */
    private void disableAllInputs() {
        inputHouseNo.setEnabled(false);
        inputStreet.setEnabled(false);
        inputBarangay.setEnabled(false);
        inputPhoneNo.setEnabled(false);
        inputOtherBarangay.setEnabled(false);
        inputMunicipality.setEnabled(false);
        uploadIdButton.setEnabled(false);
        nextButton.setEnabled(false);
    }

    /**
     * Re-enable all input fields after upload
     */
    private void enableAllInputs() {
        inputHouseNo.setEnabled(true);
        inputStreet.setEnabled(true);
        inputBarangay.setEnabled(true);
        inputPhoneNo.setEnabled(true);

        // Only enable these fields if needed based on barangay selection
        String selectedBarangay = inputBarangay.getSelectedItem().toString();
        if (selectedBarangay.equals("Others")) {
            inputOtherBarangay.setEnabled(true);
            inputMunicipality.setEnabled(true);
        }

        uploadIdButton.setEnabled(true);
        nextButton.setEnabled(true);
    }

    /**
     * Show loading state
     */
    private void showLoading() {
        loadingOverlay.setVisibility(View.VISIBLE);
        uploadProgressBar.setVisibility(View.VISIBLE);
        idUploadStatus.setText("Uploading...");
        disableAllInputs();
    }

    /**
     * Hide loading state
     */
    private void hideLoading() {
        loadingOverlay.setVisibility(View.GONE);
        uploadProgressBar.setVisibility(View.GONE);
        enableAllInputs();
    }

    private void showImageSourceOptions() {
        CharSequence[] options = {"Take Photo", "Choose from Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select ID Picture Source");
        builder.setItems(options, (dialog, item) -> {
            if (options[item].equals("Take Photo")) {
                openCamera();
            } else if (options[item].equals("Choose from Gallery")) {
                openGallery();
            }
        });
        builder.show();
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                cameraPhotoUri = FileProvider.getUriForFile(this,
                        "com.example.udyongbayanihan.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
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

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void displaySelectedImage() {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), idPictureUri);
            idPictureImageView.setImageBitmap(bitmap);
            idPictureImageView.setVisibility(View.VISIBLE);
            idUploadStatus.setText("ID Picture Selected");
            idUploadStatus.setVisibility(View.VISIBLE);
            uploadIdButton.setText("Change ID Picture");
            isIdPictureUploaded = false;

            // Reset ID button border to normal when an image is selected
            uploadIdButton.setBackground(getResources().getDrawable(R.drawable.border));
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(RegisterPhase2.this, "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadIdPicture(final Runnable onSuccess) {
        if (idPictureUri == null) {
            Toast.makeText(this, "Please select an ID picture first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state and disable all inputs
        showLoading();

        // Create a unique filename
        String filename = "id_pictures/" + UUID.randomUUID().toString();
        StorageReference idPictureRef = storageRef.child(filename);

        try {
            // Compress the image to reduce storage usage
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), idPictureUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] data = baos.toByteArray();

            // Upload the file
            UploadTask uploadTask = idPictureRef.putBytes(data);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Get the download URL
                idPictureRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String idPictureUrl = uri.toString();

                    // Store the URL in registration data instead of Firestore
                    registrationData.setIdPictureUrl(idPictureUrl);

                    // Update UI
                    hideLoading();
                    idUploadStatus.setText("ID Picture Uploaded Successfully");
                    isIdPictureUploaded = true;

                    // Execute the success callback
                    if (onSuccess != null) {
                        onSuccess.run();
                    }
                }).addOnFailureListener(e -> {
                    // Update UI on failure
                    hideLoading();
                    idUploadStatus.setText("Upload Failed");
                    Toast.makeText(RegisterPhase2.this, "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                // Update UI on failure
                hideLoading();
                idUploadStatus.setText("Upload Failed");
                Toast.makeText(RegisterPhase2.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }).addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                uploadProgressBar.setProgress((int) progress);

                // Update progress text
                int progressInt = (int) progress;
                loadingText.setText("Uploading ID Picture... " + progressInt + "%");
            });
        } catch (IOException e) {
            // Update UI on error
            hideLoading();
            idUploadStatus.setText("Upload Failed");
            Toast.makeText(RegisterPhase2.this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Method to reset all borders to normal
    private void resetFieldBorders() {
        inputHouseNo.setBackground(getResources().getDrawable(R.drawable.border));
        inputStreet.setBackground(getResources().getDrawable(R.drawable.border));
        inputBarangay.setBackground(getResources().getDrawable(R.drawable.border));
        inputPhoneNo.setBackground(getResources().getDrawable(R.drawable.border));
        inputOtherBarangay.setBackground(getResources().getDrawable(R.drawable.border));
        inputMunicipality.setBackground(getResources().getDrawable(R.drawable.border));
        uploadIdButton.setBackground(getResources().getDrawable(R.drawable.border));
    }

    private void validateAndProceed() {
        // Reset borders to normal first
        resetFieldBorders();

        boolean hasErrors = false;

        // Retrieve the data entered in Phase 2
        String houseNoInput = inputHouseNo.getText().toString().trim();
        String street = inputStreet.getText().toString().trim();
        String barangay = inputBarangay.getSelectedItem().toString().trim();
        String phoneNoInput = inputPhoneNo.getText().toString().trim();
        String otherBarangay = inputOtherBarangay.getText().toString().trim();
        String municipality = inputMunicipality.getSelectedItem().toString().trim();

        // Validate required fields
        if (barangay.equals("Select Barangay")) {
            inputBarangay.setBackground(getResources().getDrawable(R.drawable.red_border));
            hasErrors = true;
        }

        if (barangay.equals("Others")) {
            if (otherBarangay.isEmpty()) {
                inputOtherBarangay.setBackground(getResources().getDrawable(R.drawable.red_border));
                hasErrors = true;
            }
            if (municipality.equals("Select Municipality")) {
                inputMunicipality.setBackground(getResources().getDrawable(R.drawable.red_border));
                hasErrors = true;
            }
        }

        if (phoneNoInput.isEmpty()) {
            inputPhoneNo.setBackground(getResources().getDrawable(R.drawable.red_border));
            hasErrors = true;
        } else {
            // Validate phone number format
            try {
                Long.parseLong(phoneNoInput);
            } catch (NumberFormatException e) {
                inputPhoneNo.setBackground(getResources().getDrawable(R.drawable.red_border));
                hasErrors = true;
            }
        }

        // Check if ID picture is selected
        if (idPictureUri == null && registrationData.getIdPictureUrl() == null) {
            uploadIdButton.setBackground(getResources().getDrawable(R.drawable.red_border));
            hasErrors = true;
        }

        // Validate house number if provided
        if (!houseNoInput.isEmpty()) {
            try {
                Long.parseLong(houseNoInput);
            } catch (NumberFormatException e) {
                inputHouseNo.setBackground(getResources().getDrawable(R.drawable.red_border));
                hasErrors = true;
            }
        }

        // If there are validation errors, show error message and stop
        if (hasErrors) {
            Toast.makeText(RegisterPhase2.this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse house number if provided (after validation)
        long houseNo = 0;
        if (!houseNoInput.isEmpty()) {
            houseNo = Long.parseLong(houseNoInput);
        }

        // Parse phone number (after validation)
        long phoneNo = Long.parseLong(phoneNoInput);

        // Save validated data to registration object
        registrationData.setHouseNo(houseNo);
        registrationData.setStreet(street);
        registrationData.setBarangay(barangay);
        registrationData.setOtherBarangay(otherBarangay);
        registrationData.setMunicipality(municipality);
        registrationData.setPhoneNo(phoneNo);

        // If the image is not yet uploaded, upload it now and then proceed
        if (!isIdPictureUploaded && idPictureUri != null) {
            uploadIdPicture(this::navigateToPhase3);
        } else {
            // Image is already uploaded or was loaded from previous state, proceed directly
            navigateToPhase3();
        }
    }

    private void navigateToPhase3() {
        Intent intent = new Intent(RegisterPhase2.this, RegisterPhase3.class);
        intent.putExtra("registrationData", registrationData);
        startActivity(intent);
    }
}