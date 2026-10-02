package cn.xuexizhitu.controller;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.service.ContentAdminService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.UUID;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/admin/courses/{courseId}/releases")
public class ContentAdminController {
    private final ContentAdminService service;
    @GetMapping public ApiResponse<Object> list(@PathVariable UUID courseId,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok(service.releases(courseId.toString(),page,size));}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public ApiResponse<Object> create(@PathVariable UUID courseId,@RequestBody JsonNode w){return ApiResponse.ok(service.create(courseId.toString(),w));}
    @GetMapping("/{releaseId}/{section:catalog|knowledge|questions|assessment-policy|task-templates}") public ApiResponse<Object> read(@PathVariable UUID courseId,@PathVariable UUID releaseId,@PathVariable String section){return ApiResponse.ok(service.section(courseId.toString(),releaseId.toString(),section));}
    @PutMapping("/{releaseId}/{section:catalog|knowledge|questions|assessment-policy|task-templates}") public ApiResponse<Object> put(@PathVariable UUID courseId,@PathVariable UUID releaseId,@PathVariable String section,@RequestBody JsonNode w){return ApiResponse.ok(service.put(courseId.toString(),releaseId.toString(),section,w));}
    @GetMapping("/{releaseId}/validation") public ApiResponse<Object> validation(@PathVariable UUID courseId,@PathVariable UUID releaseId){return ApiResponse.ok(service.validation(courseId.toString(),releaseId.toString()));}
    @PostMapping("/{releaseId}/publication") public ApiResponse<Object> publication(@PathVariable UUID courseId,@PathVariable UUID releaseId,@RequestBody JsonNode w){return ApiResponse.ok(service.publish(courseId.toString(),releaseId.toString(),w));}
}
