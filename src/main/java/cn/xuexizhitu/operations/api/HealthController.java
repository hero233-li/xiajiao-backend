package cn.xuexizhitu.operations.api;
import cn.xuexizhitu.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
@RestController @Tag(name="system",description="系统健康检查") public class HealthController {
    public record HealthData(String status) {
    }
    @GetMapping("/api/v1/health") @Operation(summary="进程存活检查",description="只检查HTTP进程存活，不代表数据库就绪")     public ApiResponse<HealthData> health() {
        return ApiResponse.ok(new HealthData("UP"));
    }
}
