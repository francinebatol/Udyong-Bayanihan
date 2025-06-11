package com.example.udyongbayanihan;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.util.Log;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class JoinedUsersAdapter extends RecyclerView.Adapter<JoinedUsersAdapter.ViewHolder> {

    private List<JoinedUser> joinedUsersList;

    public JoinedUsersAdapter(List<JoinedUser> joinedUsersList) {
        this.joinedUsersList = joinedUsersList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.joined_user_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        JoinedUser user = joinedUsersList.get(position);
        holder.firstName.setText(user.getFirstName() != null ? user.getFirstName() : "Unknown");
        holder.lastName.setText(user.getLastName() != null ? user.getLastName() : "Unknown");

        // Set click listener for the RelativeLayout inside each item
        holder.clickJoinedUser.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), AdminJoinedUsersDetails.class);
            intent.putExtra("firstName", user.getFirstName());
            intent.putExtra("middleName", user.getMiddleName());
            intent.putExtra("lastName", user.getLastName());
            intent.putExtra("age", user.getAge() != null ? user.getAge() : -1);
            intent.putExtra("gender", user.getGender());
            intent.putExtra("barangay", user.getBarangay());
            intent.putExtra("municipality", user.getMunicipality());
            intent.putStringArrayListExtra("skills", user.getSkills() != null ? new ArrayList<>(user.getSkills()) : new ArrayList<>());

            Log.d("IntentData", "Passing data: " + intent.getExtras());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return joinedUsersList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView firstName, lastName, age, gender, barangay, municipality, otherBarangay, skills;
        RelativeLayout clickJoinedUser; // Reference to the RelativeLayout inside each item

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            firstName = itemView.findViewById(R.id.joinedFirstName);
            lastName = itemView.findViewById(R.id.joinedLastName);
            clickJoinedUser = itemView.findViewById(R.id.clickJoinedUser);
        }
    }
}

