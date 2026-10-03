package cn.xuexizhitu.content.api;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.content.api.ContentDtos.*;
import cn.xuexizhitu.learning.api.LearningDtos.Page;
import cn.xuexizhitu.content.application.ContentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.UUID;
@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor public class ContentController {
    private final ContentService service;
    private static String id(UUID id) {
        return id==null?null:id.toString();
    }
    @GetMapping("/catalog/courses/{courseId}/knowledge")     public ApiResponse<Page<KnowledgeModule>> knowledge(@PathVariable UUID courseId,@RequestParam(required=false) @Size(max=200) String q,             @RequestParam(required=false) @Min(1) @Max(5) Integer difficulty,@RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(service.knowledge(id(courseId),q,difficulty,page,size));
    }
    @GetMapping("/catalog/courses/{courseId}/knowledge/{moduleId}")     public ApiResponse<KnowledgeModule> module(@PathVariable UUID courseId,@PathVariable UUID moduleId) {
        return ApiResponse.ok(service.module(id(courseId),id(moduleId)));
    }
    @PutMapping("/catalog/courses/{courseId}/knowledge/{moduleId}/note")     public ApiResponse<KnowledgeModule> knowledgeNote(@PathVariable UUID courseId,@PathVariable UUID moduleId,@Valid @RequestBody KnowledgeNoteWrite w) {
        return ApiResponse.ok(service.saveKnowledgeNote(id(courseId),id(moduleId),w));
    }
    @GetMapping("/catalog/courses/{courseId}/examples/{exampleId}/solution")     public ApiResponse<ExampleSolution> solution(@PathVariable UUID courseId,@PathVariable UUID exampleId) {
        return ApiResponse.ok(service.solution(id(courseId),id(exampleId)));
    }
    @GetMapping("/catalog/courses/{courseId}/manual")     public ApiResponse<Manual> manual(@PathVariable UUID courseId) {
        return ApiResponse.ok(service.manual(id(courseId)));
    }
    @GetMapping("/catalog/courses/{courseId}/resources/{fileId}")     public ApiResponse<FileDownload> resource(@PathVariable UUID courseId,@PathVariable UUID fileId) {
        return ApiResponse.ok(service.resource(id(courseId),id(fileId)));
    }
    @GetMapping("/notes")     public ApiResponse<Page<Note>> notes(@RequestParam(required=false) UUID courseId,@RequestParam(required=false) @Size(max=200) String q,             @RequestParam(required=false) @Size(max=100) String tag,             @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,             @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,             @RequestParam(defaultValue="1") @Min(1) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok(service.notes(id(courseId),q,tag,from,to,page,size));
    }
    @PostMapping("/notes") @ResponseStatus(HttpStatus.CREATED)     public ApiResponse<Note> createNote(@Valid @RequestBody NoteWrite w) {
        return ApiResponse.ok(service.createNote(w));
    }
    @GetMapping("/notes/{noteId}")     public ApiResponse<Note> note(@PathVariable UUID noteId) {
        return ApiResponse.ok(service.note(id(noteId)));
    }
    @PutMapping("/notes/{noteId}")     public ApiResponse<Note> updateNote(@PathVariable UUID noteId,@Valid @RequestBody NoteUpdate w) {
        return ApiResponse.ok(service.updateNote(id(noteId),w));
    }
    @GetMapping("/notes/tags")     public ApiResponse<NoteTags> tags(@RequestParam(required=false) UUID courseId) {
        return ApiResponse.ok(service.tags(id(courseId)));
    }
}
