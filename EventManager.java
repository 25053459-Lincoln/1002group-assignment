Eventmanager- package calendarapp;

import java.io.*;
import java.time.*;
import java.util.*;

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
public void viewWeeklyList(LocalDate startDate) {
    System.out.println("=== Week of " + startDate + " ===");

    for (int i = 0; i < 7; i++) {
        LocalDate day = startDate.plusDays(i);
        DayOfWeek dow = day.getDayOfWeek();
        boolean found = false;

        System.out.print(dow.toString().substring(0, 3) + " " +
                String.format("%02d", day.getDayOfMonth()) + ": ");

        for (Event e : events) {
            if (e.getStart().toLocalDate().equals(day)) {
                System.out.print(e.getTitle() +
                        " (" + e.getStart().toLocalTime() + ")");
                found = true;
            }
        }

        if (!found) System.out.print("No events");
        System.out.println();
    }
}
public void viewDailyList(LocalDate date) {
    System.out.println("=== " + date + " ===");
    boolean found = false;

    for (Event e : events) {
        if (e.getStart().toLocalDate().equals(date)) {
            System.out.println(
                e.getTitle() + " (" + e.getStart().toLocalTime() + ")"
            );
            found = true;
        }
    }

    if (!found) System.out.println("No events");
}
public void viewMonthlyList(YearMonth month) {
    System.out.println("=== " + month + " ===");

    boolean found = false;
    for (Event e : events) {
        if (YearMonth.from(e.getStart()).equals(month)) {
            System.out.println(
                e.getStart().toLocalDate() + ": " +
                e.getTitle() + " (" + e.getStart().toLocalTime() + ")"
            );
            found = true;
        }
    }

    if (!found) System.out.println("No events");
}
public void viewCalendarMonthCLI(YearMonth month) {
    System.out.println(month.getMonth().toString().substring(0, 3) +
            " " + month.getYear());
    System.out.println("Su Mo Tu We Th Fr Sa");

    LocalDate firstDay = month.atDay(1);
    int startDay = firstDay.getDayOfWeek().getValue() % 7;
    int daysInMonth = month.lengthOfMonth();

    // spacing before first day
    for (int i = 0; i < startDay; i++) {
        System.out.print("   ");
    }

    for (int day = 1; day <= daysInMonth; day++) {
        LocalDate current = month.atDay(day);
        boolean hasEvent = false;

        for (Event e : events) {
            if (e.getStart().toLocalDate().equals(current)) {
                hasEvent = true;
                break;
            }
        }

        System.out.print(day);
        if (hasEvent) System.out.print("*");
        else System.out.print(" ");

        if ((day + startDay) % 7 == 0) System.out.println();
        else System.out.print(" ");
    }

    System.out.println("\n");

    // Event details
    for (Event e : events) {
        if (YearMonth.from(e.getStart()).equals(month)) {
            System.out.println("* " +
                e.getStart().getDayOfMonth() + ": " +
                e.getTitle() +
                " (" + e.getStart().toLocalTime() + ")");
        }
    }
}
public void showLaunchReminder() {
    LocalDateTime now = LocalDateTime.now();
    Event nextEvent = null;
    Duration shortest = null;

    for (Event e : events) {
        if (e.getStart().isAfter(now)) {
            Duration untilEvent = Duration.between(now, e.getStart());

            if (shortest == null || untilEvent.compareTo(shortest) < 0) {
                shortest = untilEvent;
                nextEvent = e;
            }
        }
    }

    if (nextEvent == null) {
        System.out.println("No upcoming events.");
        return;
    }

    long minutes = shortest.toMinutes();

    System.out.println("🔔 Reminder");
    if (minutes < 60) {
        System.out.println("Your next event \"" + nextEvent.getTitle() +
                "\" is coming soon in " + minutes + " minutes.");
    } else if (minutes < 1440) {
        System.out.println("Your next event \"" + nextEvent.getTitle() +
                "\" is coming today.");
    } else {
        System.out.println("Your next event \"" + nextEvent.getTitle() +
                "\" is coming soon.");
    }
}


}

Mainapp- package calendarapp;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.LocalDate;

public class MainApp {
    public static void main(String[] args) {
        EventManager manager = new EventManager();

        // 🔔 Show reminder on program launch
        manager.showLaunchReminder();

        // Create an event
        manager.createEvent("Meeting", "Team discussion",
                LocalDateTime.of(2025, 10, 5, 11, 0),
                LocalDateTime.of(2025, 10, 5, 12, 0));

        // View all events
        manager.viewAllEvents();

        // Update the event
        manager.updateEvent(1, "Updated Meeting", "Updated description",
                LocalDateTime.of(2025, 10, 5, 12, 0),
                LocalDateTime.of(2025, 10, 5, 13, 0));

        // View again
        manager.viewAllEvents();

        // Delete the event
        manager.deleteEvent(1);

        // View again
        manager.viewAllEvents();

        // ===== VIEW FEATURES (CLI) =====
        System.out.println("\n--- WEEKLY LIST VIEW ---");
        manager.viewWeeklyList(LocalDate.of(2025, 10, 5));

        System.out.println("\n--- DAILY LIST VIEW ---");
        manager.viewDailyList(LocalDate.of(2025, 10, 5));

        System.out.println("\n--- MONTHLY LIST VIEW ---");
        manager.viewMonthlyList(YearMonth.of(2025, 10));

        System.out.println("\n--- CALENDAR MONTH VIEW ---");
        manager.viewCalendarMonthCLI(YearMonth.of(2025, 10));
    }
}

mainappgui-package calendarapp;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainAppGUI extends JFrame {

    private EventManager manager;
    private JTable eventTable;
    private DefaultTableModel tableModel;
    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public MainAppGUI() {
        manager = new EventManager();
        setTitle("Calendar App");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Table setup
        tableModel = new DefaultTableModel(new String[]{"ID", "Title", "Start", "End"}, 0);
        eventTable = new JTable(tableModel);
        refreshTable();

        JScrollPane scrollPane = new JScrollPane(eventTable);
        add(scrollPane, BorderLayout.CENTER);

        // Buttons panel
        JPanel panel = new JPanel();
        JButton addBtn = new JButton("Add Event");
        JButton updateBtn = new JButton("Update Event");
        JButton deleteBtn = new JButton("Delete Event");
        JButton backupBtn = new JButton("Backup");
        JButton restoreBtn = new JButton("Restore");

        panel.add(addBtn);
        panel.add(updateBtn);
        panel.add(deleteBtn);
        panel.add(backupBtn);
        panel.add(restoreBtn);
        add(panel, BorderLayout.SOUTH);

        // Button actions
        addBtn.addActionListener(e -> addEventDialog(false));
        updateBtn.addActionListener(e -> updateEventDialog());
        deleteBtn.addActionListener(e -> deleteSelectedEvent());
        backupBtn.addActionListener(e -> backupEvents());
        restoreBtn.addActionListener(e -> restoreEvents());

        // Auto reminder timer (checks every minute)
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkReminders();
            }
        }, 0, 60 * 1000); // every minute

        setVisible(true);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        List<Event> events = manager.getEvents();
        for (Event e : events) {
            tableModel.addRow(new Object[]{e.getEventId(), e.getTitle(),
                    e.getStart().format(dtf), e.getEnd().format(dtf)});
        }
    }

    private void addEventDialog(boolean recurring) {
        JTextField titleField = new JTextField();
        JTextField descField = new JTextField();
        JTextField startField = new JTextField("yyyy-MM-dd HH:mm");
        JTextField endField = new JTextField("yyyy-MM-dd HH:mm");

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Description:"));
        panel.add(descField);
        panel.add(new JLabel("Start (yyyy-MM-dd HH:mm):"));
        panel.add(startField);
        panel.add(new JLabel("End (yyyy-MM-dd HH:mm):"));
        panel.add(endField);

        if (recurring) {
            // Could add recurrence options here
        }

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Event",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                LocalDateTime start = LocalDateTime.parse(startField.getText(), dtf);
                LocalDateTime end = LocalDateTime.parse(endField.getText(), dtf);
                manager.createEvent(titleField.getText(), descField.getText(), start, end);
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid date/time format!");
            }
        }
    }

    private void updateEventDialog() {
        int selected = eventTable.getSelectedRow();
        if (selected == -1) {
            JOptionPane.showMessageDialog(this, "Select an event first!");
            return;
        }
        int id = (int) tableModel.getValueAt(selected, 0);
        Event e = manager.getEvents().stream().filter(ev -> ev.getEventId() == id).findFirst().orElse(null);
        if (e == null) return;

        JTextField titleField = new JTextField(e.getTitle());
        JTextField descField = new JTextField(e.getDescription());
        JTextField startField = new JTextField(e.getStart().format(dtf));
        JTextField endField = new JTextField(e.getEnd().format(dtf));

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Title:"));
        panel.add(titleField);
        panel.add(new JLabel("Description:"));
        panel.add(descField);
        panel.add(new JLabel("Start (yyyy-MM-dd HH:mm):"));
        panel.add(startField);
        panel.add(new JLabel("End (yyyy-MM-dd HH:mm):"));
        panel.add(endField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Update Event",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            try {
                LocalDateTime start = LocalDateTime.parse(startField.getText(), dtf);
                LocalDateTime end = LocalDateTime.parse(endField.getText(), dtf);
                manager.updateEvent(id, titleField.getText(), descField.getText(), start, end);
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid date/time format!");
            }
        }
    }

    private void deleteSelectedEvent() {
        int selected = eventTable.getSelectedRow();
        if (selected == -1) {
            JOptionPane.showMessageDialog(this, "Select an event first!");
            return;
        }
        int id = (int) tableModel.getValueAt(selected, 0);
        manager.deleteEvent(id);
        refreshTable();
    }

    private void backupEvents() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            manager.backupEvents(chooser.getSelectedFile().getAbsolutePath());
            JOptionPane.showMessageDialog(this, "Backup done!");
        }
    }

    private void restoreEvents() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            manager.restoreEvents(chooser.getSelectedFile().getAbsolutePath());
            refreshTable();
            JOptionPane.showMessageDialog(this, "Restore done!");
        }
    }

    private void checkReminders() {
        LocalDateTime now = LocalDateTime.now();
        for (Event e : manager.getEvents()) {
            if (e.getReminderMinutes() > 0) {
                LocalDateTime reminderTime = e.getStart().minusMinutes(e.getReminderMinutes());
                if (now.isAfter(reminderTime.minusSeconds(30)) && now.isBefore(reminderTime.plusSeconds(30))) {
                    SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                            "Reminder: " + e.getTitle() + " at " + e.getStart().format(dtf))
                    );
                }
            }
        }
    }

    public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        MainAppGUI app = new MainAppGUI();

        // 🔔 Show reminder on program launch
        app.manager.showLaunchReminder();

        // Additional fields demo
        AdditionalFieldManager extra = new AdditionalFieldManager();

        // Add additional fields
        extra.saveFields(1, "Room 204", "Academic", "Alice; Bob; Charlie");

        // Search additional fields
        System.out.println("\n--- SEARCH ADDITIONAL FIELDS ---");
        extra.search("Academic");

        // Backup & restore
        extra.backup("additional_backup.csv");
        extra.restore("additional_backup.csv");
    });
}    
}
