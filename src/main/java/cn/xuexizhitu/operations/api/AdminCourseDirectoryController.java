package cn.xuexizhitu.operations.api;

import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.learning.api.LearningDtos.Page;
import cn.xuexizhitu.security.CurrentUser;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** Administrative directory includes disabled courses, so deactivation remains reversible. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/courses")
public class AdminCourseDirectoryController {
    private final JdbcTemplate jdbc;
    public record Entry(String id,String code,String name,String courseType,boolean active,String releaseId) {}
    @GetMapping
    public ApiResponse<Page<Entry>> list(@RequestParam(required=false) UUID cycleId,
            @RequestParam(defaultValue="1") @Min(1) int page,
            @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        CurrentUser.requireAdmin();
        List<Object> args=new ArrayList<>();
        String where="";
        if(cycleId!=null){
            if(jdbc.queryForObject("SELECT COUNT(*) FROM exam_cycle WHERE id=?",Integer.class,cycleId.toString())==0)
                throw new cn.xuexizhitu.common.BusinessException(cn.xuexizhitu.common.ErrorCode.NOT_FOUND,"考试周期不存在");
            where=" WHERE EXISTS (SELECT 1 FROM cycle_course cc WHERE cc.course_id=c.id AND cc.cycle_id=?)";
            args.add(cycleId.toString());
        }
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM course c"+where,Long.class,args.toArray());
        args.add(size);args.add((page-1L)*size);
        var rows=jdbc.query("SELECT c.id,c.code,c.name,c.course_type,c.active,(SELECT r.id FROM content_release r WHERE r.course_id=c.id AND r.state='PUBLISHED' ORDER BY r.version_no DESC LIMIT 1) release_id FROM course c"+where+" ORDER BY c.code,c.id LIMIT ? OFFSET ?",
                (r,i)->new Entry(r.getString("id"),r.getString("code"),r.getString("name"),r.getString("course_type"),r.getBoolean("active"),r.getString("release_id")),args.toArray());
        return ApiResponse.ok(new Page<>(rows,page,size,total));
    }
}
