package com.example.udyongbayanihan;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AdminCommentsAdapter extends RecyclerView.Adapter<AdminCommentsAdapter.CommentViewHolder> {
    private static final String TAG = "AdminCommentsAdapter";

    private Context context;
    private List<Comment> comments;
    private SimpleDateFormat dateFormat;
    private FirebaseFirestore db;

    public AdminCommentsAdapter(Context context, List<Comment> comments) {
        this.context = context;
        this.comments = comments;
        this.dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);

        // Set username and comment text
        holder.commentUserName.setText(comment.getFullName());
        holder.commentText.setText(comment.getCommentText());

        // Set timestamp if available
        if (comment.getTimestamp() != null) {
            holder.commentTimestamp.setText(dateFormat.format(comment.getTimestamp().toDate()));
        } else {
            holder.commentTimestamp.setText("");
        }

        // Show pin indicator if comment is pinned
        holder.pinnedIndicator.setVisibility(comment.isPinned() ? View.VISIBLE : View.GONE);

        // Set up options menu
        holder.btnOptions.setOnClickListener(v -> showCommentOptions(holder, comment));
    }

    private void showCommentOptions(CommentViewHolder holder, Comment comment) {
        PopupMenu popupMenu = new PopupMenu(context, holder.btnOptions);
        popupMenu.inflate(R.menu.menu_comment_options);

        // Change menu text based on pinned status
        if (comment.isPinned()) {
            popupMenu.getMenu().findItem(R.id.action_pin_comment).setTitle("Unpin Comment");
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.action_pin_comment) {
                togglePinComment(comment);
                return true;
            } else if (itemId == R.id.action_delete_comment) {
                confirmDeleteComment(comment);
                return true;
            }

            return false;
        });

        popupMenu.show();
    }

    private void togglePinComment(Comment comment) {
        // Toggle pinned status
        boolean newPinnedStatus = !comment.isPinned();
        comment.setPinned(newPinnedStatus);

        // Update in Firestore
        db.collection("CommunityComments")
                .document(comment.getCommentId())
                .update("pinned", newPinnedStatus)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Comment " + (newPinnedStatus ? "pinned" : "unpinned") + " successfully");
                    Toast.makeText(context, "Comment " + (newPinnedStatus ? "pinned" : "unpinned"), Toast.LENGTH_SHORT).show();

                    // Reorder the list - pinned comments at top
                    sortComments();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating comment pinned status", e);
                    comment.setPinned(!newPinnedStatus); // Revert if failed
                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmDeleteComment(Comment comment) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Comment")
                .setMessage("Are you sure you want to delete this comment?")
                .setPositiveButton("Yes", (dialog, which) -> deleteComment(comment))
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteComment(Comment comment) {
        db.collection("CommunityComments")
                .document(comment.getCommentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Comment deleted successfully");
                    Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show();

                    // Remove from our local list as well
                    int position = comments.indexOf(comment);
                    if (position != -1) {
                        comments.remove(position);
                        notifyItemRemoved(position);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting comment", e);
                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void sortComments() {
        // Sort the comments - pinned first, then by timestamp
        Collections.sort(comments, (c1, c2) -> {
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

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView commentUserName, commentText, commentTimestamp;
        ImageButton btnOptions;
        View pinnedIndicator;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            commentUserName = itemView.findViewById(R.id.commentUserName);
            commentText = itemView.findViewById(R.id.commentText);
            commentTimestamp = itemView.findViewById(R.id.commentTimestamp);
            btnOptions = itemView.findViewById(R.id.btnCommentOptions);
            pinnedIndicator = itemView.findViewById(R.id.pinnedIndicator);
        }
    }
}