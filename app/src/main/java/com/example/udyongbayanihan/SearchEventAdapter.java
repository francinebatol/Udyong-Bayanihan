package com.example.udyongbayanihan;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchEventAdapter extends RecyclerView.Adapter<SearchEventAdapter.ViewHolder> {

    private Context context;
    private List<Post> eventsList;
    private List<Post> filteredList;
    private OnEventClickListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

    public interface OnEventClickListener {
        void onEventClick(Post event);
    }

    public SearchEventAdapter(Context context, List<Post> eventsList, OnEventClickListener listener) {
        this.context = context;
        this.eventsList = eventsList;
        this.filteredList = new ArrayList<>(eventsList);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.search_event_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post event = filteredList.get(position);

        holder.eventName.setText(event.getNameOfEvent());
        holder.eventType.setText(event.getTypeOfEvent());
        holder.eventOrganization.setText(event.getOrganizations());

        if (event.getDate() != null) {
            holder.eventDate.setText(sdf.format(event.getDate().toDate()));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    public void filter(String query) {
        filteredList.clear();

        if (query.isEmpty()) {
            filteredList.addAll(eventsList);
        } else {
            String searchQuery = query.toLowerCase().trim();

            for (Post event : eventsList) {
                if (event.getNameOfEvent().toLowerCase().contains(searchQuery)) {
                    filteredList.add(event);
                }
            }
        }

        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView eventName, eventType, eventOrganization, eventDate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            eventName = itemView.findViewById(R.id.eventName);
            eventType = itemView.findViewById(R.id.eventType);
            eventOrganization = itemView.findViewById(R.id.eventOrganization);
            eventDate = itemView.findViewById(R.id.eventDate);
        }
    }
}