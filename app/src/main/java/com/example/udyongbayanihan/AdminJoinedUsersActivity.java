package com.example.udyongbayanihan;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminJoinedUsersActivity extends AppCompatActivity implements AdminJoinedSkillsAdapter.OnSkillClickListener {

    private static final String TAG = "AdminJoinedUsersActivity";

    private FirebaseFirestore firestore;
    private RecyclerView usersRecyclerView;
    private RecyclerView skillsRecyclerView;
    private JoinedUsersAdapter usersAdapter;
    private AdminJoinedSkillsAdapter skillsAdapter;
    private List<JoinedUser> allJoinedUsersList;
    private List<JoinedUser> filteredUsersList;
    private TextView eventTitle;
    private TextView usersHeaderText;
    private TextView eventSkillsLabel;

    private String eventName;
    private String eventId;
    private String eventBarangay;
    private ArrayList<String> eventSkills;
    private Map<String, Integer> skillCounts;
    private Map<String, List<JoinedUser>> skillToUsersMap;

    private String currentlySelectedSkill = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_joined_users);

        eventTitle = findViewById(R.id.joinedEventTitle);
        eventSkillsLabel = findViewById(R.id.eventSkillsLabel);
        usersHeaderText = findViewById(R.id.usersHeaderText);
        usersRecyclerView = findViewById(R.id.joinedUsersRecyclerView);
        skillsRecyclerView = findViewById(R.id.skillsRecyclerView);

        // Get data from intent
        eventName = getIntent().getStringExtra("eventName");
        eventId = getIntent().getStringExtra("eventId");
        eventBarangay = getIntent().getStringExtra("eventBarangay");
        eventSkills = getIntent().getStringArrayListExtra("eventSkills");

        // Set event name and barangay
        eventTitle.setText(eventName);
        usersHeaderText.setText("Joined Users");

        firestore = FirebaseFirestore.getInstance();

        // Initialize lists and maps
        allJoinedUsersList = new ArrayList<>();
        filteredUsersList = new ArrayList<>();
        skillCounts = new HashMap<>();
        skillToUsersMap = new HashMap<>();

        // Initialize the event skills list if it's null
        if (eventSkills == null) {
            eventSkills = new ArrayList<>();
            eventSkillsLabel.setText("Skills Required: None");
        }

        // Set up the users recycler view
        usersAdapter = new JoinedUsersAdapter(filteredUsersList);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        usersRecyclerView.setAdapter(usersAdapter);

        // Set up the skills recycler view
        skillsAdapter = new AdminJoinedSkillsAdapter(this, eventSkills, skillCounts, this);
        skillsRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        skillsRecyclerView.setAdapter(skillsAdapter);

        // Fetch joined users
        fetchJoinedUsers(eventName);
    }

    private void fetchJoinedUsers(String eventName) {
        firestore.collection("UserJoinEvents")
                .whereEqualTo("eventName", eventName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No users found for event: " + eventName);
                        Toast.makeText(this, "No users joined this event.", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d(TAG, "Found users for event: " + eventName);
                        allJoinedUsersList.clear(); // Clear the list to avoid duplicates

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String userId = document.getString("userId");
                            Log.d(TAG, "Fetching user details for userId: " + userId);

                            if (userId != null) {
                                fetchUserDetails(userId);
                            } else {
                                Log.w(TAG, "userId is null for document: " + document.getId());
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching joined users: " + e.getMessage());
                    Toast.makeText(this, "Failed to load joined users.", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchUserDetails(String userId) {
        // Query usersName collection by userId field
        Task<QuerySnapshot> nameTask = firestore.collection("usersName")
                .whereEqualTo("userId", userId)
                .limit(1) // Limit to one result since userId is supposed to be unique
                .get();

        // Query usersAddress collection by userId field
        Task<QuerySnapshot> addressTask = firestore.collection("usersAddress")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get();

        // Query usersOtherDetails collection by userId field
        Task<QuerySnapshot> detailsTask = firestore.collection("usersOtherDetails")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get();

        // Combine all tasks
        Tasks.whenAllSuccess(nameTask, addressTask, detailsTask)
                .addOnSuccessListener(results -> {
                    QuerySnapshot nameSnapshot = nameTask.getResult();
                    QuerySnapshot addressSnapshot = addressTask.getResult();
                    QuerySnapshot detailsSnapshot = detailsTask.getResult();

                    // Make sure there is at least one document in each query result
                    if (!nameSnapshot.isEmpty() && !addressSnapshot.isEmpty() && !detailsSnapshot.isEmpty()) {
                        // Get the first document from each query result
                        DocumentSnapshot nameDoc = nameSnapshot.getDocuments().get(0);
                        DocumentSnapshot addressDoc = addressSnapshot.getDocuments().get(0);
                        DocumentSnapshot detailsDoc = detailsSnapshot.getDocuments().get(0);

                        // Create the JoinedUser object
                        JoinedUser user = new JoinedUser();
                        user.setFirstName(nameDoc.getString("firstName"));
                        user.setLastName(nameDoc.getString("lastName"));

                        // Fetch additional details if they exist
                        if (detailsDoc.exists()) {
                            user.setAge(detailsDoc.getLong("age"));
                            user.setGender(detailsDoc.getString("gender"));

                            // Fetch skills array if it exists
                            List<String> skills = (List<String>) detailsDoc.get("skills");
                            if (skills != null) {
                                user.setSkills(skills);
                            }
                        }

                        if (addressDoc.exists()) {
                            user.setBarangay(addressDoc.getString("barangay"));

                            // Fetch municipality if it exists
                            String municipality = addressDoc.getString("municipality");
                            if (municipality != null) {
                                user.setMunicipality(municipality);
                            }
                        }

                        Log.d(TAG, "User loaded: " + user.getFirstName() + " " + user.getLastName());

                        // Add the user to the main list
                        allJoinedUsersList.add(user);

                        // Also add to filtered list initially
                        filteredUsersList.add(user);

                        // Update the skill counts and maps
                        updateSkillCounts(user);

                        // Notify the adapter that data has changed
                        usersAdapter.notifyDataSetChanged();
                        skillsAdapter.notifyDataSetChanged();
                    } else {
                        Log.w(TAG, "No data found for userId: " + userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user details: " + e.getMessage());
                });
    }

    private void updateSkillCounts(JoinedUser user) {
        List<String> userSkills = user.getSkills();
        String userBarangay = user.getBarangay();

        // Check if user is from the event's barangay
        boolean isFromEventBarangay = eventBarangay != null && eventBarangay.equals(userBarangay);

        // First, handle the barangay as a special "skill" for filtering
        if (isFromEventBarangay) {
            String barangayKey = "From " + eventBarangay;

            // If not already in eventSkills, add it
            if (!eventSkills.contains(barangayKey)) {
                eventSkills.add(barangayKey);
                skillsAdapter.notifyDataSetChanged();
            }

            // Increment count and add to map
            skillCounts.put(barangayKey, skillCounts.getOrDefault(barangayKey, 0) + 1);

            List<JoinedUser> usersFromBarangay = skillToUsersMap.getOrDefault(barangayKey, new ArrayList<>());
            if (!usersFromBarangay.contains(user)) {
                usersFromBarangay.add(user);
                skillToUsersMap.put(barangayKey, usersFromBarangay);
            }
        }

        // Now handle the event skills
        if (userSkills != null && eventSkills != null) {
            for (String eventSkill : eventSkills) {
                // Skip the barangay key we added
                if (eventSkill.startsWith("From ")) continue;

                // Check if user has this skill
                if (userSkills.contains(eventSkill)) {
                    // Increment the count for this skill
                    skillCounts.put(eventSkill, skillCounts.getOrDefault(eventSkill, 0) + 1);

                    // Add user to the skill->users map
                    List<JoinedUser> usersWithSkill = skillToUsersMap.getOrDefault(eventSkill, new ArrayList<>());
                    if (!usersWithSkill.contains(user)) {
                        usersWithSkill.add(user);
                        skillToUsersMap.put(eventSkill, usersWithSkill);
                    }
                }
            }
        }
    }

    @Override
    public void onSkillClick(String skill, int position) {
        // If the same skill is clicked again, clear the filter
        if (skill.equals(currentlySelectedSkill)) {
            clearFilter();
            return;
        }

        currentlySelectedSkill = skill;

        // Update header text
        usersHeaderText.setText("Users with " + skill);

        // Filter the list to show only users with this skill
        List<JoinedUser> usersWithSkill = skillToUsersMap.getOrDefault(skill, new ArrayList<>());

        filteredUsersList.clear();
        filteredUsersList.addAll(usersWithSkill);
        usersAdapter.notifyDataSetChanged();
    }

    private void clearFilter() {
        currentlySelectedSkill = null;

        // Reset the header text
        usersHeaderText.setText("Joined Users");

        // Clear skill selection in adapter
        skillsAdapter.clearSelection();

        // Show all users
        filteredUsersList.clear();
        filteredUsersList.addAll(allJoinedUsersList);
        usersAdapter.notifyDataSetChanged();
    }
}