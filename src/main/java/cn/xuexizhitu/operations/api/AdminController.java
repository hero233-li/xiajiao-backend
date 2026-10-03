package cn.xuexizhitu.operations.api;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.learning.application.CurriculumAdminService;
import cn.xuexizhitu.files.application.FileAdminService;
import cn.xuexizhitu.exams.application.PaperAdminService;
import cn.xuexizhitu.assessment.application.ModerationService;
import cn.xuexizhitu.contract.WorkflowDtos.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/admin") public class AdminController {
    private final CurriculumAdminService curriculum;
    private final FileAdminService files;
    private final PaperAdminService papers;
    private final ModerationService moderation;
    private static String id(UUID id) {
        return id==null?null:id.toString();
    }
    @PostMapping("/courses") @ResponseStatus(HttpStatus.CREATED)     public ApiResponse<cn.xuexizhitu.learning.api.LearningDtos.Course> createCourse(@RequestParam UUID cycleId,@Valid @RequestBody CourseAdminWrite w) {
        return ApiResponse.ok(curriculum.course(null,w,id(cycleId)));
    }
    @PutMapping("/courses/{courseId}")     public ApiResponse<cn.xuexizhitu.learning.api.LearningDtos.Course> course(@PathVariable UUID courseId,@RequestParam UUID cycleId,@Valid @RequestBody CourseAdminWrite w) {
        return ApiResponse.ok(curriculum.course(id(courseId),w,id(cycleId)));
    }
    @PostMapping("/exams/cycles") @ResponseStatus(HttpStatus.CREATED)     public ApiResponse<cn.xuexizhitu.learning.api.LearningDtos.ExamCycle> createCycle(@Valid @RequestBody CycleWrite w) {
        return ApiResponse.ok(curriculum.cycle(null,w));
    }
    @PutMapping("/exams/cycles/{cycleId}")     public ApiResponse<cn.xuexizhitu.learning.api.LearningDtos.ExamCycle> cycle(@PathVariable UUID cycleId,@Valid @RequestBody CycleWrite w) {
        return ApiResponse.ok(curriculum.cycle(id(cycleId),w));
    }
    @GetMapping("/files")     public ApiResponse<cn.xuexizhitu.learning.api.LearningDtos.Page<cn.xuexizhitu.files.domain.FileTypes.Metadata>> files(@RequestParam(required=false) String purpose,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(files.files(purpose,page,size));
    }
    @PostMapping(value="/files",consumes="multipart/form-data") @ResponseStatus(HttpStatus.CREATED)     public ApiResponse<cn.xuexizhitu.files.domain.FileTypes.Metadata> upload(@RequestPart MultipartFile file,@RequestParam String purpose,@RequestParam boolean containsAnswers) {
        return ApiResponse.ok(files.upload(file,purpose,containsAnswers));
    }
    @PostMapping("/exams/courses/{courseId}/papers") @ResponseStatus(HttpStatus.CREATED)     public ApiResponse<cn.xuexizhitu.exams.domain.ExamTypes.Paper> createPaper(@PathVariable UUID courseId,@Valid @RequestBody PaperWrite w) {
        return ApiResponse.ok(papers.paper(id(courseId),null,w));
    }
    @PutMapping("/exams/courses/{courseId}/papers/{paperId}")     public ApiResponse<cn.xuexizhitu.exams.domain.ExamTypes.Paper> paper(@PathVariable UUID courseId,@PathVariable UUID paperId,@Valid @RequestBody PaperWrite w) {
        return ApiResponse.ok(papers.paper(id(courseId),id(paperId),w));
    }
    @PostMapping("/practice/passes/{passId}/invalidation")     public ApiResponse<ModerationService.Invalidation> invalidate(@PathVariable UUID passId,@Valid @RequestBody InvalidatePass w) {
        return ApiResponse.ok(moderation.invalidate(id(passId),w));
    }
    @GetMapping("/practice/alerts")     public ApiResponse<cn.xuexizhitu.learning.api.LearningDtos.Page<cn.xuexizhitu.assessment.infrastructure.ModerationRepository.Alert>> alerts(@RequestParam(required=false) UUID courseId,@RequestParam(required=false) Boolean acknowledged,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(moderation.alerts(id(courseId),acknowledged,page,size));
    }
    @PostMapping("/practice/alerts/{alertId}/acknowledgement")     public ApiResponse<cn.xuexizhitu.assessment.infrastructure.ModerationRepository.Alert> acknowledge(@PathVariable UUID alertId,@Valid @RequestBody cn.xuexizhitu.exams.domain.ExamTypes.Confirm w) {
        return ApiResponse.ok(moderation.acknowledge(id(alertId),w));
    }
    @GetMapping("/dashboard/audit-events")     public ApiResponse<cn.xuexizhitu.learning.api.LearningDtos.Page<cn.xuexizhitu.assessment.infrastructure.ModerationRepository.Audit>> audits(@RequestParam(required=false) @Size(max=100) String action,@RequestParam(required=false) UUID targetId,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(moderation.audits(action,id(targetId),page,size));
    }
}
