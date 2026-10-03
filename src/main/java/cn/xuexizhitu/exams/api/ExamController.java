package cn.xuexizhitu.exams.api;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.exams.application.ExamService;
import cn.xuexizhitu.exams.domain.ExamTypes.*;
import cn.xuexizhitu.files.domain.FileTypes.*;
import cn.xuexizhitu.learning.api.LearningDtos.Page;
import cn.xuexizhitu.assessment.api.AssessmentDtos.Unlock;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/exams/courses/{courseId}") public class ExamController {
    private final ExamService service;
    private static String id(UUID id) {
        return id==null?null:id.toString();
    }
    @PostMapping("/cycles/{cycleId}/overrides") @ResponseStatus(HttpStatus.CREATED)     public ApiResponse<Unlock> confirm(@PathVariable UUID courseId,@PathVariable UUID cycleId,@Valid @RequestBody Confirm w) {
        return ApiResponse.ok(service.confirmOverride(id(courseId),id(cycleId),w));
    }
    @PostMapping("/cycles/{cycleId}/overrides/current/revocation")     public ApiResponse<Unlock> revoke(@PathVariable UUID courseId,@PathVariable UUID cycleId,@Valid @RequestBody Revoke w) {
        return ApiResponse.ok(service.revokeOverride(id(courseId),id(cycleId),w));
    }
    @GetMapping("/papers")     public ApiResponse<Page<Paper>> papers(@PathVariable UUID courseId,@RequestParam UUID cycleId,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(service.papers(id(courseId),id(cycleId),page,size));
    }
    @GetMapping("/papers/{paperId}")     public ApiResponse<Paper> paper(@PathVariable UUID courseId,@PathVariable UUID paperId) {
        return ApiResponse.ok(service.paper(id(courseId),id(paperId)));
    }
    @GetMapping("/papers/{paperId}/file")     public ApiResponse<Download> file(@PathVariable UUID courseId,@PathVariable UUID paperId,@RequestParam UUID cycleId,@RequestParam String part) {
        return ApiResponse.ok(service.paperFile(id(courseId),id(paperId),id(cycleId),part));
    }
    @GetMapping("/scores")     public ApiResponse<Page<ScoreRecord>> scores(@PathVariable UUID courseId,@RequestParam(required=false) UUID cycleId,@RequestParam(required=false) UUID paperId,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(service.scores(id(courseId),id(cycleId),id(paperId),page,size));
    }
    @PostMapping("/scores") @ResponseStatus(HttpStatus.CREATED)     public ApiResponse<Mutation> create(@PathVariable UUID courseId,@RequestHeader(value="Idempotency-Key",required=false) UUID key,@Valid @RequestBody CreateScore w) {
        return ApiResponse.ok(service.createScore(id(courseId),w,id(key)));
    }
    @GetMapping("/scores/{scoreId}")     public ApiResponse<ScoreRecord> score(@PathVariable UUID courseId,@PathVariable UUID scoreId) {
        return ApiResponse.ok(service.score(id(courseId),id(scoreId)));
    }
    @PutMapping("/scores/{scoreId}")     public ApiResponse<Mutation> update(@PathVariable UUID courseId,@PathVariable UUID scoreId,@Valid @RequestBody UpdateScore w) {
        return ApiResponse.ok(service.updateScore(id(courseId),id(scoreId),w));
    }
    @GetMapping("/prediction")     public ApiResponse<Prediction> prediction(@PathVariable UUID courseId) {
        return ApiResponse.ok(service.prediction(id(courseId)));
    }
    @GetMapping("/score-trend")     public ApiResponse<Trend> trend(@PathVariable UUID courseId,@RequestParam(defaultValue="12") @Min(1) @Max(100) int limit) {
        return ApiResponse.ok(service.trend(id(courseId),limit));
    }
    @PostMapping(value="/scores/{scoreId}/images",consumes="multipart/form-data") @ResponseStatus(HttpStatus.CREATED)     public ApiResponse<Metadata> upload(@PathVariable UUID courseId,@PathVariable UUID scoreId,@RequestPart MultipartFile file) {
        return ApiResponse.ok(service.uploadImage(id(courseId),id(scoreId),file));
    }
    @GetMapping("/scores/{scoreId}/images/{fileId}")     public ApiResponse<Download> image(@PathVariable UUID courseId,@PathVariable UUID scoreId,@PathVariable UUID fileId) {
        return ApiResponse.ok(service.image(id(courseId),id(scoreId),id(fileId)));
    }
    @DeleteMapping("/scores/{scoreId}/images/{fileId}")     public ApiResponse<Deletion> delete(@PathVariable UUID courseId,@PathVariable UUID scoreId,@PathVariable UUID fileId) {
        return ApiResponse.ok(service.deleteImage(id(courseId),id(scoreId),id(fileId)));
    }
    @GetMapping("/scores/{scoreId}/revisions")     public ApiResponse<Page<Revision>> revisions(@PathVariable UUID courseId,@PathVariable UUID scoreId,             @RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(service.revisions(id(courseId),id(scoreId),page,size));
    }
}
