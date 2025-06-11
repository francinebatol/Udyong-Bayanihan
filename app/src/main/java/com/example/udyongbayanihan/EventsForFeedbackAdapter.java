package com.example.udyongbayanihan;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class EventsForFeedbackAdapter extends RecyclerView.Adapter<EventsForFeedbackAdapter.ViewHolder> {
    private List<EventFeedbackModel> eventsList;

    public EventsForFeedbackAdapter(List<EventFeedbackModel> eventsList) {
        this.eventsList = eventsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_events_for_feedback, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EventFeedbackModel event = eventsList.get(position);
        holder.eventName.setText(event.getNameOfEvent());

        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), AdminEventsFeedbacks.class);
            intent.putExtra("eventId", event.getEventId());
            intent.putExtra("eventName", event.getNameOfEvent());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return eventsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView eventName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            eventName = itemView.findViewById(R.id.eventName);
        }
    }
}