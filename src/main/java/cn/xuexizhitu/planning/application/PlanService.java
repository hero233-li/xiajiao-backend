package cn.xuexizhitu.planning.application;
import cn.xuexizhitu.common.*;
import static cn.xuexizhitu.common.BusinessData.*;
import cn.xuexizhitu.contract.WorkflowDtos.PlanConfig;
import cn.xuexizhitu.contract.WorkflowDtos.PlanCreate;
import cn.xuexizhitu.contract.WorkflowDtos.PreviewConfirm;
import cn.xuexizhitu.contract.WorkflowDtos.RescheduleRequest;
import cn.xuexizhitu.contract.WorkflowDtos.TaskCompletionWrite;
import cn.xuexizhitu.planning.domain.*;
import cn.xuexizhitu.planning.domain.PlanTypes.*;
import cn.xuexizhitu.planning.infrastructure.PlanRepository;
import cn.xuexizhitu.learning.api.LearningDtos.Page;
import cn.xuexizhitu.learning.api.LearningDtos.CourseType;
import cn.xuexizhitu.audit.infrastructure.AuditRepository;
import cn.xuexizhitu.learning.infrastructure.LearningRepository;
import cn.xuexizhitu.learning.application.LearningService;
import cn.xuexizhitu.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.math.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true) public class PlanService {
    private final PlanRepository repository;
    private final LearningRepository learning;
    private final LearningService courses;
    private final AuditRepository audit;
    private final RequestReplay hashing;
    private void validate(PlanConfig c) {
        PlanRules.validate(c);
        for(UUID course:c.courseScope()) {
            learning.requireCycleCourse(course.toString(),c.cycleId().toString());
            if(PlanRules.weekly(c))require(learning.course(course.toString()).type()==CourseType.THEORY,ErrorCode.INVALID_RELATION,"五周计划只选择4门理论课");
        }
    }
    private List<Task> sources(PlanConfig c) {
        List<Task> tasks=new ArrayList<>();
        String user=CurrentUser.idOrThrow();
        for(UUID course:c.coursePriority()) {
            var row=learning.course(course.toString());
            require(row.releaseId()!=null,ErrorCode.INVALID_RELATION,"所选科目没有已发布内容");
            tasks.addAll(repository.sourceItems(user,row.id(),row.releaseId(),row.code()));
            var templates=repository.sourceTemplates(row.id(),row.releaseId(),row.code());
            require(!PlanRules.weekly(c)||templates.stream().anyMatch(t->t.kind().equals("REVIEW")),ErrorCode.INVALID_RELATION,"五周计划缺少管理员维护的复习任务模板，请先补齐模板和估时");
            tasks.addAll(templates);
        }
        return tasks;
    }
    private static List<Task> scope(List<String> ids,List<Task> all) {
        if(ids==null)return all;
        Map<String,Task> byId=new HashMap<>();
        all.forEach(t->byId.put(t.id(),t));
        List<Task> result=new ArrayList<>();
        for(String id:ids) {
            Task task=byId.get(id);
            require(task!=null,ErrorCode.INVALID_RELATION,"历史计划任务缺失");
            result.add(task);
        }
        return result;
    }
    private List<Task> tasks(String id,int revision) {
        return scope(repository.revision(id,revision).snapshot().taskIds(),repository.allTasks(CurrentUser.idOrThrow(),id));
    }
    private List<Segment> allocate(PlanConfig c,List<Task> tasks) {
        var deadlines=repository.deadlines(c);
        var work=tasks.stream().map(t->new PlanAllocator.Work(t.id(),t.estimatedMinutes(),deadlines.get(t.courseId()))).toList();
        if(!PlanRules.weekly(c))return PlanAllocator.allocate(work,PlanRules.capacities(c),Map.of(),c.startDate(),c.endDate(),0);
        Map<String,Task> byId=new HashMap<>();
        tasks.forEach(t->byId.put(t.id(),t));
        return WeeklyPlanAllocator.allocate(work.stream().map(w-> {
            Task t=byId.get(w.task());return new WeeklyPlanAllocator.Work(w,t.courseId(),t.kind());
        }
        ).toList(),                 c.coursePriority().stream().map(UUID::toString).toList(),PlanRules.capacities(c),Map.of(),c.startDate(),c.startDate(),0);
    }
    private Plan assemble(Row p,int revision,PlanConfig c,List<Task> tasks,List<Segment> segments,Instant confirmed) {
        var summaries=c.courseScope().stream().map(course->courses.historicalCourse(course.toString(),c.cycleId().toString())).toList();
        return PlanRules.assemble(p.id(),revision,c,tasks,segments,p.createdAt(),confirmed,today(),summaries);
    }
    public Plan get(String id,Integer revision) {
        Row p=repository.owned(CurrentUser.idOrThrow(),id,false);
        int no=revision==null?p.revision():revision;
        var r=repository.revision(id,no);
        return assemble(p,no,r.snapshot().config(),scope(r.snapshot().taskIds(),repository.allTasks(p.userId(),id)),repository.segments(id,no),r.confirmedAt());
    }
    public Page<Summary> list(String cycle,int page,int size) {
        if(cycle!=null)learning.requireCycle(cycle);
        return repository.list(CurrentUser.idOrThrow(),cycle,page,size);
    }
    @Transactional     public Plan create(PlanCreate w) {
        String user=CurrentUser.idOrThrow();
        learning.lockUser(user);
        validate(w.config());
        var tasks=sources(w.config());
        var segments=allocate(w.config(),tasks);
        int gap=PlanRules.gap(segments);
        require(gap==0||Boolean.TRUE.equals(w.acceptUnscheduled()),ErrorCode.PLAN_GAP,"计划未排入"+gap+"分钟，请调整配置或明确接受");
        String id=uuid();
        repository.create(id,user,w.config());
        repository.insertTasks(id,tasks);
        repository.saveRevision(id,1,w.config(),tasks,segments,Instant.now());
        return get(id,null);
    }
    @Transactional     public Completion complete(String id,String taskId,TaskCompletionWrite w) {
        String user=CurrentUser.idOrThrow();
        learning.lockUser(user);
        String hash=hashing.hash(new CompletionRequest(id,taskId,w)),key=w.clientMutationId().toString();
        Completion previous=hashing.find(user,"PLAN_TASK_COMPLETION",key,hash,Completion.class);
        if(previous!=null)return previous;
        Row p=repository.owned(user,id,true);
        require(p.revision()==w.baseRevision(),ErrorCode.CONFLICT,"计划版本已变化");
        Task task=tasks(id,p.revision()).stream().filter(t->t.id().equals(taskId)).findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
        require(task.completed()==w.expectedCompleted(),ErrorCode.CONFLICT,"任务完成状态已变化");
        List<String> affected;
        if(task.kind().equals("ITEM")) {
            require(w.expectedItemRevision()!=null,ErrorCode.INVALID_RELATION,"目录任务需提供条目修订号");
            String release=learning.course(task.courseId()).releaseId();
            affected=learning.complete(user,release,task.itemId(),w.completed(),w.expectedItemRevision(),Instant.now());
        }
        else {
            require(w.expectedItemRevision()==null,ErrorCode.INVALID_RELATION,"非目录任务无条目修订号");
            repository.completeTask(id,taskId,w.completed());
            affected=List.of(id);
        }
        Task updated=tasks(id,p.revision()).stream().filter(t->t.id().equals(taskId)).findFirst().orElseThrow();
        Completion result=new Completion(updated,p.revision(),learning.progress(user,learning.course(task.courseId()).releaseId()),learning.overallProgress(user),affected,w.clientMutationId(),Instant.now());
        hashing.save(user,"PLAN_TASK_COMPLETION",key,hash,result);
        return result;
    }
    private record CompletionRequest(String plan,String task,TaskCompletionWrite write) {
    }
    private record Fingerprint(int revision,LocalDate today,PlanConfig config,List<Task> tasks,Map<String,LocalDate> exams,List<PlanRepository.CapacityState> capacities) {
    }
    private String fingerprint(String id,int revision,PlanConfig c,List<Task> tasks) {
        return hashing.hash(new Fingerprint(revision,today(),c,tasks,repository.deadlines(c),repository.capacityState(id,revision)));
    }
    private record CourseGuard(String course,String release,LocalDate exam,List<PlanRepository.ProgressState> progress) {
    }
    private String configurationGuard(PlanConfig c) {
        var deadlines=repository.deadlines(c);
        return hashing.hash(c.coursePriority().stream().map(id-> {
            var course=learning.course(id.toString());return new CourseGuard(course.id(),course.releaseId(),deadlines.get(course.id()),repository.progressState(CurrentUser.idOrThrow(),course.releaseId()));
        }
        ).toList());
    }
    private static PlanConfig adjusted(PlanConfig c,RescheduleRequest w) {
        return new PlanConfig(c.name(),c.strategy(),c.cycleId(),c.startDate(),c.endDate(),w.coursePriority()==null?c.coursePriority():w.coursePriority(),                 w.dayCapacities()==null?c.dayCapacities():w.dayCapacities().stream().map(d->new cn.xuexizhitu.contract.WorkflowDtos.PlanConfigDayCapacitiesItem(d.day(),d.capacityMinutes())).toList(),c.courseScope());
    }
    @Transactional     public Preview preview(String id,RescheduleRequest w) {
        String user=CurrentUser.idOrThrow();
        learning.lockUser(user);
        Row row=repository.owned(user,id,true);
        require(row.revision()==w.baseRevision(),ErrorCode.CONFLICT,"计划版本已变化");
        Plan old=get(id,row.revision());
        boolean configuration=w.config()!=null;
        require(!configuration||(w.dayCapacities()==null&&w.coursePriority()==null),ErrorCode.INVALID_RELATION,"完整编辑与逾期顺延配置不能混用");
        PlanConfig config=configuration?w.config():adjusted(old.config(),w);
        validate(config);
        require(config.cycleId().equals(old.config().cycleId()),ErrorCode.INVALID_RELATION,"编辑计划不能更换考试周期");
        List<Task> proposedTasks=new ArrayList<>(),newTasks=new ArrayList<>();
        List<Segment> segments;
        List<Move> moves;
        int gap;
        if(configuration) {
            Map<String,Task> lookup=new HashMap<>();
            repository.allTasks(user,id).forEach(t->lookup.put(t.key(),t));
            for(Task source:sources(config)) {
                Task existing=lookup.get(source.key());
                Task proposed=existing==null?source:source.reuse(existing);
                proposedTasks.add(proposed);
                if(existing==null)newTasks.add(proposed);
            }
            segments=allocate(config,proposedTasks);
            moves=List.of();
            gap=PlanRules.gap(segments);
        }
        else {
            proposedTasks.addAll(old.tasks());
            var result=PlanRules.reschedule(config,proposedTasks,repository.segments(id,row.revision()),repository.deadlines(config),today());
            segments=result.segments();
            moves=result.moves();
            gap=result.gap();
        }
        String preview=uuid(),fp=fingerprint(id,row.revision(),old.config(),old.tasks());
        Plan proposed=assemble(row,row.revision()+1,config,proposedTasks,segments,null);
        Preview result=new Preview(preview,id,row.revision(),today(),fp,proposed,moves,gap,BigDecimal.valueOf(gap).divide(BigDecimal.valueOf(60),2,RoundingMode.HALF_UP),List.of("INCREASE_DAILY_TIME","CHANGE_COURSE_PRIORITY","ACCEPT_UNSCHEDULED"),Instant.now());
        repository.preview(preview,id,user,row.revision(),fp,new PreviewSnapshot(result,config,segments,old.config(),configuration,newTasks,proposedTasks.stream().map(Task::id).toList(),configuration?configurationGuard(config):null));
        return result;
    }
    public Preview previewGet(String id,String preview) {
        repository.owned(CurrentUser.idOrThrow(),id,false);
        return repository.preview(CurrentUser.idOrThrow(),id,preview,false).snapshot().response();
    }
    @Transactional     public Plan confirm(String id,String preview,PreviewConfirm w) {
        require(Boolean.TRUE.equals(w.confirm()),ErrorCode.INVALID_PARAMETER,"需要确认");
        String user=CurrentUser.idOrThrow();
        learning.lockUser(user);
        Row p=repository.owned(user,id,true);
        PreviewRow r=repository.preview(user,id,preview,true);
        require(r.confirmedAt()==null,ErrorCode.CONFLICT,"预览已确认");
        require(r.baseRevision()==p.revision()&&r.baseRevision()==w.baseRevision()&&r.asOf().equals(today()),ErrorCode.CONFLICT,"原版本或预览日期已变化");
        var snapshot=r.snapshot();
        var active=tasks(id,p.revision());
        String fp=fingerprint(id,p.revision(),snapshot.oldConfig(),active);
        require(fp.equals(r.inputFingerprint())&&fp.equals(w.inputFingerprint()),ErrorCode.CONFLICT,"进度、考试日或配置已变化，请重新预览");
        require(r.gapMinutes()==0||Boolean.TRUE.equals(w.acceptUnscheduled()),ErrorCode.PLAN_GAP,"仍有未排入任务，请调整或明确接受");
        if(Boolean.TRUE.equals(snapshot.configuration())) {
            require(Objects.equals(snapshot.guard(),configurationGuard(snapshot.config())),ErrorCode.CONFLICT,"课程、考试日期或学习记录已变化，请重新预览");
            validate(snapshot.config());
            repository.freezeLegacyTaskSets(id,repository.allTasks(user,id));
            repository.insertTasks(id,snapshot.newTasks());
            active=scope(snapshot.taskIds(),repository.allTasks(user,id));
            repository.updateConfig(id,snapshot.config());
        }
        repository.saveRevision(id,p.revision()+1,snapshot.config(),active,snapshot.segments(),Instant.now());
        repository.confirm(preview);
        audit.add(Boolean.TRUE.equals(snapshot.configuration())?"PLAN_CONFIGURATION_CONFIRMED":"PLAN_RESCHEDULE_CONFIRMED","LEARNING_PLAN",id,null,obj("baseRevision",p.revision(),"newRevision",p.revision()+1,"gapMinutes",r.gapMinutes()));
        return get(id,p.revision()+1);
    }
}
