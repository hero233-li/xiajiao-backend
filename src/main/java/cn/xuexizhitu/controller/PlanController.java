package cn.xuexizhitu.controller;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.service.PlanService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.UUID;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/schedule/plans")
public class PlanController {
    private final PlanService service;
    @GetMapping public ApiResponse<Object> list(@RequestParam(required=false) UUID cycleId,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok(service.list(cycleId==null?null:cycleId.toString(),page,size));}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<Object> create(@RequestBody JsonNode w){return ApiResponse.ok(service.create(w));}
    @GetMapping("/{planId}") public ApiResponse<Object> get(@PathVariable UUID planId){return ApiResponse.ok(service.get(planId.toString(),null));}
    @GetMapping("/{planId}/revisions/{revisionNo}") public ApiResponse<Object> revision(@PathVariable UUID planId,@PathVariable @Min(1) int revisionNo){return ApiResponse.ok(service.get(planId.toString(),revisionNo));}
    @PutMapping("/{planId}/tasks/{taskId}/completion") public ApiResponse<Object> complete(@PathVariable UUID planId,@PathVariable UUID taskId,@RequestBody JsonNode w){return ApiResponse.ok(service.complete(planId.toString(),taskId.toString(),w));}
    @PostMapping("/{planId}/reschedule-previews") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<Object> preview(@PathVariable UUID planId,@RequestBody JsonNode w){return ApiResponse.ok(service.preview(planId.toString(),w));}
    @GetMapping("/{planId}/reschedule-previews/{previewId}") public ApiResponse<Object> previewGet(@PathVariable UUID planId,@PathVariable UUID previewId){return ApiResponse.ok(service.previewGet(planId.toString(),previewId.toString()));}
    @PostMapping("/{planId}/reschedule-previews/{previewId}/confirmation") public ApiResponse<Object> confirm(@PathVariable UUID planId,@PathVariable UUID previewId,@RequestBody JsonNode w){return ApiResponse.ok(service.confirm(planId.toString(),previewId.toString(),w));}
}
