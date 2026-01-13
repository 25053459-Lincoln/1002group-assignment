import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CalendarManager {
    private final String DATA_DIR = "data/";
    private final String EVENT_FILE = DATA_DIR + "event.csv";
    private final String ADD_FILE = DATA_DIR + "additional.csv";
    private final String RECUR_FILE = DATA_DIR + "recurrent.csv";

    public CalendarManager() { new File(DATA_DIR).mkdirs(); }

    // Requirement 13: 冲突检测逻辑 
    public boolean isConflicting(Event newEvent) {
        List<Event> events = loadEvents();
        return events.stream().anyMatch(e -> 
            newEvent.getStartDateTime().isBefore(e.getEndDateTime()) && 
            newEvent.getEndDateTime().isAfter(e.getStartDateTime()));
    }

    // Requirement 4: 备份到单一文件（这里建议使用 ZIP 或合并 CSV） 
    public void backupAll(String backupDir) throws IOException {
        Path dest = Paths.get(backupDir);
        if (!Files.exists(dest)) Files.createDirectories(dest);
        Files.copy(Paths.get(EVENT_FILE), dest.resolve("event_bak.csv"), StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Backup completed to " + backupDir); // 
    }

    public List<Event> loadEvents() {
        List<Event> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(EVENT_FILE))) {
            br.readLine(); // Skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                list.add(new Event(Integer.parseInt(p[0]), p[1], p[2], 
                        java.time.LocalDateTime.parse(p[3]), java.time.LocalDateTime.parse(p[4])));
            }
        } catch (Exception e) { /* Handle error */ }
        return list;
    }
}
