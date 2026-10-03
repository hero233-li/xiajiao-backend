package cn.xuexizhitu.grading.api;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.grading.application.GradingService;
import cn.xuexizhitu.grading.domain.GradingTypes.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/grading-worker") public class WorkerController {
    private final GradingService service;
    @PostMapping("/heartbeat") public ApiResponse<Void> heartbeat(@AuthenticationPrincipal WorkerPrincipal p,@Valid @RequestBody Heartbeat h) {
        service.heartbeat(p,h);
        return ApiResponse.ok(null);
    }
    @PostMapping("/claim") public ApiResponse<Claim> claim(@AuthenticationPrincipal WorkerPrincipal p) {
        return ApiResponse.ok(service.claim(p));
    }
    @PostMapping("/tasks/{id}/renew") public ApiResponse<Claim> renew(@AuthenticationPrincipal WorkerPrincipal p,@PathVariable UUID id,@Valid @RequestBody Lease l) {
        return ApiResponse.ok(service.renew(p,id.toString(),l));
    }
    @GetMapping("/tasks/{id}/materials/{file}") public ApiResponse<Material> material(@AuthenticationPrincipal WorkerPrincipal p,@PathVariable UUID id,@PathVariable UUID file,@RequestHeader("X-Claim-Token") UUID nonce) {
        return ApiResponse.ok(service.material(p,id.toString(),file.toString(),nonce));
    }
    @PostMapping("/tasks/{id}/result") public ApiResponse<Task> result(@AuthenticationPrincipal WorkerPrincipal p,@PathVariable UUID id,@Valid @RequestBody Callback c) {
        return ApiResponse.ok(service.callback(p,id.toString(),c));
    }
    @PostMapping("/tasks/{id}/failure") public ApiResponse<Task> failure(@AuthenticationPrincipal WorkerPrincipal p,@PathVariable UUID id,@Valid @RequestBody Failure f) {
        return ApiResponse.ok(service.failure(p,id.toString(),f));
    }
}
