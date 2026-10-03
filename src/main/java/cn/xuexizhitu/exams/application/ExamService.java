package cn.xuexizhitu.exams.application;
import cn.xuexizhitu.common.*;
import static cn.xuexizhitu.common.BusinessData.*;
import cn.xuexizhitu.exams.domain.ExamTypes.*;
import cn.xuexizhitu.exams.domain.ScoreAnalysis;
import cn.xuexizhitu.exams.infrastructure.ExamRepository;
import cn.xuexizhitu.files.application.PrivateFileStore;
import cn.xuexizhitu.files.domain.FileTypes.*;
import cn.xuexizhitu.audit.infrastructure.AuditRepository;
import cn.xuexizhitu.learning.infrastructure.LearningRepository;
import cn.xuexizhitu.learning.api.LearningDtos.Page;
import cn.xuexizhitu.assessment.api.AssessmentDtos.Unlock;
import cn.xuexizhitu.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true) public class ExamService {
    private final ExamRepository repository;
    private final LearningRepository learning;
    private final ExamAccess access;
    private final AuditRepository audit;
    private final PrivateFileStore files;
    private final RequestReplay replay;
    @Transactional     public Unlock confirmOverride(String course,String cycle,Confirm w) {
        require(Boolean.TRUE.equals(w.confirm()),ErrorCode.INVALID_PARAMETER,"需要确认");
        String user=CurrentUser.idOrThrow();
        learning.lockUser(user);
        Unlock unlock=access.unlock(user,course,cycle);
        if(unlock.activeOverride()!=null)return unlock;
        require(unlock.skipWindow().canConfirm(),ErrorCode.INVALID_DATE,unlock.skipWindow().reason());
        String id=uuid();
        repository.confirmOverride(id,user,course,cycle,unlock.skipWindow().examDate());
        audit.add("OVERRIDE_CONFIRMED","UNLOCK_OVERRIDE",id,null,obj("courseId",course,"cycleId",cycle));
        return access.unlock(user,course,cycle);
    }
    @Transactional     public Unlock revokeOverride(String course,String cycle,Revoke w) {
        require(Boolean.TRUE.equals(w.confirm()),ErrorCode.INVALID_PARAMETER,"需要确认");
        String user=CurrentUser.idOrThrow();
        learning.lockUser(user);
        var active=access.unlock(user,course,cycle).activeOverride();
        require(active!=null,ErrorCode.NOT_FOUND,"没有活动临考确认");
        require(active.revision()==w.expectedRevision(),ErrorCode.REVISION_CONFLICT,"临考确认修订号冲突");
        repository.revokeOverride(active.id());
        audit.add("OVERRIDE_REVOKED","UNLOCK_OVERRIDE",active.id(),null,obj("courseId",course,"cycleId",cycle));
        return access.unlock(user,course,cycle);
    }
    private Paper paperDto(PaperData r) {
        String month=r.paperMonth().toString().substring(0,7);
        return new Paper(r.id(),r.courseId(),r.code()+":"+month,month,r.sourceCourseCode(),files.metadata(r.questionFileId()),                 r.answerFileId()==null?null:files.metadata(r.answerFileId()),r.questionPages(),r.answerPages(),r.note());
    }
    public Paper paper(String course,String id) {
        CurrentUser.require();
        learning.course(course);
        return paperDto(repository.paper(course,id));
    }
    public Page<Paper> papers(String course,String cycle,int page,int size) {
        CurrentUser.require();
        learning.requireCycleCourse(course,cycle);
        var rows=repository.papers(course,page,size);
        return new Page<>(rows.items().stream().map(this::paperDto).toList(),page,size,rows.total());
    }
    public Download paperFile(String course,String paper,String cycle,String part) {
        access.requireWrite(CurrentUser.idOrThrow(),course,cycle);
        require(Set.of("QUESTION","ANSWER").contains(part),ErrorCode.INVALID_PARAMETER,"试卷部分无效");
        var p=repository.paper(course,paper);
        String id=part.equals("QUESTION")?p.questionFileId():p.answerFileId();
        require(id!=null,ErrorCode.NOT_FOUND,"参考答案文件未收录");
        require(files.metadata(id).purpose().equals("PAPER"),ErrorCode.NOT_FOUND,"文件用途无效");
        return files.download(id);
    }
    private ScoreRecord record(ScoreData r,ScoreAnalysis.Analysis a,List<Metadata> images) {
        return new ScoreRecord(r.id(),r.cycleId(),r.courseId(),r.paperId(),r.paperKey(),r.practicedOn(),r.score(),r.minutes(),r.limitMinutes(),                 r.complete(),r.closedBook(),r.answersSeenBefore(),r.source(),r.note(),r.createdAt(),r.updatedAt(),r.revision(),images,                 ScoreAnalysis.eligibility(r).isEmpty(),Objects.equals(r.id(),a.firstIds().get(r.paperId())),a.included().contains(r.id()),a.firstIds().get(r.paperId()),List.copyOf(a.reasons().get(r.id())));
    }
    public ScoreRecord score(String course,String id) {
        String user=CurrentUser.idOrThrow();
        ScoreData r=repository.owned(user,course,id);
        var analysis=ScoreAnalysis.analyze(course,repository.records(user,course),today());
        return record(r,analysis,repository.images(user,List.of(id)).getOrDefault(id,List.of()));
    }
    public Page<ScoreRecord> scores(String course,String cycle,String paper,int page,int size) {
        String user=CurrentUser.idOrThrow();
        learning.course(course);
        if(cycle!=null)learning.requireCycleCourse(course,cycle);
        if(paper!=null)repository.paper(course,paper);
        var selected=repository.selected(user,course,cycle,paper,page,size);
        var all=repository.records(user,course);
        var analysis=ScoreAnalysis.analyze(course,all,today());
        Map<String,ScoreData> byId=new HashMap<>();
        all.forEach(r->byId.put(r.id(),r));
        var images=repository.images(user,selected.ids());
        return new Page<>(selected.ids().stream().map(id->record(byId.get(id),analysis,images.getOrDefault(id,List.of()))).toList(),page,size,selected.total());
    }
    public Prediction prediction(String course) {
        learning.course(course);
        return ScoreAnalysis.analyze(course,repository.records(CurrentUser.idOrThrow(),course),today()).prediction();
    }
    public Trend trend(String course,int limit) {
        return new Trend(course,scores(course,null,null,1,limit).items());
    }
    public Page<Revision> revisions(String course,String id,int page,int size) {
        return repository.revisions(CurrentUser.idOrThrow(),course,id,page,size);
    }
    private void validate(ScoreFields fields) {
        require(!fields.practicedOn().isAfter(today())&&fields.practicedOn().getYear()>=1000,ErrorCode.INVALID_RELATION,"练习日期不能在未来且年份须不小于1000");
    }
    @Transactional     public Mutation createScore(String course,CreateScore w,String key) {
        String user=CurrentUser.idOrThrow();
        learning.lockUser(user);
        String operation="CREATE_SCORE:"+course,hash=replay.hash(w);
        Mutation previous=replay.find(user,operation,key,hash,Mutation.class);
        if(previous!=null)return previous;
        access.requireWrite(user,course,w.cycleId().toString());
        repository.paper(course,w.paperId().toString());
        validate(w.fields());
        String id=uuid();
        repository.create(id,user,course,w);
        repository.capture(id,user,"CREATED");
        audit.add("SCORE_CREATED","SCORE_RECORD",id,null,obj("revision",0,"courseId",course));
        Mutation result=new Mutation(score(course,id),prediction(course),access.unlock(user,course,w.cycleId().toString()));
        replay.save(user,operation,key,hash,result);
        return result;
    }
    @Transactional     public Mutation updateScore(String course,String id,UpdateScore w) {
        String user=CurrentUser.idOrThrow();
        learning.lockUser(user);
        ScoreData old=repository.owned(user,course,id);
        access.requireWrite(user,course,old.cycleId());
        validate(w.fields());
        repository.update(id,user,w);
        repository.capture(id,user,"REVISED");
        audit.add("SCORE_REVISED","SCORE_RECORD",id,null,obj("previousRevision",old.revision(),"revision",old.revision()+1));
        return new Mutation(score(course,id),prediction(course),access.unlock(user,course,old.cycleId()));
    }
    @Transactional     public Metadata uploadImage(String course,String score,MultipartFile file) {
        String user=CurrentUser.idOrThrow();
        learning.lockUser(user);
        var r=repository.owned(user,course,score);
        access.requireWrite(user,course,r.cycleId());
        String id=files.upload(file,"SCORE_IMAGE",user,false);
        repository.attach(user,score,id);
        return files.metadata(id);
    }
    private void requireImage(String course,String score,String file) {
        String user=CurrentUser.idOrThrow();
        repository.owned(user,course,score);
        require(repository.hasImage(user,score,file),ErrorCode.NOT_FOUND,"附件不属于当前成绩");
        var r=files.row(file);
        require(r.metadata().purpose().equals("SCORE_IMAGE")&&user.equals(r.ownerId()),ErrorCode.NOT_FOUND,"附件不属于当前用户");
    }
    public Download image(String course,String score,String file) {
        requireImage(course,score,file);
        return files.download(file);
    }
    @Transactional     public Deletion deleteImage(String course,String score,String file) {
        learning.lockUser(CurrentUser.idOrThrow());
        requireImage(course,score,file);
        require(files.metadata(file).state().equals("ACTIVE"),ErrorCode.NOT_FOUND,"文件不可用");
        files.delete(file);
        audit.add("SCORE_IMAGE_DELETED","STORED_FILE",file,null,obj("scoreId",score));
        return new Deletion(file,"DELETE_PENDING",false,Instant.now());
    }
}
