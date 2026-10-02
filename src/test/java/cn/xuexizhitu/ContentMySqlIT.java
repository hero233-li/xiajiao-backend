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
class ContentMySqlIT {
    @Container static MySQLContainer<?> mysql=new MySQLContainer<>("mysql:8.4").withDatabaseName("content_test");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",mysql::getJdbcUrl);r.add("spring.datasource.username",mysql::getUsername);r.add("spring.datasource.password",mysql::getPassword);
        r.add("app.bootstrap.enabled",()->false);r.add("app.auth.registration-mode",()->"DISABLED");
        r.add("app.jwt.secret-base64",()->"MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
    }
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired JdbcTemplate jdbc; @Autowired AccountService accounts;
    private UserDto user;
    private static final List<JsonNode> samples=new CopyOnWriteArrayList<>();
    @AfterAll static void saveContractSamples() throws Exception {
        new ObjectMapper().writeValue(new java.io.File("target/content-contract-samples.json"),samples);
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
    private static final String JAVA="e2dd5f58-a12f-55e0-a691-455ea2233699", DATABASE="72494a28-2cb8-5df8-8f3f-2ba84b681e0d";
    private String knowledgeUrl(String course){return "/api/v1/catalog/courses/"+course+"/knowledge";}
    private String manualUrl(String course){return "/api/v1/catalog/courses/"+course+"/manual";}
    private String resourceUrl(String course,String file){return "/api/v1/catalog/courses/"+course+"/resources/"+file;}
    private String noteBody(String course,String text){return "{\"courseId\":\""+course+"\",\"noteDate\":\"2026-10-02\",\"content\":\""+text+"\"}";}
    @Test void importsKnowledgeAndReadsDoNotCreatePrivateNotes() throws Exception {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_module",Integer.class)).isEqualTo(38);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM knowledge_example e JOIN content_release r ON r.id=e.release_id WHERE r.state='PUBLISHED'",Integer.class)).isEqualTo(204);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM content_release WHERE state='PUBLISHED'",Integer.class)).isEqualTo(6);
        JsonNode page=read(get(knowledgeUrl(COURSE)).param("size","1"));assertThat(page.path("total").asInt()).isEqualTo(15);
        String module=page.path("items").get(0).path("id").asText();JsonNode detail=read(get(knowledgeUrl(COURSE)+"/"+module));
        assertThat(detail.path("examples").size()).isGreaterThan(0);
        for(JsonNode example:detail.path("examples")){assertThat(example.has("answer")).isFalse();assertThat(example.has("solution")).isFalse();}
        assertThat(detail.path("userNote").path("revision").asInt()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_knowledge_note WHERE user_id=?",Integer.class,user.id())).isZero();
        JsonNode filter=read(get(knowledgeUrl(COURSE)).param("q",detail.path("title").asText()).param("difficulty",detail.path("difficulty").asText()));assertThat(filter.path("total").asInt()).isGreaterThan(0);
        assertThat(read(get(knowledgeUrl(COURSE)).param("q","%_")).path("total").asInt()).isZero();
        mvc.perform(as(user,get(knowledgeUrl(COURSE)).param("difficulty","6"))).andExpect(status().isBadRequest());
        mvc.perform(as(user,get(knowledgeUrl(OTHER)+"/"+module))).andExpect(status().isNotFound());
    }
    @Test void knowledgeNotesPersistWithVersionAndOwnership() throws Exception {
        String module=read(get(knowledgeUrl(COURSE))).path("items").get(0).path("id").asText(),url=knowledgeUrl(COURSE)+"/"+module+"/note";
        String body="{\"mastery\":3,\"note\":\"理解了条件\",\"expectedRevision\":0}";
        assertThat(read(put(url).contentType("application/json").content(body)).path("userNote").path("revision").asInt()).isEqualTo(1);
        assertThat(read(get(knowledgeUrl(COURSE)+"/"+module)).path("userNote").path("note").asText()).isEqualTo("理解了条件");
        mvc.perform(as(newUser(),get(knowledgeUrl(COURSE)+"/"+module))).andExpect(jsonPath("$.data.userNote.mastery").value(0));
        mvc.perform(as(user,put(url).contentType("application/json").content(body))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(40901));
        mvc.perform(as(user,put(url).contentType("application/json").content(body.replace("\"mastery\":3","\"mastery\":5")))).andExpect(status().isBadRequest());
        mvc.perform(as(user,put(url).contentType("application/json").content(body.replace("\"mastery\":3","\"userId\":\"foreign\",\"mastery\":3")))).andExpect(status().isBadRequest());
    }
    @Test void solutionsRequireLoginAndCurrentCourseAssociation() throws Exception {
        JsonNode module=read(get(knowledgeUrl(COURSE))).path("items").get(0);String example=module.path("examples").get(0).path("id").asText();
        String url="/api/v1/catalog/courses/"+COURSE+"/examples/"+example+"/solution";
        JsonNode solution=read(get(url));assertThat(solution.path("answer").asText()).isNotEmpty();
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(as(user,get(url.replace(COURSE,OTHER)))).andExpect(status().isNotFound());
    }
    @Test void manualsReuseCatalogItemsAndSeparateReferenceAnswers() throws Exception {
        int total=0;
        for(String course:List.of(JAVA,DATABASE)) {
            JsonNode manual=read(get(manualUrl(course)));assertThat(manual.path("sections").size()).isGreaterThan(0);
            StringBuilder text=new StringBuilder();manual.path("sections").forEach(section->text.append(section.path("markdown").asText()));String publicText=text.toString();assertThat(publicText).doesNotContain("展开参考答案","展开模拟题参考答案","<details>","验收示例");
            String target=null;Set<String> revealed=new HashSet<>();
            for(JsonNode section:manual.path("sections"))for(JsonNode exercise:section.path("exercises")) {total++;target=exercise.path("item").path("id").asText();
                if(!exercise.path("exampleId").isNull()) {
                    String example=exercise.path("exampleId").asText();if(!revealed.add(example))continue;JsonNode solution=read(get("/api/v1/catalog/courses/"+course+"/examples/"+example+"/solution"));
                    assertThat(publicText).doesNotContain(solution.path("answer").asText());
                }
            }
            read(put("/api/v1/catalog/courses/"+course+"/items/"+target+"/completion").contentType("application/json").content(command(true,0)));
            JsonNode refreshed=read(get(manualUrl(course)));boolean found=false;
            for(JsonNode section:refreshed.path("sections"))for(JsonNode exercise:section.path("exercises"))if(exercise.path("item").path("id").asText().equals(target)){found=true;assertThat(exercise.path("item").path("completed").asBoolean()).isTrue();}
            assertThat(found).isTrue();
        }
        assertThat(total).isEqualTo(207);mvc.perform(as(user,get(manualUrl(COURSE)))).andExpect(status().isNotFound());
    }
    @Test void manualDownloadsCheckReleasePurposeStateAndIntegrity() throws Exception {
        JsonNode page=read(get(knowledgeUrl(JAVA)));String file=null;
        for(JsonNode module:page.path("items"))for(JsonNode resource:module.path("resources"))if(resource.path("kind").asText().equals("FILE"))file=resource.path("fileId").asText();
        assertThat(file).isNotNull();String url=resourceUrl(JAVA,file);JsonNode download=read(get(url));
        byte[] bytes=Base64.getDecoder().decode(download.path("contentBase64").asText());assertThat(bytes.length).isEqualTo(download.path("file").path("sizeBytes").asInt());
        assertThat(download.path("file").has("storageKey")).isFalse();assertThat(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).doesNotContain("<details>","验收示例");
        mvc.perform(get(url)).andExpect(status().isUnauthorized());mvc.perform(as(user,get(resourceUrl(DATABASE,file)))).andExpect(status().isNotFound());
        String sha=jdbc.queryForObject("SELECT sha256 FROM stored_file WHERE id=?",String.class,file);
        try {
            jdbc.update("UPDATE stored_file SET contains_answers=true WHERE id=?",file);mvc.perform(as(user,get(url))).andExpect(status().isNotFound());
            jdbc.update("UPDATE stored_file SET contains_answers=false,state='DELETE_PENDING' WHERE id=?",file);mvc.perform(as(user,get(url))).andExpect(status().isNotFound());
            jdbc.update("UPDATE stored_file SET state='ACTIVE',purpose='PAPER' WHERE id=?",file);mvc.perform(as(user,get(url))).andExpect(status().isNotFound());
            jdbc.update("UPDATE stored_file SET purpose='MANUAL',sha256=? WHERE id=?","0".repeat(64),file);mvc.perform(as(user,get(url))).andExpect(status().isUnprocessableEntity());
        } finally {jdbc.update("UPDATE stored_file SET state='ACTIVE',purpose='MANUAL',contains_answers=false,sha256=? WHERE id=?",sha,file);}
        mvc.perform(as(user,get("/manuals/java-v2.json"))).andExpect(status().isNotFound());
    }
    @Test void notesSupportExactDerivedTagsSearchAndPagination() throws Exception {
        JsonNode a=read(post("/api/v1/notes").contentType("application/json").content(noteBody(COURSE,"复习极限 #高数 #高数 #数学")));
        read(post("/api/v1/notes").contentType("application/json").content(noteBody(OTHER,"逻辑 #高数进阶")));
        read(post("/api/v1/notes").contentType("application/json").content(noteBody(COURSE,"公式 %_ #高数")));
        assertThat(a.path("tags").size()).isEqualTo(2);assertThat(a.path("revision").asInt()).isZero();assertThat(a.path("createdAt").asText()).endsWith("Z");
        read(get("/api/v1/notes/"+a.path("id").asText()));
        JsonNode filtered=read(get("/api/v1/notes").param("tag","高数").param("size","1"));assertThat(filtered.path("total").asInt()).isEqualTo(2);assertThat(filtered.path("items").size()).isEqualTo(1);
        assertThat(read(get("/api/v1/notes").param("courseId",COURSE).param("q","%_")).path("total").asInt()).isEqualTo(1);
        assertThat(read(get("/api/v1/notes").param("from","2026-10-03")).path("total").asInt()).isZero();
        JsonNode tags=read(get("/api/v1/notes/tags").param("courseId",COURSE));assertThat(tags.toString()).contains("高数");
        for(JsonNode tag:tags.path("tags"))if(tag.path("name").asText().equals("高数"))assertThat(tag.path("count").asInt()).isEqualTo(2);
        mvc.perform(as(newUser(),get("/api/v1/notes/tags"))).andExpect(jsonPath("$.data.tags").isEmpty());
        mvc.perform(as(user,get("/api/v1/notes").param("from","2026-10-03").param("to","2026-10-02"))).andExpect(status().isBadRequest());
    }
    @Test void notesRejectForeignIdsStaleVersionsAndInvalidInput() throws Exception {
        JsonNode note=read(post("/api/v1/notes").contentType("application/json").content(noteBody(COURSE,"原备注 #旧标签")));String url="/api/v1/notes/"+note.path("id").asText();
        String body="{\"noteDate\":\"2026-10-01\",\"content\":\"修改 #新标签\",\"expectedRevision\":0}";
        UserDto other=newUser();mvc.perform(as(other,get(url))).andExpect(status().isNotFound());mvc.perform(as(other,put(url).contentType("application/json").content(body))).andExpect(status().isNotFound());
        JsonNode changed=read(put(url).contentType("application/json").content(body));assertThat(changed.path("revision").asInt()).isEqualTo(1);
        assertThat(read(get("/api/v1/notes/tags")).toString()).doesNotContain("旧标签").contains("新标签");
        mvc.perform(as(user,put(url).contentType("application/json").content(body))).andExpect(status().isConflict());
        mvc.perform(as(user,post("/api/v1/notes").contentType("application/json").content(noteBody(COURSE,"valid").replace("2026-10-02","0001-01-01")))).andExpect(status().isBadRequest());
        mvc.perform(as(user,post("/api/v1/notes").contentType("application/json").content(noteBody(COURSE,"   ")))).andExpect(status().isBadRequest());
        mvc.perform(as(user,post("/api/v1/notes").contentType("application/json").content(noteBody(COURSE,"a".repeat(5001))))).andExpect(status().isBadRequest());
        mvc.perform(as(user,post("/api/v1/notes").contentType("application/json").content(noteBody(COURSE,"valid").replace("\"content\":","\"tags\":[\"fake\"],\"content\":")))).andExpect(status().isBadRequest());
    }
    @Test void realJwtCanAccessContentAndCapabilitiesMatchImportedModules() throws Exception {
        JsonNode login=mapper.readTree(mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(mapper.writeValueAsString(new LoginRequest(user.username(),"12345678")))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
        mvc.perform(get(knowledgeUrl(COURSE)).header("Authorization","Bearer "+login.path("accessToken").asText())).andExpect(status().isOk());
        JsonNode course=read(get("/api/v1/courses/"+JAVA).param("cycleId",CYCLE));assertThat(course.path("capabilities").path("knowledge").asBoolean()).isTrue();assertThat(course.path("capabilities").path("manual").asBoolean()).isTrue();
        assertThat(catalog().path("overallProgress").path("totalItems").asInt()).isEqualTo(546);
    }
}
