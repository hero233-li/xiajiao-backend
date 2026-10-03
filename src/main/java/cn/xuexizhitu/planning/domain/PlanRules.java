package cn.xuexizhitu.planning.domain;
import cn.xuexizhitu.common.*;
import static cn.xuexizhitu.common.BusinessData.require;
import cn.xuexizhitu.contract.WorkflowDtos.PlanConfig;
import cn.xuexizhitu.planning.domain.PlanTypes.*;
import cn.xuexizhitu.learning.api.LearningDtos.Course;
import java.time.*;
import java.util.*;
public final class PlanRules {
    private PlanRules() {
    }
    public static boolean weekly(PlanConfig c) {
        return "WEEKLY_35".equals(c.strategy());
    }
    public static Map<LocalDate,Integer> capacities(PlanConfig c) {
        Map<LocalDate,Integer> result=new TreeMap<>();
        for(var day:c.dayCapacities())require(result.put(day.day(),day.capacityMinutes())==null,ErrorCode.INVALID_RELATION,"每日容量日期重复");
        return result;
    }
    public static void validate(PlanConfig c) {
        long days=java.time.temporal.ChronoUnit.DAYS.between(c.startDate(),c.endDate());
        require(days>=0&&days<=3660,ErrorCode.INVALID_RELATION,"计划日期范围无效或超过十年");
        require(c.startDate().getYear()>=1000&&c.endDate().getYear()<=9999,ErrorCode.INVALID_RELATION,"计划日期年份无效");
        require(new HashSet<>(c.courseScope()).size()==c.courseScope().size()&&new HashSet<>(c.coursePriority()).size()==c.coursePriority().size()                 &&new HashSet<>(c.courseScope()).equals(new HashSet<>(c.coursePriority())),ErrorCode.INVALID_RELATION,"科目优先级须完整覆盖所选科目且不能重复");
        var caps=capacities(c);
        for(var day:caps.entrySet())require(!day.getKey().isBefore(c.startDate())&&!day.getKey().isAfter(c.endDate())&&day.getValue()>=0&&day.getValue()<=1440,ErrorCode.INVALID_RELATION,"容量日期或分钟不在范围");
        if(weekly(c))require(days==34&&c.courseScope().size()==4&&caps.size()==35,ErrorCode.INVALID_RELATION,"五周计划须完整35天、4门理论课、逐日配置容量");
    }
    public static int gap(List<Segment> segments) {
        return segments.stream().filter(s->!s.state().equals("SCHEDULED")).mapToInt(Segment::minutes).sum();
    }
    public static int percent(int done,int total) {
        return total==0?0:(int)Math.round(done*100.0/total);
    }
    public static Plan assemble(String id,int revision,PlanConfig c,List<Task> tasks,List<Segment> segments,             Instant created,Instant confirmed,LocalDate today,List<Course> summaries) {
        Map<String,Task> byId=new HashMap<>();
        tasks.forEach(t->byId.put(t.id(),t));
        int total=tasks.stream().mapToInt(Task::estimatedMinutes).sum(),done=tasks.stream().filter(Task::completed).mapToInt(Task::estimatedMinutes).sum();
        List<Segment> unscheduled=new ArrayList<>(),awaiting=new ArrayList<>();
        int overdue=0;
        for(Segment s:segments) {
            require(byId.containsKey(s.taskId()),ErrorCode.INVALID_RELATION,"计划段缺少快照任务");
            if(s.state().equals("UNSCHEDULED"))unscheduled.add(s);
            if(s.state().equals("AWAITING_DATE"))awaiting.add(s);
            if(s.scheduledOn()!=null&&s.scheduledOn().isBefore(today)&&!byId.get(s.taskId()).completed())overdue+=s.minutes();
        }
        List<Day> days=new ArrayList<>();
        var caps=capacities(c);
        int completedDays=0;
        Map<LocalDate,List<Segment>> perDay=new HashMap<>();
        for(Segment s:segments)if(s.scheduledOn()!=null)perDay.computeIfAbsent(s.scheduledOn(),d->new ArrayList<>()).add(s);
        for(LocalDate d=c.startDate();!d.isAfter(c.endDate());d=d.plusDays(1)) {
            var scheduled=perDay.getOrDefault(d,List.of());
            int used=scheduled.stream().mapToInt(Segment::minutes).sum();
            int completed=scheduled.stream().filter(s->byId.get(s.taskId()).completed()).mapToInt(Segment::minutes).sum();
            if(used>0&&used==completed)completedDays++;
            days.add(new Day(d,caps.getOrDefault(d,0),used,Math.max(0,caps.getOrDefault(d,0)-used),scheduled,completed,percent(completed,used)));
        }
        List<Week> weeks=new ArrayList<>();
        for(int offset=0,index=1;offset<days.size();offset+=7,index++) {
            var group=days.subList(offset,Math.min(offset+7,days.size()));
            int used=group.stream().mapToInt(Day::reservedMinutes).sum(),completed=group.stream().mapToInt(Day::completedMinutes).sum();
            weeks.add(new Week(index,group.get(0).day(),group.get(group.size()-1).day(),used,completed,percent(completed,used)));
        }
        return new Plan(id,revision,c,tasks,days,unscheduled,awaiting,new PlanProgress(done,total,percent(done,total)),created,confirmed,Instant.now(),weeks,completedDays,days.size(),overdue,summaries);
    }
    public record Rescheduled(List<Segment> segments,List<Move> moves,int gap) {
    }
    public static Rescheduled reschedule(PlanConfig c,List<Task> tasks,List<Segment> original,Map<String,LocalDate> deadlines,LocalDate today) {
        Map<String,Task> byId=new HashMap<>();
        tasks.forEach(t->byId.put(t.id(),t));
        List<Segment> fixed=new ArrayList<>(),moving=new ArrayList<>();
        Map<LocalDate,Integer> reserved=new HashMap<>();
        int order=0;
        for(Segment s:original) {
            if(s.scheduledOn()!=null&&s.scheduledOn().isBefore(today)&&!byId.get(s.taskId()).completed())moving.add(s);
            else {
                fixed.add(s.copy());
                order=Math.max(order,s.sortOrder()+1);
                if(s.scheduledOn()!=null)reserved.merge(s.scheduledOn(),s.minutes(),Integer::sum);
            }
        }
        var caps=capacities(c);
        for(var e:reserved.entrySet())if(!e.getKey().isBefore(today))require(caps.getOrDefault(e.getKey(),0)>=e.getValue(),ErrorCode.INVALID_RELATION,"新容量不能小于今天或未来已有安排");
        Map<String,Integer> priority=new HashMap<>();
        for(int i=0;i<c.coursePriority().size();i++)priority.put(c.coursePriority().get(i).toString(),i);
        moving.sort(Comparator.<Segment,Integer>comparing(s->weekly(c)&&!byId.get(s.taskId()).kind().equals("ITEM")?4:priority.get(byId.get(s.taskId()).courseId()))                 .thenComparing(s->priority.get(byId.get(s.taskId()).courseId())).thenComparing(Segment::sortOrder));
        // Allocate each old segment separately to preserve the exact move relationship, including split work.
        List<Segment> added=new ArrayList<>();
        List<Move> moves=new ArrayList<>();
        for(Segment s:moving) {
            Task t=byId.get(s.taskId());
            var work=new PlanAllocator.Work(t.id(),s.minutes(),deadlines.get(t.courseId()));
            List<Segment> replacements;
            if(weekly(c))replacements=WeeklyPlanAllocator.allocate(List.of(new WeeklyPlanAllocator.Work(work,t.courseId(),t.kind())),c.coursePriority().stream().map(UUID::toString).toList(),caps,reserved,c.startDate(),today.plusDays(1),order);
            else replacements=PlanAllocator.allocate(List.of(work),caps,reserved,today.plusDays(1),c.endDate(),order);
            added.addAll(replacements);
            order+=replacements.size();
            for(Segment r:replacements)if(r.scheduledOn()!=null)reserved.merge(r.scheduledOn(),r.minutes(),Integer::sum);
            moves.add(new Move(s.id(),s.scheduledOn(),replacements));
        }
        List<Segment> all=new ArrayList<>(fixed);
        all.addAll(added);
        return new Rescheduled(all,moves,gap(added));
    }
}
