package cn.xuexizhitu.service;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.dto.AssessmentDtos.*;
import cn.xuexizhitu.dto.LearningDtos.Page;
import cn.xuexizhitu.repository.*;
import cn.xuexizhitu.repository.AssessmentRepository.*;
import cn.xuexizhitu.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.math.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class AssessmentService {
    private final LearningRepository learning;private final PracticeRepository practice;private final AssessmentRepository repository;
    @Transactional(noRollbackFor=BusinessException.class)
    public Session apply(String course,String cycle,String key,Apply w) {
        String user=CurrentUser.idOrThrow(),chapter=w.chapterId()==null?null:w.chapterId().toString();learning.lockUser(user);learning.requireCycleCourse(course,cycle);
        if((w.kind()==Kind.CHAPTER)!=(chapter!=null))throw new BusinessException(ErrorCode.INVALID_RELATION,"章节检测须有章节，模拟卷须无章节");
        Row previous=repository.replay(user,key);
        if(previous!=null){if(!previous.course().equals(course)||!Objects.equals(previous.cycle(),cycle)||previous.kind()!=w.kind()||!Objects.equals(previous.chapter(),chapter))throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);return previous.snapshot()==null?repository.session(previous):repository.replaySession(previous);}
        var c=learning.course(course);if(c.releaseId()==null)throw new BusinessException(ErrorCode.NOT_FOUND);
        String release=c.releaseId();Policy policy=repository.policy(course,release);Map<String,Object> snapshot=new LinkedHashMap<>();snapshot.put("policy",repository.policySnapshot(policy.id()));snapshot.put("cycleId",cycle);snapshot.put("kind",w.kind());
        List<AssessmentPlanner.Candidate> chosen=new ArrayList<>();
        if(w.kind()==Kind.CHAPTER) {
            practice.requireChapter(course,release,chapter);
            if(!repository.participates(release,chapter))throw new BusinessException(ErrorCode.ASSESSMENT_BLOCKED,"此章节不参与检测");
            var stats=practice.stats(user,course,release,chapter);
            if(!stats.canApplyChapterAssessment()) {
                if(stats.blockReasons().stream().anyMatch(s->s.startsWith("题库"))) {repository.alert(course,chapter,release,String.join("；",stats.blockReasons()));throw new BusinessException(ErrorCode.BANK_INSUFFICIENT);}
                throw new BusinessException(ErrorCode.ASSESSMENT_BLOCKED,String.join("；",stats.blockReasons()));
            }
            List<String> points=repository.points(release,chapter);int count=Math.max(policy.minimum(),points.size());
            try{chosen=AssessmentPlanner.chapter(points,repository.candidates(user,course,release,chapter),count);}catch(IllegalArgumentException e){repository.alert(course,chapter,release,e.getMessage());throw new BusinessException(ErrorCode.BANK_INSUFFICIENT);}
        } else {
            List<String> chapters=repository.participating(release);
            if(chapters.isEmpty()||!repository.missing(user,course,release).isEmpty())throw new BusinessException(ErrorCode.ASSESSMENT_BLOCKED,"当前参与检测章节尚未全部通过");
            Map<String,BigDecimal> weights=repository.weights(policy,chapters);snapshot.put("weights",repository.weightSnapshot(policy.id()));
            Map<String,Integer> quotas=AssessmentPlanner.quotas(weights,policy.mockCount());snapshot.put("quotas",quotas);
            for(var quota:quotas.entrySet()) {
                List<AssessmentPlanner.Candidate> bank=new ArrayList<>(repository.candidates(user,course,release,quota.getKey()));Collections.shuffle(bank);bank.sort(Comparator.comparing(AssessmentPlanner.Candidate::answered));
                if(bank.size()<quota.getValue()){repository.alert(course,quota.getKey(),release,"模拟分配题数超过本章可用题量");throw new BusinessException(ErrorCode.BANK_INSUFFICIENT);}
                chosen.addAll(bank.subList(0,quota.getValue()));
            }
            Collections.shuffle(chosen);
        }
        Row row=repository.create(user,course,chapter,cycle,w.kind(),policy,chosen,key,snapshot);Session session=repository.session(row);repository.snapshot(row.id(),session);return session;
    }
    public Session session(String course,String id){return repository.session(repository.owned(CurrentUser.idOrThrow(),course,id,false));}
    public Page<Summary> list(String course,Kind kind,String chapter,Status status,int page,int size){learning.course(course);return repository.list(CurrentUser.idOrThrow(),course,kind,chapter,status,page,size);}
    @Transactional(noRollbackFor=BusinessException.class)
    public SavedAnswer save(String course,String id,String revision,AnswerWrite w) {
        Row row=repository.owned(CurrentUser.idOrThrow(),course,id,true);
        if(row.status()!=Status.IN_PROGRESS)throw new BusinessException(ErrorCode.CONFLICT,"检测已经交卷");
        if(!AssessmentRepository.now().isBefore(row.deadline())){repository.finish(row,Status.TIMED_OUT,row.deadline());throw new BusinessException(ErrorCode.CONFLICT,"检测已截止并结算");}
        List<SessionQuestion> questions=repository.questions(id);SessionQuestion question=questions.stream().filter(q->q.question().revisionId().equals(revision)).findFirst().orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND));
        if(w.selectedOption()>=question.question().options().size())throw new BusinessException(ErrorCode.INVALID_RELATION,"选项超出题目范围");
        if(!Objects.equals(w.expectedSavedAt(),question.answerSavedAt()))throw new BusinessException(ErrorCode.CONFLICT,"最后保存选项已变化，请重新读取");
        Instant at=AssessmentRepository.now();if(question.answerSavedAt()!=null&&!at.isAfter(question.answerSavedAt()))at=question.answerSavedAt().plusNanos(1000);
        if(!at.isBefore(row.deadline())){repository.finish(row,Status.TIMED_OUT,row.deadline());throw new BusinessException(ErrorCode.CONFLICT,"检测已截止并结算");}
        Instant saved=repository.save(id,revision,w.selectedOption(),at);return new SavedAnswer(id,revision,w.selectedOption(),saved,repository.fingerprint(repository.questions(id)),AssessmentRepository.now());
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Result submit(String course,String id,String cycle,Submit w) {
        learning.requireCycleCourse(course,cycle);Row row=repository.owned(CurrentUser.idOrThrow(),course,id,true);
        if(row.status()==Status.IN_PROGRESS) {
            Instant now=AssessmentRepository.now();boolean expired=!now.isBefore(row.deadline());
            if(!expired&&!repository.fingerprint(repository.questions(id)).equals(w.answerFingerprint()))throw new BusinessException(ErrorCode.CONFLICT,"最后保存答案与交卷指纹不同");
            repository.finish(row,expired?Status.TIMED_OUT:Status.SUBMITTED,expired?row.deadline():now);
            row=repository.owned(row.user(),course,id,false);
        }
        return result(row,cycle);
    }
    public Result result(String course,String id,String cycle){learning.requireCycleCourse(course,cycle);return result(repository.owned(CurrentUser.idOrThrow(),course,id,false),cycle);}
    private Result result(Row row,String cycle) {
        if(row.status()==Status.IN_PROGRESS)throw new BusinessException(ErrorCode.ANSWERS_LOCKED);
        String release=learning.course(row.course()).releaseId();
        String statsRelease=release==null||(row.chapter()!=null&&!repository.hasChapter(release,row.chapter()))?row.release():release;
        return new Result(row.id(),row.status(),row.submitted(),row.correct(),row.count(),BigDecimal.valueOf(row.correct()*100L).divide(BigDecimal.valueOf(row.count()),2,RoundingMode.HALF_UP),row.passScore(),row.passed(),repository.pass(row.id()),repository.answers(row.id()),practice.stats(row.user(),row.course(),statsRelease,row.chapter()),repository.unlock(row.user(),row.course(),cycle,release));
    }
    public Unlock unlock(String course,String cycle){learning.requireCycleCourse(course,cycle);return repository.unlock(CurrentUser.idOrThrow(),course,cycle,learning.course(course).releaseId());}
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void settleTimeout(String id){Row row=repository.lock(id);if(row.status()==Status.IN_PROGRESS&&!AssessmentRepository.now().isBefore(row.deadline()))repository.finish(row,Status.TIMED_OUT,row.deadline());}
}
