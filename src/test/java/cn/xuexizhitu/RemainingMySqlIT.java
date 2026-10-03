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
class RemainingMySqlIT {
    @Container static MySQLContainer<?> mysql=new MySQLContainer<>("mysql:8.4").withDatabaseName("remaining_test");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",mysql::getJdbcUrl);r.add("spring.datasource.username",mysql::getUsername);r.add("spring.datasource.password",mysql::getPassword);
        r.add("app.files.root",()->"/private/tmp/xuexi-files-it-"+mysql.getContainerId());r.add("app.bootstrap.enabled",()->false);r.add("app.auth.registration-mode",()->"DISABLED");
        r.add("app.jwt.secret-base64",()->"MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
    }
    @Autowired cn.xuexizhitu.service.PrivateFileStore files;
    @Autowired cn.xuexizhitu.service.PlanService plans;
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired JdbcTemplate jdbc; @Autowired AccountService accounts;
    private UserDto user;
    private static final List<JsonNode> samples=new CopyOnWriteArrayList<>();
    @AfterAll static void saveContractSamples() throws Exception {
        var directory=java.nio.file.Path.of("/private/tmp/xuexi-files-it-"+mysql.getContainerId());if(java.nio.file.Files.exists(directory))try(var paths=java.nio.file.Files.walk(directory)){for(var path:paths.sorted(java.util.Comparator.reverseOrder()).toList())java.nio.file.Files.deleteIfExists(path);}
        new ObjectMapper().writeValue(new java.io.File("target/remaining-contract-samples.json"),samples);
    }
    private static final String COURSE="afdac469-0fe4-5007-833c-51a71333967b", OTHER="ccd819bb-a48b-5c75-bfbf-e26219023fef", CYCLE="0ebdbbfe-d607-54b5-9c21-4e0adade5e4c";
    @BeforeEach void account() {user=newUser();}
    private UserDto newUser() {String name="learning"+UUID.randomUUID();return accounts.create(new RegisterRequest(name,name+"@example.com","12345678"),Role.ADMIN);}
    private MockHttpServletRequestBuilder as(UserDto u,MockHttpServletRequestBuilder request) {
        return request.with(authentication(new UsernamePasswordAuthenticationToken(new CurrentUser(u.id(),u.username(),u.role(),0),null,List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_"+u.role())))));
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
    private String base(){return "/api/v1/exams/courses/"+COURSE;}
    private JsonNode json(MockHttpServletRequestBuilder r,JsonNode body)throws Exception{return read(r.contentType("application/json").content(body.toString()));}
    private JsonNode node(Object value){return mapper.valueToTree(value);}
    private JsonNode uploadPdf()throws Exception {return read(multipart("/api/v1/admin/files").file(new org.springframework.mock.web.MockMultipartFile("file","试卷.pdf","application/pdf","%PDF-1.7\n1 0 obj\n<< >>\nendobj\n%%EOF\n".getBytes(java.nio.charset.StandardCharsets.UTF_8))).param("purpose","PAPER").param("containsAnswers","true"));}
    private JsonNode paper(String file,String month)throws Exception{return json(post("/api/v1/admin/exams/courses/"+COURSE+"/papers"),node(cn.xuexizhitu.common.BusinessData.obj("paperMonth",month,"sourceCourseCode",null,"questionFileId",file,"answerFileId",null,"questionPages",1,"answerPages",null,"note","测试试卷")));}
    private void unlock()throws Exception{jdbc.update("INSERT INTO enrollment(user_id,cycle_id,course_id,paid) VALUES(?,?,?,true)",user.id(),CYCLE,COURSE);jdbc.update("UPDATE cycle_course SET exam_date=? WHERE cycle_id=? AND course_id=?",cn.xuexizhitu.common.BusinessData.today().plusDays(7),CYCLE,COURSE);json(post(base()+"/cycles/"+CYCLE+"/overrides"),node(Map.of("confirm",true)));}
    private JsonNode scoreCommand(String paper,java.time.LocalDate day,int score,boolean complete){return node(cn.xuexizhitu.common.BusinessData.obj("cycleId",CYCLE,"paperId",paper,"practicedOn",day.toString(),"score",score,"minutes",120,"limitMinutes",150,"complete",complete,"closedBook",true,"answersSeenBefore",false,"note","测试"));}
    @Test void papersScoresImagesAndRevocationUseOneAuthorizationAndKeepHistory()throws Exception {
        JsonNode file=uploadPdf();String month="2010-"+String.format("%02d",1+jdbc.queryForObject("SELECT COUNT(*) FROM paper",Integer.class));JsonNode paper=paper(file.path("id").asText(),month);String pid=paper.path("id").asText();
        json(put("/api/v1/admin/exams/courses/"+COURSE+"/papers/"+pid),node(cn.xuexizhitu.common.BusinessData.obj("paperMonth",month,"sourceCourseCode",null,"questionFileId",file.path("id").asText(),"answerFileId",null,"questionPages",2,"answerPages",null,"note","更新页数")));
        read(get("/api/v1/admin/files").param("purpose","PAPER"));read(get(base()+"/papers").param("cycleId",CYCLE));read(get(base()+"/papers/"+pid));
        mvc.perform(as(user,get(base()+"/papers/"+pid+"/file").param("cycleId",CYCLE).param("part","QUESTION"))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(40302));
        unlock();JsonNode downloaded=read(get(base()+"/papers/"+pid+"/file").param("cycleId",CYCLE).param("part","QUESTION"));assertThat(downloaded.path("file").path("containsAnswers").asBoolean()).isTrue();
        JsonNode body=scoreCommand(pid,cn.xuexizhitu.common.BusinessData.today(),72,true);JsonNode score=json(post(base()+"/scores"),body);String sid=score.path("record").path("id").asText();read(get(base()+"/scores"));read(get(base()+"/scores/"+sid));read(get(base()+"/prediction"));read(get(base()+"/score-trend"));
        var update=(com.fasterxml.jackson.databind.node.ObjectNode)body.deepCopy();update.remove(List.of("cycleId","paperId"));update.put("expectedRevision",0);update.put("score",81);JsonNode edited=json(put(base()+"/scores/"+sid),update);assertThat(edited.path("record").path("createdAt")).isEqualTo(score.path("record").path("createdAt"));
        mvc.perform(as(user,put(base()+"/scores/"+sid).contentType("application/json").content(update.toString()))).andExpect(status().isConflict());
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);
        JsonNode image=read(multipart(base()+"/scores/"+sid+"/images").file(new org.springframework.mock.web.MockMultipartFile("file","答案.png","image/png",out.toByteArray())));String fid=image.path("id").asText();read(get(base()+"/scores/"+sid+"/images/"+fid));
        JsonNode unlocked=read(get(base()+"/unlock").param("cycleId",CYCLE));json(post(base()+"/cycles/"+CYCLE+"/overrides/current/revocation"),node(Map.of("confirm",true,"expectedRevision",unlocked.path("activeOverride").path("revision").asInt())));
        mvc.perform(as(user,get(base()+"/papers/"+pid+"/file").param("cycleId",CYCLE).param("part","QUESTION"))).andExpect(status().isForbidden());
        read(get(base()+"/scores/"+sid));read(get(base()+"/scores/"+sid+"/images/"+fid));read(delete(base()+"/scores/"+sid+"/images/"+fid));mvc.perform(as(user,get(base()+"/scores/"+sid+"/images/"+fid))).andExpect(status().isNotFound());files.cleanup();assertThat(jdbc.queryForObject("SELECT state FROM stored_file WHERE id=?",String.class,fid)).isEqualTo("DELETED");
        var other=accounts.create(new RegisterRequest("normal"+UUID.randomUUID(),UUID.randomUUID()+"@example.com","12345678"),Role.USER);mvc.perform(as(other,get("/api/v1/admin/files"))).andExpect(status().isForbidden());mvc.perform(as(other,get(base()+"/scores/"+sid))).andExpect(status().isNotFound());
    }
    @Test void draftPublishingIsIsolatedAndStableProgressSurvivesReleaseUpgrade()throws Exception {
        String javaCourse="e2dd5f58-a12f-55e0-a691-455ea2233699";String release=jdbc.queryForObject("SELECT id FROM content_release WHERE course_id=? AND state='PUBLISHED'",String.class,javaCourse);String admin="/api/v1/admin/courses/"+javaCourse+"/releases";
        JsonNode draft=json(post(admin),node(cn.xuexizhitu.common.BusinessData.obj("basedOnReleaseId",release,"sourceSha",null)));String id=draft.path("id").asText();read(get(admin));
        for(String section:List.of("catalog","knowledge","questions","task-templates")){JsonNode content=read(get(admin+"/"+id+"/"+section));json(put(admin+"/"+id+"/"+section),content);}
        JsonNode policy=read(get(admin+"/"+id+"/assessment-policy"));var write=(com.fasterxml.jackson.databind.node.ObjectNode)policy.deepCopy();write.remove(List.of("id","courseId","releaseId"));write.put("confirmWeightReview",true);json(put(admin+"/"+id+"/assessment-policy"),write);
        assertThat(jdbc.queryForObject("SELECT id FROM content_release WHERE course_id=? AND state='PUBLISHED'",String.class,javaCourse)).isEqualTo(release);
        JsonNode catalog=read(get(admin+"/"+id+"/knowledge"));JsonNode bad=catalog.deepCopy();var parents=bad.findParents("fileId");assertThat(parents).isNotEmpty();String oldFile=jdbc.queryForObject("SELECT id FROM stored_file WHERE storage_key LIKE 'classpath:manuals/%' AND original_name LIKE '%java%' LIMIT 1",String.class);((com.fasterxml.jackson.databind.node.ObjectNode)parents.get(0)).put("fileId",oldFile);json(put(admin+"/"+id+"/knowledge"),bad);
        assertThat(read(get(admin+"/"+id+"/validation")).path("valid").asBoolean()).isFalse();mvc.perform(as(user,post(admin+"/"+id+"/publication").contentType("application/json").content("{\"confirm\":true}"))).andExpect(status().isUnprocessableEntity());assertThat(jdbc.queryForObject("SELECT id FROM content_release WHERE course_id=? AND state='PUBLISHED'",String.class,javaCourse)).isEqualTo(release);json(put(admin+"/"+id+"/knowledge"),catalog);
        JsonNode validation=read(get(admin+"/"+id+"/validation"));assertThat(validation.path("valid").asBoolean()).withFailMessage(validation.toString()).isTrue();
        json(post(admin+"/"+id+"/publication"),node(Map.of("confirm",true)));assertThat(jdbc.queryForObject("SELECT id FROM content_release WHERE course_id=? AND state='PUBLISHED'",String.class,javaCourse)).isEqualTo(id);
        read(get("/api/v1/catalog/courses/"+javaCourse+"/manual"));read(get("/api/v1/admin/dashboard/audit-events").param("targetId",id));
        mvc.perform(as(user,put(admin+"/"+id+"/questions").contentType("application/json").content("{\"questions\":[]}"))).andExpect(status().isConflict());
    }
    private String planCourse,planRelease,planChapter,planItem,planCycle;
    private void planFixture(){planCourse=UUID.randomUUID().toString();planRelease=UUID.randomUUID().toString();planChapter=UUID.randomUUID().toString();planItem=UUID.randomUUID().toString();planCycle=UUID.randomUUID().toString();jdbc.update("INSERT INTO course(id,code,name,course_type) VALUES(?,?,?,'THEORY')",planCourse,String.valueOf(90000+jdbc.queryForObject("SELECT COUNT(*) FROM course",Integer.class)),"计划测试");jdbc.update("INSERT INTO exam_cycle(id,name,start_date,end_date) VALUES(?,'计划测试',?,?)",planCycle,cn.xuexizhitu.common.BusinessData.today().minusDays(3),cn.xuexizhitu.common.BusinessData.today().plusDays(10));jdbc.update("INSERT INTO cycle_course(cycle_id,course_id,exam_date) VALUES(?,?,?)",planCycle,planCourse,cn.xuexizhitu.common.BusinessData.today().plusDays(10));jdbc.update("INSERT INTO content_release(id,course_id,version_no,state,published_at) VALUES(?,?,1,'PUBLISHED',UTC_TIMESTAMP(6))",planRelease,planCourse);jdbc.update("INSERT INTO chapter(id,course_id,stable_key) VALUES(?,?,'p')",planChapter,planCourse);jdbc.update("INSERT INTO chapter_revision(release_id,chapter_id,course_id,title,sort_order,participates_in_assessment) VALUES(?,?,?,'测试章',0,false)",planRelease,planChapter,planCourse);jdbc.update("INSERT INTO study_item(id,chapter_id,stable_key) VALUES(?,?,'p')",planItem,planChapter);jdbc.update("INSERT INTO item_revision(release_id,item_id,chapter_id,title,estimated_minutes,sort_order) VALUES(?,?,?,'测试任务',180,0)",planRelease,planItem,planChapter);}
    private JsonNode planCommand(){var start=cn.xuexizhitu.common.BusinessData.today().minusDays(2);List<Object> caps=new ArrayList<>();for(int i=0;i<7;i++)caps.add(cn.xuexizhitu.common.BusinessData.obj("day",start.plusDays(i).toString(),"capacityMinutes",60));return node(Map.of("config",cn.xuexizhitu.common.BusinessData.obj("cycleId",planCycle,"startDate",start.toString(),"endDate",start.plusDays(6).toString(),"coursePriority",List.of(planCourse),"courseScope",List.of(planCourse),"dayCapacities",caps),"acceptUnscheduled",false));}
    @Test void planPreviewsOnlyMoveOverdueSegmentsAndRequireFreshConfirmation()throws Exception {
        planFixture();JsonNode created=json(post("/api/v1/schedule/plans"),planCommand());String id=created.path("id").asText();String route="/api/v1/schedule/plans/"+id;read(get("/api/v1/schedule/plans").param("cycleId",planCycle));read(get(route));read(get(route+"/revisions/1"));
        JsonNode preview=json(post(route+"/reschedule-previews"),node(Map.of("baseRevision",1)));assertThat(preview.path("moves").size()).isEqualTo(2);assertThat(preview.path("proposedPlan").path("days").get(2).path("reservedMinutes").asInt()).isEqualTo(60);assertThat(read(get(route)).path("revision").asInt()).isEqualTo(1);String pid=preview.path("id").asText();read(get(route+"/reschedule-previews/"+pid));
        JsonNode confirmed=json(post(route+"/reschedule-previews/"+pid+"/confirmation"),node(Map.of("baseRevision",1,"inputFingerprint",preview.path("inputFingerprint").asText(),"acceptUnscheduled",false,"confirm",true)));assertThat(confirmed.path("revision").asInt()).isEqualTo(2);
        JsonNode next=json(post(route+"/reschedule-previews"),node(Map.of("baseRevision",2)));String task=created.path("tasks").get(0).path("id").asText();json(put(route+"/tasks/"+task+"/completion"),node(cn.xuexizhitu.common.BusinessData.obj("completed",true,"expectedCompleted",false,"expectedItemRevision",0,"baseRevision",2,"clientMutationId",UUID.randomUUID().toString())));
        mvc.perform(as(user,post(route+"/reschedule-previews/"+next.path("id").asText()+"/confirmation").contentType("application/json").content(node(Map.of("baseRevision",2,"inputFingerprint",next.path("inputFingerprint").asText(),"acceptUnscheduled",false,"confirm",true)).toString()))).andExpect(status().isConflict());
        jdbc.update("UPDATE course SET active=false WHERE id=?",planCourse);read(get(route));
        read(get("/api/v1/dashboard").param("cycleId",planCycle).param("planId",id));mvc.perform(as(newUser(),get(route))).andExpect(status().isNotFound());
    }
    @Test void administratorCyclesCourseUpdatesAndAlertsAreAudited()throws Exception {
        JsonNode cycle=node(cn.xuexizhitu.common.BusinessData.obj("name","管理测试","startDate",cn.xuexizhitu.common.BusinessData.today().toString(),"endDate",cn.xuexizhitu.common.BusinessData.today().plusDays(30).toString(),"courses",List.of(cn.xuexizhitu.common.BusinessData.obj("courseId",COURSE,"examDate",null,"startsAt",null,"endsAt",null))));JsonNode created=json(post("/api/v1/admin/exams/cycles"),cycle);json(put("/api/v1/admin/exams/cycles/"+created.path("id").asText()),cycle);
        json(put("/api/v1/admin/courses/"+COURSE).param("cycleId",CYCLE),node(Map.of("code","00023","name","高等数学","courseType","THEORY","active",true)));
        String release=jdbc.queryForObject("SELECT id FROM content_release WHERE course_id=? AND state='PUBLISHED'",String.class,COURSE),alert=UUID.randomUUID().toString();jdbc.update("INSERT INTO bank_alert(id,course_id,release_id,alert_code,details) VALUES(?,?,?,'INSUFFICIENT_QUESTIONS','{\"reason\":\"测试\"}')",alert,COURSE,release);read(get("/api/v1/admin/practice/alerts").param("courseId",COURSE));json(post("/api/v1/admin/practice/alerts/"+alert+"/acknowledgement"),node(Map.of("confirm",true)));
        var badTime=cycle.deepCopy();((com.fasterxml.jackson.databind.node.ObjectNode)badTime.path("courses").get(0)).put("examDate",cn.xuexizhitu.common.BusinessData.today().toString()).put("startsAt","99:00").put("endsAt","99:30");mvc.perform(as(user,post("/api/v1/admin/exams/cycles").contentType("application/json").content(badTime.toString()))).andExpect(status().isBadRequest());mvc.perform(as(user,get("/api/v1/dashboard"))).andExpect(status().isBadRequest());
        mvc.perform(as(user,post("/api/v1/admin/courses/"+UUID.randomUUID()+"/releases").contentType("application/json").content("{\"basedOnReleaseId\":null,\"sourceSha\":null}"))).andExpect(status().isNotFound());
        read(get("/api/v1/practice/courses/"+COURSE+"/passes"));read(get("/api/v1/admin/practice/legacy-credits"));read(get("/api/v1/admin/exams/legacy-pass-reviews"));read(get("/api/v1/exams/history"));
    }

    private String legacy(String kind,JsonNode payload){String batch=UUID.randomUUID().toString(),id=UUID.randomUUID().toString(),hash="0".repeat(64);jdbc.update("INSERT INTO legacy_import_batch(id,source_sha,target_user_id,manifest_sha256,state,created_at) VALUES(?,?,?,?,'IMPORTED',UTC_TIMESTAMP(6))",batch,hash,user.id(),hash);jdbc.update("INSERT INTO legacy_record(id,batch_id,user_id,old_owner,old_id,old_kind,payload,source_hash) VALUES(?,?,?,'test-owner',?,?,?,?)",id,batch,user.id(),id,kind,payload.toString(),hash);return id;}
    @Test void legacyReviewAndFileDeletionPreserveArchiveAndCloseRevokedPass()throws Exception {
        String release=jdbc.queryForObject("SELECT id FROM content_release WHERE course_id=? AND state='PUBLISHED'",String.class,COURSE);
        String id=legacy("grading",node(Map.of("courseId",COURSE,"score",78,"status","old_done","token","hidden"))),review=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO legacy_pass_review(id,legacy_record_id,user_id,course_id,release_id,kind,old_score,old_threshold) VALUES(?,?,?,?,?,'MOCK',78,70)",review,id,user.id(),COURSE,release);
        String original=jdbc.queryForObject("SELECT payload FROM legacy_record WHERE id=?",String.class,id);
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);
        String file=files.upload(new org.springframework.mock.web.MockMultipartFile("file","历史.png","image/png",out.toByteArray()),"SCORE_IMAGE",user.id(),false);jdbc.update("UPDATE stored_file SET purpose='LEGACY_ATTACHMENT' WHERE id=?",file);jdbc.update("INSERT INTO legacy_file_link(legacy_record_id,file_id,old_object_key) VALUES(?,?,'private-old-key')",id,file);
        JsonNode detail=read(get("/api/v1/exams/history/"+id));assertThat(detail.path("archivedContent").asText()).doesNotContain("hidden");read(get("/api/v1/exams/history/"+id+"/files/"+file));mvc.perform(as(newUser(),delete("/api/v1/exams/history/"+id+"/files/"+file))).andExpect(status().isNotFound());
        read(get("/api/v1/admin/exams/legacy-pass-reviews/"+review));JsonNode accepted=json(post("/api/v1/admin/exams/legacy-pass-reviews/"+review+"/decision"),node(cn.xuexizhitu.common.BusinessData.obj("decision","ACCEPT","reason","人工核对旧分数和课程映射","courseId",COURSE,"chapterId",null,"mappingReleaseId",release)));assertThat(accepted.path("oldVersion").isNull()).isTrue();String pass=accepted.path("passId").asText();
        jdbc.update("INSERT INTO enrollment(user_id,cycle_id,course_id,paid) VALUES(?,?,?,true)",user.id(),CYCLE,COURSE);assertThat(read(get(base()+"/unlock").param("cycleId",CYCLE)).path("canWriteScores").asBoolean()).isTrue();
        JsonNode invalid=json(post("/api/v1/admin/practice/passes/"+pass+"/invalidation"),node(Map.of("confirm",true,"reason","复核发现映射错误")));assertThat(invalid.path("affectedUnlocks").get(0).path("canWriteScores").asBoolean()).isFalse();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM assessment_session WHERE user_id=?",Integer.class,user.id())).isZero();
        read(delete("/api/v1/exams/history/"+id+"/files/"+file));mvc.perform(as(user,get("/api/v1/exams/history/"+id+"/files/"+file))).andExpect(status().isNotFound());assertThat(read(get("/api/v1/exams/history/"+id)).path("files")).isEmpty();files.cleanup();assertThat(jdbc.queryForObject("SELECT payload FROM legacy_record WHERE id=?",String.class,id)).isEqualTo(original);
    }
    @Test void legacyCreditCountsDistinctQuestionsWithoutInventingAttempts()throws Exception {
        String question=jdbc.queryForList("SELECT id FROM question WHERE course_id=? AND mode='CHAPTER' LIMIT 1",String.class,COURSE).get(0);boolean original=Boolean.TRUE.equals(jdbc.queryForObject("SELECT eligible_original FROM question WHERE id=?",Boolean.class,question));jdbc.update("UPDATE question SET eligible_original=true WHERE id=?",question);
        try {String id=legacy("practice",node(Map.of("attempts",5)));jdbc.update("INSERT INTO legacy_practice_summary(legacy_record_id,user_id,question_id,old_question_id,attempts) VALUES(?,?,?,'old-question',5)",id,user.id(),question);
            JsonNode decision=node(Map.of("approved",true,"canonicalQuestionId",question,"reason","人工核对原始作答","confirm",true));JsonNode result=json(post("/api/v1/admin/practice/legacy-credits/"+id+"/decision"),decision);assertThat(result.path("summary").path("attempts").asInt()).isEqualTo(5);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM practice_submission WHERE user_id=?",Integer.class,user.id())).isZero();
            ((com.fasterxml.jackson.databind.node.ObjectNode)decision).put("approved",false);json(post("/api/v1/admin/practice/legacy-credits/"+id+"/decision"),decision);
        }finally{jdbc.update("UPDATE question SET eligible_original=? WHERE id=?",original,question);}
    }
    @Test void courseCreationUsesRealContractAndRejectsDuplicateCode()throws Exception {
        String code="13171",existing=jdbc.queryForObject("SELECT id FROM course WHERE code=?",String.class,code);jdbc.update("UPDATE course SET code='89999' WHERE id=?",existing);String created=null;
        try {JsonNode command=node(Map.of("code",code,"name","管理创建测试","courseType","PRACTICE","active",true));created=json(post("/api/v1/admin/courses").param("cycleId",CYCLE),command).path("id").asText();mvc.perform(as(user,post("/api/v1/admin/courses").param("cycleId",CYCLE).contentType("application/json").content(command.toString()))).andExpect(status().isUnprocessableEntity());
        }finally{if(created!=null){jdbc.update("DELETE FROM cycle_course WHERE course_id=?",created);jdbc.update("DELETE FROM course WHERE id=?",created);}jdbc.update("UPDATE course SET code=? WHERE id=?",code,existing);}
    }

    private void rejectConfirmation(String route,JsonNode preview)throws Exception {mvc.perform(as(user,post(route+"/reschedule-previews/"+preview.path("id").asText()+"/confirmation").contentType("application/json").content(node(Map.of("baseRevision",1,"inputFingerprint",preview.path("inputFingerprint").asText(),"acceptUnscheduled",false,"confirm",true)).toString()))).andExpect(status().isConflict());assertThat(read(get(route)).path("revision").asInt()).isEqualTo(1);}
    @Test void planConfirmationRejectsChangedExamDateCapacityAndBusinessDay()throws Exception {
        planFixture();String id=json(post("/api/v1/schedule/plans"),planCommand()).path("id").asText(),route="/api/v1/schedule/plans/"+id;
        JsonNode exam=json(post(route+"/reschedule-previews"),node(Map.of("baseRevision",1)));jdbc.update("UPDATE cycle_course SET exam_date=DATE_ADD(exam_date,INTERVAL 1 DAY) WHERE cycle_id=? AND course_id=?",planCycle,planCourse);rejectConfirmation(route,exam);
        JsonNode capacity=json(post(route+"/reschedule-previews"),node(Map.of("baseRevision",1)));jdbc.update("UPDATE plan_day_capacity SET capacity_minutes=capacity_minutes+1 WHERE plan_id=? AND revision_no=1",id);rejectConfirmation(route,capacity);
        JsonNode midnight=json(post(route+"/reschedule-previews"),node(Map.of("baseRevision",1)));jdbc.update("UPDATE plan_preview SET as_of=DATE_SUB(as_of,INTERVAL 1 DAY) WHERE id=?",midnight.path("id").asText());rejectConfirmation(route,midnight);
    }
    @Test void unknownExamDateNeedsExplicitGapAcceptanceAndInvalidImageIsRejected()throws Exception {
        planFixture();jdbc.update("UPDATE cycle_course SET exam_date=NULL WHERE cycle_id=? AND course_id=?",planCycle,planCourse);JsonNode command=planCommand();mvc.perform(as(user,post("/api/v1/schedule/plans").contentType("application/json").content(command.toString()))).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value(42204));((com.fasterxml.jackson.databind.node.ObjectNode)command).put("acceptUnscheduled",true);JsonNode plan=json(post("/api/v1/schedule/plans"),command);assertThat(plan.path("awaitingDate").get(0).path("minutes").asInt()).isEqualTo(180);assertThat(plan.path("unscheduled")).isEmpty();
        assertThatThrownBy(()->files.upload(new org.springframework.mock.web.MockMultipartFile("file","fake.png","image/png",java.util.HexFormat.of().parseHex("89504e470d0a1a0a")),"SCORE_IMAGE",user.id(),false)).isInstanceOf(cn.xuexizhitu.common.BusinessException.class);
    }

    @Test void completeConfigurationEditPreservesOldVersionAndCompletion() throws Exception {
        planFixture();JsonNode created=json(post("/api/v1/schedule/plans"),planCommand());String id=created.path("id").asText(),route="/api/v1/schedule/plans/"+id,task=created.path("tasks").get(0).path("id").asText();
        json(put(route+"/tasks/"+task+"/completion"),node(cn.xuexizhitu.common.BusinessData.obj("completed",true,"expectedCompleted",false,"expectedItemRevision",0,"baseRevision",1,"clientMutationId",UUID.randomUUID().toString())));
        var config=(com.fasterxml.jackson.databind.node.ObjectNode)created.path("config").deepCopy();config.put("name","修改后的计划");config.put("strategy","SEQUENTIAL");var start=cn.xuexizhitu.common.BusinessData.today();config.put("startDate",start.toString());config.put("endDate",start.plusDays(6).toString());List<Object> caps=new ArrayList<>();for(int i=0;i<7;i++)caps.add(cn.xuexizhitu.common.BusinessData.obj("day",start.plusDays(i).toString(),"capacityMinutes",i==0?0:60));config.set("dayCapacities",node(caps));
        JsonNode preview=json(post(route+"/reschedule-previews"),node(Map.of("baseRevision",1,"config",config)));assertThat(read(get(route)).path("config").has("name")).isFalse();assertThat(preview.path("proposedPlan").path("tasks").get(0).path("completed").asBoolean()).isTrue();assertThat(preview.path("proposedPlan").path("days").get(0).path("capacityMinutes").asInt()).isZero();
        JsonNode saved=json(post(route+"/reschedule-previews/"+preview.path("id").asText()+"/confirmation"),node(Map.of("baseRevision",1,"inputFingerprint",preview.path("inputFingerprint").asText(),"acceptUnscheduled",false,"confirm",true)));assertThat(saved.path("revision").asInt()).isEqualTo(2);assertThat(saved.path("config").path("name").asText()).isEqualTo("修改后的计划");assertThat(saved.path("tasks").get(0).path("id").asText()).isEqualTo(task);assertThat(saved.path("tasks").get(0).path("completed").asBoolean()).isTrue();assertThat(read(get(route+"/revisions/1")).path("config").path("startDate")).isEqualTo(created.path("config").path("startDate"));
    }
    @Test void changingScopeKeepsHistoricalTaskSetsAndRejectsStaleNewCourseProgress() throws Exception {
        planFixture();String oldCycle=planCycle;JsonNode created=json(post("/api/v1/schedule/plans"),planCommand());String route="/api/v1/schedule/plans/"+created.path("id").asText();String oldItem=planItem;planFixture();String newCourse=planCourse,newItem=planItem;
        jdbc.update("INSERT INTO cycle_course(cycle_id,course_id,exam_date) VALUES(?,?,?)",oldCycle,newCourse,cn.xuexizhitu.common.BusinessData.today().plusDays(10));var config=(com.fasterxml.jackson.databind.node.ObjectNode)created.path("config").deepCopy();config.put("name","换科目");config.set("courseScope",node(List.of(newCourse)));config.set("coursePriority",node(List.of(newCourse)));
        JsonNode preview=json(post(route+"/reschedule-previews"),node(Map.of("baseRevision",1,"config",config)));jdbc.update("INSERT INTO user_item_progress(user_id,item_id,completed,revision,completed_at) VALUES(?,?,true,1,UTC_TIMESTAMP(6))",user.id().toString(),newItem);rejectConfirmation(route,preview);
        JsonNode fresh=json(post(route+"/reschedule-previews"),node(Map.of("baseRevision",1,"config",config)));json(post(route+"/reschedule-previews/"+fresh.path("id").asText()+"/confirmation"),node(Map.of("baseRevision",1,"inputFingerprint",fresh.path("inputFingerprint").asText(),"acceptUnscheduled",false,"confirm",true)));assertThat(read(get(route)).path("tasks").get(0).path("itemId").asText()).isEqualTo(newItem);assertThat(read(get(route+"/revisions/1")).path("tasks").get(0).path("itemId").asText()).isEqualTo(oldItem);assertThat(read(get(route+"/revisions/1")).path("tasks").size()).isEqualTo(1);
    }
    @Test void weeklyPlanCreatesFourCourseWindowsAndFinalPaperWeek() throws Exception {
        List<String> order=new ArrayList<>();String cycle=null;var start=cn.xuexizhitu.common.BusinessData.today();for(int i=0;i<4;i++){planFixture();if(cycle==null)cycle=planCycle;order.add(planCourse);if(!cycle.equals(planCycle))jdbc.update("INSERT INTO cycle_course(cycle_id,course_id,exam_date) VALUES(?,?,?)",cycle,planCourse,start.plusDays(40));else jdbc.update("UPDATE cycle_course SET exam_date=? WHERE cycle_id=? AND course_id=?",start.plusDays(40),cycle,planCourse);jdbc.update("INSERT INTO plan_task_template(id,release_id,course_id,kind,title,estimated_minutes,sort_order) VALUES(?,?,?,'PAPER','真题测试',60,0)",UUID.randomUUID().toString(),planRelease,planCourse);}
        List<Object> caps=new ArrayList<>();for(int i=0;i<35;i++)caps.add(cn.xuexizhitu.common.BusinessData.obj("day",start.plusDays(i).toString(),"capacityMinutes",60));JsonNode config=node(cn.xuexizhitu.common.BusinessData.obj("cycleId",cycle,"name","完整五周","strategy","WEEKLY_35","startDate",start.toString(),"endDate",start.plusDays(34).toString(),"coursePriority",order,"courseScope",order,"dayCapacities",caps));JsonNode plan=json(post("/api/v1/schedule/plans"),node(Map.of("config",config,"acceptUnscheduled",false)));assertThat(plan.path("dayCount").asInt()).isEqualTo(35);assertThat(plan.path("weeks").size()).isEqualTo(5);
        Map<String,JsonNode> tasks=new HashMap<>();plan.path("tasks").forEach(t->tasks.put(t.path("id").asText(),t));for(int i=0;i<35;i++)for(JsonNode segment:plan.path("days").get(i).path("segments")){var task=tasks.get(segment.path("taskId").asText());if(i<28){assertThat(task.path("kind").asText()).isEqualTo("ITEM");assertThat(task.path("courseId").asText()).isEqualTo(order.get(i/7));}else assertThat(task.path("kind").asText()).isEqualTo("PAPER");}
    }
    @Test void weeklyPlanWithoutTemplatesHasRealPracticeReviewTasks() throws Exception {
        List<String> order=new ArrayList<>();String cycle=null;var start=cn.xuexizhitu.common.BusinessData.today();
        for(int i=0;i<4;i++){planFixture();if(cycle==null)cycle=planCycle;order.add(planCourse);if(!cycle.equals(planCycle))jdbc.update("INSERT INTO cycle_course(cycle_id,course_id,exam_date) VALUES(?,?,?)",cycle,planCourse,start.plusDays(40));else jdbc.update("UPDATE cycle_course SET exam_date=? WHERE cycle_id=? AND course_id=?",start.plusDays(40),cycle,planCourse);}
        List<Object> caps=new ArrayList<>();for(int i=0;i<35;i++)caps.add(cn.xuexizhitu.common.BusinessData.obj("day",start.plusDays(i).toString(),"capacityMinutes",60));
        JsonNode config=node(cn.xuexizhitu.common.BusinessData.obj("cycleId",cycle,"strategy","WEEKLY_35","startDate",start.toString(),"endDate",start.plusDays(34).toString(),"coursePriority",order,"courseScope",order,"dayCapacities",caps));
        JsonNode plan=json(post("/api/v1/schedule/plans"),node(Map.of("config",config,"acceptUnscheduled",false)));
        List<JsonNode> review=new ArrayList<>();plan.path("tasks").forEach(t->{if(t.path("kind").asText().equals("REVIEW"))review.add(t);});
        assertThat(review).hasSize(4);assertThat(plan.path("weeks").get(4).path("scheduledMinutes").asInt()).isEqualTo(240);
        for(var task:review){assertThat(task.path("templateId").isNull()).isTrue();assertThat(task.path("target").path("pane").asText()).isEqualTo("PRACTICE");}
        String route="/api/v1/schedule/plans/"+plan.path("id").asText();var changed=(com.fasterxml.jackson.databind.node.ObjectNode)config.deepCopy();changed.put("name","复习任务保留");
        JsonNode preview=json(post(route+"/reschedule-previews"),node(Map.of("baseRevision",1,"config",changed)));
        JsonNode saved=json(post(route+"/reschedule-previews/"+preview.path("id").asText()+"/confirmation"),node(Map.of("baseRevision",1,"inputFingerprint",preview.path("inputFingerprint").asText(),"acceptUnscheduled",false,"confirm",true)));
        assertThat(saved.path("tasks").size()).isEqualTo(8);
    }

}
