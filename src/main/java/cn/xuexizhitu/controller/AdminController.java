package cn.xuexizhitu.controller;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.service.AdminService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/admin")
public class AdminController {
    private final AdminService service;
    private static String id(UUID id){return id==null?null:id.toString();}
    @PostMapping("/courses") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Object> createCourse(@RequestParam UUID cycleId,@RequestBody JsonNode w){return ApiResponse.ok(service.course(null,w,id(cycleId)));}
    @PutMapping("/courses/{courseId}")
    public ApiResponse<Object> course(@PathVariable UUID courseId,@RequestParam UUID cycleId,@RequestBody JsonNode w){return ApiResponse.ok(service.course(id(courseId),w,id(cycleId)));}
    @PostMapping("/exams/cycles") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Object> createCycle(@RequestBody JsonNode w){return ApiResponse.ok(service.cycle(null,w));}
    @PutMapping("/exams/cycles/{cycleId}")
    public ApiResponse<Object> cycle(@PathVariable UUID cycleId,@RequestBody JsonNode w){return ApiResponse.ok(service.cycle(id(cycleId),w));}
    @GetMapping("/files")
    public ApiResponse<Object> files(@RequestParam(required=false) String purpose,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok(service.files(purpose,page,size));}
    @PostMapping(value="/files",consumes="multipart/form-data") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Object> upload(@RequestPart MultipartFile file,@RequestParam String purpose,@RequestParam boolean containsAnswers){return ApiResponse.ok(service.upload(file,purpose,containsAnswers));}
    @PostMapping("/exams/courses/{courseId}/papers") @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Object> createPaper(@PathVariable UUID courseId,@RequestBody JsonNode w){return ApiResponse.ok(service.paper(id(courseId),null,w));}
    @PutMapping("/exams/courses/{courseId}/papers/{paperId}")
    public ApiResponse<Object> paper(@PathVariable UUID courseId,@PathVariable UUID paperId,@RequestBody JsonNode w){return ApiResponse.ok(service.paper(id(courseId),id(paperId),w));}
    @PostMapping("/practice/passes/{passId}/invalidation")
    public ApiResponse<Object> invalidate(@PathVariable UUID passId,@RequestBody JsonNode w){return ApiResponse.ok(service.invalidate(id(passId),w));}
    @GetMapping("/practice/alerts")
    public ApiResponse<Object> alerts(@RequestParam(required=false) UUID courseId,@RequestParam(required=false) Boolean acknowledged,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok(service.alerts(id(courseId),acknowledged,page,size));}
    @PostMapping("/practice/alerts/{alertId}/acknowledgement")
    public ApiResponse<Object> acknowledge(@PathVariable UUID alertId,@RequestBody JsonNode w){return ApiResponse.ok(service.acknowledge(id(alertId),w));}
    @GetMapping("/dashboard/audit-events")
    public ApiResponse<Object> audits(@RequestParam(required=false) @Size(max=100) String action,@RequestParam(required=false) UUID targetId,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return ApiResponse.ok(service.audits(action,id(targetId),page,size));}
}
