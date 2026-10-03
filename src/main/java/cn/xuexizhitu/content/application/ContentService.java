package cn.xuexizhitu.content.application;
import cn.xuexizhitu.files.application.PrivateFileStore;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.content.api.ContentDtos.*;
import cn.xuexizhitu.learning.api.LearningDtos.*;
import cn.xuexizhitu.content.infrastructure.ContentRepository;
import cn.xuexizhitu.learning.infrastructure.LearningRepository;
import cn.xuexizhitu.learning.infrastructure.LearningRepository.CourseRow;
import cn.xuexizhitu.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional(readOnly=true) public class ContentService {
    private final LearningRepository learning;
    private final ContentRepository repository;
    private final ObjectMapper mapper;
    private final PrivateFileStore files;
    private CourseRow published(String course) {
        CourseRow c=learning.course(course);
        if(c.releaseId()==null)throw new BusinessException(ErrorCode.NOT_FOUND);
        return c;
    }
    public Page<KnowledgeModule> knowledge(String course,String q,Integer difficulty,int page,int size) {
        CourseRow c=published(course);
        return repository.knowledge(CurrentUser.idOrThrow(),course,c.releaseId(),q,difficulty,page,size);
    }
    public KnowledgeModule module(String course,String id) {
        CourseRow c=published(course);
        return repository.module(CurrentUser.idOrThrow(),course,c.releaseId(),id);
    }
    @Transactional     public KnowledgeModule saveKnowledgeNote(String course,String id,KnowledgeNoteWrite w) {
        String user=CurrentUser.idOrThrow();
        learning.lockUser(user);
        CourseRow c=published(course);
        repository.saveKnowledgeNote(user,course,c.releaseId(),id,w);
        return repository.module(user,course,c.releaseId(),id);
    }
    public ExampleSolution solution(String course,String id) {
        CurrentUser.require();
        CourseRow c=published(course);
        return repository.solution(course,c.releaseId(),id);
    }
    private byte[] fileBytes(ContentRepository.FileRow file) {
        if(file.key().startsWith("managed/"))return Base64.getDecoder().decode(files.download(file.id()).contentBase64());
        if(!file.key().matches("classpath:manuals/[a-z0-9-]+\\.json")||!"application/json".equals(file.mime()))throw new BusinessException(ErrorCode.INVALID_RELATION,"手册资源类型不受支持");
        try(var input=new ClassPathResource(file.key().substring("classpath:".length())).getInputStream()) {
            byte[] data=input.readNBytes(8*1024*1024+1);
            if(data.length>8*1024*1024||data.length!=file.size()||!HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)).equals(file.sha()))throw new BusinessException(ErrorCode.INVALID_RELATION,"手册资源校验失败");
            return data;
        }
        catch(BusinessException e) {
            throw e;
        }
        catch(Exception e) {
            throw new BusinessException(ErrorCode.INVALID_RELATION,"手册资源缺失或无法读取");
        }
    }
    public FileDownload resource(String course,String id) {
        CurrentUser.require();
        CourseRow c=published(course);
        var file=repository.manualFile(c.releaseId(),id);
        byte[] data=fileBytes(file);
        validateManifest(course,c.releaseId(),data);
        return new FileDownload(new FileMetadata(file.id(),file.filename(),file.mime(),file.size(),file.sha(),false,"ACTIVE","MANUAL"),Base64.getEncoder().encodeToString(data),"base64");
    }
    private ManualManifest validateManifest(String course,String release,byte[] data) {
        try {
            ManualManifest m=mapper.readValue(data,ManualManifest.class);
            if(!course.equals(m.courseId())||!release.equals(m.releaseId())||m.sections()==null||m.sections().isEmpty())throw new BusinessException(ErrorCode.INVALID_RELATION,"手册发布关联不匹配");
            return m;
        }
        catch(BusinessException e) {
            throw e;
        }
        catch(Exception e) {
            throw new BusinessException(ErrorCode.INVALID_RELATION,"手册内容映射不合法");
        }
    }
    public Manual manual(String course) {
        String user=CurrentUser.idOrThrow();
        CourseRow c=published(course);
        var file=repository.manualFile(c.releaseId(),null);
        ManualManifest manifest=validateManifest(course,c.releaseId(),fileBytes(file));
        Map<String,CatalogChapter> chapters=new HashMap<>();
        learning.chapters(user,c.releaseId()).forEach(ch->chapters.put(ch.id(),ch));
        List<ManualSection> result=new ArrayList<>();
        Set<String> seen=new HashSet<>(),checkedExamples=new HashSet<>();
        for(ManifestSection s:manifest.sections()) {
            CatalogChapter chapter=chapters.get(s.chapterId());
            if(chapter==null||!seen.add(s.chapterId()))throw new BusinessException(ErrorCode.INVALID_RELATION,"手册章节映射缺失或重复");
            Map<String,CatalogItem> items=new HashMap<>();
            chapter.items().forEach(i->items.put(i.id(),i));
            List<ManualExercise> exercises=new ArrayList<>();
            Set<String> seenItems=new HashSet<>();
            for(ManifestExercise e:s.exercises()) {
                CatalogItem item=items.get(e.itemId());
                if(item==null||!seenItems.add(e.itemId()))throw new BusinessException(ErrorCode.INVALID_RELATION,"手册练习映射缺失或重复");
                if(e.exampleId()!=null&&checkedExamples.add(e.exampleId()))repository.solution(course,c.releaseId(),e.exampleId());
                exercises.add(new ManualExercise(item,e.exampleId()));
            }
            result.add(new ManualSection(s.chapterId(),s.title(),s.markdown(),exercises));
        }
        return new Manual(course,c.releaseId(),result);
    }
    private void validateDate(LocalDate date) {
        if(date!=null&&(date.getYear()<1000||date.getYear()>9999))throw new BusinessException(ErrorCode.INVALID_PARAMETER,"日期年份须在1000至9999之间");
    }
    public Page<Note> notes(String course,String q,String tag,LocalDate from,LocalDate to,int page,int size) {
        validateDate(from);
        validateDate(to);
        if(course!=null)learning.course(course);
        if(from!=null&&to!=null&&from.isAfter(to))throw new BusinessException(ErrorCode.INVALID_PARAMETER,"开始日期不能晚于结束日期");
        return repository.notes(CurrentUser.idOrThrow(),course,q,tag,from,to,page,size);
    }
    public Note note(String id) {
        return repository.note(CurrentUser.idOrThrow(),id);
    }
    @Transactional     public Note createNote(NoteWrite w) {
        validateDate(w.noteDate());
        String user=CurrentUser.idOrThrow();
        learning.lockUser(user);
        learning.course(w.courseId().toString());
        return repository.note(user,repository.createNote(user,w));
    }
    @Transactional     public Note updateNote(String id,NoteUpdate w) {
        validateDate(w.noteDate());
        String user=CurrentUser.idOrThrow();
        learning.lockUser(user);
        repository.updateNote(user,id,w);
        return repository.note(user,id);
    }
    public NoteTags tags(String course) {
        if(course!=null)learning.course(course);
        return repository.noteTags(CurrentUser.idOrThrow(),course);
    }
}
