package cn.xuexizhitu.dashboard.application;
import cn.xuexizhitu.common.*;
import static cn.xuexizhitu.common.BusinessData.*;
import cn.xuexizhitu.learning.infrastructure.LearningRepository;
import cn.xuexizhitu.learning.application.LearningService;
import cn.xuexizhitu.learning.api.LearningDtos.*;
import cn.xuexizhitu.planning.application.PlanService;
import cn.xuexizhitu.dashboard.infrastructure.DashboardRepository;
import cn.xuexizhitu.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true) public class DashboardService {
    private final DashboardRepository repository;
    private final LearningRepository learning;
    private final LearningService courses;
    private final PlanService plans;
    public record Suggestion(String title,String reason,String taskId,Navigation target) {
    }
    public record Countdown(String courseId,String courseCode,LocalDate examDate,Long daysRemaining,String status) {
    }
    public record Dashboard(String snapshotId,Instant asOf,LocalDate localDate,ExamCycle cycle,LearningPosition continueLearning,             Suggestion todaySuggestion,Progress overallProgress,List<Countdown> countdowns,List<Course> courses,             String selectedPlanId,String todaySuggestionMessage) {
    }
    public Dashboard dashboard(String cycle,String plan) {
        String user=CurrentUser.idOrThrow();
        LocalDate day=today();
        if(cycle==null)cycle=repository.defaultCycle(day);
        if(cycle==null) {
            require(plan==null,ErrorCode.INVALID_RELATION,"没有周期不能指定计划");
            return new Dashboard(uuid(),Instant.now(),day,null,null,null,learning.overallProgress(user),List.of(),List.of(),null,"暂无今日安排");
        }
        learning.requireCycle(cycle);
        ExamCycle cycleDto=learning.cycle(cycle);
        List<Course> courseList=new ArrayList<>();
        int page=1;
        Page<Course> batch;
        do {
            batch=courses.courses(cycle,null,page++,100);
            courseList.addAll(batch.items());
        }
        while(courseList.size()<batch.total()&&!batch.items().isEmpty());
        String recent=repository.recentCourse(user);
        LearningPosition position=recent==null?null:courses.position(recent);
        if(plan==null)plan=repository.latestPlan(user,cycle);
        Suggestion suggestion=null;
        if(plan!=null) {
            var p=plans.get(plan,null);
            require(p.config().cycleId().toString().equals(cycle),ErrorCode.INVALID_RELATION,"计划不属于所选周期");
            Map<String,cn.xuexizhitu.planning.domain.PlanTypes.Task> tasks=new HashMap<>();
            p.tasks().forEach(t->tasks.put(t.id(),t));
            outer:for(var d:p.days())if(d.day().equals(day))for(var s:d.segments()) {
                var t=tasks.get(s.taskId());
                if(!t.completed()) {
                    suggestion=new Suggestion(t.title(),"今日计划首个未完成任务",t.id(),t.target());
                    break outer;
                }
            }
        }
        List<Countdown> countdowns=new ArrayList<>();
        Map<String,Course> byId=new HashMap<>();
        courseList.forEach(c->byId.put(c.id(),c));
        for(var c:cycleDto.courses())if(byId.containsKey(c.courseId())) {
            var row=byId.get(c.courseId());
            Long remaining=c.examDate()==null?null:java.time.temporal.ChronoUnit.DAYS.between(day,c.examDate());
            countdowns.add(new Countdown(row.id(),row.code(),c.examDate(),remaining,remaining==null?"DATE_UNKNOWN":remaining<0?"FINISHED":remaining==0?"TODAY":"UPCOMING"));
        }
        return new Dashboard(uuid(),Instant.now(),day,cycleDto,position,suggestion,learning.overallProgress(user),countdowns,courseList,plan,suggestion==null?"暂无今日安排":"今日建议："+suggestion.title());
    }
}
