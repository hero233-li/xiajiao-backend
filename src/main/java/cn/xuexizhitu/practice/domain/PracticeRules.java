package cn.xuexizhitu.practice.domain;
import java.math.*;
public final class PracticeRules {
    private PracticeRules() {
    }
    public static int threshold(int available,Integer override,int floor,int cap,BigDecimal ratio) {
        return override!=null?override:Math.min(cap,Math.max(floor,ratio.multiply(BigDecimal.valueOf(available)).setScale(0,RoundingMode.CEILING).intValueExact()));
    }
    public static double accuracy(int correct,int attempts) {
        return attempts==0?0:Math.round(10000.0*correct/attempts)/100.0;
    }
}
