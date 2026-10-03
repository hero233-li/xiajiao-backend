package cn.xuexizhitu.files.infrastructure;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.files.domain.FileTypes.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.util.*;
@Repository @RequiredArgsConstructor public class FileRepository {
    private final JdbcTemplate jdbc;
    public static Stored map(ResultSet r) throws SQLException {
        return new Stored(r.getString("id"), r.getString("owner_id"), r.getString("storage_key"),                 new Metadata(r.getString("id"), r.getString("original_name"), r.getString("mime_type"),                         r.getLong("size_bytes"), r.getString("sha256"), r.getBoolean("contains_answers"),                         r.getString("state"), r.getString("purpose")));
    }
    public Optional<Stored> find(String id) {
        return jdbc.query("SELECT * FROM stored_file WHERE id=?", (r,i)->map(r), id).stream().findFirst();
    }
    public Stored required(String id) {
        return find(id).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    public void insert(Stored row) {
        Metadata m=row.metadata();
        jdbc.update("INSERT INTO stored_file(id,owner_id,purpose,storage_key,original_name,mime_type,size_bytes,sha256,contains_answers,state) VALUES(?,?,?,?,?,?,?,?,?,'ACTIVE')",                 row.id(), row.ownerId(), m.purpose(), row.storageKey(), m.name(), m.mimeType(), m.sizeBytes(), m.sha256(), m.containsAnswers());
    }
    public void delete(String id) {
        jdbc.update("UPDATE stored_file SET state='DELETE_PENDING' WHERE id=? AND state='ACTIVE'", id);
        jdbc.update("INSERT IGNORE INTO file_cleanup_task(file_id) SELECT id FROM stored_file WHERE id=? AND state='DELETE_PENDING'", id);
    }
    public record Cleanup(String fileId, String storageKey, String leaseToken, int attempts) {
    }
    public List<String> pending() {
        // Includes deletions written by a previous application version.
        jdbc.update("INSERT IGNORE INTO file_cleanup_task(file_id) SELECT id FROM stored_file WHERE state='DELETE_PENDING'");
        return jdbc.queryForList("SELECT t.file_id FROM file_cleanup_task t JOIN stored_file f ON f.id=t.file_id WHERE f.state='DELETE_PENDING' AND t.next_attempt_at<=UTC_TIMESTAMP(6) AND (t.lease_until IS NULL OR t.lease_until<=UTC_TIMESTAMP(6)) ORDER BY t.next_attempt_at,t.file_id LIMIT 100", String.class);
    }
    public Cleanup claim(String id) {
        String token=UUID.randomUUID().toString();
        int changed=jdbc.update("UPDATE file_cleanup_task SET lease_token=?,lease_until=DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 2 MINUTE),attempts=attempts+1 WHERE file_id=? AND next_attempt_at<=UTC_TIMESTAMP(6) AND (lease_until IS NULL OR lease_until<=UTC_TIMESTAMP(6))", token,id);
        if(changed==0)return null;
        return jdbc.queryForObject("SELECT t.*,f.storage_key FROM file_cleanup_task t JOIN stored_file f ON f.id=t.file_id WHERE t.file_id=?", (r,i)->new Cleanup(id,r.getString("storage_key"),token,r.getInt("attempts")),id);
    }
    public void completed(Cleanup task) {
        if(jdbc.update("UPDATE file_cleanup_task SET completed_at=UTC_TIMESTAMP(6),lease_until=NULL,last_error=NULL WHERE file_id=? AND lease_token=?",task.fileId(),task.leaseToken())==1)             jdbc.update("UPDATE stored_file SET state='DELETED',deleted_at=UTC_TIMESTAMP(6) WHERE id=? AND state='DELETE_PENDING'",task.fileId());
    }
    public void failed(Cleanup task, String error) {
        int delay=Math.min(3600,30*(1<<Math.min(task.attempts(),6)));
        jdbc.update("UPDATE file_cleanup_task SET last_error=?,next_attempt_at=TIMESTAMPADD(SECOND,?,UTC_TIMESTAMP(6)),lease_until=NULL WHERE file_id=? AND lease_token=?",error,delay,task.fileId(),task.leaseToken());
    }
    public void reconcile(String id,String category) {
        jdbc.update("INSERT INTO file_reconciliation(file_id,category,observed_at) VALUES(?,?,UTC_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE category=VALUES(category),observed_at=VALUES(observed_at)",id,category);
    }
    public cn.xuexizhitu.learning.api.LearningDtos.Page<Metadata> adminFiles(String purpose,int page,int size) {
        String where=" WHERE purpose IN ('PAPER','MANUAL')"+(purpose==null?"":" AND purpose=?");
        List<Object> args=new ArrayList<>();
        if(purpose!=null)args.add(purpose);
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM stored_file"+where,Long.class,args.toArray());
        args.add(size);
        args.add((page-1L)*size);
        return new cn.xuexizhitu.learning.api.LearningDtos.Page<>(jdbc.query("SELECT * FROM stored_file"+where+" ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",(r,i)->map(r).metadata(),args.toArray()),page,size,total);
    }
    public List<Stored> inventory(String after,int size) {
        return jdbc.query("SELECT * FROM stored_file WHERE id>? AND state='ACTIVE' AND storage_key LIKE 'managed/%' ORDER BY id LIMIT ?",(r,i)->map(r),after,size);
    }
}
