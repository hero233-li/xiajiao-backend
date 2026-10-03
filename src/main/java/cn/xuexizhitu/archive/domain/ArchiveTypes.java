package cn.xuexizhitu.archive.domain;
import com.fasterxml.jackson.databind.JsonNode;
import cn.xuexizhitu.files.domain.FileTypes.Metadata;
import cn.xuexizhitu.practice.api.PracticeDtos.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
public final class ArchiveTypes {
    private ArchiveTypes() {
    }
    public record StoredRecord(String id,String oldId,String kind,String courseId,JsonNode payload,int fileCount) {
    }
    public record Summary(String id,String kind,String courseId,String title,Instant createdAt,boolean readOnly,int fileCount) {
    }
    public record LegacyError(String question,String reason,BigDecimal deduction,String solution) {
    }
    public record Review(String id,String legacyRecordId,String courseId,String chapterId,String kind,BigDecimal oldScore,BigDecimal oldThreshold,String oldVersion,String mappingReleaseId,String decision,String reason,Instant decidedAt,String passId) {
    }
    public record Detail(Summary summary,String oldId,String oldStatus,BigDecimal oldScore,String summaryText,List<LegacyError> errors,List<Metadata> files,Review passReview,String archivedContent) {
    }
    public record Mapping(String courseId,String chapterId,String releaseId) {
    }
    public record CreditResult(LegacySummary summary,Stats stats,Instant auditedAt) {
    }
}
