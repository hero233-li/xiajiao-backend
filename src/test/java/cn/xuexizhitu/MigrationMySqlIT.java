package cn.xuexizhitu;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

/** Uses only a disposable database; compares every old field, not merely row counts. */
@Testcontainers
class MigrationMySqlIT {
    @Container static MySQLContainer<?> mysql=new MySQLContainer<>("mysql:8.4").withDatabaseName("migration_rehearsal");
    private Flyway flyway(String target) {
        var c=Flyway.configure().dataSource(mysql.getJdbcUrl(),mysql.getUsername(),mysql.getPassword()).cleanDisabled(true);
        if(target!=null)c.target(target);return c.load();
    }
    @Test void upgradingPopulatedVersionSevenPreservesEveryExistingFieldAndRelation() throws Exception {
        flyway("7").migrate();JdbcTemplate jdbc=new JdbcTemplate(new DriverManagerDataSource(mysql.getJdbcUrl(),mysql.getUsername(),mysql.getPassword()));
        String user=UUID.randomUUID().toString(),file=UUID.randomUUID().toString(),paper=UUID.randomUUID().toString(),score=UUID.randomUUID().toString(),batch=UUID.randomUUID().toString(),legacy=UUID.randomUUID().toString();
        String course="afdac469-0fe4-5007-833c-51a71333967b",cycle="0ebdbbfe-d607-54b5-9c21-4e0adade5e4c";
        jdbc.update("INSERT INTO app_user(id,username,email,password_hash,role) VALUES(?,'migration-user','migration@example.com','test-hash','USER')",user);
        jdbc.update("INSERT INTO enrollment(user_id,cycle_id,course_id,paid) VALUES(?,?,?,true)",user,cycle,course);
        jdbc.update("INSERT INTO stored_file(id,purpose,storage_key,original_name,mime_type,size_bytes,sha256,state) VALUES(?,'PAPER',?,'保留.pdf','application/pdf',20,?,'DELETE_PENDING')",file,"managed/"+file,"a".repeat(64));
        jdbc.update("INSERT INTO paper(id,course_id,paper_month,question_file_id) VALUES(?,?,'2025-01-01',?)",paper,course,file);
        jdbc.update("INSERT INTO score_record(id,user_id,cycle_id,course_id,paper_id,practiced_on,score,minutes,limit_minutes,complete,closed_book,answers_seen_before,source,note,created_at,updated_at,revision) VALUES(?,?,?,?,?,'2025-02-01',73.25,120,150,true,true,NULL,'LEGACY','原始未知资格',UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),4)",score,user,cycle,course,paper);
        jdbc.update("INSERT INTO legacy_import_batch(id,source_sha,target_user_id,manifest_sha256,state,created_at) VALUES(?,?,?,?,'IMPORTED',UTC_TIMESTAMP(6))",batch,"b".repeat(64),user,"c".repeat(64));
        jdbc.update("INSERT INTO legacy_record(id,batch_id,user_id,old_owner,old_id,old_kind,payload,source_hash) VALUES(?,?,?,'old','42','grading',?,?)",legacy,batch,user,"{\"score\":78,\"status\":\"old_done\",\"answersSeenBefore\":null}","d".repeat(64));
        String release=jdbc.queryForObject("SELECT id FROM content_release WHERE course_id=? AND state='PUBLISHED'",String.class,course);
        var question=jdbc.queryForMap("SELECT r.id revision,q.id question FROM question_revision r JOIN question q ON q.id=r.question_id WHERE q.course_id=? AND r.release_id=? ORDER BY r.id LIMIT 1",course,release);
        jdbc.update("INSERT INTO practice_submission(id,user_id,question_id,revision_id,selected_option,correct,submitted_at,idempotency_key) VALUES(?,?,?,?,0,false,UTC_TIMESTAMP(6),?)",UUID.randomUUID().toString(),user,question.get("question"),question.get("revision"),UUID.randomUUID().toString());
        String session=UUID.randomUUID().toString(),policy=jdbc.queryForObject("SELECT id FROM assessment_policy WHERE release_id=?",String.class,release);
        jdbc.update("INSERT INTO assessment_session(id,user_id,course_id,release_id,policy_id,kind,status,started_at,deadline_at,submitted_at,question_count,correct_count,pass_score,passed,policy_snapshot,idempotency_key) VALUES(?,?,?,?,?,'MOCK','SUBMITTED','2025-02-01 00:00:00','2025-02-01 01:00:00','2025-02-01 00:30:00',1,1,80,true,'{}',?)",session,user,course,release,policy,UUID.randomUUID().toString());
        jdbc.update("INSERT INTO assessment_pass(id,user_id,session_id,course_id,kind,release_id,source,granted_at) VALUES(?,?,?,?,'MOCK',?,'ASSESSMENT',UTC_TIMESTAMP(6))",UUID.randomUUID().toString(),user,session,course,release);
        String plan=UUID.randomUUID().toString(),task=UUID.randomUUID().toString();
        String config="{\"cycleId\":\""+cycle+"\",\"startDate\":\"2025-02-01\",\"endDate\":\"2025-03-07\",\"strategy\":\"WEEKLY_35\",\"courseScope\":[\""+course+"\"],\"coursePriority\":[\""+course+"\"],\"dayCapacities\":[]}";
        jdbc.update("INSERT INTO learning_plan(id,user_id,cycle_id,start_date,end_date,config_snapshot,created_at) VALUES(?,?,?,'2025-02-01','2025-03-07',?,UTC_TIMESTAMP(6))",plan,user,cycle,config);
        jdbc.update("INSERT INTO plan_revision(plan_id,revision_no,snapshot,confirmed_at) VALUES(?,1,?,UTC_TIMESTAMP(6))",plan,"{\"config\":"+config+"}");
        jdbc.update("INSERT INTO plan_current_revision(plan_id,revision_no) VALUES(?,1)",plan);
        jdbc.update("INSERT INTO plan_task(id,plan_id,course_id,title_snapshot,estimated_minutes,release_id,kind) VALUES(?,?,?,'旧默认复习任务',60,?,'REVIEW')",task,plan,course,release);
        jdbc.update("INSERT INTO plan_task_segment(id,plan_id,revision_no,task_id,scheduled_on,minutes,state,sort_order) VALUES(?,?,1,?,'2025-02-01',60,'SCHEDULED',0)",UUID.randomUUID().toString(),plan,task);
        Path inventory=Path.of("target/refactor-evidence/original-inventory.tsv");Files.createDirectories(inventory.getParent());Files.deleteIfExists(inventory);inventory("capture",inventory);
        List<String> tables=jdbc.queryForList("SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name<>'flyway_schema_history' ORDER BY table_name",String.class);
        Map<String,List<String>> columns=new TreeMap<>();Map<String,List<String>> before=new TreeMap<>();
        for(String table:tables) {
            var names=jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=? ORDER BY ordinal_position",String.class,table);
            columns.put(table,names);before.put(table,canonical(jdbc,table,names));
        }
        flyway(null).migrate();inventory("compare",inventory);Map<String,Object> evidence=new LinkedHashMap<>();
        for(String table:tables) {
            var after=canonical(jdbc,table,columns.get(table));assertThat(after).as(table+" original fields").isEqualTo(before.get(table));evidence.put(table,Map.of("beforeCount",before.get(table).size(),"afterCount",after.size(),"originalFieldsUnchanged",true));
        }
        assertThat(jdbc.queryForObject("SELECT answers_seen_before FROM score_record_revision WHERE score_id=?",Boolean.class,score)).isNull();
        assertThat(jdbc.queryForObject("SELECT capture_kind FROM score_record_revision WHERE score_id=?",String.class,score)).isEqualTo("MIGRATION_BASELINE");
        assertThat(jdbc.queryForObject("SELECT paper_key_snapshot FROM score_record WHERE id=?",String.class,score)).isEqualTo("00023:2025-01");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM file_cleanup_task WHERE file_id=?",Integer.class,file)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM assessment_pass WHERE user_id=?",Integer.class,user)).isEqualTo(1);
        Files.createDirectories(Path.of("target/refactor-evidence"));new ObjectMapper().writeValue(Path.of("target/refactor-evidence/migration.json").toFile(),evidence);
        flyway(null).validate();assertThat(flyway(null).migrate().migrationsExecuted).isZero();
    }
    private void inventory(String action,Path path)throws Exception {
        var driver=Path.of(com.mysql.cj.jdbc.Driver.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        var builder=new ProcessBuilder("java","--class-path",driver.toString(),"scripts/MigrationInventory.java",action,path.toString()).redirectErrorStream(true);
        builder.environment().put("DB_URL",mysql.getJdbcUrl());builder.environment().put("DB_USER",mysql.getUsername());builder.environment().put("DB_PASSWORD",mysql.getPassword());
        var run=builder.start();String output=new String(run.getInputStream().readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);assertThat(run.waitFor()).withFailMessage(output).isZero();
    }
    private List<String> canonical(JdbcTemplate jdbc,String table,List<String> columns) {
        String select=columns.stream().map(c->"`"+c+"`").collect(java.util.stream.Collectors.joining(","));
        return jdbc.query("SELECT "+select+" FROM `"+table+"`",(r,i)->{
            List<String> row=new ArrayList<>();for(int c=1;c<=columns.size();c++){Object value=r.getObject(c);row.add(value==null?"<SQL NULL>":value.toString());}return row.toString();
        }).stream().sorted().toList();
    }
}
