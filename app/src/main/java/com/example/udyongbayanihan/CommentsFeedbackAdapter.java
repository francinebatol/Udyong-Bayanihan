package com.example.udyongbayanihan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CommentsFeedbackAdapter extends RecyclerView.Adapter<CommentsFeedbackAdapter.ViewHolder> {
    private List<String> commentsList;

    public CommentsFeedbackAdapter(List<String> commentsList) {
        this.commentsList = commentsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment_feedback, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String comment = commentsList.get(position);

        // Handle different comment types differently
        if (comment.startsWith("Role:")) {
            holder.commentText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.dark_green, null));
            holder.commentText.setTextSize(16);
        }
        else if (comment.startsWith("Improvements:")) {
            holder.commentText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.colorPrimary, null));
        }
        else if (comment.startsWith("Willingness to volunteer again:")) {
            holder.commentText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.green, null));
        }
        else if (comment.startsWith("Additional:")) {
            holder.commentText.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.colorAccent, null));
        }

        holder.commentText.setText(comment);
    }

    @Override
    public int getItemCount() {
        return commentsList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView commentText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            commentText = itemView.findViewById(R.id.commentText);
        }
    }
}