package cn.xuexizhitu.assessment.application;
import cn.xuexizhitu.assessment.api.AssessmentDtos.*;
import cn.xuexizhitu.assessment.infrastructure.*;
import cn.xuexizhitu.audit.infrastructure.AuditRepository;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.contract.WorkflowDtos.InvalidatePass;
import cn.xuexizhitu.exams.domain.ExamTypes.Confirm;
import cn.xuexizhitu.learning.infrastructure.LearningRepository;
import cn.xuexizhitu.learning.api.LearningDtos.Page;
import cn.xuexizhitu.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import static cn.xuexizhitu.common.BusinessData.require;
@Service @RequiredArgsConstructor @Transactional(readOnly=true) public class ModerationService {
    private final ModerationRepository repository;
    private final LearningRepository learning;
    private final AuditRepository audit;
    private final AssessmentRepository assessments;
    public record Invalidation(Pass pass,List<Unlock> affectedUnlocks,Instant evaluatedAt) {
    }
    public Page<Pass> passes(String course,String kind,String chapter,boolean valid,int page,int size) {
        learning.course(course);
        require(kind==null||Set.of("CHAPTER","MOCK").contains(kind),ErrorCode.INVALID_PARAMETER,"通过类型无效");
        return repository.passes(CurrentUser.idOrThrow(),course,kind,chapter,valid,page,size);
    }
    @Transactional public Invalidation invalidate(String id,InvalidatePass w) {
        CurrentUser.requireAdmin();
        require(Boolean.TRUE.equals(w.confirm())&&!w.reason().isBlank(),ErrorCode.INVALID_PARAMETER,"须确认并填写作废原因");
        var owner=repository.pass(id,false);
        learning.lockUser(owner.userId());
        var locked=repository.pass(id,true);
        require(locked.pass().invalidatedAt()==null,ErrorCode.CONFLICT,"通过记录已作废");
        repository.invalidate(id,CurrentUser.idOrThrow(),w.reason());
        audit.add("PASS_INVALIDATED","ASSESSMENT_PASS",id,w.reason(),Map.of("courseId",locked.courseId(),"ownerId",locked.userId()));
        String release=learning.course(locked.courseId()).releaseId();
        var unlocks=repository.enrolledCycles(locked.userId(),locked.courseId()).stream().map(c->assessments.unlock(locked.userId(),locked.courseId(),c,release)).toList();
        return new Invalidation(repository.pass(id,false).pass(),unlocks,Instant.now());
    }
    public Page<ModerationRepository.Alert> alerts(String course,Boolean acknowledged,int page,int size) {
        CurrentUser.requireAdmin();
        return repository.alerts(course,acknowledged,page,size);
    }
    @Transactional public ModerationRepository.Alert acknowledge(String id,Confirm w) {
        CurrentUser.requireAdmin();
        require(Boolean.TRUE.equals(w.confirm()),ErrorCode.INVALID_PARAMETER,"须确认处理");
        repository.alert(id,true);
        repository.acknowledge(id,CurrentUser.idOrThrow());
        audit.add("BANK_ALERT_ACKNOWLEDGED","BANK_ALERT",id,null,Map.of());
        return repository.alert(id,false);
    }
    public Page<ModerationRepository.Audit> audits(String action,String target,int page,int size) {
        CurrentUser.requireAdmin();
        return repository.audits(action,target,page,size);
    }
}
