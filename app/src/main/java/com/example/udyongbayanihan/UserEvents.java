package com.example.udyongbayanihan;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class UserEvents extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private String userId, uaddressId, unameId, uotherDetails;
    private ImageButton imgbtnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_events);

        // Get data from intent
        userId = getIntent().getStringExtra("userId");
        uaddressId = getIntent().getStringExtra("uaddressId");
        unameId = getIntent().getStringExtra("unameId");
        uotherDetails = getIntent().getStringExtra("uotherDetails");

        // Initialize UI elements
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        imgbtnBack = findViewById(R.id.imgbtnBack);

        // Set up back button
        imgbtnBack.setOnClickListener(v -> finish());

        // Set up ViewPager with adapter
        EventsPagerAdapter pagerAdapter = new EventsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Ongoing");
                    break;
                case 1:
                    tab.setText("Ended");
                    break;
            }
        }).attach();
    }

    // Adapter for ViewPager2
    private class EventsPagerAdapter extends FragmentStateAdapter {

        public EventsPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Bundle args = new Bundle();
            args.putString("userId", userId);
            args.putString("uaddressId", uaddressId);
            args.putString("unameId", unameId);
            args.putString("uotherDetails", uotherDetails);

            switch (position) {
                case 0:
                    OngoingEventsFragment ongoingFragment = new OngoingEventsFragment();
                    ongoingFragment.setArguments(args);
                    return ongoingFragment;
                case 1:
                    EndedEventsFragment endedFragment = new EndedEventsFragment();
                    endedFragment.setArguments(args);
                    return endedFragment;
                default:
                    throw new IllegalArgumentException("Invalid position: " + position);
            }
        }

        @Override
        public int getItemCount() {
            return 2; // Two tabs
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Forward activity result to the fragments
        if (requestCode == Home.FEEDBACK_REQUEST_CODE && resultCode == RESULT_OK) {
            // Refresh the EndedEventsFragment to reflect the feedback submission
            Fragment fragment = getSupportFragmentManager().findFragmentByTag("f" + 1);
            if (fragment instanceof EndedEventsFragment) {
                ((EndedEventsFragment) fragment).refreshEvents();
            }
        }
    }
}