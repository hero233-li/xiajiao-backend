package cn.xuexizhitu;
import cn.xuexizhitu.identity.application.AccountService;
import cn.xuexizhitu.identity.api.RegisterRequest;
import cn.xuexizhitu.identity.domain.Role;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"app.assessments.timeout-enabled=false","app.files.cleanup-enabled=false"})
@Testcontainers
class FitnessHttpMySqlIT {
 @Container static MySQLContainer<?> mysql=new MySQLContainer<>("mysql:8.4").withDatabaseName("fitness_isolated");
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){r.add("spring.datasource.url",mysql::getJdbcUrl);r.add("spring.datasource.username",mysql::getUsername);r.add("spring.datasource.password",mysql::getPassword);r.add("app.files.root",()->"/private/tmp/fitness-test-files");r.add("app.bootstrap.enabled",()->false);}
 @Autowired AccountService accounts; @Autowired JdbcTemplate jdbc; @Autowired ObjectMapper mapper; @LocalServerPort int port;
 final HttpClient client=HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
 record Reply(int status,JsonNode data,JsonNode body) {}
 Reply send(String method,String path,String token,Object body,String key)throws Exception{var r=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1"+path)).timeout(Duration.ofSeconds(30));if(token!=null)r.header("Authorization","Bearer "+token);r.header("Content-Type","application/json");if(key!=null)r.header("Idempotency-Key",key);r.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));var result=client.send(r.build(),HttpResponse.BodyHandlers.ofString());assertThat(result.headers().firstValue("Cache-Control")).contains("no-store");var json=mapper.readTree(result.body());return new Reply(result.statusCode(),json.path("data"),json);}
 String account(Role role)throws Exception{String name="fit"+UUID.randomUUID();accounts.create(new RegisterRequest(name,name+"@example.com","12345678"),role);return name;}
 String login(String name)throws Exception{return send("POST","/auth/login",null,Map.of("identifier",name,"password","12345678"),null).data().path("accessToken").asText();}
 String user(String token)throws Exception{return send("GET","/auth/me",token,null,null).data().path("id").asText();}
 String today(){return LocalDate.now(ZoneId.of("Asia/Shanghai")).toString();}
 Object json(String body)throws Exception{return mapper.readTree(body);}
 Map<String,Object> writeBody(Object data,long revision,Long goal){var body=new LinkedHashMap<String,Object>();body.put("data",data);body.put("expectedRevision",revision);body.put("expectedGoalRevision",goal);return body;}
 Reply write(String token,String kind,String key,Object data,long revision)throws Exception{return send("PUT","/fitness/records/"+kind+"/"+key,token,writeBody(data,revision,null),UUID.randomUUID().toString());}
 String action(){return UUID.randomUUID().toString();}
 Object exercise(String id,boolean completed)throws Exception{return json("{\"id\":\""+id+"\",\"name\":\"深蹲\",\"type\":\"STRENGTH\",\"sets\":3,\"reps\":8,\"kg\":25.5,\"minutes\":null,\"km\":null,\"note\":null,\"completed\":"+completed+"}");}
 Object plan(String id)throws Exception{return Map.of("rest",false,"exercises",List.of(exercise(id,false)),"note","测试计划");}
 @Test void ownershipValidationDuplicatesConcurrentWritesAndReplay()throws Exception{
  String token=login(account(Role.USER)),other=login(account(Role.USER)),day=today();String path="/fitness/records/weight/"+day;var body=writeBody(Map.of("kg",70.125,"note","测试"),-1,null);String key=action();
  assertThat(send("GET","/fitness/summary",null,null,null).status()).isEqualTo(401);assertThat(send("GET","/admin/files",token,null,null).status()).isEqualTo(403);
  var first=send("PUT",path,token,body,key);assertThat(first.status()).isEqualTo(200);assertThat(send("PUT",path,token,body,key).data()).isEqualTo(first.data());
  assertThat(send("PUT",path,token,writeBody(Map.of("kg",71),-1,null),key).status()).isEqualTo(409);assertThat(send("PUT",path,token,body,action()).status()).isEqualTo(409);
  assertThat(send("GET",path,other,null,null).data().isNull()).isTrue();assertThat(write(other,"weight",day,Map.of("kg",72),0).status()).isEqualTo(409);
  var pool=Executors.newFixedThreadPool(2);try{var futures=pool.invokeAll(List.<Callable<Reply>>of(()->write(token,"weight",day,Map.of("kg",70.5),0),()->write(token,"weight",day,Map.of("kg",70.75),0)));assertThat(List.of(futures.get(0).get().status(),futures.get(1).get().status())).containsExactlyInAnyOrder(200,409);}finally{pool.shutdownNow();}
  assertThat(write(token,"weight",LocalDate.parse(day).plusDays(1).toString(),Map.of("kg",70),-1).status()).isEqualTo(422);
  assertThat(write(token,"weight",day,Map.of("kg",-1),1).status()).isEqualTo(400);
  assertThat(write(token,"weight",day,Map.of("kg",70,"userId",user(other)),1).status()).isEqualTo(400);
  assertThat(send("GET","/fitness/history?from=2020-01-01&to=2026-01-01",token,null,null).status()).isEqualTo(400);
  assertThat(send("GET","/fitness/records/weight?page=0",token,null,null).status()).isEqualTo(400);
  assertThat(send("POST","/fitness/copy",token,Map.of("destination",day,"expectedRevision",-1),action()).status()).isEqualTo(400);
  assertThat(write(token,"training-plan",day,json("{\"rest\":false,\"exercises\":[null]}"),-1).status()).isEqualTo(400);
  assertThat(write(token,"meals",day,json("{\"foods\":[{\"meal\":\"LUNCH\",\"name\":\"单位测试\",\"quantity\":1}]}"),-1).status()).isEqualTo(400);
  assertThat(write(token,"checkin",day,json("{}"),-1).status()).isEqualTo(200);assertThat(write(token,"checkin",day,json("{}"),-1).status()).isEqualTo(409);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM fitness_record WHERE user_id=? AND kind='checkin'",Integer.class,user(token))).isEqualTo(1);
 }
 @Test void goalTypesVersionsEndingAndHistory()throws Exception{
  String token=login(account(Role.USER));long revision=-1;for(String type:List.of("LOSE","GAIN","MAINTAIN")){String id=action();int target=type.equals("LOSE")?65:type.equals("GAIN")?75:70;var goal=Map.of("type",type,"startDate",today(),"startWeight",70,"targetWeight",target);var response=send("PUT","/fitness/records/goal/"+id,token,writeBody(goal,-1,revision),action());assertThat(response.status()).as(response.body().toString()).isEqualTo(200);revision++;assertThat(send("GET","/fitness/summary",token,null,null).data().path("currentGoal").path("key").asText()).isEqualTo(id);}
  assertThat(send("GET","/fitness/records/goal",token,null,null).data().path("total").asInt()).isEqualTo(3);
  assertThat(send("POST","/fitness/goals/end",token,Map.of("expectedGoalRevision",0),action()).status()).isEqualTo(409);
  String key=action();var body=Map.of("expectedGoalRevision",revision);assertThat(send("POST","/fitness/goals/end",token,body,key).status()).isEqualTo(200);assertThat(send("POST","/fitness/goals/end",token,body,key).status()).isEqualTo(200);
  assertThat(send("GET","/fitness/summary",token,null,null).data().path("currentGoal").isNull()).isTrue();assertThat(send("GET","/fitness/goals/history",token,null,null).data().path("total").asInt()).isEqualTo(4);
 }
 @Test void snapshotsWeeklyAtomicityUnknownNutritionAndStatistics()throws Exception{
  String token=login(account(Role.USER)),other=login(account(Role.USER)),day=today(),id=action(),template=action();Object p=plan(id);assertThat(write(token,"training-template",template,Map.of("name","力量模板","plan",p),-1).status()).isEqualTo(200);
  var copy=Map.of("sourceKind","training-template","sourceKey",template,"destination",day,"expectedRevision",-1);String key=action();var first=send("POST","/fitness/copy",token,copy,key);assertThat(first.status()).isEqualTo(200);assertThat(send("POST","/fitness/copy",token,copy,key).data()).isEqualTo(first.data());assertThat(send("POST","/fitness/copy",other,copy,action()).status()).isEqualTo(404);
  assertThat(write(token,"training",day,Map.of("status","COMPLETED","exercises",List.of()),-1).status()).isEqualTo(400);
  Object actual=Map.of("status","COMPLETED","exercises",List.of(exercise(id,true)),"note","已完成");assertThat(write(token,"training",day,actual,-1).status()).isEqualTo(200);
  Object rest=Map.of("rest",true,"exercises",List.of());assertThat(write(token,"training-template",template,Map.of("name","后来修改","plan",rest),0).status()).isEqualTo(200);assertThat(send("GET","/fitness/records/training-plan/"+day,token,null,null).data().path("data").path("rest").asBoolean()).isFalse();
  assertThat(write(token,"training-plan",day,rest,0).status()).isEqualTo(200);assertThat(send("GET","/fitness/records/training/"+day,token,null,null).data().path("data").path("planSnapshot").path("rest").asBoolean()).isFalse();
  var stats=send("GET","/fitness/statistics?from="+day+"&to="+day,token,null,null).data();assertThat(stats.path("plannedTrainingDays").asInt()).isEqualTo(1);assertThat(stats.path("trainingRate").decimalValue()).isEqualByComparingTo("100");
  String before=LocalDate.parse(day).minusDays(1).toString();assertThat(write(token,"training-plan",before,rest,-1).status()).isEqualTo(200);assertThat(write(token,"training",before,Map.of("status","REST","exercises",List.of()),-1).status()).isEqualTo(200);
  Object food=json("{\"foods\":[{\"meal\":\"LUNCH\",\"name\":\"测试食物\",\"quantity\":1,\"unit\":\"份\",\"kcal\":200.5},{\"meal\":\"DINNER\",\"name\":\"未填营养\"}]}");assertThat(write(token,"meals",day,food,-1).status()).isEqualTo(200);var nutrition=send("GET","/fitness/days/"+day,token,null,null).data().path("nutrition");assertThat(nutrition.path("kcal").path("knownTotal").decimalValue()).isEqualByComparingTo("200.5");assertThat(nutrition.path("kcal").path("complete").asBoolean()).isFalse();assertThat(nutrition.path("protein").path("knownTotal").isNull()).isTrue();
  String week=action();assertThat(write(token,"week-template",week,Map.of("name","一周","days",Collections.nCopies(7,rest)),-1).status()).isEqualTo(200);LocalDate monday=LocalDate.parse(day).plusWeeks(1).with(java.time.DayOfWeek.MONDAY);String collision=monday.plusDays(3).toString();assertThat(write(token,"training-plan",collision,rest,-1).status()).isEqualTo(200);
  var generate=Map.of("templateKey",week,"monday",monday.toString(),"expectedRevisions",Collections.nCopies(7,-1));assertThat(send("POST","/fitness/weeks/generate",token,generate,action()).status()).isEqualTo(409);assertThat(send("GET","/fitness/records/training-plan/"+monday,token,null,null).data().isNull()).isTrue();
  var revisions=new ArrayList<>(Collections.nCopies(7,-1));revisions.set(3,0);var generation=Map.of("templateKey",week,"monday",monday.toString(),"expectedRevisions",revisions);String generationKey=action();assertThat(send("POST","/fitness/weeks/generate",token,generation,generationKey).status()).isEqualTo(200);assertThat(send("POST","/fitness/weeks/generate",token,generation,generationKey).status()).isEqualTo(200);
 }
 @Test void gapsSampleCountsStreakAndPartialSkippedUnplanned()throws Exception{
  String token=login(account(Role.USER));LocalDate now=LocalDate.parse(today());for(int n:List.of(1,2,4)){assertThat(write(token,"checkin",now.minusDays(n).toString(),json("{}"),-1).status()).isEqualTo(200);}
  assertThat(send("GET","/fitness/summary",token,null,null).data().path("streak").asInt()).isEqualTo(2);assertThat(write(token,"checkin",today(),json("{}"),-1).status()).isEqualTo(200);assertThat(send("GET","/fitness/summary",token,null,null).data().path("streak").asInt()).isEqualTo(3);
  assertThat(write(token,"weight",today(),Map.of("kg",70.125),-1).status()).isEqualTo(200);assertThat(write(token,"weight",now.minusDays(2).toString(),Map.of("kg",71.375),-1).status()).isEqualTo(200);var mean=send("GET","/fitness/summary",token,null,null).data().path("sevenDayWeight");assertThat(mean.path("samples").asInt()).isEqualTo(2);assertThat(mean.path("mean").decimalValue()).isEqualByComparingTo("70.750");
  var days=send("GET","/fitness/history?from="+now.minusDays(2)+"&to="+now,token,null,null).data();assertThat(days.get(1).path("records").has("weight")).isFalse();
  String unplannedDate=now.minusDays(5).toString(),unplannedId=action();
  assertThat(write(token,"training",unplannedDate,Map.of("status","COMPLETED","exercises",List.of(exercise(unplannedId,true))),-1).status()).isEqualTo(200);
  assertThat(write(token,"training-plan",unplannedDate,plan(action()),-1).status()).isEqualTo(200);
  assertThat(send("GET","/fitness/statistics?from="+unplannedDate+"&to="+unplannedDate,token,null,null).data().path("trainingRate").isNull()).isTrue();
  var empty=send("GET","/fitness/statistics?from="+now.minusDays(6)+"&to="+now,token,null,null).data();assertThat(empty.path("trainingRate").isNull()).isTrue();
  for(int n=0;n<2;n++){String date=now.minusDays(n).toString(),id=action();assertThat(write(token,"training-plan",date,plan(id),-1).status()).isEqualTo(200);assertThat(write(token,"training",date,Map.of("status",n==0?"PARTIAL":"SKIPPED","exercises",List.of(exercise(id,false))),-1).status()).isEqualTo(200);}
  var stats=send("GET","/fitness/statistics?from="+now.minusDays(6)+"&to="+now,token,null,null).data();assertThat(stats.path("partialTrainingDays").asInt()).isEqualTo(1);assertThat(stats.path("skippedTrainingDays").asInt()).isEqualTo(1);assertThat(stats.path("trainingRate").decimalValue()).isZero();
 }
 @Test void realBrowserDesktopMobileAndLearningCompatibility()throws Exception{
  String name=account(Role.USER),token=login(name),user=user(token),otherName=account(Role.USER);String course="afdac469-0fe4-5007-833c-51a71333967b",cycle="0ebdbbfe-d607-54b5-9c21-4e0adade5e4c";jdbc.update("INSERT INTO enrollment(user_id,cycle_id,course_id,paid) VALUES(?,?,?,true)",user,cycle,course);
  int uiPort;try(var socket=new java.net.ServerSocket(0)){uiPort=socket.getLocalPort();}
  Path frontend=Path.of("../frontend").toRealPath(),evidence=frontend.resolve("docs/platform/evidence");Files.createDirectories(evidence);ProcessBuilder vite=new ProcessBuilder("node","node_modules/vite/bin/vite.js","--host","127.0.0.1","--port",Integer.toString(uiPort),"--strictPort").directory(frontend.toFile()).redirectErrorStream(true).redirectOutput(evidence.resolve("vite.log").toFile());vite.environment().put("API_PROXY_TARGET","http://127.0.0.1:"+port);vite.environment().put("VITE_API_MOCK","false");Process server=vite.start();try{boolean ready=false;for(int i=0;i<60;i++){if(!server.isAlive())break;try{ready=client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+uiPort+"/login")).header("Accept","text/html").timeout(Duration.ofSeconds(1)).method("HEAD",HttpRequest.BodyPublishers.noBody()).build(),HttpResponse.BodyHandlers.discarding()).statusCode()==200;}catch(Exception ignored){}if(ready)break;Thread.sleep(250);}assertThat(ready).isTrue();ProcessBuilder browser=new ProcessBuilder("node","scripts/platform-browser-check.mjs").directory(frontend.toFile()).redirectErrorStream(true).redirectOutput(evidence.resolve("browser.log").toFile());browser.environment().putAll(Map.of("PLATFORM_URL","http://127.0.0.1:"+uiPort,"PLATFORM_USERNAME",name,"PLATFORM_PASSWORD","12345678","PLATFORM_OTHER",otherName,"PLATFORM_CYCLE",cycle,"PLATFORM_BACKEND","http://127.0.0.1:"+port));Process runner=browser.start();assertThat(runner.waitFor(180,TimeUnit.SECONDS)).isTrue();assertThat(runner.exitValue()).as(Files.readString(evidence.resolve("browser.log"))).isZero();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM enrollment WHERE user_id=?",Integer.class,user)).isEqualTo(1);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_item_progress WHERE user_id=? AND completed=true",Integer.class,user)).isGreaterThan(0);
  }finally{server.destroy();if(!server.waitFor(5,TimeUnit.SECONDS))server.destroyForcibly();}
 }
}
