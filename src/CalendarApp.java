import java.time.LocalDateTime;
import java.util.List;

public class CalendarApp {
    public static void main(String[] args) {
        // Step 1: 自动生成 ID (根据现有列表大小 + 1)
        List<Event> currentEvents = DataManager.getAllEvents();
        int newId = currentEvents.isEmpty() ? 1 : currentEvents.get(currentEvents.size() - 1).getEventId() + 1;

        // 创建一个新事件
        Event myEvent = new Event(newId, "FOP Meeting", "Discuss assignment", 
                                  LocalDateTime.now(), LocalDateTime.now().plusHours(2));

        // Step 5: 保存并查看
        DataManager.saveEvent(myEvent);
        System.out.println("--- Current Events ---");
        DataManager.getAllEvents().forEach(System.out::println);

        // Step 11 & 12: 备份与恢复演示
        DataManager.backupData();
        // DataManager.restoreData(); // 需要时调用
    }
}
