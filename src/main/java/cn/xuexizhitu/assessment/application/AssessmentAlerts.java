package cn.xuexizhitu.assessment.application;
import cn.xuexizhitu.assessment.infrastructure.AssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @RequiredArgsConstructor public class AssessmentAlerts {
    private final AssessmentRepository repository;
    @Transactional(propagation=Propagation.REQUIRES_NEW)     public void record(String course,String chapter,String release,String reason) {
        repository.alert(course,chapter,release,reason);
    }
}
