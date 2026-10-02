package cn.xuexizhitu.service;

import cn.xuexizhitu.common.*;
import cn.xuexizhitu.dto.PracticeDtos.*;
import cn.xuexizhitu.dto.LearningDtos.Page;
import cn.xuexizhitu.repository.*;
import cn.xuexizhitu.repository.LearningRepository.CourseRow;
import cn.xuexizhitu.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class PracticeService {
    private final LearningRepository learning;
    private final PracticeRepository repository;
    private CourseRow published(String course){CourseRow c=learning.course(course);if(c.releaseId()==null)throw new BusinessException(ErrorCode.NOT_FOUND);return c;}
    public Page<QuestionPublic> questions(String course,String chapter,Mode mode,Filter filter,Integer difficulty,String q,int page,int size) {
        CourseRow c=published(course);repository.requireChapter(course,c.releaseId(),chapter);
        return repository.questions(CurrentUser.idOrThrow(),course,c.releaseId(),chapter,mode,filter,difficulty,q,page,size);
    }
    public QuestionPublic question(String course,String question){CourseRow c=published(course);return repository.question(CurrentUser.idOrThrow(),course,c.releaseId(),question);}
    public Stats stats(String course,String chapter){CourseRow c=published(course);repository.requireChapter(course,c.releaseId(),chapter);return repository.stats(CurrentUser.idOrThrow(),course,c.releaseId(),chapter);}
    public Overview overview(String course){CourseRow c=published(course);return repository.overview(CurrentUser.idOrThrow(),course,c.releaseId());}
    @Transactional
    public Result submit(String course,String question,String key,AnswerWrite w) {
        String user=CurrentUser.idOrThrow();learning.lockUser(user);CourseRow c=published(course);
        var previous=repository.replay(user,key);
        if(previous!=null) {
            if(!previous.question().equals(question)||!previous.revision().equals(w.revisionId().toString())||previous.selected()!=w.selectedOption())throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
            repository.questionChapter(course,question);return savedResult(user,c,previous);
        }
        QuestionPublic current=repository.question(user,course,c.releaseId(),question);
        if(!current.revisionId().equals(w.revisionId().toString()))throw new BusinessException(ErrorCode.CONFLICT,"题目版本已更新，请重新读取");
        if(w.selectedOption()>=current.options().size())throw new BusinessException(ErrorCode.INVALID_RELATION,"选项索引超出题目范围");
        var s=repository.insert(user,current,w.selectedOption(),key);
        Result result=repository.result(s,repository.stats(user,course,c.releaseId(),current.chapterId()));repository.saveSnapshot(s.id(),result);return result;
    }
    private Result savedResult(String user,CourseRow c,PracticeRepository.Submission s) {
        if(s.snapshot()!=null)return repository.result(s,null);
        return repository.result(s,repository.stats(user,c.id(),c.releaseId(),repository.questionChapter(c.id(),s.question())));
    }
    public Result result(String course,String id){CourseRow c=published(course);String user=CurrentUser.idOrThrow();return savedResult(user,c,repository.submission(user,course,id));}
    public Page<HistoryRow> history(String course,String question,String chapter,int page,int size){CourseRow c=published(course);repository.requireChapter(course,c.releaseId(),chapter);if(question!=null)repository.questionChapter(course,question);return repository.history(CurrentUser.idOrThrow(),course,question,chapter,page,size);}
    @Transactional
    public QuestionMark mark(String course,String question,MarkWrite w){String user=CurrentUser.idOrThrow();learning.lockUser(user);CourseRow c=published(course);repository.question(user,course,c.releaseId(),question);return repository.mark(user,question,w);}
    public Page<LegacySummary> legacy(String course,int page,int size){if(course!=null)learning.course(course);return repository.legacy(CurrentUser.idOrThrow(),course,page,size);}
}
