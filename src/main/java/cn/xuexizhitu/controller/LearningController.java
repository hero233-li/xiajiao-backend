package cn.xuexizhitu.controller;

import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.dto.LearningDtos.*;
import cn.xuexizhitu.service.LearningService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor
public class LearningController {
    private final LearningService service;
    @GetMapping("/courses")
    public ApiResponse<Page<Course>> courses(@RequestParam UUID cycleId,@RequestParam(required=false) CourseType courseType,
                                            @RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(service.courses(cycleId.toString(),courseType,page,size));
    }
    @GetMapping("/courses/{courseId}")
    public ApiResponse<Course> course(@PathVariable UUID courseId,@RequestParam UUID cycleId) {return ApiResponse.ok(service.course(courseId.toString(),cycleId.toString()));}
    @GetMapping("/courses/by-code/{courseCode}")
    public ApiResponse<Course> courseByCode(@PathVariable @Pattern(regexp="^[0-9]{5}$") String courseCode,@RequestParam UUID cycleId) {return ApiResponse.ok(service.courseByCode(courseCode,cycleId.toString()));}
    @GetMapping("/exams/cycles")
    public ApiResponse<Page<ExamCycle>> cycles(@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {return ApiResponse.ok(service.cycles(page,size));}
    @GetMapping("/exams/cycles/{cycleId}")
    public ApiResponse<ExamCycle> cycle(@PathVariable UUID cycleId) {return ApiResponse.ok(service.cycle(cycleId.toString()));}
    @GetMapping("/courses/{courseId}/enrollments/{cycleId}")
    public ApiResponse<Enrollment> enrollment(@PathVariable UUID courseId,@PathVariable UUID cycleId) {return ApiResponse.ok(service.enrollment(courseId.toString(),cycleId.toString()));}
    @PutMapping("/courses/{courseId}/enrollments/{cycleId}")
    public ApiResponse<Enrollment> saveEnrollment(@PathVariable UUID courseId,@PathVariable UUID cycleId,@Valid @RequestBody EnrollmentWrite write) {return ApiResponse.ok(service.saveEnrollment(courseId.toString(),cycleId.toString(),write));}
    @GetMapping("/catalog/courses/{courseId}")
    public ApiResponse<Catalog> catalog(@PathVariable UUID courseId) {return ApiResponse.ok(service.catalog(courseId.toString()));}
    @PutMapping("/catalog/courses/{courseId}/items/{itemId}/completion")
    public ApiResponse<CatalogCompletion> complete(@PathVariable UUID courseId,@PathVariable UUID itemId,@Valid @RequestBody CompletionWrite write) {return ApiResponse.ok(service.complete(courseId.toString(),itemId.toString(),write));}
    @PatchMapping("/catalog/courses/{courseId}/completions")
    public ApiResponse<BatchCompletion> batch(@PathVariable UUID courseId,@Valid @RequestBody BatchCompletionWrite write) {return ApiResponse.ok(service.completeBatch(courseId.toString(),write));}
    @GetMapping("/courses/{courseId}/learning-position")
    public ApiResponse<LearningPosition> position(@PathVariable UUID courseId) {return ApiResponse.ok(service.position(courseId.toString()));}
    @PutMapping("/courses/{courseId}/learning-position")
    public ApiResponse<LearningPosition> savePosition(@PathVariable UUID courseId,@Valid @RequestBody LearningPositionWrite write) {return ApiResponse.ok(service.savePosition(courseId.toString(),write));}
}
