package cn.xuexizhitu.repository;
import cn.xuexizhitu.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.*;
import java.util.*;
@Repository @RequiredArgsConstructor
public class AuditRepository {
    private final JdbcTemplate jdbc;private final ObjectMapper mapper;
    public void add(String action,String type,String target,String reason,Map<String,Object> details){try{jdbc.update("INSERT INTO audit_event(id,actor_id,action,target_type,target_id,reason,details,occurred_at) VALUES(?,?,?,?,?,?,?,?)",UUID.randomUUID().toString(),CurrentUser.idOrThrow(),action,type,target,reason,mapper.writeValueAsString(details),LocalDateTime.now(ZoneOffset.UTC));}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}}
}
