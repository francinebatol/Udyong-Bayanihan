package com.example.udyongbayanihan;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;
import java.util.List;

public class ImagesAdapter extends RecyclerView.Adapter<ImagesAdapter.ImageViewHolder> {
    private Context context;
    private List<String> imageUrls;
    private static final int MAX_IMAGES = 10;

    public ImagesAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        // Limit the number of images to MAX_IMAGES
        this.imageUrls = imageUrls.subList(0, Math.min(imageUrls.size(), MAX_IMAGES));
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.image_item, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);

        // Use Glide to load the image with error handling
        Glide.with(context)
                .load(imageUrl)
                .into(holder.imageView);

        // Set click listener to open fullscreen viewer
        holder.imageView.setOnClickListener(v -> openFullscreenViewer(position));
    }

    private void openFullscreenViewer(int position) {
        Intent intent = new Intent(context, FullscreenImageViewer.class);
        intent.putStringArrayListExtra("imageUrls", new ArrayList<>(imageUrls));
        intent.putExtra("position", position);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}