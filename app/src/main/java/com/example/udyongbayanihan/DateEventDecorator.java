package com.example.udyongbayanihan;

import android.graphics.drawable.Drawable;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class for tracking dates with events
 */
public class DateEventDecorator {
    private final int color;
    private final Set<String> dates;
    private final Calendar calendar;

    /**
     * Constructor
     * @param color the color to use for marking dates with events
     * @param dates list of dates that have events
     */
    public DateEventDecorator(int color, List<Date> dates) {
        this.color = color;
        this.dates = new HashSet<>();
        this.calendar = Calendar.getInstance();

        // Convert Date objects to formatted strings for easier comparison
        for (Date date : dates) {
            calendar.setTime(date);
            String formatted = formatCalendarDay(calendar);
            this.dates.add(formatted);
        }
    }

    /**
     * Check if a specific day has events
     * @param day the day to check
     * @return true if the day has events, false otherwise
     */
    public boolean shouldDecorate(Calendar day) {
        return dates.contains(formatCalendarDay(day));
    }

    /**
     * Format a Calendar day to a consistent string format
     * @param calendar the Calendar instance
     * @return formatted string representation of the date
     */
    private String formatCalendarDay(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // Month is 0-indexed
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return String.format("%04d-%02d-%02d", year, month, day);
    }

    /**
     * Get the color to use for marking dates with events
     * @return the color
     */
    public int getColor() {
        return color;
    }
}