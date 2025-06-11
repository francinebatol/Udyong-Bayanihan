package com.example.udyongbayanihan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class UserFeedbackAdapter extends RecyclerView.Adapter<UserFeedbackAdapter.ViewHolder> {
    private List<UserFeedbackModel> usersList;

    public UserFeedbackAdapter(List<UserFeedbackModel> usersList) {
        this.usersList = usersList;
    }

    /**
     * Create and return a view that shows the legend for satisfaction icons
     * @param parent The parent ViewGroup
     * @return The legend view
     */
    public View createSatisfactionLegendView(ViewGroup parent) {
        View legendView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.satisfaction_icons_legend, parent, false);
        return legendView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_feedback, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserFeedbackModel user = usersList.get(position);

        holder.nameTextView.setText(user.getName());
        holder.ageTextView.setText("Age: " + user.getAge());
        holder.roleTextView.setText(user.getRole());

        // Load and display the user's profile picture
        if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isEmpty()) {
            // Use Glide to load the image from URL
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfilePictureUrl())
                    .placeholder(R.drawable.default_icon)
                    .error(R.drawable.default_icon)
                    .circleCrop() // Makes the image circular
                    .into(holder.userIcon);
        } else {
            // Set default icon if no profile picture URL is available
            holder.userIcon.setImageResource(R.drawable.default_icon);
        }

        // Set satisfaction indicator
        String satisfaction = user.getOverallSatisfaction();
        int satisfactionIcon;

        // Set the appropriate icon based on satisfaction level
        if ("Very satisfied".equals(satisfaction)) {
            satisfactionIcon = R.drawable.ic_sentiment_satisfied;
        } else if ("Satisfied".equals(satisfaction)) {
            satisfactionIcon = R.drawable.ic_sentiment_satisfied;
        } else if ("Neutral".equals(satisfaction)) {
            satisfactionIcon = R.drawable.ic_sentiment_neutral;
        } else if ("Dissatisfied".equals(satisfaction)) {
            satisfactionIcon = R.drawable.ic_sentiment_dissatisfied;
        } else if ("Very dissatisfied".equals(satisfaction)) {
            satisfactionIcon = R.drawable.ic_sentiment_dissatisfied;
        } else {
            satisfactionIcon = R.drawable.ic_sentiment_neutral;
        }

        holder.satisfactionIcon.setImageResource(satisfactionIcon);
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        TextView ageTextView;
        TextView roleTextView;
        ImageView satisfactionIcon;
        ImageView userIcon; // Reference to the profile picture ImageView

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            ageTextView = itemView.findViewById(R.id.ageTextView);
            roleTextView = itemView.findViewById(R.id.roleTextView);
            satisfactionIcon = itemView.findViewById(R.id.satisfactionIcon);
            userIcon = itemView.findViewById(R.id.userIcon); // Initialize the userIcon reference
        }
    }
}