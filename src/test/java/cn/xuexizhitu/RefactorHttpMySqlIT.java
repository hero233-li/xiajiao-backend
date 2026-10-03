package cn.xuexizhitu;

import cn.xuexizhitu.identity.application.AccountService;
import cn.xuexizhitu.identity.api.RegisterRequest;
import cn.xuexizhitu.identity.domain.Role;
import cn.xuexizhitu.files.application.PrivateFileStore;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"app.assessments.timeout-enabled=false","app.files.cleanup-enabled=false"})
@Testcontainers
class RefactorHttpMySqlIT {
    @Container static MySQLContainer<?> mysql=new MySQLContainer<>("mysql:8.4").withDatabaseName("refactor_http_test");
    static final Path ROOT=temporaryRoot();
    static Path temporaryRoot(){try{return Path.of(System.getProperty("java.io.tmpdir")).toRealPath().resolve("refactor-http-"+UUID.randomUUID());}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
    static final String COURSE="afdac469-0fe4-5007-833c-51a71333967b", CYCLE="0ebdbbfe-d607-54b5-9c21-4e0adade5e4c";
    static final List<JsonNode> samples=new CopyOnWriteArrayList<>();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",mysql::getJdbcUrl);r.add("spring.datasource.username",mysql::getUsername);r.add("spring.datasource.password",mysql::getPassword);
        r.add("app.files.root",ROOT::toString);r.add("app.bootstrap.enabled",()->false);r.add("app.auth.registration-mode",()->"DISABLED");
    }
    @LocalServerPort int port;
    @Autowired AccountService accounts; @Autowired JdbcTemplate jdbc; @Autowired ObjectMapper mapper;
    @Autowired PrivateFileStore files; @Autowired PlatformTransactionManager transactions;
    final HttpClient client=HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
    record Reply(int status,JsonNode data,JsonNode envelope) {}
    Reply send(String method,String path,String token,Object body,String key)throws Exception {
        return send(method,path,token,body,key,null);
    }
    Reply send(String method,String path,String token,Object body,String key,Long revision)throws Exception {
        var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1"+path)).timeout(Duration.ofSeconds(30));
        if(token!=null)request.header("Authorization","Bearer "+token);
        if(revision!=null)request.header("If-Match",revision.toString());
        if(key!=null)request.header("Idempotency-Key",key);
        request.header("Content-Type","application/json");
        request.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        var response=client.send(request.build(),HttpResponse.BodyHandlers.ofString());
        assertThat(response.headers().firstValue("Cache-Control")).contains("no-store");
        JsonNode envelope=mapper.readTree(response.body());
        if(response.statusCode()<300){var sample=mapper.createObjectNode();sample.put("path",path.split("\\?")[0]);sample.put("method",method.toLowerCase());sample.put("status",response.statusCode());sample.set("body",envelope);samples.add(sample);}
        return new Reply(response.statusCode(),envelope.path("data"),envelope);
    }
    String login(String name)throws Exception {
        var result=send("POST","/auth/login",null,Map.of("identifier",name,"password","12345678"),null);
        assertThat(result.status()).isEqualTo(200);return result.data().path("accessToken").asText();
    }
    String account(Role role){String name="http"+UUID.randomUUID();accounts.create(new RegisterRequest(name,name+"@example.com","12345678"),role);return name;}
    @Test void actualHttpPreservesSnapshotsAndSerializesDuplicateAndConcurrentWrites()throws Exception {
        String name=account(Role.USER),token=login(name),other=login(account(Role.USER));
        String user=send("GET","/auth/me",token,null,null).data().path("id").asText();
        jdbc.update("INSERT INTO enrollment(user_id,cycle_id,course_id,paid) VALUES(?,?,?,true)",user,CYCLE,COURSE);
        jdbc.update("UPDATE cycle_course SET exam_date=? WHERE cycle_id=? AND course_id=?",LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(3),CYCLE,COURSE);
        String base="/exams/courses/"+COURSE;
        assertThat(send("POST",base+"/cycles/"+CYCLE+"/overrides",token,Map.of("confirm",true),null).status()).isEqualTo(201);
        String paper=UUID.randomUUID().toString();
        String file=files.upload(pdf(),"PAPER",null,false);
        jdbc.update("INSERT INTO paper(id,course_id,paper_month,question_file_id) VALUES(?,?,?,?)",paper,COURSE,"2001-03-01",file);
        Map<String,Object> body=new LinkedHashMap<>(Map.of("cycleId",CYCLE,"paperId",paper,"practicedOn",LocalDate.now(ZoneId.of("Asia/Shanghai")).toString(),"score",70,"minutes",120,"limitMinutes",150,"complete",true,"closedBook",true,"answersSeenBefore",false,"note","HTTP验证"));
        String key=UUID.randomUUID().toString();
        String item=jdbc.queryForObject("SELECT i.id FROM study_item i JOIN chapter c ON c.id=i.chapter_id WHERE c.course_id=? ORDER BY i.id LIMIT 1",String.class,COURSE);
        var completion=Map.of("completed",true,"expectedRevision",0,"clientMutationId",UUID.randomUUID().toString());
        String completionPath="/catalog/courses/"+COURSE+"/items/"+item+"/completion";
        var completed=send("PUT",completionPath,token,completion,null);assertThat(completed.status()).isEqualTo(200);
        assertThat(send("PUT",completionPath,token,completion,null).envelope()).isEqualTo(completed.envelope());
        var pool=Executors.newFixedThreadPool(2);
        try {
            var futures=pool.invokeAll(List.<Callable<Reply>>of(()->send("POST",base+"/scores",token,body,key),()->send("POST",base+"/scores",token,body,key)));
            Reply first=futures.get(0).get(),second=futures.get(1).get();
            assertThat(first.status()).isEqualTo(201);assertThat(second.envelope()).isEqualTo(first.envelope());
            String id=first.data().path("record").path("id").asText();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM score_record WHERE user_id=?",Integer.class,user)).isEqualTo(1);
            var changed=new LinkedHashMap<>(body);changed.put("score",71);
            assertThat(send("POST",base+"/scores",token,changed,key).status()).isEqualTo(409);
            changed.remove("cycleId");changed.remove("paperId");changed.put("expectedRevision",0);
            var edits=pool.invokeAll(List.<Callable<Reply>>of(()->send("PUT",base+"/scores/"+id,token,changed,null),()->send("PUT",base+"/scores/"+id,token,changed,null)));
            assertThat(List.of(edits.get(0).get().status(),edits.get(1).get().status())).containsExactlyInAnyOrder(200,409);
            var history=send("GET",base+"/scores/"+id+"/revisions",token,null,null);
            assertThat(history.status()).isEqualTo(200);assertThat(history.data().path("total").asInt()).isEqualTo(2);
            assertThat(history.data().path("items").get(1).path("record").path("score").asInt()).isEqualTo(70);
            assertThat(send("GET",base+"/scores/"+id+"/revisions",other,null,null).status()).isEqualTo(404);
            assertThat(send("GET","/admin/files",token,null,null).status()).isEqualTo(403);
            assertThat(send("GET","/auth/me",null,null,null).status()).isEqualTo(401);
            assertThat(send("GET","/health/readiness",null,null,null).data().path("status").asText()).isEqualTo("UP");
            assertThat(send("POST","/auth/logout",token,null,null).status()).isEqualTo(200);
            assertThat(send("GET","/auth/me",token,null,null).status()).isEqualTo(401);
        } finally {pool.shutdownNow();}
    }
    @Test void contentCasPreservesOriginalQuestionRevisions()throws Exception {
        String token=login(account(Role.ADMIN));String previous=jdbc.queryForObject("SELECT id FROM content_release WHERE course_id=? AND state='PUBLISHED'",String.class,COURSE);
        String revision=jdbc.queryForObject("SELECT r.id FROM question_revision r JOIN question q ON q.id=r.question_id WHERE q.course_id=? AND r.release_id=? ORDER BY r.id LIMIT 1",String.class,COURSE,previous);
        Map<String,Object> original=jdbc.queryForMap("SELECT * FROM question_revision WHERE id=?",revision);
        String base="/admin/courses/"+COURSE+"/releases";
        var command=mapper.createObjectNode().put("basedOnReleaseId",previous).putNull("sourceSha");
        var created=send("POST",base,token,command,null);assertThat(created.status()).isEqualTo(201);String id=created.data().path("id").asText();
        var catalog=send("GET",base+"/"+id+"/catalog",token,null,null).data();
        var pool=Executors.newFixedThreadPool(2);
        try {
            var results=pool.invokeAll(List.<Callable<Reply>>of(()->send("PUT",base+"/"+id+"/catalog",token,catalog,null,0L),()->send("PUT",base+"/"+id+"/catalog",token,catalog,null,0L)));
            assertThat(List.of(results.get(0).get().status(),results.get(1).get().status())).containsExactlyInAnyOrder(200,409);
            assertThat(send("POST",base+"/"+id+"/publication",token,Map.of("confirm",true),null,0L).status()).isEqualTo(409);
            assertThat(send("POST",base+"/"+id+"/publication",token,Map.of("confirm",true),null,1L).status()).isEqualTo(200);
            assertThat(jdbc.queryForMap("SELECT * FROM question_revision WHERE id=?",revision)).isEqualTo(original);
            assertThat(jdbc.queryForObject("SELECT state FROM content_release WHERE id=?",String.class,previous)).isEqualTo("RETIRED");
        } finally {pool.shutdownNow();}
    }
    @Test void realFrontendUsesBackendAndIndependentDatabase()throws Exception {
        String name=account(Role.USER),token=login(name);
        String user=send("GET","/auth/me",token,null,null).data().path("id").asText();
        jdbc.update("INSERT INTO enrollment(user_id,cycle_id,course_id,paid) VALUES(?,?,?,true)",user,CYCLE,COURSE);
        jdbc.update("UPDATE cycle_course SET exam_date=? WHERE cycle_id=? AND course_id=?",LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(3),CYCLE,COURSE);
        assertThat(send("POST","/exams/courses/"+COURSE+"/cycles/"+CYCLE+"/overrides",token,Map.of("confirm",true),null).status()).isEqualTo(201);
        String paper=UUID.randomUUID().toString(),file=files.upload(pdf(),"PAPER",null,false);
        jdbc.update("INSERT INTO paper(id,course_id,paper_month,question_file_id) VALUES(?,?,?,?)",paper,COURSE,"2002-03-01",file);
        int uiPort;try(var socket=new java.net.ServerSocket(0)){uiPort=socket.getLocalPort();}
        Path frontend=Path.of("../frontend").toAbsolutePath().normalize(),evidence=Path.of("target/refactor-evidence").toAbsolutePath();Files.createDirectories(evidence);
        ProcessBuilder vite=new ProcessBuilder("node","node_modules/vite/bin/vite.js","--host","127.0.0.1","--port",String.valueOf(uiPort),"--strictPort").directory(frontend.toFile()).redirectErrorStream(true).redirectOutput(evidence.resolve("vite.log").toFile());
        vite.environment().put("API_PROXY_TARGET","http://127.0.0.1:"+port);vite.environment().put("VITE_API_MOCK","false");vite.environment().put("VITE_API_ORIGIN","");
        Path image=evidence.resolve("answer.png");javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",image.toFile());
        Process server=vite.start();
        try {
            boolean ready=false;for(int i=0;i<100;i++){try{if(client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+uiPort+"/login")).header("Accept","text/html").timeout(Duration.ofSeconds(1)).method("HEAD",HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.discarding()).statusCode()==200){ready=true;break;}}catch(Exception ignored){}Thread.sleep(100);}
            assertThat(ready).as("frontend server ready").isTrue();
            var browser=new ProcessBuilder("node","scripts/refactor-browser-check.mjs").directory(frontend.toFile()).redirectErrorStream(true).redirectOutput(evidence.resolve("browser.log").toFile());
            browser.environment().put("REFACTOR_UI_URL","http://127.0.0.1:"+uiPort);browser.environment().put("REFACTOR_USERNAME",name);browser.environment().put("REFACTOR_PASSWORD","12345678");browser.environment().put("REFACTOR_CYCLE",CYCLE);browser.environment().put("REFACTOR_PAPER",paper);browser.environment().put("REFACTOR_IMAGE",image.toString());browser.environment().put("REFACTOR_EVIDENCE",evidence.toString());
            Process run=browser.start();boolean completed=run.waitFor(90,TimeUnit.SECONDS);if(!completed)run.destroyForcibly();assertThat(completed).as("browser timeout").isTrue();
            assertThat(run.exitValue()).withFailMessage(Files.readString(evidence.resolve("browser.log"))).isZero();
            assertThat(jdbc.queryForObject("SELECT score FROM score_record WHERE user_id=?",Integer.class,user)).isEqualTo(82);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM score_record_revision WHERE user_id=?",Integer.class,user)).isEqualTo(2);
            var attached=jdbc.queryForMap("SELECT i.score_record_id,i.file_id FROM score_record_image i WHERE i.user_id=?",user);
            String route="/exams/courses/"+COURSE+"/scores/"+attached.get("score_record_id")+"/images/"+attached.get("file_id");
            assertThat(send("GET",route,token,null,null).data().path("contentBase64").asText()).isEqualTo(Base64.getEncoder().encodeToString(Files.readAllBytes(image)));
            assertThat(send("DELETE",route,token,null,null).status()).isEqualTo(200);files.cleanup();
            assertThat(jdbc.queryForObject("SELECT state FROM stored_file WHERE id=?",String.class,attached.get("file_id"))).isEqualTo("DELETED");
        } finally {server.destroy();if(!server.waitFor(5,TimeUnit.SECONDS))server.destroyForcibly();}
    }
    MockMultipartFile pdf(){return new MockMultipartFile("file","test.pdf","application/pdf","%PDF-1.7\n%%EOF\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    @Test void filesCompensateRollbackAndRecoverMissingDeletionAndCrashOrphans()throws Exception {
        long before=jdbc.queryForObject("SELECT COUNT(*) FROM stored_file",Long.class);
        String[] failed=new String[1];
        assertThatThrownBy(()->new TransactionTemplate(transactions).executeWithoutResult(s->{failed[0]=files.upload(pdf(),"PAPER",null,false);throw new IllegalStateException("simulated business failure");})).isInstanceOf(IllegalStateException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stored_file",Long.class)).isEqualTo(before);
        assertThat(ROOT.resolve("managed/"+failed[0])).doesNotExist();assertThat(ROOT.resolve("pending/"+failed[0])).doesNotExist();
        String missing=files.upload(pdf(),"PAPER",null,false);Files.delete(ROOT.resolve("managed/"+missing));files.cleanup();
        assertThat(jdbc.queryForObject("SELECT category FROM file_reconciliation WHERE file_id=?",String.class,missing)).isEqualTo("ACTIVE_FILE_MISSING");
        assertThatThrownBy(()->files.download(missing)).isInstanceOf(cn.xuexizhitu.common.BusinessException.class);
        String retry=files.upload(pdf(),"PAPER",null,false);Path path=ROOT.resolve("managed/"+retry);Files.delete(path);Files.createDirectory(path);Files.writeString(path.resolve("obstruction"),"retry");files.delete(retry);files.cleanup();
        assertThat(jdbc.queryForObject("SELECT state FROM stored_file WHERE id=?",String.class,retry)).isEqualTo("DELETE_PENDING");
        assertThat(jdbc.queryForObject("SELECT last_error FROM file_cleanup_task WHERE file_id=?",String.class,retry)).isEqualTo("DirectoryNotEmptyException");
        Files.delete(path.resolve("obstruction"));Files.delete(path);jdbc.update("UPDATE file_cleanup_task SET next_attempt_at=UTC_TIMESTAMP(6) WHERE file_id=?",retry);files.cleanup();
        assertThat(jdbc.queryForObject("SELECT state FROM stored_file WHERE id=?",String.class,retry)).isEqualTo("DELETED");
        String orphan=UUID.randomUUID().toString();Files.writeString(ROOT.resolve("managed/"+orphan),"preserved orphan bytes");Path journal=ROOT.resolve("pending/"+orphan);Files.writeString(journal,"crash");Files.setLastModifiedTime(journal,FileTime.from(Instant.now().minus(Duration.ofHours(25))));files.cleanup();
        assertThat(Files.readString(ROOT.resolve("quarantine/"+orphan))).isEqualTo("preserved orphan bytes");
        assertThat(jdbc.queryForObject("SELECT category FROM file_reconciliation WHERE file_id=?",String.class,orphan)).isEqualTo("CRASH_ORPHAN_QUARANTINED");
    }
    @AfterAll static void evidence()throws Exception {
        Path output=Path.of("target/refactor-evidence");Files.createDirectories(output);new ObjectMapper().writeValue(output.resolve("http-contract-samples.json").toFile(),samples);
        if(Files.exists(ROOT))try(var paths=Files.walk(ROOT)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}
    }
}
