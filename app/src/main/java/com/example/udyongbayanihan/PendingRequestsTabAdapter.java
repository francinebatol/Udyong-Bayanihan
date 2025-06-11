package com.example.udyongbayanihan;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class PendingRequestsTabAdapter extends FragmentStateAdapter {
    private List<String> skills;
    private String adminId;

    public PendingRequestsTabAdapter(@NonNull FragmentActivity fragmentActivity, List<String> skills, String adminId) {
        super(fragmentActivity);
        this.skills = skills;
        this.adminId = adminId;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Create a fragment for each tab
        return PendingRequestsFragment.newInstance(skills.get(position), adminId);
    }

    @Override
    public int getItemCount() {
        return skills.size();
    }
}