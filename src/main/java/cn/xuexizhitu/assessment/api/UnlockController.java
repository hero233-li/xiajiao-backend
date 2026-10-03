package cn.xuexizhitu.assessment.api;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.assessment.api.AssessmentDtos.Unlock;
import cn.xuexizhitu.assessment.application.AssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequiredArgsConstructor public class UnlockController {
    private final AssessmentService service;
    @GetMapping("/api/v1/exams/courses/{courseId}/unlock")     public ApiResponse<Unlock> unlock(@PathVariable UUID courseId,@RequestParam UUID cycleId) {
        return ApiResponse.ok(service.unlock(courseId.toString(),cycleId.toString()));
    }
}
