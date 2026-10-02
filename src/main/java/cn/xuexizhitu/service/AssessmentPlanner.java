package cn.xuexizhitu.service;
import java.math.*;
import java.util.*;

/** Distinct question positions for every point, using augmenting paths rather than greedy choice. */
public final class AssessmentPlanner {
    private AssessmentPlanner() {}
    public record Candidate(String revision,String question,Set<String> points,boolean answered) {}
    public static List<Candidate> chapter(List<String> points,List<Candidate> candidates,int count) {
        if(points.isEmpty()||points.size()>count||candidates.size()<count)throw new IllegalArgumentException("题量或考点配置不足");
        List<Candidate> order=new ArrayList<>(candidates);Collections.shuffle(order);order.sort(Comparator.comparing(Candidate::answered));
        Map<String,String> owners=new HashMap<>();
        for(String point:points)if(!assign(point,order,owners,new HashSet<>()))throw new IllegalArgumentException("考点无法分配不同题位");
        List<Candidate> chosen=new ArrayList<>();for(Candidate c:order)if(owners.containsKey(c.revision()))chosen.add(c);
        for(Candidate c:order)if(chosen.size()<count&&!owners.containsKey(c.revision()))chosen.add(c);
        Collections.shuffle(chosen);return chosen;
    }
    private static boolean assign(String point,List<Candidate> order,Map<String,String> owners,Set<String> visited) {
        for(Candidate c:order)if(c.points().contains(point)&&visited.add(c.revision())) {
            String previous=owners.get(c.revision());
            if(previous==null||assign(previous,order,owners,visited)){owners.put(c.revision(),point);return true;}
        }
        return false;
    }
    public static Map<String,Integer> quotas(Map<String,BigDecimal> weights,int count) {
        if(weights.isEmpty()||weights.values().stream().anyMatch(w->w.signum()<0)||weights.values().stream().reduce(BigDecimal.ZERO,BigDecimal::add).compareTo(BigDecimal.ONE)!=0)throw new IllegalArgumentException("权重须完整且合计为1");
        Map<String,Integer> result=new TreeMap<>();Map<String,BigDecimal> remainders=new HashMap<>();
        weights.forEach((id,w)->{BigDecimal raw=w.multiply(BigDecimal.valueOf(count));int floor=raw.intValue();result.put(id,floor);remainders.put(id,raw.subtract(BigDecimal.valueOf(floor)));});
        List<String> order=new ArrayList<>(result.keySet());order.sort(Comparator.<String,BigDecimal>comparing(remainders::get).reversed().thenComparing(Comparator.naturalOrder()));
        int remaining=count-result.values().stream().mapToInt(Integer::intValue).sum();for(int i=0;i<remaining;i++)result.compute(order.get(i),(k,v)->v+1);
        return result;
    }
}
