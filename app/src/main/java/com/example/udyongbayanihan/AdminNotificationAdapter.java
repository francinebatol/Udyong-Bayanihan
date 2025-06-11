package com.example.udyongbayanihan;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminNotificationAdapter extends RecyclerView.Adapter<AdminNotificationAdapter.ViewHolder> {
    private List<AdminNotificationItem> notifications = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AdminNotificationItem item = notifications.get(position);

        holder.eventNameText.setText("Event: " + item.getEventName());
        holder.statusText.setText("Status: " + item.getStatus());

        // Show feedback for rejected events
        if ("Rejected".equals(item.getStatus()) && item.getFeedback() != null && !item.getFeedback().isEmpty()) {
            holder.feedbackText.setVisibility(View.VISIBLE);
            holder.feedbackText.setText("Reason: " + item.getFeedback());
        } else {
            holder.feedbackText.setVisibility(View.GONE);
        }

        // Format the timestamp
        Date date = new Date(item.getTimestamp() * 1000);
        holder.timestampText.setText(dateFormat.format(date));

        // Set color based on status
        int color = item.getStatus().equals("Accepted") ?
                holder.itemView.getContext().getColor(R.color.status_accepted) :
                holder.itemView.getContext().getColor(R.color.status_rejected);
        holder.statusText.setTextColor(color);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void setNotifications(List<AdminNotificationItem> newNotifications) {
        this.notifications = new ArrayList<>(newNotifications); // Create a new list
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView eventNameText;
        final TextView statusText;
        final TextView feedbackText;
        final TextView timestampText;

        ViewHolder(View itemView) {
            super(itemView);
            eventNameText = itemView.findViewById(R.id.eventNameText);
            statusText = itemView.findViewById(R.id.statusText);
            feedbackText = itemView.findViewById(R.id.feedbackText);
            timestampText = itemView.findViewById(R.id.timestampText);
        }
    }
}