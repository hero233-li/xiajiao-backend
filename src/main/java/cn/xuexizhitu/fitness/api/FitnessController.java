package cn.xuexizhitu.fitness.api;
import cn.xuexizhitu.common.ApiResponse;
import cn.xuexizhitu.fitness.application.FitnessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
@RestController @RequestMapping("/api/v1/fitness") @RequiredArgsConstructor
@io.swagger.v3.oas.annotations.tags.Tag(name="fitness",description="个人健身数据，按当前登录用户隔离，写入需UUID幂等键")
public class FitnessController {
 private final FitnessService service;
 @GetMapping("/summary") public ApiResponse<FitnessService.Summary> summary(){return ApiResponse.ok(service.summary());}
 @GetMapping("/days/{date}") public ApiResponse<FitnessService.Day> day(@PathVariable LocalDate date){return ApiResponse.ok(service.day(date));}
 @GetMapping("/history") public ApiResponse<List<FitnessService.Day>> history(@RequestParam LocalDate from,@RequestParam LocalDate to){return ApiResponse.ok(service.history(from,to));}
 @GetMapping("/statistics") public ApiResponse<FitnessService.Stats> statistics(@RequestParam LocalDate from,@RequestParam LocalDate to){return ApiResponse.ok(service.statistics(from,to));}
 @GetMapping("/goals/history") public ApiResponse<FitnessService.Page<FitnessService.GoalEvent>> goalHistory(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="30") int size){return ApiResponse.ok(service.goalHistory(page,size));}
 @PostMapping("/goals/end") public ApiResponse<java.util.Map<String,Object>> end(@RequestBody FitnessService.EndGoal body,@RequestHeader("Idempotency-Key") String key){return ApiResponse.ok(service.endGoal(body,key));}
 @GetMapping("/records/{kind}") public ApiResponse<FitnessService.Page<FitnessService.Entry>> list(@PathVariable String kind,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="30") int size){return ApiResponse.ok(service.list(kind,page,size));}
 @GetMapping("/records/{kind}/{key}") public ApiResponse<FitnessService.Entry> get(@PathVariable String kind,@PathVariable String key){return ApiResponse.ok(service.get(kind,key));}
 @PutMapping("/records/{kind}/{key}") public ApiResponse<FitnessService.Entry> write(@PathVariable String kind,@PathVariable String key,@Valid @RequestBody FitnessService.Write body,@RequestHeader("Idempotency-Key") String requestKey){return ApiResponse.ok(kind.equals("goal")?service.saveGoal(key,body,requestKey):service.write(kind,key,body,requestKey));}
 @DeleteMapping("/records/{kind}/{key}") public ApiResponse<Void> delete(@PathVariable String kind,@PathVariable String key,@RequestParam long revision){service.delete(kind,key,revision);return ApiResponse.ok(null);}
 @PostMapping("/copy") public ApiResponse<FitnessService.Entry> copy(@RequestBody FitnessService.Copy body,@RequestHeader("Idempotency-Key") String key){return ApiResponse.ok(service.copy(body,key));}
 @PostMapping("/weeks/generate") public ApiResponse<FitnessService.Batch> generate(@RequestBody FitnessService.GenerateWeek body,@RequestHeader("Idempotency-Key") String key){return ApiResponse.ok(service.generateWeek(body,key));}
}
