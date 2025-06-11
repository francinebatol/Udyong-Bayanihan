package com.example.udyongbayanihan;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<UserModel> users;
    private String currentUserId, currentUserType;
    private FirebaseFirestore db;
    private static final String TAG = "UserAdapter";

    public UserAdapter(List<UserModel> users, String currentUserId, String currentUserType) {
        this.users = users;
        this.currentUserId = currentUserId;
        this.currentUserType = currentUserType;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = users.get(position);

        // For admin users, fetch and display barangay and position information
        if ("admin".equals(user.getUserType())) {
            fetchAdminDetails(user, holder);
        } else {
            // For regular users, just display their name
            holder.userName.setText(user.getFullName());
        }

        holder.itemView.setOnClickListener(v -> startChat(v, user));
    }

    private void fetchAdminDetails(UserModel user, UserViewHolder holder) {
        // Temporarily set the basic name while fetching details
        holder.userName.setText(user.getFullName());

        AtomicReference<String> barangay = new AtomicReference<>("");
        AtomicReference<String> position = new AtomicReference<>("");
        AtomicBoolean hasBarangay = new AtomicBoolean(false);
        AtomicBoolean hasPosition = new AtomicBoolean(false);

        // Fetch barangay from AMAddressDetails
        db.collection("AMAddressDetails")
                .whereEqualTo("amAccountId", user.getId())
                .limit(1)
                .get()
                .addOnSuccessListener(barangayDocs -> {
                    if (!barangayDocs.isEmpty()) {
                        String foundBarangay = barangayDocs.getDocuments().get(0).getString("amBarangay");
                        if (foundBarangay != null && !foundBarangay.isEmpty()) {
                            barangay.set(foundBarangay);
                            hasBarangay.set(true);
                        }
                    }

                    // Fetch position from AMOtherDetails
                    db.collection("AMOtherDetails")
                            .whereEqualTo("amAccountid", user.getId())
                            .limit(1)
                            .get()
                            .addOnSuccessListener(positionDocs -> {
                                if (!positionDocs.isEmpty()) {
                                    String foundPosition = positionDocs.getDocuments().get(0).getString("position");
                                    if (foundPosition != null && !foundPosition.isEmpty()) {
                                        position.set(foundPosition);
                                        hasPosition.set(true);
                                    }
                                }

                                // Now construct the display name with both details
                                updateDisplayName(holder, user.getFullName(), barangay.get(), position.get(),
                                        hasBarangay.get(), hasPosition.get());
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error fetching admin position: " + e.getMessage());
                                // Update with just barangay if that was found
                                updateDisplayName(holder, user.getFullName(), barangay.get(), "",
                                        hasBarangay.get(), false);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching admin barangay: " + e.getMessage());

                    // Try to at least get the position
                    db.collection("AMOtherDetails")
                            .whereEqualTo("amAccountid", user.getId())
                            .limit(1)
                            .get()
                            .addOnSuccessListener(positionDocs -> {
                                if (!positionDocs.isEmpty()) {
                                    String foundPosition = positionDocs.getDocuments().get(0).getString("position");
                                    if (foundPosition != null && !foundPosition.isEmpty()) {
                                        position.set(foundPosition);
                                        hasPosition.set(true);
                                    }
                                }

                                // Update with just position
                                updateDisplayName(holder, user.getFullName(), "", position.get(),
                                        false, hasPosition.get());
                            })
                            .addOnFailureListener(positionError -> {
                                Log.e(TAG, "Error fetching admin position: " + positionError.getMessage());
                                // Fallback to basic admin label
                                holder.userName.setText(user.getFullName() + " (Admin)");
                            });
                });
    }

    private void updateDisplayName(UserViewHolder holder, String fullName, String barangay,
                                   String position, boolean hasBarangay, boolean hasPosition) {
        StringBuilder displayName = new StringBuilder(fullName);
        displayName.append(" (Admin");

        if (hasBarangay || hasPosition) {
            displayName.append(" - ");

            if (hasBarangay) {
                displayName.append(barangay);
                if (hasPosition) {
                    displayName.append(" ");
                }
            }

            if (hasPosition) {
                displayName.append(position);
            }
        }

        displayName.append(")");
        holder.userName.setText(displayName.toString());
    }

    private void startChat(View v, UserModel user) {
        // For admin users, fetch barangay and position before starting chat
        if ("admin".equals(user.getUserType())) {
            AtomicReference<String> barangay = new AtomicReference<>("");
            AtomicReference<String> position = new AtomicReference<>("");
            AtomicBoolean hasBarangay = new AtomicBoolean(false);
            AtomicBoolean hasPosition = new AtomicBoolean(false);

            // Fetch barangay from AMAddressDetails
            db.collection("AMAddressDetails")
                    .whereEqualTo("amAccountId", user.getId())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(barangayDocs -> {
                        if (!barangayDocs.isEmpty()) {
                            String foundBarangay = barangayDocs.getDocuments().get(0).getString("amBarangay");
                            if (foundBarangay != null && !foundBarangay.isEmpty()) {
                                barangay.set(foundBarangay);
                                hasBarangay.set(true);
                            }
                        }

                        // Fetch position from AMOtherDetails
                        db.collection("AMOtherDetails")
                                .whereEqualTo("amAccountid", user.getId())
                                .limit(1)
                                .get()
                                .addOnSuccessListener(positionDocs -> {
                                    if (!positionDocs.isEmpty()) {
                                        String foundPosition = positionDocs.getDocuments().get(0).getString("position");
                                        if (foundPosition != null && !foundPosition.isEmpty()) {
                                            position.set(foundPosition);
                                            hasPosition.set(true);
                                        }
                                    }

                                    // Create display name and start chat
                                    String displayName = createDisplayName(user.getFullName(), barangay.get(),
                                            position.get(), hasBarangay.get(), hasPosition.get());

                                    startChatActivity(v, user, displayName, barangay.get(), position.get());
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error fetching admin position: " + e.getMessage());
                                    String displayName = createDisplayName(user.getFullName(), barangay.get(),
                                            "", hasBarangay.get(), false);
                                    startChatActivity(v, user, displayName, barangay.get(), "");
                                });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching admin barangay: " + e.getMessage());

                        // Try to at least get the position
                        db.collection("AMOtherDetails")
                                .whereEqualTo("amAccountid", user.getId())
                                .limit(1)
                                .get()
                                .addOnSuccessListener(positionDocs -> {
                                    if (!positionDocs.isEmpty()) {
                                        String foundPosition = positionDocs.getDocuments().get(0).getString("position");
                                        if (foundPosition != null && !foundPosition.isEmpty()) {
                                            position.set(foundPosition);
                                            hasPosition.set(true);
                                        }
                                    }

                                    String displayName = createDisplayName(user.getFullName(), "",
                                            position.get(), false, hasPosition.get());

                                    startChatActivity(v, user, displayName, "", position.get());
                                })
                                .addOnFailureListener(positionError -> {
                                    Log.e(TAG, "Error fetching admin position: " + positionError.getMessage());
                                    // Fallback to basic admin label
                                    startChatActivity(v, user, user.getFullName() + " (Admin)", "", "");
                                });
                    });
        } else {
            // Regular user, start chat directly
            startChatActivity(v, user, user.getFullName(), null, null);
        }
    }

    private String createDisplayName(String fullName, String barangay, String position,
                                     boolean hasBarangay, boolean hasPosition) {
        StringBuilder displayName = new StringBuilder(fullName);
        displayName.append(" (Admin");

        if (hasBarangay || hasPosition) {
            displayName.append(" - ");

            if (hasBarangay) {
                displayName.append(barangay);
                if (hasPosition) {
                    displayName.append(" ");
                }
            }

            if (hasPosition) {
                displayName.append(position);
            }
        }

        displayName.append(")");
        return displayName.toString();
    }

    private void startChatActivity(View v, UserModel user, String recipientName, String barangay, String position) {
        Intent intent = new Intent(v.getContext(), Chat.class);
        intent.putExtra("userId", currentUserId);
        intent.putExtra("userType", currentUserType);
        intent.putExtra("recipientId", user.getId());
        intent.putExtra("recipientType", user.getUserType());
        intent.putExtra("recipientName", recipientName);

        // Pass barangay and position if available
        if (barangay != null && !barangay.isEmpty()) {
            intent.putExtra("recipientBarangay", barangay);
        }

        if (position != null && !position.isEmpty()) {
            intent.putExtra("recipientPosition", position);
        }

        v.getContext().startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userName;

        UserViewHolder(View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
        }
    }
}