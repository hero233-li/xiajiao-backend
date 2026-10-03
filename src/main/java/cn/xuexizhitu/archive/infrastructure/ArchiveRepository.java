package cn.xuexizhitu.archive.infrastructure;
import cn.xuexizhitu.archive.domain.ArchiveTypes.*;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.learning.api.LearningDtos.Page;
import cn.xuexizhitu.contract.WorkflowDtos.*;
import cn.xuexizhitu.files.domain.FileTypes.Metadata;
import cn.xuexizhitu.files.infrastructure.FileRepository;
import com.fasterxml.jackson.databind.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.time.*;
import java.util.*;
@Repository @RequiredArgsConstructor public class ArchiveRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private static final String RECORDS=" FROM (SELECT r.*,CASE WHEN r.old_kind IN ('ability','grading','sprint','practice','task','practice_request','other') THEN r.old_kind ELSE 'other' END resolved_kind,COALESCE((SELECT v.course_id FROM legacy_pass_review v WHERE v.legacy_record_id=r.id ORDER BY v.id LIMIT 1),c.id) resolved_course FROM legacy_record r LEFT JOIN course c ON c.id=JSON_UNQUOTE(JSON_EXTRACT(r.payload,'$.courseId')) WHERE r.user_id=?) r";
    private JsonNode json(String value) {
        try {
            return mapper.readTree(value);
        }
        catch(com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Invalid archived payload",e);
        }
    }
    private StoredRecord record(ResultSet r)throws SQLException {
        return new StoredRecord(r.getString("id"),r.getString("old_id"),r.getString("resolved_kind"),r.getString("resolved_course"),json(r.getString("payload")),r.getInt("file_count"));
    }
    private static final String RECORD_SELECT="SELECT r.*,(SELECT COUNT(*) FROM legacy_file_link l JOIN stored_file f ON f.id=l.file_id WHERE l.legacy_record_id=r.id AND f.state='ACTIVE') file_count";
    public StoredRecord owned(String user,String id) {
        return jdbc.query(RECORD_SELECT+RECORDS+" WHERE r.id=?",(r,i)->record(r),user,id).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    public Page<StoredRecord> records(String user,String kind,String course,int page,int size) {
        String where=" WHERE 1=1";
        List<Object> args=new ArrayList<>(List.of(user));
        if(kind!=null) {
            where+=" AND r.resolved_kind=?";
            args.add(kind);
        }
        if(course!=null) {
            where+=" AND r.resolved_course=?";
            args.add(course);
        }
        long total=jdbc.queryForObject("SELECT COUNT(*)"+RECORDS+where,Long.class,args.toArray());
        args.add(size);
        args.add((page-1L)*size);
        return new Page<>(jdbc.query(RECORD_SELECT+RECORDS+where+" ORDER BY r.id LIMIT ? OFFSET ?",(r,i)->record(r),args.toArray()),page,size,total);
    }
    public List<Metadata> files(String user,String record) {
        return jdbc.query("SELECT f.* FROM legacy_file_link l JOIN stored_file f ON f.id=l.file_id WHERE l.legacy_record_id=? AND f.owner_id=? AND f.state='ACTIVE' ORDER BY l.file_id",(r,i)->FileRepository.map(r).metadata(),record,user);
    }
    public boolean ownsFile(String user,String record,String file) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM legacy_file_link l JOIN stored_file f ON f.id=l.file_id WHERE l.legacy_record_id=? AND l.file_id=? AND f.owner_id=? AND f.purpose='LEGACY_ATTACHMENT'",Integer.class,record,file,user)>0;
    }
    private static Instant instant(ResultSet r,String field)throws SQLException {
        LocalDateTime value=r.getObject(field,LocalDateTime.class);
        return value==null?null:value.toInstant(ZoneOffset.UTC);
    }
    private static final String REVIEWS=" FROM legacy_pass_review v JOIN legacy_record r ON r.id=v.legacy_record_id AND r.user_id=v.user_id";
    private static final String REVIEW_SELECT="SELECT v.*,JSON_UNQUOTE(JSON_EXTRACT(r.payload,'$.version')) old_version,(SELECT p.id FROM assessment_pass p WHERE p.legacy_review_id=v.id ORDER BY p.id LIMIT 1) pass_id";
    private static Review review(ResultSet r)throws SQLException {
        return new Review(r.getString("id"),r.getString("legacy_record_id"),r.getString("course_id"),r.getString("chapter_id"),r.getString("kind"),r.getBigDecimal("old_score"),r.getBigDecimal("old_threshold"),r.getString("old_version"),r.getString("release_id"),r.getString("decision"),r.getString("reason"),instant(r,"decided_at"),r.getString("pass_id"));
    }
    public Review review(String user,String id,boolean lock) {
        return jdbc.query(REVIEW_SELECT+REVIEWS+" WHERE v.id=? AND v.user_id=?"+(lock?" FOR UPDATE":""),(r,i)->review(r),id,user).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    public Optional<Review> recordReview(String user,String record) {
        return jdbc.query(REVIEW_SELECT+REVIEWS+" WHERE v.legacy_record_id=? AND v.user_id=? ORDER BY v.id LIMIT 1",(r,i)->review(r),record,user).stream().findFirst();
    }
    public Page<Review> reviews(String user,String decision,String course,int page,int size) {
        String where=" WHERE v.user_id=?";
        List<Object> args=new ArrayList<>(List.of(user));
        if(decision!=null) {
            where+=" AND v.decision=?";
            args.add(decision);
        }
        if(course!=null) {
            where+=" AND v.course_id=?";
            args.add(course);
        }
        long total=jdbc.queryForObject("SELECT COUNT(*)"+REVIEWS+where,Long.class,args.toArray());
        args.add(size);
        args.add((page-1L)*size);
        return new Page<>(jdbc.query(REVIEW_SELECT+REVIEWS+where+" ORDER BY v.id LIMIT ? OFFSET ?",(r,i)->review(r),args.toArray()),page,size,total);
    }
    public boolean releaseMapping(String course,String release) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM content_release WHERE id=? AND course_id=? AND state<>'DRAFT'",Integer.class,release,course)>0;
    }
    public boolean chapterMapping(String course,String release,String chapter) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM chapter_revision WHERE chapter_id=? AND release_id=? AND course_id=?",Integer.class,chapter,release,course)>0;
    }
    public void decide(String user,String id,LegacyReviewDecision w) {
        jdbc.update("UPDATE legacy_pass_review SET course_id=?,chapter_id=?,release_id=?,decision=?,decided_by=?,decided_at=UTC_TIMESTAMP(6),reason=? WHERE id=? AND user_id=?",w.courseId().toString(),id(w.chapterId()),id(w.mappingReleaseId()),w.decision(),user,w.reason(),id,user);
    }
    public void grant(String user,Review review,LegacyReviewDecision w) {
        jdbc.update("INSERT INTO assessment_pass(id,user_id,course_id,chapter_id,kind,release_id,source,legacy_review_id,granted_at) VALUES(?,?,?,?,?,?,'LEGACY_CONFIRMED',?,UTC_TIMESTAMP(6))",UUID.randomUUID().toString(),user,w.courseId().toString(),id(w.chapterId()),review.kind(),id(w.mappingReleaseId()),review.id());
    }
    public record Credit(String questionId) {
    }
    public Credit credit(String user,String record) {
        return jdbc.query("SELECT question_id FROM legacy_practice_summary WHERE legacy_record_id=? AND user_id=? FOR UPDATE",(r,i)->new Credit(r.getString("question_id")),record,user).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND,"旧汇总不属于本人"));
    }
    public Optional<Mapping> currentOriginal(String question) {
        return jdbc.query("SELECT q.course_id,q.chapter_id,r.release_id FROM question q JOIN question_revision r ON r.question_id=q.id JOIN content_release cr ON cr.id=r.release_id AND cr.state='PUBLISHED' WHERE q.id=? AND q.mode='CHAPTER' AND q.eligible_original=true",(r,i)->new Mapping(r.getString("course_id"),r.getString("chapter_id"),r.getString("release_id")),question).stream().findFirst();
    }
    public Mapping previousMapping(String question) {
        return jdbc.query("SELECT q.course_id,q.chapter_id,cr.id release_id FROM question q LEFT JOIN content_release cr ON cr.course_id=q.course_id AND cr.state='PUBLISHED' WHERE q.id=?",(r,i)->new Mapping(r.getString("course_id"),r.getString("chapter_id"),r.getString("release_id")),question).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.INVALID_RELATION));
    }
    public void credit(String user,String record,String question,boolean approved) {
        jdbc.update("UPDATE legacy_practice_summary SET question_id=?,gate_credit_approved=? WHERE legacy_record_id=? AND user_id=?",question,approved,record,user);
    }
    private static String id(UUID value) {
        return value==null?null:value.toString();
    }
}
