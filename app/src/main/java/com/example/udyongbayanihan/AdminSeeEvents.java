package com.example.udyongbayanihan;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

public class AdminSeeEvents extends AppCompatActivity {

    private TabLayout tabLayoutAdminEvents;
    private ViewPager viewPagerAdminEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_see_events);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tabLayoutAdminEvents = findViewById(R.id.tabLayoutAdminEvents);
        viewPagerAdminEvents = findViewById(R.id.viewPagerAdminEvents);

        tabLayoutAdminEvents.setupWithViewPager(viewPagerAdminEvents);

        VPAdminEventsAdapter VPAdminEventsAdapter = new VPAdminEventsAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        VPAdminEventsAdapter.addFragment(new FragmentAdminPendingEvents(), "Pending");
        VPAdminEventsAdapter.addFragment(new FragmentAdminAcceptedEvents(), "Accepted");
        VPAdminEventsAdapter.addFragment(new FragmentAdminRejectedEvents(), "Rejected");
        viewPagerAdminEvents.setAdapter(VPAdminEventsAdapter);
    }
}