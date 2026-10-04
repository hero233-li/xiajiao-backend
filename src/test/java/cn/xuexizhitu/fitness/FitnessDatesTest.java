package cn.xuexizhitu.fitness;
import cn.xuexizhitu.fitness.domain.FitnessDates;
import cn.xuexizhitu.common.BusinessException;
import org.junit.jupiter.api.Test;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
class FitnessDatesTest {
 @Test void midnightUsesShanghaiRegardlessOfClockHostZone(){
  Clock before=Clock.fixed(Instant.parse("2026-10-03T15:59:59Z"),ZoneId.of("America/New_York"));
  Clock after=Clock.fixed(Instant.parse("2026-10-03T16:00:00Z"),ZoneId.of("America/New_York"));
  assertThat(FitnessDates.today(before)).isEqualTo(LocalDate.of(2026,10,3));assertThat(FitnessDates.today(after)).isEqualTo(LocalDate.of(2026,10,4));
  assertThatThrownBy(()->FitnessDates.actual(LocalDate.of(2026,10,4),FitnessDates.today(before))).isInstanceOf(BusinessException.class);
  assertThatCode(()->FitnessDates.actual(LocalDate.of(2026,10,4),FitnessDates.today(after))).doesNotThrowAnyException();
  assertThatCode(()->FitnessDates.actual(LocalDate.of(2026,10,2),FitnessDates.today(before))).doesNotThrowAnyException();
 }
}
