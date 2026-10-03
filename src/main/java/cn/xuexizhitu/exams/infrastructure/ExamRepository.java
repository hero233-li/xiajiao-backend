package cn.xuexizhitu.exams.infrastructure;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.exams.domain.ExamTypes.*;
import cn.xuexizhitu.files.domain.FileTypes.Metadata;
import cn.xuexizhitu.files.infrastructure.FileRepository;
import cn.xuexizhitu.learning.api.LearningDtos.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.time.*;
import java.util.*;
@Repository @RequiredArgsConstructor public class ExamRepository {
    private final JdbcTemplate jdbc;
    private static Instant instant(ResultSet r,String name)throws SQLException {
        var t=r.getObject(name,LocalDateTime.class);
        return t==null?null:t.toInstant(ZoneOffset.UTC);
    }
    private static ScoreData scoreRow(ResultSet r)throws SQLException {
        return new ScoreData(r.getString("id"),r.getString("user_id"),r.getString("cycle_id"),r.getString("course_id"),r.getString("paper_id"),                 r.getString("paper_key_snapshot"),r.getDate("practiced_on").toLocalDate(),                 r.getBigDecimal("score"),r.getInt("minutes"),r.getInt("limit_minutes"),r.getBoolean("complete"),r.getBoolean("closed_book"),                 r.getObject("answers_seen_before")==null?null:r.getBoolean("answers_seen_before"),r.getString("source"),r.getString("note"),                 instant(r,"created_at"),instant(r,"updated_at"),r.getLong("revision"));
    }
    private static final String SCORES=" FROM score_record s JOIN paper p ON p.id=s.paper_id JOIN course c ON c.id=s.course_id";
    public List<ScoreData> records(String user,String course) {
        return jdbc.query("SELECT s.*,p.paper_month,c.code"+SCORES+" WHERE s.user_id=? AND s.course_id=? ORDER BY s.practiced_on,s.created_at,s.id",(r,i)->scoreRow(r),user,course);
    }
    public ScoreData owned(String user,String course,String id) {
        return jdbc.query("SELECT s.*,p.paper_month,c.code"+SCORES+" WHERE s.id=? AND s.user_id=? AND s.course_id=?",(r,i)->scoreRow(r),id,user,course)                 .stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    public record Selection(List<String> ids,long total) {
    }
    public Selection selected(String user,String course,String cycle,String paper,int page,int size) {
        String sql=" FROM score_record WHERE user_id=? AND course_id=?";
        List<Object> args=new ArrayList<>(List.of(user,course));
        if(cycle!=null) {
            sql+=" AND cycle_id=?";
            args.add(cycle);
        }
        if(paper!=null) {
            sql+=" AND paper_id=?";
            args.add(paper);
        }
        long count=jdbc.queryForObject("SELECT COUNT(*)"+sql,Long.class,args.toArray());
        args.add(size);
        args.add((page-1L)*size);
        return new Selection(jdbc.queryForList("SELECT id"+sql+" ORDER BY practiced_on DESC,created_at DESC,id DESC LIMIT ? OFFSET ?",String.class,args.toArray()),count);
    }
    public Map<String,List<Metadata>> images(String user,List<String> scores) {
        Map<String,List<Metadata>> result=new HashMap<>();
        if(scores.isEmpty())return result;
        List<Object> args=new ArrayList<>();
        args.add(user);
        args.addAll(scores);
        String in=String.join(",",Collections.nCopies(scores.size(),"?"));
        jdbc.query("SELECT f.*,i.score_record_id FROM score_record_image i JOIN stored_file f ON f.id=i.file_id WHERE i.user_id=? AND i.score_record_id IN ("+in+") AND f.state='ACTIVE' ORDER BY i.score_record_id,i.sort_order,i.file_id",r-> {
            result.computeIfAbsent(r.getString("score_record_id"),k->new ArrayList<>()).add(FileRepository.map(r).metadata());
        }
        ,args.toArray());
        return result;
    }
    private static PaperData paperRow(ResultSet r)throws SQLException {
        return new PaperData(r.getString("id"),r.getString("course_id"),r.getString("code"),r.getDate("paper_month").toLocalDate(),r.getString("source_course_code"),                 r.getString("question_file_id"),r.getString("answer_file_id"),(Integer)r.getObject("question_pages"),(Integer)r.getObject("answer_pages"),r.getString("note"));
    }
    public PaperData paper(String course,String id) {
        return jdbc.query("SELECT p.*,c.code FROM paper p JOIN course c ON c.id=p.course_id WHERE p.id=? AND p.course_id=?",(r,i)->paperRow(r),id,course)                 .stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    public Page<PaperData> papers(String course,int page,int size) {
        var rows=jdbc.query("SELECT p.*,c.code FROM paper p JOIN course c ON c.id=p.course_id WHERE p.course_id=? ORDER BY p.paper_month DESC,p.id LIMIT ? OFFSET ?",(r,i)->paperRow(r),course,size,(page-1L)*size);
        return new Page<>(rows,page,size,jdbc.queryForObject("SELECT COUNT(*) FROM paper WHERE course_id=?",Long.class,course));
    }
    public void create(String id,String user,String course,CreateScore w) {
        PaperData paper=paper(course,w.paperId().toString());
        ScoreFields f=w.fields();
        jdbc.update("INSERT INTO score_record(id,user_id,cycle_id,course_id,paper_id,practiced_on,score,minutes,limit_minutes,complete,closed_book,answers_seen_before,source,note,created_at,updated_at,paper_key_snapshot) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,'MANUAL',?,UTC_TIMESTAMP(6),UTC_TIMESTAMP(6),?)",                 id,user,w.cycleId().toString(),course,w.paperId().toString(),f.practicedOn(),f.score(),f.minutes(),f.limitMinutes(),f.complete(),f.closedBook(),f.answersSeenBefore(),f.note(),paper.code()+":"+paper.paperMonth().toString().substring(0,7));
    }
    public void update(String id,String user,UpdateScore w) {
        ScoreFields f=w.fields();
        int changed=jdbc.update("UPDATE score_record SET practiced_on=?,score=?,minutes=?,limit_minutes=?,complete=?,closed_book=?,answers_seen_before=?,note=?,updated_at=UTC_TIMESTAMP(6),revision=revision+1 WHERE id=? AND user_id=? AND revision=?",                 f.practicedOn(),f.score(),f.minutes(),f.limitMinutes(),f.complete(),f.closedBook(),f.answersSeenBefore(),f.note(),id,user,w.expectedRevision());
        if(changed!=1)throw new BusinessException(ErrorCode.REVISION_CONFLICT,"成绩修订号冲突");
    }
    public void capture(String id,String user,String kind) {
        jdbc.update("INSERT INTO score_record_revision(score_id,revision,user_id,cycle_id,course_id,paper_id,practiced_on,score,minutes,limit_minutes,complete,closed_book,answers_seen_before,source,note,created_at,updated_at,paper_key_snapshot,actor_id,capture_kind) SELECT id,revision,user_id,cycle_id,course_id,paper_id,practiced_on,score,minutes,limit_minutes,complete,closed_book,answers_seen_before,source,note,created_at,updated_at,paper_key_snapshot,?,? FROM score_record WHERE id=? AND user_id=?",user,kind,id,user);
    }
    public Page<Revision> revisions(String user,String course,String id,int page,int size) {
        owned(user,course,id);
        var rows=jdbc.query("SELECT h.*,h.score_id AS id,p.paper_month,c.code FROM score_record_revision h JOIN paper p ON p.id=h.paper_id JOIN course c ON c.id=h.course_id WHERE h.score_id=? AND h.user_id=? ORDER BY h.revision DESC LIMIT ? OFFSET ?",                 (r,i)->new Revision(scoreRow(r),r.getString("capture_kind"),r.getString("actor_id"),instant(r,"captured_at")),id,user,size,(page-1L)*size);
        return new Page<>(rows,page,size,jdbc.queryForObject("SELECT COUNT(*) FROM score_record_revision WHERE score_id=? AND user_id=?",Long.class,id,user));
    }
    public boolean paperMonthExists(String course,String month,String except) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM paper WHERE course_id=? AND paper_month=? AND id<>?",Integer.class,course,month+"-01",except==null?"":except)>0;
    }
    public void writePaper(String course,String id,cn.xuexizhitu.contract.WorkflowDtos.PaperWrite w,boolean create) {
        if(create)jdbc.update("INSERT INTO paper(id,course_id,paper_month,source_course_code,question_file_id,answer_file_id,question_pages,answer_pages,note) VALUES(?,?,?,?,?,?,?,?,?)",id,course,w.paperMonth()+"-01",w.sourceCourseCode(),w.questionFileId().toString(),w.answerFileId()==null?null:w.answerFileId().toString(),w.questionPages(),w.answerPages(),w.note());
        else jdbc.update("UPDATE paper SET paper_month=?,source_course_code=?,question_file_id=?,answer_file_id=?,question_pages=?,answer_pages=?,note=? WHERE id=? AND course_id=?",w.paperMonth()+"-01",w.sourceCourseCode(),w.questionFileId().toString(),w.answerFileId()==null?null:w.answerFileId().toString(),w.questionPages(),w.answerPages(),w.note(),id,course);
    }
    public void confirmOverride(String id,String user,String course,String cycle,LocalDate exam) {
        jdbc.update("INSERT INTO unlock_override(id,user_id,course_id,cycle_id,confirmed_at,exam_date_snapshot) VALUES(?,?,?,?,UTC_TIMESTAMP(6),?)",id,user,course,cycle,exam);
    }
    public void revokeOverride(String id) {
        jdbc.update("UPDATE unlock_override SET revoked_at=UTC_TIMESTAMP(6),revision=revision+1 WHERE id=?",id);
    }
    public void attach(String user,String score,String file) {
        int order=jdbc.queryForObject("SELECT COALESCE(MAX(sort_order),-1)+1 FROM score_record_image WHERE score_record_id=?",Integer.class,score);
        jdbc.update("INSERT INTO score_record_image(user_id,score_record_id,file_id,sort_order) VALUES(?,?,?,?)",user,score,file,order);
    }
    public boolean hasImage(String user,String score,String file) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM score_record_image WHERE user_id=? AND score_record_id=? AND file_id=?",Integer.class,user,score,file)>0;
    }
}
