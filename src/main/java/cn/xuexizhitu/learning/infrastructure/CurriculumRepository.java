package cn.xuexizhitu.learning.infrastructure;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.contract.WorkflowDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.*;
@Repository @RequiredArgsConstructor public class CurriculumRepository {
    private final JdbcTemplate jdbc;
    public void lockCourse(String id) {
        if(jdbc.queryForList("SELECT id FROM course WHERE id=? FOR UPDATE",String.class,id).isEmpty())throw new BusinessException(ErrorCode.NOT_FOUND);
    }
    public void createCourse(String id,String cycle,CourseAdminWrite w) {
        jdbc.update("INSERT INTO course(id,code,name,course_type,active) VALUES(?,?,?,?,?)",id,w.code(),w.name(),w.courseType(),w.active());
        jdbc.update("INSERT INTO cycle_course(cycle_id,course_id) VALUES(?,?)",cycle,id);
    }
    public void updateCourse(String id,CourseAdminWrite w) {
        jdbc.update("UPDATE course SET code=?,name=?,course_type=?,active=? WHERE id=?",w.code(),w.name(),w.courseType(),w.active(),id);
    }
    public void createCycle(String id,CycleWrite w) {
        jdbc.update("INSERT INTO exam_cycle(id,name,start_date,end_date) VALUES(?,?,?,?)",id,w.name(),w.startDate(),w.endDate());
    }
    public void lockCycle(String id) {
        if(jdbc.queryForList("SELECT id FROM exam_cycle WHERE id=? FOR UPDATE",String.class,id).isEmpty())throw new BusinessException(ErrorCode.NOT_FOUND);
    }
    public void updateCycle(String id,CycleWrite w) {
        jdbc.update("UPDATE exam_cycle SET name=?,start_date=?,end_date=? WHERE id=?",w.name(),w.startDate(),w.endDate(),id);
    }
    public List<String> cycleCourses(String id) {
        return jdbc.queryForList("SELECT course_id FROM cycle_course WHERE cycle_id=?",String.class,id);
    }
    public void removeCycleCourse(String cycle,String course) {
        if(jdbc.queryForObject("SELECT COUNT(*) FROM enrollment WHERE course_id=? AND cycle_id=?",Integer.class,course,cycle)>0)throw new BusinessException(ErrorCode.INVALID_RELATION,"已有报考的周期科目不能移除");
        jdbc.update("DELETE FROM cycle_course WHERE cycle_id=? AND course_id=?",cycle,course);
    }
    public void putCycleCourses(String id,CycleWrite w) {
        jdbc.batchUpdate("INSERT INTO cycle_course(cycle_id,course_id,exam_date,starts_at,ends_at) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE exam_date=VALUES(exam_date),starts_at=VALUES(starts_at),ends_at=VALUES(ends_at)",w.courses(),100,(ps,c)-> {
            ps.setString(1,id);ps.setString(2,c.courseId().toString());ps.setObject(3,c.examDate());ps.setString(4,c.startsAt());ps.setString(5,c.endsAt());
        }
        );
    }
}
