package cn.xuexizhitu.operations.api;
import cn.xuexizhitu.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
@RestController @RequiredArgsConstructor public class ReadinessController {
    private final JdbcTemplate jdbc;
    public record Readiness(String status,String database) {
    }
    @GetMapping("/api/v1/health/readiness")     public ResponseEntity<ApiResponse<Readiness>> readiness() {
        try {
            jdbc.queryForObject("SELECT 1",Integer.class);
            return ResponseEntity.ok(ApiResponse.ok(new Readiness("UP","UP")));
        }
        catch(org.springframework.dao.DataAccessException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiResponse<>(50301,null,"数据库暂不可用"));
        }
    }
}
