package cn.xuexizhitu.controller;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.service.LegacyService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequiredArgsConstructor
public class LegacyController {
    private final LegacyService service;
    private static String id(UUID id){return id==null?null:id.toString();}
    @GetMapping("/api/v1/exams/history") public ApiResponse<Object> history(@RequestParam(required=false) String kind,@RequestParam(required=false) UUID courseId,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok(service.history(kind,id(courseId),page,size));}
    @GetMapping("/api/v1/exams/history/{legacyId}") public ApiResponse<Object> detail(@PathVariable UUID legacyId){return ApiResponse.ok(service.detail(id(legacyId)));}
    @GetMapping("/api/v1/exams/history/{legacyId}/files/{fileId}") public ApiResponse<Object> file(@PathVariable UUID legacyId,@PathVariable UUID fileId){return ApiResponse.ok(service.file(id(legacyId),id(fileId)));}
    @DeleteMapping("/api/v1/exams/history/{legacyId}/files/{fileId}") public ApiResponse<Object> deleteFile(@PathVariable UUID legacyId,@PathVariable UUID fileId){return ApiResponse.ok(service.deleteFile(id(legacyId),id(fileId)));}
    @GetMapping("/api/v1/admin/exams/legacy-pass-reviews") public ApiResponse<Object> reviews(@RequestParam(required=false) String decision,@RequestParam(required=false) UUID courseId,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok(service.reviews(decision,id(courseId),page,size));}
    @GetMapping("/api/v1/admin/exams/legacy-pass-reviews/{reviewId}") public ApiResponse<Object> review(@PathVariable UUID reviewId){return ApiResponse.ok(service.review(id(reviewId)));}
    @PostMapping("/api/v1/admin/exams/legacy-pass-reviews/{reviewId}/decision") public ApiResponse<Object> decide(@PathVariable UUID reviewId,@RequestBody JsonNode w){return ApiResponse.ok(service.decide(id(reviewId),w));}
    @GetMapping("/api/v1/admin/practice/legacy-credits") public ApiResponse<Object> credits(@RequestParam(required=false) UUID courseId,@RequestParam(required=false) Boolean approved,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok(service.credits(id(courseId),approved,page,size));}
    @PostMapping("/api/v1/admin/practice/legacy-credits/{legacyId}/decision") public ApiResponse<Object> credit(@PathVariable UUID legacyId,@RequestBody JsonNode w){return ApiResponse.ok(service.credit(id(legacyId),w));}
}
