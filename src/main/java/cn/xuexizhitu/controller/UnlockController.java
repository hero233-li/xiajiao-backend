package cn.xuexizhitu.controller;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.dto.AssessmentDtos.Unlock;
import cn.xuexizhitu.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequiredArgsConstructor
public class UnlockController {
    private final AssessmentService service;
    @GetMapping("/api/v1/exams/courses/{courseId}/unlock")
    public ApiResponse<Unlock> unlock(@PathVariable UUID courseId,@RequestParam UUID cycleId){return ApiResponse.ok(service.unlock(courseId.toString(),cycleId.toString()));}
}
