package cn.xuexizhitu.service;

import java.time.LocalDate;
import java.util.*;

/** Fixed course windows; unused time in another course's week cannot hide a gap. */
public final class WeeklyPlanAllocator {
    private WeeklyPlanAllocator() {}
    public record Work(PlanAllocator.Work work, String courseId, String kind) {}
    public static List<Map<String, Object>> allocate(List<Work> work, List<String> priority,
            Map<LocalDate, Integer> capacities, Map<LocalDate, Integer> reserved,
            LocalDate start, LocalDate earliest, int order) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (int week = 0; week < 5; week++) {
            final int index = week;
            List<PlanAllocator.Work> selected = work.stream()
                .filter(w -> index == 4 ? !w.kind().equals("ITEM") : w.kind().equals("ITEM") && w.courseId().equals(priority.get(index)))
                .map(Work::work).toList();
            LocalDate from = start.plusDays(week * 7L), end = from.plusDays(6);
            List<Map<String,Object>> allocated = PlanAllocator.allocate(selected, capacities, reserved,
                earliest.isAfter(from) ? earliest : from, end, order);
            result.addAll(allocated); order += allocated.size();
        }
        return result;
    }
}
