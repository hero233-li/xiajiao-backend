package cn.xuexizhitu.fitness.domain;
import java.time.*;
import cn.xuexizhitu.common.*;
/** Natural dates, never host timezone or UTC dates. */
public final class FitnessDates {
 public static LocalDate today(Clock clock){return LocalDate.now(clock.withZone(ZoneId.of("Asia/Shanghai")));}
 public static void actual(LocalDate date,LocalDate today){if(date.isAfter(today))throw new BusinessException(ErrorCode.INVALID_DATE,"实际记录不能填写未来日期");}
 private FitnessDates(){}
}
