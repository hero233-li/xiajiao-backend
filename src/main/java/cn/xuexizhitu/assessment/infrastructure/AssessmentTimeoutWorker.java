package cn.xuexizhitu.assessment.infrastructure;
import cn.xuexizhitu.assessment.infrastructure.AssessmentRepository;
import cn.xuexizhitu.assessment.application.AssessmentService;
import lombok.RequiredArgsConstructor;
import cn.xuexizhitu.operations.infrastructure.BackgroundFailures;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
@Configuration @EnableScheduling @RequiredArgsConstructor @Slf4j @ConditionalOnProperty(name="app.assessments.timeout-enabled",havingValue="true",matchIfMissing=true) public class AssessmentTimeoutWorker {
    private final BackgroundFailures failures;
    private final AssessmentRepository repository;
    private final AssessmentService service;
    @Scheduled(fixedDelayString="${app.assessments.timeout-poll-ms:5000}")     public void settle() {
        for(String id:repository.pending())try {
            service.settleTimeout(id);
            failures.recovered("ASSESSMENT_TIMEOUT",id);
        }
        catch(Exception e) {
            failures.failed("ASSESSMENT_TIMEOUT",id,e);
            log.error("Failed to settle assessment {} ({})",id,e.getClass().getSimpleName());
        }
    }
}
