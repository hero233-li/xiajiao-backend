package cn.xuexizhitu.dto;
import jakarta.validation.constraints.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;

public final class AssessmentDtos {
    private AssessmentDtos() {}
    public enum Kind { CHAPTER, MOCK }
    public enum Status { IN_PROGRESS, SUBMITTED, TIMED_OUT }
    public record Apply(@NotNull Kind kind,UUID chapterId) {}
    public record AnswerWrite(@NotNull @PositiveOrZero Integer selectedOption,Instant expectedSavedAt) {}
    public record Submit(@NotNull @Pattern(regexp="[0-9a-f]{64}") String answerFingerprint,@NotNull @AssertTrue Boolean confirm) {}
    public record Question(String id,String revisionId,String releaseId,String courseId,String chapterId,String mode,List<String> pointIds,String stem,List<String> options,int difficulty,String sourceLabel) {}
    public record SessionQuestion(int position,Question question,Integer selectedOption,Instant answerSavedAt) {}
    public record Session(String id,String courseId,String chapterId,Kind kind,String releaseId,String policyId,Status status,Instant startedAt,Instant deadlineAt,Instant serverTime,boolean deadlineReached,int questionCount,int limitMinutes,BigDecimal passScore,String answerFingerprint,List<SessionQuestion> questions) {}
    public record Summary(String id,Kind kind,String chapterId,Status status,Instant startedAt,Instant submittedAt,BigDecimal score,Boolean passed,String releaseId) {}
    public record SavedAnswer(String sessionId,String revisionId,int selectedOption,Instant savedAt,String answerFingerprint,Instant serverTime) {}
    public record Pass(String id,Kind kind,String chapterId,String releaseId,String source,Instant grantedAt,Instant invalidatedAt,String invalidatedBy,String invalidationReason) {}
    public record ResultAnswer(String revisionId,Integer selectedOption,boolean correct,int correctOption,String correctAnswer,String explanation) {}
    public record Result(String sessionId,Status status,Instant submittedAt,int correctCount,int questionCount,BigDecimal score,BigDecimal passScore,boolean passed,Pass pass,List<ResultAnswer> answers,PracticeDtos.Stats stats,Unlock unlock) {}
    public record Override(String id,String courseId,String cycleId,Instant confirmedAt,LocalDate examDateSnapshot,Instant revokedAt,long revision) {}
    public record SkipWindow(LocalDate examDate,LocalDate windowStart,LocalDate windowEnd,boolean canConfirm,String reason) {}
    public record Unlock(String courseId,String cycleId,boolean canDownloadPapers,boolean canWriteScores,boolean canApplyMock,List<String> missingChapterIds,List<String> sources,Override activeOverride,SkipWindow skipWindow,Instant evaluatedAt) {}
}
