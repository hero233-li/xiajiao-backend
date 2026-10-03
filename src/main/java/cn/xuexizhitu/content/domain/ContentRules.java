package cn.xuexizhitu.content.domain;
import cn.xuexizhitu.contract.WorkflowDtos.*;
import cn.xuexizhitu.assessment.domain.AssessmentPlanner;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.Normalizer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
/** Pure release rules. Storage and file availability are checked by the application. */ public final class ContentRules {
    private ContentRules() {
    }
    private static String normalize(String s) {
        return Normalizer.normalize(s,Normalizer.Form.NFKC).replaceAll("(?U)\\s+","").trim();
    }
    public static String fingerprint(AdminQuestion q) {
        List<String> options=q.options().stream().map(ContentRules::normalize).sorted().toList();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(new ObjectMapper().writeValueAsString(List.of(normalize(q.stem()),options)).getBytes(StandardCharsets.UTF_8)));
        }
        catch(java.io.IOException|NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
    public static void issue(List<ReleaseValidationIssuesItem> issues,String code,String path,String message) {
        issues.add(new ReleaseValidationIssuesItem(code,path,message));
    }
    public static ReleaseValidation validate(ContentDraft draft,Map<String,String> existing) {
        List<ReleaseValidationIssuesItem> issues=new ArrayList<>();
        Map<UUID,UUID> points=new HashMap<>();
        Set<UUID> chapters=new HashSet<>(),items=new HashSet<>(),questions=new HashSet<>();
        Set<String> hashes=new HashSet<>();
        int itemCount=0;
        for(ChapterDraft ch:draft.catalog().chapters()) {
            if(!chapters.add(ch.id()))issue(issues,"DUPLICATE_ID","catalog","章节ID重复");
            for(PointDraft p:ch.points())if(points.put(p.id(),ch.id())!=null)issue(issues,"DUPLICATE_ID","catalog","考点ID重复");
            for(ItemDraft item:ch.items()) {
                itemCount++;
                if(!items.add(item.id()))issue(issues,"DUPLICATE_ID","catalog","条目ID重复");
            }
        }
        Map<UUID,List<AssessmentPlanner.Candidate>> bank=new HashMap<>();
        for(AdminQuestion q:draft.questions().questions()) {
            if(!questions.add(q.id()))issue(issues,"DUPLICATE_ID","questions","题目ID重复");
            if(!chapters.contains(q.chapterId()))issue(issues,"INVALID_MAPPING","questions","题目章节不属于本版");
            if(q.correctOption()>=q.options().size()||!q.options().get(q.correctOption()).equals(q.correctAnswer()))issue(issues,"INVALID_ANSWER","questions","答案索引或文本不一致");
            Set<String> options=new HashSet<>();
            for(String o:q.options())if(!options.add(normalize(o)))issue(issues,"DUPLICATE_OPTION","questions","选项标准化后重复");
            String hash=fingerprint(q);
            if(!hashes.add(hash))issue(issues,"DUPLICATE_CONTENT","questions","不同题目内容重复");
            if(existing.containsKey(hash)&&!existing.get(hash).equals(q.id().toString()))issue(issues,"DUPLICATE_CONTENT","questions","与现有稳定题目重复");
            for(UUID point:q.pointIds())if(!q.chapterId().equals(points.get(point)))issue(issues,"INVALID_MAPPING","questions","考点不属于题目章节");
            if(q.eligibleOriginal()&&q.mode().equals("CHAPTER"))bank.computeIfAbsent(q.chapterId(),k->new ArrayList<>()).add(new AssessmentPlanner.Candidate(q.id().toString(),q.id().toString(),q.pointIds().stream().map(UUID::toString).collect(java.util.stream.Collectors.toSet()),false));
        }
        AssessmentPolicy policy=draft.policy();
        if(policy.gateFloor()>policy.gateCap()||policy.chapterMinQuestions()>policy.chapterMaxQuestions())issue(issues,"INVALID_POLICY","assessment-policy","策略范围无效");
        for(ChapterDraft chapter:draft.catalog().chapters())if(chapter.participatesInAssessment()) {
            var ids=points.entrySet().stream().filter(e->e.getValue().equals(chapter.id())).map(e->e.getKey().toString()).sorted().toList();
            int count=Math.max(policy.chapterMinQuestions(),ids.size());
            if(count>policy.chapterMaxQuestions())issue(issues,"TOO_MANY_POINTS","catalog","参与检测章考点超过题数上限");
            else try {
                AssessmentPlanner.chapter(ids,bank.getOrDefault(chapter.id(),List.of()),count);
            }
            catch(IllegalArgumentException e) {
                issue(issues,"INSUFFICIENT_QUESTIONS","questions",e.getMessage());
            }
        }
        return new ReleaseValidation(issues.isEmpty(),issues,new ReleaseValidationContentCounts(chapters.size(),itemCount,points.size(),questions.size()));
    }
    public static List<Resource> resources(ContentDraft d) {
        List<Resource> rows=new ArrayList<>();
        d.catalog().chapters().forEach(c->c.items().forEach(i-> {
            if(i.resource()!=null)rows.add(i.resource());
        }
        ));
        d.knowledge().modules().forEach(m->rows.addAll(m.resources()));
        d.templates().templates().forEach(t-> {
            if(t.resource()!=null)rows.add(t.resource());
        }
        );
        return rows;
    }
}
