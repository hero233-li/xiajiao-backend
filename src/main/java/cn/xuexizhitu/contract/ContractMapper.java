package cn.xuexizhitu.contract;
import com.fasterxml.jackson.databind.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
/** JSON conversion is confined to stored snapshots and legacy read projections. */ @Component @RequiredArgsConstructor public class ContractMapper {
    private final ObjectMapper mapper;
    public JsonNode snapshot(Object typed) {
        return mapper.valueToTree(typed);
    }
    public <T> T read(Object projection,Class<T> type) {
        return mapper.convertValue(projection,type);
    }
}
