package cn.xuexizhitu.service;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import cn.xuexizhitu.service.AssessmentPlanner.Candidate;
class AssessmentPlannerTest {
    @Test void augmentingPathsFindCoverageWhichGreedyCanMiss(){
        List<Candidate> bank=List.of(new Candidate("1","a",Set.of("p","q"),false),new Candidate("2","b",Set.of("p"),true));
        for(int i=0;i<30;i++)assertThat(AssessmentPlanner.chapter(List.of("p","q"),bank,2)).extracting(Candidate::revision).containsExactlyInAnyOrder("1","2");
    }
    @Test void impossibleDistinctPositionsRejectEvenWhenUnionCoversAllPoints(){
        var bank=List.of(new Candidate("1","a",Set.of("p","q"),false),new Candidate("2","b",Set.of(),false));
        assertThatThrownBy(()->AssessmentPlanner.chapter(List.of("p","q"),bank,2)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void fillsWithUnansweredAndUsesDecimalLargestRemainderWithStableTies(){
        var bank=List.of(new Candidate("1","a",Set.of("p"),false),new Candidate("2","b",Set.of(),false),new Candidate("3","c",Set.of(),true));
        assertThat(AssessmentPlanner.chapter(List.of("p"),bank,2)).extracting(Candidate::revision).containsExactlyInAnyOrder("1","2");
        assertThat(AssessmentPlanner.quotas(Map.of("b",new BigDecimal("0.5"),"a",new BigDecimal("0.5")),3)).containsEntry("a",2).containsEntry("b",1);
        assertThatThrownBy(()->AssessmentPlanner.quotas(Map.of("a",new BigDecimal("0.99")),40)).isInstanceOf(IllegalArgumentException.class);
    }
}
