package cn.xuexizhitu.config;
import cn.xuexizhitu.repository.AssessmentRepository;
import cn.xuexizhitu.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

@Configuration @EnableScheduling @RequiredArgsConstructor @Slf4j
@ConditionalOnProperty(name="app.assessments.timeout-enabled",havingValue="true",matchIfMissing=true)
public class AssessmentTimeoutWorker {
    private final AssessmentRepository repository;private final AssessmentService service;
    @Scheduled(fixedDelayString="${app.assessments.timeout-poll-ms:5000}")
    public void settle(){for(String id:repository.pending())try{service.settleTimeout(id);}catch(Exception e){log.error("Failed to settle assessment {}",id,e);}}
}
