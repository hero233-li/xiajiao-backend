package cn.xuexizhitu.service;
import static cn.xuexizhitu.common.BusinessData.*;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.repository.LearningRepository;
import cn.xuexizhitu.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class DashboardService {
    private final JdbcTemplate jdbc;private final LearningRepository learning;private final LearningService courses;private final PlanService plans;private final ObjectMapper mapper;
    public Object dashboard(String cycle,String plan){String user=CurrentUser.idOrThrow();if(cycle==null)cycle=jdbc.queryForList("SELECT id FROM exam_cycle ORDER BY CASE WHEN end_date>=? THEN 0 ELSE 1 END,start_date DESC,id LIMIT 1",String.class,today()).stream().findFirst().orElse(null);if(cycle==null){require(plan==null,ErrorCode.INVALID_RELATION,"没有周期不能指定计划");return obj("snapshotId",uuid(),"asOf",Instant.now(),"localDate",today(),"cycle",null,"continueLearning",null,"todaySuggestion",null,"overallProgress",learning.overallProgress(user),"countdowns",List.of(),"courses",List.of(),"selectedPlanId",null,"todaySuggestionMessage","暂无今日安排");}
        learning.requireCycle(cycle);var cycleDto=learning.cycle(cycle);var courseList=courses.courses(cycle,null,1,100).items();Object position=jdbc.queryForList("SELECT p.course_id FROM recent_learning_position p JOIN course c ON c.id=p.course_id AND c.active=true WHERE p.user_id=? ORDER BY p.updated_at DESC,p.course_id LIMIT 1",String.class,user).stream().findFirst().map(courses::position).orElse(null);
        if(plan==null)plan=jdbc.queryForList("SELECT id FROM learning_plan WHERE user_id=? AND cycle_id=? ORDER BY created_at DESC,id DESC LIMIT 1",String.class,user,cycle).stream().findFirst().orElse(null);Object suggestion=null;
        if(plan!=null){var p=mapper.valueToTree(plans.get(plan,null));require(p.path("config").path("cycleId").asText().equals(cycle),ErrorCode.INVALID_RELATION,"计划不属于所选周期");Map<String,com.fasterxml.jackson.databind.JsonNode> tasks=new HashMap<>();p.path("tasks").forEach(t->tasks.put(t.path("id").asText(),t));outer:for(var day:p.path("days"))if(day.path("day").asText().equals(today().toString()))for(var segment:day.path("segments")){var t=tasks.get(segment.path("taskId").asText());if(!t.path("completed").asBoolean()){suggestion=obj("title",t.path("title").asText(),"reason","今日计划首个未完成任务","taskId",t.path("id").asText(),"target",t.path("target"));break outer;}}}
        List<Object> countdowns=new ArrayList<>();for(var c:cycleDto.courses()){if(courseList.stream().noneMatch(x->x.id().equals(c.courseId())))continue;var row=learning.course(c.courseId());LocalDate exam=c.examDate();Long remaining=exam==null?null:java.time.temporal.ChronoUnit.DAYS.between(today(),exam);countdowns.add(obj("courseId",row.id(),"courseCode",row.code(),"examDate",exam,"daysRemaining",remaining,"status",remaining==null?"DATE_UNKNOWN":remaining<0?"FINISHED":remaining==0?"TODAY":"UPCOMING"));}
        return obj("snapshotId",uuid(),"asOf",Instant.now(),"localDate",today(),"cycle",cycleDto,"continueLearning",position,"todaySuggestion",suggestion,"overallProgress",learning.overallProgress(user),"countdowns",countdowns,"courses",courseList,"selectedPlanId",plan,"todaySuggestionMessage",suggestion==null?"暂无今日安排":"今日建议："+((Map<?,?>)suggestion).get("title"));
    }
}
