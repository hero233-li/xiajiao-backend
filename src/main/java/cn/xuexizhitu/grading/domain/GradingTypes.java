package cn.xuexizhitu.grading.domain;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
public final class GradingTypes {
    private GradingTypes() {
    }
    public record SubmissionWrite(@NotNull UUID courseId,@NotNull UUID cycleId,@NotNull UUID paperId) {
    }
    public record PageOrder(@NotNull @PositiveOrZero Long expectedRevision,@NotNull @Size(min=1,max=20) List<@NotNull UUID> fileIds) {
    }
    public record Page(String fileId,int pageNo,String name,String mimeType,long sizeBytes,String sha256) {
    }
    public record Submission(String id,String courseId,String cycleId,String paperId,long revision,List<Page> pages) {
    }
    public record Point(@NotBlank @Size(max=50) String id,@NotBlank @Size(max=5000) String description,@NotNull @DecimalMin("0.01") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal maximum) {
    }
    public record Question(@NotBlank @Size(max=50) String number,@NotBlank @Size(max=10000) String stem,@NotBlank @Size(max=10000) String referenceAnswer,@NotNull @DecimalMin("0.01") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal maximum,@NotNull @Size(min=1,max=100) List<@Valid @NotNull Point> points) {
    }
    public record RubricDocument(@NotNull @Size(min=1,max=200) List<@Valid @NotNull Question> questions) {
    }
    public record Rubric(String id,String paperId,int version,String state,RubricDocument document) {
    }
    public record Apply(@NotNull LocalDate practicedOn,@NotNull @Min(1) @Max(1440) Integer minutes,@NotNull @Min(1) @Max(1440) Integer limitMinutes,@NotNull Boolean complete,@NotNull Boolean closedBook,@NotNull Boolean answersSeenBefore,@NotNull @PositiveOrZero Long expectedRevision) {
    }
    public record Inputs(String courseId,String cycleId,Apply practice,Rubric rubric,List<Page> pages) {
    }
    public record Earned(@NotBlank @Size(max=50) String pointId,@NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal score,@NotNull @Size(max=5000) String reason) {
    }
    public record Answer(@NotBlank @Size(max=50) String number,@NotNull @Size(max=10000) String recognizedAnswer,@NotNull @Size(max=100) List<@Valid @NotNull Earned> points,@NotNull @Size(max=20) List<@Min(1) @Max(20) Integer> pages,@NotNull @Size(max=100) List<@NotBlank @Size(max=1000) String> reviewItems) {
    }
    public record Result(@NotNull UUID rubricId,@NotNull @Size(min=1,max=200) List<@Valid @NotNull Answer> answers,@NotNull @Size(max=100) List<@NotBlank @Size(max=1000) String> reviewItems) {
    }
    public record Callback(@NotNull UUID leaseToken,@NotBlank @Size(max=100) String model,@Valid Result result,@Valid RubricDocument rubric) {
    }
    public record Lease(@NotNull UUID leaseToken) {
    }
    public record Failure(@NotNull UUID leaseToken,@NotBlank @Size(max=500) String reason,@NotNull Boolean retryable,@NotNull Boolean pause) {
    }
    public record Heartbeat(@NotNull Boolean paused,@NotNull @Size(max=500) String reason) {
    }
    public record PairWrite(@NotBlank @Size(max=100) String label) {
    }
    public record Pair(String id,String token) {
    }
    public record Worker(String id,String label,boolean online,boolean paused,String reason,boolean revoked) {
    }
    public record WorkerPrincipal(String id,String userId) {
    }
    public record Task(String id,String kind,String state,String submissionId,String rubricId,String model,Result result,RubricDocument rubricDraft,String scoreId,String error,Instant leaseUntil,boolean workerOnline,String workerReason) {
    }
    public record Claim(String taskId,String kind,String leaseToken,Instant leaseUntil,Instant deadline,Inputs inputs) {
    }
    public record Material(String fileId,int pageNo,String mimeType,String sha256,String contentBase64) {
    }
    public record Reviewed(@NotNull @PositiveOrZero Long expectedRevision,@Valid @NotNull Result result) {
    }
}
