package cn.xuexizhitu.service;
import cn.xuexizhitu.assessment.domain.AssessmentScoring;
import cn.xuexizhitu.practice.domain.PracticeRules;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class BusinessRulesTest {
    @Test void roundedDisplayCannotGrantAPassBelowTheExactThreshold(){
        var grade=AssessmentScoring.grade(29,30,new BigDecimal("96.67"));assertThat(grade.displayedScore()).isEqualByComparingTo("96.67");assertThat(grade.passed()).isFalse();
        assertThat(AssessmentScoring.grade(29,30,new BigDecimal("96.66")).passed()).isTrue();assertThat(AssessmentScoring.grade(0,10,BigDecimal.ONE).passed()).isFalse();
    }
    @Test void originalQuestionGateRoundsUpThenAppliesBoundsAndExplicitOverride(){
        assertThat(PracticeRules.threshold(34,null,20,100,new BigDecimal("0.6"))).isEqualTo(21);
        assertThat(PracticeRules.threshold(1,null,20,100,new BigDecimal("0.6"))).isEqualTo(20);
        assertThat(PracticeRules.threshold(999,null,20,100,new BigDecimal("0.6"))).isEqualTo(100);
        assertThat(PracticeRules.threshold(34,27,20,100,new BigDecimal("0.6"))).isEqualTo(27);
    }
}
