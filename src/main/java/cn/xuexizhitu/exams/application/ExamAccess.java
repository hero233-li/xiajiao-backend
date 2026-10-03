package cn.xuexizhitu.exams.application;
import cn.xuexizhitu.common.*;
import cn.xuexizhitu.assessment.infrastructure.AssessmentRepository;
import cn.xuexizhitu.learning.infrastructure.LearningRepository;
import cn.xuexizhitu.assessment.api.AssessmentDtos.Unlock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
@Service @RequiredArgsConstructor public class ExamAccess {
    private final LearningRepository learning;
    private final AssessmentRepository assessments;
    public Unlock unlock(String user,String course,String cycle) {
        learning.requireCycleCourse(course,cycle);
        return assessments.unlock(user,course,cycle,learning.course(course).releaseId());
    }
    public void requireWrite(String user,String course,String cycle) {
        if(!unlock(user,course,cycle).canWriteScores())throw new BusinessException(ErrorCode.EXAMS_LOCKED,"真题或成绩录入尚未解锁");
    }
}
