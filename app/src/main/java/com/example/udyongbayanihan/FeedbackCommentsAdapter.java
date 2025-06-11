package com.example.udyongbayanihan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FeedbackCommentsAdapter extends RecyclerView.Adapter<FeedbackCommentsAdapter.ViewHolder> {
    private List<FeedbackCommentModel> commentsList;

    public FeedbackCommentsAdapter(List<FeedbackCommentModel> commentsList) {
        this.commentsList = commentsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feedback_comment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FeedbackCommentModel comment = commentsList.get(position);

        holder.commentTypeText.setText(comment.getCommentType());
        holder.commentText.setText(comment.getCommentText());

        // Display the user's name (which has now been updated with the real name)
        holder.userText.setText("From: " + comment.getUserIdentifier());

        // Set different colors based on comment type
        if (comment.getCommentType().equals("Improvements")) {
            holder.commentTypeText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.colorPrimary, null));
        }
        else if (comment.getCommentType().equals("Future Volunteering")) {
            holder.commentTypeText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.green, null));
        }
        else if (comment.getCommentType().equals("Additional Comments")) {
            holder.commentTypeText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.colorAccent, null));
        }
    }

    @Override
    public int getItemCount() {
        return commentsList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView commentTypeText;
        TextView commentText;
        TextView userText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            commentTypeText = itemView.findViewById(R.id.commentTypeText);
            commentText = itemView.findViewById(R.id.commentText);
            userText = itemView.findViewById(R.id.userText);
        }
    }
}