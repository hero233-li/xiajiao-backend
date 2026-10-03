package cn.xuexizhitu.content.api;
import cn.xuexizhitu.learning.api.LearningDtos.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
public final class ContentDtos {
    private ContentDtos() {
    }
    public record Formula(String label,String tex,String condition) {
    }
    public record KnowledgeExample(String id,String question,int stars) {
    }
    public record UserNote(int mastery,String note,long revision) {
    }
    public record KnowledgeModule(String id,String title,String content,int difficulty,List<Formula> formulas,                                   List<KnowledgeExample> examples,List<Resource> resources,UserNote userNote) {
    }
    public record KnowledgeNoteWrite(@NotNull @Min(0) @Max(4) Integer mastery,@NotNull @Size(max=10000) String note,@NotNull @PositiveOrZero Long expectedRevision) {
    }
    public record ExampleSolution(String exampleId,String answer,String solution) {
    }
    public record ManualExercise(CatalogItem item,String exampleId) {
    }
    public record ManualSection(String chapterId,String title,String markdown,List<ManualExercise> exercises) {
    }
    public record Manual(String courseId,String releaseId,List<ManualSection> sections) {
    }
    public record ManualManifest(String courseId,String releaseId,List<ManifestSection> sections) {
    }
    public record ManifestSection(String chapterId,String title,String markdown,List<ManifestExercise> exercises) {
    }
    public record ManifestExercise(String itemId,String exampleId) {
    }
    public record FileMetadata(String id,String name,String mimeType,long sizeBytes,String sha256,boolean containsAnswers,String state,String purpose) {
    }
    public record FileDownload(FileMetadata file,String contentBase64,String encoding) {
    }
    public record Note(String courseId,LocalDate noteDate,String content,String id,Instant createdAt,Instant updatedAt,long revision,List<String> tags) {
    }
    public record NoteWrite(@NotNull UUID courseId,@NotNull LocalDate noteDate,@NotBlank @Size(max=5000) String content) {
    }
    public record NoteUpdate(@NotNull LocalDate noteDate,@NotBlank @Size(max=5000) String content,@NotNull @PositiveOrZero Long expectedRevision) {
    }
    public record TagCount(String name,int count) {
    }
    public record NoteTags(List<TagCount> tags) {
    }
}
