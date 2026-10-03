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
class GradingHttpMySqlIT {
    @Container static MySQLContainer<?> mysql=new MySQLContainer<>("mysql:8.4").withDatabaseName("grading_http_test");
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
    static final java.util.concurrent.atomic.AtomicInteger month=new java.util.concurrent.atomic.AtomicInteger();
    record Fixture(String name,String token,String user,String paper,String submission,String rubric,String pair) {}
    Fixture fixture(boolean publish)throws Exception {
        String name=account(Role.ADMIN),token=login(name),user=send("GET","/auth/me",token,null,null).data().path("id").asText();
        jdbc.update("INSERT INTO enrollment(user_id,cycle_id,course_id,paid) VALUES(?,?,?,true)",user,CYCLE,COURSE);
        jdbc.update("UPDATE cycle_course SET exam_date=? WHERE cycle_id=? AND course_id=?",LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(3),CYCLE,COURSE);
        assertThat(send("POST","/exams/courses/"+COURSE+"/cycles/"+CYCLE+"/overrides",token,Map.of("confirm",true),null).status()).isEqualTo(201);
        String paper=UUID.randomUUID().toString(),pdf=files.upload(new MockMultipartFile("file","test.pdf","application/pdf","%PDF-1.7\n%%EOF\n".getBytes()),"PAPER",null,false);
        jdbc.update("INSERT INTO paper(id,course_id,paper_month,question_file_id,answer_file_id) VALUES(?,?,?, ?,?)",paper,COURSE,"1901-"+String.format("%02d",month.incrementAndGet())+"-01",pdf,pdf);
        var rubric=send("POST","/grading/rubrics?courseId="+COURSE+"&paperId="+paper,token,Map.of("questions",List.of(Map.of("number","1","stem","7 + 5 = ?","referenceAnswer","12","maximum",100,"points",List.of(Map.of("id","answer","description","正确答案12得100分，其他明确答案零分，无法辨认进入待核对","maximum",100))))),null);
        assertThat(rubric.status()).isEqualTo(200);String rubricId=rubric.data().path("id").asText();
        if(publish)assertThat(send("POST","/grading/rubrics/"+rubricId+"/publish",token,null,null).status()).isEqualTo(200);
        var submission=send("POST","/grading/submissions",token,Map.of("courseId",COURSE,"cycleId",CYCLE,"paperId",paper),null); assertThat(submission.status()).isEqualTo(200);
        String id=submission.data().path("id").asText(); upload(id,token,0,image());
        var pair=send("POST","/grading/workers",token,Map.of("label","Test Mac"),null); assertThat(pair.status()).isEqualTo(200);
        return new Fixture(name,token,user,paper,id,rubricId,pair.data().path("token").asText());
    }
    byte[] image()throws Exception {
        var image=new java.awt.image.BufferedImage(700,300,java.awt.image.BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(java.awt.Color.WHITE);g.fillRect(0,0,700,300);g.setColor(java.awt.Color.BLACK);g.setFont(new java.awt.Font("SansSerif",java.awt.Font.PLAIN,45));g.drawString("Question 1: 7 + 5 = ?",30,100);g.drawString("My answer: 12",30,190);g.dispose();var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(image,"png",bytes);return bytes.toByteArray();
    }
    Reply upload(String id,String token,long revision,byte[] bytes)throws Exception {
        String boundary="xzt"+UUID.randomUUID();var body=new java.io.ByteArrayOutputStream();body.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"answer.png\"\r\nContent-Type: image/png\r\n\r\n").getBytes());body.write(bytes);body.write(("\r\n--"+boundary+"--\r\n").getBytes());
        var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/grading/submissions/"+id+"/pages?expectedRevision="+revision)).header("Authorization","Bearer "+token).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
        var res=client.send(req,HttpResponse.BodyHandlers.ofString());JsonNode envelope=mapper.readTree(res.body()); if(res.statusCode()<300)recordSample("post","/grading/submissions/"+id+"/pages",res.statusCode(),envelope); return new Reply(res.statusCode(),envelope.path("data"),envelope);
    }
    Map<String,Object> practice(long revision) { return Map.of("practicedOn",LocalDate.now(ZoneId.of("Asia/Shanghai")).toString(),"minutes",120,"limitMinutes",150,"complete",true,"closedBook",true,"answersSeenBefore",false,"expectedRevision",revision); }
    Reply worker(String method,String path,String token,Object body,String claim)throws Exception {
        var req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/grading-worker"+path)).timeout(Duration.ofSeconds(30));if(token!=null)req.header("X-Worker-Token",token);if(claim!=null)req.header("X-Claim-Token",claim);req.header("Content-Type","application/json");req.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));var res=client.send(req.build(),HttpResponse.BodyHandlers.ofString());JsonNode env=mapper.readTree(res.body());if(res.statusCode()<300)recordSample(method.toLowerCase(),"/grading-worker"+path,res.statusCode(),env);return new Reply(res.statusCode(),env.path("data"),env);
    }
    JsonNode claim(Fixture f)throws Exception { assertThat(worker("POST","/heartbeat",f.pair(),Map.of("paused",false,"reason",""),null).status()).isEqualTo(200);var c=worker("POST","/claim",f.pair(),Map.of(),null);assertThat(c.status()).isEqualTo(200);return c.data(); }
    Map<String,Object> result(Fixture f,int score,boolean review) { return Map.of("rubricId",f.rubric(),"answers",List.of(Map.of("number","1","recognizedAnswer",review?"无法辨认":"12","points",List.of(Map.of("pointId","answer","score",score,"reason",score==100?"":"未得分")),"pages",List.of(1),"reviewItems",review?List.of("字迹模糊，请核对"):List.of())),"reviewItems",List.of()); }
    Map<String,Object> callback(Fixture f,JsonNode c,int score,boolean review) { var b=new LinkedHashMap<String,Object>();b.put("leaseToken",c.path("leaseToken").asText());b.put("model","verified-test");b.put("result",result(f,score,review));b.put("rubric",null);return b; }
    String enqueue(Fixture f)throws Exception { var applied=send("POST","/grading/submissions/"+f.submission()+"/tasks",f.token(),practice(1),null);assertThat(applied.status()).withFailMessage(applied.envelope().toString()).isEqualTo(200);return applied.data().path("id").asText(); }
    @Test void duplicateApplyAndCallbackAreAtomicAndPermissionsAreScoped()throws Exception {
        var f=fixture(true);String id=enqueue(f);assertThat(enqueue(f)).isEqualTo(id);
        var queued=send("GET","/grading/tasks/"+id,f.token(),null,null);assertThat(queued.data().path("workerOnline").asBoolean()).isFalse();assertThat(queued.data().path("workerReason").asText()).contains("离线");
        String other=login(account(Role.USER)); assertThat(send("GET","/grading/submissions/"+f.submission(),other,null,null).status()).isEqualTo(404);assertThat(send("GET","/grading/tasks/"+id,other,null,null).status()).isEqualTo(404);
        assertThat(send("POST","/grading/rubrics/"+f.rubric()+"/publish",other,null,null).status()).isEqualTo(403);assertThat(worker("POST","/claim",f.token(),Map.of(),null).status()).isEqualTo(401);
        var c=claim(f);assertThat(c.path("taskId").asText()).isEqualTo(id);assertThat(worker("POST","/claim",f.pair(),Map.of(),null).data().isNull()).isTrue();
        String page=c.path("inputs").path("pages").get(0).path("fileId").asText();var material=worker("GET","/tasks/"+id+"/materials/"+page,f.pair(),null,c.path("leaseToken").asText());assertThat(material.status()).isEqualTo(200);assertThat(Base64.getDecoder().decode(material.data().path("contentBase64").asText())).isEqualTo(image());
        assertThat(worker("POST","/tasks/"+id+"/renew",f.pair(),Map.of("leaseToken",c.path("leaseToken").asText()),null).status()).isEqualTo(200);
        var posted=worker("POST","/tasks/"+id+"/result",f.pair(),callback(f,c,100,false),null);assertThat(posted.status()).withFailMessage(posted.envelope().toString()).isEqualTo(200);assertThat(posted.data().path("state").asText()).isEqualTo("COMPLETED");
        assertThat(worker("POST","/tasks/"+id+"/result",f.pair(),callback(f,c,100,false),null).envelope()).isEqualTo(posted.envelope());assertThat(worker("POST","/tasks/"+id+"/result",f.pair(),callback(f,c,99,false),null).status()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM score_record WHERE user_id=?",Integer.class,f.user())).isEqualTo(1);assertThat(jdbc.queryForObject("SELECT source FROM score_record WHERE user_id=?",String.class,f.user())).isEqualTo("CODEX");assertThat(jdbc.queryForObject("SELECT score FROM score_record WHERE user_id=?",Integer.class,f.user())).isEqualTo(100);
        var w=send("GET","/grading/workers",f.token(),null,null).data().get(0);assertThat(send("DELETE","/grading/workers/"+w.path("id").asText(),f.token(),null,null).status()).isEqualTo(200);assertThat(worker("POST","/claim",f.pair(),Map.of(),null).status()).isEqualTo(401);
    }
    @Test void unpublishedStandardsInvalidScoresAndReviewNeverCreatePrematureScores()throws Exception {
        var f=fixture(false);assertThat(send("POST","/grading/submissions/"+f.submission()+"/tasks",f.token(),practice(1),null).status()).isEqualTo(422);
        send("POST","/grading/rubrics/"+f.rubric()+"/publish",f.token(),null,null);String id=enqueue(f);var c=claim(f);
        assertThat(worker("POST","/tasks/"+id+"/result",f.pair(),callback(f,c,101,false),null).status()).isEqualTo(400);
        var bad=callback(f,c,100,false);var wrong=new LinkedHashMap<>(result(f,100,false));wrong.put("rubricId",UUID.randomUUID().toString());bad.put("result",wrong);assertThat(worker("POST","/tasks/"+id+"/result",f.pair(),bad,null).status()).isEqualTo(422);
        var posted=worker("POST","/tasks/"+id+"/result",f.pair(),callback(f,c,0,true),null);assertThat(posted.status()).isEqualTo(200);assertThat(posted.data().path("state").asText()).isEqualTo("REVIEW");assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM score_record WHERE user_id=?",Integer.class,f.user())).isZero();
        assertThat(send("POST","/grading/tasks/"+id+"/review",f.token(),result(f,0,true),null).status()).isEqualTo(422);var finalized=send("POST","/grading/tasks/"+id+"/review",f.token(),result(f,100,false),null);assertThat(finalized.status()).isEqualTo(200);assertThat(finalized.data().path("state").asText()).isEqualTo("COMPLETED");assertThat(send("POST","/grading/tasks/"+id+"/review",f.token(),result(f,100,false),null).status()).isEqualTo(200);
    }
    @Test void expiredLeaseCannotOverwriteNewClaimAndMissingPhotoRollsBackScore()throws Exception {
        var f=fixture(true);String id=enqueue(f);var old=claim(f);jdbc.update("UPDATE grading_task SET lease_until=DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 1 SECOND) WHERE id=?",id);var c=claim(f);assertThat(c.path("leaseToken").asText()).isNotEqualTo(old.path("leaseToken").asText());
        assertThat(worker("POST","/tasks/"+id+"/result",f.pair(),callback(f,old,100,false),null).status()).isEqualTo(409);assertThat(worker("POST","/tasks/"+id+"/renew",f.pair(),Map.of("leaseToken",old.path("leaseToken").asText()),null).status()).isEqualTo(409);
        String page=c.path("inputs").path("pages").get(0).path("fileId").asText();Path path=ROOT.resolve("managed/"+page);byte[] bytes=Files.readAllBytes(path);Files.delete(path);
        assertThat(worker("POST","/tasks/"+id+"/result",f.pair(),callback(f,c,100,false),null).status()).isEqualTo(404);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM score_record WHERE user_id=?",Integer.class,f.user())).isZero();assertThat(jdbc.queryForObject("SELECT state FROM grading_task WHERE id=?",String.class,id)).isEqualTo("GRADING");
        Files.write(path,bytes);assertThat(worker("POST","/tasks/"+id+"/result",f.pair(),callback(f,c,100,false),null).status()).isEqualTo(200);
    }
    @Test void frozenInputsSurviveRemovalAndRetriesStopAfterThreeClaims()throws Exception {
        var f=fixture(true);String id=enqueue(f);var c=claim(f);String page=c.path("inputs").path("pages").get(0).path("fileId").asText();
        assertThat(send("DELETE","/grading/submissions/"+f.submission()+"/pages/"+page+"?expectedRevision=1",f.token(),null,null).status()).isEqualTo(200);assertThat(worker("GET","/tasks/"+id+"/materials/"+page,f.pair(),null,c.path("leaseToken").asText()).status()).isEqualTo(200);
        for(int i=0;i<3;i++) {var r=worker("POST","/tasks/"+id+"/failure",f.pair(),Map.of("leaseToken",c.path("leaseToken").asText(),"reason","temporary connection failure","retryable",true,"pause",false),null);assertThat(r.status()).isEqualTo(200);assertThat(r.data().path("state").asText()).isEqualTo(i==2?"FAILED":"QUEUED");if(i<2)c=claim(f);}
    }
    @Test void rubricDraftAndEveryUserMaterialEndpointUseFrozenInputs()throws Exception {
        var f=fixture(true);var s=send("GET","/grading/submissions/"+f.submission(),f.token(),null,null);String file=s.data().path("pages").get(0).path("fileId").asText();
        assertThat(send("GET","/grading/submissions?courseId="+COURSE+"&cycleId="+CYCLE,f.token(),null,null).status()).isEqualTo(200);
        assertThat(send("PUT","/grading/submissions/"+f.submission()+"/pages",f.token(),Map.of("expectedRevision",1,"fileIds",List.of(file)),null).status()).isEqualTo(200);
        assertThat(send("PUT","/grading/submissions/"+f.submission()+"/pages",f.token(),Map.of("expectedRevision",1,"fileIds",List.of(file)),null).status()).isEqualTo(409);
        var applied=send("POST","/grading/submissions/"+f.submission()+"/tasks",f.token(),practice(2),null);String id=applied.data().path("id").asText();
        assertThat(send("GET","/grading/submissions/"+f.submission()+"/tasks",f.token(),null,null).status()).isEqualTo(200);
        assertThat(send("GET","/grading/tasks/"+id+"/inputs",f.token(),null,null).data().path("rubric").path("id").asText()).isEqualTo(f.rubric());
        assertThat(send("GET","/grading/tasks/"+id+"/materials/"+file,f.token(),null,null).status()).isEqualTo(200);
        assertThat(send("GET","/grading/rubrics?courseId="+COURSE+"&cycleId="+CYCLE+"&paperId="+f.paper(),f.token(),null,null).status()).isEqualTo(200);
        var c=claim(f);worker("POST","/tasks/"+id+"/result",f.pair(),callback(f,c,100,false),null);
        var draft=send("POST","/grading/rubric-tasks?courseId="+COURSE+"&cycleId="+CYCLE+"&paperId="+f.paper(),f.token(),null,null);assertThat(draft.status()).isEqualTo(200);String draftId=draft.data().path("id").asText();var d=claim(f);
        var rubric=send("GET","/grading/rubrics?courseId="+COURSE+"&cycleId="+CYCLE+"&paperId="+f.paper(),f.token(),null,null).data().get(0).path("document");
        var payload=new LinkedHashMap<String,Object>();payload.put("leaseToken",d.path("leaseToken").asText());payload.put("model","draft-test");payload.put("result",null);payload.put("rubric",rubric);
        var returned=worker("POST","/tasks/"+draftId+"/result",f.pair(),payload,null);assertThat(returned.status()).withFailMessage(returned.envelope().toString()).isEqualTo(200);String generated=returned.data().path("rubricId").asText();assertThat(generated).isNotBlank();
        assertThat(jdbc.queryForObject("SELECT state FROM grading_rubric WHERE id=?",String.class,generated)).isEqualTo("DRAFT");assertThat(send("PUT","/grading/rubrics/"+generated,f.token(),rubric,null).status()).isEqualTo(200);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM score_record WHERE user_id=?",Integer.class,f.user())).isEqualTo(1);
    }
    @Test void concurrentRequestsAndClaimsHaveOneEffectiveTaskAndOneLease()throws Exception {
        var f=fixture(true);var pool=Executors.newFixedThreadPool(2);
        try {
            var requests=pool.invokeAll(List.<Callable<String>>of(()->enqueue(f),()->enqueue(f)));assertThat(requests.get(0).get()).isEqualTo(requests.get(1).get());
            worker("POST","/heartbeat",f.pair(),Map.of("paused",false,"reason",""),null);
            var claims=pool.invokeAll(List.<Callable<Reply>>of(()->worker("POST","/claim",f.pair(),Map.of(),null),()->worker("POST","/claim",f.pair(),Map.of(),null)));
            assertThat(List.of(claims.get(0).get(),claims.get(1).get()).stream().filter(r->!r.data().isNull()).count()).isEqualTo(1);
        } finally {pool.shutdownNow();}
    }
    @Test void quotaPauseRetainsTaskAndExpiredDeadlineRejectsResultAndForgedImages()throws Exception {
        var f=fixture(true);long before=jdbc.queryForObject("SELECT COUNT(*) FROM stored_file",Long.class);
        assertThat(upload(f.submission(),f.token(),1,"not a real PNG".getBytes()).status()).isEqualTo(415);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM stored_file",Long.class)).isEqualTo(before);
        String id=enqueue(f);var c=claim(f);var paused=worker("POST","/tasks/"+id+"/failure",f.pair(),Map.of("leaseToken",c.path("leaseToken").asText(),"reason","CODEX_AUTH_OR_USAGE: 用量限制","retryable",true,"pause",true),null);
        assertThat(paused.status()).isEqualTo(200);assertThat(paused.data().path("state").asText()).isEqualTo("QUEUED");assertThat(paused.data().path("workerReason").asText()).contains("用量限制");assertThat(jdbc.queryForObject("SELECT attempts FROM grading_task WHERE id=?",Integer.class,id)).isZero();assertThat(worker("POST","/claim",f.pair(),Map.of(),null).data().isNull()).isTrue();
        worker("POST","/heartbeat",f.pair(),Map.of("paused",false,"reason",""),null);c=claim(f);jdbc.update("UPDATE grading_task SET deadline=DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 1 SECOND) WHERE id=?",id);
        assertThat(worker("POST","/tasks/"+id+"/renew",f.pair(),Map.of("leaseToken",c.path("leaseToken").asText()),null).status()).isEqualTo(409);assertThat(worker("POST","/tasks/"+id+"/result",f.pair(),callback(f,c,100,false),null).status()).isEqualTo(409);
        var next=claim(f);assertThat(next.path("leaseToken").asText()).isNotEqualTo(c.path("leaseToken").asText());
    }
    @Test @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="REAL_CODEX_GRADING",matches="true")
    void realBrowserToLocalCodexAndScoreWithoutMocks()throws Exception {
        var f=fixture(true);int uiPort;try(var socket=new java.net.ServerSocket(0)){uiPort=socket.getLocalPort();}
        Path frontend=Path.of("../frontend").toAbsolutePath().normalize(),workerDir=Path.of("mac-grader").toAbsolutePath().normalize(),evidence=Path.of("target/grading-evidence").toAbsolutePath();Files.createDirectories(evidence);
        Path state=evidence.resolve("worker-state"),tokenFile=evidence.resolve("pairing.token"),config=evidence.resolve("worker-config.json"),image=evidence.resolve("answer.png");Files.write(image,image());Files.writeString(tokenFile,f.pair());Files.setPosixFilePermissions(tokenFile,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"));
        mapper.writeValue(config.toFile(),Map.of("server","http://127.0.0.1:"+port,"allowLocalHttp",true,"tokenFile",tokenFile.toString(),"stateDir",state.toString(),"codexBinary",System.getenv().getOrDefault("CODEX_BIN","/Users/huagu/.local/bin/codex"),"pdfRenderer",System.getenv().getOrDefault("PDF_RENDERER","/Users/huagu/.cache/codex-runtimes/codex-primary-runtime/dependencies/bin/override/pdftoppm")));
        var vite=new ProcessBuilder("node","node_modules/vite/bin/vite.js","--host","127.0.0.1","--port",String.valueOf(uiPort),"--strictPort").directory(frontend.toFile()).redirectErrorStream(true).redirectOutput(evidence.resolve("vite.log").toFile());vite.environment().put("API_PROXY_TARGET","http://127.0.0.1:"+port);vite.environment().put("VITE_API_MOCK","false");vite.environment().put("VITE_API_ORIGIN","");
        Process server=vite.start(),worker=new ProcessBuilder("node","dist/main.js",config.toString()).directory(workerDir.toFile()).redirectErrorStream(true).redirectOutput(evidence.resolve("worker.log").toFile()).start();
        try {
            boolean ready=false;for(int i=0;i<100;i++){try{if(client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+uiPort+"/login")).header("Accept","text/html").timeout(Duration.ofSeconds(1)).method("HEAD",HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.discarding()).statusCode()==200){ready=true;break;}}catch(Exception ignored){}Thread.sleep(100);}assertThat(ready).isTrue();
            var browser=new ProcessBuilder("node","scripts/grading-browser-check.mjs").directory(frontend.toFile()).redirectErrorStream(true).redirectOutput(evidence.resolve("browser.log").toFile());
            browser.environment().put("REFACTOR_UI_URL","http://127.0.0.1:"+uiPort);browser.environment().put("REFACTOR_USERNAME",f.name());browser.environment().put("REFACTOR_PASSWORD","12345678");browser.environment().put("REFACTOR_CYCLE",CYCLE);browser.environment().put("REFACTOR_PAPER",f.paper());browser.environment().put("REFACTOR_IMAGE",image.toString());browser.environment().put("REFACTOR_EVIDENCE",evidence.toString());
            Process run=browser.start();boolean done=run.waitFor(180,TimeUnit.SECONDS);if(!done)run.destroyForcibly();assertThat(done).as("real grading browser timeout").isTrue();assertThat(run.exitValue()).withFailMessage(Files.readString(evidence.resolve("browser.log"))+"\n"+Files.readString(evidence.resolve("worker.log"))).isZero();
            String question=files.upload(new MockMultipartFile("file","question.pdf","application/pdf",validPdf("Question 1: 7 + 5 = ? Full marks: 100.")),"PAPER",null,false);
            String answer=files.upload(new MockMultipartFile("file","answer.pdf","application/pdf",validPdf("Question 1 answer: 12. Award 100 for 12, otherwise 0.")),"PAPER",null,true);
            jdbc.update("UPDATE paper SET question_file_id=?,answer_file_id=? WHERE id=?",question,answer,f.paper());
            var generated=send("POST","/grading/rubric-tasks?courseId="+COURSE+"&cycleId="+CYCLE+"&paperId="+f.paper(),f.token(),null,null);assertThat(generated.status()).isEqualTo(200);String draftTask=generated.data().path("id").asText();
            JsonNode finished=null;for(int i=0;i<120;i++){finished=send("GET","/grading/tasks/"+draftTask,f.token(),null,null).data();if(Set.of("COMPLETED","FAILED").contains(finished.path("state").asText()))break;Thread.sleep(1000);}
            assertThat(finished.path("state").asText()).withFailMessage(finished.toString()+"\n"+Files.readString(evidence.resolve("worker.log"))).isEqualTo("COMPLETED");
            assertThat(jdbc.queryForObject("SELECT state FROM grading_rubric WHERE id=?",String.class,finished.path("rubricId").asText())).isEqualTo("DRAFT");
            assertThat(finished.path("rubricDraft").path("questions").size()).isEqualTo(1);assertThat(finished.path("rubricDraft").path("questions").get(0).path("referenceAnswer").asText()).contains("12");
            mapper.writeValue(evidence.resolve("real-pdf-draft.json").toFile(),Map.of("actualCodexCall",true,"pdfRenderedToLocalImages",true,"publishedAutomatically",false,"task",finished));
            assertThat(jdbc.queryForObject("SELECT score FROM score_record WHERE user_id=?",Integer.class,f.user())).isEqualTo(100);assertThat(jdbc.queryForObject("SELECT source FROM score_record WHERE user_id=?",String.class,f.user())).isEqualTo("CODEX");assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM score_record_image WHERE user_id=?",Integer.class,f.user())).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT answers_seen_before FROM score_record WHERE user_id=?",Boolean.class,f.user())).isTrue();assertThat(jdbc.queryForObject("SELECT closed_book FROM score_record WHERE user_id=?",Boolean.class,f.user())).isFalse();
            mapper.writeValue(evidence.resolve("real-codex.json").toFile(),Map.of("actualCodexCall",true,"actualBrowser",true,"mockRoutes",false,"expectedManualScore",100,"actualScore",100,"source","CODEX","qualificationPreserved",true,"fixture","controlled printed answer, manually checked arithmetic; not handwriting benchmark"));
        } finally {worker.destroy();server.destroy();worker.waitFor(10,TimeUnit.SECONDS);server.waitFor(5,TimeUnit.SECONDS);if(worker.isAlive())worker.destroyForcibly();if(server.isAlive())server.destroyForcibly();Files.deleteIfExists(tokenFile);Files.deleteIfExists(config);}
    }
    void recordSample(String method,String path,int status,JsonNode body) {var sample=mapper.createObjectNode();sample.put("method",method);sample.put("path",path);sample.put("status",status);sample.set("body",body);samples.add(sample);}
    byte[] validPdf(String text)throws Exception {
        var bytes=new java.io.ByteArrayOutputStream();bytes.write("%PDF-1.4\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        String stream="BT /F1 18 Tf 35 150 Td ("+text+") Tj ET";
        List<String> objects=List.of("<< /Type /Catalog /Pages 2 0 R >>","<< /Type /Pages /Kids [3 0 R] /Count 1 >>","<< /Type /Page /Parent 2 0 R /MediaBox [0 0 800 300] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>","<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>","<< /Length "+stream.length()+" >>\nstream\n"+stream+"\nendstream");
        List<Integer> offsets=new ArrayList<>();for(int i=0;i<objects.size();i++){offsets.add(bytes.size());bytes.write(((i+1)+" 0 obj\n"+objects.get(i)+"\nendobj\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));}
        int xref=bytes.size();bytes.write(("xref\n0 6\n0000000000 65535 f \n").getBytes());for(int offset:offsets)bytes.write(String.format("%010d 00000 n \n",offset).getBytes());bytes.write(("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n"+xref+"\n%%EOF\n").getBytes());return bytes.toByteArray();
    }
    @AfterAll static void cleanup()throws Exception {Path evidence=Path.of("target/grading-evidence");Files.createDirectories(evidence);Path contract=evidence.resolve("http-contract-samples.json");if(Files.exists(contract)){for(JsonNode previous:new ObjectMapper().readTree(contract.toFile()))samples.add(previous);}new ObjectMapper().writeValue(contract.toFile(),samples);if(Files.exists(ROOT))try(var paths=Files.walk(ROOT)){for(var p:paths.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);}}
}
