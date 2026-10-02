package cn.xuexizhitu.controller;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.service.ExamService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/exams/courses/{courseId}")
public class ExamController {
    private final ExamService service;
    private static String id(UUID id){return id==null?null:id.toString();}
    @PostMapping("/cycles/{cycleId}/overrides") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Object> confirm(@PathVariable UUID courseId,@PathVariable UUID cycleId,@RequestBody JsonNode w){return ApiResponse.ok(service.confirmOverride(id(courseId),id(cycleId),w));}
    @PostMapping("/cycles/{cycleId}/overrides/current/revocation")
    public ApiResponse<Object> revoke(@PathVariable UUID courseId,@PathVariable UUID cycleId,@RequestBody JsonNode w){return ApiResponse.ok(service.revokeOverride(id(courseId),id(cycleId),w));}
    @GetMapping("/papers")
    public ApiResponse<Object> papers(@PathVariable UUID courseId,@RequestParam UUID cycleId,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok(service.papers(id(courseId),id(cycleId),page,size));}
    @GetMapping("/papers/{paperId}")
    public ApiResponse<Object> paper(@PathVariable UUID courseId,@PathVariable UUID paperId){return ApiResponse.ok(service.paper(id(courseId),id(paperId)));}
    @GetMapping("/papers/{paperId}/file")
    public ApiResponse<Object> file(@PathVariable UUID courseId,@PathVariable UUID paperId,@RequestParam UUID cycleId,@RequestParam String part){return ApiResponse.ok(service.paperFile(id(courseId),id(paperId),id(cycleId),part));}
    @GetMapping("/scores")
    public ApiResponse<Object> scores(@PathVariable UUID courseId,@RequestParam(required=false) UUID cycleId,@RequestParam(required=false) UUID paperId,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok(service.scores(id(courseId),id(cycleId),id(paperId),page,size));}
    @PostMapping("/scores") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Object> create(@PathVariable UUID courseId,@RequestBody JsonNode w){return ApiResponse.ok(service.writeScore(id(courseId),null,w));}
    @GetMapping("/scores/{scoreId}")
    public ApiResponse<Object> score(@PathVariable UUID courseId,@PathVariable UUID scoreId){return ApiResponse.ok(service.score(id(courseId),id(scoreId)));}
    @PutMapping("/scores/{scoreId}")
    public ApiResponse<Object> update(@PathVariable UUID courseId,@PathVariable UUID scoreId,@RequestBody JsonNode w){return ApiResponse.ok(service.writeScore(id(courseId),id(scoreId),w));}
    @GetMapping("/prediction")
    public ApiResponse<Object> prediction(@PathVariable UUID courseId){return ApiResponse.ok(service.prediction(id(courseId)));}
    @GetMapping("/score-trend")
    public ApiResponse<Object> trend(@PathVariable UUID courseId,@RequestParam(defaultValue="12") @Min(1) @Max(100) int limit){return ApiResponse.ok(service.trend(id(courseId),limit));}
    @PostMapping(value="/scores/{scoreId}/images",consumes="multipart/form-data") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Object> upload(@PathVariable UUID courseId,@PathVariable UUID scoreId,@RequestPart MultipartFile file){return ApiResponse.ok(service.uploadImage(id(courseId),id(scoreId),file));}
    @GetMapping("/scores/{scoreId}/images/{fileId}")
    public ApiResponse<Object> image(@PathVariable UUID courseId,@PathVariable UUID scoreId,@PathVariable UUID fileId){return ApiResponse.ok(service.image(id(courseId),id(scoreId),id(fileId)));}
    @DeleteMapping("/scores/{scoreId}/images/{fileId}")
    public ApiResponse<Object> delete(@PathVariable UUID courseId,@PathVariable UUID scoreId,@PathVariable UUID fileId){return ApiResponse.ok(service.deleteImage(id(courseId),id(scoreId),id(fileId)));}
}
