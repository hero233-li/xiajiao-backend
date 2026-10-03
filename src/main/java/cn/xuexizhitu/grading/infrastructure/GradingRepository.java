package cn.xuexizhitu.grading.infrastructure;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.grading.domain.GradingTypes.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.time.*;
import java.util.*;
@Repository @RequiredArgsConstructor public class GradingRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        }
        catch(Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
    public <T> T decode(String value,Class<T> type) {
        if(value==null)return null;
        try {
            return json.readValue(value,type);
        }
        catch(Exception e) {
            throw new IllegalStateException("Invalid stored grading document",e);
        }
    }
    private static Instant instant(ResultSet r,String field)throws SQLException {
        var t=r.getTimestamp(field);
        return t==null?null:t.toLocalDateTime().toInstant(ZoneOffset.UTC);
    }
    public void lockUser(String user) {
        jdbc.queryForObject("SELECT id FROM app_user WHERE id=? AND enabled=true FOR UPDATE",String.class,user);
    }
    public Submission submission(String user,String id,boolean lock) {
        return jdbc.query("SELECT * FROM grading_submission WHERE id=? AND user_id=?"+(lock?" FOR UPDATE":""),(r,i)->new Submission(r.getString("id"),r.getString("course_id"),r.getString("cycle_id"),r.getString("paper_id"),r.getLong("revision"),List.of()),id,user).stream().findFirst().map(s->new Submission(s.id(),s.courseId(),s.cycleId(),s.paperId(),s.revision(),pages(s.id()))).orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    public List<Submission> submissions(String user,String course,String cycle) {
        var rows=jdbc.query("SELECT * FROM grading_submission WHERE user_id=? AND course_id=? AND cycle_id=? ORDER BY created_at DESC,id DESC LIMIT 100",(r,i)->new Submission(r.getString("id"),r.getString("course_id"),r.getString("cycle_id"),r.getString("paper_id"),r.getLong("revision"),List.of()),user,course,cycle);
        if(rows.isEmpty())return rows;
        Map<String,List<Page>> grouped=new HashMap<>();
        var ids=rows.stream().map(Submission::id).toArray();
        jdbc.query("SELECT f.*,p.submission_id,p.page_no FROM grading_submission_page p JOIN stored_file f ON f.id=p.file_id WHERE p.submission_id IN ("+String.join(",",Collections.nCopies(ids.length,"?"))+") ORDER BY p.page_no",(org.springframework.jdbc.core.RowCallbackHandler)r->grouped.computeIfAbsent(r.getString("submission_id"),key->new ArrayList<>()).add(new Page(r.getString("id"),r.getInt("page_no"),r.getString("original_name"),r.getString("mime_type"),r.getLong("size_bytes"),r.getString("sha256"))),ids);
        return rows.stream().map(row->new Submission(row.id(),row.courseId(),row.cycleId(),row.paperId(),row.revision(),grouped.getOrDefault(row.id(),List.of()))).toList();
    }
    public List<Page> pages(String submission) {
        return jdbc.query("SELECT f.*,p.page_no FROM grading_submission_page p JOIN stored_file f ON f.id=p.file_id WHERE p.submission_id=? ORDER BY p.page_no",(r,i)->new Page(r.getString("id"),r.getInt("page_no"),r.getString("original_name"),r.getString("mime_type"),r.getLong("size_bytes"),r.getString("sha256")),submission);
    }
    public void createSubmission(String id,String user,SubmissionWrite w) {
        jdbc.update("INSERT INTO grading_submission(id,user_id,course_id,cycle_id,paper_id) VALUES(?,?,?,?,?)",id,user,w.courseId().toString(),w.cycleId().toString(),w.paperId().toString());
    }
    public void addPage(String id,String file,int page) {
        jdbc.update("INSERT INTO grading_submission_page(submission_id,file_id,page_no) VALUES(?,?,?)",id,file,page);
        bump(id);
    }
    public void order(String id,List<UUID> files) {
        jdbc.update("DELETE FROM grading_submission_page WHERE submission_id=?",id);
        int n=0;
        for(UUID file:files) jdbc.update("INSERT INTO grading_submission_page VALUES(?,?,?)",id,file.toString(),++n);
        bump(id);
    }
    private void bump(String id) {
        jdbc.update("UPDATE grading_submission SET revision=revision+1 WHERE id=?",id);
    }
    public void removePage(String id,String file,List<UUID> remaining) {
        order(id,remaining);
    }
    public boolean pinned(String file) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM grading_task_page WHERE file_id=?",Integer.class,file)>0;
    }
    private Rubric rubricRow(ResultSet r)throws SQLException {
        return new Rubric(r.getString("id"),r.getString("paper_id"),r.getInt("version"),r.getString("state"),decode(r.getString("document"),RubricDocument.class));
    }
    public List<Rubric> rubrics(String paper) {
        return jdbc.query("SELECT * FROM grading_rubric WHERE paper_id=? ORDER BY version DESC LIMIT 100",(r,i)->rubricRow(r),paper);
    }
    public Rubric published(String paper) {
        return jdbc.query("SELECT * FROM grading_rubric WHERE paper_id=? AND state='PUBLISHED' ORDER BY version DESC LIMIT 1",(r,i)->rubricRow(r),paper).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.INVALID_RELATION,"没有已发布评分标准，请先生成或录入草稿并核对发布"));
    }
    public Rubric rubric(String id,boolean lock) {
        return jdbc.query("SELECT * FROM grading_rubric WHERE id=?"+(lock?" FOR UPDATE":""),(r,i)->rubricRow(r),id).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    public String createRubric(String user,String paper,RubricDocument doc) {
        jdbc.queryForObject("SELECT id FROM paper WHERE id=? FOR UPDATE",String.class,paper);
        int version=jdbc.queryForObject("SELECT COALESCE(MAX(version),0)+1 FROM grading_rubric WHERE paper_id=?",Integer.class,paper);
        String id=BusinessData.uuid();
        jdbc.update("INSERT INTO grading_rubric(id,paper_id,version,document,created_by) VALUES(?,?,?,?,?)",id,paper,version,encode(doc),user);
        return id;
    }
    public void publish(String id) {
        jdbc.update("UPDATE grading_rubric SET state='PUBLISHED',published_at=UTC_TIMESTAMP(6) WHERE id=?",id);
    }
    public void updateRubric(String id,RubricDocument doc) {
        jdbc.update("UPDATE grading_rubric SET document=? WHERE id=? AND state='DRAFT'",encode(doc),id);
    }
    public void createTask(String id,String user,String paper,String submission,Long revision,String rubric,String kind,Inputs input) {
        jdbc.update("INSERT INTO grading_task(id,user_id,paper_id,submission_id,submission_revision,rubric_id,kind,input_document) VALUES(?,?,?,?,?,?,?,?)",id,user,paper,submission,revision,rubric,kind,encode(input));
        for(Page page:input.pages())jdbc.update("INSERT INTO grading_task_page(task_id,file_id,page_no) VALUES(?,?,?)",id,page.fileId(),page.pageNo());
    }
    public Optional<String> duplicate(String submission,long revision) {
        return jdbc.queryForList("SELECT id FROM grading_task WHERE submission_id=? AND submission_revision=? AND state IN ('QUEUED','GRADING','REVIEW','COMPLETED') ORDER BY created_at DESC LIMIT 1",String.class,submission,revision).stream().findFirst();
    }
    public Optional<String> duplicateDraft(String user,String paper) {
        return jdbc.queryForList("SELECT id FROM grading_task WHERE user_id=? AND paper_id=? AND kind='RUBRIC' AND state IN ('QUEUED','GRADING') LIMIT 1",String.class,user,paper).stream().findFirst();
    }
    public record TaskRow(String id,String user,String paper,String submission,String kind,String state,Inputs input,String worker,String lease,Instant leaseUntil,Instant deadline,int attempts,Result result,RubricDocument draft,String model,String score,String hash,String error,String rubricId) {
    }
    public TaskRow task(String user,String id,boolean lock) {
        return jdbc.query("SELECT * FROM grading_task WHERE id=? AND user_id=?"+(lock?" FOR UPDATE":""),(r,i)->taskRow(r),id,user).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    private TaskRow taskRow(ResultSet r)throws SQLException {
        return new TaskRow(r.getString("id"),r.getString("user_id"),r.getString("paper_id"),r.getString("submission_id"),r.getString("kind"),r.getString("state"),decode(r.getString("input_document"),Inputs.class),r.getString("worker_id"),r.getString("lease_token"),instant(r,"lease_until"),instant(r,"deadline"),r.getInt("attempts"),r.getString("kind").equals("GRADE")?decode(r.getString("result_document"),Result.class):null,r.getString("kind").equals("RUBRIC")?decode(r.getString("result_document"),RubricDocument.class):null,r.getString("model"),r.getString("score_id"),r.getString("result_hash"),r.getString("error_message"),r.getString("rubric_id"));
    }
    public List<TaskRow> tasks(String user,String submission) {
        return jdbc.query("SELECT * FROM grading_task WHERE user_id=? AND submission_id=? ORDER BY created_at DESC,id DESC LIMIT 100",(r,i)->taskRow(r),user,submission);
    }
    public void recover(String user) {
        jdbc.update("UPDATE grading_task SET state=IF(attempts>=3,'FAILED','QUEUED'),worker_id=NULL,lease_token=NULL,lease_until=NULL,deadline=NULL,error_message=IF(attempts>=3,'处理已中断三次，请重新申请',NULL),updated_at=UTC_TIMESTAMP(6) WHERE user_id=? AND state='GRADING' AND (lease_until<UTC_TIMESTAMP(6) OR deadline<UTC_TIMESTAMP(6))",user);
    }
    public Optional<String> claimId(String user) {
        return jdbc.queryForList("SELECT id FROM grading_task WHERE user_id=? AND state='QUEUED' ORDER BY created_at,id LIMIT 1 FOR UPDATE SKIP LOCKED",String.class,user).stream().findFirst();
    }
    public void claim(String id,String worker,String lease) {
        jdbc.update("UPDATE grading_task SET state='GRADING',worker_id=?,lease_token=?,lease_until=DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 120 SECOND),deadline=DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 15 MINUTE),attempts=attempts+1,error_message=NULL,updated_at=UTC_TIMESTAMP(6) WHERE id=?",worker,lease,id);
    }
    public void renew(String id) {
        jdbc.update("UPDATE grading_task SET lease_until=LEAST(deadline,DATE_ADD(UTC_TIMESTAMP(6),INTERVAL 120 SECOND)),updated_at=UTC_TIMESTAMP(6) WHERE id=?",id);
    }
    public void result(String id,String state,Object result,String model,String hash,String score) {
        jdbc.update("UPDATE grading_task SET state=?,result_document=?,model_result_document=COALESCE(model_result_document,CAST(? AS JSON)),model=?,result_hash=?,score_id=?,updated_at=UTC_TIMESTAMP(6) WHERE id=?",state,encode(result),encode(result),model,hash,score,id);
    }
    public void fail(String id,String reason,boolean retry) {
        jdbc.update("UPDATE grading_task SET state=?,error_message=?,lease_until=NULL,deadline=NULL,updated_at=UTC_TIMESTAMP(6) WHERE id=?",retry?"QUEUED":"FAILED",reason,id);
    }
    public void pair(String id,String user,String hash,String label) {
        jdbc.update("INSERT INTO grading_worker(id,user_id,token_hash,label) VALUES(?,?,?,?)",id,user,hash,label);
    }
    public WorkerPrincipal authenticate(String hash) {
        return jdbc.query("SELECT w.id,w.user_id FROM grading_worker w JOIN app_user u ON u.id=w.user_id WHERE w.token_hash=? AND w.revoked_at IS NULL AND u.enabled=true",(r,i)->new WorkerPrincipal(r.getString("id"),r.getString("user_id")),hash).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.UNAUTHORIZED,"本地批改配对凭证已失效"));
    }
    public void requireWorker(WorkerPrincipal p) {
        if(jdbc.queryForList("SELECT id FROM grading_worker WHERE id=? AND user_id=? AND revoked_at IS NULL FOR UPDATE",String.class,p.id(),p.userId()).isEmpty())throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    public void heartbeat(String id,Heartbeat h) {
        jdbc.update("UPDATE grading_worker SET last_seen=UTC_TIMESTAMP(6),paused=?,reason=? WHERE id=?",h.paused(),h.reason(),id);
    }
    public List<Worker> workers(String user) {
        return jdbc.query("SELECT *,last_seen>=DATE_SUB(UTC_TIMESTAMP(6),INTERVAL 90 SECOND) AS online FROM grading_worker WHERE user_id=? ORDER BY created_at DESC LIMIT 100",(r,i)->new Worker(r.getString("id"),r.getString("label"),r.getBoolean("online")&&r.getTimestamp("revoked_at")==null,r.getBoolean("paused"),r.getString("reason"),r.getTimestamp("revoked_at")!=null),user);
    }
    public void revoke(String user,String id) {
        int n=jdbc.update("UPDATE grading_worker SET revoked_at=UTC_TIMESTAMP(6) WHERE id=? AND user_id=?",id,user);
        if(n==0)throw new BusinessException(ErrorCode.NOT_FOUND);
    }
}
