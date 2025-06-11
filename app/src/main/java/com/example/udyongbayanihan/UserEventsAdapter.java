package com.example.udyongbayanihan;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserEventsAdapter extends RecyclerView.Adapter<UserEventsAdapter.EventViewHolder> {

    Context context;
    ArrayList<Post> events;
    private final boolean showFeedbackButton;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
    private OnFeedbackButtonClickListener feedbackButtonClickListener;

    public interface OnFeedbackButtonClickListener {
        void onFeedbackButtonClick(String eventId, String eventName);
    }

    public UserEventsAdapter(Context context, ArrayList<Post> events, boolean showFeedbackButton) {
        this.context = context;
        this.events = events;
        this.showFeedbackButton = showFeedbackButton;
    }

    public void setFeedbackButtonClickListener(OnFeedbackButtonClickListener listener) {
        this.feedbackButtonClickListener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use the existing post.xml layout for consistency
        View v = LayoutInflater.from(context).inflate(R.layout.item_user_events, parent, false);
        return new EventViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Post post = events.get(position);
        if (post == null) return;

        holder.organization.setText(post.getOrganizations());
        holder.headCoordinator.setText(post.getHeadCoordinator());
        holder.skills.setText(post.getEventSkills() != null ?
                String.join(", ", post.getEventSkills()) : "No Skills Required");

        holder.nameOfEvent.setText(post.getNameOfEvent());
        holder.volunteersNeeded.setText(String.format("%d/%d",
                post.getParticipantsJoined(),
                post.getVolunteerNeeded()));
        holder.typeOfEvent.setText(post.getTypeOfEvent());
        holder.address.setText(post.getBarangay() != null ? post.getBarangay() : "No Barangay Specified");
        if (post.getDate() != null) {
            holder.date.setText(sdf.format(post.getDate().toDate()));
        }
        holder.caption.setText(post.getCaption());

        // Handle the join/feedback button
        if (showFeedbackButton && post.isPostFeedback() && "Answer Feedback".equals(post.getJoinButtonText())) {
            holder.joinEvent.setText("Answer Feedback");
            holder.joinEvent.setEnabled(true);
            holder.joinEvent.setOnClickListener(v -> {
                if (feedbackButtonClickListener != null) {
                    feedbackButtonClickListener.onFeedbackButtonClick(post.getEventId(), post.getNameOfEvent());
                }
            });
        } else {
            // For other cases, just show the text without click action
            holder.joinEvent.setText(post.getJoinButtonText());
            holder.joinEvent.setEnabled(false);
        }

        // Handle images
        List<String> imageUrls = post.getImageUrls();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            holder.posterRecyclerView.setVisibility(View.VISIBLE);

            // Set up horizontal layout manager
            LinearLayoutManager layoutManager = new LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL,
                    false
            );
            holder.posterRecyclerView.setLayoutManager(layoutManager);

            // Create and set adapter
            ImagesAdapter imageAdapter = new ImagesAdapter(context, imageUrls);
            holder.posterRecyclerView.setAdapter(imageAdapter);

            // Add item decoration for spacing if needed
            int spacingInPixels = context.getResources().getDimensionPixelSize(R.dimen.image_spacing);
            holder.posterRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                           @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                    outRect.right = spacingInPixels;
                }
            });
        } else {
            holder.posterRecyclerView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        public Button joinEvent;
        public TextView nameOfEvent, typeOfEvent, organization, address, date, headCoordinator, skills, caption, volunteersNeeded;
        public RecyclerView posterRecyclerView;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);

            nameOfEvent = itemView.findViewById(R.id.nameOfEvent);
            typeOfEvent = itemView.findViewById(R.id.typeOfEvent);
            organization = itemView.findViewById(R.id.organization);
            address = itemView.findViewById(R.id.address);
            date = itemView.findViewById(R.id.date);
            headCoordinator = itemView.findViewById(R.id.headCoordinator);
            skills = itemView.findViewById(R.id.skills);
            caption = itemView.findViewById(R.id.caption);
            volunteersNeeded = itemView.findViewById(R.id.volunteersNeeded);
            posterRecyclerView = itemView.findViewById(R.id.posterRecyclerView);
            joinEvent = itemView.findViewById(R.id.joinEvent);
        }
    }
}