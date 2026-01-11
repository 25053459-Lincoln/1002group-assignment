import java.time.LocalDateTime;

public class Event {
    private int eventId; // Step 1: 自动递增 ID
    private String title;
    private String description;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    public Event(int eventId, String title, String description, LocalDateTime start, LocalDateTime end) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.startDateTime = start;
        this.endDateTime = end;
    }

    // Step 2: 转换为符合 event.csv 格式的字符串
    public String toCSV() {
        return String.format("%d, %s, %s, %s, %s", 
                eventId, title, description, startDateTime.toString(), endDateTime.toString());
    }

    // Getters
    public int getEventId() { return eventId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }

    @Override
    public String toString() {
        return String.format("ID: %-3d | Title: %-15s | Start: %s | End: %s", 
                eventId, title, startDateTime, endDateTime);
    }
}
