package com.example.udyongbayanihan;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

public class LikesAdapter extends RecyclerView.Adapter<LikesAdapter.LikeViewHolder> {
    private Context context;
    private List<LikeUser> likeUsers;

    public LikesAdapter(Context context, List<LikeUser> likeUsers) {
        this.context = context;
        this.likeUsers = likeUsers;
    }

    @NonNull
    @Override
    public LikeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_like_user, parent, false);
        return new LikeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LikeViewHolder holder, int position) {
        LikeUser likeUser = likeUsers.get(position);
        holder.userName.setText(likeUser.getFullName());

        // Load profile image with circular transformation
        if (likeUser.getProfilePictureUrl() != null && !likeUser.getProfilePictureUrl().isEmpty()) {
            Glide.with(context)
                    .load(likeUser.getProfilePictureUrl())
                    .apply(RequestOptions.circleCropTransform())  // Apply circular transformation
                    .placeholder(R.drawable.user2)
                    .error(R.drawable.user2)
                    .into(holder.userProfileImage);
        } else {
            // Default image if no profile picture URL is available
            holder.userProfileImage.setImageResource(R.drawable.user2);
        }
    }

    @Override
    public int getItemCount() {
        return likeUsers.size();
    }

    static class LikeViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        ImageView userProfileImage;

        LikeViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.userName);
            userProfileImage = itemView.findViewById(R.id.userProfileImage);
        }
    }
}