package com.example.udyongbayanihan;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllSkillGroupsAdapter extends RecyclerView.Adapter<AllSkillGroupsAdapter.ViewHolder> {
    private static final String TAG = "AllSkillGroupsAdapter";
    private Context context;
    private List<SkillGroupModel> skillGroups;
    private String userId;
    private FirebaseFirestore db;

    public AllSkillGroupsAdapter(Context context, List<SkillGroupModel> skillGroups, String userId) {
        this.context = context;
        this.skillGroups = skillGroups;
        this.userId = userId;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_all_skill_groups, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SkillGroupModel group = skillGroups.get(position);
        holder.textSkillName.setText(group.getSkillName());

        if (group.getUserHasSkill()) {
            holder.btnRequestJoin.setText("Already Member");
            holder.btnRequestJoin.setEnabled(false);
        } else {
            checkRequestStatus(holder, group);
        }

        holder.btnRequestJoin.setOnClickListener(v -> {
            if (!group.getUserHasSkill() && (group.getRequestStatus().equals("NONE") || group.getRequestStatus().equals("REJECTED"))) {
                sendJoinRequest(holder, group);
            }
        });
    }

    private void checkRequestStatus(ViewHolder holder, SkillGroupModel group) {
        db.collection("SkillGroupRequests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("skillName", group.getSkillName())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String status = querySnapshot.getDocuments().get(0).getString("status");
                        group.setRequestStatus(status);
                        updateButtonState(holder, status);

                        // If the status is APPROVED, update the model to reflect that user now has this skill
                        if ("APPROVED".equals(status)) {
                            group.setUserHasSkill(true);
                        }
                    } else {
                        // No existing request found
                        updateButtonState(holder, "NONE");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking request status", e);
                    // Fall back to NONE status on error
                    updateButtonState(holder, "NONE");
                });
    }

    private void sendJoinRequest(ViewHolder holder, SkillGroupModel group) {
        // First check if a request already exists (to prevent duplicates)
        db.collection("SkillGroupRequests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("skillName", group.getSkillName())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // No existing request, create a new one
                        createNewRequest(holder, group);
                    } else {
                        // Update existing request if it was previously rejected
                        String status = querySnapshot.getDocuments().get(0).getString("status");
                        String docId = querySnapshot.getDocuments().get(0).getId();

                        if ("REJECTED".equals(status)) {
                            updateExistingRequest(holder, group, docId);
                        } else {
                            group.setRequestStatus(status);
                            updateButtonState(holder, status);
                            Toast.makeText(context, "You already have a " + status.toLowerCase() + " request", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking existing requests", e);
                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createNewRequest(ViewHolder holder, SkillGroupModel group) {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        request.put("skillName", group.getSkillName());
        request.put("status", "PENDING");
        request.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("SkillGroupRequests")
                .add(request)
                .addOnSuccessListener(documentReference -> {
                    group.setRequestStatus("PENDING");
                    updateButtonState(holder, "PENDING");
                    Toast.makeText(context, "Request sent successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error sending request", e);
                    Toast.makeText(context, "Error sending request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateExistingRequest(ViewHolder holder, SkillGroupModel group, String docId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "PENDING");
        updates.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("SkillGroupRequests")
                .document(docId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    group.setRequestStatus("PENDING");
                    updateButtonState(holder, "PENDING");
                    Toast.makeText(context, "Request updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating request", e);
                    Toast.makeText(context, "Error updating request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateButtonState(ViewHolder holder, String status) {
        switch (status) {
            case "PENDING":
                holder.btnRequestJoin.setText("Request Pending");
                holder.btnRequestJoin.setEnabled(false);
                break;
            case "APPROVED":
                holder.btnRequestJoin.setText("Approved - Member");
                holder.btnRequestJoin.setEnabled(false);
                break;
            case "REJECTED":
                holder.btnRequestJoin.setText("Request to Join");
                holder.btnRequestJoin.setEnabled(true);
                break;
            default:
                holder.btnRequestJoin.setText("Request to Join");
                holder.btnRequestJoin.setEnabled(true);
        }
    }

    @Override
    public int getItemCount() {
        return skillGroups.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textSkillName;
        Button btnRequestJoin;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textSkillName = itemView.findViewById(R.id.textSkillName);
            btnRequestJoin = itemView.findViewById(R.id.btnRequestJoin);
        }
    }
}