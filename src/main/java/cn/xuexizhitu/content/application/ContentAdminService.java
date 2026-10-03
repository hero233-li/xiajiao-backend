package cn.xuexizhitu.content.application;
import cn.xuexizhitu.content.domain.*;
import cn.xuexizhitu.content.infrastructure.ContentPublicationRepository;
import cn.xuexizhitu.contract.WorkflowDtos.*;
import cn.xuexizhitu.common.*;
import static cn.xuexizhitu.common.BusinessData.*;
import static cn.xuexizhitu.content.domain.ContentRules.issue;
import cn.xuexizhitu.assessment.infrastructure.AssessmentRepository;
import cn.xuexizhitu.audit.infrastructure.AuditRepository;
import cn.xuexizhitu.files.application.PrivateFileStore;
import cn.xuexizhitu.security.CurrentUser;
import cn.xuexizhitu.validation.BusinessInput;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;
import java.nio.charset.StandardCharsets;
@Service @RequiredArgsConstructor @Transactional(readOnly=true) public class ContentAdminService {
    private final ContentPublicationRepository repository;
    private final ObjectMapper mapper;
    private final BusinessInput input;
    private final AuditRepository audit;
    private final PrivateFileStore files;
    private final AssessmentRepository assessments;
    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        }
        catch(com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
    private JsonNode parse(Object value) {
        try {
            return mapper.readTree(value.toString());
        }
        catch(com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
    public cn.xuexizhitu.learning.api.LearningDtos.Page<ContentRelease> releases(String course,int page,int size) {
        CurrentUser.requireAdmin();
        return repository.releases(course,page,size);
    }
    public ContentDraft snapshot(String course,String release) {
        CurrentUser.requireAdmin();
        return repository.snapshot(course,release);
    }
    @Transactional public ContentRelease create(String course,ReleaseCreate w) {
        CurrentUser.requireAdmin();
        input.validate("ReleaseCreate",mapper.valueToTree(w));
        repository.lockCourse(course);
        String id=uuid();
        ContentDraft draft=w.basedOnReleaseId()==null?repository.empty(course,id):repository.snapshot(course,w.basedOnReleaseId().toString());
        ObjectNode content=mapper.valueToTree(draft);
        if(w.basedOnReleaseId()!=null) {
            Map<String,String> examples=new HashMap<>();
            for(JsonNode m:content.path("knowledge").path("modules"))for(JsonNode e:m.path("examples")) {
                String next=uuid();
                examples.put(e.path("id").asText(),next);
                ((ObjectNode)e).put("id",next);
            }
            cloneManuals(content,course,id,examples);
            for(JsonNode t:content.path("task-templates").path("templates"))((ObjectNode)t).put("id",uuid());
        }
        ObjectNode policy=(ObjectNode)content.path("assessment-policy");
        policy.put("id",uuid());
        policy.put("courseId",course);
        policy.put("releaseId",id);
        repository.create(course,id,w.sourceSha(),mapper.convertValue(content,ContentDraft.class));
        audit.add("RELEASE_CREATED","CONTENT_RELEASE",id,null,obj("courseId",course,"basedOnReleaseId",w.basedOnReleaseId()));
        return repository.release(course,id,false).metadata();
    }
    private void cloneManuals(ObjectNode content,String course,String release,Map<String,String> examples) {
        Set<String> ids=new HashSet<>();
        content.findValues("fileId").stream().filter(JsonNode::isTextual).forEach(n->ids.add(n.asText()));
        Map<String,String> replacements=new HashMap<>();
        for(String id:ids) {
            var f=files.row(id);
            if(!f.metadata().purpose().equals("MANUAL"))continue;
            var downloaded=files.download(id);
            JsonNode manifest=parse(new String(Base64.getDecoder().decode(downloaded.contentBase64()),StandardCharsets.UTF_8));
            if(manifest.isObject()&&manifest.has("sections")) {
                ((ObjectNode)manifest).put("courseId",course);
                ((ObjectNode)manifest).put("releaseId",release);
                replaceReferences(manifest,examples);
                replacements.put(id,files.uploadJson(json(manifest).getBytes(StandardCharsets.UTF_8),f.metadata().name()));
            }
        }
        replaceReferences(content,replacements);
    }
    private void replaceReferences(JsonNode node,Map<String,String> replacements) {
        if(node.isObject()) {
            var names=new ArrayList<String>();
            node.fieldNames().forEachRemaining(names::add);
            for(String field:names) {
                JsonNode value=node.path(field);
                if(value.isTextual()&&replacements.containsKey(value.asText()))((ObjectNode)node).put(field,replacements.get(value.asText()));
                else replaceReferences(value,replacements);
            }
        }
        else if(node.isArray())for(JsonNode child:node)replaceReferences(child,replacements);
    }
    private ContentDraft editable(String course,String release,Long expected) {
        var row=repository.release(course,release,true);
        require(row.metadata().state().equals("DRAFT"),ErrorCode.CONFLICT,"已发布版本不能原地编辑");
        if(expected!=null)require(row.metadata().draftRevision().equals(expected),ErrorCode.REVISION_CONFLICT,"内容草稿版本已变化，请重新读取");
        return row.draft();
    }
    private <T> T save(String release,String section,ContentDraft draft,T value) {
        repository.save(release,draft);
        audit.add("DRAFT_UPDATED","CONTENT_RELEASE",release,null,Map.of("courseId",draft.policy().courseId().toString(),"section",section));
        return value;
    }
    @Transactional public CatalogDraft catalog(String course,String release,CatalogDraft w,Long expected) {
        CurrentUser.requireAdmin();
        input.validate("CatalogDraft",mapper.valueToTree(w));
        return save(release,"catalog",editable(course,release,expected).withCatalog(w),w);
    }
    @Transactional public KnowledgeDraft knowledge(String course,String release,KnowledgeDraft w,Long expected) {
        CurrentUser.requireAdmin();
        input.validate("KnowledgeDraft",mapper.valueToTree(w));
        return save(release,"knowledge",editable(course,release,expected).withKnowledge(w),w);
    }
    @Transactional public QuestionBankDraft questions(String course,String release,QuestionBankDraft w,Long expected) {
        CurrentUser.requireAdmin();
        input.validate("QuestionBankDraft",mapper.valueToTree(w));
        return save(release,"questions",editable(course,release,expected).withQuestions(w),w);
    }
    @Transactional public TemplateDraft templates(String course,String release,TemplateDraft w,Long expected) {
        CurrentUser.requireAdmin();
        input.validate("TemplateDraft",mapper.valueToTree(w));
        return save(release,"task-templates",editable(course,release,expected).withTemplates(w),w);
    }
    @Transactional public AssessmentPolicy policy(String course,String release,AssessmentPolicyWrite w,Long expected) {
        CurrentUser.requireAdmin();
        input.validate("AssessmentPolicyWrite",mapper.valueToTree(w));
        var draft=editable(course,release,expected);
        var weights=w.weights().stream().map(v->new MockWeightRead(v.chapterId(),v.scoreShare(),v.sampleFrom(),v.sampleTo(),new MockWeightReadEvidence(v.evidence().description(),v.evidence().sourcePaperKeys()),UUID.fromString(CurrentUser.idOrThrow()),Instant.now())).toList();
        var policy=new AssessmentPolicy(w.gateRatio(),w.gateFloor(),w.gateCap(),w.chapterMinQuestions(),w.chapterMaxQuestions(),w.chapterLimitMinutes(),w.chapterPassScore(),w.mockQuestionCount(),w.mockLimitMinutes(),w.mockPassScore(),weights,draft.policy().id(),UUID.fromString(course),UUID.fromString(release));
        return save(release,"assessment-policy",draft.withPolicy(policy),policy);
    }
    public ReleaseValidation validation(String course,String release) {
        CurrentUser.requireAdmin();
        return validate(course,release,repository.snapshot(course,release));
    }
    private ReleaseValidation validate(String course,String release,ContentDraft draft) {
        var owners=repository.fingerprintOwners(draft.questions().questions().stream().map(ContentRules::fingerprint).distinct().toList());
        var result=ContentRules.validate(draft,owners);
        var issues=new ArrayList<>(result.issues());
        for(Resource r:ContentRules.resources(draft)) {
            if(r.kind().equals("FILE")) {
                try {
                    var file=files.metadata(r.fileId().toString());
                    if(!file.purpose().equals("MANUAL")||!file.state().equals("ACTIVE"))throw new IllegalArgumentException();
                }
                catch(RuntimeException e) {
                    issue(issues,"INVALID_FILE","resource","手册文件无效");
                }
            }
            else if(r.url()==null||!(r.url().startsWith("https://")||r.url().startsWith("http://")))issue(issues,"INVALID_RESOURCE","resource","资源链接必须为HTTP或HTTPS");
        }
        validateWeights(course,draft,issues);
        validateManuals(course,release,draft,issues);
        return new ReleaseValidation(issues.isEmpty(),issues,result.contentCounts());
    }
    private void validateWeights(String course,ContentDraft draft,List<ReleaseValidationIssuesItem> issues) {
        var weights=draft.policy().weights();
        if(weights.isEmpty())return;
        var expected=draft.catalog().chapters().stream().filter(ChapterDraft::participatesInAssessment).map(ChapterDraft::id).collect(java.util.stream.Collectors.toSet());
        Set<UUID> actual=new HashSet<>();
        java.math.BigDecimal total=java.math.BigDecimal.ZERO;
        String code=repository.code(course);
        for(MockWeightRead w:weights) {
            if(!actual.add(w.chapterId()))issue(issues,"INVALID_POLICY","assessment-policy","章节权重重复");
            total=total.add(w.scoreShare());
            try {
                assessments.validateWeight(json(w.evidence()),code,w.sampleFrom(),w.sampleTo(),w.approvedAt());
            }
            catch(RuntimeException e) {
                issue(issues,"INVALID_POLICY","assessment-policy","模拟权重统计窗口、依据或审核时间无效");
            }
        }
        if(!actual.equals(expected)||total.compareTo(java.math.BigDecimal.ONE)!=0)issue(issues,"INVALID_POLICY","assessment-policy","模拟权重须覆盖参与章节且合计为1");
    }
    private void validateManuals(String course,String release,ContentDraft draft,List<ReleaseValidationIssuesItem> issues) {
        Map<String,Set<String>> chapters=new HashMap<>();
        for(ChapterDraft c:draft.catalog().chapters())chapters.put(c.id().toString(),c.items().stream().map(i->i.id().toString()).collect(java.util.stream.Collectors.toSet()));
        Set<String> examples=new HashSet<>();
        draft.knowledge().modules().forEach(m->m.examples().forEach(e->examples.add(e.id().toString())));
        var fileIds=ContentRules.resources(draft).stream().filter(r->r.fileId()!=null).map(Resource::fileId).distinct().toList();
        for(UUID id:fileIds)try {
            var f=files.row(id.toString());
            if(!f.metadata().purpose().equals("MANUAL"))continue;
            JsonNode manifest=parse(new String(Base64.getDecoder().decode(files.download(id.toString()).contentBase64()),StandardCharsets.UTF_8));
            if(!course.equals(str(manifest,"courseId"))||!release.equals(str(manifest,"releaseId"))||!manifest.path("sections").isArray()||manifest.path("sections").isEmpty())throw new IllegalArgumentException();
            Set<String> seen=new HashSet<>();
            for(JsonNode section:manifest.path("sections")) {
                String chapter=str(section,"chapterId");
                if(!seen.add(chapter)||!chapters.containsKey(chapter)||!section.path("title").isTextual()||!section.path("markdown").isTextual()||!section.path("exercises").isArray())throw new IllegalArgumentException();
                Set<String> items=new HashSet<>();
                for(JsonNode e:section.path("exercises"))if(!chapters.get(chapter).contains(str(e,"itemId"))||!items.add(str(e,"itemId"))||(str(e,"exampleId")!=null&&!examples.contains(str(e,"exampleId"))))throw new IllegalArgumentException();
            }
        }
        catch(RuntimeException e) {
            issue(issues,"INVALID_FILE","resource","手册文件损坏或发布、章节、练习映射无效");
        }
    }
    @Transactional public ContentRelease publish(String course,String release,cn.xuexizhitu.exams.domain.ExamTypes.Confirm w,Long expected) {
        CurrentUser.requireAdmin();
        require(Boolean.TRUE.equals(w.confirm()),ErrorCode.INVALID_PARAMETER,"须确认发布");
        repository.lockCourse(course);
        ContentDraft draft=editable(course,release,expected);
        var result=validate(course,release,draft);
        require(result.valid(),ErrorCode.INVALID_RELATION,"发布校验未通过，请读取validation明细");
        repository.persistPublished(course,release,draft);
        if(!draft.policy().weights().isEmpty())assessments.weights(assessments.policy(course,release),assessments.participating(release));
        repository.activate(course,release);
        audit.add("RELEASE_PUBLISHED","CONTENT_RELEASE",release,null,Map.of("courseId",course,"counts",result.contentCounts()));
        return repository.release(course,release,false).metadata();
    }
}
