package cn.xuexizhitu.common;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
public final class BusinessData {
    private BusinessData() {
    }
    public static Map<String,Object> obj(Object... pairs) {
        Map<String,Object> m=new LinkedHashMap<>();
        for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);
        return m;
    }
    public static String publicSource(String value) {
        if(value==null)return "";
        return value.contains("://")||value.contains("/resources/")||value.startsWith("/")||value.contains("storageKey")?"管理员题库":value;
    }
    public static boolean bool(Object v) {
        return Boolean.TRUE.equals(v)||(v instanceof Number n&&n.intValue()!=0);
    }
    public static String uuid() {
        return UUID.randomUUID().toString();
    }
    public static LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Shanghai"));
    }
    public static String str(JsonNode n,String k) {
        return n.path(k).isNull()?null:n.path(k).asText();
    }
    public static Object value(JsonNode n,String k) {
        JsonNode v=n.path(k);
        return v.isNull()||v.isMissingNode()?null:v.isBoolean()?v.booleanValue():v.isNumber()?v.decimalValue():v.asText();
    }
    public static void require(boolean condition,ErrorCode error,String message) {
        if(!condition)throw new BusinessException(error,message);
    }
}
