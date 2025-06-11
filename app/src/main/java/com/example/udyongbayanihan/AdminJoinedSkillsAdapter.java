package com.example.udyongbayanihan;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class AdminJoinedSkillsAdapter extends RecyclerView.Adapter<AdminJoinedSkillsAdapter.SkillViewHolder> {

    private Context context;
    private List<String> skillsList;
    private Map<String, Integer> skillCounts;
    private int selectedPosition = -1;
    private OnSkillClickListener listener;

    // Interface for click listener
    public interface OnSkillClickListener {
        void onSkillClick(String skill, int position);
    }

    public AdminJoinedSkillsAdapter(Context context, List<String> skillsList, Map<String, Integer> skillCounts, OnSkillClickListener listener) {
        this.context = context;
        this.skillsList = skillsList;
        this.skillCounts = skillCounts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SkillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.skill_item, parent, false);
        return new SkillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SkillViewHolder holder, int position) {
        String skill = skillsList.get(position);
        int count = skillCounts.getOrDefault(skill, 0);

        holder.skillNameText.setText(skill);
        holder.skillCountText.setText(String.valueOf(count));

        // Set the background color based on selection
        if (selectedPosition == position) {
            holder.skillItemLayout.setBackgroundColor(0xFFE0E0FF); // Light blue color for selection
        } else {
            holder.skillItemLayout.setBackgroundColor(0xFFFFFFFF); // White color for unselected
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = position;

            // Update the previously selected item
            if (previousSelected != -1) {
                notifyItemChanged(previousSelected);
            }

            // Update the newly selected item
            notifyItemChanged(selectedPosition);

            // Call the listener
            if (listener != null) {
                listener.onSkillClick(skill, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return skillsList.size();
    }

    // Method to deselect all items
    public void clearSelection() {
        int previousSelected = selectedPosition;
        selectedPosition = -1;
        if (previousSelected != -1) {
            notifyItemChanged(previousSelected);
        }
    }

    static class SkillViewHolder extends RecyclerView.ViewHolder {
        TextView skillNameText, skillCountText;
        LinearLayout skillItemLayout;

        public SkillViewHolder(@NonNull View itemView) {
            super(itemView);
            skillNameText = itemView.findViewById(R.id.skillNameText);
            skillCountText = itemView.findViewById(R.id.skillCountText);
            skillItemLayout = itemView.findViewById(R.id.skillItemLayout);
        }
    }
}