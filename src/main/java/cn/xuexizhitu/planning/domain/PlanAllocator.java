package cn.xuexizhitu.planning.domain;
import cn.xuexizhitu.planning.domain.PlanTypes.Segment;
import static cn.xuexizhitu.common.BusinessData.*;
import java.time.*;
import java.util.*;
public final class PlanAllocator {
    private PlanAllocator() {
    }
    public record Work(String task,int minutes,LocalDate deadline) {
    }
    public static List<Segment> allocate(List<Work> work,Map<LocalDate,Integer> capacities,Map<LocalDate,Integer> reserved,LocalDate from,LocalDate end,int startingOrder) {
        List<Segment> segments=new ArrayList<>();
        Map<LocalDate,Integer> used=new HashMap<>(reserved);
        int order=startingOrder;
        for(Work w:work) {
            int remaining=w.minutes();
            if(w.deadline()==null) {
                segments.add(new Segment(uuid(),w.task(),null,remaining,"AWAITING_DATE",order++));
                continue;
            }
            for(LocalDate date=from;!date.isAfter(end)&&date.isBefore(w.deadline())&&remaining>0;date=date.plusDays(1)) {
                int free=Math.max(0,capacities.getOrDefault(date,0)-used.getOrDefault(date,0));
                int amount=Math.min(free,remaining);
                if(amount>0) {
                    segments.add(new Segment(uuid(),w.task(),date,amount,"SCHEDULED",order++));
                    remaining-=amount;
                    used.merge(date,amount,Integer::sum);
                }
            }
            if(remaining>0)segments.add(new Segment(uuid(),w.task(),null,remaining,"UNSCHEDULED",order++));
        }
        return segments;
    }
}
