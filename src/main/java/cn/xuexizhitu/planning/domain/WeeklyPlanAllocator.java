package cn.xuexizhitu.planning.domain;
import cn.xuexizhitu.planning.domain.PlanTypes.Segment;
import java.time.LocalDate;
import java.util.*;
/** Fixed course windows; unused time in another course's week cannot hide a gap. */ public final class WeeklyPlanAllocator {
    private WeeklyPlanAllocator() {
    }
    public record Work(PlanAllocator.Work work, String courseId, String kind) {
    }
    public static List<Segment> allocate(List<Work> work, List<String> priority,             Map<LocalDate, Integer> capacities, Map<LocalDate, Integer> reserved,             LocalDate start, LocalDate earliest, int order) {
        List<Segment> result = new ArrayList<>();
        for (int week = 0; week < 5; week++) {
            final int index = week;
            List<PlanAllocator.Work> selected = work.stream()                 .filter(w -> index == 4 ? !w.kind().equals("ITEM") : w.kind().equals("ITEM") && w.courseId().equals(priority.get(index)))                 .map(Work::work).toList();
            LocalDate from = start.plusDays(week * 7L), end = from.plusDays(6);
            List<Segment> allocated = PlanAllocator.allocate(selected, capacities, reserved,                 earliest.isAfter(from) ? earliest : from, end, order);
            result.addAll(allocated);
            order += allocated.size();
        }
        return result;
    }
}
