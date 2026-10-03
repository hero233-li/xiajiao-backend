package cn.xuexizhitu.archive.application;
import cn.xuexizhitu.archive.domain.ArchiveTypes.*;
import cn.xuexizhitu.archive.infrastructure.ArchiveRepository;
import cn.xuexizhitu.files.application.PrivateFileStore;
import cn.xuexizhitu.files.domain.FileTypes.*;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.audit.infrastructure.AuditRepository;
import cn.xuexizhitu.learning.infrastructure.LearningRepository;
import cn.xuexizhitu.learning.api.LearningDtos.Page;
import cn.xuexizhitu.practice.infrastructure.PracticeRepository;
import cn.xuexizhitu.practice.api.PracticeDtos.LegacySummary;
import cn.xuexizhitu.contract.WorkflowDtos.LegacyReviewDecision;
import cn.xuexizhitu.contract.WorkflowDtos.LegacyPracticeCreditDecision;
import cn.xuexizhitu.security.CurrentUser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static cn.xuexizhitu.common.BusinessData.require;
@Service @RequiredArgsConstructor @Transactional(readOnly=true) public class LegacyService {
    private final ArchiveRepository repository;
    private final ObjectMapper mapper;
    private final PrivateFileStore files;
    private final AuditRepository audit;
    private final LearningRepository learning;
    private final PracticeRepository practice;
    private static Summary summary(StoredRecord r) {
        JsonNode payload=r.payload();
        Instant created=null;
        try {
            created=Instant.parse(payload.path("createdAt").asText());
        }
        catch(java.time.format.DateTimeParseException ignored) {
        }
        return new Summary(r.id(),r.kind(),r.courseId(),payload.path("title").asText("旧"+r.kind()+"记录"),created,true,r.fileCount());
    }
    public Page<Summary> history(String kind,String course,int page,int size) {
        require(kind==null||Set.of("ability","grading","sprint","practice","task","practice_request","other").contains(kind),ErrorCode.INVALID_PARAMETER,"历史类型无效");
        var rows=repository.records(CurrentUser.idOrThrow(),kind,course,page,size);
        return new Page<>(rows.items().stream().map(LegacyService::summary).toList(),page,size,rows.total());
    }
    private JsonNode sanitize(JsonNode n) {
        if(n.isArray()) {
            ArrayNode result=mapper.createArrayNode();
            n.forEach(v->result.add(sanitize(v)));
            return result;
        }
        if(n.isObject()) {
            ObjectNode result=mapper.createObjectNode();
            n.fields().forEachRemaining(e-> {
                if(!e.getKey().toLowerCase(Locale.ROOT).matches(".*(password|token|storagekey|objectkey|signedurl|apikey).*"))result.set(e.getKey(),sanitize(e.getValue()));
            }
            );
            return result;
        }
        return n;
    }
    public Detail detail(String id) {
        String user=CurrentUser.idOrThrow();
        var row=repository.owned(user,id);
        JsonNode payload=row.payload();
        List<LegacyError> errors=new ArrayList<>();
        for(JsonNode e:payload.path("errors"))errors.add(new LegacyError(e.path("question").asText(""),e.path("reason").asText(""),e.path("deduction").isNumber()?e.path("deduction").decimalValue():null,e.path("solution").asText("")));
        return new Detail(summary(row),row.oldId(),payload.path("status").asText(null),payload.path("score").isNumber()?payload.path("score").decimalValue():null,payload.path("summary").asText(null),errors,repository.files(user,id),repository.recordReview(user,id).orElse(null),sanitize(payload).toPrettyString());
    }
    private void requireFile(String id,String file) {
        String user=CurrentUser.idOrThrow();
        repository.owned(user,id);
        require(repository.ownsFile(user,id,file),ErrorCode.NOT_FOUND,"附件不属于此历史记录");
    }
    public Download file(String id,String file) {
        requireFile(id,file);
        return files.download(file);
    }
    @Transactional public Deletion deleteFile(String id,String file) {
        learning.lockUser(CurrentUser.idOrThrow());
        requireFile(id,file);
        require(files.row(file).metadata().state().equals("ACTIVE"),ErrorCode.NOT_FOUND,"文件不可用");
        files.delete(file);
        audit.add("LEGACY_ATTACHMENT_DELETED","STORED_FILE",file,null,Map.of("legacyRecordId",id));
        return new Deletion(file,"DELETE_PENDING",false,Instant.now());
    }
    public Review review(String id) {
        CurrentUser.requireAdmin();
        return repository.review(CurrentUser.idOrThrow(),id,false);
    }
    public Page<Review> reviews(String decision,String course,int page,int size) {
        CurrentUser.requireAdmin();
        require(decision==null||Set.of("PENDING","ACCEPT","REJECT").contains(decision),ErrorCode.INVALID_PARAMETER,"审核状态无效");
        return repository.reviews(CurrentUser.idOrThrow(),decision,course,page,size);
    }
    @Transactional public Review decide(String id,LegacyReviewDecision w) {
        CurrentUser.requireAdmin();
        String user=CurrentUser.idOrThrow();
        learning.lockUser(user);
        var old=repository.review(user,id,true);
        require(old.decision().equals("PENDING"),ErrorCode.CONFLICT,"审核已有终态");
        require(!w.reason().isBlank(),ErrorCode.INVALID_PARAMETER,"审核原因不能为空");
        String course=w.courseId().toString(),release=id(w.mappingReleaseId()),chapter=id(w.chapterId());
        boolean accept=w.decision().equals("ACCEPT");
        learning.course(course);
        require(old.courseId().equals(course),ErrorCode.INVALID_RELATION,"课程映射不一致");
        require(old.kind().equals("CHAPTER")== (chapter!=null),ErrorCode.INVALID_RELATION,"章节映射与通过类型不一致");
        if(release!=null)require(repository.releaseMapping(course,release),ErrorCode.INVALID_RELATION,"发布映射无效");
        if(chapter!=null)require(release!=null&&repository.chapterMapping(course,release,chapter),ErrorCode.INVALID_RELATION,"章节映射无效");
        if(accept) {
            require(release!=null,ErrorCode.INVALID_RELATION,"认领通过须有完整映射版本");
            require(old.oldScore().compareTo(old.oldThreshold())>=0,ErrorCode.INVALID_RELATION,"旧分数没有达到旧通过线");
        }
        repository.decide(user,id,w);
        if(accept)repository.grant(user,old,w);
        audit.add("LEGACY_PASS_REVIEWED","LEGACY_PASS_REVIEW",id,w.reason(),Map.of("decision",w.decision(),"courseId",course));
        return repository.review(user,id,false);
    }
    public Page<LegacySummary> credits(String course,Boolean approved,int page,int size) {
        CurrentUser.requireAdmin();
        return practice.legacy(CurrentUser.idOrThrow(),course,approved,null,page,size);
    }
    @Transactional public CreditResult credit(String record,LegacyPracticeCreditDecision w) {
        CurrentUser.requireAdmin();
        String user=CurrentUser.idOrThrow();
        learning.lockUser(user);
        var old=repository.credit(user,record);
        require(Boolean.TRUE.equals(w.confirm())&&!w.reason().isBlank(),ErrorCode.INVALID_PARAMETER,"认领原因不能为空且须确认");
        String question=id(w.canonicalQuestionId());
        boolean approved=w.approved();
        require(!approved||question!=null,ErrorCode.INVALID_RELATION,"认领须有原创题映射");
        Mapping mapping=null;
        if(question!=null)mapping=repository.currentOriginal(question).orElseThrow(()->new BusinessException(ErrorCode.INVALID_RELATION,"映射题目不是当前已审核原创题"));
        else if(old.questionId()!=null)mapping=repository.previousMapping(old.questionId());
        require(mapping!=null&&mapping.courseId()!=null&&mapping.releaseId()!=null,ErrorCode.INVALID_RELATION,"旧汇总缺少课程映射");
        repository.credit(user,record,question,approved);
        audit.add("LEGACY_CREDIT_DECIDED","LEGACY_PRACTICE_SUMMARY",record,w.reason(),cn.xuexizhitu.common.BusinessData.obj("approved",approved,"questionId",question));
        LegacySummary summary=practice.legacy(user,null,null,record,1,1).items().stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
        return new CreditResult(summary,practice.stats(user,mapping.courseId(),mapping.releaseId(),mapping.chapterId()),Instant.now());
    }
    private static String id(UUID value) {
        return value==null?null:value.toString();
    }
}
