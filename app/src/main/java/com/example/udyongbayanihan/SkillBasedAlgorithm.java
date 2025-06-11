package com.example.udyongbayanihan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SkillBasedAlgorithm {

    // Sort events based on matching skills with the user's skills
    public static void sortEventsBySkills(ArrayList<Post> events, List<String> userSkills) {
        // Sort events by matching skills in descending order
        Collections.sort(events, (event1, event2) -> {
            int matchCount1 = countMatchingSkills(event1.getSkills(), userSkills);
            int matchCount2 = countMatchingSkills(event2.getSkills(), userSkills);
            return Integer.compare(matchCount2, matchCount1); // Descending order
        });
    }

    // Count how many skills match between the event and the user's skills
    private static int countMatchingSkills(List<String> eventSkills, List<String> userSkills) {
        if (eventSkills == null || userSkills == null) return 0;

        int matchCount = 0;
        for (String eventSkill : eventSkills) {
            if (userSkills.contains(eventSkill)) {
                matchCount++;
            }
        }
        return matchCount;
    }
}
