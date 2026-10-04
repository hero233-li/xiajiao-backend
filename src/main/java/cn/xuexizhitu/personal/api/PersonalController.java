package cn.xuexizhitu.personal.api;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.learning.api.LearningDtos.Navigation;
import cn.xuexizhitu.dashboard.application.DashboardService;
import cn.xuexizhitu.fitness.application.FitnessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
@RestController @RequestMapping("/api/v1/personal") @RequiredArgsConstructor
public class PersonalController {
 private final DashboardService learning;
 private final FitnessService fitness;
 public record Study(String title,String message,Navigation target) {}
 public record Personal(LocalDate today,Study study,FitnessService.Summary fitness,FitnessService.Day fitnessToday) {}
 @GetMapping("/summary") @Transactional(readOnly=true)
 public ApiResponse<Personal> summary(){var study=learning.dashboard(null,null);var health=fitness.summary();var suggestion=study.todaySuggestion();return ApiResponse.ok(new Personal(health.today(),new Study(suggestion==null?"今日学习":suggestion.title(),study.todaySuggestionMessage(),suggestion==null?null:suggestion.target()),health,fitness.day(health.today())));}
}
