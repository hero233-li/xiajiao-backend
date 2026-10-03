package cn.xuexizhitu.planning.domain;
import cn.xuexizhitu.contract.WorkflowDtos.PlanConfig;
import cn.xuexizhitu.learning.api.LearningDtos.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
public final class PlanTypes {
    private PlanTypes() {
    }
    public record Task(String id,String courseId,String itemId,String templateId,String kind,String title,             int estimatedMinutes,String releaseId,boolean completed,Instant completedAt,Navigation target) {
        public Task reuse(Task old) {
            return new Task(old.id,courseId,itemId,templateId,kind,title,estimatedMinutes,releaseId,old.completed,old.completedAt,target);
        }
        public String key() {
            return courseId+":"+kind+":"+(itemId==null?templateId:itemId)+":"+releaseId;
        }
    }
    public record Segment(String id,String taskId,LocalDate scheduledOn,int minutes,String state,int sortOrder) {
        public Segment copy() {
            return new Segment(UUID.randomUUID().toString(),taskId,scheduledOn,minutes,state,sortOrder);
        }
    }
    public record Day(LocalDate day,int capacityMinutes,int reservedMinutes,int remainingMinutes,List<Segment> segments,int completedMinutes,int percent) {
    }
    public record Week(int index,LocalDate startDate,LocalDate endDate,int scheduledMinutes,int completedMinutes,int percent) {
    }
    public record PlanProgress(int completedMinutes,int totalEstimatedMinutes,int percent) {
    }
    public record Plan(String id,int revision,PlanConfig config,List<Task> tasks,List<Day> days,List<Segment> unscheduled,             List<Segment> awaitingDate,PlanProgress progress,Instant createdAt,Instant confirmedAt,Instant asOf,             List<Week> weeks,int completedDayCount,int dayCount,int overdueUncompletedMinutes,List<Course> courseSummaries) {
    }
    public record Summary(String id,String cycleId,LocalDate startDate,LocalDate endDate,int revision,int completedPercent) {
    }
    public record Move(String segmentId,LocalDate fromDate,List<Segment> toSegments) {
    }
    public record Preview(String id,String planId,int baseRevision,LocalDate asOf,String inputFingerprint,Plan proposedPlan,             List<Move> moves,int gapMinutes,BigDecimal gapHours,List<String> options,Instant createdAt) {
    }
    public record Completion(Task task,int planRevision,Progress courseProgress,Progress overallProgress,             List<String> affectedPlanIds,UUID clientMutationId,Instant asOf) {
    }
    public record RevisionSnapshot(PlanConfig config,List<String> taskIds) {
    }
    public record PreviewSnapshot(Preview response,PlanConfig config,List<Segment> segments,PlanConfig oldConfig,             Boolean configuration,List<Task> newTasks,List<String> taskIds,String guard) {
    }
    public record Row(String id,String userId,String cycleId,int revision,Instant createdAt) {
    }
    public record RevisionRow(int revision,RevisionSnapshot snapshot,Instant confirmedAt) {
    }
    public record PreviewRow(String id,String planId,int baseRevision,LocalDate asOf,String inputFingerprint,int gapMinutes,             Instant confirmedAt,PreviewSnapshot snapshot) {
    }
}
