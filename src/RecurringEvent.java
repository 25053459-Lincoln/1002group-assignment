import java.time.LocalDate;

public class RecurringEvent {
    private int eventId;
    private String recurrentInterval; // 例如 "1d", "1w" [cite: 19, 22]
    private int recurrentTimes;       // 循环次数 [cite: 22]
    private LocalDate recurrentEndDate; // 截止日期 [cite: 19]

    public RecurringEvent(int eventId, String interval, int times, LocalDate endDate) {
        this.eventId = eventId;
        this.recurrentInterval = interval;
        this.recurrentTimes = times;
        this.recurrentEndDate = endDate;
    }

    // 将数据转换为 CSV 格式存储 [cite: 19]
    public String toCSV() {
        return eventId + "," + recurrentInterval + "," + recurrentTimes + "," + recurrentEndDate;
    }

    // Getters
    public int getEventId() { return eventId; }
}
