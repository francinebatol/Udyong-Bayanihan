package com.example.udyongbayanihan;

import android.content.Context;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Helper class for loading profile pictures across different activities
 */
public class ProfilePictureHelper {

    private static final String TAG = "ProfilePictureHelper";

    /**
     * Loads a user's profile picture into an ImageView
     * @param context The context (activity or fragment)
     * @param userId The user ID to load the profile picture for
     * @param imageView The ImageView to load the picture into
     * @param applyCircleCrop Whether to apply circle crop to the image
     */
    public static void loadProfilePicture(Context context, String userId, ImageView imageView, boolean applyCircleCrop) {
        if (context == null || userId == null || imageView == null) {
            Log.e(TAG, "Invalid parameters for loading profile picture");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("usersOtherDetails")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot document = queryDocumentSnapshots.getDocuments().get(0);
                        String profilePictureUrl = document.getString("profilePictureUrl");

                        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                            // Build request with Glide
                            RequestOptions options = new RequestOptions()
                                    .placeholder(R.drawable.user)
                                    .error(R.drawable.user);

                            // Apply circle crop if needed
                            if (applyCircleCrop) {
                                options = options.circleCrop();
                            }

                            // Load the image
                            Glide.with(context)
                                    .load(profilePictureUrl)
                                    .apply(options)
                                    .into(imageView);
                        } else {
                            // Set default image if no URL found
                            imageView.setImageResource(R.drawable.user);
                        }
                    } else {
                        // Set default image if no document found
                        imageView.setImageResource(R.drawable.user);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading profile picture: " + e.getMessage());
                    // Set default image on error
                    imageView.setImageResource(R.drawable.user);
                });
    }

    /**
     * Loads a user's profile picture from a specific document ID
     * @param context The context (activity or fragment)
     * @param otherDetailsDocId The document ID in usersOtherDetails collection
     * @param imageView The ImageView to load the picture into
     * @param applyCircleCrop Whether to apply circle crop to the image
     */
    public static void loadProfilePictureByDocId(Context context, String otherDetailsDocId, ImageView imageView, boolean applyCircleCrop) {
        if (context == null || otherDetailsDocId == null || imageView == null) {
            Log.e(TAG, "Invalid parameters for loading profile picture");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("usersOtherDetails")
                .document(otherDetailsDocId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String profilePictureUrl = document.getString("profilePictureUrl");

                        if (profilePictureUrl != null && !profilePictureUrl.isEmpty()) {
                            // Build request with Glide
                            RequestOptions options = new RequestOptions()
                                    .placeholder(R.drawable.user)
                                    .error(R.drawable.user);

                            // Apply circle crop if needed
                            if (applyCircleCrop) {
                                options = options.circleCrop();
                            }

                            // Load the image
                            Glide.with(context)
                                    .load(profilePictureUrl)
                                    .apply(options)
                                    .into(imageView);
                        } else {
                            // Set default image if no URL found
                            imageView.setImageResource(R.drawable.user);
                        }
                    } else {
                        // Set default image if document doesn't exist
                        imageView.setImageResource(R.drawable.user);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading profile picture: " + e.getMessage());
                    // Set default image on error
                    imageView.setImageResource(R.drawable.user);
                });
    }

    /**
     * Usage example for an activity
     */
    public static void exampleUsage(Context context, String userId, ImageView imageView) {
        // Basic usage
        ProfilePictureHelper.loadProfilePicture(context, userId, imageView, true);

        // Or for document ID approach
        // ProfilePictureHelper.loadProfilePictureByDocId(context, documentId, imageView, true);
    }
}