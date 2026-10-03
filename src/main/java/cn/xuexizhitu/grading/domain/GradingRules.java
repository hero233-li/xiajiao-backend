package cn.xuexizhitu.grading.domain;
import cn.xuexizhitu.common.*;
import static cn.xuexizhitu.common.BusinessData.require;
import static cn.xuexizhitu.grading.domain.GradingTypes.*;
import java.math.BigDecimal;
import java.util.*;
public final class GradingRules {
    private GradingRules() {
    }
    private static void check(boolean condition,String message) {
        require(condition,ErrorCode.INVALID_RELATION,message);
    }
    public static void rubric(RubricDocument doc) {
        check(doc!=null && doc.questions()!=null && !doc.questions().isEmpty(),"评分标准不能为空");
        Set<String> numbers=new HashSet<>();
        BigDecimal total=BigDecimal.ZERO;
        for(Question q:doc.questions()) {
            check(numbers.add(q.number()),"题号重复");
            Set<String> points=new HashSet<>();
            BigDecimal subtotal=BigDecimal.ZERO;
            for(Point p:q.points()) {
                check(points.add(p.id()),"评分点重复");
                check(p.maximum().signum()>0,"评分点必须为正分");
                subtotal=subtotal.add(p.maximum());
            }
            check(q.maximum().compareTo(subtotal)==0,"逐题满分必须等于评分点合计");
            total=total.add(q.maximum());
        }
        check(total.compareTo(new BigDecimal("100"))==0,"第一版只支持满分100分的试卷");
    }
    public record Validation(BigDecimal total,boolean review) {
    }
    public static Validation result(Rubric rubric,Result result,int pageCount) {
        check(result!=null && result.rubricId().toString().equals(rubric.id()),"评分标准版本不匹配");
        rubric(rubric.document());
        Map<String,Answer> answers=new HashMap<>();
        for(Answer a:result.answers()) check(answers.put(a.number(),a)==null,"结果题号重复");
        check(answers.size()==rubric.document().questions().size(),"结果必须覆盖所有题号");
        BigDecimal total=BigDecimal.ZERO;
        boolean review=!result.reviewItems().isEmpty();
        for(Question q:rubric.document().questions()) {
            Answer a=answers.get(q.number());
            check(a!=null,"缺少题号 "+q.number());
            check(!a.pages().isEmpty() || !a.reviewItems().isEmpty(),"没有图片页码时必须标为待核对");
            for(Integer page:a.pages()) check(page!=null && page>=1 && page<=pageCount,"图片页码超出答卷范围");
            Map<String,Earned> earned=new HashMap<>();
            for(Earned e:a.points()) check(earned.put(e.pointId(),e)==null,"评分点重复");
            check(earned.size()==q.points().size(),"必须覆盖全部评分点");
            for(Point p:q.points()) {
                Earned e=earned.get(p.id());
                check(e!=null,"评分点不存在");
                check(e.score()!=null && e.score().signum()>=0 && e.score().compareTo(p.maximum())<=0 && e.score().scale()<=2,"评分点分值越界");
                check(e.score().compareTo(p.maximum())==0 || !e.reason().isBlank(),"扣分必须说明原因");
                total=total.add(e.score());
            }
            review|=!a.reviewItems().isEmpty();
        }
        return new Validation(total,review);
    }
}
