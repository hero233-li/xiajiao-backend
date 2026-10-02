package cn.xuexizhitu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;

public final class LearningDtos {
    private LearningDtos() {}
    public enum CourseType { THEORY, PRACTICE }
    public enum Pane { CATALOG, KNOWLEDGE, PRACTICE, EXAMS, NOTES, MANUAL }
    public record Page<T>(List<T> items, int page, int size, long total) {}
    public record Progress(int completedItems, int totalItems, int percent) {}
    public record Capabilities(boolean catalog, boolean knowledge, boolean practice, boolean exams, boolean manual) {}
    public record Course(String id, String code, String name, CourseType courseType, boolean active,
                         String releaseId, Progress progress, Enrollment enrollment, Capabilities capabilities) {}
    public record CycleCourse(String courseId, LocalDate examDate, LocalTime startsAt, LocalTime endsAt) {}
    public record ExamCycle(String id, String name, LocalDate startDate, LocalDate endDate, String timezone, List<CycleCourse> courses) {}
    public record Enrollment(String cycleId, String courseId, boolean paid, BigDecimal fee, BigDecimal officialScore,
                             Boolean officialPassed, String passedMonth, String note, long revision) {}
    public record EnrollmentWrite(@NotNull Boolean paid,
                                  @DecimalMin("0") @DecimalMax("99999999.99") @Digits(integer=8,fraction=2) @JsonProperty(value="fee",required=true) BigDecimal fee,
                                  @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) @JsonProperty(value="officialScore",required=true) BigDecimal officialScore,
                                  @JsonProperty(value="officialPassed",required=true) Boolean officialPassed,
                                  @Pattern(regexp="^[0-9]{4}-(0[1-9]|1[0-2])$") @JsonProperty(value="passedMonth",required=true) String passedMonth,
                                  @NotNull @Size(max=10000) String note, @NotNull @PositiveOrZero Long expectedRevision) {}
    public record Resource(String kind, String label, String url, String fileId) {}
    public record CatalogItem(String id, String title, int estimatedMinutes, Resource resource,
                              boolean completed, Instant completedAt, long revision) {}
    public record CatalogChapter(String id, String title, int sortOrder, boolean participatesInAssessment, List<CatalogItem> items) {}
    public record Catalog(String courseId, String releaseId, List<CatalogChapter> chapters,
                          Progress courseProgress, Progress overallProgress, Instant asOf) {}
    public record CompletionWrite(@NotNull Boolean completed, @NotNull @PositiveOrZero Long expectedRevision,
                                  @NotNull UUID clientMutationId) {}
    public record ItemUpdate(@NotNull UUID itemId, @NotNull Boolean completed, @NotNull @PositiveOrZero Long expectedRevision) {}
    public record BatchCompletionWrite(@NotNull @Size(min=1,max=200) List<@NotNull @Valid ItemUpdate> updates, @NotNull UUID clientMutationId) {}
    public record CatalogCompletion(CatalogItem item, Progress courseProgress, Progress overallProgress,
                                    List<String> affectedPlanIds, UUID clientMutationId, Instant asOf) {}
    public record BatchCompletion(List<CatalogItem> items, Progress courseProgress, Progress overallProgress,
                                  List<String> affectedPlanIds, UUID clientMutationId, Instant asOf) {}
    public record Navigation(@NotNull Pane pane, @NotNull @Pattern(regexp="^[0-9]{5}$") String courseCode,
                             @JsonProperty(value="chapterId",required=true) UUID chapterId,
                             @JsonProperty(value="itemId",required=true) UUID itemId,
                             @JsonProperty(value="questionId",required=true) UUID questionId) {}
    public record LearningPositionWrite(@NotNull @Valid Navigation target) {}
    public record LearningPosition(String courseId, Navigation target, String title, Instant updatedAt) {}
}
