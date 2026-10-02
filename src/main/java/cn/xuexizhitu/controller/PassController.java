package cn.xuexizhitu.controller;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.service.AdminService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequiredArgsConstructor
public class PassController {
    private final AdminService service;
    @GetMapping("/api/v1/practice/courses/{courseId}/passes")
    public ApiResponse<Object> passes(@PathVariable UUID courseId,@RequestParam(required=false) String kind,@RequestParam(required=false) UUID chapterId,@RequestParam(defaultValue="true") boolean validOnly,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok(service.passes(courseId.toString(),kind,chapterId==null?null:chapterId.toString(),validOnly,page,size));}
}
