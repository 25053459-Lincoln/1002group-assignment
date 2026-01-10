package calendarapp;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

public class CalendarGUI extends JFrame {

    private JLabel monthLabel;
    private JPanel calendarPanel;
    private YearMonth currentMonth;
    private EventManager manager; // link to EventManager

    // Constructor
    public CalendarGUI(EventManager manager) {
        this.manager = manager; // connect EventManager
        currentMonth = YearMonth.now();

        setTitle("Calendar App");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Top panel with month label and navigation buttons
        JPanel topPanel = new JPanel();
        JButton prevBtn = new JButton("<");
        JButton nextBtn = new JButton(">");
        monthLabel = new JLabel("", SwingConstants.CENTER);

        topPanel.add(prevBtn);
        topPanel.add(monthLabel);
        topPanel.add(nextBtn);

        add(topPanel, BorderLayout.NORTH);

        // Calendar panel for days
        calendarPanel = new JPanel(new GridLayout(0, 7));
        add(calendarPanel, BorderLayout.CENTER);

        // Navigation button actions
        prevBtn.addActionListener(e -> {
            currentMonth = currentMonth.minusMonths(1);
            updateCalendar();
        });

        nextBtn.addActionListener(e -> {
            currentMonth = currentMonth.plusMonths(1);
            updateCalendar();
        });

        // Initial calendar render
        updateCalendar();
        setVisible(true);
    }

    // Update calendar UI
    private void updateCalendar() {
        calendarPanel.removeAll();
        monthLabel.setText(currentMonth.getMonth() + " " + currentMonth.getYear());

        // Days of week
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String day : days) {
            calendarPanel.add(new JLabel(day, SwingConstants.CENTER));
        }

        LocalDate firstDay = currentMonth.atDay(1);
        int startDay = firstDay.getDayOfWeek().getValue() % 7;
        int daysInMonth = currentMonth.lengthOfMonth();

        // Empty labels for offset
        for (int i = 0; i < startDay; i++) {
            calendarPanel.add(new JLabel(""));
        }

        // Add day buttons
        for (int day = 1; day <= daysInMonth; day++) {
            JButton dayBtn = new JButton(String.valueOf(day));
            LocalDate date = currentMonth.atDay(day);

            // Tooltip for events
            StringBuilder tooltip = new StringBuilder();
            for (Event e : manager.getEvents()) {
                if (e.getStart().toLocalDate().equals(date)) {
                    tooltip.append(e.getTitle())
                           .append(" (")
                           .append(e.getStart().getHour())
                           .append(":")
                           .append(e.getStart().getMinute())
                           .append(")\n");
                }
            }
            if (tooltip.length() > 0) {
                dayBtn.setToolTipText("<html>" + tooltip.toString().replaceAll("\n", "<br>") + "</html>");
            }

            // Click to choose Add or Manage Events
            dayBtn.addActionListener(ae -> {
                Object[] options = {"Add Event", "Manage Events"};
                int choice = JOptionPane.showOptionDialog(this, "What do you want to do?",
                        "Day Options", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                        options, options[0]);

                if (choice == 0) {
                    addEventDialog(date); // Add new event
                } else if (choice == 1) {
                    manageEventsDialog(date); // Update/Delete existing events
                }
            });

            calendarPanel.add(dayBtn);
        }

        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    // Popup to update an existing event
    private void updateEventDialog(Event event) {
        JTextField titleField = new JTextField(event.getTitle());
        JTextField descField = new JTextField(event.getDescription());
        JTextField startField = new JTextField(event.getStart().getHour() + ":" + event.getStart().getMinute());
        JTextField endField = new JTextField(event.getEnd().getHour() + ":" + event.getEnd().getMinute());

        Object[] message = {
                "Title:", titleField,
                "Description:", descField,
                "Start Time (HH:mm):", startField,
                "End Time (HH:mm):", endField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Update Event", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                LocalDate date = event.getStart().toLocalDate();
                LocalDateTime start = LocalDateTime.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                        Integer.parseInt(startField.getText().split(":")[0]),
                        Integer.parseInt(startField.getText().split(":")[1]));
                LocalDateTime end = LocalDateTime.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                        Integer.parseInt(endField.getText().split(":")[0]),
                        Integer.parseInt(endField.getText().split(":")[1]));

                manager.updateEvent(event.getEventId(), titleField.getText(), descField.getText(), start, end);
                updateCalendar();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage());
            }
        }
    }

    // Popup to add a new event
    private void addEventDialog(LocalDate date) {
        JTextField titleField = new JTextField();
        JTextField descField = new JTextField();
        JTextField startField = new JTextField("HH:mm");
        JTextField endField = new JTextField("HH:mm");

        Object[] message = {
                "Title:", titleField,
                "Description:", descField,
                "Start Time (HH:mm):", startField,
                "End Time (HH:mm):", endField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Add Event", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                LocalDateTime start = LocalDateTime.of(date.getYear(), date.getMonthValue(),
                        date.getDayOfMonth(),
                        Integer.parseInt(startField.getText().split(":")[0]),
                        Integer.parseInt(startField.getText().split(":")[1]));

                LocalDateTime end = LocalDateTime.of(date.getYear(), date.getMonthValue(),
                        date.getDayOfMonth(),
                        Integer.parseInt(endField.getText().split(":")[0]),
                        Integer.parseInt(endField.getText().split(":")[1]));

                manager.createEvent(titleField.getText(), descField.getText(), start, end);
                updateCalendar(); // refresh calendar with new tooltip
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage());
            }
        }
    }

    // Popup to manage existing events on a day
    private void manageEventsDialog(LocalDate date) {
        java.util.List<Event> dayEvents = new java.util.ArrayList<>();
        for (Event e : manager.getEvents()) {
            if (e.getStart().toLocalDate().equals(date)) {
                dayEvents.add(e);
            }
        }

        if (dayEvents.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No events on this day.");
            return;
        }

        // Show events in a list
        String[] eventTitles = new String[dayEvents.size()];
        for (int i = 0; i < dayEvents.size(); i++) {
            Event e = dayEvents.get(i);
            eventTitles[i] = e.getEventId() + ": " + e.getTitle() + " (" + e.getStart().getHour() + ":" + e.getStart().getMinute() + ")";
        }

        String selected = (String) JOptionPane.showInputDialog(this, "Select an event",
                "Manage Events", JOptionPane.PLAIN_MESSAGE, null,
                eventTitles, eventTitles[0]);

        if (selected == null) return; // cancelled

        // Find the selected event
        int selectedId = Integer.parseInt(selected.split(":")[0]);
        Event selectedEvent = null;
        for (Event e : dayEvents) {
            if (e.getEventId() == selectedId) {
                selectedEvent = e;
                break;
            }
        }

        if (selectedEvent == null) return;

        // Popup to update or delete
        Object[] options = {"Update", "Delete", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, "Update or Delete?", "Event Options",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                options, options[0]);

        if (choice == JOptionPane.YES_OPTION) { // Update
            updateEventDialog(selectedEvent);
        } else if (choice == JOptionPane.NO_OPTION) { // Delete
            manager.deleteEvent(selectedEvent.getEventId());
            updateCalendar();
        }
    }

    // Main method to launch GUI
    public static void main(String[] args) {
    EventManager manager = new EventManager();

    // TEMP TEST FOR FEATURE 4
    manager.createEvent(
        "Test Event",
        "Backup check",
        LocalDateTime.now(),
        LocalDateTime.now().plusHours(1)
    );

    manager.backupEvents("backup_events.csv");

    manager.restoreEvents("backup_events.csv");

    new CalendarGUI(manager);
}

    }

