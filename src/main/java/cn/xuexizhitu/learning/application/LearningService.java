package cn.xuexizhitu.learning.application;
import cn.xuexizhitu.content.infrastructure.ContentRepository;
import cn.xuexizhitu.practice.infrastructure.PracticeRepository;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.learning.api.LearningDtos.*;
import cn.xuexizhitu.learning.infrastructure.LearningRepository;
import cn.xuexizhitu.learning.infrastructure.LearningRepository.CourseRow;
import cn.xuexizhitu.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true) public class LearningService {
    private final LearningRepository repository;
    private final RequestReplay replay;
    private record CompletionRequest(String course,String item,Object write) {
    }
    private final cn.xuexizhitu.content.infrastructure.ContentRepository content;
    private final cn.xuexizhitu.practice.infrastructure.PracticeRepository practice;
    public Page<Course> courses(String cycle,CourseType type,int page,int size) {
        repository.requireCycle(cycle);
        String user=CurrentUser.idOrThrow();
        return new Page<>(repository.courses(cycle,type,page,size).stream().map(c->courseDto(user,c,cycle)).toList(),page,size,repository.courseCount(cycle,type));
    }
    public Course course(String id,String cycle) {
        return courseDto(CurrentUser.idOrThrow(),repository.course(id),cycle);
    }
    public Course historicalCourse(String id,String cycle) {
        return courseDto(CurrentUser.idOrThrow(),repository.historicalCourse(id),cycle);
    }
    public Course courseByCode(String code,String cycle) {
        return courseDto(CurrentUser.idOrThrow(),repository.courseByCode(code),cycle);
    }
    private Course courseDto(String user,CourseRow c,String cycle) {
        repository.requireCycleCourse(c.id(),cycle);
        // Only advertise modules which this backend currently serves.
        return new Course(c.id(),c.code(),c.name(),c.type(),c.active(),c.releaseId(),repository.progress(user,c.releaseId()),repository.enrollment(user,c.id(),cycle),new Capabilities(c.releaseId()!=null,c.releaseId()!=null&&content.hasKnowledge(c.releaseId()),c.releaseId()!=null&&practice.hasPractice(c.releaseId()),c.type()==CourseType.THEORY,c.releaseId()!=null&&content.hasManual(c.releaseId())));
    }
    public Page<ExamCycle> cycles(int page,int size) {
        CurrentUser.require();
        return repository.cycles(page,size);
    }
    public ExamCycle cycle(String id) {
        CurrentUser.require();
        return repository.cycle(id);
    }
    public Enrollment enrollment(String course,String cycle) {
        repository.course(course);
        repository.requireCycleCourse(course,cycle);
        return repository.enrollment(CurrentUser.idOrThrow(),course,cycle);
    }
    @Transactional     public Enrollment saveEnrollment(String course,String cycle,EnrollmentWrite write) {
        String user=CurrentUser.idOrThrow();
        repository.lockUser(user);
        repository.course(course);
        repository.requireCycleCourse(course,cycle);
        if(write.passedMonth()!=null && write.passedMonth().startsWith("0000")) throw new BusinessException(ErrorCode.INVALID_RELATION,"通过年月不合法");
        repository.writeEnrollment(user,course,cycle,write);
        return repository.enrollment(user,course,cycle);
    }
    private CourseRow publishedCourse(String id) {
        CourseRow c=repository.course(id);
        if(c.releaseId()==null) throw new BusinessException(ErrorCode.NOT_FOUND,"课程暂无发布内容");
        return c;
    }
    public Catalog catalog(String course) {
        String user=CurrentUser.idOrThrow();
        CourseRow c=publishedCourse(course);
        return new Catalog(course,c.releaseId(),repository.chapters(user,c.releaseId()),repository.progress(user,c.releaseId()),repository.overallProgress(user),Instant.now());
    }
    @Transactional     public CatalogCompletion complete(String course,String item,CompletionWrite write) {
        String user=CurrentUser.idOrThrow();
        repository.lockUser(user);
        String hash=replay.hash(new CompletionRequest(course,item,write)),key=write.clientMutationId().toString();
        CatalogCompletion previous=replay.find(user,"CATALOG_COMPLETION",key,hash,CatalogCompletion.class);
        if(previous!=null)return previous;
        CourseRow c=publishedCourse(course);
        Instant now=Instant.now();
        List<String> plans=repository.complete(user,c.releaseId(),item,write.completed(),write.expectedRevision(),now);
        CatalogCompletion result=new CatalogCompletion(repository.item(user,c.releaseId(),item),repository.progress(user,c.releaseId()),repository.overallProgress(user),plans,write.clientMutationId(),now);
        replay.save(user,"CATALOG_COMPLETION",key,hash,result);
        return result;
    }
    @Transactional     public BatchCompletion completeBatch(String course,BatchCompletionWrite write) {
        if(write.updates().stream().map(ItemUpdate::itemId).distinct().count()!=write.updates().size()) throw new BusinessException(ErrorCode.INVALID_PARAMETER,"条目ID不能重复");
        String user=CurrentUser.idOrThrow();
        repository.lockUser(user);
        String hash=replay.hash(new CompletionRequest(course,null,write)),key=write.clientMutationId().toString();
        BatchCompletion previous=replay.find(user,"CATALOG_BATCH_COMPLETION",key,hash,BatchCompletion.class);
        if(previous!=null)return previous;
        CourseRow c=publishedCourse(course);
        Instant now=Instant.now();
        Set<String> plans=new TreeSet<>();
        for(ItemUpdate w:write.updates()) plans.addAll(repository.complete(user,c.releaseId(),w.itemId().toString(),w.completed(),w.expectedRevision(),now));
        BatchCompletion result=new BatchCompletion(write.updates().stream().map(w->repository.item(user,c.releaseId(),w.itemId().toString())).toList(),repository.progress(user,c.releaseId()),repository.overallProgress(user),List.copyOf(plans),write.clientMutationId(),now);
        replay.save(user,"CATALOG_BATCH_COMPLETION",key,hash,result);
        return result;
    }
    public LearningPosition position(String course) {
        return repository.position(CurrentUser.idOrThrow(),repository.course(course));
    }
    @Transactional     public LearningPosition savePosition(String course,LearningPositionWrite write) {
        String user=CurrentUser.idOrThrow();
        repository.lockUser(user);
        CourseRow c=repository.course(course);
        Navigation t=repository.validateNavigation(c,write.target());
        repository.writePosition(user,c,t,Instant.now());
        return repository.position(user,c);
    }
}
