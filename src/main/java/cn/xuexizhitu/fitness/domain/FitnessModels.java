package cn.xuexizhitu.fitness.domain;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
public final class FitnessModels {
 public enum GoalType { LOSE, GAIN, MAINTAIN }
 public enum ExerciseType { STRENGTH, CARDIO, MOBILITY, OTHER }
 public enum TrainingStatus { COMPLETED, PARTIAL, SKIPPED, REST }
 public enum MealType { BREAKFAST, LUNCH, DINNER, SNACK }
 public record Goal(@NotNull GoalType type, @NotNull LocalDate startDate,
   @NotNull @DecimalMin("0.001") @Digits(integer=6,fraction=3) BigDecimal startWeight,
   @NotNull @DecimalMin("0.001") @Digits(integer=6,fraction=3) BigDecimal targetWeight, LocalDate targetDate, @Size(max=2000) String note) {}
 public record Weight(@NotNull @DecimalMin("0.001") @Digits(integer=6,fraction=3) BigDecimal kg, @Size(max=2000) String note) {}
 public record Exercise(@NotBlank String id, @NotBlank @Size(max=160) String name, @NotNull ExerciseType type,
   @Min(1) @Max(100) Integer sets, @Min(1) @Max(10000) Integer reps,
   @DecimalMin("0") @DecimalMax("2000") @Digits(integer=8,fraction=3) BigDecimal kg,
   @DecimalMin("0") @DecimalMax("1440") @Digits(integer=8,fraction=3) BigDecimal minutes,
   @DecimalMin("0") @DecimalMax("1000") @Digits(integer=8,fraction=3) BigDecimal km, @Size(max=2000) String note, Boolean completed) {}
 public record TrainingPlan(@NotNull Boolean rest, @NotNull @Size(max=100) List<@NotNull @Valid Exercise> exercises, @Size(max=2000) String note) {}
 public record Training(@NotNull TrainingStatus status, @NotNull @Size(max=100) List<@NotNull @Valid Exercise> exercises, @Size(max=2000) String note, @Valid TrainingPlan planSnapshot) {}
 public record Food(@NotNull MealType meal, @NotBlank @Size(max=160) String name,
   @DecimalMin("0.001") @DecimalMax("100000") @Digits(integer=8,fraction=3) BigDecimal quantity, @Size(max=32) String unit,
   @DecimalMin("0") @DecimalMax("100000") @Digits(integer=8,fraction=3) BigDecimal kcal, @DecimalMin("0") @DecimalMax("10000") @Digits(integer=8,fraction=3) BigDecimal protein,
   @DecimalMin("0") @DecimalMax("10000") @Digits(integer=8,fraction=3) BigDecimal carbs, @DecimalMin("0") @DecimalMax("10000") @Digits(integer=8,fraction=3) BigDecimal fat, @Size(max=2000) String note) {}
 public record Meals(@NotNull @Size(max=100) List<@NotNull @Valid Food> foods, @Size(max=2000) String note) {}
 public record Checkin(@DecimalMin("0") @DecimalMax("24") @Digits(integer=8,fraction=3) BigDecimal sleepHours, @Min(1) @Max(5) Integer feeling, @Size(max=2000) String note) {}
 public record Water(@NotNull @Min(0) @Max(20000) Integer ml) {}
 public record TrainingTemplate(@NotBlank @Size(max=160) String name, @NotNull @Valid TrainingPlan plan) {}
 public record WeekTemplate(@NotBlank @Size(max=160) String name, @NotNull @Size(min=7,max=7) List<@NotNull @Valid TrainingPlan> days) {}
 public record MealTemplate(@NotBlank @Size(max=160) String name, @NotNull @Valid Meals plan) {}
 public record Profile(@NotBlank @Size(max=80) String displayName, @Size(max=2000) String bio, @NotNull Boolean compact, @NotNull Boolean weekStartsMonday) {}
}
