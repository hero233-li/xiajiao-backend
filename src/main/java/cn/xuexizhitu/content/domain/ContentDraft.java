package cn.xuexizhitu.content.domain;
import cn.xuexizhitu.contract.WorkflowDtos.*;
import com.fasterxml.jackson.annotation.JsonProperty;
/** Versioned aggregate snapshot; field names preserve existing stored draft JSON. */ public record ContentDraft(CatalogDraft catalog,KnowledgeDraft knowledge,QuestionBankDraft questions,         @JsonProperty("task-templates") TemplateDraft templates,         @JsonProperty("assessment-policy") AssessmentPolicy policy) {
    public ContentDraft withCatalog(CatalogDraft v) {
        return new ContentDraft(v,knowledge,questions,templates,policy);
    }
    public ContentDraft withKnowledge(KnowledgeDraft v) {
        return new ContentDraft(catalog,v,questions,templates,policy);
    }
    public ContentDraft withQuestions(QuestionBankDraft v) {
        return new ContentDraft(catalog,knowledge,v,templates,policy);
    }
    public ContentDraft withTemplates(TemplateDraft v) {
        return new ContentDraft(catalog,knowledge,questions,v,policy);
    }
    public ContentDraft withPolicy(AssessmentPolicy v) {
        return new ContentDraft(catalog,knowledge,questions,templates,v);
    }
}
