package cn.xuexizhitu.grading.api;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.grading.application.GradingService;
import cn.xuexizhitu.grading.domain.GradingTypes.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/grading") public class GradingController {
    private final GradingService service;
    @GetMapping("/submissions") public ApiResponse<List<Submission>> list(@RequestParam UUID courseId,@RequestParam UUID cycleId) {
        return ApiResponse.ok(service.submissions(courseId.toString(),cycleId.toString()));
    }
    @PostMapping("/submissions") public ApiResponse<Submission> create(@Valid @RequestBody SubmissionWrite w) {
        return ApiResponse.ok(service.create(w));
    }
    @GetMapping("/submissions/{id}") public ApiResponse<Submission> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.submission(id.toString()));
    }
    @PostMapping(value="/submissions/{id}/pages",consumes="multipart/form-data") public ApiResponse<Submission> upload(@PathVariable UUID id,@RequestParam long expectedRevision,@RequestPart MultipartFile file) {
        return ApiResponse.ok(service.upload(id.toString(),expectedRevision,file));
    }
    @PutMapping("/submissions/{id}/pages") public ApiResponse<Submission> order(@PathVariable UUID id,@Valid @RequestBody PageOrder w) {
        return ApiResponse.ok(service.order(id.toString(),w));
    }
    @DeleteMapping("/submissions/{id}/pages/{file}") public ApiResponse<Submission> remove(@PathVariable UUID id,@PathVariable UUID file,@RequestParam long expectedRevision) {
        return ApiResponse.ok(service.remove(id.toString(),file.toString(),expectedRevision));
    }
    @PostMapping("/submissions/{id}/tasks") public ApiResponse<Task> apply(@PathVariable UUID id,@Valid @RequestBody Apply w) {
        return ApiResponse.ok(service.apply(id.toString(),w));
    }
    @GetMapping("/submissions/{id}/tasks") public ApiResponse<List<Task>> tasks(@PathVariable UUID id) {
        return ApiResponse.ok(service.tasks(id.toString()));
    }
    @GetMapping("/tasks/{id}/inputs") public ApiResponse<Inputs> inputs(@PathVariable UUID id) {
        return ApiResponse.ok(service.inputs(id.toString()));
    }
    @GetMapping("/tasks/{id}/materials/{file}") public ApiResponse<Material> material(@PathVariable UUID id,@PathVariable UUID file) {
        return ApiResponse.ok(service.ownMaterial(id.toString(),file.toString()));
    }
    @GetMapping("/tasks/{id}") public ApiResponse<Task> task(@PathVariable UUID id) {
        return ApiResponse.ok(service.task(id.toString()));
    }
    @PostMapping("/tasks/{id}/review") public ApiResponse<Task> review(@PathVariable UUID id,@Valid @RequestBody Result result) {
        return ApiResponse.ok(service.review(id.toString(),result));
    }
    @GetMapping("/rubrics") public ApiResponse<List<Rubric>> rubrics(@RequestParam UUID courseId,@RequestParam UUID cycleId,@RequestParam UUID paperId) {
        return ApiResponse.ok(service.rubrics(courseId.toString(),cycleId.toString(),paperId.toString()));
    }
    @PostMapping("/rubrics") public ApiResponse<Rubric> rubric(@RequestParam UUID courseId,@RequestParam UUID paperId,@Valid @RequestBody RubricDocument doc) {
        return ApiResponse.ok(service.createRubric(courseId.toString(),paperId.toString(),doc));
    }
    @PutMapping("/rubrics/{id}") public ApiResponse<Rubric> edit(@PathVariable UUID id,@Valid @RequestBody RubricDocument doc) {
        return ApiResponse.ok(service.updateRubric(id.toString(),doc));
    }
    @PostMapping("/rubrics/{id}/publish") public ApiResponse<Rubric> publish(@PathVariable UUID id) {
        return ApiResponse.ok(service.publish(id.toString()));
    }
    @PostMapping("/rubric-tasks") public ApiResponse<Task> draft(@RequestParam UUID courseId,@RequestParam UUID cycleId,@RequestParam UUID paperId) {
        return ApiResponse.ok(service.draft(courseId.toString(),cycleId.toString(),paperId.toString()));
    }
    @GetMapping("/workers") public ApiResponse<List<Worker>> workers() {
        return ApiResponse.ok(service.workers());
    }
    @PostMapping("/workers") public ApiResponse<Pair> pair(@Valid @RequestBody PairWrite w) {
        return ApiResponse.ok(service.pair(w));
    }
    @DeleteMapping("/workers/{id}") public ApiResponse<Void> revoke(@PathVariable UUID id) {
        service.revoke(id.toString());
        return ApiResponse.ok(null);
    }
}
