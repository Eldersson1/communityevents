package com.Eldersson.communityevents;

import org.bukkit.Location;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class GameEvent {
    private String name;
    private String dateString;
    private String timeString;
    private int duration; // in minutes
    private Location location;
    private long startTime;
    private long endTime;

    public GameEvent(String name, String dateString, String timeString, int duration, Location location) {
        this.name = name;
        this.dateString = dateString;
        this.timeString = timeString;
        this.duration = duration;
        this.location = location;
        calculateTimes();
    }

    private void calculateTimes() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.UK);
            sdf.setTimeZone(TimeZone.getTimeZone("Europe/London")); // BST timezone
            Date startDate = sdf.parse(dateString + " " + timeString);
            this.startTime = startDate.getTime();
            this.endTime = this.startTime + (duration * 60 * 1000L); // Convert minutes to milliseconds
        } catch (ParseException e) {
            // Fallback to current time if parsing fails
            this.startTime = System.currentTimeMillis();
            this.endTime = this.startTime + (duration * 60 * 1000L);
        }
    }

    public boolean isActive() {
        long currentTime = System.currentTimeMillis();
        return currentTime >= startTime && currentTime <= endTime;
    }

    public boolean hasStarted() {
        return System.currentTimeMillis() >= startTime;
    }

    public boolean hasEnded() {
        return System.currentTimeMillis() > endTime;
    }

    public long getTimeUntilStart() {
        return Math.max(0, startTime - System.currentTimeMillis());
    }

    public long getTimeUntilEnd() {
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getDateString() {
        return dateString;
    }

    public String getTimeString() {
        return timeString;
    }

    public int getDuration() {
        return duration;
    }

    public Location getLocation() {
        return location;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setDateString(String dateString) {
        this.dateString = dateString;
        calculateTimes();
    }

    public void setTimeString(String timeString) {
        this.timeString = timeString;
        calculateTimes();
    }

    public void setDuration(int duration) {
        this.duration = duration;
        calculateTimes();
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public String toString() {
        return "GameEvent{" +
                "name='" + name + '\'' +
                ", date='" + dateString + '\'' +
                ", time='" + timeString + '\'' +
                ", duration=" + duration +
                ", location=" + location +
                '}';
    }
}