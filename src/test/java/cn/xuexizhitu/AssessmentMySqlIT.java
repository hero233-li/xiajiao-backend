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
class AssessmentMySqlIT {
    @Container static MySQLContainer<?> mysql=new MySQLContainer<>("mysql:8.4").withDatabaseName("assessment_test");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",mysql::getJdbcUrl);r.add("spring.datasource.username",mysql::getUsername);r.add("spring.datasource.password",mysql::getPassword);
        r.add("app.assessments.timeout-enabled",()->false);r.add("app.bootstrap.enabled",()->false);r.add("app.auth.registration-mode",()->"DISABLED");
        r.add("app.jwt.secret-base64",()->"MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
    }
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired JdbcTemplate jdbc; @Autowired AccountService accounts;
    private UserDto user;
    private static final List<JsonNode> samples=new CopyOnWriteArrayList<>();
    @AfterAll static void saveContractSamples() throws Exception {
        new ObjectMapper().writeValue(new java.io.File("target/assessment-contract-samples.json"),samples);
    }
    private static final String COURSE="afdac469-0fe4-5007-833c-51a71333967b", OTHER="ccd819bb-a48b-5c75-bfbf-e26219023fef", CYCLE="0ebdbbfe-d607-54b5-9c21-4e0adade5e4c";
    @Autowired cn.xuexizhitu.service.AssessmentService service;
    @Autowired cn.xuexizhitu.repository.AssessmentRepository repository;
    private String course,release,chapter,policy;
    private final List<String> revisions=new ArrayList<>();
    private final List<String> questionIds=new ArrayList<>();
    @BeforeEach void account() {
        user=newUser();course=uuid();release=uuid();chapter=uuid();policy=uuid();revisions.clear();questionIds.clear();
        jdbc.update("INSERT INTO course(id,code,name,course_type) VALUES(?,?,?,'THEORY')",course,"T"+course.substring(0,10),"测试课程");
        jdbc.update("INSERT INTO cycle_course(cycle_id,course_id,exam_date) VALUES(?,?,?)",CYCLE,course,java.sql.Date.valueOf(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).plusDays(7)));
        jdbc.update("INSERT INTO content_release(id,course_id,version_no,state,published_at) VALUES(?,?,1,'PUBLISHED',UTC_TIMESTAMP(6))",release,course);
        jdbc.update("INSERT INTO chapter(id,course_id,stable_key) VALUES(?,?,'test')",chapter,course);
        jdbc.update("INSERT INTO chapter_revision(release_id,chapter_id,course_id,title,sort_order,participates_in_assessment) VALUES(?,?,?,'测试章',0,true)",release,chapter,course);
        jdbc.update("INSERT INTO assessment_policy(id,course_id,release_id) VALUES(?,?,?)",policy,course,release);
        String point=uuid();jdbc.update("INSERT INTO knowledge_point(id,chapter_id,stable_key,title) VALUES(?,?,'p','考点')",point,chapter);
        jdbc.update("INSERT INTO release_point(release_id,chapter_id,point_id,title_snapshot) VALUES(?,?,?,'考点')",release,chapter,point);
        for(int i=0;i<25;i++) {
            String q=uuid(),r=uuid();questionIds.add(q);revisions.add(r);
            jdbc.update("INSERT INTO question(id,course_id,chapter_id,original_key,content_fingerprint,mode,eligible_original) VALUES(?,?,?,?,?,'CHAPTER',true)",q,course,chapter,q,q.replace("-","")+"0".repeat(32));
            jdbc.update("INSERT INTO question_revision(id,release_id,question_id,stem,options,difficulty,source_locator,sort_order) VALUES(?,?,?,?,'[\"正确\",\"错误\",\"其他\"]',1,'测试来源',?)",r,release,q,"测试题"+i,i);
            jdbc.update("INSERT INTO question_solution(revision_id,correct_option,explanation) VALUES(?,0,'测试解析')",r);
            jdbc.update("INSERT INTO question_point(revision_id,point_id) VALUES(?,?)",r,point);
        }
        credit(20);
    }
    private static String uuid(){return UUID.randomUUID().toString();}
    private void credit(int count){jdbc.update("DELETE FROM practice_submission WHERE user_id=? AND question_id IN (SELECT id FROM question WHERE course_id=?)",user.id(),course);for(int i=0;i<count;i++)jdbc.update("INSERT INTO practice_submission(id,user_id,question_id,revision_id,selected_option,correct,submitted_at,idempotency_key) VALUES(?,?,?,?,0,true,UTC_TIMESTAMP(6),?)",uuid(),user.id(),questionIds.get(i),revisions.get(i),uuid());}
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
    private String base(){return "/api/v1/practice/courses/"+course;}
    private String body(){return "{\"kind\":\"CHAPTER\",\"chapterId\":\""+chapter+"\"}";}
    private JsonNode apply(String key)throws Exception{return read(post(base()+"/assessments").param("cycleId",CYCLE).header("Idempotency-Key",key).contentType("application/json").content(body()));}
    private JsonNode session(String id)throws Exception{return read(get(base()+"/assessments/"+id));}
    private JsonNode save(String id,JsonNode question,int option)throws Exception {
        var command=mapper.createObjectNode();command.put("selectedOption",option);command.set("expectedSavedAt",question.path("answerSavedAt"));
        return read(put(base()+"/assessments/"+id+"/answers/"+question.path("question").path("revisionId").asText()).contentType("application/json").content(command.toString()));
    }
    private JsonNode submit(String id,String fingerprint)throws Exception{return read(post(base()+"/assessments/"+id+"/submission").param("cycleId",CYCLE).contentType("application/json").content("{\"confirm\":true,\"answerFingerprint\":\""+fingerprint+"\"}"));}
    private void error(MockHttpServletRequestBuilder r,int statusCode,int code)throws Exception {mvc.perform(as(user,r)).andExpect(status().is(statusCode)).andExpect(jsonPath("$.code").value(code));}
    @Test void defaultPolicyDoesNotApproveImportedBankAndGateUsesServerCounts()throws Exception {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM assessment_policy p JOIN content_release r ON r.id=p.release_id JOIN course c ON c.id=r.course_id WHERE r.state='PUBLISHED' AND c.course_type='THEORY' AND c.id<>?",Integer.class,course)).isGreaterThanOrEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM question WHERE eligible_original=true AND course_id=?",Integer.class,COURSE)).isZero();
        credit(0);error(post(base()+"/assessments").param("cycleId",CYCLE).header("Idempotency-Key",uuid()).contentType("application/json").content(body()),403,40303);
        assertThat(read(get(base()+"/stats").param("chapterId",chapter)).path("canApplyChapterAssessment").asBoolean()).isFalse();
        credit(20);assertThat(read(get(base()+"/stats").param("chapterId",chapter)).path("canApplyChapterAssessment").asBoolean()).isTrue();
        error(post(base()+"/assessments").param("cycleId",CYCLE).header("Idempotency-Key",uuid()).contentType("application/json").content(body().replace("}",",\"score\":100}")),400,40001);
    }
    @Test void createsFrozenQuestionsPrefersUnansweredAndExactReplayAfterSave()throws Exception {
        String key=uuid();JsonNode original=apply(key);String id=original.path("id").asText();assertThat(original.path("questionCount").asInt()).isEqualTo(20);
        Set<String> ids=new HashSet<>();original.path("questions").forEach(q->{var question=q.path("question");ids.add(question.path("id").asText());assertThat(question.has("correctOption")).isFalse();assertThat(question.has("latestOutcome")).isFalse();assertThat(question.has("explanation")).isFalse();});
        assertThat(ids).hasSize(20).containsAll(questionIds.subList(20,25));
        save(id,original.path("questions").get(0),0);assertThat(apply(key)).isEqualTo(original);
        error(post(base()+"/assessments").param("cycleId",CYCLE).header("Idempotency-Key",key).contentType("application/json").content("{\"kind\":\"MOCK\",\"chapterId\":null}"),409,40904);
        read(get(base()+"/assessments").param("chapterId",chapter).param("kind","CHAPTER").param("status","IN_PROGRESS"));
        error(get(base()+"/assessments/"+id+"/result").param("cycleId",CYCLE),403,40304);
        mvc.perform(as(newUser(),get(base()+"/assessments/"+id))).andExpect(status().isNotFound());
    }
    @Test void answerCASAndFingerprintPreventLostUpdatesAndDuplicateSubmission()throws Exception {
        JsonNode session=apply(uuid());String id=session.path("id").asText();JsonNode q=session.path("questions").get(0);JsonNode saved=save(id,q,0);
        error(put(base()+"/assessments/"+id+"/answers/"+q.path("question").path("revisionId").asText()).contentType("application/json").content("{\"selectedOption\":1,\"expectedSavedAt\":null}"),409,40902);
        error(post(base()+"/assessments/"+id+"/submission").param("cycleId",CYCLE).contentType("application/json").content("{\"confirm\":true,\"answerFingerprint\":\""+session.path("answerFingerprint").asText()+"\"}"),409,40902);
        JsonNode result=submit(id,saved.path("answerFingerprint").asText());assertThat(result.path("correctCount").asInt()).isEqualTo(1);assertThat(result.path("passed").asBoolean()).isFalse();assertThat(result.path("answers").size()).isEqualTo(20);
        assertThat(submit(id,"0".repeat(64)).path("submittedAt")).isEqualTo(result.path("submittedAt"));
        read(get(base()+"/assessments/"+id+"/result").param("cycleId",CYCLE));
        error(put(base()+"/assessments/"+id+"/answers/"+q.path("question").path("revisionId").asText()).contentType("application/json").content("{\"selectedOption\":0,\"expectedSavedAt\":null}"),409,40902);
    }
    @Test void invalidCommandsNeverSaveOrRevealAnotherUsersSession()throws Exception {
        JsonNode s=apply(uuid());String id=s.path("id").asText();String revision=s.path("questions").get(0).path("question").path("revisionId").asText();
        error(put(base()+"/assessments/"+id+"/answers/"+revision).contentType("application/json").content("{\"selectedOption\":99,\"expectedSavedAt\":null}"),422,42202);
        error(put(base()+"/assessments/"+id+"/answers/"+uuid()).contentType("application/json").content("{\"selectedOption\":0,\"expectedSavedAt\":null}"),404,40401);
        error(post(base()+"/assessments/"+id+"/submission").param("cycleId",CYCLE).contentType("application/json").content("{\"confirm\":false,\"answerFingerprint\":\""+s.path("answerFingerprint").asText()+"\"}"),400,40001);
        mvc.perform(as(newUser(),put(base()+"/assessments/"+id+"/answers/"+revision).contentType("application/json").content("{\"selectedOption\":0,\"expectedSavedAt\":null}"))).andExpect(status().isNotFound());
        mvc.perform(get(base()+"/assessments/"+id)).andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM assessment_answer_event WHERE session_id=?",Integer.class,id)).isZero();
        save(id,s.path("questions").get(0),0);JsonNode current=session(id);save(id,current.path("questions").get(0),1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM assessment_answer_event WHERE session_id=?",Integer.class,id)).isEqualTo(2);
    }
    @Test void exactRatioPassDoesNotRoundAndLaterFailurePreservesEarlierPass()throws Exception {
        jdbc.update("UPDATE assessment_policy SET chapter_min_questions=21,chapter_pass_score=90.48 WHERE id=?",policy);
        JsonNode s=apply(uuid());String id=s.path("id").asText();for(int i=0;i<19;i++)save(id,s.path("questions").get(i),0);
        JsonNode fail=submit(id,session(id).path("answerFingerprint").asText());assertThat(fail.path("score").asDouble()).isEqualTo(90.48);assertThat(fail.path("passed").asBoolean()).isFalse();
        jdbc.update("UPDATE assessment_policy SET chapter_pass_score=90 WHERE id=?",policy);
        s=apply(uuid());id=s.path("id").asText();for(int i=0;i<19;i++)save(id,s.path("questions").get(i),0);
        JsonNode pass=submit(id,session(id).path("answerFingerprint").asText());assertThat(pass.path("passed").asBoolean()).isTrue();assertThat(pass.path("pass").path("releaseId").asText()).isEqualTo(release);
        s=apply(uuid());submit(s.path("id").asText(),s.path("answerFingerprint").asText());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM assessment_pass WHERE user_id=? AND course_id=? AND invalidated_at IS NULL",Integer.class,user.id(),course)).isEqualTo(1);
    }
    @Test void expiredGETIsReadOnlyThenBackgroundSettlesOfflineAndWritesCannotChangeAnswers()throws Exception {
        JsonNode s=apply(uuid());String id=s.path("id").asText();save(id,s.path("questions").get(0),0);expire(id);
        JsonNode before=session(id);assertThat(before.path("deadlineReached").asBoolean()).isTrue();assertThat(before.path("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(repository.pending()).contains(id);new cn.xuexizhitu.config.AssessmentTimeoutWorker(repository,service).settle();service.settleTimeout(id);
        JsonNode result=read(get(base()+"/assessments/"+id+"/result").param("cycleId",CYCLE));assertThat(result.path("status").asText()).isEqualTo("TIMED_OUT");assertThat(result.path("correctCount").asInt()).isEqualTo(1);
        assertThat(result.path("submittedAt").asText()).isEqualTo(before.path("deadlineAt").asText());
        JsonNode second=apply(uuid());id=second.path("id").asText();expire(id);
        error(put(base()+"/assessments/"+id+"/answers/"+second.path("questions").get(0).path("question").path("revisionId").asText()).contentType("application/json").content("{\"selectedOption\":0,\"expectedSavedAt\":null}"),409,40902);
        assertThat(jdbc.queryForObject("SELECT status FROM assessment_session WHERE id=?",String.class,id)).isEqualTo("TIMED_OUT");
    }
    private void expire(String id){jdbc.update("UPDATE assessment_session SET started_at=UTC_TIMESTAMP(6)-INTERVAL 41 MINUTE,deadline_at=UTC_TIMESTAMP(6)-INTERVAL 1 MINUTE WHERE id=?",id);}
    @Test void insufficientPointMatchingPersistsAlertAndNoSession()throws Exception {
        String p=uuid();jdbc.update("INSERT INTO knowledge_point(id,chapter_id,stable_key,title) VALUES(?,?,'missing','未覆盖')",p,chapter);jdbc.update("INSERT INTO release_point(release_id,chapter_id,point_id,title_snapshot) VALUES(?,?,?,'未覆盖')",release,chapter,p);
        error(post(base()+"/assessments").param("cycleId",CYCLE).header("Idempotency-Key",uuid()).contentType("application/json").content(body()),422,42201);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM bank_alert WHERE course_id=?",Integer.class,course)).isEqualTo(1);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM assessment_session WHERE course_id=?",Integer.class,course)).isZero();
    }
    @Test void concurrentApplyAndSubmitOnlyCreateOneSessionAndPass()throws Exception {
        String key=uuid();ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try {
            Callable<String> applyTask=()->{start.await();return mvc.perform(as(user,post(base()+"/assessments").param("cycleId",CYCLE).header("Idempotency-Key",key).contentType("application/json").content(body()))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();};
            var a=pool.submit(applyTask);var b=pool.submit(applyTask);start.countDown();assertThat(mapper.readTree(a.get(30,TimeUnit.SECONDS))).isEqualTo(mapper.readTree(b.get(30,TimeUnit.SECONDS)));
            JsonNode s=apply(key);String id=s.path("id").asText();for(JsonNode q:s.path("questions"))save(id,q,0);String fingerprint=session(id).path("answerFingerprint").asText();
            Callable<String> task=()->mvc.perform(as(user,post(base()+"/assessments/"+id+"/submission").param("cycleId",CYCLE).contentType("application/json").content("{\"confirm\":true,\"answerFingerprint\":\""+fingerprint+"\"}"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            a=pool.submit(task);b=pool.submit(task);assertThat(mapper.readTree(a.get(30,TimeUnit.SECONDS)).path("data").path("pass")).isEqualTo(mapper.readTree(b.get(30,TimeUnit.SECONDS)).path("data").path("pass"));
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM assessment_pass WHERE session_id=?",Integer.class,id)).isEqualTo(1);
        }finally{pool.shutdownNow();}
    }
    @Test void publishedUpgradeKeepsOldQuestionSnapshotAndApplicationReplay()throws Exception {
        String key=uuid();JsonNode original=apply(key);String id=original.path("id").asText();String newer=uuid();
        jdbc.update("UPDATE content_release SET state='RETIRED' WHERE id=?",release);jdbc.update("INSERT INTO content_release(id,course_id,version_no,state,published_at) VALUES(?,?,2,'PUBLISHED',UTC_TIMESTAMP(6))",newer,course);
        jdbc.update("INSERT INTO chapter_revision SELECT ?,chapter_id,course_id,title,sort_order,participates_in_assessment,gate_override FROM chapter_revision WHERE release_id=?",newer,release);
        jdbc.update("INSERT INTO assessment_policy(id,course_id,release_id) VALUES(?,?,?)",uuid(),course,newer);
        assertThat(apply(key)).isEqualTo(original);assertThat(session(id).path("questions")).isEqualTo(original.path("questions"));
        for(JsonNode q:original.path("questions"))save(id,q,0);
        assertThat(submit(id,session(id).path("answerFingerprint").asText()).path("pass").path("releaseId").asText()).isEqualTo(release);
    }
    @Test void mockRequiresValidWeightsAndPassesThenUnlockNeedsEnrollment()throws Exception {
        error(post(base()+"/assessments").param("cycleId",CYCLE).header("Idempotency-Key",uuid()).contentType("application/json").content("{\"kind\":\"MOCK\",\"chapterId\":null}"),403,40303);
        JsonNode s=apply(uuid());String id=s.path("id").asText();for(JsonNode q:s.path("questions"))save(id,q,0);submit(id,session(id).path("answerFingerprint").asText());
        error(post(base()+"/assessments").param("cycleId",CYCLE).header("Idempotency-Key",uuid()).contentType("application/json").content("{\"kind\":\"MOCK\",\"chapterId\":null}"),422,42202);
        int year=java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).getYear();
        UserDto reviewer=accounts.create(new RegisterRequest("admin"+uuid(),uuid()+"@example.com","12345678"),Role.ADMIN);
        jdbc.update("INSERT INTO mock_chapter_weight(policy_id,chapter_id,score_share,sample_from,sample_to,evidence,approved_by,approved_at) VALUES(?,?,1,?,?,?, ?,UTC_TIMESTAMP(6))",policy,chapter,(year-5)+"-01-01",(year-1)+"-12-31","{\"description\":\"仅测试审核\",\"sourcePaperKeys\":[\"T"+course.substring(0,10)+":"+(year-1)+"-04\"]}",reviewer.id());
        jdbc.update("UPDATE assessment_policy SET mock_question_count=26 WHERE id=?",policy);
        error(post(base()+"/assessments").param("cycleId",CYCLE).header("Idempotency-Key",uuid()).contentType("application/json").content("{\"kind\":\"MOCK\",\"chapterId\":null}"),422,42201);
        jdbc.update("UPDATE assessment_policy SET mock_question_count=20 WHERE id=?",policy);
        String validEvidence=jdbc.queryForObject("SELECT evidence FROM mock_chapter_weight WHERE policy_id=?",String.class,policy);
        jdbc.update("UPDATE mock_chapter_weight SET evidence='{\"description\":\"错误课程\",\"sourcePaperKeys\":[\"00023:2025-04\"]}' WHERE policy_id=?",policy);
        error(post(base()+"/assessments").param("cycleId",CYCLE).header("Idempotency-Key",uuid()).contentType("application/json").content("{\"kind\":\"MOCK\",\"chapterId\":null}"),422,42202);
        jdbc.update("UPDATE mock_chapter_weight SET evidence=? WHERE policy_id=?",validEvidence,policy);
        JsonNode mock=read(post(base()+"/assessments").param("cycleId",CYCLE).header("Idempotency-Key",uuid()).contentType("application/json").content("{\"kind\":\"MOCK\",\"chapterId\":null}"));id=mock.path("id").asText();for(JsonNode q:mock.path("questions"))save(id,q,0);
        JsonNode result=submit(id,session(id).path("answerFingerprint").asText());assertThat(result.path("unlock").path("canDownloadPapers").asBoolean()).isFalse();assertThat(result.path("pass").path("kind").asText()).isEqualTo("MOCK");
        jdbc.update("INSERT INTO enrollment(user_id,cycle_id,course_id,paid) VALUES(?,?,?,true)",user.id(),CYCLE,course);
        JsonNode unlock=read(get("/api/v1/exams/courses/"+course+"/unlock").param("cycleId",CYCLE));assertThat(unlock.path("canDownloadPapers").asBoolean()).isTrue();
        jdbc.update("UPDATE assessment_pass SET invalidated_at=UTC_TIMESTAMP(6),invalidated_by=?,invalidation_reason='测试作废' WHERE user_id=? AND course_id=? AND kind='MOCK'",user.id(),user.id(),course);
        assertThat(read(get("/api/v1/exams/courses/"+course+"/unlock").param("cycleId",CYCLE)).path("canWriteScores").asBoolean()).isFalse();
    }
}
