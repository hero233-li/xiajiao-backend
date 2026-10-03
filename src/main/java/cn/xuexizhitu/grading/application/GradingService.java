package cn.xuexizhitu.grading.application;
import cn.xuexizhitu.common.*;
import static cn.xuexizhitu.common.BusinessData.*;
import cn.xuexizhitu.grading.domain.*;
import cn.xuexizhitu.grading.domain.GradingTypes.*;
import cn.xuexizhitu.grading.infrastructure.GradingRepository;
import cn.xuexizhitu.grading.infrastructure.GradingRepository.TaskRow;
import cn.xuexizhitu.exams.application.ExamAccess;
import cn.xuexizhitu.exams.infrastructure.ExamRepository;
import cn.xuexizhitu.exams.domain.ExamTypes.CreateScore;
import cn.xuexizhitu.files.application.PrivateFileStore;
import cn.xuexizhitu.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
@Service @RequiredArgsConstructor public class GradingService {
    private final GradingRepository repo;
    private final ExamAccess access;
    private final ExamRepository exams;
    private final PrivateFileStore files;
    private final JdbcTemplate jdbc;
    public static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
    private String user() {
        return CurrentUser.idOrThrow();
    }
    private void revision(Submission s,long expected) {
        require(s.revision()==expected,ErrorCode.REVISION_CONFLICT,"答卷已修改，请重新读取");
    }
    public List<Submission> submissions(String course,String cycle) {
        access.unlock(user(),course,cycle);
        return repo.submissions(user(),course,cycle);
    }
    public Submission submission(String id) {
        return repo.submission(user(),id,false);
    }
    @Transactional public Submission create(SubmissionWrite w) {
        String user=user();
        repo.lockUser(user);
        access.requireWrite(user,w.courseId().toString(),w.cycleId().toString());
        exams.paper(w.courseId().toString(),w.paperId().toString());
        String id=uuid();
        repo.createSubmission(id,user,w);
        return repo.submission(user,id,false);
    }
    @Transactional public Submission upload(String id,long expected,MultipartFile file) {
        String user=user();
        repo.lockUser(user);
        Submission s=repo.submission(user,id,true);
        revision(s,expected);
        access.requireWrite(user,s.courseId(),s.cycleId());
        require(s.pages().size()<20,ErrorCode.PAYLOAD_TOO_LARGE,"每份答卷最多20张图片");
        String f=files.upload(file,"SCORE_IMAGE",user,false);
        require(Set.of("image/jpeg","image/png").contains(files.metadata(f).mimeType()),ErrorCode.UNSUPPORTED_MEDIA,"答卷仅支持可完整解码校验的JPG和PNG图片");
        repo.addPage(id,f,s.pages().size()+1);
        return repo.submission(user,id,false);
    }
    @Transactional public Submission order(String id,PageOrder w) {
        String user=user();
        repo.lockUser(user);
        Submission s=repo.submission(user,id,true);
        revision(s,w.expectedRevision());
        access.requireWrite(user,s.courseId(),s.cycleId());
        Set<String> requested=new HashSet<>(w.fileIds().stream().map(UUID::toString).toList());
        require(requested.size()==w.fileIds().size() && requested.equals(new HashSet<>(s.pages().stream().map(Page::fileId).toList())),ErrorCode.INVALID_RELATION,"页序必须包含且仅包含本答卷全部图片");
        repo.order(id,w.fileIds());
        return repo.submission(user,id,false);
    }
    @Transactional public Submission remove(String id,String file,long expected) {
        String user=user();
        repo.lockUser(user);
        Submission s=repo.submission(user,id,true);
        revision(s,expected);
        access.requireWrite(user,s.courseId(),s.cycleId());
        require(s.pages().stream().anyMatch(p->p.fileId().equals(file)),ErrorCode.NOT_FOUND,"图片不属于答卷");
        repo.removePage(id,file,s.pages().stream().filter(p->!p.fileId().equals(file)).map(p->UUID.fromString(p.fileId())).toList());
        // A task snapshot or completed score retains its original material.
        if(!repo.pinned(file))files.delete(file);
        return repo.submission(user,id,false);
    }
    public List<Rubric> rubrics(String course,String cycle,String paper) {
        access.requireWrite(user(),course,cycle);
        exams.paper(course,paper);
        return repo.rubrics(paper).stream().filter(r->r.state().equals("PUBLISHED")||CurrentUser.require().role()==cn.xuexizhitu.identity.domain.Role.ADMIN).toList();
    }
    @Transactional public Rubric createRubric(String course,String paper,RubricDocument doc) {
        String user=CurrentUser.requireAdmin().id();
        exams.paper(course,paper);
        GradingRules.rubric(doc);
        return repo.rubric(repo.createRubric(user,paper,doc),false);
    }
    @Transactional public Rubric updateRubric(String id,RubricDocument doc) {
        CurrentUser.requireAdmin();
        Rubric r=repo.rubric(id,true);
        require(r.state().equals("DRAFT"),ErrorCode.CONFLICT,"已发布标准不可修改，请新建版本");
        GradingRules.rubric(doc);
        repo.updateRubric(id,doc);
        return repo.rubric(id,false);
    }
    @Transactional public Rubric publish(String id) {
        CurrentUser.requireAdmin();
        Rubric r=repo.rubric(id,true);
        GradingRules.rubric(r.document());
        repo.publish(id);
        return repo.rubric(id,false);
    }
    @Transactional public Task draft(String course,String cycle,String paper) {
        String user=CurrentUser.requireAdmin().id();
        repo.lockUser(user);
        access.requireWrite(user,course,cycle);
        var p=exams.paper(course,paper);
        var duplicate=repo.duplicateDraft(user,paper);
        if(duplicate.isPresent())return task(duplicate.get());
        require(p.answerFileId()!=null,ErrorCode.INVALID_RELATION,"试卷缺少答案PDF，无法生成评分标准草稿");
        List<Page> pages=new ArrayList<>();
        int i=0;
        for(String f:List.of(p.questionFileId(),p.answerFileId())) {
            var m=files.metadata(f);
            require(m.state().equals("ACTIVE")&&m.mimeType().equals("application/pdf"),ErrorCode.INVALID_RELATION,"试卷或答案PDF不可用");
            pages.add(new Page(f,++i,m.name(),m.mimeType(),m.sizeBytes(),m.sha256()));
        }
        String id=uuid();
        repo.createTask(id,user,paper,null,null,null,"RUBRIC",new Inputs(course,cycle,null,null,pages));
        return task(id);
    }
    @Transactional public Task apply(String id,Apply w) {
        String user=user();
        repo.lockUser(user);
        Submission s=repo.submission(user,id,true);
        revision(s,w.expectedRevision());
        access.requireWrite(user,s.courseId(),s.cycleId());
        require(!w.practicedOn().isAfter(today()) && w.practicedOn().getYear()>=1000,ErrorCode.INVALID_DATE,"练习日期无效");
        var duplicate=repo.duplicate(id,s.revision());
        if(duplicate.isPresent()) {
            var t=repo.task(user,duplicate.get(),false);
            require(t.input().practice().equals(w),ErrorCode.IDEMPOTENCY_CONFLICT,"此版本已申请，练习信息与原申请不同");
            return task(duplicate.get());
        }
        require(!s.pages().isEmpty(),ErrorCode.INVALID_RELATION,"请先上传答题图片");
        for(Page p:s.pages()) {
            var stored=files.row(p.fileId());
            require(user.equals(stored.ownerId()) && stored.metadata().state().equals("ACTIVE"),ErrorCode.INVALID_RELATION,"图片不可用");
            files.download(p.fileId());
        }
        Rubric rubric=repo.published(s.paperId());
        String task=uuid();
        repo.createTask(task,user,s.paperId(),s.id(),s.revision(),rubric.id(),"GRADE",new Inputs(s.courseId(),s.cycleId(),w,rubric,s.pages()));
        return task(task);
    }
    public Inputs inputs(String id) {
        return repo.task(user(),id,false).input();
    }
    public Material ownMaterial(String id,String file) {
        var t=repo.task(user(),id,false);
        var page=t.input().pages().stream().filter(p->p.fileId().equals(file)).findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
        var d=files.download(file);
        require(d.file().sha256().equals(page.sha256()),ErrorCode.CONFLICT,"材料哈希与快照不一致");
        return new Material(file,page.pageNo(),page.mimeType(),page.sha256(),d.contentBase64());
    }
    public List<Task> tasks(String submission) {
        repo.submission(user(),submission,false);
        var workers=repo.workers(user());
        return repo.tasks(user(),submission).stream().map(row->view(row,workers)).toList();
    }
    public Task task(String id) {
        return view(repo.task(user(),id,false));
    }
    private Task view(TaskRow t) {
        return view(t,repo.workers(t.user()));
    }
    private Task view(TaskRow t,List<Worker> workers) {
        var online=workers.stream().anyMatch(w->w.online()&&!w.paused());
        String reason=online?"":workers.stream().filter(w->!w.revoked()&&w.online()&&w.paused()).map(w->Objects.toString(w.reason(),"已暂停")).findFirst().orElse("本地批改程序离线，任务等待处理");
        return new Task(t.id(),t.kind(),t.state(),t.submission(),t.rubricId(),t.model(),t.result(),t.draft(),t.score(),t.error(),t.leaseUntil(),online,reason);
    }
    public List<Worker> workers() {
        return repo.workers(user());
    }
    @Transactional public Pair pair(PairWrite w) {
        String user=user();
        repo.lockUser(user);
        byte[] secret=new byte[32];
        new SecureRandom().nextBytes(secret);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        String id=uuid();
        repo.pair(id,user,hash(token),w.label());
        return new Pair(id,token);
    }
    @Transactional public void revoke(String id) {
        repo.lockUser(user());
        repo.revoke(user(),id);
    }
    public WorkerPrincipal authenticate(String token) {
        require(token!=null&&token.length()>=40&&token.length()<=100,ErrorCode.UNAUTHORIZED,"需要工作程序配对凭证");
        return repo.authenticate(hash(token));
    }
    @Transactional public void heartbeat(WorkerPrincipal p,Heartbeat h) {
        repo.requireWorker(p);
        repo.heartbeat(p.id(),h);
    }
    @Transactional public Claim claim(WorkerPrincipal p) {
        repo.lockUser(p.userId());
        repo.requireWorker(p);
        var worker=repo.workers(p.userId()).stream().filter(w->w.id().equals(p.id())).findFirst().orElseThrow();
        if(worker.paused())return null;
        repo.recover(p.userId());
        // Personal first version: one live task per owner, including several paired devices.
        if(jdbc.queryForObject("SELECT COUNT(*) FROM grading_task WHERE user_id=? AND state='GRADING'",Integer.class,p.userId())>0)return null;
        var id=repo.claimId(p.userId());
        if(id.isEmpty())return null;
        repo.claim(id.get(),p.id(),uuid());
        var t=repo.task(p.userId(),id.get(),false);
        return new Claim(t.id(),t.kind(),t.lease(),t.leaseUntil(),t.deadline(),t.input());
    }
    private void lease(TaskRow t,WorkerPrincipal p,UUID nonce,boolean terminal) {
        require(p.id().equals(t.worker())&&nonce.toString().equals(t.lease()),ErrorCode.CONFLICT,"领取凭据已失效");
        if(!terminal)require(t.state().equals("GRADING")&&t.leaseUntil()!=null&&t.leaseUntil().isAfter(Instant.now())&&t.deadline().isAfter(Instant.now()),ErrorCode.CONFLICT,"任务租约已过期");
    }
    @Transactional public Claim renew(WorkerPrincipal p,String id,Lease l) {
        repo.requireWorker(p);
        var t=repo.task(p.userId(),id,true);
        lease(t,p,l.leaseToken(),false);
        repo.renew(id);
        t=repo.task(p.userId(),id,false);
        return new Claim(id,t.kind(),t.lease(),t.leaseUntil(),t.deadline(),t.input());
    }
    @Transactional public Material material(WorkerPrincipal p,String id,String file,UUID nonce) {
        repo.requireWorker(p);
        var t=repo.task(p.userId(),id,true);
        lease(t,p,nonce,false);
        var page=t.input().pages().stream().filter(x->x.fileId().equals(file)).findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
        var d=files.download(file);
        require(d.file().sha256().equals(page.sha256())&&d.file().sizeBytes()==page.sizeBytes(),ErrorCode.CONFLICT,"材料与申请快照不一致");
        return new Material(file,page.pageNo(),page.mimeType(),page.sha256(),d.contentBase64());
    }
    @Transactional public Task callback(WorkerPrincipal p,String id,Callback c) {
        repo.lockUser(p.userId());
        repo.requireWorker(p);
        var t=repo.task(p.userId(),id,true);
        String digest=hash(repo.encode(c));
        boolean terminal=Set.of("REVIEW","COMPLETED").contains(t.state());
        lease(t,p,c.leaseToken(),terminal);
        if(terminal) {
            require(digest.equals(t.hash()),ErrorCode.IDEMPOTENCY_CONFLICT,"已接收不同结果");
            return view(t);
        }
        if(t.kind().equals("RUBRIC")) {
            require(c.rubric()!=null&&c.result()==null,ErrorCode.INVALID_PARAMETER,"需要评分标准草稿");
            GradingRules.rubric(c.rubric());
            String rubric=repo.createRubric(p.userId(),t.paper(),c.rubric());
            jdbc.update("UPDATE grading_task SET rubric_id=? WHERE id=?",rubric,id);
            repo.result(id,"COMPLETED",c.rubric(),c.model(),digest,null);
        }
        else {
            require(c.result()!=null&&c.rubric()==null,ErrorCode.INVALID_PARAMETER,"需要逐题批改结果");
            var validation=GradingRules.result(t.input().rubric(),c.result(),t.input().pages().size());
            boolean review=validation.review()||!access.unlock(t.user(),t.input().courseId(),t.input().cycleId()).canWriteScores();
            String score=review?null:createScore(t,c.result(),validation.total(),c.model());
            repo.result(id,review?"REVIEW":"COMPLETED",c.result(),c.model(),digest,score);
        }
        return view(repo.task(p.userId(),id,false));
    }
    private String createScore(TaskRow t,Result result,BigDecimal total,String model) {
        access.requireWrite(t.user(),t.input().courseId(),t.input().cycleId());
        var w=t.input().practice();
        String score=uuid();
        exams.create(score,t.user(),t.input().courseId(),new CreateScore(UUID.fromString(t.input().cycleId()),UUID.fromString(t.paper()),w.practicedOn(),total,w.minutes(),w.limitMinutes(),w.complete(),w.closedBook(),w.answersSeenBefore(),"Codex批改任务 "+t.id()+"；模型 "+model+"；评分标准 "+t.input().rubric().id()));
        jdbc.update("UPDATE score_record SET source='CODEX' WHERE id=?",score);
        exams.capture(score,t.user(),"CREATED");
        for(Page p:t.input().pages()) {
            files.download(p.fileId());
            exams.attach(t.user(),score,p.fileId());
        }
        return score;
    }
    @Transactional public Task review(String id,Result result) {
        String user=user();
        repo.lockUser(user);
        var t=repo.task(user,id,true);
        if(t.state().equals("COMPLETED")&&repo.encode(t.result()).equals(repo.encode(result)))return view(t);
        require(t.state().equals("REVIEW")&&t.kind().equals("GRADE"),ErrorCode.CONFLICT,"任务不是待核对答卷");
        var validation=GradingRules.result(t.input().rubric(),result,t.input().pages().size());
        require(!validation.review(),ErrorCode.INVALID_RELATION,"确认前请处理全部待核对项");
        String score=createScore(t,result,validation.total(),t.model());
        repo.result(id,"COMPLETED",result,t.model(),t.hash(),score);
        return view(repo.task(user,id,false));
    }
    @Transactional public Task failure(WorkerPrincipal p,String id,Failure f) {
        repo.lockUser(p.userId());
        repo.requireWorker(p);
        var t=repo.task(p.userId(),id,true);
        lease(t,p,f.leaseToken(),false);
        if(f.pause())jdbc.update("UPDATE grading_task SET attempts=GREATEST(0,attempts-1) WHERE id=?",id);
        repo.fail(id,f.reason(),f.pause()||(f.retryable()&&t.attempts()<3));
        if(f.pause())repo.heartbeat(p.id(),new Heartbeat(true,f.reason()));
        return view(repo.task(p.userId(),id,false));
    }
}
