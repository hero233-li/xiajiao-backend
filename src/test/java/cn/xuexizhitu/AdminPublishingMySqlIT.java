package cn.xuexizhitu;

import cn.xuexizhitu.identity.application.AccountService;
import cn.xuexizhitu.identity.api.RegisterRequest;
import cn.xuexizhitu.identity.domain.Role;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.*;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

/** Real HTTP and isolated MySQL regression for administrator publishing and plan refresh. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"app.assessments.timeout-enabled=false","app.files.cleanup-enabled=false"})
@Testcontainers
class AdminPublishingMySqlIT {
    @Container static MySQLContainer<?> mysql=new MySQLContainer<>("mysql:8.4").withDatabaseName("admin_publishing_test");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",mysql::getJdbcUrl);r.add("spring.datasource.username",mysql::getUsername);r.add("spring.datasource.password",mysql::getPassword);
        r.add("app.files.root",()->"/private/tmp/admin-publishing-"+mysql.getContainerId());
        r.add("app.bootstrap.enabled",()->false);r.add("app.auth.registration-mode",()->"DISABLED");
    }
    @LocalServerPort int port;
    @Autowired AccountService accounts; @Autowired ObjectMapper mapper;
    @Autowired cn.xuexizhitu.files.application.PrivateFileStore files;
    final HttpClient client=HttpClient.newHttpClient();
    record Reply(int status,JsonNode data) {}
    Reply send(String method,String path,String token,Object body,Long revision)throws Exception {
        var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1"+path)).timeout(Duration.ofSeconds(30)).header("Content-Type","application/json");
        if(token!=null)req.header("Authorization","Bearer "+token);
        if(revision!=null)req.header("If-Match",revision.toString());
        req.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        var response=client.send(req.build(),HttpResponse.BodyHandlers.ofString());
        return new Reply(response.statusCode(),mapper.readTree(response.body()).path("data"));
    }
    JsonNode ok(String method,String path,String token,Object body,Long revision)throws Exception {
        var reply=send(method,path,token,body,revision);assertThat(reply.status()).as(method+" "+path).isBetween(200,299);return reply.data();
    }
    String login(Role role)throws Exception {
        String name="adminit"+UUID.randomUUID();accounts.create(new RegisterRequest(name,name+"@example.com","12345678"),role);
        return ok("POST","/auth/login",null,Map.of("identifier",name,"password","12345678"),null).path("accessToken").asText();
    }
    @Test void administratorCanMaintainRubricsBeforeStudentUnlock() throws Exception {
        String admin=login(Role.ADMIN),user=login(Role.USER),course="afdac469-0fe4-5007-833c-51a71333967b",cycle="0ebdbbfe-d607-54b5-9c21-4e0adade5e4c";
        var file=new org.springframework.mock.web.MockMultipartFile("file","admin-test.pdf","application/pdf","%PDF-1.7\n%%EOF\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String fileId=files.upload(file,"PAPER",null,false);
        var paperBody=mapper.createObjectNode().put("paperMonth","1990-01").put("questionFileId",fileId).put("questionPages",1).put("note","isolated administrator test").putNull("sourceCourseCode").putNull("answerFileId").putNull("answerPages");
        String paper=ok("POST","/admin/exams/courses/"+course+"/papers",admin,paperBody,null).path("id").asText();
        String path="/grading/rubrics?courseId="+course+"&cycleId="+cycle+"&paperId="+paper;
        assertThat(ok("GET",path,admin,null,null)).isEmpty();
        assertThat(send("GET",path,user,null,null).status()).isEqualTo(403);
        var body=Map.of("questions",List.of(Map.of("number","1","stem","7+5=?","referenceAnswer","12","maximum",100,"points",List.of(Map.of("id","answer","description","正确答案12","maximum",100)))));
        String id=ok("POST","/grading/rubrics?courseId="+course+"&paperId="+paper,admin,body,null).path("id").asText();
        assertThat(ok("GET",path,admin,null,null)).hasSize(1);
        ok("PUT","/grading/rubrics/"+id,admin,body,null);
        ok("POST","/grading/rubrics/"+id+"/publish",admin,null,null);
        assertThat(send("PUT","/grading/rubrics/"+id,admin,body,null).status()).isEqualTo(409);
        assertThat(send("POST","/grading/rubrics?courseId="+course+"&paperId="+paper,user,body,null).status()).isEqualTo(403);
        assertThat(send("GET",path,user,null,null).status()).isEqualTo(403);
    }
    @Test void fullTemplateReplacementPreservesContentAndPublishesLatestIntoPreviewOnly()throws Exception {
        String admin=login(Role.ADMIN),user=login(Role.USER);
        String cycle=ok("GET","/exams/cycles",admin,null,null).path("items").get(0).path("id").asText();
        var courses=ok("GET","/courses?cycleId="+cycle,admin,null,null).path("items");
        List<String> scope=new ArrayList<>();String target=null,previous=null;
        for(JsonNode c:courses) {
            if(!c.path("courseType").asText().equals("THEORY"))continue;
            String course=c.path("id").asText();scope.add(course);
            String base="/admin/courses/"+course+"/releases";
            var draft=ok("POST",base,admin,mapper.createObjectNode().put("basedOnReleaseId",c.path("releaseId").asText()).putNull("sourceSha"),null);
            String id=draft.path("id").asText(),b=base+"/"+id;
            var body=mapper.createObjectNode();var list=body.putArray("templates");
            list.addObject().put("id",UUID.randomUUID().toString()).put("kind","PAPER").put("title","保留真题").put("estimatedMinutes",75).put("sortOrder",0).putNull("resource");
            list.addObject().put("id",UUID.randomUUID().toString()).put("kind","REVIEW").put("title","原有复习").put("estimatedMinutes",40).put("sortOrder",1).putNull("resource");
            ok("PUT",b+"/task-templates",admin,body,0L);
            assertThat(ok("GET",b+"/validation",admin,null,null).path("valid").asBoolean()).isTrue();
            ok("POST",b+"/publication",admin,Map.of("confirm",true),1L);
            if(target==null){target=course;previous=id;}
        }
        assertThat(scope).hasSize(4);
        LocalDate start=LocalDate.now(ZoneId.of("Asia/Shanghai"));
        var capacities=new ArrayList<Map<String,Object>>();for(int i=0;i<35;i++)capacities.add(Map.of("day",start.plusDays(i).toString(),"capacityMinutes",480));
        Map<String,Object> config=Map.of("name","发布前计划","strategy","WEEKLY_35","cycleId",cycle,"startDate",start.toString(),"endDate",start.plusDays(34).toString(),"coursePriority",scope,"courseScope",scope,"dayCapacities",capacities);
        var originalPlan=ok("POST","/schedule/plans",admin,Map.of("config",config,"acceptUnscheduled",true),null);
        assertThat(send("GET","/admin/courses",user,null,null).status()).isEqualTo(403);
        assertThat(ok("GET","/admin/courses?cycleId="+cycle,admin,null,null).path("items")).hasSize(6);
        var chosen=courses.get(0);
        var disabled=mapper.createObjectNode().put("code",chosen.path("code").asText()).put("name",chosen.path("name").asText()).put("courseType",chosen.path("courseType").asText()).put("active",false);
        ok("PUT","/admin/courses/"+chosen.path("id").asText()+"?cycleId="+cycle,admin,disabled,null);
        var directory=ok("GET","/admin/courses?cycleId="+cycle,admin,null,null).path("items");
        assertThat(directory).hasSize(6);assertThat(directory.get(0).path("active").asBoolean()).isFalse();
        disabled.put("active",true);
        ok("PUT","/admin/courses/"+chosen.path("id").asText()+"?cycleId="+cycle,admin,disabled,null);
        String base="/admin/courses/"+target+"/releases";
        var draft=ok("POST",base,admin,mapper.createObjectNode().put("basedOnReleaseId",previous).putNull("sourceSha"),null);String b=base+"/"+draft.path("id").asText();
        // A large seeded draft must remain listable without sorting its multi-megabyte JSON payload.
        assertThat(ok("GET",base,admin,null,null).path("items").size()).isGreaterThan(2);
        var catalog=ok("GET",b+"/catalog",admin,null,null);var questions=ok("GET",b+"/questions",admin,null,null);
        var body=(ObjectNode)ok("GET",b+"/task-templates",admin,null,null);var kept=body.path("templates").deepCopy();
        JsonNode resource=catalog.path("chapters").get(0).path("items").get(0).path("resource");
        var added=mapper.createObjectNode().put("id",UUID.randomUUID().toString()).put("kind","REVIEW").put("title","新增人工估时复习").put("estimatedMinutes",85).put("sortOrder",0);added.set("resource",resource);
        ArrayNode list=(ArrayNode)body.path("templates");list.insert(0,added);for(int i=1;i<list.size();i++)((ObjectNode)list.get(i)).put("sortOrder",i);
        assertThat(send("PUT",b+"/task-templates",user,body,0L).status()).isEqualTo(403);
        assertThat(send("GET",base,user,null,null).status()).isEqualTo(403);
        ok("PUT",b+"/task-templates",admin,body,0L);
        var reread=ok("GET",b+"/task-templates",admin,null,null);assertThat(reread).isEqualTo(body);
        for(int i=0;i<kept.size();i++)assertThat(reread.path("templates").get(i+1).path("id")).isEqualTo(kept.get(i).path("id"));
        assertThat(ok("GET",b+"/catalog",admin,null,null)).isEqualTo(catalog);
        assertThat(ok("GET",b+"/questions",admin,null,null)).isEqualTo(questions);
        assertThat(send("PUT",b+"/task-templates",admin,body,0L).status()).isEqualTo(409);
        assertThat(send("POST",b+"/publication",admin,Map.of("confirm",true),0L).status()).isEqualTo(409);
        assertThat(ok("GET",b+"/validation",admin,null,null).path("valid").asBoolean()).isTrue();
        ok("POST",b+"/publication",admin,Map.of("confirm",true),1L);
        assertThat(send("PUT",b+"/task-templates",admin,body,2L).status()).isEqualTo(409);
        String plan="/schedule/plans/"+originalPlan.path("id").asText();
        var preview=ok("POST",plan+"/reschedule-previews",admin,Map.of("baseRevision",1,"config",config),null);
        assertThat(preview.path("proposedPlan").path("tasks").toString()).contains("新增人工估时复习",draft.path("id").asText());
        var stillOriginal=ok("GET",plan,admin,null,null);
        assertThat(stillOriginal.path("revision").asInt()).isEqualTo(1);
        assertThat(stillOriginal.path("tasks")).isEqualTo(originalPlan.path("tasks"));
        assertThat(ok("GET",base+"/"+previous+"/task-templates",admin,null,null).path("templates")).hasSize(kept.size());
    }
}
