package com.example.udyongbayanihan;

import android.content.Context;
import android.content.Intent;
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

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminEventsAdapter extends RecyclerView.Adapter<AdminEventsAdapter.MyViewHolder> {
    private static final String TAG = "AdminEventsAdapter";
    private Context context;
    private ArrayList<Post> eventList;
    private final SimpleDateFormat dateFormat;
    private FirebaseFirestore firestore;

    public AdminEventsAdapter(Context context, ArrayList<Post> eventList) {
        this.context = context;
        this.eventList = eventList;
        this.dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        this.firestore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.adminevents, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Post event = eventList.get(position);

        // Set basic event information
        holder.adminNameOfEvent.setText(event.getNameOfEvent());
        holder.adminTypeOfEvent.setText(event.getTypeOfEvent());
        holder.adminOrganization.setText(event.getOrganizations());
        holder.adminAddress.setText(event.getBarangay());
        holder.adminHeadCoordinator.setText(event.getHeadCoordinator());

        // Format and set date
        Timestamp dateTimestamp = event.getDate();
        if (dateTimestamp != null) {
            holder.adminDdate.setText(dateFormat.format(dateTimestamp.toDate()));
        } else {
            holder.adminDdate.setText("No Date Provided");
        }

        // Set event details
        List<String> skills = event.getEventSkills();
        holder.adminSkills.setText(skills != null && !skills.isEmpty() ?
                String.join(", ", skills) : "No Skills Specified");
        holder.adminCaption.setText(event.getCaption());

        // Set volunteer counts
        holder.adminVolunteersNeeded.setText("Volunteers Needed: " + event.getVolunteerNeeded());
        holder.adminParticipantsJoined.setText("Participants Joined: " + event.getParticipantsJoined());

        // Handle event images using the new RecyclerView
        setupEventImages(holder.imagesRecyclerView, event);

        // Setup buttons and interactivity
        setupButtons(holder, event, skills);
    }

    private void setupEventImages(RecyclerView recyclerView, Post event) {
        List<String> imageUrls = event.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            // Show RecyclerView and setup adapter for multiple images
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setLayoutManager(
                    new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

            EventImagesAdapter imagesAdapter = new EventImagesAdapter(context, imageUrls);
            recyclerView.setAdapter(imagesAdapter);

            Log.d(TAG, "Set up images recycler view with " + imageUrls.size() + " images");
        } else {
            // Hide RecyclerView if no images
            recyclerView.setVisibility(View.GONE);
        }
    }

    private void setupButtons(MyViewHolder holder, Post event, List<String> skills) {
        // Setup See Joined Users button
        holder.seeJoinedUsers.setOnClickListener(v -> {
            Intent intent = new Intent(context, AdminJoinedUsersActivity.class);
            intent.putExtra("eventName", event.getNameOfEvent());
            context.startActivity(intent);
        });

        // Setup Share button and checkboxes
        setupShareFunctionality(holder, event, skills);
    }

    private void setupShareFunctionality(MyViewHolder holder, Post event, List<String> skills) {
        holder.imgbtnShare.setOnClickListener(v -> toggleShareControls(holder, true));

        holder.checkboxShareContainer.removeAllViews();
        ArrayList<String> selectedSkills = new ArrayList<>();

        if (skills != null && !skills.isEmpty()) {
            createSkillCheckboxes(holder, skills, selectedSkills);
        } else {
            addNoSkillsMessage(holder);
        }
        setupShareButton(holder, event, selectedSkills); // Pass the Post object
    }

    private void toggleShareControls(MyViewHolder holder, boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        holder.checkboxShareContainer.setVisibility(visibility);
        holder.btnCheckboxShare.setVisibility(visibility);
        holder.textChooseGroup.setVisibility(visibility);
    }

    private void createSkillCheckboxes(MyViewHolder holder, List<String> skills, ArrayList<String> selectedSkills) {
        for (String skill : skills) {
            CheckBox checkBox = new CheckBox(context);
            checkBox.setText(skill);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedSkills.add(skill);
                } else {
                    selectedSkills.remove(skill);
                }
            });
            holder.checkboxShareContainer.addView(checkBox);
        }
    }

    private void addNoSkillsMessage(MyViewHolder holder) {
        TextView noSkillsText = new TextView(context);
        noSkillsText.setText("No required skills for this event.");
        holder.checkboxShareContainer.addView(noSkillsText);
    }

    private void setupShareButton(MyViewHolder holder, Post event, ArrayList<String> selectedSkills) {
        holder.btnCheckboxShare.setOnClickListener(v -> {
            if (selectedSkills.isEmpty()) {
                Toast.makeText(context, "Please select at least one skill to share.", Toast.LENGTH_SHORT).show();
            } else {
                handleSuccessfulShare(event, selectedSkills); // Pass event and skills
                toggleShareControls(holder, false);
            }
        });
    }

    private void handleSuccessfulShare(Post event, ArrayList<String> selectedSkills) {
        String amAccountId = event.getAmAccountId();
        String eventId = event.getEventId();

        if (amAccountId == null || eventId == null) {
            Toast.makeText(context, "Missing account or event details.", Toast.LENGTH_SHORT).show();
            return;
        }

        for (String skill : selectedSkills) {
            // Reference the specific skill document in the CommunityGroupSkills collection
            CollectionReference skillSubCollection = firestore.collection("CommunityGroupSkills")
                    .document(skill)
                    .collection(skill); // Subcollection with the same name as the skill

            // Auto-generate unique document ID
            DocumentReference skillDocRef = skillSubCollection.document();

            // Create the data to be stored in the document
            Map<String, Object> skillData = new HashMap<>();
            skillData.put("amAccountId", amAccountId);
            skillData.put("eventId", eventId);

            skillDocRef.set(skillData)
                    .addOnSuccessListener(aVoid ->
                            Log.d("Firestore", "Skill " + skill + " shared successfully."))
                    .addOnFailureListener(e ->
                            Log.e("Firestore", "Error sharing skill " + skill + ": " + e.getMessage()));
        }

        // Display a message summarizing the shared skills
        String skillsMessage = String.join(", ", selectedSkills);
        Toast.makeText(context, "Skills shared: " + skillsMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView adminNameOfEvent, adminTypeOfEvent, adminOrganization, adminAddress;
        TextView adminDdate, adminHeadCoordinator, adminSkills, adminCaption;
        TextView adminVolunteersNeeded, adminParticipantsJoined, textChooseGroup;
        RecyclerView imagesRecyclerView; // Changed from ImageView to RecyclerView
        Button seeJoinedUsers, btnCheckboxShare;
        ImageButton imgbtnShare;
        LinearLayout checkboxShareContainer;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            adminNameOfEvent = itemView.findViewById(R.id.adminNameOfEvent);
            adminTypeOfEvent = itemView.findViewById(R.id.adminTypeOfEvent);
            adminOrganization = itemView.findViewById(R.id.adminOrganization);
            adminAddress = itemView.findViewById(R.id.adminAddress);
            adminDdate = itemView.findViewById(R.id.adminDate);
            adminHeadCoordinator = itemView.findViewById(R.id.adminHeadCoordinator);
            adminSkills = itemView.findViewById(R.id.adminSkills);
            adminCaption = itemView.findViewById(R.id.adminCaption);
            adminVolunteersNeeded = itemView.findViewById(R.id.adminVolunteersNeeded);
            adminParticipantsJoined = itemView.findViewById(R.id.adminParticipantsJoined);
            imagesRecyclerView = itemView.findViewById(R.id.adminImagesRecyclerView); // Updated ID
            seeJoinedUsers = itemView.findViewById(R.id.seeJoinedUsers);
            imgbtnShare = itemView.findViewById(R.id.imgbtnShare);
            checkboxShareContainer = itemView.findViewById(R.id.checkboxShareContainer);
            btnCheckboxShare = itemView.findViewById(R.id.btnCheckboxShare);
            textChooseGroup = itemView.findViewById(R.id.textChooseGroup);
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
                // Could implement full screen image viewer here if needed
                Toast.makeText(context, "Image " + (position + 1) + " of " + imageUrls.size(),
                        Toast.LENGTH_SHORT).show();
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