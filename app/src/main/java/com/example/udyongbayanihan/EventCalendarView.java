package com.example.udyongbayanihan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.CalendarView;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom CalendarView that displays event indicators
 */
public class EventCalendarView extends CalendarView {

    private Map<String, Boolean> eventDates = new HashMap<>();
    private Paint eventIndicatorPaint;
    private int indicatorColor = Color.GREEN; // Default color

    public EventCalendarView(Context context) {
        super(context);
        init();
    }

    public EventCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EventCalendarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        eventIndicatorPaint = new Paint();
        eventIndicatorPaint.setColor(indicatorColor);
        eventIndicatorPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * Set the color for event indicators
     * @param color the color to use
     */
    public void setIndicatorColor(int color) {
        this.indicatorColor = color;
        eventIndicatorPaint.setColor(color);
        invalidate();
    }

    /**
     * Add dates that have events
     * @param dates List of dates with events
     */
    public void addEventDates(List<Date> dates) {
        Calendar calendar = Calendar.getInstance();
        for (Date date : dates) {
            calendar.setTime(date);
            String key = formatDateKey(calendar);
            eventDates.put(key, true);
        }
        invalidate();
    }

    /**
     * Format a date as a key string
     */
    private String formatDateKey(Calendar calendar) {
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return String.format("%04d-%02d-%02d", year, month + 1, day);
    }

    /**
     * Check if a date has events
     */
    public boolean hasEvent(Calendar calendar) {
        String key = formatDateKey(calendar);
        return eventDates.containsKey(key) && eventDates.get(key);
    }

    /**
     * Check if a date has events
     */
    public boolean hasEvent(int year, int month, int day) {
        String key = String.format("%04d-%02d-%02d", year, month + 1, day);
        return eventDates.containsKey(key) && eventDates.get(key);
    }
}