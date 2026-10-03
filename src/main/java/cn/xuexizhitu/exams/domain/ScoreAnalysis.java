package cn.xuexizhitu.exams.domain;
import cn.xuexizhitu.exams.domain.ExamTypes.*;
import java.time.LocalDate;
import java.util.*;
import java.math.*;
/** Pure rule: chronological first valid per paper, then age window, then latest five. */ public final class ScoreAnalysis {
    private ScoreAnalysis() {
    }
    public record Analysis(Prediction prediction,Map<String,List<String>> reasons,Map<String,String> firstIds,Set<String> included) {
    }
    public static List<String> eligibility(ScoreData r) {
        List<String> reasons=new ArrayList<>();
        if(!r.complete())reasons.add("INCOMPLETE");
        if(!r.closedBook())reasons.add("OPEN_BOOK");
        if(r.minutes()>r.limitMinutes())reasons.add("OVERTIME");
        if(r.answersSeenBefore()==null)reasons.add("ANSWERS_STATE_UNKNOWN");
        else if(r.answersSeenBefore())reasons.add("ANSWERS_SEEN");
        return reasons;
    }
    public static Analysis analyze(String course,List<ScoreData> records,LocalDate asOf) {
        var ordered=records.stream().sorted(Comparator.comparing(ScoreData::practicedOn)                 .thenComparing(ScoreData::createdAt).thenComparing(ScoreData::id)).toList();
        Map<String,ScoreData> first=new LinkedHashMap<>(),representatives=new LinkedHashMap<>();
        Map<String,List<String>> reasons=new HashMap<>();
        for(ScoreData r:ordered) {
            representatives.putIfAbsent(r.paperId(),r);
            List<String> why=eligibility(r);
            reasons.put(r.id(),why);
            if(why.isEmpty()&&!r.practicedOn().isAfter(asOf))first.putIfAbsent(r.paperId(),r);
        }
        var recent=first.values().stream().filter(r->!r.practicedOn().isBefore(asOf.minusDays(60))).toList();
        recent=recent.subList(Math.max(0,recent.size()-5),recent.size());
        Set<String> included=new HashSet<>();
        Map<String,String> firstIds=new HashMap<>();
        first.forEach((p,r)->firstIds.put(p,r.id()));
        List<PredictionSample> samples=new ArrayList<>();
        BigDecimal sum=BigDecimal.ZERO,min=null,max=null;
        int totalWeight=0,atLeast60=0,index=0;
        for(ScoreData r:recent) {
            int weight=++index;
            included.add(r.id());
            BigDecimal score=r.score();
            sum=sum.add(score.multiply(BigDecimal.valueOf(weight)));
            totalWeight+=weight;
            min=min==null?score:min.min(score);
            max=max==null?score:max.max(score);
            if(score.compareTo(BigDecimal.valueOf(60))>=0)atLeast60++;
            samples.add(new PredictionSample(r.paperId(),r.paperKey(),r.id(),r.practicedOn(),r.createdAt(),score,weight));
        }
        List<PredictionExclusion> exclusions=new ArrayList<>();
        for(var e:representatives.entrySet()) {
            ScoreData selected=first.get(e.getKey());
            String reason=selected==null?"NO_VALID_RECORD":                     selected.practicedOn().isBefore(asOf.minusDays(60))?"FIRST_VALID_TOO_OLD":!included.contains(selected.id())?"OUTSIDE_LATEST_FIVE":null;
            if(reason!=null)exclusions.add(new PredictionExclusion(e.getKey(),e.getValue().paperKey(),firstIds.get(e.getKey()),reason,                     switch(reason) {
                case "NO_VALID_RECORD"->"无符合条件的记录";case "FIRST_VALID_TOO_OLD"->"首条有效记录超过60天，不使用重做替代";default->"不在最近五套样本";
            }
            ));
        }
        for(ScoreData r:ordered) {
            List<String> why=reasons.get(r.id());
            String f=firstIds.get(r.paperId());
            if(why.isEmpty()&&!Objects.equals(f,r.id()))why.add("NOT_FIRST_VALID");
            if(Objects.equals(f,r.id())&&!included.contains(f))why.add(r.practicedOn().isBefore(asOf.minusDays(60))?"FIRST_VALID_TOO_OLD":"OUTSIDE_LATEST_FIVE");
        }
        Integer predicted=recent.size()<3?null:sum.divide(BigDecimal.valueOf(totalWeight),0,RoundingMode.HALF_UP).intValue();
        return new Analysis(new Prediction(course,asOf,predicted==null?"INSUFFICIENT_SAMPLES":"AVAILABLE",predicted,recent.size(),3,5,60,                 List.copyOf(samples),List.copyOf(exclusions),min,max,atLeast60,"FIRST_VALID_60D_V1"),reasons,firstIds,included);
    }
}
