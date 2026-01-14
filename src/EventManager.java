package Assigment; 

import java.io.*;
import java.time.*;
import java.util.*;
import javax.swing.JOptionPane; // Add this line to your pop-up window

public class EventManager {

    private List<Event> events;
    private int nextEventId;

    public EventManager() {
        events = FileManager.readEvents(); // load events from CSV if exists
        nextEventId = getNextEventId();
    }

    // Create a new single event
    public void createEvent(String title, String desc, LocalDateTime start, LocalDateTime end) {
        int id = nextEventId++;
        Event e = new Event(id, title, desc, start, end);
        events.add(e);
        FileManager.saveEvent(e);
    }

    // Add a recurring event (creates all occurrences)
    public void addRecurringEvent(Event event) {
        int seriesId = nextEventId; // first event ID becomes series ID
        LocalDateTime nextStart = event.getStart();
        LocalDateTime nextEnd = event.getEnd();

        for (int i = 0; i < event.getRecurrenceCount(); i++) {
            Event e = new Event(nextEventId++, event.getTitle(), event.getDescription(), nextStart, nextEnd);
            e.setRecurring(true);
            e.setRecurrenceType(event.getRecurrenceType());
            e.setRecurrenceCount(event.getRecurrenceCount());
            e.setSeriesId(seriesId);
            e.setReminderMinutes(event.getReminderMinutes());
            events.add(e);

            // advance start and end for next occurrence
            switch (event.getRecurrenceType()) {
                case "DAILY" -> { nextStart = nextStart.plusDays(1); nextEnd = nextEnd.plusDays(1); }
                case "WEEKLY" -> { nextStart = nextStart.plusWeeks(1); nextEnd = nextEnd.plusWeeks(1); }
                case "MONTHLY" -> { nextStart = nextStart.plusMonths(1); nextEnd = nextEnd.plusMonths(1); }
            }
        }
        saveAllEvents();
    }

    // Update a single event by ID
    public void updateEvent(int id, String newTitle, String newDesc, LocalDateTime newStart, LocalDateTime newEnd) {
        for (Event e : events) {
            if (e.getEventId() == id) {
                e.setTitle(newTitle);
                e.setDescription(newDesc);
                e.setStart(newStart);
                e.setEnd(newEnd);
                saveAllEvents();
                return;
            }
        }
        System.out.println("Event ID not found!");
    }

    // Update all events in a recurring series
    public void updateRecurringEvent(Event event) {
        int seriesId = event.getSeriesId();
        for (Event e : events) {
            if (e.getSeriesId() == seriesId) {
                e.setTitle(event.getTitle());
                e.setDescription(event.getDescription());
                e.setStart(event.getStart());
                e.setEnd(event.getEnd());
                e.setRecurring(event.isRecurring());
                e.setRecurrenceType(event.getRecurrenceType());
                e.setRecurrenceCount(event.getRecurrenceCount());
                e.setReminderMinutes(event.getReminderMinutes());
            }
        }
        saveAllEvents();
    }

    // Delete single event
    public void deleteEvent(int id) {
        events.removeIf(e -> e.getEventId() == id);
        saveAllEvents();
    }

    // Delete a recurring series
    public void deleteRecurringEvent(Event event) {
        if (event.getSeriesId() != 0) {
            events.removeIf(e -> e.getSeriesId() == event.getSeriesId());
        } else {
            deleteEvent(event.getEventId());
        }
        saveAllEvents();
    }

    // Delete a single occurrence
    public void deleteSingleOccurrence(Event event) {
        deleteEvent(event.getEventId());
    }

    // Conflict check excluding a specific event (for updates)
    public boolean hasConflictExcludingEvent(LocalDateTime newStart, LocalDateTime newEnd, int excludeId) {
        for (Event e : events) {
            if (e.getEventId() == excludeId) continue;
            if (newStart.isBefore(e.getEnd()) && newEnd.isAfter(e.getStart())) return true;
        }
        return false;
    }

    // Conflict check for new events
    public boolean hasConflict(LocalDateTime newStart, LocalDateTime newEnd) {
        return hasConflictExcludingEvent(newStart, newEnd, -1);
    }

    // Search events by date range
    public List<Event> searchByDateRange(LocalDate start, LocalDate end) {
        List<Event> results = new ArrayList<>();
        for (Event e : events) {
            LocalDate eventDate = e.getStart().toLocalDate();
            if (!eventDate.isBefore(start) && !eventDate.isAfter(end)) results.add(e);
        }
        return results;
    }

    // Backup events to a CSV file
    public void backupEvents(String path) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path))) {
            for (Event e : events) {
                writer.println(e.getEventId() + "," + e.getTitle() + "," + e.getDescription() + "," +
                               e.getStart() + "," + e.getEnd() + "," + e.isRecurring() + "," +
                               e.getRecurrenceType() + "," + e.getRecurrenceCount() + "," +
                               e.getSeriesId() + "," + e.getReminderMinutes());
            }
        } catch (IOException ex) {
            System.out.println("Backup failed: " + ex.getMessage());
        }
    }

    // Restore events from a CSV file
    public void restoreEvents(String path) {
        events.clear();
        nextEventId = 1;
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                int id = Integer.parseInt(parts[0]);
                String title = parts[1];
                String desc = parts[2];
                LocalDateTime start = LocalDateTime.parse(parts[3]);
                LocalDateTime end = LocalDateTime.parse(parts[4]);
                boolean recurring = Boolean.parseBoolean(parts[5]);
                String recurrenceType = parts[6];
                int recurrenceCount = Integer.parseInt(parts[7]);
                int seriesId = Integer.parseInt(parts[8]);
                int reminderMinutes = Integer.parseInt(parts[9]);

                Event e = new Event(id, title, desc, start, end);
                e.setRecurring(recurring);
                e.setRecurrenceType(recurrenceType);
                e.setRecurrenceCount(recurrenceCount);
                e.setSeriesId(seriesId);
                e.setReminderMinutes(reminderMinutes);
                events.add(e);
                nextEventId = Math.max(nextEventId, id + 1);
            }
        } catch (IOException ex) {
            System.out.println("Restore failed: " + ex.getMessage());
        }
    }

    // Stats
    public int getTotalEvents() { return events.size(); }
    public int getRecurringEventCount() { return (int) events.stream().filter(Event::isRecurring).count(); }
    public String getBusiestDay() {
        Map<DayOfWeek, Long> map = new HashMap<>();
        for (Event e : events) {
            map.put(e.getStart().getDayOfWeek(), map.getOrDefault(e.getStart().getDayOfWeek(), 0L) + 1);
        }
        return map.entrySet().stream().max(Map.Entry.comparingByValue()).map(e -> e.getKey().toString()).orElse("N/A");
    }

    // Utilities
    public int getNextEventId() {
        return events.stream().mapToInt(Event::getEventId).max().orElse(0) + 1;
    }

    public List<Event> getEvents() { return events; }

    // Save all events
    private void saveAllEvents() { FileManager.saveEvents(events); }

    public void viewAllEvents() {
        System.out.println("=== All Events ===");
        for (Event e : events) {
            System.out.println(e.getEventId() + ": " + e.getTitle() +
                    " (" + e.getStart() + " to " + e.getEnd() + ")");
        }
    }

    // ==========================================================
    //  YOUR CUSTOM CODE SECTION (Req 8, 9, 13)
    // ==========================================================

    // [Req 13] Conflict Detection: Check if the new event's time overlaps with any existing events
    public boolean isTimeSlotAvailable(LocalDateTime newStart, LocalDateTime newEnd, List<Event> allEvents) {
        for (Event event : allEvents) {
            // Logic: (NewStart < ExistingEnd) AND (NewEnd > ExistingStart) indicates an overlap
            if (newStart.isBefore(event.getEnd()) && newEnd.isAfter(event.getStart())) {
                System.out.println("Conflict detected: Time clash with event [" + event.getTitle() + "]!");
                return false; // Time slot is unavailable
            }
        }
        return true; // No conflict found
    }

    // [Req 8] Reminders: Find upcoming events (e.g., within the next 24 hours)
    public void checkUpcomingReminders(List<Event> allEvents) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime upcomingThreshold = now.plusHours(24); // Set reminder range: within 24 hours

        StringBuilder reminderMessage = new StringBuilder();
        int count = 0;

        for (Event event : allEvents) {
            LocalDateTime start = event.getStart();
            
            // Check if event is between NOW and the next 24 hours
            if (start.isAfter(now) && start.isBefore(upcomingThreshold)) {
                long hoursLeft = java.time.temporal.ChronoUnit.HOURS.between(now, start);
                reminderMessage.append("• ").append(event.getTitle())
                               .append(" (starts in ").append(hoursLeft).append(" hours)\n");
                count++;
            }
        }

        if (count > 0) {
            JOptionPane.showMessageDialog(null, 
                "You have " + count + " upcoming events in the next 24h:\n" + reminderMessage.toString(), 
                "Schedule Reminder (Req 8)", 
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // [Req 9] Event Statistics: Calculate the busiest day and general stats
    public String getEventStatistics(List<Event> allEvents) {
        if (allEvents.isEmpty()) return "No statistics data available.";

        long totalEvents = allEvents.size();
        long pastEvents = allEvents.stream()
                .filter(e -> e.getEnd().isBefore(LocalDateTime.now()))
                .count();
        long futureEvents = totalEvents - pastEvents;

        // Count events per day to find the busiest one
        Map<java.time.LocalDate, Long> eventsPerDay = allEvents.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    e -> e.getStart().toLocalDate(), 
                    java.util.stream.Collectors.counting()
                ));

        // Find the day with the maximum number of events
        Map.Entry<java.time.LocalDate, Long> busiestDay = eventsPerDay.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        StringBuilder stats = new StringBuilder();
        stats.append("=== Event Statistics (Req 9) ===\n");
        stats.append("Total Events: ").append(totalEvents).append("\n");
        stats.append("Past Events: ").append(pastEvents).append("\n");
        stats.append("Upcoming: ").append(futureEvents).append("\n");
        
        if (busiestDay != null) {
            stats.append("Busiest Day: ").append(busiestDay.getKey())
                 .append(" (").append(busiestDay.getValue()).append(" events)");
        }

        return stats.toString();
    }
}
