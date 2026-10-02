package cn.xuexizhitu.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class PracticeDtos {
    private PracticeDtos() {}
    public enum Mode { CHAPTER, VARIANT }
    public enum Filter { ALL, UNANSWERED, WRONG, BOOKMARKED, UNCERTAIN }
    public enum Outcome { CORRECT, WRONG }
    public record PublicMark(boolean bookmarked,boolean uncertain,long revision) {}
    public record QuestionPublic(String id,String revisionId,String releaseId,String courseId,String chapterId,Mode mode,
                                 List<String> pointIds,String stem,List<String> options,int difficulty,String sourceLabel,PublicMark mark,Outcome latestOutcome) {}
    public record AnswerWrite(@NotNull UUID revisionId,@NotNull @PositiveOrZero Integer selectedOption) {}
    public record MarkWrite(@NotNull Boolean bookmarked,@NotNull Boolean uncertain,@NotNull @PositiveOrZero Long expectedRevision) {}
    public record QuestionMark(String questionId,boolean bookmarked,boolean uncertain,long revision,Instant updatedAt) {}
    public record Stats(String courseId,String chapterId,int availableOriginalCount,int answeredOriginalCount,int practiceAttemptCount,
                        int practiceCorrectCount,double practiceAccuracy,int latestWrongCount,Integer gateThreshold,Boolean canApplyChapterAssessment,List<String> blockReasons) {}
    public record PracticeChapter(String chapterId,String title,Stats stats,boolean passed,List<String> passReleaseIds) {}
    public record Overview(String courseId,String releaseId,List<PracticeChapter> chapters,Stats stats,int variantQuestionCount) {}
    public record HistoryRow(String id,String questionId,String revisionId,int selectedOption,boolean correct,Instant submittedAt) {}
    public record Result(String submissionId,String questionId,String revisionId,int selectedOption,boolean correct,int correctOption,String correctAnswer,String explanation,Instant submittedAt,Stats stats) {}
    public record LegacySummary(String id,String questionId,String oldQuestionId,Integer attempts,Integer selectedOption,Boolean correct,
                                Instant lastUpdatedAt,boolean gateCreditApproved,String courseId,String chapterId) {}
}
