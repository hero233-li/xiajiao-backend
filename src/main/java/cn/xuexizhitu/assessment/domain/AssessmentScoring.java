package cn.xuexizhitu.assessment.domain;
import java.math.*;
public final class AssessmentScoring {
    private AssessmentScoring() {
    }
    public record Grade(BigDecimal displayedScore,boolean passed) {
    }
    public static Grade grade(int correct,int count,BigDecimal passScore) {
        if(count<=0||correct<0||correct>count||passScore.signum()<0||passScore.compareTo(BigDecimal.valueOf(100))>0)throw new IllegalArgumentException("Invalid scoring inputs");
        BigDecimal points=BigDecimal.valueOf(correct*100L);
        return new Grade(points.divide(BigDecimal.valueOf(count),2,RoundingMode.HALF_UP),points.compareTo(passScore.multiply(BigDecimal.valueOf(count)))>=0);
    }
}
