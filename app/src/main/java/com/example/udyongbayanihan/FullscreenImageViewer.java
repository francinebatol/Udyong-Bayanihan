package com.example.udyongbayanihan;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class FullscreenImageViewer extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TextView tvImageCounter;
    private ImageButton btnClose;
    private ArrayList<String> imageUrls;
    private int initialPosition;
    private int totalImages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image_viewer);

        // Find views
        viewPager = findViewById(R.id.viewPager);
        tvImageCounter = findViewById(R.id.tvImageCounter);
        btnClose = findViewById(R.id.btnClose);

        // Get data from intent
        imageUrls = getIntent().getStringArrayListExtra("imageUrls");
        initialPosition = getIntent().getIntExtra("position", 0);
        totalImages = imageUrls != null ? imageUrls.size() : 0;

        // Set up the adapter
        FullscreenImageAdapter adapter = new FullscreenImageAdapter(this, imageUrls);
        viewPager.setAdapter(adapter);

        // Set initial position
        viewPager.setCurrentItem(initialPosition, false);
        updateImageCounter(initialPosition);

        // Set up page change listener
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateImageCounter(position);
            }
        });

        // Set close button click listener
        btnClose.setOnClickListener(v -> finish());

        // Hide system UI for immersive experience
        hideSystemUI();
    }

    private void updateImageCounter(int position) {
        tvImageCounter.setText((position + 1) + "/" + totalImages);
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }
}