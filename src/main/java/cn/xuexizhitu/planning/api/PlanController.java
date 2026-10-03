package cn.xuexizhitu.planning.api;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.planning.application.PlanService;
import cn.xuexizhitu.contract.WorkflowDtos.PlanCreate;
import cn.xuexizhitu.contract.WorkflowDtos.TaskCompletionWrite;
import cn.xuexizhitu.contract.WorkflowDtos.PreviewConfirm;
import cn.xuexizhitu.contract.WorkflowDtos.RescheduleRequest;
import cn.xuexizhitu.planning.domain.PlanTypes.*;
import cn.xuexizhitu.learning.api.LearningDtos.Page;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.UUID;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/schedule/plans") public class PlanController {
    private final PlanService service;
    @GetMapping public ApiResponse<Page<Summary>> list(@RequestParam(required=false) UUID cycleId,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(service.list(cycleId==null?null:cycleId.toString(),page,size));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<Plan> create(@Valid @RequestBody PlanCreate w) {
        return ApiResponse.ok(service.create(w));
    }
    @GetMapping("/{planId}") public ApiResponse<Plan> get(@PathVariable UUID planId) {
        return ApiResponse.ok(service.get(planId.toString(),null));
    }
    @GetMapping("/{planId}/revisions/{revisionNo}") public ApiResponse<Plan> revision(@PathVariable UUID planId,@PathVariable @Min(1) int revisionNo) {
        return ApiResponse.ok(service.get(planId.toString(),revisionNo));
    }
    @PutMapping("/{planId}/tasks/{taskId}/completion") public ApiResponse<Completion> complete(@PathVariable UUID planId,@PathVariable UUID taskId,@Valid @RequestBody TaskCompletionWrite w) {
        return ApiResponse.ok(service.complete(planId.toString(),taskId.toString(),w));
    }
    @PostMapping("/{planId}/reschedule-previews") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<Preview> preview(@PathVariable UUID planId,@Valid @RequestBody RescheduleRequest w) {
        return ApiResponse.ok(service.preview(planId.toString(),w));
    }
    @GetMapping("/{planId}/reschedule-previews/{previewId}") public ApiResponse<Preview> previewGet(@PathVariable UUID planId,@PathVariable UUID previewId) {
        return ApiResponse.ok(service.previewGet(planId.toString(),previewId.toString()));
    }
    @PostMapping("/{planId}/reschedule-previews/{previewId}/confirmation") public ApiResponse<Plan> confirm(@PathVariable UUID planId,@PathVariable UUID previewId,@Valid @RequestBody PreviewConfirm w) {
        return ApiResponse.ok(service.confirm(planId.toString(),previewId.toString(),w));
    }
}
