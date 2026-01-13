package calendarapp;

import java.time.LocalDateTime;

public class Event {
    private int eventId;
    private String title;
    private String description;
    private LocalDateTime start;
    private LocalDateTime end;
    private boolean recurring;
    private String recurrenceType;
    private int recurrenceCount;
    private int seriesId;

    // Full constructor (for normal events)
    public Event(int eventId, String title, String description, LocalDateTime start, LocalDateTime end) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.start = start;
        this.end = end;
    }

    // Minimal constructor (for recurring copies)
    public Event(String title, String description, LocalDateTime start) {
        this.title = title;
        this.description = description;
        this.start = start;
        this.end = start.plusHours(1); // default duration
    }

    // getters & setters...
    public int getEventId() { return eventId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }

    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }

    public String getRecurrenceType() { return recurrenceType; }
    public void setRecurrenceType(String type) { this.recurrenceType = type; }

    public int getRecurrenceCount() { return recurrenceCount; }
    public void setRecurrenceCount(int count) { this.recurrenceCount = count; }

    public int getSeriesId() { return seriesId; }
    public void setSeriesId(int seriesId) { this.seriesId = seriesId; }

    public LocalDateTime getStartDateTime() { return start; }
}




