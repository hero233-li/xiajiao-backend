package cn.xuexizhitu.dashboard.infrastructure;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
@Repository @RequiredArgsConstructor public class DashboardRepository {
    private final JdbcTemplate jdbc;
    public String defaultCycle(LocalDate day) {
        return jdbc.queryForList("SELECT id FROM exam_cycle ORDER BY CASE WHEN end_date>=? THEN 0 ELSE 1 END,start_date DESC,id LIMIT 1",String.class,day).stream().findFirst().orElse(null);
    }
    public String recentCourse(String user) {
        return jdbc.queryForList("SELECT p.course_id FROM recent_learning_position p JOIN course c ON c.id=p.course_id AND c.active=true WHERE p.user_id=? ORDER BY p.updated_at DESC,p.course_id LIMIT 1",String.class,user).stream().findFirst().orElse(null);
    }
    public String latestPlan(String user,String cycle) {
        return jdbc.queryForList("SELECT id FROM learning_plan WHERE user_id=? AND cycle_id=? ORDER BY created_at DESC,id DESC LIMIT 1",String.class,user,cycle).stream().findFirst().orElse(null);
    }
}
