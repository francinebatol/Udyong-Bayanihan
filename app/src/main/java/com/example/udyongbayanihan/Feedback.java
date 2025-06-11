package com.example.udyongbayanihan;

import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Feedback extends AppCompatActivity {

    // UI Elements
    private EditText roleEditText;
    private RadioGroup previousVolunteerRadioGroup;
    private Button submitFeedbackButton;
    private TextView welcomeMessageTextView;

    // Comments section
    private EditText improvementComments;
    private EditText willingnessComments;
    private EditText additionalComments;

    // Checkboxes for challenges
    private List<CheckBox> challengeCheckboxes = new ArrayList<>();

    // Rating buttons
    private HashMap<Integer, ImageButton[]> ratingButtons = new HashMap<>();
    private HashMap<Integer, Integer> selectedRatings = new HashMap<>();

    // Firebase
    private FirebaseFirestore db;
    private String userId = "";
    private String eventId = "";
    private String eventName = "";
    private String barangay = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feedback);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get intent extras
        if (getIntent().hasExtra("userId")) {
            userId = getIntent().getStringExtra("userId");
        }

        if (getIntent().hasExtra("eventId")) {
            eventId = getIntent().getStringExtra("eventId");
        }

        if (getIntent().hasExtra("eventName")) {
            eventName = getIntent().getStringExtra("eventName");
        }

        // Get barangay from intent extras
        if (getIntent().hasExtra("barangay")) {
            barangay = getIntent().getStringExtra("barangay");
        }

        // Initialize UI elements
        initializeUIElements();

        // Fetch event details if eventId is available but no event name or barangay
        if (!eventId.isEmpty() && (eventName.isEmpty() || barangay.isEmpty())) {
            fetchEventDetails();
        } else {
            // If we already have event name and barangay, just update welcome message
            updateWelcomeMessage();
        }

        // Setup window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup submit button click listener
        submitFeedbackButton.setOnClickListener(v -> submitFeedback());
    }

    private void fetchEventDetails() {
        // Show loading indication or progress bar if needed

        db.collection("EventDetails")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get event name
                        if (documentSnapshot.contains("nameOfEvent")) {
                            eventName = documentSnapshot.getString("nameOfEvent");
                        }

                        // Get barangay
                        if (documentSnapshot.contains("barangay")) {
                            barangay = documentSnapshot.getString("barangay");
                        }

                        // Update welcome message with fetched data
                        updateWelcomeMessage();
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle error
                    Toast.makeText(Feedback.this, "Failed to fetch event details", Toast.LENGTH_SHORT).show();
                    updateWelcomeMessage(); // Still update with whatever data we have
                });
    }

    private void updateWelcomeMessage() {
        if (welcomeMessageTextView != null) {
            // Create custom message with bold event name and barangay
            String message;

            if (!barangay.isEmpty()) {
                message = "Thank you for participating in the event <b>" + eventName + "</b> held at barangay <b>" + barangay + "</b>. " +
                        "We would like to know your experiences with volunteering. Please spare some time to give us valuable feedback to improve our management of the event.";
            } else {
                message = "Thank you for participating in the event <b>" + eventName + "</b>. " +
                        "We would like to know your experiences with volunteering. Please spare some time to give us valuable feedback to improve our management of the event.";
            }

            // Set the formatted HTML text
            Spanned formattedText = Html.fromHtml(message, Html.FROM_HTML_MODE_COMPACT);
            welcomeMessageTextView.setText(formattedText);
        }
    }

    private void initializeUIElements() {
        // Initialize welcome message TextView
        welcomeMessageTextView = findViewById(R.id.welcomeMessageTextView);

        // Initialize text inputs
        roleEditText = findViewById(R.id.roleEditText);
        improvementComments = findViewById(R.id.inputCommentsImprovement);
        willingnessComments = findViewById(R.id.inputCommentsWillingness);
        additionalComments = findViewById(R.id.inputCommentsSuggestions);

        // Initialize radio group
        previousVolunteerRadioGroup = findViewById(R.id.radioGroup);

        // Initialize submit button
        submitFeedbackButton = findViewById(R.id.submitFeedbackButton);

        int[] checkboxIds = {
                R.id.challengeCheckbox1,
                R.id.challengeCheckbox2,
                R.id.challengeCheckbox3,
                R.id.challengeCheckbox4,
        };

        for (int id : checkboxIds) {
            CheckBox checkBox = findViewById(id);
            if (checkBox != null) {
                challengeCheckboxes.add(checkBox);
            }
        }

        // Initialize rating buttons
        initializeRatingButtons();
    }

    private void initializeRatingButtons() {
        // Question 1: How well-organized was the event? (5 options)
        ratingButtons.put(1, new ImageButton[]{
                findViewById(R.id.imgbtnExp1Option1),
                findViewById(R.id.imgbtnExp1Option2),
                findViewById(R.id.imgbtnExp1Option3),
                findViewById(R.id.imgbtnExp1Option4),
                findViewById(R.id.imgbtnExp1Option5)
        });

        // Question 2: Was your role and responsibility clearly explained? (5 options)
        ratingButtons.put(2, new ImageButton[]{
                findViewById(R.id.imgbtnExp2Option1),
                findViewById(R.id.imgbtnExp2Option2),
                findViewById(R.id.imgbtnExp2Option3),
                findViewById(R.id.imgbtnExp2Option4),
                findViewById(R.id.imgbtnExp2Option5)
        });

        // Question 3: Were materials and resources adequate? (3 options)
        ratingButtons.put(3, new ImageButton[]{
                findViewById(R.id.imgbtnExp3Option1),
                findViewById(R.id.imgbtnExp3Option2),
                findViewById(R.id.imgbtnExp3Option3)
        });

        // Question 4: How satisfied are you with your volunteer experience? (5 options)
        ratingButtons.put(4, new ImageButton[]{
                findViewById(R.id.imgbtnExp4Option1),
                findViewById(R.id.imgbtnExp4Option2),
                findViewById(R.id.imgbtnExp4Option3),
                findViewById(R.id.imgbtnExp4Option4),
                findViewById(R.id.imgbtnExp4Option5)
        });

        // Question 5: Did community members actively participate? (5 options)
        ratingButtons.put(5, new ImageButton[]{
                findViewById(R.id.imgbtnExp5Option1),
                findViewById(R.id.imgbtnExp5Option2),
                findViewById(R.id.imgbtnExp5Option3),
                findViewById(R.id.imgbtnExp5Option4),
                findViewById(R.id.imgbtnExp5Option5)
        });

        // Question 6: Did you feel your contributions were valued? (5 options)
        ratingButtons.put(6, new ImageButton[]{
                findViewById(R.id.imgbtnExp6Option1),
                findViewById(R.id.imgbtnExp6Option2),
                findViewById(R.id.imgbtnExp6Option3),
                findViewById(R.id.imgbtnExp6Option4),
                findViewById(R.id.imgbtnExp6Option5)
        });

        // Question 7: Do you think the event was beneficial to the community? (5 options)
        ratingButtons.put(7, new ImageButton[]{
                findViewById(R.id.imgbtnExp7Option1),
                findViewById(R.id.imgbtnExp7Option2),
                findViewById(R.id.imgbtnExp7Option3),
                findViewById(R.id.imgbtnExp7Option4),
                findViewById(R.id.imgbtnExp7Option5)
        });

        // Setup click listeners for all rating buttons
        for (int questionNumber : ratingButtons.keySet()) {
            ImageButton[] buttons = ratingButtons.get(questionNumber);
            if (buttons != null) {
                for (int i = 0; i < buttons.length; i++) {
                    final int rating = i;
                    if (buttons[i] != null) {
                        buttons[i].setOnClickListener(v -> selectRating(questionNumber, rating));
                    }
                }
            }
        }
    }

    private void selectRating(int questionNumber, int ratingIndex) {
        ImageButton[] buttons = ratingButtons.get(questionNumber);
        if (buttons == null) return;

        // Reset all buttons for this question
        for (ImageButton button : buttons) {
            if (button != null) {
                button.setImageResource(R.drawable.rate);
            }
        }

        // Set selected button
        buttons[ratingIndex].setImageResource(R.drawable.rated);
        selectedRatings.put(questionNumber, ratingIndex);
    }

    private void submitFeedback() {
        // Validate form
        if (!validateForm()) {
            return;
        }

        // Collect form data
        Map<String, Object> feedbackData = collectFormData();

        // Store in Firestore
        db.collection("Feedback")
                .document(userId + "_" + eventId)
                .set(feedbackData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Feedback.this, "Feedback submitted successfully", Toast.LENGTH_SHORT).show();
                    // Set result to OK to inform Home activity that feedback was submitted
                    setResult(RESULT_OK);
                    finish(); // Close the activity
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Feedback.this, "Error submitting feedback: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateForm() {
        // Check if role is entered
        if (roleEditText.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter your role in the event", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check if a previous volunteer option is selected
        if (previousVolunteerRadioGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please indicate if you have volunteered before", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check if all rating questions are answered
        // We need at least the first 4 core rating questions answered
        int[] mandatoryQuestions = {1, 2, 3, 4};
        for (int q : mandatoryQuestions) {
            if (!selectedRatings.containsKey(q)) {
                Toast.makeText(this, "Please answer all rating questions", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return true;
    }

    private Map<String, Object> collectFormData() {
        Map<String, Object> feedbackData = new HashMap<>();

        // Basic information
        feedbackData.put("userId", userId);
        feedbackData.put("eventId", eventId);
        feedbackData.put("eventName", eventName);
        feedbackData.put("barangay", barangay);
        feedbackData.put("timestamp", Timestamp.now());

        // Role information
        feedbackData.put("role", roleEditText.getText().toString().trim());

        // Previous volunteer experience
        int selectedId = previousVolunteerRadioGroup.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedRadioButton = findViewById(selectedId);
            if (selectedRadioButton != null) {
                feedbackData.put("previousVolunteer", selectedRadioButton.getText().toString());
            }
        }

        // Challenges faced
        List<String> challenges = new ArrayList<>();
        for (CheckBox checkBox : challengeCheckboxes) {
            if (checkBox != null && checkBox.isChecked()) {
                challenges.add(checkBox.getText().toString());
            }
        }
        feedbackData.put("challenges", challenges);

        // Comments
        Map<String, String> comments = new HashMap<>();
        comments.put("improvements", improvementComments.getText().toString().trim());
        comments.put("willingness", willingnessComments.getText().toString().trim());
        comments.put("additional", additionalComments.getText().toString().trim());
        feedbackData.put("comments", comments);

        // Ratings - convert to meaningful strings for admin view
        Map<String, String> ratings = new HashMap<>();

        // Map for question 1 (organization)
        String[] q1Ratings = {"Very organized", "Somewhat organized", "Neutral", "Somewhat disorganized", "Very disorganized"};

        // Map for question 2 (role clarity)
        String[] q2Ratings = {"Yes, very clear", "Somewhat clear", "Neutral", "Somewhat unclear", "Not at all"};

        // Map for question 3 (resources)
        String[] q3Ratings = {"Yes", "No, some were lacking", "No, most were lacking"};

        // Map for question 4 (satisfaction)
        String[] q4Ratings = {"Very satisfied", "Satisfied", "Neutral", "Dissatisfied", "Very dissatisfied"};

        // Map for question 5 (community participation)
        String[] q5Ratings = {"Yes, most were engaged", "Some participated", "Neutral", "Few participated", "No engagement"};

        // Map for question 6 (valued contributions)
        String[] q6Ratings = {"Yes, very much", "Somewhat", "Neutral", "Not really", "Not at all"};

        // Map for question 7 (community benefit)
        String[] q7Ratings = {"Yes, very much", "Somewhat", "Neutral", "Not really", "Not at all"};

        // Add all ratings to the map
        if (selectedRatings.containsKey(1) && selectedRatings.get(1) < q1Ratings.length) {
            ratings.put("organization", q1Ratings[selectedRatings.get(1)]);
        }

        if (selectedRatings.containsKey(2) && selectedRatings.get(2) < q2Ratings.length) {
            ratings.put("roleClarity", q2Ratings[selectedRatings.get(2)]);
        }

        if (selectedRatings.containsKey(3) && selectedRatings.get(3) < q3Ratings.length) {
            ratings.put("resources", q3Ratings[selectedRatings.get(3)]);
        }

        if (selectedRatings.containsKey(4) && selectedRatings.get(4) < q4Ratings.length) {
            ratings.put("satisfaction", q4Ratings[selectedRatings.get(4)]);
        }

        if (selectedRatings.containsKey(5) && selectedRatings.get(5) < q5Ratings.length) {
            ratings.put("communityParticipation", q5Ratings[selectedRatings.get(5)]);
        }

        if (selectedRatings.containsKey(6) && selectedRatings.get(6) < q6Ratings.length) {
            ratings.put("valuedContributions", q6Ratings[selectedRatings.get(6)]);
        }

        if (selectedRatings.containsKey(7) && selectedRatings.get(7) < q7Ratings.length) {
            ratings.put("communityBenefit", q7Ratings[selectedRatings.get(7)]);
        }

        // Also store as question_# format with expanded rating scale for admin view compatibility
        for (Map.Entry<Integer, Integer> entry : selectedRatings.entrySet()) {
            int questionNumber = entry.getKey();
            int value = entry.getValue();
            String rating;

            // For questions with 5 options (1, 2, 4, 5, 6, 7)
            if (questionNumber == 1 || questionNumber == 2 || questionNumber == 4 ||
                    questionNumber == 5 || questionNumber == 6 || questionNumber == 7) {

                if (value == 0) {
                    rating = "Very Excellent";
                } else if (value == 1) {
                    rating = "Excellent";
                } else if (value == 2) {
                    rating = "Fair";
                } else if (value == 3) {
                    rating = "Poor";
                } else {
                    rating = "Very Poor";
                }
            }
            // For question with 3 options (question 3)
            else {
                if (value == 0) {
                    rating = "Very Excellent";
                } else if (value == 1) {
                    rating = "Fair";
                } else {
                    rating = "Poor";
                }
            }

            ratings.put("question_" + questionNumber, rating);
        }

        feedbackData.put("ratings", ratings);

        return feedbackData;
    }
}