package cn.xuexizhitu.planning.infrastructure;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.contract.WorkflowDtos.PlanConfig;
import cn.xuexizhitu.planning.domain.PlanTypes.*;
import cn.xuexizhitu.learning.api.LearningDtos.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.time.*;
import java.util.*;
@Repository @RequiredArgsConstructor public class PlanRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private String json(Object o) {
        try {
            return mapper.writeValueAsString(o);
        }
        catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
    private <T> T read(String s,Class<T> t) {
        try {
            return mapper.readValue(s,t);
        }
        catch(Exception e) {
            throw new IllegalStateException("Invalid stored plan snapshot",e);
        }
    }
    private static Instant at(ResultSet r,String n)throws SQLException {
        var t=r.getObject(n,LocalDateTime.class);
        return t==null?null:t.toInstant(ZoneOffset.UTC);
    }
    private static LocalDateTime time(Instant at) {
        return at==null?null:LocalDateTime.ofInstant(at,ZoneOffset.UTC);
    }
    private static UUID uuid(String s) {
        return s==null?null:UUID.fromString(s);
    }
    private static Navigation nav(String code,String chapter,String item,String kind,String template) {
        Pane pane=kind.equals("ITEM")?Pane.CATALOG:kind.equals("PAPER")?Pane.EXAMS:template==null?Pane.PRACTICE:Pane.KNOWLEDGE;
        return new Navigation(pane,code,uuid(chapter),uuid(item),null);
    }
    public Row owned(String user,String id,boolean lock) {
        return jdbc.query("SELECT p.*,cr.revision_no FROM learning_plan p JOIN plan_current_revision cr ON cr.plan_id=p.id WHERE p.id=? AND p.user_id=?"+(lock?" FOR UPDATE":""),                 (r,i)->new Row(id,user,r.getString("cycle_id"),r.getInt("revision_no"),at(r,"created_at")),id,user).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    public RevisionRow revision(String id,int revision) {
        return jdbc.query("SELECT snapshot,confirmed_at FROM plan_revision WHERE plan_id=? AND revision_no=?",(r,i)->new RevisionRow(revision,read(r.getString("snapshot"),RevisionSnapshot.class),at(r,"confirmed_at")),id,revision)                 .stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND,"计划版本不存在"));
    }
    public List<Task> allTasks(String user,String id) {
        return jdbc.query("SELECT t.*,c.code,i.chapter_id,CASE WHEN t.kind='ITEM' THEN COALESCE(p.completed,false) ELSE t.completed END effective_completed,CASE WHEN t.kind='ITEM' THEN p.completed_at ELSE t.completed_at END effective_at FROM plan_task t JOIN course c ON c.id=t.course_id LEFT JOIN study_item i ON i.id=t.item_id LEFT JOIN user_item_progress p ON p.item_id=t.item_id AND p.user_id=? WHERE t.plan_id=? ORDER BY t.id",(r,i)->                 new Task(r.getString("id"),r.getString("course_id"),r.getString("item_id"),r.getString("template_id"),r.getString("kind"),r.getString("title_snapshot"),r.getInt("estimated_minutes"),r.getString("release_id"),r.getBoolean("effective_completed"),at(r,"effective_at"),nav(r.getString("code"),r.getString("chapter_id"),r.getString("item_id"),r.getString("kind"),r.getString("template_id"))),user,id);
    }
    public List<Task> sourceItems(String user,String course,String release,String code) {
        return jdbc.query("SELECT i.*,COALESCE(p.completed,false) completed,p.completed_at FROM item_revision i JOIN chapter_revision ch ON ch.release_id=i.release_id AND ch.chapter_id=i.chapter_id LEFT JOIN user_item_progress p ON p.item_id=i.item_id AND p.user_id=? WHERE i.release_id=? ORDER BY ch.sort_order,ch.chapter_id,i.sort_order,i.item_id",(r,i)->                 new Task(UUID.randomUUID().toString(),course,r.getString("item_id"),null,"ITEM",r.getString("title"),r.getInt("estimated_minutes"),release,r.getBoolean("completed"),at(r,"completed_at"),nav(code,r.getString("chapter_id"),r.getString("item_id"),"ITEM",null)),user,release);
    }
    public List<Task> sourceTemplates(String course,String release,String code) {
        return jdbc.query("SELECT * FROM plan_task_template WHERE release_id=? AND course_id=? ORDER BY sort_order,id",(r,i)->                 new Task(UUID.randomUUID().toString(),course,null,r.getString("id"),r.getString("kind"),r.getString("title"),r.getInt("estimated_minutes"),release,false,null,nav(code,null,null,r.getString("kind"),r.getString("id"))),release,course);
    }
    public Map<String,LocalDate> deadlines(PlanConfig c) {
        Map<String,LocalDate> result=new LinkedHashMap<>();
        jdbc.query("SELECT course_id,exam_date FROM cycle_course WHERE cycle_id=? ORDER BY course_id",r-> {
            result.put(r.getString("course_id"),r.getDate("exam_date")==null?null:r.getDate("exam_date").toLocalDate());
        }
        ,c.cycleId().toString());
        return result;
    }
    public List<Segment> segments(String id,int revision) {
        return jdbc.query("SELECT * FROM plan_task_segment WHERE plan_id=? AND revision_no=? ORDER BY sort_order,id",(r,i)->new Segment(r.getString("id"),r.getString("task_id"),r.getDate("scheduled_on")==null?null:r.getDate("scheduled_on").toLocalDate(),r.getInt("minutes"),r.getString("state"),r.getInt("sort_order")),id,revision);
    }
    public void create(String id,String user,PlanConfig c) {
        jdbc.update("INSERT INTO learning_plan(id,user_id,cycle_id,start_date,end_date,config_snapshot,created_at) VALUES(?,?,?,?,?,?,UTC_TIMESTAMP(6))",id,user,c.cycleId().toString(),c.startDate(),c.endDate(),json(c));
    }
    public void insertTasks(String plan,List<Task> tasks) {
        jdbc.batchUpdate("INSERT INTO plan_task(id,plan_id,course_id,item_id,template_id,title_snapshot,estimated_minutes,release_id,kind,completed,completed_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",tasks,tasks.isEmpty()?1:Math.min(200,tasks.size()),(ps,t)-> {
            Object[] args= {
                t.id(),plan,t.courseId(),t.itemId(),t.templateId(),t.title(),t.estimatedMinutes(),t.releaseId(),t.kind(),t.completed(),time(t.completedAt())
            }
            ;for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);
        }
        );
    }
    public void saveRevision(String id,int revision,PlanConfig c,List<Task> tasks,List<Segment> segments,Instant at) {
        jdbc.update("INSERT INTO plan_revision(plan_id,revision_no,snapshot,confirmed_at) VALUES(?,?,?,?)",id,revision,json(new RevisionSnapshot(c,tasks.stream().map(Task::id).toList())),time(at));
        jdbc.batchUpdate("INSERT INTO plan_day_capacity(plan_id,revision_no,day,capacity_minutes) VALUES(?,?,?,?)",c.dayCapacities(),200,(ps,d)-> {
            ps.setString(1,id);ps.setInt(2,revision);ps.setObject(3,d.day());ps.setInt(4,d.capacityMinutes());
        }
        );
        jdbc.batchUpdate("INSERT INTO plan_task_segment(id,plan_id,revision_no,task_id,scheduled_on,minutes,state,sort_order) VALUES(?,?,?,?,?,?,?,?)",segments,200,(ps,s)-> {
            Object[] args= {
                s.id(),id,revision,s.taskId(),s.scheduledOn(),s.minutes(),s.state(),s.sortOrder()
            }
            ;for(int i=0;i<args.length;i++)ps.setObject(i+1,args[i]);
        }
        );
        jdbc.update("INSERT INTO plan_current_revision(plan_id,revision_no) VALUES(?,?) ON DUPLICATE KEY UPDATE revision_no=VALUES(revision_no)",id,revision);
    }
    public void completeTask(String id,String task,boolean done) {
        jdbc.update("UPDATE plan_task SET completed=?,completed_at=? WHERE id=? AND plan_id=?",done,done?time(Instant.now()):null,task,id);
    }
    public void preview(String id,String plan,String user,int base,String fp,PreviewSnapshot s) {
        jdbc.update("INSERT INTO plan_preview(id,plan_id,user_id,base_revision,as_of,snapshot,gap_minutes,input_fingerprint,created_at) VALUES(?,?,?,?,?,?,?,?,?)",id,plan,user,base,s.response().asOf(),json(s),s.response().gapMinutes(),fp,time(s.response().createdAt()));
    }
    public PreviewRow preview(String user,String plan,String id,boolean lock) {
        return jdbc.query("SELECT * FROM plan_preview WHERE id=? AND plan_id=? AND user_id=?"+(lock?" FOR UPDATE":""),(r,i)->new PreviewRow(id,plan,r.getInt("base_revision"),r.getDate("as_of").toLocalDate(),r.getString("input_fingerprint"),r.getInt("gap_minutes"),at(r,"confirmed_at"),read(r.getString("snapshot"),PreviewSnapshot.class)),id,plan,user)                 .stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    public void confirm(String id) {
        jdbc.update("UPDATE plan_preview SET confirmed_at=UTC_TIMESTAMP(6) WHERE id=?",id);
    }
    public void updateConfig(String id,PlanConfig c) {
        jdbc.update("UPDATE learning_plan SET start_date=?,end_date=?,config_snapshot=? WHERE id=?",c.startDate(),c.endDate(),json(c),id);
    }
    public void freezeLegacyTaskSets(String id,List<Task> existing) {
        for(var r:jdbc.queryForList("SELECT revision_no,snapshot FROM plan_revision WHERE plan_id=?",id)) {
            var snapshot=read(r.get("snapshot").toString(),RevisionSnapshot.class);
            if(snapshot.taskIds()==null)jdbc.update("UPDATE plan_revision SET snapshot=? WHERE plan_id=? AND revision_no=?",json(new RevisionSnapshot(snapshot.config(),existing.stream().map(Task::id).toList())),id,r.get("revision_no"));
        }
    }
    public Page<Summary> list(String user,String cycle,int page,int size) {
        String sql=" FROM learning_plan p WHERE p.user_id=?";
        List<Object> args=new ArrayList<>(List.of(user));
        if(cycle!=null) {
            sql+=" AND p.cycle_id=?";
            args.add(cycle);
        }
        long total=jdbc.queryForObject("SELECT COUNT(*)"+sql,Long.class,args.toArray());
        args.add(size);
        args.add((page-1L)*size);
        var rows=jdbc.query("SELECT p.*,cr.revision_no FROM learning_plan p JOIN plan_current_revision cr ON cr.plan_id=p.id"+sql.substring(sql.indexOf(" WHERE"))+" ORDER BY p.created_at DESC,p.id DESC LIMIT ? OFFSET ?",(r,i)->new Summary(r.getString("id"),r.getString("cycle_id"),r.getDate("start_date").toLocalDate(),r.getDate("end_date").toLocalDate(),r.getInt("revision_no"),0),args.toArray());
        Map<String,int[]> progress=new HashMap<>();
        if(!rows.isEmpty()) {
            var parameters=new ArrayList<Object>();
            parameters.add(user);
            parameters.addAll(rows.stream().map(Summary::id).toList());
            jdbc.query("SELECT t.plan_id,SUM(t.estimated_minutes) total_minutes,SUM(CASE WHEN (CASE WHEN t.kind='ITEM' THEN COALESCE(ip.completed,false) ELSE t.completed END) THEN t.estimated_minutes ELSE 0 END) done_minutes FROM plan_task t JOIN plan_current_revision cr ON cr.plan_id=t.plan_id JOIN plan_revision r ON r.plan_id=cr.plan_id AND r.revision_no=cr.revision_no LEFT JOIN user_item_progress ip ON ip.item_id=t.item_id AND ip.user_id=? WHERE t.plan_id IN ("+String.join(",",Collections.nCopies(rows.size(),"?"))+") AND (JSON_EXTRACT(r.snapshot,'$.taskIds') IS NULL OR JSON_CONTAINS(r.snapshot,JSON_QUOTE(t.id),'$.taskIds')) GROUP BY t.plan_id",r-> {
                progress.put(r.getString("plan_id"),new int[] {
                    r.getInt("done_minutes"),r.getInt("total_minutes")
                }
                );
            }
            ,parameters.toArray());
        }
        return new Page<>(rows.stream().map(r-> {
            int[] values=progress.getOrDefault(r.id(),new int[] {
                0,0
            }
            );return new Summary(r.id(),r.cycleId(),r.startDate(),r.endDate(),r.revision(),cn.xuexizhitu.planning.domain.PlanRules.percent(values[0],values[1]));
        }
        ).toList(),page,size,total);
    }
    public record CapacityState(LocalDate day,int capacityMinutes) {
    }
    public List<CapacityState> capacityState(String plan,int revision) {
        return jdbc.query("SELECT day,capacity_minutes FROM plan_day_capacity WHERE plan_id=? AND revision_no=? ORDER BY day",(r,i)->new CapacityState(r.getDate(1).toLocalDate(),r.getInt(2)),plan,revision);
    }
    public record ProgressState(String itemId,boolean completed,long revision) {
    }
    public List<ProgressState> progressState(String user,String release) {
        return jdbc.query("SELECT item_id,completed,revision FROM user_item_progress WHERE user_id=? AND item_id IN (SELECT item_id FROM item_revision WHERE release_id=?) ORDER BY item_id",(r,i)->new ProgressState(r.getString(1),r.getBoolean(2),r.getLong(3)),user,release);
    }
}
