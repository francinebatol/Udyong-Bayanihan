package com.example.udyongbayanihan;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import android.util.Log;

public class AdminPostPagerAdapter extends FragmentStateAdapter {
    private static final String TAG = "AdminPostPagerAdapter";

    private String adminId;
    private String adminName;
    private String barangay;

    // Add variables to track refresh state
    private long[] itemIds = {0, 1}; // Base IDs for the two tabs
    private Fragment[] fragmentCache = new Fragment[2];

    public AdminPostPagerAdapter(@NonNull FragmentActivity fragmentActivity,
                                 String adminId, String adminName, String barangay) {
        super(fragmentActivity);
        this.adminId = adminId;
        this.adminName = adminName;
        this.barangay = barangay;

        Log.d(TAG, "AdminPostPagerAdapter created");
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Log.d(TAG, "createFragment called for position: " + position + " with itemId: " + itemIds[position]);

        Fragment fragment;

        // First check if we have a cached fragment for this position
        if (fragmentCache[position] != null) {
            Log.d(TAG, "Returning cached fragment for position: " + position);
            return fragmentCache[position];
        }

        // Create a new fragment
        if (position == 0) {
            // First tab - Barangay posts
            fragment = AdminBarangayPostsFragment.newInstance(adminId, adminName, barangay);
        } else {
            // Second tab - Skills posts
            fragment = AdminSkillsPostsFragment.newInstance(adminId, adminName);
        }

        // Cache the fragment
        fragmentCache[position] = fragment;

        return fragment;
    }

    @Override
    public int getItemCount() {
        return 2; // We have 2 tabs
    }

    // Override getItemId to return a dynamic ID that changes when refresh is needed
    @Override
    public long getItemId(int position) {
        return itemIds[position];
    }

    // Override containsItem to correctly handle our custom item IDs
    @Override
    public boolean containsItem(long itemId) {
        // Check if the itemId exists in our itemIds array
        return itemId == itemIds[0] || itemId == itemIds[1];
    }

    /**
     * Refresh a specific tab
     * @param position Position of the tab to refresh (0 for Barangay, 1 for Skills)
     */
    public void refreshItem(int position) {
        if (position >= 0 && position < itemIds.length) {
            // Clear the cached fragment
            fragmentCache[position] = null;

            // Increment the ID to force ViewPager2 to recreate the fragment
            itemIds[position]++;

            Log.d(TAG, "Refreshing item at position: " + position + " with new itemId: " + itemIds[position]);

            // Notify the adapter that this item has changed
            notifyItemChanged(position);
        }
    }

    /**
     * Force refresh of all fragments in the ViewPager
     */
    public void refreshAllItems() {
        // Clear all cached fragments
        fragmentCache[0] = null;
        fragmentCache[1] = null;

        // Increment all IDs to force recreation of all fragments
        for (int i = 0; i < itemIds.length; i++) {
            itemIds[i]++;
            Log.d(TAG, "Refreshing all items, position " + i + " new itemId: " + itemIds[i]);
        }

        // Notify the adapter that data has changed
        notifyDataSetChanged();
    }
}