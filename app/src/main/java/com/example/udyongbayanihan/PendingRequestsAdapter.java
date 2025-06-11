package com.example.udyongbayanihan;

import android.app.AlertDialog;
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

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PendingRequestsAdapter extends RecyclerView.Adapter<PendingRequestsAdapter.ViewHolder> {
    private static final String TAG = "PendingRequestsAdapter";
    private Context context;
    private List<PendingRequestModel> requests;
    private FirebaseFirestore db;
    private RefreshCallback refreshCallback;
    private NotificationHelper notificationHelper;

    public interface RefreshCallback {
        void onRefresh();
    }

    public PendingRequestsAdapter(Context context, List<PendingRequestModel> requests, RefreshCallback refreshCallback) {
        this.context = context;
        this.requests = requests;
        this.refreshCallback = refreshCallback;
        this.db = FirebaseFirestore.getInstance();
        this.notificationHelper = new NotificationHelper(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_pending_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PendingRequestModel request = requests.get(position);

        holder.textUsername.setText(request.getUsername());
        holder.textEmail.setText(request.getEmail());
        holder.textSkill.setText("Skill: " + request.getSkillName());

        holder.btnAccept.setOnClickListener(v -> acceptRequest(request));
        holder.btnReject.setOnClickListener(v -> rejectRequest(request));
    }

    private void acceptRequest(PendingRequestModel request) {
        // Update request status to APPROVED
        db.collection("SkillGroupRequests")
                .document(request.getRequestId())
                .update("status", "APPROVED")
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Request approved successfully");

                    // Add the skill to the user's skills array
                    db.collection("usersOtherDetails")
                            .whereEqualTo("userId", request.getUserId())
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    String docId = queryDocumentSnapshots.getDocuments().get(0).getId();

                                    db.collection("usersOtherDetails")
                                            .document(docId)
                                            .update("skills", FieldValue.arrayUnion(request.getSkillName()))
                                            .addOnSuccessListener(aVoid1 -> {
                                                Toast.makeText(context, "Request approved and skill added", Toast.LENGTH_SHORT).show();

                                                // Create notification in Firestore
                                                createSkillGroupNotification(request.getUserId(), request.getSkillName(), "APPROVED");

                                                // Send system notification
                                                notificationHelper.showSkillGroupNotification(
                                                        request.getUserId(),
                                                        request.getSkillName(),
                                                        "APPROVED");

                                                refreshCallback.onRefresh();
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "Error adding skill to user", e);
                                                Toast.makeText(context, "Error adding skill to user", Toast.LENGTH_SHORT).show();
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error finding user details", e);
                                Toast.makeText(context, "Error finding user details", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error approving request", e);
                    Toast.makeText(context, "Error approving request", Toast.LENGTH_SHORT).show();
                });
    }

    private void rejectRequest(PendingRequestModel request) {
        // Show confirmation dialog before rejecting
        new AlertDialog.Builder(context)
                .setTitle("Confirm Rejection")
                .setMessage("Are you sure you want to reject the request from " + request.getUsername() + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // User confirmed, proceed with rejection
                    db.collection("SkillGroupRequests")
                            .document(request.getRequestId())
                            .update("status", "REJECTED")
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(context, "Request rejected", Toast.LENGTH_SHORT).show();

                                // Create notification in Firestore
                                createSkillGroupNotification(request.getUserId(), request.getSkillName(), "REJECTED");

                                // Send system notification
                                notificationHelper.showSkillGroupNotification(
                                        request.getUserId(),
                                        request.getSkillName(),
                                        "REJECTED");

                                refreshCallback.onRefresh();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error rejecting request", e);
                                Toast.makeText(context, "Error rejecting request", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // User canceled, do nothing
                    dialog.dismiss();
                })
                .setCancelable(false) // Prevent dismissing by tapping outside
                .show();
    }

    private void createSkillGroupNotification(String userId, String skillName, String requestStatus) {
        // Create a notification object using the factory method
        Notification notification = Notification.createSkillGroupNotification(userId, skillName, requestStatus);

        // Store it in Firestore
        db.collection("Notifications")
                .document(notification.getId())
                .set(notification)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Skill group notification created for user: " + userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating skill group notification", e);
                });
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textUsername, textEmail, textSkill;
        Button btnAccept, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textUsername = itemView.findViewById(R.id.textUsername);
            textEmail = itemView.findViewById(R.id.textEmail);
            textSkill = itemView.findViewById(R.id.textSkill);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}