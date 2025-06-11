package com.example.udyongbayanihan;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CommunityGroupAdapter extends RecyclerView.Adapter<CommunityGroupAdapter.ViewHolder> {

    private Context context;
    private List<CommunityGroupModel> groupList;
    private String userId, uaddressId, unameId, uotherDetails;

    public CommunityGroupAdapter(Context context, List<CommunityGroupModel> groupList,
                                 String userId, String uaddressId, String unameId, String uotherDetails) {
        this.context = context;
        this.groupList = groupList;
        this.userId = userId;
        this.uaddressId = uaddressId;
        this.unameId = unameId;
        this.uotherDetails = uotherDetails;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_community_group, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        CommunityGroupModel group = groupList.get(position);
        holder.textView2.setText(group.getSkillName());  // Set the skill name to the TextView

        // Set the button click listener
        holder.btnEnterGroup.setOnClickListener(v -> {
            Log.d("CommunityGroupAdapter", "Entering skill group: " + group.getSkillName());
            Log.d("CommunityGroupAdapter", "unameId: " + unameId); // Add logging

            Intent intent = new Intent(context, SkillsBasedEvents.class);
            intent.putExtra("skillName", group.getSkillName());
            intent.putExtra("userId", userId);
            intent.putExtra("unameId", unameId);  // Critical parameter for comments functionality

            // Optional extras
            intent.putExtra("uaddressId", uaddressId);
            intent.putExtra("uotherDetails", uotherDetails);

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView2;
        ImageButton btnEnterGroup;

        public ViewHolder(View itemView) {
            super(itemView);
            textView2 = itemView.findViewById(R.id.textView2);
            btnEnterGroup = itemView.findViewById(R.id.btnEnterGroup);
        }
    }
}