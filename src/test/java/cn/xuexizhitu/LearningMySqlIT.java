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
class LearningMySqlIT {
    @Container static MySQLContainer<?> mysql=new MySQLContainer<>("mysql:8.4").withDatabaseName("learning_test");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",mysql::getJdbcUrl);r.add("spring.datasource.username",mysql::getUsername);r.add("spring.datasource.password",mysql::getPassword);
        r.add("app.bootstrap.enabled",()->false);r.add("app.auth.registration-mode",()->"DISABLED");
        r.add("app.jwt.secret-base64",()->"MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
    }
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired JdbcTemplate jdbc; @Autowired AccountService accounts;
    private UserDto user;
    private static final List<JsonNode> samples=new CopyOnWriteArrayList<>();
    @AfterAll static void saveContractSamples() throws Exception {
        new ObjectMapper().writeValue(new java.io.File("target/learning-contract-samples.json"),samples);
    }
    private static final String COURSE="afdac469-0fe4-5007-833c-51a71333967b", OTHER="ccd819bb-a48b-5c75-bfbf-e26219023fef", CYCLE="0ebdbbfe-d607-54b5-9c21-4e0adade5e4c";
    @BeforeEach void account() {user=newUser();}
    private UserDto newUser() {String name="learning"+UUID.randomUUID();return accounts.create(new RegisterRequest(name,name+"@example.com","12345678"),Role.USER);}
    private MockHttpServletRequestBuilder as(UserDto u,MockHttpServletRequestBuilder request) {
        return request.with(authentication(new UsernamePasswordAuthenticationToken(new CurrentUser(u.id(),u.username(),Role.USER,0),null,List.of())));
    }
    private JsonNode read(MockHttpServletRequestBuilder request) throws Exception {
        var result=mvc.perform(as(user,request)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andReturn();
        JsonNode envelope=mapper.readTree(result.getResponse().getContentAsString());
        var sample=mapper.createObjectNode();sample.put("path",result.getRequest().getRequestURI().substring("/api/v1".length()));
        sample.put("method",result.getRequest().getMethod().toLowerCase());sample.set("body",envelope);samples.add(sample);
        return envelope.path("data");
    }
    private JsonNode catalog() throws Exception {return read(get("/api/v1/catalog/courses/"+COURSE));}
    private List<String> items() throws Exception {
        List<String> ids=new ArrayList<>();catalog().path("chapters").forEach(c->c.path("items").forEach(i->ids.add(i.path("id").asText())));return ids;
    }
    private String completionUrl(String item) {return "/api/v1/catalog/courses/"+COURSE+"/items/"+item+"/completion";}
    private String command(boolean done,long revision) {return "{\"completed\":"+done+",\"expectedRevision\":"+revision+",\"clientMutationId\":\""+UUID.randomUUID()+"\"}";}
    @Test void importsRealContentAndReadRequestsDoNotCreateUserState() throws Exception {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM course",Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chapter",Integer.class)).isEqualTo(52);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM study_item",Integer.class)).isEqualTo(546);
        JsonNode catalog=catalog();assertThat(catalog.path("courseProgress").path("totalItems").asInt()).isEqualTo(87);
        assertThat(catalog.path("overallProgress").path("totalItems").asInt()).isEqualTo(546);
        read(get("/api/v1/courses").param("cycleId",CYCLE));
        read(get("/api/v1/courses/"+COURSE+"/enrollments/"+CYCLE));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_item_progress WHERE user_id=?",Integer.class,user.id())).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM enrollment WHERE user_id=?",Integer.class,user.id())).isZero();
        JsonNode cycle=read(get("/api/v1/exams/cycles/"+CYCLE));
        assertThat(cycle.path("courses").toString()).contains("2026-10-24","2026-10-25").doesNotContain("2026-10-17");
    }
    @Test void paginationFilteringAndCodeResolutionUseContract() throws Exception {
        read(get("/api/v1/courses/"+COURSE).param("cycleId",CYCLE));
        read(get("/api/v1/exams/cycles"));
        JsonNode page=read(get("/api/v1/courses").param("cycleId",CYCLE).param("courseType","PRACTICE").param("size","1"));
        assertThat(page.path("total").asInt()).isEqualTo(2);assertThat(page.path("items").size()).isEqualTo(1);
        assertThat(read(get("/api/v1/courses/by-code/00023").param("cycleId",CYCLE)).path("id").asText()).isEqualTo(COURSE);
        mvc.perform(as(user,get("/api/v1/courses").param("cycleId",CYCLE).param("size","101"))).andExpect(status().isBadRequest());
        mvc.perform(as(user,get("/api/v1/courses").param("cycleId","bad"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/courses").param("cycleId",CYCLE)).andExpect(status().isUnauthorized());
    }
    @Test void completionPersistsRejectsStaleVersionAndIsolatesUsers() throws Exception {
        String item=items().get(0);JsonNode result=read(put(completionUrl(item)).contentType("application/json").content(command(true,0)));
        assertThat(result.path("item").path("revision").asInt()).isEqualTo(1);
        assertThat(result.path("item").path("completedAt").asText()).endsWith("Z");
        assertThat(catalog().path("courseProgress").path("completedItems").asInt()).isEqualTo(1);
        mvc.perform(as(user,put(completionUrl(item)).contentType("application/json").content(command(false,0))))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(40901));
        UserDto another=newUser();mvc.perform(as(another,get("/api/v1/catalog/courses/"+COURSE))).andExpect(jsonPath("$.data.courseProgress.completedItems").value(0));
        read(put(completionUrl(item)).contentType("application/json").content(command(false,1)));
        assertThat(catalog().path("courseProgress").path("completedItems").asInt()).isZero();
        mvc.perform(as(user,put("/api/v1/catalog/courses/"+OTHER+"/items/"+item+"/completion").contentType("application/json").content(command(true,2)))).andExpect(status().isNotFound());
    }
    @Test void concurrentInitialCompletionOnlyAcceptsOneWriter() throws Exception {
        String item=items().get(0);ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try {
            Callable<Integer> call=()->{start.await();return mvc.perform(as(user,put(completionUrl(item)).contentType("application/json").content(command(true,0)))).andReturn().getResponse().getStatus();};
            Future<Integer> a=pool.submit(call),b=pool.submit(call);start.countDown();
            assertThat(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
            assertThat(jdbc.queryForObject("SELECT revision FROM user_item_progress WHERE user_id=? AND item_id=?",Long.class,user.id(),item)).isEqualTo(1);
        } finally {pool.shutdownNow();}
    }
    @Test void batchIsAtomicAndRejectsDuplicateItems() throws Exception {
        mvc.perform(as(user,patch("/api/v1/catalog/courses/"+COURSE+"/completions").contentType("application/json")
            .content("{\"updates\":[null],\"clientMutationId\":\""+UUID.randomUUID()+"\"}"))).andExpect(status().isBadRequest());
        List<String> ids=items();String a=ids.get(0),b=ids.get(1);
        read(put(completionUrl(b)).contentType("application/json").content(command(true,0)));
        String batch="{\"updates\":[{\"itemId\":\""+a+"\",\"completed\":true,\"expectedRevision\":0},{\"itemId\":\""+b+"\",\"completed\":false,\"expectedRevision\":0}],\"clientMutationId\":\""+UUID.randomUUID()+"\"}";
        mvc.perform(as(user,patch("/api/v1/catalog/courses/"+COURSE+"/completions").contentType("application/json").content(batch))).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_item_progress WHERE user_id=? AND item_id=?",Integer.class,user.id(),a)).isZero();
        mvc.perform(as(user,patch("/api/v1/catalog/courses/"+COURSE+"/completions").contentType("application/json").content(batch.replace(b,a)))).andExpect(status().isBadRequest());
        JsonNode success=read(patch("/api/v1/catalog/courses/"+COURSE+"/completions").contentType("application/json").content(batch.replace("\"completed\":false,\"expectedRevision\":0","\"completed\":false,\"expectedRevision\":1")));
        assertThat(success.path("items").size()).isEqualTo(2);assertThat(success.path("courseProgress").path("completedItems").asInt()).isEqualTo(1);
    }
    @Test void enrollmentSupportsUnknownResultsAndOptimisticLocking() throws Exception {
        String url="/api/v1/courses/"+COURSE+"/enrollments/"+CYCLE;
        String body="{\"paid\":true,\"fee\":65,\"officialScore\":null,\"officialPassed\":null,\"passedMonth\":null,\"note\":\"报名备注\",\"expectedRevision\":0}";
        mvc.perform(as(user,put(url).contentType("application/json").content(body.replace("\"fee\":65,","")))).andExpect(status().isBadRequest());
        JsonNode result=read(put(url).contentType("application/json").content(body));assertThat(result.path("revision").asInt()).isEqualTo(1);
        assertThat(result.path("officialScore").isNull()).isTrue();assertThat(read(get(url)).path("note").asText()).isEqualTo("报名备注");
        mvc.perform(as(user,put(url).contentType("application/json").content(body))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(40901));
        mvc.perform(as(newUser(),get(url))).andExpect(jsonPath("$.data.paid").value(false)).andExpect(jsonPath("$.data.revision").value(0));
        mvc.perform(as(user,put(url).contentType("application/json").content(body.replace("\"fee\":65","\"fee\":-1")))).andExpect(status().isBadRequest());
        mvc.perform(as(user,put(url).contentType("application/json").content(body.replace("\"paid\":true","\"userId\":\"other\",\"paid\":true")))).andExpect(status().isBadRequest());
    }
    @Test void navigationRejectsCrossCourseAndCrossChapterTargets() throws Exception {
        String url="/api/v1/courses/"+COURSE+"/learning-position";
        mvc.perform(as(user,get(url))).andExpect(status().isNotFound());
        JsonNode chapters=catalog().path("chapters");String chapter=chapters.get(0).path("id").asText(),item=chapters.get(0).path("items").get(0).path("id").asText();
        String body="{\"target\":{\"pane\":\"CATALOG\",\"courseCode\":\"00023\",\"chapterId\":\""+chapter+"\",\"itemId\":\""+item+"\",\"questionId\":null}}";
        mvc.perform(as(user,put(url).contentType("application/json").content(body.replace(",\"questionId\":null","")))).andExpect(status().isBadRequest());
        read(put(url).contentType("application/json").content(body));assertThat(read(get(url)).path("target").path("itemId").asText()).isEqualTo(item);
        mvc.perform(as(newUser(),get(url))).andExpect(status().isNotFound());
        mvc.perform(as(user,put(url).contentType("application/json").content(body.replace("00023","02324")))).andExpect(status().isUnprocessableEntity());
        mvc.perform(as(user,put(url).contentType("application/json").content(body.replace(chapter,chapters.get(1).path("id").asText())))).andExpect(status().isUnprocessableEntity());
    }
    @Test void completionSynchronizesOnlyOwnedPlanTasks() throws Exception {
        String item=items().get(0),release=catalog().path("releaseId").asText();UserDto another=newUser();String own=plan(user,item,release),foreign=plan(another,item,release);
        JsonNode result=read(put(completionUrl(item)).contentType("application/json").content(command(true,0)));
        assertThat(result.path("affectedPlanIds").toString()).contains(own).doesNotContain(foreign);
        assertThat(jdbc.queryForObject("SELECT completed FROM plan_task WHERE plan_id=?",Boolean.class,own)).isTrue();
        assertThat(jdbc.queryForObject("SELECT completed FROM plan_task WHERE plan_id=?",Boolean.class,foreign)).isFalse();
    }
    private String plan(UserDto owner,String item,String release) {
        String id=UUID.randomUUID().toString();jdbc.update("INSERT INTO learning_plan(id,user_id,cycle_id,start_date,end_date,config_snapshot,created_at) VALUES(?,?,?,'2026-10-01','2026-10-31','{}',UTC_TIMESTAMP(6))",id,owner.id(),CYCLE);
        jdbc.update("INSERT INTO plan_task(id,plan_id,course_id,item_id,title_snapshot,estimated_minutes,release_id,kind) VALUES(?,?,?,?,?,30,?,'ITEM')",UUID.randomUUID().toString(),id,COURSE,item,"学习任务",release);return id;
    }
}
