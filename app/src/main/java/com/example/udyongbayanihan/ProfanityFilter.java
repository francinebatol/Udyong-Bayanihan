package com.example.udyongbayanihan;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfanityFilter {
    private static final Set<String> BAD_WORDS = new HashSet<>(Arrays.asList(
            "bitch", "shit", "fuck", "ass", "damn", "cunt", "bastard", "whore",
            "putangina", "putang ina", "tangina", "tanga", "bobo", "tarantado", "gago", "bastardo", "puta",
            "punyeta", "leche", "siraulo", "sira ulo", "kupal", "ulol", "gaga", "putragis", "buang",
            "lintik", "batugan", "potek", "shet", "burat", "hinayupak", "ungas", "pucha", "yawa",
            "pakyu", "puke", "jerk", "batukan", "dakugan", "gahasain", "gulpihin", "kurutin", "patayin",
            "patayan", "pilayan", "pilayin", "sakalin", "saksakin", "saktan", "sipain", "sikmuraan", "gulpihin", "kurutin", "patayin",
            "bully", "bullying", "butt", "kick", "kidnap", "kill", "rape", "raping",
            "stab", "slave", "master", "nigga", "pussy", "dick", "kiffy", "kipay",
            "cum", "tamod", "yank", "negro", "chekwa", "tsekwa", "ching chong", "titi",
            "tite", "burat", "sex", "boombayah", "kantot", "moron", "slut", "puta"
            ));

    public static String filterProfanity(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // First, detect and handle spaced-out bad words (like "b i t c h")
        String normalizedText = text;
        for (String badWord : BAD_WORDS) {
            // Create a pattern to match spaced-out versions of bad words
            StringBuilder spacedPattern = new StringBuilder();
            for (int i = 0; i < badWord.length(); i++) {
                spacedPattern.append(badWord.charAt(i));
                if (i < badWord.length() - 1) {
                    spacedPattern.append("[\\s.\\-_]*"); // Match spaces, dots, hyphens, underscores
                }
            }

            Pattern pattern = Pattern.compile(spacedPattern.toString(), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(normalizedText);

            StringBuilder tempText = new StringBuilder(normalizedText);
            int offset = 0;

            // Replace every match with asterisks
            while (matcher.find()) {
                int start = matcher.start() + offset;
                int end = matcher.end() + offset;
                String asterisks = "";
                for (int i = 0; i < badWord.length(); i++) {
                    asterisks += "*";
                }

                tempText.replace(start, end, asterisks);
                // Update offset based on replacement length difference
                offset += asterisks.length() - (end - start);
                // Update matcher with the new text
                matcher = pattern.matcher(tempText.toString());
            }

            normalizedText = tempText.toString();
        }

        // Now handle bad words embedded in larger words
        StringBuilder result = new StringBuilder(normalizedText);

        for (String badWord : BAD_WORDS) {
            Pattern pattern = Pattern.compile(badWord, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(result.toString());

            int offset = 0;

            while (matcher.find()) {
                int start = matcher.start() + offset;
                int end = matcher.end() + offset;

                // Generate replacement string (asterisks)
                String replacement = "";
                for (int i = 0; i < end - start; i++) {
                    replacement += "*";
                }

                result.replace(start, end, replacement);
                // Adjust offset for future replacements
                offset += replacement.length() - (end - start);
                // Update matcher with the new text
                matcher = pattern.matcher(result.toString());
            }
        }

        return result.toString();
    }
}