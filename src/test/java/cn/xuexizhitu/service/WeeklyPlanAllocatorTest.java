package cn.xuexizhitu.service;

import cn.xuexizhitu.planning.domain.PlanAllocator;
import cn.xuexizhitu.planning.domain.WeeklyPlanAllocator;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class WeeklyPlanAllocatorTest {
    final LocalDate start=LocalDate.of(2026,9,19);
    Map<LocalDate,Integer> caps(){Map<LocalDate,Integer> c=new TreeMap<>();for(int i=0;i<35;i++)c.put(start.plusDays(i),60);return c;}
    WeeklyPlanAllocator.Work task(String id,int minutes,String course,String kind){return new WeeklyPlanAllocator.Work(new PlanAllocator.Work(id,minutes,start.plusDays(36)),course,kind);}
    @Test void fourCoursesStayInTheirOwnWeeksAndPapersShareFifthWeek(){var result=WeeklyPlanAllocator.allocate(List.of(task("a",90,"a","ITEM"),task("b",60,"b","ITEM"),task("c",60,"c","ITEM"),task("d",60,"d","ITEM"),task("paper",90,"a","PAPER"),task("review",60,"b","REVIEW")),List.of("a","b","c","d"),caps(),Map.of(),start,start,0);assertThat(result.stream().filter(s->s.taskId().equals("a"))).extracting(s->s.scheduledOn()).containsExactly(start,start.plusDays(1));assertThat(result.stream().filter(s->s.taskId().equals("b"))).extracting(s->s.scheduledOn()).containsExactly(start.plusDays(7));assertThat(result.stream().filter(s->s.taskId().equals("paper"))).extracting(s->s.scheduledOn()).containsExactly(start.plusDays(28),start.plusDays(29));assertThat(result.stream().filter(s->s.taskId().equals("review"))).extracting(s->s.scheduledOn()).containsExactly(start.plusDays(29),start.plusDays(30));}
    @Test void insufficientCourseWeekDoesNotSpillIntoAnotherWeek(){var result=WeeklyPlanAllocator.allocate(List.of(task("a",500,"a","ITEM")),List.of("a","b","c","d"),caps(),Map.of(),start,start,0);assertThat(result.stream().mapToInt(s->s.minutes()).sum()).isEqualTo(500);assertThat(result.get(result.size()-1).state()).isEqualTo("UNSCHEDULED");assertThat(result.get(result.size()-1).minutes()).isEqualTo(80);assertThat(result.stream().filter(s->s.scheduledOn()!=null)).allMatch(s->s.scheduledOn().isBefore(start.plusDays(7)));}
    @Test void examDeadlineAndUnknownDateStillApply(){var result=WeeklyPlanAllocator.allocate(List.of(new WeeklyPlanAllocator.Work(new PlanAllocator.Work("exam",120,start.plusDays(1)),"a","ITEM"),new WeeklyPlanAllocator.Work(new PlanAllocator.Work("unknown",60,null),"b","ITEM")),List.of("a","b","c","d"),caps(),Map.of(),start,start,0);assertThat(result).anyMatch(s->s.taskId().equals("exam")&&s.state().equals("UNSCHEDULED")&&s.minutes()==60);assertThat(result).anyMatch(s->s.taskId().equals("unknown")&&s.state().equals("AWAITING_DATE"));}
    @Test void zeroCapacityDayAndReservedTimeAreNotOverwritten(){var c=caps();c.put(start,0);var result=WeeklyPlanAllocator.allocate(List.of(task("a",90,"a","ITEM")),List.of("a","b","c","d"),c,Map.of(start.plusDays(1),40),start,start,0);assertThat(result).extracting(s->s.minutes()).containsExactly(20,60,10);assertThat(result.get(0).scheduledOn()).isEqualTo(start.plusDays(1));}
    @Test void expiredWeekStaysUnscheduledWhenReschedulingOverdue(){var result=WeeklyPlanAllocator.allocate(List.of(task("a",30,"a","ITEM")),List.of("a","b","c","d"),caps(),Map.of(),start,start.plusDays(8),0);assertThat(result).hasSize(1);assertThat(result.get(0).state()).isEqualTo("UNSCHEDULED");}
}
