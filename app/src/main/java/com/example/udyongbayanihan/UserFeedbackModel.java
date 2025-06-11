package com.example.udyongbayanihan;

import java.util.Map;

public class UserFeedbackModel {
    private String userId;
    private String name;
    private String age;
    private String role;
    private Map<String, String> ratings;
    private String profilePictureUrl; // Added field for profile picture URL

    public UserFeedbackModel(String userId, String name, String age, String role, Map<String, String> ratings, String profilePictureUrl) {
        this.userId = userId;
        this.name = name;
        this.age = age;
        this.role = role;
        this.ratings = ratings;
        this.profilePictureUrl = profilePictureUrl;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getAge() {
        return age;
    }

    public String getRole() {
        return role;
    }

    public Map<String, String> getRatings() {
        return ratings;
    }

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    /**
     * Get the overall satisfaction rating for this user
     * @return String representing the satisfaction level
     */
    public String getOverallSatisfaction() {
        if (ratings == null) {
            return "N/A";
        }

        // Check for the extended rating scheme first
        for (int i = 1; i <= 7; i++) {
            String extendedRating = ratings.get("question_" + i);
            if (extendedRating != null) {
                if (extendedRating.equals("Very Excellent")) {
                    return "Very satisfied";
                } else if (extendedRating.equals("Excellent")) {
                    return "Satisfied";
                } else if (extendedRating.equals("Fair")) {
                    return "Neutral";
                } else if (extendedRating.equals("Poor")) {
                    return "Dissatisfied";
                } else if (extendedRating.equals("Very Poor")) {
                    return "Very dissatisfied";
                }
            }
        }

        // Try to get the direct satisfaction rating if no extended rating found
        String satisfaction = ratings.get("satisfaction");
        if (satisfaction != null && !satisfaction.isEmpty()) {
            return satisfaction;
        }

        // If direct satisfaction is not available, calculate an aggregate sentiment
        int positiveCount = 0;
        int neutralCount = 0;
        int negativeCount = 0;

        // Count positive, neutral, and negative responses
        for (String value : ratings.values()) {
            if (value == null || value.isEmpty()) continue;

            String lowerValue = value.toLowerCase();

            if (lowerValue.contains("very") &&
                    (lowerValue.contains("organized") ||
                            lowerValue.contains("satisfied") ||
                            lowerValue.contains("clear") ||
                            lowerValue.contains("yes") ||
                            lowerValue.contains("engaged") ||
                            lowerValue.contains("valued") ||
                            lowerValue.contains("beneficial"))) {
                positiveCount += 2;
            } else if (lowerValue.contains("somewhat") &&
                    (lowerValue.contains("organized") ||
                            lowerValue.contains("satisfied") ||
                            lowerValue.contains("clear"))) {
                positiveCount += 1;
            } else if (lowerValue.contains("neutral")) {
                neutralCount += 1;
            } else if (lowerValue.contains("somewhat") &&
                    (lowerValue.contains("disorganized") ||
                            lowerValue.contains("dissatisfied") ||
                            lowerValue.contains("unclear"))) {
                negativeCount += 1;
            } else if (lowerValue.contains("very") &&
                    (lowerValue.contains("disorganized") ||
                            lowerValue.contains("dissatisfied") ||
                            lowerValue.contains("not at all"))) {
                negativeCount += 2;
            }
        }

        // Determine overall sentiment based on counts
        if (positiveCount > (neutralCount + negativeCount)) {
            return "Very satisfied";
        } else if (positiveCount > negativeCount) {
            return "Satisfied";
        } else if (neutralCount > (positiveCount + negativeCount)) {
            return "Neutral";
        } else if (negativeCount > 0) {
            return "Dissatisfied";
        } else {
            return "N/A";
        }
    }

    /**
     * Calculate numerical satisfaction score for this user (1-5 scale)
     * Used for overall event success rating calculation
     * @return float value from 1.0 to 5.0, or 0 if no ratings
     */
    public float calculateSatisfactionScore() {
        if (ratings == null || ratings.isEmpty()) {
            return 0f;
        }

        // Define weights for each question (total should equal 7 for backward compatibility)
        float[] questionWeights = {
                1.0f,  // Q1
                1.0f,  // Q2
                1.0f,  // Q3
                1.0f,  // Q4
                1.0f,  // Q5
                1.0f,  // Q6
                1.0f   // Q7
        };

        float weightedTotalScore = 0f;
        float totalWeightsUsed = 0f;

        // Process only question_1 to question_7
        for (int i = 1; i <= 7; i++) {
            float questionScore = 0f;
            boolean scoreFound = false;

            // Check only for the extended ratings (question_1 to question_7)
            String questionKey = "question_" + i;
            String extendedRating = ratings.get(questionKey);

            if (extendedRating != null) {
                if (extendedRating.equals("Very Excellent")) questionScore = 5f;
                else if (extendedRating.equals("Excellent")) questionScore = 4f;
                else if (extendedRating.equals("Fair")) questionScore = 3f;
                else if (extendedRating.equals("Poor")) questionScore = 2f;
                else if (extendedRating.equals("Very Poor")) questionScore = 1f;
                scoreFound = true;
            }

            // Only add to the total if we found a score for this question
            if (scoreFound) {
                weightedTotalScore += questionScore * questionWeights[i-1];
                totalWeightsUsed += questionWeights[i-1];
            }
        }

        // Return weighted average score if we have ratings, otherwise 0
        return totalWeightsUsed > 0 ? (weightedTotalScore / totalWeightsUsed) : 0f;
    }
}