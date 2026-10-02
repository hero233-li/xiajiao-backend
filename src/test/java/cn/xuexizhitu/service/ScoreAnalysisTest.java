package cn.xuexizhitu.service;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static cn.xuexizhitu.common.BusinessData.*;
import static org.assertj.core.api.Assertions.*;
class ScoreAnalysisTest {
    private final LocalDate today=LocalDate.of(2026,10,2);
    private Map<String,Object> record(String id,String paper,int days,int score,boolean complete,Object seen){return obj("id",id,"paperId",paper,"paperKey","00023:"+paper,"practicedOn",today.minusDays(days).toString(),"createdAt",Instant.EPOCH.plusSeconds(days),"score",score,"minutes",120,"limitMinutes",150,"complete",complete,"closedBook",true,"answersSeenBefore",seen);}
    @Test void firstValidIsChosenBeforeAgeFilterAndLaterRedoCannotReplaceExpiredFirst(){var rows=List.of(record("a","p",61,60,true,false),record("b","p",2,100,true,false));var a=ScoreAnalysis.analyze("course",rows,today);assertThat(a.included()).isEmpty();assertThat(a.firstIds()).containsEntry("p","a");assertThat(a.reasons().get("a")).contains("FIRST_VALID_TOO_OLD");assertThat(a.reasons().get("b")).contains("NOT_FIRST_VALID");}
    @Test void invalidEarlyRecordIsSkippedAndDay60IncludedWithUnknownAnswersExcluded(){var rows=List.of(record("bad","p",61,50,false,false),record("unknown","q",60,100,true,null),record("good","p",60,75,true,false));var a=ScoreAnalysis.analyze("course",rows,today);assertThat(a.included()).containsExactly("good");assertThat(a.reasons().get("unknown")).contains("ANSWERS_STATE_UNKNOWN");}
    @Test void latestFiveWeightOneToFiveAndThreeSamplesRequired(){List<Map<String,Object>> rows=new ArrayList<>();for(int i=0;i<6;i++)rows.add(record("r"+i,"p"+i,10-i,50+i*10,true,false));var a=ScoreAnalysis.analyze("course",rows,today);assertThat(a.included()).hasSize(5).doesNotContain("r0");assertThat(a.prediction().get("predictedScore")).isEqualTo(87);assertThat(a.prediction().get("actualMinimum").toString()).isEqualTo("60");assertThat(ScoreAnalysis.analyze("course",rows.subList(0,2),today).prediction().get("predictedScore")).isNull();}
}
