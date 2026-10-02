package cn.xuexizhitu.controller;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequiredArgsConstructor
public class DashboardController {
    private final DashboardService service;
    @GetMapping("/api/v1/dashboard") public ApiResponse<Object> get(@RequestParam UUID cycleId,@RequestParam(required=false) UUID planId){return ApiResponse.ok(service.dashboard(cycleId==null?null:cycleId.toString(),planId==null?null:planId.toString()));}
}
