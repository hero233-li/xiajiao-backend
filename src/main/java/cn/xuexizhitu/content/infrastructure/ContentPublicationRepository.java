package cn.xuexizhitu.content.infrastructure;
import cn.xuexizhitu.content.domain.ContentDraft;
import cn.xuexizhitu.content.domain.ContentRules;
import cn.xuexizhitu.contract.WorkflowDtos.*;
import cn.xuexizhitu.common.*;
import static cn.xuexizhitu.common.BusinessData.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;
import java.time.*;
@Repository @RequiredArgsConstructor public class ContentPublicationRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private JsonNode parse(Object value) {
        try {
            return value==null?mapper.nullNode():mapper.readTree(value.toString());
        }
        catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
    private String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        }
        catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
    private Map<String,Object> raw(String course,String release,boolean lock) {
        return jdbc.queryForList("SELECT * FROM content_release WHERE id=? AND course_id=?"+(lock?" FOR UPDATE":""),release,course).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    private Map<String,Object> releaseDto(Map<String,Object> r) {
        return obj("id",r.get("id"),"courseId",r.get("course_id"),"versionNo",r.get("version_no"),"state",r.get("state"),"publishedAt",r.get("published_at")==null?null:((LocalDateTime)r.get("published_at")).toInstant(ZoneOffset.UTC),"sourceSha",r.get("source_sha"),"draftRevision",r.get("draft_revision"));
    }
    private ObjectNode defaults(String course) {
        ObjectNode result=mapper.createObjectNode();
        for(String section:List.of("catalog","knowledge","questions","task-templates"))result.set(section,mapper.valueToTree(obj(section.equals("catalog")?"chapters":section.equals("knowledge")?"modules":section.equals("questions")?"questions":"templates",List.of())));
        result.set("assessment-policy",mapper.valueToTree(obj("id",uuid(),"courseId",course,"releaseId",null,"gateRatio",0.6,"gateFloor",20,"gateCap",100,"chapterMinQuestions",20,"chapterMaxQuestions",40,"chapterLimitMinutes",40,"chapterPassScore",90,"mockQuestionCount",40,"mockLimitMinutes",80,"mockPassScore",80,"weights",List.of())));
        return result;
    }
    private Object catalog(String course,String release) {
        Map<String,List<Map<String,Object>>> points=new HashMap<>(),items=new HashMap<>();
        for(var p:jdbc.queryForList("SELECT rp.*,p.stable_key FROM release_point rp JOIN knowledge_point p ON p.id=rp.point_id WHERE rp.release_id=? ORDER BY rp.point_id",release))points.computeIfAbsent(p.get("chapter_id").toString(),k->new ArrayList<>()).add(obj("id",p.get("point_id"),"stableKey",p.get("stable_key"),"title",p.get("title_snapshot")));
        for(var i:jdbc.queryForList("SELECT r.*,i.stable_key FROM item_revision r JOIN study_item i ON i.id=r.item_id WHERE r.release_id=? ORDER BY r.sort_order,r.item_id",release))items.computeIfAbsent(i.get("chapter_id").toString(),k->new ArrayList<>()).add(obj("id",i.get("item_id"),"stableKey",i.get("stable_key"),"title",i.get("title"),"estimatedMinutes",i.get("estimated_minutes"),"resource",parse(i.get("resource_locator")),"sortOrder",i.get("sort_order")));
        var chapters=jdbc.queryForList("SELECT r.*,c.stable_key FROM chapter_revision r JOIN chapter c ON c.id=r.chapter_id WHERE r.release_id=? AND r.course_id=? ORDER BY r.sort_order,r.chapter_id",release,course).stream().map(ch->obj("id",ch.get("chapter_id"),"stableKey",ch.get("stable_key"),"title",ch.get("title"),"sortOrder",ch.get("sort_order"),"participatesInAssessment",ch.get("participates_in_assessment"),"gateOverride",ch.get("gate_override"),"points",points.getOrDefault(ch.get("chapter_id"),List.of()),"items",items.getOrDefault(ch.get("chapter_id"),List.of()))).toList();
        return obj("chapters",chapters);
    }
    private Object knowledge(String course,String release) {
        Map<String,List<Map<String,Object>>> examples=new HashMap<>();
        for(var e:jdbc.queryForList("SELECT e.*,s.answer,s.solution FROM knowledge_example e JOIN example_solution s ON s.example_id=e.id WHERE e.release_id=? ORDER BY e.sort_order,e.id",release))examples.computeIfAbsent(e.get("module_id").toString(),k->new ArrayList<>()).add(obj("id",e.get("id"),"stars",e.get("stars"),"question",e.get("question"),"answer",e.get("answer"),"solution",e.get("solution"),"sortOrder",e.get("sort_order")));
        return obj("modules",jdbc.queryForList("SELECT r.*,m.stable_key FROM module_revision r JOIN knowledge_module m ON m.id=r.module_id WHERE r.release_id=? AND m.course_id=? ORDER BY m.stable_key,m.id",release,course).stream().map(m->obj("id",m.get("module_id"),"stableKey",m.get("stable_key"),"title",m.get("title"),"content",m.get("content"),"difficulty",m.get("difficulty"),"formulas",parse(m.get("formulas")),"resources",parse(m.get("resources")),"examples",examples.getOrDefault(m.get("module_id"),List.of()))).toList());
    }
    private Object questions(String course,String release) {
        Map<String,List<String>> points=new HashMap<>();
        jdbc.query("SELECT p.revision_id,p.point_id FROM question_point p JOIN question_revision r ON r.id=p.revision_id WHERE r.release_id=? ORDER BY p.point_id",r-> {
            points.computeIfAbsent(r.getString("revision_id"),k->new ArrayList<>()).add(r.getString("point_id"));
        }
        ,release);
        return obj("questions",jdbc.queryForList("SELECT q.*,r.stem,r.options,r.difficulty,r.source_locator,r.sort_order,r.id AS revision_id,s.correct_option,s.explanation FROM question q JOIN question_revision r ON r.question_id=q.id JOIN question_solution s ON s.revision_id=r.id WHERE q.course_id=? AND r.release_id=? ORDER BY r.sort_order,q.id",course,release).stream().map(q-> {
            JsonNode options=parse(q.get("options"));return obj("id",q.get("id"),"originalKey",q.get("original_key"),"chapterId",q.get("chapter_id"),"eligibleOriginal",q.get("eligible_original"),"mode",q.get("mode"),"stem",q.get("stem"),"options",options,"difficulty",q.get("difficulty"),"pointIds",points.getOrDefault(q.get("revision_id"),List.of()),"correctOption",q.get("correct_option"),"correctAnswer",options.path(((Number)q.get("correct_option")).intValue()).asText(),"explanation",q.get("explanation"),"sourceLocator",q.get("source_locator"),"sortOrder",q.get("sort_order"));
        }
        ).toList());
    }
    private Object policy(String course,String release) {
        var rows=jdbc.queryForList("SELECT * FROM assessment_policy WHERE course_id=? AND release_id=?",course,release);
        if(rows.isEmpty()) {
            ObjectNode p=(ObjectNode)defaults(course).path("assessment-policy");
            p.put("releaseId",release);
            return p;
        }
        var r=rows.get(0);
        Map<String,Object> p=obj("id",r.get("id"),"courseId",course,"releaseId",release);
        for(String[] pair:POLICY_FIELDS)p.put(pair[0],r.get(pair[1]));
        p.put("weights",jdbc.queryForList("SELECT * FROM mock_chapter_weight WHERE policy_id=? ORDER BY chapter_id",r.get("id")).stream().map(w->obj("chapterId",w.get("chapter_id"),"scoreShare",w.get("score_share"),"sampleFrom",w.get("sample_from").toString(),"sampleTo",w.get("sample_to").toString(),"evidence",parse(w.get("evidence")),"approvedBy",w.get("approved_by"),"approvedAt",((LocalDateTime)w.get("approved_at")).toInstant(ZoneOffset.UTC))).toList());
        return p;
    }
    private void stable(String table,String id,String ownerColumn,String owner,String key) {
        var existing=jdbc.queryForList("SELECT "+ownerColumn+",stable_key FROM "+table+" WHERE id=?",id);
        if(existing.isEmpty()) {
            if(table.equals("knowledge_point"))jdbc.update("INSERT INTO knowledge_point(id,chapter_id,stable_key,title) VALUES(?,?,?,?)",id,owner,key,key);
            else jdbc.update("INSERT INTO "+table+"(id,"+ownerColumn+",stable_key) VALUES(?,?,?)",id,owner,key);
        }
        else require(existing.get(0).get(ownerColumn).equals(owner)&&existing.get(0).get("stable_key").equals(key),ErrorCode.INVALID_RELATION,"稳定实体的归属和标识不能改变");
    }
    private static final String[][] POLICY_FIELDS= {
        {
            "gateRatio","gate_ratio"
        }
        , {
            "gateFloor","gate_floor"
        }
        , {
            "gateCap","gate_cap"
        }
        , {
            "chapterMinQuestions","chapter_min_questions"
        }
        , {
            "chapterMaxQuestions","chapter_max_questions"
        }
        , {
            "chapterLimitMinutes","chapter_limit_minutes"
        }
        , {
            "chapterPassScore","chapter_pass_score"
        }
        , {
            "mockQuestionCount","mock_question_count"
        }
        , {
            "mockLimitMinutes","mock_limit_minutes"
        }
        , {
            "mockPassScore","mock_pass_score"
        }
    }
    ;
    public record Release(ContentRelease metadata,ContentDraft draft) {
    }
    public void lockCourse(String course) {
        require(!jdbc.queryForList("SELECT id FROM course WHERE id=? FOR UPDATE",String.class,course).isEmpty(),ErrorCode.NOT_FOUND,"课程不存在");
    }
    public String code(String course) {
        return jdbc.queryForObject("SELECT code FROM course WHERE id=?",String.class,course);
    }
    public Release release(String course,String id,boolean lock) {
        var row=raw(course,id,lock);
        ContentDraft draft=null;
        if(row.get("state").equals("DRAFT")&&row.get("draft_content")!=null)draft=mapper.convertValue(parse(row.get("draft_content")),ContentDraft.class);
        return new Release(mapper.convertValue(releaseDto(row),ContentRelease.class),draft);
    }
    public cn.xuexizhitu.learning.api.LearningDtos.Page<ContentRelease> releases(String course,int page,int size) {
        require(jdbc.queryForObject("SELECT COUNT(*) FROM course WHERE id=?",Integer.class,course)>0,ErrorCode.NOT_FOUND,"课程不存在");
        return new cn.xuexizhitu.learning.api.LearningDtos.Page<>(jdbc.queryForList("SELECT * FROM content_release WHERE course_id=? ORDER BY version_no DESC,id LIMIT ? OFFSET ?",course,size,(page-1L)*size).stream().map(r->mapper.convertValue(releaseDto(r),ContentRelease.class)).toList(),page,size,jdbc.queryForObject("SELECT COUNT(*) FROM content_release WHERE course_id=?",Long.class,course));
    }
    public ContentDraft snapshot(String course,String release) {
        var row=release(course,release,false);
        if(row.draft()!=null)return row.draft();
        return new ContentDraft(mapper.convertValue(catalog(course,release),CatalogDraft.class),mapper.convertValue(knowledge(course,release),KnowledgeDraft.class),mapper.convertValue(questions(course,release),QuestionBankDraft.class),new TemplateDraft(jdbc.queryForList("SELECT * FROM plan_task_template WHERE release_id=? AND course_id=? ORDER BY sort_order,id",release,course).stream().map(t->new TaskTemplate(UUID.fromString(t.get("id").toString()),t.get("kind").toString(),t.get("title").toString(),((Number)t.get("estimated_minutes")).intValue(),t.get("resource_locator")==null?null:mapper.convertValue(parse(t.get("resource_locator")),Resource.class),((Number)t.get("sort_order")).intValue())).toList()),mapper.convertValue(policy(course,release),AssessmentPolicy.class));
    }
    public ContentDraft empty(String course,String release) {
        ObjectNode d=defaults(course);
        ((ObjectNode)d.path("assessment-policy")).put("releaseId",release);
        return mapper.convertValue(d,ContentDraft.class);
    }
    public void create(String course,String release,String sourceSha,ContentDraft draft) {
        int version=jdbc.queryForObject("SELECT COALESCE(MAX(version_no),0)+1 FROM content_release WHERE course_id=?",Integer.class,course);
        jdbc.update("INSERT INTO content_release(id,course_id,version_no,state,source_sha,draft_content) VALUES(?,?,?,'DRAFT',?,?)",release,course,version,sourceSha,json(draft));
    }
    public void save(String release,ContentDraft draft) {
        jdbc.update("UPDATE content_release SET draft_content=?,draft_revision=draft_revision+1 WHERE id=?",json(draft),release);
    }
    public Map<String,String> fingerprintOwners(List<String> hashes) {
        Map<String,String> result=new HashMap<>();
        for(int from=0;from<hashes.size();from+=200) {
            var part=hashes.subList(from,Math.min(from+200,hashes.size()));
            jdbc.query("SELECT content_fingerprint,id FROM question WHERE content_fingerprint IN ("+String.join(",",Collections.nCopies(part.size(),"?"))+")",r-> {
                result.put(r.getString("content_fingerprint"),r.getString("id"));
            }
            ,part.toArray());
        }
        return result;
    }
    public void persistPublished(String course,String release,ContentDraft snapshot) {
        JsonNode draft=mapper.valueToTree(snapshot);
        for(JsonNode ch:draft.path("catalog").path("chapters")) {
            String cid=str(ch,"id");
            stable("chapter",cid,"course_id",course,str(ch,"stableKey"));
            jdbc.update("INSERT INTO chapter_revision(release_id,chapter_id,course_id,title,sort_order,participates_in_assessment,gate_override) VALUES(?,?,?,?,?,?,?)",release,cid,course,str(ch,"title"),ch.path("sortOrder").asInt(),ch.path("participatesInAssessment").asBoolean(),value(ch,"gateOverride"));
            for(JsonNode p:ch.path("points")) {
                stable("knowledge_point",str(p,"id"),"chapter_id",cid,str(p,"stableKey"));
                jdbc.update("INSERT INTO release_point(release_id,chapter_id,point_id,title_snapshot) VALUES(?,?,?,?)",release,cid,str(p,"id"),str(p,"title"));
            }
            for(JsonNode i:ch.path("items")) {
                stable("study_item",str(i,"id"),"chapter_id",cid,str(i,"stableKey"));
                jdbc.update("INSERT INTO item_revision(release_id,item_id,chapter_id,title,resource_locator,estimated_minutes,sort_order) VALUES(?,?,?,?,?,?,?)",release,str(i,"id"),cid,str(i,"title"),i.path("resource").isNull()?null:json(i.path("resource")),i.path("estimatedMinutes").asInt(),i.path("sortOrder").asInt());
            }
        }
        for(JsonNode m:draft.path("knowledge").path("modules")) {
            String mid=str(m,"id");
            stable("knowledge_module",mid,"course_id",course,str(m,"stableKey"));
            jdbc.update("INSERT INTO module_revision(release_id,module_id,title,content,difficulty,formulas,resources) VALUES(?,?,?,?,?,?,?)",release,mid,str(m,"title"),str(m,"content"),m.path("difficulty").asInt(),json(m.path("formulas")),json(m.path("resources")));
            for(JsonNode e:m.path("examples")) {
                jdbc.update("INSERT INTO knowledge_example(id,release_id,module_id,stars,question,sort_order) VALUES(?,?,?,?,?,?)",str(e,"id"),release,mid,e.path("stars").asInt(),str(e,"question"),e.path("sortOrder").asInt());
                jdbc.update("INSERT INTO example_solution(example_id,answer,solution) VALUES(?,?,?)",str(e,"id"),str(e,"answer"),str(e,"solution"));
            }
        }
        for(JsonNode q:draft.path("questions").path("questions")) {
            String id=str(q,"id"),revision=uuid(),fp=ContentRules.fingerprint(mapper.convertValue(q,AdminQuestion.class));
            var old=jdbc.queryForList("SELECT * FROM question WHERE id=?",id);
            if(old.isEmpty())jdbc.update("INSERT INTO question(id,course_id,chapter_id,original_key,content_fingerprint,mode,eligible_original) VALUES(?,?,?,?,?,?,?)",id,course,str(q,"chapterId"),str(q,"originalKey"),fp,str(q,"mode"),q.path("eligibleOriginal").asBoolean());
            else {
                var existing=old.get(0);
                require(existing.get("course_id").equals(course)&&existing.get("chapter_id").equals(str(q,"chapterId"))&&existing.get("original_key").equals(str(q,"originalKey"))&&existing.get("mode").equals(str(q,"mode")),ErrorCode.INVALID_RELATION,"稳定题目归属或身份不能改变");
                jdbc.update("UPDATE question SET content_fingerprint=?,eligible_original=? WHERE id=?",fp,q.path("eligibleOriginal").asBoolean(),id);
            }
            jdbc.update("INSERT INTO question_revision(id,release_id,question_id,stem,options,difficulty,source_locator,sort_order) VALUES(?,?,?,?,?,?,?,?)",revision,release,id,str(q,"stem"),json(q.path("options")),q.path("difficulty").asInt(),str(q,"sourceLocator"),q.path("sortOrder").asInt());
            jdbc.update("INSERT INTO question_solution(revision_id,correct_option,explanation) VALUES(?,?,?)",revision,q.path("correctOption").asInt(),str(q,"explanation"));
            for(JsonNode p:q.path("pointIds"))jdbc.update("INSERT INTO question_point(revision_id,point_id) VALUES(?,?)",revision,p.asText());
        }
        for(JsonNode t:draft.path("task-templates").path("templates"))jdbc.update("INSERT INTO plan_task_template(id,release_id,course_id,kind,title,estimated_minutes,resource_locator,sort_order) VALUES(?,?,?,?,?,?,?,?)",str(t,"id"),release,course,str(t,"kind"),str(t,"title"),t.path("estimatedMinutes").asInt(),t.path("resource").isNull()?null:json(t.path("resource")),t.path("sortOrder").asInt());
        JsonNode policy=draft.path("assessment-policy");
        List<Object> args=new ArrayList<>(List.of(str(policy,"id"),course,release));
        for(String[] f:POLICY_FIELDS)args.add(value(policy,f[0]));
        jdbc.update("INSERT INTO assessment_policy(id,course_id,release_id,gate_ratio,gate_floor,gate_cap,chapter_min_questions,chapter_max_questions,chapter_limit_minutes,chapter_pass_score,mock_question_count,mock_limit_minutes,mock_pass_score) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",args.toArray());
        for(JsonNode weight:policy.path("weights"))jdbc.update("INSERT INTO mock_chapter_weight(policy_id,chapter_id,score_share,sample_from,sample_to,evidence,approved_by,approved_at) VALUES(?,?,?,?,?,?,?,?)",str(policy,"id"),str(weight,"chapterId"),value(weight,"scoreShare"),str(weight,"sampleFrom"),str(weight,"sampleTo"),json(weight.path("evidence")),str(weight,"approvedBy"),LocalDateTime.ofInstant(Instant.parse(str(weight,"approvedAt")),ZoneOffset.UTC));
    }
    public void activate(String course,String release) {
        jdbc.update("UPDATE content_release SET state='RETIRED' WHERE course_id=? AND state='PUBLISHED'",course);
        jdbc.update("UPDATE content_release SET state='PUBLISHED',published_at=UTC_TIMESTAMP(6),draft_revision=draft_revision+1 WHERE id=?",release);
    }
}
