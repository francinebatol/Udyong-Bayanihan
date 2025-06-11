package com.example.udyongbayanihan;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminEventsFeedbacks extends AppCompatActivity {
    private FirebaseFirestore db;
    private String eventId;
    private String eventName;
    private TextView txtEventName;
    private LinearLayout ratingContainer;
    private RecyclerView commentsRecyclerView;
    private RecyclerView usersRecyclerView;
    private List<FeedbackCommentModel> commentsList;
    private List<UserFeedbackModel> usersList;
    private FeedbackCommentsAdapter commentsAdapter;
    private UserFeedbackAdapter userFeedbackAdapter;
    private LayoutInflater inflater;

    // For tracking volunteer challenges
    private Map<String, Integer> challengesCount = new HashMap<>();

    // Success rating components
    private View overallRatingView; // Added to store the overall rating view
    private boolean overallRatingDisplayed = false; // Flag to track if overall rating has been displayed
    private int totalUsersFetched = 0; // Counter for total users fetched
    private int totalUsers = 0; // Total number of users to be fetched

    // Updated question texts to match the new UI
    private final String[] QUESTIONS = {
            "1. How well-organized was the event?",
            "2. Was your role and responsibility as a volunteer clearly explained?",
            "3. Were the materials and resources provided adequate for your tasks?",
            "4. How satisfied are you with your volunteer experience?",
            "5. Did community members actively participate in the project?",
            "6. Did you feel that your contributions were valued?",
            "7. Do you think the event was beneficial to the barangay community?"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_events_feedbacks);

        inflater = LayoutInflater.from(this);
        db = FirebaseFirestore.getInstance();

        eventId = getIntent().getStringExtra("eventId");
        eventName = getIntent().getStringExtra("eventName");

        txtEventName = findViewById(R.id.txtEventName);
        ratingContainer = findViewById(R.id.ratingContainer);
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        usersRecyclerView = findViewById(R.id.usersRecyclerView);

        txtEventName.setText(eventName);

        // Setup back button listener

        // Initialize comments list and adapter
        commentsList = new ArrayList<>();
        commentsAdapter = new FeedbackCommentsAdapter(commentsList);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsRecyclerView.setAdapter(commentsAdapter);

        // Initialize users list and adapter
        usersList = new ArrayList<>();
        userFeedbackAdapter = new UserFeedbackAdapter(usersList);

        ViewGroup usersRecyclerViewParent = (ViewGroup) usersRecyclerView.getParent();
        View satisfactionLegend = userFeedbackAdapter.createSatisfactionLegendView(usersRecyclerViewParent);

        // Find the index where usersRecyclerView is located in its parent
        int recyclerViewIndex = usersRecyclerViewParent.indexOfChild(usersRecyclerView);

        // Insert the legend just before the RecyclerView
        usersRecyclerViewParent.addView(satisfactionLegend, recyclerViewIndex);

        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        usersRecyclerView.setAdapter(userFeedbackAdapter);

        fetchFeedbackData();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void fetchFeedbackData() {
        db.collection("Feedback")
                .whereEqualTo("eventId", eventId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalResponses = queryDocumentSnapshots.size();

                    // Set total users to be fetched
                    totalUsers = totalResponses;

                    // Reset counters and flags
                    totalUsersFetched = 0;
                    overallRatingDisplayed = false;

                    // Maps for each question with each question's specific options
                    Map<Integer, Map<String, Integer>> questionCounts = new HashMap<>();

                    // Initialize counters for each question with their specific options
                    // Question 1: How well-organized was the event? (5 options)
                    Map<String, Integer> q1Counts = new HashMap<>();
                    q1Counts.put("Very organized", 0);
                    q1Counts.put("Somewhat organized", 0);
                    q1Counts.put("Neutral", 0);
                    q1Counts.put("Somewhat disorganized", 0);
                    q1Counts.put("Very disorganized", 0);
                    questionCounts.put(1, q1Counts);

                    // Question 2: Was your role and responsibility clearly explained? (5 options)
                    Map<String, Integer> q2Counts = new HashMap<>();
                    q2Counts.put("Yes, very clear", 0);
                    q2Counts.put("Somewhat clear", 0);
                    q2Counts.put("Neutral", 0);
                    q2Counts.put("Somewhat unclear", 0);
                    q2Counts.put("Not at all", 0);
                    questionCounts.put(2, q2Counts);

                    // Question 3: Were materials and resources adequate? (3 options)
                    Map<String, Integer> q3Counts = new HashMap<>();
                    q3Counts.put("Yes", 0);
                    q3Counts.put("No, some were lacking", 0);
                    q3Counts.put("No, most were lacking", 0);
                    questionCounts.put(3, q3Counts);

                    // Question 4: How satisfied are you with your volunteer experience? (5 options)
                    Map<String, Integer> q4Counts = new HashMap<>();
                    q4Counts.put("Very satisfied", 0);
                    q4Counts.put("Satisfied", 0);
                    q4Counts.put("Neutral", 0);
                    q4Counts.put("Dissatisfied", 0);
                    q4Counts.put("Very dissatisfied", 0);
                    questionCounts.put(4, q4Counts);

                    // Question 5: Did community members actively participate? (5 options)
                    Map<String, Integer> q5Counts = new HashMap<>();
                    q5Counts.put("Yes, most were engaged", 0);
                    q5Counts.put("Some participated", 0);
                    q5Counts.put("Neutral", 0);
                    q5Counts.put("Few participated", 0);
                    q5Counts.put("No engagement", 0);
                    questionCounts.put(5, q5Counts);

                    // Question 6: Did you feel your contributions were valued? (5 options)
                    Map<String, Integer> q6Counts = new HashMap<>();
                    q6Counts.put("Yes, very much", 0);
                    q6Counts.put("Somewhat", 0);
                    q6Counts.put("Neutral", 0);
                    q6Counts.put("Not really", 0);
                    q6Counts.put("Not at all", 0);
                    questionCounts.put(6, q6Counts);

                    // Question 7: Do you think the event was beneficial to the community? (5 options)
                    Map<String, Integer> q7Counts = new HashMap<>();
                    q7Counts.put("Yes, very much", 0);
                    q7Counts.put("Somewhat", 0);
                    q7Counts.put("Neutral", 0);
                    q7Counts.put("Not really", 0);
                    q7Counts.put("Not at all", 0);
                    questionCounts.put(7, q7Counts);

                    // Reset challenges data
                    challengesCount.clear();

                    // Clear comments and users lists
                    commentsList.clear();
                    usersList.clear();

                    // Process each feedback document
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Get user ID to fetch user details
                        String userId = document.getString("userId");

                        if (userId != null) {
                            fetchUserData(userId, document);
                        } else {
                            // If no user ID, increment counter
                            totalUsersFetched++;
                        }

                        // Get ratings
                        Map<String, String> ratings = (Map<String, String>) document.get("ratings");
                        if (ratings != null) {
                            // Process organization rating (Q1)
                            String organization = ratings.get("organization");
                            if (organization != null && questionCounts.get(1).containsKey(organization)) {
                                questionCounts.get(1).put(organization, questionCounts.get(1).get(organization) + 1);
                            }

                            // Process role clarity rating (Q2)
                            String roleClarity = ratings.get("roleClarity");
                            if (roleClarity != null && questionCounts.get(2).containsKey(roleClarity)) {
                                questionCounts.get(2).put(roleClarity, questionCounts.get(2).get(roleClarity) + 1);
                            }

                            // Process resources rating (Q3)
                            String resources = ratings.get("resources");
                            if (resources != null && questionCounts.get(3).containsKey(resources)) {
                                questionCounts.get(3).put(resources, questionCounts.get(3).get(resources) + 1);
                            }

                            // Process satisfaction rating (Q4)
                            String satisfaction = ratings.get("satisfaction");
                            if (satisfaction != null && questionCounts.get(4).containsKey(satisfaction)) {
                                questionCounts.get(4).put(satisfaction, questionCounts.get(4).get(satisfaction) + 1);
                            }

                            // Process community participation (Q5)
                            String communityParticipation = ratings.get("communityParticipation");
                            if (communityParticipation != null && questionCounts.get(5).containsKey(communityParticipation)) {
                                questionCounts.get(5).put(communityParticipation, questionCounts.get(5).get(communityParticipation) + 1);
                            }

                            // Process valued contributions (Q6)
                            String valuedContributions = ratings.get("valuedContributions");
                            if (valuedContributions != null && questionCounts.get(6).containsKey(valuedContributions)) {
                                questionCounts.get(6).put(valuedContributions, questionCounts.get(6).get(valuedContributions) + 1);
                            }

                            // Process community benefit (Q7)
                            String communityBenefit = ratings.get("communityBenefit");
                            if (communityBenefit != null && questionCounts.get(7).containsKey(communityBenefit)) {
                                questionCounts.get(7).put(communityBenefit, questionCounts.get(7).get(communityBenefit) + 1);
                            }
                        }

                        // Track challenges faced by volunteers
                        List<String> challenges = (List<String>) document.get("challenges");
                        if (challenges != null && !challenges.isEmpty()) {
                            for (String challenge : challenges) {
                                challengesCount.put(challenge, challengesCount.getOrDefault(challenge, 0) + 1);
                            }
                        }

                        // Get and process all comments
                        Map<String, String> comments = (Map<String, String>) document.get("comments");
                        if (comments != null) {
                            // Get user information to temporarily identify the commenter
                            String tempIdentifier = "User";
                            String role = document.getString("role");
                            if (role != null && !role.isEmpty()) {
                                tempIdentifier += " (" + role + ")";
                            }

                            // Store the userId for later name lookup
                            String commentUserId = document.getString("userId");

                            // Process improvements comments
                            String improvementsComment = comments.get("improvements");
                            if (improvementsComment != null && !improvementsComment.trim().isEmpty()) {
                                commentsList.add(new FeedbackCommentModel(
                                        "What aspects of the event could be improved?",
                                        improvementsComment,
                                        tempIdentifier,
                                        commentUserId  // Store the userId with the comment
                                ));
                            }

                            // Process willingness comments
                            String willingnessComment = comments.get("willingness");
                            if (willingnessComment != null && !willingnessComment.trim().isEmpty()) {
                                commentsList.add(new FeedbackCommentModel(
                                        "Would you be willing to volunteer again in the future events? Why or why not?",
                                        willingnessComment,
                                        tempIdentifier,
                                        commentUserId  // Store the userId with the comment
                                ));
                            }

                            // Process additional comments
                            String additionalComment = comments.get("additional");
                            if (additionalComment != null && !additionalComment.trim().isEmpty()) {
                                commentsList.add(new FeedbackCommentModel(
                                        "Any additional comments or suggestions?",
                                        additionalComment,
                                        tempIdentifier,
                                        commentUserId  // Store the userId with the comment
                                ));
                            }
                        }
                    }

                    // Display results
                    displayResults(questionCounts, totalResponses);

                    // Display challenge statistics
                    if (!challengesCount.isEmpty()) {
                        displayChallenges(totalResponses);
                    }

                    // Notify adapters
                    commentsAdapter.notifyDataSetChanged();
                    userFeedbackAdapter.notifyDataSetChanged();

                    // Only calculate and display overall rating if no user data to fetch
                    if (totalUsers == 0) {
                        calculateAndDisplayOverallRating();
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle any errors
                });
    }

    /**
     * Calculate and display the overall event success rating
     * This method will be called once after all user data has been fetched
     */
    private void calculateAndDisplayOverallRating() {
        if (usersList.isEmpty() || overallRatingDisplayed) {
            return; // No data to calculate or already displayed
        }

        float totalScore = 0f;
        int userCount = 0;

        // Sum up satisfaction scores from all users
        for (UserFeedbackModel user : usersList) {
            float userScore = user.calculateSatisfactionScore();
            if (userScore > 0) {
                totalScore += userScore;
                userCount++;
            }
        }

        // Calculate overall rating (0-5 scale)
        float starRating = userCount > 0 ? totalScore / userCount : 0f;

        // Calculate percentage (0-100)
        float percentage = (starRating / 5f) * 100f;

        // Create and inflate the overall rating view if not already created
        if (overallRatingView == null) {
            overallRatingView = inflater.inflate(R.layout.overall_event_rating, null);
        }

        // Find rating components in the view
        TextView ratingText = overallRatingView.findViewById(R.id.overallRatingText);
        TextView ratingPercentage = overallRatingView.findViewById(R.id.overallRatingPercentage);
        RatingBar ratingStars = overallRatingView.findViewById(R.id.overallRatingStars);

        // Set rating values
        ratingStars.setRating(starRating);
        ratingPercentage.setText(String.format("%.1f%%", percentage));

        // Set rating description and color
        String ratingDescription;
        int color;

        if (percentage >= 90) {
            ratingDescription = "Excellent";
            color = getResources().getColor(R.color.positive_green, null);
        } else if (percentage >= 80) {
            ratingDescription = "Very Good";
            color = getResources().getColor(R.color.positive_green, null);
        } else if (percentage >= 70) {
            ratingDescription = "Good";
            color = getResources().getColor(R.color.light_green, null);
        } else if (percentage >= 60) {
            ratingDescription = "Satisfactory";
            color = getResources().getColor(R.color.light_green, null);
        } else if (percentage >= 50) {
            ratingDescription = "Fair";
            color = getResources().getColor(R.color.neutral_yellow, null);
        } else {
            ratingDescription = "Needs Improvement";
            color = getResources().getColor(R.color.light_red, null);
        }

        ratingText.setText(ratingDescription);
        ratingText.setTextColor(color);
        ratingPercentage.setTextColor(color);

        // Remove existing overall rating view if it exists in the container
        if (ratingContainer.indexOfChild(overallRatingView) != -1) {
            ratingContainer.removeView(overallRatingView);
        }

        // Add to the beginning of the rating container
        ratingContainer.addView(overallRatingView, 0);

        // Mark that overall rating has been displayed
        overallRatingDisplayed = true;
    }

    private void fetchUserData(String userId, QueryDocumentSnapshot feedbackDocument) {
        // First get the user's name
        db.collection("usersName")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(nameDocuments -> {
                    String firstName = "";
                    String lastName = "";

                    if (!nameDocuments.isEmpty()) {
                        QueryDocumentSnapshot nameDoc = (QueryDocumentSnapshot) nameDocuments.getDocuments().get(0);
                        firstName = nameDoc.getString("firstName") != null ? nameDoc.getString("firstName") : "";
                        lastName = nameDoc.getString("lastName") != null ? nameDoc.getString("lastName") : "";
                    }

                    final String fullName = firstName + " " + lastName;

                    // Update any comments from this user with their real name
                    updateCommentsWithUserName(userId, fullName.trim(), feedbackDocument.getString("role"));

                    // Now get user's age and profile picture
                    db.collection("usersOtherDetails")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(detailsDocuments -> {
                                String age = "N/A";
                                String profilePictureUrl = null; // Initialize profile picture URL

                                if (!detailsDocuments.isEmpty()) {
                                    QueryDocumentSnapshot detailsDoc = (QueryDocumentSnapshot) detailsDocuments.getDocuments().get(0);
                                    Number ageNumber = detailsDoc.getLong("age");
                                    if (ageNumber != null) {
                                        age = String.valueOf(ageNumber);
                                    }

                                    // Get the profile picture URL
                                    profilePictureUrl = detailsDoc.getString("profilePictureUrl");
                                }

                                // Get rating data and role from the feedback document
                                Map<String, String> ratings = (Map<String, String>) feedbackDocument.get("ratings");
                                String role = feedbackDocument.getString("role");

                                // Create user feedback model with the profile picture URL
                                UserFeedbackModel userModel = new UserFeedbackModel(
                                        userId,
                                        !fullName.trim().isEmpty() ? fullName : "Unknown User",
                                        age,
                                        role != null ? role : "Volunteer",
                                        ratings,
                                        profilePictureUrl  // Add profile picture URL
                                );

                                usersList.add(userModel);
                                userFeedbackAdapter.notifyDataSetChanged();

                                // Increment counter for users fetched
                                totalUsersFetched++;

                                // Calculate and display overall rating only when all users have been fetched
                                if (totalUsersFetched >= totalUsers && !overallRatingDisplayed) {
                                    calculateAndDisplayOverallRating();
                                }
                            })
                            .addOnFailureListener(e -> {
                                // Handle error and update comments anyway with the partial name we have
                                updateCommentsWithUserName(userId, !fullName.trim().isEmpty() ? fullName : "Unknown User",
                                        feedbackDocument.getString("role"));

                                // Increment counter even on failure
                                totalUsersFetched++;

                                // Handle error fetching user name
                                // Still try to create a user entry with available information
                                String role = feedbackDocument.getString("role");
                                Map<String, String> ratings = (Map<String, String>) feedbackDocument.get("ratings");

                                UserFeedbackModel userModel = new UserFeedbackModel(
                                        userId,
                                        "Unknown User",
                                        "N/A",
                                        role != null ? role : "Volunteer",
                                        ratings,
                                        null  // No profile picture available
                                );

                                usersList.add(userModel);
                                userFeedbackAdapter.notifyDataSetChanged();

                                // Calculate and display overall rating only when all users have been fetched
                                if (totalUsersFetched >= totalUsers && !overallRatingDisplayed) {
                                    calculateAndDisplayOverallRating();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    // Update comments with a generic name since we failed to get the real one
                    updateCommentsWithUserName(userId, "Unknown User", feedbackDocument.getString("role"));

                    // Increment counter even on failure
                    totalUsersFetched++;

                    // Handle error fetching user name
                    // Still try to create a user entry with available information
                    String role = feedbackDocument.getString("role");
                    Map<String, String> ratings = (Map<String, String>) feedbackDocument.get("ratings");

                    UserFeedbackModel userModel = new UserFeedbackModel(
                            userId,
                            "Unknown User",
                            "N/A",
                            role != null ? role : "Volunteer",
                            ratings,
                            null  // No profile picture available
                    );

                    usersList.add(userModel);
                    userFeedbackAdapter.notifyDataSetChanged();

                    // Calculate and display overall rating only when all users have been fetched
                    if (totalUsersFetched >= totalUsers && !overallRatingDisplayed) {
                        calculateAndDisplayOverallRating();
                    }
                });
    }

    /**
     * Update all comments from a specific user with their actual name
     */
    private void updateCommentsWithUserName(String userId, String userName, String role) {
        // Skip if name is empty
        if (userId == null || userName == null || userName.isEmpty()) {
            return;
        }

        // Add role if available
        String displayName = userName;
        if (role != null && !role.isEmpty()) {
            displayName += " (" + role + ")";
        }

        // Update all comments that match this user ID
        for (FeedbackCommentModel comment : commentsList) {
            if (userId.equals(comment.getUserId())) {
                comment.setUserIdentifier(displayName);
            }
        }

        // Notify adapter of changes
        if (commentsAdapter != null) {
            commentsAdapter.notifyDataSetChanged();
        }
    }

    private void displayResults(Map<Integer, Map<String, Integer>> questionCounts, int totalResponses) {
        ratingContainer.removeAllViews();

        // Add a title for all feedback responses
        TextView responseHeader = new TextView(this);
        responseHeader.setText("Feedback Responses: " + totalResponses + " volunteers");
        responseHeader.setTextSize(18);
        responseHeader.setTextColor(getResources().getColor(R.color.dark_green, null));
        responseHeader.setPadding(16, 20, 16, 20);
        ratingContainer.addView(responseHeader);

        // Add section header for Event Organization
        TextView organizationHeader = new TextView(this);
        organizationHeader.setText("Event Organization and Implementation");
        organizationHeader.setTextSize(18);
        organizationHeader.setTextColor(getResources().getColor(R.color.dark_green, null));
        organizationHeader.setPadding(16, 40, 16, 16);
        ratingContainer.addView(organizationHeader);

        // Display organization questions (1-3)
        for (int i = 1; i <= 3; i++) {
            displayQuestionRating(i, questionCounts.get(i), totalResponses);
        }

        // Add section header for Volunteer Experience
        TextView experienceHeader = new TextView(this);
        experienceHeader.setText("Volunteer Experience");
        experienceHeader.setTextSize(18);
        experienceHeader.setTextColor(getResources().getColor(R.color.dark_green, null));
        experienceHeader.setPadding(16, 40, 16, 16);
        ratingContainer.addView(experienceHeader);

        // Display volunteer experience questions (4-7)
        for (int i = 4; i <= 7; i++) {
            displayQuestionRating(i, questionCounts.get(i), totalResponses);
        }
    }

    private void displayQuestionRating(int questionNumber, Map<String, Integer> optionCounts, int totalResponses) {
        if (optionCounts != null) {
            View ratingView = inflater.inflate(R.layout.rating_bar_item_multibar, ratingContainer, false);
            TextView questionTitle = ratingView.findViewById(R.id.questionTitle);
            LinearLayout barsContainer = ratingView.findViewById(R.id.barsContainer);

            // Use our updated question texts array
            if (questionNumber <= QUESTIONS.length) {
                questionTitle.setText(QUESTIONS[questionNumber-1]);
            } else {
                questionTitle.setText("Question " + questionNumber);
            }

            // Calculate the max value for this question (to scale the bars)
            int maxValue = 0;
            for (int count : optionCounts.values()) {
                if (count > maxValue) maxValue = count;
            }

            // Create a bar for each option
            for (Map.Entry<String, Integer> option : optionCounts.entrySet()) {
                View optionView = inflater.inflate(R.layout.item_rating_option, barsContainer, false);

                TextView optionLabel = optionView.findViewById(R.id.optionLabel);
                View optionBar = optionView.findViewById(R.id.optionBar);
                TextView optionCount = optionView.findViewById(R.id.optionCount);

                String optionName = option.getKey();
                int count = option.getValue();
                float percentage = totalResponses > 0 ? (count * 100f / totalResponses) : 0;

                optionLabel.setText(optionName);

                // Set the bar width based on the count relative to max
                LinearLayout.LayoutParams barParams = (LinearLayout.LayoutParams) optionBar.getLayoutParams();
                barParams.weight = maxValue > 0 ? (count * 100f / maxValue) : 0;
                optionBar.setLayoutParams(barParams);

                // Set option count and percentage
                optionCount.setText(String.format("%d (%.1f%%)", count, percentage));

                // Assign colors based on specific option text for each question
                // This approach is more direct and explicit than the previous keyword-based approach
                assignColorToBar(questionNumber, optionName, optionBar);

                barsContainer.addView(optionView);
            }

            ratingContainer.addView(ratingView);
        }
    }

    /**
     * Assign the appropriate color to a rating bar based on the question and option
     */
    private void assignColorToBar(int questionNumber, String optionName, View optionBar) {
        // Define colors for specific options for each question
        switch (questionNumber) {
            case 1: // Organization
                if (optionName.equals("Very organized")) {
                    optionBar.setBackgroundResource(R.color.positive_green);
                } else if (optionName.equals("Somewhat organized")) {
                    optionBar.setBackgroundResource(R.color.light_green);
                } else if (optionName.equals("Neutral")) {
                    optionBar.setBackgroundResource(R.color.neutral_yellow);
                } else if (optionName.equals("Somewhat disorganized")) {
                    optionBar.setBackgroundResource(R.color.light_red);
                } else if (optionName.equals("Very disorganized")) {
                    optionBar.setBackgroundResource(R.color.negative_red);
                }
                break;

            case 2: // Role clarity
                if (optionName.equals("Yes, very clear")) {
                    optionBar.setBackgroundResource(R.color.positive_green);
                } else if (optionName.equals("Somewhat clear")) {
                    optionBar.setBackgroundResource(R.color.light_green);
                } else if (optionName.equals("Neutral")) {
                    optionBar.setBackgroundResource(R.color.neutral_yellow);
                } else if (optionName.equals("Somewhat unclear")) {
                    optionBar.setBackgroundResource(R.color.light_red);
                } else if (optionName.equals("Not at all")) {
                    optionBar.setBackgroundResource(R.color.negative_red);
                }
                break;

            case 3: // Resources
                if (optionName.equals("Yes")) {
                    optionBar.setBackgroundResource(R.color.positive_green);
                } else if (optionName.equals("No, some were lacking")) {
                    optionBar.setBackgroundResource(R.color.neutral_yellow);
                } else if (optionName.equals("No, most were lacking")) {
                    optionBar.setBackgroundResource(R.color.negative_red);
                }
                break;

            case 4: // Satisfaction
                if (optionName.equals("Very satisfied")) {
                    optionBar.setBackgroundResource(R.color.positive_green);
                } else if (optionName.equals("Satisfied")) {
                    optionBar.setBackgroundResource(R.color.light_green);
                } else if (optionName.equals("Neutral")) {
                    optionBar.setBackgroundResource(R.color.neutral_yellow);
                } else if (optionName.equals("Dissatisfied")) {
                    optionBar.setBackgroundResource(R.color.light_red);
                } else if (optionName.equals("Very dissatisfied")) {
                    optionBar.setBackgroundResource(R.color.negative_red);
                }
                break;

            case 5: // Community participation
                if (optionName.equals("Yes, most were engaged")) {
                    optionBar.setBackgroundResource(R.color.positive_green);
                } else if (optionName.equals("Some participated")) {
                    optionBar.setBackgroundResource(R.color.light_green);
                } else if (optionName.equals("Neutral")) {
                    optionBar.setBackgroundResource(R.color.neutral_yellow);
                } else if (optionName.equals("Few participated")) {
                    optionBar.setBackgroundResource(R.color.light_red);
                } else if (optionName.equals("No engagement")) {
                    optionBar.setBackgroundResource(R.color.negative_red);
                }
                break;

            case 6: // Valued contributions
                if (optionName.equals("Yes, very much")) {
                    optionBar.setBackgroundResource(R.color.positive_green);
                } else if (optionName.equals("Somewhat")) {
                    optionBar.setBackgroundResource(R.color.light_green);
                } else if (optionName.equals("Neutral")) {
                    optionBar.setBackgroundResource(R.color.neutral_yellow);
                } else if (optionName.equals("Not really")) {
                    optionBar.setBackgroundResource(R.color.light_red);
                } else if (optionName.equals("Not at all")) {
                    optionBar.setBackgroundResource(R.color.negative_red);
                }
                break;

            case 7: // Community benefit
                if (optionName.equals("Yes, very much")) {
                    optionBar.setBackgroundResource(R.color.positive_green);
                } else if (optionName.equals("Somewhat")) {
                    optionBar.setBackgroundResource(R.color.light_green);
                } else if (optionName.equals("Neutral")) {
                    optionBar.setBackgroundResource(R.color.neutral_yellow);
                } else if (optionName.equals("Not really")) {
                    optionBar.setBackgroundResource(R.color.light_red);
                } else if (optionName.equals("Not at all")) {
                    optionBar.setBackgroundResource(R.color.negative_red);
                }
                break;

            default:
                // Default color if question number doesn't match
                optionBar.setBackgroundResource(R.color.colorPrimary);
                break;
        }
    }

    private void displayChallenges(int totalResponses) {
        // Add a section header for challenges
        TextView challengesHeader = new TextView(this);
        challengesHeader.setText("Challenges Faced by Volunteers");
        challengesHeader.setTextSize(18);
        challengesHeader.setTextColor(getResources().getColor(R.color.dark_green, null));
        challengesHeader.setPadding(16, 40, 16, 16);
        ratingContainer.addView(challengesHeader);

        // Create a layout for challenges
        LinearLayout challengesLayout = new LinearLayout(this);
        challengesLayout.setOrientation(LinearLayout.VERTICAL);
        challengesLayout.setPadding(16, 16, 16, 16);

        // Sort challenges by frequency
        List<Map.Entry<String, Integer>> sortedChallenges = new ArrayList<>(challengesCount.entrySet());
        sortedChallenges.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // Display each challenge with percentage
        for (Map.Entry<String, Integer> challenge : sortedChallenges) {
            LinearLayout challengeItem = new LinearLayout(this);
            challengeItem.setOrientation(LinearLayout.HORIZONTAL);
            challengeItem.setPadding(0, 8, 0, 8);

            TextView challengeText = new TextView(this);
            challengeText.setText(challenge.getKey());
            challengeText.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 3));

            TextView challengeCount = new TextView(this);
            float percentage = totalResponses > 0 ?
                    (challenge.getValue() * 100f / totalResponses) : 0;
            challengeCount.setText(String.format("%.1f%% (%d)", percentage, challenge.getValue()));
            challengeCount.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            challengeCount.setTextColor(getResources().getColor(R.color.dark_green, null));

            challengeItem.addView(challengeText);
            challengeItem.addView(challengeCount);
            challengesLayout.addView(challengeItem);
        }

        ratingContainer.addView(challengesLayout);
    }
}