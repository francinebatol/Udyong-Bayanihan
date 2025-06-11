package com.example.udyongbayanihan;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

class AcceptedEventsAdapter extends RecyclerView.Adapter<AcceptedEventsAdapter.EventViewHolder> {
    private List<EventModel> eventsList;
    private Context context;
    private FirebaseFirestore firestore;
    private static final String TAG = "AcceptedEventsAdapter";
    private boolean isShareControlsVisible = false;  // Track the visibility state

    public AcceptedEventsAdapter(List<EventModel> eventsList) {
        this.eventsList = eventsList;
        this.firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.adminevents, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventModel event = eventsList.get(position);
        holder.bind(event);

        // Set up click listener for seeJoinedUsers
        holder.seeJoinedUsers.setOnClickListener(v -> {
            Intent intent = new Intent(context, AdminJoinedUsersActivity.class);
            intent.putExtra("eventName", event.getName());
            intent.putExtra("eventId", event.getEventId());
            intent.putExtra("eventBarangay", event.getAddress());
            intent.putStringArrayListExtra("eventSkills", new ArrayList<>(event.getEventSkills()));
            context.startActivity(intent);
        });

        // Check if event date has passed
        boolean isEventPassed = isEventDatePassed(event.getDate());

        // Setup share button functionality
        if (isEventPassed) {
            // Disable the share button if event date has passed
            holder.imgbtnShare.setEnabled(false);
            holder.imgbtnShare.setAlpha(0.5f); // Make it look disabled
        } else {
            holder.imgbtnShare.setEnabled(true);
            holder.imgbtnShare.setAlpha(1.0f);

            // Setup share button click listener to toggle visibility
            holder.imgbtnShare.setOnClickListener(v -> {
                if (isShareControlsVisible) {
                    toggleShareControls(holder, false);
                    isShareControlsVisible = false;
                } else {
                    toggleShareControls(holder, true);
                    isShareControlsVisible = true;
                    setupShareFunctionality(holder, event);
                }
            });
        }

        // Check if event is ongoing or ended - show post feedback button only in these cases
        checkEventDateAndUpdateFeedbackButton(holder, event);
    }

    /**
     * Check if event date has passed
     */
    private boolean isEventDatePassed(Date eventDate) {
        if (eventDate == null) {
            return false;
        }

        // Get today's date without time
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        // Get event date without time
        Calendar eventCal = Calendar.getInstance();
        eventCal.setTime(eventDate);
        eventCal.set(Calendar.HOUR_OF_DAY, 0);
        eventCal.set(Calendar.MINUTE, 0);
        eventCal.set(Calendar.SECOND, 0);
        eventCal.set(Calendar.MILLISECOND, 0);

        // Return true if event date is in the past
        return eventCal.before(today);
    }

    /**
     * Check if event is ongoing or ended and update the feedback button visibility
     */
    private void checkEventDateAndUpdateFeedbackButton(EventViewHolder holder, EventModel event) {
        // First check if event type is "Volunteer Activity"
        if (event.getType() != null && event.getType().equals("Volunteer Activity")) {
            Date eventDate = event.getDate();
            if (eventDate != null) {
                // Get today's date (without time)
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);

                // Get event date (without time)
                Calendar eventCal = Calendar.getInstance();
                eventCal.setTime(eventDate);
                eventCal.set(Calendar.HOUR_OF_DAY, 0);
                eventCal.set(Calendar.MINUTE, 0);
                eventCal.set(Calendar.SECOND, 0);
                eventCal.set(Calendar.MILLISECOND, 0);

                // Show button if event is today or in the past
                if (eventCal.compareTo(today) <= 0) {
                    holder.postFeedbackButton.setVisibility(View.VISIBLE);

                    // Check if feedback has already been posted
                    checkFeedbackPostedStatus(event.getEventId(), holder);
                } else {
                    holder.postFeedbackButton.setVisibility(View.GONE);
                }
            } else {
                holder.postFeedbackButton.setVisibility(View.GONE);
            }
        } else {
            holder.postFeedbackButton.setVisibility(View.GONE);
        }
    }

    private void checkFeedbackPostedStatus(String eventId, EventViewHolder holder) {
        firestore.collection("EventInformation")
                .document(eventId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean postFeedback = documentSnapshot.getBoolean("postFeedback");
                        if (postFeedback != null && postFeedback) {
                            // Feedback already posted
                            holder.postFeedbackButton.setText("Feedback already posted");
                            holder.postFeedbackButton.setEnabled(false);
                            holder.postFeedbackButton.setAlpha(0.5f); // Visually show it's disabled
                        } else {
                            // Feedback not posted yet
                            holder.postFeedbackButton.setText("Post Feedback");
                            holder.postFeedbackButton.setEnabled(true);
                            holder.postFeedbackButton.setAlpha(1.0f);

                            // Setup post feedback button functionality
                            holder.postFeedbackButton.setOnClickListener(v -> {
                                updatePostFeedbackStatus(eventId, holder);
                            });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error checking feedback status: " + e.getMessage());
                });
    }

    private void updatePostFeedbackStatus(String eventId, EventViewHolder holder) {
        if (eventId == null) {
            Toast.makeText(context, "Event ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current device user ID from SharedPreferences
        SharedPreferences notificationPrefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
        String currentDeviceUserId = notificationPrefs.getString("current_device_user_id", null);
        Log.d("Feedback", "Current device user ID: " + currentDeviceUserId);

        // Update event status first
        DocumentReference eventRef = firestore.collection("EventInformation")
                .document(eventId);

        Map<String, Object> updates = new HashMap<>();
        updates.put("postFeedback", true);
        updates.put("feedbackNotificationsSent", true);

        eventRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update the button UI immediately
                    holder.postFeedbackButton.setText("Feedback already posted");
                    holder.postFeedbackButton.setEnabled(false);
                    holder.postFeedbackButton.setAlpha(0.5f);

                    Toast.makeText(context, "Feedback request sent to participants", Toast.LENGTH_SHORT).show();

                    // First get event details
                    firestore.collection("EventDetails")
                            .whereEqualTo("eventId", eventId)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(eventDetailsDocs -> {
                                if (!eventDetailsDocs.isEmpty()) {
                                    DocumentSnapshot detailsDoc = eventDetailsDocs.getDocuments().get(0);
                                    String eventName = detailsDoc.getString("nameOfEvent");
                                    String barangay = detailsDoc.getString("barangay");

                                    if (eventName != null) {
                                        // STEP 1: Get a list of all admins to exclude
                                        firestore.collection("AdminMobileAccount")
                                                .get()
                                                .addOnSuccessListener(adminDocs -> {
                                                    // Create a set of all admin IDs to exclude
                                                    Set<String> adminIds = new HashSet<>();
                                                    for (DocumentSnapshot adminDoc : adminDocs) {
                                                        String adminId = adminDoc.getString("amAccountId");
                                                        if (adminId != null && !adminId.isEmpty()) {
                                                            adminIds.add(adminId);
                                                        }
                                                    }

                                                    Log.d("Feedback", "Found " + adminIds.size() + " admin IDs to exclude from notifications");

                                                    // Get current admin ID from SharedPreferences for extra safety
                                                    SharedPreferences adminPrefs = context.getSharedPreferences("AdminSession", Context.MODE_PRIVATE);
                                                    String currentAdminId = adminPrefs.getString("amAccountId", null);
                                                    if (currentAdminId != null && !adminIds.contains(currentAdminId)) {
                                                        adminIds.add(currentAdminId);
                                                        Log.d("Feedback", "Added current admin ID to exclusion list: " + currentAdminId);
                                                    }

                                                    // STEP 2: Get a list of all regular users
                                                    firestore.collection("usersAccount")
                                                            .get()
                                                            .addOnSuccessListener(userAccountDocs -> {
                                                                // Create a set of all regular user IDs
                                                                Set<String> regularUserIds = new HashSet<>();
                                                                for (DocumentSnapshot userDoc : userAccountDocs) {
                                                                    String userId = userDoc.getId();
                                                                    // Only add if not an admin
                                                                    if (!adminIds.contains(userId)) {
                                                                        regularUserIds.add(userId);
                                                                    }
                                                                }

                                                                Log.d("Feedback", "Found " + regularUserIds.size() + " regular users (excluding admins)");

                                                                // STEP 3: Find all users who joined this event
                                                                firestore.collection("UserJoinEvents")
                                                                        .whereEqualTo("eventName", eventName)
                                                                        .get()
                                                                        .addOnSuccessListener(userJoinDocs -> {
                                                                            Log.d("Feedback", "Found " + userJoinDocs.size() + " users who joined event: " + eventName);

                                                                            int totalParticipants = userJoinDocs.size();
                                                                            int adminsSkipped = 0;
                                                                            int duplicatesSkipped = 0;
                                                                            int notificationsCreated = 0;

                                                                            // Track users we've already processed
                                                                            List<String> processedUsers = new ArrayList<>();

                                                                            // Send notifications with delay between each
                                                                            Handler handler = new Handler(context.getMainLooper());
                                                                            int delayIncrement = 500; // 500ms between notifications
                                                                            int currentDelay = 0;

                                                                            for (QueryDocumentSnapshot userJoinDoc : userJoinDocs) {
                                                                                String userId = userJoinDoc.getString("userId");

                                                                                // Skip invalid user IDs
                                                                                if (userId == null || userId.isEmpty()) {
                                                                                    continue;
                                                                                }

                                                                                // CRITICAL: EXPLICITLY skip admins
                                                                                if (userId.equals(currentAdminId)) {
                                                                                    Log.d("Feedback", "⚠️ EXPLICITLY SKIPPING current admin: " + userId);
                                                                                    continue;
                                                                                }

                                                                                // CRITICAL: Only send to users who are confirmed regular users
                                                                                if (!regularUserIds.contains(userId)) {
                                                                                    Log.d("Feedback", "⚠️ EXCLUDING non-regular user: " + userId);
                                                                                    adminsSkipped++;
                                                                                    continue;
                                                                                }

                                                                                // Skip duplicate users
                                                                                if (processedUsers.contains(userId)) {
                                                                                    Log.d("Feedback", "⚠️ Skipping duplicate user: " + userId);
                                                                                    duplicatesSkipped++;
                                                                                    continue;
                                                                                }

                                                                                processedUsers.add(userId);

                                                                                String notificationId = "feedback_" + eventId + "_" + userId;

                                                                                // Process with delay to prevent throttling
                                                                                final int delay = currentDelay;
                                                                                handler.postDelayed(() -> {
                                                                                    // Check if notification already exists
                                                                                    firestore.collection("Notifications")
                                                                                            .document(notificationId)
                                                                                            .get()
                                                                                            .addOnSuccessListener(notificationDoc -> {
                                                                                                if (notificationDoc.exists()) {
                                                                                                    Log.d("Feedback", "Notification already exists for user: " + userId);
                                                                                                    return;
                                                                                                }

                                                                                                // Get user details for notification
                                                                                                firestore.collection("usersAccount")
                                                                                                        .document(userId)
                                                                                                        .get()
                                                                                                        .addOnSuccessListener(userDoc -> {
                                                                                                            if (userDoc.exists()) {
                                                                                                                String uaddressId = userDoc.getString("uaddressId");
                                                                                                                String unameId = userDoc.getString("unameId");
                                                                                                                String uotherDetails = userDoc.getString("uotherDetails");

                                                                                                                // Create notification object
                                                                                                                Notification notification = Notification.createFeedbackNotification(
                                                                                                                        userId, eventId, eventName, barangay);
                                                                                                                notification.setRead(false);

                                                                                                                // Add to Firestore with explicit ID
                                                                                                                firestore.collection("Notifications")
                                                                                                                        .document(notification.getId())
                                                                                                                        .set(notification)
                                                                                                                        .addOnSuccessListener(notifResult -> {
                                                                                                                            Log.d("Feedback", "✓ Notification saved for user: " + userId);

                                                                                                                            // Only show system notification if this is the current device user
                                                                                                                            if (userId.equals(currentDeviceUserId)) {
                                                                                                                                // Show system notification
                                                                                                                                NotificationHelper helper = new NotificationHelper(context);
                                                                                                                                helper.showFeedbackRequestNotification(
                                                                                                                                        userId, eventId, eventName, barangay,
                                                                                                                                        uaddressId, unameId, uotherDetails);
                                                                                                                                Log.d("Feedback", "✓ System notification shown for CURRENT DEVICE USER: " + userId);
                                                                                                                            } else {
                                                                                                                                Log.d("Feedback", "✓ Firestore notification saved for user: " + userId +
                                                                                                                                        " (system notification skipped - not current device user)");
                                                                                                                            }
                                                                                                                        });
                                                                                                            }
                                                                                                        });
                                                                                            });
                                                                                }, delay);

                                                                                currentDelay += delayIncrement;
                                                                                notificationsCreated++;
                                                                            }

                                                                            Log.d("Feedback", "Summary: " + totalParticipants + " total participants, "
                                                                                    + adminsSkipped + " admins/non-regular users excluded, "
                                                                                    + duplicatesSkipped + " duplicates skipped, "
                                                                                    + notificationsCreated + " notifications created in Firestore, "
                                                                                    + "current device user ID: " + currentDeviceUserId);
                                                                        });
                                                            });
                                                });
                                    }
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Error updating feedback status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Feedback", "Error updating postFeedback status: " + e.getMessage());
                });
    }

    private void toggleShareControls(EventViewHolder holder, boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        holder.checkboxShareContainer.setVisibility(visibility);
        holder.btnCheckboxShare.setVisibility(visibility);
        holder.textChooseGroup.setVisibility(visibility);
    }

    private void setupShareFunctionality(EventViewHolder holder, EventModel event) {
        holder.checkboxShareContainer.removeAllViews();
        ArrayList<String> selectedSkills = new ArrayList<>();

        List<String> eventSkills = event.getEventSkills();
        if (eventSkills != null && !eventSkills.isEmpty()) {
            // First, check which skills this event has already been shared to
            if (event.getEventId() != null) {
                checkAlreadySharedSkills(event.getEventId(), eventSkills, sharedSkills -> {
                    createSkillCheckboxes(holder, eventSkills, selectedSkills, sharedSkills);
                });
            } else {
                createSkillCheckboxes(holder, eventSkills, selectedSkills, new HashSet<>());
            }
        } else {
            addNoSkillsMessage(holder);
        }

        setupShareButton(holder, event, selectedSkills);
    }

    /**
     * Check which skills this event has already been shared to
     */
    private void checkAlreadySharedSkills(String eventId, List<String> eventSkills,
                                          OnSharedSkillsCheckedListener listener) {
        Set<String> sharedSkills = new HashSet<>();
        AtomicInteger checkCounter = new AtomicInteger(0);

        if (eventSkills.isEmpty()) {
            listener.onSharedSkillsChecked(sharedSkills);
            return;
        }

        for (String skill : eventSkills) {
            // Query the skill subcollection to see if this event has been shared
            firestore.collection("CommunityGroupSkills")
                    .document(skill)
                    .collection(skill)
                    .whereEqualTo("eventId", eventId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Event has been shared to this skill
                            sharedSkills.add(skill);
                            Log.d(TAG, "Event " + eventId + " already shared to skill: " + skill);
                        }

                        // Check if all queries have completed
                        if (checkCounter.incrementAndGet() == eventSkills.size()) {
                            listener.onSharedSkillsChecked(sharedSkills);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error checking if event is shared to skill " + skill + ": " + e.getMessage());

                        // Even on failure, increment counter to ensure callback is called
                        if (checkCounter.incrementAndGet() == eventSkills.size()) {
                            listener.onSharedSkillsChecked(sharedSkills);
                        }
                    });
        }
    }

    // Interface for callback when shared skills check is complete
    interface OnSharedSkillsCheckedListener {
        void onSharedSkillsChecked(Set<String> sharedSkills);
    }

    private void createSkillCheckboxes(EventViewHolder holder, List<String> skills,
                                       ArrayList<String> selectedSkills, Set<String> sharedSkills) {
        for (String skill : skills) {
            CheckBox checkBox = new CheckBox(context);
            checkBox.setText(skill);

            // Check if this skill has already been shared to
            if (sharedSkills.contains(skill)) {
                checkBox.setChecked(true);
                checkBox.setEnabled(false); // Disable the checkbox
                checkBox.setText(skill + " (Already shared)");
            } else {
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        selectedSkills.add(skill);
                    } else {
                        selectedSkills.remove(skill);
                    }
                });
            }

            holder.checkboxShareContainer.addView(checkBox);
        }
    }

    private void addNoSkillsMessage(EventViewHolder holder) {
        TextView noSkillsText = new TextView(context);
        noSkillsText.setText("No required skills for this event.");
        holder.checkboxShareContainer.addView(noSkillsText);
    }

    private void setupShareButton(EventViewHolder holder, EventModel event, ArrayList<String> selectedSkills) {
        holder.btnCheckboxShare.setOnClickListener(v -> {
            if (selectedSkills.isEmpty()) {
                Toast.makeText(context, "Please select at least one skill to share.", Toast.LENGTH_SHORT).show();
            } else {
                handleShare(event, selectedSkills);
                toggleShareControls(holder, false);
                isShareControlsVisible = false;
            }
        });
    }

    private void handleShare(EventModel event, ArrayList<String> selectedSkills) {
        String eventId = event.getEventId();

        if (eventId == null) {
            Toast.makeText(context, "Missing event details.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a timestamp for the current time
        Timestamp currentTimestamp = new Timestamp(new Date());

        // Counter to track completed operations
        AtomicInteger completedOperations = new AtomicInteger(0);
        int totalOperations = selectedSkills.size();

        for (String skill : selectedSkills) {
            // Reference to the skill document
            DocumentReference skillDocRef = firestore.collection("CommunityGroupSkills")
                    .document(skill);

            // Reference to the skill-named subcollection
            CollectionReference skillSubCollection = skillDocRef
                    .collection(skill);

            // Create the shared event document
            DocumentReference sharedEventDocRef = skillSubCollection.document();

            Map<String, Object> sharedEventData = new HashMap<>();
            sharedEventData.put("eventId", eventId);
            sharedEventData.put("timeShared", currentTimestamp);  // Add timestamp when shared

            // Create/update the lastUpdated timestamp
            Map<String, Object> timestampData = new HashMap<>();
            timestampData.put("lastUpdated", currentTimestamp);

            // Batch write to ensure atomicity
            firestore.runTransaction(transaction -> {
                // Set the shared event data
                transaction.set(sharedEventDocRef, sharedEventData);

                // Set/update the lastUpdated timestamp in the skill document
                transaction.set(skillDocRef, timestampData, SetOptions.merge());

                return null;
            }).addOnSuccessListener(aVoid -> {
                Log.d("Firestore", "Event " + eventId + " shared successfully with timestamp to " + skill);

                // Increment the counter and check if all operations are complete
                if (completedOperations.incrementAndGet() == totalOperations) {
                    String skillsMessage = String.join(", ", selectedSkills);
                    Toast.makeText(context, "Event shared to: " + skillsMessage, Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Log.e("Firestore", "Error sharing event " + eventId + " to " + skill + ": " + e.getMessage());
                Toast.makeText(context, "Error sharing event to: " + skill, Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public int getItemCount() {
        return eventsList.size();
    }

    class EventViewHolder extends RecyclerView.ViewHolder {
        private TextView adminOrganization, adminNameOfEvent, adminTypeOfEvent, adminDate, adminHeadCoordinator,
                adminSkills, adminVolunteersNeeded, adminCaption, textChooseGroup, adminAddress, adminParticipantsJoined;
        private Button seeJoinedUsers, btnCheckboxShare, postFeedbackButton;
        private ImageButton imgbtnShare;
        private RecyclerView imageRecyclerView;
        LinearLayout checkboxShareContainer;

        public EventViewHolder(View itemView) {
            super(itemView);
            adminOrganization = itemView.findViewById(R.id.adminOrganization);
            adminNameOfEvent = itemView.findViewById(R.id.adminNameOfEvent);
            adminTypeOfEvent = itemView.findViewById(R.id.adminTypeOfEvent);
            adminDate = itemView.findViewById(R.id.adminDate);
            adminHeadCoordinator = itemView.findViewById(R.id.adminHeadCoordinator);
            adminSkills = itemView.findViewById(R.id.adminSkills);
            adminVolunteersNeeded = itemView.findViewById(R.id.adminVolunteersNeeded);
            adminCaption = itemView.findViewById(R.id.adminCaption);
            adminAddress = itemView.findViewById(R.id.adminAddress);
            adminParticipantsJoined = itemView.findViewById(R.id.adminParticipantsJoined);

            // Replace the ImageView with RecyclerView for multiple images
            imageRecyclerView = itemView.findViewById(R.id.adminImagesRecyclerView);

            seeJoinedUsers = itemView.findViewById(R.id.seeJoinedUsers);
            btnCheckboxShare = itemView.findViewById(R.id.btnCheckboxShare);
            imgbtnShare = itemView.findViewById(R.id.imgbtnShare);
            checkboxShareContainer = itemView.findViewById(R.id.checkboxShareContainer);
            textChooseGroup = itemView.findViewById(R.id.textChooseGroup);
            postFeedbackButton = itemView.findViewById(R.id.postFeedbackButton);
        }

        public void bind(EventModel event) {
            adminOrganization.setText(event.getOrganization());
            adminNameOfEvent.setText(event.getName());
            adminTypeOfEvent.setText(event.getType());

            // Format the date nicely
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            if (event.getDate() != null) {
                adminDate.setText(dateFormat.format(event.getDate()));
            } else {
                adminDate.setText("No date");
            }

            adminHeadCoordinator.setText(event.getHeadCoordinator());

            // Set skills
            if (event.getEventSkills() != null && !event.getEventSkills().isEmpty()) {
                adminSkills.setText(android.text.TextUtils.join(", ", event.getEventSkills()));
            } else {
                adminSkills.setText("None");
            }

            // Set volunteers needed
            if (event.getVolunteersNeeded() != null) {
                adminVolunteersNeeded.setText(String.valueOf(event.getVolunteersNeeded()));
            } else {
                adminVolunteersNeeded.setText("0");
            }

            adminCaption.setText(event.getCaption());

            // Set address (barangay) - this was missing in original code
            adminAddress.setText(event.getAddress());

            // Set participants joined - this was missing in original code
            // We need to fetch this from Firestore since it's not part of the event model
            fetchParticipantsJoined(event.getEventId());

            // Setup image recycler view for multiple images
            setupImagesRecyclerView(event.getImageUrls());
        }

        private void fetchParticipantsJoined(String eventId) {
            if (eventId == null || eventId.isEmpty()) {
                adminParticipantsJoined.setText("0");
                return;
            }

            // First get the event name from EventDetails
            firestore.collection("EventDetails")
                    .whereEqualTo("eventId", eventId)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(eventDetailsSnapshot -> {
                        if (!eventDetailsSnapshot.isEmpty()) {
                            // Get the event name
                            String eventName = eventDetailsSnapshot.getDocuments().get(0).getString("nameOfEvent");
                            if (eventName != null && !eventName.isEmpty()) {
                                // Now query UserJoinEvents using the event name
                                firestore.collection("UserJoinEvents")
                                        .whereEqualTo("eventName", eventName)
                                        .get()
                                        .addOnSuccessListener(querySnapshot -> {
                                            int count = querySnapshot.size();
                                            adminParticipantsJoined.setText(String.valueOf(count));
                                            Log.d(TAG, "Participants joined for event " + eventName + ": " + count);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error fetching participants count by name: " + e.getMessage());
                                            adminParticipantsJoined.setText("0");
                                        });
                            } else {
                                adminParticipantsJoined.setText("0");
                                Log.w(TAG, "Event name is null or empty for event ID: " + eventId);
                            }
                        } else {
                            adminParticipantsJoined.setText("0");
                            Log.w(TAG, "No event details found for event ID: " + eventId);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching event details: " + e.getMessage());
                        adminParticipantsJoined.setText("0");
                    });
        }

        private void setupImagesRecyclerView(List<String> imageUrls) {
            if (imageUrls == null || imageUrls.isEmpty()) {
                imageRecyclerView.setVisibility(View.GONE);
                return;
            }

            imageRecyclerView.setVisibility(View.VISIBLE);
            imageRecyclerView.setLayoutManager(
                    new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

            // Use adapter for the image recycler view
            EventImagesAdapter imagesAdapter = new EventImagesAdapter(context, imageUrls);
            imageRecyclerView.setAdapter(imagesAdapter);
        }
    }

    /**
     * Adapter for the horizontal image gallery
     */
    private static class EventImagesAdapter extends RecyclerView.Adapter<EventImagesAdapter.ImageViewHolder> {
        private Context context;
        private List<String> imageUrls;

        public EventImagesAdapter(Context context, List<String> imageUrls) {
            this.context = context;
            this.imageUrls = imageUrls;
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.event_image_item, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            String imageUrl = imageUrls.get(position);

            // Load image with Glide
            Glide.with(context)
                    .load(imageUrl)
                    .centerCrop()
                    .into(holder.imageView);

            // Set click listener to open image in full screen
            holder.imageView.setOnClickListener(v -> {
                // Launch fullscreen image viewer with the ability to swipe between images
                Intent intent = new Intent(context, FullscreenImageViewer.class);
                intent.putStringArrayListExtra("imageUrls", new ArrayList<>(imageUrls));
                intent.putExtra("position", position);
                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return imageUrls.size();
        }

        static class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.eventImage);
            }
        }
    }
}