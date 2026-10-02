package cn.xuexizhitu;

import cn.xuexizhitu.dto.*;
import cn.xuexizhitu.entity.Role;
import cn.xuexizhitu.security.CurrentUser;
import cn.xuexizhitu.service.AccountService;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.assessments.timeout-enabled=false","app.files.cleanup-enabled=false"}) @AutoConfigureMockMvc @Testcontainers
class PracticeMySqlIT {
    @Container static MySQLContainer<?> mysql=new MySQLContainer<>("mysql:8.4").withDatabaseName("practice_test");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",mysql::getJdbcUrl);r.add("spring.datasource.username",mysql::getUsername);r.add("spring.datasource.password",mysql::getPassword);
        r.add("app.bootstrap.enabled",()->false);r.add("app.auth.registration-mode",()->"DISABLED");
        r.add("app.jwt.secret-base64",()->"MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
    }
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired JdbcTemplate jdbc; @Autowired AccountService accounts;
    private UserDto user;
    private static final List<JsonNode> samples=new CopyOnWriteArrayList<>();
    @AfterAll static void saveContractSamples() throws Exception {
        new ObjectMapper().writeValue(new java.io.File("target/practice-contract-samples.json"),samples);
    }
    private static final String COURSE="afdac469-0fe4-5007-833c-51a71333967b", OTHER="ccd819bb-a48b-5c75-bfbf-e26219023fef", CYCLE="0ebdbbfe-d607-54b5-9c21-4e0adade5e4c";
    @BeforeEach void account() {user=newUser();}
    private UserDto newUser() {String name="learning"+UUID.randomUUID();return accounts.create(new RegisterRequest(name,name+"@example.com","12345678"),Role.USER);}
    private MockHttpServletRequestBuilder as(UserDto u,MockHttpServletRequestBuilder request) {
        return request.with(authentication(new UsernamePasswordAuthenticationToken(new CurrentUser(u.id(),u.username(),Role.USER,0),null,List.of())));
    }
    private JsonNode read(MockHttpServletRequestBuilder request) throws Exception {
        var result=mvc.perform(as(user,request)).andReturn();
        assertThat(result.getResponse().getStatus()).isIn(200,201);
        assertThat(result.getResponse().getHeader("Cache-Control")).isEqualTo("no-store");
        JsonNode envelope=mapper.readTree(result.getResponse().getContentAsString());
        assertThat(envelope.path("code").asInt()).isZero();
        var sample=mapper.createObjectNode();sample.put("path",result.getRequest().getRequestURI().substring("/api/v1".length()));
        sample.put("method",result.getRequest().getMethod().toLowerCase());sample.put("status",result.getResponse().getStatus());sample.set("body",envelope);samples.add(sample);
        return envelope.path("data");
    }
    private JsonNode catalog() throws Exception {return read(get("/api/v1/catalog/courses/"+COURSE));}
    private List<String> items() throws Exception {
        List<String> ids=new ArrayList<>();catalog().path("chapters").forEach(c->c.path("items").forEach(i->ids.add(i.path("id").asText())));return ids;
    }
    private String completionUrl(String item) {return "/api/v1/catalog/courses/"+COURSE+"/items/"+item+"/completion";}
    private String command(boolean done,long revision) {return "{\"completed\":"+done+",\"expectedRevision\":"+revision+",\"clientMutationId\":\""+UUID.randomUUID()+"\"}";}
    private String base(){return "/api/v1/practice/courses/"+COURSE;}
    private JsonNode first()throws Exception{return read(get(base()+"/questions").param("mode","CHAPTER").param("size","1")).path("items").get(0);}
    private String submissionUrl(JsonNode q){return base()+"/questions/"+q.path("id").asText()+"/submissions";}
    private String answer(JsonNode q,int option){return "{\"revisionId\":\""+q.path("revisionId").asText()+"\",\"selectedOption\":"+option+"}";}
    private int correct(JsonNode q){return jdbc.queryForObject("SELECT correct_option FROM question_solution WHERE revision_id=?",Integer.class,q.path("revisionId").asText());}
    private JsonNode submit(JsonNode q,int option,String key)throws Exception{return read(post(submissionUrl(q)).header("Idempotency-Key",key).contentType("application/json").content(answer(q,option)));}
    @Test void realBankHasStableChaptersPointsAndPublicQuestionsNeverContainSolutions()throws Exception {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM question",Integer.class)).isEqualTo(3671);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM question WHERE eligible_original=true",Integer.class)).isZero();
        JsonNode page=read(get(base()+"/questions").param("size","2"));assertThat(page.path("total").asInt()).isEqualTo(1202);assertThat(page.path("items").size()).isEqualTo(2);
        JsonNode q=page.path("items").get(0);JsonNode detail=read(get(base()+"/questions/"+q.path("id").asText()));
        for(String forbidden:List.of("answer","correctOption","explanation","solution","answerGuide"))assertThat(detail.has(forbidden)).isFalse();
        assertThat(detail.path("pointIds").size()).isPositive();assertThat(detail.path("sourceLabel").asText()).doesNotContain("http","/resources/");
        assertThat(read(get(base()+"/questions").param("q","%_")).path("total").asInt()).isZero();
        assertThat(read(get(base()+"/questions").param("chapterId",q.path("chapterId").asText()).param("difficulty",q.path("difficulty").asText())).path("total").asInt()).isPositive();
        read(get(base()+"/questions").param("mode","VARIANT"));
        JsonNode overview=read(get(base()+"/overview"));assertThat(overview.path("variantQuestionCount").asInt()).isPositive();
        assertThat(read(get(base()+"/stats")).path("gateThreshold").isNull()).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM practice_submission WHERE user_id=?",Integer.class,user.id())).isZero();
        mvc.perform(get(base()+"/questions")).andExpect(status().isUnauthorized());
        mvc.perform(as(user,get(base()+"/questions").param("filter","INVALID"))).andExpect(status().isBadRequest());
    }
    @Test void serverGradesAndLatestWrongFilterChangesAfterCorrectRedo()throws Exception {
        JsonNode q=first();int correct=correct(q),wrong=(correct+1)%q.path("options").size();String question=q.path("id").asText();
        JsonNode fail=submit(q,wrong,UUID.randomUUID().toString());assertThat(fail.path("correct").asBoolean()).isFalse();assertThat(fail.path("correctOption").asInt()).isEqualTo(correct);
        assertThat(read(get(base()+"/questions").param("filter","WRONG")).path("total").asInt()).isEqualTo(1);
        JsonNode success=submit(q,correct,UUID.randomUUID().toString());assertThat(success.path("correct").asBoolean()).isTrue();
        assertThat(read(get(base()+"/questions").param("filter","WRONG")).path("total").asInt()).isZero();
        JsonNode stats=read(get(base()+"/stats").param("chapterId",q.path("chapterId").asText()));assertThat(stats.path("practiceAttemptCount").asInt()).isEqualTo(2);assertThat(stats.path("practiceAccuracy").asDouble()).isEqualTo(50);
        assertThat(stats.path("canApplyChapterAssessment").asBoolean()).isFalse();
        JsonNode history=read(get(base()+"/submissions").param("questionId",question).param("size","1"));assertThat(history.path("total").asInt()).isEqualTo(2);assertThat(history.path("items").get(0).path("id").asText()).isEqualTo(success.path("submissionId").asText());
        assertThat(read(get(base()+"/submissions/"+fail.path("submissionId").asText()))).isEqualTo(fail);
        JsonNode detail=read(get(base()+"/questions/"+question));assertThat(detail.path("latestOutcome").asText()).isEqualTo("CORRECT");assertThat(detail.has("correctOption")).isFalse();
        assertThat(read(get(base()+"/questions").param("filter","UNANSWERED")).path("total").asInt()).isEqualTo(1201);
    }
    @Test void exactReplaySurvivesLaterAttemptsAndConflictingKeyIsRejected()throws Exception {
        JsonNode q=first();String key=UUID.randomUUID().toString();JsonNode original=submit(q,0,key);submit(q,1,UUID.randomUUID().toString());
        assertThat(submit(q,0,key)).isEqualTo(original);
        mvc.perform(as(user,post(submissionUrl(q)).header("Idempotency-Key",key).contentType("application/json").content(answer(q,1)))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(40904));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM practice_submission WHERE user_id=?",Integer.class,user.id())).isEqualTo(2);
        UserDto other=newUser();mvc.perform(as(other,post(submissionUrl(q)).header("Idempotency-Key",key).contentType("application/json").content(answer(q,0)))).andExpect(status().isCreated());
    }
    @Test void concurrentSameKeyCreatesOneImmutableSubmission()throws Exception {
        JsonNode q=first();String key=UUID.randomUUID().toString();ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try {
            Callable<String> task=()->{start.await();return mvc.perform(as(user,post(submissionUrl(q)).header("Idempotency-Key",key).contentType("application/json").content(answer(q,0)))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();};
            Future<String> a=pool.submit(task),b=pool.submit(task);start.countDown();assertThat(mapper.readTree(a.get(30,TimeUnit.SECONDS))).isEqualTo(mapper.readTree(b.get(30,TimeUnit.SECONDS)));
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM practice_submission WHERE user_id=? AND idempotency_key=?",Integer.class,user.id(),key)).isEqualTo(1);
        }finally{pool.shutdownNow();}
    }
    @Test void marksPersistUseOptimisticVersionsAndNeverAffectOtherUsers()throws Exception {
        JsonNode q=first();String question=q.path("id").asText(),url=base()+"/questions/"+question+"/mark",body="{\"bookmarked\":true,\"uncertain\":true,\"expectedRevision\":0}";
        JsonNode mark=read(put(url).contentType("application/json").content(body));assertThat(mark.path("revision").asInt()).isEqualTo(1);
        assertThat(read(get(base()+"/questions").param("filter","BOOKMARKED")).path("total").asInt()).isEqualTo(1);assertThat(read(get(base()+"/questions").param("filter","UNCERTAIN")).path("total").asInt()).isEqualTo(1);
        mvc.perform(as(user,put(url).contentType("application/json").content(body))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(40901));
        mvc.perform(as(newUser(),get(base()+"/questions/"+question))).andExpect(jsonPath("$.data.mark.bookmarked").value(false));
        read(put(url).contentType("application/json").content(body.replace("true","false").replace(":0",":1")));
        assertThat(read(get(base()+"/questions").param("filter","BOOKMARKED")).path("total").asInt()).isZero();
    }
    @Test void rejectsInvalidChoicesInjectedResultsAndForeignHistory()throws Exception {
        JsonNode q=first();String key=UUID.randomUUID().toString();
        mvc.perform(as(user,post(submissionUrl(q)).contentType("application/json").content(answer(q,0)))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(40001));
        mvc.perform(as(user,post(submissionUrl(q)).header("Idempotency-Key",key).contentType("application/json").content(answer(q,-1)))).andExpect(status().isBadRequest());
        mvc.perform(as(user,post(submissionUrl(q)).header("Idempotency-Key",key).contentType("application/json").content(answer(q,99)))).andExpect(status().isUnprocessableEntity());
        mvc.perform(as(user,post(submissionUrl(q)).header("Idempotency-Key",key).contentType("application/json").content(answer(q,0).replace("\"selectedOption\":","\"correct\":true,\"selectedOption\":")))).andExpect(status().isBadRequest());
        JsonNode saved=submit(q,0,key);String url=base()+"/submissions/"+saved.path("submissionId").asText();
        mvc.perform(as(newUser(),get(url))).andExpect(status().isNotFound());mvc.perform(as(user,get(url.replace(COURSE,OTHER)))).andExpect(status().isNotFound());
        mvc.perform(as(user,get(base()+"/questions").param("chapterId",UUID.randomUUID().toString()))).andExpect(status().isNotFound());
    }
    @Test void publishingNewRevisionPreservesHistoricalAnswersAndReplay()throws Exception {
        JsonNode q=first();String key=UUID.randomUUID().toString();JsonNode original=submit(q,0,key);String oldRelease=q.path("releaseId").asText(),release=UUID.randomUUID().toString(),revision=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO content_release(id,course_id,version_no,state,published_at) VALUES(?,?,4,'PUBLISHED',UTC_TIMESTAMP(6))",release,COURSE);
        jdbc.update("INSERT INTO chapter_revision SELECT ?,chapter_id,course_id,title,sort_order,participates_in_assessment,gate_override FROM chapter_revision WHERE release_id=?",release,oldRelease);
        jdbc.update("INSERT INTO question_revision(id,release_id,question_id,stem,options,difficulty,sort_order) VALUES(?,?,?,'版本更新题','[\"新A\",\"新B\"]',1,0)",revision,release,q.path("id").asText());
        jdbc.update("INSERT INTO question_solution(revision_id,correct_option,explanation) VALUES(?,1,'新版本解答')",revision);
        jdbc.update("UPDATE content_release SET state='RETIRED' WHERE id=?",oldRelease);
        try {
            assertThat(read(get(base()+"/questions/"+q.path("id").asText())).path("revisionId").asText()).isEqualTo(revision);
            mvc.perform(as(user,post(submissionUrl(q)).header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content(answer(q,0)))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(40902));
            assertThat(read(get(base()+"/submissions/"+original.path("submissionId").asText()))).isEqualTo(original);assertThat(submit(q,0,key)).isEqualTo(original);
        }finally{jdbc.update("UPDATE content_release SET state='RETIRED' WHERE id=?",release);jdbc.update("UPDATE content_release SET state='PUBLISHED' WHERE id=?",oldRelease);}
    }
    @Test void originalCreditsDeduplicatePracticeTerminalAssessmentsAndReviewedLegacy()throws Exception {
        JsonNode list=read(get(base()+"/questions").param("mode","CHAPTER").param("size","3")).path("items");List<String> ids=new ArrayList<>();list.forEach(q->ids.add(q.path("id").asText()));
        for(String id:ids)jdbc.update("UPDATE question SET eligible_original=true WHERE id=?",id);
        try {
            JsonNode first=list.get(0);submit(first,0,UUID.randomUUID().toString());submit(first,1,UUID.randomUUID().toString());
            String release=first.path("releaseId").asText(),chapter=first.path("chapterId").asText(),policy=jdbc.queryForObject("SELECT id FROM assessment_policy WHERE release_id=?",String.class,release),session=UUID.randomUUID().toString();
            jdbc.update("INSERT INTO assessment_session(id,user_id,course_id,chapter_id,release_id,policy_id,kind,status,started_at,deadline_at,question_count,pass_score,policy_snapshot,idempotency_key) VALUES(?,?,?,?,?,?,'CHAPTER','IN_PROGRESS',UTC_TIMESTAMP(6),DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 40 MINUTE),1,90,'{}',?)",session,user.id(),COURSE,chapter,release,policy,UUID.randomUUID().toString());
            jdbc.update("INSERT INTO assessment_session_question(session_id,revision_id,position,selected_option) VALUES(?,?,1,0)",session,list.get(1).path("revisionId").asText());
            legacy(ids.get(1),false);
            assertThat(read(get(base()+"/stats")).path("answeredOriginalCount").asInt()).isEqualTo(1);
            jdbc.update("UPDATE assessment_session SET status='SUBMITTED',submitted_at=UTC_TIMESTAMP(6),correct_count=0,passed=false WHERE id=?",session);
            legacy(ids.get(2),true);legacy(ids.get(0),true);
            assertThat(read(get(base()+"/stats")).path("answeredOriginalCount").asInt()).isEqualTo(3);
            jdbc.update("UPDATE assessment_policy SET gate_floor=2,gate_ratio=1 WHERE id=?",policy);
            assertThat(read(get(base()+"/stats").param("chapterId",chapter)).path("gateThreshold").asInt()).isEqualTo(3);
            JsonNode old=read(get("/api/v1/practice/history").param("courseId",COURSE));assertThat(old.path("total").asInt()).isEqualTo(3);
            assertThat(read(get(base()+"/submissions")).path("total").asInt()).isEqualTo(2);
        }finally{for(String id:ids)jdbc.update("UPDATE question SET eligible_original=false WHERE id=?",id);jdbc.update("UPDATE assessment_policy SET gate_floor=20,gate_ratio=0.6 WHERE course_id=?",COURSE);}
    }
    private void legacy(String question,boolean approved){
        String batch=UUID.randomUUID().toString(),record=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO legacy_import_batch(id,source_sha,target_user_id,manifest_sha256,state,created_at) VALUES(?,?,?,?,'IMPORTED',UTC_TIMESTAMP(6))",batch,"0".repeat(64),user.id(),"0".repeat(64));
        jdbc.update("INSERT INTO legacy_record(id,batch_id,user_id,old_owner,old_id,old_kind,payload,source_hash) VALUES(?,?,?,?,?,'practice','{}',?)",record,batch,user.id(),"old-owner",record,"0".repeat(64));
        jdbc.update("INSERT INTO legacy_practice_summary(legacy_record_id,user_id,question_id,old_question_id,attempts,gate_credit_approved) VALUES(?,?,?,?,5,?)",record,user.id(),question,"old-"+question,approved);
    }
}
