package cn.xuexizhitu.assessment.infrastructure;
import cn.xuexizhitu.assessment.api.AssessmentDtos.*;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.learning.api.LearningDtos.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.time.*;
import java.util.*;
@Repository @RequiredArgsConstructor public class ModerationRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public record OwnedPass(String userId,String courseId,Pass pass) {
    }
    public record Alert(String id,String courseId,String chapterId,String releaseId,String alertCode,String message,Instant createdAt,Instant acknowledgedAt) {
    }
    public record Audit(String id,String actorId,String action,String targetType,String targetId,String reason,String details,Instant occurredAt) {
    }
    private static Instant instant(ResultSet r,String field)throws SQLException {
        LocalDateTime t=r.getObject(field,LocalDateTime.class);
        return t==null?null:t.toInstant(ZoneOffset.UTC);
    }
    private static Pass pass(ResultSet r)throws SQLException {
        return new Pass(r.getString("id"),Kind.valueOf(r.getString("kind")),r.getString("chapter_id"),r.getString("release_id"),r.getString("source"),instant(r,"granted_at"),instant(r,"invalidated_at"),r.getString("invalidated_by"),r.getString("invalidation_reason"));
    }
    public OwnedPass pass(String id,boolean lock) {
        return jdbc.query("SELECT * FROM assessment_pass WHERE id=?"+(lock?" FOR UPDATE":""),(r,i)->new OwnedPass(r.getString("user_id"),r.getString("course_id"),pass(r)),id).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    public Page<Pass> passes(String user,String course,String kind,String chapter,boolean valid,int page,int size) {
        String sql=" FROM assessment_pass WHERE user_id=? AND course_id=?";
        List<Object> args=new ArrayList<>(List.of(user,course));
        if(kind!=null) {
            sql+=" AND kind=?";
            args.add(kind);
        }
        if(chapter!=null) {
            sql+=" AND chapter_id=?";
            args.add(chapter);
        }
        if(valid)sql+=" AND invalidated_at IS NULL";
        long total=jdbc.queryForObject("SELECT COUNT(*)"+sql,Long.class,args.toArray());
        args.add(size);
        args.add((page-1L)*size);
        return new Page<>(jdbc.query("SELECT *"+sql+" ORDER BY granted_at DESC,id DESC LIMIT ? OFFSET ?",(r,i)->pass(r),args.toArray()),page,size,total);
    }
    public void invalidate(String id,String actor,String reason) {
        jdbc.update("UPDATE assessment_pass SET invalidated_at=UTC_TIMESTAMP(6),invalidated_by=?,invalidation_reason=? WHERE id=?",actor,reason,id);
    }
    public List<String> enrolledCycles(String user,String course) {
        return jdbc.queryForList("SELECT cycle_id FROM enrollment WHERE user_id=? AND course_id=? ORDER BY cycle_id",String.class,user,course);
    }
    private Alert alert(ResultSet r)throws SQLException {
        String code=r.getString("alert_code");
        if(code.equals("ASSESSMENT_BANK_INSUFFICIENT"))code="INSUFFICIENT_QUESTIONS";
        String message="题库需复核";
        try {
            message=mapper.readTree(r.getString("details")).path("reason").asText(message);
        }
        catch(com.fasterxml.jackson.core.JsonProcessingException ignored) {
        }
        return new Alert(r.getString("id"),r.getString("course_id"),r.getString("chapter_id"),r.getString("release_id"),code,message,instant(r,"created_at"),instant(r,"acknowledged_at"));
    }
    public Page<Alert> alerts(String course,Boolean acknowledged,int page,int size) {
        String sql=" FROM bank_alert WHERE 1=1";
        List<Object> args=new ArrayList<>();
        if(course!=null) {
            sql+=" AND course_id=?";
            args.add(course);
        }
        if(acknowledged!=null)sql+=" AND acknowledged_at IS "+(acknowledged?"NOT NULL":"NULL");
        long total=jdbc.queryForObject("SELECT COUNT(*)"+sql,Long.class,args.toArray());
        args.add(size);
        args.add((page-1L)*size);
        return new Page<>(jdbc.query("SELECT *"+sql+" ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?",(r,i)->alert(r),args.toArray()),page,size,total);
    }
    public Alert alert(String id,boolean lock) {
        return jdbc.query("SELECT * FROM bank_alert WHERE id=?"+(lock?" FOR UPDATE":""),(r,i)->alert(r),id).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    public void acknowledge(String id,String actor) {
        jdbc.update("UPDATE bank_alert SET acknowledged_at=COALESCE(acknowledged_at,UTC_TIMESTAMP(6)),acknowledged_by=COALESCE(acknowledged_by,?) WHERE id=?",actor,id);
    }
    public Page<Audit> audits(String action,String target,int page,int size) {
        String sql=" FROM audit_event WHERE 1=1";
        List<Object> args=new ArrayList<>();
        if(action!=null) {
            sql+=" AND action=?";
            args.add(action);
        }
        if(target!=null) {
            sql+=" AND target_id=?";
            args.add(target);
        }
        long total=jdbc.queryForObject("SELECT COUNT(*)"+sql,Long.class,args.toArray());
        args.add(size);
        args.add((page-1L)*size);
        return new Page<>(jdbc.query("SELECT *"+sql+" ORDER BY occurred_at DESC,id DESC LIMIT ? OFFSET ?",(r,i)->new Audit(r.getString("id"),r.getString("actor_id"),r.getString("action"),r.getString("target_type"),r.getString("target_id"),r.getString("reason"),r.getString("details"),instant(r,"occurred_at")),args.toArray()),page,size,total);
    }
}
