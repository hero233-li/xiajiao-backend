package cn.xuexizhitu.exams.domain;
import cn.xuexizhitu.files.domain.FileTypes.Metadata;
import cn.xuexizhitu.assessment.api.AssessmentDtos.Unlock;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
public final class ExamTypes {
    private ExamTypes() {
    }
    public record Confirm(@NotNull @AssertTrue Boolean confirm) {
    }
    public record Revoke(@NotNull @PositiveOrZero Long expectedRevision, @NotNull @AssertTrue Boolean confirm) {
    }
    public record CreateScore(@NotNull UUID cycleId, @NotNull UUID paperId,             @NotNull LocalDate practicedOn, @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal score,             @NotNull @Min(1) @Max(1440) Integer minutes, @NotNull @Min(1) @Max(1440) Integer limitMinutes,             @NotNull Boolean complete, @NotNull Boolean closedBook, @NotNull Boolean answersSeenBefore,             @NotNull @Size(max=5000) String note) {
        public ScoreFields fields() {
            return new ScoreFields(practicedOn,score,minutes,limitMinutes,complete,closedBook,answersSeenBefore,note);
        }
    }
    public record UpdateScore(@NotNull LocalDate practicedOn,             @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal score,             @NotNull @Min(1) @Max(1440) Integer minutes, @NotNull @Min(1) @Max(1440) Integer limitMinutes,             @NotNull Boolean complete, @NotNull Boolean closedBook, @NotNull Boolean answersSeenBefore,             @NotNull @Size(max=5000) String note, @NotNull @PositiveOrZero Long expectedRevision) {
        public ScoreFields fields() {
            return new ScoreFields(practicedOn,score,minutes,limitMinutes,complete,closedBook,answersSeenBefore,note);
        }
    }
    public record ScoreFields(LocalDate practicedOn, BigDecimal score, int minutes, int limitMinutes,                               boolean complete, boolean closedBook, Boolean answersSeenBefore, String note) {
    }
    public record ScoreData(String id,String userId,String cycleId,String courseId,String paperId,String paperKey,             LocalDate practicedOn,BigDecimal score,int minutes,int limitMinutes,boolean complete,boolean closedBook,             Boolean answersSeenBefore,String source,String note,Instant createdAt,Instant updatedAt,long revision) {
    }
    public record ScoreRecord(String id,String cycleId,String courseId,String paperId,String paperKey,             LocalDate practicedOn,BigDecimal score,int minutes,int limitMinutes,boolean complete,boolean closedBook,             Boolean answersSeenBefore,String source,String note,Instant createdAt,Instant updatedAt,long revision,             List<Metadata> images,boolean eligibleConditionsMet,boolean selectedAsFirstValid,             boolean includedInPrediction,String firstValidRecordId,List<String> predictionExclusionReasons) {
    }
    public record Paper(String id,String courseId,String paperKey,String paperMonth,String sourceCourseCode,             Metadata questionFile,Metadata answerFile,Integer questionPages,Integer answerPages,String note) {
    }
    public record PaperData(String id,String courseId,String code,LocalDate paperMonth,String sourceCourseCode,             String questionFileId,String answerFileId,Integer questionPages,Integer answerPages,String note) {
    }
    public record PredictionSample(String paperId,String paperKey,String recordId,LocalDate practicedOn,             Instant createdAt,BigDecimal score,int weight) {
    }
    public record PredictionExclusion(String paperId,String paperKey,String firstValidRecordId,String reason,String detail) {
    }
    public record Prediction(String courseId,LocalDate asOf,String status,Integer predictedScore,int sampleCount,             int minimumSamples,int maximumSamples,int windowDays,List<PredictionSample> samples,             List<PredictionExclusion> exclusions,BigDecimal actualMinimum,BigDecimal actualMaximum,             int scoresAtLeast60,String ruleVersion) {
    }
    public record Mutation(ScoreRecord record,Prediction prediction,Unlock unlock) {
    }
    public record Trend(String courseId,List<ScoreRecord> records) {
    }
    public record Revision(ScoreData record,String captureKind,String actorId,Instant capturedAt) {
    }
}
