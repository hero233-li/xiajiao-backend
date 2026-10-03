package cn.xuexizhitu.assessment.api;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.assessment.api.AssessmentDtos.*;
import cn.xuexizhitu.learning.api.LearningDtos.Page;
import cn.xuexizhitu.assessment.application.AssessmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/practice/courses/{courseId}") @RequiredArgsConstructor public class AssessmentController {
    private final AssessmentService service;
    private static String id(UUID id) {
        return id==null?null:id.toString();
    }
    @PostMapping("/assessments") @ResponseStatus(HttpStatus.CREATED)     public ApiResponse<Session> apply(@PathVariable UUID courseId,@RequestParam UUID cycleId,@RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody Apply w) {
        return ApiResponse.ok(service.apply(id(courseId),id(cycleId),id(key),w));
    }
    @GetMapping("/assessments")     public ApiResponse<Page<Summary>> list(@PathVariable UUID courseId,@RequestParam(required=false) Kind kind,@RequestParam(required=false) UUID chapterId,@RequestParam(required=false) Status status,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(service.list(id(courseId),kind,id(chapterId),status,page,size));
    }
    @GetMapping("/assessments/{sessionId}")     public ApiResponse<Session> session(@PathVariable UUID courseId,@PathVariable UUID sessionId) {
        return ApiResponse.ok(service.session(id(courseId),id(sessionId)));
    }
    @PutMapping("/assessments/{sessionId}/answers/{revisionId}")     public ApiResponse<SavedAnswer> save(@PathVariable UUID courseId,@PathVariable UUID sessionId,@PathVariable UUID revisionId,@Valid @RequestBody AnswerWrite w) {
        return ApiResponse.ok(service.save(id(courseId),id(sessionId),id(revisionId),w));
    }
    @PostMapping("/assessments/{sessionId}/submission")     public ApiResponse<Result> submit(@PathVariable UUID courseId,@PathVariable UUID sessionId,@RequestParam UUID cycleId,@Valid @RequestBody Submit w) {
        return ApiResponse.ok(service.submit(id(courseId),id(sessionId),id(cycleId),w));
    }
    @GetMapping("/assessments/{sessionId}/result")     public ApiResponse<Result> result(@PathVariable UUID courseId,@PathVariable UUID sessionId,@RequestParam UUID cycleId) {
        return ApiResponse.ok(service.result(id(courseId),id(sessionId),id(cycleId)));
    }
}
