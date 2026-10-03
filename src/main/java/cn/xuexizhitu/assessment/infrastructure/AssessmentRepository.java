package cn.xuexizhitu.assessment.infrastructure;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.assessment.api.AssessmentDtos.*;
import cn.xuexizhitu.learning.api.LearningDtos.Page;
import cn.xuexizhitu.assessment.domain.AssessmentPlanner.Candidate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.time.*;
import java.math.*;
import java.util.*;
import java.sql.*;
@Repository @RequiredArgsConstructor public class AssessmentRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public record Policy(String id,String release,int minimum,int maximum,int minutes,BigDecimal passScore,int mockCount,int mockMinutes,BigDecimal mockPassScore) {
    }
    public Policy policy(String course,String release) {
        return jdbc.query("SELECT * FROM assessment_policy WHERE course_id=? AND release_id=?",(r,i)->new Policy(r.getString("id"),release,r.getInt("chapter_min_questions"),r.getInt("chapter_max_questions"),r.getInt("chapter_limit_minutes"),r.getBigDecimal("chapter_pass_score"),r.getInt("mock_question_count"),r.getInt("mock_limit_minutes"),r.getBigDecimal("mock_pass_score")),course,release).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.INVALID_RELATION,"发布版本缺少检测策略"));
    }
    public record Row(String id,String user,String course,String chapter,String release,String policy,Kind kind,Status status,Instant start,Instant deadline,Instant submitted,int count,Integer correct,BigDecimal passScore,Boolean passed,String cycle,String snapshot) {
    }
    private Row row(ResultSet r,int i)throws SQLException {
        return new Row(r.getString("id"),r.getString("user_id"),r.getString("course_id"),r.getString("chapter_id"),r.getString("release_id"),r.getString("policy_id"),Kind.valueOf(r.getString("kind")),Status.valueOf(r.getString("status")),instant(r,"started_at"),instant(r,"deadline_at"),instant(r,"submitted_at"),r.getInt("question_count"),(Integer)r.getObject("correct_count"),r.getBigDecimal("pass_score"),(Boolean)r.getObject("passed"),r.getString("request_cycle_id"),r.getString("apply_snapshot"));
    }
    public Row owned(String user,String course,String session,boolean lock) {
        return jdbc.query("SELECT * FROM assessment_session WHERE id=? AND user_id=? AND course_id=?"+(lock?" FOR UPDATE":""),this::row,session,user,course).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    public Row replay(String user,String key) {
        return jdbc.query("SELECT * FROM assessment_session WHERE user_id=? AND idempotency_key=?",this::row,user,key).stream().findFirst().orElse(null);
    }
    public Row lock(String id) {
        return jdbc.queryForObject("SELECT * FROM assessment_session WHERE id=? FOR UPDATE",this::row,id);
    }
    public List<String> pending() {
        return jdbc.queryForList("SELECT id FROM assessment_session WHERE status='IN_PROGRESS' AND deadline_at<=UTC_TIMESTAMP(6) AND NOT EXISTS(SELECT 1 FROM background_failure b WHERE b.task_kind='ASSESSMENT_TIMEOUT' AND b.target_id=assessment_session.id AND b.next_attempt_at>UTC_TIMESTAMP(6)) ORDER BY deadline_at,id LIMIT 100",String.class);
    }
    public List<String> points(String release,String chapter) {
        return jdbc.queryForList("SELECT point_id FROM release_point WHERE release_id=? AND chapter_id=? ORDER BY point_id",String.class,release,chapter);
    }
    public boolean participates(String release,String chapter) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT participates_in_assessment FROM chapter_revision WHERE release_id=? AND chapter_id=?",Boolean.class,release,chapter));
    }
    public List<String> participating(String release) {
        return jdbc.queryForList("SELECT chapter_id FROM chapter_revision WHERE release_id=? AND participates_in_assessment=true ORDER BY chapter_id",String.class,release);
    }
    public List<String> missing(String user,String course,String release) {
        return jdbc.queryForList("SELECT ch.chapter_id FROM chapter_revision ch WHERE ch.release_id=? AND ch.participates_in_assessment=true AND NOT EXISTS(SELECT 1 FROM assessment_pass p WHERE p.user_id=? AND p.course_id=? AND p.chapter_id=ch.chapter_id AND p.kind='CHAPTER' AND p.invalidated_at IS NULL) ORDER BY ch.chapter_id",String.class,release,user,course);
    }
    public List<Candidate> candidates(String user,String course,String release,String chapter) {
        Map<String,Set<String>> pointSets=new HashMap<>();
        jdbc.query("SELECT qp.revision_id,qp.point_id FROM question_point qp JOIN question_revision r ON r.id=qp.revision_id JOIN question q ON q.id=r.question_id WHERE r.release_id=? AND q.chapter_id=? ORDER BY qp.revision_id,qp.point_id",rs-> {
            pointSets.computeIfAbsent(rs.getString(1),k->new LinkedHashSet<>()).add(rs.getString(2));
        }
        ,release,chapter);
        String credits="SELECT question_id FROM practice_submission WHERE user_id=? UNION SELECT r.question_id FROM assessment_session s JOIN assessment_session_question sq ON sq.session_id=s.id JOIN question_revision r ON r.id=sq.revision_id WHERE s.user_id=? AND s.status<>'IN_PROGRESS' AND sq.selected_option IS NOT NULL UNION SELECT question_id FROM legacy_practice_summary WHERE user_id=? AND gate_credit_approved=true";
        return jdbc.query("SELECT r.id,q.id AS question_id,q.id IN ("+credits+") AS answered FROM question q JOIN question_revision r ON r.question_id=q.id JOIN question_solution sol ON sol.revision_id=r.id WHERE q.course_id=? AND r.release_id=? AND q.chapter_id=? AND q.mode='CHAPTER' AND q.eligible_original=true AND sol.correct_option<JSON_LENGTH(r.options) ORDER BY q.id",(r,i)->new Candidate(r.getString("id"),r.getString("question_id"),pointSets.getOrDefault(r.getString("id"),Set.of()),r.getBoolean("answered")),user,user,user,course,release,chapter);
    }
    public Map<String,BigDecimal> weights(Policy p,List<String> chapters) {
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
        Map<String,BigDecimal> weights=new TreeMap<>();
        jdbc.query("SELECT w.*,c.code FROM mock_chapter_weight w JOIN assessment_policy p ON p.id=w.policy_id JOIN course c ON c.id=p.course_id WHERE w.policy_id=?",r-> {
            LocalDate from=r.getDate("sample_from").toLocalDate(),to=r.getDate("sample_to").toLocalDate();             validateWeight(r.getString("evidence"),r.getString("code"),from,to,instant(r,"approved_at"));             weights.put(r.getString("chapter_id"),r.getBigDecimal("score_share"));
        }
        ,p.id());
        if(chapters.isEmpty()||!weights.keySet().equals(new HashSet<>(chapters))||weights.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add).compareTo(BigDecimal.ONE)!=0)throw new BusinessException(ErrorCode.INVALID_RELATION,"模拟权重未完整审核或合计不为1");
        return weights;
    }
    public void validateWeight(String evidence,String code,LocalDate from,LocalDate to,Instant approved) {
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
        boolean calendar=from.equals(LocalDate.of(today.getYear()-5,1,1))&&to.equals(LocalDate.of(today.getYear()-1,12,31));
        boolean rolling=!to.isAfter(today)&&!to.isBefore(today.minusDays(1))&&!from.isBefore(to.minusYears(5))&&!from.isAfter(to.minusYears(5).plusDays(1));
        if(!calendar&&!rolling)throw new BusinessException(ErrorCode.INVALID_RELATION,"模拟权重须有完整近五年统计且及时审核");
        validateEvidence(evidence,code,from,to);
        if(approved==null||approved.isAfter(now()))throw new BusinessException(ErrorCode.INVALID_RELATION,"权重审核时间无效");
    }
    private void validateEvidence(String value,String courseCode,LocalDate from,LocalDate to) {
        try {
            var evidence=mapper.readTree(value);
            if(!evidence.isObject()||!evidence.path("description").isTextual()||evidence.path("description").asText().isBlank()||!evidence.path("sourcePaperKeys").isArray()||evidence.path("sourcePaperKeys").isEmpty())throw new IllegalArgumentException();
            for(var key:evidence.path("sourcePaperKeys")) {
                if(!key.isTextual()||!key.asText().startsWith(courseCode+":"))throw new IllegalArgumentException();
                String month=key.asText().substring(courseCode.length()+1);
                if(!month.matches("[0-9]{4}-(0[1-9]|1[0-2])"))throw new IllegalArgumentException();
                LocalDate date=YearMonth.parse(month).atDay(1);
                if(date.isBefore(YearMonth.from(from).atDay(1))||date.isAfter(to))throw new IllegalArgumentException();
            }
        }
        catch(Exception e) {
            throw new BusinessException(ErrorCode.INVALID_RELATION,"权重依据须有同课程、统计窗口内的真实试卷标识及说明");
        }
    }
    public Map<String,Object> policySnapshot(String id) {
        return jdbc.queryForMap("SELECT * FROM assessment_policy WHERE id=?",id);
    }
    public boolean hasChapter(String release,String chapter) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM chapter_revision WHERE release_id=? AND chapter_id=?",Integer.class,release,chapter)>0;
    }
    public List<Map<String,Object>> weightSnapshot(String policy) {
        return jdbc.queryForList("SELECT chapter_id,score_share,sample_from,sample_to,evidence,approved_by,approved_at FROM mock_chapter_weight WHERE policy_id=? ORDER BY chapter_id",policy);
    }
    public void alert(String course,String chapter,String release,String reason) {
        jdbc.update("INSERT INTO bank_alert(id,course_id,chapter_id,release_id,alert_code,details) VALUES(?,?,?,?,?,?)",UUID.randomUUID().toString(),course,chapter,release,"ASSESSMENT_BANK_INSUFFICIENT",json(Map.of("reason",reason)));
    }
    public Row create(String user,String course,String chapter,String cycle,Kind kind,Policy p,List<Candidate> chosen,String key,Map<String,Object> snapshot) {
        String id=UUID.randomUUID().toString();
        Instant now=now();
        int minutes=kind==Kind.CHAPTER?p.minutes():p.mockMinutes();
        BigDecimal score=kind==Kind.CHAPTER?p.passScore():p.mockPassScore();
        jdbc.update("INSERT INTO assessment_session(id,user_id,course_id,chapter_id,release_id,policy_id,kind,status,started_at,deadline_at,question_count,pass_score,policy_snapshot,idempotency_key,request_cycle_id) VALUES(?,?,?,?,?,?,?,'IN_PROGRESS',?,?,?,?,?,?,?)",id,user,course,chapter,p.release(),p.id(),kind.name(),time(now),time(now.plusSeconds(minutes*60L)),chosen.size(),score,json(snapshot),key,cycle);
        int position=0;
        for(Candidate c:chosen)jdbc.update("INSERT INTO assessment_session_question(session_id,revision_id,position) VALUES(?,?,?)",id,c.revision(),++position);
        return owned(user,course,id,false);
    }
    public String json(Object value) {
        try {
            return mapper.writeValueAsString(value);
        }
        catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
    public Session replaySession(Row row) {
        try {
            return mapper.readValue(row.snapshot(),Session.class);
        }
        catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
    public void snapshot(String id,Session session) {
        jdbc.update("UPDATE assessment_session SET apply_snapshot=? WHERE id=?",json(session),id);
    }
    private List<String> options(String value) {
        try {
            return mapper.readValue(value,new TypeReference<List<String>>() {
            }
            );
        }
        catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
    public List<SessionQuestion> questions(String id) {
        Map<String,List<String>> pointSets=new HashMap<>();
        jdbc.query("SELECT qp.revision_id,qp.point_id FROM question_point qp JOIN assessment_session_question sq ON sq.revision_id=qp.revision_id WHERE sq.session_id=? ORDER BY qp.revision_id,qp.point_id",rs-> {
            pointSets.computeIfAbsent(rs.getString(1),k->new ArrayList<>()).add(rs.getString(2));
        }
        ,id);
        return jdbc.query("SELECT sq.*,r.*,q.course_id,q.chapter_id,q.mode,sq.revision_id FROM assessment_session_question sq JOIN question_revision r ON r.id=sq.revision_id JOIN question q ON q.id=r.question_id WHERE sq.session_id=? ORDER BY sq.position",(r,i)->new SessionQuestion(r.getInt("position"),new Question(r.getString("question_id"),r.getString("revision_id"),r.getString("release_id"),r.getString("course_id"),r.getString("chapter_id"),r.getString("mode"),pointSets.getOrDefault(r.getString("revision_id"),List.of()),r.getString("stem"),options(r.getString("options")),r.getInt("difficulty"),cn.xuexizhitu.common.BusinessData.publicSource(r.getString("source_locator"))),(Integer)r.getObject("selected_option"),instant(r,"answer_saved_at")),id);
    }
    public String fingerprint(List<SessionQuestion> questions) {
        StringBuilder b=new StringBuilder();
        for(SessionQuestion q:questions)b.append(q.position()).append(':').append(q.question().revisionId()).append(':').append(q.selectedOption()).append(':').append(q.answerSavedAt()).append('\n');
        try {
            return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(b.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        }
        catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
    public Session session(Row row) {
        List<SessionQuestion> qs=questions(row.id());
        Instant now=now();
        return new Session(row.id(),row.course(),row.chapter(),row.kind(),row.release(),row.policy(),row.status(),row.start(),row.deadline(),now,!now.isBefore(row.deadline()),row.count(),(int)java.time.Duration.between(row.start(),row.deadline()).toMinutes(),row.passScore(),fingerprint(qs),qs);
    }
    public Instant save(String id,String revision,int option,Instant now) {
        jdbc.update("UPDATE assessment_session_question SET selected_option=?,answer_saved_at=? WHERE session_id=? AND revision_id=?",option,time(now),id,revision);
        jdbc.update("INSERT INTO assessment_answer_event(id,session_id,revision_id,selected_option,saved_at) VALUES(?,?,?,?,?)",UUID.randomUUID().toString(),id,revision,option,time(now));
        return now;
    }
    public List<ResultAnswer> answers(String session) {
        return jdbc.query("SELECT sq.revision_id,sq.selected_option,s.correct_option,s.explanation,r.options FROM assessment_session_question sq JOIN question_revision r ON r.id=sq.revision_id JOIN question_solution s ON s.revision_id=r.id WHERE sq.session_id=? ORDER BY sq.position",(r,i)-> {
            Integer selected=(Integer)r.getObject("selected_option");int correct=r.getInt("correct_option");return new ResultAnswer(r.getString("revision_id"),selected,selected!=null&&selected==correct,correct,options(r.getString("options")).get(correct),r.getString("explanation"));
        }
        ,session);
    }
    public void finish(Row row,Status status,Instant at) {
        int correct=(int)answers(row.id()).stream().filter(ResultAnswer::correct).count();
        boolean passed=cn.xuexizhitu.assessment.domain.AssessmentScoring.grade(correct,row.count(),row.passScore()).passed();
        jdbc.update("UPDATE assessment_session SET status=?,submitted_at=?,correct_count=?,passed=? WHERE id=?",status.name(),time(at),correct,passed,row.id());
        if(passed)jdbc.update("INSERT INTO assessment_pass(id,user_id,session_id,course_id,chapter_id,kind,release_id,source,granted_at) VALUES(?,?,?,?,?,?,?,'ASSESSMENT',?)",UUID.randomUUID().toString(),row.user(),row.id(),row.course(),row.chapter(),row.kind().name(),row.release(),time(at));
    }
    public Pass pass(String session) {
        return jdbc.query("SELECT * FROM assessment_pass WHERE session_id=?",(r,i)->new Pass(r.getString("id"),Kind.valueOf(r.getString("kind")),r.getString("chapter_id"),r.getString("release_id"),r.getString("source"),instant(r,"granted_at"),instant(r,"invalidated_at"),r.getString("invalidated_by"),r.getString("invalidation_reason")),session).stream().findFirst().orElse(null);
    }
    public Page<Summary> list(String user,String course,Kind kind,String chapter,Status status,int page,int size) {
        String sql=" FROM assessment_session WHERE user_id=? AND course_id=?";
        List<Object> args=new ArrayList<>(List.of(user,course));
        if(kind!=null) {
            sql+=" AND kind=?";
            args.add(kind.name());
        }
        if(chapter!=null) {
            sql+=" AND chapter_id=?";
            args.add(chapter);
        }
        if(status!=null) {
            sql+=" AND status=?";
            args.add(status.name());
        }
        long total=jdbc.queryForObject("SELECT COUNT(*)"+sql,Long.class,args.toArray());
        args.add(size);
        args.add((long)(page-1)*size);
        return new Page<>(jdbc.query("SELECT *"+sql+" ORDER BY started_at DESC,id DESC LIMIT ? OFFSET ?",this::row,args.toArray()).stream().map(r->new Summary(r.id(),r.kind(),r.chapter(),r.status(),r.start(),r.submitted(),r.correct()==null?null:cn.xuexizhitu.assessment.domain.AssessmentScoring.grade(r.correct(),r.count(),r.passScore()).displayedScore(),r.passed(),r.release())).toList(),page,size,total);
    }
    public Unlock unlock(String user,String course,String cycle,String release) {
        LocalDate exam=jdbc.queryForObject("SELECT exam_date FROM cycle_course WHERE course_id=? AND cycle_id=?",(r,i)->r.getDate(1)==null?null:r.getDate(1).toLocalDate(),course,cycle);
        boolean paid=jdbc.queryForObject("SELECT COUNT(*) FROM enrollment WHERE user_id=? AND course_id=? AND cycle_id=? AND paid=true",Integer.class,user,course,cycle)>0;
        boolean mock=jdbc.queryForObject("SELECT COUNT(*) FROM assessment_pass WHERE user_id=? AND course_id=? AND kind='MOCK' AND invalidated_at IS NULL",Integer.class,user,course)>0;
        cn.xuexizhitu.assessment.api.AssessmentDtos.Override override=jdbc.query("SELECT * FROM unlock_override WHERE user_id=? AND course_id=? AND cycle_id=? AND revoked_at IS NULL",(r,i)->new cn.xuexizhitu.assessment.api.AssessmentDtos.Override(r.getString("id"),course,cycle,instant(r,"confirmed_at"),r.getDate("exam_date_snapshot").toLocalDate(),null,r.getLong("revision")),user,course,cycle).stream().findFirst().orElse(null);
        LocalDate today=LocalDate.now(ZoneId.of("Asia/Shanghai"));
        boolean window=exam!=null&&!today.isBefore(exam.minusDays(14))&&!today.isAfter(exam);
        List<String> sources=new ArrayList<>();
        if(mock)sources.add("MOCK_PASS");
        if(override!=null)sources.add("MANUAL_OVERRIDE");
        List<String> missing=release==null?List.of():missing(user,course,release);
        boolean canMock=release!=null&&!participating(release).isEmpty()&&missing.isEmpty();
        if(canMock)try {
            Policy policy=policy(course,release);
            weights(policy,participating(release));
        }
        catch(BusinessException e) {
            canMock=false;
        }
        return new Unlock(course,cycle,paid&&!sources.isEmpty(),paid&&!sources.isEmpty(),canMock,missing,sources,override,new SkipWindow(exam,exam==null?null:exam.minusDays(14),exam,paid&&window&&override==null,exam==null?"考试日期未知":!paid?"尚未报考缴费":!window?"不在临考允许窗口":override!=null?"已有活动确认":null),now());
    }
    public static Instant now() {
        return Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
    private static LocalDateTime time(Instant t) {
        return LocalDateTime.ofInstant(t,ZoneOffset.UTC);
    }
    private static Instant instant(ResultSet r,String name)throws SQLException {
        LocalDateTime value=r.getObject(name,LocalDateTime.class);
        return value==null?null:value.toInstant(ZoneOffset.UTC);
    }
}
