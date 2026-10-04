package cn.xuexizhitu.fitness.application;

import cn.xuexizhitu.common.*;
import cn.xuexizhitu.security.CurrentUser;
import cn.xuexizhitu.identity.infrastructure.UserRepository;
import com.fasterxml.jackson.databind.*;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import static cn.xuexizhitu.fitness.domain.FitnessModels.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class FitnessService {
 private final JdbcTemplate jdbc;
 private final ObjectMapper mapper;
 private final Validator validator;
 private final UserRepository users;
 private final RequestReplay replay;
 private final Clock clock;
 public record Entry(String kind,String key,long revision,JsonNode data) {}
 public record Write(@jakarta.validation.constraints.Min(-1) @com.fasterxml.jackson.annotation.JsonProperty(required=true) long expectedRevision, Long expectedGoalRevision, @jakarta.validation.constraints.NotNull JsonNode data) {}
 public record Page<T>(List<T> items,long total,int page,int size) {}
 public record Nutrient(BigDecimal knownTotal, boolean complete, int knownCount, int foodCount) {}
 public record Nutrition(Nutrient kcal,Nutrient protein,Nutrient carbs,Nutrient fat) {}
 public record Day(LocalDate date,Map<String,Entry> records,boolean checkedIn,boolean rest,boolean partial,String state,String trainingState,Nutrition nutrition,Nutrition plannedNutrition) {}
 public record WeightWindow(BigDecimal mean,int samples,LocalDate from,LocalDate to) {}
 public record Stats(LocalDate from,LocalDate to,int checkinDays,int dietDays,int plannedTrainingDays,int completedTrainingDays,int partialTrainingDays,int skippedTrainingDays,int restDays,BigDecimal trainingRate,WeightWindow sevenDayWeight,Nutrition nutrition) {}
 public record Summary(LocalDate today,Entry currentGoal,long goalRevision,Entry latestWeight,int streak,int weekCheckins,int weekElapsedDays,BigDecimal weekRate,WeightWindow sevenDayWeight) {}
 public record EndGoal(@com.fasterxml.jackson.annotation.JsonProperty(required=true) long expectedGoalRevision) {}
 public record GoalEvent(long version,String goalKey,String action,String createdAt) {}
 public record Copy(String sourceKind,String sourceKey,LocalDate destination,@com.fasterxml.jackson.annotation.JsonProperty(required=true) long expectedRevision) {}
 public record GenerateWeek(String templateKey,LocalDate monday,List<Long> expectedRevisions) {}
 public record Batch(List<Entry> items) {}
 public record ImportDay(@jakarta.validation.constraints.NotNull @jakarta.validation.Valid TrainingPlan training,
  @jakarta.validation.constraints.NotNull @jakarta.validation.Valid Meals meals,
  @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(-1) Long expectedTrainingRevision,
  @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(-1) Long expectedMealRevision) {}
 public record ImportWeek(@jakarta.validation.constraints.NotNull LocalDate startDate,
  @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(min=7,max=7) List<@jakarta.validation.constraints.NotNull @jakarta.validation.Valid ImportDay> days) {}
 @Transactional public Batch importWeek(ImportWeek body,String requestKey){
  idempotency(requestKey);lock();String user=CurrentUser.idOrThrow(),hash=replay.hash(body);
  Batch old=replay.find(user,"fitness-import-week",requestKey,hash,Batch.class);if(old!=null)return old;
  var errors=validator.validate(body);require(errors.isEmpty(),"请提供开始日期、7天训练与食谱，以及各计划版本");
  List<Entry> rows=new ArrayList<>();
  for(int i=0;i<7;i++){
   ImportDay d=body.days().get(i);String date=body.startDate().plusDays(i).toString();
   rows.add(writeLocked("training-plan",date,new Write(d.expectedTrainingRevision(),null,mapper.valueToTree(d.training()))));
   rows.add(writeLocked("meal-plan",date,new Write(d.expectedMealRevision(),null,mapper.valueToTree(d.meals()))));
  }
  Batch result=new Batch(rows);replay.save(user,"fitness-import-week",requestKey,hash,result);return result;
 }
 private static final Map<String,Class<?>> TYPES=Map.ofEntries(
  Map.entry("goal",Goal.class),Map.entry("weight",Weight.class),Map.entry("training-plan",TrainingPlan.class),Map.entry("training",Training.class),
  Map.entry("meal-plan",Meals.class),Map.entry("meals",Meals.class),Map.entry("checkin",Checkin.class),Map.entry("water",Water.class),
  Map.entry("training-template",TrainingTemplate.class),Map.entry("week-template",WeekTemplate.class),Map.entry("meal-template",MealTemplate.class),Map.entry("profile",Profile.class));
 private static final Set<String> DAILY=Set.of("weight","training-plan","training","meal-plan","meals","checkin","water");
 public LocalDate today(){return cn.xuexizhitu.fitness.domain.FitnessDates.today(clock);}
 private void key(String kind,String key,boolean writing) {
  require(kind!=null&&key!=null,"资源类型和标识不能为空");
  if(!TYPES.containsKey(kind))throw new BusinessException(ErrorCode.NOT_FOUND);
  try {
   if(DAILY.contains(kind)){LocalDate day=LocalDate.parse(key);if(!day.toString().equals(key)||day.isBefore(LocalDate.of(1900,1,1))||day.isAfter(today().plusYears(5)))throw new IllegalArgumentException();
    if(writing&&!Set.of("training-plan","meal-plan").contains(kind))cn.xuexizhitu.fitness.domain.FitnessDates.actual(day,today());
   }else if(kind.equals("profile")){if(!key.equals("current"))throw new IllegalArgumentException();}
   else if(!UUID.fromString(key).toString().equals(key))throw new IllegalArgumentException();
  }catch(IllegalArgumentException e){throw new BusinessException(ErrorCode.INVALID_PARAMETER,"记录日期或标识不合法");}
 }
 private void require(boolean condition,String message){if(!condition)throw new BusinessException(ErrorCode.INVALID_PARAMETER,message);}
 private <T> T model(JsonNode node,Class<T> type){try{return mapper.treeToValue(node,type);}catch(Exception e){throw new BusinessException(ErrorCode.INVALID_PARAMETER,"记录字段或类型不合法");}}
 private JsonNode validate(String kind,JsonNode node){
  Object value=model(node,TYPES.get(kind));require(value!=null,"记录不能为空");
  var errors=validator.validate(value);if(!errors.isEmpty())throw new BusinessException(ErrorCode.INVALID_PARAMETER,errors.iterator().next().getPropertyPath()+"：填写值不合法");
  if(value instanceof Goal g){require(g.targetDate()==null||!g.targetDate().isBefore(g.startDate()),"目标日期不能早于起始日期");int delta=g.targetWeight().compareTo(g.startWeight());require(g.type()==GoalType.LOSE?delta<0:g.type()==GoalType.GAIN?delta>0:delta==0,"目标体重应符合目标类型；维持目标与起始体重相同");}
  if(value instanceof TrainingPlan p){require(p.rest()?p.exercises().isEmpty():!p.exercises().isEmpty(),"训练安排需添加动作；休息安排应为空");exercises(p.exercises());}
  if(value instanceof Training t){require(t.status()!=TrainingStatus.REST||t.exercises().isEmpty(),"休息记录不能包含训练项目");exercises(t.exercises());}
  if(value instanceof Meals meals)for(Food f:meals.foods())require((f.quantity()==null)==(f.unit()==null||f.unit().isBlank()),"份量与单位需要同时填写或同时留空");
  if(value instanceof TrainingTemplate t)validate("training-plan",mapper.valueToTree(t.plan()));
  if(value instanceof MealTemplate t)validate("meal-plan",mapper.valueToTree(t.plan()));
  if(value instanceof WeekTemplate t)t.days().forEach(d->validate("training-plan",mapper.valueToTree(d)));
  return mapper.valueToTree(value);
 }
 private void exercises(List<Exercise> rows){Set<String> ids=new HashSet<>();for(Exercise e:rows){try{UUID.fromString(e.id());}catch(Exception ex){throw new BusinessException(ErrorCode.INVALID_PARAMETER,"动作标识不合法");}require(ids.add(e.id()),"同一安排中的动作标识不能重复");
  if(e.type()==ExerciseType.STRENGTH)require(e.minutes()==null&&e.km()==null,"力量项目请填写组数、次数与重量");
  else require(e.sets()==null&&e.reps()==null&&e.kg()==null&&(e.type()==ExerciseType.CARDIO||e.km()==null),"非力量项目请填写时长；距离用于有氧项目");
 }}
 private Entry row(java.sql.ResultSet rs)throws java.sql.SQLException {try{String value=rs.getString("payload");return new Entry(rs.getString("kind"),rs.getString("record_key"),rs.getLong("revision"),value==null?null:mapper.readTree(value));}catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException(e);}}
 public Entry get(String kind,String key){key(kind,key,false);return find(kind,key);}
 private Entry find(String kind,String key){return jdbc.query("SELECT * FROM fitness_record WHERE user_id=? AND kind=? AND record_key=?",(rs,n)->row(rs),CurrentUser.idOrThrow(),kind,key).stream().findFirst().orElse(null);}
 public Page<Entry> list(String kind,int page,int size){require(page>=1&&page<=100000&&size>=1&&size<=100,"分页范围不合法");if(!TYPES.containsKey(kind))throw new BusinessException(ErrorCode.NOT_FOUND);String user=CurrentUser.idOrThrow();long count=jdbc.queryForObject("SELECT COUNT(*) FROM fitness_record WHERE user_id=? AND kind=? AND payload IS NOT NULL",Long.class,user,kind);
  var items=jdbc.query("SELECT * FROM fitness_record WHERE user_id=? AND kind=? AND payload IS NOT NULL ORDER BY updated_at DESC,record_key DESC LIMIT ? OFFSET ?",(rs,n)->row(rs),user,kind,size,(page-1)*size);return new Page<>(items,count,page,size);
 }
 private void lock(){users.findLockedById(CurrentUser.idOrThrow()).orElseThrow(()->new BusinessException(ErrorCode.UNAUTHORIZED));}
 private void idempotency(String requestKey){try{require(requestKey!=null&&UUID.fromString(requestKey).toString().equals(requestKey),"请提供UUID格式的Idempotency-Key");}catch(IllegalArgumentException e){throw new BusinessException(ErrorCode.INVALID_PARAMETER,"幂等键格式不合法");}}
 @Transactional public Entry write(String kind,String key,Write body,String requestKey){
  idempotency(requestKey);lock();String user=CurrentUser.idOrThrow(),operation="fitness-write-"+kind;String hash=replay.hash(Map.of("key",key,"body",body));Entry previous=replay.find(user,operation,requestKey,hash,Entry.class);if(previous!=null)return previous;
  Entry result=writeLocked(kind,key,body);replay.save(user,operation,requestKey,hash,result);return result;
 }
 private Entry writeLocked(String kind,String key,Write body){
  key(kind,key,true);require(body.expectedRevision()>=-1,"版本不能小于-1");JsonNode data=validate(kind,body.data());String user=CurrentUser.idOrThrow();
  if(kind.equals("goal")){require(body.expectedGoalRevision()!=null,"请提供当前目标版本");var state=goalState();if(body.expectedGoalRevision()!=((Number)state.get("revision")).longValue())throw new BusinessException(ErrorCode.REVISION_CONFLICT);
   if(find(kind,key)!=null)throw new BusinessException(ErrorCode.CONFLICT,"目标历史不可覆盖，请新增调整后的目标");
   jdbc.update("INSERT INTO fitness_record(user_id,kind,record_key,revision,payload) VALUES(?,'goal',?,0,?)",user,key,data.toString());
   jdbc.update("UPDATE fitness_goal_state SET current_key=?,revision=revision+1 WHERE user_id=?",key,user);
   jdbc.update("INSERT INTO fitness_goal_event(user_id,version,goal_key,action) VALUES(?,?,?,?)",user,((Number)state.get("revision")).longValue()+1,key,state.get("current_key")==null?"CREATED":"ADJUSTED");return find(kind,key);
  }
  if(kind.equals("training")){
   var t=model(data,Training.class);Entry old=find(kind,key),plan=find("training-plan",key);TrainingPlan snapshot=old!=null&&old.data()!=null?model(old.data(),Training.class).planSnapshot():plan==null||plan.data()==null?null:model(plan.data(),TrainingPlan.class);
   if(snapshot!=null&&snapshot.rest())require(t.status()==TrainingStatus.REST,"休息安排请记录为休息状态");
   if(snapshot!=null&&!snapshot.rest())require(t.status()!=TrainingStatus.REST,"已有训练安排，请使用完成、部分完成或跳过状态");
   if(t.status()==TrainingStatus.COMPLETED){require(!t.exercises().isEmpty(),"完成训练需要实际动作数据");Set<String> done=new HashSet<>();t.exercises().stream().filter(e->Boolean.TRUE.equals(e.completed())).forEach(e->done.add(e.id()));require(snapshot==null?t.exercises().stream().allMatch(e->Boolean.TRUE.equals(e.completed())):snapshot.exercises().stream().allMatch(e->done.contains(e.id())),"全部计划项目完成才可标记完成；否则请使用部分完成");}
   data=mapper.valueToTree(new Training(t.status(),t.exercises(),t.note(),snapshot));
  }
  if(body.expectedRevision()==-1){if(find(kind,key)!=null)throw new BusinessException(ErrorCode.REVISION_CONFLICT,"该日期或模板已有记录，请进入修改流程");jdbc.update("INSERT INTO fitness_record(user_id,kind,record_key,revision,payload) VALUES(?,?,?,0,?)",user,kind,key,data.toString());}
  else if(jdbc.update("UPDATE fitness_record SET payload=?,revision=revision+1,updated_at=CURRENT_TIMESTAMP(6) WHERE user_id=? AND kind=? AND record_key=? AND revision=?",data.toString(),user,kind,key,body.expectedRevision())!=1)throw new BusinessException(ErrorCode.REVISION_CONFLICT);
  return find(kind,key);
 }
 private Map<String,Object> goalState(){String user=CurrentUser.idOrThrow();var rows=jdbc.queryForList("SELECT * FROM fitness_goal_state WHERE user_id=?",user);return rows.isEmpty()?new HashMap<>(Map.of("revision",-1L)):rows.get(0);}
 private void ensureGoalState(){jdbc.update("INSERT IGNORE INTO fitness_goal_state(user_id) VALUES(?)",CurrentUser.idOrThrow());}
 @Transactional public Entry saveGoal(String key,Write body,String requestKey){lock();ensureGoalState();return write("goal",key,body,requestKey);}
 @Transactional public Map<String,Object> endGoal(EndGoal body,String requestKey){idempotency(requestKey);lock();ensureGoalState();String user=CurrentUser.idOrThrow(),hash=replay.hash(body);var old=replay.find(user,"fitness-end-goal",requestKey,hash,JsonNode.class);if(old!=null)return mapper.convertValue(old,Map.class);var state=goalState();if(body.expectedGoalRevision()!=((Number)state.get("revision")).longValue())throw new BusinessException(ErrorCode.REVISION_CONFLICT);require(state.get("current_key")!=null,"没有当前目标可以结束");long version=body.expectedGoalRevision()+1;
  jdbc.update("UPDATE fitness_goal_state SET current_key=NULL,revision=? WHERE user_id=?",version,user);jdbc.update("INSERT INTO fitness_goal_event(user_id,version,goal_key,action) VALUES(?,?,?,'ENDED')",user,version,state.get("current_key"));var result=Map.<String,Object>of("revision",version,"ended",true);replay.save(user,"fitness-end-goal",requestKey,hash,result);return result;
 }
 public Page<GoalEvent> goalHistory(int page,int size){require(page>=1&&size>=1&&size<=100,"分页范围不合法");String user=CurrentUser.idOrThrow();var rows=jdbc.query("SELECT * FROM fitness_goal_event WHERE user_id=? ORDER BY version DESC LIMIT ? OFFSET ?",(rs,n)->new GoalEvent(rs.getLong("version"),rs.getString("goal_key"),rs.getString("action"),rs.getString("created_at")),user,size,(page-1)*size);return new Page<>(rows,jdbc.queryForObject("SELECT COUNT(*) FROM fitness_goal_event WHERE user_id=?",Long.class,user),page,size);}
 @Transactional public void delete(String kind,String key,long revision){key(kind,key,false);lock();if(kind.equals("goal"))throw new BusinessException(ErrorCode.CONFLICT,"目标历史保留，调整请新增目标");if(jdbc.update("UPDATE fitness_record SET payload=NULL,revision=revision+1,updated_at=CURRENT_TIMESTAMP(6) WHERE user_id=? AND kind=? AND record_key=? AND revision=? AND payload IS NOT NULL",CurrentUser.idOrThrow(),kind,key,revision)!=1)throw new BusinessException(ErrorCode.REVISION_CONFLICT);}
 @Transactional public Entry copy(Copy body,String requestKey){idempotency(requestKey);lock();require(body.destination()!=null,"目标日期不能为空");require(body.sourceKind()!=null&&Set.of("training-plan","training-template","meal-plan","meal-template").contains(body.sourceKind()),"复制源类型不合法");key(body.sourceKind(),body.sourceKey(),false);String user=CurrentUser.idOrThrow(),hash=replay.hash(body);var old=replay.find(user,"fitness-copy",requestKey,hash,Entry.class);if(old!=null)return old;
  Entry source=find(body.sourceKind(),body.sourceKey());if(source==null||source.data()==null)throw new BusinessException(ErrorCode.NOT_FOUND);String kind=body.sourceKind().startsWith("training")?"training-plan":"meal-plan";JsonNode data=body.sourceKind().endsWith("template")?source.data().path("plan"):source.data();Entry result=writeLocked(kind,body.destination().toString(),new Write(body.expectedRevision(),null,data));replay.save(user,"fitness-copy",requestKey,hash,result);return result;
 }
 @Transactional public Batch generateWeek(GenerateWeek body,String requestKey){idempotency(requestKey);lock();require(body.monday()!=null&&body.monday().getDayOfWeek()==java.time.DayOfWeek.MONDAY,"周计划起始日期必须是周一");require(body.expectedRevisions()!=null&&body.expectedRevisions().size()==7,"请提供7天版本");key("week-template",body.templateKey(),false);String user=CurrentUser.idOrThrow(),hash=replay.hash(body);Batch old=replay.find(user,"fitness-generate-week",requestKey,hash,Batch.class);if(old!=null)return old;Entry source=find("week-template",body.templateKey());if(source==null||source.data()==null)throw new BusinessException(ErrorCode.NOT_FOUND);var template=model(source.data(),WeekTemplate.class);List<Entry> rows=new ArrayList<>();for(int i=0;i<7;i++){require(body.expectedRevisions().get(i)!=null,"版本不能为空");rows.add(writeLocked("training-plan",body.monday().plusDays(i).toString(),new Write(body.expectedRevisions().get(i),null,mapper.valueToTree(template.days().get(i)))));}Batch result=new Batch(rows);replay.save(user,"fitness-generate-week",requestKey,hash,result);return result;}
 public List<Day> history(LocalDate from,LocalDate to){range(from,to);Map<String,Map<String,Entry>> rows=new HashMap<>();jdbc.query("SELECT * FROM fitness_record WHERE user_id=? AND record_key BETWEEN ? AND ?",(rs,n)->row(rs),CurrentUser.idOrThrow(),from.toString(),to.toString()).stream().filter(e->DAILY.contains(e.kind())).forEach(e->rows.computeIfAbsent(e.key(),k->new LinkedHashMap<>()).put(e.kind(),e));return from.datesUntil(to.plusDays(1)).map(d->makeDay(d,rows.getOrDefault(d.toString(),Map.of()))).toList();}
 private void range(LocalDate from,LocalDate to){require(from!=null&&to!=null&&!from.isAfter(to)&&ChronoUnit.DAYS.between(from,to)<=366,"一次最多查看367天");require(!from.isBefore(LocalDate.of(1900,1,1))&&!to.isAfter(today().plusYears(5)),"日期范围不合法");}
 private Day makeDay(LocalDate date,Map<String,Entry> records){boolean checked=has(records,"checkin"),rest=has(records,"training-plan")&&records.get("training-plan").data().path("rest").asBoolean();boolean partial=Set.of("weight","training","meals","water").stream().anyMatch(k->has(records,k));String state=checked?"CHECKED_IN":date.isAfter(today())?"FUTURE":date.equals(today())?"TODAY_PENDING":partial?"PARTIAL_RECORDS":"PAST_MISSING";String training=has(records,"training")?records.get("training").data().path("status").asText():rest?"REST":has(records,"training-plan")?"PENDING":"UNPLANNED";var foods=has(records,"meals")?model(records.get("meals").data(),Meals.class).foods():List.<Food>of();return new Day(date,records,checked,rest,partial,state,training,nutrition(foods),nutrition(has(records,"meal-plan")?model(records.get("meal-plan").data(),Meals.class).foods():List.of()));}
 private static boolean has(Map<String,Entry> r,String kind){return r.containsKey(kind)&&r.get(kind).data()!=null;}
 public Day day(LocalDate date){return history(date,date).get(0);}
 private Nutrient nutrient(List<Food> foods,java.util.function.Function<Food,BigDecimal> value){BigDecimal sum=BigDecimal.ZERO;int count=0;for(Food f:foods){BigDecimal v=value.apply(f);if(v!=null){sum=sum.add(v);count++;}}return new Nutrient(count==0?null:sum,!foods.isEmpty()&&count==foods.size(),count,foods.size());}
 private Nutrition nutrition(List<Food> foods){return new Nutrition(nutrient(foods,Food::kcal),nutrient(foods,Food::protein),nutrient(foods,Food::carbs),nutrient(foods,Food::fat));}
 public WeightWindow weightWindow(LocalDate end){LocalDate from=end.minusDays(6);List<BigDecimal> values=jdbc.query("SELECT weight_kg FROM fitness_record WHERE user_id=? AND kind='weight' AND record_key BETWEEN ? AND ? AND payload IS NOT NULL ORDER BY record_key",(rs,n)->rs.getBigDecimal(1),CurrentUser.idOrThrow(),from.toString(),end.toString());return new WeightWindow(values.isEmpty()?null:values.stream().reduce(BigDecimal.ZERO,BigDecimal::add).divide(BigDecimal.valueOf(values.size()),3,RoundingMode.HALF_UP),values.size(),from,end);}
 private JsonNode read(String s){try{return mapper.readTree(s);}catch(Exception e){throw new IllegalStateException(e);}}
 public Stats statistics(LocalDate from,LocalDate to){List<Day> days=history(from,to);int checks=0,diet=0,planned=0,complete=0,partial=0,skipped=0,rest=0;List<Food> foods=new ArrayList<>();for(Day d:days){if(d.checkedIn())checks++;if(has(d.records(),"meals")){diet++;foods.addAll(model(d.records().get("meals").data(),Meals.class).foods());}if(d.rest())rest++;Entry actual=d.records().get("training");Training t=actual==null||actual.data()==null?null:model(actual.data(),Training.class);TrainingPlan basis=t!=null?t.planSnapshot():has(d.records(),"training-plan")?model(d.records().get("training-plan").data(),TrainingPlan.class):null;
  if(!d.date().isAfter(today())&&basis!=null&&!basis.rest()){planned++;if(t!=null){if(t.status()==TrainingStatus.COMPLETED)complete++;else if(t.status()==TrainingStatus.PARTIAL)partial++;else if(t.status()==TrainingStatus.SKIPPED)skipped++;}}
 }return new Stats(from,to,checks,diet,planned,complete,partial,skipped,rest,planned==0?null:BigDecimal.valueOf(100L*complete).divide(BigDecimal.valueOf(planned),2,RoundingMode.HALF_UP),weightWindow(to),nutrition(foods));}
 private int streak(LocalDate end){int count=0;LocalDate cursor=end;while(true){var rows=jdbc.queryForList("SELECT record_key FROM fitness_record WHERE user_id=? AND kind='checkin' AND payload IS NOT NULL AND record_key<=? ORDER BY record_key DESC LIMIT 100",String.class,CurrentUser.idOrThrow(),cursor.toString());if(rows.isEmpty())return count;for(String key:rows){if(!key.equals(cursor.toString()))return count;count++;cursor=cursor.minusDays(1);}if(rows.size()<100)return count;}}
 public Summary summary(){LocalDate now=today(),monday=now.minusDays(now.getDayOfWeek().getValue()-1);var state=goalState();Entry goal=state.get("current_key")==null?null:find("goal",state.get("current_key").toString());Entry weight=jdbc.query("SELECT * FROM fitness_record WHERE user_id=? AND kind='weight' AND record_key<=? AND payload IS NOT NULL ORDER BY record_key DESC LIMIT 1",(rs,n)->row(rs),CurrentUser.idOrThrow(),now.toString()).stream().findFirst().orElse(null);int count=jdbc.queryForObject("SELECT COUNT(*) FROM fitness_record WHERE user_id=? AND kind='checkin' AND payload IS NOT NULL AND record_key BETWEEN ? AND ?",Integer.class,CurrentUser.idOrThrow(),monday.toString(),now.toString());int elapsed=now.getDayOfWeek().getValue();Entry check=find("checkin",now.toString());return new Summary(now,goal,((Number)state.get("revision")).longValue(),weight,streak(check!=null&&check.data()!=null?now:now.minusDays(1)),count,elapsed,BigDecimal.valueOf(100L*count).divide(BigDecimal.valueOf(elapsed),2,RoundingMode.HALF_UP),weightWindow(now));}
}
