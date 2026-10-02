package cn.xuexizhitu.repository;

import cn.xuexizhitu.common.*;
import cn.xuexizhitu.dto.LearningDtos.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Explicit SQL for versioned public content and per-user state; schema remains owned by Flyway. */
@Repository @RequiredArgsConstructor
public class LearningRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public record CourseRow(String id, String code, String name, CourseType type, boolean active, String releaseId) {}
    private static final String COURSE_SELECT = """
        SELECT c.*, (SELECT r.id FROM content_release r WHERE r.course_id=c.id AND r.state='PUBLISHED'
                     ORDER BY r.version_no DESC LIMIT 1) release_id FROM course c
        """;
    private CourseRow courseRow(ResultSet r, int n) throws SQLException {
        return new CourseRow(r.getString("id"),r.getString("code"),r.getString("name"),CourseType.valueOf(r.getString("course_type")),r.getBoolean("active"),r.getString("release_id"));
    }
    public CourseRow course(String id) {
        return jdbc.query(COURSE_SELECT+" WHERE c.id=? AND c.active=true",this::courseRow,id).stream().findFirst().orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
    public CourseRow courseByCode(String code) {
        return jdbc.query(COURSE_SELECT+" WHERE c.code=? AND c.active=true",this::courseRow,code).stream().findFirst().orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
    public List<CourseRow> courses(String cycle, CourseType type, int page, int size) {
        return jdbc.query(COURSE_SELECT+" WHERE c.active=true AND EXISTS (SELECT 1 FROM cycle_course cc WHERE cc.course_id=c.id AND cc.cycle_id=?)"
            +(type==null?"":" AND c.course_type=?")+" ORDER BY c.code LIMIT ? OFFSET ?",this::courseRow,
            type==null?new Object[]{cycle,size,(long)(page-1)*size}:new Object[]{cycle,type.name(),size,(long)(page-1)*size});
    }
    public long courseCount(String cycle, CourseType type) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM course c JOIN cycle_course cc ON cc.course_id=c.id WHERE c.active=true AND cc.cycle_id=?"+(type==null?"":" AND c.course_type=?"),Long.class,type==null?new Object[]{cycle}:new Object[]{cycle,type.name()});
    }
    public void requireCycleCourse(String course, String cycle) {
        if(jdbc.queryForObject("SELECT COUNT(*) FROM cycle_course WHERE course_id=? AND cycle_id=?",Integer.class,course,cycle)==0) throw new BusinessException(ErrorCode.NOT_FOUND);
    }
    public void requireCycle(String id) {
        if(jdbc.queryForObject("SELECT COUNT(*) FROM exam_cycle WHERE id=?",Integer.class,id)==0) throw new BusinessException(ErrorCode.NOT_FOUND);
    }
    public ExamCycle cycle(String id) {
        requireCycle(id);
        return jdbc.queryForObject("SELECT * FROM exam_cycle WHERE id=?",(r,n)->new ExamCycle(id,r.getString("name"),r.getDate("start_date").toLocalDate(),r.getDate("end_date").toLocalDate(),r.getString("timezone"),
            jdbc.query("SELECT cc.* FROM cycle_course cc JOIN course c ON c.id=cc.course_id WHERE cc.cycle_id=? ORDER BY c.code",(s,i)->new CycleCourse(s.getString("course_id"),date(s,"exam_date"),time(s,"starts_at"),time(s,"ends_at")),id)),id);
    }
    public Page<ExamCycle> cycles(int page,int size) {
        List<String> ids=jdbc.queryForList("SELECT id FROM exam_cycle ORDER BY start_date DESC,id LIMIT ? OFFSET ?",String.class,size,(long)(page-1)*size);
        return new Page<>(ids.stream().map(this::cycle).toList(),page,size,jdbc.queryForObject("SELECT COUNT(*) FROM exam_cycle",Long.class));
    }
    public Enrollment enrollment(String user,String course,String cycle) {
        return jdbc.query("SELECT * FROM enrollment WHERE user_id=? AND course_id=? AND cycle_id=?",(r,n)->new Enrollment(cycle,course,r.getBoolean("paid"),r.getBigDecimal("fee"),r.getBigDecimal("official_score"),(Boolean)r.getObject("official_passed"),
            r.getDate("passed_month")==null?null:YearMonth.from(r.getDate("passed_month").toLocalDate()).toString(),r.getString("note"),r.getLong("revision")),user,course,cycle)
            .stream().findFirst().orElse(new Enrollment(cycle,course,false,null,null,null,null,"",0));
    }
    // A user-row lock serializes initial writes too, where no progress/enrollment row exists to lock yet.
    public void lockUser(String user) {
        jdbc.queryForObject("SELECT id FROM app_user WHERE id=? FOR UPDATE",String.class,user);
    }
    public void writeEnrollment(String user,String course,String cycle,EnrollmentWrite w) {
        long revision=enrollment(user,course,cycle).revision();
        if(revision!=w.expectedRevision()) throw new BusinessException(ErrorCode.REVISION_CONFLICT);
        jdbc.update("""
            INSERT INTO enrollment(user_id,course_id,cycle_id,paid,fee,official_score,official_passed,passed_month,note,revision)
            VALUES (?,?,?,?,?,?,?,?,?,1) ON DUPLICATE KEY UPDATE paid=VALUES(paid),fee=VALUES(fee),official_score=VALUES(official_score),
            official_passed=VALUES(official_passed),passed_month=VALUES(passed_month),note=VALUES(note),revision=revision+1
            """,user,course,cycle,w.paid(),w.fee(),w.officialScore(),w.officialPassed(),w.passedMonth()==null?null:w.passedMonth()+"-01",w.note());
    }
    public Progress progress(String user,String release) {
        if(release==null) return new Progress(0,0,0);
        return jdbc.queryForObject("""
            SELECT COUNT(*) total,COALESCE(SUM(CASE WHEN p.completed=true THEN 1 ELSE 0 END),0) done
            FROM item_revision i LEFT JOIN user_item_progress p ON p.item_id=i.item_id AND p.user_id=? WHERE i.release_id=?
            """,(r,n)->progress(r.getInt("done"),r.getInt("total")),user,release);
    }
    public Progress overallProgress(String user) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) total,COALESCE(SUM(CASE WHEN p.completed=true THEN 1 ELSE 0 END),0) done
            FROM item_revision i JOIN content_release r ON r.id=i.release_id JOIN course c ON c.id=r.course_id
            LEFT JOIN user_item_progress p ON p.item_id=i.item_id AND p.user_id=?
            WHERE c.code IN ('00023','02324','13175','13015','13216','13171') AND r.state='PUBLISHED'
            AND r.version_no=(SELECT MAX(r2.version_no) FROM content_release r2 WHERE r2.course_id=c.id AND r2.state='PUBLISHED')
            """,(r,n)->progress(r.getInt("done"),r.getInt("total")),user);
    }
    private Progress progress(int done,int total) {return new Progress(done,total,total==0?0:(int)Math.round(100.0*done/total));}
    private CatalogItem itemRow(ResultSet r,int n) throws SQLException {
        Resource resource=null;String raw=r.getString("resource_locator");
        if(raw!=null) {try {resource=mapper.readValue(raw,Resource.class);} catch(Exception e) {throw new IllegalStateException("Invalid published resource",e);}}
        return new CatalogItem(r.getString("item_id"),r.getString("title"),r.getInt("estimated_minutes"),resource,r.getBoolean("completed"),instant(r,"completed_at"),r.getLong("revision"));
    }
    private static final String ITEMS="""
        SELECT i.*,COALESCE(p.completed,false) completed,p.completed_at,COALESCE(p.revision,0) revision
        FROM item_revision i LEFT JOIN user_item_progress p ON p.item_id=i.item_id AND p.user_id=?
        WHERE i.release_id=?
        """;
    public List<CatalogChapter> chapters(String user,String release) {
        return jdbc.query("SELECT * FROM chapter_revision WHERE release_id=? ORDER BY sort_order,chapter_id",(r,n)->new CatalogChapter(r.getString("chapter_id"),r.getString("title"),r.getInt("sort_order"),r.getBoolean("participates_in_assessment"),
            jdbc.query(ITEMS+" AND i.chapter_id=? ORDER BY i.sort_order,i.item_id",this::itemRow,user,release,r.getString("chapter_id"))),release);
    }
    public CatalogItem item(String user,String release,String item) {
        return jdbc.query(ITEMS+" AND i.item_id=?",this::itemRow,user,release,item).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    public List<String> complete(String user,String release,String item,boolean completed,long expected,Instant now) {
        if(item(user,release,item).revision()!=expected) throw new BusinessException(ErrorCode.REVISION_CONFLICT);
        LocalDateTime at=LocalDateTime.ofInstant(now,ZoneOffset.UTC);
        jdbc.update("""
            INSERT INTO user_item_progress(user_id,item_id,completed,completed_at,updated_at,revision) VALUES (?,?,?,?,?,1)
            ON DUPLICATE KEY UPDATE completed=VALUES(completed),completed_at=VALUES(completed_at),updated_at=VALUES(updated_at),revision=revision+1
            """,user,item,completed,completed?at:null,at);
        List<String> plans=jdbc.queryForList("SELECT DISTINCT t.plan_id FROM plan_task t JOIN learning_plan p ON p.id=t.plan_id WHERE p.user_id=? AND t.item_id=?",String.class,user,item);
        jdbc.update("UPDATE plan_task t JOIN learning_plan p ON p.id=t.plan_id SET t.completed=?,t.completed_at=? WHERE p.user_id=? AND t.item_id=?",completed,completed?at:null,user,item);
        return plans;
    }
    public Navigation validateNavigation(CourseRow c, Navigation target) {
        if(!c.code().equals(target.courseCode())) throw new BusinessException(ErrorCode.INVALID_RELATION,"目标课程不匹配");
        if(target.chapterId()!=null && jdbc.queryForObject("SELECT COUNT(*) FROM chapter_revision WHERE chapter_id=? AND release_id=?",Integer.class,target.chapterId().toString(),c.releaseId())==0) throw new BusinessException(ErrorCode.INVALID_RELATION,"章节不属于当前发布课程");
        if(target.itemId()!=null && jdbc.queryForObject("SELECT COUNT(*) FROM item_revision WHERE item_id=? AND release_id=?"+(target.chapterId()==null?"":" AND chapter_id=?"),Integer.class,
            target.chapterId()==null?new Object[]{target.itemId().toString(),c.releaseId()}:new Object[]{target.itemId().toString(),c.releaseId(),target.chapterId().toString()})==0) throw new BusinessException(ErrorCode.INVALID_RELATION,"条目不属于目标章节或当前课程");
        if(target.questionId()!=null && jdbc.queryForObject("SELECT COUNT(*) FROM question_revision qr JOIN question q ON q.id=qr.question_id WHERE q.course_id=? AND qr.release_id=? AND qr.question_id=?"+(target.chapterId()==null?"":" AND q.chapter_id=?"),Integer.class,
            target.chapterId()==null?new Object[]{c.id(),c.releaseId(),target.questionId().toString()}:new Object[]{c.id(),c.releaseId(),target.questionId().toString(),target.chapterId().toString()})==0) throw new BusinessException(ErrorCode.INVALID_RELATION,"题目不属于目标章节或当前课程");
        return target;
    }
    public void writePosition(String user,CourseRow c,Navigation t,Instant now) {
        jdbc.update("""
            INSERT INTO recent_learning_position(user_id,course_id,chapter_id,item_id,question_id,pane,updated_at) VALUES (?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE chapter_id=VALUES(chapter_id),item_id=VALUES(item_id),question_id=VALUES(question_id),pane=VALUES(pane),updated_at=VALUES(updated_at)
            """,user,c.id(),str(t.chapterId()),str(t.itemId()),str(t.questionId()),t.pane().name(),LocalDateTime.ofInstant(now,ZoneOffset.UTC));
    }
    public LearningPosition position(String user,CourseRow c) {
        return jdbc.query("SELECT * FROM recent_learning_position WHERE user_id=? AND course_id=?",(r,n)-> {
            Navigation target=new Navigation(Pane.valueOf(r.getString("pane")),c.code(),uuid(r.getString("chapter_id")),uuid(r.getString("item_id")),uuid(r.getString("question_id")));
            return new LearningPosition(c.id(),target,c.name(),instant(r,"updated_at"));
        },user,c.id()).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    private static String str(UUID value) {return value==null?null:value.toString();}
    private static UUID uuid(String value) {return value==null?null:UUID.fromString(value);}
    private static LocalDate date(ResultSet r,String name) throws SQLException {return r.getDate(name)==null?null:r.getDate(name).toLocalDate();}
    private static LocalTime time(ResultSet r,String name) throws SQLException {return r.getTime(name)==null?null:r.getTime(name).toLocalTime();}
    private static Instant instant(ResultSet r,String name) throws SQLException {LocalDateTime value=r.getObject(name,LocalDateTime.class);return value==null?null:value.toInstant(ZoneOffset.UTC);}
}
