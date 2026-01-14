package calendarapp;

import java.io.*;
import java.util.*;

public class AdditionalFieldManager {

    private static final String FILE = "additional.csv";

    // In-memory map to store additional fields
    private Map<Integer, AdditionalFields> fieldsMap = new HashMap<>();

    public AdditionalFieldManager() {
        loadFromFile();
    }

    // Add or update additional fields
    public void saveFields(int eventId, String location, String category, String attendees) {
        AdditionalFields f = new AdditionalFields(location, category, attendees);
        fieldsMap.put(eventId, f);
        writeAll(); // save map to CSV
    }

    // Get fields for a specific event
    public AdditionalFields getFields(int eventId) {
        return fieldsMap.get(eventId);
    }

    // Search IDs by keyword
    public List<Integer> searchIds(String keyword) {
        List<Integer> ids = new ArrayList<>();
        for (Map.Entry<Integer, AdditionalFields> entry : fieldsMap.entrySet()) {
            AdditionalFields f = entry.getValue();
            if ((f.field1 != null && f.field1.toLowerCase().contains(keyword.toLowerCase())) ||
                (f.field2 != null && f.field2.toLowerCase().contains(keyword.toLowerCase())) ||
                (f.field3 != null && f.field3.toLowerCase().contains(keyword.toLowerCase()))) {
                ids.add(entry.getKey());
            }
        }
        return ids;
    }

    // ---------- Backup/Restore ----------

    public void backup(String backupFile) {
        copy(FILE, backupFile);
    }

    public void restore(String backupFile) {
        copy(backupFile, FILE);
        loadFromFile();
    }

    // ---------- Helpers ----------

    private void loadFromFile() {
        fieldsMap.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(FILE))) {
            String line = br.readLine(); // skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    int id = Integer.parseInt(parts[0]);
                    fieldsMap.put(id, new AdditionalFields(parts[1], parts[2], parts[3]));
                }
            }
        } catch (IOException ignored) {}
    }

    private void writeAll() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE))) {
            pw.println("eventId,location,category,attendees");
            for (Map.Entry<Integer, AdditionalFields> entry : fieldsMap.entrySet()) {
                AdditionalFields f = entry.getValue();
                pw.println(entry.getKey() + "," + f.field1 + "," + f.field2 + "," + f.field3);
            }
        } catch (IOException ignored) {}
    }

    private void copy(String from, String to) {
        try (FileInputStream fis = new FileInputStream(from);
             FileOutputStream fos = new FileOutputStream(to)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        } catch (IOException ignored) {}
    }

    // ---------- AdditionalFields ----------
    public static class AdditionalFields {
        public String field1;
        public String field2;
        public String field3;

        public AdditionalFields(String f1, String f2, String f3) {
            this.field1 = f1;
            this.field2 = f2;
            this.field3 = f3;
        }
    }
}
