package cn.xuexizhitu.controller;

import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.dto.PracticeDtos.*;
import cn.xuexizhitu.dto.LearningDtos.Page;
import cn.xuexizhitu.service.PracticeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/practice") @RequiredArgsConstructor
public class PracticeController {
    private final PracticeService service;
    private static String id(UUID id){return id==null?null:id.toString();}
    @GetMapping("/courses/{courseId}/overview")
    public ApiResponse<Overview> overview(@PathVariable UUID courseId){return ApiResponse.ok(service.overview(id(courseId)));}
    @GetMapping("/courses/{courseId}/stats")
    public ApiResponse<Stats> stats(@PathVariable UUID courseId,@RequestParam(required=false) UUID chapterId){return ApiResponse.ok(service.stats(id(courseId),id(chapterId)));}
    @GetMapping("/courses/{courseId}/questions")
    public ApiResponse<Page<QuestionPublic>> questions(@PathVariable UUID courseId,@RequestParam(required=false) UUID chapterId,
            @RequestParam(required=false) Mode mode,@RequestParam(defaultValue="ALL") Filter filter,@RequestParam(required=false) @Min(1) @Max(5) Integer difficulty,
            @RequestParam(required=false) @Size(max=200) String q,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(service.questions(id(courseId),id(chapterId),mode,filter,difficulty,q,page,size));
    }
    @GetMapping("/courses/{courseId}/questions/{questionId}")
    public ApiResponse<QuestionPublic> question(@PathVariable UUID courseId,@PathVariable UUID questionId){return ApiResponse.ok(service.question(id(courseId),id(questionId)));}
    @PostMapping("/courses/{courseId}/questions/{questionId}/submissions") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Result> submit(@PathVariable UUID courseId,@PathVariable UUID questionId,@RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody AnswerWrite w){return ApiResponse.ok(service.submit(id(courseId),id(questionId),id(key),w));}
    @GetMapping("/courses/{courseId}/submissions")
    public ApiResponse<Page<HistoryRow>> history(@PathVariable UUID courseId,@RequestParam(required=false) UUID questionId,@RequestParam(required=false) UUID chapterId,
            @RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok(service.history(id(courseId),id(questionId),id(chapterId),page,size));}
    @GetMapping("/courses/{courseId}/submissions/{submissionId}")
    public ApiResponse<Result> result(@PathVariable UUID courseId,@PathVariable UUID submissionId){return ApiResponse.ok(service.result(id(courseId),id(submissionId)));}
    @PutMapping("/courses/{courseId}/questions/{questionId}/mark")
    public ApiResponse<QuestionMark> mark(@PathVariable UUID courseId,@PathVariable UUID questionId,@Valid @RequestBody MarkWrite w){return ApiResponse.ok(service.mark(id(courseId),id(questionId),w));}
    @GetMapping("/history")
    public ApiResponse<Page<LegacySummary>> legacy(@RequestParam(required=false) UUID courseId,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok(service.legacy(id(courseId),page,size));}
}
