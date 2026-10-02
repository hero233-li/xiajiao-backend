package cn.xuexizhitu.repository;

import cn.xuexizhitu.common.*;
import cn.xuexizhitu.dto.ContentDtos.*;
import cn.xuexizhitu.dto.LearningDtos.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;

@Repository @RequiredArgsConstructor
public class ContentRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private static final String MODULES="""
        SELECT m.*,COALESCE(n.mastery,0) mastery,COALESCE(n.note,'') note,COALESCE(n.revision,0) revision
        FROM module_revision m JOIN knowledge_module k ON k.id=m.module_id
        LEFT JOIN user_knowledge_note n ON n.module_id=m.module_id AND n.user_id=?
        WHERE m.release_id=? AND k.course_id=?
        """;
    public KnowledgeModule module(String user,String course,String release,String id) {
        return jdbc.query(MODULES+" AND m.module_id=?",this::moduleRow,user,release,course,id).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    private KnowledgeModule moduleRow(ResultSet r,int index) throws SQLException {
        String id=r.getString("module_id"),release=r.getString("release_id");
        List<KnowledgeExample> examples=jdbc.query("SELECT id,question,stars FROM knowledge_example WHERE release_id=? AND module_id=? ORDER BY sort_order,id",(s,n)->new KnowledgeExample(s.getString("id"),s.getString("question"),s.getInt("stars")),release,id);
        return new KnowledgeModule(id,r.getString("title"),r.getString("content"),r.getInt("difficulty"),json(r.getString("formulas"),new TypeReference<List<Formula>>() {}),examples,json(r.getString("resources"),new TypeReference<List<Resource>>() {}),new UserNote(r.getInt("mastery"),r.getString("note"),r.getLong("revision")));
    }
    private <T> T json(String raw,TypeReference<T> type) {try{return mapper.readValue(raw,type);}catch(Exception e){throw new IllegalStateException("Invalid published JSON",e);}}
    public Page<KnowledgeModule> knowledge(String user,String course,String release,String q,Integer difficulty,int page,int size) {
        List<Object> args=new ArrayList<>(List.of(user,release,course));String where="";
        if(q!=null&&!q.isEmpty()){where+=" AND (LOCATE(?,m.title)>0 OR LOCATE(?,m.content)>0)";args.add(q);args.add(q);}
        if(difficulty!=null){where+=" AND m.difficulty=?";args.add(difficulty);}
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM ("+MODULES+where+") matches",Long.class,args.toArray());
        args.add(size);args.add((long)(page-1)*size);
        return new Page<>(jdbc.query(MODULES+where+" ORDER BY k.stable_key,m.module_id LIMIT ? OFFSET ?",this::moduleRow,args.toArray()),page,size,total);
    }
    public void saveKnowledgeNote(String user,String course,String release,String module,KnowledgeNoteWrite w) {
        if(module(user,course,release,module).userNote().revision()!=w.expectedRevision())throw new BusinessException(ErrorCode.REVISION_CONFLICT);
        jdbc.update("""
            INSERT INTO user_knowledge_note(user_id,module_id,mastery,note,updated_at,revision) VALUES(?,?,?,?,?,1)
            ON DUPLICATE KEY UPDATE mastery=VALUES(mastery),note=VALUES(note),updated_at=VALUES(updated_at),revision=revision+1
            """,user,module,w.mastery(),w.note(),LocalDateTime.now(ZoneOffset.UTC));
    }
    public ExampleSolution solution(String course,String release,String example) {
        return jdbc.query("""
            SELECT s.* FROM example_solution s JOIN knowledge_example e ON e.id=s.example_id
            JOIN knowledge_module m ON m.id=e.module_id WHERE e.release_id=? AND m.course_id=? AND e.id=?
            """,(r,n)->new ExampleSolution(example,r.getString("answer"),r.getString("solution")),release,course,example)
            .stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    public boolean hasKnowledge(String release) {return jdbc.queryForObject("SELECT COUNT(*) FROM module_revision WHERE release_id=?",Integer.class,release)>0;}
    public record FileRow(String id,String key,String filename,String mime,long size,String sha) {}
    public FileRow manualFile(String release,String requested) {
        List<Resource> resources=new ArrayList<>();
        jdbc.queryForList("SELECT resources FROM module_revision WHERE release_id=?",String.class,release).forEach(raw->resources.addAll(json(raw,new TypeReference<List<Resource>>() {})));
        Set<String> referenced=new HashSet<>();resources.stream().filter(r->"FILE".equals(r.kind())&&r.fileId()!=null).forEach(r->referenced.add(r.fileId()));
        if(requested!=null&&!referenced.contains(requested))throw new BusinessException(ErrorCode.NOT_FOUND);
        for(String id:new TreeSet<>(referenced)) {
            if(requested!=null&&!id.equals(requested))continue;
            List<FileRow> files=jdbc.query("SELECT * FROM stored_file WHERE id=? AND purpose='MANUAL' AND state='ACTIVE' AND owner_id IS NULL AND contains_answers=false",
                (r,n)->new FileRow(id,r.getString("storage_key"),r.getString("original_name"),r.getString("mime_type"),r.getLong("size_bytes"),r.getString("sha256")),id);
            if(!files.isEmpty())return files.getFirst();
        }
        throw new BusinessException(ErrorCode.NOT_FOUND,"当前发布课程没有可用手册");
    }
    public boolean hasManual(String release) {try{manualFile(release,null);return true;}catch(BusinessException e){return false;}}
    private static final Pattern TAG=Pattern.compile("(?<![\\p{L}\\p{N}_#])#([\\p{L}\\p{N}_-]{1,100})(?![\\p{L}\\p{N}_-])");
    public static List<String> tags(String content) {Set<String> result=new TreeSet<>();var m=TAG.matcher(content);while(m.find())result.add(m.group(1));return List.copyOf(result);}
    private Note noteRow(ResultSet r,int index) throws SQLException {
        String content=r.getString("content");return new Note(r.getString("course_id"),r.getDate("note_date").toLocalDate(),content,r.getString("id"),instant(r,"created_at"),instant(r,"updated_at"),r.getLong("revision"),tags(content));
    }
    public Note note(String user,String id) {
        return jdbc.query("SELECT * FROM study_note WHERE id=? AND user_id=?",this::noteRow,id,user).stream().findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
    }
    public String createNote(String user,NoteWrite w) {
        String id=UUID.randomUUID().toString();LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        jdbc.update("INSERT INTO study_note(id,user_id,course_id,note_date,content,created_at,updated_at,revision) VALUES(?,?,?,?,?,?,?,0)",id,user,w.courseId().toString(),w.noteDate(),w.content(),now,now);return id;
    }
    public void updateNote(String user,String id,NoteUpdate w) {
        if(note(user,id).revision()!=w.expectedRevision())throw new BusinessException(ErrorCode.REVISION_CONFLICT);
        if(jdbc.update("UPDATE study_note SET note_date=?,content=?,updated_at=?,revision=revision+1 WHERE id=? AND user_id=? AND revision=?",w.noteDate(),w.content(),LocalDateTime.now(ZoneOffset.UTC),id,user,w.expectedRevision())!=1)throw new BusinessException(ErrorCode.REVISION_CONFLICT);
    }
    public Page<Note> notes(String user,String course,String q,String tag,LocalDate from,LocalDate to,int page,int size) {
        List<Object> args=new ArrayList<>(List.of(user));String sql="SELECT * FROM study_note WHERE user_id=?";
        if(course!=null){sql+=" AND course_id=?";args.add(course);}
        if(q!=null&&!q.isEmpty()){sql+=" AND LOCATE(?,content)>0";args.add(q);}
        if(from!=null){sql+=" AND note_date>=?";args.add(from);}
        if(to!=null){sql+=" AND note_date<=?";args.add(to);}
        if(tag!=null&&!tag.isEmpty()) {
            // Tags are derived from content, with no independent editable tag state in the approved schema.
            List<Note> matched=jdbc.query(sql+" ORDER BY note_date DESC,created_at DESC,id",this::noteRow,args.toArray()).stream().filter(n->n.tags().contains(tag)).toList();
            long offset=(long)(page-1)*size;return new Page<>(matched.stream().skip(offset).limit(size).toList(),page,size,matched.size());
        }
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM ("+sql+") matches",Long.class,args.toArray());args.add(size);args.add((long)(page-1)*size);
        return new Page<>(jdbc.query(sql+" ORDER BY note_date DESC,created_at DESC,id LIMIT ? OFFSET ?",this::noteRow,args.toArray()),page,size,total);
    }
    public NoteTags noteTags(String user,String course) {
        List<String> texts=jdbc.queryForList("SELECT content FROM study_note WHERE user_id=?"+(course==null?"":" AND course_id=?"),String.class,course==null?new Object[]{user}:new Object[]{user,course});
        Map<String,Integer> counts=new TreeMap<>();texts.forEach(text->tags(text).forEach(t->counts.merge(t,1,Integer::sum)));
        return new NoteTags(counts.entrySet().stream().map(e->new TagCount(e.getKey(),e.getValue())).toList());
    }
    private static Instant instant(ResultSet r,String name)throws SQLException{return r.getObject(name,LocalDateTime.class).toInstant(ZoneOffset.UTC);}
}
