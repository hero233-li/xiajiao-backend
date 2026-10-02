package cn.xuexizhitu.repository;

import cn.xuexizhitu.common.*;
import cn.xuexizhitu.dto.PracticeDtos.*;
import cn.xuexizhitu.dto.LearningDtos.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.time.*;
import java.util.*;

@Repository @RequiredArgsConstructor
public class PracticeRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private static final String QUESTIONS="""
        SELECT q.id,q.course_id,q.chapter_id,q.mode,r.id revision_id,r.release_id,r.stem,r.options,r.difficulty,r.source_locator,
               COALESCE(m.bookmarked,false) bookmarked,COALESCE(m.uncertain,false) uncertain,COALESCE(m.revision,0) mark_revision,l.correct latest_correct
        FROM question q JOIN question_revision r ON r.question_id=q.id
        JOIN chapter_revision ch ON ch.release_id=r.release_id AND ch.chapter_id=q.chapter_id
        LEFT JOIN user_question_mark m ON m.question_id=q.id AND m.user_id=?
        LEFT JOIN practice_submission l ON l.id=(SELECT s.id FROM practice_submission s WHERE s.question_id=q.id AND s.user_id=? ORDER BY s.submitted_at DESC,s.id DESC LIMIT 1)
        WHERE q.course_id=? AND r.release_id=?
        """;
    private QuestionPublic questionRow(ResultSet r,int index)throws SQLException {
        String revision=r.getString("revision_id");Boolean correct=(Boolean)r.getObject("latest_correct");
        return new QuestionPublic(r.getString("id"),revision,r.getString("release_id"),r.getString("course_id"),r.getString("chapter_id"),Mode.valueOf(r.getString("mode")),
            jdbc.queryForList("SELECT point_id FROM question_point WHERE revision_id=? ORDER BY point_id",String.class,revision),r.getString("stem"),options(r.getString("options")),r.getInt("difficulty"),cn.xuexizhitu.common.BusinessData.publicSource(r.getString("source_locator")),
            new PublicMark(r.getBoolean("bookmarked"),r.getBoolean("uncertain"),r.getLong("mark_revision")),correct==null?null:correct?Outcome.CORRECT:Outcome.WRONG);
    }
    private List<String> options(String json){try{return mapper.readValue(json,new TypeReference<List<String>>(){});}catch(Exception e){throw new IllegalStateException("Invalid question options",e);}}
    public QuestionPublic question(String user,String course,String release,String question) {
        return jdbc.query(QUESTIONS+" AND q.id=?",this::questionRow,user,user,course,release,question).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    public void requireChapter(String course,String release,String chapter) {
        if(chapter!=null&&jdbc.queryForObject("SELECT COUNT(*) FROM chapter_revision WHERE chapter_id=? AND course_id=? AND release_id=?",Integer.class,chapter,course,release)==0)throw new BusinessException(ErrorCode.NOT_FOUND);
    }
    public Page<QuestionPublic> questions(String user,String course,String release,String chapter,Mode mode,Filter filter,Integer difficulty,String q,int page,int size) {
        List<Object> args=new ArrayList<>(List.of(user,user,course,release));String where="";
        if(chapter!=null){where+=" AND q.chapter_id=?";args.add(chapter);}if(mode!=null){where+=" AND q.mode=?";args.add(mode.name());}
        if(difficulty!=null){where+=" AND r.difficulty=?";args.add(difficulty);}if(q!=null&&!q.isEmpty()){where+=" AND LOCATE(?,r.stem)>0";args.add(q);}
        where+=switch(filter){case ALL->"";case UNANSWERED->" AND l.id IS NULL";case WRONG->" AND l.correct=false";case BOOKMARKED->" AND m.bookmarked=true";case UNCERTAIN->" AND m.uncertain=true";};
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM ("+QUESTIONS+where+") matches",Long.class,args.toArray());args.add(size);args.add((long)(page-1)*size);
        return new Page<>(jdbc.query(QUESTIONS+where+" ORDER BY ch.sort_order,r.sort_order,q.id LIMIT ? OFFSET ?",this::questionRow,args.toArray()),page,size,total);
    }
    public record Submission(String id,String question,String revision,int selected,boolean correct,Instant at,String snapshot) {}
    private Submission submissionRow(ResultSet r,int i)throws SQLException{return new Submission(r.getString("id"),r.getString("question_id"),r.getString("revision_id"),r.getInt("selected_option"),r.getBoolean("correct"),instant(r,"submitted_at"),r.getString("result_snapshot"));}
    public Submission replay(String user,String key) {return jdbc.query("SELECT * FROM practice_submission WHERE user_id=? AND idempotency_key=?",this::submissionRow,user,key).stream().findFirst().orElse(null);}
    public Submission submission(String user,String course,String id) {
        return jdbc.query("SELECT s.* FROM practice_submission s JOIN question q ON q.id=s.question_id WHERE s.user_id=? AND q.course_id=? AND s.id=?",this::submissionRow,user,course,id).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    public int correctOption(String revision) {return jdbc.queryForObject("SELECT correct_option FROM question_solution WHERE revision_id=?",Integer.class,revision);}
    public Submission insert(String user,QuestionPublic q,int selected,String key) {
        int correct=correctOption(q.revisionId());if(correct<0||correct>=q.options().size())throw new BusinessException(ErrorCode.INVALID_RELATION,"题目标准答案配置不合法");
        String id=UUID.randomUUID().toString();Instant at=Instant.now();
        jdbc.update("INSERT INTO practice_submission(id,user_id,question_id,revision_id,selected_option,correct,submitted_at,idempotency_key) VALUES(?,?,?,?,?,?,?,?)",id,user,q.id(),q.revisionId(),selected,correct==selected,LocalDateTime.ofInstant(at,ZoneOffset.UTC),key);
        return submission(user,q.courseId(),id);
    }
    public Result result(Submission s,Stats stats) {
        if(s.snapshot()!=null){try{return mapper.readValue(s.snapshot(),Result.class);}catch(Exception e){throw new IllegalStateException("Invalid submission snapshot",e);}}
        return jdbc.queryForObject("SELECT r.options,s.correct_option,s.explanation FROM question_revision r JOIN question_solution s ON s.revision_id=r.id WHERE r.id=?",(r,i)->{
            int correct=r.getInt("correct_option");return new Result(s.id(),s.question(),s.revision(),s.selected(),s.correct(),correct,options(r.getString("options")).get(correct),r.getString("explanation"),s.at(),stats);
        },s.revision());
    }
    public void saveSnapshot(String id,Result result) {
        try{jdbc.update("UPDATE practice_submission SET result_snapshot=? WHERE id=?",mapper.writeValueAsString(result),id);}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}
    }
    public String questionChapter(String course,String question) {
        return jdbc.queryForList("SELECT chapter_id FROM question WHERE id=? AND course_id=?",String.class,question,course).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    public Page<HistoryRow> history(String user,String course,String question,String chapter,int page,int size) {
        List<Object> args=new ArrayList<>(List.of(user,course));String sql="FROM practice_submission s JOIN question q ON q.id=s.question_id WHERE s.user_id=? AND q.course_id=?";
        if(question!=null){sql+=" AND q.id=?";args.add(question);}if(chapter!=null){sql+=" AND q.chapter_id=?";args.add(chapter);}
        long total=jdbc.queryForObject("SELECT COUNT(*) "+sql,Long.class,args.toArray());args.add(size);args.add((long)(page-1)*size);
        return new Page<>(jdbc.query("SELECT s.* "+sql+" ORDER BY s.submitted_at DESC,s.id DESC LIMIT ? OFFSET ?",(r,i)->new HistoryRow(r.getString("id"),r.getString("question_id"),r.getString("revision_id"),r.getInt("selected_option"),r.getBoolean("correct"),instant(r,"submitted_at")),args.toArray()),page,size,total);
    }
    public QuestionMark mark(String user,String question,MarkWrite w) {
        Long revision=jdbc.query("SELECT revision FROM user_question_mark WHERE user_id=? AND question_id=?",(r,i)->r.getLong(1),user,question).stream().findFirst().orElse(0L);
        if(revision!=w.expectedRevision().longValue())throw new BusinessException(ErrorCode.REVISION_CONFLICT);
        LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        jdbc.update("INSERT INTO user_question_mark(user_id,question_id,bookmarked,uncertain,updated_at,revision) VALUES(?,?,?,?,?,1) ON DUPLICATE KEY UPDATE bookmarked=VALUES(bookmarked),uncertain=VALUES(uncertain),updated_at=VALUES(updated_at),revision=revision+1",user,question,w.bookmarked(),w.uncertain(),now);
        return new QuestionMark(question,w.bookmarked(),w.uncertain(),revision+1,now.toInstant(ZoneOffset.UTC));
    }
    public boolean hasPractice(String release){return jdbc.queryForObject("SELECT COUNT(*) FROM question_revision WHERE release_id=?",Integer.class,release)>0;}
    private final AssessmentRepository assessments;
    private record Gate(int floor,int cap,java.math.BigDecimal ratio,int minimum) {}
    public Stats stats(String user,String course,String release,String chapter) {
        List<Object> args=new ArrayList<>(List.of(course,release));String current="FROM question q JOIN question_revision r ON r.question_id=q.id WHERE q.course_id=? AND r.release_id=?";
        if(chapter!=null){current+=" AND q.chapter_id=?";args.add(chapter);}
        int available=jdbc.queryForObject("SELECT COUNT(*) "+current+" AND q.eligible_original=true AND q.mode='CHAPTER'",Integer.class,args.toArray());
        String credits="""
            SELECT question_id FROM practice_submission WHERE user_id=?
            UNION SELECT qr.question_id FROM assessment_session s JOIN assessment_session_question sq ON sq.session_id=s.id
                  JOIN question_revision qr ON qr.id=sq.revision_id WHERE s.user_id=? AND s.status IN ('SUBMITTED','TIMED_OUT') AND sq.selected_option IS NOT NULL
            UNION SELECT question_id FROM legacy_practice_summary WHERE user_id=? AND gate_credit_approved=true AND question_id IS NOT NULL
            """;
        List<Object> creditArgs=new ArrayList<>(args);creditArgs.addAll(List.of(user,user,user));
        int answered=jdbc.queryForObject("SELECT COUNT(*) "+current+" AND q.eligible_original=true AND q.mode='CHAPTER' AND q.id IN ("+credits+")",Integer.class,creditArgs.toArray());
        List<Object> attemptsArgs=new ArrayList<>(List.of(user,course));String attempts="FROM practice_submission s JOIN question q ON q.id=s.question_id WHERE s.user_id=? AND q.course_id=?";
        if(chapter!=null){attempts+=" AND q.chapter_id=?";attemptsArgs.add(chapter);}
        int[] totals=jdbc.queryForObject("SELECT COUNT(*),COALESCE(SUM(s.correct),0) "+attempts,(r,i)->new int[]{r.getInt(1),r.getInt(2)},attemptsArgs.toArray());
        List<Object> latestArgs=new ArrayList<>(List.of(user,user,course,release));String latestWhere=" AND l.correct=false";if(chapter!=null){latestWhere+=" AND q.chapter_id=?";latestArgs.add(chapter);}
        int wrong=jdbc.queryForObject("SELECT COUNT(*) FROM ("+QUESTIONS+latestWhere+") matches",Integer.class,latestArgs.toArray());
        Integer threshold=null;Boolean can=null;List<String> reasons=new ArrayList<>();
        if(chapter!=null) {
            Integer override=jdbc.queryForObject("SELECT gate_override FROM chapter_revision WHERE release_id=? AND chapter_id=?",Integer.class,release,chapter);
            Gate gate=jdbc.query("SELECT gate_floor,gate_cap,gate_ratio,chapter_min_questions FROM assessment_policy WHERE course_id=? AND release_id=?",(r,i)->new Gate(r.getInt(1),r.getInt(2),r.getBigDecimal(3),r.getInt(4)),course,release).stream().findFirst().orElse(new Gate(20,100,new java.math.BigDecimal("0.6"),20));
            threshold=override==null?Math.min(gate.cap(),Math.max(gate.floor(),gate.ratio().multiply(java.math.BigDecimal.valueOf(available)).setScale(0,java.math.RoundingMode.CEILING).intValue())):override;
            if(available<gate.minimum())reasons.add("可用且已审核原创题不足"+gate.minimum()+"道");if(answered<threshold)reasons.add("已答原创题数未达到门槛");
            if(!assessments.participates(release,chapter))reasons.add("此章节不参与检测");
            try {
                var policy=assessments.policy(course,release);var points=assessments.points(release,chapter);
                int count=Math.max(policy.minimum(),points.size());
                if(count>policy.maximum())reasons.add("题库考点数超过检测题数上限");
                else if(available<policy.minimum())reasons.add("题库可用原创题量不足");
                else try{cn.xuexizhitu.service.AssessmentPlanner.chapter(points,assessments.candidates(user,course,release,chapter),count);}catch(IllegalArgumentException e){reasons.add("题库题量或考点覆盖不足");}
            }catch(BusinessException e){reasons.add(e.getMessage());}
            can=reasons.isEmpty();
        }
        return new Stats(course,chapter,available,answered,totals[0],totals[1],totals[0]==0?0:Math.round(10000.0*totals[1]/totals[0])/100.0,wrong,threshold,can,reasons);
    }
    public Overview overview(String user,String course,String release) {
        List<PracticeChapter> chapters=jdbc.query("SELECT * FROM chapter_revision WHERE course_id=? AND release_id=? ORDER BY sort_order,chapter_id",(r,i)->{
            String chapter=r.getString("chapter_id");List<String> passes=jdbc.queryForList("SELECT DISTINCT release_id FROM assessment_pass WHERE user_id=? AND chapter_id=? AND course_id=? AND kind='CHAPTER' AND invalidated_at IS NULL ORDER BY release_id",String.class,user,chapter,course);
            return new PracticeChapter(chapter,r.getString("title"),stats(user,course,release,chapter),!passes.isEmpty(),passes);
        },course,release);
        int variant=jdbc.queryForObject("SELECT COUNT(*) FROM question_revision r JOIN question q ON q.id=r.question_id WHERE r.release_id=? AND q.course_id=? AND q.mode='VARIANT'",Integer.class,release,course);
        return new Overview(course,release,chapters,stats(user,course,release,null),variant);
    }
    public Page<LegacySummary> legacy(String user,String course,int page,int size) {
        String sql="FROM legacy_practice_summary s LEFT JOIN question q ON q.id=s.question_id WHERE s.user_id=?"+(course==null?"":" AND q.course_id=?");
        List<Object> args=new ArrayList<>(List.of(user));if(course!=null)args.add(course);
        long count=jdbc.queryForObject("SELECT COUNT(*) "+sql,Long.class,args.toArray());args.add(size);args.add((long)(page-1)*size);
        List<LegacySummary> rows=jdbc.query("SELECT s.*,q.course_id,q.chapter_id "+sql+" ORDER BY s.last_updated_at DESC,s.legacy_record_id LIMIT ? OFFSET ?",(r,i)->new LegacySummary(r.getString("legacy_record_id"),r.getString("question_id"),r.getString("old_question_id"),(Integer)r.getObject("attempts"),(Integer)r.getObject("selected_option"),(Boolean)r.getObject("correct"),instant(r,"last_updated_at"),r.getBoolean("gate_credit_approved"),r.getString("course_id"),r.getString("chapter_id")),args.toArray());
        return new Page<>(rows,page,size,count);
    }
    private static Instant instant(ResultSet r,String name)throws SQLException{LocalDateTime value=r.getObject(name,LocalDateTime.class);return value==null?null:value.toInstant(ZoneOffset.UTC);}
}
