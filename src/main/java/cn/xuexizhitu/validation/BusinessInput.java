package cn.xuexizhitu.validation;
import cn.xuexizhitu.common.*;
import com.fasterxml.jackson.databind.*;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.*;

/** Validates the checked-in contract's request shapes before business writes. */
@Component
public class BusinessInput {
    private final JsonNode schemas;
    public BusinessInput(ObjectMapper mapper)throws java.io.IOException {try(var in=getClass().getResourceAsStream("/business-contract-schemas.json")){schemas=mapper.readTree(in);}}
    public void validate(String name,JsonNode value){try{check(schemas.path(name),value,"body");}catch(IllegalArgumentException e){throw new BusinessException(ErrorCode.INVALID_PARAMETER,e.getMessage());}}
    private void bad(String path){throw new IllegalArgumentException(path+"不符合接口约定");}
    private void check(JsonNode schema,JsonNode value,String path){
        if(schema.isMissingNode())throw new IllegalStateException("Missing input schema");
        if(schema.has("$ref")){check(schemas.path(schema.path("$ref").asText().substring("#/components/schemas/".length())),value,path);return;}
        if(value==null||value.isMissingNode()){bad(path);return;}
        for(String combination:List.of("anyOf","oneOf"))if(schema.has(combination)) {int matches=0;for(JsonNode branch:schema.path(combination))try{check(branch,value,path);matches++;}catch(IllegalArgumentException ignored){}if(matches==0||(combination.equals("oneOf")&&matches!=1))bad(path);return;}
        if(schema.has("allOf")){for(JsonNode branch:schema.path("allOf"))check(branch,value,path);return;}
        if(value.isNull()){if(!schema.path("nullable").asBoolean())bad(path);return;}
        if(schema.has("enum")){boolean found=false;for(JsonNode e:schema.path("enum"))if(e.equals(value))found=true;if(!found)bad(path);}
        switch(schema.path("type").asText()) {
            case "object" -> {if(!value.isObject())bad(path);for(JsonNode field:schema.path("required"))if(!value.has(field.asText()))bad(path+"."+field.asText());var it=value.fields();while(it.hasNext()){var entry=it.next();JsonNode prop=schema.path("properties").path(entry.getKey());if(prop.isMissingNode()){if(schema.has("additionalProperties")&&!schema.path("additionalProperties").asBoolean())bad(path+"."+entry.getKey());}else check(prop,entry.getValue(),path+"."+entry.getKey());}}
            case "array" -> {if(!value.isArray())bad(path);if(value.size()<schema.path("minItems").asInt(0)||value.size()>schema.path("maxItems").asInt(Integer.MAX_VALUE))bad(path);if(schema.path("uniqueItems").asBoolean()){Set<JsonNode> seen=new HashSet<>();for(JsonNode e:value)if(!seen.add(e))bad(path);}int index=0;for(JsonNode e:value)check(schema.path("items"),e,path+"["+(index++)+"]");}
            case "boolean" -> {if(!value.isBoolean())bad(path);}
            case "integer","number" -> {if(!value.isNumber()||(schema.path("type").asText().equals("integer")&&!value.isIntegralNumber()))bad(path);if(schema.has("minimum")&&value.decimalValue().compareTo(schema.path("minimum").decimalValue())<0)bad(path);if(schema.has("maximum")&&value.decimalValue().compareTo(schema.path("maximum").decimalValue())>0)bad(path);}
            case "string" -> {if(!value.isTextual())bad(path);String s=value.asText();int length=s.codePointCount(0,s.length());if(length<schema.path("minLength").asInt(0)||length>schema.path("maxLength").asInt(Integer.MAX_VALUE))bad(path);if(schema.has("pattern")&&!java.util.regex.Pattern.compile(schema.path("pattern").asText()).matcher(s).find())bad(path);try{switch(schema.path("format").asText()){case "uuid"->{if(!UUID.fromString(s).toString().equalsIgnoreCase(s))bad(path);}case "date"->{LocalDate.parse(s);}case "date-time"->{Instant.parse(s);}case "uri"->{var uri=java.net.URI.create(s);if(!uri.isAbsolute())bad(path);}default->{}}}catch(RuntimeException e){bad(path);}}
            default -> {}
        }
    }
}
