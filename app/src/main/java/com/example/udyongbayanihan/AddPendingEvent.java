package com.example.udyongbayanihan;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AddPendingEvent extends AppCompatActivity {

    private FirebaseFirestore db;
    private EditText inputNameOfEvent, inputOrganization, inputDate, inputHeadCoordinator, inputCaption, inputVolunteersNeeded;
    private ImageButton inputImage;
    private Spinner inputTypeOfEvent, inputAddress;
    private CheckBox checkboxCollaboration, checkboxDedication, checkboxPatience, checkboxReliability, checkboxParticipation,
            checkboxDecisionsMaking, checkboxCoaching, checkboxMentoring, checkboxPlanning, checkboxTraining,
            checkboxCriticalThinking, checkboxDataGathering, checkboxDetermination, checkboxResearch, checkboxFlexibility,
            checkboxAdaptability, checkboxConflictResolution, checkboxResilience, checkboxEmotionalIntelligence, checkboxEmpathy,
            checkboxBrainstorming, checkboxDesign, checkboxInnovation, checkboxExplorationAndDiscovery, checkboxVisualThinking,
            checkboxBenchmarking, checkboxMindfulness, checkboxProcessAnalysis, checkboxScenarioPlanning, checkboxTroubleshooting;
    private Button addEventButton;
    private ArrayList<EditText> skillsFields;
    private String amAccountId, eventId;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_CODE_DATE_PICKER = 100;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private RecyclerView selectedImagesPreview;
    private ImagesAdapter imagesAdapter;
    private List<Uri> imageUris;
    private EditText inputVolunteersPerBarangay;
    private ProgressBar loadingProgressBar;
    private boolean isSubmitting = false;
    private long selectedEventTimestamp; // Store the full timestamp including time


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_pending_event);

        // Initialize Firebase components
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Retrieve the passed data
        amAccountId = getIntent().getStringExtra("amAccountId");
        Log.d("AddPendingEvent", "Received amAccountId: " + amAccountId);

        // Validate the amAccountId
        if (amAccountId == null) {
            Log.e("AddPendingEvent", "amAccountId is null. Ensure it is passed correctly.");
        } else {
            // Use amAccountId for your logic
            Log.d("AddPendingEvent", "Successfully retrieved amAccountId: " + amAccountId);
        }

        // Initialize Views
        inputImage = findViewById(R.id.inputImage);
        selectedImagesPreview = findViewById(R.id.selectedImagesPreview);

        // Initialize Image List and Adapter
        imageUris = new ArrayList<>();
        List<String> imageUriStrings = new ArrayList<>();
        imagesAdapter = new ImagesAdapter(this, imageUriStrings);
        selectedImagesPreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        selectedImagesPreview.setAdapter(imagesAdapter);;

        // Set up the image upload button
        inputImage.setOnClickListener(v -> openImageSelector());

        // Initialize fields
        inputNameOfEvent = findViewById(R.id.inputNameOfEvent);
        inputTypeOfEvent = findViewById(R.id.inputTypeOfEvent);
        inputOrganization = findViewById(R.id.inputOrganization);
        inputAddress = findViewById(R.id.inputAddress);
        inputDate = findViewById(R.id.inputDate);
        inputDate.setFocusable(false); // Prevent keyboard from showing up
        inputDate.setClickable(true);  // Ensure it's clickable
        inputDate.setOnClickListener(v -> {
            // Launch the EventDatePicker activity to select a date
            Intent intent = new Intent(AddPendingEvent.this, EventDatePicker.class);
            startActivityForResult(intent, REQUEST_CODE_DATE_PICKER);
        });

        inputHeadCoordinator = findViewById(R.id.inputHeadCoordinator);
        inputVolunteersPerBarangay = findViewById(R.id.inputVolunteersPerBarangay);
        inputCaption = findViewById(R.id.inputCaption);
        inputImage = findViewById(R.id.inputImage);

        // Set up the image upload button
        inputImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Allow multiple image selection
            startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGE_REQUEST);
        });

        // Initialize checkboxes
        checkboxCollaboration = findViewById(R.id.checkboxCollaboration);
        checkboxDedication = findViewById(R.id.checkboxDedication);
        checkboxPatience = findViewById(R.id.checkboxPatience);
        checkboxReliability = findViewById(R.id.checkboxReliability);
        checkboxParticipation = findViewById(R.id.checkboxParticipation);
        checkboxDecisionsMaking = findViewById(R.id.checkboxDecisionsMaking);
        checkboxCoaching = findViewById(R.id.checkboxCoaching);
        checkboxMentoring = findViewById(R.id.checkboxMentoring);
        checkboxPlanning = findViewById(R.id.checkboxPlanning);
        checkboxTraining = findViewById(R.id.checkboxTraining);
        checkboxCriticalThinking = findViewById(R.id.checkboxCriticalThinking);
        checkboxDataGathering = findViewById(R.id.checkboxDataGathering);
        checkboxDetermination = findViewById(R.id.checkboxDetermination);
        checkboxResearch = findViewById(R.id.checkboxResearch);
        checkboxFlexibility = findViewById(R.id.checkboxFlexibility);
        checkboxAdaptability = findViewById(R.id.checkboxAdaptability);
        checkboxConflictResolution = findViewById(R.id.checkboxConflictResolution);
        checkboxResilience = findViewById(R.id.checkboxResilience);
        checkboxEmotionalIntelligence = findViewById(R.id.checkboxEmotionalIntelligence);
        checkboxEmpathy = findViewById(R.id.checkboxEmpathy);
        checkboxBrainstorming = findViewById(R.id.checkboxBrainstorming);
        checkboxDesign = findViewById(R.id.checkboxDesign);
        checkboxInnovation = findViewById(R.id.checkboxInnovation);
        checkboxExplorationAndDiscovery = findViewById(R.id.checkboxExplorationAndDiscovery);
        checkboxVisualThinking = findViewById(R.id.checkboxVisualThinking);
        checkboxBenchmarking = findViewById(R.id.checkboxBenchmarking);
        checkboxMindfulness = findViewById(R.id.checkboxMindfulness);
        checkboxProcessAnalysis = findViewById(R.id.checkboxProcessAnalysis);
        checkboxScenarioPlanning = findViewById(R.id.checkboxScenarioPlanning);
        checkboxTroubleshooting = findViewById(R.id.checkboxTroubleshooting);

        // Hide all skill input fields by default
        hideAllSkillInputs();

        addEventButton = findViewById(R.id.addEvent);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        skillsFields = new ArrayList<>();

        addEventButton.setOnClickListener(v -> handleAddEvent());

        // Initialize all field borders
        initializeFieldBorders();
    }

    // Method to hide all skill volunteer input fields
    private void hideAllSkillInputs() {
        // Find all skill input EditTexts and hide them
        hideInputField("input_collaboration_volunteers");
        hideInputField("input_dedication_volunteers");
        hideInputField("input_patience_volunteers");
        hideInputField("input_reliability_volunteers");
        hideInputField("input_participation_volunteers");
        hideInputField("input_decisionmaking_volunteers");
        hideInputField("input_coaching_volunteers");
        hideInputField("input_mentoring_volunteers");
        hideInputField("input_planning_volunteers");
        hideInputField("input_training_volunteers");
        hideInputField("input_criticalthinking_volunteers");
        hideInputField("input_datagathering_volunteers");
        hideInputField("input_determination_volunteers");
        hideInputField("input_research_volunteers");
        hideInputField("input_flexibility_volunteers");
        hideInputField("input_adaptability_volunteers");
        hideInputField("input_conflictresolution_volunteers");
        hideInputField("input_resilience_volunteers");
        hideInputField("input_emotionalintelligence_volunteers");
        hideInputField("input_empathy_volunteers");
        hideInputField("input_brainstorming_volunteers");
        hideInputField("input_design_volunteers");
        hideInputField("input_innovation_volunteers");
        hideInputField("input_explorationanddiscovery_volunteers");
        hideInputField("input_visualthinking_volunteers");
        hideInputField("input_benchmarking_volunteers");
        hideInputField("input_mindfulness_volunteers");
        hideInputField("input_processanalysis_volunteers");
        hideInputField("input_scenarioplanning_volunteers");
        hideInputField("input_troubleshooting_volunteers");
    }

    // Helper method to hide a specific input field
    private void hideInputField(String fieldId) {
        int id = getResources().getIdentifier(fieldId, "id", getPackageName());
        if (id != 0) {
            EditText field = findViewById(id);
            if (field != null) {
                field.setVisibility(View.GONE);
            }
        }
    }

    // Method to initialize field borders with normal appearance
    private void initializeFieldBorders() {
        // Set normal borders to all input fields
        inputNameOfEvent.setBackground(getResources().getDrawable(R.drawable.normal_border));
        inputOrganization.setBackground(getResources().getDrawable(R.drawable.normal_border));
        inputDate.setBackground(getResources().getDrawable(R.drawable.normal_border));
        inputHeadCoordinator.setBackground(getResources().getDrawable(R.drawable.normal_border));
        inputVolunteersPerBarangay.setBackground(getResources().getDrawable(R.drawable.normal_border));
        inputCaption.setBackground(getResources().getDrawable(R.drawable.normal_border));

        // Set normal borders to spinners
        inputTypeOfEvent.setBackground(getResources().getDrawable(R.drawable.normal_border));
        inputAddress.setBackground(getResources().getDrawable(R.drawable.normal_border));
    }

    // Method to reset all field borders to normal
    private void resetFieldBorders() {
        inputNameOfEvent.setBackground(getResources().getDrawable(R.drawable.normal_border));
        inputOrganization.setBackground(getResources().getDrawable(R.drawable.normal_border));
        inputDate.setBackground(getResources().getDrawable(R.drawable.normal_border));
        inputHeadCoordinator.setBackground(getResources().getDrawable(R.drawable.normal_border));
        inputVolunteersPerBarangay.setBackground(getResources().getDrawable(R.drawable.normal_border));
        inputCaption.setBackground(getResources().getDrawable(R.drawable.normal_border));

        // Reset spinner borders
        inputTypeOfEvent.setBackground(getResources().getDrawable(R.drawable.normal_border));
        inputAddress.setBackground(getResources().getDrawable(R.drawable.normal_border));
    }

    private void openImageSelector() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Allow multiple image selection
        startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_IMAGE_REQUEST);
    }

    // Validate the form before starting submission process
    private boolean validateForm() {
        // Reset all field borders first
        resetFieldBorders();

        boolean hasErrors = false;

        String nameOfEvent = inputNameOfEvent.getText().toString().trim();
        String typeOfEvent = inputTypeOfEvent.getSelectedItem().toString().trim();
        String organization = inputOrganization.getText().toString().trim();
        String address = inputAddress.getSelectedItem().toString().trim();
        String dateString = inputDate.getText().toString().trim();
        String headCoordinator = inputHeadCoordinator.getText().toString().trim();
        String volunteersPerBarangayStr = inputVolunteersPerBarangay.getText().toString().trim();
        String caption = inputCaption.getText().toString().trim();

        // Check required fields one by one and set red borders where needed

        if (nameOfEvent.isEmpty()) {
            inputNameOfEvent.setBackground(getResources().getDrawable(R.drawable.event_red_border));
            hasErrors = true;
        }

        if (typeOfEvent.equals("Select Event Type")) {
            inputTypeOfEvent.setBackground(getResources().getDrawable(R.drawable.event_red_border));
            hasErrors = true;
        }

        if (organization.isEmpty()) {
            inputOrganization.setBackground(getResources().getDrawable(R.drawable.event_red_border));
            hasErrors = true;
        }

        if (address.equals("Select Barangay")) {
            inputAddress.setBackground(getResources().getDrawable(R.drawable.event_red_border));
            hasErrors = true;
        }

        if (dateString.isEmpty()) {
            inputDate.setBackground(getResources().getDrawable(R.drawable.event_red_border));
            hasErrors = true;
        }

        if (headCoordinator.isEmpty()) {
            inputHeadCoordinator.setBackground(getResources().getDrawable(R.drawable.event_red_border));
            hasErrors = true;
        }

        if (volunteersPerBarangayStr.isEmpty()) {
            inputVolunteersPerBarangay.setBackground(getResources().getDrawable(R.drawable.event_red_border));
            hasErrors = true;
        }

        if (caption.isEmpty()) {
            inputCaption.setBackground(getResources().getDrawable(R.drawable.event_red_border));
            hasErrors = true;
        }

        // Check if timestamp is set (date and time selected)
        if (selectedEventTimestamp == 0) {
            inputDate.setBackground(getResources().getDrawable(R.drawable.event_red_border));
            hasErrors = true;
        }

        // If form has errors, show a toast message
        if (hasErrors) {
            Toast.makeText(this, "Please fill in all required fields marked with *", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Validate selected skills - at least one skill must be selected
        if (!isAnySkillSelected()) {
            Toast.makeText(this, "Please select at least one skill.", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    // Check if at least one skill checkbox is selected
    private boolean isAnySkillSelected() {
        return checkboxCollaboration.isChecked() ||
                checkboxDedication.isChecked() ||
                checkboxPatience.isChecked() ||
                checkboxReliability.isChecked() ||
                checkboxParticipation.isChecked() ||
                checkboxDecisionsMaking.isChecked() ||
                checkboxCoaching.isChecked() ||
                checkboxMentoring.isChecked() ||
                checkboxPlanning.isChecked() ||
                checkboxTraining.isChecked() ||
                checkboxCriticalThinking.isChecked() ||
                checkboxDataGathering.isChecked() ||
                checkboxDetermination.isChecked() ||
                checkboxResearch.isChecked() ||
                checkboxFlexibility.isChecked() ||
                checkboxAdaptability.isChecked() ||
                checkboxConflictResolution.isChecked() ||
                checkboxResilience.isChecked() ||
                checkboxEmotionalIntelligence.isChecked() ||
                checkboxEmpathy.isChecked() ||
                checkboxBrainstorming.isChecked() ||
                checkboxDesign.isChecked() ||
                checkboxInnovation.isChecked() ||
                checkboxExplorationAndDiscovery.isChecked() ||
                checkboxVisualThinking.isChecked() ||
                checkboxBenchmarking.isChecked() ||
                checkboxMindfulness.isChecked() ||
                checkboxProcessAnalysis.isChecked() ||
                checkboxScenarioPlanning.isChecked() ||
                checkboxTroubleshooting.isChecked();
    }

    private void savePendingEvent(List<String> imageUrls) {
        String nameOfEvent = inputNameOfEvent.getText().toString().trim();
        String typeOfEvent = inputTypeOfEvent.getSelectedItem().toString().trim();
        String organization = inputOrganization.getText().toString().trim();
        String address = inputAddress.getSelectedItem().toString().trim();
        String headCoordinator = inputHeadCoordinator.getText().toString().trim();
        String caption = inputCaption.getText().toString().trim();
        String volunteersPerBarangayStr = inputVolunteersPerBarangay.getText().toString().trim();
        int volunteersPerBarangay = Integer.parseInt(volunteersPerBarangayStr);

        // Use the saved timestamp from date and time picker
        Date eventDate = new Date(selectedEventTimestamp);
        com.google.firebase.Timestamp timestamp = new com.google.firebase.Timestamp(eventDate);

        this.eventId = UUID.randomUUID().toString();

        // Get selected skills
        List<String> selectedSkills = getSelectedSkills();

        // Prepare EventInformation data
        Map<String, Object> eventInformation = new HashMap<>();
        eventInformation.put("eventId", eventId);
        eventInformation.put("amAccountId", amAccountId);
        eventInformation.put("headCoordinator", headCoordinator);
        eventInformation.put("organizations", organization);
        eventInformation.put("eventSkills", selectedSkills);
        eventInformation.put("status", "Pending");

        // Prepare EventDetails data
        Map<String, Object> eventDetails = new HashMap<>();
        eventDetails.put("eventId", eventId);
        eventDetails.put("nameOfEvent", nameOfEvent);
        eventDetails.put("typeOfEvent", typeOfEvent);
        eventDetails.put("date", timestamp);
        eventDetails.put("volunteerNeeded", volunteersPerBarangay); // Just use the total volunteers per barangay
        eventDetails.put("volunteersPerBarangay", volunteersPerBarangay);
        eventDetails.put("caption", caption);
        eventDetails.put("imageUrls", imageUrls != null ? imageUrls : new ArrayList<>());
        eventDetails.put("barangay", address);
        // Add empty map for skill requirements (for backward compatibility)
        eventDetails.put("skillRequirements", new HashMap<String, Integer>());

        saveToFirebase(eventInformation, eventDetails);
    }

    // Get a list of selected skills
    private List<String> getSelectedSkills() {
        List<String> skills = new ArrayList<>();

        if (checkboxCollaboration.isChecked()) skills.add("Collaboration");
        if (checkboxDedication.isChecked()) skills.add("Dedication");
        if (checkboxPatience.isChecked()) skills.add("Patience");
        if (checkboxReliability.isChecked()) skills.add("Reliability");
        if (checkboxParticipation.isChecked()) skills.add("Participation");
        if (checkboxDecisionsMaking.isChecked()) skills.add("Decision Making");
        if (checkboxCoaching.isChecked()) skills.add("Coaching");
        if (checkboxMentoring.isChecked()) skills.add("Mentoring");
        if (checkboxPlanning.isChecked()) skills.add("Planning");
        if (checkboxTraining.isChecked()) skills.add("Training");
        if (checkboxCriticalThinking.isChecked()) skills.add("Critical Thinking");
        if (checkboxDataGathering.isChecked()) skills.add("Data Gathering");
        if (checkboxDetermination.isChecked()) skills.add("Determination");
        if (checkboxResearch.isChecked()) skills.add("Research");
        if (checkboxFlexibility.isChecked()) skills.add("Flexibility");
        if (checkboxAdaptability.isChecked()) skills.add("Adaptability");
        if (checkboxConflictResolution.isChecked()) skills.add("Conflict Resolution");
        if (checkboxResilience.isChecked()) skills.add("Resilience");
        if (checkboxEmotionalIntelligence.isChecked()) skills.add("Emotional Intelligence");
        if (checkboxEmpathy.isChecked()) skills.add("Empathy");
        if (checkboxBrainstorming.isChecked()) skills.add("Brainstorming");
        if (checkboxDesign.isChecked()) skills.add("Design");
        if (checkboxInnovation.isChecked()) skills.add("Innovation");
        if (checkboxExplorationAndDiscovery.isChecked()) skills.add("Exploration and Discovery");
        if (checkboxVisualThinking.isChecked()) skills.add("Visual Thinking");
        if (checkboxBenchmarking.isChecked()) skills.add("Benchmarking");
        if (checkboxMindfulness.isChecked()) skills.add("Mindfulness");
        if (checkboxProcessAnalysis.isChecked()) skills.add("Process Analysis");
        if (checkboxScenarioPlanning.isChecked()) skills.add("Scenario Planning");
        if (checkboxTroubleshooting.isChecked()) skills.add("Troubleshooting");

        return skills;
    }

    // Modify the saveToFirebase method to reset button state after completion
    private void saveToFirebase(Map<String, Object> eventInformation, Map<String, Object> eventDetails) {
        db.collection("EventInformation").document(eventId).set(eventInformation)
                .addOnSuccessListener(aVoid -> {
                    db.collection("EventDetails").document(eventId).set(eventDetails)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(AddPendingEvent.this, "Event added successfully.", Toast.LENGTH_SHORT).show();
                                // No need to reset button state here as we're finishing the activity
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                resetButtonState(); // Reset on failure
                                Toast.makeText(AddPendingEvent.this, "Error saving EventDetails: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    resetButtonState(); // Reset on failure
                    Toast.makeText(AddPendingEvent.this, "Error saving EventInformation: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadImages(List<Uri> imageUris, OnCompleteListener<List<String>> onCompleteListener) {
        List<String> uploadedImageUrls = new ArrayList<>();
        int totalImages = imageUris.size();
        int[] uploadCount = {0};

        for (Uri uri : imageUris) {  // imageUris is a List<Uri> here
            StorageReference fileReference = storageReference.child("event_images/" + System.currentTimeMillis() + ".jpg");

            fileReference.putFile(uri)  // uri is of type Uri
                    .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                uploadedImageUrls.add(downloadUri.toString());  // Add download URL as String
                                uploadCount[0]++;
                                if (uploadCount[0] == totalImages) {
                                    // All images uploaded successfully
                                    onCompleteListener.onComplete(uploadedImageUrls);  // Return list of image URLs as Strings
                                }
                            }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error uploading image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        onCompleteListener.onFailure(e);  // Notify failure
                    });
        }
    }

    private void handleAddEvent() {
        // Prevent double-submission
        if (isSubmitting) {
            return;
        }

        // First validate the form before showing progress or starting submission
        if (!validateForm()) {
            // Validation failed, don't start progress bar or submission
            return;
        }

        // Set the submitting flag after validation succeeds
        isSubmitting = true;

        // Show loading state
        addEventButton.setText(""); // Clear button text
        addEventButton.setEnabled(false); // Disable button
        loadingProgressBar.setVisibility(View.VISIBLE);

        // Check if images are selected
        if (imageUris == null || imageUris.isEmpty()) {
            // If no images are selected, save event with null imageUrls
            savePendingEvent(null);
        } else {
            // Upload images and then save event
            uploadImages(imageUris, new OnCompleteListener<List<String>>() {
                @Override
                public void onComplete(List<String> uploadedImageUrls) {
                    // Hide loading indicator
                    savePendingEvent(uploadedImageUrls);
                }

                @Override
                public void onFailure(Exception e) {
                    // Hide loading indicator
                    resetButtonState();
                    Toast.makeText(AddPendingEvent.this,
                            "Error uploading images: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void resetButtonState() {
        runOnUiThread(() -> {
            isSubmitting = false;
            addEventButton.setText("Add Event");
            addEventButton.setEnabled(true);
            loadingProgressBar.setVisibility(View.GONE);
        });
    }

    public interface OnCompleteListener<T> {
        void onComplete(T result); // Called on success
        void onFailure(Exception e); // Called on failure
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            List<String> currentImageUrls = new ArrayList<>();
            imageUris = new ArrayList<>(); // Reset the image URIs list

            if (data.getClipData() != null) {
                // Multiple images selected
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    imageUris.add(imageUri); // Keep URI for upload
                    currentImageUrls.add(imageUri.toString()); // Convert to string for adapter
                }
            } else if (data.getData() != null) {
                // Single image selected
                Uri imageUri = data.getData();
                imageUris.add(imageUri); // Keep URI for upload
                currentImageUrls.add(imageUri.toString()); // Convert to string for adapter
            }

            // Update adapter with new string URLs
            imagesAdapter = new ImagesAdapter(this, currentImageUrls);
            selectedImagesPreview.setAdapter(imagesAdapter);
        }

        // Handle date picker result - now includes time information
        if (requestCode == REQUEST_CODE_DATE_PICKER && resultCode == RESULT_OK) {
            String selectedDate = data.getStringExtra("selectedDate");
            String selectedTime = data.getStringExtra("selectedTime");

            // Get the full timestamp from the picker
            selectedEventTimestamp = data.getLongExtra("timestamp", 0);

            if (selectedDate != null) {
                // Display the date and time in the input field
                if (selectedTime != null) {
                    inputDate.setText(selectedDate + " " + selectedTime);
                } else {
                    inputDate.setText(selectedDate);
                }

                Toast.makeText(this, "Selected Date and Time: " + selectedDate + " " + selectedTime, Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Custom ImagesAdapter for the selected images preview
    public class ImagesAdapter extends RecyclerView.Adapter<ImagesAdapter.ImageViewHolder> {
        private Context context;
        private List<String> imageUrls;
        private static final int MAX_IMAGES = 10;

        public ImagesAdapter(Context context, List<String> imageUrls) {
            this.context = context;
            // Limit the number of images to MAX_IMAGES
            this.imageUrls = imageUrls.size() <= MAX_IMAGES ?
                    new ArrayList<>(imageUrls) :
                    new ArrayList<>(imageUrls.subList(0, MAX_IMAGES));
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.image_item, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            String imageUrl = imageUrls.get(position);

            // Use Glide to load the image with error handling
            com.bumptech.glide.Glide.with(context)
                    .load(imageUrl)
                    .into(holder.imageView);

            // Set click listener to open fullscreen viewer
            holder.imageView.setOnClickListener(v -> openFullscreenViewer(position));
        }

        private void openFullscreenViewer(int position) {
            Intent intent = new Intent(context, FullscreenImageViewer.class);
            intent.putStringArrayListExtra("imageUrls", new ArrayList<>(imageUrls));
            intent.putExtra("position", position);
            context.startActivity(intent);
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
        }

        class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageView);
            }
        }
    }
}