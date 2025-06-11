package com.example.udyongbayanihan;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class PendingEventsAdapter extends RecyclerView.Adapter<PendingEventsAdapter.EventViewHolder> {
    private static final String TAG = "PendingEventsAdapter";
    private List<EventModel> eventsList;
    private Context context;

    public PendingEventsAdapter(List<EventModel> eventsList) {
        this.eventsList = eventsList;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.pending_admin_events, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(EventViewHolder holder, int position) {
        EventModel event = eventsList.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return eventsList.size();
    }

    class EventViewHolder extends RecyclerView.ViewHolder {
        private TextView adminOrganization;
        private TextView adminNameOfEvent;
        private TextView adminTypeOfEvent;
        private TextView adminDate;
        private TextView adminAddress;
        private TextView adminHeadCoordinator;
        private TextView adminSkills;
        private TextView adminVolunteersNeeded;
        private TextView adminCaption;
        private RecyclerView imagesRecyclerView;

        public EventViewHolder(View itemView) {
            super(itemView);
            adminOrganization = itemView.findViewById(R.id.adminOrganization);
            adminNameOfEvent = itemView.findViewById(R.id.adminNameOfEvent);
            adminTypeOfEvent = itemView.findViewById(R.id.adminTypeOfEvent);
            adminDate = itemView.findViewById(R.id.adminDate);
            adminAddress = itemView.findViewById(R.id.adminAddress);
            adminHeadCoordinator = itemView.findViewById(R.id.adminHeadCoordinator);
            adminSkills = itemView.findViewById(R.id.adminSkills);
            adminVolunteersNeeded = itemView.findViewById(R.id.adminVolunteersNeeded);
            adminCaption = itemView.findViewById(R.id.adminCaption);
            imagesRecyclerView = itemView.findViewById(R.id.pendingImagesRecyclerView);
        }

        public void bind(EventModel event) {
            adminOrganization.setText(event.getOrganization());
            adminNameOfEvent.setText(event.getName());
            adminTypeOfEvent.setText(event.getType());

            // Format date nicely
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            if (event.getDate() != null) {
                adminDate.setText(dateFormat.format(event.getDate()));
            } else {
                adminDate.setText("No date");
            }

            // Set address (barangay)
            adminAddress.setText(event.getAddress());

            adminHeadCoordinator.setText(event.getHeadCoordinator());

            // Format skills list
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

            // Setup the images recycler view
            setupImagesRecyclerView(event.getImageUrls());
        }

        private void setupImagesRecyclerView(List<String> imageUrls) {
            if (imageUrls == null || imageUrls.isEmpty()) {
                imagesRecyclerView.setVisibility(View.GONE);
                return;
            }

            imagesRecyclerView.setVisibility(View.VISIBLE);
            imagesRecyclerView.setLayoutManager(
                    new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

            // Use adapter for the image recycler view
            EventImagesAdapter imagesAdapter = new EventImagesAdapter(context, imageUrls);
            imagesRecyclerView.setAdapter(imagesAdapter);

            Log.d(TAG, "Set up images recycler view with " + imageUrls.size() + " images");
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