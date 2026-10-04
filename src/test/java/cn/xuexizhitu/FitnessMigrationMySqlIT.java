package cn.xuexizhitu;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
@Testcontainers
class FitnessMigrationMySqlIT {
 @Container static MySQLContainer<?> mysql=new MySQLContainer<>("mysql:8.4").withDatabaseName("fitness_upgrade");
 @Test void v11PreservesExistingUsersEnrollmentProgressAndExecutedMigrationChecksums(){
  var ds=new DriverManagerDataSource(mysql.getJdbcUrl(),mysql.getUsername(),mysql.getPassword());
  Flyway.configure().dataSource(ds).locations("classpath:db/migration").target("10").load().migrate();JdbcTemplate jdbc=new JdbcTemplate(ds);
  String user=UUID.randomUUID().toString();jdbc.update("INSERT INTO app_user(id,username,email,password_hash,role) VALUES(?,?,?,?,?)",user,"migration_user","migration@example.com","preserved_hash","ADMIN");
  jdbc.update("INSERT INTO enrollment(user_id,cycle_id,course_id,paid) VALUES(?,?,?,true)",user,"0ebdbbfe-d607-54b5-9c21-4e0adade5e4c","afdac469-0fe4-5007-833c-51a71333967b");
  var before=new LinkedHashMap<String,List<Map<String,Object>>>();for(String table:List.of("app_user","login_identifier","enrollment","course","chapter","study_item","user_item_progress","user_knowledge_note","learning_plan","score_record"))before.put(table,jdbc.queryForList("SELECT * FROM "+table));
  var checksums=jdbc.queryForList("SELECT version,checksum FROM flyway_schema_history WHERE success=true ORDER BY installed_rank");
  Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
  before.forEach((table,rows)->assertThat(jdbc.queryForList("SELECT * FROM "+table)).as(table).isEqualTo(rows));assertThat(jdbc.queryForList("SELECT version,checksum FROM flyway_schema_history WHERE success=true AND version<>'11' ORDER BY installed_rank")).isEqualTo(checksums);
  for(String table:List.of("fitness_record","fitness_goal_state","fitness_goal_event"))assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class)).isZero();
 }
}
