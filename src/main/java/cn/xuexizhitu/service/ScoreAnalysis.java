package cn.xuexizhitu.service;
import java.time.*;
import java.util.*;
import java.math.*;
import static cn.xuexizhitu.common.BusinessData.*;
/** Recomputes first-valid selection across all cycles before applying the age window. */
public final class ScoreAnalysis {
    private ScoreAnalysis() {}
    public record Analysis(Map<String,Object> prediction,Map<String,List<String>> reasons,Map<String,String> firstIds,Set<String> included) {}
    public static Analysis analyze(String course,List<Map<String,Object>> records,LocalDate asOf){
        Map<String,Map<String,Object>> first=new LinkedHashMap<>(),representatives=new LinkedHashMap<>();Map<String,List<String>> reasons=new HashMap<>();
        for(var r:records){String id=(String)r.get("id"),paper=(String)r.get("paperId");representatives.putIfAbsent(paper,r);List<String> exclusions=new ArrayList<>();if(!Boolean.TRUE.equals(r.get("complete")))exclusions.add("INCOMPLETE");if(!Boolean.TRUE.equals(r.get("closedBook")))exclusions.add("OPEN_BOOK");if(((Number)r.get("minutes")).intValue()>((Number)r.get("limitMinutes")).intValue())exclusions.add("OVERTIME");if(r.get("answersSeenBefore")==null)exclusions.add("ANSWERS_STATE_UNKNOWN");else if(Boolean.TRUE.equals(r.get("answersSeenBefore")))exclusions.add("ANSWERS_SEEN");reasons.put(id,exclusions);
            if(exclusions.isEmpty()&&!LocalDate.parse(r.get("practicedOn").toString()).isAfter(asOf))first.putIfAbsent(paper,r);
        }
        List<Map<String,Object>> recent=new ArrayList<>();for(var r:first.values())if(!LocalDate.parse(r.get("practicedOn").toString()).isBefore(asOf.minusDays(60)))recent.add(r);
        if(recent.size()>5)recent=new ArrayList<>(recent.subList(recent.size()-5,recent.size()));Set<String> included=new HashSet<>();Map<String,String> firstIds=new HashMap<>();first.forEach((p,r)->firstIds.put(p,(String)r.get("id")));List<Map<String,Object>> samples=new ArrayList<>();BigDecimal sum=BigDecimal.ZERO,min=null,max=null;int totalWeight=0,atLeast60=0,index=0;
        for(var r:recent){int weight=++index;String id=(String)r.get("id");included.add(id);BigDecimal score=new BigDecimal(r.get("score").toString());sum=sum.add(score.multiply(BigDecimal.valueOf(weight)));totalWeight+=weight;min=min==null?score:min.min(score);max=max==null?score:max.max(score);if(score.compareTo(BigDecimal.valueOf(60))>=0)atLeast60++;samples.add(obj("paperId",r.get("paperId"),"paperKey",r.get("paperKey"),"recordId",id,"practicedOn",r.get("practicedOn"),"createdAt",r.get("createdAt"),"score",score,"weight",weight));}
        List<Map<String,Object>> exclusions=new ArrayList<>();for(var e:representatives.entrySet()){var selected=first.get(e.getKey());String reason=selected==null?"NO_VALID_RECORD":LocalDate.parse(selected.get("practicedOn").toString()).isBefore(asOf.minusDays(60))?"FIRST_VALID_TOO_OLD":!included.contains(selected.get("id"))?"OUTSIDE_LATEST_FIVE":null;if(reason!=null)exclusions.add(obj("paperId",e.getKey(),"paperKey",e.getValue().get("paperKey"),"firstValidRecordId",firstIds.get(e.getKey()),"reason",reason,"detail",reason.equals("NO_VALID_RECORD")?"无符合条件的记录":reason.equals("FIRST_VALID_TOO_OLD")?"首条有效记录超过60天，不使用重做替代":"不在最近五套样本"));}
        for(var r:records){List<String> list=reasons.get(r.get("id"));String f=firstIds.get(r.get("paperId"));if(list.isEmpty()&&!Objects.equals(f,r.get("id")))list.add("NOT_FIRST_VALID");if(Objects.equals(f,r.get("id"))&&!included.contains(f))list.add(LocalDate.parse(r.get("practicedOn").toString()).isBefore(asOf.minusDays(60))?"FIRST_VALID_TOO_OLD":"OUTSIDE_LATEST_FIVE");}
        Integer predicted=recent.size()<3?null:sum.divide(BigDecimal.valueOf(totalWeight),0,RoundingMode.HALF_UP).intValue();
        return new Analysis(obj("courseId",course,"asOf",asOf,"status",predicted==null?"INSUFFICIENT_SAMPLES":"AVAILABLE","predictedScore",predicted,"sampleCount",recent.size(),"minimumSamples",3,"maximumSamples",5,"windowDays",60,"samples",samples,"exclusions",exclusions,"actualMinimum",min,"actualMaximum",max,"scoresAtLeast60",atLeast60,"ruleVersion","FIRST_VALID_60D_V1"),reasons,firstIds,included);
    }
}
