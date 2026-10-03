package cn.xuexizhitu.service;

import cn.xuexizhitu.planning.domain.PlanAllocator;
import cn.xuexizhitu.planning.domain.WeeklyPlanAllocator;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
class PlanAllocatorTest {
    private final LocalDate day=LocalDate.of(2026,10,2);
    @Test void splitsOnlyIntoFreeMinutesAndNeverUsesExamDay(){var s=PlanAllocator.allocate(List.of(new PlanAllocator.Work("t",100,day.plusDays(2))),Map.of(day,60,day.plusDays(1),60,day.plusDays(2),60),Map.of(day,40),day,day.plusDays(2),0);assertThat(s.stream().mapToInt(x->x.minutes()).sum()).isEqualTo(100);assertThat(s).extracting(x->x.minutes()).containsExactly(20,60,20);assertThat(s.get(s.size() - 1).state()).isEqualTo("UNSCHEDULED");}
    @Test void unknownDeadlineDoesNotInventDateOrDiscardWork(){var s=PlanAllocator.allocate(List.of(new PlanAllocator.Work("t",30,null)),Map.of(day,60),Map.of(),day,day,0);assertThat(s.get(0).state()).isEqualTo("AWAITING_DATE");assertThat(s.get(0).scheduledOn()).isNull();}
}
