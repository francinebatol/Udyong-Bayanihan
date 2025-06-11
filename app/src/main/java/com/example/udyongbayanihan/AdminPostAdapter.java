package com.example.udyongbayanihan;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class AdminPostAdapter extends RecyclerView.Adapter<AdminPostAdapter.AdminPostViewHolder> {

    private Context context;
    private ArrayList<AdminPostItem> posts;
    private String amAccountId;
    private SimpleDateFormat dateFormat;

    public AdminPostAdapter(Context context, ArrayList<AdminPostItem> posts, String amAccountId) {
        this.context = context;
        this.posts = posts;
        this.amAccountId = amAccountId;
        this.dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public AdminPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_post, parent, false);
        return new AdminPostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminPostViewHolder holder, int position) {
        AdminPostItem post = posts.get(position);

        holder.adminNameText.setText(post.getAdminName());
        holder.adminPositionText.setText(post.getPosition());
        holder.postContentText.setText(post.getPostContent());

        // Format and set the date
        if (post.getTimestamp() != null) {
            holder.postDateText.setText(dateFormat.format(post.getTimestamp().toDate()));
        } else {
            holder.postDateText.setText("No date");
        }

        // Set post location (where it was posted)
        holder.postLocationText.setText(post.getFormattedLocation());

        // Set likes and comments count
        holder.likesCountText.setText(String.valueOf(post.getLikesCount()));
        holder.commentsCountText.setText(String.valueOf(post.getCommentsCount()));

        // Set up click listeners
        holder.likesContainer.setOnClickListener(v -> {
            // Open admin likes details activity
            Intent intent = new Intent(context, AdminLikesDetails.class);
            intent.putExtra("postId", post.getPostId());
            intent.putExtra("adminName", post.getAdminName());
            context.startActivity(intent);
        });

        holder.commentsContainer.setOnClickListener(v -> {
            openCommentsView(post);
        });

        holder.viewDetailsButton.setOnClickListener(v -> {
            openCommentsView(post);
        });
    }

    private void openCommentsView(AdminPostItem post) {
        // Navigate to admin comments screen
        Intent intent = new Intent(context, AdminSeeComments.class);
        intent.putExtra("postId", post.getPostId());
        intent.putExtra("adminId", amAccountId);
        intent.putExtra("adminName", post.getAdminName());
        intent.putExtra("position", post.getPosition());
        intent.putExtra("content", post.getPostContent());
        if (post.getTimestamp() != null) {
            intent.putExtra("timestampMillis", post.getTimestamp().toDate().getTime());
        }

        // Pass barangay if available
        if (post.isPostedInBarangay()) {
            intent.putExtra("barangay", post.getBarangay());
        }

        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    public void updateData(ArrayList<AdminPostItem> newPosts) {
        this.posts = newPosts;
        notifyDataSetChanged();
    }

    static class AdminPostViewHolder extends RecyclerView.ViewHolder {
        TextView adminNameText, adminPositionText, postContentText, postDateText;
        TextView postLocationText, likesCountText, commentsCountText;
        LinearLayout likesContainer, commentsContainer;
        Button viewDetailsButton;

        AdminPostViewHolder(@NonNull View itemView) {
            super(itemView);
            adminNameText = itemView.findViewById(R.id.adminNameText);
            adminPositionText = itemView.findViewById(R.id.adminPositionText);
            postContentText = itemView.findViewById(R.id.postContentText);
            postDateText = itemView.findViewById(R.id.postDateText);
            postLocationText = itemView.findViewById(R.id.postLocationText);
            likesCountText = itemView.findViewById(R.id.likesCountText);
            commentsCountText = itemView.findViewById(R.id.commentsCountText);
            likesContainer = itemView.findViewById(R.id.likesContainer);
            commentsContainer = itemView.findViewById(R.id.commentsContainer);
            viewDetailsButton = itemView.findViewById(R.id.viewDetailsButton);
        }
    }
}