package cn.xuexizhitu.content.api;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.content.application.ContentAdminService;
import cn.xuexizhitu.contract.WorkflowDtos.*;
import cn.xuexizhitu.exams.domain.ExamTypes.Confirm;
import cn.xuexizhitu.learning.api.LearningDtos.Page;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.UUID;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/admin/courses/{courseId}/releases") public class ContentAdminController {
    private final ContentAdminService service;
    @GetMapping public ApiResponse<Page<ContentRelease>> list(@PathVariable UUID courseId,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(service.releases(courseId.toString(),page,size));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)     public ApiResponse<ContentRelease> create(@PathVariable UUID courseId,@Valid @RequestBody ReleaseCreate w) {
        return ApiResponse.ok(service.create(courseId.toString(),w));
    }
    @GetMapping("/{releaseId}/catalog")     public ApiResponse<CatalogDraft> readCatalog(@PathVariable UUID courseId,@PathVariable UUID releaseId) {
        return ApiResponse.ok(service.snapshot(courseId.toString(),releaseId.toString()).catalog());
    }
    @PutMapping("/{releaseId}/catalog")     public ApiResponse<CatalogDraft> putCatalog(@PathVariable UUID courseId,@PathVariable UUID releaseId,             @RequestHeader(value="If-Match",required=false) Long revision,@Valid @RequestBody CatalogDraft w) {
        return ApiResponse.ok(service.catalog(courseId.toString(),releaseId.toString(),w,revision));
    }
    @GetMapping("/{releaseId}/knowledge")     public ApiResponse<KnowledgeDraft> readKnowledge(@PathVariable UUID courseId,@PathVariable UUID releaseId) {
        return ApiResponse.ok(service.snapshot(courseId.toString(),releaseId.toString()).knowledge());
    }
    @PutMapping("/{releaseId}/knowledge")     public ApiResponse<KnowledgeDraft> putKnowledge(@PathVariable UUID courseId,@PathVariable UUID releaseId,             @RequestHeader(value="If-Match",required=false) Long revision,@Valid @RequestBody KnowledgeDraft w) {
        return ApiResponse.ok(service.knowledge(courseId.toString(),releaseId.toString(),w,revision));
    }
    @GetMapping("/{releaseId}/questions")     public ApiResponse<QuestionBankDraft> readQuestions(@PathVariable UUID courseId,@PathVariable UUID releaseId) {
        return ApiResponse.ok(service.snapshot(courseId.toString(),releaseId.toString()).questions());
    }
    @PutMapping("/{releaseId}/questions")     public ApiResponse<QuestionBankDraft> putQuestions(@PathVariable UUID courseId,@PathVariable UUID releaseId,             @RequestHeader(value="If-Match",required=false) Long revision,@Valid @RequestBody QuestionBankDraft w) {
        return ApiResponse.ok(service.questions(courseId.toString(),releaseId.toString(),w,revision));
    }
    @GetMapping("/{releaseId}/task-templates")     public ApiResponse<TemplateDraft> readTemplates(@PathVariable UUID courseId,@PathVariable UUID releaseId) {
        return ApiResponse.ok(service.snapshot(courseId.toString(),releaseId.toString()).templates());
    }
    @PutMapping("/{releaseId}/task-templates")     public ApiResponse<TemplateDraft> putTemplates(@PathVariable UUID courseId,@PathVariable UUID releaseId,             @RequestHeader(value="If-Match",required=false) Long revision,@Valid @RequestBody TemplateDraft w) {
        return ApiResponse.ok(service.templates(courseId.toString(),releaseId.toString(),w,revision));
    }
    @GetMapping("/{releaseId}/assessment-policy")     public ApiResponse<AssessmentPolicy> readPolicy(@PathVariable UUID courseId,@PathVariable UUID releaseId) {
        return ApiResponse.ok(service.snapshot(courseId.toString(),releaseId.toString()).policy());
    }
    @PutMapping("/{releaseId}/assessment-policy")     public ApiResponse<AssessmentPolicy> putPolicy(@PathVariable UUID courseId,@PathVariable UUID releaseId,             @RequestHeader(value="If-Match",required=false) Long revision,@Valid @RequestBody AssessmentPolicyWrite w) {
        return ApiResponse.ok(service.policy(courseId.toString(),releaseId.toString(),w,revision));
    }
    @GetMapping("/{releaseId}/validation")     public ApiResponse<ReleaseValidation> validation(@PathVariable UUID courseId,@PathVariable UUID releaseId) {
        return ApiResponse.ok(service.validation(courseId.toString(),releaseId.toString()));
    }
    @PostMapping("/{releaseId}/publication")     public ApiResponse<ContentRelease> publication(@PathVariable UUID courseId,@PathVariable UUID releaseId,             @RequestHeader(value="If-Match",required=false) Long revision,@Valid @RequestBody Confirm w) {
        return ApiResponse.ok(service.publish(courseId.toString(),releaseId.toString(),w,revision));
    }
}
