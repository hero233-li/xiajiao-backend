package cn.xuexizhitu.config;
import cn.xuexizhitu.service.PrivateFileStore;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
@Configuration @EnableScheduling @RequiredArgsConstructor
@ConditionalOnProperty(name="app.files.cleanup-enabled",havingValue="true",matchIfMissing=true)
public class PrivateFileCleanup {
    private final PrivateFileStore files;
    @Scheduled(fixedDelayString="${app.files.cleanup-poll-ms:30000}") public void cleanup(){files.cleanup();}
}
