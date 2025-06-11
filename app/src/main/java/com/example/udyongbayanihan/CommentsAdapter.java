package com.example.udyongbayanihan;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private Context context;
    private List<Comment> commentsList;
    private SimpleDateFormat dateFormat;

    public CommentsAdapter(Context context, List<Comment> commentsList) {
        this.context = context;
        this.commentsList = commentsList;
        this.dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentsList.get(position);

        holder.commentUserName.setText(comment.getFullName());
        holder.commentText.setText(comment.getCommentText());

        if (comment.getTimestamp() != null) {
            holder.commentTimestamp.setText(
                    dateFormat.format(comment.getTimestamp().toDate())
            );
        }

        // Show pinned indicator and label if comment is pinned
        if (comment.isPinned()) {
            holder.pinnedIndicator.setVisibility(View.VISIBLE);
            holder.pinnedLabel.setVisibility(View.VISIBLE);
        } else {
            holder.pinnedIndicator.setVisibility(View.GONE);
            holder.pinnedLabel.setVisibility(View.GONE);
        }

        // Load user profile picture using ProfilePictureHelper
        if (comment.getUserId() != null && !comment.getUserId().isEmpty()) {
            ProfilePictureHelper.loadProfilePicture(
                    context,               // Context
                    comment.getUserId(),   // User ID
                    holder.commentUserImage, // ImageView to load the picture into
                    true                   // Apply circle crop
            );
        } else {
            // Set default image if userId is not available
            holder.commentUserImage.setImageResource(R.drawable.user);
        }
    }

    @Override
    public int getItemCount() {
        return commentsList.size();
    }

    /**
     * Sort comments so that pinned comments appear at the top
     */
    public void sortComments() {
        // Sort the comments - pinned first, then by timestamp
        Collections.sort(commentsList, (c1, c2) -> {
            // First sort by pinned status
            if (c1.isPinned() && !c2.isPinned()) return -1;
            if (!c1.isPinned() && c2.isPinned()) return 1;

            // Then sort by timestamp (older first)
            if (c1.getTimestamp() == null && c2.getTimestamp() == null) return 0;
            if (c1.getTimestamp() == null) return 1;
            if (c2.getTimestamp() == null) return -1;
            return c1.getTimestamp().compareTo(c2.getTimestamp());
        });

        notifyDataSetChanged();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView commentUserName, commentText, commentTimestamp, pinnedLabel;
        ImageView commentUserImage;
        View pinnedIndicator;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            commentUserName = itemView.findViewById(R.id.commentUserName);
            commentText = itemView.findViewById(R.id.commentText);
            commentTimestamp = itemView.findViewById(R.id.commentTimestamp);
            pinnedIndicator = itemView.findViewById(R.id.pinnedIndicator);
            pinnedLabel = itemView.findViewById(R.id.pinnedLabel);
            commentUserImage = itemView.findViewById(R.id.commentUserImage);
        }
    }
}