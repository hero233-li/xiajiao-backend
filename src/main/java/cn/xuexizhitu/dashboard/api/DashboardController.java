package cn.xuexizhitu.dashboard.api;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.dashboard.application.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequiredArgsConstructor public class DashboardController {
    private final DashboardService service;
    @GetMapping("/api/v1/dashboard") public ApiResponse<DashboardService.Dashboard> get(@RequestParam UUID cycleId,@RequestParam(required=false) UUID planId) {
        return ApiResponse.ok(service.dashboard(cycleId==null?null:cycleId.toString(),planId==null?null:planId.toString()));
    }
}
