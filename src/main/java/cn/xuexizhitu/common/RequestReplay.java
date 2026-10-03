package cn.xuexizhitu.common;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.xuexizhitu.files.infrastructure.LocalPrivateStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
/** Caller holds the user lock; replay and mutation commit together. */ @Component @RequiredArgsConstructor public class RequestReplay {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public String hash(Object request) {
        try {
            return LocalPrivateStorage.hash(mapper.writeValueAsBytes(request));
        }
        catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
    public <T> T find(String user,String operation,String key,String hash,Class<T> type) {
        if(key==null)return null;
        var rows=jdbc.queryForList("SELECT request_hash,response_json FROM request_replay WHERE user_id=? AND operation=? AND request_key=?",user,operation,key);
        if(rows.isEmpty())return null;
        if(!hash.equals(rows.get(0).get("request_hash")))throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
        try {
            return mapper.readValue(rows.get(0).get("response_json").toString(),type);
        }
        catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
    public void save(String user,String operation,String key,String hash,Object result) {
        if(key==null)return;
        try {
            jdbc.update("INSERT INTO request_replay(user_id,operation,request_key,request_hash,response_json) VALUES(?,?,?,?,?)",user,operation,key,hash,mapper.writeValueAsString(result));
        }
        catch(com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
