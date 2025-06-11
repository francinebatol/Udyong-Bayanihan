package com.example.udyongbayanihan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventSorter {
    public static List<Post> sortEvents(List<Post> events, String userBarangay, List<String> userSkills) {
        if (events == null || events.isEmpty()) return events;

        // Priority score for each event
        for (Post event : events) {
            int priorityScore = calculatePriorityScore(event, userBarangay, userSkills);
            event.setPriorityScore(priorityScore);
        }

        Collections.sort(events, (event1, event2) -> {
            // Higher score should come first
            return Integer.compare(event2.getPriorityScore(), event1.getPriorityScore());
        });

        return events;
    }

    private static int calculatePriorityScore(Post event, String userBarangay, List<String> userSkills) {
        int score = 0;
        boolean isMatchingBarangay = event.getBarangay().equals(userBarangay);
        int matchingSkills = countMatchingSkills(event.getEventSkills(), userSkills);
        int distance = BarangayDistances.getDistance(userBarangay, event.getBarangay());

        // 1. Matching Barangay and multiple Skills
        if (isMatchingBarangay && matchingSkills > 1) {
            score += 1000000;
            // More matching skills = higher score
            score += matchingSkills * 10000;
        }
        // 2. Matching multiple Skills (prioritizing more matching skills)
        else if (matchingSkills > 1) {
            score += 500000;
            // More matching skills = significantly higher score
            score += matchingSkills * 10000;
            // Add a smaller distance bonus within this category
            score += Math.max(0, 5000 - (distance / 10));
        }
        // 3. Matching Barangay and exactly 1 Skill
        else if (isMatchingBarangay && matchingSkills == 1) {
            score += 250000;
        }
        // 4a. Matching Barangay (higher priority than matching skill)
        else if (isMatchingBarangay) {
            score += 150000;
        }
        // 4b. Matching a single Skill
        else if (matchingSkills == 1) {
            score += 100000;
            // Add distance bonus for events with matching skill
            score += Math.max(0, 5000 - (distance / 10));
        }
        // 5. Near Barangay (events in barangays close to user's barangay)
        else if (distance < 3000) {
            score += 50000;
            // Closer barangays get higher scores
            score += 3000 - distance;
        }
        // 6. Distant Barangay based on distance
        else {
            score += 10000;
            // Gradually decreasing score as distance increases
            score += Math.max(0, 10000 - (distance / 20));
        }

        return score;
    }

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